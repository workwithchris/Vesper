package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

val LocalLauncherHapticsEnabled = compositionLocalOf { true }

enum class LauncherHapticEvent {
  APP_LAUNCH,
  LONG_PRESS,
  DRAWER_OPEN_SWIPE,
  DRAWER_CLOSE_SWIPE,
  SCRUBBER_TICK,
  TOGGLE_SWITCH,
  CONFIRM_ACTION,
  ERROR_FEEDBACK,
  SUBTLE_TAP,
}

object LauncherHapticsResolver {
  fun resolveAndroidHapticConstant(
    event: LauncherHapticEvent,
    sdkInt: Int = Build.VERSION.SDK_INT,
  ): Int =
    when (event) {
      LauncherHapticEvent.APP_LAUNCH -> HapticFeedbackConstants.CONTEXT_CLICK
      LauncherHapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
      LauncherHapticEvent.DRAWER_OPEN_SWIPE ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_START
        else HapticFeedbackConstants.CONTEXT_CLICK
      LauncherHapticEvent.DRAWER_CLOSE_SWIPE ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END
        else HapticFeedbackConstants.CLOCK_TICK
      LauncherHapticEvent.SCRUBBER_TICK -> HapticFeedbackConstants.CLOCK_TICK
      LauncherHapticEvent.TOGGLE_SWITCH -> HapticFeedbackConstants.CONTEXT_CLICK
      LauncherHapticEvent.CONFIRM_ACTION ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.CONTEXT_CLICK
      LauncherHapticEvent.ERROR_FEEDBACK ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS
      LauncherHapticEvent.SUBTLE_TAP -> HapticFeedbackConstants.CLOCK_TICK
    }
}

@Immutable
class LauncherHaptics(
  private val view: View,
  val enabled: Boolean,
) {
  fun trigger(event: LauncherHapticEvent, forceWhenDisabled: Boolean = false) {
    if (!enabled && !forceWhenDisabled) return
    val constant = LauncherHapticsResolver.resolveAndroidHapticConstant(event)
    view.performHapticFeedback(constant)
  }
}

@Composable
fun rememberLauncherHaptics(
  enabled: Boolean = LocalLauncherHapticsEnabled.current,
): LauncherHaptics {
  val view = LocalView.current
  return remember(view, enabled) { LauncherHaptics(view = view, enabled = enabled) }
}
