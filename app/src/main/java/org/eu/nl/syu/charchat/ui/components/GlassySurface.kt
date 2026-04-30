package org.eu.nl.syu.charchat.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun GlassySurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = MaterialTheme.colorScheme.onSurface, //surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
//                .background(color)
                .drawWithContent {
                    // Add subtle texture/noise so the blur has something to "smear"
                    // This makes the blur effect visible even on solid backgrounds
                    val colors = listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.1f))//, Color.Transparent, Color.White.copy(alpha = 0.05f))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = colors,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                    drawContent()
                }
                .then(
                    Modifier.graphicsLayer {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            150f, 150f, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                )
        )

        // Content layer
        Surface(
            color = Color.Transparent,
            contentColor = contentColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }

        // Optional subtle highlight border
//        Box(
//            modifier = Modifier
//                .matchParentSize()
//                .border(
//                    width = 0.5.dp,
//                    brush = Brush.verticalGradient(
//                        colors = listOf(
//                            Color.White.copy(alpha = 0.3f),
//                            Color.White.copy(alpha = 0.05f)
//                        )
//                    ),
//                    shape = shape
//                )
//        )
    }
}
