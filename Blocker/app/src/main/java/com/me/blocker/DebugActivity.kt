package com.me.blocker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class DebugActivity : Activity() {
    private lateinit var events: TextView
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setTextColor(Color.WHITE)
        setOnClickListener { action() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(Color.rgb(10, 15, 20))
        }
        root.addView(TextView(this).apply {
            text = "DEBUG & TEST"; textSize = 22f; setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "Recent accessibility events and blocker decisions (maximum 50)."
            textSize = 13f; setTextColor(Color.LTGRAY)
        })
        root.addView(button("TEST BLOCK SCREEN (UI ONLY)") {
            startActivity(Intent(this, BlockActivity::class.java)
                .putExtra("mode", "intent").putExtra("pkg", packageName))
        })
        root.addView(button("REFRESH EVENTS") { refresh() })
        root.addView(button("CLEAR EVENTS") { DebugLog.clear(this); refresh() })
        events = TextView(this).apply {
            textSize = 12f; setTextColor(Color.rgb(120, 226, 169))
            setPadding(0, dp(12), 0, 0)
            gravity = Gravity.TOP
        }
        val scroll = ScrollView(this)
        scroll.addView(events)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }
    private fun refresh() {
        events.text = DebugLog.read(this).ifEmpty { listOf("No events recorded yet.") }.joinToString("\\n")
    }
}
