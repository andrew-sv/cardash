package com.cardash

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

// Full-scale deflection of the G plot; the outer ring is this many G.
private const val MAX_SCALE_G = 1.5f

// Reference rings drawn inside the plot, in G.
private val SCALE_RINGS = floatArrayOf(0.5f, 1.0f)

private val GCream = Color(0xFFEDE6D6)
private val GCreamDim = Color(0xFF6E6A60)
private val GLive = Color(0xFFFF5A36)
private val GPeak = Color(0xFFE6A23C)

/**
 * GR Yaris–style face: an analog speedometer up top and a live two-axis G-meter below.
 * The G dot tracks the car's acceleration vector (right turn → right, throttle → up,
 * braking → down); an amber ring holds the peak |G|. Tap the G-meter to clear the peak.
 */
@Composable
fun GMeterFace(state: DashboardState, settings: DashboardSettings) {
    val gForce = rememberGForce()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val panelSize = minOf(maxWidth - 16.dp, (maxHeight - 24.dp) / 2)
        val maxSpeed = settings.maxSpeed.toFloat()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GaugePanel(size = panelSize) {
                Gauge(
                    value = state.speedKmh,
                    minValue = 0f,
                    maxValue = maxSpeed,
                    title = "SPEED",
                    unit = "km/h",
                    majorStep = niceMajorStep(maxSpeed, targetMajors = 10),
                    minorPerMajor = 3,
                    zones = listOf(
                        GaugeZone(maxSpeed * 0.65f, maxSpeed * 0.8f, Color(0xFFE6A23C)),
                        GaugeZone(maxSpeed * 0.8f, maxSpeed, Color(0xFFE54A2E)),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            GaugePanel(
                size = panelSize,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { gForce.resetPeak() },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Read sensor state inside the draw scope so updates only redraw.
                    drawGMeter(
                        lateralG = gForce.lateralG,
                        longitudinalG = gForce.longitudinalG,
                        peakG = gForce.peakG,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGMeter(lateralG: Float, longitudinalG: Float, peakG: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outerR = min(cx, cy) * 0.98f
    val bezelW = outerR * 0.10f
    val faceR = outerR - bezelW
    val plotR = faceR * 0.90f // outer ring (= MAX_SCALE_G) sits just inside the bezel

    // --- Bezel + dark face, matching the round Gauge primitive ---
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF3A3A3E), Color(0xFF1F1F22), Color(0xFF1A1A1D),
                Color(0xFF2C2C30), Color(0xFF6A6A70), Color(0xFF8A8A90),
                Color(0xFF6A6A70), Color(0xFF2C2C30), Color(0xFF1A1A1D),
                Color(0xFF1F1F22), Color(0xFF3A3A3E),
            ),
            center = Offset(cx, cy),
        ),
        radius = outerR,
        center = Offset(cx, cy),
    )
    drawCircle(color = Color(0xFF050507), radius = outerR - bezelW * 0.18f, center = Offset(cx, cy))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1A1A1C), Color(0xFF050506)),
            center = Offset(cx - faceR * 0.25f, cy - faceR * 0.35f),
            radius = faceR * 1.3f,
        ),
        radius = faceR,
        center = Offset(cx, cy),
    )

    // --- Crosshair ---
    drawLine(GCreamDim, Offset(cx - plotR, cy), Offset(cx + plotR, cy), strokeWidth = faceR * 0.006f)
    drawLine(GCreamDim, Offset(cx, cy - plotR), Offset(cx, cy + plotR), strokeWidth = faceR * 0.006f)

    // --- Reference rings ---
    SCALE_RINGS.forEach { g ->
        drawCircle(
            color = GCreamDim,
            radius = plotR * (g / MAX_SCALE_G),
            center = Offset(cx, cy),
            style = Stroke(width = faceR * 0.006f),
        )
    }
    drawCircle(
        color = GCream.copy(alpha = 0.55f),
        radius = plotR,
        center = Offset(cx, cy),
        style = Stroke(width = faceR * 0.010f),
    )

    // --- Ring value labels, just right of the upper vertical axis ---
    val ringLabelPaint = Paint().apply {
        color = android.graphics.Color.argb(200, 184, 176, 160)
        textAlign = Paint.Align.LEFT
        textSize = faceR * 0.06f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    drawIntoCanvas { canvas ->
        SCALE_RINGS.forEach { g ->
            val r = plotR * (g / MAX_SCALE_G)
            canvas.nativeCanvas.drawText(
                String.format(Locale.US, "%.1f", g),
                cx + faceR * 0.03f,
                cy - r + ringLabelPaint.textSize / 3f,
                ringLabelPaint,
            )
        }
    }

    // --- Peak |G| ring + label (left of the upper vertical axis) ---
    if (peakG > 0.05f) {
        val pr = (plotR * (peakG / MAX_SCALE_G)).coerceAtMost(plotR)
        drawCircle(
            color = GPeak.copy(alpha = 0.75f),
            radius = pr,
            center = Offset(cx, cy),
            style = Stroke(width = faceR * 0.012f),
        )
        val peakLabelPaint = Paint().apply {
            color = android.graphics.Color.argb(230, 230, 162, 60)
            textAlign = Paint.Align.RIGHT
            textSize = faceR * 0.06f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                String.format(Locale.US, "%.2f", peakG),
                cx - faceR * 0.03f,
                cy - pr + peakLabelPaint.textSize / 3f,
                peakLabelPaint,
            )
        }
    }

    // --- Live G dot (clamped to the outer ring) ---
    val mag = sqrt(lateralG * lateralG + longitudinalG * longitudinalG)
    val clamp = if (mag > MAX_SCALE_G) MAX_SCALE_G / mag else 1f
    val dx = cx + (lateralG * clamp / MAX_SCALE_G) * plotR
    val dy = cy - (longitudinalG * clamp / MAX_SCALE_G) * plotR

    drawLine(GLive.copy(alpha = 0.40f), Offset(cx, cy), Offset(dx, dy), strokeWidth = faceR * 0.012f)
    drawCircle(GLive.copy(alpha = 0.22f), radius = faceR * 0.10f, center = Offset(dx, dy))
    drawCircle(GLive, radius = faceR * 0.05f, center = Offset(dx, dy))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = faceR * 0.018f, center = Offset(dx, dy))

    // --- Axis direction labels, inside the outer ring ---
    val axisPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 184, 176, 160)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.075f
        isAntiAlias = true
        letterSpacing = 0.12f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("ACCEL", cx, cy - faceR * 0.95f, axisPaint)
        canvas.nativeCanvas.drawText("BRAKE", cx, cy + faceR * 1.00f, axisPaint)
        canvas.nativeCanvas.drawText("L", cx - faceR * 0.96f, cy + axisPaint.textSize / 3f, axisPaint)
        canvas.nativeCanvas.drawText("R", cx + faceR * 0.96f, cy + axisPaint.textSize / 3f, axisPaint)
    }

    // --- Title + live value, grouped in the lower half ---
    val titlePaint = Paint().apply {
        color = android.graphics.Color.argb(220, 184, 176, 160)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.085f
        isAntiAlias = true
        letterSpacing = 0.22f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val valuePaint = Paint().apply {
        color = android.graphics.Color.argb(255, 245, 240, 228)
        textAlign = Paint.Align.CENTER
        textSize = faceR * 0.20f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val unitPaint = Paint().apply {
        color = android.graphics.Color.argb(210, 184, 176, 160)
        textAlign = Paint.Align.LEFT
        textSize = faceR * 0.09f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    // Subtle dark pill behind the value so it stays legible over the crosshair/rings.
    val valueCy = cy + faceR * 0.62f
    val pillW = faceR * 0.62f
    val pillH = faceR * 0.30f
    drawRoundRect(
        color = Color(0xFF050506).copy(alpha = 0.72f),
        topLeft = Offset(cx - pillW / 2f, valueCy - pillH / 2f),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(pillH / 2f),
    )

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("G-FORCE", cx, cy - faceR * 0.70f, titlePaint)
        val valueText = String.format(Locale.US, "%.2f", mag)
        val baseline = valueCy + valuePaint.textSize / 3f
        canvas.nativeCanvas.drawText(valueText, cx - faceR * 0.04f, baseline, valuePaint)
        val half = valuePaint.measureText(valueText) / 2f
        canvas.nativeCanvas.drawText("g", cx - faceR * 0.04f + half + faceR * 0.03f, baseline, unitPaint)
    }
}
