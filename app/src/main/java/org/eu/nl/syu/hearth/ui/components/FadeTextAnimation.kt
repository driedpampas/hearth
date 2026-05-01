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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

/**
 * A composable that displays text with a moving fade animation, creating a "stealthy" shimmer effect.
 * @param modifier Modifier for styling and layout.
 * @param text The text to display with the fade animation.
 * @param style The TextStyle to apply to the text.
 * @param fontWeight Optional FontWeight to apply to the text.
 * @param textColor The base color of the text, which will be used in the gradient.
 * @param mixinColor The color used for the fading wave, typically a transparent or lighter version of textColor.
 * @param waveWidth The thickness/softness of the fading wave (in pixels).
 * @param slant The vertical slant of the wave, controlling the angle of the fade (in pixels).
 * @param durationMillis The duration of one full animation cycle in milliseconds.
 */
@Preview
@Composable
fun FadeTextAnimation(
    modifier: Modifier = Modifier,
    text: String = "Recalculating Embeddings...",
    style: TextStyle = MaterialTheme.typography.titleLarge,
    fontWeight: FontWeight? = FontWeight.Bold,
    textColor: Color = MaterialTheme.colorScheme.primary,
    mixinColor: Color = Color.Transparent,
    waveWidth: Float = 4000f, // Thickness/Softness of the fading wave
    slant: Float = 750f,
    durationMillis: Int = 2000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StealthFade")

    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -waveWidth - slant,
        targetValue = 750f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

//    val brush = Brush.linearGradient(
//        colors = listOf(
//            textColor,
//            textColor.copy(alpha = 0f),
//            textColor
//        ),
//        start = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim),
//        end = androidx.compose.ui.geometry.Offset(translateAnim + 500f, translateAnim + 500f)
//    )

    val brush = Brush.linearGradient(
        0.0f to textColor,
        0.45f to textColor,
        0.5f to mixinColor,
        0.55f to textColor,
        1.0f to textColor,

        // 'start' is the top-left of the wave
        start = androidx.compose.ui.geometry.Offset(translateAnim, 0f),
        // 'end' is shifted by waveWidth (thickness) and slant (angle)
        end = androidx.compose.ui.geometry.Offset(translateAnim + waveWidth, slant)
    )

    Text(
        text = text,
        style = style.copy(
            brush = brush,
            fontWeight = fontWeight
        ),
        modifier = modifier
    )
}