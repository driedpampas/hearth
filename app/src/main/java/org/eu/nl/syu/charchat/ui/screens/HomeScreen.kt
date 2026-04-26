package org.eu.nl.syu.charchat.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.DefaultCharacters
import org.eu.nl.syu.charchat.ui.viewmodels.HomeViewModel
import org.eu.nl.syu.charchat.ui.viewmodels.HomeUiState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCharacter: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyGridState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showModelPicker by rememberSaveable { mutableStateOf(false) }
    var showCharacterPicker by rememberSaveable { mutableStateOf(false) }
    var showModelSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notification) {
        uiState.notification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    val filteredCharacters = remember(searchQuery, uiState.characters) {
        if (searchQuery.isEmpty()) uiState.characters
        else uiState.characters.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.tagline.contains(searchQuery, ignoreCase = true) 
        }
    }

    val assistantCharacters = remember(uiState.characters) {
        uiState.characters
            .filter { it.id == DefaultCharacters.ASSISTANT_CHARACTER_ID }
            .sortedWith(compareByDescending<Character> { it.lastUsedAt }.thenBy { it.name.lowercase() })
    }

    BackHandler(fabMenuExpanded || showModelPicker || showCharacterPicker || showModelSettings) {
        when {
            showModelSettings -> showModelSettings = false
            showModelPicker -> showModelPicker = false
            showCharacterPicker -> showCharacterPicker = false
            else -> fabMenuExpanded = false
        }
    }

    if (showCharacterPicker) {
        CharacterPickerScreen(
            characters = assistantCharacters,
            onDismiss = { showCharacterPicker = false },
            onCharacterSelected = { character ->
                showCharacterPicker = false
                viewModel.markCharacterOpened(character.id)
                onNavigateToChat(character.id)
            }
        )
        return
    }

    if (showModelPicker) {
        ModelPickerScreen(
            uiState = uiState,
            onDismiss = { showModelPicker = false },
            onSelectModel = { file -> viewModel.selectModel(file) },
            onOpenSettings = { showModelSettings = true },
            showModelSettings = showModelSettings,
            onDismissModelSettings = { showModelSettings = false },
            onSaveBackend = { viewModel.setPreferredBackend(it) },
            onSaveMaxTokens = { viewModel.setDefaultMaxTokens(it) }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButtonMenu(
                    expanded = fabMenuExpanded,
                    button = {
                        ToggleFloatingActionButton(
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = it },
                            modifier = Modifier.semantics {
                                contentDescription = if (fabMenuExpanded) "Close menu" else "Open menu"
                            }
                        ) {
                            val progress by animateFloatAsState(if (fabMenuExpanded) 1f else 0f)
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = progress * 45f
                                }
                            )
                        }
                    }
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            showCharacterPicker = true
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                        text = { Text("New Chat") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            onNavigateToCreateCharacter()
                        },
                        icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                        text = { Text("Create Character") }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Characters",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ElevatedCard(
                    onClick = { showModelPicker = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (uiState.selectedModel != null) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                                contentDescription = "Model Selection",
                                tint = if (uiState.selectedModel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Model", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    uiState.selectedModel ?: "Select a model",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (uiState.selectedModel != null) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                        Icon(Icons.Default.Tune, contentDescription = "Model Settings")
                    }
                }
            }

            if (uiState.isModelLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }

            if (uiState.characters.isEmpty() && searchQuery.isEmpty()) {
                Text(
                    text = "Start a new adventure",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredCharacters) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onNavigateToChat(character.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerScreen(
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
                        Icon(Icons.Default.Tune, contentDescription = "Model Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
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
                        items(uiState.downloadedModels) { file ->
                            ElevatedCard(
                                onClick = { onSelectModel(file) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = if (uiState.selectedModel == file.name) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            if (file.name.contains("embedding", ignoreCase = true)) "Embedding model" else "Chat model",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (uiState.selectedModel == file.name) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterPickerScreen(
    characters: List<Character>,
    onDismiss: () -> Unit,
    onCharacterSelected: (Character) -> Unit
) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("New Chat") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (characters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No assistant character available")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                items(characters) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onCharacterSelected(character) }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.large),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (character.avatarUrl != null) {
                    AsyncImage(
                        model = character.avatarUrl,
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = character.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ModelSettingsDialog(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onSaveBackend: (String) -> Unit,
    onSaveMaxTokens: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Model Settings") },
        text = {
            Column {
                Text("Preferred Hardware", style = MaterialTheme.typography.labelMedium)
                for (backend in listOf("Automatic", "CPU", "GPU", "NPU")) {
                    if (backend == "NPU" && !uiState.experimentalNpuEnabled) continue
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSaveBackend(backend) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.preferredBackend == backend,
                            onClick = { onSaveBackend(backend) }
                        )
                        Text(backend)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Max Context Tokens: ${uiState.defaultMaxTokens}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = uiState.defaultMaxTokens.toFloat(),
                    onValueChange = { onSaveMaxTokens(it.toInt()) },
                    valueRange = 1024f..16384f,
                    steps = 15
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
