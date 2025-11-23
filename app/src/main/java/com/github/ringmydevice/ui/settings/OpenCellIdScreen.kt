package com.github.ringmydevice.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCellIdScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val token by viewModel.openCellIdToken.collectAsState()
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenCelliD") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "FMD can use the OpenCelliD service to get an approximate location from the current cell tower. You need an OpenCelliD account and API token.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = { openUrl(context, "https://opencellid.org") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open OpenCelliD website")
            }
            Divider()
            Text(
                "Contribute by collecting and uploading tower information. Tower Collector is one option available on F-Droid.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { openUrl(context, "https://f-droid.org/en/packages/info.zamojski.soft.towercollector/") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download Tower Collector")
            }
            Spacer(Modifier.height(12.dp))
            Text("API Access Token", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = token,
                onValueChange = { viewModel.setOpenCellIdToken(it) },
                placeholder = { Text("Enter your OpenCelliD token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Save the token here for use when calling OpenCelliD.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                enabled = token.isNotBlank() && !testing,
                onClick = {
                    testing = true
                    testResult = "Testing connection…"
                    scope.launch {
                        val result = testOpenCellId(token)
                        testResult = result
                        testing = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (testing) "Testing..." else "Test OpenCelliD connection")
            }
            testResult?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private suspend fun testOpenCellId(token: String): String = withContext(Dispatchers.IO) {
    if (token.isBlank()) return@withContext "Error: API key not valid: (empty). Get an API key at https://opencellid.org"
    val testUrl = buildSampleTestUrl(token)
    return@withContext runCatching {
        val conn = (URL(testUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        val body = conn.inputStream.bufferedReader().use { it.readText().take(600) }
        "Sample query (static cell):\n$testUrl\n\nResponse:\n$body"
    }.getOrElse { err ->
        "OpenCelliD sample query:\n$testUrl\n\nError: API key not valid: $token\nGet an API key at https://opencellid.org\nDetails: ${err.message ?: "Unknown error"}"
    }
}

private fun buildSampleTestUrl(token: String): String {
    val mcc = 302
    val mnc = 490
    val lac = 60400
    val cellId = 103735829
    val radio = "LTE"
    return "https://opencellid.org/cell/get?key=$token&mcc=$mcc&mnc=$mnc&lac=$lac&cellid=$cellId&radio=$radio&format=json"
}
