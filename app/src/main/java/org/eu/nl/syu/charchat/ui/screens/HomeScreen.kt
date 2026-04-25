package org.eu.nl.syu.charchat.ui.screens

import android.annotation.SuppressLint
import androidx.compose.ui.semantics.contentDescription
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.charchat.data.Character

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCharacter: () -> Unit,
    predefinedCharacters: List<Character> = emptyList(),
    activeChats: List<Character> = emptyList()
) {
    val scrollState = rememberLazyGridState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // FAB visibility based on scroll
    val isFabVisible by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex == 0 || scrollState.firstVisibleItemScrollOffset <= 0
        }
    }

    BackHandler(fabMenuExpanded) {
        fabMenuExpanded = false
    }

    Scaffold(
        floatingActionButton = {
            if (isFabVisible || fabMenuExpanded) {
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
                                // Logic to select character for new chat
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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { searchActive = false },
                        expanded = searchActive,
                        onExpandedChange = { searchActive = it },
                        placeholder = { Text("Search characters") },
                        leadingIcon = {
                            IconButton(onClick = { /* Menu */ }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                },
                expanded = searchActive,
                onExpandedChange = { searchActive = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = 0.dp),
            ) {
                // Search suggestions or results could go here
            }

            Text(
                text = if (activeChats.isEmpty()) "Start a new adventure" else "Recent Chats",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            activeChats.ifEmpty {  }

            val displayList = activeChats.ifEmpty { predefinedCharacters }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayList) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onNavigateToChat(character.id) }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Placeholder
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = character.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}
