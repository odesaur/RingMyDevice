package com.github.ringmydevice.ui.transport

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.ringmydevice.commands.CommandHelpResponder
import com.github.ringmydevice.commands.CommandProcessor
import com.github.ringmydevice.commands.CommandSource
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.permissions.Permissions
import com.github.ringmydevice.ui.settings.AllowedContactsScreen
import com.github.ringmydevice.ui.settings.FmdServerScreen
import com.github.ringmydevice.viewmodel.AllowedContactsViewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.sms.SmsFeedbackSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val allowedContactsViewModel: AllowedContactsViewModel = viewModel()
    val allowedContacts by allowedContactsViewModel.contacts.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel()
    val baseCommand by settingsViewModel.rmdCommand.collectAsState(initial = "rmd")

    var showAllowedContacts by remember { mutableStateOf(false) }
    var showFmdSettings by remember { mutableStateOf(false) }
    var showInAppDialog by remember { mutableStateOf(false) }
    var showMeshtasticDialog by remember { mutableStateOf(false) }
    var showSendHelpDialog by remember { mutableStateOf(false) }

    var smsPermissionGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECEIVE_SMS) && hasPermission(context, Permissions.requiredForSmsSend())) }
    var postNotificationsGranted by remember { mutableStateOf(hasPostNotificationPermission(context)) }
    var notificationAccessGranted by remember { mutableStateOf(hasNotificationListenerAccess(context)) }
    var bluetoothPermissionGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) }
    var locationPermissionGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        smsPermissionGranted = result.all { it.value }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        postNotificationsGranted = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }
    val notificationAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        notificationAccessGranted = hasNotificationListenerAccess(context)
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        bluetoothPermissionGranted = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationPermissionGranted = granted
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smsPermissionGranted = hasPermission(context, Manifest.permission.RECEIVE_SMS) && hasPermission(context, Permissions.requiredForSmsSend())
                postNotificationsGranted = hasPostNotificationPermission(context)
                notificationAccessGranted = hasNotificationListenerAccess(context)
                bluetoothPermissionGranted = hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                locationPermissionGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SmsTransportCard(
                hasSmsPermission = smsPermissionGranted,
                onRequestSmsPermission = {
                    smsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECEIVE_SMS,
                            Permissions.requiredForSmsSend()
                        )
                    )
                },
                onManageAllowedContacts = { showAllowedContacts = true },
                onSendHelp = {
                    if (allowedContacts.isEmpty()) {
                        Toast.makeText(context, "Add an allowed contact first", Toast.LENGTH_SHORT).show()
                    } else if (!smsPermissionGranted) {
                        Toast.makeText(context, "Grant SMS permission to send help", Toast.LENGTH_SHORT).show()
                    } else {
                        showSendHelpDialog = true
                    }
                }
            )
        }
        item {
            NotificationReplyCard(
                hasNotificationAccess = notificationAccessGranted,
                onOpenSettings = {
                    notificationAccessLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
        }
        item {
            FmdServerCard(onOpenSettings = { showFmdSettings = true })
        }
        item {
            InAppCommandCard(
                hasNotificationPermission = postNotificationsGranted,
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onSendCommand = { showInAppDialog = true }
            )
        }
        item {
            MeshtasticCard(
                bluetoothGranted = bluetoothPermissionGranted,
                locationGranted = locationPermissionGranted,
                onRequestBluetooth = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                },
                onRequestLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                onConfigure = { showMeshtasticDialog = true }
            )
        }
    }

    if (showAllowedContacts) {
        FullScreenDialog(onDismiss = { showAllowedContacts = false }) {
            AllowedContactsScreen(onBack = { showAllowedContacts = false })
        }
    }
    if (showFmdSettings) {
        FullScreenDialog(onDismiss = { showFmdSettings = false }) {
            FmdServerScreen(onBack = { showFmdSettings = false })
        }
    }
    if (showInAppDialog) {
        InAppCommandDialog(
            onDismiss = { showInAppDialog = false },
            onSend = { command ->
                showInAppDialog = false
                scope.launch {
                    CommandProcessor.handle(context, sender = "local", rawMessage = command, source = CommandSource.IN_APP)
                }
            }
        )
    }
    if (showMeshtasticDialog) {
        AlertDialog(
            onDismissRequest = { showMeshtasticDialog = false },
            title = { Text("Meshtastic support") },
            text = {
                Text(
                    "Pair your Meshtastic node to relay RMD commands when you are off-grid. " +
                        "After granting Bluetooth and location permissions, link a channel via the Meshtastic app. " +
                        "Future updates will allow direct configuration from RMD."
                )
            },
            confirmButton = {
                TextButton(onClick = { showMeshtasticDialog = false }) { Text("Got it") }
            }
        )
    }
    if (showSendHelpDialog) {
        AlertDialog(
            onDismissRequest = { showSendHelpDialog = false },
            title = { Text("Send help SMS") },
            text = {
                if (allowedContacts.isEmpty()) {
                    Text("No allowed contacts available.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allowedContacts.forEach { contact ->
                            Button(
                                onClick = {
                                    val sendResult = sendHelpSms(context, contact, baseCommand)
                                    val message = when (sendResult) {
                                        SmsFeedbackSender.Result.Sent -> "Sent help to ${contact.displayName()}"
                                        SmsFeedbackSender.Result.PermissionMissing -> "Grant SMS permission to send help"
                                        SmsFeedbackSender.Result.Failed -> "Unable to send SMS"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    showSendHelpDialog = false
                                }
                            ) {
                                Text(contact.displayName())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSendHelpDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SmsTransportCard(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onManageAllowedContacts: () -> Unit,
    onSendHelp: () -> Unit
) {
    TransportCard(
        icon = Icons.Outlined.Sms,
        title = "SMS",
        description = "Send commands to your device over SMS. Only numbers in the allowlist can control RMD. " +
            "Unlisted numbers must include the PIN, e.g. \"rmd 1234 help\". Numbers that authenticate with the PIN are allowed for 10 minutes.",
        content = {
            PermissionRow(label = "Required permissions: SMS", granted = hasSmsPermission)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onManageAllowedContacts) {
                Text("Allowed contacts")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRequestSmsPermission, enabled = !hasSmsPermission) {
                Text(if (hasSmsPermission) "Permission granted" else "Grant SMS permission")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSendHelp, enabled = hasSmsPermission) { Text("Send help SMS") }
        }
    )
}

@Composable
private fun NotificationReplyCard(
    hasNotificationAccess: Boolean,
    onOpenSettings: () -> Unit
) {
    TransportCard(
        icon = Icons.Outlined.Notifications,
        title = "Notification reply",
        description = "Allow RMD to read notifications that support quick reply. " +
            "This lets you send commands via apps like Signal by replying \"rmd ring\". " +
            "Every message must include the PIN.",
        content = {
            PermissionRow(label = "Required permissions: Notification access", granted = hasNotificationAccess)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) {
                Text("Open notification access settings")
            }
        }
    )
}

@Composable
private fun FmdServerCard(onOpenSettings: () -> Unit) {
    TransportCard(
        icon = Icons.Outlined.Cloud,
        title = "RMD Server",
        description = "Use a self-hosted server (or the official instance) to control your device over the internet. " +
            "The server stores your data encrypted with your password.",
        content = {
            PermissionRow(label = "Requires network access", granted = true)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) { Text("Settings") }
        }
    )
}

@Composable
private fun InAppCommandCard(
    hasNotificationPermission: Boolean,
    onRequestNotification: () -> Unit,
    onSendCommand: () -> Unit
) {
    TransportCard(
        icon = Icons.Outlined.WifiTethering,
        title = "In-app",
        description = "Send commands directly from RMD to test responses. RMD replies with a notification.",
        content = {
            PermissionRow(label = "Required permissions: Post notifications", granted = hasNotificationPermission)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSendCommand) { Text("Send in-app command") }
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRequestNotification) {
                    Text("Grant notification permission")
                }
            }
        }
    )
}

@Composable
private fun MeshtasticCard(
    bluetoothGranted: Boolean,
    locationGranted: Boolean,
    onRequestBluetooth: () -> Unit,
    onRequestLocation: () -> Unit,
    onConfigure: () -> Unit
) {
    TransportCard(
        icon = Icons.Outlined.AccountTree,
        title = "Meshtastic",
        description = "Relay commands over the Meshtastic mesh network for off-grid control. " +
            "Requires Bluetooth and location access to talk to your node.",
        content = {
            PermissionRow(label = "Bluetooth permission", granted = bluetoothGranted)
            PermissionRow(label = "Location permission", granted = locationGranted)
            Spacer(Modifier.height(12.dp))
            RowedButtons(
                primaryLabel = "Pair / configure",
                onPrimary = onConfigure,
                secondaryLabel = if (bluetoothGranted && locationGranted) "Permissions granted" else "Grant permissions",
                onSecondary = {
                    if (!bluetoothGranted) onRequestBluetooth()
                    if (!locationGranted) onRequestLocation()
                },
                secondaryEnabled = !bluetoothGranted || !locationGranted
            )
        }
    )
}

@Composable
private fun TransportCard(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    val color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Text(
        text = label + if (granted) " ✓" else " ✕",
        color = color,
        style = MaterialTheme.typography.labelLarge
    )
}

private fun sendHelpSms(context: Context, contact: AllowedContact, baseCommand: String): SmsFeedbackSender.Result {
    val message = CommandHelpResponder.buildHelpMessageFromCommands(baseCommand)
    return SmsFeedbackSender.send(
        context = context,
        destinationPhoneNumber = contact.phoneNumber,
        messageBody = message,
        requestPermissionIfNeeded = true
    )
}

private fun AllowedContact.displayName(): String =
    if (name.isNotBlank()) "$name (${phoneNumber})" else phoneNumber

@Composable
private fun RowedButtons(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    secondaryEnabled: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onPrimary, modifier = Modifier.weight(1f)) { Text(primaryLabel) }
        OutlinedButton(
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
            enabled = secondaryEnabled
        ) { Text(secondaryLabel) }
    }
}

@Composable
private fun FullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun InAppCommandDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("rmd ring") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send in-app command") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a command (e.g., \"rmd ring long\").")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(text) }) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun hasPostNotificationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun hasNotificationListenerAccess(context: android.content.Context): Boolean {
    val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabled.contains(context.packageName)
}
