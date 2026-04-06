package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BattleResultActivity : AppCompatActivity() {

    private val uiHandler = Handler(Looper.getMainLooper())

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
        val enemiesStarted = intent.getIntExtra("enemies_started", 0)
        val moraleEnd = intent.getIntExtra("morale_end", 0)
        val casualtiesRows = intent.getStringArrayListExtra("casualties_rows") ?: arrayListOf()
        val suppliesLost = intent.getIntExtra("supplies_lost", 0)
        val warbandStatus = intent.getStringExtra("warband_status") ?: "Unknown"
        val nodeId = intent.getStringExtra("node_id") ?: ""

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiTheme.BASE_BG))
            setPadding(dp(32), dp(40), dp(32), dp(40))
        }

        val resultText = TextView(this).apply {
            text = if (playerWon) "VICTORY" else "DEFEAT"
            textSize = 42f
            setTextColor(if (playerWon) Color.parseColor(UiTheme.GOLD) else Color.parseColor(UiTheme.DANGER))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        layout.addView(resultText, marginParams(bottom = 8))

        if (!playerWon) {
            layout.addView(TextView(this).apply {
                text = "☠"
                textSize = 44f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(UiTheme.DANGER))
            }, marginParams(bottom = 8))
        }

        layout.addView(TextView(this).apply {
            text = nodeName
            textSize = 18f
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            gravity = Gravity.CENTER
        }, marginParams(bottom = 24))

        val statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(statsContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val rows = mutableListOf<Triple<String, String, Int>>()
        rows.add(Triple("Enemies Defeated", "$enemiesKilled / $enemiesStarted", Color.WHITE))
        rows.add(Triple("Morale at End", "$moraleEnd%", Color.parseColor(UiTheme.SUCCESS)))
        casualtiesRows.forEach { row ->
            rows.add(Triple("Casualties", row, Color.parseColor(UiTheme.GOLD)))
        }

        if (playerWon) {
            rows.add(Triple("Supplies", "+$suppliesReward", Color.parseColor(UiTheme.SUCCESS)))
            rows.add(Triple("Renown", "+$renownReward", Color.parseColor(UiTheme.GOLD)))
        } else {
            rows.add(Triple("Supplies Lost", "-$suppliesLost", Color.parseColor(UiTheme.DANGER)))
            rows.add(Triple("Warband Status", warbandStatus, Color.parseColor(UiTheme.DANGER)))
        }

        var delay = 100L
        for ((label, value, color) in rows) {
            val rowView = buildStatRow(label, value, color).apply { alpha = 0f }
            statsContainer.addView(rowView)
            uiHandler.postDelayed({ rowView.animate().alpha(1f).setDuration(220).start() }, delay)
            delay += 130L
        }

        layout.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        if (playerWon) {
            layout.addView(makeActionButton("Continue Campaign", UiTheme.SUCCESS) {
                finish()
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        } else {
            layout.addView(makeActionButton("⚔ Try Again", UiTheme.DANGER) {
                val intent = Intent(this, BattleActivity::class.java)
                intent.putExtra("node_id", nodeId)
                startActivity(intent)
                finish()
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 14
            })

            layout.addView(makeActionButton("↩ Retreat", UiTheme.SURFACE_ALT) {
                finish()
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        setContentView(layout)
    }

    private fun makeActionButton(label: String, color: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            setPadding(dp(20), dp(14), dp(20), dp(14))
            applyUiButtonStyle(color, 16f)
            setOnClickListener { action() }
        }
    }

    private fun buildStatRow(label: String, value: String, valueColor: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 8, 20, 8)
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(valueColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        })
        return row
    }

    private fun marginParams(bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = bottom }
    }
}
