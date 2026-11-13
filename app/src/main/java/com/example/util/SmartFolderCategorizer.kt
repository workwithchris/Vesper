package com.example.util

import android.content.pm.ApplicationInfo
import com.example.model.AppFolder
import com.example.model.AppInfo

enum class SmartAppCategory(
  val folderId: String,
  val displayName: String,
) {
  SOCIAL("auto_social", "Social"),
  MEDIA("auto_media", "Media"),
  PRODUCTIVITY("auto_productivity", "Productivity"),
  UTILITIES("auto_utilities", "Utilities"),
  GAMES("auto_games", "Games"),
}

object SmartFolderCategorizer {

  const val AUTO_FOLDER_PREFIX = "auto_"
  private const val DEFAULT_MAX_APPS_PER_FOLDER = 8

  private val socialKeywords =
    listOf(
      "social",
      "chat",
      "messag",
      "sms",
      "mms",
      "whatsapp",
      "telegram",
      "signal",
      "discord",
      "slack",
      "instagram",
      "twitter",
      "threads",
      "reddit",
      "linkedin",
      "facebook",
      "snapchat",
      "tiktok",
      "wechat",
      "mastodon",
      "bluesky",
      "skype",
      "teams",
      "zoom",
      "meet",
    )

  private val mediaKeywords =
    listOf(
      "music",
      "audio",
      "video",
      "media",
      "spotify",
      "youtube",
      "ytmusic",
      "netflix",
      "twitch",
      "podcast",
      "pocketcasts",
      "plex",
      "vlc",
      "camera",
      "photo",
      "gallery",
      "album",
      "soundcloud",
      "tidal",
      "deezer",
      "primevideo",
      "disney",
      "hbo",
      "radio",
      "player",
      "kindle",
      "audible",
      "books",
    )

  private val productivityKeywords =
    listOf(
      "calendar",
      "notes",
      "note",
      "docs",
      "sheets",
      "slides",
      "drive",
      "notion",
      "obsidian",
      "todo",
      "task",
      "office",
      "word",
      "excel",
      "powerpoint",
      "keep",
      "evernote",
      "mail",
      "gmail",
      "outlook",
      "protonmail",
      "github",
      "gitlab",
      "code",
      "terminal",
      "pdf",
      "scanner",
      "files",
      "translate",
      "work",
    )

  private val gamesKeywords =
    listOf(
      "game",
      "chess",
      "puzzle",
      "arcade",
      "solitaire",
      "sudoku",
      "minecraft",
      "roblox",
      "steam",
      "playgames",
    )

  fun isSmartFolder(folder: AppFolder): Boolean = folder.id.startsWith(AUTO_FOLDER_PREFIX)

  fun classifyApp(app: AppInfo): SmartAppCategory {
    when (app.appCategory) {
      ApplicationInfo.CATEGORY_SOCIAL -> return SmartAppCategory.SOCIAL
      ApplicationInfo.CATEGORY_AUDIO,
      ApplicationInfo.CATEGORY_VIDEO,
      ApplicationInfo.CATEGORY_IMAGE -> return SmartAppCategory.MEDIA
      ApplicationInfo.CATEGORY_PRODUCTIVITY,
      ApplicationInfo.CATEGORY_NEWS -> return SmartAppCategory.PRODUCTIVITY
      ApplicationInfo.CATEGORY_GAME -> return SmartAppCategory.GAMES
      ApplicationInfo.CATEGORY_MAPS,
      ApplicationInfo.CATEGORY_ACCESSIBILITY -> return SmartAppCategory.UTILITIES
    }

    val haystack = "${app.packageName} ${app.originalLabel} ${app.label}".lowercase()

    return when {
      socialKeywords.any { haystack.contains(it) } -> SmartAppCategory.SOCIAL
      mediaKeywords.any { haystack.contains(it) } -> SmartAppCategory.MEDIA
      productivityKeywords.any { haystack.contains(it) } -> SmartAppCategory.PRODUCTIVITY
      gamesKeywords.any { haystack.contains(it) } -> SmartAppCategory.GAMES
      else -> SmartAppCategory.UTILITIES
    }
  }

  fun sortAppsByUsage(apps: List<AppInfo>): List<AppInfo> =
    apps.sortedWith(
      compareByDescending<AppInfo> { it.dailyLaunchCount }
        .thenBy { it.label.lowercase() }
    )

  fun generateSmartFolders(
    apps: List<AppInfo>,
    existingFolders: List<AppFolder> = emptyList(),
    maxAppsPerFolder: Int = DEFAULT_MAX_APPS_PER_FOLDER,
  ): List<AppFolder> {
    if (apps.isEmpty()) return existingFolders

    val grouped = apps.distinctBy { it.componentKey }.groupBy { classifyApp(it) }

    val sortedCategories =
      grouped.entries
        .filter { it.value.isNotEmpty() }
        .sortedWith(
          compareByDescending<Map.Entry<SmartAppCategory, List<AppInfo>>> { entry ->
            entry.value.sumOf { it.dailyLaunchCount }
          }.thenBy { entry -> entry.key.ordinal }
        )

    val generatedAutoFolders =
      sortedCategories.map { (category, categoryApps) ->
        val prioritizedApps = sortAppsByUsage(categoryApps).take(maxAppsPerFolder.coerceAtLeast(1))
        AppFolder(
          id = category.folderId,
          name = category.displayName,
          componentKeys = prioritizedApps.map { it.componentKey },
        )
      }

    val customUserFolders = existingFolders.filterNot { isSmartFolder(it) }
    return generatedAutoFolders + customUserFolders
  }
}
