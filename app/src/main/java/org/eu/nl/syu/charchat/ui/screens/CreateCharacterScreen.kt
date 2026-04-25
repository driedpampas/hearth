package org.eu.nl.syu.charchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.toEntity
import org.eu.nl.syu.charchat.domain.ScraperUseCase
import org.eu.nl.syu.charchat.data.Character
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID

data class CreateCharacterState(
    val name: String = "",
    val tagline: String = "",
    val lore: String = "",
    val reminderMessage: String = "",
    val modelPath: String = "gemma-2b.litertlm",
    val initialMessages: List<String> = listOf("Greetings! I am ready for our story."),
    val isAdvancedExpanded: Boolean = false,
    val urlToScrape: String = "",
    val isScraping: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class CreateCharacterViewModel @Inject constructor(
    private val scraperUseCase: ScraperUseCase,
    private val characterDao: CharacterDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateCharacterState())
    val uiState: StateFlow<CreateCharacterState> = _uiState.asStateFlow()

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateTagline(tagline: String) = _uiState.update { it.copy(tagline = tagline) }
    fun updateLore(lore: String) = _uiState.update { it.copy(lore = lore) }
    fun updateReminder(reminder: String) = _uiState.update { it.copy(reminderMessage = reminder) }
    fun updateModel(model: String) = _uiState.update { it.copy(modelPath = model) }
    fun toggleAdvanced() = _uiState.update { it.copy(isAdvancedExpanded = !it.isAdvancedExpanded) }
    fun updateUrl(url: String) = _uiState.update { it.copy(urlToScrape = url) }

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
            val character = Character(
                id = UUID.randomUUID().toString(),
                name = state.name,
                tagline = state.tagline,
                avatarUrl = null,
                systemPromptLore = state.lore,
                reminderMessage = state.reminderMessage,
                modelReference = state.modelPath,
                temp = 0.7f,
                topP = 0.9f,
                topK = 40,
                sceneBackgroundUrl = null,
                isPredefined = false
            )
            characterDao.insertCharacter(character.toEntity())
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateCharacterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Character Creator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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

                        // Model Selection (Mock)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.modelPath,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Model Selection") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("Llama-3-8B-Instruct.gguf", "Mistral-7B-v0.3.litert", "Gemma-2b-it.gguf").forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            viewModel.updateModel(model)
                                            expanded = false
                                        }
                                    )
                                }
                            }
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
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text("Create Character")
                }
            }
        }
    }
}
