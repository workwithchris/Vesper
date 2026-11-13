package com.example.ui.home

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
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
import com.example.ui.theme.GeistMonoNumericStyle
import com.example.ui.theme.WeatherHeatCoral
import com.example.ui.theme.WeatherHeatGold
import com.example.ui.theme.WeatherRainCyan
import com.example.ui.theme.WeatherStormViolet
import com.example.ui.theme.WeatherSunGold
import com.example.util.LauncherHapticEvent
import com.example.util.LauncherHaptics
import com.example.util.rememberLauncherHaptics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
  onMoveFavorite: (AppInfo, Int) -> Unit = { _, _ -> },
  onSaveDailyIntention: (String) -> Unit = {},
  onLaunchShortcut: (AppShortcutItem) -> Unit = {},
  onAutoCategorizeFolders: () -> Unit = {},
  onSaveQuickScratchpad: (String) -> Unit = {},
  isDrawerOpen: Boolean = false,
  homeIntentTrigger: Int = 0,
) {
  val context = LocalContext.current
  val view = LocalView.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var renameTarget by remember { mutableStateOf<AppInfo?>(null) }
  var editingFolder by remember { mutableStateOf<AppFolder?>(null) }
  var expandedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var showCityPicker by remember { mutableStateOf(false) }
  var showIntentionDialog by remember { mutableStateOf(false) }
  var showScratchpadDialog by remember { mutableStateOf(false) }
  var systemLowPowerMode by remember { mutableStateOf(isSystemLowPowerOrReducedMotion(context)) }
  var batteryPercentState by remember { mutableStateOf<Int?>(null) }
  val hapticsEnabled = uiState.settings.hapticFeedbackEnabled
  val launcherHaptics = rememberLauncherHaptics(hapticsEnabled)
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

  BackHandler(enabled = expandedFolderIds.isNotEmpty()) {
    expandedFolderIds = emptySet()
  }

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

  DisposableEffect(context, uiState.settings.showBatteryIndicator) {
    if (!uiState.settings.showBatteryIndicator) {
      batteryPercentState = null
      onDispose {}
    } else {
      batteryPercentState = readBatteryCapacityFallback(context)
      val receiver =
        object : BroadcastReceiver() {
          override fun onReceive(ctx: Context?, intent: Intent?) {
            val parsed = parseBatteryIntentLevel(intent)
            if (parsed != null) batteryPercentState = parsed
          }
        }
      val sticky =
        ContextCompat.registerReceiver(
          context,
          receiver,
          IntentFilter(Intent.ACTION_BATTERY_CHANGED),
          ContextCompat.RECEIVER_EXPORTED,
        )
      parseBatteryIntentLevel(sticky)?.let { batteryPercentState = it }
      onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
  }

  val showClockSeconds = uiState.settings.showClockSeconds
  LaunchedEffect(showClockSeconds) {
    while (true) {
      currentTimeMillis = System.currentTimeMillis()
      val periodMs = if (showClockSeconds) 1_000L else 60_000L
      val offset = currentTimeMillis % periodMs
      delay(periodMs - offset + 25L)
    }
  }

  val effectiveReducedMotion = uiState.settings.reducedMotionEnabled || systemLowPowerMode
  val pageParallaxProgress by
    remember(pagerState) {
      derivedStateOf {
        val span = (pagerState.pageCount - 1).coerceAtLeast(1).toFloat()
        ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / span)
          .coerceIn(-0.15f, 1.15f)
      }
    }

  BackHandler(enabled = pagerState.currentPage > 0 && expandedFolderIds.isEmpty() && !isDrawerOpen) {
    coroutineScope.launch {
      if (effectiveReducedMotion) pagerState.scrollToPage(0)
      else pagerState.animateScrollToPage(0)
    }
  }

  LaunchedEffect(homeIntentTrigger) {
    if (homeIntentTrigger > 0 && pagerState.currentPage != 0) {
      if (effectiveReducedMotion) pagerState.scrollToPage(0)
      else pagerState.animateScrollToPage(0)
    }
  }

  val wallpaperManager = remember(context) { WallpaperManager.getInstance(context) }
  LaunchedEffect(uiState.settings.useSystemWallpaper, pagerState.pageCount) {
    if (uiState.settings.useSystemWallpaper) {
      val steps = (pagerState.pageCount - 1).coerceAtLeast(1)
      runCatching {
        wallpaperManager.setWallpaperOffsetSteps(1f / steps.toFloat(), 0f)
      }
    }
  }
  LaunchedEffect(pageParallaxProgress, uiState.settings.useSystemWallpaper, effectiveReducedMotion) {
    val token = view.windowToken
    if (uiState.settings.useSystemWallpaper && token != null) {
      val targetX = if (effectiveReducedMotion) 0.5f else pageParallaxProgress.coerceIn(0f, 1f)
      runCatching {
        wallpaperManager.setWallpaperOffsets(token, targetX, 0.5f)
      }
    }
  }

  val timeFormatter =
    remember(showClockSeconds) {
      SimpleDateFormat(if (showClockSeconds) "h:mm:ss" else "h:mm", Locale.getDefault())
    }
  val amPmFormatter = remember { SimpleDateFormat("a", Locale.getDefault()) }
  val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
  val tickBucket = currentTimeMillis / (if (showClockSeconds) 1_000L else 60_000L)
  val nowDate = remember(tickBucket) { Date(currentTimeMillis) }
  val formattedTime = timeFormatter.format(nowDate)
  val amPmLabel = amPmFormatter.format(nowDate)
  val formattedDate = dateFormatter.format(nowDate)
  val minuteBucket = currentTimeMillis / 60_000L
  val hourOfDay =
    remember(minuteBucket) {
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

  val edgeOverscrollConnection =
    remember(
      pagerState,
      uiState.settings.swipeLeftAction,
      uiState.settings.swipeRightAction,
      launcherHaptics,
      onTriggerGestureAction,
    ) {
      object : NestedScrollConnection {
        private var leftEdgeOverscroll = 0f
        private var rightEdgeOverscroll = 0f
        private var triggeredInGesture = false

        override fun onPostScroll(
          consumed: Offset,
          available: Offset,
          source: NestedScrollSource,
        ): Offset {
          if (source == NestedScrollSource.UserInput && !triggeredInGesture) {
            if (available.x > 0f && !pagerState.canScrollBackward) {
              rightEdgeOverscroll = 0f
              leftEdgeOverscroll += available.x
              if (leftEdgeOverscroll > 96f) {
                triggeredInGesture = true
                leftEdgeOverscroll = 0f
                val action = uiState.settings.swipeRightAction
                if (action != HomeGestureAction.NONE) {
                  launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                }
                onTriggerGestureAction(action)
              }
            } else if (available.x < 0f && !pagerState.canScrollForward) {
              leftEdgeOverscroll = 0f
              rightEdgeOverscroll += -available.x
              if (rightEdgeOverscroll > 96f) {
                triggeredInGesture = true
                rightEdgeOverscroll = 0f
                val action = uiState.settings.swipeLeftAction
                if (action != HomeGestureAction.NONE) {
                  launcherHaptics.trigger(LauncherHapticEvent.DRAWER_OPEN_SWIPE)
                }
                onTriggerGestureAction(action)
              }
            }
          }
          return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
          leftEdgeOverscroll = 0f
          rightEdgeOverscroll = 0f
          triggeredInGesture = false
          return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
          leftEdgeOverscroll = 0f
          rightEdgeOverscroll = 0f
          triggeredInGesture = false
          return Velocity.Zero
        }
      }
    }

    val hasCustomWallpaper = !uiState.settings.customWallpaperPath.isNullOrBlank()
    val usesBackdropOverlay =
      uiState.settings.useSystemWallpaper || uiState.settings.coastalBackdropEnabled || hasCustomWallpaper

    Box(
      modifier =
        modifier
          .fillMaxSize()
          .nestedScroll(drawerSwipeConnection)
          .nestedScroll(edgeOverscrollConnection)
          .pointerInput(uiState.settings.doubleTapAction, launcherHaptics, onTriggerGestureAction) {
            detectTapGestures(
              onDoubleTap = {
                if (uiState.settings.doubleTapAction != HomeGestureAction.NONE) {
                  launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
                }
                onTriggerGestureAction(uiState.settings.doubleTapAction)
              },
            )
          },
      contentAlignment = Alignment.TopCenter,
    ) {
      if (usesBackdropOverlay) {
        val gyroParallax = rememberGyroscopeParallaxState(enabled = !effectiveReducedMotion)

        CoastalGradientOverlay(
          hourOfDay = hourOfDay,
          reducedMotion = effectiveReducedMotion,
          parallaxState = gyroParallax,
          pageParallaxProgress = pageParallaxProgress,
          customWallpaperPath = uiState.settings.customWallpaperPath,
          wallpaperDimPercent = uiState.settings.wallpaperDimPercent,
          useSystemWallpaper = uiState.settings.useSystemWallpaper,
          coastalBackdropEnabled = uiState.settings.coastalBackdropEnabled,
        )

        if (uiState.settings.coastalBackdropEnabled && !isDrawerOpen) {
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
                  val tiltX = if (effectiveReducedMotion) 0f else gyroParallax.offsetX
                  val tiltY = if (effectiveReducedMotion) 0f else gyroParallax.offsetY
                  val depth =
                    WallpaperParallaxMath.computeHorizontalDepth(
                      widthPx = size.width,
                      pageProgress = pageParallaxProgress,
                      tiltX = tiltX,
                      tiltPxPerUnit = 16.dp.toPx(),
                      reducedMotion = effectiveReducedMotion,
                    )
                  translationX = depth.weatherTranslationXPx
                  translationY = tiltY * 26.dp.toPx()
                },
          )
        }
      }

    if (!isDrawerOpen) {
      BoxWithConstraints(
        modifier =
          Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
      ) {
      val isTwoPaneTablet = maxWidth >= 700.dp

      if (isTwoPaneTablet) {
        Row(
          modifier =
            Modifier
              .fillMaxSize()
              .widthIn(max = 1080.dp)
              .padding(horizontal = 24.dp, vertical = 8.dp)
              .testTag("home_two_pane_layout"),
          horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
          Column(
            modifier = Modifier.weight(0.46f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            HomeHeroBlock(
              uiState = uiState,
              formattedTime = formattedTime,
              amPmLabel = amPmLabel,
              formattedDate = formattedDate,
              batteryPercent = batteryPercentState,
              currentTimeMillis = currentTimeMillis,
              effectiveReducedMotion = effectiveReducedMotion,
              hapticsEnabled = hapticsEnabled,
              launcherHaptics = launcherHaptics,
              onClockClick = onClockClick,
              onRefreshWeather = onRefreshWeather,
              onShowCityPicker = { showCityPicker = true },
              onOpenCalendar = onOpenCalendar,
              onExpandNotificationShade = onExpandNotificationShade,
              onOpenDrawer = onOpenDrawer,
              onStartFocusTimer = onStartFocusTimer,
              onStopFocusTimer = onStopFocusTimer,
              onToggleFocusMode = onToggleFocusMode,
              onOpenIntentionDialog = { showIntentionDialog = true },
              onOpenScratchpadDialog = { showScratchpadDialog = true },
            )

            HomeBottomActionBar(
              launcherHaptics = launcherHaptics,
              onOpenDrawer = onOpenDrawer,
              onOpenSettings = onOpenSettings,
              horizontalPadding = 0.dp,
            )
          }

          Column(modifier = Modifier.weight(0.54f).fillMaxHeight()) {
            HorizontalPager(
              state = pagerState,
              beyondViewportPageCount = 1,
              modifier = Modifier.weight(1f).fillMaxWidth().testTag("home_horizontal_pager"),
            ) { page ->
              when (page) {
                0 -> {
                  Row(modifier = Modifier.fillMaxSize().testTag("home_page_0")) {
                    if (uiState.settings.alphabetIndexEnabled && uiState.settings.leftHandedScrubber) {
                      AlphabetScrubberRail(
                        activeLetter = null,
                        hapticEnabled = hapticsEnabled,
                        onLetterSelected = { letter -> onOpenDrawer(false, letter) },
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, end = 6.dp),
                      )
                    }

                    HomeFavoritesPane(
                      uiState = uiState,
                      listState = listState,
                      expandedFolderIds = expandedFolderIds,
                      hapticsEnabled = hapticsEnabled,
                      launcherHaptics = launcherHaptics,
                      onToggleExpandFolder = { folderId ->
                        expandedFolderIds =
                          if (folderId in expandedFolderIds) expandedFolderIds - folderId
                          else expandedFolderIds + folderId
                      },
                      onLaunchApp = onLaunchApp,
                      onOpenAppInfo = onOpenAppInfo,
                      onRenameApp = { renameTarget = it },
                      onToggleMindfulPause = onToggleMindfulPause,
                      onEditFolder = { editingFolder = it },
                      onDeleteFolder = onDeleteFolder,
                      onRemoveFromFavorites = onRemoveFromFavorites,
                      onMoveFavorite = onMoveFavorite,
                      onLaunchShortcut = onLaunchShortcut,
                      onAutoCategorizeFolders = onAutoCategorizeFolders,
                      modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 12.dp),
                    )

                    if (uiState.settings.alphabetIndexEnabled && !uiState.settings.leftHandedScrubber) {
                      AlphabetScrubberRail(
                        activeLetter = null,
                        hapticEnabled = hapticsEnabled,
                        onLetterSelected = { letter -> onOpenDrawer(false, letter) },
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                      )
                    }
                  }
                }
                1 -> {
                  HomeFocusDailyPage(
                    uiState = uiState,
                    hapticsEnabled = hapticsEnabled,
                    launcherHaptics = launcherHaptics,
                    onOpenIntentionDialog = { showIntentionDialog = true },
                    onOpenScratchpadDialog = { showScratchpadDialog = true },
                    onStartFocusTimer = onStartFocusTimer,
                    onStopFocusTimer = onStopFocusTimer,
                    onToggleFocusMode = onToggleFocusMode,
                    onLaunchApp = onLaunchApp,
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp).testTag("home_page_1"),
                  )
                }
                else -> {
                  HomeAtmosSpacesPage(
                    uiState = uiState,
                    currentTimeMillis = currentTimeMillis,
                    launcherHaptics = launcherHaptics,
                    onRefreshWeather = onRefreshWeather,
                    onShowCityPicker = { showCityPicker = true },
                    onOpenCalendar = onOpenCalendar,
                    onAutoCategorizeFolders = onAutoCategorizeFolders,
                    onLaunchApp = onLaunchApp,
                    onOpenDrawer = onOpenDrawer,
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp).testTag("home_page_2"),
                  )
                }
              }
            }

            HomePageIndicator(
              pageCount = pagerState.pageCount,
              currentPage = pagerState.currentPage,
              onSelectPage = { targetPage ->
                launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                coroutineScope.launch {
                  if (effectiveReducedMotion) pagerState.scrollToPage(targetPage)
                  else pagerState.animateScrollToPage(targetPage)
                }
              },
              modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
            )
          }
        }
      } else {
        val leftScrubber = uiState.settings.alphabetIndexEnabled && uiState.settings.leftHandedScrubber
        val rightScrubber = uiState.settings.alphabetIndexEnabled && !uiState.settings.leftHandedScrubber

        Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
          HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("home_horizontal_pager"),
          ) { page ->
            when (page) {
              0 -> {
                Row(modifier = Modifier.fillMaxSize().testTag("home_page_0")) {
                  if (leftScrubber) {
                    AlphabetScrubberRail(
                      activeLetter = null,
                      hapticEnabled = hapticsEnabled,
                      onLetterSelected = { letter -> onOpenDrawer(false, letter) },
                      modifier = Modifier.padding(top = 136.dp, bottom = 12.dp, start = 6.dp),
                    )
                  }

                  Column(
                    modifier =
                      Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                          start = if (leftScrubber) 8.dp else 24.dp,
                          end = if (rightScrubber) 8.dp else 24.dp,
                        ),
                  ) {
                    HomeHeroBlock(
                      uiState = uiState,
                      formattedTime = formattedTime,
                      amPmLabel = amPmLabel,
                      formattedDate = formattedDate,
                      batteryPercent = batteryPercentState,
                      currentTimeMillis = currentTimeMillis,
                      effectiveReducedMotion = effectiveReducedMotion,
                      hapticsEnabled = hapticsEnabled,
                      launcherHaptics = launcherHaptics,
                      onClockClick = onClockClick,
                      onRefreshWeather = onRefreshWeather,
                      onShowCityPicker = { showCityPicker = true },
                      onOpenCalendar = onOpenCalendar,
                      onExpandNotificationShade = onExpandNotificationShade,
                      onOpenDrawer = onOpenDrawer,
                      onStartFocusTimer = onStartFocusTimer,
                      onStopFocusTimer = onStopFocusTimer,
                      onToggleFocusMode = onToggleFocusMode,
                      onOpenIntentionDialog = { showIntentionDialog = true },
                      onOpenScratchpadDialog = { showScratchpadDialog = true },
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HomeFavoritesPane(
                      uiState = uiState,
                      listState = listState,
                      expandedFolderIds = expandedFolderIds,
                      hapticsEnabled = hapticsEnabled,
                      launcherHaptics = launcherHaptics,
                      onToggleExpandFolder = { folderId ->
                        expandedFolderIds =
                          if (folderId in expandedFolderIds) expandedFolderIds - folderId
                          else expandedFolderIds + folderId
                      },
                      onLaunchApp = onLaunchApp,
                      onOpenAppInfo = onOpenAppInfo,
                      onRenameApp = { renameTarget = it },
                      onToggleMindfulPause = onToggleMindfulPause,
                      onEditFolder = { editingFolder = it },
                      onDeleteFolder = onDeleteFolder,
                      onRemoveFromFavorites = onRemoveFromFavorites,
                      onMoveFavorite = onMoveFavorite,
                      onLaunchShortcut = onLaunchShortcut,
                      onAutoCategorizeFolders = onAutoCategorizeFolders,
                      modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                  }

                  if (rightScrubber) {
                    AlphabetScrubberRail(
                      activeLetter = null,
                      hapticEnabled = hapticsEnabled,
                      onLetterSelected = { letter -> onOpenDrawer(false, letter) },
                      modifier = Modifier.padding(top = 136.dp, bottom = 12.dp, end = 6.dp),
                    )
                  }
                }
              }
              1 -> {
                HomeFocusDailyPage(
                  uiState = uiState,
                  hapticsEnabled = hapticsEnabled,
                  launcherHaptics = launcherHaptics,
                  onOpenIntentionDialog = { showIntentionDialog = true },
                  onOpenScratchpadDialog = { showScratchpadDialog = true },
                  onStartFocusTimer = onStartFocusTimer,
                  onStopFocusTimer = onStopFocusTimer,
                  onToggleFocusMode = onToggleFocusMode,
                  onLaunchApp = onLaunchApp,
                  modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag("home_page_1"),
                )
              }
              else -> {
                HomeAtmosSpacesPage(
                  uiState = uiState,
                  currentTimeMillis = currentTimeMillis,
                  launcherHaptics = launcherHaptics,
                  onRefreshWeather = onRefreshWeather,
                  onShowCityPicker = { showCityPicker = true },
                  onOpenCalendar = onOpenCalendar,
                  onAutoCategorizeFolders = onAutoCategorizeFolders,
                  onLaunchApp = onLaunchApp,
                  onOpenDrawer = onOpenDrawer,
                  modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag("home_page_2"),
                )
              }
            }
          }

          HomePageIndicator(
            pageCount = pagerState.pageCount,
            currentPage = pagerState.currentPage,
            onSelectPage = { targetPage ->
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              coroutineScope.launch {
                if (effectiveReducedMotion) pagerState.scrollToPage(targetPage)
                else pagerState.animateScrollToPage(targetPage)
              }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
          )

          HomeBottomActionBar(
            launcherHaptics = launcherHaptics,
            onOpenDrawer = onOpenDrawer,
            onOpenSettings = onOpenSettings,
            horizontalPadding = 20.dp,
          )
        }
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

  if (showIntentionDialog) {
    DailyIntentionDialog(
      currentIntention = uiState.settings.dailyIntention,
      onSave = { intention ->
        onSaveDailyIntention(intention)
        showIntentionDialog = false
      },
      onDismiss = { showIntentionDialog = false },
    )
  }

  if (showScratchpadDialog) {
    QuickScratchpadDialog(
      currentText = uiState.settings.quickScratchpadText,
      onSave = { note ->
        onSaveQuickScratchpad(note)
        showScratchpadDialog = false
      },
      onDismiss = { showScratchpadDialog = false },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHeroBlock(
  uiState: LauncherUiState,
  formattedTime: String,
  amPmLabel: String,
  formattedDate: String,
  batteryPercent: Int?,
  currentTimeMillis: Long,
  effectiveReducedMotion: Boolean,
  hapticsEnabled: Boolean,
  launcherHaptics: LauncherHaptics,
  onClockClick: () -> Unit,
  onRefreshWeather: () -> Unit,
  onShowCityPicker: () -> Unit,
  onOpenCalendar: () -> Unit,
  onExpandNotificationShade: () -> Unit,
  onOpenDrawer: (Boolean, Char?) -> Unit,
  onStartFocusTimer: (Int) -> Unit,
  onStopFocusTimer: () -> Unit,
  onToggleFocusMode: () -> Unit,
  onOpenIntentionDialog: () -> Unit,
  onOpenScratchpadDialog: () -> Unit = {},
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
      clockStyle = uiState.settings.clockStyle,
      batteryPercent = batteryPercent,
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onClockClick()
      },
    )

    Spacer(modifier = Modifier.height(8.dp))

    val weatherSlot: @Composable () -> Unit = {
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
                onShowCityPicker()
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
    }

    val focusTimerSlot: @Composable () -> Unit = {
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

    val intentionSlot: @Composable () -> Unit = {
      DailyIntentionPill(
        intention = uiState.settings.dailyIntention,
        hapticEnabled = hapticsEnabled,
        onClick = onOpenIntentionDialog,
      )
    }

    val scratchpadSlot: @Composable () -> Unit = {
      QuickScratchpadPill(
        scratchpadText = uiState.settings.quickScratchpadText,
        hapticEnabled = hapticsEnabled,
        onClick = onOpenScratchpadDialog,
      )
    }

    val nextEvent = uiState.nextCalendarEvent
    val calendarSlot: (@Composable () -> Unit)? =
      if (nextEvent != null) {
        {
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
      } else {
        null
      }

    if (uiState.settings.widgetStackEnabled) {
      val stackedSlots =
        buildList<@Composable () -> Unit> {
          add(weatherSlot)
          add(focusTimerSlot)
          add(intentionSlot)
          add(scratchpadSlot)
          if (calendarSlot != null) add(calendarSlot)
        }
      GeistWidgetStack(
        slots = stackedSlots,
        hapticEnabled = hapticsEnabled,
        reducedMotion = effectiveReducedMotion,
      )
    } else {
      weatherSlot()
      if (calendarSlot != null) {
        calendarSlot()
      }
      Spacer(modifier = Modifier.height(6.dp))
      focusTimerSlot()
      Spacer(modifier = Modifier.height(8.dp))
      intentionSlot()
      Spacer(modifier = Modifier.height(6.dp))
      scratchpadSlot()
    }
  }
}

@Composable
private fun HomeFavoritesPane(
  uiState: LauncherUiState,
  listState: LazyListState,
  expandedFolderIds: Set<String>,
  hapticsEnabled: Boolean,
  launcherHaptics: LauncherHaptics,
  onToggleExpandFolder: (String) -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppInfo: (AppInfo) -> Unit,
  onRenameApp: (AppInfo) -> Unit,
  onToggleMindfulPause: (AppInfo) -> Unit,
  onEditFolder: (AppFolder) -> Unit,
  onDeleteFolder: (String) -> Unit,
  onRemoveFromFavorites: (AppInfo) -> Unit,
  onMoveFavorite: (AppInfo, Int) -> Unit,
  onLaunchShortcut: (AppShortcutItem) -> Unit,
  onAutoCategorizeFolders: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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

      if (uiState.allVisibleApps.isNotEmpty()) {
        Text(
          text = "AUTO-GROUP",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier =
            Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable {
                launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
                onAutoCategorizeFolders()
              }
              .padding(horizontal = 6.dp, vertical = 2.dp)
              .testTag("home_auto_categorize_button"),
        )
      }
    }

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
            verticalArrangement =
              if (uiState.settings.bottomAlignFavorites) Arrangement.Bottom else Arrangement.Top,
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
                showDailyLaunchCount = uiState.settings.showDailyLaunchCounts,
                onToggleExpand = { onToggleExpandFolder(folderId) },
                onLaunchApp = onLaunchApp,
                onOpenAppInfo = onOpenAppInfo,
                onRenameApp = onRenameApp,
                onToggleMindfulPause = onToggleMindfulPause,
                onEditFolder = { onEditFolder(resolvedFolder.folder) },
                onDeleteFolder = { onDeleteFolder(folderId) },
                shortcutsByPackage = uiState.appShortcutsByPackage,
                onLaunchShortcut = onLaunchShortcut,
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
                showDailyLaunchCount = uiState.settings.showDailyLaunchCounts,
                onTap = { onLaunchApp(app) },
                showDivider = false,
              ) { dismiss ->
                FavoriteAppContextMenuItems(
                  app = app,
                  index = index,
                  totalCount = uiState.favoriteApps.size,
                  shortcuts = uiState.appShortcutsByPackage[app.packageName].orEmpty(),
                  launcherHaptics = launcherHaptics,
                  dismiss = dismiss,
                  onOpenAppInfo = onOpenAppInfo,
                  onMoveFavorite = onMoveFavorite,
                  onLaunchShortcut = onLaunchShortcut,
                  onRenameApp = onRenameApp,
                  onToggleMindfulPause = onToggleMindfulPause,
                  onRemoveFromFavorites = onRemoveFromFavorites,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FavoriteAppContextMenuItems(
  app: AppInfo,
  index: Int,
  totalCount: Int,
  shortcuts: List<AppShortcutItem>,
  launcherHaptics: LauncherHaptics,
  dismiss: () -> Unit,
  onOpenAppInfo: (AppInfo) -> Unit,
  onMoveFavorite: (AppInfo, Int) -> Unit,
  onLaunchShortcut: (AppShortcutItem) -> Unit,
  onRenameApp: (AppInfo) -> Unit,
  onToggleMindfulPause: (AppInfo) -> Unit,
  onRemoveFromFavorites: (AppInfo) -> Unit,
) {
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
  if (index > 0) {
    MinimalDropdownItem(
      text = "Move up",
      icon = {
        Icon(Icons.Outlined.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      },
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        dismiss()
        onMoveFavorite(app, -1)
      },
    )
  }
  if (index < totalCount - 1) {
    MinimalDropdownItem(
      text = "Move down",
      icon = {
        Icon(Icons.Outlined.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      },
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        dismiss()
        onMoveFavorite(app, +1)
      },
    )
  }
  shortcuts.take(3).forEach { shortcut ->
    MinimalDropdownItem(
      text = shortcut.shortLabel,
      icon = {
        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      },
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
        dismiss()
        onLaunchShortcut(shortcut)
      },
    )
  }
  MinimalDropdownItem(
    text = stringResource(R.string.rename_app),
    icon = {
      Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    },
    onClick = {
      launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
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

@Composable
private fun HomeBottomActionBar(
  launcherHaptics: LauncherHaptics,
  onOpenDrawer: (Boolean, Char?) -> Unit,
  onOpenSettings: () -> Unit,
  horizontalPadding: androidx.compose.ui.unit.Dp,
) {
  Box(
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
        .padding(horizontal = horizontalPadding, vertical = 10.dp),
  ) {
    IconButton(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onOpenDrawer(false, null)
      },
      modifier =
        Modifier
          .align(Alignment.Center)
          .minimumInteractiveComponentSize()
          .testTag("home_all_apps_button"),
    ) {
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowUp,
        contentDescription = "Open all apps",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        modifier = Modifier.size(24.dp),
      )
    }

    IconButton(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onOpenSettings()
      },
      modifier =
        Modifier
          .align(Alignment.CenterEnd)
          .minimumInteractiveComponentSize()
          .testTag("settings_button"),
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

@Composable
private fun HomePageIndicator(
  pageCount: Int,
  currentPage: Int,
  onSelectPage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(top = 4.dp).testTag("home_page_indicator"),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val pageLabels = remember { listOf("Home page", "Focus and daily page", "Atmos and spaces page") }
    for (index in 0 until pageCount) {
      val selected = index == currentPage
      val label = pageLabels.getOrElse(index) { "Page ${index + 1}" }
      Box(
        modifier =
          Modifier
            .width(28.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectPage(index) }
            .semantics { contentDescription = label }
            .testTag("home_page_dot_$index"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier
              .width(if (selected) 16.dp else 5.dp)
              .height(5.dp)
              .clip(CircleShape)
              .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
              )
        )
      }
    }
  }
}

@Composable
private fun HomeFocusDailyPage(
  uiState: LauncherUiState,
  hapticsEnabled: Boolean,
  launcherHaptics: LauncherHaptics,
  onOpenIntentionDialog: () -> Unit,
  onOpenScratchpadDialog: () -> Unit,
  onStartFocusTimer: (Int) -> Unit,
  onStopFocusTimer: () -> Unit,
  onToggleFocusMode: () -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  val totalLaunches = remember(uiState.allVisibleApps) {
    uiState.allVisibleApps.sumOf { it.dailyLaunchCount }
  }
  val topTodayApps = remember(uiState.allVisibleApps) {
    uiState.allVisibleApps
      .filter { it.dailyLaunchCount > 0 }
      .sortedByDescending { it.dailyLaunchCount }
      .take(3)
  }

  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 20.dp, bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "FOCUS & DAILY",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    HomePageGlassCard(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onOpenIntentionDialog()
      },
      modifier = Modifier.testTag("page1_intention_card"),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.TaskAlt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "DAILY INTENTION",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          if (uiState.settings.dailyIntention.isNotBlank()) uiState.settings.dailyIntention
          else "Tap to anchor one quiet priority for today",
        style = MaterialTheme.typography.titleMedium,
        color =
          if (uiState.settings.dailyIntention.isNotBlank()) MaterialTheme.colorScheme.onBackground
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    HomePageGlassCard(modifier = Modifier.testTag("page1_focus_session_card")) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.SelfImprovement,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "FOCUS SESSION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text =
            if (uiState.isFocusModeActive) "ACTIVE · ${uiState.settings.focusEssentialComponentKeys.size} ESSENTIALS"
            else "STANDBY",
          style = GeistMonoNumericStyle,
          color =
            if (uiState.isFocusModeActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
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

    HomePageGlassCard(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onOpenScratchpadDialog()
      },
      modifier = Modifier.testTag("page1_scratchpad_card"),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "QUICK SCRATCHPAD",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          if (uiState.settings.quickScratchpadText.isNotBlank()) uiState.settings.quickScratchpadText
          else "Tap to jot a thought or quick checklist...",
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (uiState.settings.quickScratchpadText.isNotBlank()) MaterialTheme.colorScheme.onBackground
          else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
      )
    }

    HomePageGlassCard(modifier = Modifier.testTag("page1_mindful_insights_card")) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "MINDFUL INSIGHTS",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "$totalLaunches launches · ${uiState.settings.mindfulPausedComponentKeys.size} guarded",
          style = GeistMonoNumericStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (topTodayApps.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        topTodayApps.forEach { app ->
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                  onLaunchApp(app)
                }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = app.label,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = "${app.dailyLaunchCount}×",
              style = GeistMonoNumericStyle,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HomeAtmosSpacesPage(
  uiState: LauncherUiState,
  currentTimeMillis: Long,
  launcherHaptics: LauncherHaptics,
  onRefreshWeather: () -> Unit,
  onShowCityPicker: () -> Unit,
  onOpenCalendar: () -> Unit,
  onAutoCategorizeFolders: () -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenDrawer: (Boolean, Char?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val weather = uiState.weather
  val frequentApps = remember(uiState.allVisibleApps) {
    uiState.allVisibleApps
      .sortedByDescending { it.dailyLaunchCount }
      .take(4)
  }

  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 20.dp, bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "ATMOS & SPACES",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    HomePageGlassCard(modifier = Modifier.testTag("page2_weather_card")) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${weather.temperatureCelsius}° · ${weather.locationName}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = weather.conditionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            onRefreshWeather()
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_weather_refresh_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = "Refresh weather",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "FEELS LIKE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${weather.apparentTemperatureCelsius}°C",
            style = GeistMonoNumericStyle,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        Column {
          Text(
            text = "WIND",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${weather.windSpeedKmh} km/h",
            style = GeistMonoNumericStyle,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        Column {
          Text(
            text = "CLOUD",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${weather.cloudCoverPercent}%",
            style = GeistMonoNumericStyle,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      TextButton(
        onClick = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onShowCityPicker()
        },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_change_city_button"),
      ) {
        Text("Change coastal city")
      }
    }

    val nextEvent = uiState.nextCalendarEvent
    if (nextEvent != null) {
      HomePageGlassCard(
        onClick = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onOpenCalendar()
        },
        modifier = Modifier.testTag("page2_calendar_card"),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "NEXT UP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = nextEvent.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = formatRelativeEventTime(nextEvent, currentTimeMillis),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    HomePageGlassCard(modifier = Modifier.testTag("page2_spaces_card")) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "SMART SPACES · ${uiState.homeFolders.size} FOLDERS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
            onAutoCategorizeFolders()
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_auto_group_button"),
        ) {
          Text("Auto-group")
        }
      }

      if (frequentApps.isNotEmpty()) {
        frequentApps.forEach { app ->
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                  onLaunchApp(app)
                }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = app.label,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (app.dailyLaunchCount > 0) {
              Text(
                text = "${app.dailyLaunchCount}×",
                style = GeistMonoNumericStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      TextButton(
        onClick = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onOpenDrawer(false, null)
        },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_open_drawer_button"),
      ) {
        Text("Browse all ${uiState.allVisibleApps.size} apps")
      }
    }
  }
}

@Composable
private fun HomePageGlassCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(14.dp)
  val color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  if (onClick != null) {
    Surface(
      onClick = onClick,
      shape = shape,
      color = color,
      border = border,
      modifier = modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        content()
      }
    }
  } else {
    Surface(
      shape = shape,
      color = color,
      border = border,
      modifier = modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        content()
      }
    }
  }
}

private fun parseBatteryIntentLevel(intent: Intent?): Int? {
  if (intent == null) return null
  val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
  val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
  if (rawLevel < 0 || scale <= 0) return null
  return ((rawLevel * 100f) / scale).toInt().coerceIn(0, 100)
}

private fun readBatteryCapacityFallback(context: Context): Int? {
  val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
  val capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
  return capacity.takeIf { it in 0..100 }
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
