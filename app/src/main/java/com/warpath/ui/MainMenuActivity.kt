package com.warpath.ui

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.warpath.R

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
            setBackgroundColor(Color.parseColor("#0a0a18"))
            setPadding(60, 80, 60, 80)
        }

        // ── Title block ───────────────────────────────────────────────────────
        val title = TextView(this).apply {
            text = "WARPATH"
            textSize = 54f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setShadowLayer(10f, 2f, 4f, Color.parseColor("#aa000000"))
            letterSpacing = 0.12f
        }
        root.addView(title, lp(bottom = 8))

        val subtitle = TextView(this).apply {
            text = "Rise of the Warband"
            textSize = 17f
            setTextColor(Color.parseColor("#997744"))
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        root.addView(subtitle, lp(bottom = 6))

        // Thin gold divider
        root.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#3a3010"))
        }, LinearLayout.LayoutParams(200, 2, 0f).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 70
            topMargin = 10
        })

        // ── Menu buttons ──────────────────────────────────────────────────────
        val newCampaignBtn = buildMenuBtn("⚔  New Campaign", "#cc2222", accent = true)
        newCampaignBtn.setOnClickListener {
            startActivity(Intent(this, CampaignActivity::class.java).apply {
                putExtra("new_game", true)
            })
        }
        root.addView(newCampaignBtn, lp(bottom = 16, hMargin = 20))

        val continueBtn = buildMenuBtn("▶  Continue Campaign", "#223355")
        continueBtn.setOnClickListener {
            // Continue without new_game flag — uses existing campaignManager state
            startActivity(Intent(this, CampaignActivity::class.java))
        }
        root.addView(continueBtn, lp(bottom = 16, hMargin = 20))

        val warbandBtn = buildMenuBtn("⚔  View Warband", "#223355")
        warbandBtn.setOnClickListener {
            startActivity(Intent(this, WarbandActivity::class.java))
        }
        root.addView(warbandBtn, lp(bottom = 16, hMargin = 20))

        val howToBtn = buildMenuBtn("?  How to Play", "#1a1a30")
        howToBtn.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        root.addView(howToBtn, lp(bottom = 0, hMargin = 20))

        // ── Feature stubs (not yet implemented — shown as disabled) ───────────
        root.addView(buildStubLabel("Multiplayer, Save/Load & Upgrades — Coming Soon"), lp(top = 40))

        // ── Version footer ────────────────────────────────────────────────────
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })
        val version = TextView(this).apply {
            text = "v0.5.0  ·  Touch Prototype"
            textSize = 11f
            setTextColor(Color.parseColor("#333355"))
            gravity = Gravity.CENTER
        }
        root.addView(version, lp(top = 30))

        setContentView(root)
    }

    private fun buildMenuBtn(label: String, bgHex: String, accent: Boolean = false): Button {
        return Button(this).apply {
            text = label
            textSize = 17f
            setTextColor(if (accent) Color.WHITE else Color.parseColor("#aaaacc"))
            setBackgroundColor(Color.parseColor(bgHex))
            setPadding(40, 26, 40, 26)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            stateListAnimator = null
        }
    }

    private fun buildStubLabel(msg: String): TextView {
        return TextView(this).apply {
            text = msg
            textSize = 12f
            setTextColor(Color.parseColor("#333350"))
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
    }

    private fun lp(bottom: Int = 0, top: Int = 0, hMargin: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = bottom
            topMargin    = top
            marginStart  = hMargin
            marginEnd    = hMargin
        }
    }
}
