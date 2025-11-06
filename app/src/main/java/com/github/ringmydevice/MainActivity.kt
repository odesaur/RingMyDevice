package com.github.ringmydevice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.github.ringmydevice.ui.HomeSetup
import com.github.ringmydevice.ui.theme.RMDTheme
import com.github.ringmydevice.ui.theme.ThemePreference
import com.github.ringmydevice.ui.theme.rememberThemeSettingsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeSettings = rememberThemeSettingsState()
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
