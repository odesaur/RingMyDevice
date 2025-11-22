package com.github.ringmydevice.commands

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
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
                "ring", "ringlong", "ring-long" -> {
                    val longRequested = command.contains("long") || args.any { it.equals("long", ignoreCase = true) }
                    dispatchRing(appContext, sender, longRequested, source)
                }
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

    private suspend fun dispatchHelp(context: Context, sender: String?) {
        if (sender.isNullOrBlank()) return
        val permission = Permissions.requiredForSmsSend()
        if (!Permissions.has(context, permission)) {
            Log.w("RMD", "Cannot send help SMS; SEND_SMS permission missing")
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
