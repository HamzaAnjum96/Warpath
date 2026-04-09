package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BattleResultActivity : AppCompatActivity() {

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
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

        val accentColor = if (playerWon) UiTheme.GOLD else UiTheme.HOSTILE

        val frame = FrameLayout(this).apply { setBackgroundColor(Color.parseColor(UiTheme.BASE_BG)) }

        // Atmospheric top glow
        frame.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                Color.parseColor(if (playerWon) "#20D4B15A" else "#20C56A5D"),
                Color.TRANSPARENT
            ))
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(200), Gravity.TOP))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(UiTheme.SPACE_6), dp(UiTheme.SPACE_7), dp(UiTheme.SPACE_6), dp(UiTheme.SPACE_6))
        }

        // Outcome header
        layout.addView(TextView(this).apply {
            text = "MISSION REPORT"
            textSize = UiTheme.TEXT_CHIP
            setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
            typeface = UiTheme.TYPEFACE_LABEL
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
        }, marginParams(bottom = UiTheme.SPACE_2))

        layout.addView(TextView(this).apply {
            text = if (playerWon) "VICTORY" else "DEFEAT"
            textSize = UiTheme.TEXT_HERO
            setTextColor(Color.parseColor(accentColor))
            typeface = UiTheme.TYPEFACE_TITLE
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setShadowLayer(16f, 0f, 2f, Color.parseColor("#66000000"))
        }, marginParams(bottom = UiTheme.SPACE_1))

        // Decorative rule
        layout.addView(View(this).apply {
            setBackgroundColor(Color.parseColor(if (playerWon) "#44D4B15A" else "#44C56A5D"))
        }, LinearLayout.LayoutParams(dp(UiTheme.SHEET_HANDLE_WIDTH), dp(2)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(UiTheme.SPACE_2)
        })

        layout.addView(TextView(this).apply {
            text = nodeName
            textSize = UiTheme.TEXT_BODY
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            gravity = Gravity.CENTER
        }, marginParams(bottom = UiTheme.SPACE_5))

        // Stats card
        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiTheme.roundedRect(UiTheme.SURFACE, UiTheme.BORDER, UiTheme.RADIUS_MD)
            setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
            elevation = dpF(UiTheme.CARD_ELEVATION)
        }

        val rows = mutableListOf<Triple<String, String, Int>>()
        rows.add(Triple("Hostiles Destroyed", "$enemiesKilled / $enemiesStarted", Color.parseColor(UiTheme.TEXT_PRIMARY)))
        rows.add(Triple("Pilot Confidence", "$moraleEnd%", Color.parseColor(UiTheme.POSITIVE)))
        casualtiesRows.forEach { row ->
            rows.add(Triple("Aircraft Lost", row, Color.parseColor(UiTheme.WARNING)))
        }
        if (playerWon) {
            rows.add(Triple("Fuel", "+$suppliesReward", Color.parseColor(UiTheme.POSITIVE)))
            rows.add(Triple("RDNS", "+$renownReward", Color.parseColor(UiTheme.WARNING)))
        } else {
            rows.add(Triple("Fuel Consumed", "-$suppliesLost", Color.parseColor(UiTheme.HOSTILE)))
            rows.add(Triple("Squadron", warbandStatus, Color.parseColor(UiTheme.HOSTILE)))
        }

        var delay = 100L
        rows.forEachIndexed { index, (label, value, color) ->
            if (index > 0) {
                statsCard.addView(View(this).apply {
                    setBackgroundColor(Color.parseColor(UiTheme.DIVIDER))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                    topMargin = dp(UiTheme.SPACE_1); bottomMargin = dp(UiTheme.SPACE_1)
                })
            }
            val rowView = buildStatRow(label, value, color).apply { alpha = 0f }
            statsCard.addView(rowView)
            uiHandler.postDelayed({ rowView.animate().alpha(1f).setDuration(200).start() }, delay)
            delay += 110L
        }
        layout.addView(statsCard)

        // Spacer
        layout.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Action buttons
        if (playerWon) {
            layout.addView(makeActionButton("Continue Mission", UiTheme.POSITIVE, true) {
                finish()
            }, marginParams(bottom = 0))
        } else {
            layout.addView(makeActionButton("Re-engage", UiTheme.HOSTILE, true) {
                startActivity(Intent(this, BattleActivity::class.java).apply {
                    putExtra("node_id", nodeId)
                })
                finish()
            }, marginParams(bottom = UiTheme.SPACE_3))

            layout.addView(makeActionButton("Abort Mission", UiTheme.SURFACE_ALT, false) {
                finish()
            }, marginParams(bottom = 0))
        }

        frame.addView(layout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(frame)
    }

    private fun makeActionButton(label: String, color: String, primary: Boolean, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = UiTheme.TEXT_BUTTON
            typeface = UiTheme.TYPEFACE_HEADING
            isAllCaps = false
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_3))
            stateListAnimator = null
            minHeight = dp(UiTheme.BUTTON_HEIGHT)
            minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
            background = if (primary) {
                UiTheme.rippleDrawable(color, null, UiTheme.RADIUS_MD)
            } else {
                UiTheme.rippleDrawable(color, UiTheme.BORDER, UiTheme.RADIUS_MD)
            }
            setOnClickListener { action() }
        }
    }

    private fun buildStatRow(label: String, value: String, valueColor: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_2))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = UiTheme.TEXT_BODY_SM
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(this).apply {
            text = value
            textSize = UiTheme.TEXT_BODY_SM
            setTextColor(valueColor)
            typeface = UiTheme.TYPEFACE_HEADING
            gravity = Gravity.END
        })
        return row
    }

    private fun marginParams(bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottom) }
    }
}
