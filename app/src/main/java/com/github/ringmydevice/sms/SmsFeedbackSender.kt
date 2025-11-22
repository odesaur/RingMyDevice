package com.github.ringmydevice.sms

import android.app.Activity
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (requestPermissionIfNeeded && context is Activity) {
                ActivityCompat.requestPermissions(context, arrayOf(permission), REQUEST_CODE_SEND_SMS)
            }
            Log.w("RMD", "SEND_SMS permission missing; skipping SMS to $destinationPhoneNumber")
            return Result.PermissionMissing
        }
        val smsManager = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> context.getSystemService(SmsManager::class.java)
            else -> @Suppress("DEPRECATION") SmsManager.getDefault()
        } ?: @Suppress("DEPRECATION") SmsManager.getDefault()
        return runCatching {
            smsManager.sendTextMessage(destinationPhoneNumber, null, messageBody, null, null)
        }.fold(
            onSuccess = { Result.Sent },
            onFailure = {
                Log.e("RMD", "Failed to send SMS feedback", it)
                Result.Failed
            }
        )
    }
}
