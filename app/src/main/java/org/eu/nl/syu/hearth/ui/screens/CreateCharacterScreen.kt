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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ModelManager
import org.eu.nl.syu.hearth.data.local.CharacterDao
import org.eu.nl.syu.hearth.data.local.toDomain
import org.eu.nl.syu.hearth.data.local.toEntity
import org.eu.nl.syu.hearth.domain.ScraperUseCase
import org.eu.nl.syu.hearth.runtime.LoreSyncManager
import org.eu.nl.syu.hearth.ui.components.*
import java.io.File
import java.util.UUID
import javax.inject.Inject
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.MessageRole

data class StarterMessage(
    val content: String = "",
    val author: MessageRole = MessageRole.MODEL,
    val isHiddenFromUser: Boolean = false,
    val isHiddenFromAi: Boolean = false
)

data class CreateCharacterState(
    val name: String = "",
    val tagline: String = "",
    val lore: String = "",
    val reminderMessage: String = "",
    val knowledgeBase: String = "",
    val modelPath: String = "",
    val temp: Float = 0.8f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val enableThinking: Boolean = false,
    val availableModels: List<File> = emptyList(),
    val initialMessages: List<StarterMessage> = listOf(StarterMessage("Greetings! I am ready for our story.")),
    val isAdvancedExpanded: Boolean = false,
    val urlToScrape: String = "",
    val isScraping: Boolean = false,
    val isSaved: Boolean = false,
    val characterId: String? = null
)

@HiltViewModel
class CreateCharacterViewModel @Inject constructor(
    private val scraperUseCase: ScraperUseCase,
    private val characterDao: CharacterDao,
    private val modelManager: ModelManager,
    private val loreSyncManager: LoreSyncManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateCharacterState())
    val uiState: StateFlow<CreateCharacterState> = _uiState.asStateFlow()
    val syncState = loreSyncManager.syncState

    init {
        refreshModels()
    }

    fun loadCharacter(characterId: String) {
        viewModelScope.launch {
            val character = characterDao.getCharacterById(characterId)?.toDomain() ?: return@launch
            _uiState.update {
                it.copy(
                    characterId = characterId,
                    name = character.name,
                    tagline = character.tagline,
                    lore = character.roleInstruction,
                    reminderMessage = character.reminderMessage,
                    modelPath = character.modelReference,
                    temp = character.temp,
                    topP = character.topP,
                    topK = character.topK,
                    enableThinking = character.enableThinking,
                    knowledgeBase = character.knowledgeBase,
                    initialMessages = character.initialMessages.map { msg ->
                        StarterMessage(
                            content = msg.content,
                            author = msg.role,
                            isHiddenFromUser = msg.isHiddenFromUser,
                            isHiddenFromAi = msg.isHiddenFromAi
                        )
                    }
                )
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateTagline(tagline: String) = _uiState.update { it.copy(tagline = tagline) }
    fun updateLore(lore: String) = _uiState.update { it.copy(lore = lore) }
    fun updateReminder(reminder: String) = _uiState.update { it.copy(reminderMessage = reminder) }
    fun updateKnowledgeBase(kb: String) = _uiState.update { it.copy(knowledgeBase = kb) }
    fun updateModel(model: String) = _uiState.update { it.copy(modelPath = model) }
    fun toggleAdvanced() = _uiState.update { it.copy(isAdvancedExpanded = !it.isAdvancedExpanded) }
    fun updateUrl(url: String) = _uiState.update { it.copy(urlToScrape = url) }
    fun updateTemp(temp: Float) = _uiState.update { it.copy(temp = temp) }
    fun updateTopP(topP: Float) = _uiState.update { it.copy(topP = topP) }
    fun updateTopK(topK: Int) = _uiState.update { it.copy(topK = topK) }
    fun updateEnableThinking(enableThinking: Boolean) = _uiState.update { it.copy(enableThinking = enableThinking) }

    fun refreshModels() {
        val models = modelManager.getLocalModels()
            .filter { it.name.endsWith(".litertlm") }
            .sortedWith(
                compareBy<File> { it.name.contains("embedding", ignoreCase = true) }
                    .thenBy { it.name.lowercase() }
            )

        val currentPath = _uiState.value.modelPath
        val nextPath = when {
            currentPath.isNotBlank() && models.any { it.absolutePath == currentPath } -> currentPath
            models.isNotEmpty() -> models.first().absolutePath
            else -> ""
        }

        _uiState.update { it.copy(availableModels = models, modelPath = nextPath) }
    }

    fun addInitialMessage() = _uiState.update { 
        it.copy(initialMessages = it.initialMessages + StarterMessage()) 
    }
    
    fun updateInitialMessage(index: Int, text: String) = _uiState.update { state ->
        val newList = state.initialMessages.toMutableList()
        if (index in newList.indices) {
            newList[index] = newList[index].copy(content = text)
        }
        state.copy(initialMessages = newList)
    }

    fun updateInitialMessageAuthor(index: Int, author: MessageRole) = _uiState.update { state ->
        val newList = state.initialMessages.toMutableList()
        if (index in newList.indices) {
            newList[index] = newList[index].copy(author = author)
        }
        state.copy(initialMessages = newList)
    }

    fun toggleInitialMessageUserVisibility(index: Int) = _uiState.update { state ->
        val newList = state.initialMessages.toMutableList()
        if (index in newList.indices) {
            newList[index] = newList[index].copy(isHiddenFromUser = !newList[index].isHiddenFromUser)
        }
        state.copy(initialMessages = newList)
    }

    fun toggleInitialMessageAiVisibility(index: Int) = _uiState.update { state ->
        val newList = state.initialMessages.toMutableList()
        if (index in newList.indices) {
            newList[index] = newList[index].copy(isHiddenFromAi = !newList[index].isHiddenFromAi)
        }
        state.copy(initialMessages = newList)
    }

    fun removeInitialMessage(index: Int) = _uiState.update { state ->
        val newList = state.initialMessages.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
        }
        state.copy(initialMessages = newList)
    }

    fun generateFromUrl() {
        if (_uiState.value.urlToScrape.isBlank()) return
        
        _uiState.update { it.copy(isScraping = true) }
        viewModelScope.launch {
            val scraped = scraperUseCase.scrapeCharacterFromUrl(_uiState.value.urlToScrape)
            if (scraped != null) {
                _uiState.update { 
                    it.copy(
                        isScraping = false,
                        name = scraped["name"] ?: it.name,
                        tagline = scraped["tagline"] ?: it.tagline,
                        knowledgeBase = scraped["roleInstruction"] ?: it.knowledgeBase
                    )
                }
            } else {
                _uiState.update { it.copy(isScraping = false) }
            }
        }
    }

    fun saveCharacter(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.modelPath.isBlank()) return@launch
            val characterId = state.characterId ?: UUID.randomUUID().toString()
            val character = Character(
                id = characterId,
                name = state.name,
                tagline = state.tagline,
                avatarUrl = null,
                roleInstruction = state.lore,
                reminderMessage = state.reminderMessage,
                modelReference = state.modelPath,
                temp = state.temp,
                topP = state.topP,
                topK = state.topK,
                enableThinking = state.enableThinking,
                knowledgeBase = state.knowledgeBase,
                sceneBackgroundUrl = null,
                isPredefined = false,
                initialMessages = state.initialMessages.map { sm ->
                    ChatMessage(
                        role = sm.author,
                        content = sm.content,
                        isHiddenFromUser = sm.isHiddenFromUser,
                        isHiddenFromAi = sm.isHiddenFromAi
                    )
                }
            )
            characterDao.insertCharacter(character.toEntity())
            
            // Sync BOTH core identity and deep lore to RAG layer
            val combinedLore = listOf(state.lore, state.knowledgeBase)
                .filter { it.isNotBlank() }
                .joinToString("\n\n---\n\n")
            
            loreSyncManager.syncLore(character, combinedLore)
            onSuccess()
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterScreen(
    characterId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateCharacterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    
    LaunchedEffect(characterId) {
        if (characterId != null) {
            viewModel.loadCharacter(characterId)
        }
    }
    var showModelPicker by rememberSaveable { mutableStateOf(false) }
    var showModelSettings by rememberSaveable { mutableStateOf(false) }

    if (showModelPicker) {
        CreateCharacterModelPickerScreen(
            availableModels = uiState.availableModels,
            selectedModelPath = uiState.modelPath,
            onDismiss = { showModelPicker = false },
            onSelectModel = { model ->
                viewModel.updateModel(model.absolutePath)
                showModelPicker = false
            }
        )
        return
    }

    if (showModelSettings) {
        AlertDialog(
            onDismissRequest = { showModelSettings = false },
            title = { Text("Model Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Sampling")
                    Text("Temperature: ${uiState.temp}")
                    Slider(value = uiState.temp, onValueChange = viewModel::updateTemp, valueRange = 0.0f..2.0f)
                    Text("Top P: ${uiState.topP}")
                    Slider(value = uiState.topP, onValueChange = viewModel::updateTopP, valueRange = 0.0f..1.0f)
                    Text("Top K: ${uiState.topK}")
                    Slider(value = uiState.topK.toFloat(), onValueChange = { viewModel.updateTopK(it.toInt()) }, valueRange = 1f..100f, steps = 98)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = uiState.enableThinking, onClick = { viewModel.updateEnableThinking(!uiState.enableThinking) })
                        Text("Enable thinking")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModelSettings = false }) { Text("Done") } }
        )
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = { Text(if (uiState.characterId != null) "Edit Character" else "Character Creator", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.saveCharacter(onNavigateBack) }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
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
            // 1. Identity Section (Hero)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassySurface(
                    modifier = Modifier.fillMaxWidth(),
                    blurRadius = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    //border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Avatar Placeholder with Camera Overlay
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                    .clickable { /* Trigger image picker */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            // Camera Icon Badge
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Edit Image",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Name Input with better affordance
                        BasicTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
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
                                        if (uiState.name.isEmpty()) {
                                            Text(
                                                "Character Name",
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
                                                if (uiState.name.isEmpty()) MaterialTheme.colorScheme.outlineVariant 
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Tagline Input with better affordance
                        BasicTextField(
                            value = uiState.tagline,
                            onValueChange = viewModel::updateTagline,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (uiState.tagline.isEmpty()) {
                                            Text(
                                                "Tagline (Short description)",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        innerTextField()
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(1.dp)
                                            .background(
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 2. The Brain (Instructions vs Reminder)
            org.eu.nl.syu.hearth.ui.components.GlassySurface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CORE IDENTITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.lore,
                        onValueChange = viewModel::updateLore,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("System Instructions (~500 words)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        minLines = 6
                    )
                    
                    WavyHorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(vertical = 8.dp),
                        waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    
                    Text(
                        "BEHAVIORAL NUDGE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.reminderMessage,
                        onValueChange = viewModel::updateReminder,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reminder Message (~100 words)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        minLines = 3
                    )
                }
            }

            // 3. Knowledge Base (Lore & Scraper)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "KNOWLEDGE BASE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
            GlassySurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.urlToScrape,
                                onValueChange = viewModel::updateUrl,
                                placeholder = { Text("Scrape from URL (Wiki, Bio...)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            IconButton(
                                onClick = viewModel::generateFromUrl,
                                enabled = !uiState.isScraping && uiState.urlToScrape.isNotBlank(),
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.medium
                                )
                            ) {
                                if (uiState.isScraping) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Scrape", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                        
                        if (uiState.isScraping) {
                            FadeTextAnimation(
                                text = "Extracting knowledge...",
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = uiState.knowledgeBase,
                            onValueChange = viewModel::updateKnowledgeBase,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Extended lore for RAG retrieval...") },
                            minLines = 8,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            // 4. Creative: Initial Messages (Full Width)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "GREETINGS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                uiState.initialMessages.forEachIndexed { index, starterMessage ->
                    val isHiddenFromAi = starterMessage.isHiddenFromAi
                    val isHiddenFromUser = starterMessage.isHiddenFromUser
                    
                    GlassySurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isHiddenFromUser) 0.6f else 1f),
                        color = when (starterMessage.author) {
                            MessageRole.USER -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            MessageRole.MODEL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            MessageRole.SYSTEM -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        },
                        shape = MaterialTheme.shapes.medium,
                        border = if (isHiddenFromAi) 
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) 
                            else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header: Author Toggle and Visibility Icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Author Pill Selector
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MessageRole.values().forEach { role ->
                                        val isSelected = starterMessage.author == role
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { viewModel.updateInitialMessageAuthor(index, role) }
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = role.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                // Visibility Controls
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { viewModel.toggleInitialMessageUserVisibility(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Hidden from User",
                                            modifier = Modifier.size(16.dp).alpha(if (isHiddenFromUser) 0.3f else 1f),
                                            tint = if (isHiddenFromUser) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleInitialMessageAiVisibility(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = "Hidden from AI",
                                            modifier = Modifier.size(16.dp).alpha(if (isHiddenFromAi) 0.3f else 1f),
                                            tint = if (isHiddenFromAi) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeInitialMessage(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(16.dp), 
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            BasicTextField(
                                value = starterMessage.content,
                                onValueChange = { viewModel.updateInitialMessage(index, it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = if (starterMessage.author == MessageRole.SYSTEM) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                ),
                                decorationBox = { inner ->
                                    if (starterMessage.content.isEmpty()) {
                                        Text(
                                            "Message content...", 
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }
                if (uiState.initialMessages.size < 5) {
                    TextButton(
                        onClick = viewModel::addInitialMessage,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Greeting", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // 5. Model & Technical Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Memory, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MODEL & SAMPLING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                GlassySurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        // Model Selection
                        GlassySurface(
                            onClick = { showModelPicker = true },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AI MODEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = uiState.modelPath.takeIf { it.isNotBlank() }?.let { java.io.File(it).name } ?: "Select Model",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                            }
                        }

                        // Sampling Sliders
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                TechnicalSlider(
                                    label = "Temperature",
                                    value = uiState.temp,
                                    onValueChange = viewModel::updateTemp,
                                    range = 0f..2f
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                TechnicalSlider(
                                    label = "Top-P",
                                    value = uiState.topP,
                                    onValueChange = viewModel::updateTopP,
                                    range = 0f..1f
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Reasoning (Thinking)", style = MaterialTheme.typography.bodyMedium)
                                Text("Enables internal chain-of-thought", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.enableThinking,
                                onCheckedChange = viewModel::updateEnableThinking,
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (syncState is LoreSyncManager.SyncState.Syncing) {
            LoreSyncOverlay()
        }
    }
}
}

@Composable
private fun TechnicalSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCharacterModelPickerScreen(
    availableModels: List<File>,
    selectedModelPath: String,
    onDismiss: () -> Unit,
    onSelectModel: (File) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Character Model", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (availableModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(64.dp).alpha(0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No local models found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(availableModels) { model ->
                    val isSelected = model.absolutePath == selectedModelPath
                    val isEmbedding = model.name.contains("embedding", ignoreCase = true)
                    
                    GlassySurface(
                        onClick = { onSelectModel(model) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        blurRadius = if (isSelected) 12.dp else 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEmbedding) MaterialTheme.colorScheme.secondaryContainer 
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isEmbedding) Icons.Default.Dataset else Icons.Default.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isEmbedding) MaterialTheme.colorScheme.onSecondaryContainer 
                                           else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isEmbedding) "Vector Embedding Model" else "LLM Chat Model",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoreSyncOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "loreSync")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Recalculating embeddings...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
