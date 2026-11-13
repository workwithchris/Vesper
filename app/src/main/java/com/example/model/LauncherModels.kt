package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

enum class AppIconShapeProfile {
  SYSTEM_ADAPTIVE,
  CIRCULAR,
  ROUNDED_RECT,
  CUSTOM_SILHOUETTE,
}

@Immutable
data class AppInfo(
  val label: String,
  val packageName: String,
  val activityName: String,
  val icon: ImageBitmap?,
  val isAdaptiveIcon: Boolean = false,
  val originalLabel: String = label,
  val dailyLaunchCount: Int = 0,
  val isMindfulPaused: Boolean = false,
  val isWorkProfile: Boolean = false,
  val userSerialNumber: Long = 0L,
  val appCategory: Int = -1,
  val isSystemApp: Boolean = false,
  val shapeProfile: AppIconShapeProfile =
    if (isAdaptiveIcon) AppIconShapeProfile.SYSTEM_ADAPTIVE
    else AppIconShapeProfile.CUSTOM_SILHOUETTE,
  val adaptiveForegroundIcon: ImageBitmap? = null,
  val adaptiveAccentArgb: Int? = null,
  val hasMonochromeLayer: Boolean = false,
) {
  val componentKey: String
    get() =
      if (userSerialNumber == 0L) "$packageName/$activityName"
      else "$packageName/$activityName#$userSerialNumber"

  val initialLetter: Char
    get() {
      val first = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
      return if (first in 'A'..'Z') first else '#'
    }
}

@Immutable
data class AppShortcutItem(
  val id: String,
  val packageName: String,
  val shortLabel: String,
  val appLabel: String,
)

@Immutable
data class AppFolder(
  val id: String,
  val name: String,
  val componentKeys: List<String>,
)

@Immutable
data class WeatherLocation(
  val name: String,
  val region: String,
  val latitude: Double,
  val longitude: Double,
) {
  companion object {
    val MIAMI_DEFAULT =
      WeatherLocation(
        name = "Miami",
        region = "Florida, US",
        latitude = 25.7617,
        longitude = -80.1918,
      )
  }
}

enum class HomeGestureAction(val label: String) {
  NONE("None"),
  OPEN_CAMERA("Launch Camera"),
  OPEN_DIALER("Launch Phone"),
  OPEN_SEARCH("Search apps"),
  OPEN_DRAWER("Open All apps"),
  EXPAND_NOTIFICATIONS("Notification shade"),
  TOGGLE_FOCUS_MODE("Toggle Focus mode");

  companion object {
    fun fromName(name: String?, default: HomeGestureAction): HomeGestureAction =
      entries.firstOrNull { it.name == name } ?: default
  }
}

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  DARK("Dark"),
  LIGHT("Light");

  companion object {
    fun fromName(name: String?): ThemeMode =
      entries.firstOrNull { it.name == name } ?: SYSTEM
  }
}

enum class AccentOption(
  val label: String,
  val primaryColor: Color,
) {
  MONO("Mono", Color(0xFFEDEDED)),
  EMERALD("Emerald", Color(0xFF3AD18D)),
  PURPLE("Purple", Color(0xFF9B5DE5)),
  BLUE("Blue", Color(0xFF3B9EFF)),
  ORANGE("Orange", Color(0xFFFF922B));

  fun resolvedPrimary(isDark: Boolean): Color =
    if (this == MONO && !isDark) Color(0xFF171717) else primaryColor

  companion object {
    fun fromName(name: String?): AccentOption =
      entries.firstOrNull { it.name == name } ?: EMERALD
  }
}

enum class ScrollSpeed(
  val label: String,
  val scrollMultiplier: Float,
) {
  SLOW("Slow", 0.65f),
  NORMAL("Normal", 1.0f),
  FAST("Fast", 1.55f);

  companion object {
    fun fromName(name: String?): ScrollSpeed =
      entries.firstOrNull { it.name == name } ?: NORMAL
  }
}

enum class AdaptiveIconShape(val label: String) {
  SYSTEM("System"),
  ROUNDED_SQUARE("Rounded"),
  SQUIRCLE("Squircle"),
  CIRCLE("Circle"),
  TEARDROP("Teardrop");

  companion object {
    fun fromName(name: String?): AdaptiveIconShape =
      entries.firstOrNull { it.name == name } ?: ROUNDED_SQUARE
  }
}

enum class ClockStyle(val label: String) {
  NEON_HORIZON("Neon Horizon"),
  MONO_TERMINAL("Mono Terminal"),
  MINIMAL_EDITORIAL("Editorial");

  companion object {
    fun fromName(name: String?): ClockStyle =
      entries.firstOrNull { it.name == name } ?: NEON_HORIZON
  }
}

enum class DrawerProfileFilter(val label: String) {
  ALL("All"),
  PERSONAL("Personal"),
  WORK("Work"),
}

enum class TimeOfDayBucket(val label: String) {
  MORNING("Morning"),
  AFTERNOON("Afternoon"),
  EVENING("Evening");

  companion object {
    fun fromHour(hourOfDay: Int): TimeOfDayBucket =
      when (hourOfDay) {
        in 5..11 -> MORNING
        in 12..17 -> AFTERNOON
        else -> EVENING
      }
  }
}

@Immutable
data class CalendarEventPreview(
  val id: Long,
  val title: String,
  val startTimeMillis: Long,
  val endTimeMillis: Long,
)

@Immutable
data class MindfulInsights(
  val totalLaunchesToday: Int = 0,
  val stayedPresentToday: Int = 0,
  val continuedAnywayToday: Int = 0,
  val topLaunchedApps: List<Pair<AppInfo, Int>> = emptyList(),
)

@Immutable
data class LauncherSettings(
  val themeMode: ThemeMode = ThemeMode.DARK,
  val accent: AccentOption = AccentOption.EMERALD,
  val iconShape: AdaptiveIconShape = AdaptiveIconShape.SYSTEM,
  val respectIndividualIconShapes: Boolean = true,
  val coastalBackdropEnabled: Boolean = true,
  val alphabetIndexEnabled: Boolean = true,
  val hideAppLabels: Boolean = false,
  val scrollSpeed: ScrollSpeed = ScrollSpeed.NORMAL,
  val hapticFeedbackEnabled: Boolean = true,
  val favoriteComponentKeys: List<String> = emptyList(),
  val folders: List<AppFolder> = emptyList(),
  val hiddenComponentKeys: Set<String> = emptySet(),
  val customAppLabels: Map<String, String> = emptyMap(),
  val mindfulPauseEnabled: Boolean = true,
  val mindfulPausedComponentKeys: Set<String> = emptySet(),
  val showDailyLaunchCounts: Boolean = false,
  val calendarPreviewEnabled: Boolean = false,
  val reducedMotionEnabled: Boolean = false,
  val useDeviceLocation: Boolean = true,
  val weatherLocation: WeatherLocation = WeatherLocation.MIAMI_DEFAULT,
  val swipeLeftAction: HomeGestureAction = HomeGestureAction.OPEN_CAMERA,
  val swipeRightAction: HomeGestureAction = HomeGestureAction.TOGGLE_FOCUS_MODE,
  val doubleTapAction: HomeGestureAction = HomeGestureAction.OPEN_SEARCH,
  val focusModeManualEnabled: Boolean = false,
  val focusScheduleEnabled: Boolean = false,
  val focusScheduleStartMinutes: Int = 9 * 60,
  val focusScheduleEndMinutes: Int = 17 * 60,
  val focusEssentialComponentKeys: Set<String> = emptySet(),
  val focusTimerDurationMinutes: Int = 25,
  val focusTimerEndMillis: Long = 0L,
  val hasCompletedOnboarding: Boolean = false,
  val clockStyle: ClockStyle = ClockStyle.NEON_HORIZON,
  val showClockSeconds: Boolean = false,
  val showBatteryIndicator: Boolean = true,
  val useSystemWallpaper: Boolean = true,
  val customWallpaperPath: String? = null,
  val wallpaperDimPercent: Int = 40,
  val dailyIntention: String = "",
  val widgetStackEnabled: Boolean = true,
  val autoLaunchSingleMatch: Boolean = false,
  val bottomAlignFavorites: Boolean = false,
  val leftHandedScrubber: Boolean = false,
  val lockHiddenApps: Boolean = false,
  val quickScratchpadText: String = "",
)

@Immutable
data class AlphabetSection(
  val letter: Char,
  val apps: List<AppInfo>,
)

enum class WeatherEffectType {
  CLEAR_BREEZE,
  HEAT_HAZE,
  CLOUDY,
  RAIN,
  THUNDERSTORM,
}

@Immutable
data class WeatherInfo(
  val temperatureCelsius: Int = 24,
  val apparentTemperatureCelsius: Int = 25,
  val conditionLabel: String = "Mostly clear",
  val locationName: String = "Miami",
  val effectType: WeatherEffectType = WeatherEffectType.CLEAR_BREEZE,
  val cloudCoverPercent: Int = 15,
  val windSpeedKmh: Int = 14,
  val isDay: Boolean = true,
  val isLive: Boolean = false,
  val isRefreshing: Boolean = false,
)

@Immutable
data class ResolvedFolder(
  val folder: AppFolder,
  val apps: List<AppInfo>,
)

sealed interface DrawerQuickAction {
  data class DialPhone(val displayNumber: String) : DrawerQuickAction

  data class OpenUrl(val url: String) : DrawerQuickAction
}

@Immutable
data class LauncherUiState(
  val isLoading: Boolean = true,
  val allApps: List<AppInfo> = emptyList(),
  val hiddenApps: List<AppInfo> = emptyList(),
  val allVisibleApps: List<AppInfo> = emptyList(),
  val favoriteApps: List<AppInfo> = emptyList(),
  val homeFolders: List<ResolvedFolder> = emptyList(),
  val filteredDrawerApps: List<AppInfo> = emptyList(),
  val drawerSections: List<AlphabetSection> = emptyList(),
  val matchedShortcuts: List<AppShortcutItem> = emptyList(),
  val drawerQuickAction: DrawerQuickAction? = null,
  val searchQuery: String = "",
  val quickMathResult: String? = null,
  val selectedAppKey: String? = null,
  val settings: LauncherSettings = LauncherSettings(),
  val weather: WeatherInfo = WeatherInfo(),
  val nextCalendarEvent: CalendarEventPreview? = null,
  val pendingMindfulApp: AppInfo? = null,
  val isFocusModeActive: Boolean = false,
  val isDefaultHomeLauncher: Boolean = false,
  val mindfulInsights: MindfulInsights = MindfulInsights(),
  val drawerProfileFilter: DrawerProfileFilter = DrawerProfileFilter.ALL,
  val hasWorkProfileApps: Boolean = false,
  val frequentDrawerApps: List<AppInfo> = emptyList(),
  val activeTimeOfDayBucket: TimeOfDayBucket = TimeOfDayBucket.MORNING,
  val appShortcutsByPackage: Map<String, List<AppShortcutItem>> = emptyMap(),
  val isDetectingLocation: Boolean = false,
  val locationStatusMessage: String? = null,
)

enum class LauncherDestination {
  HOME,
  ALL_APPS,
  SETTINGS,
}
