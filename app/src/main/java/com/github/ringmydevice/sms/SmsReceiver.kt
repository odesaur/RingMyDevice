package com.github.ringmydevice.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stub: real handling will parse and dispatch commands.
        Log.d("RMD", "SmsReceiver invoked with action=${intent.action}")
    }
}
