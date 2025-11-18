package com.github.ringmydevice.ui.settings

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.github.ringmydevice.R
import androidx.core.net.toUri

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // access system services


    // read from SettingsViewModel
    val ringEnabled by viewModel.ringEnabled.collectAsState()
    val locationEnabled by viewModel.locationEnabled.collectAsState()
    val photoEnabled by viewModel.photoEnabled.collectAsState()

    // initial load for TextFields
    val initialNumber by viewModel.savedTrustedNumber.collectAsState(initial = "")
    val initialSecret by viewModel.savedSecretKey.collectAsState(initial = "")

    // local state for text inputs
    var trustedNumber by remember(initialNumber) { mutableStateOf(initialNumber) }
    var secretKey by remember(initialSecret) { mutableStateOf(initialSecret) }

    LaunchedEffect(initialNumber) { trustedNumber = initialNumber }
    LaunchedEffect(initialSecret) { secretKey = initialSecret }

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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                SwitchRow(
                    title = "Enable Ring Command",
                    subtitle = "Allow remote ring even if the phone is on silent",
                    checked = ringEnabled,
                    onCheckedChange = { viewModel.setRingEnabled(it) }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
            item {
                SwitchRow(
                    title = "Enable Location",
                    subtitle = "Return GPS coordinates on request",
                    checked = locationEnabled,
                    onCheckedChange = { viewModel.setLocationEnabled(it) }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
            item {
                SwitchRow(
                    title = "Enable Photo Capture",
                    subtitle = "Take a snapshot when requested",
                    checked = photoEnabled,
                    onCheckedChange = { viewModel.setPhotoEnabled(it) }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
            item {
                OutlinedTextField(
                    value = trustedNumber,
                    onValueChange = { trustedNumber = it },
                    label = { Text("Trusted phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                            viewModel.saveTextSettings(trustedNumber, secretKey)
                            scope.launch {
                                snackbarHostState.showSnackbar("Settings saved")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val audioManager =
                                    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                                // store the user current volume and ringer mode
                                // we want the test ring to be at max volume and in normal mode
                                val oldRingerMode = audioManager.ringerMode
                                val oldVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_RING)
                                val maxVolume =
                                    audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

                                val ringtoneUri =
                                    "android.resource://${context.packageName}/${R.raw.ping_sound}".toUri()
                                val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)

                                try {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_RING,
                                        maxVolume,
                                        0
                                    )

                                    snackbarHostState.showSnackbar("Playing testing ring...")

                                    ringtone.isLooping = true
                                    ringtone.play()

                                    delay(8000)

                                    ringtone.stop()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error playing test ring: ${e.message}")
                                } finally {
                                    // restore the user volume and ringer mode
                                    ringtone.isLooping = false
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_RING,
                                        oldVolume,
                                        0
                                    )
                                    audioManager.ringerMode = oldRingerMode
                                }
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
