package com.github.ringmydevice.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel
import com.github.ringmydevice.data.repo.RmdServerRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.material3.Divider
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.github.ringmydevice.R
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmdServerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { RmdServerRepository.getInstance() }
    val serverUrl by viewModel.fmdServerUrl.collectAsState()
    val userId by viewModel.fmdUserId.collectAsState(initial = "")
    val accessToken by viewModel.fmdAccessToken.collectAsState(initial = "")
    val uploadEnabled by viewModel.fmdUploadWhenOnline.collectAsState(initial = true) // kept for future use
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val isLoggedIn = accessToken.isNotBlank() && userId.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RMD Server") },
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
            if (isLoggedIn) {
                ServerInfoCard(
                    serverUrl = serverUrl,
                    userId = userId,
                    onCopyUrl = { copyToClipboard(context, "Server URL", serverUrl) },
                    onOpenUrl = { if (serverUrl.isNotBlank()) openUrl(context, serverUrl) },
                    onCopyUser = { copyToClipboard(context, "User ID", userId) },
                    status = if (statusMessage.isNotBlank()) statusMessage else "Connected",
                    isLoggedIn = true
                )
                PostAuthActions(
                    onVerify = {
                        scope.launch {
                            val baseUrl = serverUrl.trim().trimEnd('/')
                            val trimmedToken = accessToken.trim()
                            if (baseUrl.isBlank() || trimmedToken.isBlank()) {
                                val message = "Enter server URL and access token"
                                statusMessage = message
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            try {
                                val ok = repo.verifyAccessToken(baseUrl, trimmedToken)
                                val message = if (ok) "Connection OK" else "Failed to verify token/server"
                                statusMessage = message
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                val message = "Verify error: ${e.message ?: "unknown"}"
                                statusMessage = message
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onChangePassword = { showChangePasswordDialog = true },
                    onLogout = {
                        viewModel.setFmdAccessToken("")
                        viewModel.setFmdUserId("")
                        statusMessage = "Logged out"
                        Toast.makeText(context, "Logged out", Toast.LENGTH_LONG).show()
                    },
                    onDelete = {
                        val url = serverUrl.trim().trimEnd('/')
                        val token = accessToken.trim()
                        if (url.isBlank() || token.isBlank()) {
                            Toast.makeText(context, "Enter server URL and access token", Toast.LENGTH_LONG).show()
                            return@PostAuthActions
                        }
                        scope.launch {
                            val ok = repo.deleteAccount(url, token)
                            val msg = if (ok) "Account deleted" else "Delete failed"
                            statusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (ok) {
                                viewModel.setFmdAccessToken("")
                                viewModel.setFmdUserId("")
                            }
                        }
                    }
                )
            } else {
                PreAuthCard(
                    serverUrl = serverUrl,
                    onUrlChange = { viewModel.setFmdServerUrl(it) },
                    status = statusMessage,
                    onPing = {
                        scope.launch {
                            val baseUrl = serverUrl.trim().trimEnd('/')
                            if (baseUrl.isBlank()) {
                                statusMessage = "Enter server URL"
                                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            val version = repo.getServerVersion(baseUrl)
                            val msg = version?.let { "Server reachable (version $it)" } ?: "Cannot reach server"
                            statusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onRegister = { showRegisterDialog = true },
                    onLogin = { showLoginDialog = true }
                )
            }
        }
    }
    if (showRegisterDialog) {
        RegisterDialog(
            initialServerUrl = serverUrl,
            onDismiss = { showRegisterDialog = false },
            onRegister = { url, desiredId, password, token ->
                scope.launch {
                    val baseUrl = url.trim().trimEnd('/')
                    if (baseUrl.isBlank()) {
                        Toast.makeText(context, "Enter server URL", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    try {
                        val deviceId = repo.registerDevice(baseUrl, desiredId, password, token)
                        if (deviceId.isNullOrBlank()) {
                            Toast.makeText(context, "Registration failed", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        val access = repo.login(baseUrl, deviceId, password)
                        if (access.isNullOrBlank()) {
                            Toast.makeText(context, "Login failed after registration", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        viewModel.setFmdServerUrl(baseUrl)
                        viewModel.setFmdUserId(deviceId)
                        viewModel.setFmdAccessToken(access)
                        statusMessage = "Connected"
                        Toast.makeText(context, "Registered as $deviceId", Toast.LENGTH_LONG).show()
                        showRegisterDialog = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "Registration error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
    if (showLoginDialog) {
        LoginDialog(
            initialServerUrl = serverUrl,
            onDismiss = { showLoginDialog = false },
            onLogin = { url, id, password ->
                scope.launch {
                    val baseUrl = url.trim().trimEnd('/')
                    if (baseUrl.isBlank()) {
                        Toast.makeText(context, "Enter server URL", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val access = repo.login(baseUrl, id, password)
                    if (access.isNullOrBlank()) {
                        Toast.makeText(context, "Login failed", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    viewModel.setFmdServerUrl(baseUrl)
                    viewModel.setFmdUserId(id)
                    viewModel.setFmdAccessToken(access)
                    statusMessage = "Connected"
                    Toast.makeText(context, "Logged in", Toast.LENGTH_LONG).show()
                    showLoginDialog = false
                }
            }
        )
    }
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onChange = { newPassword ->
                val url = serverUrl.trim().trimEnd('/')
                val token = accessToken.trim()
                if (url.isBlank() || token.isBlank()) {
                    Toast.makeText(context, "Enter server URL and access token", Toast.LENGTH_LONG).show()
                    return@ChangePasswordDialog
                }
                scope.launch {
                    val ok = repo.changePassword(url, token, newPassword)
                    val msg = if (ok) "Password updated" else "Password change failed"
                    statusMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    showChangePasswordDialog = false
                }
            }
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
private fun RegisterDialog(
    initialServerUrl: String,
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String) -> Unit
) {
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register with RMD Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID (optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Registration token (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRegister(serverUrl, userId, password, token) }) { Text("Register") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LoginDialog(
    initialServerUrl: String,
    onDismiss: () -> Unit,
    onLogin: (String, String, String) -> Unit
) {
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login to RMD Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onLogin(serverUrl, userId, password) }) { Text("Login") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChange: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onChange(newPassword) }) { Text("Update") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ServerInfoCard(
    serverUrl: String,
    userId: String,
    onCopyUrl: () -> Unit,
    onOpenUrl: () -> Unit,
    onCopyUser: () -> Unit,
    status: String,
    isLoggedIn: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1f1d2e)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("General", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFebbcba)))
            InfoRow(label = "Server URL", value = serverUrl, actions = {
                IconButton(onClick = onOpenUrl) { Icon(Icons.Outlined.OpenInBrowser, contentDescription = "Open") }
                IconButton(onClick = onCopyUrl) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy URL") }
            })
            InfoRow(label = "User ID", value = userId, actions = {
                IconButton(onClick = onCopyUser) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy user id") }
            })
            val statusColor = if (isLoggedIn) Color(0xFF9ccfd8) else Color(0xFFeb6f92)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(10.dp)
                        .padding(end = 8.dp)
                        .background(color = statusColor, shape = RoundedCornerShape(50))
                )
                Text(status.ifBlank { if (isLoggedIn) "Connected" else "Not connected" }, color = statusColor, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PreAuthCard(
    serverUrl: String,
    onUrlChange: (String) -> Unit,
    status: String,
    onPing: () -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1f1d2e)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Connect to server", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFebbcba)))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = onUrlChange,
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.x.x:8080") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (status.isNotBlank()) {
                Text(status, color = Color(0xFF9ccfd8), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onPing, modifier = Modifier.weight(1f)) { Text("Check server") }
                OutlinedButton(onClick = onRegister, modifier = Modifier.weight(1f)) { Text("Register") }
                Button(onClick = onLogin, modifier = Modifier.weight(1f)) { Text("Login") }
            }
        }
    }
}

@Composable
private fun PostAuthActions(
    onVerify: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1f1d2e)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Account actions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFebbcba)))
            Button(onClick = onVerify, modifier = Modifier.fillMaxWidth()) { Text("Verify connection") }
            Button(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) { Text("Change password") }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFeb6f92))
            ) { Text("Delete account") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, actions: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6e6a86))
            Text(value.ifBlank { "Not set" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

private fun copyToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
