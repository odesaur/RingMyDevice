package com.github.ringmydevice.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.github.ringmydevice.ui.commands.CommandScreen
import com.github.ringmydevice.ui.model.HomeTab
import com.github.ringmydevice.ui.settings.SettingsNavHost
import com.github.ringmydevice.ui.theme.RMDTheme
import com.github.ringmydevice.ui.theme.ThemePreference
import com.github.ringmydevice.ui.theme.ThemeSettingsState
import com.github.ringmydevice.ui.theme.rememberThemeSettingsState
import com.github.ringmydevice.ui.transport.TransportScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSetup(themeSettings: ThemeSettingsState) {
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
    ) { inner ->
        when (selectedTab) {
            HomeTab.Commands -> CommandScreen(Modifier.padding(inner))
            HomeTab.Transport -> TransportScreen(Modifier.padding(inner))
            HomeTab.Settings -> SettingsNavHost(
                modifier = Modifier.padding(inner),
                themeSettings = themeSettings
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewHome() {
    val themeSettings = rememberThemeSettingsState(
        preference = ThemePreference.DARK,
        useDynamicColor = false
    )
    RMDTheme(useDarkTheme = true, useDynamicColor = false) {
        HomeSetup(themeSettings)
    }
}
