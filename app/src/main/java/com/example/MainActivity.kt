package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.HomeGestureAction
import com.example.model.LauncherDestination
import com.example.ui.components.MindfulLaunchDialog
import com.example.ui.components.OnboardingWelcomeDialog
import com.example.ui.drawer.AppDrawer
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.LocalLauncherIsDark
import com.example.ui.theme.NeonSunsetTheme
import com.example.util.LocalLauncherHapticsEnabled
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

  private val launcherViewModel: LauncherViewModel by viewModels()
  private val homeIntentCounter = androidx.compose.runtime.mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Root back-press guard: as a Home Launcher, MainActivity must never finish() on system back.
    // Compose BackHandlers (AppDrawer, SettingsScreen, expanded folders) are registered after this
    // in LIFO order and take precedence whenever active.
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          // Stay on the Home screen without finishing or recreating MainActivity
        }
      },
    )

    setContent {
      LauncherAppRoot(
        viewModel = launcherViewModel,
        homeIntentTrigger = homeIntentCounter.intValue,
        onSystemBarThemeChange = { isDark ->
          enableEdgeToEdge(
            statusBarStyle =
              if (isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
              else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle =
              if (isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
              else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
          )
        },
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    homeIntentCounter.intValue++
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // Also reset drawer/settings when user leaves or swipes home gesture
    homeIntentCounter.intValue++
  }
}

@Composable
private fun LauncherAppRoot(
  viewModel: LauncherViewModel,
  homeIntentTrigger: Int,
  onSystemBarThemeChange: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  var destination by rememberSaveable { mutableStateOf(LauncherDestination.HOME) }
  var drawerFocusSearch by rememberSaveable { mutableStateOf(false) }
  var drawerInitialLetter by rememberSaveable { mutableStateOf<Char?>(null) }

  // Consume Back on the Home screen so the root launcher Activity never finishes and relaunches.
  BackHandler(enabled = destination == LauncherDestination.HOME) {
    // Intentionally no-op at root Home state
  }

  LaunchedEffect(homeIntentTrigger) {
    if (homeIntentTrigger > 0 && destination != LauncherDestination.HOME) {
      destination = LauncherDestination.HOME
      drawerFocusSearch = false
      drawerInitialLetter = null
      viewModel.onSearchQueryChange("")
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.onLauncherResumed()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  DisposableEffect(context) {
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          viewModel.refreshInstalledApps()
        }
      }
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addDataScheme("package")
      }
    ContextCompat.registerReceiver(
      context,
      receiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )
    onDispose { runCatching { context.unregisterReceiver(receiver) } }
  }

  fun performGestureAction(action: HomeGestureAction) {
    when (action) {
      HomeGestureAction.NONE -> Unit
      HomeGestureAction.OPEN_CAMERA -> viewModel.launchCameraShortcut()
      HomeGestureAction.OPEN_DIALER -> viewModel.dialPhoneNumber(null)
      HomeGestureAction.OPEN_SEARCH -> {
        drawerFocusSearch = true
        drawerInitialLetter = null
        destination = LauncherDestination.ALL_APPS
      }
      HomeGestureAction.OPEN_DRAWER -> {
        drawerFocusSearch = false
        drawerInitialLetter = null
        destination = LauncherDestination.ALL_APPS
      }
      HomeGestureAction.EXPAND_NOTIFICATIONS -> viewModel.expandNotificationShade()
      HomeGestureAction.TOGGLE_FOCUS_MODE -> viewModel.toggleFocusModeManual()
    }
  }

  NeonSunsetTheme(
    accentOption = uiState.settings.accent,
    themeMode = uiState.settings.themeMode,
    iconShape = uiState.settings.iconShape,
  ) {
    val isDark = LocalLauncherIsDark.current
    LaunchedEffect(isDark) { onSystemBarThemeChange(isDark) }

    CompositionLocalProvider(
      LocalLauncherHapticsEnabled provides uiState.settings.hapticFeedbackEnabled
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
      ) {
      when (destination) {
        LauncherDestination.HOME, LauncherDestination.ALL_APPS -> {
          HomeScreen(
            uiState = uiState,
            onOpenDrawer = { focusSearch, jumpLetter ->
              drawerFocusSearch = focusSearch
              drawerInitialLetter = jumpLetter
              destination = LauncherDestination.ALL_APPS
            },
            onOpenSettings = { destination = LauncherDestination.SETTINGS },
            onClockClick = viewModel::openClockApp,
            onRefreshWeather = viewModel::refreshWeather,
            onSearchWeatherCities = viewModel::searchWeatherCities,
            onSelectWeatherLocation = viewModel::selectWeatherLocation,
            onOpenCalendar = viewModel::openCalendarApp,
            onExpandNotificationShade = viewModel::expandNotificationShade,
            onTriggerGestureAction = ::performGestureAction,
            onStartFocusTimer = viewModel::startFocusTimer,
            onStopFocusTimer = viewModel::stopFocusTimer,
            onToggleFocusMode = viewModel::toggleFocusModeManual,
            onLaunchApp = { app -> viewModel.requestLaunchApp(app) },
            onOpenAppInfo = viewModel::openAppInfo,
            onRemoveFromFavorites = viewModel::toggleFavorite,
            onRenameApp = viewModel::renameApp,
            onToggleMindfulPause = viewModel::toggleMindfulPausedApp,
            onSaveFolder = viewModel::saveFolder,
            onDeleteFolder = viewModel::deleteFolder,
            onMoveFavorite = viewModel::moveFavoriteApp,
            onSaveDailyIntention = viewModel::setDailyIntention,
            onLaunchShortcut = viewModel::launchShortcut,
            onAutoCategorizeFolders = { viewModel.autoCategorizeFolders() },
          )

          AppDrawer(
            isOpen = destination == LauncherDestination.ALL_APPS,
            focusSearchOnOpen = drawerFocusSearch,
            initialLetter = drawerInitialLetter,
            uiState = uiState,
            onClose = {
              destination = LauncherDestination.HOME
              drawerFocusSearch = false
              drawerInitialLetter = null
              viewModel.onSearchQueryChange("")
            },
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onLaunchApp = { app ->
              val launchedImmediately = viewModel.requestLaunchApp(app)
              if (launchedImmediately) {
                destination = LauncherDestination.HOME
                drawerFocusSearch = false
                drawerInitialLetter = null
                viewModel.onSearchQueryChange("")
              }
            },
            onLaunchShortcut = { shortcut ->
              viewModel.launchShortcut(shortcut)
              destination = LauncherDestination.HOME
              drawerFocusSearch = false
              drawerInitialLetter = null
              viewModel.onSearchQueryChange("")
            },
            onDialNumber = { number ->
              viewModel.dialPhoneNumber(number)
              destination = LauncherDestination.HOME
              drawerFocusSearch = false
              drawerInitialLetter = null
              viewModel.onSearchQueryChange("")
            },
            onOpenUrl = { url ->
              viewModel.openUrl(url)
              destination = LauncherDestination.HOME
              drawerFocusSearch = false
              drawerInitialLetter = null
              viewModel.onSearchQueryChange("")
            },
            onWebSearch = { query ->
              viewModel.openWebSearch(query)
              destination = LauncherDestination.HOME
              drawerFocusSearch = false
              drawerInitialLetter = null
              viewModel.onSearchQueryChange("")
            },
            onOpenAppInfo = viewModel::openAppInfo,
            onToggleFavorite = viewModel::toggleFavorite,
            onRenameApp = viewModel::renameApp,
            onHideApp = viewModel::hideApp,
            onToggleMindfulPause = viewModel::toggleMindfulPausedApp,
            onSelectProfileFilter = viewModel::setDrawerProfileFilter,
          )
        }
        LauncherDestination.SETTINGS -> {
          SettingsScreen(
            uiState = uiState,
            onBack = { destination = LauncherDestination.HOME },
            onSelectThemeMode = viewModel::setThemeMode,
            onSelectAccent = viewModel::setAccent,
            onSelectIconShape = viewModel::setIconShape,
            onToggleCoastalBackdrop = viewModel::setCoastalBackdropEnabled,
            onSearchWeatherCities = viewModel::searchWeatherCities,
            onSelectWeatherLocation = viewModel::selectWeatherLocation,
            onSaveFavorites = viewModel::setFavorites,
            onSaveFolder = viewModel::saveFolder,
            onDeleteFolder = viewModel::deleteFolder,
            onUnhideApp = viewModel::unhideApp,
            onToggleAlphabetIndex = viewModel::setAlphabetIndexEnabled,
            onToggleHideLabels = viewModel::setHideAppLabels,
            onSelectSwipeLeftAction = viewModel::setSwipeLeftAction,
            onSelectSwipeRightAction = viewModel::setSwipeRightAction,
            onSelectDoubleTapAction = viewModel::setDoubleTapAction,
            onToggleFocusModeManual = viewModel::setFocusModeManualEnabled,
            onSaveFocusSchedule = viewModel::setFocusSchedule,
            onSaveFocusEssentials = viewModel::setFocusEssentialApps,
            onSelectFocusTimerDuration = viewModel::setFocusTimerDuration,
            onToggleMindfulPause = viewModel::setMindfulPauseEnabled,
            onSaveMindfulApps = viewModel::setMindfulPausedApps,
            onToggleDailyLaunchCounts = viewModel::setShowDailyLaunchCounts,
            onToggleCalendarPreview = viewModel::setCalendarPreviewEnabled,
            onSelectScrollSpeed = viewModel::setScrollSpeed,
            onToggleHapticFeedback = viewModel::setHapticFeedbackEnabled,
            onToggleReducedMotion = viewModel::setReducedMotionEnabled,
            onCreateDefaultHomeRoleIntent = viewModel::createDefaultHomeRoleIntent,
            onOpenHomeLauncherSettings = viewModel::openHomeLauncherSettings,
            onDefaultLauncherResult = viewModel::onLauncherResumed,
            onReplayWelcomeSetup = {
              viewModel.reopenOnboarding()
              destination = LauncherDestination.HOME
            },
            onResetLayout = viewModel::resetLayout,
            onExportBackupJson = viewModel::exportSettingsBackupJson,
            onImportBackupJson = viewModel::importSettingsBackupJson,
            onSelectClockStyle = viewModel::setClockStyle,
            onToggleClockSeconds = viewModel::setShowClockSeconds,
            onToggleBatteryIndicator = viewModel::setShowBatteryIndicator,
            onPickCustomWallpaper = viewModel::pickCustomWallpaper,
            onClearCustomWallpaper = viewModel::clearCustomWallpaper,
            onChangeWallpaperDimPercent = viewModel::setWallpaperDimPercent,
            onToggleWidgetStack = viewModel::setWidgetStackEnabled,
            onAutoCategorizeFolders = viewModel::autoCategorizeFolders,
          )
        }
      }

      if (destination == LauncherDestination.HOME &&
        !uiState.isLoading &&
        !uiState.settings.hasCompletedOnboarding
      ) {
        OnboardingWelcomeDialog(
          allApps = uiState.allVisibleApps,
          initialAccent = uiState.settings.accent,
          initialShape = uiState.settings.iconShape,
          initialFavorites = uiState.favoriteApps,
          onSelectAccent = viewModel::setAccent,
          onSelectShape = viewModel::setIconShape,
          onFinish = { selected -> viewModel.completeOnboarding(selected) },
          onSkip = { viewModel.completeOnboarding(null) },
        )
      }

      val mindfulTarget = uiState.pendingMindfulApp
      if (mindfulTarget != null) {
        MindfulLaunchDialog(
          app = mindfulTarget,
          onStayPresent = viewModel::dismissMindfulLaunch,
          onContinueToApp = {
            viewModel.confirmMindfulLaunch()
            if (destination == LauncherDestination.ALL_APPS) {
              destination = LauncherDestination.HOME
              drawerFocusSearch = false
              drawerInitialLetter = null
              viewModel.onSearchQueryChange("")
            }
          },
        )
      }
    }
    }
  }
}
