package com.cardash

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardSettings(
    val maxSpeed: Int = DEFAULT_MAX_SPEED,
    val maxAltitude: Int = DEFAULT_MAX_ALTITUDE,
) {
    companion object {
        const val DEFAULT_MAX_SPEED = 200
        const val DEFAULT_MAX_ALTITUDE = 2500
    }
}

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("cardash_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        DashboardSettings(
            maxSpeed = prefs.getInt(KEY_MAX_SPEED, DashboardSettings.DEFAULT_MAX_SPEED),
            maxAltitude = prefs.getInt(KEY_MAX_ALTITUDE, DashboardSettings.DEFAULT_MAX_ALTITUDE),
        )
    )
    val settings: StateFlow<DashboardSettings> = _settings.asStateFlow()

    fun update(maxSpeed: Int, maxAltitude: Int) {
        prefs.edit()
            .putInt(KEY_MAX_SPEED, maxSpeed)
            .putInt(KEY_MAX_ALTITUDE, maxAltitude)
            .apply()
        _settings.value = DashboardSettings(maxSpeed = maxSpeed, maxAltitude = maxAltitude)
    }

    companion object {
        private const val KEY_MAX_SPEED = "max_speed"
        private const val KEY_MAX_ALTITUDE = "max_altitude"
    }
}
