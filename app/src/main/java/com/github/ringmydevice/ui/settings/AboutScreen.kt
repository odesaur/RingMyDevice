package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.BuildConfig
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val libs = remember {
        listOf(
            LibEntry("Jetpack Compose (UI, Material3, Navigation)", "Apache-2.0"),
            LibEntry("AndroidX Core/Activity/Lifecycle", "Apache-2.0"),
            LibEntry("Datastore Preferences", "Apache-2.0"),
            LibEntry("Kotlin Stdlib / Coroutines", "Apache-2.0"),
            LibEntry("Material Icons Extended", "Apache-2.0"),
            LibEntry("Osmdroid (OpenStreetMap tiles)", "Apache-2.0")
        )
    }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("RMD", style = MaterialTheme.typography.titleLarge)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            Text("Works offline. Receive commands via SMS or mesh and trigger device actions.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { openUrl(context, "https://github.com/odesaur/RingMyDevice") }) {
                    Text("Source code")
                }
            }
            Text("License", style = MaterialTheme.typography.titleMedium)
            Text("GPL-3.0. Inspired by FMD Android (https://gitlab.com/fmd-foss/fmd-android).")
            HorizontalDivider()
            Text("Libraries", style = MaterialTheme.typography.titleMedium)
            libs.forEach { entry ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall)
                        Text("License: ${entry.license}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Android system services for SMS, location, notifications", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private data class LibEntry(val name: String, val license: String)
