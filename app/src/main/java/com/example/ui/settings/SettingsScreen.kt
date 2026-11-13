package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.ClockStyle
import com.example.model.HomeGestureAction
import com.example.model.LauncherUiState
import com.example.model.ScrollSpeed
import com.example.model.ThemeMode
import com.example.model.WeatherLocation
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.FavoriteAppsPickerDialog
import com.example.ui.components.FocusScheduleDialog
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.HiddenAppsDialog
import com.example.ui.components.MindfulAppsPickerDialog
import com.example.ui.components.MindfulInsightsDialog
import com.example.ui.components.WeatherCityPickerDialog
import com.example.util.FocusAndSearchUtils
import com.example.util.LauncherHapticEvent
import com.example.util.rememberLauncherHaptics
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  uiState: LauncherUiState,
  onBack: () -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onSelectAccent: (AccentOption) -> Unit,
  onSelectIconShape: (AdaptiveIconShape) -> Unit,
  onToggleCoastalBackdrop: (Boolean) -> Unit,
  onSearchWeatherCities: suspend (String) -> List<WeatherLocation>,
  onSelectWeatherLocation: (WeatherLocation) -> Unit,
  onSaveFavorites: (List<AppInfo>) -> Unit,
  onSaveFolder: (existingId: String?, name: String, apps: List<AppInfo>) -> Unit,
  onDeleteFolder: (folderId: String) -> Unit,
  onUnhideApp: (AppInfo) -> Unit,
  onToggleAlphabetIndex: (Boolean) -> Unit,
  onToggleHideLabels: (Boolean) -> Unit,
  onSelectSwipeLeftAction: (HomeGestureAction) -> Unit,
  onSelectSwipeRightAction: (HomeGestureAction) -> Unit,
  onSelectDoubleTapAction: (HomeGestureAction) -> Unit,
  onToggleFocusModeManual: (Boolean) -> Unit,
  onSaveFocusSchedule: (Boolean, Int, Int) -> Unit,
  onSaveFocusEssentials: (List<AppInfo>) -> Unit,
  onSelectFocusTimerDuration: (Int) -> Unit,
  onToggleMindfulPause: (Boolean) -> Unit,
  onSaveMindfulApps: (List<AppInfo>) -> Unit,
  onToggleDailyLaunchCounts: (Boolean) -> Unit,
  onToggleCalendarPreview: (Boolean) -> Unit,
  onSelectScrollSpeed: (ScrollSpeed) -> Unit,
  onToggleHapticFeedback: (Boolean) -> Unit,
  onToggleReducedMotion: (Boolean) -> Unit,
  onCreateDefaultHomeRoleIntent: () -> Intent?,
  onOpenHomeLauncherSettings: () -> Unit,
  onDefaultLauncherResult: () -> Unit,
  onReplayWelcomeSetup: () -> Unit,
  onResetLayout: () -> Unit,
  onExportBackupJson: () -> String = { "" },
  onImportBackupJson: suspend (String) -> Boolean = { false },
  modifier: Modifier = Modifier,
  onSelectClockStyle: (ClockStyle) -> Unit = {},
  onToggleClockSeconds: (Boolean) -> Unit = {},
  onToggleBatteryIndicator: (Boolean) -> Unit = {},
  onPickCustomWallpaper: (Uri) -> Unit = {},
  onClearCustomWallpaper: () -> Unit = {},
  onChangeWallpaperDimPercent: (Int) -> Unit = {},
  onToggleWidgetStack: (Boolean) -> Unit = {},
  onAutoCategorizeFolders: ((Int) -> Unit) -> Unit = {},
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val settings = uiState.settings
  val accentColor = MaterialTheme.colorScheme.primary
  val launcherHaptics = rememberLauncherHaptics(settings.hapticFeedbackEnabled)

  BackHandler {
    launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
    onBack()
  }

  var showThemeDialog by remember { mutableStateOf(false) }
  var showClockStyleDialog by remember { mutableStateOf(false) }
  var showWallpaperOptionsDialog by remember { mutableStateOf(false) }
  var showIconShapeDialog by remember { mutableStateOf(false) }
  var showCityPickerDialog by remember { mutableStateOf(false) }
  var showFavoritesDialog by remember { mutableStateOf(false) }
  var editingFolder by remember { mutableStateOf<AppFolder?>(null) }
  var creatingFolder by remember { mutableStateOf(false) }
  var showHiddenAppsDialog by remember { mutableStateOf(false) }
  var gestureDialogTarget by remember { mutableStateOf<String?>(null) }
  var showFocusScheduleDialog by remember { mutableStateOf(false) }
  var showFocusEssentialsDialog by remember { mutableStateOf(false) }
  var showFocusTimerDialog by remember { mutableStateOf(false) }
  var showInsightsDialog by remember { mutableStateOf(false) }
  var showMindfulAppsDialog by remember { mutableStateOf(false) }
  var showScrollSpeedDialog by remember { mutableStateOf(false) }
  var showBackupDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

  val wallpaperPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        onPickCustomWallpaper(uri)
        scope.launch { snackbarHostState.showSnackbar("Custom wallpaper applied") }
      }
    }

  val calendarPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        onToggleCalendarPreview(true)
      } else {
        onToggleCalendarPreview(false)
        scope.launch {
          snackbarHostState.showSnackbar("Calendar permission is required to show upcoming events")
        }
      }
    }

  val roleRequestLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      onDefaultLauncherResult()
    }

  fun triggerDefaultLauncherRequest() {
    val roleIntent = onCreateDefaultHomeRoleIntent()
    if (roleIntent != null) {
      runCatching { roleRequestLauncher.launch(roleIntent) }
        .onFailure { onOpenHomeLauncherSettings() }
    } else {
      onOpenHomeLauncherSettings()
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .statusBarsPadding()
          .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
      ) {
        IconButton(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            onBack()
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.settings_title),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))

        LauncherHealthStatusCard(
          context = context,
          isDefaultHomeLauncher = uiState.isDefaultHomeLauncher,
          onRequestDefaultLauncher = ::triggerDefaultLauncherRequest,
          onRequestCalendarPermission = {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
          },
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader("APPEARANCE")

        SettingsValueRow(
          icon = Icons.Outlined.Palette,
          title = "Theme",
          value = settings.themeMode.label,
          onClick = { showThemeDialog = true },
          testTag = "settings_theme_row",
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.WaterDrop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(16.dp))
          Text(
            text = "Accent color",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val isDarkTheme = com.example.ui.theme.LocalLauncherIsDark.current
            AccentOption.entries.forEach { option ->
              val selected = settings.accent == option
              Box(
                modifier =
                  Modifier
                    .size(if (selected) 24.dp else 20.dp)
                    .clip(CircleShape)
                    .background(option.resolvedPrimary(isDarkTheme))
                    .border(
                      width = if (selected) 2.dp else 1.dp,
                      color =
                        if (selected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outlineVariant,
                      shape = CircleShape,
                    )
                    .clickable {
                      launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                      onSelectAccent(option)
                    }
                    .testTag("accent_option_${option.name.lowercase()}"),
              )
            }
          }
        }

        SettingsValueRow(
          icon = Icons.Outlined.Category,
          title = "Icon shape",
          value = settings.iconShape.label,
          onClick = { showIconShapeDialog = true },
          testTag = "settings_icon_shape_row",
        )

        SettingsValueRow(
          icon = Icons.Outlined.Style,
          title = "Clock style",
          value = settings.clockStyle.label,
          onClick = { showClockStyleDialog = true },
          testTag = "settings_clock_style_row",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.AccessTime,
          title = "Show clock seconds",
          checked = settings.showClockSeconds,
          onCheckedChange = onToggleClockSeconds,
          accentColor = accentColor,
          testTag = "clock_seconds_switch",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.BatteryFull,
          title = "Battery badge on clock",
          checked = settings.showBatteryIndicator,
          onCheckedChange = onToggleBatteryIndicator,
          accentColor = accentColor,
          testTag = "battery_indicator_switch",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.Water,
          title = "Coastal atmosphere",
          checked = settings.coastalBackdropEnabled,
          onCheckedChange = onToggleCoastalBackdrop,
          accentColor = accentColor,
          testTag = "coastal_backdrop_switch",
        )

        val hasCustomWallpaper = !settings.customWallpaperPath.isNullOrBlank()
        SettingsValueRow(
          icon = Icons.Outlined.Wallpaper,
          title = "Backdrop wallpaper",
          value = if (hasCustomWallpaper) "Custom photo" else "Miami sunset",
          onClick = {
            if (hasCustomWallpaper) {
              showWallpaperOptionsDialog = true
            } else {
              wallpaperPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
          },
          testTag = "settings_wallpaper_row",
        )

        if (hasCustomWallpaper) {
          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.BrightnessMedium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(16.dp))
              Text(
                text = "Wallpaper dim",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = "${settings.wallpaperDimPercent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Slider(
              value = settings.wallpaperDimPercent.toFloat(),
              onValueChange = { onChangeWallpaperDimPercent(it.toInt()) },
              valueRange = 20f..85f,
              colors =
                SliderDefaults.colors(
                  thumbColor = accentColor,
                  activeTrackColor = accentColor,
                ),
              modifier = Modifier.fillMaxWidth().padding(start = 38.dp).testTag("wallpaper_dim_slider"),
            )
          }
        }

        SettingsValueRow(
          icon = Icons.Outlined.LocationOn,
          title = "Weather location",
          value = settings.weatherLocation.name,
          onClick = { showCityPickerDialog = true },
          testTag = "settings_weather_location_row",
        )

        SectionDivider()
        SectionHeader("HOME SCREEN & FOLDERS")

        SettingsValueRow(
          icon = Icons.Outlined.StarOutline,
          title = "Favorite apps",
          value = "${uiState.favoriteApps.size}",
          onClick = { showFavoritesDialog = true },
          testTag = "settings_favorites_row",
        )

        settings.folders.forEach { folder ->
          SettingsValueRow(
            icon = Icons.Outlined.FolderOpen,
            title = "Folder · ${folder.name}",
            value = "${folder.componentKeys.size} apps",
            onClick = { editingFolder = folder },
            testTag = "settings_folder_${folder.name}",
          )
        }

        SettingsValueRow(
          icon = Icons.Outlined.CreateNewFolder,
          title = "New folder",
          value = null,
          onClick = { creatingFolder = true },
          testTag = "settings_create_folder_row",
        )

        SettingsValueRow(
          icon = Icons.Outlined.Category,
          title = "Auto-group by usage",
          value = "Smart folders",
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
            onAutoCategorizeFolders { count ->
              scope.launch {
                snackbarHostState.showSnackbar("Grouped apps into $count smart folders by usage")
              }
            }
          },
          testTag = "settings_auto_categorize_folders_row",
        )

        SettingsValueRow(
          icon = Icons.Outlined.VisibilityOff,
          title = "Hidden apps",
          value = if (uiState.hiddenApps.isEmpty()) "None" else "${uiState.hiddenApps.size} hidden",
          onClick = { showHiddenAppsDialog = true },
          testTag = "settings_hidden_apps_row",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.Layers,
          title = "Compact widget stack",
          checked = settings.widgetStackEnabled,
          onCheckedChange = onToggleWidgetStack,
          accentColor = accentColor,
          testTag = "widget_stack_switch",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.Tag,
          title = "Alphabet index",
          checked = settings.alphabetIndexEnabled,
          onCheckedChange = onToggleAlphabetIndex,
          accentColor = accentColor,
          testTag = "alphabet_index_switch",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.VisibilityOff,
          title = "Hide app labels",
          checked = settings.hideAppLabels,
          onCheckedChange = onToggleHideLabels,
          accentColor = accentColor,
          testTag = "hide_labels_switch",
        )

        SectionDivider()
        SectionHeader("HOME GESTURES")

        SettingsValueRow(
          icon = Icons.Outlined.Gesture,
          title = "Swipe left",
          value = settings.swipeLeftAction.label,
          onClick = { gestureDialogTarget = "left" },
          testTag = "settings_swipe_left_row",
        )
        SettingsValueRow(
          icon = Icons.Outlined.Gesture,
          title = "Swipe right",
          value = settings.swipeRightAction.label,
          onClick = { gestureDialogTarget = "right" },
          testTag = "settings_swipe_right_row",
        )
        SettingsValueRow(
          icon = Icons.Outlined.TouchApp,
          title = "Double tap",
          value = settings.doubleTapAction.label,
          onClick = { gestureDialogTarget = "double_tap" },
          testTag = "settings_double_tap_row",
        )

        SectionDivider()
        SectionHeader("FOCUS & MINDFULNESS")

        SettingsToggleRow(
          icon = Icons.Outlined.SelfImprovement,
          title = "Focus Mode",
          checked = settings.focusModeManualEnabled,
          onCheckedChange = onToggleFocusModeManual,
          accentColor = accentColor,
          testTag = "focus_mode_switch",
        )

        val scheduleCopy =
          if (settings.focusScheduleEnabled) {
            "${FocusAndSearchUtils.formatMinutesOfDay(settings.focusScheduleStartMinutes)}–${
              FocusAndSearchUtils.formatMinutesOfDay(settings.focusScheduleEndMinutes)
            }"
          } else {
            "Off"
          }
        SettingsValueRow(
          icon = Icons.Outlined.Schedule,
          title = "Focus schedule",
          value = scheduleCopy,
          onClick = { showFocusScheduleDialog = true },
          testTag = "settings_focus_schedule_row",
        )

        SettingsValueRow(
          icon = Icons.Outlined.StarOutline,
          title = "Focus essential apps",
          value =
            if (settings.focusEssentialComponentKeys.isEmpty()) "All"
            else "${settings.focusEssentialComponentKeys.size} apps",
          onClick = { showFocusEssentialsDialog = true },
          testTag = "settings_focus_essentials_row",
        )

        SettingsValueRow(
          icon = Icons.Outlined.Timer,
          title = "Focus timer duration",
          value = "${settings.focusTimerDurationMinutes} min",
          onClick = { showFocusTimerDialog = true },
          testTag = "settings_focus_timer_row",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.SelfImprovement,
          title = "Mindful launch pause",
          checked = settings.mindfulPauseEnabled,
          onCheckedChange = onToggleMindfulPause,
          accentColor = accentColor,
          testTag = "mindful_pause_switch",
        )

        SettingsValueRow(
          icon = Icons.Outlined.SelfImprovement,
          title = "Mindful pause apps",
          value = "${settings.mindfulPausedComponentKeys.size} apps",
          onClick = { showMindfulAppsDialog = true },
          testTag = "settings_mindful_apps_row",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.QueryStats,
          title = "Daily launch counts",
          checked = settings.showDailyLaunchCounts,
          onCheckedChange = onToggleDailyLaunchCounts,
          accentColor = accentColor,
          testTag = "daily_launch_counts_switch",
        )

        SettingsValueRow(
          icon = Icons.Outlined.Insights,
          title = "Today's mindful summary",
          value = "${uiState.mindfulInsights.totalLaunchesToday} opens",
          onClick = { showInsightsDialog = true },
          testTag = "settings_mindful_insights_row",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.CalendarToday,
          title = "Upcoming calendar event",
          checked = settings.calendarPreviewEnabled,
          onCheckedChange = { wantEnable ->
            if (!wantEnable) {
              onToggleCalendarPreview(false)
            } else {
              val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                  PackageManager.PERMISSION_GRANTED
              if (granted) {
                onToggleCalendarPreview(true)
              } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
              }
            }
          },
          accentColor = accentColor,
          testTag = "calendar_preview_switch",
        )

        SectionDivider()
        SectionHeader("BEHAVIOR")

        SettingsValueRow(
          icon = Icons.Outlined.Bolt,
          title = "Scroll speed",
          value = settings.scrollSpeed.label,
          onClick = { showScrollSpeedDialog = true },
          testTag = "settings_scroll_speed_row",
        )

        SettingsToggleRow(
          icon = Icons.Outlined.GraphicEq,
          title = "Haptic feedback",
          checked = settings.hapticFeedbackEnabled,
          onCheckedChange = onToggleHapticFeedback,
          accentColor = accentColor,
          testTag = "haptic_feedback_switch",
          forceWhenToggledOn = true,
        )

        SettingsToggleRow(
          icon = Icons.Outlined.SlowMotionVideo,
          title = "Reduced motion",
          checked = settings.reducedMotionEnabled,
          onCheckedChange = onToggleReducedMotion,
          accentColor = accentColor,
          testTag = "reduced_motion_switch",
        )

        SectionDivider()

        SettingsValueRow(
          icon = Icons.Outlined.Backup,
          title = "Backup & restore",
          value = "JSON",
          onClick = { showBackupDialog = true },
          testTag = "settings_backup_row",
        )

        SettingsValueRow(
          icon = Icons.Outlined.Info,
          title = "About launcher",
          value = null,
          onClick = { showAboutDialog = true },
          testTag = "settings_about_row",
        )

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  if (showThemeDialog) {
    RadioOptionDialog(
      title = "Theme",
      options = ThemeMode.entries,
      selected = settings.themeMode,
      labelFor = { it.label },
      onSelect = {
        onSelectThemeMode(it)
        showThemeDialog = false
      },
      onDismiss = { showThemeDialog = false },
    )
  }

  if (showIconShapeDialog) {
    RadioOptionDialog(
      title = "Icon shape",
      options = AdaptiveIconShape.entries,
      selected = settings.iconShape,
      labelFor = { it.label },
      onSelect = {
        onSelectIconShape(it)
        showIconShapeDialog = false
      },
      onDismiss = { showIconShapeDialog = false },
    )
  }

  if (showClockStyleDialog) {
    RadioOptionDialog(
      title = "Clock style",
      options = ClockStyle.entries,
      selected = settings.clockStyle,
      labelFor = { it.label },
      onSelect = {
        onSelectClockStyle(it)
        showClockStyleDialog = false
      },
      onDismiss = { showClockStyleDialog = false },
    )
  }

  if (showWallpaperOptionsDialog) {
    AlertDialog(
      onDismissRequest = { showWallpaperOptionsDialog = false },
      shape = RoundedCornerShape(12.dp),
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text("Backdrop wallpaper") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Choose another photo from your library or restore the built-in Miami sunset artwork.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = {
              showWallpaperOptionsDialog = false
              wallpaperPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Pick another photo")
          }
          OutlinedButton(
            onClick = {
              showWallpaperOptionsDialog = false
              onClearCustomWallpaper()
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Reset to Miami sunset")
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showWallpaperOptionsDialog = false }) { Text("Close") }
      },
    )
  }

  if (showCityPickerDialog) {
    WeatherCityPickerDialog(
      currentLocation = settings.weatherLocation,
      onSearchCities = onSearchWeatherCities,
      onSelectLocation = { location ->
        onSelectWeatherLocation(location)
        showCityPickerDialog = false
      },
      onDismiss = { showCityPickerDialog = false },
    )
  }

  if (showScrollSpeedDialog) {
    RadioOptionDialog(
      title = "Scroll speed",
      options = ScrollSpeed.entries,
      selected = settings.scrollSpeed,
      labelFor = { it.label },
      onSelect = {
        onSelectScrollSpeed(it)
        showScrollSpeedDialog = false
      },
      onDismiss = { showScrollSpeedDialog = false },
    )
  }

  val gestureTarget = gestureDialogTarget
  if (gestureTarget != null) {
    val currentSelection =
      when (gestureTarget) {
        "left" -> settings.swipeLeftAction
        "right" -> settings.swipeRightAction
        else -> settings.doubleTapAction
      }
    val title =
      when (gestureTarget) {
        "left" -> "Swipe left action"
        "right" -> "Swipe right action"
        else -> "Double tap action"
      }
    RadioOptionDialog(
      title = title,
      options = HomeGestureAction.entries,
      selected = currentSelection,
      labelFor = { it.label },
      onSelect = { action ->
        when (gestureTarget) {
          "left" -> onSelectSwipeLeftAction(action)
          "right" -> onSelectSwipeRightAction(action)
          else -> onSelectDoubleTapAction(action)
        }
        gestureDialogTarget = null
      },
      onDismiss = { gestureDialogTarget = null },
    )
  }

  if (showFocusScheduleDialog) {
    FocusScheduleDialog(
      enabled = settings.focusScheduleEnabled,
      startMinutes = settings.focusScheduleStartMinutes,
      endMinutes = settings.focusScheduleEndMinutes,
      onSave = { enabled, startMin, endMin ->
        onSaveFocusSchedule(enabled, startMin, endMin)
        showFocusScheduleDialog = false
      },
      onDismiss = { showFocusScheduleDialog = false },
    )
  }

  if (showFocusEssentialsDialog) {
    MindfulAppsPickerDialog(
      allApps = uiState.allVisibleApps,
      pausedKeys = settings.focusEssentialComponentKeys,
      title = "Focus essential apps",
      subtitle = "Only selected apps appear on your home screen while Focus Mode is active.",
      onSave = { apps ->
        onSaveFocusEssentials(apps)
        showFocusEssentialsDialog = false
      },
      onDismiss = { showFocusEssentialsDialog = false },
    )
  }

  if (showFocusTimerDialog) {
    RadioOptionDialog(
      title = "Focus timer duration",
      options = listOf(15, 25, 45, 60),
      selected = settings.focusTimerDurationMinutes,
      labelFor = { "$it minutes" },
      onSelect = { mins ->
        onSelectFocusTimerDuration(mins)
        showFocusTimerDialog = false
      },
      onDismiss = { showFocusTimerDialog = false },
    )
  }

  if (showInsightsDialog) {
    MindfulInsightsDialog(
      insights = uiState.mindfulInsights,
      onDismiss = { showInsightsDialog = false },
    )
  }

  if (showFavoritesDialog) {
    FavoriteAppsPickerDialog(
      allApps = uiState.allVisibleApps,
      currentFavorites = uiState.favoriteApps,
      onSave = { selected ->
        onSaveFavorites(selected)
        showFavoritesDialog = false
      },
      onDismiss = { showFavoritesDialog = false },
    )
  }

  if (creatingFolder || editingFolder != null) {
    val activeFolder = editingFolder
    FolderEditorDialog(
      existingFolder = activeFolder,
      allApps = uiState.allVisibleApps,
      onSave = { name, apps ->
        onSaveFolder(activeFolder?.id, name, apps)
        creatingFolder = false
        editingFolder = null
      },
      onDelete =
        activeFolder?.let { folder ->
          {
            onDeleteFolder(folder.id)
            editingFolder = null
          }
        },
      onDismiss = {
        creatingFolder = false
        editingFolder = null
      },
    )
  }

  if (showHiddenAppsDialog) {
    HiddenAppsDialog(
      hiddenApps = uiState.hiddenApps,
      onUnhide = onUnhideApp,
      onDismiss = { showHiddenAppsDialog = false },
    )
  }

  if (showMindfulAppsDialog) {
    MindfulAppsPickerDialog(
      allApps = uiState.allApps,
      pausedKeys = settings.mindfulPausedComponentKeys,
      onSave = { selected ->
        onSaveMindfulApps(selected)
        showMindfulAppsDialog = false
      },
      onDismiss = { showMindfulAppsDialog = false },
    )
  }

  if (showBackupDialog) {
    val snapshotJson = remember(settings) { onExportBackupJson() }
    BackupRestoreDialog(
      exportedJson = snapshotJson,
      onImportJson = onImportBackupJson,
      onRestoreSuccess = {
        showBackupDialog = false
        scope.launch {
          snackbarHostState.showSnackbar("Settings restored from backup")
        }
      },
      onDismiss = { showBackupDialog = false },
    )
  }

  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      shape = RoundedCornerShape(12.dp),
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text("About launcher") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text =
              "${stringResource(R.string.app_name)} • ${uiState.allApps.size} installed apps detected with adaptive icon outlines.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              showAboutDialog = false
              triggerDefaultLauncherRequest()
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Set as Default Home Launcher")
          }
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              showAboutDialog = false
              onReplayWelcomeSetup()
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.replay_welcome))
          }
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
              onResetLayout()
              showAboutDialog = false
              scope.launch {
                snackbarHostState.showSnackbar("Launcher preferences reset to defaults")
              }
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Reset to Defaults")
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
      },
    )
  }
}
