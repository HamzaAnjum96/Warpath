package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.warpath.R

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val frame = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.BASE_BG))
        }

        // Atmospheric vignette overlay
        val vignette = View(this).apply {
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 600f
                colors = intArrayOf(Color.TRANSPARENT, Color.parseColor("#88000000"))
                setGradientCenter(0.5f, 0.4f)
            }
        }
        frame.addView(vignette, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Subtle top warm glow
        val topGlow = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                Color.parseColor("#18D4B15A"),
                Color.TRANSPARENT
            ))
        }
        frame.addView(topGlow, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(180), Gravity.TOP
        ))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(UiTheme.SPACE_6), dp(UiTheme.SPACE_7), dp(UiTheme.SPACE_6), dp(UiTheme.SPACE_7))
        }

        // Decorative rule above title
        val topRule = View(this).apply {
            setBackgroundColor(Color.parseColor("#44D4B15A"))
        }
        root.addView(topRule, LinearLayout.LayoutParams(dp(48), dp(2)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(UiTheme.SPACE_4)
        })

        val title = TextView(this).apply {
            text = "WARPATH"
            textSize = UiTheme.TEXT_HERO + 4f
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = UiTheme.TYPEFACE_TITLE
            gravity = Gravity.CENTER
            letterSpacing = 0.14f
            setShadowLayer(12f, 0f, 2f, Color.parseColor("#66000000"))
        }
        root.addView(title, lp(bottom = UiTheme.SPACE_2))

        val tagline = TextView(this).apply {
            text = "LEAD YOUR SQUADRON"
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
            typeface = UiTheme.TYPEFACE_LABEL
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
        }
        root.addView(tagline, lp(bottom = UiTheme.SPACE_1))

        val version = TextView(this).apply {
            text = "v1.3.1"
            textSize = UiTheme.TEXT_MICRO
            setTextColor(Color.parseColor(UiTheme.TEXT_DISABLED))
            typeface = UiTheme.TYPEFACE_BODY
            gravity = Gravity.CENTER
        }
        root.addView(version, lp(bottom = UiTheme.SPACE_7))

        // Buttons
        val playBtn = buildMenuBtn("Launch Campaign", UiTheme.PRIMARY, primary = true, iconRes = R.drawable.ic_lucide_play)
        playBtn.setOnClickListener {
            startActivity(
                Intent(this, CampaignActivity::class.java).apply {
                    putExtra("new_game", true)
                }
            )
        }
        root.addView(playBtn, lp(bottom = UiTheme.SPACE_3, hMargin = UiTheme.SPACE_4))

        val howToPlayBtn = buildMenuBtn("How to Play", UiTheme.SURFACE_ALT, iconRes = R.drawable.ic_lucide_book_open)
        howToPlayBtn.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        root.addView(howToPlayBtn, lp(bottom = UiTheme.SPACE_3, hMargin = UiTheme.SPACE_4))

        val quitBtn = buildMenuBtn("Exit", UiTheme.SURFACE, iconRes = R.drawable.ic_lucide_log_out)
        quitBtn.setOnClickListener { finish() }
        root.addView(quitBtn, lp(bottom = 0, hMargin = UiTheme.SPACE_4))

        // Bottom decorative rule
        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(0, 0, 1f))

        val bottomRule = View(this).apply {
            setBackgroundColor(Color.parseColor("#22D4B15A"))
        }
        root.addView(bottomRule, LinearLayout.LayoutParams(dp(32), dp(1)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(UiTheme.SPACE_5)
        })

        val footer = TextView(this).apply {
            text = "INTERCEPT · STRIKE · DOMINATE"
            textSize = UiTheme.TEXT_MICRO
            setTextColor(Color.parseColor(UiTheme.TEXT_DISABLED))
            typeface = UiTheme.TYPEFACE_LABEL
            gravity = Gravity.CENTER
            letterSpacing = 0.16f
            setPadding(0, dp(UiTheme.SPACE_3), 0, 0)
        }
        root.addView(footer)

        frame.addView(root, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(frame)
    }

    private fun buildMenuBtn(label: String, bgHex: String, primary: Boolean = false, iconRes: Int = 0): Button {
        return Button(this).apply {
            text = label
            textSize = UiTheme.TEXT_BUTTON
            isAllCaps = false
            typeface = UiTheme.TYPEFACE_HEADING
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_4))
            stateListAnimator = null
            minHeight = dp(UiTheme.BUTTON_HEIGHT)
            minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
            background = if (primary) {
                UiTheme.rippleDrawable(bgHex, null, UiTheme.RADIUS_MD)
            } else {
                UiTheme.rippleDrawable(bgHex, UiTheme.BORDER, UiTheme.RADIUS_MD)
            }
            if (iconRes != 0) {
                AppCompatResources.getDrawable(context, iconRes)?.let { d ->
                    DrawableCompat.setTint(d.mutate(), Color.parseColor(UiTheme.TEXT_PRIMARY))
                    d.setBounds(0, 0, dp(18), dp(18))
                    setCompoundDrawables(d, null, null, null)
                    compoundDrawablePadding = dp(UiTheme.SPACE_2)
                }
            }
        }
    }

    private fun lp(bottom: Int = 0, top: Int = 0, hMargin: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(bottom)
            topMargin = dp(top)
            marginStart = dp(hMargin)
            marginEnd = dp(hMargin)
        }
    }
}
