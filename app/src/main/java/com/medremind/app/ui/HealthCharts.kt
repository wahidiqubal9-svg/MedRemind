package com.medremind.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medremind.app.data.Metric
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ChartSeries(
    val values: List<Float>,
    val color: Color,
    val label: String,
    val low: Float? = null,
    val high: Float? = null
)

/* ---------------- Vitals (BP & CBG) ---------------- */

enum class HealthZone { GREEN, YELLOW, RED }

object Vitals {
    val Green = Color(0xFF2E9C6E)
    val Yellow = Color(0xFFE6B422)
    val Red = Color(0xFFD94A4A)
    val Systolic = Color(0xFF1E5B9E)
    val Diastolic = Color(0xFF8B5CF6)
    val Glucose = Color(0xFF2B7A4B)

    val GreenBg = Color(0xFFE1F3EA)
    val YellowBg = Color(0xFFFEF5E0)
    val RedBg = Color(0xFFFDE7E7)

    fun classifyBP(sys: Float, dia: Float): HealthZone = when {
        sys in 90f..120f && dia in 60f..80f -> HealthZone.GREEN
        sys <= 139f && dia <= 89f -> HealthZone.YELLOW
        else -> HealthZone.RED
    }

    fun classifyCBG(
        value: Float,
        context: String = com.medremind.app.data.MetricContext.NONE
    ): HealthZone {
        val preMeal = context == com.medremind.app.data.MetricContext.PRE_MEAL
        return if (preMeal) {
            when {
                value in 80f..130f -> HealthZone.GREEN
                value > 130f && value <= 180f -> HealthZone.YELLOW
                else -> HealthZone.RED
            }
        } else {
            when {
                value in 80f..180f -> HealthZone.GREEN
                value > 180f && value <= 250f -> HealthZone.YELLOW
                else -> HealthZone.RED
            }
        }
    }

    fun color(zone: HealthZone): Color = when (zone) {
        HealthZone.GREEN -> Green
        HealthZone.YELLOW -> Yellow
        HealthZone.RED -> Red
    }

    fun badgeTint(pct: Int): Color = when {
        pct >= 70 -> Green
        pct >= 50 -> Yellow
        else -> Red
    }
}

data class ZoneBand(val from: Float, val to: Float, val color: Color)

data class VitalSeries(
    val label: String,
    val color: Color,
    val values: List<Float>,
    val zones: List<HealthZone>
)

@Composable
fun VitalsChartCard(
    title: String,
    subtitle: String,
    currentText: String,
    series: List<VitalSeries>,
    bands: List<ZoneBand>,
    yMin: Float,
    yMax: Float,
    xLabels: List<String> = emptyList(),
    pointLabels: List<String> = emptyList(),
    unit: String = "",
    onAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp
) {
    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    MedCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
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
            if (onAdd != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onAdd,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Add",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        var started by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { started = true }
        val progress by animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = tween(MedMotion.Slow, easing = MedMotion.Emphasized),
            label = "vitalsProgress"
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(series, pointLabels, yMin, yMax) {
                    detectTapGestures { tap ->
                        val l = 88f
                        val r = size.width - 26f
                        val t = 14f
                        val b = size.height - 36f
                        val w = (r - l).coerceAtLeast(1f)
                        val h = (b - t).coerceAtLeast(1f)
                        val sp = (yMax - yMin).coerceAtLeast(1f)
                        val count = series.maxOfOrNull { it.values.size } ?: 0
                        fun px(i: Int): Float =
                            if (count <= 1) l + w / 2f else l + w * i / (count - 1).toFloat()
                        fun py(v: Float): Float = b - ((v - yMin) / sp) * h
                        var best = -1
                        var bestDist = Float.MAX_VALUE
                        series.forEach { s ->
                            s.values.forEachIndexed { i, v ->
                                val dx = tap.x - px(i)
                                val dy = tap.y - py(v)
                                val dist = kotlin.math.hypot(dx, dy)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    best = i
                                }
                            }
                        }
                        selectedPoint = if (best >= 0 && bestDist < 46f) best else null
                    }
                }
        ) {
            val left = 88f
            val right = size.width - 26f
            val top = 14f
            val bottom = size.height - 36f
            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)
            val span = (yMax - yMin).coerceAtLeast(1f)

            fun yFor(v: Float): Float = bottom - ((v - yMin) / span) * height
            fun xFor(i: Int, n: Int): Float =
                if (n <= 1) left + width / 2f
                else left + width * i / (n - 1).toFloat()

            val yPaint = android.graphics.Paint().apply {
                color = 0xFF98A2B3.toInt()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            val xPaint = android.graphics.Paint().apply {
                color = 0xFF98A2B3.toInt()
                textSize = 9.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val gridColor = Color.Gray.copy(alpha = 0.16f)
            for (i in 0..4) {
                val value = yMin + span * i / 4f
                val gy = yFor(value)
                drawLine(gridColor, Offset(left, gy), Offset(right, gy), strokeWidth = 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    value.toInt().toString(), left - 10f, gy + 4f, yPaint
                )
            }

            val maxCount = series.maxOfOrNull { it.values.size } ?: 0

            series.forEach { s ->
                val values = s.values
                if (values.isEmpty()) return@forEach
                val n = values.size
                val drawn = progress * (n - 1).toFloat()
                val path = Path()
                path.moveTo(xFor(0, n), yFor(values[0]))
                var i = 1
                while (i < n && i <= drawn) {
                    path.lineTo(xFor(i, n), yFor(values[i]))
                    i++
                }
                if (i - 1 < drawn && i < n) {
                    val frac = drawn - (i - 1)
                    val x0 = xFor(i - 1, n)
                    val x1 = xFor(i, n)
                    val y0 = yFor(values[i - 1])
                    val y1 = yFor(values[i])
                    path.lineTo(x0 + (x1 - x0) * frac, y0 + (y1 - y0) * frac)
                }
                drawPath(path, color = s.color, style = Stroke(width = 3f, cap = StrokeCap.Round))

                for (index in values.indices) {
                    if (index > drawn) break
                    val isLast = index == n - 1
                    val cx = xFor(index, n)
                    val cy = yFor(values[index])
                    val dot = s.zones.getOrElse(index) { HealthZone.GREEN }.let { Vitals.color(it) }
                    if (isLast) {
                        drawCircle(Color.White, radius = 25f, center = Offset(cx, cy))
                        drawCircle(dot, radius = 20f, center = Offset(cx, cy))
                    } else {
                        drawCircle(Color.White, radius = 21f, center = Offset(cx, cy))
                        drawCircle(dot, radius = 16f, center = Offset(cx, cy))
                    }
                    if (index == selectedPoint) {
                        drawCircle(
                            color = s.color,
                            radius = if (isLast) 25f else 21f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            // X-axis labels (dates), spaced out to avoid overlap.
            if (xLabels.isNotEmpty()) {
                val gapPerPoint = if (maxCount > 1) width / (maxCount - 1) else width
                val step = kotlin.math.ceil(60f / gapPerPoint).toInt().coerceAtLeast(1)
                xLabels.forEachIndexed { index, label ->
                    if (index % step == 0 || index == xLabels.lastIndex) {
                        drawContext.canvas.nativeCanvas.drawText(
                            label, xFor(index, maxCount), bottom + 22f, xPaint
                        )
                    }
                }
            }

            // In-chart tooltip for the tapped point.
            val sel = selectedPoint
            val firstSeries = series.firstOrNull()
            if (sel != null && firstSeries != null && sel < firstSeries.values.size) {
                val cx = xFor(sel, maxCount)
                val cy = yFor(firstSeries.values[sel])
                val valueText = series.joinToString("/") { s ->
                    s.values.getOrNull(sel)?.let { formatChartValue(it) } ?: ""
                }
                val line1 = pointLabels.getOrNull(sel).orEmpty()
                val line2 = ("$valueText $unit").trim()
                val zone = firstSeries.zones.getOrNull(sel) ?: HealthZone.GREEN
                val statusColor = Vitals.color(zone)
                val line3 = when (zone) {
                    HealthZone.GREEN -> "In range"
                    HealthZone.YELLOW -> "Borderline"
                    HealthZone.RED -> "Out of range"
                }
                val bodyPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 11.sp.toPx()
                    isAntiAlias = true
                }
                val valuePaint = android.graphics.Paint().apply {
                    color = statusColor.toArgb()
                    textSize = 14.sp.toPx()
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                val width1 = bodyPaint.measureText(line1)
                val width2 = valuePaint.measureText(line2)
                val width3 = bodyPaint.measureText(line3)
                val boxW = maxOf(width1, width2, width3) + 26f
                val lineH = bodyPaint.textSize + 7f
                val boxH = lineH * 3f + 16f
                val bx = (cx - boxW / 2f).coerceIn(left, (right - boxW).coerceAtLeast(left))
                var by = cy - boxH - 24f
                if (by < top) by = cy + 26f
                by = by.coerceIn(top, (bottom - boxH).coerceAtLeast(top))
                drawRoundRect(
                    color = Color(0xF20B2B4A.toInt()),
                    topLeft = Offset(bx, by),
                    size = Size(boxW, boxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )
                val tx = bx + 13f
                var ty = by + 12f + bodyPaint.textSize
                drawContext.canvas.nativeCanvas.drawText(line1, tx, ty, bodyPaint)
                ty += lineH
                drawContext.canvas.nativeCanvas.drawText(line2, tx, ty, valuePaint)
                ty += lineH
                drawContext.canvas.nativeCanvas.drawText(line3, tx, ty, bodyPaint)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Vitals.Green, "In range")
            LegendDot(Vitals.Yellow, "Borderline")
            LegendDot(Vitals.Red, "Out of range")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VitalsStatCard(
    label: String,
    value: String,
    badgeText: String?,
    badgeTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(92.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = MedElevation.card
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            if (badgeText != null) {
                Spacer(Modifier.height(5.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeTint.copy(alpha = 0.16f),
                    contentColor = badgeTint
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

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

@Composable
fun GlucoseTrendsCard(
    readings: List<Metric>,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (readings.isEmpty()) return
    val chips = readings.takeLast(6)
    var selectedIndex by remember(readings.size) { mutableStateOf(chips.lastIndex) }
    val index = selectedIndex.coerceIn(0, chips.lastIndex)
    val selected = chips[index]
    val zone = Vitals.classifyCBG(selected.value, selected.context)
    val zoneColor = Vitals.color(zone)
    val zoneBg = when (zone) {
        HealthZone.GREEN -> Vitals.GreenBg
        HealthZone.YELLOW -> Vitals.YellowBg
        HealthZone.RED -> Vitals.RedBg
    }
    val statusText = when (zone) {
        HealthZone.GREEN -> "In Control"
        HealthZone.YELLOW -> "Borderline"
        HealthZone.RED -> "Out of control"
    }
    val hour = Calendar.getInstance().apply { timeInMillis = selected.recordedAt }
        .get(Calendar.HOUR_OF_DAY)
            val timeOfDay = when (hour) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
    val dateFormat = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    val isMostRecent = index == chips.lastIndex

    MedCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Vitals.Red),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Bloodtype,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Blood Glucose",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Add",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(zoneBg)
                .border(1.5.dp, zoneColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = zoneColor
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    com.medremind.app.data.MetricContext.label(selected.context)
                        .ifBlank { timeOfDay },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    dateFormat.format(Date(selected.recordedAt)) +
                        if (isMostRecent) "  (Most Recent)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${selected.value.toInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = zoneColor
                )
                Text(
                    "mg/dL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            chips.forEachIndexed { chipIndex, metric ->
                val z = Vitals.classifyCBG(metric.value, metric.context)
                val c = Vitals.color(z)
                val selectedChip = chipIndex == index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.height(14.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (selectedChip) {
                            Text(
                                "\u25BC",
                                style = MaterialTheme.typography.labelSmall,
                                color = c
                            )
                        }
                    }
                    Surface(
                        onClick = { selectedIndex = chipIndex },
                        shape = CircleShape,
                        color = if (selectedChip) c else MaterialTheme.colorScheme.surface,
                        border = if (selectedChip) null
                        else androidx.compose.foundation.BorderStroke(1.dp, c.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${metric.value.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedChip) Color.White else c
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        com.medremind.app.data.MetricContext.label(metric.context)
                            .ifBlank {
                                SimpleDateFormat("h a", Locale.getDefault())
                                    .format(Date(metric.recordedAt))
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BpTrendsCard(
    readings: List<Metric>,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (readings.isEmpty()) return
    val chips = readings.takeLast(6)
    var selectedIndex by remember(readings.size) { mutableStateOf(chips.lastIndex) }
    val index = selectedIndex.coerceIn(0, chips.lastIndex)
    val selected = chips[index]
    val zone = Vitals.classifyBP(selected.value, selected.value2)
    val zoneColor = Vitals.color(zone)
    val zoneBg = when (zone) {
        HealthZone.GREEN -> Vitals.GreenBg
        HealthZone.YELLOW -> Vitals.YellowBg
        HealthZone.RED -> Vitals.RedBg
    }
    val statusText = when (zone) {
        HealthZone.GREEN -> "In Control"
        HealthZone.YELLOW -> "Borderline"
        HealthZone.RED -> "Out of control"
    }
    val hour = Calendar.getInstance().apply { timeInMillis = selected.recordedAt }
        .get(Calendar.HOUR_OF_DAY)
    val timeOfDay = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..20 -> "Evening"
        else -> "Night"
    }
    val dateFormat = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    val isMostRecent = index == chips.lastIndex

    MedCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Vitals.Systolic),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Blood Pressure",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Add",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(zoneBg)
                .border(1.5.dp, zoneColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = zoneColor
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    timeOfDay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    dateFormat.format(Date(selected.recordedAt)) +
                        if (isMostRecent) "  (Most Recent)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${selected.value.toInt()}/${selected.value2.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = zoneColor
                )
                Text(
                    "mmHg",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            chips.forEachIndexed { chipIndex, metric ->
                val z = Vitals.classifyBP(metric.value, metric.value2)
                val c = Vitals.color(z)
                val selectedChip = chipIndex == index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.height(14.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (selectedChip) {
                            Text(
                                "\u25BC",
                                style = MaterialTheme.typography.labelSmall,
                                color = c
                            )
                        }
                    }
                    Surface(
                        onClick = { selectedIndex = chipIndex },
                        shape = CircleShape,
                        color = if (selectedChip) c else MaterialTheme.colorScheme.surface,
                        border = if (selectedChip) null
                        else androidx.compose.foundation.BorderStroke(1.dp, c.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier.size(58.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${metric.value.toInt()}/${metric.value2.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedChip) Color.White else c
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        SimpleDateFormat("h a", Locale.getDefault())
                            .format(Date(metric.recordedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatChartValue(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString()
    else String.format(Locale.getDefault(), "%.1f", v)
