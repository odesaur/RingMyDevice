package com.github.ringmydevice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.ui.theme.RMDTheme

/* Tabs */
private enum class HomeTab(val label: String, val icon: ImageVector) {
    Commands("Commands", Icons.Outlined.PhoneIphone),
    Transport("Transport channels", Icons.Outlined.Public),
    Settings("Settings", Icons.Outlined.Settings)
}

/* Command cards */
private data class CommandItem(
    val title: String,
    val description: String,
    val requiredPermissions: List<String>,
    val icon: ImageVector,
    val exampleSyntax: String
)

/* Settings rows */
private data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val description: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RMDTheme { RingMyDeviceApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RingMyDeviceApp() {
    var selectedTab by remember { mutableStateOf(HomeTab.Commands) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    text = "Ring My Device",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            })
        },
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.Commands -> CommandsScreen(Modifier.padding(innerPadding))
            HomeTab.Transport -> TransportScreen(Modifier.padding(innerPadding))
            HomeTab.Settings -> SettingsScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun CommandsScreen(modifier: Modifier = Modifier) {
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

        items(commandItems) { commandItem ->
            CommandCard(commandItem)
        }
    }
}

@Composable
private fun CommandCard(commandItem: CommandItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(commandItem.icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = commandItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = commandItem.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.ListAlt, contentDescription = "Details")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(text = "Required permissions", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = commandItem.requiredPermissions.joinToString(),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))
            Text(text = "SMS syntax", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(text = commandItem.exampleSyntax, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = {}) { Text("Grant") }
            }
        }
    }
}

@Composable
private fun TransportScreen(modifier: Modifier = Modifier) {
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
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    val settingsItems = listOf(
        SettingsItem("General", Icons.Outlined.Settings),
        SettingsItem("FMD Server", Icons.Outlined.Public),
        SettingsItem("Allowed contacts", Icons.Outlined.People),
        SettingsItem("OpenCelliD", Icons.Outlined.Map),
        SettingsItem("Appearance", Icons.Outlined.Palette),
        SettingsItem("Export settings", Icons.Outlined.ImportExport, "Create a backup"),
        SettingsItem("Import settings", Icons.Outlined.ImportExport, "Restore from a backup"),
        SettingsItem("Logs", Icons.Outlined.ListAlt),
        SettingsItem("About", Icons.Outlined.Info)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(settingsItems) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = item.description?.let { { Text(it) } },
                leadingContent = { Icon(item.icon, contentDescription = null) }
            )
        }
    }
}
