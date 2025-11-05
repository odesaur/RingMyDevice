package com.github.ringmydevice.permissions

import android.content.Context
object DoNotDisturbAccessPermission {
    fun isGranted(ctx: Context): Boolean = Permissions.hasDndAccess(ctx)
    fun request(ctx: Context) = Permissions.openDndAccessSettings(ctx)
}
