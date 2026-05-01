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

import android.content.ClipData
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.eu.nl.syu.hearth.data.AllowedModel
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.MessageRole
import org.eu.nl.syu.hearth.ui.components.MarkdownText
import org.eu.nl.syu.hearth.ui.viewmodels.ChatViewModel
import org.eu.nl.syu.hearth.ui.viewmodels.DeletionMode
import org.eu.nl.syu.hearth.ui.components.GlassySurface
import org.eu.nl.syu.hearth.ui.components.ThinkingProcess
import org.eu.nl.syu.hearth.ui.components.parseThinkingContent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String,
    onNavigateBack: () -> Unit,
    onNavigateToModelSettings: (String) -> Unit,
    onNavigateToEditCharacter: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    modelsViewModel: ModelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modelsUiState by modelsViewModel.uiState.collectAsStateWithLifecycle()
    val statsForNerdsEnabled by modelsViewModel.statsForNerdsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val autoLoadChatModel by modelsViewModel.autoLoadChatModel.collectAsStateWithLifecycle(initialValue = false)
    val modelNamesByFile = remember(modelsUiState.availableModels) {
        buildModelNameIndex(modelsUiState.availableModels)
    }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var threadMenuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newThreadTitle by remember { mutableStateOf("") }

    LaunchedEffect(threadId) {
        viewModel.loadConversation(threadId)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.currentGeneratingText) {
        if (uiState.messages.isNotEmpty() || uiState.currentGeneratingText.isNotEmpty()) {
            scrollState.animateScrollToItem(if (uiState.currentGeneratingText.isNotEmpty()) uiState.messages.size else uiState.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Immersive Background
        if (uiState.character?.sceneBackgroundUrl != null) {
            AsyncImage(
                model = uiState.character?.sceneBackgroundUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(8.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            )
        } else {
            // Default gradient background for immersion
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
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(uiState.threadTitle ?: uiState.character?.name ?: threadId, style = MaterialTheme.typography.titleMedium)
                            TokenIndicator(uiState.tokenCount, uiState.maxTokens, uiState.modelError, uiState.isRawModel, uiState.fallbackReason)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { threadMenuExpanded = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Thread settings")
                            }
                            
                            DropdownMenu(
                                expanded = threadMenuExpanded,
                                onDismissRequest = { threadMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Thread") },
                                    onClick = {
                                        threadMenuExpanded = false
                                        newThreadTitle = uiState.threadTitle ?: ""
                                        showRenameDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Character Editor") },
                                    onClick = {
                                        threadMenuExpanded = false
                                        onNavigateToEditCharacter(uiState.character?.id ?: "")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Model: ${resolveModelDisplayName(uiState.character?.modelReference, modelNamesByFile) ?: "Default"}") },
                                    onClick = {
                                        threadMenuExpanded = false
                                        onNavigateToModelSettings(uiState.character?.id ?: "")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                    enabled = uiState.modelError == null
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                ChatInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onStop = { viewModel.stopGeneration() },
                    isGenerating = uiState.isGenerating,
                    enabled = !uiState.isLoadingModel && uiState.modelError == null && !uiState.isRawModel
                )
            }
        ) { paddingValues ->
            LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        val isRegenerating = uiState.regeneratingMessageId != null && 
                                           (message.versionGroupId ?: message.id) == uiState.regeneratingMessageId
                        
                        val displayMessage = if (isRegenerating) {
                            message.copy(content = uiState.currentGeneratingText)
                        } else {
                            message
                        }

                        ChatBubble(
                            message = displayMessage,
                            statsForNerdsEnabled = statsForNerdsEnabled,
                            modelDisplayName = resolveModelDisplayName(displayMessage.modelReference, modelNamesByFile),
                            isGenerating = isRegenerating,
                            versionCount = uiState.versionCounts[displayMessage.versionGroupId ?: displayMessage.id] ?: 1,
                            onRegenerate = { viewModel.regenerateMessage(displayMessage.id) },
                            onEdit = { viewModel.editMessage(displayMessage.id, it) },
                            onDelete = { viewModel.deleteMessage(displayMessage.id, it) },
                            onFork = { viewModel.forkThread(displayMessage.id) },
                            onVersionChange = { direction -> viewModel.switchMessageVersion(displayMessage.versionGroupId ?: displayMessage.id, direction) },
                            isChatEnabled = !uiState.isRawModel && uiState.modelError == null
                        )
                    }

                    if (uiState.currentGeneratingText.isNotEmpty() && uiState.regeneratingMessageId == null) {
                        item {
                            ChatBubble(
                                message = ChatMessage(
                                    role = MessageRole.MODEL,
                                    content = uiState.currentGeneratingText
                                ),
                                statsForNerdsEnabled = statsForNerdsEnabled,
                                modelDisplayName = null,
                                isChatEnabled = !uiState.isRawModel && uiState.modelError == null
                            )
                        }
                    }

                    if (uiState.isGenerating && uiState.currentGeneratingText.isEmpty() && uiState.regeneratingMessageId == null) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Thread") },
            text = {
                OutlinedTextField(
                    value = newThreadTitle,
                    onValueChange = { newThreadTitle = it },
                    label = { Text("New Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameThread(newThreadTitle)
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TokenIndicator(current: Int, max: Int, modelError: String?, isRawModel: Boolean = false, fallbackReason: String? = null) {
    if (modelError != null || isRawModel) {
        val message = when {
            modelError == "Model not loaded." -> "Model not loaded"
            isRawModel -> "Raw Mode (No Chat)"
            else -> "Model error"
        }
        val color = if (isRawModel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isRawModel) Icons.Default.Tune else Icons.Default.Tune, 
                    contentDescription = null, 
                    modifier = Modifier.size(12.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontSize = 10.sp
                )
            }
            if (isRawModel && fallbackReason != null) {
                Text(
                    fallbackReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    } else {
        val progress = (current.toFloat() / max).coerceIn(0f, 1f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(60.dp).height(4.dp).clip(CircleShape),
                color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("${(progress * 100).toInt()}% Context", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    enabled: Boolean = true
) {
    GlassySurface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.ime),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (enabled) "Speak with your character..." else if (isGenerating) "AI is thinking..." else "Model not available") },
                enabled = enabled && !isGenerating,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = if (isGenerating) onStop else onSend,
                enabled = enabled && (isGenerating || value.isNotBlank()),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (isGenerating) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isGenerating) "Stop" else "Send"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage, 
    statsForNerdsEnabled: Boolean, 
    modelDisplayName: String?,
    isGenerating: Boolean = false,
    versionCount: Int = 1,
    onRegenerate: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onDelete: (DeletionMode) -> Unit = {},
    onFork: () -> Unit = {},
    onVersionChange: (Int) -> Unit = {},
    isChatEnabled: Boolean = true
) {
    val isUser = message.role == MessageRole.USER
    val (thought, mainContent, isThoughtComplete) = remember(message.content) {
        if (isUser) Triple(null, message.content, true) else parseThinkingContent(message.content)
    }
    
    var menuExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 20.dp
        )
        
        val clipboardManager = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current

        GlassySurface(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { /* No-op or single tap action if needed */ },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    }
                ),
            shape = bubbleShape,
            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Box {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (thought != null) {
                        ThinkingProcess(
                            thought = thought,
                            isComplete = isThoughtComplete
                        )
                    }
                    if (isEditing) {
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                unfocusedTextColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { isEditing = false }) { Text("Cancel", color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) }
                            TextButton(onClick = { 
                                onEdit(if (thought != null) "<think>\n$thought\n</think>\n$editValue" else editValue)
                                isEditing = false 
                            }) { Text("Save", color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) }
                        }
                    } else if (mainContent.isNotEmpty()) {
                        MarkdownText(
                            text = mainContent,
                            textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else if (isGenerating && thought == null) {
                        TypingIndicator()
                    }
                    
                    if (versionCount > 1 && !isEditing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp).alpha(0.7f)
                        ) {
                            IconButton(onClick = { onVersionChange(-1) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous version", tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "${message.versionIndex + 1}/$versionCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            IconButton(onClick = { onVersionChange(1) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next version", tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = {
                            menuExpanded = false
                            scope.launch {
                                clipboardManager.setClipEntry(
                                    ClipEntry(
                                        ClipData.newPlainText(
                                            "Copied Message",
                                            message.content
                                        )
                                    )
                                )
                            }
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    if (message.role == MessageRole.MODEL) {
                        DropdownMenuItem(
                            text = { Text("Regenerate") },
                            onClick = {
                                menuExpanded = false
                                onRegenerate()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            enabled = isChatEnabled
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            editValue = mainContent
                            isEditing = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Fork from here") },
                        onClick = {
                            menuExpanded = false
                            onFork()
                        },
                        leadingIcon = { Icon(Icons.Default.ForkRight, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Message") },
                text = { Text("How would you like to delete this message?") },
                confirmButton = {
                    Column {
                        TextButton(
                            onClick = {
                                onDelete(DeletionMode.ONLY_THIS)
                                showDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            Text("Delete only this message", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) 
                        }
                        TextButton(
                            onClick = {
                                onDelete(DeletionMode.EVERYTHING_AFTER)
                                showDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            Text("Delete this and all after", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) 
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
        
        // Stats for Nerds: show below model messages when enabled
        if (!isUser && statsForNerdsEnabled && message.modelReference != null && message.tokensPerSecond != null && message.generationTimeMs != null) {
            Text(
                text = "${modelDisplayName ?: File(message.modelReference).nameWithoutExtension} • ${String.format("%.1f", message.tokensPerSecond)} t/s • ${message.generationTimeMs} ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

private fun buildModelNameIndex(availableModels: List<AllowedModel>): Map<String, String> {
    return buildMap {
        availableModels.forEach { model ->
            put(model.modelFile, model.name)
            model.socToModelFiles?.values?.forEach { socModel ->
                socModel.modelFile?.let { put(it, model.name) }
            }
        }
    }
}

private fun resolveModelDisplayName(modelReference: String?, modelNamesByFile: Map<String, String>): String? {
    if (modelReference.isNullOrBlank()) return null

    val fileName = File(modelReference).name
    return modelNamesByFile[fileName] ?: fileName.substringBeforeLast('.')
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition()
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150)
                )
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset { IntOffset(x = 0, y = yOffset.dp.roundToPx()) }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("AI is thinking...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
    }
}
