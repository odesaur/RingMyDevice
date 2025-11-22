package com.github.ringmydevice.commands

import android.Manifest
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.model.CommandType
import com.github.ringmydevice.data.repo.SettingsRepository
import com.github.ringmydevice.di.AppGraph
import com.github.ringmydevice.permissions.AdminReceiver
import com.github.ringmydevice.service.RingService
import com.github.ringmydevice.sms.SmsFeedbackSender
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
            val baseCommand = settingsRepo.getRmdCommandKeyword()
            val keyword = baseCommand.trim().lowercase()
            if (keyword.isBlank()) return@withContext
            if (tokens.first().lowercase() != keyword) return@withContext
            val feedbackEnabled = settingsRepo.isSmsFeedbackEnabled()

            var index = 1
            val allowedRepo = AppGraph.allowedRepo
            val requiresTrustCheck = source == CommandSource.SMS || source == CommandSource.NOTIFICATION_REPLY
            var authorized = !requiresTrustCheck
            var trustedForFeedback = false
            if (requiresTrustCheck) {
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
                        logResult(sender, unauthorizedResult())
                        return@withContext
                    }
                    index++
                    allowedRepo.grantTemporaryAccess(sender ?: "")
                    authorized = true
                }
                trustedForFeedback = authorized
            }

            if (!authorized) {
                logResult(sender, unauthorizedResult())
                return@withContext
            }

            val commandToken = tokens.getOrNull(index) ?: return@withContext
            val args = tokens.drop(index + 1)
            val commandId = mapToCommandId(commandToken)
            val result = when (commandId) {
                CommandId.NODISTURB -> dispatchDoNotDisturb(appContext, args, source)
                CommandId.RING -> {
                    val longRequested = commandToken.contains("long") || args.any { it.equals("long", ignoreCase = true) }
                    dispatchRing(appContext, longRequested, source)
                }
                CommandId.RINGER_MODE -> dispatchRingerMode(appContext, args, source)
                CommandId.STATS -> dispatchStats(appContext, source)
                CommandId.GPS -> dispatchGps(appContext, args, source)
                CommandId.LOCATE -> dispatchLocate(appContext, args, source)
                CommandId.LOCK -> dispatchLock(appContext, args, source)
                CommandId.HELP -> dispatchHelp(baseCommand)
                CommandId.UNKNOWN -> CommandExecutionResult(
                    CommandId.UNKNOWN,
                    CommandStatus.INVALID_ARGUMENTS,
                    feedbackMessage = "Unknown command: $commandToken",
                    logNotes = "Unknown command: $commandToken"
                )
            }

            logResult(sender, result)
            if (trustedForFeedback && feedbackEnabled) {
                sendFeedbackForCommand(appContext, sender, baseCommand, args, result)
            }
        }.onFailure {
            Log.e("RMD", "Command handling failed", it)
        }
    }

    private suspend fun dispatchRing(
        context: Context,
        longRequested: Boolean,
        source: CommandSource
    ): CommandExecutionResult {
        val seconds = if (longRequested) SettingsRepository.LONG_RING_SECONDS else SettingsRepository.DEFAULT_RING_SECONDS
        val settingsRepo = AppGraph.settingsRepo
        if (!settingsRepo.isRingEnabled()) {
            val msg = "Ring command ignored (feature disabled)"
            return CommandExecutionResult(CommandId.RING, CommandStatus.FAILURE, feedbackMessage = msg, logNotes = msg)
        }
        val ringtoneUri = settingsRepo.getRingtoneUri()
        if (!hasNotificationPermission(context)) {
            val msg = "The ring command could not be executed because required permissions are missing."
            if (source == CommandSource.IN_APP) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
            return CommandExecutionResult(
                CommandId.RING,
                CommandStatus.PERMISSION_MISSING,
                feedbackMessage = msg,
                logNotes = "Notifications disabled. Enable notification/FG permissions to ring."
            )
        }

        val serviceIntent = Intent(context, RingService::class.java).apply {
            putExtra(RingService.EXTRA_SECONDS, seconds)
            putExtra(RingService.EXTRA_RINGTONE_URI, ringtoneUri)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        return CommandExecutionResult(
            CommandId.RING,
            CommandStatus.SUCCESS,
            feedbackMessage = "The device should now be ringing.",
            logNotes = "Triggered ring for ${seconds}s"
        )
    }

    private suspend fun dispatchDoNotDisturb(
        context: Context,
        args: List<String>,
        source: CommandSource
    ): CommandExecutionResult {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager == null || !manager.isNotificationPolicyAccessGranted) {
            notifyUser(context, source, "Do Not Disturb access missing")
            return CommandExecutionResult(
                CommandId.NODISTURB,
                CommandStatus.PERMISSION_MISSING,
                feedbackMessage = "Do Not Disturb access missing",
                logNotes = "nodisturb failed - missing access"
            )
        }
        val target = args.firstOrNull()?.lowercase()
        val filter = when (target) {
            "on" -> NotificationManager.INTERRUPTION_FILTER_NONE
            "off" -> NotificationManager.INTERRUPTION_FILTER_ALL
            else -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        manager.setInterruptionFilter(filter)
        notifyUser(context, source, "Do Not Disturb set to ${target ?: "priority"}")
        return CommandExecutionResult(
            CommandId.NODISTURB,
            CommandStatus.SUCCESS,
            feedbackMessage = "Do Not Disturb mode has been updated.",
            logNotes = "nodisturb set to $target"
        )
    }

    private suspend fun dispatchRingerMode(
        context: Context,
        args: List<String>,
        source: CommandSource
    ): CommandExecutionResult {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio == null) {
            notifyUser(context, source, "Audio service unavailable")
            return CommandExecutionResult(
                CommandId.RINGER_MODE,
                CommandStatus.FAILURE,
                feedbackMessage = "Audio service unavailable",
                logNotes = "ringermode failed - no audio service"
            )
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
        return CommandExecutionResult(
            CommandId.RINGER_MODE,
            CommandStatus.SUCCESS,
            feedbackMessage = "The ringer mode has been changed.",
            logNotes = "ringermode set to $target"
        )
    }

    private suspend fun dispatchStats(context: Context, source: CommandSource): CommandExecutionResult {
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
        notifyUser(context, source, message)
        return CommandExecutionResult(
            CommandId.STATS,
            CommandStatus.SUCCESS,
            feedbackMessage = message,
            logNotes = "stats provided"
        )
    }

    private suspend fun dispatchGps(
        context: Context,
        args: List<String>,
        source: CommandSource
    ): CommandExecutionResult {
        val target = args.firstOrNull()?.lowercase()
        if (target == null) {
            val msg = "GPS command requires on/off"
            notifyUser(context, source, msg)
            return CommandExecutionResult(
                CommandId.GPS,
                CommandStatus.INVALID_ARGUMENTS,
                feedbackMessage = msg,
                logNotes = "gps failed - missing arg"
            )
        }
        val canWriteSettings = canModifyLocationMode(context)
        val mode = when (target) {
            "on" -> Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            "off" -> Settings.Secure.LOCATION_MODE_OFF
            else -> Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
        }
        @Suppress("DEPRECATION")
        val success = canWriteSettings && Settings.Secure.putInt(context.contentResolver, Settings.Secure.LOCATION_MODE, mode)
        val msg = when {
            success -> "GPS ${if (target == "off") "disabled" else "enabled"}"
            !canWriteSettings -> "Android prevents changing GPS mode; please adjust in system settings."
            else -> "Unable to modify GPS"
        }
        notifyUser(context, source, msg)
        return CommandExecutionResult(
            CommandId.GPS,
            status = if (success) CommandStatus.SUCCESS else CommandStatus.FAILURE,
            feedbackMessage = msg,
            logNotes = msg
        )
    }

    private suspend fun dispatchLocate(
        context: Context,
        args: List<String>,
        source: CommandSource
    ): CommandExecutionResult {
        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            val msg = "Location service unavailable"
            notifyUser(context, source, msg)
            return CommandExecutionResult(
                CommandId.LOCATE,
                CommandStatus.FAILURE,
                feedbackMessage = msg,
                logNotes = "locate failed - service unavailable"
            )
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
            val msg = "Location permission missing"
            notifyUser(context, source, msg)
            return CommandExecutionResult(
                CommandId.LOCATE,
                CommandStatus.PERMISSION_MISSING,
                feedbackMessage = "$msg.",
                logNotes = "locate failed - permission missing"
            )
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
        notifyUser(context, source, message)
        return CommandExecutionResult(
            CommandId.LOCATE,
            CommandStatus.SUCCESS,
            feedbackMessage = message,
            logNotes = "locate result sent"
        )
    }

    private suspend fun dispatchLock(
        context: Context,
        args: List<String>,
        source: CommandSource
    ): CommandExecutionResult {
        val manager = context.getSystemService(DevicePolicyManager::class.java)
        val component = ComponentName(context, AdminReceiver::class.java)
        if (manager == null || !manager.isAdminActive(component)) {
            val msg = "Device admin not active"
            notifyUser(context, source, msg)
            return CommandExecutionResult(
                CommandId.LOCK,
                CommandStatus.PERMISSION_MISSING,
                feedbackMessage = msg,
                logNotes = "lock failed - admin not active"
            )
        }
        manager.lockNow()
        val message = args.joinToString(" ").takeIf { it.isNotBlank() }
        if (!message.isNullOrBlank()) {
            notifyUser(context, source, "Lock message: $message")
        }
        val notes = if (message.isNullOrBlank()) "lock executed" else "lock executed with message: $message"
        return CommandExecutionResult(
            CommandId.LOCK,
            CommandStatus.SUCCESS,
            feedbackMessage = "The device has been locked.",
            logNotes = notes
        )
    }

    private suspend fun dispatchHelp(baseCommand: String): CommandExecutionResult {
        val message = CommandHelpResponder.buildHelpMessageFromCommands(baseCommand)
        return CommandExecutionResult(
            CommandId.HELP,
            CommandStatus.SUCCESS,
            feedbackMessage = message,
            logNotes = "Sent help response"
        )
    }

    private suspend fun sendFeedbackForCommand(
        context: Context,
        sender: String?,
        baseCommand: String,
        args: List<String>,
        result: CommandExecutionResult
    ) {
        if (sender.isNullOrBlank()) return
        val message = CommandFeedbackBuilder.buildFeedbackMessage(baseCommand, args, result) ?: return
        val smsResult = SmsFeedbackSender.send(context, sender, message)
        if (smsResult == SmsFeedbackSender.Result.PermissionMissing) {
            Log.w("RMD", "SMS feedback skipped due to missing SEND_SMS permission")
        }
    }

    private suspend fun logResult(sender: String?, result: CommandExecutionResult) {
        withContext(Dispatchers.IO) {
            AppGraph.commandRepo.log(
                CommandLog(
                    type = commandTypeFor(result.commandId),
                    from = sender,
                    success = result.status == CommandStatus.SUCCESS,
                    notes = result.logNotes ?: result.feedbackMessage
                )
            )
        }
    }

    private fun mapToCommandId(token: String): CommandId = when (token.lowercase()) {
        "nodisturb" -> CommandId.NODISTURB
        "ring", "ringlong", "ring-long" -> CommandId.RING
        "ringermode" -> CommandId.RINGER_MODE
        "stats" -> CommandId.STATS
        "gps" -> CommandId.GPS
        "locate" -> CommandId.LOCATE
        "lock" -> CommandId.LOCK
        "help" -> CommandId.HELP
        else -> CommandId.UNKNOWN
    }

    private fun commandTypeFor(commandId: CommandId): CommandType = when (commandId) {
        CommandId.RING -> CommandType.RING
        CommandId.LOCATE -> CommandType.LOCATE
        CommandId.NODISTURB -> CommandType.NODISTURB
        CommandId.RINGER_MODE -> CommandType.RINGER_MODE
        CommandId.STATS -> CommandType.STATS
        CommandId.GPS -> CommandType.GPS
        CommandId.LOCK -> CommandType.LOCK
        CommandId.HELP -> CommandType.HELP
        CommandId.UNKNOWN -> CommandType.UNKNOWN
    }

    private fun unauthorizedResult(): CommandExecutionResult =
        CommandExecutionResult(
            CommandId.UNKNOWN,
            CommandStatus.FAILURE,
            feedbackMessage = "Unauthorized sender",
            logNotes = "Unauthorized sender"
        )
}

private fun canModifyLocationMode(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.System.canWrite(context)
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (!enabled) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
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
