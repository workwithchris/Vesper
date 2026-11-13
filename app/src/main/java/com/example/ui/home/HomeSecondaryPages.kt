package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.LauncherUiState
import com.example.model.WeatherEffectType
import com.example.ui.components.FramedAppIcon
import com.example.ui.theme.GeistMonoNumericStyle
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLauncherIsDark
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.util.LauncherHapticEvent
import com.example.util.LauncherHaptics
import kotlinx.coroutines.delay

@Composable
internal fun HomeFocusDailyPage(
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
  val totalLaunches =
    remember(uiState.allVisibleApps) { uiState.allVisibleApps.sumOf { it.dailyLaunchCount } }
  val topTodayApps =
    remember(uiState.allVisibleApps) {
      uiState.allVisibleApps
        .filter { it.dailyLaunchCount > 0 }
        .sortedByDescending { it.dailyLaunchCount }
        .take(3)
    }
  val maxLaunchCount = remember(topTodayApps) { (topTodayApps.maxOfOrNull { it.dailyLaunchCount } ?: 1).coerceAtLeast(1) }

  var liveNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val endMillis = uiState.settings.focusTimerEndMillis
  val timerRunning = endMillis > liveNowMillis
  LaunchedEffect(timerRunning, endMillis) {
    while (timerRunning && System.currentTimeMillis() < endMillis) {
      liveNowMillis = System.currentTimeMillis()
      delay(1_000L)
    }
    liveNowMillis = System.currentTimeMillis()
  }

  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 18.dp, bottom = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PageSectionHeader(
      indexLabel = "02 / FOCUS & DAILY",
      badgeText = if (uiState.isFocusModeActive) "DEEP FOCUS" else "CALM FLOW",
      badgeActive = uiState.isFocusModeActive,
    )

    // 1. Daily Intention Anchor Card
    val hasIntention = uiState.settings.dailyIntention.isNotBlank()
    BentoGlassCard(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onOpenIntentionDialog()
      },
      accentTint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.testTag("page1_intention_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(
            imageVector = Icons.Outlined.TaskAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "TODAY'S ANCHOR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        BentoPillBadge(
          text = if (hasIntention) "Edit" else "Set",
          highlighted = hasIntention,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .width(3.dp)
              .height(34.dp)
              .clip(CircleShape)
              .background(
                Brush.verticalGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                  )
                )
              ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text =
            if (hasIntention) uiState.settings.dailyIntention
            else "Tap to anchor one quiet priority for today",
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontFamily = PlusJakartaSansFamily,
              fontSize = 17.sp,
              lineHeight = 23.sp,
              fontWeight = if (hasIntention) FontWeight.Medium else FontWeight.Normal,
            ),
          color =
            if (hasIntention) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 2. Focus Session Studio Card with Live Dial Ring
    BentoGlassCard(
      accentTint = if (uiState.isFocusModeActive) MaterialTheme.colorScheme.primary else null,
      modifier = Modifier.testTag("page1_focus_session_card"),
    ) {
      val durationMin = uiState.settings.focusTimerDurationMinutes.coerceIn(5, 90)
      val remainingMs = (endMillis - liveNowMillis).coerceAtLeast(0L)
      val totalMs = (durationMin * 60_000L).coerceAtLeast(1L)
      val progress =
        if (timerRunning) (1f - (remainingMs.toFloat() / totalMs.toFloat())).coerceIn(0.05f, 1f)
        else if (uiState.isFocusModeActive) 1f
        else 0.28f

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        FocusRingDial(
          progress = progress,
          centerText =
            if (timerRunning) "${((remainingMs / 1000L) / 60L).coerceAtLeast(1)}m"
            else "${durationMin}m",
          active = uiState.isFocusModeActive || timerRunning,
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = "FOCUS SESSION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            BentoPillBadge(
              text =
                if (uiState.isFocusModeActive) "${uiState.settings.focusEssentialComponentKeys.size} ESSENTIALS"
                else "STANDBY",
              highlighted = uiState.isFocusModeActive,
            )
          }
          Text(
            text =
              if (timerRunning) "Sprint in progress · stay uninterrupted"
              else if (uiState.isFocusModeActive) "Non-essential apps softly paused"
              else "Quiet timer & distraction shield ready",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HeroFocusTimerBar(
        isFocusModeActive = uiState.isFocusModeActive,
        focusTimerEndMillis = uiState.settings.focusTimerEndMillis,
        defaultDurationMinutes = durationMin,
        hapticEnabled = hapticsEnabled,
        onStartTimer = onStartFocusTimer,
        onStopTimer = onStopFocusTimer,
        onToggleFocusMode = onToggleFocusMode,
        tagPrefix = "page1",
      )
    }

    // 3. Notebook-Ruled Quick Scratchpad Card
    val noteText = uiState.settings.quickScratchpadText
    val ruleColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    BentoGlassCard(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
        onOpenScratchpadDialog()
      },
      modifier = Modifier.testTag("page1_scratchpad_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "QUICK SCRATCHPAD",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        BentoPillBadge(
          text = if (noteText.isNotBlank()) "${noteText.length} chars" else "Jot note",
          highlighted = false,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .drawBehind {
              val stepPx = 22.dp.toPx()
              var y = stepPx
              while (y < size.height) {
                drawLine(
                  color = ruleColor,
                  start = Offset(0f, y),
                  end = Offset(size.width, y),
                  strokeWidth = 1f,
                )
                y += stepPx
              }
            }
            .padding(vertical = 2.dp),
      ) {
        Text(
          text =
            if (noteText.isNotBlank()) noteText
            else "Tap to jot a thought or quick checklist...",
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
          color =
            if (noteText.isNotBlank()) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 4. Mindful Insights Bento Card
    BentoGlassCard(modifier = Modifier.testTag("page1_mindful_insights_card")) {
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
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BentoMetricTile(
          label = "TODAY",
          value = "$totalLaunches",
          unit = "opens",
          highlighted = totalLaunches > 0,
          modifier = Modifier.weight(1f),
        )
        BentoMetricTile(
          label = "GUARDED",
          value = "${uiState.settings.mindfulPausedComponentKeys.size}",
          unit = "apps",
          highlighted = uiState.settings.mindfulPausedComponentKeys.isNotEmpty(),
          modifier = Modifier.weight(1f),
        )
      }

      if (topTodayApps.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          topTodayApps.forEach { app ->
            val ratio = (app.dailyLaunchCount.toFloat() / maxLaunchCount.toFloat()).coerceIn(0.12f, 1f)
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onLaunchApp(app)
                  }
                  .minimumInteractiveComponentSize()
                  .padding(horizontal = 6.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FramedAppIcon(app = app, frameSize = 24.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = app.label,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Box(
                  modifier =
                    Modifier
                      .fillMaxWidth(0.88f)
                      .height(3.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                ) {
                  Box(
                    modifier =
                      Modifier
                        .fillMaxWidth(ratio)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
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
}

@Composable
internal fun HomeAtmosSpacesPage(
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
  val frequentApps =
    remember(uiState.allVisibleApps) {
      uiState.allVisibleApps.sortedByDescending { it.dailyLaunchCount }.take(4)
    }

  val atmosOrbColor =
    when {
      weather.effectType == WeatherEffectType.THUNDERSTORM -> Color(0xFF7B7EFF)
      weather.effectType == WeatherEffectType.RAIN -> Color(0xFF4FB3FF)
      weather.effectType == WeatherEffectType.HEAT_HAZE -> Color(0xFFFF8F52)
      !weather.isDay -> Color(0xFF8A94FF)
      else -> Color(0xFFFFB26B)
    }

  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 18.dp, bottom = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PageSectionHeader(
      indexLabel = "03 / ATMOS & SPACES",
      badgeText = if (weather.isLive) "LIVE SKY" else "CACHED SKY",
      badgeActive = weather.isLive,
    )

    // 1. Atmospheric Horizon Weather Bento Card
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = if (LocalLauncherIsDark.current) 0.74f else 0.88f),
      border = BorderStroke(1.dp, atmosOrbColor.copy(alpha = 0.34f)),
      modifier = Modifier.fillMaxWidth().testTag("page2_weather_card"),
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val orbCenter = Offset(size.width * 0.84f, size.height * 0.22f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    atmosOrbColor.copy(alpha = 0.30f),
                    atmosOrbColor.copy(alpha = 0.08f),
                    Color.Transparent,
                  ),
                center = orbCenter,
                radius = size.width * 0.52f,
              ),
            center = orbCenter,
            radius = size.width * 0.52f,
          )
          val wavePath =
            Path().apply {
              moveTo(0f, size.height * 0.82f)
              cubicTo(
                size.width * 0.32f,
                size.height * 0.72f,
                size.width * 0.68f,
                size.height * 0.90f,
                size.width,
                size.height * 0.78f,
              )
            }
          drawPath(
            path = wavePath,
            color = atmosOrbColor.copy(alpha = 0.16f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
          )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "${weather.temperatureCelsius}°",
                style =
                  MaterialTheme.typography.displayMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                  ),
                color = MaterialTheme.colorScheme.onBackground,
              )
              Column(modifier = Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = weather.locationName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  if (uiState.settings.useDeviceLocation) {
                    Icon(
                      imageVector = Icons.Outlined.MyLocation,
                      contentDescription = "Using device location",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(13.dp),
                    )
                  }
                }
                Text(
                  text = weather.conditionLabel,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            IconButton(
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                onRefreshWeather()
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_weather_refresh_button"),
            ) {
              if (weather.isRefreshing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary,
                )
              } else {
                Icon(
                  imageVector = Icons.Outlined.Refresh,
                  contentDescription = "Refresh weather",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            WeatherTelemetryChip(
              icon = Icons.Outlined.Thermostat,
              label = "FEELS",
              value = "${weather.apparentTemperatureCelsius}°C",
              accent = atmosOrbColor,
              modifier = Modifier.weight(1f),
            )
            WeatherTelemetryChip(
              icon = Icons.Outlined.Air,
              label = "WIND",
              value = "${weather.windSpeedKmh} km/h",
              accent = atmosOrbColor,
              modifier = Modifier.weight(1f),
            )
            WeatherTelemetryChip(
              icon = Icons.Outlined.Cloud,
              label = "CLOUD",
              value = "${weather.cloudCoverPercent}%",
              accent = atmosOrbColor,
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              onShowCityPicker()
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_change_city_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.settings.useDeviceLocation) "Device location · Change" else "Change coastal city",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    // 2. Next Calendar Event Timeline Card
    val nextEvent = uiState.nextCalendarEvent
    if (nextEvent != null) {
      BentoGlassCard(
        onClick = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onOpenCalendar()
        },
        accentTint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("page2_calendar_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
              modifier =
                Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
            )
            Box(
              modifier =
                Modifier
                  .width(2.dp)
                  .height(22.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            )
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "NEXT UP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              BentoPillBadge(
                text = formatRelativeEventTime(nextEvent, currentTimeMillis).uppercase(),
                highlighted = true,
              )
            }
            Text(
              text = nextEvent.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // 3. Smart Spaces 2x2 Bento Quick-Launch Card
    BentoGlassCard(modifier = Modifier.testTag("page2_spaces_card")) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "SMART SPACES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          BentoPillBadge(text = "${uiState.homeFolders.size} folders", highlighted = false)
        }

        OutlinedButton(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
            onAutoCategorizeFolders()
          },
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              contentColor = MaterialTheme.colorScheme.primary,
            ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("page2_auto_group_button"),
        ) {
          Text("Auto-group", style = MaterialTheme.typography.labelMedium)
        }
      }

      if (frequentApps.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          frequentApps.chunked(2).forEach { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              pair.forEach { app ->
                Surface(
                  onClick = {
                    launcherHaptics.trigger(LauncherHapticEvent.APP_LAUNCH)
                    onLaunchApp(app)
                  },
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                  border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    FramedAppIcon(app = app, frameSize = 26.dp)
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = app.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      if (app.dailyLaunchCount > 0) {
                        Text(
                          text = "${app.dailyLaunchCount}× today",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                }
              }
              if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        onClick = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onOpenDrawer(false, null)
        },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("page2_open_drawer_button"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Browse all ${uiState.allVisibleApps.size} apps",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun PageSectionHeader(
  indexLabel: String,
  badgeText: String,
  badgeActive: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = indexLabel,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    BentoPillBadge(text = badgeText, highlighted = badgeActive)
  }
}

@Composable
private fun BentoGlassCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  accentTint: Color? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val isDark = LocalLauncherIsDark.current
  val shape = RoundedCornerShape(16.dp)
  val baseColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.70f else 0.86f)
  val borderColor =
    accentTint?.copy(alpha = 0.34f) ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)

  val innerContent: @Composable () -> Unit = {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(
                (accentTint ?: Color.White).copy(alpha = if (accentTint != null) 0.08f else 0.03f),
                Color.Transparent,
              )
            )
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(15.dp),
        content = content,
      )
    }
  }

  if (onClick != null) {
    Surface(
      onClick = onClick,
      shape = shape,
      color = baseColor,
      border = BorderStroke(1.dp, borderColor),
      modifier = modifier.fillMaxWidth(),
      content = innerContent,
    )
  } else {
    Surface(
      shape = shape,
      color = baseColor,
      border = BorderStroke(1.dp, borderColor),
      modifier = modifier.fillMaxWidth(),
      content = innerContent,
    )
  }
}

@Composable
private fun BentoPillBadge(
  text: String,
  highlighted: Boolean,
) {
  Surface(
    shape = CircleShape,
    color =
      if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border =
      BorderStroke(
        0.8.dp,
        if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
      ),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color =
        if (highlighted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun BentoMetricTile(
  label: String,
  value: String,
  unit: String,
  highlighted: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(11.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
          text = value,
          style =
            MaterialTheme.typography.titleLarge.copy(
              fontFamily = JetBrainsMonoFamily,
              fontWeight = FontWeight.SemiBold,
            ),
          color =
            if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = unit,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 3.dp),
        )
      }
    }
  }
}

@Composable
private fun FocusRingDial(
  progress: Float,
  centerText: String,
  active: Boolean,
) {
  val ringColor =
    if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
  val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
  Box(
    modifier = Modifier.size(52.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val stroke = 4.dp.toPx()
      val pad = stroke / 2f
      val arcSize = Size(size.width - stroke, size.height - stroke)
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
      drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
    }
    Text(
      text = centerText,
      style = GeistMonoNumericStyle.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
    )
  }
}

@Composable
private fun WeatherTelemetryChip(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(11.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(13.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = value,
        style = GeistMonoNumericStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
      )
    }
  }
}
