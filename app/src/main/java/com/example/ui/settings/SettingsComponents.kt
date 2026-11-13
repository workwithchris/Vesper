package com.example.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationBadgeStore
import com.example.util.LauncherHapticEvent
import com.example.util.rememberLauncherHaptics

@Composable
fun LauncherHealthStatusCard(
  context: Context,
  isDefaultHomeLauncher: Boolean,
  onRequestDefaultLauncher: () -> Unit,
  onOpenNotificationAccess: () -> Unit,
  onRequestCalendarPermission: () -> Unit,
) {
  val notificationAccessGranted = NotificationBadgeStore.isListenerEnabled(context)
  val calendarGranted =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
      PackageManager.PERMISSION_GRANTED

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth().testTag("launcher_health_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "LAUNCHER STATUS",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      StatusCheckRow(
        title = "Default Home Launcher",
        isActive = isDefaultHomeLauncher,
        actionLabel = if (isDefaultHomeLauncher) "Active" else "Set default",
        onClick = onRequestDefaultLauncher,
      )
      StatusCheckRow(
        title = "Notification Dots Access",
        isActive = notificationAccessGranted,
        actionLabel = if (notificationAccessGranted) "Granted" else "Grant",
        onClick = onOpenNotificationAccess,
      )
      StatusCheckRow(
        title = "Calendar Events Permission",
        isActive = calendarGranted,
        actionLabel = if (calendarGranted) "Granted" else "Grant",
        onClick = onRequestCalendarPermission,
      )
    }
  }
}

@Composable
private fun StatusCheckRow(
  title: String,
  isActive: Boolean,
  actionLabel: String,
  onClick: () -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onClick()
        }
        .padding(vertical = 4.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector =
        if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
      contentDescription = null,
      tint =
        if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = actionLabel,
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun <T> RadioOptionDialog(
  title: String,
  options: List<T>,
  selected: T,
  labelFor: (T) -> String,
  onSelect: (T) -> Unit,
  onDismiss: () -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        options.forEach { option ->
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                  onSelect(option)
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selected == option,
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                onSelect(option)
              },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = labelFor(option), style = MaterialTheme.typography.bodyLarge)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}

@Composable
fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
  )
}

@Composable
fun SectionDivider() {
  Spacer(modifier = Modifier.height(6.dp))
  HorizontalDivider(
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
    thickness = 0.8.dp,
  )
  Spacer(modifier = Modifier.height(14.dp))
}

@Composable
fun SettingsValueRow(
  icon: ImageVector,
  title: String,
  value: String?,
  onClick: () -> Unit,
  testTag: String,
) {
  val launcherHaptics = rememberLauncherHaptics()
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onClick()
        }
        .padding(horizontal = 4.dp, vertical = 14.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.weight(1f),
    )
    if (value != null) {
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.width(6.dp))
    }
    Icon(
      imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
fun SettingsToggleRow(
  icon: ImageVector,
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  accentColor: Color,
  testTag: String,
  forceWhenToggledOn: Boolean = false,
) {
  val launcherHaptics = rememberLauncherHaptics()
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable {
          val nextValue = !checked
          launcherHaptics.trigger(
            LauncherHapticEvent.TOGGLE_SWITCH,
            forceWhenDisabled = forceWhenToggledOn && nextValue,
          )
          onCheckedChange(nextValue)
        }
        .padding(horizontal = 4.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.weight(1f),
    )
    Switch(
      checked = checked,
      onCheckedChange = { nextValue ->
        launcherHaptics.trigger(
          LauncherHapticEvent.TOGGLE_SWITCH,
          forceWhenDisabled = forceWhenToggledOn && nextValue,
        )
        onCheckedChange(nextValue)
      },
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = accentColor,
          uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
      modifier = Modifier.testTag(testTag),
    )
  }
}
