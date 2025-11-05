package com.github.ringmydevice.ui.commands

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.ui.model.CommandItem

@Composable
fun CommandScreen(modifier: Modifier = Modifier) {
    val commandItems = listOf(
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Offline phone recovery that works anywhere",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Find, lock, wipe, ring, and get photos by SMS or over a mesh. No Google account. No internet required.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(commandItems) { item ->
            CommandCard(item)
        }
    }
}

@Composable
private fun CommandCard(item: CommandItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Details") }
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
                OutlinedButton(onClick = {}) { Text("Grant") }
            }
        }
    }
}
