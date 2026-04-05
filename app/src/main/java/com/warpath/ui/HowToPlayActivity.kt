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
            setBackgroundColor(Color.parseColor("#1a1a2e"))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val title = TextView(this).apply {
            text = "How to Play"
            textSize = 28f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        layout.addView(title, marginParams(bottom = 30))

        addSection(
            layout,
            "Campaign Map",
            "v1.2.0 expands overworld navigation with a larger map camera.\n\n" +
                "Your warband stays centered by default while the world moves beneath you. " +
                "Drag the map to look around, then tap Recenter to lock focus on your warband again."
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
            "When near a point of interest, tap Interact to open context actions.\n\n" +
                "Enemy nodes: Fight, Run, or Bribe.\n" +
                "Towns/Villages: Buy, Sell, Recruit, Rest.\n" +
                "Outposts/Camps/Caches: situational utility actions."
        )

        val backBtn = Button(this).apply {
            text = "Back to Menu"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2a2a4e"))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(30, 20, 30, 20)
            stateListAnimator = null
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
            setTextColor(Color.parseColor("#aabb99"))
            typeface = Typeface.DEFAULT_BOLD
        }
        parent.addView(titleTv, marginParams(bottom = 6))

        val bodyTv = TextView(this).apply {
            text = body
            textSize = 14f
            setTextColor(Color.parseColor("#8888aa"))
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
