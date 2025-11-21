package com.github.ringmydevice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCellIdScreen(onBack: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    // Mock Data representing what we send to OpenCelliD
    val mockUploadData = """
        {
          "token": "pk.7f0...",
          "radio": "lte",
          "mcc": 310,
          "mnc": 410,
          "cells": [
            {
              "lac": 7033,
              "cid": 17811,
              "psc": 0
            }
          ],
          "address": 1
        }
    """.trimIndent()

    fun startMockScan() {
        isScanning = true
        scanResult = null
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(2000) // Fake network delay
            isScanning = false
            scanResult = """
                {
                  "status": "ok",
                  "lat": 37.7749,
                  "lon": -122.4194,
                  "accuracy": 600,
                  "address": "Market St, San Francisco, CA"
                }
            """.trimIndent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenCelliD Mock") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                icon = Icons.Outlined.Upload,
                title = "Data Payload (Mock)",
                subtitle = "The following cell tower data is gathered from the modem and prepared for upload:",
                code = mockUploadData
            )

            Button(
                onClick = { startMockScan() },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Text("Simulate Upload")
                }
            }

            if (scanResult != null) {
                InfoCard(
                    icon = Icons.Outlined.CellTower,
                    title = "API Response (Mock)",
                    subtitle = "OpenCelliD returned the following location for this cell tower:",
                    code = scanResult ?: ""
                )
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, subtitle: String, code: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    text = code,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}