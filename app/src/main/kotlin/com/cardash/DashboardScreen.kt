package com.cardash

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val Background = Color(0xFF0A0A0B)
private val Accent = Color(0xFF7BB661)
private val MutedText = Color(0xFF9A9A9A)

enum class DashboardFace(val label: String) {
    ANALOG("Analog"),
    DIGITAL("Digital"),
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var face by rememberSaveable { mutableStateOf(DashboardFace.ANALOG) }
    var showFaceDialog by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.start() }

    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background),
    ) {
        TopBar(onSettingsClick = { showFaceDialog = true })
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (face) {
                DashboardFace.ANALOG -> AnalogFace(state)
                DashboardFace.DIGITAL -> DigitalFace(state)
            }
        }
        BottomBar(state = state)
    }

    if (showFaceDialog) {
        FaceSelectionDialog(
            current = face,
            onSelect = {
                face = it
                showFaceDialog = false
            },
            onDismiss = { showFaceDialog = false },
        )
    }
}

@Composable
private fun TopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MutedText,
                modifier = Modifier.size(22.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AnalogFace(state: DashboardState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val panelSize = minOf(maxWidth - 16.dp, (maxHeight - 24.dp) / 2)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GaugePanel(size = panelSize) {
                Gauge(
                    value = state.speedKmh,
                    minValue = 0f,
                    maxValue = 200f,
                    title = "SPEED",
                    unit = "km/h",
                    majorStep = 20f,
                    minorPerMajor = 3,
                    zones = listOf(
                        GaugeZone(110f, 140f, Color(0xFF7BB661)),
                        GaugeZone(140f, 170f, Color(0xFFE6A23C)),
                        GaugeZone(170f, 200f, Color(0xFFE54A2E)),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            GaugePanel(size = panelSize) {
                Gauge(
                    value = state.altitudeM,
                    minValue = 0f,
                    maxValue = 2500f,
                    title = "ALTITUDE",
                    unit = "m",
                    majorStep = 250f,
                    minorPerMajor = 4,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun GaugePanel(
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val cornerRadius = 22.dp
    val screwInset = 14.dp
    val screwRadius = 7.dp
    val gaugeInset = 18.dp

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
                        colors = listOf(Color(0xFF202023), Color(0xFF131316)),
                    ),
                    cornerRadius = CornerRadius(cr),
                )
                drawRoundRect(
                    color = Color(0xFF2D2D31),
                    cornerRadius = CornerRadius(cr),
                    style = Stroke(width = 1.dp.toPx()),
                )

                listOf(
                    Offset(si + sr, si + sr),
                    Offset(w - si - sr, si + sr),
                    Offset(si + sr, h - si - sr),
                    Offset(w - si - sr, h - si - sr),
                ).forEach { center ->
                    drawScrew(center, sr)
                }
            }
            .padding(gaugeInset),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScrew(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF36363A), Color(0xFF0E0E10)),
            center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color(0xFF050506),
        radius = radius * 0.85f,
        center = center,
    )
    drawCircle(
        color = Color(0xFF1F1F22),
        radius = radius * 0.85f,
        center = center,
        style = Stroke(width = radius * 0.08f),
    )
    drawLine(
        color = Color(0xFF000000),
        start = Offset(center.x - radius * 0.55f, center.y),
        end = Offset(center.x + radius * 0.55f, center.y),
        strokeWidth = radius * 0.18f,
    )
    drawLine(
        color = Color(0xFF000000),
        start = Offset(center.x, center.y - radius * 0.55f),
        end = Offset(center.x, center.y + radius * 0.55f),
        strokeWidth = radius * 0.18f,
    )
    drawCircle(
        color = Color(0xFF55555A).copy(alpha = 0.55f),
        radius = radius * 0.22f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
    )
}

@Composable
private fun DigitalFace(state: DashboardState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        ReadingBlock(
            label = "SPEED",
            value = state.speedKmh?.let { "%.0f".format(it) } ?: "--",
            unit = "km/h",
            color = Color.White,
        )
        ReadingBlock(
            label = "ALTITUDE",
            value = state.altitudeM?.let { "%.0f".format(it) } ?: "--",
            unit = "m",
            color = Color(0xFF1EC8E6),
        )
    }
}

@Composable
private fun ReadingBlock(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 18.sp, letterSpacing = 4.sp)
        Text(
            value,
            color = color,
            fontSize = 180.sp,
            fontWeight = FontWeight.Light,
        )
        Text(unit, color = color.copy(alpha = 0.7f), fontSize = 24.sp)
    }
}

@Composable
private fun FaceSelectionDialog(
    current: DashboardFace,
    onSelect: (DashboardFace) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dashboard face") },
        text = {
            Column {
                DashboardFace.entries.forEach { f ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(f) }
                            .padding(vertical = 6.dp),
                    ) {
                        RadioButton(selected = current == f, onClick = { onSelect(f) })
                        Spacer(Modifier.width(8.dp))
                        Text(f.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun BottomBar(state: DashboardState) {
    val utc by produceState(initialValue = currentUtcTime()) {
        while (true) {
            value = currentUtcTime()
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text("GPS", color = Accent, fontSize = 11.sp, letterSpacing = 1.sp)
            if (state.latitude != null && state.longitude != null) {
                Text(formatLat(state.latitude), color = Color.White, fontSize = 13.sp)
                Text(formatLon(state.longitude), color = Color.White, fontSize = 13.sp)
            } else {
                Text("--", color = Color.White, fontSize = 13.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("UTC", color = Accent, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(
                utc,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun currentUtcTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

private fun formatLat(lat: Double): String =
    "%.4f° %s".format(Locale.US, abs(lat), if (lat >= 0) "N" else "S")

private fun formatLon(lon: Double): String =
    "%.4f° %s".format(Locale.US, abs(lon), if (lon >= 0) "E" else "W")
