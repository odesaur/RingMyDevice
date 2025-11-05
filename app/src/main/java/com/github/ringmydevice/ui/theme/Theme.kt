package com.github.ringmydevice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/* Material 3 color scheme mapped from Rosé Pine (dark) */
private val DarkColors: ColorScheme = darkColorScheme(
    primary = RpLove,
    onPrimary = RpBase,
    primaryContainer = RpRose,
    onPrimaryContainer = RpBase,
    secondary = RpIris,
    onSecondary = RpBase,
    secondaryContainer = RpSurface1,
    onSecondaryContainer = RpText,
    tertiary = RpFoam,
    onTertiary = RpBase,
    tertiaryContainer = RpSurface1,
    onTertiaryContainer = RpText,
    background = RpBase,
    onBackground = RpText,
    surface = RpSurface0,
    onSurface = RpText,
    surfaceVariant = RpSurface1,
    onSurfaceVariant = RpSubtext1,
    outline = RpOverlay2,
    error = RpLove,
    onError = RpBase,
    errorContainer = RpRose,
    onErrorContainer = RpBase
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
