package com.github.ringmydevice.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.github.ringmydevice.commands.CommandProcessor
import com.github.ringmydevice.commands.CommandSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) {
            pendingResult.finish()
            return
        }
        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        val sender = messages.first().displayOriginatingAddress

        scope.launch {
            try {
                CommandProcessor.handle(appContext, sender, body, CommandSource.SMS)
            } catch (t: Throwable) {
                Log.e("RMD", "Failed to handle SMS command", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
