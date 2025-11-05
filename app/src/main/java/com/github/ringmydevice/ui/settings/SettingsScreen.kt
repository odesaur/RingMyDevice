package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.ui.model.SettingsItem

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val items = listOf(
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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = item.description?.let { { Text(it) } },
                leadingContent = { Icon(item.icon, contentDescription = null) }
            )
        }
    }
}
