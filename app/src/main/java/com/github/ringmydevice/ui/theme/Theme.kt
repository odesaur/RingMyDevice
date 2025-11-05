package com.github.ringmydevice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/* Material 3 color scheme mapped from Catppuccin Macchiato */
private val DarkColors: ColorScheme = darkColorScheme(
    primary = CpBlue,
    onPrimary = CpCrust,
    primaryContainer = CpMauve,
    onPrimaryContainer = CpCrust,
    secondary = CpLavender,
    onSecondary = CpCrust,
    secondaryContainer = CpSurface1,
    onSecondaryContainer = CpText,
    tertiary = CpGreen,
    onTertiary = CpCrust,
    tertiaryContainer = CpSurface1,
    onTertiaryContainer = CpText,
    background = CpBase,
    onBackground = CpText,
    surface = CpSurface0,
    onSurface = CpText,
    surfaceVariant = CpSurface1,
    onSurfaceVariant = CpSubtext0,
    outline = CpOverlay1,
    error = CpRed,
    onError = CpCrust,
    errorContainer = CpMaroon,
    onErrorContainer = CpCrust
)

@Composable
fun RMDTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = DarkColors
    MaterialTheme(
        colorScheme = scheme,
        typography = RMDTypography,
        content = content
    )
}
