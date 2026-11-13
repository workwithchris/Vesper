package com.example.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalLauncherIsDark
import com.example.ui.theme.SynthwaveAmber
import com.example.ui.theme.SynthwaveClockDigitsStyle
import com.example.ui.theme.SynthwaveCyan
import com.example.ui.theme.SynthwaveMagenta
import com.example.ui.theme.SynthwaveViolet

@Composable
fun SynthwaveClockWidget(
  formattedTime: String,
  amPmLabel: String,
  formattedDate: String,
  locationLabel: String,
  reducedMotion: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalLauncherIsDark.current

  val neonPulseAlpha: Float =
    if (reducedMotion) {
      0.72f
    } else {
      val transition = rememberInfiniteTransition(label = "synthwave_clock_glow")
      val animatedAlpha by
        transition.animateFloat(
          initialValue = 0.52f,
          targetValue = 0.88f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "neon_alpha",
        )
      animatedAlpha
    }

  val cardShape = RoundedCornerShape(10.dp)
  val borderColor =
    if (isDark) MaterialTheme.colorScheme.outlineVariant
    else MaterialTheme.colorScheme.outline
  val surfaceColor =
    if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)

  val digitColors =
    if (isDark) {
      listOf(SynthwaveCyan, Color.White, SynthwaveMagenta)
    } else {
      listOf(Color(0xFF0E7490), Color(0xFF6D28D9), Color(0xFFBE185D))
    }
  val glowColorPrimary = if (isDark) SynthwaveMagenta else SynthwaveViolet
  val glowColorSecondary = if (isDark) SynthwaveCyan else SynthwaveMagenta

  Surface(
    shape = cardShape,
    color = surfaceColor,
    border = BorderStroke(1.dp, borderColor),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(cardShape)
        .clickable(onClick = onClick)
        .semantics { contentDescription = "Digital clock $formattedTime $amPmLabel, $formattedDate" }
        .testTag("status_clock"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .drawWithCache {
            val topAura =
              Brush.radialGradient(
                colors =
                  listOf(
                    glowColorPrimary.copy(alpha = if (isDark) 0.14f else 0.08f),
                    glowColorSecondary.copy(alpha = if (isDark) 0.06f else 0.03f),
                    Color.Transparent,
                  ),
                center = Offset(size.width * 0.20f, size.height * 0.45f),
                radius = size.width * 0.75f,
              )
            onDrawBehind { drawRect(brush = topAura) }
          }
          .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      // Top Geist / Synthwave micro-status header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isDark) SynthwaveCyan else MaterialTheme.colorScheme.primary)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${locationLabel.uppercase()} · SYNTH",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
        ) {
          Text(
            text = amPmLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isDark) SynthwaveCyan else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Layered neon-glow digital time readout
      Box(contentAlignment = Alignment.CenterStart) {
        // Outer neon bloom layer (blurred shadow only)
        Text(
          text = formattedTime,
          style =
            SynthwaveClockDigitsStyle.copy(
              color = glowColorPrimary.copy(alpha = (neonPulseAlpha * 0.55f).coerceIn(0f, 1f)),
              shadow =
                Shadow(
                  color = glowColorPrimary.copy(alpha = neonPulseAlpha),
                  offset = Offset.Zero,
                  blurRadius = if (isDark) 28f else 14f,
                ),
            ),
          maxLines = 1,
        )
        // Secondary cyan rim glow + crisp gradient foreground digits
        Text(
          text = formattedTime,
          style =
            SynthwaveClockDigitsStyle.copy(
              brush = Brush.horizontalGradient(colors = digitColors),
              shadow =
                Shadow(
                  color = glowColorSecondary.copy(alpha = neonPulseAlpha * 0.45f),
                  offset = Offset(0f, 1f),
                  blurRadius = if (isDark) 10f else 4f,
                ),
            ),
          maxLines = 1,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Minimal glowing synthwave horizon beam with subtle perspective ticks
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(6.dp)
            .drawWithCache {
              val beamBrush =
                Brush.horizontalGradient(
                  0.0f to SynthwaveCyan.copy(alpha = if (isDark) 0.90f else 0.70f),
                  0.45f to SynthwaveMagenta.copy(alpha = if (isDark) 0.92f else 0.75f),
                  0.80f to SynthwaveAmber.copy(alpha = if (isDark) 0.75f else 0.55f),
                  1.0f to Color.Transparent,
                )
              val tickColor =
                (if (isDark) SynthwaveCyan else SynthwaveViolet).copy(alpha = 0.28f)
              onDrawBehind {
                drawLine(
                  brush = beamBrush,
                  start = Offset(0f, 1.dp.toPx()),
                  end = Offset(size.width, 1.dp.toPx()),
                  strokeWidth = 1.5.dp.toPx(),
                )
                val tickCount = 6
                val step = size.width * 0.14f
                for (i in 0 until tickCount) {
                  val x = i * step
                  drawLine(
                    color = tickColor,
                    start = Offset(x, 1.dp.toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                  )
                }
              }
            }
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = formattedDate,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}
