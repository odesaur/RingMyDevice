@file:Suppress("FunctionName")

package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

private data class SettingRow(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenGeneral: () -> Unit = {},
    onOpenServer: () -> Unit = {},
    onOpenAllowedContacts: () -> Unit = {},
    onOpenOpenCellId: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val items = listOf(
        SettingRow("General", icon = Icons.Outlined.Settings, onClick = onOpenGeneral),
        SettingRow("FMD Server", icon = Icons.Outlined.Public, onClick = onOpenServer),
        SettingRow("Allowed contacts", icon = Icons.Outlined.People, onClick = onOpenAllowedContacts),
        SettingRow("OpenCelliD", icon = Icons.Outlined.Map, onClick = onOpenOpenCellId),
        SettingRow("Appearance", icon = Icons.Outlined.Palette, onClick = onOpenAppearance),
        SettingRow("Export settings", subtitle = "Create a backup", icon = Icons.Outlined.ImportExport, onClick = onExport),
        SettingRow("Import settings", subtitle = "Restore from a backup", icon = Icons.Outlined.ImportExport, onClick = onImport),
        SettingRow("Logs", icon = Icons.AutoMirrored.Outlined.ListAlt, onClick = onOpenLogs),
        SettingRow("About", icon = Icons.Outlined.Info, onClick = onOpenAbout)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items) { row ->
            ListItem(
                headlineContent = { Text(row.title) },
                supportingContent = row.subtitle?.let { { Text(it) } },
                leadingContent = { Icon(row.icon, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { row.onClick() }
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}
