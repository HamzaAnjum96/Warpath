package com.warpath.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView

object UiTheme {
    // ── Air Ops design tokens ──
    const val BG_PRIMARY = "#060B14"
    const val BG_PANEL = "#0B1524"
    const val TERRAIN_LAND = "#253241"
    const val TERRAIN_WATER = "#101C2F"
    const val GRID_LINE = "#21354B"
    const val RADAR_CYAN = "#4EC7D9"
    const val WARNING_RED = "#C45A5A"
    const val HIGHLIGHT_AMBER = "#C9A45E"
    const val FRIENDLY_BLUE = "#5F9DD6"
    const val NEUTRAL_GRAY = "#8894A3"

    // ── Core surfaces ──
    const val BASE_BG = BG_PRIMARY
    const val SURFACE = BG_PANEL
    const val SURFACE_ALT = "#122033"
    const val SURFACE_ELEVATED = "#16293F"
    const val BORDER = "#2A4258"
    const val BORDER_LIGHT = "#375874"
    const val DIVIDER = GRID_LINE

    // ── Text ──
    const val TEXT_PRIMARY = "#D9E4EE"
    const val TEXT_MUTED = "#8FA2B6"
    const val TEXT_SUBTLE = "#76889E"
    const val TEXT_DISABLED = "#506279"

    // ── State palette ──
    const val PRIMARY = RADAR_CYAN
    const val PRIMARY_MUTED = "#1F5F72"
    const val ALLY = FRIENDLY_BLUE
    const val HOSTILE = WARNING_RED
    const val HOSTILE_MUTED = "#8C4A42"
    const val NEUTRAL = NEUTRAL_GRAY
    const val POSITIVE = "#5FAF7A"
    const val WARNING = HIGHLIGHT_AMBER

    // ── Terrain palette by biome ──
    const val BIOME_PLAINS = TERRAIN_LAND
    const val BIOME_FOREST = "#1D2A35"
    const val BIOME_DESERT = "#2E3540"
    const val BIOME_HILLS = "#2A3443"
    const val BIOME_SETTLEMENT = "#202C3B"
    const val BIOME_WATER = TERRAIN_WATER

    // Legacy aliases
    const val GOLD = WARNING
    const val SUCCESS = POSITIVE
    const val DANGER = HOSTILE

    // ── Layout spacing scale (dp) ──
    const val SPACE_1 = 4
    const val SPACE_2 = 8
    const val SPACE_3 = 12
    const val SPACE_4 = 16
    const val SPACE_5 = 24
    const val SPACE_6 = 32
    const val SPACE_7 = 48

    // ── Radius scale ──
    const val RADIUS_XS = 6f
    const val RADIUS_SM = 10f
    const val RADIUS_MD = 16f
    const val RADIUS_LG = 22f
    const val RADIUS_XL = 28f

    // ── Border/shadow ──
    const val STROKE_WIDTH = 1
    const val STROKE_WIDTH_HEAVY = 2
    const val HUD_ELEVATION = 4f
    const val CARD_ELEVATION = 2f
    const val SHEET_ELEVATION = 8f

    // ── Typography scale (sp) ──
    const val TEXT_HERO = 42f
    const val TEXT_HEADER = 28f
    const val TEXT_SECTION = 20f
    const val TEXT_CARD_TITLE = 18f
    const val TEXT_BODY = 14f
    const val TEXT_BODY_SM = 13f
    const val TEXT_SECONDARY = 12f
    const val TEXT_CHIP = 10f
    const val TEXT_MICRO = 9f
    const val TEXT_BUTTON = 14f
    const val TEXT_BUTTON_SM = 12f

    // ── Component sizes (dp) ──
    const val TOP_BAR_HEIGHT = 56
    const val CHIP_HEIGHT = 28
    const val BUTTON_HEIGHT = 44
    const val BUTTON_HEIGHT_SM = 36
    const val ICON_BUTTON_SIZE = 40
    const val BANNER_HEIGHT = 40
    const val SHEET_HANDLE_WIDTH = 40
    const val MARKER_LABEL_HEIGHT = 24

    // ── Icon stroke ──
    const val ICON_STROKE = 2.8f
    const val ICON_STROKE_THIN = 2f

    // ── Typefaces ──
    val TYPEFACE_TITLE: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val TYPEFACE_HEADING: Typeface = Typeface.create("monospace", Typeface.BOLD)
    val TYPEFACE_BODY: Typeface = Typeface.create("monospace", Typeface.NORMAL)
    val TYPEFACE_LABEL: Typeface = Typeface.create("monospace", Typeface.BOLD)

    /** Create a themed rounded-rect drawable */
    fun roundedRect(
        fillHex: String = SURFACE_ALT,
        borderHex: String? = BORDER,
        radius: Float = RADIUS_MD,
        topOnly: Boolean = false
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        if (topOnly) {
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        } else {
            cornerRadius = radius
        }
        setColor(Color.parseColor(fillHex))
        if (borderHex != null) {
            setStroke(STROKE_WIDTH, Color.parseColor(borderHex))
        }
    }

    /** Create a gradient surface drawable */
    fun gradientSurface(
        topHex: String = SURFACE_ELEVATED,
        bottomHex: String = SURFACE,
        borderHex: String? = BORDER,
        radius: Float = RADIUS_LG
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        orientation = GradientDrawable.Orientation.TOP_BOTTOM
        colors = intArrayOf(Color.parseColor(topHex), Color.parseColor(bottomHex))
        cornerRadius = radius
        if (borderHex != null) {
            setStroke(STROKE_WIDTH, Color.parseColor(borderHex))
        }
    }

    /** Create a ripple-wrapped drawable for touch feedback */
    fun rippleDrawable(
        fillHex: String = SURFACE_ALT,
        borderHex: String? = BORDER,
        radius: Float = RADIUS_MD,
        rippleHex: String = "#33FFFFFF"
    ): RippleDrawable {
        val content = roundedRect(fillHex, borderHex, radius)
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(Color.parseColor(rippleHex)),
            content,
            mask
        )
    }
}

fun Context.dp(value: Int): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value.toFloat(),
    resources.displayMetrics
).toInt()

fun Context.dpF(value: Float): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value,
    resources.displayMetrics
)

fun Button.applyUiButtonStyle(fill: String = UiTheme.SURFACE_ALT, radiusDp: Float = UiTheme.RADIUS_MD) {
    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
    isAllCaps = false
    typeface = UiTheme.TYPEFACE_HEADING
    stateListAnimator = null
    background = UiTheme.rippleDrawable(fill, UiTheme.BORDER, radiusDp)
}

fun Button.applyPrimaryStyle(fill: String = UiTheme.PRIMARY, radiusDp: Float = UiTheme.RADIUS_MD) {
    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
    isAllCaps = false
    typeface = UiTheme.TYPEFACE_HEADING
    textSize = UiTheme.TEXT_BUTTON
    stateListAnimator = null
    background = UiTheme.rippleDrawable(fill, null, radiusDp)
}

fun Button.applySecondaryStyle(fill: String = UiTheme.SURFACE_ALT, radiusDp: Float = UiTheme.RADIUS_MD) {
    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
    isAllCaps = false
    typeface = UiTheme.TYPEFACE_HEADING
    textSize = UiTheme.TEXT_BUTTON
    stateListAnimator = null
    background = UiTheme.rippleDrawable(fill, UiTheme.BORDER, radiusDp)
}

fun TextView.applyUiChipStyle(fill: String = UiTheme.SURFACE_ALT, radiusDp: Float = UiTheme.RADIUS_SM) {
    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
    typeface = UiTheme.TYPEFACE_LABEL
    textSize = UiTheme.TEXT_CHIP
    background = UiTheme.roundedRect(fill, null, radiusDp)
}
