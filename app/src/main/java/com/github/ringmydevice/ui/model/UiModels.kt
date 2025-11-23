package com.github.ringmydevice.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/* Tabs */
enum class HomeTab(val label: String, val icon: ImageVector) {
    Commands("Commands", Icons.Outlined.PhoneIphone),
    Transport("Transport", Icons.Outlined.Public),
    Map("Map", Icons.Outlined.Map),
    Settings("Settings", Icons.Outlined.Settings)
}

/* Settings rows */
data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val description: String? = null
)
