package org.eu.nl.syu.charchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterScreen(
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }
    var lore by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Character") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Save logic */ }) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = shortDescription,
                onValueChange = { shortDescription = it },
                label = { Text("Short Description") },
                placeholder = { Text("Visible on home screen") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = lore,
                onValueChange = { lore = it },
                label = { Text("Lore / System Instructions") },
                placeholder = { Text("Define your character's personality, background, and knowledge...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 5
            )

            Button(
                onClick = { /* Save logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Character")
            }
        }
    }
}
