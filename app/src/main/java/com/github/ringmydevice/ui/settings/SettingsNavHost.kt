package com.github.ringmydevice.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.ringmydevice.ui.theme.ThemeSettingsState

private object SettingsRoutes {
    const val HOME = "settings/home"
    const val LOGS = "settings/logs"
    const val ABOUT = "settings/about"
    const val ALLOWED = "settings/allowed"
    const val GENERAL = "settings/general"
    const val FMD = "settings/fmd"
    const val APPEARANCE = "settings/appearance"
}

@Composable
fun SettingsNavHost(
    modifier: Modifier = Modifier,
    themeSettings: ThemeSettingsState
) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = SettingsRoutes.HOME,
        modifier = modifier
    ) {
        // Home list (your stubbed items)
        composable(SettingsRoutes.HOME) {
            SettingsScreen(
                onOpenGeneral = { nav.navigate(SettingsRoutes.GENERAL) },
                onOpenServer = { nav.navigate(SettingsRoutes.FMD) },
                onOpenAppearance = { nav.navigate(SettingsRoutes.APPEARANCE) },
                onOpenLogs = { nav.navigate(SettingsRoutes.LOGS) },
                onOpenAbout = { nav.navigate(SettingsRoutes.ABOUT) },
                onOpenAllowedContacts = { nav.navigate(SettingsRoutes.ALLOWED) },
            )
        }

        // Detail pages
        composable(SettingsRoutes.LOGS) {
            LogsScreen(onBack = { nav.popBackStack() })
        }
        composable(SettingsRoutes.ABOUT) {
            AboutScreen(onBack = { nav.popBackStack() })
        }
        composable(SettingsRoutes.ALLOWED) {
            AllowedContactsScreen(onBack = { nav.popBackStack() })
        }
        composable(SettingsRoutes.GENERAL) {
            GeneralSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(SettingsRoutes.FMD) {
            FmdServerScreen(onBack = { nav.popBackStack() })
        }
        composable(SettingsRoutes.APPEARANCE) {
            AppearanceScreen(
                onBack = { nav.popBackStack() },
                themeSettings = themeSettings
            )
        }
    }
}
