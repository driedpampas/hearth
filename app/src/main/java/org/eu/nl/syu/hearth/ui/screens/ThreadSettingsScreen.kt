/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.eu.nl.syu.hearth.ui.components.*
import org.eu.nl.syu.hearth.ui.viewmodels.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadSettingsScreen(
    threadId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditCharacter: (String) -> Unit,
    onNavigateToModelSettings: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    
    var threadTitle by remember(uiState.threadTitle) { mutableStateOf(uiState.threadTitle ?: "") }
    var threadLore by remember(uiState.threadLore) { mutableStateOf(uiState.threadLore ?: "") }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(threadId) {
        viewModel.loadConversation(threadId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassySurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    blurRadius = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = { Text("Thread Settings", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                viewModel.renameThread(threadTitle)
                                viewModel.updateThreadLore(threadLore)
                                onNavigateBack()
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Identity Header (Hero Style)
                GlassySurface(
                    modifier = Modifier.fillMaxWidth(),
                    blurRadius = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Title Input
                        BasicTextField(
                            value = threadTitle,
                            onValueChange = { threadTitle = it },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (threadTitle.isEmpty()) {
                                            Text(
                                                "Thread Title",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        innerTextField()
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(2.dp)
                                            .background(
                                                if (threadTitle.isEmpty()) MaterialTheme.colorScheme.outlineVariant 
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CONVERSATION INSTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Navigation Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassySurface(
                        onClick = { onNavigateToEditCharacter(uiState.character?.id ?: "") },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            Text("Edit Character", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    GlassySurface(
                        onClick = { onNavigateToModelSettings(uiState.character?.id ?: "") },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(8.dp))
                            Text("Model Config", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // 3. Thread Lore Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "THREAD LORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = threadLore,
                        onValueChange = { threadLore = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Facts specific to this thread...") },
                        minLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f),
                            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // 4. Danger Zone / Reset
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showResetDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Character Defaults")
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        if (uiState.isEmbedding) {
            LoreSyncOverlay()
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Thread Settings") },
                text = { Text("This will revert the title and lore to match the character's base values. Existing messages will remain.") },
                confirmButton = {
                    TextButton(onClick = {
                        threadTitle = uiState.character?.name ?: ""
                        threadLore = ""
                        showResetDialog = false
                    }) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
