package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.HomeGestureAction
import com.example.model.LauncherSettings
import com.example.model.LauncherUiState
import com.example.model.ResolvedFolder
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.MindfulLaunchDialog
import com.example.ui.components.OnboardingWelcomeDialog
import com.example.ui.drawer.AppDrawer
import com.example.ui.home.CoastalGradientOverlay
import com.example.ui.home.GyroscopeParallaxState
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NeonSunsetTheme
import com.example.util.FocusAndSearchUtils
import com.example.util.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

    composeTestRule.onNodeWithTag("hero_focus_timer_start").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(25, timerStartedMinutes)
  }

  @Test
  fun `home screen long-press weather opens city picker and horizontal swipe triggers gesture`() {
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
    composeTestRule.onNodeWithTag("weather_placeholder_card").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    assertEquals(HomeGestureAction.OPEN_CAMERA, triggeredGesture)

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
  fun `onboarding welcome dialog steps through customization and saves starter favorites`() {
    var finishedFavorites: List<AppInfo>? = null
    var chosenAccent by mutableStateOf(AccentOption.EMERALD)

    composeTestRule.setContent {
      NeonSunsetTheme(accentOption = chosenAccent) {
        OnboardingWelcomeDialog(
          allApps = listOf(sampleCalendar, sampleNotes, sampleMusic),
          initialAccent = chosenAccent,
          initialShape = AdaptiveIconShape.ROUNDED_SQUARE,
          initialFavorites = listOf(sampleCalendar, sampleNotes),
          onSelectAccent = { chosenAccent = it },
          onSelectShape = {},
          onFinish = { finishedFavorites = it },
          onSkip = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_accent_purple").performClick()
    assertEquals(AccentOption.PURPLE, chosenAccent)

    composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_finish_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf(sampleCalendar, sampleNotes), finishedFavorites)
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
          onToggleNotificationDots = {},
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
}
