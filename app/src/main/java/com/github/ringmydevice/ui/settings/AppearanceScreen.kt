package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.ui.theme.ThemePreference
import com.github.ringmydevice.ui.theme.ThemeSettingsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    themeSettings: ThemeSettingsState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val themeOptions = listOf(
        ThemePreference.SYSTEM to "System default",
        ThemePreference.LIGHT to "Light",
        ThemePreference.DARK to "Dark"
    )
    val selectTheme: (ThemePreference, String) -> Unit = { preference, label ->
        if (themeSettings.themePreference != preference) {
            themeSettings.themePreference = preference
            scope.launch { snackbarHostState.showSnackbar("Theme updated: $label") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(themeOptions.size) { index ->
                val (preference, label) = themeOptions[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectTheme(preference, label) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    RadioButton(
                        selected = themeSettings.themePreference == preference,
                        onClick = { selectTheme(preference, label) }
                    )
                    Text(label, modifier = Modifier.padding(start = 12.dp))
                }
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Dynamic colors") },
                    supportingContent = { Text("Match system accent colors") },
                    trailingContent = {
                        Switch(
                            checked = themeSettings.useDynamicColor,
                            onCheckedChange = { enabled ->
                                themeSettings.useDynamicColor = enabled
                                val status = if (enabled) "enabled" else "disabled"
                                scope.launch {
                                    snackbarHostState.showSnackbar("Dynamic colors $status")
                                }
                            }
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
