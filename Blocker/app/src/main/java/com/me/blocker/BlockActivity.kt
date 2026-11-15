package com.me.blocker

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent

class BlockActivity : Activity() {
    private var timer: CountDownTimer? = null
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val mode = intent.getStringExtra("mode") ?: "intent"
        val pkg = intent.getStringExtra("pkg") ?: ""
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#111111"))
            setPadding(64, 64, 64, 64)
        }
        val title = TextView(this).apply {
            textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            text = when (mode) {
                "schedule" -> "Abhi blocked time hai"
                "limit" -> "Aaj ki limit khatam"
                else -> "Ruko. Saans lo."
            }
        }
        val sub = TextView(this).apply {
            textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(0, 32, 0, 32)
            text = if (mode == "intent") "Kya sach mein ye app abhi chahiye?" else "Kal phir try karna."
        }
        val home = Button(this).apply { text = "Ghar jao"; setOnClickListener { goHome() } }
        root.addView(title); root.addView(sub); root.addView(home)
        if (mode == "intent") {
            val cont = Button(this).apply { isEnabled = false }
            root.addView(cont)
            val secs = Prefs.getInt(this, "wait", 10)
            timer = object : CountDownTimer(secs * 1000L, 1000) {
                override fun onTick(ms: Long) { cont.text = "Wait ${ms / 1000 + 1}s" }
                override fun onFinish() {
                    cont.text = "Phir bhi kholo (5 min)"; cont.isEnabled = true
                    cont.setOnClickListener {
                        Prefs.setAllowUntil(this@BlockActivity, pkg, System.currentTimeMillis() + 5 * 60_000L)
                        finish()
                    }
                }
            }.start()
        }
        setContentView(root)
    }
    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()
    override fun onDestroy() { timer?.cancel(); super.onDestroy() }
}
