# Cardash — developer notes

Single-module Android phone app. Portrait-locked car dashboard that shows GPS-derived speed and altitude.

Target hardware: Samsung Galaxy Note 10 Lite, Android 13. `minSdk = 33`.

## Toolchain

- Gradle 8.10.2 (wrapper), AGP 8.7.2, Kotlin 2.0.21, JDK 17
- Jetpack Compose, Compose BOM `2024.10.01`, Material3 `1.3.1`
- `play-services-location` 21.3.0 (FusedLocationProviderClient)
- Single `:app` module, namespace + applicationId both `com.cardash`
- Source root is `app/src/main/kotlin` (not `java/`)

## Files

| File | Role |
|---|---|
| `MainActivity.kt` | `ComponentActivity`, sets `FLAG_KEEP_SCREEN_ON`, hosts `DashboardScreen`. Starts/stops the location stream in `onStart`/`onStop`. |
| `DashboardScreen.kt` | Top bar (gear/place icons), face switcher, bottom status bar (GPS coords + live UTC). Permission flow lives here. |
| `Gauge.kt` | Pure `Canvas` skeuomorphic round gauge. Configurable range, tick density, danger zones. |
| `DashboardViewModel.kt` | Owns `StateFlow<DashboardState>`. `LocationCallback` translates m/s → km/h and copies altitude/lat/lon into state. |

## Architecture

Single source of truth: `DashboardViewModel.state: StateFlow<DashboardState>`.

```
FusedLocationProviderClient → LocationCallback → DashboardState
                                                       ↓
                                       collectAsStateWithLifecycle
                                                       ↓
                                          AnalogFace / DigitalFace
                                                       ↓
                                                BottomBar
```

The location request is `PRIORITY_HIGH_ACCURACY` at 1 Hz with a 500 ms minimum interval. Updates are stopped in `onStop` to keep the app from holding GPS in the background.

When `Location.hasSpeed()` / `hasAltitude()` is false, the previous value is preserved (no `null` flicker on a single bad fix). `latitude`/`longitude` are always overwritten from the latest fix.

## Adding a new dashboard face

The face system is intentionally tiny. To add one:

1. Add an enum entry in `DashboardScreen.kt`:
   ```kotlin
   enum class DashboardFace(val label: String) {
       ANALOG("Analog"),
       DIGITAL("Digital"),
       MY_NEW_FACE("Heads-up"),  // ← here
   }
   ```
2. Write a `@Composable private fun MyNewFace(state: DashboardState) { ... }` in the same file.
3. Add one `when` branch in `DashboardScreen()`:
   ```kotlin
   when (face) {
       DashboardFace.ANALOG -> AnalogFace(state)
       DashboardFace.DIGITAL -> DigitalFace(state)
       DashboardFace.MY_NEW_FACE -> MyNewFace(state)
   }
   ```

The face picker dialog renders all entries from `DashboardFace.entries` automatically. Selection persists across process death via `rememberSaveable`.

## Gauge composable

`Gauge(value, minValue, maxValue, title, unit, majorStep, minorPerMajor, zones)` is the reusable round-dial primitive. The needle animates with `animateFloatAsState` (600 ms tween). When `value == null` (no GPS fix yet) the needle sits at `minValue` and the digital window shows `--`.

Sweep is fixed: 270° from `START_ANGLE_DEG = 135` (lower-left, 7-o'clock) clockwise to lower-right.

`zones` are colored arcs drawn just inside the tick ring — used for the speed gauge's green/orange/red bands.

## Conventions

- Background colour: `Color(0xFF0A0A0B)` (near-black).
- Cream tick/needle colour: `Color(0xFFEDE6D6)`.
- Accent green for status labels: `Color(0xFF7BB661)`.
- Distances/speeds in metric only (km/h, m). No imperial toggle yet.
- All `String.format` calls use `Locale.US` so coords always render with `.` decimals.

## Permissions

- `ACCESS_FINE_LOCATION` (runtime, requested in `DashboardScreen` on first composition)
- `ACCESS_COARSE_LOCATION` (declared, mostly so the system shows the right rationale UI)

The app does **not** request background location — GPS only runs while the activity is started.

## Build / install

```bash
./gradlew :app:assembleDebug              # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug               # over USB
adb shell am start -n com.cardash/.MainActivity
```
