package com.github.ringmydevice.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

object Permissions {

    // ---- Runtime permission checks ----
    fun has(ctx: Context, perm: String): Boolean =
        ActivityCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(ctx: Context): Boolean {
        val fine = has(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = has(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine || coarse
    }

    fun hasNearbyWifiPermission(ctx: Context): Boolean =
        requiredForNearbyWifi()?.let { has(ctx, it) } ?: true

    fun requiredForBluetoothConnect(): String =
        if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
    fun requiredForFineLocation(): String = Manifest.permission.ACCESS_FINE_LOCATION
    fun requiredForNearbyWifi(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.NEARBY_WIFI_DEVICES else null
    fun requiredForCamera(): String = Manifest.permission.CAMERA
    fun requiredForSmsReceive(): String = Manifest.permission.RECEIVE_SMS
    fun requiredForSmsSend(): String = Manifest.permission.SEND_SMS
    fun requiredForPostNotifications(): String? =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null

    // ---- Special access helpers (open settings screens) ----
    fun hasDndAccess(ctx: Context): Boolean {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }
    fun openDndAccessSettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openAppDetails(ctx: Context) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(i)
    }
}
