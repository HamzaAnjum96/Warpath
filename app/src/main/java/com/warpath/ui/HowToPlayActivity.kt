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

        addSection(layout, "Campaign Map",
            "Navigate connected nodes on the campaign map. Each node is an encounter - " +
            "battles, resource caches, recovery camps, or outposts. Choose your path wisely " +
            "to balance risk and reward. Your goal: reach and defeat the Regional Boss.")

        addSection(layout, "Battles",
            "Battles are auto-resolved with your warband fighting automatically. " +
            "Use intervention commands to turn the tide:\n\n" +
            "  FOCUS - All squads target one enemy\n" +
            "  PUSH - Advance aggressively\n" +
            "  HOLD - Defend current positions\n" +
            "  RALLY - Restore morale\n" +
            "  RETREAT - Pull back to safety\n\n" +
            "Tap enemy squads on the battlefield to select focus targets.")

        addSection(layout, "Unit Types",
            "FRONTLINE (Square) - Tough melee fighters\n" +
            "RANGED (Triangle) - Long-range attackers\n" +
            "SKIRMISH (Triangle) - Fast mobile ranged\n" +
            "CAVALRY (Diamond) - Quick flankers\n" +
            "SUPPORT (Circle) - Healers and buffers")

        addSection(layout, "Warband Management",
            "Recruit troops at Faction Outposts. Expand your warband slots to field " +
            "more squads. Balance your composition between frontline tanks, ranged damage, " +
            "and support units.")

        addSection(layout, "Resources",
            "SUPPLIES - Spend on recruitment, healing, and upgrades.\n" +
            "RENOWN - Tracks your growing reputation as a warband commander.")

        addSection(layout, "Tips",
            "- Rest at Recovery Camps to heal wounded squads\n" +
            "- Don't rush the boss - build up your warband first\n" +
            "- Morale matters! Routed squads become useless\n" +
            "- Focus fire eliminates threats faster\n" +
            "- Balance risk: elite challenges give great rewards")

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
        layout.addView(backBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 30 })

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
