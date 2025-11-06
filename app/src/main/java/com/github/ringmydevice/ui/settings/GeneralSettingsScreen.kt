package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // demo / local state only
    var ringEnabled by rememberSaveable { mutableStateOf(true) }
    var locationEnabled by rememberSaveable { mutableStateOf(true) }
    var photoEnabled by rememberSaveable { mutableStateOf(false) }
    var trustedNumber by rememberSaveable { mutableStateOf("") }
    var secretKey by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SwitchRow(
                    title = "Enable Ring Command",
                    subtitle = "Allow remote ring even if the phone is on silent",
                    checked = ringEnabled,
                    onCheckedChange = { ringEnabled = it }
                )
                Divider()
            }
            item {
                SwitchRow(
                    title = "Enable Location",
                    subtitle = "Return GPS coordinates on request",
                    checked = locationEnabled,
                    onCheckedChange = { locationEnabled = it }
                )
                Divider()
            }
            item {
                SwitchRow(
                    title = "Enable Photo Capture",
                    subtitle = "Take a snapshot when requested",
                    checked = photoEnabled,
                    onCheckedChange = { photoEnabled = it }
                )
                Divider()
            }
            item {
                OutlinedTextField(
                    value = trustedNumber,
                    onValueChange = { trustedNumber = it },
                    label = { Text("Trusted phone number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("Shared secret (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            // placeholder
                            // TODO: wire to persistence later
                            scope.launch {
                                snackbarHostState.showSnackbar("Settings saved")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Test ring")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Test ring") }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}