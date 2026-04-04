package com.warpath.ui

import android.content.Intent
import android.graphics.*
import android.os.Bundle
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
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(60, 100, 60, 100)
        }

        // Title
        val title = TextView(this).apply {
            text = "WARPATH"
            textSize = 48f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setShadowLayer(8f, 2f, 2f, Color.parseColor("#80000000"))
        }
        layout.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        // Subtitle
        val subtitle = TextView(this).apply {
            text = "Rise of the Warband"
            textSize = 18f
            setTextColor(Color.parseColor("#aa8844"))
            gravity = android.view.Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        layout.addView(subtitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 80 })

        // Version
        val version = TextView(this).apply {
            text = "v0.5.0 - Touch-First Prototype"
            textSize = 12f
            setTextColor(Color.parseColor("#666688"))
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(version, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 60 })

        // New Campaign button
        val newCampaignBtn = createMenuButton("New Campaign")
        newCampaignBtn.setOnClickListener {
            startActivity(Intent(this, CampaignActivity::class.java).apply {
                putExtra("new_game", true)
            })
        }
        layout.addView(newCampaignBtn, buttonParams())

        // Warband button
        val warbandBtn = createMenuButton("View Warband")
        warbandBtn.setOnClickListener {
            startActivity(Intent(this, WarbandActivity::class.java))
        }
        layout.addView(warbandBtn, buttonParams())

        // How to Play
        val howToBtn = createMenuButton("How to Play")
        howToBtn.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        layout.addView(howToBtn, buttonParams())

        setContentView(layout)
    }

    private fun createMenuButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2a2a4e"))
            setPadding(40, 28, 40, 28)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            stateListAnimator = null
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 24
            marginStart = 40
            marginEnd = 40
        }
    }
}
