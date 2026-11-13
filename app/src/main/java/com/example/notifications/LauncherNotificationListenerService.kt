package com.example.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationBadgeStore {
  private val _countsByPackage = MutableStateFlow<Map<String, Int>>(emptyMap())
  val countsByPackage: StateFlow<Map<String, Int>> = _countsByPackage.asStateFlow()

  fun updateFromActiveNotifications(notifications: Array<StatusBarNotification>?) {
    if (notifications == null) {
      _countsByPackage.value = emptyMap()
      return
    }
    val map = mutableMapOf<String, Int>()
    for (sbn in notifications) {
      val notif = sbn.notification ?: continue
      val isSummary = (notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0
      val isOngoing = (notif.flags and Notification.FLAG_ONGOING_EVENT) != 0
      if (isSummary || isOngoing) continue
      val pkg = sbn.packageName ?: continue
      map[pkg] = (map[pkg] ?: 0) + 1
    }
    _countsByPackage.value = map
  }

  fun clear() {
    _countsByPackage.value = emptyMap()
  }

  fun isListenerEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
      .contains(context.packageName)
  }

  fun openNotificationListenerSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        val componentName =
          ComponentName(context, LauncherNotificationListenerService::class.java)
        putExtra(":settings:fragment_args_key", componentName.flattenToString())
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    runCatching { context.startActivity(intent) }
  }
}

class LauncherNotificationListenerService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    runCatching {
      NotificationBadgeStore.updateFromActiveNotifications(activeNotifications)
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    runCatching {
      NotificationBadgeStore.updateFromActiveNotifications(activeNotifications)
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
    runCatching {
      NotificationBadgeStore.updateFromActiveNotifications(activeNotifications)
    }
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    NotificationBadgeStore.clear()
  }
}
