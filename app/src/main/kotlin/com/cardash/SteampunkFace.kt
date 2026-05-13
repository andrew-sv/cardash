package com.cardash

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SP_START_ANGLE_DEG = 135f
private const val SP_SWEEP_DEG = 270f

private val SepiaBg = Color(0xFF3A2616)
private val SepiaBgLight = Color(0xFF55381F)
private val SepiaBgDark = Color(0xFF1F1208)
private val BrassLight = Color(0xFFE0BC7E)
private val BrassMid = Color(0xFFA87B4A)
private val BrassDark = Color(0xFF6B4422)
private val BrassShadow = Color(0xFF3E2810)
private val PaperCream = Color(0xFFEFE3C6)
private val PaperCreamShadow = Color(0xFFC9B58A)
private val InkBrown = Color(0xFF1F140A)
private val WindowBg = Color(0xFF0E0703)
private val WindowAmber = 0xFFE8C97B.toInt()

@Composable
fun SteampunkFace(state: DashboardState, settings: DashboardSettings) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawSteampunkBackdrop(size.width, size.height) },
        contentAlignment = Alignment.Center,
    ) {
        val panelSize = minOf(maxWidth - 16.dp, (maxHeight - 24.dp) / 2)
        val maxSpeed = settings.maxSpeed.toFloat()
        val maxAltitude = settings.maxAltitude.toFloat()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SteampunkGaugePanel(size = panelSize) {
                SteampunkGauge(
                    value = state.speedKmh,
                    minValue = 0f,
                    maxValue = maxSpeed,
                    title = "SPEED",
                    unit = "km/h",
                    majorStep = niceMajorStep(maxSpeed, targetMajors = 10),
                    minorPerMajor = 3,
                    zones = listOf(
                        GaugeZone(maxSpeed * 0.5f, maxSpeed * 0.65f, Color(0xFF5E8A3A)),
                        GaugeZone(maxSpeed * 0.65f, maxSpeed * 0.8f, Color(0xFFC98A2C)),
                        GaugeZone(maxSpeed * 0.8f, maxSpeed, Color(0xFFB8392A)),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            SteampunkGaugePanel(size = panelSize) {
                SteampunkGauge(
                    value = state.altitudeM,
                    minValue = 0f,
                    maxValue = maxAltitude,
                    title = "ALTITUDE",
                    unit = "m",
                    majorStep = niceMajorStep(maxAltitude, targetMajors = 10),
                    minorPerMajor = 4,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun DrawScope.drawSteampunkBackdrop(w: Float, h: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(SepiaBgLight, SepiaBg, SepiaBgDark),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h),
        ),
        size = Size(w, h),
    )
}

@Composable
private fun SteampunkGaugePanel(
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val cornerRadius = 10.dp
    val screwInset = 6.dp
    val screwRadius = 5.dp
    val gaugeInset = 6.dp

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val cr = cornerRadius.toPx()
                val si = screwInset.toPx()
                val sr = screwRadius.toPx()
                val w = this.size.width
                val h = this.size.height

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A3018),
                            Color(0xFF2F1D0E),
                            Color(0xFF1B1006),
                        ),
                    ),
                    cornerRadius = CornerRadius(cr),
                )
                drawRoundRect(
                    color = BrassMid,
                    cornerRadius = CornerRadius(cr),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawRoundRect(
                    color = BrassDark,
                    cornerRadius = CornerRadius(cr),
                    style = Stroke(width = 0.5.dp.toPx()),
                )

                listOf(
                    Offset(si + sr, si + sr),
                    Offset(w - si - sr, si + sr),
                    Offset(si + sr, h - si - sr),
                    Offset(w - si - sr, h - si - sr),
                ).forEach { center ->
                    drawBrassScrew(center, sr)
                }
            }
            .padding(gaugeInset),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private fun DrawScope.drawBrassScrew(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BrassLight, BrassDark),
            center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
            radius = radius * 1.8f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = BrassShadow,
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.10f),
    )
    drawLine(
        color = Color(0xFF1A0E05),
        start = Offset(center.x - radius * 0.55f, center.y - radius * 0.55f),
        end = Offset(center.x + radius * 0.55f, center.y + radius * 0.55f),
        strokeWidth = radius * 0.18f,
    )
}

@Composable
private fun SteampunkGauge(
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
        label = "spneedle",
    )
    val displayText = if (value == null) "--" else valueFormatter(value)

    Canvas(modifier = modifier) {
        drawSteampunkGauge(
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

private fun DrawScope.drawSteampunkGauge(
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
    val bezelW = outerR * 0.11f
    val faceR = outerR - bezelW
    val totalRange = maxValue - minValue

    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                BrassDark,
                BrassMid,
                BrassLight,
                BrassMid,
                BrassDark,
                BrassMid,
                BrassLight,
                BrassMid,
                BrassDark,
            ),
            center = Offset(cx, cy),
        ),
        radius = outerR,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = BrassShadow,
        radius = outerR,
        center = Offset(cx, cy),
        style = Stroke(width = bezelW * 0.10f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BrassLight, BrassMid),
            center = Offset(cx - outerR * 0.3f, cy - outerR * 0.4f),
            radius = outerR,
        ),
        radius = faceR + bezelW * 0.25f,
        center = Offset(cx, cy),
        style = Stroke(width = bezelW * 0.18f),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PaperCream, PaperCreamShadow),
            center = Offset(cx - faceR * 0.15f, cy - faceR * 0.20f),
            radius = faceR * 1.25f,
        ),
        radius = faceR,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = BrassShadow.copy(alpha = 0.35f),
        radius = faceR,
        center = Offset(cx, cy),
        style = Stroke(width = faceR * 0.012f),
    )

    val zoneStrokeWidth = faceR * 0.045f
    val zoneRadius = faceR - zoneStrokeWidth * 0.9f
    zones.forEach { zone ->
        val startT = ((zone.from - minValue) / totalRange).coerceIn(0f, 1f)
        val endT = ((zone.to - minValue) / totalRange).coerceIn(0f, 1f)
        val zoneStart = SP_START_ANGLE_DEG + startT * SP_SWEEP_DEG
        val zoneSweep = (endT - startT) * SP_SWEEP_DEG
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
        val angleDeg = SP_START_ANGLE_DEG + t * SP_SWEEP_DEG
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val len = if (isMajor) majorLen else minorLen
        val sx = cx + (tickOuterR - len) * cos(angleRad)
        val sy = cy + (tickOuterR - len) * sin(angleRad)
        val ex = cx + tickOuterR * cos(angleRad)
        val ey = cy + tickOuterR * sin(angleRad)
        drawLine(
            color = InkBrown,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) faceR * 0.016f else faceR * 0.008f,
        )
    }

    val labelRadius = tickOuterR - majorLen - faceR * 0.10f
    val labelPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 31, 20, 10)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.13f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }
    drawIntoCanvas { canvas ->
        var v = minValue
        while (v <= maxValue + 0.001f) {
            val t = (v - minValue) / totalRange
            val angleDeg = SP_START_ANGLE_DEG + t * SP_SWEEP_DEG
            val angleRad = (angleDeg * PI / 180.0).toFloat()
            val lx = cx + labelRadius * cos(angleRad)
            val ly = cy + labelRadius * sin(angleRad) + labelPaint.textSize / 3f
            canvas.nativeCanvas.drawText(v.toInt().toString(), lx, ly, labelPaint)
            v += majorStep
        }
    }

    val titlePaint = Paint().apply {
        color = android.graphics.Color.argb(255, 31, 20, 10)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.14f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        letterSpacing = 0.04f
    }
    val unitPaint = Paint().apply {
        color = android.graphics.Color.argb(220, 80, 50, 20)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.085f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(title, cx, cy - faceR * 0.18f - 30f, titlePaint)
        canvas.nativeCanvas.drawText(unit, cx, cy - faceR * 0.06f - 30f, unitPaint)
    }

    val windowW = faceR * 0.42f
    val windowH = faceR * 0.20f
    val windowTop = cy + faceR * 0.32f
    val windowLeft = cx - windowW / 2f
    drawRoundRect(
        color = WindowBg,
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(faceR * 0.025f),
    )
    drawRoundRect(
        color = BrassMid,
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(faceR * 0.025f),
        style = Stroke(width = faceR * 0.010f),
    )
    val displayPaint = Paint().apply {
        color = WindowAmber
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.17f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    drawIntoCanvas { canvas ->
        val baseline = windowTop + windowH / 2f + displayPaint.textSize / 3f
        canvas.nativeCanvas.drawText(displayText, cx, baseline, displayPaint)
    }

    val needleT = ((needleValue - minValue) / totalRange).coerceIn(0f, 1f)
    val needleAngleDeg = SP_START_ANGLE_DEG + needleT * SP_SWEEP_DEG
    val needleLen = faceR * 0.78f
    val tailLen = faceR * 0.16f
    val baseHalf = faceR * 0.020f
    val bellyHalf = faceR * 0.034f
    val bellyPos = needleLen * 0.18f
    val tipBaseHalf = faceR * 0.005f
    val tipStart = needleLen * 0.94f

    val needlePath = Path().apply {
        moveTo(cx - tailLen, cy)
        lineTo(cx - tailLen * 0.6f, cy - baseHalf * 0.9f)
        lineTo(cx, cy - baseHalf)
        lineTo(cx + bellyPos, cy - bellyHalf)
        lineTo(cx + tipStart, cy - tipBaseHalf)
        lineTo(cx + needleLen, cy)
        lineTo(cx + tipStart, cy + tipBaseHalf)
        lineTo(cx + bellyPos, cy + bellyHalf)
        lineTo(cx, cy + baseHalf)
        lineTo(cx - tailLen * 0.6f, cy + baseHalf * 0.9f)
        close()
    }

    rotate(degrees = needleAngleDeg, pivot = Offset(cx, cy)) {
        drawPath(needlePath, color = Color(0xFF0F0905))
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BrassLight, BrassDark),
            center = Offset(cx - faceR * 0.03f, cy - faceR * 0.03f),
            radius = faceR * 0.10f,
        ),
        radius = faceR * 0.075f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = BrassShadow,
        radius = faceR * 0.075f,
        center = Offset(cx, cy),
        style = Stroke(width = faceR * 0.006f),
    )
    // drawCircle(
    //     color = Color(0xFF0A0604),
    //     radius = faceR * 0.025f,
    //     center = Offset(cx, cy),
    // )
}
