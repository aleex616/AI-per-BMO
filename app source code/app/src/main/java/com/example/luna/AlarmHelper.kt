package com.example.luna

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log

object AlarmHelper {
    fun setAlarm(context: Context, hour: Int, minute: Int, message: String = "Sveglia") {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            Log.d("AlarmHelper", "Alarm set: $hour:$minute - $message")
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to set alarm: ${e.message}")
        }
    }
}
