package com.example.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoastalDawnHorizon
import com.example.ui.theme.CoastalDawnSky
import com.example.ui.theme.CoastalDawnWash
import com.example.ui.theme.CoastalDayHorizon
import com.example.ui.theme.CoastalDaySky
import com.example.ui.theme.CoastalDayWash
import com.example.ui.theme.CoastalNightHorizon
import com.example.ui.theme.CoastalNightSky
import com.example.ui.theme.CoastalNightWash
import com.example.ui.theme.CoastalSunsetHorizon
import com.example.ui.theme.CoastalSunsetSky
import com.example.ui.theme.CoastalSunsetWash
import com.example.ui.theme.LocalLauncherIsDark

@Immutable
data class CoastalGradientPalette(
  val upperSky: Color,
  val sunsetWash: Color,
  val oceanHorizon: Color,
)

private data class CoastalTimeAnchor(
  val hour: Float,
  val palette: CoastalGradientPalette,
)

private val NightPalette =
  CoastalGradientPalette(
    upperSky = CoastalNightSky,
    sunsetWash = CoastalNightWash,
    oceanHorizon = CoastalNightHorizon,
  )

private val DawnPalette =
  CoastalGradientPalette(
    upperSky = CoastalDawnSky,
    sunsetWash = CoastalDawnWash,
    oceanHorizon = CoastalDawnHorizon,
  )

private val DayPalette =
  CoastalGradientPalette(
    upperSky = CoastalDaySky,
    sunsetWash = CoastalDayWash,
    oceanHorizon = CoastalDayHorizon,
  )

private val SunsetPalette =
  CoastalGradientPalette(
    upperSky = CoastalSunsetSky,
    sunsetWash = CoastalSunsetWash,
    oceanHorizon = CoastalSunsetHorizon,
  )

private val CoastalScheduleAnchors =
  listOf(
    CoastalTimeAnchor(0.0f, NightPalette),
    CoastalTimeAnchor(4.5f, NightPalette),
    CoastalTimeAnchor(6.75f, DawnPalette),
    CoastalTimeAnchor(10.5f, DayPalette),
    CoastalTimeAnchor(15.5f, DayPalette),
    CoastalTimeAnchor(18.25f, SunsetPalette),
    CoastalTimeAnchor(20.75f, NightPalette),
    CoastalTimeAnchor(24.0f, NightPalette),
  )

fun resolveMiamiCoastalPalette(hourOfDay: Float): CoastalGradientPalette {
  val wrappedHour = ((hourOfDay % 24f) + 24f) % 24f
  for (i in 0 until CoastalScheduleAnchors.lastIndex) {
    val start = CoastalScheduleAnchors[i]
    val end = CoastalScheduleAnchors[i + 1]
    if (wrappedHour >= start.hour && wrappedHour <= end.hour) {
      val span = (end.hour - start.hour).coerceAtLeast(0.001f)
      val linear = ((wrappedHour - start.hour) / span).coerceIn(0f, 1f)
      val smooth = linear * linear * (3f - 2f * linear)
      return CoastalGradientPalette(
        upperSky = lerp(start.palette.upperSky, end.palette.upperSky, smooth),
        sunsetWash = lerp(start.palette.sunsetWash, end.palette.sunsetWash, smooth),
        oceanHorizon = lerp(start.palette.oceanHorizon, end.palette.oceanHorizon, smooth),
      )
    }
  }
  return NightPalette
}

@Composable
fun CoastalGradientOverlay(
  hourOfDay: Float,
  reducedMotion: Boolean = false,
  parallaxState: GyroscopeParallaxState? = null,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalLauncherIsDark.current
  val bgCanvasColor = MaterialTheme.colorScheme.background
  val targetPalette = remember(hourOfDay) { resolveMiamiCoastalPalette(hourOfDay) }

  val upperColor by
    animateColorAsState(
      targetValue = targetPalette.upperSky,
      animationSpec = tween(durationMillis = if (reducedMotion) 250 else 1200),
      label = "coastal_upper_sky",
    )
  val washColor by
    animateColorAsState(
      targetValue = targetPalette.sunsetWash,
      animationSpec = tween(durationMillis = if (reducedMotion) 250 else 1200),
      label = "coastal_sunset_wash",
    )
  val horizonColor by
    animateColorAsState(
      targetValue = targetPalette.oceanHorizon,
      animationSpec = tween(durationMillis = if (reducedMotion) 250 else 1200),
      label = "coastal_ocean_horizon",
    )

  val skyAlpha = if (isDark) 0.22f else 0.16f
  val washAlpha = if (isDark) 0.25f else 0.18f
  val horizonAlpha = if (isDark) 0.24f else 0.19f
  val bloomAlpha = if (isDark) 0.28f else 0.22f

  // Zero-allocation GPU transform animation: brushes are cached once per minute/theme change
  val driftPhase: Float
  val glowBreath: Float
  if (reducedMotion) {
    driftPhase = 0.5f
    glowBreath = 1.0f
  } else {
    val infiniteTransition = rememberInfiniteTransition(label = "coastal_sunset_drift")
    val animatedDrift by
      infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "coastal_drift_phase",
      )
    val animatedBreath by
      infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "coastal_glow_breath",
      )
    driftPhase = animatedDrift
    glowBreath = animatedBreath
  }

  Box(modifier = modifier.fillMaxSize().clipToBounds().testTag("coastal_gradient_overlay")) {
    // Far depth plane: sunset illustration pre-scaled 1.14x so GPU tilt shifts never expose edges
    Image(
      painter = painterResource(id = R.drawable.img_miami_sunset_hero_1794549247807),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alpha = if (isDark) 0.30f else 0.18f,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            val tiltX = if (reducedMotion) 0f else (parallaxState?.offsetX ?: 0f)
            val tiltY = if (reducedMotion) 0f else (parallaxState?.offsetY ?: 0f)
            scaleX = 1.14f
            scaleY = 1.14f
            translationX = tiltX * 16.dp.toPx()
            translationY = tiltY * 14.dp.toPx()
          }
          .testTag("sunset_parallax_illustration"),
    )

    // Smooth vertical vignette + diagonal atmospheric wash over the sunset illustration
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .drawWithCache {
            val scrimBrush =
              Brush.verticalGradient(
                0.0f to bgCanvasColor.copy(alpha = if (isDark) 0.56f else 0.64f),
                0.35f to bgCanvasColor.copy(alpha = if (isDark) 0.42f else 0.55f),
                0.78f to bgCanvasColor.copy(alpha = if (isDark) 0.80f else 0.84f),
                1.0f to bgCanvasColor.copy(alpha = if (isDark) 0.94f else 0.96f),
              )
            val skyBrush =
              Brush.linearGradient(
                0.0f to upperColor.copy(alpha = skyAlpha),
                0.48f to washColor.copy(alpha = washAlpha),
                1.0f to horizonColor.copy(alpha = horizonAlpha),
                start = Offset(size.width * 0.12f, 0f),
                end = Offset(size.width * 0.88f, size.height),
              )
            onDrawBehind {
              drawRect(brush = scrimBrush)
              drawRect(brush = skyBrush)
            }
          }
    )

    // Mid depth plane: upper horizon radial solar bloom shifted by drift + gyroscope tilt
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            val tiltX = if (reducedMotion) 0f else (parallaxState?.offsetX ?: 0f)
            val tiltY = if (reducedMotion) 0f else (parallaxState?.offsetY ?: 0f)
            translationX = (0.5f - driftPhase) * size.width * 0.12f + tiltX * 24.dp.toPx()
            translationY = (driftPhase - 0.5f) * size.height * 0.06f + tiltY * 20.dp.toPx()
            scaleX = glowBreath
            scaleY = glowBreath
          }
          .drawWithCache {
            val center = Offset(size.width * 0.72f, size.height * 0.19f)
            val radius = size.maxDimension * 0.68f
            val bloomBrush =
              Brush.radialGradient(
                colors =
                  listOf(
                    washColor.copy(alpha = bloomAlpha),
                    horizonColor.copy(alpha = bloomAlpha * 0.55f),
                    Color.Transparent,
                  ),
                center = center,
                radius = radius,
              )
            onDrawBehind {
              drawCircle(brush = bloomBrush, radius = radius, center = center)
            }
          }
    )

    // Near-mid shoreline reflection glow shifted slightly faster than the horizon bloom
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            val tiltX = if (reducedMotion) 0f else (parallaxState?.offsetX ?: 0f)
            val tiltY = if (reducedMotion) 0f else (parallaxState?.offsetY ?: 0f)
            translationX = (driftPhase - 0.5f) * size.width * 0.14f + tiltX * 30.dp.toPx()
            translationY = tiltY * 22.dp.toPx()
          }
          .drawWithCache {
            val center = Offset(size.width * 0.30f, size.height * 0.96f)
            val radius = size.maxDimension * 0.62f
            val bayBrush =
              Brush.radialGradient(
                colors =
                  listOf(
                    horizonColor.copy(alpha = horizonAlpha * 0.95f),
                    upperColor.copy(alpha = skyAlpha * 0.45f),
                    Color.Transparent,
                  ),
                center = center,
                radius = radius,
              )
            onDrawBehind {
              drawCircle(brush = bayBrush, radius = radius, center = center)
            }
          }
    )
  }
}
