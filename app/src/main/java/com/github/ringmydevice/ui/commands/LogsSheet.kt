package com.github.ringmydevice.ui.commands

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ringmydevice.viewmodel.CommandViewModel
import java.text.SimpleDateFormat

@Composable
fun LogsSheet(vm: CommandViewModel, onClose: () -> Unit) {
    val logs = vm.logs.collectAsState()
    Surface(tonalElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Recent actions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs.value) { log ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text("${log.type} — ${log.notes ?: ""}")
                            Text(
                                text = SimpleDateFormat("HH:mm:ss")
                                    .format(java.util.Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
        }
    }
}
