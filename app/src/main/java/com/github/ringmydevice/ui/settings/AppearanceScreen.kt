package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val themeOptions = listOf("System default", "Light", "Dark")
    var selectedTheme by rememberSaveable { mutableStateOf(themeOptions.first()) }
    var dynamicColors by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(themeOptions.size) { i ->
                val option = themeOptions[i]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTheme = option }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    RadioButton(
                        selected = selectedTheme == option,
                        onClick = { selectedTheme = option }
                    )
                    Text(option, modifier = Modifier.padding(start = 12.dp))
                }
                Divider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Dynamic colors") },
                    supportingContent = { Text("Match system accent colors") },
                    trailingContent = {
                        Switch(checked = dynamicColors, onCheckedChange = { dynamicColors = it })
                    }
                )
                Divider()
            }
            item {
                Button(
                    onClick = {
                        // placeholder
                        scope.launch {
                            snackbarHostState.showSnackbar("Appearance updated")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}