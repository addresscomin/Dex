package com.me.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var cur = ""
    private val tick = object : Runnable {
        override fun run() {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (cur.isNotEmpty() && pm.isInteractive) evaluate(cur)
            h.postDelayed(this, 10_000)
        }
    }

    override fun onServiceConnected() { h.post(tick) }
    override fun onDestroy() { h.removeCallbacks(tick); super.onDestroy() }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) { cur = ""; return }
        cur = pkg
        evaluate(pkg)
    }

    private fun evaluate(pkg: String) {
        if (pkg !in Prefs.blocked(this)) return
        val now = System.currentTimeMillis()
        val limitMs = Prefs.getInt(this, "limit", 30) * 60_000L
        val mode = when {
            Prefs.scheduleActive(this) -> "schedule"
            Prefs.usedTodayMs(this, pkg) >= limitMs -> "limit"
            now < Prefs.allowUntil(this, pkg) -> return
            else -> "intent"
        }
        cur = ""
        startActivity(Intent(this, BlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("mode", mode).putExtra("pkg", pkg))
    }
}
