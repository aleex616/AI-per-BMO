package com.example.luna

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import java.util.Calendar

object CalendarHelper {
    fun addEvent(context: Context, title: String, startTime: Calendar, endTime: Calendar, description: String = "") {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
            if (description.isNotBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, description)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            Log.d("CalendarHelper", "Calendar intent launched: $title at ${startTime.time}")
        } catch (e: Exception) {
            Log.e("CalendarHelper", "Failed to launch calendar intent: ${e.message}")
        }
    }
}
