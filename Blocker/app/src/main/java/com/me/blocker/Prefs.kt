package com.me.blocker

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("blocker", Context.MODE_PRIVATE)
    fun blocked(c: Context): MutableSet<String> = HashSet(p(c).getStringSet("blocked", emptySet()) ?: emptySet())
    fun setBlocked(c: Context, s: Set<String>) = p(c).edit().putStringSet("blocked", HashSet(s)).apply()
    fun getInt(c: Context, k: String, d: Int) = p(c).getInt(k, d)
    fun putInt(c: Context, k: String, v: Int) = p(c).edit().putInt(k, v).apply()
    fun getBool(c: Context, k: String, d: Boolean) = p(c).getBoolean(k, d)
    fun putBool(c: Context, k: String, v: Boolean) = p(c).edit().putBoolean(k, v).apply()
    fun allowUntil(c: Context, pkg: String) = p(c).getLong("allow_$pkg", 0L)
    fun setAllowUntil(c: Context, pkg: String, t: Long) = p(c).edit().putLong("allow_$pkg", t).apply()

    fun hasUsageAccess(c: Context): Boolean = try {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    fun scheduleActive(c: Context): Boolean {
        if (!getBool(c, "sched_on", false)) return false
        val s = getInt(c, "sched_start", 22).coerceIn(0, 23)
        val e = getInt(c, "sched_end", 6).coerceIn(0, 23)
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (s == e) true else if (s < e) h in s until e else (h >= s || h < e)
    }

    fun totalSelectedUsageTodayMs(c: Context): Long {
        if (!hasUsageAccess(c)) return 0L
        val selected = blocked(c)
        if (selected.isEmpty()) return 0L
        return try {
            val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val stats = usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())
            selected.sumOf { stats[it]?.totalTimeInForeground ?: 0L }
        } catch (_: Exception) {
            0L
        }
    }

    fun appUsageTodayMs(c: Context, pkg: String): Long {
        if (!hasUsageAccess(c)) return 0L
        return try {
            val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())[pkg]?.totalTimeInForeground ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}