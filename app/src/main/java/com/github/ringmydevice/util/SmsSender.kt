package com.github.ringmydevice.util

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsSender {

    private const val TAG = "RMD_SMS"

    fun sendText(
        context: Context,
        to: String?,
        message: String?
    ) {
        if (to.isNullOrBlank() || message.isNullOrBlank()) {
            Log.e(TAG, "Invalid SMS arguments: to=$to message=$message")
            return
        }

        try {

            val smsManager: SmsManager = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    context.getSystemService(SmsManager::class.java)
                }
                else -> {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } ?: run {
                Log.e(TAG, "SmsManager instance is null")
                return
            }

            val parts = smsManager.divideMessage(message)

            if (parts.size <= 1) {
                smsManager.sendTextMessage(
                    to,
                    null,
                    message,
                    null,
                    null
                )
            } else {
                smsManager.sendMultipartTextMessage(
                    to,
                    null,
                    parts,
                    null,
                    null
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
}