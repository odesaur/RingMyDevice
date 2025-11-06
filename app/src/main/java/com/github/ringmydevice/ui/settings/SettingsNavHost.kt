package com.github.ringmydevice.ui.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object SettingsRoutes {
    const val HOME = "settings/home"
    const val LOGS = "settings/logs"
    const val ABOUT = "settings/about"
    const val ALLOWED = "settings/allowed"
}

@Composable
fun SettingsNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = SettingsRoutes.HOME,
        modifier = modifier
    ) {
        // Home list (your stubbed items)
        composable(SettingsRoutes.HOME) {
            SettingsScreen(
                onNavigateToLogs = { nav.navigate(SettingsRoutes.LOGS) },
                onNavigateToAbout = { nav.navigate(SettingsRoutes.ABOUT) },
                onNavigateToAllowedContacts = { nav.navigate(SettingsRoutes.ALLOWED) },
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
    }
}
