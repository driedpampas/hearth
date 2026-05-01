package org.eu.nl.syu.charchat.ui.components

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
 * Returns Triple(thought, remainingContent, isComplete)
 */
fun parseThinkingContent(content: String): Triple<String?, String, Boolean> {
    val startTag = "<think>"
    val endTag = "</think>"
    
    val startIndex = content.indexOf(startTag)
    if (startIndex == -1) return Triple(null, content, true)
    
    val beforeThought = content.substring(0, startIndex)
    val afterStart = content.substring(startIndex + startTag.length)
    
    val endIndex = afterStart.indexOf(endTag)
    return if (endIndex == -1) {
        // Tag started but not ended (streaming)
        Triple(afterStart, beforeThought, false)
    } else {
        // Tag completed
        val thought = afterStart.substring(0, endIndex)
        val afterThought = afterStart.substring(endIndex + endTag.length)
        // Combine text before and after the thought as the main content
        val combinedContent = (beforeThought.trim() + "\n" + afterThought.trim()).trim()
        Triple(thought.trim(), combinedContent, true)
    }
}
