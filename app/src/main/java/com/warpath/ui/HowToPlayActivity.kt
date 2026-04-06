package com.warpath.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HowToPlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.BASE_BG))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "How to Play"
            textSize = 30f
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        layout.addView(title, marginParams(bottom = 30))

        addSection(
            layout,
            "Campaign Map",
            "v1.3.0 introduces fog-of-war discovery for points of interest.\n\n" +
                "POIs are revealed as your warband passes nearby. You can scout by dragging the map, " +
                "then tap Recenter to lock camera focus back on your warband."
        )

        addSection(
            layout,
            "Movement Controls",
            "TAP MAP - click-to-move the warband to any location (not only nodes).\n" +
                "LEFT THUMB - drag joystick to move the warband marker.\n" +
                "MAP DRAG - free-look without moving the player.\n" +
                "RECENTER - snap camera focus back to your warband."
        )

        addSection(
            layout,
            "Interaction Menus",
            "When near a discovered point of interest, tap Open Actions to open context actions.\n\n" +
                "Enemy nodes: Fight, Run, or Bribe.\n" +
                "Towns/Villages: Buy, Sell, Recruit, Rest.\n" +
                "Outposts/Camps/Caches: situational utility actions.\n\n" +
                "Most POIs no longer require route-chain unlocks. Exception: elite fights can reveal hidden hideout intel."
        )

        val backBtn = Button(this).apply {
            text = "Back to Menu"
            textSize = 16f
            setPadding(dp(24), dp(16), dp(24), dp(16))
            applyUiButtonStyle(UiTheme.SURFACE_ALT, 16f)
            setOnClickListener { finish() }
        }
        layout.addView(
            backBtn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30 }
        )

        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun addSection(parent: LinearLayout, title: String, body: String) {
        val titleTv = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
            typeface = Typeface.DEFAULT_BOLD
        }
        parent.addView(titleTv, marginParams(bottom = 6))

        val bodyTv = TextView(this).apply {
            text = body
            textSize = 14f
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            setLineSpacing(4f, 1f)
        }
        parent.addView(bodyTv, marginParams(bottom = 24))
    }

    private fun marginParams(bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = bottom }
    }
}
