package org.eu.nl.syu.charchat.ui.screens

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
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
import org.eu.nl.syu.charchat.data.ChatThread
import org.eu.nl.syu.charchat.data.DefaultCharacters
import org.eu.nl.syu.charchat.ui.components.GlassySurface
import org.eu.nl.syu.charchat.ui.viewmodels.HomeViewModel
import androidx.compose.foundation.lazy.grid.items as gridItems

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCharacter: () -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToModelPicker: () -> Unit,
    onNavigateToCharacterPicker: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notification) {
        uiState.notification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    val visibleCharacters = remember(searchQuery, uiState.characters) {
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

    val filteredThreads = remember(searchQuery, uiState.chatThreads, visibleCharacters) {
        val characterIds = visibleCharacters.map { it.id }.toSet()
        uiState.chatThreads.filter { it.characterId in characterIds }
    }

    val recentCharacters = remember(uiState.characters, searchQuery) {
        if (searchQuery.isNotEmpty()) emptyList()
        else uiState.characters
            .filter { it.lastUsedAt > 0 }
            .sortedByDescending { it.lastUsedAt }
            .take(4)
    }

    val openCharacter: (Character) -> Unit = { character ->
        viewModel.markCharacterOpened(character.id)
        onNavigateToChat(character.id)
    }

    val openThread: (ChatThread) -> Unit = { thread ->
        onNavigateToChat(thread.id)
    }

    BackHandler(fabMenuExpanded) {
        fabMenuExpanded = false
    }



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
                            onNavigateToCharacterPicker()
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
            SearchBar(
                inputField = {
                    androidx.compose.material3.SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { searchActive = false },
                        expanded = searchActive,
                        onExpandedChange = { searchActive = it },
                        placeholder = { Text("Search characters") },
                        leadingIcon = {
                            if (!searchActive) {
                                IconButton(onClick = {
                                    if (uiState.isModelLoaded) {
                                        onNavigateToModelSettings()
                                    } else {
                                        onNavigateToModelPicker()
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (uiState.isModelLoaded) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                                        contentDescription = if (uiState.isModelLoaded) "Model Settings" else "Download Models",
                                        tint = if (uiState.isModelLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            if (searchActive) {
                                IconButton(onClick = {
                                    if (searchQuery.isNotEmpty()) searchQuery = "" else searchActive = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            } else {
                                IconButton(onClick = onNavigateToSettings) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                }
                            }
                        }
                    )
                },
                expanded = searchActive,
                onExpandedChange = { searchActive = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = 0.dp)
            ) { }

            if (!searchActive) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (visibleCharacters.isEmpty()) {
                Text(
                    text = if (searchQuery.isEmpty()) "Start a new adventure" else "No characters match your search",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Show Recent Characters only if there are existing threads to justify a shortcut section
                if (filteredThreads.isNotEmpty() && recentCharacters.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Characters",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(recentCharacters, key = { "recent-${it.id}" }) { character ->
                        CharacterCard(
                            character = character,
                            onClick = { openCharacter(character) }
                        )
                    }
                }

                if (filteredThreads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Chats",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = if (recentCharacters.isNotEmpty()) 4.dp else 0.dp, bottom = 4.dp)
                        )
                    }

                    items(filteredThreads, key = { "thread-${it.id}" }) { thread ->
                        ThreadCard(
                            thread = thread,
                            character = uiState.characters.firstOrNull { it.id == thread.characterId },
                            onClick = { openThread(thread) }
                        )
                    }
                }

                if (visibleCharacters.isNotEmpty()) {
                    item {
                        Text(
                            text = if (filteredThreads.isEmpty()) "Characters" else "Start a new chat",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = if (filteredThreads.isNotEmpty() || (filteredThreads.isNotEmpty() && recentCharacters.isNotEmpty())) 4.dp else 0.dp, bottom = 4.dp)
                        )
                    }

                    items(visibleCharacters, key = { "visible-${it.id}" }) { character ->
                        CharacterCard(
                            character = character,
                            onClick = { openCharacter(character) }
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ThreadCard(
    thread: ChatThread,
    character: Character?,
    onClick: () -> Unit
) {
    GlassySurface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = thread.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(thread.lastMessageAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (thread.sequenceId > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = thread.sequenceId.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterPickerScreen(
    onDismiss: () -> Unit,
    onCharacterSelected: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val characters = remember(uiState.characters) {
        uiState.characters
            .filter { it.id == DefaultCharacters.ASSISTANT_CHARACTER_ID }
            .sortedWith(compareByDescending<Character> { it.lastUsedAt }.thenBy { it.name.lowercase() })
    }

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
                    title = { Text("New Chat") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    gridItems(characters) { character ->
                        CharacterCard(
                            character = character,
                            onClick = { onCharacterSelected(character.id) }
                        )
                    }
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
    GlassySurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (character.sceneBackgroundUrl != null) {
                AsyncImage(
                    model = character.sceneBackgroundUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().blur(8.dp).alpha(0.3f)
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassySurface(
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.large
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
}
