package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AppFolder
import com.example.model.ClockStyle
import com.example.model.HomeGestureAction
import com.example.model.LauncherSettings
import com.example.model.ScrollSpeed
import com.example.model.ThemeMode
import com.example.model.TimeOfDayBucket
import com.example.model.WeatherEffectType
import com.example.model.WeatherInfo
import com.example.model.WeatherLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.launcherDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "neon_sunset_launcher_prefs")

data class DailyMindfulTotals(
  val stayedPresentCount: Int = 0,
  val continuedAnywayCount: Int = 0,
)

class SettingsRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT = stringPreferencesKey("accent_option_v2")
    val ICON_SHAPE = stringPreferencesKey("adaptive_icon_shape")
    val COASTAL_BACKDROP = booleanPreferencesKey("coastal_backdrop_enabled")
    val ALPHABET_INDEX = booleanPreferencesKey("alphabet_index_enabled")
    val HIDE_LABELS = booleanPreferencesKey("hide_app_labels")
    val SCROLL_SPEED = stringPreferencesKey("scroll_speed")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
    val FAVORITE_KEYS = stringPreferencesKey("favorite_component_keys")
    val FOLDERS_JSON = stringPreferencesKey("app_folders_json")
    val HIDDEN_KEYS = stringPreferencesKey("hidden_component_keys")
    val CUSTOM_LABELS_JSON = stringPreferencesKey("custom_app_labels_json")
    val MINDFUL_PAUSE_ENABLED = booleanPreferencesKey("mindful_pause_enabled")
    val MINDFUL_PAUSED_KEYS = stringPreferencesKey("mindful_paused_component_keys")
    val SHOW_DAILY_LAUNCH_COUNTS = booleanPreferencesKey("show_daily_launch_counts")
    val CALENDAR_PREVIEW_ENABLED = booleanPreferencesKey("calendar_preview_enabled")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion_enabled")
    val USE_DEVICE_LOCATION = booleanPreferencesKey("use_device_location")
    val WEATHER_LOC_NAME = stringPreferencesKey("weather_loc_name")
    val WEATHER_LOC_REGION = stringPreferencesKey("weather_loc_region")
    val WEATHER_LOC_LAT = doublePreferencesKey("weather_loc_lat")
    val WEATHER_LOC_LON = doublePreferencesKey("weather_loc_lon")
    val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
    val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")
    val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action")
    val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
    val FOCUS_MODE_MANUAL = booleanPreferencesKey("focus_mode_manual_enabled")
    val FOCUS_SCHEDULE_ENABLED = booleanPreferencesKey("focus_schedule_enabled")
    val FOCUS_SCHEDULE_START_MIN = intPreferencesKey("focus_schedule_start_min")
    val FOCUS_SCHEDULE_END_MIN = intPreferencesKey("focus_schedule_end_min")
    val FOCUS_ESSENTIAL_KEYS = stringPreferencesKey("focus_essential_keys")
    val FOCUS_TIMER_DURATION_MIN = intPreferencesKey("focus_timer_duration_min")
    val FOCUS_TIMER_END_MILLIS = longPreferencesKey("focus_timer_end_millis")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    val DAILY_LAUNCH_COUNTS_DATE = stringPreferencesKey("daily_launch_counts_date")
    val DAILY_LAUNCH_COUNTS_JSON = stringPreferencesKey("daily_launch_counts_json")
    val DAILY_MINDFUL_DATE = stringPreferencesKey("daily_mindful_date")
    val DAILY_MINDFUL_STAYED = intPreferencesKey("daily_mindful_stayed")
    val DAILY_MINDFUL_CONTINUED = intPreferencesKey("daily_mindful_continued")
    val CLOCK_STYLE = stringPreferencesKey("clock_style")
    val SHOW_CLOCK_SECONDS = booleanPreferencesKey("show_clock_seconds")
    val SHOW_BATTERY_INDICATOR = booleanPreferencesKey("show_battery_indicator")
    val USE_SYSTEM_WALLPAPER = booleanPreferencesKey("use_system_wallpaper")
    val CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
    val WALLPAPER_DIM_PERCENT = intPreferencesKey("wallpaper_dim_percent")
    val DAILY_INTENTION = stringPreferencesKey("daily_intention")
    val WIDGET_STACK_ENABLED = booleanPreferencesKey("widget_stack_enabled")
    val AUTO_LAUNCH_SINGLE_MATCH = booleanPreferencesKey("auto_launch_single_match")
    val BOTTOM_ALIGN_FAVORITES = booleanPreferencesKey("bottom_align_favorites")
    val LEFT_HANDED_SCRUBBER = booleanPreferencesKey("left_handed_scrubber")
    val LOCK_HIDDEN_APPS = booleanPreferencesKey("lock_hidden_apps")
    val QUICK_SCRATCHPAD_TEXT = stringPreferencesKey("quick_scratchpad_text")
    val CONTEXTUAL_LAUNCH_COUNTS_JSON = stringPreferencesKey("contextual_launch_counts_json")
    val RESPECT_INDIVIDUAL_SHAPES = booleanPreferencesKey("respect_individual_icon_shapes")
  }

  private fun todayKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

  val settingsFlow: Flow<LauncherSettings> =
    context.launcherDataStore.data.map { prefs ->
      val favorites = decodeLines(prefs[Keys.FAVORITE_KEYS])
      val folders = decodeFoldersJson(prefs[Keys.FOLDERS_JSON])
      val hiddenKeys = decodeLines(prefs[Keys.HIDDEN_KEYS]).toSet()
      val customLabels = decodeStringMap(prefs[Keys.CUSTOM_LABELS_JSON])
      val mindfulPausedKeys = decodeLines(prefs[Keys.MINDFUL_PAUSED_KEYS]).toSet()
      val focusEssentials = decodeLines(prefs[Keys.FOCUS_ESSENTIAL_KEYS]).toSet()
      val weatherLocation =
        WeatherLocation(
          name = prefs[Keys.WEATHER_LOC_NAME] ?: WeatherLocation.MIAMI_DEFAULT.name,
          region = prefs[Keys.WEATHER_LOC_REGION] ?: WeatherLocation.MIAMI_DEFAULT.region,
          latitude = prefs[Keys.WEATHER_LOC_LAT] ?: WeatherLocation.MIAMI_DEFAULT.latitude,
          longitude = prefs[Keys.WEATHER_LOC_LON] ?: WeatherLocation.MIAMI_DEFAULT.longitude,
        )

      LauncherSettings(
        themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name),
        accent = AccentOption.fromName(prefs[Keys.ACCENT]),
        iconShape =
          prefs[Keys.ICON_SHAPE]?.let(AdaptiveIconShape::fromName) ?: AdaptiveIconShape.SYSTEM,
        respectIndividualIconShapes = prefs[Keys.RESPECT_INDIVIDUAL_SHAPES] ?: true,
        coastalBackdropEnabled = prefs[Keys.COASTAL_BACKDROP] ?: true,
        alphabetIndexEnabled = prefs[Keys.ALPHABET_INDEX] ?: true,
        hideAppLabels = prefs[Keys.HIDE_LABELS] ?: false,
        scrollSpeed = ScrollSpeed.fromName(prefs[Keys.SCROLL_SPEED]),
        hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
        favoriteComponentKeys = favorites,
        folders = folders,
        hiddenComponentKeys = hiddenKeys,
        customAppLabels = customLabels,
        mindfulPauseEnabled = prefs[Keys.MINDFUL_PAUSE_ENABLED] ?: true,
        mindfulPausedComponentKeys = mindfulPausedKeys,
        showDailyLaunchCounts = prefs[Keys.SHOW_DAILY_LAUNCH_COUNTS] ?: false,
        calendarPreviewEnabled = prefs[Keys.CALENDAR_PREVIEW_ENABLED] ?: false,
        reducedMotionEnabled = prefs[Keys.REDUCED_MOTION] ?: false,
        useDeviceLocation = prefs[Keys.USE_DEVICE_LOCATION] ?: true,
        weatherLocation = weatherLocation,
        swipeLeftAction =
          HomeGestureAction.fromName(prefs[Keys.SWIPE_LEFT_ACTION], HomeGestureAction.OPEN_CAMERA),
        swipeRightAction =
          HomeGestureAction.fromName(
            prefs[Keys.SWIPE_RIGHT_ACTION],
            HomeGestureAction.TOGGLE_FOCUS_MODE,
          ),
        doubleTapAction =
          HomeGestureAction.fromName(prefs[Keys.DOUBLE_TAP_ACTION], HomeGestureAction.OPEN_SEARCH),
        focusModeManualEnabled = prefs[Keys.FOCUS_MODE_MANUAL] ?: false,
        focusScheduleEnabled = prefs[Keys.FOCUS_SCHEDULE_ENABLED] ?: false,
        focusScheduleStartMinutes = prefs[Keys.FOCUS_SCHEDULE_START_MIN] ?: (9 * 60),
        focusScheduleEndMinutes = prefs[Keys.FOCUS_SCHEDULE_END_MIN] ?: (17 * 60),
        focusEssentialComponentKeys = focusEssentials,
        focusTimerDurationMinutes = (prefs[Keys.FOCUS_TIMER_DURATION_MIN] ?: 25).coerceIn(5, 90),
        focusTimerEndMillis = prefs[Keys.FOCUS_TIMER_END_MILLIS] ?: 0L,
        hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
        clockStyle = ClockStyle.fromName(prefs[Keys.CLOCK_STYLE]),
        showClockSeconds = prefs[Keys.SHOW_CLOCK_SECONDS] ?: false,
        showBatteryIndicator = prefs[Keys.SHOW_BATTERY_INDICATOR] ?: true,
        useSystemWallpaper = prefs[Keys.USE_SYSTEM_WALLPAPER] ?: true,
        customWallpaperPath = prefs[Keys.CUSTOM_WALLPAPER_PATH]?.takeIf { it.isNotBlank() },
        wallpaperDimPercent = (prefs[Keys.WALLPAPER_DIM_PERCENT] ?: 40).coerceIn(0, 85),
        dailyIntention = prefs[Keys.DAILY_INTENTION] ?: "",
        widgetStackEnabled = prefs[Keys.WIDGET_STACK_ENABLED] ?: true,
        autoLaunchSingleMatch = prefs[Keys.AUTO_LAUNCH_SINGLE_MATCH] ?: false,
        bottomAlignFavorites = prefs[Keys.BOTTOM_ALIGN_FAVORITES] ?: false,
        leftHandedScrubber = prefs[Keys.LEFT_HANDED_SCRUBBER] ?: false,
        lockHiddenApps = prefs[Keys.LOCK_HIDDEN_APPS] ?: false,
        quickScratchpadText = prefs[Keys.QUICK_SCRATCHPAD_TEXT] ?: "",
      )
    }

  val cachedWeatherFlow: Flow<WeatherInfo?> =
    context.launcherDataStore.data.map { prefs -> decodeWeatherJson(prefs[Keys.CACHED_WEATHER_JSON]) }

  val dailyLaunchCountsFlow: Flow<Map<String, Int>> =
    context.launcherDataStore.data.map { prefs ->
      if (prefs[Keys.DAILY_LAUNCH_COUNTS_DATE] != todayKey()) emptyMap()
      else decodeIntMap(prefs[Keys.DAILY_LAUNCH_COUNTS_JSON])
    }

  val contextualLaunchCountsFlow: Flow<Map<String, Int>> =
    context.launcherDataStore.data.map { prefs ->
      decodeIntMap(prefs[Keys.CONTEXTUAL_LAUNCH_COUNTS_JSON])
    }

  val dailyMindfulTotalsFlow: Flow<DailyMindfulTotals> =
    context.launcherDataStore.data.map { prefs ->
      if (prefs[Keys.DAILY_MINDFUL_DATE] != todayKey()) {
        DailyMindfulTotals()
      } else {
        DailyMindfulTotals(
          stayedPresentCount = prefs[Keys.DAILY_MINDFUL_STAYED] ?: 0,
          continuedAnywayCount = prefs[Keys.DAILY_MINDFUL_CONTINUED] ?: 0,
        )
      }
    }

  suspend fun saveCachedWeather(weather: WeatherInfo) {
    context.launcherDataStore.edit { it[Keys.CACHED_WEATHER_JSON] = encodeWeatherJson(weather) }
  }

  suspend fun setThemeMode(themeMode: ThemeMode) {
    context.launcherDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
  }

  suspend fun setAccent(accent: AccentOption) {
    context.launcherDataStore.edit { it[Keys.ACCENT] = accent.name }
  }

  suspend fun setIconShape(iconShape: AdaptiveIconShape) {
    context.launcherDataStore.edit { it[Keys.ICON_SHAPE] = iconShape.name }
  }

  suspend fun setRespectIndividualIconShapes(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.RESPECT_INDIVIDUAL_SHAPES] = enabled }
  }

  suspend fun setCoastalBackdropEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.COASTAL_BACKDROP] = enabled }
  }

  suspend fun setAlphabetIndexEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.ALPHABET_INDEX] = enabled }
  }

  suspend fun setHideAppLabels(hidden: Boolean) {
    context.launcherDataStore.edit { it[Keys.HIDE_LABELS] = hidden }
  }

  suspend fun setScrollSpeed(speed: ScrollSpeed) {
    context.launcherDataStore.edit { it[Keys.SCROLL_SPEED] = speed.name }
  }

  suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
  }

  suspend fun setFavorites(orderedKeys: List<String>) {
    context.launcherDataStore.edit {
      it[Keys.FAVORITE_KEYS] = orderedKeys.distinct().joinToString("\n")
    }
  }

  suspend fun saveFolder(folder: AppFolder) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeFoldersJson(prefs[Keys.FOLDERS_JSON]).toMutableList()
      val existingIdx = current.indexOfFirst { it.id == folder.id }
      if (existingIdx >= 0) current[existingIdx] = folder else current.add(folder)
      prefs[Keys.FOLDERS_JSON] = encodeFoldersJson(current)
    }
  }

  suspend fun deleteFolder(folderId: String) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeFoldersJson(prefs[Keys.FOLDERS_JSON]).filterNot { it.id == folderId }
      prefs[Keys.FOLDERS_JSON] = encodeFoldersJson(current)
    }
  }

  suspend fun replaceFolders(folders: List<AppFolder>) {
    context.launcherDataStore.edit { prefs ->
      prefs[Keys.FOLDERS_JSON] = encodeFoldersJson(folders)
    }
  }

  suspend fun setAppHidden(componentKey: String, hidden: Boolean) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeLines(prefs[Keys.HIDDEN_KEYS]).toMutableSet()
      if (hidden) current.add(componentKey) else current.remove(componentKey)
      prefs[Keys.HIDDEN_KEYS] = current.joinToString("\n")
    }
  }

  suspend fun setCustomLabel(componentKey: String, newLabel: String?) {
    context.launcherDataStore.edit { prefs ->
      val map = decodeStringMap(prefs[Keys.CUSTOM_LABELS_JSON]).toMutableMap()
      val trimmed = newLabel?.trim().orEmpty()
      if (trimmed.isEmpty()) map.remove(componentKey) else map[componentKey] = trimmed
      prefs[Keys.CUSTOM_LABELS_JSON] = encodeStringMap(map)
    }
  }

  suspend fun setMindfulPauseEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.MINDFUL_PAUSE_ENABLED] = enabled }
  }

  suspend fun setMindfulPausedApps(keys: Set<String>) {
    context.launcherDataStore.edit { it[Keys.MINDFUL_PAUSED_KEYS] = keys.joinToString("\n") }
  }

  suspend fun toggleMindfulPausedApp(componentKey: String) {
    context.launcherDataStore.edit { prefs ->
      val current = decodeLines(prefs[Keys.MINDFUL_PAUSED_KEYS]).toMutableSet()
      if (componentKey in current) current.remove(componentKey) else current.add(componentKey)
      prefs[Keys.MINDFUL_PAUSED_KEYS] = current.joinToString("\n")
    }
  }

  suspend fun setShowDailyLaunchCounts(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.SHOW_DAILY_LAUNCH_COUNTS] = enabled }
  }

  suspend fun setCalendarPreviewEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.CALENDAR_PREVIEW_ENABLED] = enabled }
  }

  suspend fun setReducedMotionEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
  }

  suspend fun setUseDeviceLocation(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.USE_DEVICE_LOCATION] = enabled }
  }

  suspend fun setWeatherLocation(location: WeatherLocation) {
    context.launcherDataStore.edit { prefs ->
      prefs[Keys.WEATHER_LOC_NAME] = location.name
      prefs[Keys.WEATHER_LOC_REGION] = location.region
      prefs[Keys.WEATHER_LOC_LAT] = location.latitude
      prefs[Keys.WEATHER_LOC_LON] = location.longitude
    }
  }

  suspend fun setSwipeLeftAction(action: HomeGestureAction) {
    context.launcherDataStore.edit { it[Keys.SWIPE_LEFT_ACTION] = action.name }
  }

  suspend fun setSwipeRightAction(action: HomeGestureAction) {
    context.launcherDataStore.edit { it[Keys.SWIPE_RIGHT_ACTION] = action.name }
  }

  suspend fun setDoubleTapAction(action: HomeGestureAction) {
    context.launcherDataStore.edit { it[Keys.DOUBLE_TAP_ACTION] = action.name }
  }

  suspend fun setFocusModeManualEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.FOCUS_MODE_MANUAL] = enabled }
  }

  suspend fun setFocusSchedule(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
    context.launcherDataStore.edit { prefs ->
      prefs[Keys.FOCUS_SCHEDULE_ENABLED] = enabled
      prefs[Keys.FOCUS_SCHEDULE_START_MIN] = startMinutes
      prefs[Keys.FOCUS_SCHEDULE_END_MIN] = endMinutes
    }
  }

  suspend fun setFocusEssentialApps(keys: Set<String>) {
    context.launcherDataStore.edit { it[Keys.FOCUS_ESSENTIAL_KEYS] = keys.joinToString("\n") }
  }

  suspend fun setFocusTimer(durationMinutes: Int, endMillis: Long) {
    context.launcherDataStore.edit { prefs ->
      prefs[Keys.FOCUS_TIMER_DURATION_MIN] = durationMinutes
      prefs[Keys.FOCUS_TIMER_END_MILLIS] = endMillis
    }
  }

  suspend fun setFocusTimerDuration(durationMinutes: Int) {
    context.launcherDataStore.edit { prefs ->
      prefs[Keys.FOCUS_TIMER_DURATION_MIN] = durationMinutes.coerceIn(5, 90)
    }
  }

  suspend fun setHasCompletedOnboarding(completed: Boolean) {
    context.launcherDataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
  }

  suspend fun setClockStyle(style: ClockStyle) {
    context.launcherDataStore.edit { it[Keys.CLOCK_STYLE] = style.name }
  }

  suspend fun setShowClockSeconds(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.SHOW_CLOCK_SECONDS] = enabled }
  }

  suspend fun setShowBatteryIndicator(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.SHOW_BATTERY_INDICATOR] = enabled }
  }

  suspend fun setUseSystemWallpaper(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.USE_SYSTEM_WALLPAPER] = enabled }
  }

  suspend fun setCustomWallpaperPath(path: String?) {
    context.launcherDataStore.edit { prefs ->
      if (path.isNullOrBlank()) prefs.remove(Keys.CUSTOM_WALLPAPER_PATH)
      else prefs[Keys.CUSTOM_WALLPAPER_PATH] = path
    }
  }

  suspend fun setWallpaperDimPercent(percent: Int) {
    context.launcherDataStore.edit { it[Keys.WALLPAPER_DIM_PERCENT] = percent.coerceIn(0, 85) }
  }

  suspend fun setDailyIntention(intention: String) {
    context.launcherDataStore.edit { prefs ->
      val trimmed = intention.trim()
      if (trimmed.isEmpty()) prefs.remove(Keys.DAILY_INTENTION)
      else prefs[Keys.DAILY_INTENTION] = trimmed
    }
  }

  suspend fun setWidgetStackEnabled(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.WIDGET_STACK_ENABLED] = enabled }
  }

  suspend fun setAutoLaunchSingleMatch(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.AUTO_LAUNCH_SINGLE_MATCH] = enabled }
  }

  suspend fun setBottomAlignFavorites(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.BOTTOM_ALIGN_FAVORITES] = enabled }
  }

  suspend fun setLeftHandedScrubber(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.LEFT_HANDED_SCRUBBER] = enabled }
  }

  suspend fun setLockHiddenApps(enabled: Boolean) {
    context.launcherDataStore.edit { it[Keys.LOCK_HIDDEN_APPS] = enabled }
  }

  suspend fun setQuickScratchpadText(text: String) {
    context.launcherDataStore.edit { prefs ->
      val trimmed = text.trim()
      if (trimmed.isEmpty()) prefs.remove(Keys.QUICK_SCRATCHPAD_TEXT)
      else prefs[Keys.QUICK_SCRATCHPAD_TEXT] = trimmed
    }
  }

  suspend fun importSettingsBackup(jsonString: String): Boolean {
    val imported = decodeSettingsBackupJson(jsonString) ?: return false
    context.launcherDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = imported.themeMode.name
      prefs[Keys.ACCENT] = imported.accent.name
      prefs[Keys.ICON_SHAPE] = imported.iconShape.name
      prefs[Keys.RESPECT_INDIVIDUAL_SHAPES] = imported.respectIndividualIconShapes
      prefs[Keys.COASTAL_BACKDROP] = imported.coastalBackdropEnabled
      prefs[Keys.ALPHABET_INDEX] = imported.alphabetIndexEnabled
      prefs[Keys.HIDE_LABELS] = imported.hideAppLabels
      prefs[Keys.SCROLL_SPEED] = imported.scrollSpeed.name
      prefs[Keys.HAPTIC_FEEDBACK] = imported.hapticFeedbackEnabled
      prefs[Keys.FAVORITE_KEYS] = imported.favoriteComponentKeys.joinToString("\n")
      prefs[Keys.FOLDERS_JSON] = encodeFoldersJson(imported.folders)
      prefs[Keys.HIDDEN_KEYS] = imported.hiddenComponentKeys.joinToString("\n")
      prefs[Keys.CUSTOM_LABELS_JSON] = encodeStringMap(imported.customAppLabels)
      prefs[Keys.MINDFUL_PAUSE_ENABLED] = imported.mindfulPauseEnabled
      prefs[Keys.MINDFUL_PAUSED_KEYS] = imported.mindfulPausedComponentKeys.joinToString("\n")
      prefs[Keys.SHOW_DAILY_LAUNCH_COUNTS] = imported.showDailyLaunchCounts
      prefs[Keys.REDUCED_MOTION] = imported.reducedMotionEnabled
      prefs[Keys.USE_DEVICE_LOCATION] = imported.useDeviceLocation
      prefs[Keys.WEATHER_LOC_NAME] = imported.weatherLocation.name
      prefs[Keys.WEATHER_LOC_REGION] = imported.weatherLocation.region
      prefs[Keys.WEATHER_LOC_LAT] = imported.weatherLocation.latitude
      prefs[Keys.WEATHER_LOC_LON] = imported.weatherLocation.longitude
      prefs[Keys.SWIPE_LEFT_ACTION] = imported.swipeLeftAction.name
      prefs[Keys.SWIPE_RIGHT_ACTION] = imported.swipeRightAction.name
      prefs[Keys.DOUBLE_TAP_ACTION] = imported.doubleTapAction.name
      prefs[Keys.FOCUS_SCHEDULE_ENABLED] = imported.focusScheduleEnabled
      prefs[Keys.FOCUS_SCHEDULE_START_MIN] = imported.focusScheduleStartMinutes
      prefs[Keys.FOCUS_SCHEDULE_END_MIN] = imported.focusScheduleEndMinutes
      prefs[Keys.FOCUS_ESSENTIAL_KEYS] = imported.focusEssentialComponentKeys.joinToString("\n")
      prefs[Keys.FOCUS_TIMER_DURATION_MIN] = imported.focusTimerDurationMinutes
      prefs[Keys.CLOCK_STYLE] = imported.clockStyle.name
      prefs[Keys.SHOW_CLOCK_SECONDS] = imported.showClockSeconds
      prefs[Keys.SHOW_BATTERY_INDICATOR] = imported.showBatteryIndicator
      prefs[Keys.USE_SYSTEM_WALLPAPER] = imported.useSystemWallpaper
      prefs[Keys.WALLPAPER_DIM_PERCENT] = imported.wallpaperDimPercent
      if (imported.dailyIntention.isNotBlank()) prefs[Keys.DAILY_INTENTION] = imported.dailyIntention
      else prefs.remove(Keys.DAILY_INTENTION)
      prefs[Keys.WIDGET_STACK_ENABLED] = imported.widgetStackEnabled
      prefs[Keys.AUTO_LAUNCH_SINGLE_MATCH] = imported.autoLaunchSingleMatch
      prefs[Keys.BOTTOM_ALIGN_FAVORITES] = imported.bottomAlignFavorites
      prefs[Keys.LEFT_HANDED_SCRUBBER] = imported.leftHandedScrubber
      prefs[Keys.LOCK_HIDDEN_APPS] = imported.lockHiddenApps
      if (imported.quickScratchpadText.isNotBlank()) {
        prefs[Keys.QUICK_SCRATCHPAD_TEXT] = imported.quickScratchpadText
      } else {
        prefs.remove(Keys.QUICK_SCRATCHPAD_TEXT)
      }
      prefs[Keys.HAS_COMPLETED_ONBOARDING] = true
    }
    return true
  }

  suspend fun recordMindfulOutcome(stayedPresent: Boolean) {
    val today = todayKey()
    context.launcherDataStore.edit { prefs ->
      val sameDay = prefs[Keys.DAILY_MINDFUL_DATE] == today
      val stayed = if (sameDay) (prefs[Keys.DAILY_MINDFUL_STAYED] ?: 0) else 0
      val continued = if (sameDay) (prefs[Keys.DAILY_MINDFUL_CONTINUED] ?: 0) else 0
      prefs[Keys.DAILY_MINDFUL_DATE] = today
      if (stayedPresent) {
        prefs[Keys.DAILY_MINDFUL_STAYED] = stayed + 1
        prefs[Keys.DAILY_MINDFUL_CONTINUED] = continued
      } else {
        prefs[Keys.DAILY_MINDFUL_STAYED] = stayed
        prefs[Keys.DAILY_MINDFUL_CONTINUED] = continued + 1
      }
    }
  }

  suspend fun incrementDailyLaunchCount(componentKey: String) {
    val today = todayKey()
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val bucket = TimeOfDayBucket.fromHour(currentHour)
    val bucketKey = "${bucket.name}:$componentKey"

    context.launcherDataStore.edit { prefs ->
      val map =
        if (prefs[Keys.DAILY_LAUNCH_COUNTS_DATE] == today) {
          decodeIntMap(prefs[Keys.DAILY_LAUNCH_COUNTS_JSON]).toMutableMap()
        } else {
          mutableMapOf()
        }
      map[componentKey] = (map[componentKey] ?: 0) + 1
      prefs[Keys.DAILY_LAUNCH_COUNTS_DATE] = today
      prefs[Keys.DAILY_LAUNCH_COUNTS_JSON] = encodeIntMap(map)

      val contextualMap =
        decodeIntMap(prefs[Keys.CONTEXTUAL_LAUNCH_COUNTS_JSON]).toMutableMap()
      contextualMap[bucketKey] = (contextualMap[bucketKey] ?: 0) + 1
      prefs[Keys.CONTEXTUAL_LAUNCH_COUNTS_JSON] = encodeIntMap(contextualMap)
    }
  }

  suspend fun resetLayout() {
    context.launcherDataStore.edit { prefs ->
      prefs.remove(Keys.FAVORITE_KEYS)
      prefs.remove(Keys.FOLDERS_JSON)
      prefs.remove(Keys.HIDDEN_KEYS)
      prefs.remove(Keys.CUSTOM_LABELS_JSON)
      prefs[Keys.ICON_SHAPE] = AdaptiveIconShape.SYSTEM.name
      prefs[Keys.RESPECT_INDIVIDUAL_SHAPES] = true
      prefs[Keys.COASTAL_BACKDROP] = true
      prefs[Keys.ALPHABET_INDEX] = true
      prefs[Keys.HIDE_LABELS] = false
      prefs[Keys.SCROLL_SPEED] = ScrollSpeed.NORMAL.name
      prefs[Keys.HAPTIC_FEEDBACK] = true
      prefs[Keys.SWIPE_LEFT_ACTION] = HomeGestureAction.OPEN_CAMERA.name
      prefs[Keys.SWIPE_RIGHT_ACTION] = HomeGestureAction.TOGGLE_FOCUS_MODE.name
      prefs[Keys.DOUBLE_TAP_ACTION] = HomeGestureAction.OPEN_SEARCH.name
      prefs[Keys.CLOCK_STYLE] = ClockStyle.NEON_HORIZON.name
      prefs[Keys.SHOW_CLOCK_SECONDS] = false
      prefs[Keys.SHOW_BATTERY_INDICATOR] = true
      prefs[Keys.USE_SYSTEM_WALLPAPER] = true
      prefs[Keys.WALLPAPER_DIM_PERCENT] = 40
      prefs.remove(Keys.CUSTOM_WALLPAPER_PATH)
      prefs.remove(Keys.DAILY_INTENTION)
      prefs[Keys.WIDGET_STACK_ENABLED] = true
      prefs[Keys.AUTO_LAUNCH_SINGLE_MATCH] = false
      prefs[Keys.BOTTOM_ALIGN_FAVORITES] = false
      prefs[Keys.LEFT_HANDED_SCRUBBER] = false
      prefs[Keys.LOCK_HIDDEN_APPS] = false
      prefs[Keys.USE_DEVICE_LOCATION] = true
      prefs.remove(Keys.QUICK_SCRATCHPAD_TEXT)
    }
  }

  private fun decodeLines(raw: String?): List<String> =
    if (raw.isNullOrBlank()) emptyList() else raw.split('\n').filter { it.isNotBlank() }

  private fun decodeStringMap(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
      val obj = JSONObject(raw)
      buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val v = obj.optString(k, "")
          if (v.isNotBlank()) put(k, v)
        }
      }
    }.getOrDefault(emptyMap())
  }

  private fun encodeStringMap(map: Map<String, String>): String =
    JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }.toString()

  private fun decodeIntMap(raw: String?): Map<String, Int> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
      val obj = JSONObject(raw)
      buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val v = obj.optInt(k, 0)
          if (v > 0) put(k, v)
        }
      }
    }.getOrDefault(emptyMap())
  }

  private fun encodeIntMap(map: Map<String, Int>): String =
    JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }.toString()

  companion object {
    fun encodeWeatherJson(w: WeatherInfo): String =
      JSONObject().apply {
        put("tempC", w.temperatureCelsius)
        put("apparentC", w.apparentTemperatureCelsius)
        put("condition", w.conditionLabel)
        put("locationName", w.locationName)
        put("effect", w.effectType.name)
        put("cloud", w.cloudCoverPercent)
        put("wind", w.windSpeedKmh)
        put("isDay", w.isDay)
      }.toString()

    fun decodeWeatherJson(raw: String?): WeatherInfo? {
      if (raw.isNullOrBlank()) return null
      return runCatching {
        val obj = JSONObject(raw)
        val effect =
          WeatherEffectType.entries.firstOrNull { it.name == obj.optString("effect") }
            ?: WeatherEffectType.CLEAR_BREEZE
        WeatherInfo(
          temperatureCelsius = obj.getInt("tempC"),
          apparentTemperatureCelsius = obj.optInt("apparentC", obj.getInt("tempC")),
          conditionLabel = obj.optString("condition", "Mostly clear"),
          locationName = obj.optString("locationName", "Miami"),
          effectType = effect,
          cloudCoverPercent = obj.optInt("cloud", 15),
          windSpeedKmh = obj.optInt("wind", 14),
          isDay = obj.optBoolean("isDay", true),
          isLive = false,
          isRefreshing = false,
        )
      }.getOrNull()
    }

    fun encodeSettingsBackupJson(s: LauncherSettings): String =
      JSONObject().apply {
        put("version", 1)
        put("themeMode", s.themeMode.name)
        put("accent", s.accent.name)
        put("iconShape", s.iconShape.name)
        put("respectIndividualShapes", s.respectIndividualIconShapes)
        put("coastalBackdrop", s.coastalBackdropEnabled)
        put("alphabetIndex", s.alphabetIndexEnabled)
        put("hideLabels", s.hideAppLabels)
        put("scrollSpeed", s.scrollSpeed.name)
        put("haptics", s.hapticFeedbackEnabled)
        put("reducedMotion", s.reducedMotionEnabled)
        put("useDeviceLocation", s.useDeviceLocation)
        put("favorites", JSONArray(s.favoriteComponentKeys))
        put("folders", JSONArray(encodeFoldersJson(s.folders)))
        put("hidden", JSONArray(s.hiddenComponentKeys.toList()))
        put("customLabels", JSONObject(s.customAppLabels))
        put("mindfulPause", s.mindfulPauseEnabled)
        put("mindfulApps", JSONArray(s.mindfulPausedComponentKeys.toList()))
        put("dailyCounts", s.showDailyLaunchCounts)
        put("swipeLeft", s.swipeLeftAction.name)
        put("swipeRight", s.swipeRightAction.name)
        put("doubleTap", s.doubleTapAction.name)
        put("focusSchedEnabled", s.focusScheduleEnabled)
        put("focusSchedStart", s.focusScheduleStartMinutes)
        put("focusSchedEnd", s.focusScheduleEndMinutes)
        put("focusEssentials", JSONArray(s.focusEssentialComponentKeys.toList()))
        put("focusTimerMin", s.focusTimerDurationMinutes)
        put("clockStyle", s.clockStyle.name)
        put("showSeconds", s.showClockSeconds)
        put("showBattery", s.showBatteryIndicator)
        put("useSystemWallpaper", s.useSystemWallpaper)
        put("wallpaperDim", s.wallpaperDimPercent)
        put("dailyIntention", s.dailyIntention)
        put("widgetStack", s.widgetStackEnabled)
        put("autoLaunchSingle", s.autoLaunchSingleMatch)
        put("bottomAlignFavorites", s.bottomAlignFavorites)
        put("leftHandedScrubber", s.leftHandedScrubber)
        put("lockHiddenApps", s.lockHiddenApps)
        put("quickScratchpad", s.quickScratchpadText)
        put(
          "weatherLoc",
          JSONObject().apply {
            put("name", s.weatherLocation.name)
            put("region", s.weatherLocation.region)
            put("lat", s.weatherLocation.latitude)
            put("lon", s.weatherLocation.longitude)
          },
        )
      }.toString(2)

    fun decodeSettingsBackupJson(raw: String): LauncherSettings? {
      if (raw.isBlank()) return null
      return runCatching {
        val obj = JSONObject(raw)
        if (!obj.has("version")) return null
        fun jsonArrayStrings(key: String): List<String> {
          val arr = obj.optJSONArray(key) ?: return emptyList()
          return List(arr.length()) { arr.optString(it, "") }.filter { it.isNotBlank() }
        }
        val labelsObj = obj.optJSONObject("customLabels")
        val customLabels =
          if (labelsObj == null) emptyMap()
          else buildMap {
            val keys = labelsObj.keys()
            while (keys.hasNext()) {
              val k = keys.next()
              val v = labelsObj.optString(k, "")
              if (v.isNotBlank()) put(k, v)
            }
          }
        val locObj = obj.optJSONObject("weatherLoc")
        val location =
          if (locObj != null && locObj.has("name")) {
            WeatherLocation(
              name = locObj.optString("name", WeatherLocation.MIAMI_DEFAULT.name),
              region = locObj.optString("region", ""),
              latitude = locObj.optDouble("lat", WeatherLocation.MIAMI_DEFAULT.latitude),
              longitude = locObj.optDouble("lon", WeatherLocation.MIAMI_DEFAULT.longitude),
            )
          } else WeatherLocation.MIAMI_DEFAULT

        LauncherSettings(
          themeMode = ThemeMode.fromName(obj.optString("themeMode", ThemeMode.DARK.name)),
          accent = AccentOption.fromName(obj.optString("accent")),
          iconShape =
            if (obj.has("iconShape")) AdaptiveIconShape.fromName(obj.optString("iconShape"))
            else AdaptiveIconShape.SYSTEM,
          respectIndividualIconShapes = obj.optBoolean("respectIndividualShapes", true),
          coastalBackdropEnabled = obj.optBoolean("coastalBackdrop", true),
          alphabetIndexEnabled = obj.optBoolean("alphabetIndex", true),
          hideAppLabels = obj.optBoolean("hideLabels", false),
          scrollSpeed = ScrollSpeed.fromName(obj.optString("scrollSpeed")),
          hapticFeedbackEnabled = obj.optBoolean("haptics", true),
          reducedMotionEnabled = obj.optBoolean("reducedMotion", false),
          favoriteComponentKeys = jsonArrayStrings("favorites"),
          folders = decodeFoldersJson(obj.optJSONArray("folders")?.toString()),
          hiddenComponentKeys = jsonArrayStrings("hidden").toSet(),
          customAppLabels = customLabels,
          mindfulPauseEnabled = obj.optBoolean("mindfulPause", true),
          mindfulPausedComponentKeys = jsonArrayStrings("mindfulApps").toSet(),
          showDailyLaunchCounts = obj.optBoolean("dailyCounts", false),
          swipeLeftAction = HomeGestureAction.fromName(obj.optString("swipeLeft"), HomeGestureAction.OPEN_CAMERA),
          swipeRightAction = HomeGestureAction.fromName(obj.optString("swipeRight"), HomeGestureAction.TOGGLE_FOCUS_MODE),
          doubleTapAction = HomeGestureAction.fromName(obj.optString("doubleTap"), HomeGestureAction.OPEN_SEARCH),
          focusScheduleEnabled = obj.optBoolean("focusSchedEnabled", false),
          focusScheduleStartMinutes = obj.optInt("focusSchedStart", 9 * 60),
          focusScheduleEndMinutes = obj.optInt("focusSchedEnd", 17 * 60),
          focusEssentialComponentKeys = jsonArrayStrings("focusEssentials").toSet(),
          focusTimerDurationMinutes = obj.optInt("focusTimerMin", 25).coerceIn(5, 90),
          clockStyle = ClockStyle.fromName(obj.optString("clockStyle")),
          showClockSeconds = obj.optBoolean("showSeconds", false),
          showBatteryIndicator = obj.optBoolean("showBattery", true),
          useSystemWallpaper = obj.optBoolean("useSystemWallpaper", true),
          wallpaperDimPercent = obj.optInt("wallpaperDim", 40).coerceIn(0, 85),
          dailyIntention = obj.optString("dailyIntention", ""),
          widgetStackEnabled = obj.optBoolean("widgetStack", true),
          autoLaunchSingleMatch = obj.optBoolean("autoLaunchSingle", false),
          bottomAlignFavorites = obj.optBoolean("bottomAlignFavorites", false),
          leftHandedScrubber = obj.optBoolean("leftHandedScrubber", false),
          lockHiddenApps = obj.optBoolean("lockHiddenApps", false),
          quickScratchpadText = obj.optString("quickScratchpad", ""),
          useDeviceLocation = obj.optBoolean("useDeviceLocation", true),
          weatherLocation = location,
          hasCompletedOnboarding = true,
        )
      }.getOrNull()
    }

    fun encodeFoldersJson(folders: List<AppFolder>): String {
      val array = JSONArray()
      folders.forEach { folder ->
        val obj =
          JSONObject().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("keys", JSONArray(folder.componentKeys))
          }
        array.put(obj)
      }
      return array.toString()
    }

    fun decodeFoldersJson(raw: String?): List<AppFolder> {
      if (raw.isNullOrBlank()) return emptyList()
      return runCatching {
        val array = JSONArray(raw)
        buildList {
          for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").trim()
            val name = obj.optString("name", "").trim()
            if (id.isEmpty() || name.isEmpty()) continue
            val keysArr = obj.optJSONArray("keys") ?: JSONArray()
            val keys =
              buildList {
                for (j in 0 until keysArr.length()) {
                  val k = keysArr.optString(j, "").trim()
                  if (k.isNotEmpty()) add(k)
                }
              }
            add(AppFolder(id = id, name = name, componentKeys = keys))
          }
        }
      }.getOrDefault(emptyList())
    }
  }
}
