package com.github.ringmydevice.ui.commands

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.ringmydevice.permissions.DoNotDisturbAccessPermission
import com.github.ringmydevice.permissions.Permissions
import com.github.ringmydevice.permissions.Permissions.openAppDetails
import com.github.ringmydevice.permissions.rememberPermissionRequester
import com.github.ringmydevice.ui.model.CommandItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandScreen(modifier: Modifier = Modifier) {
    val commandItems = remember {
        listOf(
            CommandItem(
                title = "bluetooth [on | off]",
                description = "Toggle Bluetooth on and off",
                requiredPermissions = listOf("Connect Bluetooth"),
                icon = Icons.Outlined.Bluetooth,
                exampleSyntax = "rmd bluetooth on"
            ),
            CommandItem(
                title = "camera [front | back]",
                description = "Take a picture",
                requiredPermissions = listOf("Camera"),
                icon = Icons.Outlined.CameraAlt,
                exampleSyntax = "rmd camera front"
            ),
            CommandItem(
                title = "delete <pin> [dryrun]",
                description = "Factory-reset the device",
                requiredPermissions = listOf("Device admin"),
                icon = Icons.Outlined.DeleteForever,
                exampleSyntax = "rmd delete 1234"
            )
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { RingCommandCardUI() }
        item { RingerModeCommandCardUI() }
        item { StatsCommandCardUI() }

        items(commandItems) { item ->
            CommandCard(item)
        }
    }
}


@Composable
private fun CommandCard(item: CommandItem) {
    val ctx = LocalContext.current
    val requestPermission = rememberPermissionRequester {
        android.util.Log.d("RMD", "Permission result: $it")
    }

    // State: whether permission is currently granted
    val permissionGranted = remember {
        mutableStateOf(
            when {
                item.title.startsWith("bluetooth") ->
                    Permissions.has(ctx, Permissions.requiredForBluetoothConnect())
                item.title.startsWith("camera") ->
                    Permissions.has(ctx, Permissions.requiredForCamera())
                else -> false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Details")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Required permissions", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(item.requiredPermissions.joinToString(), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            Text("SMS syntax", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(item.exampleSyntax, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = {
                    when {
                        item.title.startsWith("bluetooth") -> {
                            val p = Permissions.requiredForBluetoothConnect()
                            if (permissionGranted.value) {
                                // "Revoke" path — open app settings so user can revoke manually
                                openAppDetails(ctx)
                            } else {
                                requestPermission(p)
                            }
                            permissionGranted.value = Permissions.has(ctx, p)
                        }

                        item.title.startsWith("camera") -> {
                            val p = Permissions.requiredForCamera()
                            if (permissionGranted.value) {
                                openAppDetails(ctx)
                            } else {
                                requestPermission(p)
                            }
                            permissionGranted.value = Permissions.has(ctx, p)
                        }

                        item.title.startsWith("delete") -> {
                            DoNotDisturbAccessPermission.request(ctx)
                        }
                    }
                }) {
                    Text(if (permissionGranted.value) "Revoke" else "Grant")
                }
            }
        }
    }
}

@Composable
private fun RingCommandCardUI() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }

    val refreshPermissions = remember(context) {
        {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            dndGranted = manager.isNotificationPolicyAccessGranted
            overlayGranted = Settings.canDrawOverlays(context)
        }
    }

    LaunchedEffect(Unit) { refreshPermissions() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val allGranted = dndGranted && overlayGranted
    var showRevokeMenu by remember { mutableStateOf(false) }
    val buttonLabel = if (allGranted) "Revoke" else "Grant"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ring [long]", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Make the device ring", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Details")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Required permissions", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ListItem(
                headlineContent = { Text("Do Not Disturb access") },
                trailingContent = { if (dndGranted) Icon(Icons.Outlined.Check, null) }
            )
            ListItem(
                headlineContent = { Text("Display over other apps") },
                trailingContent = { if (overlayGranted) Icon(Icons.Outlined.Check, null) }
            )
            Spacer(Modifier.height(8.dp))
            Text("SMS syntax", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text("rmd ring long", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    OutlinedButton(onClick = {
                        when {
                            !dndGranted -> DoNotDisturbAccessPermission.request(context)
                            !overlayGranted -> openOverlaySettings(context)
                            else -> showRevokeMenu = true
                        }
                    }) {
                        Text(buttonLabel)
                    }
                    DropdownMenu(expanded = showRevokeMenu, onDismissRequest = { showRevokeMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Open Do Not Disturb settings") },
                            onClick = {
                                showRevokeMenu = false
                                openDoNotDisturbSettings(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open overlay settings") },
                            onClick = {
                                showRevokeMenu = false
                                openOverlaySettings(context)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RingerModeCommandCardUI() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndGranted by remember { mutableStateOf(false) }

    val refreshPermission = remember(context) {
        {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            dndGranted = manager.isNotificationPolicyAccessGranted
        }
    }

    LaunchedEffect(Unit) { refreshPermission() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermission()
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val allGranted = dndGranted
    val onClick = if (dndGranted) { { openDoNotDisturbSettings(context) } } else { { DoNotDisturbAccessPermission.request(context) } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Vibration, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ringermode [normal | vibrate | silent]", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Change the ringer mode", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Note that \"silent\" also enables Do Not Disturb mode. This is expected behaviour and is defined by Android.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Details")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Required permissions", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ListItem(
                headlineContent = { Text("Do Not Disturb access") },
                trailingContent = { if (dndGranted) Icon(Icons.Outlined.Check, null) }
            )
            Spacer(Modifier.height(8.dp))
            Text("SMS syntax", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text("rmd ringermode vibrate", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onClick) { Text(if (allGranted) "Revoke" else "Grant") }
            }
        }
    }
}

@Composable
private fun StatsCommandCardUI() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var locationGranted by remember { mutableStateOf(false) }

    val refreshPermission = remember(context) {
        {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            locationGranted = fine || coarse
        }
    }

    LaunchedEffect(Unit) { refreshPermission() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermission()
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val onClick = if (locationGranted) { { openAppDetails(context) } } else { { openAppDetails(context) } } // keep old revoke path

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SignalCellularAlt, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Get network statistics", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Details")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Required permissions", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ListItem(
                headlineContent = { Text("Location") },
                trailingContent = { if (locationGranted) Icon(Icons.Outlined.Check, null) }
            )
            Spacer(Modifier.height(8.dp))
            Text("SMS syntax", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text("rmd stats", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onClick) { Text(if (locationGranted) "Revoke" else "Grant") }
            }
        }
    }
}

private fun openDoNotDisturbSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    )
}
