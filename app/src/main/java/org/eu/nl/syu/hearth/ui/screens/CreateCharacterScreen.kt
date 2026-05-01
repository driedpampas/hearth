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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
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
import org.eu.nl.syu.hearth.data.ModelManager
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.local.CharacterDao
import org.eu.nl.syu.hearth.data.local.toEntity
import org.eu.nl.syu.hearth.data.local.toDomain
import org.eu.nl.syu.hearth.domain.ScraperUseCase
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class CreateCharacterState(
    val name: String = "",
    val tagline: String = "",
    val lore: String = "",
    val reminderMessage: String = "",
    val modelPath: String = "",
    val temp: Float = 0.8f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val enableThinking: Boolean = false,
    val availableModels: List<File> = emptyList(),
    val initialMessages: List<String> = listOf("Greetings! I am ready for our story."),
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
    private val modelManager: ModelManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateCharacterState())
    val uiState: StateFlow<CreateCharacterState> = _uiState.asStateFlow()

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
                    lore = character.systemPromptLore,
                    reminderMessage = character.reminderMessage,
                    modelPath = character.modelReference,
                    temp = character.temp,
                    topP = character.topP,
                    topK = character.topK,
                    enableThinking = character.enableThinking
                )
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateTagline(tagline: String) = _uiState.update { it.copy(tagline = tagline) }
    fun updateLore(lore: String) = _uiState.update { it.copy(lore = lore) }
    fun updateReminder(reminder: String) = _uiState.update { it.copy(reminderMessage = reminder) }
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
        it.copy(initialMessages = it.initialMessages + "") 
    }
    
    fun updateInitialMessage(index: Int, text: String) = _uiState.update { state ->
        val newList = state.initialMessages.toMutableList()
        if (index in newList.indices) {
            newList[index] = text
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
                        lore = scraped["systemPromptLore"] ?: it.lore
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
                systemPromptLore = state.lore,
                reminderMessage = state.reminderMessage,
                modelReference = state.modelPath,
                temp = state.temp,
                topP = state.topP,
                topK = state.topK,
                enableThinking = state.enableThinking,
                sceneBackgroundUrl = null,
                isPredefined = false
            )
            characterDao.insertCharacter(character.toEntity())
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.characterId != null) "Edit Character" else "Character Creator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showModelSettings = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Model settings")
                    }
                    IconButton(onClick = { viewModel.saveCharacter(onNavigateBack) }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Scraping Feature
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Generate from URL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.urlToScrape,
                        onValueChange = viewModel::updateUrl,
                        placeholder = { Text("Fandom wiki, bio, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (uiState.isScraping) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = viewModel::generateFromUrl) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Generate")
                                }
                            }
                        }
                    )
                    Text(
                        "Uses LLM to parse character lore automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.tagline,
                    onValueChange = viewModel::updateTagline,
                    label = { Text("Tagline") },
                    placeholder = { Text("Short, punchy description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.lore,
                    onValueChange = viewModel::updateLore,
                    label = { Text("Roleplay Instructions / Lore") },
                    placeholder = { Text("Define personality, background, and knowledge...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    minLines = 4
                )

                // Advanced Options (Progressive Disclosure)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAdvanced() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Advanced Options",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (uiState.isAdvancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = uiState.isAdvancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.reminderMessage,
                            onValueChange = viewModel::updateReminder,
                            label = { Text("Reminder Message") },
                            placeholder = { Text("Injected late in context window...") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("Helps prevent personality drift.") }
                        )

                        ElevatedCard(
                            onClick = { showModelPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Model Selection", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.modelPath.takeIf { it.isNotBlank() }?.let { File(it).name }
                                        ?: if (uiState.availableModels.isEmpty()) "No local models downloaded" else "Select a model",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.availableModels.isEmpty()) {
                            Text(
                                text = "Download a model in Settings first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Scenario Editor
                        Text("Scenario Editor (Greeting Messages)", style = MaterialTheme.typography.titleSmall)
                        uiState.initialMessages.forEachIndexed { index, message ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = message,
                                    onValueChange = { viewModel.updateInitialMessage(index, it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Message ${index + 1}") }
                                )
                                IconButton(onClick = { viewModel.removeInitialMessage(index) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                        TextButton(
                            onClick = viewModel::addInitialMessage,
                            enabled = uiState.initialMessages.size < 3
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Scenario Message")
                        }
                    }
                }

                Button(
                    onClick = { viewModel.saveCharacter(onNavigateBack) },
                    enabled = uiState.modelPath.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(if (uiState.characterId != null) "Save Changes" else "Create Character")
                }
            }
        }
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
                title = { Text("Select Model") },
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
                Text("No local models downloaded. Download one in Settings.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(availableModels) { model ->
                    ElevatedCard(
                        onClick = { onSelectModel(model) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.name, fontWeight = FontWeight.Medium)
                                Text(
                                    if (model.name.contains("embedding", ignoreCase = true)) "Embedding model" else "Chat model",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (model.absolutePath == selectedModelPath) {
                                Icon(Icons.Filled.Save, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
