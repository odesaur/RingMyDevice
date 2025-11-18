package com.github.ringmydevice.commands
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.model.CommandType
import com.github.ringmydevice.data.repo.SettingsRepository
import com.github.ringmydevice.di.AppGraph
import com.github.ringmydevice.service.RingService
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
                dispatchRing(context, sender, longRequested)
            }
            else -> logResult(sender, CommandType.UNKNOWN, success = false, notes = "Unknown command: $command")
        }
    }

    private suspend fun dispatchRing(
        context: Context,
        sender: String?,
        longRequested: Boolean
    ) {
        val seconds = if (longRequested) SettingsRepository.LONG_RING_SECONDS else SettingsRepository.DEFAULT_RING_SECONDS
        val ringtoneUri = AppGraph.settingsRepo.getRingtoneUri()
        val intent = Intent(context, RingService::class.java).apply {
            putExtra(RingService.EXTRA_SECONDS, seconds)
            putExtra(RingService.EXTRA_RINGTONE_URI, ringtoneUri)
        }
        ContextCompat.startForegroundService(context, intent)
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
}
