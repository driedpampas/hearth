package org.eu.nl.syu.charchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text("System Default") },
                    trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Haptics") },
                    trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Models", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            // LiteRT Section
            item {
                Text("LiteRT Models (Active)", style = MaterialTheme.typography.labelLarge)
            }
            item {
                ModelItem(
                    name = "Gemma 4 E2B",
                    status = "Active",
                    isActive = true
                )
            }

            // GGUF Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("GGUF Models (Coming Soon)", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            }
            item {
                ModelItem(
                    name = "Llama 3 8B (Placeholder)",
                    status = "Future Support",
                    isActive = false
                )
            }
        }
    }
}

@Composable
fun ModelItem(
    name: String,
    status: String,
    isActive: Boolean
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        enabled = isActive
    ) {
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = { Text(status) },
            leadingContent = { Icon(Icons.Filled.ModelTraining, contentDescription = null) },
            trailingContent = {
                if (isActive) {
                    Icon(Icons.Filled.Download, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
