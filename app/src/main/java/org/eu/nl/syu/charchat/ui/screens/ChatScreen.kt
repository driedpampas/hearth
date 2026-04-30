package org.eu.nl.syu.charchat.ui.screens

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.eu.nl.syu.charchat.data.AllowedModel
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.MessageRole
import org.eu.nl.syu.charchat.ui.components.MarkdownText
import org.eu.nl.syu.charchat.ui.viewmodels.ChatViewModel
import org.eu.nl.syu.charchat.ui.screens.ModelsViewModel
import org.eu.nl.syu.charchat.ui.components.GlassySurface
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String,
    onNavigateBack: () -> Unit,
    onNavigateToModelSettings: (String) -> Unit,
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
                            TokenIndicator(uiState.tokenCount, uiState.maxTokens)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToModelSettings(uiState.character?.id ?: "") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Model settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                if (uiState.modelError == null && !uiState.isLoadingModel) {
                    ChatInput(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            if (uiState.isLoadingModel && autoLoadChatModel) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading assistant model...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (uiState.modelError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Assistant unavailable", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.modelError ?: "Model unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatBubble(
                            message = message,
                            statsForNerdsEnabled = statsForNerdsEnabled,
                            modelDisplayName = resolveModelDisplayName(message.modelReference, modelNamesByFile)
                        )
                    }

                    if (uiState.currentGeneratingText.isNotEmpty()) {
                        item {
                            ChatBubble(
                                message = ChatMessage(
                                    role = MessageRole.MODEL,
                                    content = uiState.currentGeneratingText
                                ),
                                statsForNerdsEnabled = statsForNerdsEnabled,
                                modelDisplayName = null
                            )
                        }
                    }

                    if (uiState.isGenerating && uiState.currentGeneratingText.isEmpty()) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokenIndicator(current: Int, max: Int) {
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

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
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
                placeholder = { Text("Speak with your character...") },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatMessage, statsForNerdsEnabled: Boolean, modelDisplayName: String?) {
    val isUser = message.role == MessageRole.USER
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
        
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current

        GlassySurface(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { /* No-op or single tap action if needed */ },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    }
                ),
            shape = bubbleShape,
            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            MarkdownText(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
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
                    .offset(y = yOffset.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("AI is thinking...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
    }
}
