package com.github.ringmydevice.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest

object CameraEligibility {

    fun canStartCameraFgs(context: Context): Boolean {
        val hasCameraPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) return false

        val am = context.getSystemService(ActivityManager::class.java)
        val processes = am.runningAppProcesses ?: return false

        val myPackage = context.packageName
        val isForeground = processes.any {
            it.processName == myPackage &&
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }

        return isForeground
    }
}