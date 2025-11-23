package com.github.ringmydevice.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.viewmodel.AllowedContactsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedContactsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AllowedContactsViewModel = viewModel()
) {
    val contacts by vm.contacts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var dialogName by remember { mutableStateOf("") }
    var dialogPhone by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun openDialog(name: String = "", phone: String = "") {
        dialogName = name
        dialogPhone = phone
        showDialog = true
    }

    fun pickPhoneIntent(): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val picked = readAllowedContactFromUri(context, uri)
        if (picked == null) {
            scope.launch { snackbarHostState.showSnackbar("Unable to read contact") }
        } else {
            openDialog(picked.name, picked.phoneNumber)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPickerLauncher.launch(pickPhoneIntent())
        } else {
            scope.launch { snackbarHostState.showSnackbar("Contacts permission denied") }
        }
    }

    fun launchContactPicker() {
        val permission = Manifest.permission.READ_CONTACTS
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            contactPickerLauncher.launch(pickPhoneIntent())
        } else {
            permissionLauncher.launch(permission)
        }
    }

    fun confirmAdd() {
        val digits = dialogPhone.filter { it.isDigit() }
        if (digits.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Enter a valid phone number") }
            return
        }
        val added = vm.add(dialogName, dialogPhone) {
            scope.launch { snackbarHostState.showSnackbar("Already in list") }
        }
        if (added) {
            showDialog = false
            dialogName = ""
            dialogPhone = ""
            scope.launch { snackbarHostState.showSnackbar("Added") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Allowed contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        enabled = contacts.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = "Clear all")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Contacts on this list can send commands via SMS.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Text("Allowed list", style = MaterialTheme.typography.titleMedium)

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No allowed contacts. Click the button to add some!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        contacts,
                        key = { _, contact -> contact.phoneNumber.filter { it.isDigit() } + contact.name }
                    ) { idx, contact ->
                        val formatted = vm.displayNumber(contact.phoneNumber)
                        AllowedContactRow(
                            contact = contact,
                            formattedNumber = formatted,
                            onRemove = { vm.removeAt(idx) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { launchContactPicker() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add contact")
                }
                Button(
                    onClick = { openDialog() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add phone number")
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add phone number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogPhone,
                        onValueChange = { dialogPhone = it },
                        label = { Text("Phone number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmAdd() },
                    enabled = dialogPhone.any { it.isDigit() }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear allowed contacts?") },
            text = { Text("Are you sure you want to remove all allowed contacts?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    vm.clear()
                    scope.launch { snackbarHostState.showSnackbar("Cleared allowed list") }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AllowedContactRow(contact: AllowedContact, formattedNumber: String, onRemove: () -> Unit) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (contact.name.isNotBlank()) contact.name else contact.phoneNumber,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    formattedNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
    }
}

private fun readAllowedContactFromUri(context: Context, uri: Uri): AllowedContact? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
        val phoneNumber = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
        if (phoneNumber.isBlank()) return null
        return AllowedContact(name = displayName, phoneNumber = phoneNumber)
    }
    return null
}
