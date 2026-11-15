package com.me.blocker

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.content.Intent
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockActivity : Activity() {
    private var timer: CountDownTimer? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun panel(color: Int, radius: Int = 22) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val mode = intent.getStringExtra("mode") ?: "intent"
        val pkg = intent.getStringExtra("pkg") ?: ""
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(26), dp(28), dp(26), dp(28))
            setBackgroundColor(Color.rgb(10, 15, 20))
        }
        root.addView(label("MYBLOCKER  •  FOCUS MODE", 12f, Color.rgb(120, 226, 169), true))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), dp(30), dp(24), dp(26))
            background = panel(Color.rgb(21, 29, 36))
        }
        val title = when (mode) {
            "schedule" -> "Focus time is active"
            "limit" -> "Daily limit reached"
            else -> "Pause before you open"
        }
        card.addView(label(title, 25f, Color.WHITE, true))
        val subtitle = when (mode) {
            "schedule" -> "This app is paused during your scheduled focus hours."
            "limit" -> "You have reached the time you set for selected apps today."
            else -> "Take a moment. Is opening this app important right now?"
        }
        card.addView(label(subtitle, 15f, Color.rgb(155, 171, 181)).apply {
            setPadding(0, dp(14), 0, dp(22))
        })
        val home = Button(this).apply {
            text = "BACK TO HOME"; isAllCaps = false; textSize = 14f
            setTextColor(Color.rgb(10, 15, 20)); background = panel(Color.rgb(120, 226, 169), 15)
            setOnClickListener { goHome() }
        }
        card.addView(home, LinearLayout.LayoutParams(-1, dp(52)))
        if (mode == "intent") {
            val cont = Button(this).apply {
                isEnabled = false; isAllCaps = false; textSize = 13f
                setTextColor(Color.WHITE); background = panel(Color.rgb(42, 56, 64), 15)
            }
            card.addView(cont, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
            val secs = Prefs.getInt(this, "wait", 10).coerceIn(0, 300)
            if (secs == 0) {
                cont.text = "Continue for 5 minutes"
                cont.isEnabled = true
                cont.setOnClickListener { grantPass(pkg) }
            } else {
                timer = object : CountDownTimer(secs * 1000L, 1000) {
                    override fun onTick(ms: Long) { cont.text = "Pause for ${ms / 1000 + 1}s" }
                    override fun onFinish() {
                        cont.text = "Continue for 5 minutes"; cont.isEnabled = true
                        cont.setOnClickListener { grantPass(pkg) }
                    }
                }.start()
            }
        }
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(24); bottomMargin = dp(18)
        })
        root.addView(label("Your time. Your choice. Your focus.", 13f, Color.rgb(155, 171, 181)))
        setContentView(root)
    }

    private fun grantPass(pkg: String) {
        Prefs.setAllowUntil(this, pkg, System.currentTimeMillis() + 5 * 60_000L)
        finish()
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
