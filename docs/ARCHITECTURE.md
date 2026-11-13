# Architecture Guide

Vesper follows a lightweight **MVVM** architecture designed for low-overhead cold starts, zero-recomposition animations, and complete offline resilience.

## Package Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Single-activity launcher host, edge-to-edge system bars,
│                                    # BroadcastReceiver for package install/remove, CompositionLocals
├── data/
│   ├── AppRepository.kt             # Installed launcher app queries, hollow monochrome outline bitmap
│   │                                # generator, SHA-256 disk PNG cache, ShortcutManager queries
│   ├── CalendarRepository.kt        # Next upcoming CalendarContract event lookup
│   ├── SettingsRepository.kt        # Jetpack DataStore persistence, cached weather JSON & backup JSON
│   └── WeatherRepository.kt         # Open-Meteo live forecast & geocoding client (HttpURLConnection)
├── model/
│   └── LauncherModels.kt            # Immutable UI state, AppInfo, AppFolder, AccentOption, WeatherInfo
├── notifications/
│   ├── LauncherNotificationListenerService.kt # Unread package counter for minimal notification dots
│   └── NotificationBadgeStore.kt    # Thread-safe StateFlow map of active badge counts
├── ui/
│   ├── components/
│   │   ├── FocusComponents.kt       # MindfulLaunchDialog, RenameAppDialog, HiddenAppsDialog, pickers
│   │   ├── PowerFocusDialogs.kt     # FolderEditorDialog, WeatherCityPickerDialog, FocusScheduleDialog,
│   │   │                            # MindfulInsightsDialog, BackupRestoreDialog, OnboardingWelcomeDialog
│   │   └── RetroComponents.kt       # FramedAppIcon, AppListRow, AlphabetScrubberRail, Favorites picker
│   ├── drawer/
│   │   └── AppDrawer.kt             # Command-K search, Quick Math card, Quick Actions, A-Z app list
│   ├── home/
│   │   ├── CoastalGradientOverlay.kt# Time-of-day sky wash + sunset illustration + depth bloom layers
│   │   ├── GyroscopeParallax.kt     # Lifecycle-bound TYPE_GYROSCOPE / TYPE_GRAVITY tilt state
│   │   ├── HeroWeatherEffects.kt    # Rain, storm, cloud, breeze & heat haze Canvas shaders
│   │   ├── HomeHeroComponents.kt    # HeroFocusTimerBar & expandable HomeFolderRow
│   │   ├── HomeScreen.kt            # Primary launcher canvas, gesture detectors, favorites & search pill
│   │   └── SynthwaveClockWidget.kt  # Neon-glow digital clock card with horizon spectrum beam
│   ├── settings/
│   │   ├── SettingsComponents.kt    # LauncherHealthStatusCard, RadioOptionDialog, toggles
│   │   └── SettingsScreen.kt        # Categorized Geist Settings screen
│   └── theme/
│       ├── Color.kt                 # Geist dark/light palettes, synthwave & coastal gradient tokens
│       ├── Theme.kt                 # NeonSunsetTheme, accent scheme builder, CompositionLocals
│       └── Type.kt                  # Bundled Inter & JetBrains Mono typography scales
├── util/
│   ├── FocusAndSearchUtils.kt       # Schedule window math, phone/URL quick-action detector
│   ├── LauncherHaptics.kt           # API-level aware View.performHapticFeedback wrapper
│   └── MathEvaluator.kt             # Safe recursive-descent inline calculator for drawer search
└── viewmodel/
    └── LauncherViewModel.kt         # Reactive state coordinator, focus ticker, app launch & mindful checks
```

## Key Design Decisions

### 1. Adaptive Monochrome Outline Pipeline (`AppRepository`)
- On background `Dispatchers.IO`, Vesper draws each `LauncherActivityInfo` icon onto a small `96×96` ARGB canvas, extracts perimeter edges, and hollows out solid fills so icons render as clean monochrome vector-style outlines.
- Each outline is saved as a compressed PNG in `cacheDir/outline_icons/` keyed by `sha256(componentKey)_v<versionCode>`. Subsequent cold boots read cached bitmaps directly without re-rasterizing `AdaptiveIconDrawable`s.

### 2. Zero-Recomposition Tilt Parallax (`GyroscopeParallax` & `CoastalGradientOverlay`)
- `rememberGyroscopeParallaxState(enabled)` listens to `Sensor.TYPE_GYROSCOPE` (integrated and damped smoothly via `GyroParallaxMath`) or falls back to high-pass filtered `Sensor.TYPE_GRAVITY` / `TYPE_ACCELEROMETER` when a hardware gyroscope is absent.
- Offsets are read strictly inside GPU `Modifier.graphicsLayer { ... }` lambdas across three depth planes (`16dp` background illustration, `24dp` solar bloom, `30dp` shoreline glow, `36dp` weather layer) so tilting the phone never triggers Compose recomposition or layout passes.

### 3. Hardware-Safe Haptics (`LauncherHaptics`)
- Instead of using `Vibrator` + `VIBRATE` permission, UI interactions invoke `View.performHapticFeedback(constant)` through `LauncherHaptics`.
- `LauncherHapticsResolver` maps semantic events (`DRAWER_OPEN_SWIPE`, `DRAWER_CLOSE_SWIPE`, `APP_LAUNCH`, `LONG_PRESS`, `SCRUBBER_TICK`, `TOGGLE_SWITCH`, `CONFIRM_ACTION`, `ERROR_FEEDBACK`, `SUBTLE_TAP`) to `HapticFeedbackConstants` (`GESTURE_START`, `GESTURE_END`, `CONFIRM`, `REJECT`, `CLOCK_TICK`, `CONTEXT_CLICK`, `LONG_PRESS`) with safe API 26–29 fallbacks.
- Settings updates propagate via `LocalLauncherHapticsEnabled` so every screen and modal honors user preferences immediately.
