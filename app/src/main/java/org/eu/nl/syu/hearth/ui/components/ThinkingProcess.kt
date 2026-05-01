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

package org.eu.nl.syu.hearth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThinkingProcess(
    thought: String,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (!isComplete) {
                FadeTextAnimation(
                    text = "Thinking Process",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = null,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "Thinking Process",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            GlassySurface(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Box(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth().padding(12.dp).verticalScroll(rememberScrollState())) {
                    MarkdownText(
                        text = thought,
                        smallFontSize = true,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Parses content to extract thinking process wrapped in <think> tags.
 * Supports multiple blocks and streaming (unclosed tags at the end).
 * Returns Triple(thought, remainingContent, isComplete)
 */
fun parseThinkingContent(content: String): Triple<String?, String, Boolean> {
    val formats = listOf(
        Pair("<think>", "</think>"),
        Pair("<|channel>thought\n", "<channel|>"),
        Pair("<|channel>thought", "<channel|>"),
        Pair("<|channel>thought\n", "<|endoftext|>"),
        Pair("<|channel>thought", "<|endoftext|>")
    )

    val thoughts = mutableListOf<String>()
    var mainContent = content
    var isComplete = true

    // We process each format. For each format, we extract all occurrences.
    // To handle interleaving correctly, we'd need a more complex state machine,
    // but usually only one format is used per message.
    for ((startTag, endTag) in formats) {
        var searchIndex = 0
        while (true) {
            val startIndex = mainContent.indexOf(startTag, searchIndex)
            if (startIndex == -1) break

            val endIndex = mainContent.indexOf(endTag, startIndex + startTag.length)
            if (endIndex == -1) {
                // Unclosed tag - it's streaming at the end
                val thought = mainContent.substring(startIndex + startTag.length)
                if (thought.isNotEmpty()) thoughts.add(thought)
                mainContent = mainContent.substring(0, startIndex)
                isComplete = false
                break
            } else {
                // Closed tag
                val thought = mainContent.substring(startIndex + startTag.length, endIndex)
                if (thought.isNotEmpty()) thoughts.add(thought)
                
                // Remove the block from main content
                val before = mainContent.substring(0, startIndex)
                val after = mainContent.substring(endIndex + endTag.length)
                mainContent = before + after
                // Don't increment searchIndex, because mainContent has shrunk
                searchIndex = startIndex
            }
        }
    }

    val combinedThought = if (thoughts.isEmpty()) null else thoughts.joinToString("\n").trim()
    
    // Final cleanup of main content
    val cleanedMain = mainContent.trim()

    return Triple(combinedThought, cleanedMain, isComplete)
}
