package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Row(
    val title: String,
    val subtitle: String? = null,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
fun SettingsHome(
    onOpenGeneral: () -> Unit,
    onOpenFmd: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAllowed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        Row("General", "Core commands and recovery options", Icons.Outlined.Tune, onOpenGeneral),
        Row("FMD Server", "Manage your self-hosted server", Icons.Outlined.Cloud, onOpenFmd),
        Row("Appearance", "Theme and color preferences", Icons.Outlined.Palette, onOpenAppearance),
        Row("Logs", "Recent command activity", Icons.Outlined.ListAlt, onOpenLogs),
        Row("Allowed contacts", "Who can send commands", Icons.Outlined.People, onOpenAllowed),
        Row("About", "Version & credits", Icons.Outlined.Info, onOpenAbout),
    )
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(rows) { r ->
            ListItem(
                headlineContent = { Text(r.title, style = MaterialTheme.typography.titleMedium) },
                supportingContent = r.subtitle?.let { { Text(it) } },
                leadingContent = { Icon(r.icon, contentDescription = null) },
                modifier = Modifier
                    .then(Modifier) // clickable provided by ListItem via onClick param in M3 1.3+, else wrap in clickable
                    .clickable(onClick = r.onClick)
            )
            // If your M3 version doesn't have onClick in ListItem, wrap with Clickable modifier:
            // .clickable(onClick = r.onClick)
            // For simplicity, call immediately:
            androidx.compose.runtime.SideEffect { /* no-op */ }
        }
    }
}
