package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.CalendarRepository
import com.example.data.DailyMindfulTotals
import com.example.data.SettingsRepository
import com.example.data.WeatherRepository
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AlphabetSection
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
import com.example.model.CalendarEventPreview
import com.example.model.HomeGestureAction
import com.example.model.LauncherUiState
import com.example.model.MindfulInsights
import com.example.model.ResolvedFolder
import com.example.model.ScrollSpeed
import com.example.model.ThemeMode
import com.example.model.WeatherInfo
import com.example.model.WeatherLocation
import com.example.util.FocusAndSearchUtils
import com.example.util.MathEvaluator
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val appRepository = AppRepository(application)
  private val settingsRepository = SettingsRepository(application)
  private val weatherRepository = WeatherRepository()
  private val calendarRepository = CalendarRepository(application)

  private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
  private val isLoadingFlow = MutableStateFlow(true)
  private val searchQueryFlow = MutableStateFlow("")
  private val selectedAppKeyFlow = MutableStateFlow<String?>(null)
  private val weatherFlow = MutableStateFlow(WeatherInfo())
  private val calendarEventFlow = MutableStateFlow<CalendarEventPreview?>(null)
  private val pendingMindfulAppFlow = MutableStateFlow<AppInfo?>(null)
  private val shortcutsFlow = MutableStateFlow<List<AppShortcutItem>>(emptyList())
  private val isDefaultLauncherFlow = MutableStateFlow(appRepository.isDefaultHomeLauncher())
  private val clockPulseFlow = MutableStateFlow(System.currentTimeMillis())

  private data class CoreSnapshot(
    val rawApps: List<AppInfo>,
    val isLoading: Boolean,
    val query: String,
    val selectedKey: String?,
    val settings: com.example.model.LauncherSettings,
  )

  private data class EnvironmentSnapshot(
    val launchCounts: Map<String, Int>,
    val mindfulTotals: DailyMindfulTotals,
    val weather: WeatherInfo,
    val nextEvent: CalendarEventPreview?,
  )

  private data class TransientSnapshot(
    val pendingMindful: AppInfo?,
    val shortcuts: List<AppShortcutItem>,
    val isDefaultHome: Boolean,
    val nowMillis: Long,
  )

  private val coreStateFlow =
    combine(
      installedAppsFlow,
      isLoadingFlow,
      searchQueryFlow,
      selectedAppKeyFlow,
      settingsRepository.settingsFlow,
    ) { rawApps, isLoading, query, selectedKey, settings ->
      CoreSnapshot(rawApps, isLoading, query, selectedKey, settings)
    }

  private val environmentFlow =
    combine(
      settingsRepository.dailyLaunchCountsFlow,
      settingsRepository.dailyMindfulTotalsFlow,
      weatherFlow,
      calendarEventFlow,
    ) { launchCounts, mindfulTotals, weather, nextEvent ->
      EnvironmentSnapshot(launchCounts, mindfulTotals, weather, nextEvent)
    }

  private val transientFlow =
    combine(
      pendingMindfulAppFlow,
      shortcutsFlow,
      isDefaultLauncherFlow,
      clockPulseFlow,
    ) { pendingMindful, shortcuts, isDefaultHome, nowMillis ->
      TransientSnapshot(pendingMindful, shortcuts, isDefaultHome, nowMillis)
    }

  val uiState: StateFlow<LauncherUiState> =
    combine(coreStateFlow, environmentFlow, transientFlow) { core, env, trans ->
      val settings = core.settings
      val focusActive = FocusAndSearchUtils.isFocusModeActive(settings, trans.nowMillis)

      val enrichedApps =
        core.rawApps
          .map { app ->
            val customLabel = settings.customAppLabels[app.componentKey]
            val launches = env.launchCounts[app.componentKey] ?: 0
            val mindful =
              app.componentKey in settings.mindfulPausedComponentKeys ||
                (focusActive &&
                  settings.focusEssentialComponentKeys.isNotEmpty() &&
                  app.componentKey !in settings.focusEssentialComponentKeys)
            app.copy(
              label = customLabel ?: app.originalLabel,
              dailyLaunchCount = launches,
              isMindfulPaused = mindful,
            )
          }
          .sortedBy { it.label.lowercase() }

      val appByKey = enrichedApps.associateBy { it.componentKey }
      val hiddenApps = enrichedApps.filter { it.componentKey in settings.hiddenComponentKeys }
      val visibleApps = enrichedApps.filter { it.componentKey !in settings.hiddenComponentKeys }
      val visibleByKey = visibleApps.associateBy { it.componentKey }

      val baseFavorites =
        if (settings.favoriteComponentKeys.isEmpty()) {
          appRepository.resolveDefaultFavorites(visibleApps)
        } else {
          settings.favoriteComponentKeys.mapNotNull { visibleByKey[it] }
        }

      val favoriteApps =
        if (focusActive && settings.focusEssentialComponentKeys.isNotEmpty()) {
          baseFavorites
            .filter { it.componentKey in settings.focusEssentialComponentKeys }
            .ifEmpty { visibleApps.filter { it.componentKey in settings.focusEssentialComponentKeys } }
        } else {
          baseFavorites
        }

      val resolvedFolders =
        settings.folders.mapNotNull { folder ->
          val apps = folder.componentKeys.mapNotNull { visibleByKey[it] }
          if (apps.isEmpty()) null else ResolvedFolder(folder = folder, apps = apps)
        }

      val trimmedQuery = core.query.trim()
      val quickMath = MathEvaluator.evaluateToString(trimmedQuery)
      val quickAction = FocusAndSearchUtils.detectDrawerQuickAction(trimmedQuery)
      val filteredDrawerApps =
        if (trimmedQuery.isEmpty()) {
          visibleApps
        } else {
          visibleApps.filter {
            it.label.contains(trimmedQuery, ignoreCase = true) ||
              it.originalLabel.contains(trimmedQuery, ignoreCase = true) ||
              it.packageName.contains(trimmedQuery, ignoreCase = true)
          }
        }

      val matchedShortcuts =
        if (trimmedQuery.length < 2) {
          emptyList()
        } else {
          trans.shortcuts
            .filter {
              it.shortLabel.contains(trimmedQuery, ignoreCase = true) ||
                it.appLabel.contains(trimmedQuery, ignoreCase = true)
            }
            .take(4)
        }

      val drawerSections =
        filteredDrawerApps
          .groupBy { it.initialLetter }
          .entries
          .sortedWith(
            compareBy<Map.Entry<Char, List<AppInfo>>> {
              if (it.key == '#') 1 else 0
            }.thenBy { it.key }
          )
          .map { (letter, apps) -> AlphabetSection(letter = letter, apps = apps) }

      val topLaunchedApps =
        env.launchCounts.entries
          .sortedByDescending { it.value }
          .mapNotNull { (key, count) ->
            val app = appByKey[key] ?: return@mapNotNull null
            app to count
          }
          .take(5)

      val insights =
        MindfulInsights(
          totalLaunchesToday = env.launchCounts.values.sum(),
          stayedPresentToday = env.mindfulTotals.stayedPresentCount,
          continuedAnywayToday = env.mindfulTotals.continuedAnywayCount,
          topLaunchedApps = topLaunchedApps,
        )

      val resolvedPending = trans.pendingMindful?.let { appByKey[it.componentKey] ?: it }

      LauncherUiState(
        isLoading = core.isLoading,
        allApps = enrichedApps,
        hiddenApps = hiddenApps,
        allVisibleApps = visibleApps,
        favoriteApps = favoriteApps,
        homeFolders = resolvedFolders,
        filteredDrawerApps = filteredDrawerApps,
        drawerSections = drawerSections,
        matchedShortcuts = matchedShortcuts,
        drawerQuickAction = quickAction,
        searchQuery = core.query,
        quickMathResult = quickMath,
        selectedAppKey = core.selectedKey,
        settings = settings,
        weather = env.weather,
        nextCalendarEvent = if (settings.calendarPreviewEnabled) env.nextEvent else null,
        pendingMindfulApp = resolvedPending,
        isFocusModeActive = focusActive,
        isDefaultHomeLauncher = trans.isDefaultHome,
        mindfulInsights = insights,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
      )

  init {
    viewModelScope.launch {
      settingsRepository.cachedWeatherFlow.first()?.let { cached ->
        if (!weatherFlow.value.isLive) {
          weatherFlow.value = cached
        }
      }
    }
    refreshInstalledApps()
    startWeatherRefreshLoop()
    startCalendarRefreshLoop()
    startFocusPulseLoop()
  }

  private fun startFocusPulseLoop() {
    viewModelScope.launch {
      settingsRepository.settingsFlow.collect { settings ->
        // Wake precisely at timer expiry or next minute boundary when schedule is active
        val endMillis = settings.focusTimerEndMillis
        while (isActive) {
          val now = System.currentTimeMillis()
          clockPulseFlow.value = now
          if (endMillis in 1L..now) {
            settingsRepository.setFocusTimer(settings.focusTimerDurationMinutes, 0L)
            break
          }
          if (!settings.focusScheduleEnabled && endMillis <= now) break
          val nextMinuteDelay = 60_000L - (now % 60_000L) + 50L
          val waitMs =
            if (endMillis > now) minOf(nextMinuteDelay, (endMillis - now) + 50L)
            else nextMinuteDelay
          delay(waitMs)
        }
      }
    }
  }

  private fun startWeatherRefreshLoop() {
    viewModelScope.launch {
      while (isActive) {
        fetchAndApplyWeather()
        delay(30 * 60 * 1000L)
      }
    }
  }

  private fun startCalendarRefreshLoop() {
    viewModelScope.launch {
      while (isActive) {
        refreshCalendarEvent()
        delay(5 * 60 * 1000L)
      }
    }
  }

  fun onLauncherResumed() {
    clockPulseFlow.value = System.currentTimeMillis()
    isDefaultLauncherFlow.value = appRepository.isDefaultHomeLauncher()
    refreshCalendarEvent()
    viewModelScope.launch {
      val settings = settingsRepository.settingsFlow.first()
      if (settings.focusTimerEndMillis in 1L..System.currentTimeMillis()) {
        settingsRepository.setFocusTimer(settings.focusTimerDurationMinutes, 0L)
      }
    }
  }

  fun refreshWeather() {
    viewModelScope.launch { fetchAndApplyWeather() }
  }

  suspend fun searchWeatherCities(query: String): List<WeatherLocation> =
    weatherRepository.searchCities(query)

  fun selectWeatherLocation(location: WeatherLocation) {
    viewModelScope.launch {
      settingsRepository.setWeatherLocation(location)
      weatherFlow.value = weatherFlow.value.copy(locationName = location.name, isRefreshing = true)
      val fetched = weatherRepository.fetchWeather(location)
      if (fetched != null) {
        weatherFlow.value = fetched
        settingsRepository.saveCachedWeather(fetched)
      } else {
        weatherFlow.value = weatherFlow.value.copy(isRefreshing = false)
      }
    }
  }

  fun refreshCalendarEvent() {
    viewModelScope.launch {
      val settings = settingsRepository.settingsFlow.first()
      calendarEventFlow.value =
        if (settings.calendarPreviewEnabled && calendarRepository.hasCalendarPermission()) {
          calendarRepository.loadNextUpcomingEvent()
        } else {
          null
        }
    }
  }

  private suspend fun fetchAndApplyWeather() {
    if (weatherFlow.value.isRefreshing) return
    val location = settingsRepository.settingsFlow.first().weatherLocation
    weatherFlow.value = weatherFlow.value.copy(locationName = location.name, isRefreshing = true)
    val fetched = weatherRepository.fetchWeather(location)
    if (fetched != null) {
      weatherFlow.value = fetched
      settingsRepository.saveCachedWeather(fetched)
    } else {
      weatherFlow.value = weatherFlow.value.copy(isRefreshing = false)
    }
  }

  fun refreshInstalledApps() {
    viewModelScope.launch {
      isLoadingFlow.value = installedAppsFlow.value.isEmpty()
      val entries = appRepository.loadInstalledAppEntries()

      val initialApps =
        entries.map { entry ->
          AppInfo(
            label = entry.label,
            packageName = entry.packageName,
            activityName = entry.activityName,
            icon = entry.cachedIcon,
            isAdaptiveIcon = entry.isAdaptiveIcon,
          )
        }
      installedAppsFlow.value = initialApps
      isLoadingFlow.value = false

      // Refresh shortcuts cleanly when app acts as shortcut host
      val labelMap = entries.associate { it.packageName to it.label }
      shortcutsFlow.value = appRepository.loadAppShortcuts(labelMap)

      val currentFavorites = settingsRepository.settingsFlow.first().favoriteComponentKeys.toSet()
      val uncachedEntries =
        entries
          .filter { it.cachedIcon == null }
          .sortedBy { if (it.componentKey in currentFavorites) 0 else 1 }

      if (uncachedEntries.isEmpty()) return@launch

      val workingList = initialApps.toMutableList()
      val indexByKey = workingList.withIndex().associate { it.value.componentKey to it.index }

      uncachedEntries.chunked(8).forEach { batch ->
        batch.forEach { entry ->
          val (bitmap, isAdaptive) = appRepository.loadIconForEntry(entry)
          val idx = indexByKey[entry.componentKey]
          if (idx != null) {
            workingList[idx] = workingList[idx].copy(icon = bitmap, isAdaptiveIcon = isAdaptive)
          }
        }
        installedAppsFlow.value = workingList.toList()
      }
    }
  }

  fun onSearchQueryChange(query: String) {
    searchQueryFlow.value = query
  }

  fun requestLaunchApp(app: AppInfo): Boolean {
    val state = uiState.value
    val settings = state.settings
    val nonEssentialInFocus =
      state.isFocusModeActive &&
        settings.focusEssentialComponentKeys.isNotEmpty() &&
        app.componentKey !in settings.focusEssentialComponentKeys
    val explicitPaused =
      settings.mindfulPauseEnabled && app.componentKey in settings.mindfulPausedComponentKeys

    if (explicitPaused || nonEssentialInFocus) {
      pendingMindfulAppFlow.value = app
      return false
    }
    executeLaunch(app)
    return true
  }

  fun confirmMindfulLaunch() {
    val app = pendingMindfulAppFlow.value ?: return
    pendingMindfulAppFlow.value = null
    viewModelScope.launch { settingsRepository.recordMindfulOutcome(stayedPresent = false) }
    executeLaunch(app)
  }

  fun dismissMindfulLaunch() {
    if (pendingMindfulAppFlow.value != null) {
      pendingMindfulAppFlow.value = null
      viewModelScope.launch { settingsRepository.recordMindfulOutcome(stayedPresent = true) }
    }
  }

  private fun executeLaunch(app: AppInfo) {
    selectedAppKeyFlow.value = app.componentKey
    val launched = appRepository.launchApp(app)
    if (launched) {
      viewModelScope.launch { settingsRepository.incrementDailyLaunchCount(app.componentKey) }
    }
  }

  fun launchShortcut(shortcut: AppShortcutItem) {
    appRepository.launchShortcut(shortcut)
  }

  fun dialPhoneNumber(number: String?) {
    appRepository.launchDialer(number)
  }

  fun openUrl(url: String) {
    appRepository.openUrl(url)
  }

  fun openWebSearch(query: String) {
    appRepository.openWebSearch(query)
  }

  fun expandNotificationShade() {
    appRepository.expandNotificationShade()
  }

  fun launchCameraShortcut() {
    appRepository.launchCamera(uiState.value.allApps)
  }

  fun openAppInfo(app: AppInfo) {
    appRepository.openAppInfo(app.packageName)
  }

  fun openClockApp() {
    appRepository.openClockApp()
  }

  fun openCalendarApp() {
    calendarRepository.openCalendarApp()
  }

  fun openHomeLauncherSettings() {
    appRepository.openHomeSettings()
  }

  fun createDefaultHomeRoleIntent(): Intent? = appRepository.createDefaultHomeRoleIntent()

  fun toggleFavorite(app: AppInfo) {
    viewModelScope.launch {
      val currentSettings = settingsRepository.settingsFlow.first()
      val currentKeys =
        if (currentSettings.favoriteComponentKeys.isEmpty()) {
          appRepository.resolveDefaultFavorites(uiState.value.allVisibleApps).map { it.componentKey }
        } else {
          currentSettings.favoriteComponentKeys
        }
      val updated =
        if (app.componentKey in currentKeys) currentKeys - app.componentKey
        else currentKeys + app.componentKey
      settingsRepository.setFavorites(updated)
    }
  }

  fun setFavorites(orderedApps: List<AppInfo>) {
    viewModelScope.launch {
      settingsRepository.setFavorites(orderedApps.map { it.componentKey })
    }
  }

  fun saveFolder(existingId: String?, name: String, apps: List<AppInfo>) {
    val cleanName = name.trim()
    if (cleanName.isEmpty() || apps.isEmpty()) return
    val folderId = existingId ?: UUID.randomUUID().toString()
    viewModelScope.launch {
      settingsRepository.saveFolder(
        AppFolder(id = folderId, name = cleanName, componentKeys = apps.map { it.componentKey })
      )
    }
  }

  fun deleteFolder(folderId: String) {
    viewModelScope.launch { settingsRepository.deleteFolder(folderId) }
  }

  fun hideApp(app: AppInfo) {
    viewModelScope.launch { settingsRepository.setAppHidden(app.componentKey, true) }
  }

  fun unhideApp(app: AppInfo) {
    viewModelScope.launch { settingsRepository.setAppHidden(app.componentKey, false) }
  }

  fun renameApp(app: AppInfo, customName: String?) {
    viewModelScope.launch { settingsRepository.setCustomLabel(app.componentKey, customName) }
  }

  fun setThemeMode(themeMode: ThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
  }

  fun setAccent(accent: AccentOption) {
    viewModelScope.launch { settingsRepository.setAccent(accent) }
  }

  fun setIconShape(iconShape: AdaptiveIconShape) {
    viewModelScope.launch { settingsRepository.setIconShape(iconShape) }
  }

  fun setCoastalBackdropEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setCoastalBackdropEnabled(enabled) }
  }

  fun setAlphabetIndexEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setAlphabetIndexEnabled(enabled) }
  }

  fun setHideAppLabels(hidden: Boolean) {
    viewModelScope.launch { settingsRepository.setHideAppLabels(hidden) }
  }

  fun setScrollSpeed(speed: ScrollSpeed) {
    viewModelScope.launch { settingsRepository.setScrollSpeed(speed) }
  }

  fun setHapticFeedbackEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setHapticFeedbackEnabled(enabled) }
  }

  fun setMindfulPauseEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setMindfulPauseEnabled(enabled) }
  }

  fun setMindfulPausedApps(apps: List<AppInfo>) {
    viewModelScope.launch {
      settingsRepository.setMindfulPausedApps(apps.map { it.componentKey }.toSet())
    }
  }

  fun toggleMindfulPausedApp(app: AppInfo) {
    viewModelScope.launch { settingsRepository.toggleMindfulPausedApp(app.componentKey) }
  }

  fun setShowDailyLaunchCounts(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setShowDailyLaunchCounts(enabled) }
  }

  fun setCalendarPreviewEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.setCalendarPreviewEnabled(enabled)
      calendarEventFlow.value =
        if (enabled && calendarRepository.hasCalendarPermission()) {
          calendarRepository.loadNextUpcomingEvent()
        } else {
          null
        }
    }
  }

  fun setReducedMotionEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setReducedMotionEnabled(enabled) }
  }

  fun setSwipeLeftAction(action: HomeGestureAction) {
    viewModelScope.launch { settingsRepository.setSwipeLeftAction(action) }
  }

  fun setSwipeRightAction(action: HomeGestureAction) {
    viewModelScope.launch { settingsRepository.setSwipeRightAction(action) }
  }

  fun setDoubleTapAction(action: HomeGestureAction) {
    viewModelScope.launch { settingsRepository.setDoubleTapAction(action) }
  }

  fun setFocusModeManualEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setFocusModeManualEnabled(enabled) }
  }

  fun toggleFocusModeManual() {
    val current = uiState.value.settings.focusModeManualEnabled
    viewModelScope.launch { settingsRepository.setFocusModeManualEnabled(!current) }
  }

  fun setFocusSchedule(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
    viewModelScope.launch { settingsRepository.setFocusSchedule(enabled, startMinutes, endMinutes) }
  }

  fun setFocusEssentialApps(apps: List<AppInfo>) {
    viewModelScope.launch {
      settingsRepository.setFocusEssentialApps(apps.map { it.componentKey }.toSet())
    }
  }

  fun setFocusTimerDuration(durationMinutes: Int) {
    viewModelScope.launch { settingsRepository.setFocusTimerDuration(durationMinutes) }
  }

  fun startFocusTimer(durationMinutes: Int) {
    val clamped = durationMinutes.coerceIn(5, 90)
    val endMillis = System.currentTimeMillis() + clamped * 60_000L
    viewModelScope.launch { settingsRepository.setFocusTimer(clamped, endMillis) }
  }

  fun stopFocusTimer() {
    val duration = uiState.value.settings.focusTimerDurationMinutes
    viewModelScope.launch { settingsRepository.setFocusTimer(duration, 0L) }
  }

  fun completeOnboarding(selectedFavorites: List<AppInfo>?) {
    viewModelScope.launch {
      if (!selectedFavorites.isNullOrEmpty()) {
        settingsRepository.setFavorites(selectedFavorites.map { it.componentKey })
      }
      settingsRepository.setHasCompletedOnboarding(true)
    }
  }

  fun reopenOnboarding() {
    viewModelScope.launch { settingsRepository.setHasCompletedOnboarding(false) }
  }

  fun exportSettingsBackupJson(): String =
    SettingsRepository.encodeSettingsBackupJson(uiState.value.settings)

  suspend fun importSettingsBackupJson(json: String): Boolean =
    settingsRepository.importSettingsBackup(json)

  fun resetLayout() {
    viewModelScope.launch {
      selectedAppKeyFlow.value = null
      settingsRepository.resetLayout()
    }
  }
}
