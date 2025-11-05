package com.github.ringmydevice.ui.transport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TransportScreen(modifier: Modifier = Modifier) {
    var smsEnabled by remember { mutableStateOf(true) }
    var meshEnabled by remember { mutableStateOf(false) }
    var serverEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TransportRow(
                icon = Icons.Outlined.Map,
                title = "SMS transport",
                description = "Receive and send commands over SMS",
                checked = smsEnabled,
                onCheckedChange = { smsEnabled = it }
            )
        }
        item {
            TransportRow(
                icon = Icons.Outlined.AccountTree,
                title = "Meshtastic",
                description = "Local mesh channel for off-grid use",
                checked = meshEnabled,
                onCheckedChange = { meshEnabled = it }
            )
        }
        item {
            TransportRow(
                icon = Icons.Outlined.Public,
                title = "Self-hosted server",
                description = "Optional device dashboard and map",
                checked = serverEnabled,
                onCheckedChange = { serverEnabled = it }
            )
        }
    }
}

@Composable
private fun TransportRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
