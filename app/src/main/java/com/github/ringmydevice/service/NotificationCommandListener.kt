package com.github.ringmydevice.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.github.ringmydevice.commands.CommandProcessor
import com.github.ringmydevice.commands.CommandSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationCommandListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val extras = sbn.notification.extras
        val body = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (body.isEmpty()) return
        val sender = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: sbn.tag ?: sbn.packageName
        scope.launch {
            CommandProcessor.handle(
                context = applicationContext,
                sender = sender,
                rawMessage = body,
                source = CommandSource.NOTIFICATION_REPLY
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
