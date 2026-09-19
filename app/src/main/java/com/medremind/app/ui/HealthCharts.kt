package com.medremind.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

data class ChartSeries(
    val values: List<Float>,
    val color: Color,
    val label: String,
    val low: Float? = null,
    val high: Float? = null
)

/**
 * A compact line chart with an optional healthy "target band" (lower/upper limit)
 * drawn behind the readings, plus the latest value highlighted.
 */
@Composable
fun MetricChartCard(
    title: String,
    currentText: String,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp
) {
    MedCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Shaded band is the healthy range",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    currentText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(s.color)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        s.label + if (s.low != null && s.high != null) {
                            "  ${s.low.toInt()}\u2013${s.high.toInt()}"
                        } else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        var started by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { started = true }
        val progress by animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = tween(MedMotion.Slow, easing = MedMotion.Emphasized),
            label = "chartProgress"
        )

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)) {
            val left = 6f
            val right = size.width - 6f
            val top = 8f
            val bottom = size.height - 8f
            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)

            val allValues = series.flatMap { it.values }
            val lows = series.mapNotNull { it.low }
            val highs = series.mapNotNull { it.high }
            if (allValues.isEmpty()) return@Canvas
            val dataMin = min(allValues.min(), lows.minOrNull() ?: allValues.min())
            val dataMax = max(allValues.max(), highs.maxOrNull() ?: allValues.max())
            val pad = ((dataMax - dataMin) * 0.18f).coerceAtLeast(2f)
            val chartMin = dataMin - pad
            val chartMax = dataMax + pad
            val span = (chartMax - chartMin).coerceAtLeast(1f)

            fun yFor(v: Float): Float =
                bottom - ((v - chartMin) / span) * height

            // Grid lines.
            val gridColor = Color.Gray.copy(alpha = 0.18f)
            for (i in 0..3) {
                val gy = top + height * i / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(left, gy),
                    end = Offset(right, gy),
                    strokeWidth = 1f
                )
            }

            // Target bands.
            series.forEach { s ->
                val lo = s.low
                val hi = s.high
                if (lo != null && hi != null) {
                    val yTop = yFor(hi)
                    val yBottom = yFor(lo)
                    drawRoundRect(
                        color = s.color.copy(alpha = 0.12f),
                        topLeft = Offset(left, yTop),
                        size = Size(width, (yBottom - yTop).coerceAtLeast(1f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }

            // Series lines and points.
            series.forEach { s ->
                val values = s.values
                if (values.isEmpty()) return@forEach
                val n = values.size
                fun xFor(i: Int): Float =
                    if (n == 1) left + width / 2f
                    else left + width * i / (n - 1).toFloat()

                val drawn = progress * (n - 1).toFloat()
                val path = Path()
                path.moveTo(xFor(0), yFor(values[0]))
                var i = 1
                while (i < n && i <= drawn) {
                    path.lineTo(xFor(i), yFor(values[i]))
                    i++
                }
                if (i - 1 < drawn && i < n) {
                    val frac = drawn - (i - 1)
                    val x0 = xFor(i - 1)
                    val x1 = xFor(i)
                    val y0 = yFor(values[i - 1])
                    val y1 = yFor(values[i])
                    path.lineTo(x0 + (x1 - x0) * frac, y0 + (y1 - y0) * frac)
                }
                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                for (index in 0 until n) {
                    if (index > drawn) break
                    val isLast = index == n - 1
                    val cx = xFor(index)
                    val cy = yFor(values[index])
                    if (isLast) {
                        drawCircle(Color.White, radius = 7f, center = Offset(cx, cy))
                        drawCircle(s.color, radius = 5f, center = Offset(cx, cy))
                    } else {
                        drawCircle(s.color, radius = 3.5f, center = Offset(cx, cy))
                    }
                }
            }
        }
    }
}
