package com.example.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
import com.example.model.DrawerQuickAction
import com.example.model.LauncherUiState
import com.example.ui.components.AlphabetScrubberRail
import com.example.ui.components.AppListRow
import com.example.ui.components.MinimalDropdownItem
import com.example.ui.components.RenameAppDialog
import com.example.util.LauncherHapticEvent
import com.example.util.rememberLauncherHaptics
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
  isOpen: Boolean,
  focusSearchOnOpen: Boolean,
  initialLetter: Char?,
  uiState: LauncherUiState,
  onClose: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onLaunchShortcut: (AppShortcutItem) -> Unit,
  onDialNumber: (String) -> Unit,
  onOpenUrl: (String) -> Unit,
  onWebSearch: (String) -> Unit,
  onOpenAppInfo: (AppInfo) -> Unit,
  onToggleFavorite: (AppInfo) -> Unit,
  onRenameApp: (AppInfo, String?) -> Unit,
  onHideApp: (AppInfo) -> Unit,
  onToggleMindfulPause: (AppInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  val launcherHaptics = rememberLauncherHaptics(uiState.settings.hapticFeedbackEnabled)

  BackHandler(enabled = isOpen) {
    launcherHaptics.trigger(LauncherHapticEvent.DRAWER_CLOSE_SWIPE)
    if (uiState.searchQuery.isNotEmpty()) {
      onSearchQueryChange("")
    } else {
      onClose()
    }
  }

  var renameTarget by remember { mutableStateOf<AppInfo?>(null) }

  AnimatedVisibility(
    visible = isOpen,
    enter =
      fadeIn(animationSpec = tween(220)) +
        scaleIn(
          initialScale = 0.94f,
          animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
        ),
    exit =
      fadeOut(animationSpec = tween(180)) +
        scaleOut(targetScale = 0.94f, animationSpec = tween(180)),
    modifier = modifier.fillMaxSize(),
  ) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    var scrubbingLetter by remember { mutableStateOf<Char?>(null) }
    var copiedMathAnswer by remember(uiState.quickMathResult) { mutableStateOf(false) }

    val sectionIndexMap =
      remember(uiState.drawerSections) {
        val map = mutableMapOf<Char, Int>()
        var runningIndex = 0
        uiState.drawerSections.forEach { section ->
          map[section.letter] = runningIndex
          runningIndex += 1 + section.apps.size
        }
        map
      }

    val visibleTopLetter by
      remember(uiState.drawerSections) {
        derivedStateOf {
          val firstIdx = listState.firstVisibleItemIndex
          var runningIndex = 0
          var currentLetter: Char? = uiState.drawerSections.firstOrNull()?.letter
          for (section in uiState.drawerSections) {
            val sectionEnd = runningIndex + 1 + section.apps.size
            if (firstIdx < sectionEnd) {
              currentLetter = section.letter
              break
            }
            runningIndex = sectionEnd
          }
          currentLetter
        }
      }

    fun jumpToLetter(letter: Char) {
      val exactIndex = sectionIndexMap[letter]
      if (exactIndex != null) {
        coroutineScope.launch { listState.scrollToItem(exactIndex) }
        return
      }
      val available = sectionIndexMap.keys.toList()
      if (available.isNotEmpty()) {
        val nearest = available.firstOrNull { it >= letter } ?: available.last()
        val targetIndex = sectionIndexMap[nearest] ?: 0
        coroutineScope.launch { listState.scrollToItem(targetIndex) }
      }
    }

    LaunchedEffect(isOpen, focusSearchOnOpen) {
      if (isOpen && focusSearchOnOpen) {
        runCatching { searchFocusRequester.requestFocus() }
      }
    }

    LaunchedEffect(isOpen, initialLetter, sectionIndexMap) {
      if (isOpen && initialLetter != null && sectionIndexMap.isNotEmpty()) {
        jumpToLetter(initialLetter)
      }
    }

    val favoriteKeys =
      remember(uiState.favoriteApps) { uiState.favoriteApps.map { it.componentKey }.toSet() }
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .statusBarsPadding()
          .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
      ) {
        // Header ("All apps") with swipe-down to close
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .pointerInput(onClose, launcherHaptics) {
                var dragY = 0f
                detectVerticalDragGestures(
                  onDragStart = { dragY = 0f },
                  onVerticalDrag = { _, delta ->
                    dragY += delta
                    if (dragY > 60f) {
                      dragY = 0f
                      launcherHaptics.trigger(LauncherHapticEvent.DRAWER_CLOSE_SWIPE)
                      onClose()
                    }
                  },
                )
              }
              .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.all_apps_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.DRAWER_CLOSE_SWIPE)
              onClose()
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_drawer_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.KeyboardArrowDown,
              contentDescription = "Back to home",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // "Find an app" search pill with Enter-to-launch top result or web search
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchQueryChange,
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions =
            KeyboardActions(
              onGo = {
                val topApp = uiState.filteredDrawerApps.firstOrNull()
                val quickAction = uiState.drawerQuickAction
                when {
                  topApp != null -> {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onLaunchApp(topApp)
                  }
                  quickAction is DrawerQuickAction.DialPhone -> {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onDialNumber(quickAction.displayNumber)
                  }
                  quickAction is DrawerQuickAction.OpenUrl -> {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onOpenUrl(quickAction.url)
                  }
                  uiState.quickMathResult != null -> {
                    launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
                    clipboard.setText(AnnotatedString(uiState.quickMathResult))
                    copiedMathAnswer = true
                  }
                  uiState.searchQuery.isNotBlank() -> {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onWebSearch(uiState.searchQuery)
                  }
                }
              }
            ),
          placeholder = {
            Text(
              text = stringResource(R.string.find_app_hint),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(
                onClick = {
                  launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                  onSearchQueryChange("")
                }
              ) {
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Clear search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedTextColor = MaterialTheme.colorScheme.onBackground,
              unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
              cursorColor = accentColor,
            ),
          shape = RoundedCornerShape(10.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp)
              .focusRequester(searchFocusRequester)
              .testTag("drawer_search_input"),
        )

        // Inline Quick Math card when typing arithmetic expressions
        val mathAnswer = uiState.quickMathResult
        if (mathAnswer != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
              clipboard.setText(AnnotatedString(mathAnswer))
              copiedMathAnswer = true
            },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("quick_math_result_card"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Calculate,
                contentDescription = "Calculation result",
                tint = accentColor,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "= $mathAnswer",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = com.example.ui.theme.JetBrainsMonoFamily,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
              )
              Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy result",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (copiedMathAnswer) "Copied" else "Copy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Inline Quick Action card (Call number / Open URL)
        val quickAction = uiState.drawerQuickAction
        if (quickAction != null) {
          Spacer(modifier = Modifier.height(10.dp))
          QuickActionCard(
            quickAction = quickAction,
            accentColor = accentColor,
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
              when (quickAction) {
                is DrawerQuickAction.DialPhone -> onDialNumber(quickAction.displayNumber)
                is DrawerQuickAction.OpenUrl -> onOpenUrl(quickAction.url)
              }
            },
          )
        }

        // Inline App Shortcuts matched while searching
        if (uiState.matchedShortcuts.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
          ) {
            uiState.matchedShortcuts.forEach { shortcut ->
              Surface(
                onClick = {
                  launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                  onLaunchShortcut(shortcut)
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("shortcut_result_${shortcut.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = "App shortcut",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "${shortcut.appLabel} · ${shortcut.shortLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          if (uiState.drawerSections.isEmpty()) {
            Column(
              modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Spacer(modifier = Modifier.height(48.dp))
              Text(
                text = stringResource(R.string.no_apps_found),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (uiState.searchQuery.isNotBlank() && quickAction == null && mathAnswer == null) {
                Spacer(modifier = Modifier.height(16.dp))
                WebSearchActionCard(
                  query = uiState.searchQuery.trim(),
                  accentColor = accentColor,
                  onClick = {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onWebSearch(uiState.searchQuery)
                  },
                )
              }
            }
          } else {
            Row(modifier = Modifier.fillMaxSize()) {
              val defaultFling = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()
              val scrollMultiplier = uiState.settings.scrollSpeed.scrollMultiplier
              val scaledFling =
                remember(defaultFling, scrollMultiplier) {
                  object : androidx.compose.foundation.gestures.FlingBehavior {
                    override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(
                      initialVelocity: Float
                    ): Float {
                      return with(defaultFling) {
                        performFling(initialVelocity * scrollMultiplier)
                      }
                    }
                  }
                }
              LazyColumn(
                state = listState,
                flingBehavior = scaledFling,
                contentPadding =
                  PaddingValues(
                    start = 24.dp,
                    end = if (uiState.settings.alphabetIndexEnabled) 8.dp else 24.dp,
                    top = 6.dp,
                    bottom = 24.dp,
                  ),
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("drawer_app_list"),
              ) {
                uiState.drawerSections.forEach { section ->
                  item(
                    key = "header_${section.letter}",
                    contentType = "section_header",
                  ) {
                    Text(
                      text = section.letter.toString(),
                      style =
                        MaterialTheme.typography.titleMedium.copy(
                          fontSize = 15.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = accentColor,
                        ),
                      modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
                    )
                  }
                  itemsIndexed(
                    items = section.apps,
                    key = { _, app -> "drawer_${app.componentKey}" },
                    contentType = { _, _ -> "drawer_app" },
                  ) { index, app ->
                    val isFav = app.componentKey in favoriteKeys
                    AppListRow(
                      app = app,
                      showLabel = true,
                      hapticEnabled = uiState.settings.hapticFeedbackEnabled,
                      showDailyLaunchCount = uiState.settings.showDailyLaunchCounts,
                      onTap = { onLaunchApp(app) },
                      showDivider = index == section.apps.lastIndex,
                    ) { dismiss ->
                      MinimalDropdownItem(
                        text = stringResource(R.string.app_info),
                        icon = {
                          Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = accentColor,
                          )
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                          dismiss()
                          onOpenAppInfo(app)
                        },
                      )
                      MinimalDropdownItem(
                        text =
                          stringResource(
                            if (isFav) R.string.remove_from_favorites
                            else R.string.add_to_favorites
                          ),
                        icon = {
                          Icon(
                            if (isFav) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = accentColor,
                          )
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
                          dismiss()
                          onToggleFavorite(app)
                        },
                      )
                      MinimalDropdownItem(
                        text = stringResource(R.string.rename_app),
                        icon = {
                          Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = accentColor,
                          )
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                          dismiss()
                          renameTarget = app
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
                            tint = accentColor,
                          )
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
                          dismiss()
                          onToggleMindfulPause(app)
                        },
                      )
                      MinimalDropdownItem(
                        text = stringResource(R.string.hide_app),
                        icon = {
                          Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                          dismiss()
                          onHideApp(app)
                        },
                      )
                    }
                  }
                }

                if (uiState.searchQuery.isNotBlank() && mathAnswer == null) {
                  item(key = "web_search_footer") {
                    Spacer(modifier = Modifier.height(12.dp))
                    WebSearchActionCard(
                      query = uiState.searchQuery.trim(),
                      accentColor = accentColor,
                      onClick = {
                        launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                        onWebSearch(uiState.searchQuery)
                      },
                    )
                  }
                }
              }

              if (uiState.settings.alphabetIndexEnabled) {
                AlphabetScrubberRail(
                  activeLetter = scrubbingLetter ?: visibleTopLetter,
                  hapticEnabled = uiState.settings.hapticFeedbackEnabled,
                  onLetterSelected = { letter ->
                    scrubbingLetter = letter
                    jumpToLetter(letter)
                  },
                  onScrubEnd = { scrubbingLetter = null },
                  modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, end = 6.dp),
                )
              }
            }
          }

          val activeBubble = scrubbingLetter
          if (activeBubble != null) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = accentColor,
              shadowElevation = 8.dp,
              modifier = Modifier.align(Alignment.CenterEnd).padding(end = 44.dp),
            ) {
              Text(
                text = activeBubble.toString(),
                style =
                  MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                  ),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
              )
            }
          }
        }
      }
    }
  }

  val targetToRename = renameTarget
  if (targetToRename != null) {
    RenameAppDialog(
      app = targetToRename,
      onSave = { customName ->
        onRenameApp(targetToRename, customName)
        renameTarget = null
      },
      onDismiss = { renameTarget = null },
    )
  }
}

@Composable
private fun WebSearchActionCard(
  query: String,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth().testTag("web_search_action_card"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Public,
        contentDescription = "Search web",
        tint = accentColor,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = "Search web for \"$query\"",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun QuickActionCard(
  quickAction: DrawerQuickAction,
  accentColor: Color,
  onClick: () -> Unit,
) {
  val icon =
    when (quickAction) {
      is DrawerQuickAction.DialPhone -> Icons.Outlined.Call
      is DrawerQuickAction.OpenUrl -> Icons.Outlined.OpenInBrowser
    }
  val label =
    when (quickAction) {
      is DrawerQuickAction.DialPhone -> "Call ${quickAction.displayNumber}"
      is DrawerQuickAction.OpenUrl -> "Open ${quickAction.url}"
    }
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f)),
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .testTag("drawer_quick_action_card"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accentColor,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
