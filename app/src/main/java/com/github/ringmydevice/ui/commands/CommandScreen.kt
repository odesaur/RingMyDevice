package com.github.ringmydevice.ui.commands

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.commands.CommandId
import com.github.ringmydevice.commands.CommandMetadata
import com.github.ringmydevice.commands.CommandRegistry
import com.github.ringmydevice.permissions.DoNotDisturbAccessPermission
import com.github.ringmydevice.permissions.Permissions
import com.github.ringmydevice.permissions.rememberPermissionRequester
import com.github.ringmydevice.viewmodel.SettingsViewModel

@Composable
fun CommandScreen(modifier: Modifier = Modifier) {
    val definitions = remember { CommandRegistry.commands }
    val expandedState = remember { mutableStateMapOf<CommandId, Boolean>() }
    val settingsViewModel: SettingsViewModel = viewModel()
    val baseCommand by settingsViewModel.rmdCommand.collectAsState(initial = "rmd")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(definitions, key = { it.id }) { definition ->
            val permissionState = permissionStateFor(definition.id)
            CommandListItem(
                definition = definition,
                icon = commandIcon(definition.id),
                permissionState = permissionState,
                baseCommand = baseCommand,
                expanded = expandedState[definition.id] ?: false,
                onToggleInfo = {
                    val current = expandedState[definition.id] ?: false
                    expandedState[definition.id] = !current
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun CommandListItem(
    definition: CommandMetadata,
    icon: ImageVector,
    permissionState: CommandPermissionUiState,
    baseCommand: String,
    expanded: Boolean,
    onToggleInfo: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(definition.syntax, style = MaterialTheme.typography.titleMedium)
                Text(definition.summary, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (permissionState.allGranted) {
                    Icon(Icons.Outlined.Check, contentDescription = "Permissions granted")
                } else {
                    OutlinedButton(onClick = permissionState.onGrantClick) { Text("Grant") }
                }
                IconButton(onClick = onToggleInfo, modifier = Modifier.rotate(if (expanded) 180f else 0f)) {
                    Icon(Icons.Outlined.Info, contentDescription = "Toggle details")
                }
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(definition.description, style = MaterialTheme.typography.bodyMedium)
                definition.details.forEach { detail ->
                    Spacer(Modifier.height(6.dp))
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                PermissionSection("Required permissions", permissionState.requiredEntries)
                if (permissionState.optionalEntries.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    PermissionSection("Optional permissions", permissionState.optionalEntries)
                }
                Spacer(Modifier.height(8.dp))
                Text("SMS syntax", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text("${baseCommand.trim()} ${definition.smsExample}", style = MaterialTheme.typography.bodyMedium)

                if (permissionState.allGranted && permissionState.onRevokeClick != null) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = permissionState.onRevokeClick) {
                        Text("Revoke")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSection(title: String, entries: List<PermissionEntry>) {
    if (entries.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    entries.forEach { entry ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
            if (entry.granted) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            } else {
                Icon(Icons.Outlined.ToggleOff, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            }
            Text(entry.label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun permissionStateFor(id: CommandId): CommandPermissionUiState =
    when (id) {
        CommandId.NODISTURB -> rememberNodisturbPermissionState()
        CommandId.RING -> rememberRingPermissionState()
        CommandId.RINGER_MODE -> rememberRingerModePermissionState()
        CommandId.STATS -> rememberStatsPermissionState()
        CommandId.GPS -> rememberSecureSettingsPermissionState()
        CommandId.LOCATE -> rememberLocatePermissionState()
        CommandId.LOCK -> rememberLockPermissionState()
        CommandId.HELP -> CommandPermissionUiState(requiredEntries = emptyList(), onGrantClick = {})
    }

@Composable
private fun rememberNodisturbPermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndGranted by remember { mutableStateOf(Permissions.hasDndAccess(context)) }

    val refresh = { dndGranted = Permissions.hasDndAccess(context) }
    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entries = listOf(PermissionEntry("Do Not Disturb access", dndGranted))
    return CommandPermissionUiState(
        requiredEntries = entries,
        onGrantClick = {
            if (dndGranted) {
                Permissions.openDndAccessSettings(context)
            } else {
                DoNotDisturbAccessPermission.request(context)
            }
        },
        onRevokeClick = { Permissions.openDndAccessSettings(context) }
    )
}

@Composable
private fun rememberRingPermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndGranted by remember { mutableStateOf(Permissions.hasDndAccess(context)) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val refresh = {
        dndGranted = Permissions.hasDndAccess(context)
        overlayGranted = Settings.canDrawOverlays(context)
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entries = listOf(
        PermissionEntry("Do Not Disturb access", dndGranted),
        PermissionEntry("Display over other apps", overlayGranted)
    )
    val onGrant = {
        when {
            !dndGranted -> DoNotDisturbAccessPermission.request(context)
            !overlayGranted -> openOverlaySettings(context)
        }
    }
    return CommandPermissionUiState(
        requiredEntries = entries,
        onGrantClick = onGrant,
        onRevokeClick = { Permissions.openDndAccessSettings(context) }
    )
}

@Composable
private fun rememberRingerModePermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndGranted by remember { mutableStateOf(Permissions.hasDndAccess(context)) }

    val refresh = { dndGranted = Permissions.hasDndAccess(context) }
    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entries = listOf(PermissionEntry("Do Not Disturb access", dndGranted))
    return CommandPermissionUiState(
        requiredEntries = entries,
        onGrantClick = {
            if (dndGranted) {
                Permissions.openDndAccessSettings(context)
            } else {
                DoNotDisturbAccessPermission.request(context)
            }
        },
        onRevokeClick = { Permissions.openDndAccessSettings(context) }
    )
}

@Composable
private fun rememberStatsPermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requestPermission = rememberPermissionRequester { }
    var granted by remember { mutableStateOf(locationGranted(context)) }

    val refresh = { granted = locationGranted(context) }
    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entries = listOf(PermissionEntry("Location", granted))
    return CommandPermissionUiState(
        requiredEntries = entries,
        onGrantClick = {
            if (granted) {
                Permissions.openAppDetails(context)
            } else {
                requestPermission(Permissions.requiredForFineLocation())
                refresh()
            }
        },
        onRevokeClick = { Permissions.openAppDetails(context) }
    )
}

@Composable
private fun rememberSecureSettingsPermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val entries = listOf(PermissionEntry("Write to secure settings", false))
    return CommandPermissionUiState(
        requiredEntries = entries,
        onGrantClick = { Permissions.openAppDetails(context) },
        onRevokeClick = { Permissions.openAppDetails(context) }
    )
}

@Composable
private fun rememberLocatePermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requestPermission = rememberPermissionRequester { }
    var granted by remember { mutableStateOf(locationGranted(context)) }

    val refresh = { granted = locationGranted(context) }
    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entries = listOf(PermissionEntry("Location", granted))
    val optional = listOf(PermissionEntry("Write to secure settings", false))
    return CommandPermissionUiState(
        requiredEntries = entries,
        optionalEntries = optional,
        onGrantClick = {
            if (granted) {
                Permissions.openAppDetails(context)
            } else {
                requestPermission(Permissions.requiredForFineLocation())
                refresh()
            }
        },
        onRevokeClick = { Permissions.openAppDetails(context) }
    )
}

@Composable
private fun rememberLockPermissionState(): CommandPermissionUiState {
    val context = LocalContext.current
    val entries = listOf(PermissionEntry("Device admin", false))
    return CommandPermissionUiState(
        requiredEntries = entries,
        onGrantClick = { Permissions.openAppDetails(context) },
        onRevokeClick = { Permissions.openAppDetails(context) }
    )
}

private data class PermissionEntry(
    val label: String,
    val granted: Boolean
)

private data class CommandPermissionUiState(
    val requiredEntries: List<PermissionEntry>,
    val optionalEntries: List<PermissionEntry> = emptyList(),
    val onGrantClick: () -> Unit,
    val onRevokeClick: (() -> Unit)? = null
) {
    val allGranted: Boolean = requiredEntries.all { it.granted }
}

private fun commandIcon(id: CommandId): ImageVector =
    when (id) {
        CommandId.NODISTURB -> Icons.Outlined.ToggleOff
        CommandId.RING -> Icons.AutoMirrored.Outlined.VolumeUp
        CommandId.RINGER_MODE -> Icons.Outlined.Vibration
        CommandId.STATS -> Icons.Outlined.SignalCellularAlt
        CommandId.GPS -> Icons.Outlined.GpsFixed
        CommandId.LOCATE -> Icons.Outlined.Public
        CommandId.LOCK -> Icons.Outlined.Lock
        CommandId.HELP -> Icons.Outlined.Info
    }

private fun locationGranted(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
