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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCellIdScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val token by viewModel.openCellIdToken.collectAsState()
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
