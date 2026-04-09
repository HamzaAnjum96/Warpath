package com.warpath.ui

import android.graphics.Color
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
        @Suppress("DEPRECATION")
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
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5))
        }

        // Back + title row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(TextView(this).apply {
            text = "‹"
            textSize = UiTheme.TEXT_HEADER
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            setPadding(0, 0, dp(UiTheme.SPACE_3), 0)
            setOnClickListener { finish() }
        })
        titleRow.addView(TextView(this).apply {
            text = "HOW TO PLAY"
            textSize = UiTheme.TEXT_SECTION
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = UiTheme.TYPEFACE_TITLE
            letterSpacing = 0.06f
        })
        layout.addView(titleRow, marginParams(bottom = UiTheme.SPACE_5))

        addSection(
            layout,
            "Campaign Map",
            "Points of interest are revealed through fog-of-war as your flight passes nearby. " +
                "Scout by dragging the map freely, then tap the recenter button to lock camera focus back on your flight."
        )

        addSection(
            layout,
            "Movement",
            "Tap the map to mark a flight vector and tap again to begin transit. " +
                "Direct flight paths are fastest unless threat avoidance is needed near hostile airspace. " +
                "Drag the map to free-look without moving."
        )

        addSection(
            layout,
            "Interactions",
            "When near a discovered point of interest, tap it to open context actions.\n\n" +
                "Threat contacts: Engage, Ambush, or Bribe.\n" +
                "Airbases: Scramble, Repair, Trade Fuel, Rest.\n" +
                "FOBs: Scramble, Trade Fuel, Take Contracts.\n" +
                "Fuel Caches and Waypoints: Situational utility actions.\n\n" +
                "Elite engagements can reveal hidden intel about HVT locations."
        )

        addSection(
            layout,
            "Battle",
            "Issue commands to your flights during engagements. " +
                "Lock targets, engage forward, hold pattern, reform retreating flights, or order a tactical disengage. " +
                "Each command has a cooldown between uses."
        )

        addSection(
            layout,
            "Squadron",
            "Manage your flights from the flight roster screen. Scramble new flights at airbases and FOBs. " +
                "Expand your flight slots to field more flights simultaneously."
        )

        // Bottom spacer for scroll comfort
        layout.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(UiTheme.SPACE_7)
        ))

        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun addSection(parent: LinearLayout, title: String, body: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiTheme.roundedRect(UiTheme.SURFACE, UiTheme.BORDER, UiTheme.RADIUS_MD)
            setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
            elevation = dpF(UiTheme.CARD_ELEVATION)
        }

        card.addView(TextView(this).apply {
            text = title.uppercase()
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
            typeface = UiTheme.TYPEFACE_LABEL
            letterSpacing = 0.06f
        })

        card.addView(View(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.DIVIDER))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(UiTheme.SPACE_2)
            bottomMargin = dp(UiTheme.SPACE_2)
        })

        card.addView(TextView(this).apply {
            text = body
            textSize = UiTheme.TEXT_BODY_SM
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            setLineSpacing(4f, 1f)
        })

        parent.addView(card, marginParams(bottom = UiTheme.SPACE_3))
    }

    private fun marginParams(bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottom) }
    }
}
