/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.eu.nl.syu.hearth.ui.components.*
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

    LaunchedEffect(selectedScope) {
        viewModel.init(selectedScope, threadId, characterId)
    }

    Scaffold(
        topBar = {
            GlassySurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                blurRadius = 8.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("User Persona", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (selectedScope == EditScope.THREAD) {
                            IconButton(onClick = { viewModel.saveRawBio(rawBio); onNavigateBack() }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
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
            ScopedButtonGroup(
                selectedScope = selectedScope.name,
                scopes = listOf(EditScope.GLOBAL.name, EditScope.CHARACTER.name, EditScope.THREAD.name),
                onScopeSelected = { selectedScope = EditScope.valueOf(it) }
            )

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

            uiState.allPersonas.forEach { persona ->
                val isSelected = uiState.selectedPersona?.id == persona.id
                GlassySurface(
                    onClick = { viewModel.selectPersona(persona) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
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
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}
