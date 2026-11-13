package com.example.ui.onboarding

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AccentOption
import com.example.model.AdaptiveIconShape
import com.example.model.AppIconShapeProfile
import com.example.model.AppInfo
import com.example.model.ClockStyle
import com.example.ui.components.FramedAppIcon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLauncherIsDark
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.util.LauncherHapticEvent
import com.example.util.rememberLauncherHaptics
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 4

@Composable
fun OnboardingWelcomeScreen(
  allApps: List<AppInfo>,
  initialAccent: AccentOption,
  initialShape: AdaptiveIconShape,
  initialFavorites: List<AppInfo>,
  onSelectAccent: (AccentOption) -> Unit,
  onSelectShape: (AdaptiveIconShape) -> Unit,
  onFinish: (selectedFavorites: List<AppInfo>) -> Unit,
  onSkip: () -> Unit,
  initialClockStyle: ClockStyle = ClockStyle.MONO_TERMINAL,
  initialRespectIndividualShapes: Boolean = true,
  initialUseSystemWallpaper: Boolean = true,
  isDefaultLauncher: Boolean = false,
  reducedMotion: Boolean = false,
  onSelectClockStyle: (ClockStyle) -> Unit = {},
  onToggleRespectIndividualShapes: (Boolean) -> Unit = {},
  onUsePhoneSystemWallpaper: () -> Unit = {},
  onApplyMiamiSunsetWallpaper: () -> Unit = {},
  onCreateDefaultHomeRoleIntent: () -> Intent? = { null },
  onOpenHomeLauncherSettings: () -> Unit = {},
  onDefaultLauncherResult: () -> Unit = {},
) {
  val launcherHaptics = rememberLauncherHaptics()
  val coroutineScope = rememberCoroutineScope()
  val pagerState = rememberPagerState(initialPage = 0) { ONBOARDING_PAGE_COUNT }

  var selectedKeys by remember(initialFavorites) {
    mutableStateOf(initialFavorites.map { it.componentKey }.toSet())
  }

  val defaultHomeLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      onDefaultLauncherResult()
    }

  fun goToPage(target: Int) {
    val clamped = target.coerceIn(0, ONBOARDING_PAGE_COUNT - 1)
    coroutineScope.launch {
      if (reducedMotion) pagerState.scrollToPage(clamped)
      else pagerState.animateScrollToPage(clamped, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    }
  }

  BackHandler {
    if (pagerState.currentPage > 0) {
      goToPage(pagerState.currentPage - 1)
    } else {
      onSkip()
    }
  }

  val pageProgress =
    (pagerState.currentPage + pagerState.currentPageOffsetFraction)
      .coerceIn(0f, (ONBOARDING_PAGE_COUNT - 1).toFloat())

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(DarkBackground)
        .testTag("onboarding_dialog"),
    contentAlignment = Alignment.TopCenter,
  ) {
    OnboardingAtmosphericBackdrop(
      pageProgress = pageProgress,
      accentColor = MaterialTheme.colorScheme.primary,
      reducedMotion = reducedMotion,
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp)
          .statusBarsPadding()
          .navigationBarsPadding(),
    ) {
      OnboardingTopHeader(
        currentPage = pagerState.currentPage,
        onSelectPage = { page ->
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          goToPage(page)
        },
        onSkip = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          onSkip()
        },
      )

      HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("onboarding_pager"),
      ) { page ->
        when (page) {
          0 -> OnboardingWelcomeStep()
          1 ->
            OnboardingAtmosphereStep(
              sampleApps = allApps,
              accent = initialAccent,
              iconShape = initialShape,
              respectIndividualShapes = initialRespectIndividualShapes,
              clockStyle = initialClockStyle,
              useSystemWallpaper = initialUseSystemWallpaper,
              onSelectAccent = onSelectAccent,
              onSelectShape = onSelectShape,
              onToggleRespectIndividualShapes = onToggleRespectIndividualShapes,
              onSelectClockStyle = onSelectClockStyle,
              onUsePhoneSystemWallpaper = onUsePhoneSystemWallpaper,
              onApplyMiamiSunsetWallpaper = onApplyMiamiSunsetWallpaper,
            )
          2 ->
            OnboardingFavoritesStep(
              allApps = allApps,
              selectedKeys = selectedKeys,
              adaptiveShape = initialShape,
              respectIndividualShapes = initialRespectIndividualShapes,
              onToggleApp = { app ->
                selectedKeys =
                  if (app.componentKey in selectedKeys) selectedKeys - app.componentKey
                  else selectedKeys + app.componentKey
              },
              onAutoPick = {
                selectedKeys = allApps.take(5).map { it.componentKey }.toSet()
              },
              onClearSelection = { selectedKeys = emptySet() },
            )
          else ->
            OnboardingReadyStep(
              isDefaultLauncher = isDefaultLauncher,
              selectedFavoritesCount = selectedKeys.size,
              onRequestDefaultHome = {
                launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
                val roleIntent = onCreateDefaultHomeRoleIntent()
                if (roleIntent != null) {
                  runCatching { defaultHomeLauncher.launch(roleIntent) }
                    .onFailure { onOpenHomeLauncherSettings() }
                } else {
                  onOpenHomeLauncherSettings()
                }
              },
            )
        }
      }

      OnboardingFooterBar(
        currentPage = pagerState.currentPage,
        onBack = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          goToPage(pagerState.currentPage - 1)
        },
        onNext = {
          launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
          goToPage(pagerState.currentPage + 1)
        },
        onFinish = {
          launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
          onFinish(allApps.filter { it.componentKey in selectedKeys })
        },
      )
    }
  }
}

@Composable
private fun OnboardingAtmosphericBackdrop(
  pageProgress: Float,
  accentColor: Color,
  reducedMotion: Boolean,
) {
  val glowPulse =
    if (reducedMotion) {
      0.5f
    } else {
      val transition = rememberInfiniteTransition(label = "onboarding_glow")
      val pulse by
        transition.animateFloat(
          initialValue = 0.35f,
          targetValue = 0.72f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 5600),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "onboarding_pulse",
        )
      pulse
    }

  Box(modifier = Modifier.fillMaxSize()) {
    Image(
      painter = painterResource(id = R.drawable.img_miami_sunset_hero_1794549247807),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = 1.16f
            scaleY = 1.16f
            val normalized = (pageProgress / (ONBOARDING_PAGE_COUNT - 1).toFloat()) - 0.5f
            translationX = if (reducedMotion) 0f else -normalized * 48f
          },
      alpha = 0.42f,
    )

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops =
                arrayOf(
                  0.0f to Color(0xCC06070B),
                  0.38f to Color(0x99080A12),
                  0.78f to Color(0xE807080D),
                  1.0f to Color(0xFA050609),
                ),
            )
          ),
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
      val horizonCenter =
        Offset(
          x = size.width * (0.28f + 0.14f * pageProgress),
          y = size.height * 0.24f,
        )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                accentColor.copy(alpha = 0.22f * glowPulse),
                accentColor.copy(alpha = 0.06f * glowPulse),
                Color.Transparent,
              ),
            center = horizonCenter,
            radius = size.maxDimension * 0.52f,
          ),
        center = horizonCenter,
        radius = size.maxDimension * 0.52f,
      )
    }
  }
}

@Composable
private fun OnboardingTopHeader(
  currentPage: Int,
  onSelectPage: (Int) -> Unit,
  onSkip: () -> Unit,
) {
  val stepLabels = remember { listOf("Welcome", "Atmosphere", "Favorites", "Launchpad") }
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier =
            Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
              .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
          )
        }
        Column {
          Text(
            text = "VESPER",
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.SemiBold,
              ),
            color = Color.White.copy(alpha = 0.92f),
          )
          Text(
            text = "0${currentPage + 1} / ${stepLabels.getOrElse(currentPage) { "Setup" }}",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = Color.White.copy(alpha = 0.58f),
          )
        }
      }

      TextButton(
        onClick = onSkip,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_skip_button"),
      ) {
        Text(
          text = "Skip",
          style = MaterialTheme.typography.labelLarge,
          color = Color.White.copy(alpha = 0.72f),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      for (index in 0 until ONBOARDING_PAGE_COUNT) {
        val active = index == currentPage
        val completed = index < currentPage
        val barColor by
          animateColorAsState(
            targetValue =
              when {
                active -> MaterialTheme.colorScheme.primary
                completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                else -> Color.White.copy(alpha = 0.16f)
              },
            animationSpec = tween(220),
            label = "step_bar_color_$index",
          )
        Box(
          modifier =
            Modifier
              .weight(if (active) 1.5f else 1f)
              .height(4.dp)
              .clip(CircleShape)
              .background(barColor)
              .clickable { onSelectPage(index) },
        )
      }
    }
  }
}

@Composable
private fun OnboardingWelcomeStep() {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xAA0C0F18),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9.5f)) {
        Image(
          painter = painterResource(id = R.drawable.img_miami_sunset_hero_1794549247807),
          contentDescription = "Vesper sunset horizon preview",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color(0x22000000), Color(0xBB07080D)),
                )
              ),
        )
        Row(
          modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = CircleShape,
            color = Color(0x99090B10),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
          ) {
            Text(
              text = "TWILIGHT LAUNCHER",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, letterSpacing = 1.sp),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
          Text(
            text = "9:41",
            style =
              MaterialTheme.typography.displayMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Light,
                fontSize = 36.sp,
              ),
            color = Color.White,
          )
          Text(
            text = "Evening calm · Zero badge clutter",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.78f),
          )
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "Your phone, unhurried.",
        style =
          MaterialTheme.typography.headlineLarge.copy(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 33.sp,
          ),
        color = Color.White,
      )
      Text(
        text =
          "Vesper replaces noisy app grids with twilight depth, adaptive silhouettes matched to your device, and quiet focus spaces.",
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        color = Color.White.copy(alpha = 0.74f),
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      OnboardingFeatureCard(
        icon = Icons.Outlined.Water,
        title = "Parallax Sunset & Wallpaper Sync",
        subtitle = "Mirror your phone's wallpaper or glide through animated dusk horizons.",
      )
      OnboardingFeatureCard(
        icon = Icons.Outlined.AutoAwesome,
        title = "System-Wide Adaptive Icon Shapes",
        subtitle = "Respects every app's silhouette cleanly with optional unified masking.",
      )
      OnboardingFeatureCard(
        icon = Icons.Outlined.SelfImprovement,
        title = "Mindful Focus & Instant Drawer",
        subtitle = "Focus schedules, timed sprints, swipe gestures, and quick math search.",
      )
    }
  }
}

@Composable
private fun OnboardingFeatureCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0x99111420),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
          color = Color.White,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.66f),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingAtmosphereStep(
  sampleApps: List<AppInfo>,
  accent: AccentOption,
  iconShape: AdaptiveIconShape,
  respectIndividualShapes: Boolean,
  clockStyle: ClockStyle,
  useSystemWallpaper: Boolean,
  onSelectAccent: (AccentOption) -> Unit,
  onSelectShape: (AdaptiveIconShape) -> Unit,
  onToggleRespectIndividualShapes: (Boolean) -> Unit,
  onSelectClockStyle: (ClockStyle) -> Unit,
  onUsePhoneSystemWallpaper: () -> Unit,
  onApplyMiamiSunsetWallpaper: () -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  val isDarkTheme = LocalLauncherIsDark.current

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Craft your atmosphere",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
      Text(
        text = "Changes preview live below and can be tuned anytime in Settings.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.70f),
      )
    }

    // Live Preview Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xBB0D1019),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_live_preview"),
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        if (!useSystemWallpaper) {
          Image(
            painter = painterResource(id = R.drawable.img_miami_sunset_hero_1794549247807),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.32f,
            modifier = Modifier.matchParentSize(),
          )
        }
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                when (clockStyle) {
                  ClockStyle.MONO_TERMINAL -> "> 08:55"
                  ClockStyle.MINIMAL_EDITORIAL -> "8:55"
                  ClockStyle.NEON_HORIZON -> "08:55"
                },
              style =
                MaterialTheme.typography.headlineLarge.copy(
                  fontFamily =
                    when (clockStyle) {
                      ClockStyle.MONO_TERMINAL -> JetBrainsMonoFamily
                      ClockStyle.MINIMAL_EDITORIAL -> PlusJakartaSansFamily
                      ClockStyle.NEON_HORIZON -> JetBrainsMonoFamily
                    },
                  fontWeight =
                    if (clockStyle == ClockStyle.MINIMAL_EDITORIAL) FontWeight.Light
                    else FontWeight.SemiBold,
                  letterSpacing = if (clockStyle == ClockStyle.NEON_HORIZON) 2.5.sp else 0.sp,
                ),
              color = if (clockStyle == ClockStyle.NEON_HORIZON) MaterialTheme.colorScheme.primary else Color.White,
            )
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            ) {
              Text(
                text = accent.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          val previewList =
            remember(sampleApps) {
              sampleApps.take(3).ifEmpty {
                listOf(
                  AppInfo("Calendar", "com.preview.cal", "Main", null, shapeProfile = AppIconShapeProfile.SYSTEM_ADAPTIVE),
                  AppInfo("Music", "com.preview.music", "Main", null, shapeProfile = AppIconShapeProfile.CIRCULAR),
                  AppInfo("Notes", "com.preview.notes", "Main", null, shapeProfile = AppIconShapeProfile.ROUNDED_RECT),
                )
              }
            }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            previewList.forEach { app ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FramedAppIcon(
                  app = app,
                  frameSize = 28.dp,
                  adaptiveShape = iconShape,
                  respectIndividualShapes = respectIndividualShapes,
                )
                Text(
                  text = app.label,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.88f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    // Accent Selector
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "SIGNATURE ACCENT",
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
        color = Color.White.copy(alpha = 0.68f),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AccentOption.entries.forEach { option ->
          val selected = option == accent
          val swatchSize by animateDpAsState(if (selected) 38.dp else 32.dp, label = "accent_size_${option.name}")
          Box(
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable {
                  launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                  onSelectAccent(option)
                }
                .semantics { contentDescription = "${option.label} accent" }
                .testTag("onboarding_accent_${option.name.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier
                  .size(swatchSize)
                  .clip(CircleShape)
                  .background(option.resolvedPrimary(isDarkTheme))
                  .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                    shape = CircleShape,
                  ),
            )
          }
        }
      }
    }

    // Wallpaper Source
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "BACKDROP SOURCE",
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
        color = Color.White.copy(alpha = 0.68f),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OnboardingChoiceTile(
          title = "Phone Wallpaper",
          subtitle = "Sync system",
          icon = Icons.Outlined.Wallpaper,
          selected = useSystemWallpaper,
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            onUsePhoneSystemWallpaper()
          },
          modifier = Modifier.weight(1f).testTag("onboarding_wallpaper_system"),
        )
        OnboardingChoiceTile(
          title = "Miami Sunset",
          subtitle = "Built-in vista",
          icon = Icons.Outlined.Palette,
          selected = !useSystemWallpaper,
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            onApplyMiamiSunsetWallpaper()
          },
          modifier = Modifier.weight(1f).testTag("onboarding_wallpaper_sunset"),
        )
      }
    }

    // Clock Style
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "CLOCK TYPOGRAPHY",
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
        color = Color.White.copy(alpha = 0.68f),
      )
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ClockStyle.entries.forEach { style ->
          val selected = style == clockStyle
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              onSelectClockStyle(style)
            },
            border =
              BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
              ),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                  else Color(0x55111420),
                contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.White,
              ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("onboarding_clock_${style.name.lowercase()}"),
          ) {
            Text(text = style.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
          }
        }
      }
    }

    // Icon Shape & Individual Respect
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "ADAPTIVE ICON SHAPE",
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
        color = Color.White.copy(alpha = 0.68f),
      )
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AdaptiveIconShape.entries.forEach { shape ->
          val selected = shape == iconShape
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              onSelectShape(shape)
            },
            border =
              BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
              ),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor =
                  if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                  else Color(0x55111420),
                contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.White,
              ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("onboarding_shape_${shape.name.lowercase()}"),
          ) {
            Text(
              text = shape.label,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x88111420),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Respect individual app shapes",
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
            )
            Text(
              text = "Preserve custom & circular silhouettes instead of uniform cropping.",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.64f),
            )
          }
          Switch(
            checked = respectIndividualShapes,
            onCheckedChange = {
              launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
              onToggleRespectIndividualShapes(it)
            },
            modifier = Modifier.testTag("onboarding_respect_shapes_switch"),
          )
        }
      }
    }
  }
}

@Composable
private fun OnboardingChoiceTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
      else Color(0x88111420),
    border =
      BorderStroke(
        1.dp,
        if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
      ),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.72f),
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.62f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun OnboardingFavoritesStep(
  allApps: List<AppInfo>,
  selectedKeys: Set<String>,
  adaptiveShape: AdaptiveIconShape,
  respectIndividualShapes: Boolean,
  onToggleApp: (AppInfo) -> Unit,
  onAutoPick: () -> Unit,
  onClearSelection: () -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(allApps, query) {
      val trimmed = query.trim()
      if (trimmed.isEmpty()) allApps
      else allApps.filter { it.label.contains(trimmed, ignoreCase = true) }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("onboarding_favorites_list"),
    contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "favorites_header") {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Curate your quiet dock",
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
        )
        Text(
          text = "Keep only the essentials on your Home screen. Everything else lives a swipe away.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.70f),
        )
      }
    }

    item(key = "favorites_actions") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        ) {
          Text(
            text = "${selectedKeys.size} selected",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              onAutoPick()
            },
            modifier = Modifier.testTag("onboarding_autopick_button"),
          ) {
            Text("Auto-pick", color = MaterialTheme.colorScheme.primary)
          }
          if (selectedKeys.isNotEmpty()) {
            TextButton(
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                onClearSelection()
              },
              modifier = Modifier.testTag("onboarding_clear_favorites_button"),
            ) {
              Text("Clear", color = Color.White.copy(alpha = 0.72f))
            }
          }
        }
      }
    }

    item(key = "favorites_filter") {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        singleLine = true,
        placeholder = { Text("Filter apps...", color = Color.White.copy(alpha = 0.52f)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f)) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Outlined.Close, contentDescription = "Clear filter", tint = Color.White.copy(alpha = 0.72f))
            }
          }
        },
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
            focusedContainerColor = Color(0xAA111420),
            unfocusedContainerColor = Color(0x88111420),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("onboarding_favorites_filter_input"),
      )
    }

    items(filtered, key = { it.componentKey }) { app ->
        val checked = app.componentKey in selectedKeys
        Surface(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
            onToggleApp(app)
          },
          shape = RoundedCornerShape(12.dp),
          color =
            if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            else Color(0x80111420),
          border =
            BorderStroke(
              1.dp,
              if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
              else Color.White.copy(alpha = 0.08f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_app_pick_${app.packageName}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FramedAppIcon(
              app = app,
              frameSize = 36.dp,
              adaptiveShape = adaptiveShape,
              respectIndividualShapes = respectIndividualShapes,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = app.label,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (checked) {
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Check,
                  contentDescription = "Selected",
                  tint = Color.Black,
                  modifier = Modifier.size(15.dp),
                )
              }
            }
          }
      }
    }
  }
}

@Composable
private fun OnboardingReadyStep(
  isDefaultLauncher: Boolean,
  selectedFavoritesCount: Int,
  onRequestDefaultHome: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Ready for takeoff",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
      Text(
        text =
          if (selectedFavoritesCount > 0) "$selectedFavoritesCount favorites queued for your twilight dock."
          else "Your home screen is clean and ready whenever you are.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.70f),
      )
    }

    // Set as Default Home Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color =
        if (isDefaultLauncher) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else Color(0xBB111420),
      border =
        BorderStroke(
          1.dp,
          if (isDefaultLauncher) MaterialTheme.colorScheme.primary
          else Color.White.copy(alpha = 0.14f),
        ),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_default_home_card"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (isDefaultLauncher) Icons.Outlined.Check else Icons.Outlined.Home,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isDefaultLauncher) "Default Home Active" else "Make Vesper your Home",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color.White,
            )
            Text(
              text =
                if (isDefaultLauncher) "Pressing the Home gesture returns straight to Vesper."
                else "Set Vesper as your default Android launcher so Home always opens here.",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.70f),
            )
          }
        }
        if (!isDefaultLauncher) {
          Button(
            onClick = onRequestDefaultHome,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("onboarding_set_default_button"),
          ) {
            Text("Set as Default Home", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    Text(
      text = "GESTURES AT A GLANCE",
      style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
      color = Color.White.copy(alpha = 0.68f),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OnboardingGestureCard(
          icon = Icons.Outlined.KeyboardArrowUp,
          gesture = "Swipe up or tap ^",
          result = "All Apps & Search",
          modifier = Modifier.weight(1f),
        )
        OnboardingGestureCard(
          icon = Icons.Outlined.SwapHoriz,
          gesture = "Swipe left / right",
          result = "Focus & Atmos pages",
          modifier = Modifier.weight(1f),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OnboardingGestureCard(
          icon = Icons.Outlined.KeyboardArrowDown,
          gesture = "Swipe down",
          result = "Notifications shade",
          modifier = Modifier.weight(1f),
        )
        OnboardingGestureCard(
          icon = Icons.Outlined.TouchApp,
          gesture = "Long-press app",
          result = "Rename, Pause, Folder",
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun OnboardingGestureCard(
  icon: ImageVector,
  gesture: String,
  result: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0x88111420),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = gesture,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = result,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.64f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun OnboardingFooterBar(
  currentPage: Int,
  onBack: () -> Unit,
  onNext: () -> Unit,
  onFinish: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (currentPage > 0) {
      OutlinedButton(
        onClick = onBack,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Back")
      }
    } else {
      Spacer(modifier = Modifier.width(1.dp))
    }

    if (currentPage < ONBOARDING_PAGE_COUNT - 1) {
      Button(
        onClick = onNext,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_next_button"),
      ) {
        Text(
          text = if (currentPage == 0) "Get started" else "Continue",
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    } else {
      Button(
        onClick = onFinish,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_finish_button"),
      ) {
        Text("Enter Vesper", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}
