package com.github.ringmydevice.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager

object MmsSender {

    fun sendMms(
        context: Context,
        to: String,
        imageUri: Uri,
        text: String
    ) {
        val smsManager = SmsManager.getDefault()

        val sentIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("RMD_MMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val config = Bundle().apply {
            putString("subject", text)
            putString("text", text)
        }

        smsManager.sendMultimediaMessage(
            context,
            imageUri,
            to,
            config,
            sentIntent
        )
    }
}