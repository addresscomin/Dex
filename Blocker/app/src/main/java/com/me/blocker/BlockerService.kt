package com.me.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockedPackage = ""
    private var lastLaunchElapsed = 0L

    private val foregroundCheck = object : Runnable {
        override fun run() {
            try {
                if (Prefs.getBool(this@BlockerService, "is_blocking", false)) {
                    val activePackage = rootInActiveWindow?.packageName?.toString()
                    if (!activePackage.isNullOrBlank() && activePackage != packageName) {
                        evaluateForegroundPackage(activePackage)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Foreground check failed", e)
            } finally {
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            notificationTimeout = 50
        }
        handler.removeCallbacks(foregroundCheck)
        handler.post(foregroundCheck)
        DebugLog.add(this, "Accessibility service connected")
        Log.i(TAG, "Accessibility blocker connected")
    }

    override fun onDestroy() {
        handler.removeCallbacks(foregroundCheck)
        DebugLog.add(this, "Accessibility service disconnected")
        Log.i(TAG, "Accessibility blocker disconnected")
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !Prefs.getBool(this, "is_blocking", false)) return

        val eventPackage = event.packageName?.toString()
        val activePackage = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (_: Exception) {
            null
        }
        val pkg = activePackage?.takeIf { it.isNotBlank() && it != packageName }
            ?: eventPackage?.takeIf { it.isNotBlank() }
            ?: return

        if (pkg == packageName) return
        DebugLog.add(this, "Event ${event.eventType}: foreground=$pkg")
        evaluateForegroundPackage(pkg)
    }

    private fun evaluateForegroundPackage(pkg: String) {
        if (!Prefs.getBool(this, "is_blocking", false)) return
        if (pkg == packageName || pkg !in Prefs.blocked(this)) return

        val nowElapsed = SystemClock.elapsedRealtime()
        if (pkg == lastBlockedPackage && nowElapsed - lastLaunchElapsed < DEBOUNCE_MS) return

        val now = System.currentTimeMillis()
        val hasUsage = Prefs.hasUsageAccess(this)
        val dailyBudgetMs = Prefs.getInt(this, "limit", 180).coerceIn(15, 720) * 60_000L
        val perAppLimitMs = Prefs.getInt(this, "app_limit", 30).coerceIn(5, 720) * 60_000L

        // A voluntary 5-minute pass must work even during a schedule or after a limit.
        if (now < Prefs.allowUntil(this, pkg)) return

        val mode = when {
            Prefs.scheduleActive(this) -> "schedule"
            hasUsage && Prefs.totalSelectedUsageTodayMs(this) >= dailyBudgetMs -> "limit"
            hasUsage && Prefs.appUsageTodayMs(this, pkg) >= perAppLimitMs -> "limit"
            else -> "intent"
        }

        lastBlockedPackage = pkg
        lastLaunchElapsed = nowElapsed

        // Move away from the target app first, then show the blocker screen.
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({
            if (!Prefs.getBool(this, "is_blocking", false)) return@postDelayed
            try {
                startActivity(
                    Intent(this, BlockActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra("mode", mode)
                        .putExtra("pkg", pkg)
                )
            } catch (e: Exception) {
                DebugLog.add(this, "ERROR showing blocker: ${e.javaClass.simpleName}")
                Log.e(TAG, "Could not show blocker for $pkg", e)
            }
        }, HOME_BEFORE_BLOCK_MS)
        DebugLog.add(this, "BLOCK triggered: $pkg ($mode)")
        Log.i(TAG, "Blocking selected package: $pkg ($mode)")
    }

    companion object {
        private const val TAG = "MyBlockerService"
        private const val CHECK_INTERVAL_MS = 5_000L
        private const val DEBOUNCE_MS = 800L
        private const val HOME_BEFORE_BLOCK_MS = 120L
    }
}
