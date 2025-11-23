package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.data.model.CommandType
import com.github.ringmydevice.viewmodel.CommandViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: CommandViewModel = viewModel()
) {
    // collect logs from the Room Database Flow
    val logs by vm.logs.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Activity Logs")
                        Text(
                            "${logs.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("Clear logs", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = {}
    ) { inner ->

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear history?") },
                text = { Text("This will permanently delete all activity logs. This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.clearLogs()
                            showClearDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (logs.isEmpty()) {
            EmptyStateView(modifier.padding(inner))
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItemCard(
                        type = log.type,
                        notes = log.notes,
                        timestamp = log.timestamp,
                        formatter = dateFormatter
                    )
                }
            }
        }
    }
}

@Composable
fun LogItemCard(
    type: CommandType,
    notes: String?,
    timestamp: Long,
    formatter: SimpleDateFormat
) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = getIconForCommand(type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = {
                Text(
                    text = formatCommandType(type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!notes.isNullOrEmpty()) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatter.format(Date(timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No activity yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Commands received will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getIconForCommand(type: CommandType): ImageVector {
    return when (type) {
        CommandType.RING -> Icons.Default.NotificationsActive
        CommandType.LOCATE -> Icons.Default.LocationOn
        CommandType.PHOTO -> Icons.Default.CameraAlt
        CommandType.WIPE -> Icons.Default.DeleteForever
        CommandType.UNKNOWN -> Icons.Default.Info
        CommandType.NODISTURB -> Icons.Default.DoNotDisturbOn
        CommandType.RINGER_MODE -> Icons.AutoMirrored.Filled.VolumeUp
        CommandType.STATS -> Icons.Default.SignalCellularAlt
        CommandType.GPS -> Icons.Default.GpsFixed
        CommandType.LOCK -> Icons.Default.Lock
        CommandType.HELP -> Icons.AutoMirrored.Filled.Help
        CommandType.CONTACT -> Icons.Default.PersonAdd
    }
}

private fun formatCommandType(type: CommandType): String {
    return when (type) {
        CommandType.NODISTURB -> "Do Not Disturb"
        CommandType.RINGER_MODE -> "Ringer Mode"
        CommandType.RING -> "Ring Device"
        CommandType.LOCATE -> "Locate Device"
        CommandType.PHOTO -> "Take Photo"
        CommandType.WIPE -> "Wipe Data"
        CommandType.STATS -> "Device Stats"
        CommandType.GPS -> "Enable GPS"
        CommandType.LOCK -> "Lock Device"
        CommandType.HELP -> "Help Command"
        CommandType.UNKNOWN -> "Unknown Action"
        CommandType.CONTACT -> "Allowed Contacts"
    }
}