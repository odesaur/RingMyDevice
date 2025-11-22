package com.github.ringmydevice.commands

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.model.CommandType
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.data.repo.SettingsRepository
import com.github.ringmydevice.di.AppGraph
import com.github.ringmydevice.service.RingService
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class CommandSource { SMS, NOTIFICATION_REPLY, IN_APP, MESHTASTIC }

object CommandProcessor {
    suspend fun handle(
        context: Context,
        sender: String?,
        rawMessage: String,
        source: CommandSource = CommandSource.SMS
    ) = withContext(Dispatchers.Default) {
        runCatching {
            val appContext = context.applicationContext
            val normalizedMessage = rawMessage.replace("\n", " ").trim()
            if (normalizedMessage.isEmpty()) return@withContext
            val tokens = normalizedMessage.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return@withContext

            val settingsRepo = AppGraph.settingsRepo
            val keyword = settingsRepo.getRmdCommandKeyword().trim().lowercase()
            if (keyword.isBlank()) return@withContext
            if (tokens.first().lowercase() != keyword) return@withContext

            var index = 1
            val allowedRepo = AppGraph.allowedRepo
            var authorized = source != CommandSource.SMS && source != CommandSource.NOTIFICATION_REPLY
            if (!authorized) {
                authorized = allowedRepo.isAllowed(sender)
                if (!authorized && !sender.isNullOrBlank() && !allowedRepo.hasContacts()) {
                    allowedRepo.add(AllowedContact(name = sender, phoneNumber = sender))
                    authorized = true
                }
                if (!authorized) {
                    val pinEnabled = settingsRepo.isPinEnabled()
                    val storedPin = settingsRepo.getPin().trim()
                    val providedPin = tokens.getOrNull(index)?.lowercase()
                    if (!pinEnabled || storedPin.isBlank() || providedPin != storedPin.lowercase()) {
                        logResult(sender, CommandType.UNKNOWN, success = false, notes = "Unauthorized sender")
                        return@withContext
                    }
                    index++
                    allowedRepo.grantTemporaryAccess(sender ?: "")
                    authorized = true
                }
            }

            val command = tokens.getOrNull(index) ?: return@withContext
            val args = tokens.drop(index + 1)
            when (command.lowercase()) {
                "ring", "ringlong", "ring-long" -> {
                    val longRequested = command.contains("long") || args.any { it.equals("long", ignoreCase = true) }
                    dispatchRing(appContext, sender, longRequested, source)
                }
                "locate", "location", "where" -> {
                    dispatchLocate(appContext, sender, source)
                }

                "photo", "snap", "camera" -> {
                    dispatchPhoto(appContext, sender, source)
                }

                "wipe", "factoryreset", "factory-reset" -> {
                    dispatchWipe(appContext, sender, source)
                }
                else -> logResult(sender, CommandType.UNKNOWN, success = false, notes = "Unknown command: $command")
            }
        }.onFailure {
            Log.e("RMD", "Command handling failed", it)
        }
    }

    private suspend fun dispatchRing(
        context: Context,
        sender: String?,
        longRequested: Boolean,
        source: CommandSource
    ) {
        val seconds = if (longRequested) SettingsRepository.LONG_RING_SECONDS else SettingsRepository.DEFAULT_RING_SECONDS
        val settingsRepo = AppGraph.settingsRepo
        if (!settingsRepo.isRingEnabled()) {
            logResult(sender, CommandType.RING, success = false, notes = "Ring command ignored (feature disabled)")
            return
        }
        val ringtoneUri = AppGraph.settingsRepo.getRingtoneUri()
        if (!hasNotificationPermission(context)) {
            val msg = "Notifications disabled. Enable notification/FG permissions to ring."
            if (source == CommandSource.IN_APP) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
            logResult(sender, CommandType.RING, success = false, notes = msg)
            return
        }

        val serviceIntent = Intent(context, RingService::class.java).apply {
            putExtra(RingService.EXTRA_SECONDS, seconds)
            putExtra(RingService.EXTRA_RINGTONE_URI, ringtoneUri)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        logResult(sender, CommandType.RING, success = true, notes = "Triggered ring for ${seconds}s")
    }

    private suspend fun dispatchLocate(
        context: Context,
        sender: String?,
        source: CommandSource
    ) {
        if (!hasLocationPermission(context)) {
            val msg = "Locate failed: location permission not granted."
            if (source == CommandSource.IN_APP) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
            logResult(sender, CommandType.LOCATE, success = false, notes = msg)
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)
        val loc = getLastKnownLocationSuspend(fused)

        if (loc == null) {
            val msg = "Locate failed: no recent GPS fix."
            if (source == CommandSource.IN_APP) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
            logResult(sender, CommandType.LOCATE, success = false, notes = msg)
            return
        }

        val lat = loc.latitude
        val lon = loc.longitude

        val reply = """
        RMD Location:
        $lat, $lon
        https://maps.google.com/?q=$lat,$lon
        """.trimIndent()

        if (!sender.isNullOrBlank() &&
            (source == CommandSource.SMS || source == CommandSource.NOTIFICATION_REPLY)
        ) {
            sendSmsReply(context, sender, reply)
        }

        logResult(sender, CommandType.LOCATE, success = true, notes = "Location sent ($lat,$lon)", lat = lat, lon = lon)
    }

    // just stub & log.
    private suspend fun dispatchPhoto(
        context: Context,
        sender: String?,
        source: CommandSource
    ) {
        val settingsRepo = AppGraph.settingsRepo


        val msg = "PHOTO command received – feature planned for final milestone."
        if (source == CommandSource.IN_APP) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
        // Later: trigger camera capture via a service / Activity:
        logResult(sender, CommandType.PHOTO, success = false, notes = msg)
    }

    // just stub & log.
    private suspend fun dispatchWipe(
        context: Context,
        sender: String?,
        source: CommandSource
    ) {
        val msg = "WIPE command received – feature planned for final milestone."
        if (source == CommandSource.IN_APP) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }

        // In final: check DeviceAdmin / policyManager.isAdminActive()
        // and call policyManager.wipeData(0) inside a dedicated service.

        logResult(sender, CommandType.WIPE, success = false, notes = msg)
    }


    @SuppressLint("MissingPermission") // we check in dispatchLocate()
    private suspend fun getLastKnownLocationSuspend(
        client: FusedLocationProviderClient
    ) = suspendCancellableCoroutine<android.location.Location?> { cont ->
        try {
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (e: Exception) {
            cont.resume(null)
        }
    }

    // move to utils
    private fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }


    private fun sendSmsReply(context: android.content.Context, phoneNumber: String, text: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, text, null, null)
        } catch (e: Exception) {
            Log.e("RMD", "Failed to send SMS reply", e)
        }
    }

    private suspend fun logResult(
        sender: String?,
        type: CommandType,
        success: Boolean,
        notes: String?,
        lat: Double? = null,
        lon: Double? = null
    ) {
        withContext(Dispatchers.IO) {
            AppGraph.commandRepo.log(
                CommandLog(
                    type = type,
                    from = sender,
                    success = success,
                    notes = notes,
                    lat = lat,
                    lon = lon
                )
            )
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (!enabled) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
