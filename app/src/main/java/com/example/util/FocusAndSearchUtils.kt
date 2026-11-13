package com.example.util

import com.example.model.DrawerQuickAction
import com.example.model.LauncherSettings
import java.util.Calendar

object FocusAndSearchUtils {

  private val PHONE_REGEX = Regex("^[+]?[0-9\\-().\\s]{3,20}$")
  private val DOMAIN_OR_URL_REGEX =
    Regex("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$")

  fun isFocusModeActive(
    settings: LauncherSettings,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    if (settings.focusModeManualEnabled || settings.focusTimerEndMillis > nowMillis) {
      return true
    }
    if (!settings.focusScheduleEnabled) return false
    return isFocusScheduleActive(
      startMinutes = settings.focusScheduleStartMinutes,
      endMinutes = settings.focusScheduleEndMinutes,
      nowMillis = nowMillis,
    )
  }

  fun isFocusScheduleActive(
    startMinutes: Int,
    endMinutes: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    return isMinuteInsideSchedule(currentMinutes, startMinutes, endMinutes)
  }

  fun isMinuteInsideSchedule(
    currentMinutes: Int,
    startMinutes: Int,
    endMinutes: Int,
  ): Boolean {
    val normCurrent = ((currentMinutes % 1440) + 1440) % 1440
    val normStart = ((startMinutes % 1440) + 1440) % 1440
    val normEnd = ((endMinutes % 1440) + 1440) % 1440
    if (normStart == normEnd) return true
    return if (normStart < normEnd) {
      normCurrent in normStart until normEnd
    } else {
      // Overnight schedule (e.g., 22:00 to 07:00)
      normCurrent >= normStart || normCurrent < normEnd
    }
  }

  fun formatMinutesOfDay(minutes: Int): String {
    val norm = ((minutes % 1440) + 1440) % 1440
    val h24 = norm / 60
    val m = norm % 60
    val amPm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
      h24 == 0 -> 12
      h24 > 12 -> h24 - 12
      else -> h24
    }
    return "%d:%02d %s".format(h12, m, amPm)
  }

  fun detectDrawerQuickAction(rawQuery: String): DrawerQuickAction? {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return null
    val digitCount = trimmed.count { it.isDigit() }
    val clearlyFormattedPhone =
      trimmed.startsWith("+") || trimmed.contains('(') || trimmed.contains(' ')
    if (digitCount >= 3 &&
      PHONE_REGEX.matches(trimmed) &&
      (clearlyFormattedPhone || MathEvaluator.evaluateToString(trimmed) == null)
    ) {
      return DrawerQuickAction.DialPhone(displayNumber = trimmed)
    }
    if (!trimmed.contains(' ') && DOMAIN_OR_URL_REGEX.matches(trimmed)) {
      val normalizedUrl =
        if (trimmed.startsWith("http://", ignoreCase = true) ||
          trimmed.startsWith("https://", ignoreCase = true)
        ) {
          trimmed
        } else {
          "https://$trimmed"
        }
      return DrawerQuickAction.OpenUrl(url = normalizedUrl)
    }
    return null
  }
}
