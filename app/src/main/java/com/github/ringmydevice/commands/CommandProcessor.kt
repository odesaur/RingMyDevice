package com.github.ringmydevice.commands

import android.Manifest
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.telephony.SmsManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.model.CommandType
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.data.repo.SettingsRepository
import com.github.ringmydevice.di.AppGraph
import com.github.ringmydevice.service.RingService
import com.github.ringmydevice.permissions.AdminReceiver
import com.github.ringmydevice.permissions.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                "nodisturb" -> dispatchDoNotDisturb(appContext, sender, args, source)
                "ring", "ringlong", "ring-long" -> {
                    val longRequested = command.contains("long") || args.any { it.equals("long", ignoreCase = true) }
                    dispatchRing(appContext, sender, longRequested, source)
                }
                "ringermode" -> dispatchRingerMode(appContext, sender, args, source)
                "stats" -> dispatchStats(appContext, sender, source)
                "gps" -> dispatchGps(appContext, sender, args, source)
                "locate" -> dispatchLocate(appContext, sender, args, source)
                "lock" -> dispatchLock(appContext, sender, args, source)
                "help" -> {
                    dispatchHelp(appContext, sender)
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

    private suspend fun logResult(sender: String?, type: CommandType, success: Boolean, notes: String?) {
        withContext(Dispatchers.IO) {
            AppGraph.commandRepo.log(
                CommandLog(
                    type = type,
                    from = sender,
                    success = success,
                    notes = notes
                )
            )
        }
    }

    private suspend fun dispatchDoNotDisturb(
        context: Context,
        sender: String?,
        args: List<String>,
        source: CommandSource
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager == null || !manager.isNotificationPolicyAccessGranted) {
            notifyUser(context, source, "Do Not Disturb access missing")
            logResult(sender, CommandType.UNKNOWN, success = false, notes = "nodisturb failed - missing access")
            return
        }
        val target = args.firstOrNull()?.lowercase()
        val filter = when (target) {
            "on" -> NotificationManager.INTERRUPTION_FILTER_NONE
            "off" -> NotificationManager.INTERRUPTION_FILTER_ALL
            else -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        manager.setInterruptionFilter(filter)
        notifyUser(context, source, "Do Not Disturb set to ${target ?: "priority"}")
        logResult(sender, CommandType.UNKNOWN, success = true, notes = "nodisturb set to $target")
    }

    private suspend fun dispatchRingerMode(
        context: Context,
        sender: String?,
        args: List<String>,
        source: CommandSource
    ) {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio == null) {
            notifyUser(context, source, "Audio service unavailable")
            logResult(sender, CommandType.UNKNOWN, success = false, notes = "ringermode failed - no audio service")
            return
        }
        val target = args.firstOrNull()?.lowercase() ?: "normal"
        val mode = when (target) {
            "normal" -> AudioManager.RINGER_MODE_NORMAL
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            "silent" -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        audio.ringerMode = mode
        if (target == "silent") {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm?.isNotificationPolicyAccessGranted == true) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        }
        notifyUser(context, source, "Ringer mode set to $target")
        logResult(sender, CommandType.UNKNOWN, success = true, notes = "ringermode set to $target")
    }

    private suspend fun dispatchStats(context: Context, sender: String?, source: CommandSource) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        val builder = StringBuilder()
        builder.append("Metered: ${cm?.isActiveNetworkMetered ?: false}\n")
        if (capabilities != null) {
            builder.append("Capabilities: ")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) builder.append("Wi-Fi ")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) builder.append("Cell ")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) builder.append("BT ")
            builder.append("\n")
            builder.append("Bandwidth: down=${capabilities.linkDownstreamBandwidthKbps}kbps up=${capabilities.linkUpstreamBandwidthKbps}kbps")
        }
        val message = builder.toString().ifBlank { "No network data" }
        sendSmsResponse(context, sender, message)
        notifyUser(context, source, message)
        logResult(sender, CommandType.UNKNOWN, success = true, notes = "stats provided")
    }

    private suspend fun dispatchGps(
        context: Context,
        sender: String?,
        args: List<String>,
        source: CommandSource
    ) {
        val target = args.firstOrNull()?.lowercase()
        if (target == null) {
            notifyUser(context, source, "GPS command requires on/off")
            logResult(sender, CommandType.UNKNOWN, success = false, notes = "gps failed - missing arg")
            return
        }
        val mode = when (target) {
            "on" -> Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            "off" -> Settings.Secure.LOCATION_MODE_OFF
            else -> Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
        }
        @Suppress("DEPRECATION")
        val success = Settings.Secure.putInt(context.contentResolver, Settings.Secure.LOCATION_MODE, mode)
        val msg = if (success) "GPS ${if (target == "off") "disabled" else "enabled"}" else "Unable to modify GPS"
        notifyUser(context, source, msg)
        logResult(sender, CommandType.UNKNOWN, success = success, notes = msg)
    }

    private suspend fun dispatchLocate(
        context: Context,
        sender: String?,
        args: List<String>,
        source: CommandSource
    ) {
        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            notifyUser(context, source, "Location service unavailable")
            logResult(sender, CommandType.LOCATE, success = false, notes = "locate failed - service unavailable")
            return
        }
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            notifyUser(context, source, "Location permission missing")
            logResult(sender, CommandType.LOCATE, success = false, notes = "locate failed - permission missing")
            return
        }
        val providers = when (args.firstOrNull()?.lowercase()) {
            "gps" -> listOf(LocationManager.GPS_PROVIDER)
            "cell" -> listOf(LocationManager.NETWORK_PROVIDER)
            else -> locationManager.allProviders
        }
        val builder = StringBuilder()
        providers.forEach { provider ->
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            if (loc != null) {
                builder.append(provider)
                    .append(": ")
                    .append(loc.latitude)
                    .append(", ")
                    .append(loc.longitude)
                    .append(" acc ")
                    .append(loc.accuracy)
                    .append("m\n")
            }
        }
        val message = builder.toString().ifBlank { "No location available" }
        sendSmsResponse(context, sender, message)
        notifyUser(context, source, message)
        logResult(sender, CommandType.LOCATE, success = true, notes = "locate result sent")
    }

    private suspend fun dispatchLock(
        context: Context,
        sender: String?,
        args: List<String>,
        source: CommandSource
    ) {
        val manager = context.getSystemService(DevicePolicyManager::class.java)
        val component = ComponentName(context, AdminReceiver::class.java)
        if (manager == null || !manager.isAdminActive(component)) {
            notifyUser(context, source, "Device admin not active")
            logResult(sender, CommandType.UNKNOWN, success = false, notes = "lock failed - admin not active")
            return
        }
        manager.lockNow()
        val message = args.joinToString(" ").takeIf { it.isNotBlank() }
        if (!message.isNullOrBlank()) {
            notifyUser(context, source, "Lock message: $message")
        }
        logResult(sender, CommandType.UNKNOWN, success = true, notes = "lock executed")
    }

    private suspend fun dispatchHelp(context: Context, sender: String?) {
        if (sender.isNullOrBlank()) return
        val permission = Permissions.requiredForSmsSend()
        if (!Permissions.has(context, permission)) {
            Log.w("RMD", "Cannot send help SMS; SEND_SMS permission missing")
            notifyUser(context, CommandSource.IN_APP, "SEND_SMS permission missing")
            return
        }
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val message = CommandHelpResponder.buildHelpMessageFromCommands()
        smsManager?.sendTextMessage(sender, null, message, null, null)
        logResult(sender, CommandType.UNKNOWN, success = true, notes = "Sent help response")
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

private fun notifyUser(context: Context, source: CommandSource, message: String) {
    if (source != CommandSource.IN_APP) return
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

private fun sendSmsResponse(context: Context, recipient: String?, message: String) {
    if (recipient.isNullOrBlank() || message.isBlank()) return
    val permission = Permissions.requiredForSmsSend()
    if (!Permissions.has(context, permission)) return
    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }
    smsManager?.sendTextMessage(recipient, null, message, null, null)
}
