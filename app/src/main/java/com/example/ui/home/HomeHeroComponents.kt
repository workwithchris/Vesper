package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.ResolvedFolder
import com.example.ui.components.AppListRow
import com.example.ui.components.MinimalDropdownItem
import com.example.ui.components.toComposeShape
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GeistMonoNumericStyle
import com.example.ui.theme.LocalAdaptiveIconShape
import com.example.ui.theme.LocalLauncherIsDark
import kotlinx.coroutines.delay

@Composable
fun HeroFocusTimerBar(
  isFocusModeActive: Boolean,
  focusTimerEndMillis: Long,
  defaultDurationMinutes: Int,
  hapticEnabled: Boolean = true,
  onStartTimer: (Int) -> Unit,
  onStopTimer: () -> Unit,
  onToggleFocusMode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val isTimerRunning = focusTimerEndMillis > nowMillis
  val haptics = LocalHapticFeedback.current

  // Zero-allocation idle: only ticks once per second while an active focus countdown is visible
  LaunchedEffect(focusTimerEndMillis) {
    nowMillis = System.currentTimeMillis()
    while (focusTimerEndMillis > nowMillis) {
      delay(1000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  val accent = MaterialTheme.colorScheme.primary
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (isTimerRunning) {
      val remainingSec = ((focusTimerEndMillis - nowMillis) / 1000L).coerceAtLeast(0L)
      val mins = remainingSec / 60L
      val secs = remainingSec % 60L
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = Modifier.testTag("hero_focus_timer_running"),
      ) {
        Row(
          modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = "Focus timer running",
            tint = accent,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "FOCUS · %02d:%02d".format(mins, secs),
            style = GeistMonoNumericStyle.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
          )
          IconButton(
            onClick = {
              if (hapticEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              onStopTimer()
            },
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Stop focus timer",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    } else {
      Surface(
        onClick = {
          if (hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
          onStartTimer(defaultDurationMinutes)
        },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.testTag("hero_focus_timer_start"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = "Start focus timer",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${defaultDurationMinutes}M FOCUS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (isFocusModeActive) {
        Surface(
          onClick = {
            if (hapticEnabled) {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onToggleFocusMode()
          },
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
          modifier = Modifier.testTag("hero_focus_active_chip"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.SelfImprovement,
              contentDescription = "Focus mode active",
              tint = accent,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "FOCUS ACTIVE",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeFolderRow(
  resolvedFolder: ResolvedFolder,
  expanded: Boolean,
  showLabel: Boolean,
  hapticEnabled: Boolean,
  showNotificationDot: Boolean,
  showDailyLaunchCount: Boolean,
  onToggleExpand: () -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppInfo: (AppInfo) -> Unit,
  onRenameApp: (AppInfo) -> Unit,
  onToggleMindfulPause: (AppInfo) -> Unit,
  onEditFolder: () -> Unit,
  onDeleteFolder: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val haptics = LocalHapticFeedback.current
  val iconShape = LocalAdaptiveIconShape.current.toComposeShape()
  val isDark = LocalLauncherIsDark.current
  val iconTint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.92f else 0.88f)
  val unreadSum = if (showNotificationDot) resolvedFolder.apps.sumOf { it.unreadCount } else 0

  Box(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
              onClick = {
                if (hapticEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onToggleExpand()
              },
              onLongClick = {
                if (hapticEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                menuExpanded = true
              },
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("home_folder_${resolvedFolder.folder.name}"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(42.dp)
              .clip(iconShape)
              .background(MaterialTheme.colorScheme.surface)
              .border(1.dp, MaterialTheme.colorScheme.outline, iconShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (expanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
            contentDescription = resolvedFolder.folder.name,
            tint = iconTint,
            modifier = Modifier.size(21.dp),
          )
        }

        if (showLabel) {
          Spacer(modifier = Modifier.width(16.dp))
          Text(
            text = resolvedFolder.folder.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (unreadSum > 0) {
            Box(
              modifier =
                Modifier
                  .size(7.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text = "${resolvedFolder.apps.size}",
            style = GeistMonoNumericStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector =
              if (expanded) Icons.Outlined.KeyboardArrowDown
              else Icons.Outlined.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse folder" else "Expand folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      if (expanded) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(start = 18.dp, bottom = 6.dp)
              .testTag("folder_expanded_${resolvedFolder.folder.name}"),
        ) {
          resolvedFolder.apps.forEachIndexed { idx, app ->
            AppListRow(
              app = app,
              showLabel = showLabel,
              hapticEnabled = hapticEnabled,
              showNotificationDot = showNotificationDot,
              showDailyLaunchCount = showDailyLaunchCount,
              iconSize = 36.dp,
              onTap = { onLaunchApp(app) },
              showDivider = idx < resolvedFolder.apps.lastIndex,
            ) { dismiss ->
              MinimalDropdownItem(
                text = stringResource(R.string.app_info),
                icon = {
                  Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  dismiss()
                  onOpenAppInfo(app)
                },
              )
              MinimalDropdownItem(
                text = stringResource(R.string.rename_app),
                icon = {
                  Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  dismiss()
                  onRenameApp(app)
                },
              )
              MinimalDropdownItem(
                text =
                  stringResource(
                    if (app.isMindfulPaused) R.string.mindful_pause_remove
                    else R.string.mindful_pause_add
                  ),
                icon = {
                  Icon(
                    Icons.Outlined.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  dismiss()
                  onToggleMindfulPause(app)
                },
              )
            }
          }
        }
      }

      HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        thickness = 0.8.dp,
      )
    }

    DropdownMenu(
      expanded = menuExpanded,
      onDismissRequest = { menuExpanded = false },
      modifier =
        Modifier
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
    ) {
      MinimalDropdownItem(
        text = "Edit folder",
        icon = {
          Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        onClick = {
          menuExpanded = false
          onEditFolder()
        },
      )
      MinimalDropdownItem(
        text = "Delete folder",
        icon = {
          Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = DangerRed)
        },
        onClick = {
          menuExpanded = false
          onDeleteFolder()
        },
      )
    }
  }
}
