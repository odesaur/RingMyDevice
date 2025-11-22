package com.github.ringmydevice.sms

import android.app.Activity
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.github.ringmydevice.permissions.Permissions

object SmsFeedbackSender {
    private const val REQUEST_CODE_SEND_SMS = 1024

    sealed class Result {
        object Sent : Result()
        object PermissionMissing : Result()
        object Failed : Result()
    }

    fun send(
        context: Context,
        destinationPhoneNumber: String?,
        messageBody: String,
        requestPermissionIfNeeded: Boolean = false
    ): Result {
        if (destinationPhoneNumber.isNullOrBlank() || messageBody.isBlank()) return Result.Failed
        val permission = Permissions.requiredForSmsSend()
        if (!Permissions.has(context, permission)) {
            if (requestPermissionIfNeeded && context is Activity) {
                ActivityCompat.requestPermissions(context, arrayOf(permission), REQUEST_CODE_SEND_SMS)
            }
            Log.w("RMD", "SEND_SMS permission missing; skipping SMS to $destinationPhoneNumber")
            return Result.PermissionMissing
        }
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        return runCatching {
            smsManager?.sendTextMessage(destinationPhoneNumber, null, messageBody, null, null)
        }.fold(
            onSuccess = { if (smsManager != null) Result.Sent else Result.Failed },
            onFailure = {
                Log.e("RMD", "Failed to send SMS feedback", it)
                Result.Failed
            }
        )
    }
}
