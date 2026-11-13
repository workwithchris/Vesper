package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClockStyle
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SynthwaveClockDigitsStyle

@Composable
fun SynthwaveClockWidget(
  formattedTime: String,
  amPmLabel: String,
  formattedDate: String,
  locationLabel: String,
  reducedMotion: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  clockStyle: ClockStyle = ClockStyle.NEON_HORIZON,
  batteryPercent: Int? = null,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp)
        .semantics { contentDescription = "Digital clock $formattedTime $amPmLabel, $formattedDate" }
        .testTag("status_clock"),
  ) {
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.Start,
    ) {
      when (clockStyle) {
        ClockStyle.NEON_HORIZON -> {
          Text(
            text = formattedTime,
            style =
              SynthwaveClockDigitsStyle.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
              ),
            maxLines = 1,
          )
        }

        ClockStyle.MONO_TERMINAL -> {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = ">",
              style =
                SynthwaveClockDigitsStyle.copy(
                  fontFamily = JetBrainsMonoFamily,
                  color = MaterialTheme.colorScheme.primary,
                ),
              maxLines = 1,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = formattedTime,
              style =
                SynthwaveClockDigitsStyle.copy(
                  fontFamily = JetBrainsMonoFamily,
                  color = MaterialTheme.colorScheme.onBackground,
                ),
              maxLines = 1,
            )
          }
        }

        ClockStyle.MINIMAL_EDITORIAL -> {
          Text(
            text = formattedTime,
            style =
              SynthwaveClockDigitsStyle.copy(
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
              ),
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = amPmLabel.uppercase(),
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
      )

      if (batteryPercent != null) {
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "· ${batteryPercent}%",
          style =
            MaterialTheme.typography.labelMedium.copy(
              fontFamily = JetBrainsMonoFamily,
              fontSize = 13.sp,
            ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp).testTag("clock_battery_chip"),
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = formattedDate,
      style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}
