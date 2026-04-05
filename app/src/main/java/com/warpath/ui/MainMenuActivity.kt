package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0b1020"))
            setPadding(56, 72, 56, 72)
        }

        val title = TextView(this).apply {
            text = "SARHAD"
            textSize = 52f
            setTextColor(Color.parseColor("#F2D06B"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
        }
        root.addView(title, lp(bottom = 8))

        val subtitle = TextView(this).apply {
            text = "v1.2.0 · Minor Release"
            textSize = 15f
            setTextColor(Color.parseColor("#9AA4C2"))
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, lp(bottom = 36))

        val playBtn = buildMenuBtn("Start", "#2C4A9E", accent = true)
        playBtn.setOnClickListener {
            startActivity(
                Intent(this, CampaignActivity::class.java).apply {
                    putExtra("new_game", true)
                }
            )
        }
        root.addView(playBtn, lp(bottom = 14, hMargin = 20))

        val howToPlayBtn = buildMenuBtn("How to Play", "#243053")
        howToPlayBtn.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        root.addView(howToPlayBtn, lp(bottom = 14, hMargin = 20))

        val quitBtn = buildMenuBtn("Exit", "#1D2438")
        quitBtn.setOnClickListener { finish() }
        root.addView(quitBtn, lp(bottom = 0, hMargin = 20))

        val note = TextView(this).apply {
            text = "Minor release focus: camera-follow map, free look, recenter, and contextual interactions."
            textSize = 12f
            setTextColor(Color.parseColor("#6D789B"))
            gravity = Gravity.CENTER
            setPadding(0, 34, 0, 0)
        }
        root.addView(note, lp())

        setContentView(root)
    }

    private fun buildMenuBtn(label: String, bgHex: String, accent: Boolean = false): Button {
        return Button(this).apply {
            text = label
            textSize = 17f
            setTextColor(if (accent) Color.WHITE else Color.parseColor("#D6DCEF"))
            setBackgroundColor(Color.parseColor(bgHex))
            setPadding(40, 24, 40, 24)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            stateListAnimator = null
        }
    }

    private fun lp(bottom: Int = 0, top: Int = 0, hMargin: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = bottom
            topMargin = top
            marginStart = hMargin
            marginEnd = hMargin
        }
    }
}
