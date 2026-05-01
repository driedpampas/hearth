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

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WavyHorizontalDivider(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.outlineVariant,
    waveLength: Float = 40f,
    waveHeight: Float = 10f,
    inset: Dp = 0.dp
) {
    Canvas(modifier = modifier) {
        val insetPx = inset.toPx()
        val path = Path().apply {
            moveTo(insetPx, size.height / 2)
            var x = insetPx
            while (x < size.width - insetPx) {
                val remainingX = size.width - insetPx - x
                val segmentWidth = if (remainingX < waveLength) remainingX else waveLength
                relativeQuadraticTo(segmentWidth / 4, -waveHeight, segmentWidth / 2, 0f)
                relativeQuadraticTo(segmentWidth / 4, waveHeight, segmentWidth / 2, 0f)
                x += segmentWidth
            }
        }
        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
