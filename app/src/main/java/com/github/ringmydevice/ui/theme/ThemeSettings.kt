package com.github.ringmydevice.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

class ThemeSettingsState internal constructor(
    initialPreference: ThemePreference,
    initialUseDynamicColor: Boolean
) {
    var themePreference by mutableStateOf(initialPreference)
    var useDynamicColor by mutableStateOf(initialUseDynamicColor)

    companion object {
        val Saver: Saver<ThemeSettingsState, Any> = Saver(
            save = { listOf(it.themePreference.name, it.useDynamicColor) },
            restore = {
                val values = it as List<*>
                val preference = ThemePreference.valueOf(values[0] as String)
                val dynamic = values[1] as Boolean
                ThemeSettingsState(preference, dynamic)
            }
        )
    }
}

@Composable
fun rememberThemeSettingsState(
    preference: ThemePreference = ThemePreference.SYSTEM,
    useDynamicColor: Boolean = true
): ThemeSettingsState = rememberSaveable(saver = ThemeSettingsState.Saver) {
    ThemeSettingsState(preference, useDynamicColor)
}
