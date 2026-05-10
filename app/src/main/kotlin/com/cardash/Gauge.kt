package com.cardash

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE_DEG = 135f
private const val SWEEP_DEG = 270f

private val FaceCream = Color(0xFFEDE6D6)
private val FaceCreamDim = Color(0xFFB8B0A0)

data class GaugeZone(val from: Float, val to: Float, val color: Color)

@Composable
fun Gauge(
    value: Float?,
    minValue: Float,
    maxValue: Float,
    title: String,
    unit: String,
    majorStep: Float,
    minorPerMajor: Int = 4,
    zones: List<GaugeZone> = emptyList(),
    valueFormatter: (Float) -> String = { "%.0f".format(it) },
    modifier: Modifier = Modifier,
) {
    val target = (value ?: minValue).coerceIn(minValue, maxValue)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "needle",
    )
    val displayText = if (value == null) "--" else valueFormatter(value)

    Canvas(modifier = modifier) {
        drawGauge(
            needleValue = animated,
            displayText = displayText,
            minValue = minValue,
            maxValue = maxValue,
            title = title,
            unit = unit,
            majorStep = majorStep,
            minorPerMajor = minorPerMajor,
            zones = zones,
        )
    }
}

private fun DrawScope.drawGauge(
    needleValue: Float,
    displayText: String,
    minValue: Float,
    maxValue: Float,
    title: String,
    unit: String,
    majorStep: Float,
    minorPerMajor: Int,
    zones: List<GaugeZone>,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outerR = minOf(cx, cy) * 0.98f
    val bezelW = outerR * 0.05f
    val faceR = outerR - bezelW
    val totalRange = maxValue - minValue

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3A3A3C), Color(0xFF0A0A0B)),
            center = Offset(cx - outerR * 0.4f, cy - outerR * 0.4f),
            radius = outerR * 1.6f,
        ),
        radius = outerR,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color(0xFF111113),
        radius = outerR - bezelW * 0.3f,
        center = Offset(cx, cy),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1A1A1C), Color(0xFF050506)),
            center = Offset(cx - faceR * 0.25f, cy - faceR * 0.35f),
            radius = faceR * 1.3f,
        ),
        radius = faceR,
        center = Offset(cx, cy),
    )

    val zoneStrokeWidth = faceR * 0.05f
    val zoneRadius = faceR - zoneStrokeWidth * 0.9f
    zones.forEach { zone ->
        val startT = ((zone.from - minValue) / totalRange).coerceIn(0f, 1f)
        val endT = ((zone.to - minValue) / totalRange).coerceIn(0f, 1f)
        val zoneStart = START_ANGLE_DEG + startT * SWEEP_DEG
        val zoneSweep = (endT - startT) * SWEEP_DEG
        if (zoneSweep > 0f) {
            drawArc(
                color = zone.color,
                startAngle = zoneStart,
                sweepAngle = zoneSweep,
                useCenter = false,
                topLeft = Offset(cx - zoneRadius, cy - zoneRadius),
                size = Size(zoneRadius * 2, zoneRadius * 2),
                style = Stroke(width = zoneStrokeWidth),
            )
        }
    }

    val tickOuterR = faceR - zoneStrokeWidth * 1.9f
    val majorLen = faceR * 0.10f
    val minorLen = faceR * 0.05f
    val majorCount = (totalRange / majorStep).toInt()
    val totalSteps = majorCount * (minorPerMajor + 1)
    for (i in 0..totalSteps) {
        val isMajor = i % (minorPerMajor + 1) == 0
        val t = i.toFloat() / totalSteps
        val angleDeg = START_ANGLE_DEG + t * SWEEP_DEG
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val len = if (isMajor) majorLen else minorLen
        val sx = cx + (tickOuterR - len) * cos(angleRad)
        val sy = cy + (tickOuterR - len) * sin(angleRad)
        val ex = cx + tickOuterR * cos(angleRad)
        val ey = cy + tickOuterR * sin(angleRad)
        drawLine(
            color = FaceCream,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) faceR * 0.013f else faceR * 0.008f,
        )
    }

    val labelRadius = tickOuterR - majorLen - faceR * 0.1f
    val labelPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 237, 230, 214)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.11f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    drawIntoCanvas { canvas ->
        var v = minValue
        while (v <= maxValue + 0.001f) {
            val t = (v - minValue) / totalRange
            val angleDeg = START_ANGLE_DEG + t * SWEEP_DEG
            val angleRad = (angleDeg * PI / 180.0).toFloat()
            val lx = cx + labelRadius * cos(angleRad)
            val ly = cy + labelRadius * sin(angleRad) + labelPaint.textSize / 3f
            canvas.nativeCanvas.drawText(v.toInt().toString(), lx, ly, labelPaint)
            v += majorStep
        }
    }

    val titlePaint = Paint().apply {
        color = android.graphics.Color.argb(255, 237, 230, 214)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.10f
        isAntiAlias = true
        letterSpacing = 0.18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val unitPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 184, 176, 160)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.07f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(title, cx, cy - faceR * 0.18f - 30f, titlePaint)
        canvas.nativeCanvas.drawText(unit, cx, cy - faceR * 0.06f - 30f, unitPaint)
    }

    val windowW = faceR * 0.55f
    val windowH = faceR * 0.22f
    val windowTop = cy + faceR * 0.16f
    val windowLeft = cx - windowW / 2f
    drawRoundRect(
        color = Color(0xFF050506),
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(faceR * 0.025f),
    )
    drawRoundRect(
        color = Color(0xFF2A2A2C),
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(faceR * 0.025f),
        style = Stroke(width = faceR * 0.005f),
    )
    val displayPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 245, 240, 228)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.18f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    drawIntoCanvas { canvas ->
        val baseline = windowTop + windowH / 2f + displayPaint.textSize / 3f
        canvas.nativeCanvas.drawText(displayText, cx, baseline, displayPaint)
    }

    val needleT = ((needleValue - minValue) / totalRange).coerceIn(0f, 1f)
    val needleAngleRad = ((START_ANGLE_DEG + needleT * SWEEP_DEG) * PI / 180.0).toFloat()
    val needleLen = faceR * 0.78f
    val needleBack = faceR * 0.16f
    val nx = cx + needleLen * cos(needleAngleRad)
    val ny = cy + needleLen * sin(needleAngleRad)
    val bx = cx - needleBack * cos(needleAngleRad)
    val by = cy - needleBack * sin(needleAngleRad)
    drawLine(
        color = Color.Black.copy(alpha = 0.5f),
        start = Offset(bx + 2f, by + 2f),
        end = Offset(nx + 2f, ny + 2f),
        strokeWidth = faceR * 0.028f,
    )
    drawLine(
        color = FaceCream,
        start = Offset(bx, by),
        end = Offset(nx, ny),
        strokeWidth = faceR * 0.025f,
    )

    drawCircle(
        color = Color(0xFF2A2A2C),
        radius = faceR * 0.07f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color(0xFF55524A),
        radius = faceR * 0.07f,
        center = Offset(cx, cy),
        style = Stroke(width = faceR * 0.008f),
    )
    drawCircle(
        color = Color(0xFF0A0A0B),
        radius = faceR * 0.025f,
        center = Offset(cx, cy),
    )
}
