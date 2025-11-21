package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.github.ringmydevice.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmdServerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe values from ViewModel
    val serverUrl by viewModel.fmdServerUrl.collectAsState()
    val accessToken by viewModel.fmdAccessToken.collectAsState()
    val uploadWhenOnline by viewModel.fmdUploadWhenOnline.collectAsState()

    // Observe the connection test status
    // Note: You must add 'connectionStatus' to your SettingsViewModel (see below)
    val connectionStatus = viewModel.connectionStatus

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FMD Server") },
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
            // --- SERVER URL INPUT ---
            item {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { viewModel.setFmdServerUrl(it) },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://your-server.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // --- ACCESS TOKEN INPUT ---
            item {
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { viewModel.setFmdAccessToken(it) },
                    label = { Text("Access token (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // --- UPLOAD TOGGLE ---
            item {
                ListItem(
                    headlineContent = { Text("Upload when online") },
                    supportingContent = { Text("Queue updates offline and send when network is available") },
                    trailingContent = {
                        Switch(
                            checked = uploadWhenOnline,
                            onCheckedChange = { viewModel.setFmdUploadWhenOnline(it) }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // --- ACTION BUTTONS ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // The Test Connection Button
                    Button(
                        onClick = {
                            // Call the test function in ViewModel
                            viewModel.testServerConnection()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Connection")
                    }

                    // The Unlink/Clear Button
                    OutlinedButton(
                        onClick = {
                            viewModel.setFmdServerUrl("")
                            viewModel.setFmdAccessToken("")
                            scope.launch {
                                snackbarHostState.showSnackbar("Server settings cleared")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlink")
                    }
                }
            }

            // --- CONNECTION STATUS CARD ---
            // Only shows up if a test has been run
            if (connectionStatus != null) {
                item {
                    val isSuccess = connectionStatus.contains("Success", ignoreCase = true)
                    val cardColor = if (isSuccess) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                    val icon = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Error

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = connectionStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- HELPER TEXT ---
            item {
                Text(
                    text = "Once linked, your device can post locations and receive commands via the server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}