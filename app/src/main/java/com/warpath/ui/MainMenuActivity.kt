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
            setBackgroundColor(Color.parseColor(UiTheme.BASE_BG))
            setPadding(dp(32), dp(48), dp(32), dp(48))
        }

        val title = TextView(this).apply {
            text = "WARPATH"
            textSize = 46f
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
        }
        root.addView(title, lp(bottom = 8))

        val subtitle = TextView(this).apply {
            text = "v1.3.0 · Minor Release"
            textSize = 14f
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, lp(bottom = 36))

        val playBtn = buildMenuBtn("Start", UiTheme.PRIMARY, accent = true)
        playBtn.setOnClickListener {
            startActivity(
                Intent(this, CampaignActivity::class.java).apply {
                    putExtra("new_game", true)
                }
            )
        }
        root.addView(playBtn, lp(bottom = 14, hMargin = 20))

        val howToPlayBtn = buildMenuBtn("How to Play", UiTheme.SURFACE_ALT)
        howToPlayBtn.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        root.addView(howToPlayBtn, lp(bottom = 14, hMargin = 20))

        val quitBtn = buildMenuBtn("Exit", UiTheme.SURFACE)
        quitBtn.setOnClickListener { finish() }
        root.addView(quitBtn, lp(bottom = 0, hMargin = 20))

        val note = TextView(this).apply {
            text = "Phase 1 focus: fog-of-war POI discovery, free roam scouting, UI polish, and direct Fight/Run/Bribe interactions."
            textSize = 12f
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            gravity = Gravity.CENTER
            setPadding(0, dp(32), 0, 0)
        }
        root.addView(note, lp())

        setContentView(root)
    }

    private fun buildMenuBtn(label: String, bgHex: String, accent: Boolean = false): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            setTextColor(if (accent) Color.parseColor(UiTheme.TEXT_PRIMARY) else Color.parseColor(UiTheme.TEXT_PRIMARY))
            setPadding(dp(24), dp(16), dp(24), dp(16))
            applyUiButtonStyle(bgHex, 16f)
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
