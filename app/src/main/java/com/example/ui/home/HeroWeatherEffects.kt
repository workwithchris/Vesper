package com.example.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.WeatherEffectType
import com.example.model.WeatherInfo
import com.example.ui.theme.LocalLauncherIsDark
import com.example.ui.theme.WeatherCloudDusk
import com.example.ui.theme.WeatherCloudMist
import com.example.ui.theme.WeatherHeatCoral
import com.example.ui.theme.WeatherHeatGold
import com.example.ui.theme.WeatherLightningGlow
import com.example.ui.theme.WeatherRainCyan
import com.example.ui.theme.WeatherStormViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

private data class RainDropSpec(
  val xFraction: Float,
  val speedMultiple: Int,
  val phaseOffset: Float,
  val lengthDp: Float,
  val strokeDp: Float,
  val depthAlpha: Float,
)

private data class CoastalCloudSpec(
  val yFraction: Float,
  val widthFraction: Float,
  val heightDp: Float,
  val speedMultiple: Int,
  val phaseOffset: Float,
  val opacity: Float,
)

private data class BreezeMoteSpec(
  val xFraction: Float,
  val yFraction: Float,
  val radiusDp: Float,
  val speedMultiple: Int,
  val phaseOffset: Float,
  val swayAmplitudeDp: Float,
)

@Composable
fun HeroWeatherAnimationLayer(
  weather: WeatherInfo,
  coastalPalette: CoastalGradientPalette,
  reducedMotion: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalLauncherIsDark.current

  val fastCycle: Float
  val slowDrift: Float
  val shimmerBreath: Float
  if (reducedMotion) {
    fastCycle = 0.36f
    slowDrift = 0.25f
    shimmerBreath = 0.5f
  } else {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_weather_fx")
    val animFast by
      infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
        label = "weather_fast_cycle",
      )
    val animSlow by
      infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
        label = "weather_slow_drift",
      )
    val animBreath by
      infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 4_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "weather_shimmer_breath",
      )
    fastCycle = animFast
    slowDrift = animSlow
    shimmerBreath = animBreath
  }

  val rainDrops = remember { buildRainDrops(count = 24) }
  val clouds = remember { buildCoastalClouds() }
  val breezeMotes = remember { buildBreezeMotes(count = 12) }
  val reusablePath = remember { Path() }

  Canvas(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("hero_weather_animation_${weather.effectType.name.lowercase()}"),
  ) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    when (weather.effectType) {
      WeatherEffectType.CLEAR_BREEZE -> {
        drawClearCoastalBreeze(
          w = w,
          h = h,
          isDay = weather.isDay,
          isDark = isDark,
          palette = coastalPalette,
          slowDrift = slowDrift,
          shimmerBreath = shimmerBreath,
          clouds = clouds.take(2),
          motes = breezeMotes,
        )
      }
      WeatherEffectType.HEAT_HAZE -> {
        drawHeatHazeDistortion(
          w = w,
          h = h,
          isDark = isDark,
          palette = coastalPalette,
          fastCycle = fastCycle,
          shimmerBreath = shimmerBreath,
          reusablePath = reusablePath,
        )
      }
      WeatherEffectType.CLOUDY -> {
        drawDriftingCoastalClouds(
          w = w,
          h = h,
          isDark = isDark,
          isStorm = false,
          cloudDensity = (weather.cloudCoverPercent / 100f).coerceIn(0.45f, 1f),
          palette = coastalPalette,
          slowDrift = slowDrift,
          clouds = clouds,
        )
      }
      WeatherEffectType.RAIN -> {
        drawDriftingCoastalClouds(
          w = w,
          h = h,
          isDark = isDark,
          isStorm = false,
          cloudDensity = 0.65f,
          palette = coastalPalette,
          slowDrift = slowDrift,
          clouds = clouds.take(3),
        )
        drawTropicalRain(
          w = w,
          h = h,
          isDark = isDark,
          palette = coastalPalette,
          fastCycle = fastCycle,
          drops = rainDrops,
        )
      }
      WeatherEffectType.THUNDERSTORM -> {
        drawSheetLightningGlow(
          w = w,
          h = h,
          palette = coastalPalette,
          fastCycle = fastCycle,
        )
        drawDriftingCoastalClouds(
          w = w,
          h = h,
          isDark = isDark,
          isStorm = true,
          cloudDensity = 0.95f,
          palette = coastalPalette,
          slowDrift = slowDrift,
          clouds = clouds,
        )
        drawTropicalRain(
          w = w,
          h = h,
          isDark = isDark,
          palette = coastalPalette,
          fastCycle = fastCycle,
          drops = rainDrops,
          isStorm = true,
        )
      }
    }
  }
}

private fun heroVerticalFade(y: Float, height: Float): Float {
  val normY = (y / height).coerceIn(0f, 1f)
  val topFade = smoothstep(0f, 0.08f, normY)
  val bottomFade = 1f - smoothstep(0.64f, 0.98f, normY)
  return (topFade * bottomFade).coerceIn(0f, 1f)
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
  val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
  return t * t * (3f - 2f * t)
}

private fun DrawScope.drawSoftRadialPuff(
  center: Offset,
  radius: Float,
  innerColor: Color,
  outerColor: Color,
  alpha: Float,
) {
  if (alpha <= 0.005f || radius <= 1f) return
  drawCircle(
    color = outerColor.copy(alpha = alpha * 0.25f),
    radius = radius,
    center = center,
  )
  drawCircle(
    color = innerColor.copy(alpha = alpha * 0.42f),
    radius = radius * 0.66f,
    center = center,
  )
  drawCircle(
    color = innerColor.copy(alpha = alpha * 0.52f),
    radius = radius * 0.36f,
    center = center,
  )
}

private fun DrawScope.drawClearCoastalBreeze(
  w: Float,
  h: Float,
  isDay: Boolean,
  isDark: Boolean,
  palette: CoastalGradientPalette,
  slowDrift: Float,
  shimmerBreath: Float,
  clouds: List<CoastalCloudSpec>,
  motes: List<BreezeMoteSpec>,
) {
  val haloCenter = Offset(w * 0.84f, h * 0.24f)
  val haloRadius = h * (0.58f + shimmerBreath * 0.08f)
  val haloColor = if (isDay) lerp(WeatherHeatGold, palette.sunsetWash, 0.35f) else WeatherRainCyan
  val haloAlpha = if (isDark) 0.14f else 0.16f
  drawSoftRadialPuff(
    center = haloCenter,
    radius = haloRadius,
    innerColor = haloColor,
    outerColor = palette.sunsetWash,
    alpha = haloAlpha,
  )

  drawDriftingCoastalClouds(
    w = w,
    h = h,
    isDark = isDark,
    isStorm = false,
    cloudDensity = 0.38f,
    palette = palette,
    slowDrift = slowDrift,
    clouds = clouds,
  )

  val moteColor = if (isDay) WeatherHeatGold else WeatherRainCyan
  val twoPi = (2.0 * PI).toFloat()
  val baseMoteAlpha = if (isDark) 0.36f else 0.28f
  motes.forEach { mote ->
    val t = ((slowDrift * mote.speedMultiple) + mote.phaseOffset) % 1f
    val x = (-0.1f + t * 1.2f) * w
    val baseY = mote.yFraction * h
    val y = baseY + sin(t * twoPi * 2f + mote.phaseOffset * twoPi) * mote.swayAmplitudeDp.dp.toPx()
    val alpha = heroVerticalFade(y, h) * baseMoteAlpha
    if (alpha > 0.01f) {
      drawCircle(
        color = moteColor.copy(alpha = alpha),
        radius = mote.radiusDp.dp.toPx(),
        center = Offset(x, y),
      )
    }
  }
}

private fun DrawScope.drawHeatHazeDistortion(
  w: Float,
  h: Float,
  isDark: Boolean,
  palette: CoastalGradientPalette,
  fastCycle: Float,
  shimmerBreath: Float,
  reusablePath: Path,
) {
  val sunCenter = Offset(w * (0.80f + shimmerBreath * 0.04f), h * 0.22f)
  val sunRadius = h * (0.82f + shimmerBreath * 0.10f)
  val coronaColor = lerp(WeatherHeatGold, WeatherHeatCoral, 0.45f)
  drawSoftRadialPuff(
    center = sunCenter,
    radius = sunRadius,
    innerColor = coronaColor,
    outerColor = palette.sunsetWash,
    alpha = if (isDark) 0.20f else 0.22f,
  )

  val twoPi = (2.0 * PI).toFloat()
  val bandCount = 5
  val steps = 20
  val baseWaveColor = lerp(WeatherHeatGold, palette.sunsetWash, 0.4f)
  val waveScale = if (isDark) 0.15f else 0.13f

  for (band in 0 until bandCount) {
    val riseProgress = ((fastCycle * 2f) + band / bandCount.toFloat()) % 1f
    val centerY = h * (0.84f - riseProgress * 0.72f)
    val fade = heroVerticalFade(centerY, h)
    val cycleFade = sin(riseProgress * PI.toFloat()).coerceIn(0f, 1f)
    val alpha = fade * cycleFade * waveScale
    if (alpha <= 0.005f) continue

    val ampPx = (3.5f + (band % 3) * 1.5f).dp.toPx()
    val phase = fastCycle * twoPi * 3f + band * 1.15f

    reusablePath.reset()
    for (s in 0..steps) {
      val fracX = s / steps.toFloat()
      val x = fracX * w
      val waveY =
        centerY +
          sin(fracX * twoPi * 2.5f + phase) * ampPx +
          cos(fracX * twoPi * 4.0f - phase * 1.25f) * (ampPx * 0.45f)
      if (s == 0) reusablePath.moveTo(x, waveY) else reusablePath.lineTo(x, waveY)
    }

    val strokeW = (9.dp.toPx() - band * 0.8.dp.toPx()).coerceAtLeast(3.dp.toPx())
    val waveColor = if (band % 2 == 0) baseWaveColor else WeatherHeatCoral
    drawPath(
      path = reusablePath,
      color = waveColor.copy(alpha = alpha),
      style = Stroke(width = strokeW, cap = StrokeCap.Round),
    )
  }

  val filamentScale = if (isDark) 0.11f else 0.09f
  for (i in 0 until 6) {
    val x = w * (0.14f + i * 0.14f) + sin(fastCycle * twoPi * 2f + i) * 5.dp.toPx()
    val filamentProgress = ((fastCycle * 2f) + i * 0.19f) % 1f
    val topY = h * (0.76f - filamentProgress * 0.56f)
    val segHeight = 28.dp.toPx()
    val alpha = heroVerticalFade(topY, h) * sin(filamentProgress * PI.toFloat()) * filamentScale
    if (alpha > 0.005f) {
      drawLine(
        color = WeatherHeatGold.copy(alpha = alpha),
        start = Offset(x, topY - segHeight),
        end = Offset(x + sin(fastCycle * twoPi * 4f + i) * 3.dp.toPx(), topY + segHeight),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}

private fun DrawScope.drawDriftingCoastalClouds(
  w: Float,
  h: Float,
  isDark: Boolean,
  isStorm: Boolean,
  cloudDensity: Float,
  palette: CoastalGradientPalette,
  slowDrift: Float,
  clouds: List<CoastalCloudSpec>,
) {
  val baseColor =
    when {
      isStorm && isDark -> lerp(WeatherCloudDusk, palette.upperSky, 0.35f)
      isStorm -> lerp(WeatherCloudDusk, WeatherCloudMist, 0.45f)
      isDark -> lerp(WeatherCloudMist, palette.sunsetWash, 0.28f)
      else -> lerp(WeatherCloudMist, palette.oceanHorizon, 0.15f)
    }
  val rimColor =
    if (isStorm) lerp(palette.sunsetWash, WeatherStormViolet, 0.5f)
    else lerp(palette.sunsetWash, WeatherHeatGold, 0.35f)

  val modeAlphaScale = if (isDark) 0.24f else 0.36f
  clouds.forEach { spec ->
    val t = ((slowDrift * spec.speedMultiple) + spec.phaseOffset) % 1f
    val cloudWidth = w * spec.widthFraction
    val cloudHeight = spec.heightDp.dp.toPx()
    val centerX = -cloudWidth * 0.45f + t * (w + cloudWidth * 0.9f)
    val centerY = h * spec.yFraction
    val edgeAlpha = heroVerticalFade(centerY, h)
    val baseAlpha = spec.opacity * cloudDensity * edgeAlpha * modeAlphaScale
    if (baseAlpha <= 0.006f) return@forEach

    drawSoftRadialPuff(
      center = Offset(centerX - 0.24f * cloudWidth, centerY + 0.08f * cloudHeight),
      radius = (cloudWidth * 0.20f).coerceAtLeast(cloudHeight * 0.65f),
      innerColor = baseColor,
      outerColor = rimColor,
      alpha = baseAlpha,
    )
    drawSoftRadialPuff(
      center = Offset(centerX - 0.02f * cloudWidth, centerY - 0.12f * cloudHeight),
      radius = (cloudWidth * 0.25f).coerceAtLeast(cloudHeight * 0.78f),
      innerColor = baseColor,
      outerColor = rimColor,
      alpha = baseAlpha,
    )
    drawSoftRadialPuff(
      center = Offset(centerX + 0.20f * cloudWidth, centerY + 0.02f * cloudHeight),
      radius = (cloudWidth * 0.21f).coerceAtLeast(cloudHeight * 0.68f),
      innerColor = baseColor,
      outerColor = rimColor,
      alpha = baseAlpha,
    )
  }
}

private fun DrawScope.drawTropicalRain(
  w: Float,
  h: Float,
  isDark: Boolean,
  palette: CoastalGradientPalette,
  fastCycle: Float,
  drops: List<RainDropSpec>,
  isStorm: Boolean = false,
) {
  val dropColorPrimary = lerp(WeatherRainCyan, Color.White, 0.35f)
  val dropColorSunsetTint = lerp(palette.sunsetWash, WeatherRainCyan, 0.55f)
  val speedBoost = if (isStorm) 2 else 1
  val modeScale = if (isDark) 0.48f else 0.38f

  drops.forEachIndexed { idx, drop ->
    val t = ((fastCycle * drop.speedMultiple * speedBoost) + drop.phaseOffset) % 1f
    val headY = t * h * 0.96f
    val lengthPx = drop.lengthDp.dp.toPx() * (if (isStorm) 1.25f else 1f)
    val windLeanPx = -lengthPx * (if (isStorm) 0.28f else 0.18f)
    val startX = drop.xFraction * w + (t * h * (if (isStorm) -0.12f else -0.07f))
    val wrappedX = ((startX % w) + w) % w

    val fade = heroVerticalFade(headY, h)
    val alpha = fade * drop.depthAlpha * modeScale
    if (alpha > 0.01f) {
      val color = if (idx % 3 == 0) dropColorSunsetTint else dropColorPrimary
      val strokePx = drop.strokeDp.dp.toPx()
      drawLine(
        color = color.copy(alpha = alpha * 0.40f),
        start = Offset(wrappedX - windLeanPx, headY - lengthPx),
        end = Offset(wrappedX - windLeanPx * 0.45f, headY - lengthPx * 0.45f),
        strokeWidth = strokePx,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(wrappedX - windLeanPx * 0.45f, headY - lengthPx * 0.45f),
        end = Offset(wrappedX, headY),
        strokeWidth = strokePx,
        cap = StrokeCap.Round,
      )
    }

    if (idx % 3 == 0 && t > 0.78f && t < 0.96f) {
      val rippleProgress = ((t - 0.78f) / 0.18f).coerceIn(0f, 1f)
      val rippleY = h * (0.70f + (idx % 4) * 0.045f)
      val rippleAlpha = (1f - rippleProgress) * heroVerticalFade(rippleY, h) * 0.22f
      if (rippleAlpha > 0.008f) {
        val rx = (4f + rippleProgress * 10f).dp.toPx()
        val ry = rx * 0.35f
        drawOval(
          color = dropColorPrimary.copy(alpha = rippleAlpha),
          topLeft = Offset(wrappedX - rx, rippleY - ry),
          size = Size(rx * 2f, ry * 2f),
          style = Stroke(width = 1.dp.toPx()),
        )
      }
    }
  }
}

private fun DrawScope.drawSheetLightningGlow(
  w: Float,
  h: Float,
  palette: CoastalGradientPalette,
  fastCycle: Float,
) {
  val pulse1 = exp(-((fastCycle - 0.22f) / 0.016f).pow(2))
  val pulse2 = exp(-((fastCycle - 0.27f) / 0.012f).pow(2)) * 0.7f
  val pulse3 = exp(-((fastCycle - 0.74f) / 0.018f).pow(2)) * 0.85f
  val intensity = (pulse1 + pulse2 + pulse3).coerceIn(0f, 1f)
  if (intensity <= 0.01f) return

  val flashCenter = Offset(w * (0.28f + (if (fastCycle > 0.5f) 0.42f else 0f)), h * 0.18f)
  val flashColor = lerp(WeatherStormViolet, WeatherLightningGlow, 0.55f)
  val tintWash = lerp(flashColor, palette.sunsetWash, 0.25f)
  drawSoftRadialPuff(
    center = flashCenter,
    radius = w * 0.72f,
    innerColor = tintWash,
    outerColor = WeatherStormViolet,
    alpha = 0.28f * intensity,
  )
}

private fun buildRainDrops(count: Int): List<RainDropSpec> =
  List(count) { i ->
    val fracX = ((i * 37 + 13) % 100) / 100f
    val speed = if (i % 3 == 0) 4 else if (i % 2 == 0) 3 else 2
    val phase = ((i * 29 + 7) % 100) / 100f
    val length = 14f + (i % 5) * 4.5f
    val stroke = if (speed >= 4) 1.45f else 1.05f
    val depthAlpha = if (speed >= 4) 0.90f else 0.55f + (i % 3) * 0.1f
    RainDropSpec(
      xFraction = fracX,
      speedMultiple = speed,
      phaseOffset = phase,
      lengthDp = length,
      strokeDp = stroke,
      depthAlpha = depthAlpha,
    )
  }

private fun buildCoastalClouds(): List<CoastalCloudSpec> =
  listOf(
    CoastalCloudSpec(yFraction = 0.16f, widthFraction = 0.58f, heightDp = 52f, speedMultiple = 1, phaseOffset = 0.08f, opacity = 0.85f),
    CoastalCloudSpec(yFraction = 0.30f, widthFraction = 0.66f, heightDp = 58f, speedMultiple = 1, phaseOffset = 0.48f, opacity = 0.95f),
    CoastalCloudSpec(yFraction = 0.22f, widthFraction = 0.46f, heightDp = 42f, speedMultiple = 2, phaseOffset = 0.72f, opacity = 0.72f),
    CoastalCloudSpec(yFraction = 0.46f, widthFraction = 0.62f, heightDp = 50f, speedMultiple = 1, phaseOffset = 0.26f, opacity = 0.78f),
    CoastalCloudSpec(yFraction = 0.38f, widthFraction = 0.50f, heightDp = 44f, speedMultiple = 2, phaseOffset = 0.91f, opacity = 0.65f),
  )

private fun buildBreezeMotes(count: Int): List<BreezeMoteSpec> =
  List(count) { i ->
    BreezeMoteSpec(
      xFraction = ((i * 43 + 11) % 100) / 100f,
      yFraction = 0.10f + (((i * 23 + 5) % 62) / 100f),
      radiusDp = 1.4f + (i % 3) * 0.65f,
      speedMultiple = if (i % 2 == 0) 2 else 1,
      phaseOffset = ((i * 31 + 19) % 100) / 100f,
      swayAmplitudeDp = 6f + (i % 4) * 3.5f,
    )
  }
