/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Save
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
import org.eu.nl.syu.hearth.ui.components.GlassySurface
import org.eu.nl.syu.hearth.ui.components.WavyHorizontalDivider
import org.eu.nl.syu.hearth.ui.viewmodels.CreatePersonaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonaScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatePersonaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
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
                    title = { Text("Create Persona", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.savePersona() },
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text("Save Persona") }
            )
        }
    ) { paddingValues ->
        Box(
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Identity Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                GlassySurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.updateName(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Persona Name") },
                            placeholder = { Text("e.g. Mysterious Traveler") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedTextField(
                            value = uiState.bio,
                            onValueChange = { viewModel.updateBio(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Bio / Description") },
                            placeholder = { Text("Describe who you are, your background, and personality...") },
                            minLines = 8,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                WavyHorizontalDivider(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )

                Text(
                    "This persona will be available in your library and can be used as a default for characters or specific threads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }
}
