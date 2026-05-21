package com.cardash

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sqrt

private const val GRAVITY = 9.81f

// Low-pass smoothing applied to each raw sample (0..1, higher = snappier, noisier).
private const val EMA_ALPHA = 0.18f

/**
 * Live G-force readings derived from the device accelerometer.
 *
 * [lateralG] is positive to the car's right (right-hand corner) and [longitudinalG]
 * is positive under forward acceleration (throttle), negative under braking — so the
 * value pair is the acceleration vector the car is experiencing.
 *
 * Assumes the phone is dash-mounted in portrait with the screen's horizontal axis
 * aligned with the car's left/right axis. Any forward/backward or sideways *tilt* is
 * corrected automatically from the gravity vector; only yaw (rotating the phone flat
 * about the vertical) would mis-map the axes. If the readings ever feel mirrored for a
 * given mount, flip the sign in [onSample] where [lateralG]/[longitudinalG] are set.
 */
@Stable
class GForceState {
    var lateralG by mutableFloatStateOf(0f)
        private set
    var longitudinalG by mutableFloatStateOf(0f)
        private set

    /** Largest resultant |G| seen since the last [resetPeak]. */
    var peakG by mutableFloatStateOf(0f)
        private set

    fun resetPeak() {
        peakG = 0f
    }

    /**
     * @param a linear acceleration in the device frame (gravity already removed), m/s²
     * @param g gravity vector in the device frame, m/s² (points "up", away from earth)
     */
    internal fun onSample(a: FloatArray, g: FloatArray) {
        val gMag = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2])
        if (gMag < 1e-3f) return
        // Unit "up" axis in device coordinates.
        val ux = g[0] / gMag
        val uy = g[1] / gMag
        val uz = g[2] / gMag

        // Drop the vertical part of the acceleration — keep what's in the ground plane.
        val aDotU = a[0] * ux + a[1] * uy + a[2] * uz
        val hx = a[0] - aDotU * ux
        val hy = a[1] - aDotU * uy
        val hz = a[2] - aDotU * uz

        // Car's lateral axis = device +X projected onto the ground plane (X·up = ux).
        var lx = 1f - ux * ux
        var ly = -ux * uy
        var lz = -ux * uz
        val lMag = sqrt(lx * lx + ly * ly + lz * lz)
        if (lMag < 1e-3f) return // phone lying flat: lateral axis is undefined
        lx /= lMag; ly /= lMag; lz /= lMag

        // Forward axis = up × lateral (right-handed, points toward the car's nose).
        val fx = uy * lz - uz * ly
        val fy = uz * lx - ux * lz
        val fz = ux * ly - uy * lx

        val lateral = (hx * lx + hy * ly + hz * lz) / GRAVITY
        val longitudinal = (hx * fx + hy * fy + hz * fz) / GRAVITY

        lateralG += (lateral - lateralG) * EMA_ALPHA
        longitudinalG += (longitudinal - longitudinalG) * EMA_ALPHA

        val mag = sqrt(lateralG * lateralG + longitudinalG * longitudinalG)
        if (mag > peakG) peakG = mag
    }
}

/**
 * Provides [GForceState] backed by the accelerometer. Listeners are registered on
 * ON_RESUME and torn down on ON_PAUSE, so the sensor only runs while the dashboard is
 * in the foreground — mirroring how GPS is released in the background.
 */
@Composable
fun rememberGForce(): GForceState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gForce = remember { GForceState() }

    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val linearAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gravity = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

        // Latest device-frame vectors; default gravity points up (+Y) until the first fix.
        val g = floatArrayOf(0f, GRAVITY, 0f)
        val a = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY -> System.arraycopy(event.values, 0, g, 0, 3)
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        System.arraycopy(event.values, 0, a, 0, 3)
                        gForce.onSample(a, g)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    linearAccel?.let {
                        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                    }
                    gravity?.let {
                        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> sensorManager?.unregisterListener(listener)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
        }
    }

    return gForce
}
