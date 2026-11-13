package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppRepository
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AppInfo
import com.example.model.LauncherSettings
import com.example.model.ScrollSpeed
import com.example.model.ThemeMode
import com.example.ui.components.toComposeShape
import com.example.ui.theme.colorSchemeForAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherModelsTest {

  @Test
  fun `accent option resolves fallback to EMERALD for unknown name`() {
    assertEquals(AccentOption.EMERALD, AccentOption.fromName(null))
    assertEquals(AccentOption.EMERALD, AccentOption.fromName("UNKNOWN"))
    assertEquals(AccentOption.MONO, AccentOption.fromName("MONO"))
    assertEquals(AccentOption.PURPLE, AccentOption.fromName("PURPLE"))
    assertEquals(AccentOption.BLUE, AccentOption.fromName("BLUE"))
    assertEquals(AccentOption.ORANGE, AccentOption.fromName("ORANGE"))
  }

  @Test
  fun `adaptive icon shape resolves fallback to ROUNDED_SQUARE and maps all shapes`() {
    assertEquals(AdaptiveIconShape.ROUNDED_SQUARE, AdaptiveIconShape.fromName(null))
    assertEquals(AdaptiveIconShape.ROUNDED_SQUARE, AdaptiveIconShape.fromName("INVALID"))
    AdaptiveIconShape.entries.forEach { shape ->
      assertEquals(shape, AdaptiveIconShape.fromName(shape.name))
      assertNotNull(shape.toComposeShape())
    }
  }

  @Test
  fun `default launcher settings match minimal vertical launcher defaults`() {
    val defaults = LauncherSettings()
    assertEquals(ThemeMode.DARK, defaults.themeMode)
    assertEquals(AccentOption.EMERALD, defaults.accent)
    assertEquals(ScrollSpeed.NORMAL, defaults.scrollSpeed)
    assertTrue(defaults.alphabetIndexEnabled)
    assertFalse(defaults.hideAppLabels)
    assertTrue(defaults.hapticFeedbackEnabled)
  }

  @Test
  fun `app initial letter groups alphabetically and falls back to hash`() {
    val calendar = AppInfo("Calendar", "com.cal", "Main", null)
    val numeric = AppInfo("1Password", "com.onepass", "Main", null)
    assertEquals('C', calendar.initialLetter)
    assertEquals('#', numeric.initialLetter)
  }

  @Test
  fun `theme color schemes reflect selected accent primary color in dark and light mode`() {
    AccentOption.entries.forEach { accent ->
      val darkScheme = colorSchemeForAccent(accent, isDark = true)
      val lightScheme = colorSchemeForAccent(accent, isDark = false)
      assertEquals(accent.resolvedPrimary(isDark = true), darkScheme.primary)
      assertEquals(accent.resolvedPrimary(isDark = false), lightScheme.primary)
    }
  }

  @Test
  fun `renderConsistentAppIcon converts AdaptiveIconDrawable foreground into hollow white outline`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fgBitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
    Canvas(fgBitmap).drawRect(30f, 30f, 78f, 78f, Paint().apply { color = Color.CYAN })

    val bg = ColorDrawable(Color.MAGENTA)
    val fg = BitmapDrawable(context.resources, fgBitmap)
    val adaptive = AdaptiveIconDrawable(bg, fg)

    val bitmap = AppRepository.renderConsistentAppIcon(adaptive)
    assertEquals(AppRepository.ICON_BITMAP_SIZE_PX, bitmap.width)
    assertEquals(AppRepository.ICON_BITMAP_SIZE_PX, bitmap.height)

    // Outer corner stays transparent so the warm-tinted glass frame shows behind it
    assertEquals(0, Color.alpha(bitmap.getPixel(2, 2)))
    // Solid square interior is hollowed out
    assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)))

    // Perimeter scan along middle row finds opaque white outline pixels
    val midY = bitmap.height / 2
    val outlinePixels = (0 until bitmap.width).map { x -> bitmap.getPixel(x, midY) }.filter { Color.alpha(it) > 180 }
    assertTrue(outlinePixels.isNotEmpty())
    assertTrue(outlinePixels.all { Color.red(it) == 255 && Color.green(it) == 255 && Color.blue(it) == 255 })
  }

  @Test
  fun `renderConsistentAppIcon converts legacy drawable into hollow white outline`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rawLegacy = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    Canvas(rawLegacy).drawRect(20f, 20f, 44f, 44f, Paint().apply { color = Color.YELLOW })
    val legacyDrawable = BitmapDrawable(context.resources, rawLegacy)

    val bitmap = AppRepository.renderConsistentAppIcon(legacyDrawable)
    assertEquals(AppRepository.ICON_BITMAP_SIZE_PX, bitmap.width)
    assertEquals(AppRepository.ICON_BITMAP_SIZE_PX, bitmap.height)

    assertEquals(0, Color.alpha(bitmap.getPixel(2, 2)))
    assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)))

    val midY = bitmap.height / 2
    val outlinePixels = (0 until bitmap.width).map { x -> bitmap.getPixel(x, midY) }.filter { Color.alpha(it) > 180 }
    assertTrue(outlinePixels.isNotEmpty())
    assertTrue(outlinePixels.all { Color.red(it) == 255 && Color.green(it) == 255 && Color.blue(it) == 255 })
  }

  @Test
  fun `resolveMiamiCoastalPalette shifts across dawn day sunset and night with wrapping`() {
    val dawn = com.example.ui.home.resolveMiamiCoastalPalette(6.75f)
    val day = com.example.ui.home.resolveMiamiCoastalPalette(13.0f)
    val sunset = com.example.ui.home.resolveMiamiCoastalPalette(18.25f)
    val night = com.example.ui.home.resolveMiamiCoastalPalette(23.0f)

    assertEquals(com.example.ui.theme.CoastalDawnWash, dawn.sunsetWash)
    assertEquals(com.example.ui.theme.CoastalDayWash, day.sunsetWash)
    assertEquals(com.example.ui.theme.CoastalSunsetWash, sunset.sunsetWash)
    assertEquals(com.example.ui.theme.CoastalNightWash, night.sunsetWash)

    val wrappedMidnight = com.example.ui.home.resolveMiamiCoastalPalette(24.0f)
    val zeroMidnight = com.example.ui.home.resolveMiamiCoastalPalette(0.0f)
    val negativeWrap = com.example.ui.home.resolveMiamiCoastalPalette(-1.0f)
    assertEquals(zeroMidnight, wrappedMidnight)
    assertEquals(night, negativeWrap)

    val goldenHourApproach = com.example.ui.home.resolveMiamiCoastalPalette(17.0f)
    assertNotEquals(day.sunsetWash, goldenHourApproach.sunsetWash)
    assertNotEquals(sunset.sunsetWash, goldenHourApproach.sunsetWash)
  }

  @Test
  fun `WeatherRepository maps WMO weather codes to rain clouds heat haze and thunderstorm`() {
    assertEquals(
      com.example.model.WeatherEffectType.RAIN,
      com.example.data.WeatherRepository.mapWmoWeather(61, 25, 26, 80).first,
    )
    assertEquals(
      com.example.model.WeatherEffectType.CLOUDY,
      com.example.data.WeatherRepository.mapWmoWeather(3, 24, 24, 90).first,
    )
    assertEquals(
      com.example.model.WeatherEffectType.HEAT_HAZE,
      com.example.data.WeatherRepository.mapWmoWeather(0, 32, 35, 5).first,
    )
    assertEquals(
      com.example.model.WeatherEffectType.THUNDERSTORM,
      com.example.data.WeatherRepository.mapWmoWeather(95, 27, 29, 95).first,
    )
    assertEquals(
      com.example.model.WeatherEffectType.CLEAR_BREEZE,
      com.example.data.WeatherRepository.mapWmoWeather(1, 24, 24, 15).first,
    )
  }

  @Test
  fun `WeatherRepository parses valid Open-Meteo JSON and rejects invalid payloads`() {
    val json =
      """
      {
        "current": {
          "temperature_2m": 30.6,
          "apparent_temperature": 33.2,
          "is_day": 1,
          "precipitation": 0.0,
          "weather_code": 0,
          "cloud_cover": 10,
          "wind_speed_10m": 18.4
        }
      }
      """.trimIndent()
    val parsed = com.example.data.WeatherRepository.parseOpenMeteoJson(json)
    assertNotNull(parsed)
    assertEquals(31, parsed?.temperatureCelsius)
    assertEquals(33, parsed?.apparentTemperatureCelsius)
    assertEquals(18, parsed?.windSpeedKmh)
    assertEquals(com.example.model.WeatherEffectType.HEAT_HAZE, parsed?.effectType)
    assertTrue(parsed?.isLive == true)

    assertEquals(null, com.example.data.WeatherRepository.parseOpenMeteoJson("{}"))
    assertEquals(null, com.example.data.WeatherRepository.parseOpenMeteoJson("not-json"))
  }

  @Test
  fun `MathEvaluator evaluates arithmetic expressions safely and ignores plain app queries`() {
    assertEquals("168", com.example.util.MathEvaluator.evaluateToString("24 * 7"))
    assertEquals("55", com.example.util.MathEvaluator.evaluateToString("(120 + 45) / 3"))
    assertEquals("22.7", com.example.util.MathEvaluator.evaluateToString("18.5 + 4.2"))
    assertEquals(null, com.example.util.MathEvaluator.evaluateToString("10 / 0"))
    assertEquals(null, com.example.util.MathEvaluator.evaluateToString("spotify"))
    assertEquals(null, com.example.util.MathEvaluator.evaluateToString("-5"))
  }

  @Test
  fun `LauncherSettings power and focus defaults initialize cleanly`() {
    val defaults = LauncherSettings()
    assertTrue(defaults.hiddenComponentKeys.isEmpty())
    assertTrue(defaults.customAppLabels.isEmpty())
    assertTrue(defaults.mindfulPauseEnabled)
    assertTrue(defaults.mindfulPausedComponentKeys.isEmpty())
    assertFalse(defaults.showDailyLaunchCounts)
    assertFalse(defaults.calendarPreviewEnabled)
    assertFalse(defaults.reducedMotionEnabled)
    assertFalse(defaults.hasCompletedOnboarding)
    assertTrue(defaults.coastalBackdropEnabled)
    assertEquals("Miami", defaults.weatherLocation.name)
  }

  @Test
  fun `WeatherRepository parses Open-Meteo geocoding results accurately`() {
    val json =
      """
      {
        "results": [
          {
            "name": "Miami Beach",
            "latitude": 25.79065,
            "longitude": -80.13005,
            "admin1": "Florida",
            "country": "United States"
          }
        ]
      }
      """.trimIndent()
    val parsed = com.example.data.WeatherRepository.parseGeocodingJson(json)
    assertEquals(1, parsed.size)
    assertEquals("Miami Beach", parsed[0].name)
    assertEquals("Florida, United States", parsed[0].region)
    assertEquals(25.79065, parsed[0].latitude, 0.0001)
  }

  @Test
  fun `FocusAndSearchUtils evaluates daytime and overnight schedules and quick actions`() {
    // Daytime 09:00 - 17:00
    assertTrue(com.example.util.FocusAndSearchUtils.isMinuteInsideSchedule(10 * 60, 9 * 60, 17 * 60))
    assertFalse(com.example.util.FocusAndSearchUtils.isMinuteInsideSchedule(18 * 60, 9 * 60, 17 * 60))
    // Overnight 22:00 - 07:00
    assertTrue(com.example.util.FocusAndSearchUtils.isMinuteInsideSchedule(23 * 60, 22 * 60, 7 * 60))
    assertTrue(com.example.util.FocusAndSearchUtils.isMinuteInsideSchedule(6 * 60, 22 * 60, 7 * 60))
    assertFalse(com.example.util.FocusAndSearchUtils.isMinuteInsideSchedule(12 * 60, 22 * 60, 7 * 60))

    val dial = com.example.util.FocusAndSearchUtils.detectDrawerQuickAction("305 555 1234")
    assertTrue(dial is com.example.model.DrawerQuickAction.DialPhone)

    val url = com.example.util.FocusAndSearchUtils.detectDrawerQuickAction("miamibeachfl.gov")
    assertEquals(
      com.example.model.DrawerQuickAction.OpenUrl("https://miamibeachfl.gov"),
      url,
    )
  }

  @Test
  fun `SettingsRepository roundtrips AppFolder list via JSON`() {
    val folders =
      listOf(
        com.example.model.AppFolder(
          id = "f1",
          name = "Finance",
          componentKeys = listOf("com.bank/Main", "com.wallet/Home"),
        )
      )
    val encoded = com.example.data.SettingsRepository.encodeFoldersJson(folders)
    val decoded = com.example.data.SettingsRepository.decodeFoldersJson(encoded)
    assertEquals(folders, decoded)
  }

  @Test
  fun `SettingsRepository roundtrips cached WeatherInfo via JSON and marks cached as non-live`() {
    val live =
      com.example.model.WeatherInfo(
        temperatureCelsius = 29,
        apparentTemperatureCelsius = 32,
        conditionLabel = "Partly cloudy",
        locationName = "Miami",
        effectType = com.example.model.WeatherEffectType.CLOUDY,
        cloudCoverPercent = 45,
        windSpeedKmh = 19,
        isDay = true,
        isLive = true,
      )
    val json = com.example.data.SettingsRepository.encodeWeatherJson(live)
    val decoded = com.example.data.SettingsRepository.decodeWeatherJson(json)
    assertNotNull(decoded)
    assertEquals(29, decoded?.temperatureCelsius)
    assertEquals(32, decoded?.apparentTemperatureCelsius)
    assertEquals("Partly cloudy", decoded?.conditionLabel)
    assertEquals(com.example.model.WeatherEffectType.CLOUDY, decoded?.effectType)
    assertFalse(decoded?.isLive ?: true)
    assertEquals(null, com.example.data.SettingsRepository.decodeWeatherJson("bad-json"))
  }

  @Test
  fun `SettingsRepository roundtrips full LauncherSettings backup JSON`() {
    val original =
      LauncherSettings(
        themeMode = ThemeMode.LIGHT,
        accent = AccentOption.MONO,
        iconShape = AdaptiveIconShape.SQUIRCLE,
        coastalBackdropEnabled = false,
        alphabetIndexEnabled = false,
        favoriteComponentKeys = listOf("com.cal/Main", "com.notes/Main"),
        folders = listOf(com.example.model.AppFolder("w1", "Work", listOf("com.slack/Home"))),
        hiddenComponentKeys = setOf("com.bloat/Main"),
        customAppLabels = mapOf("com.cal/Main" to "Agenda"),
        focusScheduleEnabled = true,
        focusScheduleStartMinutes = 8 * 60 + 30,
        focusScheduleEndMinutes = 16 * 60,
        focusEssentialComponentKeys = setOf("com.cal/Main"),
        focusTimerDurationMinutes = 45,
        weatherLocation = com.example.model.WeatherLocation("Tokyo", "Japan", 35.6762, 139.6503),
      )

    val json = com.example.data.SettingsRepository.encodeSettingsBackupJson(original)
    val restored = com.example.data.SettingsRepository.decodeSettingsBackupJson(json)
    assertNotNull(restored)
    assertEquals(ThemeMode.LIGHT, restored?.themeMode)
    assertEquals(AccentOption.MONO, restored?.accent)
    assertEquals(AdaptiveIconShape.SQUIRCLE, restored?.iconShape)
    assertFalse(restored?.coastalBackdropEnabled ?: true)
    assertFalse(restored?.alphabetIndexEnabled ?: true)
    assertEquals(original.favoriteComponentKeys, restored?.favoriteComponentKeys)
    assertEquals(original.folders, restored?.folders)
    assertEquals(original.hiddenComponentKeys, restored?.hiddenComponentKeys)
    assertEquals(original.customAppLabels, restored?.customAppLabels)
    assertTrue(restored?.focusScheduleEnabled == true)
    assertEquals(8 * 60 + 30, restored?.focusScheduleStartMinutes)
    assertEquals(45, restored?.focusTimerDurationMinutes)
    assertEquals("Tokyo", restored?.weatherLocation?.name)

    assertEquals(null, com.example.data.SettingsRepository.decodeSettingsBackupJson("{\"no_version\":true}"))
  }

  @Test
  fun `AppRepository sha256Short produces deterministic 16-char hex digest`() {
    val h1 = AppRepository.sha256Short("com.example/MainActivity")
    val h2 = AppRepository.sha256Short("com.example/MainActivity")
    val h3 = AppRepository.sha256Short("com.other/MainActivity")
    assertEquals(16, h1.length)
    assertEquals(h1, h2)
    assertNotEquals(h1, h3)
    assertTrue(h1.all { it in '0'..'9' || it in 'a'..'f' })
  }

  @Test
  fun `GyroParallaxMath integrates tilt velocity, clamps bounds, and decays smoothly to rest`() {
    val step1 =
      com.example.ui.home.GyroParallaxMath.stepGyroscopeTilt(
        currentX = 0f,
        currentY = 0f,
        angularVelX = 1.2f,
        angularVelY = -1.5f,
        dtSeconds = 0.05f,
      )
    assertTrue(step1.x < 0f)
    assertTrue(step1.y > 0f)

    // Extreme angular velocity must still clamp inside [-1f, 1f]
    val saturated =
      com.example.ui.home.GyroParallaxMath.stepGyroscopeTilt(
        currentX = 0.9f,
        currentY = -0.9f,
        angularVelX = -50f,
        angularVelY = 50f,
        dtSeconds = 0.08f,
      )
    assertTrue(saturated.x in -1f..1f)
    assertTrue(saturated.y in -1f..1f)

    // Zero angular velocity over successive steps decays toward center
    var decayX = saturated.x
    var decayY = saturated.y
    repeat(60) {
      val s =
        com.example.ui.home.GyroParallaxMath.stepGyroscopeTilt(
          currentX = decayX,
          currentY = decayY,
          angularVelX = 0f,
          angularVelY = 0f,
          dtSeconds = 0.05f,
        )
      decayX = s.x
      decayY = s.y
    }
    assertTrue(kotlin.math.abs(decayX) < 0.02f)
    assertTrue(kotlin.math.abs(decayY) < 0.02f)

    val smoothed = com.example.ui.home.GyroParallaxMath.smoothToward(0f, 1f, 0.016f)
    assertTrue(smoothed > 0f && smoothed < 1f)
  }

  @Test
  fun `GyroParallaxMath gravity fallback adapts baseline and normalizes tilt`() {
    val step =
      com.example.ui.home.GyroParallaxMath.stepGravityTilt(
        rawX = 3.5f,
        rawY = -2.0f,
        baseX = 0f,
        baseY = 0f,
        dtSeconds = 0.02f,
      )
    assertTrue(step.tiltX > 0.5f && step.tiltX <= 1f)
    assertTrue(step.tiltY > 0.2f && step.tiltY <= 1f)
    assertTrue(step.baseX > 0f)
  }

  @Test
  fun `LauncherHapticsResolver resolves nuanced constants on API 30+ and clean fallbacks on API 26`() {
    assertEquals(
      android.view.HapticFeedbackConstants.GESTURE_START,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.DRAWER_OPEN_SWIPE,
        sdkInt = android.os.Build.VERSION_CODES.R,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.CONTEXT_CLICK,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.DRAWER_OPEN_SWIPE,
        sdkInt = android.os.Build.VERSION_CODES.O,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.GESTURE_END,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.DRAWER_CLOSE_SWIPE,
        sdkInt = android.os.Build.VERSION_CODES.R,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.CLOCK_TICK,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.DRAWER_CLOSE_SWIPE,
        sdkInt = android.os.Build.VERSION_CODES.O,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.CONFIRM,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.CONFIRM_ACTION,
        sdkInt = android.os.Build.VERSION_CODES.R,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.REJECT,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.ERROR_FEEDBACK,
        sdkInt = android.os.Build.VERSION_CODES.R,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.LONG_PRESS,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.LONG_PRESS,
        sdkInt = android.os.Build.VERSION_CODES.O,
      ),
    )
    assertEquals(
      android.view.HapticFeedbackConstants.CLOCK_TICK,
      com.example.util.LauncherHapticsResolver.resolveAndroidHapticConstant(
        com.example.util.LauncherHapticEvent.SCRUBBER_TICK,
        sdkInt = android.os.Build.VERSION_CODES.R,
      ),
    )
  }

  @Test
  fun `ClockStyle resolves fallback to NEON_HORIZON and work profile keys preserve backward compatibility`() {
    assertEquals(com.example.model.ClockStyle.NEON_HORIZON, com.example.model.ClockStyle.fromName(null))
    assertEquals(com.example.model.ClockStyle.NEON_HORIZON, com.example.model.ClockStyle.fromName("UNKNOWN"))
    com.example.model.ClockStyle.entries.forEach { style ->
      assertEquals(style, com.example.model.ClockStyle.fromName(style.name))
    }

    val personalApp = AppInfo("Mail", "com.mail", "Main", null, isWorkProfile = false, userSerialNumber = 0L)
    val workApp = AppInfo("Mail", "com.mail", "Main", null, isWorkProfile = true, userSerialNumber = 12L)
    assertEquals("com.mail/Main", personalApp.componentKey)
    assertEquals("com.mail/Main#12", workApp.componentKey)
  }

  @Test
  fun `MathEvaluator supports percentage expressions and unit conversions`() {
    assertEquals("36", com.example.util.MathEvaluator.evaluateToString("20% of 180"))
    assertEquals("30", com.example.util.MathEvaluator.evaluateToString("15% * 200"))
    assertEquals("0 °C", com.example.util.MathEvaluator.evaluateToString("32 f to c"))
    assertEquals("212 °F", com.example.util.MathEvaluator.evaluateToString("100 c in f"))
    assertEquals("2,048 MB", com.example.util.MathEvaluator.evaluateToString("2 gb to mb"))
    assertEquals("6.213712 mi", com.example.util.MathEvaluator.evaluateToString("10 km to mi"))
    assertEquals(null, com.example.util.MathEvaluator.evaluateToString("10 km to kg"))
  }

  @Test
  fun `SettingsRepository backup roundtrips clock, battery, dim, and intention fields`() {
    val original =
      LauncherSettings(
        clockStyle = com.example.model.ClockStyle.MONO_TERMINAL,
        showClockSeconds = true,
        showBatteryIndicator = true,
        wallpaperDimPercent = 65,
        dailyIntention = "Finish v1.2 polish",
      )
    val json = com.example.data.SettingsRepository.encodeSettingsBackupJson(original)
    val restored = com.example.data.SettingsRepository.decodeSettingsBackupJson(json)
    assertNotNull(restored)
    assertEquals(com.example.model.ClockStyle.MONO_TERMINAL, restored?.clockStyle)
    assertTrue(restored?.showClockSeconds == true)
    assertTrue(restored?.showBatteryIndicator == true)
    assertEquals(65, restored?.wallpaperDimPercent)
    assertEquals("Finish v1.2 polish", restored?.dailyIntention)
  }

  @Test
  fun `SmartFolderCategorizer groups apps into Social Media Productivity by usage and preserves custom folders`() {
    val slack = AppInfo("Slack", "com.Slack", "Main", null, dailyLaunchCount = 4)
    val whatsapp = AppInfo("WhatsApp", "com.whatsapp", "Main", null, dailyLaunchCount = 9)
    val spotify =
      AppInfo(
        label = "Spotify",
        packageName = "com.spotify.music",
        activityName = "Main",
        icon = null,
        dailyLaunchCount = 15,
        appCategory = android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
      )
    val youtube = AppInfo("YouTube", "com.google.android.youtube", "Main", null, dailyLaunchCount = 6)
    val calendar = AppInfo("Calendar", "com.google.android.calendar", "Main", null, dailyLaunchCount = 5)
    val customFolder = com.example.model.AppFolder("custom_1", "Deep Work", listOf(calendar.componentKey))

    assertEquals(
      com.example.util.SmartAppCategory.SOCIAL,
      com.example.util.SmartFolderCategorizer.classifyApp(slack),
    )
    assertEquals(
      com.example.util.SmartAppCategory.MEDIA,
      com.example.util.SmartFolderCategorizer.classifyApp(spotify),
    )
    assertEquals(
      com.example.util.SmartAppCategory.PRODUCTIVITY,
      com.example.util.SmartFolderCategorizer.classifyApp(calendar),
    )

    val folders =
      com.example.util.SmartFolderCategorizer.generateSmartFolders(
        apps = listOf(slack, whatsapp, spotify, youtube, calendar),
        existingFolders = listOf(customFolder),
      )

    // Media has 15 + 6 = 21 launches, Social has 9 + 4 = 13 launches, Productivity has 5 launches, followed by custom folder
    assertEquals(listOf("Media", "Social", "Productivity", "Deep Work"), folders.map { it.name })
    // Inside Media, Spotify (15) comes before YouTube (6)
    assertEquals(listOf(spotify.componentKey, youtube.componentKey), folders[0].componentKeys)
    // Inside Social, WhatsApp (9) comes before Slack (4)
    assertEquals(listOf(whatsapp.componentKey, slack.componentKey), folders[1].componentKeys)
  }

  @Test
  fun `TimeOfDayBucket maps morning afternoon and evening hours and backup roundtrips new UX settings`() {
    assertEquals(
      com.example.model.TimeOfDayBucket.MORNING,
      com.example.model.TimeOfDayBucket.fromHour(7),
    )
    assertEquals(
      com.example.model.TimeOfDayBucket.AFTERNOON,
      com.example.model.TimeOfDayBucket.fromHour(14),
    )
    assertEquals(
      com.example.model.TimeOfDayBucket.EVENING,
      com.example.model.TimeOfDayBucket.fromHour(21),
    )
    assertEquals(
      com.example.model.TimeOfDayBucket.EVENING,
      com.example.model.TimeOfDayBucket.fromHour(2),
    )

    val original =
      LauncherSettings(
        autoLaunchSingleMatch = true,
        bottomAlignFavorites = true,
        leftHandedScrubber = true,
        lockHiddenApps = true,
        quickScratchpadText = "Call Alex at 4pm",
      )
    val json = com.example.data.SettingsRepository.encodeSettingsBackupJson(original)
    val restored = com.example.data.SettingsRepository.decodeSettingsBackupJson(json)
    assertNotNull(restored)
    assertTrue(restored?.autoLaunchSingleMatch == true)
    assertTrue(restored?.bottomAlignFavorites == true)
    assertTrue(restored?.leftHandedScrubber == true)
    assertTrue(restored?.lockHiddenApps == true)
    assertEquals("Call Alex at 4pm", restored?.quickScratchpadText)
  }
}

