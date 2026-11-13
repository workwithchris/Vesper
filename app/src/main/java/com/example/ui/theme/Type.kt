package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily =
  FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Light),
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
  )

val JetBrainsMonoFamily =
  FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Light),
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
  )

val GeistMonoNumericStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
    fontFeatureSettings = "tnum",
  )

val SynthwaveClockDigitsStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 52.sp,
    lineHeight = 56.sp,
    letterSpacing = (-2.0).sp,
    fontFeatureSettings = "tnum",
  )

val Typography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2.0).sp,
        fontFeatureSettings = "tnum",
      ),
    headlineMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        fontFeatureSettings = "tnum",
      ),
    labelSmall =
      TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = "tnum",
      ),
  )
