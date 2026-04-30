package org.eu.nl.syu.charchat.ui.components

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
fun WavyVerticalDivider(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.outlineVariant,
    waveLength: Float = 24f,
    waveHeight: Float = 8f,
    inset: Dp = 0.dp
) {
    Canvas(modifier = modifier) {
        val insetPx = inset.toPx()
        val path = Path().apply {
            moveTo(size.width / 2, insetPx)
            var y = insetPx
            while (y < size.height - insetPx) {
                val remainingY = size.height - insetPx - y
                val segmentHeight = if (remainingY < waveLength) remainingY else waveLength
                relativeQuadraticTo(-waveHeight, segmentHeight / 4, 0f, segmentHeight / 2)
                relativeQuadraticTo(waveHeight, segmentHeight / 4, 0f, segmentHeight / 2)
                y += segmentHeight
            }
        }
        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
