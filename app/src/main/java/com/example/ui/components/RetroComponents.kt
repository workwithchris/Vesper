package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AdaptiveIconShape
import com.example.model.AppInfo
import com.example.ui.theme.LocalAdaptiveIconShape
import com.example.ui.theme.LocalLauncherIsDark
import kotlinx.coroutines.launch

val SquircleShape: Shape =
  GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    cubicTo(w * 0.915f, 0f, w, h * 0.085f, w, h * 0.5f)
    cubicTo(w, h * 0.915f, w * 0.915f, h, w * 0.5f, h)
    cubicTo(w * 0.085f, h, 0f, h * 0.915f, 0f, h * 0.5f)
    cubicTo(0f, h * 0.085f, w * 0.085f, 0f, w * 0.5f, 0f)
    close()
  }

fun AdaptiveIconShape.toComposeShape(): Shape =
  when (this) {
    AdaptiveIconShape.ROUNDED_SQUARE -> RoundedCornerShape(8.dp)
    AdaptiveIconShape.SQUIRCLE -> SquircleShape
    AdaptiveIconShape.CIRCLE -> CircleShape
    AdaptiveIconShape.TEARDROP ->
      RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomStartPercent = 50,
        bottomEndPercent = 14,
      )
  }

@Composable
fun FramedAppIcon(
  app: AppInfo,
  modifier: Modifier = Modifier,
  frameSize: Dp = 42.dp,
  adaptiveShape: AdaptiveIconShape = LocalAdaptiveIconShape.current,
) {
  val iconShape = remember(adaptiveShape) { adaptiveShape.toComposeShape() }
  val isDark = LocalLauncherIsDark.current
  val tileFill = MaterialTheme.colorScheme.surface
  val tileBorder = MaterialTheme.colorScheme.outline
  val iconTint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.94f else 0.90f)

  Box(
    modifier =
      modifier
        .size(frameSize)
        .clip(iconShape)
        .background(tileFill)
        .border(
          width = 1.dp,
          color = tileBorder,
          shape = iconShape,
        )
        .padding(8.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (app.icon != null) {
      Image(
        bitmap = app.icon,
        contentDescription = app.label,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(iconTint),
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Icon(
        imageVector = Icons.Outlined.Android,
        contentDescription = app.label,
        tint = iconTint,
        modifier = Modifier.size(frameSize * 0.52f),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListRow(
  app: AppInfo,
  showLabel: Boolean,
  hapticEnabled: Boolean,
  onTap: () -> Unit,
  modifier: Modifier = Modifier,
  iconSize: Dp = 42.dp,
  showDivider: Boolean = true,
  showNotificationDot: Boolean = false,
  showDailyLaunchCount: Boolean = false,
  adaptiveShape: AdaptiveIconShape = LocalAdaptiveIconShape.current,
  menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val coroutineScope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current

  val pressScale = remember { Animatable(1f) }
  val tapBounceScale = remember { Animatable(1f) }

  LaunchedEffect(isPressed) {
    if (isPressed) {
      pressScale.animateTo(
        targetValue = 0.92f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
          ),
      )
    } else {
      pressScale.animateTo(
        targetValue = 1f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
          ),
      )
    }
  }

  val iconShape = remember(adaptiveShape) { adaptiveShape.toComposeShape() }

  Box(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
              interactionSource = interactionSource,
              indication = LocalIndication.current,
              onClick = {
                if (hapticEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                coroutineScope.launch {
                  tapBounceScale.snapTo(0.91f)
                  tapBounceScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec =
                      spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                      ),
                  )
                }
                onTap()
              },
              onLongClick = {
                if (hapticEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                menuExpanded = true
              },
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("app_row_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(contentAlignment = Alignment.TopEnd) {
          FramedAppIcon(
            app = app,
            frameSize = iconSize,
            adaptiveShape = adaptiveShape,
            modifier =
              Modifier.graphicsLayer {
                val combinedScale = pressScale.value * tapBounceScale.value
                scaleX = combinedScale
                scaleY = combinedScale
              },
          )
          if (!showLabel && showNotificationDot && app.unreadCount > 0) {
            Box(
              modifier =
                Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
                  .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                  .testTag("notification_dot_${app.packageName}"),
            )
          }
        }

        if (showLabel) {
          Spacer(modifier = Modifier.width(16.dp))
          Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )

          if (showDailyLaunchCount && app.dailyLaunchCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
            ) {
              Text(
                text = "${app.dailyLaunchCount}×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("launch_count_${app.packageName}"),
              )
            }
          }

          if (showNotificationDot && app.unreadCount > 0) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
              modifier =
                Modifier
                  .size(14.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                  .testTag("notification_dot_${app.packageName}"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
              )
            }
          }
        }
      }

      if (showDivider) {
        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          thickness = 0.8.dp,
        )
      }
    }

    DropdownMenu(
      expanded = menuExpanded,
      onDismissRequest = { menuExpanded = false },
      modifier =
        Modifier
          .background(MaterialTheme.colorScheme.surface)
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(10.dp),
          ),
    ) {
      menuContent { menuExpanded = false }
    }
  }
}

@Composable
fun AlphabetScrubberRail(
  activeLetter: Char?,
  hapticEnabled: Boolean,
  onLetterSelected: (Char) -> Unit,
  onScrubEnd: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val letters = remember { ('A'..'Z').toList() }
  var railHeightPx by remember { mutableIntStateOf(1) }
  var lastReported by remember { mutableStateOf<Char?>(null) }
  val haptics = LocalHapticFeedback.current
  val accentColor = MaterialTheme.colorScheme.primary

  fun resolveLetter(offsetY: Float): Char {
    val fraction = (offsetY / railHeightPx.coerceAtLeast(1)).coerceIn(0f, 0.9999f)
    val index = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
    return letters[index]
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxHeight().width(24.dp),
  ) {
    val step =
      when {
        maxHeight < 260.dp -> 3
        maxHeight < 400.dp -> 2
        else -> 1
      }
    val visibleLetters = remember(step) { letters.filterIndexed { index, _ -> index % step == 0 } }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .onSizeChanged { railHeightPx = it.height }
          .pointerInput(hapticEnabled) {
            detectTapGestures { offset ->
              val letter = resolveLetter(offset.y)
              if (hapticEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              onLetterSelected(letter)
            }
          }
          .pointerInput(hapticEnabled) {
            detectVerticalDragGestures(
              onDragStart = { offset ->
                val letter = resolveLetter(offset.y)
                lastReported = letter
                if (hapticEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onLetterSelected(letter)
              },
              onVerticalDrag = { change, _ ->
                change.consume()
                val letter = resolveLetter(change.position.y)
                if (letter != lastReported) {
                  lastReported = letter
                  if (hapticEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  }
                  onLetterSelected(letter)
                }
              },
              onDragEnd = {
                lastReported = null
                onScrubEnd()
              },
              onDragCancel = {
                lastReported = null
                onScrubEnd()
              },
            )
          }
          .padding(vertical = 8.dp)
          .testTag("alphabet_scrubber"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(
        modifier =
          Modifier
            .width(3.dp)
            .height(14.dp)
            .clip(CircleShape)
            .background(accentColor)
      )

      visibleLetters.forEach { letter ->
        val isActive = activeLetter == letter
        Text(
          text = letter.toString(),
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          lineHeight = 11.sp,
          fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
          color =
            if (isActive) accentColor
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
      }
    }
  }
}

@Composable
fun FavoriteAppsPickerDialog(
  allApps: List<AppInfo>,
  currentFavorites: List<AppInfo>,
  onSave: (List<AppInfo>) -> Unit,
  onDismiss: () -> Unit,
) {
  var filterQuery by remember { mutableStateOf("") }
  var orderedSelectedKeys by remember(currentFavorites) {
    mutableStateOf(currentFavorites.map { it.componentKey })
  }
  val appByKey = remember(allApps) { allApps.associateBy { it.componentKey } }
  val selectedKeySet = remember(orderedSelectedKeys) { orderedSelectedKeys.toSet() }
  val selectedOrderedApps =
    remember(orderedSelectedKeys, appByKey) {
      orderedSelectedKeys.mapNotNull { appByKey[it] }
    }
  val unselectedFilteredApps =
    remember(allApps, selectedKeySet, filterQuery) {
      val trimmed = filterQuery.trim()
      allApps
        .filter { it.componentKey !in selectedKeySet }
        .filter { trimmed.isEmpty() || it.label.contains(trimmed, ignoreCase = true) }
    }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Favorite apps",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Reorder selected favorites or add more apps below.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = filterQuery,
          onValueChange = { filterQuery = it },
          singleLine = true,
          placeholder = { Text("Find an app to add") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            if (filterQuery.isNotEmpty()) {
              IconButton(onClick = { filterQuery = "" }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear filter")
              }
            }
          },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (selectedOrderedApps.isNotEmpty()) {
            item(key = "header_selected_order", contentType = "header") {
              Text(
                text = "SELECTED ORDER (${selectedOrderedApps.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp),
              )
            }
            items(
              items = selectedOrderedApps,
              key = { "selected_${it.componentKey}" },
              contentType = { "selected_app" },
            ) { app ->
              val index = orderedSelectedKeys.indexOf(app.componentKey)
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth().animateItem(),
              ) {
                Row(
                  modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  FramedAppIcon(
                    app = app,
                    frameSize = 36.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  IconButton(
                    onClick = {
                      if (index > 0) {
                        val mutable = orderedSelectedKeys.toMutableList()
                        val item = mutable.removeAt(index)
                        mutable.add(index - 1, item)
                        orderedSelectedKeys = mutable
                      }
                    },
                    enabled = index > 0,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.KeyboardArrowUp,
                      contentDescription = "Move up",
                    )
                  }
                  IconButton(
                    onClick = {
                      if (index in 0 until orderedSelectedKeys.lastIndex) {
                        val mutable = orderedSelectedKeys.toMutableList()
                        val item = mutable.removeAt(index)
                        mutable.add(index + 1, item)
                        orderedSelectedKeys = mutable
                      }
                    },
                    enabled = index in 0 until orderedSelectedKeys.lastIndex,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.KeyboardArrowDown,
                      contentDescription = "Move down",
                    )
                  }
                  IconButton(
                    onClick = {
                      orderedSelectedKeys = orderedSelectedKeys - app.componentKey
                    },
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Close,
                      contentDescription = "Remove favorite",
                    )
                  }
                }
              }
            }
          }

          if (unselectedFilteredApps.isNotEmpty()) {
            item(key = "header_add_apps", contentType = "header") {
              Text(
                text = "ADD APPS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
              )
            }
            items(
              items = unselectedFilteredApps,
              key = { "unselected_${it.componentKey}" },
              contentType = { "unselected_app" },
            ) { app ->
              Surface(
                onClick = {
                  orderedSelectedKeys = orderedSelectedKeys + app.componentKey
                },
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  FramedAppIcon(
                    app = app,
                    frameSize = 36.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Add to favorites",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              onSave(selectedOrderedApps)
            }
          ) {
            Text("Save", color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

@Composable
fun MinimalDropdownItem(
  text: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  DropdownMenuItem(
    text = {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    leadingIcon = icon,
    onClick = onClick,
  )
}
