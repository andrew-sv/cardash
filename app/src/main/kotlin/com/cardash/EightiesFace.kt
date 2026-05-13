package com.cardash

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val E_START_ANGLE_DEG = 135f
private const val E_SWEEP_DEG = 270f

private val EBackground = Color(0xFF0A100C)
private val EFaceDark = Color(0xFF0E140F)
private val EPhosphor = Color(0xFFB6DC8E)
private val EPhosphorDim = Color(0xFF6F8E5C)
private val ETickMajor = Color(0xFFEAEFE0)
private val ETickMinor = Color(0xFF9CA68F)
private val ENeedleCream = Color(0xFFEDE3A8)
private val ELcdLime = Color(0xFF5FE850)
private val ELcdLimeDim = Color(0xFF22381A)
private val ELcdBg = Color(0xFF080D06)

private val GearRpmPerKmh = floatArrayOf(100.51f, 54.35f, 34.5f, 25f, 22.98f, 18.13f)
private const val MinRpm = 800f
private const val DownshiftRpm = 1500f
private const val ModerateUpshiftRpm = 2500f
private const val FastUpshiftRpm = 4000f
private const val FastAccelThresholdMps2 = 2.0f
private const val AccelEmaAlpha = 0.4f

private fun rpmFor(gear: Int, speedKmh: Float): Float =
    GearRpmPerKmh[gear - 1] * speedKmh.coerceAtLeast(0f)

private fun resolveGear(startGear: Int, speedKmh: Float, accelMps2: Float): Int {
    var gear = startGear.coerceIn(1, 6)
    val upshiftRpm = if (accelMps2 >= FastAccelThresholdMps2) FastUpshiftRpm else ModerateUpshiftRpm
    while (gear < 6 && rpmFor(gear, speedKmh) >= upshiftRpm) gear++
    while (gear > 1 && rpmFor(gear, speedKmh) <= DownshiftRpm) {
        if (rpmFor(gear - 1, speedKmh) >= upshiftRpm) break
        gear--
    }
    return gear
}

@Composable
fun EightiesTachoFace(state: DashboardState, settings: DashboardSettings) {
    var gear by rememberSaveable { mutableIntStateOf(1) }
    var smoothedAccel by remember { mutableFloatStateOf(0f) }
    var lastSample by remember { mutableStateOf<Pair<Float, Long>?>(null) }

    LaunchedEffect(state.speedKmh) {
        val s = state.speedKmh ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        val prev = lastSample
        if (prev != null) {
            val dt = (now - prev.second) / 1000f
            if (dt > 0.05f) {
                val instant = (s - prev.first) / 3.6f / dt
                smoothedAccel = smoothedAccel * (1f - AccelEmaAlpha) + instant * AccelEmaAlpha
            }
        }
        lastSample = s to now
        gear = resolveGear(gear, s, smoothedAccel)
    }

    val displayRpm = state.speedKmh?.let {
        maxOf(rpmFor(gear, it), MinRpm)
    }
    val rpmDialValue = displayRpm?.div(100f)
    val gearText = if (state.speedKmh == null) "--" else gear.toString()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EBackground),
        contentAlignment = Alignment.Center,
    ) {
        val panelSize = minOf(maxWidth - 8.dp, (maxHeight - 12.dp) / 2)
        val maxSpeed = settings.maxSpeed.toFloat()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.size(panelSize), contentAlignment = Alignment.Center) {
                EightiesGauge(
                    value = state.speedKmh,
                    minValue = 0f,
                    maxValue = maxSpeed,
                    title = "SPEED",
                    unit = "km/h",
                    majorStep = niceMajorStep(maxSpeed, targetMajors = 10),
                    minorPerMajor = 3,
                    zones = listOf(
                        GaugeZone(maxSpeed * 0.5f, maxSpeed * 0.65f, Color(0xFF6CC55B)),
                        GaugeZone(maxSpeed * 0.65f, maxSpeed * 0.8f, Color(0xFFD8B73C)),
                        GaugeZone(maxSpeed * 0.8f, maxSpeed, Color(0xFFD84A2A)),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(modifier = Modifier.size(panelSize), contentAlignment = Alignment.Center) {
                EightiesGauge(
                    value = rpmDialValue,
                    minValue = 0f,
                    maxValue = 70f,
                    title = "RPM",
                    unit = "x100",
                    majorStep = 10f,
                    minorPerMajor = 4,
                    zones = listOf(
                        GaugeZone(60f, 70f, Color(0xFFD84A2A)),
                    ),
                    displayTextOverride = gearText,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun EightiesFace(state: DashboardState, settings: DashboardSettings) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EBackground),
        contentAlignment = Alignment.Center,
    ) {
        val panelSize = minOf(maxWidth - 8.dp, (maxHeight - 12.dp) / 2)
        val maxSpeed = settings.maxSpeed.toFloat()
        val maxAltitude = settings.maxAltitude.toFloat()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.size(panelSize), contentAlignment = Alignment.Center) {
                EightiesGauge(
                    value = state.speedKmh,
                    minValue = 0f,
                    maxValue = maxSpeed,
                    title = "SPEED",
                    unit = "km/h",
                    majorStep = niceMajorStep(maxSpeed, targetMajors = 10),
                    minorPerMajor = 3,
                    zones = listOf(
                        GaugeZone(maxSpeed * 0.5f, maxSpeed * 0.65f, Color(0xFF6CC55B)),
                        GaugeZone(maxSpeed * 0.65f, maxSpeed * 0.8f, Color(0xFFD8B73C)),
                        GaugeZone(maxSpeed * 0.8f, maxSpeed, Color(0xFFD84A2A)),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(modifier = Modifier.size(panelSize), contentAlignment = Alignment.Center) {
                EightiesGauge(
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

@Composable
private fun EightiesGauge(
    value: Float?,
    minValue: Float,
    maxValue: Float,
    title: String,
    unit: String,
    majorStep: Float,
    minorPerMajor: Int = 4,
    zones: List<GaugeZone> = emptyList(),
    valueFormatter: (Float) -> String = { "%.0f".format(it) },
    displayTextOverride: String? = null,
    modifier: Modifier = Modifier,
) {
    val target = (value ?: minValue).coerceIn(minValue, maxValue)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "eightiesneedle",
    )
    val displayText = displayTextOverride
        ?: if (value == null) "--" else valueFormatter(value)
    val context = LocalContext.current
    val digitalTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.dseg7_classic)
    }

    Canvas(modifier = modifier) {
        drawEightiesGauge(
            needleValue = animated,
            displayText = displayText,
            minValue = minValue,
            maxValue = maxValue,
            title = title,
            unit = unit,
            majorStep = majorStep,
            minorPerMajor = minorPerMajor,
            zones = zones,
            digitalTypeface = digitalTypeface,
        )
    }
}

private fun DrawScope.drawEightiesGauge(
    needleValue: Float,
    displayText: String,
    minValue: Float,
    maxValue: Float,
    title: String,
    unit: String,
    majorStep: Float,
    minorPerMajor: Int,
    zones: List<GaugeZone>,
    digitalTypeface: Typeface?,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val faceR = minOf(cx, cy) * 0.96f
    val totalRange = maxValue - minValue

    drawCircle(
        color = EFaceDark,
        radius = faceR,
        center = Offset(cx, cy),
    )

    val zoneStrokeWidth = faceR * 0.045f
    val zoneRadius = faceR - zoneStrokeWidth * 0.6f
    zones.forEach { zone ->
        val startT = ((zone.from - minValue) / totalRange).coerceIn(0f, 1f)
        val endT = ((zone.to - minValue) / totalRange).coerceIn(0f, 1f)
        val zoneStart = E_START_ANGLE_DEG + startT * E_SWEEP_DEG
        val zoneSweep = (endT - startT) * E_SWEEP_DEG
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

    val tickOuterR = faceR - zoneStrokeWidth * 1.7f
    val majorLen = faceR * 0.11f
    val minorLen = faceR * 0.055f
    val majorCount = (totalRange / majorStep).toInt()
    val totalSteps = majorCount * (minorPerMajor + 1)
    for (i in 0..totalSteps) {
        val isMajor = i % (minorPerMajor + 1) == 0
        val t = i.toFloat() / totalSteps
        val angleDeg = E_START_ANGLE_DEG + t * E_SWEEP_DEG
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val len = if (isMajor) majorLen else minorLen
        val sx = cx + (tickOuterR - len) * cos(angleRad)
        val sy = cy + (tickOuterR - len) * sin(angleRad)
        val ex = cx + tickOuterR * cos(angleRad)
        val ey = cy + tickOuterR * sin(angleRad)
        drawLine(
            color = if (isMajor) ETickMajor else ETickMinor,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) faceR * 0.022f else faceR * 0.010f,
        )
    }

    val labelRadius = tickOuterR - majorLen - faceR * 0.10f
    val labelPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 234, 239, 224)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.13f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    drawIntoCanvas { canvas ->
        var v = minValue
        while (v <= maxValue + 0.001f) {
            val t = (v - minValue) / totalRange
            val angleDeg = E_START_ANGLE_DEG + t * E_SWEEP_DEG
            val angleRad = (angleDeg * PI / 180.0).toFloat()
            val lx = cx + labelRadius * cos(angleRad)
            val ly = cy + labelRadius * sin(angleRad) + labelPaint.textSize / 3f
            canvas.nativeCanvas.drawText(v.toInt().toString(), lx, ly, labelPaint)
            v += majorStep
        }
    }

    val titlePaint = Paint().apply {
        color = android.graphics.Color.argb(255, 182, 220, 142)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.13f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        letterSpacing = 0.08f
    }
    val unitPaint = Paint().apply {
        color = android.graphics.Color.argb(220, 111, 142, 92)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.085f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(title, cx, cy - faceR * 0.18f - 30f, titlePaint)
        canvas.nativeCanvas.drawText(unit, cx, cy - faceR * 0.06f - 30f, unitPaint)
    }

    val windowW = faceR * 0.40f
    val windowH = faceR * 0.22f
    val windowTop = cy + faceR * 0.32f
    val windowLeft = cx - windowW / 2f
    drawRoundRect(
        color = ELcdBg,
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(faceR * 0.02f),
    )
    drawRoundRect(
        color = ELcdLimeDim,
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(faceR * 0.02f),
        style = Stroke(width = faceR * 0.006f),
    )
    val displayPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 95, 232, 80)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.14f
        isAntiAlias = true
        typeface = digitalTypeface ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    drawIntoCanvas { canvas ->
        val baseline = windowTop + windowH / 2f + displayPaint.textSize / 2f
        canvas.nativeCanvas.drawText(displayText, cx, baseline, displayPaint)
    }

    val needleT = ((needleValue - minValue) / totalRange).coerceIn(0f, 1f)
    val needleAngleDeg = E_START_ANGLE_DEG + needleT * E_SWEEP_DEG
    val needleLen = faceR * 0.80f
    val tailLen = faceR * 0.12f
    val baseHalf = faceR * 0.022f
    val tipHalf = faceR * 0.006f

    val needlePath = Path().apply {
        moveTo(cx - tailLen, cy - baseHalf * 0.6f)
        lineTo(cx, cy - baseHalf)
        lineTo(cx + needleLen, cy - tipHalf)
        lineTo(cx + needleLen, cy + tipHalf)
        lineTo(cx, cy + baseHalf)
        lineTo(cx - tailLen, cy + baseHalf * 0.6f)
        close()
    }

    rotate(degrees = needleAngleDeg, pivot = Offset(cx, cy)) {
        drawPath(needlePath, color = ENeedleCream)
    }

    drawCircle(
        color = Color(0xFF1A2218),
        radius = faceR * 0.05f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = EPhosphor,
        radius = faceR * 0.05f,
        center = Offset(cx, cy),
        style = Stroke(width = faceR * 0.005f),
    )
}
