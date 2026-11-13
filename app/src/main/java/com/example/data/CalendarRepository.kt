package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.model.CalendarEventPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val context: Context) {

  fun hasCalendarPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
      PackageManager.PERMISSION_GRANTED
  }

  suspend fun loadNextUpcomingEvent(nowMillis: Long = System.currentTimeMillis()): CalendarEventPreview? =
    withContext(Dispatchers.IO) {
      if (!hasCalendarPermission()) return@withContext null

      val windowEnd = nowMillis + 6 * 60 * 60 * 1000L
      val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
      ContentUris.appendId(builder, nowMillis)
      ContentUris.appendId(builder, windowEnd)

      val projection =
        arrayOf(
          CalendarContract.Instances.EVENT_ID,
          CalendarContract.Instances.TITLE,
          CalendarContract.Instances.BEGIN,
          CalendarContract.Instances.END,
          CalendarContract.Instances.ALL_DAY,
          CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )

      val selection = "${CalendarContract.Instances.ALL_DAY} = 0 AND ${CalendarContract.Instances.END} > ?"
      val selectionArgs = arrayOf(nowMillis.toString())
      val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

      return@withContext runCatching {
        context.contentResolver.query(
          builder.build(),
          projection,
          selection,
          selectionArgs,
          sortOrder,
        )?.use { cursor ->
          val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
          val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
          val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
          val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
          val statusIdx = cursor.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)

          while (cursor.moveToNext()) {
            val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else CalendarContract.Attendees.ATTENDEE_STATUS_NONE
            if (status == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue

            val title = if (titleIdx >= 0) cursor.getString(titleIdx)?.trim().orEmpty() else ""
            if (title.isEmpty()) continue

            val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
            val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else nowMillis
            val end = if (endIdx >= 0) cursor.getLong(endIdx) else begin
            return@use CalendarEventPreview(
              id = id,
              title = title,
              startTimeMillis = begin,
              endTimeMillis = end,
            )
          }
          null
        }
      }.getOrNull()
    }

  fun openCalendarApp() {
    val intent =
      Intent(Intent.ACTION_VIEW).apply {
        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    val launched = runCatching { context.startActivity(intent) }.isSuccess
    if (!launched) {
      val fallback =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_APP_CALENDAR)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
      runCatching { context.startActivity(fallback) }
    }
  }
}
