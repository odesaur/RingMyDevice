package com.github.ringmydevice.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val rmdPinEnabled by viewModel.rmdPinEnabled.collectAsState()
    val rmdCommand by viewModel.rmdCommand.collectAsState()
    val ringtoneUri by viewModel.rmdRingtone.collectAsState()
    val lockMessage by viewModel.rmdLockMessage.collectAsState()
    val smsFeedbackEnabled by viewModel.smsFeedbackEnabled.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    var commandText by remember(rmdCommand) { mutableStateOf(rmdCommand) }
    var lockMessageText by remember(lockMessage) { mutableStateOf(lockMessage) }

    fun commitCommand() {
        val trimmed = commandText.trim()
        if (trimmed.isNotEmpty()) {
            viewModel.setRmdCommand(trimmed)
        } else {
            commandText = rmdCommand
        }
    }

    fun commitLockMessage() {
        viewModel.setRmdLockMessage(lockMessageText.trim())
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        viewModel.setRmdRingtone(uri?.toString() ?: "")
    }

    fun launchRingtonePicker() {
        val currentUri = ringtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        }
        ringtonePickerLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General") },
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("RMD via PIN", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Allows you to communicate with RMD via a PIN. This allows you to send commands from a phone number that is not in the allowed contacts list.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = rmdPinEnabled,
                            onCheckedChange = { enabled -> viewModel.setRmdPinEnabled(enabled) }
                        )
                        Text("Enable", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PIN", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Used as confirmation before wiping the device and for anonymous usage of the service.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Warning: The PIN is not included in the app backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            pinInput = ""
                            showPinDialog = true
                        },
                        enabled = rmdPinEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set PIN")
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("rmd command", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The command used to communicate with the RMD.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused) {
                                    commitCommand()
                                }
                            },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                commitCommand()
                                focusManager.clearFocus()
                            }
                        )
                    )
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SMS feedback", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Send SMS confirmations back to trusted contacts after commands are processed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = smsFeedbackEnabled,
                            onCheckedChange = { enabled -> viewModel.setSmsFeedbackEnabled(enabled) }
                        )
                        Text("Send SMS feedback", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("rmd ring", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Select a ringtone that will be played when rmd ring is sent.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { launchRingtonePicker() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select ringtone")
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("rmd lock", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Lock screen message:\nWhen locking the device, this message will appear on the lock screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = lockMessageText,
                        onValueChange = { lockMessageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .onFocusChanged { focus ->
                                if (!focus.isFocused) {
                                    commitLockMessage()
                                }
                            },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                commitLockMessage()
                                focusManager.clearFocus()
                            }
                        )
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a numeric PIN to confirm sensitive actions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { value ->
                            pinInput = value.filter { it.isDigit() }.take(8)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pinInput.length >= 4) {
                                    viewModel.setRmdPin(pinInput)
                                    showPinDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("PIN updated")
                                    }
                                }
                            }
                        ),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text(
                        text = "PIN must be at least 4 digits.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length >= 4) {
                            viewModel.setRmdPin(pinInput)
                            showPinDialog = false
                            scope.launch { snackbarHostState.showSnackbar("PIN updated") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Enter at least 4 digits") }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
