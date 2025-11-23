package com.github.ringmydevice.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.data.model.LogCategory
import com.github.ringmydevice.viewmodel.AppLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = viewModel<AppLogViewModel>()
    LaunchedEffect(Unit) { vm.refresh() }
    val logs by vm.logs.collectAsState()
    val selectedCategory = remember { mutableStateOf<LogCategory?>(null) }
    val categories = listOf<LogCategory?>(null, LogCategory.COMMAND, LogCategory.PERMISSION, LogCategory.SETTINGS, LogCategory.GENERAL)
    var status by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.export(context.contentResolver, uri) { ok ->
                status = if (ok) "Logs exported" else "Export failed"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val name = "fmd-logs-${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.json"
                        exportLauncher.launch(name)
                    }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "Export logs")
                    }
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear logs")
                    }
                }
            )
        }
    ) { inner ->
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear logs?") },
                text = { Text("Are you sure you want to clear all stored logs?") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearConfirm = false
                        vm.clear { ok ->
                            status = if (ok) "Logs cleared" else "Unable to clear logs"
                        }
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                }
            )
        }
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory.value = category },
                            label = {
                                Text(
                                    when (category) {
                                        null -> "All"
                                        LogCategory.COMMAND -> "Commands"
                                        LogCategory.PERMISSION -> "Permissions"
                                        LogCategory.SETTINGS -> "Settings"
                                        LogCategory.GENERAL -> "General"
                                    }
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCategory.value == category) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        )
                    }
                }
            }
            val filtered = logs.filter { selectedCategory.value == null || it.category == selectedCategory.value }
                .sortedByDescending { it.timeMillis }
            items(filtered) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "${log.tag} · ${log.category.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date(log.timeMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.HorizontalDivider()
            }
        }
    }
}
