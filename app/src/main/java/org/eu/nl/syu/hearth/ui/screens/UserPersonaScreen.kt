/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.eu.nl.syu.hearth.ui.components.GlassySurface
import org.eu.nl.syu.hearth.ui.components.HearthSearchBar
import org.eu.nl.syu.hearth.ui.components.ScopedButtonGroup
import org.eu.nl.syu.hearth.ui.components.WavyHorizontalDivider
import org.eu.nl.syu.hearth.ui.viewmodels.EditScope
import org.eu.nl.syu.hearth.ui.viewmodels.UserPersonaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPersonaScreen(
    threadId: String?,
    characterId: String?,
    initialScope: EditScope,
    onNavigateBack: () -> Unit,
    onNavigateToCreatePersona: () -> Unit,
    viewModel: UserPersonaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedScope by remember { mutableStateOf(initialScope) }
    var rawBio by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var infoPersona by remember { mutableStateOf<org.eu.nl.syu.hearth.data.UserPersona?>(null) }

    val filteredPersonas = remember(searchQuery, uiState.allPersonas) {
        if (searchQuery.isEmpty()) uiState.allPersonas
        else uiState.allPersonas.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.bio.contains(searchQuery, ignoreCase = true) 
        }
    }

    LaunchedEffect(selectedScope) {
        viewModel.init(selectedScope, threadId, characterId)
    }

    if (infoPersona != null) {
        AlertDialog(
            onDismissRequest = { infoPersona = null },
            title = { Text(infoPersona?.name ?: "Persona Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(infoPersona?.bio ?: "", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoPersona = null }) { Text("Close") }
            },
            dismissButton = if (threadId != null) {
                {
                    TextButton(onClick = { 
                        infoPersona?.let { viewModel.selectPersona(it) }
                        infoPersona = null
                    }) { Text("Select") }
                }
            } else null
        )
    }

    Scaffold(
        topBar = {
            HearthSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                active = searchActive,
                onActiveChange = { searchActive = it },
                onSearch = { searchActive = false },
                placeholder = "Search personas",
                leadingIcon = {
                    if (threadId != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    if (selectedScope == EditScope.THREAD) {
                        IconButton(onClick = { viewModel.saveRawBio(rawBio); onNavigateBack() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreatePersona) {
                Icon(Icons.Default.Add, contentDescription = "Create Persona")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (threadId != null) {
                ScopedButtonGroup(
                    selectedScope = selectedScope.name,
                    scopes = listOf(EditScope.GLOBAL.name, EditScope.CHARACTER.name, EditScope.THREAD.name),
                    onScopeSelected = { selectedScope = EditScope.valueOf(it) }
                )
            }

            if (selectedScope == EditScope.THREAD) {
                Text(
                    "THREAD-SPECIFIC BIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = rawBio,
                    onValueChange = { rawBio = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe yourself for this thread only...") },
                    minLines = 6,
                    shape = MaterialTheme.shapes.medium
                )
                
                WavyHorizontalDivider(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                
                Text(
                    "OR SELECT FROM LIBRARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            filteredPersonas.forEach { persona ->
                val isSelected = uiState.selectedPersona?.id == persona.id
                GlassySurface(
                    onClick = { 
                        if (threadId != null) {
                            viewModel.selectPersona(persona)
                        } else {
                            infoPersona = persona
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isSelected && threadId != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = if (isSelected && threadId != null) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
                             else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(persona.name, fontWeight = FontWeight.Bold)
                            Text(
                                persona.bio,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected && threadId != null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}
