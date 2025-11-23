package com.github.ringmydevice.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.ringmydevice.data.backup.SettingsBackupManager
import com.github.ringmydevice.ui.theme.ThemeSettingsState
import com.github.ringmydevice.ui.settings.OpenCellIdScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private object SettingsRoutes {
    const val HOME = "settings/home"
    const val LOGS = "settings/logs"
    const val ABOUT = "settings/about"
    const val ALLOWED = "settings/allowed"
    const val GENERAL = "settings/general"
    const val FMD = "settings/fmd"
    const val APPEARANCE = "settings/appearance"
    const val OPENCELLID = "settings/opencellid"
}

@Composable
fun SettingsNavHost(
    modifier: Modifier = Modifier,
    themeSettings: ThemeSettingsState
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = SettingsBackupManager.export(context, uri)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        result.fold(
                            onSuccess = { "Settings exported" },
                            onFailure = { "Export failed: ${it.message ?: "Unknown error"}" }
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = SettingsBackupManager.import(context, uri)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        result.fold(
                            onSuccess = { "Settings imported" },
                            onFailure = { "Import failed: ${it.message ?: "Unknown error"}" }
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

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
                onOpenOpenCellId = { nav.navigate(SettingsRoutes.OPENCELLID) },
                onExport = {
                    val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now())
                    val randomSuffix = UUID.randomUUID().toString().take(4)
                    val fileName = "rmd-backup-${timestamp}${randomSuffix}.json"
                    exportLauncher.launch(fileName)
                },
                onImport = {
                    importLauncher.launch(arrayOf("application/json"))
                }
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
        composable(SettingsRoutes.OPENCELLID) {
            OpenCellIdScreen(onBack = { nav.popBackStack() })
        }
    }
}
