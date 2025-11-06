package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.ui.model.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToGeneral: () -> Unit = {},
    onNavigateToFmd: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAllowedContacts: () -> Unit = {},
) {
    val items = listOf(
        SettingsItem("General", Icons.Outlined.Settings, "System and app defaults"),
        SettingsItem("FMD Server", Icons.Outlined.Public, "Link a Find-My-Device server"), // TODO
        SettingsItem("Allowed contacts", Icons.Outlined.People, "Set who can trigger commands"),
        SettingsItem("OpenCelliD", Icons.Outlined.Map, "Contribute tower data"), // TODO
        SettingsItem("Appearance", Icons.Outlined.Palette, "Theme and color scheme"), // TODO
        SettingsItem("Export settings", Icons.Outlined.FileUpload, "Create a backup"), // TODO
        SettingsItem("Import settings", Icons.Outlined.FileDownload, "Restore from a backup"), // TODO
        SettingsItem("Logs", Icons.Outlined.ListAlt, "View recent actions and SMS triggers"),
        SettingsItem("About", Icons.Outlined.Info, "Version and credits")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = item.description?.let { { Text(it) } },
                leadingContent = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (item.title) {
                            "General" -> onNavigateToGeneral()
                            "FMD Server" -> onNavigateToFmd()
                            "Appearance" -> onNavigateToAppearance()
                            "Allowed contacts" -> onNavigateToAllowedContacts()
                            "Logs" -> onNavigateToLogs()
                            "About" -> onNavigateToAbout()
                            else -> {
                                // Stub for later
                                println("TODO: ${item.title} not implemented yet.")
                            }
                        }
                    }
            )
            Divider()
        }
    }
}
