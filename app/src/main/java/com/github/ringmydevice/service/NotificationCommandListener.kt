package com.github.ringmydevice.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.github.ringmydevice.commands.CommandProcessor
import com.github.ringmydevice.commands.CommandSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationCommandListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val meshtasticPackage = "com.geeksville.mesh"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val message = extractMessage(sbn) ?: return
        val sender = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: sbn.tag ?: sbn.packageName
        val source = if (sbn.packageName == meshtasticPackage) CommandSource.MESHTASTIC else CommandSource.NOTIFICATION_REPLY
        scope.launch {
            CommandProcessor.handle(
                context = applicationContext,
                sender = sender,
                rawMessage = message,
                source = source
            )
        }
    }

    private fun extractMessage(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val candidates = listOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUMMARY_TEXT
        )
        candidates.forEach { key ->
            val text = extras?.getCharSequence(key)?.toString()?.trim()
            if (!text.isNullOrBlank()) return text
        }
        val ticker = sbn.notification.tickerText?.toString()?.trim()
        return ticker?.takeIf { it.isNotBlank() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
