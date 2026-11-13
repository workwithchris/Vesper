package com.example.data

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppInfo
import com.example.model.AppShortcutItem
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

  private val packageManager: PackageManager = context.packageManager

  private val iconCache =
    object : LruCache<String, CachedIcon>(MAX_CACHED_ICONS) {
      override fun sizeOf(key: String, value: CachedIcon): Int = 1
    }
  private val iconCacheLock = Any()

  private data class CachedIcon(
    val bitmap: ImageBitmap,
    val isAdaptive: Boolean,
  )

  data class InstalledAppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val resolveInfo: ResolveInfo?,
    val cachedIcon: ImageBitmap?,
    val isAdaptiveIcon: Boolean,
    val apkStamp: Long = 0L,
    val isWorkProfile: Boolean = false,
    val userSerialNumber: Long = 0L,
    val launcherActivityInfo: LauncherActivityInfo? = null,
    val appCategory: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED,
    val isSystemApp: Boolean = false,
  ) {
    val componentKey: String
      get() =
        if (userSerialNumber == 0L) "$packageName/$activityName"
        else "$packageName/$activityName#$userSerialNumber"
  }

  private val diskIconDir: File by lazy {
    File(context.cacheDir, "outline_icons").apply { mkdirs() }
  }

  private fun computeApkStamp(info: ResolveInfo): Long {
    val sourceDir = info.activityInfo?.applicationInfo?.sourceDir ?: return 0L
    return runCatching { File(sourceDir).lastModified() }.getOrDefault(0L)
  }

  private fun loadFromDiskCache(componentKey: String, apkStamp: Long): CachedIcon? {
    if (apkStamp <= 0L) return null
    val hash = sha256Short(componentKey)
    val adaptiveFile = File(diskIconDir, "${hash}_${apkStamp}_a.png")
    val legacyFile = File(diskIconDir, "${hash}_${apkStamp}_l.png")
    val targetFile: File
    val isAdaptive: Boolean
    when {
      adaptiveFile.exists() -> {
        targetFile = adaptiveFile
        isAdaptive = true
      }
      legacyFile.exists() -> {
        targetFile = legacyFile
        isAdaptive = false
      }
      else -> return null
    }
    val decoded = runCatching { BitmapFactory.decodeFile(targetFile.absolutePath) }.getOrNull()
      ?: return null
    val imageBitmap = decoded.asImageBitmap()
    val cached = CachedIcon(bitmap = imageBitmap, isAdaptive = isAdaptive)
    synchronized(iconCacheLock) { iconCache.put(componentKey, cached) }
    return cached
  }

  private fun saveToDiskCache(
    componentKey: String,
    apkStamp: Long,
    bitmap: Bitmap,
    isAdaptive: Boolean,
  ) {
    if (apkStamp <= 0L) return
    runCatching {
      val hash = sha256Short(componentKey)
      val suffix = if (isAdaptive) "a" else "l"
      val outFile = File(diskIconDir, "${hash}_${apkStamp}_$suffix.png")
      FileOutputStream(outFile).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      }
      pruneDiskCacheIfNeeded()
    }
  }

  private fun pruneDiskCacheIfNeeded() {
    val files = diskIconDir.listFiles() ?: return
    if (files.size <= MAX_DISK_CACHED_ICONS) return
    files.sortBy { it.lastModified() }
    val deleteCount = files.size - MAX_DISK_CACHED_ICONS
    for (i in 0 until deleteCount) {
      files[i].delete()
    }
  }

  suspend fun loadInstalledAppEntries(): List<InstalledAppEntry> =
    withContext(Dispatchers.IO) {
      val launcherIntent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

      val resolveInfos: List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(0L),
          )
        } else {
          @Suppress("DEPRECATION")
          packageManager.queryIntentActivities(launcherIntent, 0)
        }

      val primaryEntries =
        resolveInfos
          .asSequence()
          .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
          .map { info ->
            val pkg = info.activityInfo.packageName
            val cls = info.activityInfo.name
            val key = "$pkg/$cls"
            val stamp = computeApkStamp(info)
            val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            val cached =
              synchronized(iconCacheLock) { iconCache.get(key) } ?: loadFromDiskCache(key, stamp)
            val appInfo = info.activityInfo.applicationInfo
            val category =
              appInfo?.category ?: android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED
            val systemFlag =
              appInfo != null &&
                ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                  (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            InstalledAppEntry(
              label = label,
              packageName = pkg,
              activityName = cls,
              resolveInfo = info,
              cachedIcon = cached?.bitmap,
              isAdaptiveIcon = cached?.isAdaptive ?: false,
              apkStamp = stamp,
              appCategory = category,
              isSystemApp = systemFlag,
            )
          }

      val profileEntries =
        runCatching {
          val launcherApps =
            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
          val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
          val myHandle = Process.myUserHandle()
          if (launcherApps != null && userManager != null) {
            userManager.userProfiles
              .asSequence()
              .filter { it != myHandle }
              .flatMap { handle ->
                val serial = userManager.getSerialNumberForUser(handle)
                launcherApps.getActivityList(null, handle).asSequence().map { activityInfo ->
                  val component = activityInfo.componentName
                  val key = "${component.packageName}/${component.className}#$serial"
                  val stamp =
                    runCatching {
                      File(activityInfo.applicationInfo.sourceDir).lastModified()
                    }.getOrDefault(0L)
                  val label =
                    activityInfo.label?.toString()?.trim().orEmpty().ifEmpty {
                      component.packageName
                    }
                  val cached =
                    synchronized(iconCacheLock) { iconCache.get(key) }
                      ?: loadFromDiskCache(key, stamp)
                  val appInfo = activityInfo.applicationInfo
                  val systemFlag =
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                      (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                  InstalledAppEntry(
                    label = label,
                    packageName = component.packageName,
                    activityName = component.className,
                    resolveInfo = null,
                    cachedIcon = cached?.bitmap,
                    isAdaptiveIcon = cached?.isAdaptive ?: false,
                    apkStamp = stamp,
                    isWorkProfile = true,
                    userSerialNumber = serial,
                    launcherActivityInfo = activityInfo,
                    appCategory = appInfo.category,
                    isSystemApp = systemFlag,
                  )
                }
              }
          } else {
            emptySequence()
          }
        }.getOrDefault(emptySequence())

      (primaryEntries + profileEntries)
        .distinctBy { it.componentKey }
        .sortedBy { it.label.lowercase() }
        .toList()
    }

  suspend fun loadIconForEntry(entry: InstalledAppEntry): Pair<ImageBitmap, Boolean> =
    withContext(iconLoaderDispatcher) {
      val key = entry.componentKey
      synchronized(iconCacheLock) { iconCache.get(key) }?.let { cached ->
        return@withContext cached.bitmap to cached.isAdaptive
      }
      loadFromDiskCache(key, entry.apkStamp)?.let { diskCached ->
        return@withContext diskCached.bitmap to diskCached.isAdaptive
      }

      val drawable =
        entry.launcherActivityInfo?.getBadgedIcon(0)
          ?: entry.resolveInfo?.loadIcon(packageManager)
          ?: packageManager.defaultActivityIcon
      val isAdaptive = drawable is AdaptiveIconDrawable
      val bitmap = renderConsistentAppIcon(drawable)
      saveToDiskCache(key, entry.apkStamp, bitmap, isAdaptive)
      val imageBitmap = bitmap.asImageBitmap()
      val cached = CachedIcon(bitmap = imageBitmap, isAdaptive = isAdaptive)
      synchronized(iconCacheLock) { iconCache.put(key, cached) }
      imageBitmap to isAdaptive
    }

  suspend fun loadInstalledApps(): List<AppInfo> {
    val entries = loadInstalledAppEntries()
    return entries.map { entry ->
      val (icon, isAdaptive) = loadIconForEntry(entry)
      AppInfo(
        label = entry.label,
        packageName = entry.packageName,
        activityName = entry.activityName,
        icon = icon,
        isAdaptiveIcon = isAdaptive,
        isWorkProfile = entry.isWorkProfile,
        userSerialNumber = entry.userSerialNumber,
        appCategory = entry.appCategory,
        isSystemApp = entry.isSystemApp,
      )
    }
  }

  fun resolveDefaultFavorites(installedApps: List<AppInfo>): List<AppInfo> {
    if (installedApps.isEmpty()) return emptyList()

    val candidates =
      listOfNotNull(
        findAppForIntent(Intent(Intent.ACTION_DIAL), installedApps)
          ?: findByKeywords(installedApps, listOf("phone", "dialer")),
        findAppForIntent(
          Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
          installedApps,
        )
          ?: findAppForIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")), installedApps)
          ?: findByKeywords(installedApps, listOf("messag", "sms", "mms")),
        findAppForIntent(Intent(MediaStore.ACTION_IMAGE_CAPTURE), installedApps)
          ?: findByKeywords(installedApps, listOf("camera")),
        findAppForIntent(
          Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
          installedApps,
        )
          ?: findAppForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            installedApps,
          )
          ?: findByKeywords(installedApps, listOf("chrome", "browser", "firefox")),
        findByKeywords(installedApps, listOf("maps", "navigation")),
        findAppForIntent(
          Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
          installedApps,
        )
          ?: findByKeywords(installedApps, listOf("music", "spotify", "ytmusic", "audio")),
      )

    val unique = candidates.distinctBy { it.componentKey }.toMutableList()
    if (unique.size < 6) {
      for (app in installedApps) {
        if (unique.size >= 6) break
        if (unique.none { it.componentKey == app.componentKey }) {
          unique.add(app)
        }
      }
    }
    return unique
  }

  private fun findAppForIntent(intent: Intent, installedApps: List<AppInfo>): AppInfo? {
    val resolved =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, 0)
      } ?: return null

    val pkg = resolved.activityInfo?.packageName ?: return null
    val cls = resolved.activityInfo?.name
    return installedApps.firstOrNull { it.packageName == pkg && it.activityName == cls }
      ?: installedApps.firstOrNull { it.packageName == pkg }
  }

  private fun findByKeywords(installedApps: List<AppInfo>, keywords: List<String>): AppInfo? {
    return installedApps.firstOrNull { app ->
      val lowerLabel = app.label.lowercase()
      val lowerPkg = app.packageName.lowercase()
      keywords.any { kw -> lowerLabel.contains(kw) || lowerPkg.contains(kw) }
    }
  }

  fun launchApp(app: AppInfo): Boolean {
    if (app.isWorkProfile && app.userSerialNumber != 0L) {
      val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
      val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
      val targetUser = userManager?.getUserForSerialNumber(app.userSerialNumber)
      if (launcherApps != null && targetUser != null) {
        val started =
          runCatching {
            launcherApps.startMainActivity(
              ComponentName(app.packageName, app.activityName),
              targetUser,
              null,
              null,
            )
            true
          }.getOrDefault(false)
        if (started) return true
      }
    }
    return try {
      val intent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
          component = ComponentName(app.packageName, app.activityName)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      val fallback = packageManager.getLaunchIntentForPackage(app.packageName)
      if (fallback != null) {
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallback)
        true
      } else {
        false
      }
    }
  }

  suspend fun importCustomWallpaper(sourceUri: Uri): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
        var sampleSize = 1
        while (maxDim / sampleSize > 1920) {
          sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap =
          context.contentResolver.openInputStream(sourceUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          } ?: return@withContext null
        val outFile = File(context.filesDir, "custom_wallpaper.jpg")
        FileOutputStream(outFile).use { out ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        outFile.absolutePath
      }.getOrNull()
    }

  fun clearCustomWallpaper(existingPath: String?) {
    if (!existingPath.isNullOrBlank()) {
      runCatching { File(existingPath).delete() }
    }
  }

  fun openAppInfo(packageName: String) {
    val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    runCatching { context.startActivity(intent) }
  }

  fun requestUninstallApp(packageName: String) {
    val deleteIntent =
      Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    val launched = runCatching { context.startActivity(deleteIntent) }.isSuccess
    if (!launched) {
      openAppInfo(packageName)
    }
  }

  fun openAppStorePage(packageName: String) {
    val marketIntent =
      Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    val started = runCatching { context.startActivity(marketIntent) }.isSuccess
    if (!started) {
      val webIntent =
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
      runCatching { context.startActivity(webIntent) }
    }
  }

  fun openClockApp() {
    val intent =
      Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    runCatching { context.startActivity(intent) }
  }

  fun openHomeSettings() {
    val intent =
      Intent(Settings.ACTION_HOME_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    runCatching { context.startActivity(intent) }
  }

  fun isDefaultHomeLauncher(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = context.getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
      }
    }
    val homeIntent =
      Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolved =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(homeIntent, 0)
      }
    return resolved?.activityInfo?.packageName == context.packageName
  }

  fun createDefaultHomeRoleIntent(): Intent? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = context.getSystemService(RoleManager::class.java)
      if (roleManager != null &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
      ) {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
      }
    }
    return null
  }

  fun launchCamera(installedApps: List<AppInfo>): Boolean {
    val cameraApp =
      findAppForIntent(Intent(MediaStore.ACTION_IMAGE_CAPTURE), installedApps)
        ?: findByKeywords(installedApps, listOf("camera"))
    if (cameraApp != null) return launchApp(cameraApp)
    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    return runCatching { context.startActivity(intent) }.isSuccess
  }

  fun launchDialer(number: String? = null): Boolean {
    val uri =
      if (number.isNullOrBlank()) Uri.parse("tel:")
      else Uri.parse("tel:${Uri.encode(number.trim())}")
    val intent = Intent(Intent.ACTION_DIAL, uri).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    return runCatching { context.startActivity(intent) }.isSuccess
  }

  fun openUrl(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    return runCatching { context.startActivity(intent) }.isSuccess
  }

  suspend fun loadAppShortcuts(appLabelsByPkg: Map<String, String>): List<AppShortcutItem> =
    withContext(Dispatchers.IO) {
      val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
          ?: return@withContext emptyList()
      if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()
      runCatching {
        val query =
          LauncherApps.ShortcutQuery().apply {
            setQueryFlags(
              LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
          }
        val list = launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
        list
          .asSequence()
          .filter { it.isEnabled && !it.shortLabel.isNullOrBlank() }
          .map { info ->
            val pkg = info.`package`
            AppShortcutItem(
              id = info.id,
              packageName = pkg,
              shortLabel = info.shortLabel.toString(),
              appLabel = appLabelsByPkg[pkg] ?: pkg,
            )
          }
          .distinctBy { "${it.packageName}:${it.id}" }
          .take(80)
          .toList()
      }.getOrDefault(emptyList())
    }

  fun launchShortcut(shortcut: AppShortcutItem): Boolean {
    val launcherApps =
      context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return false
    if (!launcherApps.hasShortcutHostPermission()) return false
    return runCatching {
      launcherApps.startShortcut(
        shortcut.packageName,
        shortcut.id,
        null,
        null,
        Process.myUserHandle(),
      )
      true
    }.getOrDefault(false)
  }

  fun openWebSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    val searchIntent =
      Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(android.app.SearchManager.QUERY, trimmed)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    val started = runCatching { context.startActivity(searchIntent) }.isSuccess
    if (!started) {
      val browserIntent =
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://www.google.com/search?q=${Uri.encode(trimmed)}"),
        ).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
      runCatching { context.startActivity(browserIntent) }
    }
  }

  @Suppress("WrongConstant", "PrivateApi")
  fun expandNotificationShade() {
    runCatching {
      val statusBarService = context.getSystemService("statusbar") ?: return
      val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
      val expandMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
      expandMethod.invoke(statusBarService)
    }
  }

  companion object {
    const val ICON_BITMAP_SIZE_PX = 96
    private const val MAX_CACHED_ICONS = 160
    private const val MAX_DISK_CACHED_ICONS = 220

    internal fun sha256Short(value: String): String {
      val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
      return buildString(16) {
        for (i in 0 until 8) {
          append("%02x".format(digest[i]))
        }
      }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val iconLoaderDispatcher = Dispatchers.Default.limitedParallelism(2)

    private class ScratchBuffers(size: Int) {
      val total = size * size
      val pixels = IntArray(total)
      val contourStrength = FloatArray(total)
      val internalEdges = FloatArray(total)
      val dilatedEdges = FloatArray(total)
      val outputPixels = IntArray(total)
      val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      val fallbackStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          style = Paint.Style.STROKE
          strokeWidth = 3.5f
        }

      fun clear() {
        contourStrength.fill(0f)
        internalEdges.fill(0f)
        dilatedEdges.fill(0f)
      }
    }

    private val RGB_SHIFTS = intArrayOf(16, 8, 0)

    private val scratchBuffersLocal =
      object : ThreadLocal<ScratchBuffers>() {
        override fun initialValue(): ScratchBuffers = ScratchBuffers(ICON_BITMAP_SIZE_PX)
      }

    fun renderConsistentAppIcon(drawable: Drawable): Bitmap {
      val size = ICON_BITMAP_SIZE_PX
      val buffers = scratchBuffersLocal.get() ?: ScratchBuffers(size)
      buffers.clear()

      val normalizedGlyph =
        extractNormalizedGlyphBitmap(drawable, size, buffers.pixels, buffers.bitmapPaint)
      normalizedGlyph.getPixels(buffers.pixels, 0, size, 0, 0, size, size)
      normalizedGlyph.recycle()

      val pixels = buffers.pixels
      val contourStrength = buffers.contourStrength
      val internalEdges = buffers.internalEdges
      val dilatedEdges = buffers.dilatedEdges
      val outputPixels = buffers.outputPixels

      val contourRadius = 2
      for (y in 0 until size) {
        val rowOffset = y * size
        for (x in 0 until size) {
          val idx = rowOffset + x
          val alpha = (pixels[idx] ushr 24) and 0xFF
          if (alpha <= 80) continue

          var minNeighborAlpha = 255
          val minDy = (y - contourRadius).coerceAtLeast(0)
          val maxDy = (y + contourRadius).coerceAtMost(size - 1)
          val minDx = (x - contourRadius).coerceAtLeast(0)
          val maxDx = (x + contourRadius).coerceAtMost(size - 1)
          for (ny in minDy..maxDy) {
            val dy = ny - y
            val dySq = dy * dy
            val nRow = ny * size
            for (nx in minDx..maxDx) {
              val dx = nx - x
              if (dx * dx + dySq <= contourRadius * contourRadius + 1) {
                val na = (pixels[nRow + nx] ushr 24) and 0xFF
                if (na < minNeighborAlpha) {
                  minNeighborAlpha = na
                  if (minNeighborAlpha == 0) break
                }
              }
            }
            if (minNeighborAlpha == 0) break
          }

          if (minNeighborAlpha < 84) {
            contourStrength[idx] = ((alpha - minNeighborAlpha) / 210f).coerceIn(0f, 1f)
          }

          if (x in 1 until size - 1 && y in 1 until size - 1) {
            var opaqueWindow = true
            for (ky in -1..1) {
              val kRow = (y + ky) * size
              for (kx in -1..1) {
                if (((pixels[kRow + x + kx] ushr 24) and 0xFF) < 150) {
                  opaqueWindow = false
                  break
                }
              }
              if (!opaqueWindow) break
            }
            if (opaqueWindow) {
              val edgeMag = maxChannelSobelMagnitude(pixels, size, x, y)
              val normalizedEdge = ((edgeMag - 135f) / 145f).coerceIn(0f, 1f)
              if (normalizedEdge > 0f) {
                internalEdges[idx] = normalizedEdge
              }
            }
          }
        }
      }

      for (y in 0 until size) {
        val rowOffset = y * size
        for (x in 0 until size) {
          val e = internalEdges[rowOffset + x]
          if (e <= 0.08f) continue
          val minY = (y - 1).coerceAtLeast(0)
          val maxY = (y + 1).coerceAtMost(size - 1)
          val minX = (x - 1).coerceAtLeast(0)
          val maxX = (x + 1).coerceAtMost(size - 1)
          for (ny in minY..maxY) {
            val dy = ny - y
            val nRow = ny * size
            for (nx in minX..maxX) {
              val dx = nx - x
              val distSq = dx * dx + dy * dy
              val falloff = if (distSq == 0) 1f else if (distSq == 1) 0.85f else 0.55f
              val candidate = (e * falloff).coerceIn(0f, 1f)
              val targetIdx = nRow + nx
              if (candidate > dilatedEdges[targetIdx]) {
                dilatedEdges[targetIdx] = candidate
              }
            }
          }
        }
      }

      var litPixelCount = 0
      for (i in outputPixels.indices) {
        val alphaFloat = maxOf(contourStrength[i], dilatedEdges[i]).coerceIn(0f, 1f)
        val alphaByte = (alphaFloat * 255f).toInt().coerceIn(0, 255)
        if (alphaByte > 24) litPixelCount++
        outputPixels[i] = Color.argb(alphaByte, 255, 255, 255)
      }

      val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      if (litPixelCount > 10) {
        output.setPixels(outputPixels, 0, size, 0, 0, size, size)
      } else {
        val canvas = Canvas(output)
        val inset = size * 0.16f
        canvas.drawRoundRect(
          RectF(inset, inset, size - inset, size - inset),
          size * 0.18f,
          size * 0.18f,
          buffers.fallbackStrokePaint,
        )
      }
      return output
    }

    private fun extractNormalizedGlyphBitmap(
      drawable: Drawable,
      size: Int,
      scratchPixels: IntArray,
      bitmapPaint: Paint,
    ): Bitmap {
      if (drawable is AdaptiveIconDrawable) {
        val layerBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)
        val bleed = (size * 0.25f).toInt()
        val layerBounds = Rect(-bleed, -bleed, size + bleed, size + bleed)
        val mono =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawable.monochrome
          } else {
            null
          }
        val targetLayer = mono ?: drawable.foreground
        if (targetLayer != null) {
          targetLayer.bounds = layerBounds
          targetLayer.draw(layerCanvas)
        } else {
          drawable.background?.let { bg ->
            bg.bounds = layerBounds
            bg.draw(layerCanvas)
          }
        }

        if (hasTransparentCorners(layerBitmap)) {
          val bounds = findOpaqueBounds(layerBitmap, scratchPixels)
          val scaled =
            centerScaleBounds(
              source = layerBitmap,
              contentBounds = bounds,
              size = size,
              targetFillFraction = 0.78f,
              bitmapPaint = bitmapPaint,
            )
          layerBitmap.recycle()
          return scaled
        }

        layerBitmap.recycle()
        val combined = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val combinedCanvas = Canvas(combined)
        drawable.background?.let { bg ->
          bg.bounds = layerBounds
          bg.draw(combinedCanvas)
        }
        drawable.foreground?.let { fg ->
          fg.bounds = layerBounds
          fg.draw(combinedCanvas)
        }
        return combined
      }

      val rawBitmap = drawable.toBitmap(width = size, height = size, config = Bitmap.Config.ARGB_8888)
      val contentBounds = findOpaqueBounds(rawBitmap, scratchPixels)
      val scaled =
        centerScaleBounds(
          source = rawBitmap,
          contentBounds = contentBounds,
          size = size,
          targetFillFraction = 0.78f,
          bitmapPaint = bitmapPaint,
        )
      rawBitmap.recycle()
      return scaled
    }

    private fun centerScaleBounds(
      source: Bitmap,
      contentBounds: Rect,
      size: Int,
      targetFillFraction: Float,
      bitmapPaint: Paint,
    ): Bitmap {
      val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(output)
      val targetContentSize = size * targetFillFraction
      val srcWidth = contentBounds.width().coerceAtLeast(1)
      val srcHeight = contentBounds.height().coerceAtLeast(1)
      val scale = minOf(targetContentSize / srcWidth, targetContentSize / srcHeight)
      val dstWidth = srcWidth * scale
      val dstHeight = srcHeight * scale
      val dstLeft = (size - dstWidth) * 0.5f
      val dstTop = (size - dstHeight) * 0.5f
      val dstRect = RectF(dstLeft, dstTop, dstLeft + dstWidth, dstTop + dstHeight)
      canvas.drawBitmap(source, contentBounds, dstRect, bitmapPaint)
      return output
    }

    private fun hasTransparentCorners(bitmap: Bitmap): Boolean {
      val w = bitmap.width
      val h = bitmap.height
      val x0 = 2.coerceIn(0, w - 1)
      val x1 = (w - 3).coerceIn(0, w - 1)
      val y0 = 2.coerceIn(0, h - 1)
      val y1 = (h - 3).coerceIn(0, h - 1)
      return ((bitmap.getPixel(x0, y0) ushr 24) and 0xFF) < 48 ||
        ((bitmap.getPixel(x1, y0) ushr 24) and 0xFF) < 48 ||
        ((bitmap.getPixel(x0, y1) ushr 24) and 0xFF) < 48 ||
        ((bitmap.getPixel(x1, y1) ushr 24) and 0xFF) < 48
    }

    private fun maxChannelSobelMagnitude(pixels: IntArray, width: Int, x: Int, y: Int): Float {
      val p00 = pixels[(y - 1) * width + (x - 1)]
      val p01 = pixels[(y - 1) * width + x]
      val p02 = pixels[(y - 1) * width + (x + 1)]
      val p10 = pixels[y * width + (x - 1)]
      val p12 = pixels[y * width + (x + 1)]
      val p20 = pixels[(y + 1) * width + (x - 1)]
      val p21 = pixels[(y + 1) * width + x]
      val p22 = pixels[(y + 1) * width + (x + 1)]

      var best = 0f
      for (shift in RGB_SHIFTS) {
        val c00 = (p00 ushr shift) and 0xFF
        val c01 = (p01 ushr shift) and 0xFF
        val c02 = (p02 ushr shift) and 0xFF
        val c10 = (p10 ushr shift) and 0xFF
        val c12 = (p12 ushr shift) and 0xFF
        val c20 = (p20 ushr shift) and 0xFF
        val c21 = (p21 ushr shift) and 0xFF
        val c22 = (p22 ushr shift) and 0xFF

        val gx = (-c00 + c02 - 2 * c10 + 2 * c12 - c20 + c22).toFloat()
        val gy = (-c00 - 2 * c01 - c02 + c20 + 2 * c21 + c22).toFloat()
        val mag = kotlin.math.hypot(gx, gy)
        if (mag > best) best = mag
      }
      return best
    }

    private fun findOpaqueBounds(bitmap: Bitmap, pixels: IntArray): Rect {
      val w = bitmap.width
      val h = bitmap.height
      bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

      var minX = w
      var minY = h
      var maxX = -1
      var maxY = -1

      for (y in 0 until h) {
        val rowOffset = y * w
        for (x in 0 until w) {
          val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
          if (alpha > 20) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
          }
        }
      }

      return if (maxX < minX || maxY < minY) {
        Rect(0, 0, w, h)
      } else {
        Rect(minX, minY, maxX + 1, maxY + 1)
      }
    }
  }
}
