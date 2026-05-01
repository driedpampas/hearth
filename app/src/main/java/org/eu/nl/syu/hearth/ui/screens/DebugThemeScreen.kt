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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.hearth.ui.components.GlassySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugThemeScreen(onNavigateBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Debug") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Color Scheme", style = typography.headlineSmall)
            }

            val colors = listOf(
                "Primary" to (colorScheme.primary to colorScheme.onPrimary),
                "PrimaryContainer" to (colorScheme.primaryContainer to colorScheme.onPrimaryContainer),
                "Secondary" to (colorScheme.secondary to colorScheme.onSecondary),
                "SecondaryContainer" to (colorScheme.secondaryContainer to colorScheme.onSecondaryContainer),
                "Tertiary" to (colorScheme.tertiary to colorScheme.onTertiary),
                "TertiaryContainer" to (colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer),
                "Error" to (colorScheme.error to colorScheme.onError),
                "ErrorContainer" to (colorScheme.errorContainer to colorScheme.onErrorContainer),
                "Background" to (colorScheme.background to colorScheme.onBackground),
                "Surface" to (colorScheme.surface to colorScheme.onSurface),
                "SurfaceVariant" to (colorScheme.surfaceVariant to colorScheme.onSurfaceVariant),
                "Outline" to (colorScheme.outline to colorScheme.outline),
                "InverseSurface" to (colorScheme.inverseSurface to colorScheme.inverseOnSurface),
                "InversePrimary" to (colorScheme.inversePrimary to colorScheme.primary),
                "SurfaceDim" to (colorScheme.surfaceDim to colorScheme.onSurface),
                "SurfaceBright" to (colorScheme.surfaceBright to colorScheme.onSurface),
                "SurfaceContainerLowest" to (colorScheme.surfaceContainerLowest to colorScheme.onSurface),
                "SurfaceContainerLow" to (colorScheme.surfaceContainerLow to colorScheme.onSurface),
                "SurfaceContainer" to (colorScheme.surfaceContainer to colorScheme.onSurface),
                "SurfaceContainerHigh" to (colorScheme.surfaceContainerHigh to colorScheme.onSurface),
                "SurfaceContainerHighest" to (colorScheme.surfaceContainerHighest to colorScheme.onSurface),
            )

            items(colors) { (name, colorPair) ->
                ColorSampleCard(name, colorPair.first, colorPair.second)
            }

            item {
                Text("Typography", style = typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Display Large", style = typography.displayLarge)
                    Text("Display Medium", style = typography.displayMedium)
                    Text("Display Small", style = typography.displaySmall)
                    Text("Headline Large", style = typography.headlineLarge)
                    Text("Headline Medium", style = typography.headlineMedium)
                    Text("Headline Small", style = typography.headlineSmall)
                    Text("Title Large", style = typography.titleLarge)
                    Text("Title Medium", style = typography.titleMedium)
                    Text("Title Small", style = typography.titleSmall)
                    Text("Body Large", style = typography.bodyLarge)
                    Text("Body Medium", style = typography.bodyMedium)
                    Text("Body Small", style = typography.bodySmall)
                    Text("Label Large", style = typography.labelLarge)
                    Text("Label Medium", style = typography.labelMedium)
                    Text("Label Small", style = typography.labelSmall)
                }
            }

            item {
                Text("Glassy Samples", style = typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassySurface(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        color = colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Primary", color = colorScheme.onPrimary)
                        }
                    }
                    GlassySurface(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        color = colorScheme.secondary
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Secondary", color = colorScheme.onSecondary)
                        }
                    }
                    GlassySurface(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        color = colorScheme.tertiary
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Tertiary", color = colorScheme.onTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSampleCard(name: String, color: Color, onColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color, contentColor = onColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "#${Integer.toHexString(color.toArgb()).uppercase()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(text = "Sample Text", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
