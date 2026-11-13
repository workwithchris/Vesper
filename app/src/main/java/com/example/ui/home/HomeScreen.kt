package com.example.ui.home

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.CalendarEventPreview
import com.example.model.HomeGestureAction
import com.example.model.LauncherUiState
import com.example.model.WeatherEffectType
import com.example.model.WeatherLocation
import com.example.ui.components.AlphabetScrubberRail
import com.example.ui.components.AppListRow
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.MinimalDropdownItem
import com.example.ui.components.RenameAppDialog
import com.example.ui.components.WeatherCityPickerDialog
import com.example.ui.theme.CoastalDayWash
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WeatherHeatCoral
import com.example.ui.theme.WeatherHeatGold
import com.example.ui.theme.WeatherRainCyan
import com.example.ui.theme.WeatherStormViolet
import com.example.ui.theme.WeatherSunGold
import com.example.util.LauncherHapticEvent
import com.example.util.rememberLauncherHaptics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
  uiState: LauncherUiState,
  onOpenDrawer: (focusSearch: Boolean, jumpToLetter: Char?) -> Unit,
  onOpenSettings: () -> Unit,
  onClockClick: () -> Unit,
  onRefreshWeather: () -> Unit,
  onSearchWeatherCities: suspend (String) -> List<WeatherLocation> = { emptyList() },
  onSelectWeatherLocation: (WeatherLocation) -> Unit = {},
  onOpenCalendar: () -> Unit,
  onExpandNotificationShade: () -> Unit,
  onTriggerGestureAction: (HomeGestureAction) -> Unit,
  onStartFocusTimer: (Int) -> Unit,
  onStopFocusTimer: () -> Unit,
  onToggleFocusMode: () -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppInfo: (AppInfo) -> Unit,
  onRemoveFromFavorites: (AppInfo) -> Unit,
  onRenameApp: (AppInfo, String?) -> Unit,
  onToggleMindfulPause: (AppInfo) -> Unit,
  onSaveFolder: (existingId: String?, name: String, apps: List<AppInfo>) -> Unit,
  onDeleteFolder: (folderId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var renameTarget by remember { mutableStateOf<AppInfo?>(null) }
  var editingFolder by remember { mutableStateOf<AppFolder?>(null) }
  var expandedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var showCityPicker by remember { mutableStateOf(false) }
  var systemLowPowerMode by remember { mutableStateOf(isSystemLowPowerOrReducedMotion(context)) }
  val hapticsEnabled = uiState.settings.hapticFeedbackEnabled
  val launcherHaptics = rememberLauncherHaptics(hapticsEnabled)

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        currentTimeMillis = System.currentTimeMillis()
        systemLowPowerMode = isSystemLowPowerOrReducedMotion(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    while (true) {
      currentTimeMillis = System.currentTimeMillis()
      val millisIntoMinute = currentTimeMillis % 60_000L
      delay(60_000L - millisIntoMinute + 25L)
    }
  }

  val effectiveReducedMotion = uiState.settings.reducedMotionEnabled || systemLowPowerMode
  val timeFormatter = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
  val amPmFormatter = remember { SimpleDateFormat("a", Locale.getDefault()) }
  val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
  val nowDate = remember(currentTimeMillis / 60_000L) { Date(currentTimeMillis) }
  val formattedTime = timeFormatter.format(nowDate)
  val amPmLabel = amPmFormatter.format(nowDate)
  val formattedDate = dateFormatter.format(nowDate)
  val hourOfDay =
    remember(currentTimeMillis / 60_000L) {
      val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
      cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
    }
  val coastalPalette = remember(hourOfDay) { resolveMiamiCoastalPalette(hourOfDay) }

  val listState = rememberLazyListState()
  val drawerSwipeConnection =
    remember(listState, onOpenDrawer, onExpandNotificationShade, launcherHaptics) {
      object : NestedScrollConnection {
        private var upwardOverscroll = 0f
        private var downwardOverscroll = 0f

        override fun onPostScroll(
          consumed: Offset,
          available: Offset,
          source: NestedScrollSource,
        ): Offset {
          if (source == NestedScrollSource.UserInput) {
            if (available.y < 0f && !listState.canScrollForward) {
              downwardOverscroll = 0f
              upwardOverscroll += -available.y
              if (upwardOverscroll > 64f) {
                upwardOverscroll = 0f
                launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                onOpenDrawer(false, null)
              }
            } else if (available.y > 0f && !listState.canScrollBackward) {
              upwardOverscroll = 0f
              downwardOverscroll += available.y
              if (downwardOverscroll > 72f) {
                downwardOverscroll = 0f
                launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                onExpandNotificationShade()
              }
            } else {
              upwardOverscroll = 0f
              downwardOverscroll = 0f
            }
          }
          return Offset.Zero
        }
      }
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .nestedScroll(drawerSwipeConnection)
        .pointerInput(uiState.settings.doubleTapAction, launcherHaptics, onTriggerGestureAction) {
          detectTapGestures(
            onDoubleTap = {
              if (uiState.settings.doubleTapAction != HomeGestureAction.NONE) {
                launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
              }
              onTriggerGestureAction(uiState.settings.doubleTapAction)
            },
          )
        }
        .pointerInput(
          uiState.settings.swipeLeftAction,
          uiState.settings.swipeRightAction,
          launcherHaptics,
          onTriggerGestureAction,
        ) {
          var dragX = 0f
          var triggered = false
          detectHorizontalDragGestures(
            onDragStart = {
              dragX = 0f
              triggered = false
            },
            onHorizontalDrag = { _, delta ->
              if (triggered) return@detectHorizontalDragGestures
              dragX += delta
              if (dragX < -96f) {
                triggered = true
                if (uiState.settings.swipeLeftAction != HomeGestureAction.NONE) {
                  launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                }
                onTriggerGestureAction(uiState.settings.swipeLeftAction)
              } else if (dragX > 96f) {
                triggered = true
                if (uiState.settings.swipeRightAction != HomeGestureAction.NONE) {
                  launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                }
                onTriggerGestureAction(uiState.settings.swipeRightAction)
              }
            },
          )
        },
    contentAlignment = Alignment.TopCenter,
  ) {
  if (uiState.settings.coastalBackdropEnabled) {
      val gyroParallax = rememberGyroscopeParallaxState(enabled = !effectiveReducedMotion)

      CoastalGradientOverlay(
        hourOfDay = hourOfDay,
        reducedMotion = effectiveReducedMotion,
        parallaxState = gyroParallax,
      )

      HeroWeatherAnimationLayer(
        weather = uiState.weather,
        coastalPalette = coastalPalette,
        reducedMotion = effectiveReducedMotion,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(280.dp)
            .align(Alignment.TopCenter)
            .graphicsLayer {
              if (!effectiveReducedMotion) {
                translationX = gyroParallax.offsetX * 36.dp.toPx()
                translationY = gyroParallax.offsetY * 26.dp.toPx()
              }
            },
      )
    }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .widthIn(max = 640.dp),
    ) {
      Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxHeight()
              .padding(start = 24.dp, end = if (uiState.settings.alphabetIndexEnabled) 8.dp else 24.dp),
        ) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .pointerInput(onExpandNotificationShade, onOpenDrawer, launcherHaptics) {
                  var dragY = 0f
                  detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, delta ->
                      dragY += delta
                      if (dragY > 72f) {
                        dragY = 0f
                        launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                        onExpandNotificationShade()
                      } else if (dragY < -72f) {
                        dragY = 0f
                        launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                        onOpenDrawer(false, null)
                      }
                    },
                  )
                },
          ) {
            Spacer(modifier = Modifier.height(16.dp))

            SynthwaveClockWidget(
              formattedTime = formattedTime,
              amPmLabel = amPmLabel,
              formattedDate = formattedDate,
              locationLabel = uiState.weather.locationName,
              reducedMotion = effectiveReducedMotion,
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                onClockClick()
              },
            )

            Spacer(modifier = Modifier.height(8.dp))

            val weather = uiState.weather
            val (weatherIcon, weatherTint) =
              when (weather.effectType) {
                WeatherEffectType.THUNDERSTORM -> Icons.Outlined.Thunderstorm to WeatherStormViolet
                WeatherEffectType.RAIN -> Icons.Outlined.WaterDrop to WeatherRainCyan
                WeatherEffectType.CLOUDY -> Icons.Outlined.Cloud to CoastalDayWash
                WeatherEffectType.HEAT_HAZE -> Icons.Outlined.WbSunny to WeatherHeatCoral
                WeatherEffectType.CLEAR_BREEZE ->
                  if (weather.isDay) Icons.Outlined.WbSunny to WeatherSunGold
                  else Icons.Outlined.NightsStay to WeatherHeatGold
              }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .combinedClickable(
                    onClick = {
                      launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                      onRefreshWeather()
                    },
                    onLongClick = {
                      launcherHaptics.trigger(LauncherHapticEvent.LONG_PRESS)
                      showCityPicker = true
                    },
                  )
                  .minimumInteractiveComponentSize()
                  .padding(vertical = 6.dp, horizontal = 2.dp)
                  .testTag("weather_placeholder_card"),
            ) {
              Icon(
                imageVector = weatherIcon,
                contentDescription = weather.conditionLabel,
                tint = weatherTint,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${weather.temperatureCelsius}°",
                style =
                  MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                  ),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${weather.locationName} · ${weather.conditionLabel}",
                style =
                  MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (weather.isRefreshing) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  strokeWidth = 1.6.dp,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }

            val nextEvent = uiState.nextCalendarEvent
            if (nextEvent != null) {
              val eventTimeCopy = remember(nextEvent, currentTimeMillis / 60_000L) {
                formatRelativeEventTime(nextEvent, currentTimeMillis)
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                      onOpenCalendar()
                    }
                    .minimumInteractiveComponentSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTag("calendar_event_preview"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.CalendarToday,
                  contentDescription = "Upcoming calendar event",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = nextEvent.title,
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "· $eventTimeCopy",
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HeroFocusTimerBar(
              isFocusModeActive = uiState.isFocusModeActive,
              focusTimerEndMillis = uiState.settings.focusTimerEndMillis,
              defaultDurationMinutes = uiState.settings.focusTimerDurationMinutes,
              hapticEnabled = hapticsEnabled,
              onStartTimer = onStartFocusTimer,
              onStopTimer = onStopFocusTimer,
              onToggleFocusMode = onToggleFocusMode,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text =
              if (uiState.isFocusModeActive && uiState.settings.focusEssentialComponentKeys.isNotEmpty()) {
                "ESSENTIALS"
              } else {
                stringResource(R.string.favorites_header)
              },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            when {
              uiState.isLoading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              }
              uiState.favoriteApps.isEmpty() && uiState.homeFolders.isEmpty() -> {
                Text(
                  text = "No favorites selected. Tap Settings or open All apps to add favorites.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              else -> {
                LazyColumn(
                  state = listState,
                  contentPadding = PaddingValues(bottom = 12.dp),
                  modifier = Modifier.fillMaxSize().testTag("favorites_list"),
                ) {
                  items(
                    items = uiState.homeFolders,
                    key = { "folder_${it.folder.id}" },
                    contentType = { "home_folder" },
                  ) { resolvedFolder ->
                    val folderId = resolvedFolder.folder.id
                    HomeFolderRow(
                      resolvedFolder = resolvedFolder,
                      expanded = folderId in expandedFolderIds,
                      showLabel = !uiState.settings.hideAppLabels,
                      hapticEnabled = hapticsEnabled,
                      showNotificationDot = uiState.settings.notificationDotsEnabled,
                      showDailyLaunchCount = uiState.settings.showDailyLaunchCounts,
                      onToggleExpand = {
                        expandedFolderIds =
                          if (folderId in expandedFolderIds) expandedFolderIds - folderId
                          else expandedFolderIds + folderId
                      },
                      onLaunchApp = onLaunchApp,
                      onOpenAppInfo = onOpenAppInfo,
                      onRenameApp = { renameTarget = it },
                      onToggleMindfulPause = onToggleMindfulPause,
                      onEditFolder = { editingFolder = resolvedFolder.folder },
                      onDeleteFolder = { onDeleteFolder(folderId) },
                    )
                  }

                  itemsIndexed(
                    items = uiState.favoriteApps,
                    key = { _, app -> app.componentKey },
                    contentType = { _, _ -> "favorite_app" },
                  ) { index, app ->
                    AppListRow(
                      app = app,
                      showLabel = !uiState.settings.hideAppLabels,
                      hapticEnabled = hapticsEnabled,
                      showNotificationDot = uiState.settings.notificationDotsEnabled,
                      showDailyLaunchCount = uiState.settings.showDailyLaunchCounts,
                      onTap = { onLaunchApp(app) },
                      showDivider = index < uiState.favoriteApps.lastIndex,
                    ) { dismiss ->
                      MinimalDropdownItem(
                        text = stringResource(R.string.app_info),
                        icon = {
                          Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                          Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                            tint = MaterialTheme.colorScheme.primary,
                          )
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
                          dismiss()
                          onToggleMindfulPause(app)
                        },
                      )
                      MinimalDropdownItem(
                        text = stringResource(R.string.remove_from_favorites),
                        icon = {
                          Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = DangerRed)
                        },
                        onClick = {
                          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                          dismiss()
                          onRemoveFromFavorites(app)
                        },
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (uiState.settings.alphabetIndexEnabled) {
          AlphabetScrubberRail(
            activeLetter = null,
            hapticEnabled = hapticsEnabled,
            onLetterSelected = { letter -> onOpenDrawer(false, letter) },
            modifier = Modifier.padding(top = 136.dp, bottom = 12.dp, end = 6.dp),
          )
        }
      }

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .pointerInput(onOpenDrawer, launcherHaptics) {
              var dragY = 0f
              detectVerticalDragGestures(
                onDragStart = { dragY = 0f },
                onVerticalDrag = { _, delta ->
                  dragY += delta
                  if (dragY < -54f) {
                    dragY = 0f
                    launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                    onOpenDrawer(false, null)
                  }
                },
              )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            onOpenDrawer(true, null)
          },
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.weight(1f).height(48.dp).testTag("home_search_pill"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = "Search apps",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.search_apps_hint),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
            ) {
              Text(
                text = "⌘K",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            onOpenSettings()
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
          )
        }
      }
    }
  }

  if (showCityPicker) {
    WeatherCityPickerDialog(
      currentLocation = uiState.settings.weatherLocation,
      onSearchCities = onSearchWeatherCities,
      onSelectLocation = { location ->
        onSelectWeatherLocation(location)
        showCityPicker = false
      },
      onDismiss = { showCityPicker = false },
    )
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

  val targetFolder = editingFolder
  if (targetFolder != null) {
    FolderEditorDialog(
      existingFolder = targetFolder,
      allApps = uiState.allVisibleApps,
      onSave = { name, apps ->
        onSaveFolder(targetFolder.id, name, apps)
        editingFolder = null
      },
      onDelete = {
        onDeleteFolder(targetFolder.id)
        editingFolder = null
      },
      onDismiss = { editingFolder = null },
    )
  }
}

private fun isSystemLowPowerOrReducedMotion(context: Context): Boolean {
  val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
  val powerSave = powerManager?.isPowerSaveMode == true
  val animatorScale =
    runCatching {
      Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
  return powerSave || animatorScale == 0f
}

private fun formatRelativeEventTime(event: CalendarEventPreview, nowMillis: Long): String {
  val diffMinutes = ((event.startTimeMillis - nowMillis) / 60_000L).toInt()
  return when {
    diffMinutes <= 0 -> "Now"
    diffMinutes < 60 -> "in ${diffMinutes}m"
    else -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.startTimeMillis))
  }
}
