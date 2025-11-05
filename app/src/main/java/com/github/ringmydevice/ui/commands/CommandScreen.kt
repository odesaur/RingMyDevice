package com.github.ringmydevice.ui.commands

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.permissions.*
import com.github.ringmydevice.permissions.Permissions.openAppDetails
import com.github.ringmydevice.ui.model.CommandItem
import com.github.ringmydevice.maps.openInOpenStreetMap
import com.github.ringmydevice.permissions.Permissions
import com.github.ringmydevice.permissions.DoNotDisturbAccessPermission
import androidx.lifecycle.viewmodel.compose.*
import androidx.compose.ui.platform.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val vm = viewModel<com.github.ringmydevice.viewmodel.CommandViewModel>()
    var showLogs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    // Data for the cards
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

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Developer demo tools
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Developer demo", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { vm.simulateRing(context = ctx, seconds = 5) }) {
                                Text("Simulate RING 5s")
                            }
                            OutlinedButton(onClick = {
                                val (lat, lon) = vm.simulateLocate()
                                openInOpenStreetMap(ctx, lat, lon)     // OSM in browser
                            }) {
                                Text("Simulate LOCATE")
                            }
                            OutlinedButton(onClick = { showLogs = true }) {
                                Text("Show logs")
                            }
                        }
                    }
                }
            }

            // Promo card
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
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Find, lock, wipe, ring, and get photos by SMS or over a mesh. No Google account. No internet required.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Command cards
            items(commandItems) { item ->
                CommandCard(item)
            }
        }

        if (showLogs) {
            ModalBottomSheet(onDismissRequest = { showLogs = false }) {
                LogsSheet(vm) { showLogs = false }
            }
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


