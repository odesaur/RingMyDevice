package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("RMD", style = MaterialTheme.typography.titleLarge)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            Text("Works offline. Receive commands via SMS or mesh and trigger device actions.")
            HorizontalDivider()
            Text("Credits", style = MaterialTheme.typography.titleMedium)
            Text("• Built with Jetpack Compose\n• Uses Android system services for alert, location, etc.")
        }
    }
}
