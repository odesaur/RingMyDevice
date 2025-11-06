package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.AllowedContactsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedContactsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AllowedContactsViewModel = viewModel()
) {
    var input by remember { mutableStateOf("") }
    val contacts by vm.contacts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Allowed Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.clear()
                            scope.launch { snackbarHostState.showSnackbar("Cleared allowed list") }
                        },
                        enabled = contacts.isNotEmpty()
                    ) {
                        Text("Clear all", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Phone number or name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = input.isNotBlank(),
                    onClick = {
                        val added = vm.add(input) {
                            scope.launch { snackbarHostState.showSnackbar("Already in list") }
                        }
                        if (added) {
                            input = ""
                            scope.launch { snackbarHostState.showSnackbar("Added") }
                        }
                    }
                ) { Text("Add") }
            }
            Divider()
            Text("Allowed list", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(contacts, key = { _, c -> c }) { idx, c ->
                    ElevatedCard {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(c, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { vm.removeAt(idx) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
