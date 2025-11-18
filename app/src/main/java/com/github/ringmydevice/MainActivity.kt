package com.github.ringmydevice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.ui.HomeSetup
import com.github.ringmydevice.ui.theme.RMDTheme
import com.github.ringmydevice.ui.theme.ThemePreference
import com.github.ringmydevice.ui.theme.rememberThemeSettingsState
import com.github.ringmydevice.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val persistedPreference by settingsViewModel.themePreference.collectAsState()
            val persistedDynamicColor by settingsViewModel.useDynamicColor.collectAsState()

            val themeSettings = rememberThemeSettingsState(
                preference = persistedPreference,
                useDynamicColor = persistedDynamicColor
            )

            LaunchedEffect(persistedPreference) {
                if (themeSettings.themePreference != persistedPreference) {
                    themeSettings.themePreference = persistedPreference
                }
            }

            LaunchedEffect(persistedDynamicColor) {
                if (themeSettings.useDynamicColor != persistedDynamicColor) {
                    themeSettings.useDynamicColor = persistedDynamicColor
                }
            }

            LaunchedEffect(themeSettings.themePreference) {
                if (themeSettings.themePreference != persistedPreference) {
                    settingsViewModel.setThemePreference(themeSettings.themePreference)
                }
            }

            LaunchedEffect(themeSettings.useDynamicColor) {
                if (themeSettings.useDynamicColor != persistedDynamicColor) {
                    settingsViewModel.setUseDynamicColor(themeSettings.useDynamicColor)
                }
            }

            val useDarkTheme = when (themeSettings.themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            RMDTheme(
                useDarkTheme = useDarkTheme,
                useDynamicColor = themeSettings.useDynamicColor
            ) {
                HomeSetup(themeSettings)
            }
        }
    }
}
