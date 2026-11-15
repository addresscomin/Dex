package com.me.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    private var lastBlockedPackage = ""
    private var lastLaunchElapsed = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg.isBlank()) return
        evaluateForegroundPackage(pkg)
    }

    private fun evaluateForegroundPackage(pkg: String) {
        if (!Prefs.getBool(this, "is_blocking", false)) return
        if (pkg !in Prefs.blocked(this)) return

        val nowElapsed = SystemClock.elapsedRealtime()
        if (pkg == lastBlockedPackage && nowElapsed - lastLaunchElapsed < 1_500L) return

        val now = System.currentTimeMillis()
        val hasUsage = Prefs.hasUsageAccess(this)
        val dailyBudgetMs = Prefs.getInt(this, "limit", 180).coerceIn(15, 720) * 60_000L
        val perAppLimitMs = Prefs.getInt(this, "app_limit", 30).coerceIn(5, 720) * 60_000L

        val mode = when {
            Prefs.scheduleActive(this) -> "schedule"
            hasUsage && Prefs.totalSelectedUsageTodayMs(this) >= dailyBudgetMs -> "limit"
            hasUsage && Prefs.appUsageTodayMs(this, pkg) >= perAppLimitMs -> "limit"
            now < Prefs.allowUntil(this, pkg) -> return
            else -> "intent"
        }

        lastBlockedPackage = pkg
        lastLaunchElapsed = nowElapsed
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("mode", mode)
                .putExtra("pkg", pkg)
        )
    }
}
