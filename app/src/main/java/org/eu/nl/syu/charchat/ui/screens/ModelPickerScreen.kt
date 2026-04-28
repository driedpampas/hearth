package org.eu.nl.syu.charchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.charchat.data.AllowedModel
import org.eu.nl.syu.charchat.ui.viewmodels.HomeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerScreen(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onSelectModel: (java.io.File) -> Unit,
    onOpenSettings: () -> Unit,
    showModelSettings: Boolean,
    onDismissModelSettings: () -> Unit,
    onSaveBackend: (String) -> Unit,
    onSaveMaxTokens: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Select Model") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Widgets, contentDescription = "Load Options")
                    }
                }
            )
        }
    ) { paddingValues ->
        val filenameToModel = remember(uiState.availableModels) {
            mutableMapOf<String, AllowedModel>().apply {
                uiState.availableModels.forEach { model ->
                    put(model.modelFile, model)
                    model.socToModelFiles?.values?.forEach { socModel ->
                        socModel.modelFile?.let { put(it, model) }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isModelLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading model...")
                        }
                    }
                }
                uiState.notification != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Model unavailable")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.notification.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
                uiState.downloadedModels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No models downloaded")
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItems(uiState.downloadedModels) { file ->
                            val allowedModel = filenameToModel[file.name]
                            val sizeInMb = file.length() / (1024f * 1024f)

                            ElevatedCard(
                                onClick = { onSelectModel(file) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (uiState.isModelLoading) 0.5f else 1f),
                                enabled = !uiState.isModelLoading,
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val hfName = allowedModel?.name ?: file.name
                                        Text(
                                            text = hfName,
                                            fontWeight = if (uiState.selectedModel == file.name && uiState.isModelLoaded) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FileOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = file.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storage,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = String.format("%.1f MB", sizeInMb),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (uiState.selectedModel == file.name && uiState.isModelLoaded) {
                                        Icon(Icons.Default.Widgets, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isModelLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter))
            }
        }
    }

    if (showModelSettings) {
        ModelSettingsDialog(
            uiState = uiState,
            onDismiss = onDismissModelSettings,
            onSaveBackend = onSaveBackend,
            onSaveMaxTokens = onSaveMaxTokens
        )
    }
}
