package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
import com.example.model.ResolvedFolder
import com.example.ui.components.AppListRow
import com.example.ui.components.MinimalDropdownItem
import com.example.ui.components.toComposeShape
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GeistMonoNumericStyle
import com.example.ui.theme.LocalAdaptiveIconShape
import com.example.ui.theme.LocalLauncherIsDark
import com.example.util.LauncherHapticEvent
import com.example.util.SmartFolderCategorizer
import com.example.util.rememberLauncherHaptics
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
  val launcherHaptics = rememberLauncherHaptics(hapticEnabled)

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
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
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
          launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
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
            launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
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
  showDailyLaunchCount: Boolean,
  onToggleExpand: () -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppInfo: (AppInfo) -> Unit,
  onRenameApp: (AppInfo) -> Unit,
  onToggleMindfulPause: (AppInfo) -> Unit,
  onEditFolder: () -> Unit,
  onDeleteFolder: () -> Unit,
  shortcutsByPackage: Map<String, List<AppShortcutItem>> = emptyMap(),
  onLaunchShortcut: (AppShortcutItem) -> Unit = {},
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val launcherHaptics = rememberLauncherHaptics(hapticEnabled)
  val iconShape = LocalAdaptiveIconShape.current.toComposeShape()
  val isDark = LocalLauncherIsDark.current
  val accentColor = MaterialTheme.colorScheme.primary
  val iconTint =
    if (expanded) accentColor
    else MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.92f else 0.88f)

  val chevronRotation by
    animateFloatAsState(
      targetValue = if (expanded) 90f else 0f,
      animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
      label = "folder_chevron_rotation",
    )

  val totalFolderLaunches = remember(resolvedFolder.apps) {
    resolvedFolder.apps.sumOf { it.dailyLaunchCount }
  }
  val isSmartAutoFolder = remember(resolvedFolder.folder.id) {
    SmartFolderCategorizer.isSmartFolder(resolvedFolder.folder)
  }

  val baseSurface = MaterialTheme.colorScheme.surface
  val baseOutline = MaterialTheme.colorScheme.outline

  val glassSurfaceBrush =
    remember(isDark, accentColor, baseSurface) {
      Brush.verticalGradient(
        colors =
          if (isDark) {
            listOf(
              Color.White.copy(alpha = 0.08f),
              baseSurface.copy(alpha = 0.64f),
              accentColor.copy(alpha = 0.07f),
            )
          } else {
            listOf(
              Color.White.copy(alpha = 0.82f),
              baseSurface.copy(alpha = 0.72f),
              accentColor.copy(alpha = 0.06f),
            )
          }
      )
    }

  val glassBorderBrush =
    remember(isDark, accentColor, baseOutline) {
      Brush.linearGradient(
        colors =
          if (isDark) {
            listOf(
              Color.White.copy(alpha = 0.24f),
              accentColor.copy(alpha = 0.32f),
              baseOutline.copy(alpha = 0.45f),
            )
          } else {
            listOf(
              Color.White.copy(alpha = 0.85f),
              accentColor.copy(alpha = 0.28f),
              baseOutline.copy(alpha = 0.55f),
            )
          }
      )
    }

  Box(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
                onToggleExpand()
              },
              onLongClick = {
                launcherHaptics.trigger(LauncherHapticEvent.LONG_PRESS)
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
              .clip(iconShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (expanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
            contentDescription = resolvedFolder.folder.name,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
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
          if (isSmartAutoFolder && totalFolderLaunches > 0) {
            Text(
              text = "${totalFolderLaunches}× · ",
              style = GeistMonoNumericStyle,
              color = accentColor.copy(alpha = 0.85f),
            )
          }
          Text(
            text = "${resolvedFolder.apps.size}",
            style = GeistMonoNumericStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse folder" else "Expand folder",
            tint = if (expanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
              Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = chevronRotation },
          )
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter =
          expandVertically(
            animationSpec =
              spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow,
              ),
            expandFrom = Alignment.Top,
          ) +
            fadeIn(animationSpec = tween(durationMillis = 220)) +
            scaleIn(
              initialScale = 0.96f,
              transformOrigin = TransformOrigin(0.5f, 0f),
              animationSpec = tween(durationMillis = 220),
            ),
        exit =
          shrinkVertically(
            animationSpec = tween(durationMillis = 180),
            shrinkTowards = Alignment.Top,
          ) +
            fadeOut(animationSpec = tween(durationMillis = 150)) +
            scaleOut(
              targetScale = 0.96f,
              transformOrigin = TransformOrigin(0.5f, 0f),
              animationSpec = tween(durationMillis = 180),
            ),
      ) {
        val glassShape = RoundedCornerShape(14.dp)
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 10.dp)
              .clip(glassShape)
              .background(brush = glassSurfaceBrush, shape = glassShape)
              .border(width = 1.dp, brush = glassBorderBrush, shape = glassShape)
              .padding(horizontal = 12.dp, vertical = 4.dp)
              .testTag("folder_expanded_${resolvedFolder.folder.name}"),
        ) {
          resolvedFolder.apps.forEachIndexed { idx, app ->
            AppListRow(
              app = app,
              showLabel = showLabel,
              hapticEnabled = hapticEnabled,
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
                  launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
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
                  launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                  dismiss()
                  onRenameApp(app)
                },
              )
              shortcutsByPackage[app.packageName].orEmpty().take(3).forEach { shortcut ->
                MinimalDropdownItem(
                  text = shortcut.shortLabel,
                  icon = {
                    Icon(
                      Icons.Outlined.Bolt,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  },
                  onClick = {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    dismiss()
                    onLaunchShortcut(shortcut)
                  },
                )
              }
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
                  launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
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
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
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
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          menuExpanded = false
          onDeleteFolder()
        },
      )
    }
  }
}

@Composable
fun DailyIntentionPill(
  intention: String,
  hapticEnabled: Boolean = true,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val launcherHaptics = rememberLauncherHaptics(hapticEnabled)
  val hasIntention = intention.isNotBlank()
  Surface(
    onClick = {
      launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
      onClick()
    },
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = if (hasIntention) 0.90f else 0.72f),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (hasIntention) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.outline,
      ),
    modifier = modifier.fillMaxWidth().testTag("daily_intention_pill"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.TaskAlt,
        contentDescription = "Today's focus intention",
        tint =
          if (hasIntention) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (hasIntention) intention else "Set today's intention...",
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (hasIntention) MaterialTheme.colorScheme.onBackground
          else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun DailyIntentionDialog(
  currentIntention: String,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var text by remember(currentIntention) { mutableStateOf(currentIntention) }
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(12.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = { Text("Today's intention") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Keep one quiet daily priority anchored at the top of your home screen.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = text,
          onValueChange = { if (it.length <= 80) text = it },
          singleLine = true,
          placeholder = { Text("e.g. Ship v1.1 release cleanly") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.fillMaxWidth().testTag("daily_intention_input"),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(text.trim()) },
        modifier = Modifier.testTag("daily_intention_save_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      Row {
        if (currentIntention.isNotBlank()) {
          TextButton(
            onClick = { onSave("") },
            modifier = Modifier.testTag("daily_intention_clear_button"),
          ) {
            Text("Clear", color = DangerRed)
          }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
    modifier = Modifier.testTag("daily_intention_dialog"),
  )
}

@Composable
fun GeistWidgetStack(
  slots: List<@Composable () -> Unit>,
  hapticEnabled: Boolean = true,
  reducedMotion: Boolean = false,
  modifier: Modifier = Modifier,
) {
  if (slots.isEmpty()) return
  val launcherHaptics = rememberLauncherHaptics(hapticEnabled)
  var activeIndex by rememberSaveable { mutableIntStateOf(0) }
  var swipeDirection by remember { mutableIntStateOf(1) }
  val safeIndex = activeIndex.coerceIn(0, slots.lastIndex)

  fun stepWidget(delta: Int) {
    if (slots.size <= 1) return
    swipeDirection = if (delta >= 0) 1 else -1
    activeIndex = (safeIndex + delta + slots.size) % slots.size
    launcherHaptics.trigger(LauncherHapticEvent.SCRUBBER_TICK)
  }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .pointerInput(slots.size, safeIndex, launcherHaptics) {
          var dragY = 0f
          var switchedInGesture = false
          detectVerticalDragGestures(
            onDragStart = {
              dragY = 0f
              switchedInGesture = false
            },
            onVerticalDrag = { change, delta ->
              change.consume()
              if (switchedInGesture || slots.size <= 1) return@detectVerticalDragGestures
              dragY += delta
              if (dragY < -28f) {
                switchedInGesture = true
                stepWidget(+1)
              } else if (dragY > 28f) {
                switchedInGesture = true
                stepWidget(-1)
              }
            },
          )
        }
        .testTag("home_widget_stack"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(modifier = Modifier.weight(1f)) {
      if (reducedMotion) {
        slots[safeIndex]()
      } else {
        AnimatedContent(
          targetState = safeIndex,
          transitionSpec = {
            if (swipeDirection >= 0) {
              (slideInVertically(tween(180)) { height -> height } + fadeIn(tween(180)))
                .togetherWith(slideOutVertically(tween(180)) { height -> -height } + fadeOut(tween(150)))
            } else {
              (slideInVertically(tween(180)) { height -> -height } + fadeIn(tween(180)))
                .togetherWith(slideOutVertically(tween(180)) { height -> height } + fadeOut(tween(150)))
            }
          },
          label = "geist_widget_stack",
        ) { targetIdx ->
          slots[targetIdx.coerceIn(0, slots.lastIndex)]()
        }
      }
    }

    if (slots.size > 1) {
      Spacer(modifier = Modifier.width(10.dp))
      Column(
        modifier =
          Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { stepWidget(+1) }
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("widget_stack_indicator"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        for (idx in slots.indices) {
          val selected = idx == safeIndex
          Box(
            modifier =
              Modifier
                .width(3.dp)
                .height(if (selected) 12.dp else 4.dp)
                .clip(CircleShape)
                .background(
                  if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant
                )
                .testTag("widget_stack_dot_$idx")
          )
        }
      }
    }
  }
}

@Composable
fun QuickScratchpadPill(
  scratchpadText: String,
  hapticEnabled: Boolean = true,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val launcherHaptics = rememberLauncherHaptics(hapticEnabled)
  val hasNote = scratchpadText.isNotBlank()
  val previewLine =
    remember(scratchpadText) {
      scratchpadText.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }
  Surface(
    onClick = {
      launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
      onClick()
    },
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = if (hasNote) 0.90f else 0.72f),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (hasNote) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.outline,
      ),
    modifier = modifier.fillMaxWidth().testTag("quick_scratchpad_pill"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Edit,
        contentDescription = "Quick scratchpad",
        tint =
          if (hasNote) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (hasNote) previewLine else "Quick scratchpad note...",
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (hasNote) MaterialTheme.colorScheme.onBackground
          else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun QuickScratchpadDialog(
  currentText: String,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var text by remember(currentText) { mutableStateOf(currentText) }
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(12.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = { Text("Quick scratchpad") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Jot down quick notes, thoughts, or snippets right on your home widget stack.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = text,
          onValueChange = { if (it.length <= 400) text = it },
          minLines = 3,
          maxLines = 6,
          placeholder = { Text("Type a quick note or checklist item...") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.fillMaxWidth().testTag("quick_scratchpad_input"),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(text.trim()) },
        modifier = Modifier.testTag("quick_scratchpad_save_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      Row {
        if (currentText.isNotBlank()) {
          TextButton(
            onClick = { onSave("") },
            modifier = Modifier.testTag("quick_scratchpad_clear_button"),
          ) {
            Text("Clear", color = DangerRed)
          }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
    modifier = Modifier.testTag("quick_scratchpad_dialog"),
  )
}



