package org.eu.nl.syu.charchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.eu.nl.syu.charchat.ui.viewmodels.ModelSettingsUiState
import org.eu.nl.syu.charchat.ui.viewmodels.ModelSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    characterId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ModelSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(characterId) {
        viewModel.loadSettings(characterId)
    }

    var pendingTemp by remember { mutableStateOf(0.8f) }
    var pendingTopP by remember { mutableStateOf(0.95f) }
    var pendingTopK by remember { mutableStateOf(40) }
    var pendingThinking by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.character) {
        uiState.character?.let { character ->
            pendingTemp = character.temp
            pendingTopP = character.topP
            pendingTopK = character.topK
            pendingThinking = character.enableThinking
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Global Model Settings", style = MaterialTheme.typography.titleMedium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preferred Hardware", style = MaterialTheme.typography.labelMedium)
                    for (backend in listOf("Automatic", "CPU", "GPU", "NPU")) {
                        if (backend == "NPU" && !uiState.experimentalNpuEnabled) continue
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.preferredBackend == backend,
                                onClick = { viewModel.updateBackend(backend) }
                            )
                            Text(backend)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Max Context Tokens: ${uiState.defaultMaxTokens}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = uiState.defaultMaxTokens.toFloat(),
                        onValueChange = { viewModel.updateMaxTokens(it.toInt()) },
                        valueRange = 1024f..16384f,
                        steps = 15
                    )
                }

                if (uiState.character != null) {
                    Text("Character-Specific Settings", style = MaterialTheme.typography.titleMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Temperature: $pendingTemp")
                        Slider(
                            value = pendingTemp,
                            onValueChange = { pendingTemp = it },
                            valueRange = 0.0f..2.0f
                        )

                        Text("Top P: $pendingTopP")
                        Slider(
                            value = pendingTopP,
                            onValueChange = { pendingTopP = it },
                            valueRange = 0.0f..1.0f
                        )

                        Text("Top K: $pendingTopK")
                        Slider(
                            value = pendingTopK.toFloat(),
                            onValueChange = { pendingTopK = it.toInt() },
                            valueRange = 1f..100f,
                            steps = 98
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = pendingThinking,
                                onCheckedChange = { pendingThinking = it }
                            )
                            Text("Enable thinking")
                        }

                        TextButton(
                            onClick = {
                                viewModel.updateCharacterSettings(
                                    temp = pendingTemp,
                                    topP = pendingTopP,
                                    topK = pendingTopK,
                                    enableThinking = pendingThinking
                                )
                            }
                        ) {
                            Text("Apply Character Settings")
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reloading model...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}
