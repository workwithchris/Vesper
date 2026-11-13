package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AlphabetSection
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
import com.example.model.CalendarEventPreview
import com.example.model.LauncherSettings
import com.example.model.LauncherUiState
import com.example.model.ResolvedFolder
import com.example.model.ThemeMode
import com.example.model.WeatherEffectType
import com.example.model.WeatherInfo
import com.example.ui.drawer.AppDrawer
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NeonSunsetTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotGeneratorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun requireScreenshotFlag() {
    Assume.assumeTrue(
      "Run with GENERATE_SCREENSHOTS=true to regenerate README screenshots",
      System.getenv("GENERATE_SCREENSHOTS") == "true" ||
        System.getProperty("generate.screenshots") == "true",
    )
  }

  private fun outlineGlyph(drawBlock: (Canvas, Paint) -> Unit): ImageBitmap {
    val size = 96
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }
    drawBlock(canvas, paint)
    return bmp.asImageBitmap()
  }

  private fun sampleApps(): List<AppInfo> {
    val calendarIcon = outlineGlyph { c, p ->
      c.drawRoundRect(RectF(18f, 22f, 78f, 78f), 10f, 10f, p)
      c.drawLine(18f, 38f, 78f, 38f, p)
      c.drawLine(34f, 14f, 34f, 26f, p)
      c.drawLine(62f, 14f, 62f, 26f, p)
    }
    val cameraIcon = outlineGlyph { c, p ->
      c.drawRoundRect(RectF(14f, 28f, 82f, 74f), 12f, 12f, p)
      c.drawCircle(48f, 51f, 14f, p)
      c.drawLine(34f, 28f, 40f, 18f, p)
      c.drawLine(40f, 18f, 56f, 18f, p)
      c.drawLine(56f, 18f, 62f, 28f, p)
    }
    val mailIcon = outlineGlyph { c, p ->
      c.drawRoundRect(RectF(14f, 24f, 82f, 72f), 8f, 8f, p)
      val path = Path().apply {
        moveTo(16f, 28f)
        lineTo(48f, 52f)
        lineTo(80f, 28f)
      }
      c.drawPath(path, p)
    }
    val musicIcon = outlineGlyph { c, p ->
      c.drawCircle(34f, 68f, 10f, p)
      c.drawCircle(66f, 62f, 10f, p)
      c.drawLine(44f, 68f, 44f, 24f, p)
      c.drawLine(76f, 62f, 76f, 18f, p)
      c.drawLine(44f, 24f, 76f, 18f, p)
    }
    val notesIcon = outlineGlyph { c, p ->
      c.drawRoundRect(RectF(20f, 14f, 76f, 82f), 8f, 8f, p)
      c.drawLine(32f, 34f, 64f, 34f, p)
      c.drawLine(32f, 48f, 64f, 48f, p)
      c.drawLine(32f, 62f, 54f, 62f, p)
    }
    val terminalIcon = outlineGlyph { c, p ->
      c.drawRoundRect(RectF(14f, 20f, 82f, 76f), 10f, 10f, p)
      c.drawLine(28f, 38f, 40f, 48f, p)
      c.drawLine(28f, 58f, 40f, 48f, p)
      c.drawLine(48f, 58f, 66f, 58f, p)
    }
    val slackIcon = outlineGlyph { c, p ->
      c.drawLine(36f, 20f, 36f, 76f, p)
      c.drawLine(60f, 20f, 60f, 76f, p)
      c.drawLine(20f, 36f, 76f, 36f, p)
      c.drawLine(20f, 60f, 76f, 60f, p)
    }
    return listOf(
      AppInfo("Calendar", "com.vesper.calendar", "MainActivity", calendarIcon, dailyLaunchCount = 4),
      AppInfo("Camera", "com.vesper.camera", "MainActivity", cameraIcon, dailyLaunchCount = 2),
      AppInfo("Mail", "com.vesper.mail", "MainActivity", mailIcon, dailyLaunchCount = 7),
      AppInfo("Music", "com.vesper.music", "MainActivity", musicIcon, dailyLaunchCount = 5),
      AppInfo("Notes", "com.vesper.notes", "MainActivity", notesIcon, dailyLaunchCount = 6),
      AppInfo("Slack", "com.vesper.slack", "MainActivity", slackIcon, dailyLaunchCount = 9),
      AppInfo("Terminal", "com.vesper.terminal", "MainActivity", terminalIcon, dailyLaunchCount = 3),
    )
  }

  private fun baseState(): LauncherUiState {
    val apps = sampleApps()
    val workFolder = AppFolder("work", "Work", listOf(apps[2].componentKey, apps[5].componentKey))
    val baseSettings =
      LauncherSettings(
        themeMode = ThemeMode.DARK,
        accent = AccentOption.EMERALD,
        iconShape = AdaptiveIconShape.ROUNDED_SQUARE,
        coastalBackdropEnabled = true,
        alphabetIndexEnabled = true,
        showDailyLaunchCounts = true,
        calendarPreviewEnabled = true,
        reducedMotionEnabled = true,
        hasCompletedOnboarding = true,
      )
    val now = System.currentTimeMillis()
    return LauncherUiState(
      isLoading = false,
      allApps = apps,
      allVisibleApps = apps,
      favoriteApps = listOf(apps[0], apps[3], apps[4], apps[1]),
      homeFolders = listOf(ResolvedFolder(workFolder, listOf(apps[2], apps[5]))),
      filteredDrawerApps = apps,
      drawerSections = apps.groupBy { it.initialLetter }.map { AlphabetSection(it.key, it.value) },
      settings = baseSettings,
      weather =
        WeatherInfo(
          temperatureCelsius = 27,
          apparentTemperatureCelsius = 28,
          conditionLabel = "Sunset breeze",
          locationName = "Miami",
          effectType = WeatherEffectType.CLEAR_BREEZE,
          isDay = false,
          isLive = true,
        ),
      nextCalendarEvent =
        CalendarEventPreview(
          id = 1L,
          title = "Design review",
          startTimeMillis = now + 18 * 60_000L,
          endTimeMillis = now + 48 * 60_000L,
        ),
    )
  }

  private fun resolveDocsScreenshotsDir(): File {
    val cwd = File(System.getProperty("user.dir") ?: ".")
    val root = if (cwd.name == "app") cwd.parentFile else cwd
    return File(root, "docs/screenshots").apply { mkdirs() }
  }

  private fun capturePng(filename: String, content: @Composable () -> Unit) {
    var hostView: android.view.View? = null
    composeTestRule.setContent {
      hostView = androidx.compose.ui.platform.LocalView.current
      Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        content()
      }
    }
    composeTestRule.waitForIdle()
    val view = requireNotNull(hostView) { "Host view was not attached" }
    val w = view.width.coerceAtLeast(1080)
    val h = view.height.coerceAtLeast(2400)
    val androidBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(androidBitmap)
    view.draw(canvas)
    val outFile = File(resolveDocsScreenshotsDir(), filename)
    FileOutputStream(outFile).use { stream ->
      androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
  }

  @Test
  fun generateHomeDarkScreenshot() {
    val uiState = baseState()
    capturePng("home_dark.png") {
      NeonSunsetTheme(
        accentOption = AccentOption.EMERALD,
        themeMode = ThemeMode.DARK,
        iconShape = AdaptiveIconShape.ROUNDED_SQUARE,
      ) {
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
  }

  @Test
  fun generateHomeLightScreenshot() {
    val uiState = baseState()
    capturePng("home_light.png") {
      NeonSunsetTheme(
        accentOption = AccentOption.MONO,
        themeMode = ThemeMode.LIGHT,
        iconShape = AdaptiveIconShape.SQUIRCLE,
      ) {
        HomeScreen(
          uiState =
            uiState.copy(
              settings =
                uiState.settings.copy(
                  themeMode = ThemeMode.LIGHT,
                  accent = AccentOption.MONO,
                  iconShape = AdaptiveIconShape.SQUIRCLE,
                  focusModeManualEnabled = true,
                ),
              isFocusModeActive = true,
            ),
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
  }

  @Test
  fun generateDrawerAlphabetScreenshot() {
    val uiState = baseState()
    capturePng("drawer_alphabet.png") {
      NeonSunsetTheme(
        accentOption = AccentOption.EMERALD,
        themeMode = ThemeMode.DARK,
        iconShape = AdaptiveIconShape.ROUNDED_SQUARE,
      ) {
        AppDrawer(
          isOpen = true,
          focusSearchOnOpen = false,
          initialLetter = null,
          uiState =
            uiState.copy(
              searchQuery = "",
              matchedShortcuts =
                listOf(
                  AppShortcutItem("new_note", "com.vesper.notes", "New markdown note", "Notes")
                ),
            ),
          onClose = {},
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

  @Test
  fun generateDrawerCommandKScreenshot() {
    val uiState = baseState()
    capturePng("drawer_command_k.png") {
      NeonSunsetTheme(
        accentOption = AccentOption.EMERALD,
        themeMode = ThemeMode.DARK,
        iconShape = AdaptiveIconShape.ROUNDED_SQUARE,
      ) {
        AppDrawer(
          isOpen = true,
          focusSearchOnOpen = false,
          initialLetter = null,
          uiState =
            uiState.copy(
              searchQuery = "24 * 7",
              quickMathResult = "168",
              filteredDrawerApps = emptyList(),
              drawerSections = emptyList(),
            ),
          onClose = {},
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

  @Test
  fun generateSettingsScreenshot() {
    val uiState = baseState()
    capturePng("settings_geist.png") {
      NeonSunsetTheme(
        accentOption = AccentOption.EMERALD,
        themeMode = ThemeMode.DARK,
        iconShape = AdaptiveIconShape.ROUNDED_SQUARE,
      ) {
        SettingsScreen(
          uiState = uiState.copy(isDefaultHomeLauncher = true),
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
        )
      }
    }
  }
}
