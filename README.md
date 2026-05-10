# Cardash

A car dashboard app for an Android phone mounted in the car. Shows live speed (km/h) and altitude (m) from GPS.

## Status

Working baseline:
- Two switchable dashboard faces: **Analog** (skeuomorphic round gauges, 0–200 km/h with green/orange/red speed zones, 0–2500 m altitude) and **Digital** (large white speed + cyan altitude readout).
- Top-bar gear icon opens a face picker. Selection survives process death.
- GPS coordinates and live UTC clock in the bottom status bar.
- Portrait-locked, screen kept on while in foreground.

Not yet: barometric-pressure altitude smoothing, trip stats, settings beyond face selection.

## Devices

Built and tested against Samsung Galaxy Note 10 Lite on Android 13.

`minSdk = 33`, `targetSdk = 34`. Should run on any Android 13+ phone with GPS but only the Note 10 Lite is tested.

## Build

Requires JDK 17 and the Android SDK (platform `android-34`, build-tools).

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Install

Phone over USB (Developer Options + USB debugging on):

```bash
./gradlew :app:installDebug
```

Grant the **Location** permission when first prompted — without it both readouts stay at `--`.

## Project structure

```
app/src/main/kotlin/com/cardash/
  MainActivity.kt          Activity, theme, lifecycle wiring
  DashboardScreen.kt       Top bar + face switcher + bottom status bar
  Gauge.kt                 Analog round gauge (Canvas)
  DashboardViewModel.kt    FusedLocation source, StateFlow<DashboardState>
```
