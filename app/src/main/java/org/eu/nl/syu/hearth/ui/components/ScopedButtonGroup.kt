/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScopedButtonGroup(
    selectedScope: String,
    scopes: List<String>,
    onScopeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassySurface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        blurRadius = 8.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            scopes.forEach { scope ->
                val isSelected = scope == selectedScope
                GlassySurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            else Color.Transparent,
                    blurRadius = if (isSelected) 6.dp else 0.dp,
                    onClick = { onScopeSelected(scope) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scope,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onTertiary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
