package com.me.blocker

import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var limitEt: EditText
    private lateinit var waitEt: EditText
    private lateinit var startEt: EditText
    private lateinit var endEt: EditText
    private lateinit var schedSw: Switch
    private fun dp(i: Int) = (i * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val lv = ListView(this)
        lv.addHeaderView(header(), null, false)
        lv.adapter = AppAdapter()
        setContentView(lv)
    }

    private fun field(parent: LinearLayout, label: String, value: Int): EditText {
        parent.addView(TextView(this).apply { text = label; setPadding(0, dp(8), 0, 0) })
        return EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; setText(value.toString())
        }.also { parent.addView(it) }
    }

    private fun header(): View {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(24), dp(16), dp(8))
        }
        status = TextView(this); h.addView(status)
        h.addView(Button(this).apply {
            text = "1. Accessibility ON karo (My Blocker)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        h.addView(Button(this).apply {
            text = "2. Usage access ON karo (My Blocker)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        })
        limitEt = field(h, "Daily limit per app (minutes)", Prefs.getInt(this, "limit", 180))
        waitEt = field(h, "Intention wait (seconds)", Prefs.getInt(this, "wait", 10))
        schedSw = Switch(this).apply {
            text = "Schedule: is time poora block"; isChecked = Prefs.getBool(this@MainActivity, "sched_on", false)
        }
        h.addView(schedSw)
        startEt = field(h, "Start hour (0-23)", Prefs.getInt(this, "sched_start", 22))
        endEt = field(h, "End hour (0-23)", Prefs.getInt(this, "sched_end", 6))
        h.addView(TextView(this).apply {
            text = "Block karne wale apps:"; textSize = 18f; setPadding(0, dp(20), 0, 0)
        })
        return h
    }

    override fun onResume() {
        super.onResume()
        val acc = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(packageName) == true
        val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val usage = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) ==
            AppOpsManager.MODE_ALLOWED
        status.text = "Accessibility: ${if (acc) "ON ✓" else "OFF ✗"}\nUsage access: ${if (usage) "ON ✓" else "OFF ✗"}"
    }

    override fun onPause() {
        super.onPause()
        fun n(e: EditText, d: Int) = e.text.toString().toIntOrNull() ?: d
        Prefs.putInt(this, "limit", n(limitEt, 180))
        Prefs.putInt(this, "wait", n(waitEt, 10))
        Prefs.putInt(this, "sched_start", n(startEt, 22).coerceIn(0, 23))
        Prefs.putInt(this, "sched_end", n(endEt, 6).coerceIn(0, 23))
        Prefs.putBool(this, "sched_on", schedSw.isChecked)
    }

    private inner class AppAdapter : BaseAdapter() {
        private val pm = packageManager
        private val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            // Keep essential phone, emergency, system UI and settings packages outside the block list.
            .filter { (pkg, _) ->
                pkg != packageName &&
                !listOf("dialer", "incallui", "telecom", "emergency", "systemui",
                    "permissioncontroller", "packageinstaller", "settings").any { key ->
                    pkg.contains(key, ignoreCase = true)
                }
            }
            .sortedBy { it.second.lowercase() }
        private val appsToProtect = apps.map { it.first }.toSet()
        private val sel = Prefs.blocked(this@MainActivity).also {
            if (!Prefs.getBool(this@MainActivity, "first_run_selection_done", false)) {
                it.addAll(appsToProtect)
                Prefs.setBlocked(this@MainActivity, it)
                Prefs.putBool(this@MainActivity, "first_run_selection_done", true)
            }
        }

        override fun getCount() = apps.size
        override fun getItem(i: Int) = apps[i]
        override fun getItemId(i: Int) = i.toLong()
        override fun getView(i: Int, v: View?, p: ViewGroup?): View {
            val cb = (v as? CheckBox) ?: CheckBox(this@MainActivity).apply {
                setPadding(dp(16), dp(10), dp(16), dp(10))
            }
            val (pkg, label) = apps[i]
            cb.setOnCheckedChangeListener(null)
            cb.text = label
            cb.isChecked = pkg in sel
            cb.setOnCheckedChangeListener { _, on ->
                if (on) sel.add(pkg) else sel.remove(pkg)
                Prefs.setBlocked(this@MainActivity, sel)
            }
            return cb
        }
    }
}
