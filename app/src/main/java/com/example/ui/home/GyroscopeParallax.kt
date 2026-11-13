package com.example.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.exp

data class TiltStep(
  val x: Float,
  val y: Float,
)

data class GravityFallbackStep(
  val tiltX: Float,
  val tiltY: Float,
  val baseX: Float,
  val baseY: Float,
)

object GyroParallaxMath {
  private const val MAX_DT_SECONDS = 0.08f

  /**
   * Integrates gyroscope angular velocities (rad/s) into normalized [-1f, 1f] parallax offsets
   * with exponential re-centering so comfortable resting hold angles naturally normalize to zero.
   */
  fun stepGyroscopeTilt(
    currentX: Float,
    currentY: Float,
    angularVelX: Float,
    angularVelY: Float,
    dtSeconds: Float,
    gain: Float = 1.55f,
    decayPerSec: Float = 1.65f,
  ): TiltStep {
    if (dtSeconds <= 0f) return TiltStep(currentX.coerceIn(-1f, 1f), currentY.coerceIn(-1f, 1f))
    val clampedDt = dtSeconds.coerceAtMost(MAX_DT_SECONDS)
    val decayFactor = exp((-decayPerSec * clampedDt).toDouble()).toFloat()

    // Positive roll around Y (tilting left edge toward user) shifts background rightward (+X).
    // Positive pitch around X (tilting top toward user) shifts background downward (+Y).
    val nextX = (currentX * decayFactor + angularVelY * gain * clampedDt).coerceIn(-1f, 1f)
    val nextY = (currentY * decayFactor + angularVelX * gain * clampedDt).coerceIn(-1f, 1f)
    return TiltStep(nextX, nextY)
  }

  /**
   * Fallback for budget devices without a hardware gyroscope: tracks relative gravity/accelerometer
   * changes against an adapting resting baseline.
   */
  fun stepGravityTilt(
    rawX: Float,
    rawY: Float,
    baseX: Float,
    baseY: Float,
    dtSeconds: Float,
    baselineAdaptRate: Float = 0.75f,
    sensitivityDivisor: Float = 4.5f,
  ): GravityFallbackStep {
    val clampedDt = dtSeconds.coerceIn(0f, MAX_DT_SECONDS)
    val alpha = (1f - exp((-baselineAdaptRate * clampedDt).toDouble()).toFloat()).coerceIn(0f, 1f)
    val nextBaseX = baseX + (rawX - baseX) * alpha
    val nextBaseY = baseY + (rawY - baseY) * alpha
    val tiltX = ((rawX - nextBaseX) / sensitivityDivisor).coerceIn(-1f, 1f)
    val tiltY = (-(rawY - nextBaseY) / sensitivityDivisor).coerceIn(-1f, 1f)
    return GravityFallbackStep(
      tiltX = tiltX,
      tiltY = tiltY,
      baseX = nextBaseX,
      baseY = nextBaseY,
    )
  }

  fun smoothToward(
    current: Float,
    target: Float,
    dtSeconds: Float,
    speed: Float = 12f,
  ): Float {
    if (dtSeconds <= 0f) return current
    val clampedDt = dtSeconds.coerceAtMost(MAX_DT_SECONDS)
    val factor = 1f - exp((-speed * clampedDt).toDouble()).toFloat()
    val next = current + (target - current) * factor
    return if (abs(target - next) < 0.0005f) target else next
  }
}

data class WallpaperParallaxOffsets(
  val backdropTranslationXPx: Float,
  val skyBloomTranslationXPx: Float,
  val shorelineTranslationXPx: Float,
  val weatherTranslationXPx: Float,
  val systemWallpaperXOffset: Float,
)

object WallpaperParallaxMath {
  /**
   * Computes multi-plane horizontal depth translations for a viewport of [widthPx]
   * given normalized [pageProgress] in [0f, 1f] across home pages and gyroscope [tiltX] in [-1f, 1f].
   *
   * Symmetric centered progress (-1f..+1f) keeps scaled layers (1.24x+ width) well inside bounds
   * across every page without exposing canvas edges.
   */
  fun computeHorizontalDepth(
    widthPx: Float,
    pageProgress: Float,
    tiltX: Float,
    tiltPxPerUnit: Float,
    reducedMotion: Boolean,
  ): WallpaperParallaxOffsets {
    val clampedWallpaperOffset = pageProgress.coerceIn(0f, 1f)
    if (reducedMotion || widthPx <= 0f) {
      return WallpaperParallaxOffsets(
        backdropTranslationXPx = 0f,
        skyBloomTranslationXPx = 0f,
        shorelineTranslationXPx = 0f,
        weatherTranslationXPx = 0f,
        systemWallpaperXOffset = clampedWallpaperOffset,
      )
    }
    val centered = (pageProgress.coerceIn(-0.15f, 1.15f) - 0.5f) * 2f
    return WallpaperParallaxOffsets(
      backdropTranslationXPx = -centered * (widthPx * 0.065f) + tiltX * tiltPxPerUnit * 1.0f,
      skyBloomTranslationXPx = -centered * (widthPx * 0.11f) + tiltX * tiltPxPerUnit * 1.5f,
      shorelineTranslationXPx = -centered * (widthPx * 0.16f) + tiltX * tiltPxPerUnit * 1.9f,
      weatherTranslationXPx = -centered * (widthPx * 0.09f) + tiltX * tiltPxPerUnit * 2.2f,
      systemWallpaperXOffset = clampedWallpaperOffset,
    )
  }
}

@Stable
class GyroscopeParallaxState(
  initialX: Float = 0f,
  initialY: Float = 0f,
) {
  var offsetX by mutableFloatStateOf(initialX.coerceIn(-1f, 1f))
    internal set

  var offsetY by mutableFloatStateOf(initialY.coerceIn(-1f, 1f))
    internal set

  internal var targetX: Float = initialX.coerceIn(-1f, 1f)
  internal var targetY: Float = initialY.coerceIn(-1f, 1f)

  internal fun resetImmediate() {
    targetX = 0f
    targetY = 0f
    offsetX = 0f
    offsetY = 0f
  }
}

@Composable
fun rememberGyroscopeParallaxState(enabled: Boolean): GyroscopeParallaxState {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val state = remember { GyroscopeParallaxState() }

  LaunchedEffect(enabled) {
    if (!enabled) {
      state.resetImmediate()
      return@LaunchedEffect
    }
    var lastFrameNanos = 0L
    while (true) {
      withFrameNanos { frameNanos ->
        if (lastFrameNanos != 0L) {
          val dt = (frameNanos - lastFrameNanos) * 1e-9f
          if (state.targetX != 0f || state.targetY != 0f) {
            val decay = exp((-0.9f * dt.coerceAtMost(0.08f)).toDouble()).toFloat()
            state.targetX = if (abs(state.targetX) < 0.002f) 0f else state.targetX * decay
            state.targetY = if (abs(state.targetY) < 0.002f) 0f else state.targetY * decay
          }
          state.offsetX = GyroParallaxMath.smoothToward(state.offsetX, state.targetX, dt)
          state.offsetY = GyroParallaxMath.smoothToward(state.offsetY, state.targetY, dt)
        }
        lastFrameNanos = frameNanos
      }
    }
  }

  DisposableEffect(enabled, context, lifecycleOwner) {
    if (!enabled) {
      state.resetImmediate()
      return@DisposableEffect onDispose {}
    }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      ?: return@DisposableEffect onDispose {}

    val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val fallbackSensor =
      if (gyroSensor == null) {
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
          ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      } else null
    val activeSensor = gyroSensor ?: fallbackSensor ?: return@DisposableEffect onDispose {}
    val usingGyro = gyroSensor != null

    var lastTimestampNs = 0L
    var hasGravityBaseline = false
    var baseGx = 0f
    var baseGy = 0f

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          if (event.values.size < 2) return
          if (lastTimestampNs == 0L) {
            lastTimestampNs = event.timestamp
            if (!usingGyro) {
              baseGx = event.values[0]
              baseGy = event.values[1]
              hasGravityBaseline = true
            }
            return
          }

          val dt = (event.timestamp - lastTimestampNs) * 1e-9f
          lastTimestampNs = event.timestamp
          if (dt <= 0f) return

          if (usingGyro) {
            val step =
              GyroParallaxMath.stepGyroscopeTilt(
                currentX = state.targetX,
                currentY = state.targetY,
                angularVelX = event.values[0],
                angularVelY = event.values[1],
                dtSeconds = dt,
              )
            state.targetX = step.x
            state.targetY = step.y
          } else {
            if (!hasGravityBaseline) {
              baseGx = event.values[0]
              baseGy = event.values[1]
              hasGravityBaseline = true
              return
            }
            val step =
              GyroParallaxMath.stepGravityTilt(
                rawX = event.values[0],
                rawY = event.values[1],
                baseX = baseGx,
                baseY = baseGy,
                dtSeconds = dt,
              )
            baseGx = step.baseX
            baseGy = step.baseY
            state.targetX = step.tiltX
            state.targetY = step.tiltY
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    var isRegistered = false
    fun registerSensor() {
      if (!isRegistered) {
        lastTimestampNs = 0L
        isRegistered =
          sensorManager.registerListener(listener, activeSensor, SensorManager.SENSOR_DELAY_GAME)
      }
    }

    fun unregisterSensor() {
      if (isRegistered) {
        sensorManager.unregisterListener(listener)
        isRegistered = false
      }
    }

    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      registerSensor()
    }

    val lifecycleObserver = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> registerSensor()
        Lifecycle.Event.ON_PAUSE -> {
          unregisterSensor()
          state.resetImmediate()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
      unregisterSensor()
    }
  }

  return state
}
