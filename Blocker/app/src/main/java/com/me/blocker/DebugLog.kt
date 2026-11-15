package com.me.blocker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val KEY = "debug_events"
    @Synchronized fun add(context: Context, message: String) {
        val prefs = context.getSharedPreferences("blocker", Context.MODE_PRIVATE)
        val old = prefs.getString(KEY, "") ?: ""
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val rows = (old.split("\\n").filter { it.isNotBlank() } + "$stamp  $message").takeLast(50)
        prefs.edit().putString(KEY, rows.joinToString("\\n")).apply()
    }
    fun read(context: Context): List<String> =
        (context.getSharedPreferences("blocker", Context.MODE_PRIVATE).getString(KEY, "") ?: "")
            .split("\\n").filter { it.isNotBlank() }.reversed()
    fun clear(context: Context) =
        context.getSharedPreferences("blocker", Context.MODE_PRIVATE).edit().remove(KEY).apply()
}
