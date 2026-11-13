package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.ThemeMode

val LocalAccentOption = staticCompositionLocalOf { AccentOption.EMERALD }
val LocalAdaptiveIconShape = staticCompositionLocalOf { AdaptiveIconShape.ROUNDED_SQUARE }
val LocalLauncherIsDark = staticCompositionLocalOf { true }

// Vercel / Geist sharper architectural corner geometry
private val LauncherShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
  )

fun colorSchemeForAccent(
  accentOption: AccentOption,
  isDark: Boolean = true,
): ColorScheme {
  val primary = accentOption.resolvedPrimary(isDark)
  return if (isDark) {
    darkColorScheme(
      primary = primary,
      onPrimary = DarkBackground,
      primaryContainer = primary.copy(alpha = 0.14f),
      onPrimaryContainer = DarkTextPrimary,
      secondary = primary,
      onSecondary = DarkBackground,
      background = DarkBackground,
      onBackground = DarkTextPrimary,
      surface = DarkSurface,
      onSurface = DarkTextPrimary,
      surfaceVariant = DarkSurfaceElevated,
      onSurfaceVariant = DarkTextSecondary,
      outline = DarkDivider,
      outlineVariant = DarkBorderStrong,
      error = DangerRed,
      onError = Color.White,
    )
  } else {
    lightColorScheme(
      primary = primary,
      onPrimary = Color.White,
      primaryContainer = primary.copy(alpha = 0.12f),
      onPrimaryContainer = LightTextPrimary,
      secondary = primary,
      onSecondary = Color.White,
      background = LightBackground,
      onBackground = LightTextPrimary,
      surface = LightSurface,
      onSurface = LightTextPrimary,
      surfaceVariant = LightSurfaceElevated,
      onSurfaceVariant = LightTextSecondary,
      outline = LightDivider,
      outlineVariant = LightBorderStrong,
      error = DangerRed,
      onError = Color.White,
    )
  }
}

@Composable
fun NeonSunsetTheme(
  accentOption: AccentOption = AccentOption.EMERALD,
  themeMode: ThemeMode = ThemeMode.DARK,
  iconShape: AdaptiveIconShape = AdaptiveIconShape.ROUNDED_SQUARE,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> systemDark
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
    }
  val colorScheme = colorSchemeForAccent(accentOption = accentOption, isDark = isDark)
  CompositionLocalProvider(
    LocalAccentOption provides accentOption,
    LocalAdaptiveIconShape provides iconShape,
    LocalLauncherIsDark provides isDark,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = LauncherShapes,
      content = content,
    )
  }
}
