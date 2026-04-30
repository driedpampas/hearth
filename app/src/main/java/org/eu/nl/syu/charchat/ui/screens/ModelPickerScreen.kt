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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.charchat.ui.components.GlassySurface
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.eu.nl.syu.charchat.ui.viewmodels.HomeUiState
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.eu.nl.syu.charchat.common.ModelSizeUtils
import java.io.File

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.eu.nl.syu.charchat.ui.viewmodels.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerScreen(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToModelSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onSelectModel: (java.io.File) -> Unit = { file -> viewModel.selectModel(file) }

    val filenameToModel = remember(uiState.availableModels) {
        buildMap {
            uiState.availableModels.forEach { model ->
                model.localFileName?.let { put(it, model) }
                put(model.modelFile, model)
                put(model.modelId.substringAfterLast("/"), model)
                put(model.modelId.substringAfterLast("/") + ".litertlm", model)
                put(model.modelId.substringAfterLast("/") + ".tflite", model)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.reloadLocalModels()
    }

    var showRetryDialog by remember { mutableStateOf(false) }
    var retryFile by remember { mutableStateOf<File?>(null) }

    if (showRetryDialog && retryFile != null) {
        AlertDialog(
            onDismissRequest = { showRetryDialog = false },
            title = { Text("Retry Model") },
            text = { Text("This model previously failed to load. Do you want to retry loading it?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.retryModel(retryFile!!)
                    showRetryDialog = false
                }) {
                    Text("Retry")
                }
            },
            dismissButton = {
                Button(onClick = { showRetryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    android.util.Log.d("ModelPickerScreen", "Composing ModelPickerScreen: downloadedModels=${uiState.downloadedModels.size}, selectedModel=${uiState.selectedModel}")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Select Model") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.notification != null) {
                            viewModel.clearNotification()
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                        uiState.isModelLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        org.eu.nl.syu.charchat.ui.components.PremiumLoadingText("Loading model...")
                    }
                }
                uiState.notification != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Model unavailable",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.notification.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToModelSettings,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adjust Settings")
                            }
                        }
                    }
                }
                uiState.downloadedModels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No models downloaded",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenSettings,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Models")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.downloadedModels) { mFile ->
                            val isFailed = uiState.failedModels.contains(mFile.name)
                            ModelItem(
                                mFile = mFile,
                                uiState = uiState,
                                filenameToModel = filenameToModel,
                                onSelectModel = onSelectModel,
                                onOpenModelSettings = onNavigateToModelSettings,
                                isFailed = isFailed,
                                onRetry = { retryFile = mFile; showRetryDialog = true }
                            )
                        }
                    }
                }
            }

            // PremiumLoadingText is used in the center, so no top bar indicator needed
        }
    }
    }
}

@Composable
private fun ModelItem(
    mFile: java.io.File,
    uiState: HomeUiState,
    filenameToModel: Map<String, org.eu.nl.syu.charchat.data.AllowedModel>,
    onSelectModel: (java.io.File) -> Unit,
    onOpenModelSettings: () -> Unit,
    isFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val currentModelName = mFile.name
    val allowedModel = filenameToModel[currentModelName]
    val diskSizeMb = mFile.length().toFloat() / (1024f * 1024f)
    val diskSizeText = if (diskSizeMb >= 1024f) 
        String.format(Locale.US, "%.2f GB on disk", diskSizeMb / 1024f)
    else 
        String.format(Locale.US, "%.1f MB on disk", diskSizeMb)

    GlassySurface(
        onClick = { if (isFailed) onRetry() else onSelectModel(mFile) },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (uiState.isModelLoading || isFailed) 0.5f else 1f),
        enabled = !uiState.isModelLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val hfName = allowedModel?.name ?: currentModelName
                val author = allowedModel?.author ?: "Unknown Author"
                val modelSize = ModelSizeUtils.parseModelSize(hfName)
                val context = LocalContext.current
                val compatibility = modelSize?.let { ModelSizeUtils.checkCompatibility(context, it) }

                Text(
                    text = hfName,
                    fontWeight = if (uiState.selectedModel == currentModelName && uiState.isModelLoaded) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val sizeText = modelSize?.let { ModelSizeUtils.formatParameterCount(it) } ?: "?"
                    Text(
                        text = "$sizeText Parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (compatibility != null) {
                        val (color, text, icon) = when (compatibility) {
                            ModelSizeUtils.Compatibility.FITS -> Triple(Color(0xFF4CAF50), "Fits", Icons.Default.CheckCircle)
                            ModelSizeUtils.Compatibility.CLOSE -> Triple(Color(0xFFFF9800), "Cutting it close", Icons.Default.Warning)
                            ModelSizeUtils.Compatibility.TOO_BIG -> Triple(Color(0xFFF44336), "Too big / OOM", Icons.Default.Warning)
                            ModelSizeUtils.Compatibility.UNKNOWN -> Triple(Color.Gray, "Model may not fit", Icons.Default.Warning)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = color
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = color
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp).alpha(0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = diskSizeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isFailed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Previously failed to load",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            if (uiState.selectedModel == currentModelName || isFailed) {
                IconButton(onClick = onOpenModelSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Model Options")
                }
            }
        }
    }
}


