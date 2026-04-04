package com.warpath.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BattleResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val playerWon = intent.getBooleanExtra("player_won", false)
        val nodeName = intent.getStringExtra("node_name") ?: "Battle"
        val suppliesReward = intent.getIntExtra("supplies_reward", 0)
        val renownReward = intent.getIntExtra("renown_reward", 0)
        val enemiesKilled = intent.getIntExtra("enemies_killed", 0)
        val squadsLost = intent.getIntExtra("squads_lost", 0)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(60, 80, 60, 80)
        }

        // Result header
        val resultText = TextView(this).apply {
            text = if (playerWon) "VICTORY" else "DEFEAT"
            textSize = 42f
            setTextColor(if (playerWon) Color.parseColor("#e6c84c") else Color.parseColor("#cc3333"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setShadowLayer(6f, 2f, 2f, Color.parseColor("#80000000"))
        }
        layout.addView(resultText, marginParams(bottom = 16))

        // Battle name
        val nameText = TextView(this).apply {
            text = nodeName
            textSize = 18f
            setTextColor(Color.parseColor("#aaaacc"))
            gravity = Gravity.CENTER
        }
        layout.addView(nameText, marginParams(bottom = 40))

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#333355"))
        }
        layout.addView(divider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2
        ).apply { bottomMargin = 30 })

        // Stats
        addStatRow(layout, "Enemies Defeated", "$enemiesKilled")
        addStatRow(layout, "Squads Lost", "$squadsLost")

        if (playerWon) {
            val rewardDivider = View(this).apply {
                setBackgroundColor(Color.parseColor("#333355"))
            }
            layout.addView(rewardDivider, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { topMargin = 20; bottomMargin = 20 })

            val rewardsTitle = TextView(this).apply {
                text = "REWARDS"
                textSize = 16f
                setTextColor(Color.parseColor("#e6c84c"))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            layout.addView(rewardsTitle, marginParams(bottom = 16))

            addStatRow(layout, "Supplies", "+$suppliesReward", Color.parseColor("#33aa33"))
            addStatRow(layout, "Renown", "+$renownReward", Color.parseColor("#ccaa33"))
        }

        // Spacer
        val spacer = View(this)
        layout.addView(spacer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Continue button
        val continueBtn = Button(this).apply {
            text = "Continue Campaign"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(if (playerWon) Color.parseColor("#33aa33") else Color.parseColor("#cc3333"))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(40, 24, 40, 24)
            stateListAnimator = null
            setOnClickListener { finish() }
        }
        layout.addView(continueBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(layout)
    }

    private fun addStatRow(parent: LinearLayout, label: String, value: String, valueColor: Int = Color.WHITE) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 8, 20, 8)
        }
        val labelTv = TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.parseColor("#8888aa"))
        }
        row.addView(labelTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val valueTv = TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(valueColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }
        row.addView(valueTv)
        parent.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun marginParams(bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = bottom }
    }
}
