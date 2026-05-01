/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.eu.nl.syu.hearth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.eu.nl.syu.hearth.ui.components.WavyVerticalDivider
import org.eu.nl.syu.hearth.ui.viewmodels.ModelSettingsViewModel

import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    var pendingMaxTokens by remember { mutableStateOf(4096) }
    var pendingBackend by remember { mutableStateOf("Automatic") }

    LaunchedEffect(uiState.character, uiState.defaultMaxTokens, uiState.preferredBackend) {
        uiState.character?.let { character ->
            pendingTemp = character.temp
            pendingTopP = character.topP
            pendingTopK = character.topK
            pendingThinking = character.enableThinking
        }
        pendingMaxTokens = uiState.defaultMaxTokens
        pendingBackend = uiState.preferredBackend
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.updateSettings(
                        backend = pendingBackend,
                        maxTokens = pendingMaxTokens,
                        characterId = characterId,
                        temp = pendingTemp,
                        topP = pendingTopP,
                        topK = pendingTopK,
                        enableThinking = pendingThinking
                    )
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text("Apply Settings") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val scrollState = rememberScrollState()
            
            // On larger screens we might want side-by-side, but let's stick to a premium vertical list
            // with the vertical divider used as a decorative separator or in a sub-layout.
            // However, the user said "to separate general model settings from character sopecific settings".
            // I'll try a horizontal arrangement if space allows, but for now I'll use a Column
            // and use WavyHorizontalDivider for the main split, but I'll add a WavyVerticalDivider
            // as requested in a specific way if I can.
            // Actually, I'll use a Row with IntrinsicHeight to use a Vertical Divider between sections.
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Global Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Global Model Settings", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Preferred Hardware", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        
                        val backends = listOf("Automatic", "CPU", "GPU") + 
                            if (uiState.experimentalNpuEnabled) listOf("NPU") else emptyList()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            backends.forEachIndexed { index, backend ->
                                val selected = pendingBackend == backend
                                val icon = when(backend) {
                                    "Automatic" -> Icons.Default.AutoMode
                                    "CPU" -> Icons.Default.Memory
                                    "GPU" -> Icons.Default.DeveloperBoard
                                    "NPU" -> Icons.Default.RocketLaunch
                                    else -> Icons.Default.SettingsSuggest
                                }
                                
                                ToggleButton(
                                    checked = selected,
                                    onCheckedChange = { pendingBackend = backend },
                                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        backends.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            icon, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = backend, 
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max Context Tokens", style = MaterialTheme.typography.labelLarge)
                            Text("$pendingMaxTokens", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Slider(
                            value = pendingMaxTokens.toFloat(),
                            onValueChange = { pendingMaxTokens = it.toInt() },
                            valueRange = 1024f..16384f
                        )
                    }
                }

                if (uiState.character != null) {
                    // Use a decorative vertical divider to separate character settings
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WavyVerticalDivider(
                            modifier = Modifier.width(12.dp).fillMaxHeight(),
                            inset = 8.dp
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Character Settings", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Temperature", style = MaterialTheme.typography.labelLarge)
                                    Text(String.format("%.2f", pendingTemp), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                Slider(
                                    value = pendingTemp,
                                    onValueChange = { pendingTemp = it },
                                    valueRange = 0.0f..2.0f
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Top P", style = MaterialTheme.typography.labelLarge)
                                    Text(String.format("%.2f", pendingTopP), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                Slider(
                                    value = pendingTopP,
                                    onValueChange = { pendingTopP = it },
                                    valueRange = 0.0f..1.0f
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Top K", style = MaterialTheme.typography.labelLarge)
                                    Text("$pendingTopK", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                Slider(
                                    value = pendingTopK.toFloat(),
                                    onValueChange = { pendingTopK = it.toInt() },
                                    valueRange = 1f..100f
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Psychology, 
                                    contentDescription = null,
                                    tint = if (pendingThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Thinking", style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                    Text("Allows the model to reason before answering", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = pendingThinking,
                                    onCheckedChange = { pendingThinking = it }
                                )
                            }
                        }
                    }
                }
                
                // Extra padding for FAB
                Spacer(modifier = Modifier.height(80.dp))
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        org.eu.nl.syu.hearth.ui.components.FadeTextAnimation(text = "Applying changes...")
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
