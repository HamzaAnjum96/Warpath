package com.warpath.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
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

    private lateinit var radarView: RadarView
    private val entryViews = mutableListOf<View>()
    private var entryPlayed = false

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

        // ── Background layers (bottom → top) ──

        // 1 — Animated radar rings
        radarView = RadarView(this)
        frame.addView(radarView, fullMatch())

        // 2 — Radial vignette: transparent core, dark edges
        frame.addView(View(this).apply {
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 900f
                colors = intArrayOf(Color.TRANSPARENT, Color.parseColor("#AA000000"))
                setGradientCenter(0.5f, 0.38f)
            }
        }, fullMatch())

        // 3 — Bottom-up opaque fade (anchors the button zone)
        frame.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(Color.parseColor("#E0060B14"), Color.TRANSPARENT)
            )
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(320), Gravity.BOTTOM))

        // 4 — Top amber atmospheric glow
        frame.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#22C9A45E"), Color.TRANSPARENT)
            )
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(220), Gravity.TOP))

        // 5 — Corner bracket markers (always visible, no animation)
        frame.addView(CornerMarkersView(this), fullMatch())

        // ── Main content ──
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(UiTheme.SPACE_5), 0, dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_6))
        }

        // Upper flexible spacer — pushes header up into the radar area
        root.addView(spacer(weight = 1.0f))

        // ── Header block ──

        val classChip = TextView(this).apply {
            text = "◈  AIR OPS COMMAND  ◈"
            textSize = UiTheme.TEXT_MICRO + 1f
            setTextColor(Color.parseColor(UiTheme.WARNING))
            typeface = UiTheme.TYPEFACE_LABEL
            letterSpacing = 0.22f
            gravity = Gravity.CENTER
            alpha = 0f
        }
        entryViews += classChip
        root.addView(classChip, centeredLp(bottom = UiTheme.SPACE_4))

        val title = TextView(this).apply {
            text = "WARPATH"
            textSize = 58f
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = UiTheme.TYPEFACE_TITLE
            gravity = Gravity.CENTER
            letterSpacing = 0.10f
            setShadowLayer(32f, 0f, 6f, Color.parseColor("#99000000"))
            alpha = 0f
        }
        entryViews += title
        root.addView(title, matchLp(bottom = UiTheme.SPACE_3))

        val accentRow = buildAccentDivider()
        entryViews += accentRow
        root.addView(accentRow, matchLp(bottom = UiTheme.SPACE_3, hMargin = UiTheme.SPACE_7))

        val tagline = TextView(this).apply {
            text = "LEAD YOUR SQUADRON"
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
            typeface = UiTheme.TYPEFACE_LABEL
            gravity = Gravity.CENTER
            letterSpacing = 0.22f
            alpha = 0f
        }
        entryViews += tagline
        root.addView(tagline, matchLp(bottom = UiTheme.SPACE_3))

        val version = TextView(this).apply {
            text = "v1.3.1"
            textSize = UiTheme.TEXT_MICRO
            setTextColor(Color.parseColor(UiTheme.TEXT_DISABLED))
            typeface = UiTheme.TYPEFACE_BODY
            gravity = Gravity.CENTER
            background = UiTheme.roundedRect(UiTheme.SURFACE_ALT, null, UiTheme.RADIUS_XS)
            setPadding(dp(UiTheme.SPACE_2), dp(2), dp(UiTheme.SPACE_2), dp(2))
            alpha = 0f
        }
        entryViews += version
        root.addView(version, centeredLp(bottom = 0))

        // Middle spacer — gap between header and buttons
        root.addView(spacer(weight = 0.8f))

        // ── Button block ──

        val playBtn = buildPrimaryBtn("Launch Campaign", R.drawable.ic_lucide_play).apply {
            alpha = 0f
            setOnClickListener {
                startActivity(
                    Intent(this@MainMenuActivity, CampaignActivity::class.java)
                        .putExtra("new_game", true)
                )
            }
        }
        entryViews += playBtn
        root.addView(playBtn, matchLp(bottom = UiTheme.SPACE_3))

        val howToBtn = buildSecondaryBtn("How to Play", R.drawable.ic_lucide_book_open).apply {
            alpha = 0f
            setOnClickListener {
                startActivity(Intent(this@MainMenuActivity, HowToPlayActivity::class.java))
            }
        }
        entryViews += howToBtn
        root.addView(howToBtn, matchLp(bottom = UiTheme.SPACE_2))

        val exitBtn = buildTertiaryBtn("Exit", R.drawable.ic_lucide_log_out).apply {
            alpha = 0f
            setOnClickListener { finish() }
        }
        entryViews += exitBtn
        root.addView(exitBtn, matchLp(bottom = 0))

        // ── Footer ──
        val footer = buildFooter().apply { alpha = 0f }
        entryViews += footer
        root.addView(footer, matchLp(top = UiTheme.SPACE_5))

        frame.addView(root, fullMatch())
        setContentView(frame)
    }

    override fun onStart() {
        super.onStart()
        radarView.start()
        if (!entryPlayed) {
            entryPlayed = true
            playEntryAnimations()
        }
    }

    override fun onStop() {
        super.onStop()
        radarView.stop()
    }

    private fun playEntryAnimations() {
        val interp = DecelerateInterpolator(2.2f)
        entryViews.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = dp(22).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(60L + index * 65L)
                .setDuration(400L)
                .setInterpolator(interp)
                .start()
        }
    }

    // ── Button builders ──

    private fun buildPrimaryBtn(label: String, iconRes: Int): Button = Button(this).apply {
        text = label
        textSize = UiTheme.TEXT_BUTTON
        isAllCaps = false
        typeface = UiTheme.TYPEFACE_HEADING
        setTextColor(Color.parseColor(UiTheme.BASE_BG))
        setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_4))
        stateListAnimator = null
        minHeight = dp(56)
        minimumHeight = dp(56)
        // Cyan fill, dark ripple for light-on-dark legibility
        background = UiTheme.rippleDrawable(UiTheme.PRIMARY, null, UiTheme.RADIUS_MD, "#33000000")
        applyIcon(iconRes, UiTheme.BASE_BG, 20)
    }

    private fun buildSecondaryBtn(label: String, iconRes: Int): Button = Button(this).apply {
        text = label
        textSize = UiTheme.TEXT_BUTTON
        isAllCaps = false
        typeface = UiTheme.TYPEFACE_HEADING
        setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
        setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_3))
        stateListAnimator = null
        minHeight = dp(UiTheme.BUTTON_HEIGHT)
        minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
        background = UiTheme.rippleDrawable(UiTheme.SURFACE_ALT, UiTheme.BORDER, UiTheme.RADIUS_MD)
        applyIcon(iconRes, UiTheme.TEXT_PRIMARY, 18)
    }

    private fun buildTertiaryBtn(label: String, iconRes: Int): Button = Button(this).apply {
        text = label
        textSize = UiTheme.TEXT_BUTTON_SM
        isAllCaps = false
        typeface = UiTheme.TYPEFACE_HEADING
        setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
        setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_2))
        stateListAnimator = null
        minHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
        minimumHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
        background = UiTheme.rippleDrawable(UiTheme.SURFACE, UiTheme.BORDER, UiTheme.RADIUS_MD)
        applyIcon(iconRes, UiTheme.TEXT_SUBTLE, 16)
    }

    /** Attach a sized + tinted Lucide icon to the start of a Button. */
    private fun Button.applyIcon(iconRes: Int, tintHex: String, sizeDp: Int) {
        AppCompatResources.getDrawable(context, iconRes)?.let { d ->
            DrawableCompat.setTint(d.mutate(), Color.parseColor(tintHex))
            d.setBounds(0, 0, dp(sizeDp), dp(sizeDp))
            setCompoundDrawables(d, null, null, null)
            compoundDrawablePadding = dp(UiTheme.SPACE_2)
        }
    }

    // ── Component builders ──

    private fun buildAccentDivider(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        alpha = 0f
        addView(
            View(context).apply { setBackgroundColor(Color.parseColor(UiTheme.PRIMARY_MUTED)) },
            LinearLayout.LayoutParams(0, dp(1), 1f)
        )
        addView(TextView(context).apply {
            text = "◆"
            textSize = UiTheme.TEXT_MICRO + 1f
            setTextColor(Color.parseColor(UiTheme.PRIMARY))
            gravity = Gravity.CENTER
            setPadding(dp(UiTheme.SPACE_3), 0, dp(UiTheme.SPACE_3), 0)
        })
        addView(
            View(context).apply { setBackgroundColor(Color.parseColor(UiTheme.PRIMARY_MUTED)) },
            LinearLayout.LayoutParams(0, dp(1), 1f)
        )
    }

    private fun buildFooter(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(
            View(context).apply { setBackgroundColor(Color.parseColor(UiTheme.DIVIDER)) },
            LinearLayout.LayoutParams(dp(36), dp(1)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(UiTheme.SPACE_3)
            }
        )
        addView(TextView(context).apply {
            text = "INTERCEPT  ·  STRIKE  ·  DOMINATE"
            textSize = UiTheme.TEXT_MICRO
            setTextColor(Color.parseColor(UiTheme.TEXT_DISABLED))
            typeface = UiTheme.TYPEFACE_LABEL
            gravity = Gravity.CENTER
            letterSpacing = 0.14f
        })
    }

    // ── LayoutParams helpers ──

    private fun fullMatch() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    private fun spacer(weight: Float): View = View(this).also {
        it.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, weight
        )
    }

    private fun matchLp(bottom: Int = 0, top: Int = 0, hMargin: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(bottom); topMargin = dp(top)
            marginStart = dp(hMargin); marginEnd = dp(hMargin)
        }

    private fun centeredLp(bottom: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(bottom)
        }

    // ── Custom background views ──

    /**
     * Draws 3 concentric rings that pulse outward from a focal point aligned
     * with the title area — like a radar sweep emanating from the player.
     * Static background rings provide depth; animated rings suggest active scanning.
     */
    private inner class RadarView(context: Context) : View(context) {

        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dpF(1f)
            color = Color.parseColor(UiTheme.PRIMARY)
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dpF(0.5f)
            color = Color.parseColor(UiTheme.PRIMARY)
        }

        // Each pulse ring is defined by its current fraction (0=center, 1=max radius)
        private var p0 = 0.00f
        private var p1 = 0.33f
        private var p2 = 0.67f

        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedFraction
                p0 = t
                p1 = (t + 0.33f) % 1f
                p2 = (t + 0.67f) % 1f
                invalidate()
            }
        }

        fun start() { if (!animator.isRunning) animator.start() }
        fun stop() { animator.cancel() }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height * 0.40f          // Focus on the title area
            val maxR = minOf(width, height) * 0.80f

            // Static concentric rings — provide structure and depth
            val staticRings = floatArrayOf(0.25f, 0.45f, 0.64f, 0.84f)
            val staticAlphas = intArrayOf(22, 17, 12, 7)
            for (i in staticRings.indices) {
                ringPaint.alpha = staticAlphas[i]
                canvas.drawCircle(cx, cy, maxR * staticRings[i], ringPaint)
            }

            // Crosshair axes
            linePaint.alpha = 10
            canvas.drawLine(cx - maxR, cy, cx + maxR, cy, linePaint)
            canvas.drawLine(cx, cy - maxR, cx, cy + maxR, linePaint)

            // Animated pulse rings
            drawPulse(canvas, cx, cy, maxR, p0, 55)
            drawPulse(canvas, cx, cy, maxR, p1, 42)
            drawPulse(canvas, cx, cy, maxR, p2, 30)
        }

        /** Alpha falls off quadratically as the ring expands — fast bright at center, fades out. */
        private fun drawPulse(
            canvas: Canvas, cx: Float, cy: Float, maxR: Float, frac: Float, peakAlpha: Int
        ) {
            val alpha = ((1f - frac) * (1f - frac) * peakAlpha).toInt().coerceIn(0, 255)
            if (alpha < 2) return
            ringPaint.alpha = alpha
            canvas.drawCircle(cx, cy, maxR * frac, ringPaint)
        }
    }

    /**
     * Draws thin L-shaped bracket marks in the four screen corners —
     * a common tactical HUD motif that frames the scene without cluttering it.
     */
    private inner class CornerMarkersView(context: Context) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dpF(1.5f)
            color = Color.parseColor(UiTheme.PRIMARY)
            alpha = 38
        }

        override fun onDraw(canvas: Canvas) {
            val m = dp(18).toFloat()   // margin from screen edge
            val a = dp(24).toFloat()   // arm length

            val r = width - m
            val b = height - m

            // Top-left
            canvas.drawLine(m, m + a, m, m, paint)
            canvas.drawLine(m, m, m + a, m, paint)

            // Top-right
            canvas.drawLine(r - a, m, r, m, paint)
            canvas.drawLine(r, m, r, m + a, paint)

            // Bottom-left
            canvas.drawLine(m, b - a, m, b, paint)
            canvas.drawLine(m, b, m + a, b, paint)

            // Bottom-right
            canvas.drawLine(r - a, b, r, b, paint)
            canvas.drawLine(r, b, r, b - a, paint)
        }
    }
}
