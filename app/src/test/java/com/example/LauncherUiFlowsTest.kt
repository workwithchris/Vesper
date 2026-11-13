package com.example

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AppFolder
import com.example.model.AppIconShapeProfile
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
import com.example.model.ClockStyle
import com.example.model.DrawerProfileFilter
import com.example.model.HomeGestureAction
import com.example.model.LauncherSettings
import com.example.model.LauncherUiState
import com.example.model.ResolvedFolder
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.MindfulLaunchDialog
import com.example.ui.drawer.AppDrawer
import com.example.ui.home.CoastalGradientOverlay
import com.example.ui.home.GyroscopeParallaxState
import com.example.ui.home.HomeScreen
import com.example.ui.home.SynthwaveClockWidget
import com.example.ui.onboarding.OnboardingWelcomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NeonSunsetTheme
import com.example.util.FocusAndSearchUtils
import com.example.util.LocalLauncherHapticsEnabled
import com.example.util.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherUiFlowsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleCalendar = AppInfo("Calendar", "com.cal", "MainActivity", null)
  private val sampleNotes = AppInfo("Notes", "com.notes", "MainActivity", null)
  private val sampleMusic = AppInfo("Music", "com.music", "MainActivity", null)

  @Test
  fun `home screen displays folder, expands inline folder apps with full long-press menu, and triggers focus timer`() {
    val folder = AppFolder(id = "work", name = "Work", componentKeys = listOf(sampleNotes.componentKey))
    val uiState =
      LauncherUiState(
        isLoading = false,
        allApps = listOf(sampleCalendar, sampleNotes, sampleMusic),
        allVisibleApps = listOf(sampleCalendar, sampleNotes, sampleMusic),
        favoriteApps = listOf(sampleCalendar),
        homeFolders = listOf(ResolvedFolder(folder = folder, apps = listOf(sampleNotes))),
        settings = LauncherSettings(hasCompletedOnboarding = true, reducedMotionEnabled = true),
      )

    var timerStartedMinutes: Int? = null
    var renamedTarget: Pair<String, String?>? = null
    composeTestRule.setContent {
      NeonSunsetTheme {
        HomeScreen(
          uiState = uiState,
          onOpenDrawer = { _, _ -> },
          onOpenSettings = {},
          onClockClick = {},
          onRefreshWeather = {},
          onOpenCalendar = {},
          onExpandNotificationShade = {},
          onTriggerGestureAction = {},
          onStartFocusTimer = { timerStartedMinutes = it },
          onStopFocusTimer = {},
          onToggleFocusMode = {},
          onLaunchApp = {},
          onOpenAppInfo = {},
          onRemoveFromFavorites = {},
          onRenameApp = { app, name -> renamedTarget = app.componentKey to name },
          onToggleMindfulPause = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("status_clock").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_folder_Work").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("folder_expanded_Work").assertIsDisplayed()

    // Long-press app inside expanded folder should reveal App Info / Rename / Mindful Pause menu
    composeTestRule.onNodeWithTag("app_row_com.notes").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Rename app").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("rename_app_input").performTextReplacement("Notes Pad")
    composeTestRule.onNodeWithTag("rename_app_save_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(sampleNotes.componentKey to "Notes Pad", renamedTarget)

    // Swipe up on the stacked widget deck to switch from Weather (slot 0) to Focus Timer (slot 1)
    composeTestRule.onNodeWithTag("home_widget_stack").assertIsDisplayed().performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_focus_timer_start").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(25, timerStartedMinutes)
  }

  @Test
  fun `home screen long-press weather opens city picker and outer-edge horizontal swipe triggers gesture`() {
    val uiState =
      LauncherUiState(
        isLoading = false,
        allApps = listOf(sampleCalendar),
        allVisibleApps = listOf(sampleCalendar),
        favoriteApps = listOf(sampleCalendar),
        settings =
          LauncherSettings(
            hasCompletedOnboarding = true,
            reducedMotionEnabled = true,
            swipeLeftAction = HomeGestureAction.OPEN_CAMERA,
            swipeRightAction = HomeGestureAction.OPEN_DIALER,
          ),
      )

    var triggeredGesture: HomeGestureAction? = null
    composeTestRule.setContent {
      NeonSunsetTheme {
        HomeScreen(
          uiState = uiState,
          onOpenDrawer = { _, _ -> },
          onOpenSettings = {},
          onClockClick = {},
          onRefreshWeather = {},
          onOpenCalendar = {},
          onExpandNotificationShade = {},
          onTriggerGestureAction = { triggeredGesture = it },
          onStartFocusTimer = {},
          onStopFocusTimer = {},
          onToggleFocusMode = {},
          onLaunchApp = {},
          onOpenAppInfo = {},
          onRemoveFromFavorites = {},
          onRenameApp = { _, _ -> },
          onToggleMindfulPause = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Swiping right at outer left bound (Page 0) fires swipeRightAction
    composeTestRule.onNodeWithTag("home_page_0").performTouchInput { swipeRight() }
    composeTestRule.waitForIdle()
    assertEquals(HomeGestureAction.OPEN_DIALER, triggeredGesture)

    // Swiping left navigates across pages 0 -> 1 -> 2, and overscrolling Page 2 fires swipeLeftAction
    composeTestRule.onNodeWithTag("home_horizontal_pager").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_page_1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("home_horizontal_pager").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_page_2").assertIsDisplayed()

    composeTestRule.onNodeWithTag("home_horizontal_pager").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    assertEquals(HomeGestureAction.OPEN_CAMERA, triggeredGesture)

    // Jump back to Page 0 via indicator dot and long-press weather to open city picker
    composeTestRule.onNodeWithTag("home_page_dot_0").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weather_placeholder_card").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weather_city_dialog").assertIsDisplayed()
  }

  @Test
  fun `app drawer surfaces math result, quick phone action, and web search fallback`() {
    var queryState by mutableStateOf("24 * 7")
    var dialedNumber: String? = null

    composeTestRule.setContent {
      val trimmed = queryState.trim()
      val state =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(sampleCalendar, sampleNotes),
          allVisibleApps = listOf(sampleCalendar, sampleNotes),
          filteredDrawerApps = emptyList(),
          drawerSections = emptyList(),
          searchQuery = queryState,
          quickMathResult = MathEvaluator.evaluateToString(trimmed),
          drawerQuickAction = FocusAndSearchUtils.detectDrawerQuickAction(trimmed),
          settings = LauncherSettings(hasCompletedOnboarding = true, reducedMotionEnabled = true),
        )

      NeonSunsetTheme {
        AppDrawer(
          isOpen = true,
          focusSearchOnOpen = false,
          initialLetter = null,
          uiState = state,
          onClose = {},
          onSearchQueryChange = { queryState = it },
          onLaunchApp = {},
          onLaunchShortcut = {},
          onDialNumber = { dialedNumber = it },
          onOpenUrl = {},
          onWebSearch = {},
          onOpenAppInfo = {},
          onToggleFavorite = {},
          onRenameApp = { _, _ -> },
          onHideApp = {},
          onToggleMindfulPause = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_math_result_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("= 168").assertIsDisplayed()

    queryState = "305 555 1234"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("drawer_quick_action_card").assertIsDisplayed().performClick()
    assertEquals("305 555 1234", dialedNumber)

    queryState = "miami sunset weather"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("web_search_action_card").assertIsDisplayed()
  }

  @Test
  fun `mindful launch dialog disables continue during countdown and invokes stay present`() {
    var stayedPresent = false
    composeTestRule.setContent {
      NeonSunsetTheme {
        MindfulLaunchDialog(
          app = sampleMusic,
          onStayPresent = { stayedPresent = true },
          onContinueToApp = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mindful_pause_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mindful_continue_button").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("mindful_stay_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertTrue(stayedPresent)
  }

  @Test
  fun `onboarding welcome screen steps through atmosphere customization, favorites curation, and default home step`() {
    var finishedFavorites: List<AppInfo>? = null
    var chosenAccent by mutableStateOf(AccentOption.EMERALD)
    var chosenShape by mutableStateOf(AdaptiveIconShape.SYSTEM)
    var useSystemWallpaper by mutableStateOf(true)
    var respectShapes by mutableStateOf(true)

    composeTestRule.setContent {
      NeonSunsetTheme(
        accentOption = chosenAccent,
        iconShape = chosenShape,
        respectIndividualIconShapes = respectShapes,
      ) {
        OnboardingWelcomeScreen(
          allApps = listOf(sampleCalendar, sampleNotes, sampleMusic),
          initialAccent = chosenAccent,
          initialShape = chosenShape,
          initialFavorites = listOf(sampleCalendar, sampleNotes),
          onSelectAccent = { chosenAccent = it },
          onSelectShape = { chosenShape = it },
          onFinish = { finishedFavorites = it },
          onSkip = {},
          initialRespectIndividualShapes = respectShapes,
          initialUseSystemWallpaper = useSystemWallpaper,
          reducedMotion = true,
          onToggleRespectIndividualShapes = { respectShapes = it },
          onUsePhoneSystemWallpaper = { useSystemWallpaper = true },
          onApplyMiamiSunsetWallpaper = { useSystemWallpaper = false },
        )
      }
    }

    composeTestRule.waitForIdle()
    // Step 0: Atmospheric Welcome
    composeTestRule.onNodeWithTag("onboarding_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
    composeTestRule.waitForIdle()

    // Step 1: Atmosphere & Live Preview Studio
    composeTestRule.onNodeWithTag("onboarding_live_preview").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_accent_purple").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AccentOption.PURPLE, chosenAccent)

    composeTestRule.onNodeWithTag("onboarding_wallpaper_sunset").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, useSystemWallpaper)

    composeTestRule.onNodeWithTag("onboarding_respect_shapes_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, respectShapes)

    composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
    composeTestRule.waitForIdle()

    // Step 2: Favorites Curation (add Music alongside Calendar & Notes)
    composeTestRule
      .onNodeWithTag("onboarding_favorites_list")
      .performScrollToNode(hasTestTag("onboarding_app_pick_com.music"))
    composeTestRule.onNodeWithTag("onboarding_app_pick_com.music").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
    composeTestRule.waitForIdle()

    // Step 3: Default Home & Gestures Launchpad
    composeTestRule.onNodeWithTag("onboarding_default_home_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_finish_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf(sampleCalendar, sampleNotes, sampleMusic), finishedFavorites)
  }

  @Test
  fun `folder editor dialog creates folder with selected apps`() {
    var savedResult: Pair<String, List<AppInfo>>? = null
    composeTestRule.setContent {
      NeonSunsetTheme {
        FolderEditorDialog(
          existingFolder = null,
          allApps = listOf(sampleCalendar, sampleNotes),
          onSave = { name, apps -> savedResult = name to apps },
          onDelete = null,
          onDismiss = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("folder_editor_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folder_save_button").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("folder_name_input").performTextInput("Focus")
    composeTestRule.onNodeWithTag("folder_app_pick_com.notes").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("folder_save_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()

    assertEquals("Focus", savedResult?.first)
    assertEquals(listOf(sampleNotes), savedResult?.second)
  }

  @Test
  fun `backup restore dialog validates payload and triggers restore callback`() {
    var restoreSucceeded = false
    var importedPayload: String? = null
    composeTestRule.setContent {
      NeonSunsetTheme {
        BackupRestoreDialog(
          exportedJson = "{\"version\":1}",
          onImportJson = { json ->
            importedPayload = json
            json.contains("\"version\"")
          },
          onRestoreSuccess = { restoreSucceeded = true },
          onDismiss = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("backup_restore_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("restore_backup_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals("{\"version\":1}", importedPayload)
    assertTrue(restoreSucceeded)
  }

  @Test
  fun `settings screen toggles switches and opens backup restore dialog`() {
    var coastalBackdropEnabled by mutableStateOf(true)
    var focusModeManual by mutableStateOf(false)

    composeTestRule.setContent {
      val state =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(sampleCalendar, sampleNotes),
          allVisibleApps = listOf(sampleCalendar, sampleNotes),
          settings =
            LauncherSettings(
              hasCompletedOnboarding = true,
              coastalBackdropEnabled = coastalBackdropEnabled,
              focusModeManualEnabled = focusModeManual,
            ),
        )
      NeonSunsetTheme {
        SettingsScreen(
          uiState = state,
          onBack = {},
          onSelectThemeMode = {},
          onSelectAccent = {},
          onSelectIconShape = {},
          onToggleCoastalBackdrop = { coastalBackdropEnabled = it },
          onSearchWeatherCities = { emptyList() },
          onSelectWeatherLocation = {},
          onSaveFavorites = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
          onUnhideApp = {},
          onToggleAlphabetIndex = {},
          onToggleHideLabels = {},
          onSelectSwipeLeftAction = {},
          onSelectSwipeRightAction = {},
          onSelectDoubleTapAction = {},
          onToggleFocusModeManual = { focusModeManual = it },
          onSaveFocusSchedule = { _, _, _ -> },
          onSaveFocusEssentials = {},
          onSelectFocusTimerDuration = {},
          onToggleMindfulPause = {},
          onSaveMindfulApps = {},
          onToggleDailyLaunchCounts = {},
          onToggleCalendarPreview = {},
          onSelectScrollSpeed = {},
          onToggleHapticFeedback = {},
          onToggleReducedMotion = {},
          onCreateDefaultHomeRoleIntent = { null },
          onOpenHomeLauncherSettings = {},
          onDefaultLauncherResult = {},
          onReplayWelcomeSetup = {},
          onResetLayout = {},
          onExportBackupJson = { "{\"version\":1}" },
          onImportBackupJson = { true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coastal_backdrop_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, coastalBackdropEnabled)

    composeTestRule.onNodeWithTag("focus_mode_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(focusModeManual)

    composeTestRule.onNodeWithTag("settings_backup_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("backup_restore_dialog").assertIsDisplayed()
  }

  @Test
  fun `coastal gradient overlay renders sunset illustration with gyroscope parallax state`() {
    val parallaxState = GyroscopeParallaxState(initialX = 0.45f, initialY = -0.35f)
    composeTestRule.setContent {
      NeonSunsetTheme {
        CoastalGradientOverlay(
          hourOfDay = 18.25f,
          reducedMotion = false,
          parallaxState = parallaxState,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coastal_gradient_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sunset_parallax_illustration").assertIsDisplayed()
  }

  @Test
  fun `launcher triggers distinct haptic constants for long press, drawer swipe, and respects disable toggle`() {
    var hapticsEnabled by mutableStateOf(true)
    var drawerOpened = false
    var drawerClosed = false
    var drawerOpenState by mutableStateOf(false)
    var capturedView: View? = null

    composeTestRule.setContent {
      capturedView = LocalView.current
      val uiState =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(sampleCalendar, sampleNotes),
          allVisibleApps = listOf(sampleCalendar, sampleNotes),
          favoriteApps = listOf(sampleCalendar),
          drawerSections =
            listOf(
              com.example.model.AlphabetSection('C', listOf(sampleCalendar)),
              com.example.model.AlphabetSection('N', listOf(sampleNotes)),
            ),
          settings =
            LauncherSettings(
              hasCompletedOnboarding = true,
              reducedMotionEnabled = true,
              hapticFeedbackEnabled = hapticsEnabled,
            ),
        )
      NeonSunsetTheme {
        CompositionLocalProvider(LocalLauncherHapticsEnabled provides hapticsEnabled) {
          HomeScreen(
            uiState = uiState,
            onOpenDrawer = { _, _ ->
              drawerOpened = true
              drawerOpenState = true
            },
            onOpenSettings = {},
            onClockClick = {},
            onRefreshWeather = {},
            onOpenCalendar = {},
            onExpandNotificationShade = {},
            onTriggerGestureAction = {},
            onStartFocusTimer = {},
            onStopFocusTimer = {},
            onToggleFocusMode = {},
            onLaunchApp = {},
            onOpenAppInfo = {},
            onRemoveFromFavorites = {},
            onRenameApp = { _, _ -> },
            onToggleMindfulPause = {},
            onSaveFolder = { _, _, _ -> },
            onDeleteFolder = {},
          )
          AppDrawer(
            isOpen = drawerOpenState,
            focusSearchOnOpen = false,
            initialLetter = null,
            uiState = uiState,
            onClose = {
              drawerClosed = true
              drawerOpenState = false
            },
            onSearchQueryChange = {},
            onLaunchApp = {},
            onLaunchShortcut = {},
            onDialNumber = {},
            onOpenUrl = {},
            onWebSearch = {},
            onOpenAppInfo = {},
            onToggleFavorite = {},
            onRenameApp = { _, _ -> },
            onHideApp = {},
            onToggleMindfulPause = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    val shadowView = Shadows.shadowOf(capturedView!!)

    // 1. Long-press favorite app triggers LONG_PRESS haptic
    composeTestRule.onNodeWithTag("app_row_com.cal").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    assertEquals(HapticFeedbackConstants.LONG_PRESS, shadowView.lastHapticFeedbackPerformed())

    // 2. Swipe up on hero region opens app drawer with GESTURE_START haptic
    composeTestRule.onNodeWithTag("status_clock").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()
    assertTrue(drawerOpened)
    assertEquals(HapticFeedbackConstants.GESTURE_START, shadowView.lastHapticFeedbackPerformed())

    // 3. Tap close drawer button triggers GESTURE_END haptic
    composeTestRule.onNodeWithTag("close_drawer_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(drawerClosed)
    assertEquals(HapticFeedbackConstants.GESTURE_END, shadowView.lastHapticFeedbackPerformed())

    // 4. Tapping bottom drawer chevron triggers subtle CLOCK_TICK haptic when enabled
    composeTestRule.onNodeWithTag("home_search_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_all_apps_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(HapticFeedbackConstants.CLOCK_TICK, shadowView.lastHapticFeedbackPerformed())

    // Close drawer again (GESTURE_END), then disable haptics and verify further taps emit nothing new
    composeTestRule.onNodeWithTag("close_drawer_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(HapticFeedbackConstants.GESTURE_END, shadowView.lastHapticFeedbackPerformed())

    hapticsEnabled = false
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_all_apps_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(HapticFeedbackConstants.GESTURE_END, shadowView.lastHapticFeedbackPerformed())
  }

  @Test
  fun `clock widget renders all three clock styles as frameless text and numbers with battery readout`() {
    var activeStyle by mutableStateOf(ClockStyle.MONO_TERMINAL)
    composeTestRule.setContent {
      NeonSunsetTheme {
        SynthwaveClockWidget(
          formattedTime = "9:41:08",
          amPmLabel = "AM",
          formattedDate = "Friday, November 13",
          locationLabel = "Miami",
          reducedMotion = true,
          onClick = {},
          clockStyle = activeStyle,
          batteryPercent = 82,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("status_clock").assertIsDisplayed()
    composeTestRule.onNodeWithTag("clock_battery_chip", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("· 82%", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText(">", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("9:41:08", useUnmergedTree = true).assertIsDisplayed()

    activeStyle = ClockStyle.MINIMAL_EDITORIAL
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Friday, November 13", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `home screen widget stack swipes vertically to daily intention and favorites long-press offers move and shortcuts`() {
    var savedIntention: String? = null
    var moveDelta: Int? = null
    var launchedShortcutId: String? = null
    val shortcut = AppShortcutItem("new_event", sampleCalendar.packageName, "New event", "Calendar")

    val uiState =
      LauncherUiState(
        isLoading = false,
        allApps = listOf(sampleCalendar, sampleNotes),
        allVisibleApps = listOf(sampleCalendar, sampleNotes),
        favoriteApps = listOf(sampleCalendar, sampleNotes),
        appShortcutsByPackage = mapOf(sampleCalendar.packageName to listOf(shortcut)),
        settings = LauncherSettings(hasCompletedOnboarding = true, reducedMotionEnabled = true),
      )

    composeTestRule.setContent {
      NeonSunsetTheme {
        HomeScreen(
          uiState = uiState,
          onOpenDrawer = { _, _ -> },
          onOpenSettings = {},
          onClockClick = {},
          onRefreshWeather = {},
          onOpenCalendar = {},
          onExpandNotificationShade = {},
          onTriggerGestureAction = {},
          onStartFocusTimer = {},
          onStopFocusTimer = {},
          onToggleFocusMode = {},
          onLaunchApp = {},
          onOpenAppInfo = {},
          onRemoveFromFavorites = {},
          onRenameApp = { _, _ -> },
          onToggleMindfulPause = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
          onMoveFavorite = { _, delta -> moveDelta = delta },
          onSaveDailyIntention = { savedIntention = it },
          onLaunchShortcut = { launchedShortcutId = it.id },
        )
      }
    }

    composeTestRule.waitForIdle()
    // Slot 0 = Weather, swipe up -> Slot 1 = Focus Timer, swipe up -> Slot 2 = Daily Intention
    composeTestRule.onNodeWithTag("weather_placeholder_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_widget_stack").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_focus_timer_start").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_widget_stack").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("daily_intention_pill").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("daily_intention_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("daily_intention_input").performTextReplacement("Deep work morning")
    composeTestRule.onNodeWithTag("daily_intention_save_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Deep work morning", savedIntention)

    // Swipe down returns to Focus Timer (Slot 1)
    composeTestRule.onNodeWithTag("home_widget_stack").performTouchInput { swipeDown() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_focus_timer_start").assertIsDisplayed()

    composeTestRule.onNodeWithTag("app_row_com.cal").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Move down").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, moveDelta)

    composeTestRule.onNodeWithTag("app_row_com.cal").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New event").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals("new_event", launchedShortcutId)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w840dp-h680dp-xhdpi")
  fun `home screen switches to two-pane responsive layout on tablet widths`() {
    val uiState =
      LauncherUiState(
        isLoading = false,
        allApps = listOf(sampleCalendar, sampleNotes),
        allVisibleApps = listOf(sampleCalendar, sampleNotes),
        favoriteApps = listOf(sampleCalendar, sampleNotes),
        settings = LauncherSettings(hasCompletedOnboarding = true, reducedMotionEnabled = true),
      )

    composeTestRule.setContent {
      NeonSunsetTheme {
        HomeScreen(
          uiState = uiState,
          onOpenDrawer = { _, _ -> },
          onOpenSettings = {},
          onClockClick = {},
          onRefreshWeather = {},
          onOpenCalendar = {},
          onExpandNotificationShade = {},
          onTriggerGestureAction = {},
          onStartFocusTimer = {},
          onStopFocusTimer = {},
          onToggleFocusMode = {},
          onLaunchApp = {},
          onOpenAppInfo = {},
          onRemoveFromFavorites = {},
          onRenameApp = { _, _ -> },
          onToggleMindfulPause = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_two_pane_layout").assertIsDisplayed()
    composeTestRule.onNodeWithTag("favorites_list").assertIsDisplayed()
  }

  @Test
  fun `app drawer shows profile filters and frequent apps strip and swiping down closes back to home`() {
    val workApp = AppInfo("Work Mail", "com.workmail", "MainActivity", null, isWorkProfile = true, userSerialNumber = 10L)
    var selectedFilter: DrawerProfileFilter? = null
    var launchedAppPackage: String? = null
    var drawerClosed = false

    val uiState =
      LauncherUiState(
        isLoading = false,
        allApps = listOf(sampleCalendar, workApp),
        allVisibleApps = listOf(sampleCalendar, workApp),
        filteredDrawerApps = listOf(sampleCalendar, workApp),
        drawerSections =
          listOf(
            com.example.model.AlphabetSection('C', listOf(sampleCalendar)),
            com.example.model.AlphabetSection('W', listOf(workApp)),
          ),
        hasWorkProfileApps = true,
        drawerProfileFilter = DrawerProfileFilter.ALL,
        frequentDrawerApps = listOf(sampleCalendar.copy(dailyLaunchCount = 5)),
        settings = LauncherSettings(hasCompletedOnboarding = true, reducedMotionEnabled = true),
      )

    composeTestRule.setContent {
      NeonSunsetTheme {
        AppDrawer(
          isOpen = true,
          focusSearchOnOpen = false,
          initialLetter = null,
          uiState = uiState,
          onClose = { drawerClosed = true },
          onSearchQueryChange = {},
          onLaunchApp = { launchedAppPackage = it.packageName },
          onLaunchShortcut = {},
          onDialNumber = {},
          onOpenUrl = {},
          onWebSearch = {},
          onOpenAppInfo = {},
          onToggleFavorite = {},
          onRenameApp = { _, _ -> },
          onHideApp = {},
          onToggleMindfulPause = {},
          onSelectProfileFilter = { selectedFilter = it },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_filter_work").assertIsDisplayed().performClick()
    assertEquals(DrawerProfileFilter.WORK, selectedFilter)

    composeTestRule.onNodeWithTag("frequent_apps_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frequent_app_com.cal").performClick()
    assertEquals("com.cal", launchedAppPackage)

    composeTestRule.onNodeWithTag("drawer_app_list").performTouchInput { swipeDown() }
    composeTestRule.waitForIdle()
    assertTrue(drawerClosed)
  }

  @Test
  fun `home screen auto-groups apps into categorical folders by usage and expands with glass-morphism panel`() {
    val busyMusic = sampleMusic.copy(dailyLaunchCount = 8)
    val busyCalendar = sampleCalendar.copy(dailyLaunchCount = 3)
    var foldersState by mutableStateOf<List<ResolvedFolder>>(emptyList())

    composeTestRule.setContent {
      val uiState =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(busyMusic, busyCalendar),
          allVisibleApps = listOf(busyMusic, busyCalendar),
          favoriteApps = listOf(busyCalendar),
          homeFolders = foldersState,
          settings = LauncherSettings(hasCompletedOnboarding = true, reducedMotionEnabled = true),
        )
      NeonSunsetTheme {
        HomeScreen(
          uiState = uiState,
          onOpenDrawer = { _, _ -> },
          onOpenSettings = {},
          onClockClick = {},
          onRefreshWeather = {},
          onOpenCalendar = {},
          onExpandNotificationShade = {},
          onTriggerGestureAction = {},
          onStartFocusTimer = {},
          onStopFocusTimer = {},
          onToggleFocusMode = {},
          onLaunchApp = {},
          onOpenAppInfo = {},
          onRemoveFromFavorites = {},
          onRenameApp = { _, _ -> },
          onToggleMindfulPause = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
          onAutoCategorizeFolders = {
            val generated =
              com.example.util.SmartFolderCategorizer.generateSmartFolders(
                apps = listOf(busyMusic, busyCalendar)
              )
            val byKey = listOf(busyMusic, busyCalendar).associateBy { it.componentKey }
            foldersState =
              generated.map { folder ->
                ResolvedFolder(
                  folder = folder,
                  apps = folder.componentKeys.mapNotNull { byKey[it] },
                )
              }
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_auto_categorize_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Media (8 launches) and Productivity (3 launches) smart folders are created
    composeTestRule.onNodeWithTag("home_folder_Media").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("folder_expanded_Media").assertIsDisplayed()
    composeTestRule.onNodeWithText("8× · ", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("app_row_com.music").assertIsDisplayed()
  }

  @Test
  fun `home widget stack swipes to quick scratchpad and app drawer supports auto-launch single match and uninstall`() {
    var savedScratchpad by mutableStateOf("")
    var autoLaunchedPkg: String? = null
    var uninstalledPkg: String? = null
    var storeOpenedPkg: String? = null
    var queryState by mutableStateOf("")
    var isDrawerOpen by mutableStateOf(true)

    composeTestRule.setContent {
      val trimmed = queryState.trim()
      val matchedApps =
        listOf(sampleCalendar, sampleNotes).filter {
          trimmed.isEmpty() || it.label.contains(trimmed, ignoreCase = true)
        }
      val uiState =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(sampleCalendar, sampleNotes),
          allVisibleApps = listOf(sampleCalendar, sampleNotes),
          favoriteApps = listOf(sampleCalendar),
          filteredDrawerApps = matchedApps,
          drawerSections =
            matchedApps
              .groupBy { it.initialLetter }
              .map { (ch, list) -> com.example.model.AlphabetSection(ch, list) },
          searchQuery = queryState,
          activeTimeOfDayBucket = com.example.model.TimeOfDayBucket.EVENING,
          frequentDrawerApps = listOf(sampleCalendar.copy(dailyLaunchCount = 4)),
          settings =
            LauncherSettings(
              hasCompletedOnboarding = true,
              reducedMotionEnabled = true,
              autoLaunchSingleMatch = true,
              quickScratchpadText = savedScratchpad,
            ),
        )
      NeonSunsetTheme {
        Box {
          HomeScreen(
            uiState = uiState,
            onOpenDrawer = { _, _ -> isDrawerOpen = true },
            onOpenSettings = {},
            onClockClick = {},
            onRefreshWeather = {},
            onOpenCalendar = {},
            onExpandNotificationShade = {},
            onTriggerGestureAction = {},
            onStartFocusTimer = {},
            onStopFocusTimer = {},
            onToggleFocusMode = {},
            onLaunchApp = {},
            onOpenAppInfo = {},
            onRemoveFromFavorites = {},
            onRenameApp = { _, _ -> },
            onToggleMindfulPause = {},
            onSaveFolder = { _, _, _ -> },
            onDeleteFolder = {},
            onSaveQuickScratchpad = { savedScratchpad = it },
            isDrawerOpen = isDrawerOpen,
          )
          AppDrawer(
            isOpen = isDrawerOpen,
            focusSearchOnOpen = false,
            initialLetter = null,
            uiState = uiState,
            onClose = { isDrawerOpen = false },
            onSearchQueryChange = { queryState = it },
            onLaunchApp = {
              autoLaunchedPkg = it.packageName
              isDrawerOpen = false
            },
            onLaunchShortcut = {},
            onDialNumber = {},
            onOpenUrl = {},
            onWebSearch = {},
            onOpenAppInfo = {},
            onToggleFavorite = {},
            onRenameApp = { _, _ -> },
            onHideApp = {},
            onToggleMindfulPause = {},
            onUninstallApp = { uninstalledPkg = it.packageName },
            onOpenStorePage = { storeOpenedPkg = it.packageName },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    // Contextual time-of-day header shows FREQUENT · EVENING
    composeTestRule.onNodeWithTag("frequent_apps_header").assertIsDisplayed()
    composeTestRule.onNodeWithText("FREQUENT · EVENING").assertIsDisplayed()

    // Long-press user app in drawer shows Store page and Uninstall
    composeTestRule.onNodeWithTag("drawer_app_list").assertIsDisplayed()
    composeTestRule.onNodeWithText("Notes").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Store page").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals("com.notes", storeOpenedPkg)

    composeTestRule.onNodeWithText("Notes").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Uninstall").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals("com.notes", uninstalledPkg)

    // Typing "Cal" narrows to a single match, auto-launches Calendar immediately, and closes drawer
    queryState = "Cal"
    composeTestRule.waitForIdle()
    assertEquals("com.cal", autoLaunchedPkg)

    // Swipe widget stack on Home (Slot 0 -> 1 -> 2 -> 3 = Quick Scratchpad)
    composeTestRule.onNodeWithTag("home_widget_stack").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_widget_stack").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_widget_stack").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_scratchpad_pill").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_scratchpad_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_scratchpad_input").performTextReplacement("Pick up espresso beans")
    composeTestRule.onNodeWithTag("quick_scratchpad_save_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Pick up espresso beans", savedScratchpad)
  }

  @Test
  fun `coastal gradient overlay lets system wallpaper shine through when useSystemWallpaper is true`() {
    composeTestRule.setContent {
      NeonSunsetTheme {
        CoastalGradientOverlay(
          hourOfDay = 18.25f,
          reducedMotion = true,
          useSystemWallpaper = true,
          wallpaperDimPercent = 35,
          coastalBackdropEnabled = false,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coastal_gradient_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sunset_parallax_illustration").assertDoesNotExist()
  }

  @Test
  fun `settings screen shows synced phone wallpaper dialog, system picker action, and always-visible dim slider`() {
    var systemPickerOpened = false
    var appliedSunsetTarget: com.example.data.AppRepository.WallpaperTarget? = null

    composeTestRule.setContent {
      val state =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(sampleCalendar, sampleNotes),
          allVisibleApps = listOf(sampleCalendar, sampleNotes),
          settings = LauncherSettings(hasCompletedOnboarding = true, useSystemWallpaper = true),
        )
      NeonSunsetTheme {
        SettingsScreen(
          uiState = state,
          onBack = {},
          onSelectThemeMode = {},
          onSelectAccent = {},
          onSelectIconShape = {},
          onToggleCoastalBackdrop = {},
          onSearchWeatherCities = { emptyList() },
          onSelectWeatherLocation = {},
          onSaveFavorites = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
          onUnhideApp = {},
          onToggleAlphabetIndex = {},
          onToggleHideLabels = {},
          onSelectSwipeLeftAction = {},
          onSelectSwipeRightAction = {},
          onSelectDoubleTapAction = {},
          onToggleFocusModeManual = {},
          onSaveFocusSchedule = { _, _, _ -> },
          onSaveFocusEssentials = {},
          onSelectFocusTimerDuration = {},
          onToggleMindfulPause = {},
          onSaveMindfulApps = {},
          onToggleDailyLaunchCounts = {},
          onToggleCalendarPreview = {},
          onSelectScrollSpeed = {},
          onToggleHapticFeedback = {},
          onToggleReducedMotion = {},
          onCreateDefaultHomeRoleIntent = { null },
          onOpenHomeLauncherSettings = {},
          onDefaultLauncherResult = {},
          onReplayWelcomeSetup = {},
          onResetLayout = {},
          onApplyMiamiSunsetWallpaper = { target, onDone ->
            appliedSunsetTarget = target
            onDone("Miami sunset applied to phone & launcher")
          },
          onOpenSystemWallpaperPicker = {
            systemPickerOpened = true
            true
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wallpaper_dim_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_wallpaper_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("wallpaper_options_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallpaper_pick_both_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallpaper_pick_home_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallpaper_system_picker_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(systemPickerOpened)

    composeTestRule.onNodeWithTag("settings_wallpaper_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wallpaper_apply_sunset_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(
      com.example.data.AppRepository.WallpaperTarget.HOME_AND_LOCK,
      appliedSunsetTarget,
    )
  }

  @Test
  fun `home screen parallax pager displays animated sunset wallpaper across focus and atmos pages`() {
    val uiState =
      LauncherUiState(
        isLoading = false,
        allApps = listOf(sampleCalendar, sampleNotes, sampleMusic),
        allVisibleApps = listOf(sampleCalendar, sampleNotes, sampleMusic),
        favoriteApps = listOf(sampleCalendar, sampleNotes),
        settings =
          LauncherSettings(
            hasCompletedOnboarding = true,
            useSystemWallpaper = false,
            coastalBackdropEnabled = true,
            reducedMotionEnabled = true,
            dailyIntention = "Deep focus morning",
            quickScratchpadText = "Review sunset parallax curves",
          ),
      )

    var autoGrouped = false
    composeTestRule.setContent {
      NeonSunsetTheme {
        HomeScreen(
          uiState = uiState,
          onOpenDrawer = { _, _ -> },
          onOpenSettings = {},
          onClockClick = {},
          onRefreshWeather = {},
          onOpenCalendar = {},
          onExpandNotificationShade = {},
          onTriggerGestureAction = {},
          onStartFocusTimer = {},
          onStopFocusTimer = {},
          onToggleFocusMode = {},
          onLaunchApp = {},
          onOpenAppInfo = {},
          onRemoveFromFavorites = {},
          onRenameApp = { _, _ -> },
          onToggleMindfulPause = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
          onAutoCategorizeFolders = { autoGrouped = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sunset_parallax_illustration").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_page_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_page_0").assertIsDisplayed()

    // Navigate to Page 1 (Focus & Daily) via indicator dot
    composeTestRule.onNodeWithTag("home_page_dot_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_page_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("page1_intention_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Deep focus morning").assertIsDisplayed()
    composeTestRule.onNodeWithTag("page1_focus_session_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("page1_focus_timer_start").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("page1_scratchpad_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("page1_mindful_insights_card").performScrollTo().assertIsDisplayed()

    // Navigate to Page 2 (Atmos & Spaces) via swipeLeft and verify redesigned weather & spaces cards
    composeTestRule.onNodeWithTag("home_horizontal_pager").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_page_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("page2_weather_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("FEELS").assertIsDisplayed()
    composeTestRule.onNodeWithText("WIND").assertIsDisplayed()
    composeTestRule.onNodeWithText("CLOUD").assertIsDisplayed()
    composeTestRule.onNodeWithTag("page2_spaces_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("page2_open_drawer_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("page2_auto_group_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(autoGrouped)
  }

  @Test
  fun `home screen vertical list supports adaptive icon shapes and settings omits removed grid controls`() {
    val adaptiveApp =
      sampleCalendar.copy(
        shapeProfile = AppIconShapeProfile.SYSTEM_ADAPTIVE,
        adaptiveAccentArgb = android.graphics.Color.rgb(58, 209, 141),
        hasMonochromeLayer = true,
      )
    val circularApp =
      sampleMusic.copy(
        shapeProfile = AppIconShapeProfile.CIRCULAR,
        adaptiveAccentArgb = android.graphics.Color.rgb(255, 110, 90),
      )
    val customSilhouetteApp =
      sampleNotes.copy(
        shapeProfile = AppIconShapeProfile.CUSTOM_SILHOUETTE,
      )

    var respectIndividual by mutableStateOf(true)
    var showSettings by mutableStateOf(false)
    var launchedPkg: String? = null

    composeTestRule.setContent {
      val uiState =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(adaptiveApp, circularApp, customSilhouetteApp),
          allVisibleApps = listOf(adaptiveApp, circularApp, customSilhouetteApp),
          favoriteApps = listOf(adaptiveApp, circularApp, customSilhouetteApp),
          settings =
            LauncherSettings(
              hasCompletedOnboarding = true,
              reducedMotionEnabled = true,
              iconShape = AdaptiveIconShape.SYSTEM,
              respectIndividualIconShapes = respectIndividual,
            ),
        )
      NeonSunsetTheme(
        iconShape = uiState.settings.iconShape,
        respectIndividualIconShapes = uiState.settings.respectIndividualIconShapes,
      ) {
        if (!showSettings) {
          HomeScreen(
            uiState = uiState,
            onOpenDrawer = { _, _ -> },
            onOpenSettings = { showSettings = true },
            onClockClick = {},
            onRefreshWeather = {},
            onOpenCalendar = {},
            onExpandNotificationShade = {},
            onTriggerGestureAction = {},
            onStartFocusTimer = {},
            onStopFocusTimer = {},
            onToggleFocusMode = {},
            onLaunchApp = { launchedPkg = it.packageName },
            onOpenAppInfo = {},
            onRemoveFromFavorites = {},
            onRenameApp = { _, _ -> },
            onToggleMindfulPause = {},
            onSaveFolder = { _, _, _ -> },
            onDeleteFolder = {},
          )
        } else {
          SettingsScreen(
            uiState = uiState,
            onBack = { showSettings = false },
            onSelectThemeMode = {},
            onSelectAccent = {},
            onSelectIconShape = {},
            onToggleRespectIndividualShapes = { respectIndividual = it },
            onToggleCoastalBackdrop = {},
            onSearchWeatherCities = { emptyList() },
            onSelectWeatherLocation = {},
            onSaveFavorites = {},
            onSaveFolder = { _, _, _ -> },
            onDeleteFolder = {},
            onUnhideApp = {},
            onToggleAlphabetIndex = {},
            onToggleHideLabels = {},
            onSelectSwipeLeftAction = {},
            onSelectSwipeRightAction = {},
            onSelectDoubleTapAction = {},
            onToggleFocusModeManual = {},
            onSaveFocusSchedule = { _, _, _ -> },
            onSaveFocusEssentials = {},
            onSelectFocusTimerDuration = {},
            onToggleMindfulPause = {},
            onSaveMindfulApps = {},
            onToggleDailyLaunchCounts = {},
            onToggleCalendarPreview = {},
            onSelectScrollSpeed = {},
            onToggleHapticFeedback = {},
            onToggleReducedMotion = {},
            onCreateDefaultHomeRoleIntent = { null },
            onOpenHomeLauncherSettings = {},
            onDefaultLauncherResult = {},
            onReplayWelcomeSetup = {},
            onResetLayout = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("favorites_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_favorites_grid").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_layout_toggle_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_search_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_all_apps_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("app_row_com.music").assertIsDisplayed()
    composeTestRule.onNodeWithTag("app_row_com.notes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("app_row_com.cal").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals("com.cal", launchedPkg)

    // Settings still exposes respect_individual_shapes_switch and omits removed grid controls
    showSettings = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_home_layout_row").assertDoesNotExist()
    composeTestRule.onNodeWithTag("respect_individual_shapes_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, respectIndividual)
  }

  @Test
  fun `settings screen shows device location toggle and picker triggers device location detection`() {
    val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    var useDeviceLocation by mutableStateOf(false)
    var deviceLocationTriggered = 0

    composeTestRule.setContent {
      val state =
        LauncherUiState(
          isLoading = false,
          allApps = listOf(sampleCalendar, sampleNotes),
          allVisibleApps = listOf(sampleCalendar, sampleNotes),
          settings =
            LauncherSettings(
              hasCompletedOnboarding = true,
              useDeviceLocation = useDeviceLocation,
              weatherLocation = com.example.model.WeatherLocation("Tokyo", "Japan", 35.6762, 139.6503),
            ),
        )
      NeonSunsetTheme {
        SettingsScreen(
          uiState = state,
          onBack = {},
          onSelectThemeMode = {},
          onSelectAccent = {},
          onSelectIconShape = {},
          onToggleCoastalBackdrop = {},
          onSearchWeatherCities = { emptyList() },
          onSelectWeatherLocation = {},
          onSaveFavorites = {},
          onSaveFolder = { _, _, _ -> },
          onDeleteFolder = {},
          onUnhideApp = {},
          onToggleAlphabetIndex = {},
          onToggleHideLabels = {},
          onSelectSwipeLeftAction = {},
          onSelectSwipeRightAction = {},
          onSelectDoubleTapAction = {},
          onToggleFocusModeManual = {},
          onSaveFocusSchedule = { _, _, _ -> },
          onSaveFocusEssentials = {},
          onSelectFocusTimerDuration = {},
          onToggleMindfulPause = {},
          onSaveMindfulApps = {},
          onToggleDailyLaunchCounts = {},
          onToggleCalendarPreview = {},
          onSelectScrollSpeed = {},
          onToggleHapticFeedback = {},
          onToggleReducedMotion = {},
          onCreateDefaultHomeRoleIntent = { null },
          onOpenHomeLauncherSettings = {},
          onDefaultLauncherResult = {},
          onReplayWelcomeSetup = {},
          onResetLayout = {},
          onToggleUseDeviceLocation = {
            useDeviceLocation = it
            if (it) deviceLocationTriggered++
          },
          onUseDeviceLocation = {
            useDeviceLocation = true
            deviceLocationTriggered++
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_device_location_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(useDeviceLocation)
    assertEquals(1, deviceLocationTriggered)

    composeTestRule.onNodeWithTag("settings_weather_location_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weather_city_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weather_use_device_location_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(2, deviceLocationTriggered)
  }
}

