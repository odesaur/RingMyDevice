package com.github.ringmydevice.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/** Returns a function you can call with a permission string to request it. */
@Composable
fun rememberPermissionRequester(
    onResult: (Boolean) -> Unit = {}
): (String) -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onResult(granted) }
    return { permission -> launcher.launch(permission) }
}
