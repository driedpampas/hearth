package org.eu.nl.syu.charchat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassySurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    blurRadius: Dp = 4.dp, // Controls how far the color bleeds outward
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    // 1. Outer Box: No clipping here. This allows the inner blur to escape the bounds.
    Box(
        modifier = modifier.then(
            if (onClick != null || onLongClick != null) {
                Modifier.combinedClickable(
                    enabled = enabled,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick
                )
            } else Modifier
        )
    ) {
        // 2. The Bleed Layer: This box matches the content size but blurs outwards.
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(color = color, shape = shape)
        )

        // 3. The Content Layer: The actual card surface. 
        Surface(
            modifier = Modifier
                .clip(shape), // Clip is applied ONLY to the content layer
            //color = color.copy(alpha = 0.4f), // Semi-transparent so it feels glassy
            color = Color.Transparent,
            contentColor = contentColor,
        ) {
            content()
        }
    }
}