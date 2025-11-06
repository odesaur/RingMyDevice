package com.github.ringmydevice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

/* Material 3 color scheme mapped from Rosé Pine Dawn (light) */
private val LightColors: ColorScheme = lightColorScheme(
    primary = RpDawnLove,
    onPrimary = RpDawnBase,
    primaryContainer = RpDawnRose,
    onPrimaryContainer = RpDawnBase,
    secondary = RpDawnIris,
    onSecondary = RpDawnBase,
    secondaryContainer = RpDawnOverlay0,
    onSecondaryContainer = RpDawnText,
    tertiary = RpDawnFoam,
    onTertiary = RpDawnBase,
    tertiaryContainer = RpDawnOverlay0,
    onTertiaryContainer = RpDawnText,
    background = RpDawnBase,
    onBackground = RpDawnText,
    surface = RpDawnSurface0,
    onSurface = RpDawnText,
    surfaceVariant = RpDawnSurface1,
    onSurfaceVariant = RpDawnSubtext1,
    outline = RpDawnOverlay2,
    error = RpDawnLove,
    onError = RpDawnBase,
    errorContainer = RpDawnRose,
    onErrorContainer = RpDawnText
)

@Composable
fun RMDTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RMDTypography,
        content = content
    )
}
