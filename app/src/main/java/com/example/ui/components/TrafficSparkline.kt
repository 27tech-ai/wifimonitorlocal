package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun TrafficSparkline(
    rxHistory: List<Long>,
    txHistory: List<Long>,
    modifier: Modifier = Modifier,
    rxColor: Color = Color(0xFF0284C7),
    txColor: Color = Color(0xFFF97316)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (rxHistory.isEmpty() && txHistory.isEmpty()) return@Canvas

        val maxRx = rxHistory.maxOrNull() ?: 1L
        val maxTx = txHistory.maxOrNull() ?: 1L
        val maxVal = maxOf(maxRx, maxTx, 1024L).toFloat()

        fun drawLineFor(data: List<Long>, color: Color, strokeWidth: Float) {
            if (data.size < 2) return
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            val path = Path()

            data.forEachIndexed { index, value ->
                val x = index * stepX
                val normalizedY = 1f - (value.toFloat() / maxVal).coerceIn(0f, 1f)
                val y = (normalizedY * (height - 4f)) + 2f

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Background baseline grid
        drawLine(
            color = Color.Gray.copy(alpha = 0.15f),
            start = Offset(0f, height - 1f),
            end = Offset(width, height - 1f),
            strokeWidth = 1f
        )

        drawLineFor(rxHistory, rxColor, strokeWidth = 3f)
        drawLineFor(txHistory, txColor, strokeWidth = 2.5f)
    }
}
