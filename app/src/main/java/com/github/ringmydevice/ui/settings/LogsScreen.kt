package com.github.ringmydevice.ui.settings

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
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.data.model.LogCategory
import com.github.ringmydevice.viewmodel.AppLogViewModel
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = viewModel<AppLogViewModel>()
    LaunchedEffect(Unit) { vm.refresh() }
    val logs by vm.logs.collectAsState()
    val selectedCategory = remember { mutableStateOf<LogCategory?>(null) }
    val categories = listOf<LogCategory?>(null, LogCategory.COMMAND, LogCategory.PERMISSION, LogCategory.SETTINGS, LogCategory.GENERAL)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                Divider()
            }
        }
    }
}
