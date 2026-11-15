package com.me.blocker

import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var accessText: TextView
    private lateinit var usageText: TextView
    private lateinit var selectedText: TextView
    private lateinit var budgetText: TextView
    private lateinit var startButton: Button
    private lateinit var limitEt: EditText
    private lateinit var appLimitEt: EditText
    private lateinit var waitEt: EditText
    private lateinit var startEt: EditText
    private lateinit var endEt: EditText
    private lateinit var schedSw: Switch
    private lateinit var appsBox: LinearLayout
    private var selected = mutableSetOf<String>()
    private val bg = Color.rgb(10, 15, 20)
    private val panel = Color.rgb(21, 29, 36)
    private val accent = Color.rgb(120, 226, 169)
    private val muted = Color.rgb(155, 171, 181)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 20) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    private fun card(parent: LinearLayout) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = shape(panel)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = Prefs.blocked(this)
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); clipToPadding = false }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
        }
        scroll.addView(root); setContentView(scroll)

        root.addView(text("MYBLOCKER", 12f, accent, true))
        root.addView(text("Take back your focus.", 29f, Color.WHITE, true).apply {
            setPadding(0, dp(5), 0, dp(4))
        })
        root.addView(text("A simple, intentional app blocking space.", 14f, muted).apply {
            setPadding(0, 0, 0, dp(20))
        })

        val hero = card(root)
        hero.addView(text("BLOCKING STATUS", 11f, muted, true))
        statusText = text("", 23f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(4)) }
        hero.addView(statusText)
        hero.addView(text("Select the apps you want to pause, then start blocking.", 13f, muted))
        startButton = Button(this).apply {
            text = "START APP BLOCKING"; setTextColor(bg); textSize = 14f; isAllCaps = false
            background = shape(accent, 16)
            setOnClickListener { toggleBlocking() }
        }
        hero.addView(startButton, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(18) })

        val access = card(root)
        access.addView(text("REQUIRED ACCESS", 12f, Color.WHITE, true))
        accessText = text("", 13f, muted).apply { setPadding(0, dp(8), 0, dp(10)) }
        access.addView(accessText)
        access.addView(actionButton("Enable Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        usageText = text("", 13f, muted).apply { setPadding(0, dp(12), 0, dp(8)) }
        access.addView(usageText)
        access.addView(actionButton("Enable Usage Access") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        access.addView(actionButton("Open Debug & Test") {
            startActivity(Intent(this, DebugActivity::class.java))
        })

        val budget = card(root)
        budget.addView(text("DAILY FOCUS LIMIT", 12f, Color.WHITE, true))
        budgetText = text("", 13f, muted).apply { setPadding(0, dp(6), 0, dp(8)) }
        budget.addView(budgetText)
        limitEt = field(budget, "Combined selected-app limit (minutes)", Prefs.getInt(this, "limit", 180))
        appLimitEt = field(budget, "Limit for each selected app (minutes)", Prefs.getInt(this, "app_limit", 30))
        waitEt = field(budget, "Pause before a 5-minute pass (seconds)", Prefs.getInt(this, "wait", 10))

        val schedule = card(root)
        schedule.addView(text("SCHEDULE", 12f, Color.WHITE, true))
        schedSw = Switch(this).apply {
            text = "Block selected apps during schedule"; setTextColor(Color.WHITE)
            isChecked = Prefs.getBool(this@MainActivity, "sched_on", false)
        }
        schedule.addView(schedSw)
        startEt = field(schedule, "Start hour (0–23)", Prefs.getInt(this, "sched_start", 22))
        endEt = field(schedule, "End hour (0–23)", Prefs.getInt(this, "sched_end", 6))

        val apps = card(root)
        apps.addView(text("YOUR APP LIST", 12f, Color.WHITE, true))
        selectedText = text("", 13f, accent).apply { setPadding(0, dp(6), 0, dp(12)) }
        apps.addView(selectedText)
        apps.addView(text("Select only the apps you want MyBlocker to pause.", 13f, muted))
        appsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        apps.addView(appsBox)
        apps.addView(actionButton("Select all visible apps") {
            selected = launcherApps().map { it.first }.toMutableSet()
            Prefs.setBlocked(this, selected); renderApps()
        })
        apps.addView(actionButton("Clear selection") {
            selected.clear(); Prefs.setBlocked(this, selected); renderApps()
        })
        root.addView(text("Emergency calling, Android Settings and normal app recovery stay available.", 12f, muted).apply {
            setPadding(dp(4), dp(4), dp(4), dp(8))
        })
        renderApps(); refreshStatus()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 13f; setTextColor(Color.WHITE)
        background = shape(Color.rgb(42, 56, 64), 14)
        setOnClickListener { action() }
    }

    private fun field(parent: LinearLayout, label: String, value: Int): EditText {
        parent.addView(text(label, 12f, muted).apply { setPadding(0, dp(10), 0, dp(3)) })
        return EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; setSingleLine(true)
            setTextColor(Color.WHITE); setHintTextColor(muted); setText(value.toString())
            background = shape(Color.rgb(31, 42, 50), 12)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }.also { parent.addView(it, LinearLayout.LayoutParams(-1, dp(48))) }
    }

    private fun launcherApps(): List<Pair<String, String>> {
        val pm = packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { (pkg, _) ->
                pkg != packageName && !listOf("dialer", "phone", "incallui", "telecom", "emergency",
                    "systemui", "permissioncontroller", "packageinstaller", "settings").any {
                    pkg.contains(it, ignoreCase = true)
                }
            }.sortedBy { it.second.lowercase() }
    }

    private fun renderApps() {
        if (!::appsBox.isInitialized) return
        appsBox.removeAllViews()
        for ((pkg, label) in launcherApps()) {
            appsBox.addView(CheckBox(this).apply {
                text = label; textSize = 14f; setTextColor(Color.WHITE)
                buttonTintList = android.content.res.ColorStateList.valueOf(accent)
                isChecked = pkg in selected; setPadding(0, dp(5), 0, dp(5))
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(pkg) else selected.remove(pkg)
                    Prefs.setBlocked(this@MainActivity, selected); updateCounters()
                }
            })
        }
        updateCounters()
    }

    private fun updateCounters() {
        if (!::selectedText.isInitialized) return
        selectedText.text = "${selected.size} APPS SELECTED"
        val mins = if (hasUsageAccess()) try {
            Prefs.totalSelectedUsageTodayMs(this) / 60_000L
        } catch (_: Exception) { 0L } else 0L
        budgetText.text = "Selected apps used today: ${mins} min"
    }

    private fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabled.split(':').any { it.contains(packageName, ignoreCase = true) }
    }
    private fun hasUsageAccess(): Boolean {
        return Prefs.hasUsageAccess(this)
    }

    private fun refreshStatus() {
        if (!::statusText.isInitialized) return
        val running = Prefs.getBool(this, "is_blocking", false)
        val acc = hasAccessibility(); val usage = hasUsageAccess()
        statusText.text = if (running) "Blocking is ON" else "Blocking is OFF"
        statusText.setTextColor(if (running) accent else Color.WHITE)
        startButton.text = if (running) "STOP APP BLOCKING" else "START APP BLOCKING"
        startButton.setTextColor(if (running) Color.WHITE else bg)
        startButton.background = shape(if (running) Color.rgb(172, 82, 82) else accent, 16)
        accessText.text = "Accessibility service (required): " + if (acc) "Enabled ✓" else "Not enabled"
        usageText.text = "Usage access (daily limits): " + if (usage) "Enabled ✓" else "Optional — enable for time limits"
        updateCounters()
    }

    private fun saveSettings() {
        fun n(e: EditText, d: Int) = e.text.toString().toIntOrNull() ?: d
        Prefs.putInt(this, "limit", n(limitEt, 180).coerceIn(15, 720))
        Prefs.putInt(this, "app_limit", n(appLimitEt, 30).coerceIn(5, 720))
        Prefs.putInt(this, "wait", n(waitEt, 10).coerceIn(0, 300))
        Prefs.putInt(this, "sched_start", n(startEt, 22).coerceIn(0, 23))
        Prefs.putInt(this, "sched_end", n(endEt, 6).coerceIn(0, 23))
        Prefs.putBool(this, "sched_on", schedSw.isChecked)
    }

    private fun toggleBlocking() {
        saveSettings()
        if (Prefs.getBool(this, "is_blocking", false)) {
            Prefs.putBool(this, "is_blocking", false); refreshStatus(); return
        }
        if (!hasAccessibility()) {
            Toast.makeText(this, "Enable Accessibility first. Usage Access is only needed for daily time limits.", Toast.LENGTH_LONG).show()
            refreshStatus(); return
        }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one app to block.", Toast.LENGTH_LONG).show()
            return
        }
        Prefs.putBool(this, "is_blocking", true); refreshStatus()
        val note = if (hasUsageAccess()) "App blocking started." else "Blocking started. Enable Usage Access to activate daily limits."
        Toast.makeText(this, note, Toast.LENGTH_LONG).show()
    }

    override fun onResume() { super.onResume(); if (::statusText.isInitialized) refreshStatus() }
    override fun onPause() { if (::limitEt.isInitialized) saveSettings(); super.onPause() }
}
