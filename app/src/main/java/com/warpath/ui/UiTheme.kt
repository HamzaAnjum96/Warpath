package com.warpath.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView

object UiTheme {
    // Core surfaces
    const val BASE_BG = "#0E1726"
    const val SURFACE = "#111C2D"
    const val SURFACE_ALT = "#16263A"
    const val SURFACE_ELEVATED = "#1A2D45"
    const val BORDER = "#2C3F57"

    // Text
    const val TEXT_PRIMARY = "#F2F0EA"
    const val TEXT_MUTED = "#9AA9BC"
    const val TEXT_SUBTLE = "#7F90A6"

    // State palette
    const val PRIMARY = "#6C83C8"
    const val ALLY = "#5E8FD6"
    const val HOSTILE = "#C56A5D"
    const val NEUTRAL = "#8FA0B4"
    const val POSITIVE = "#5FAF7A"
    const val WARNING = "#D4B15A"

    // Terrain palette by biome
    const val BIOME_PLAINS = "#5D6D54"
    const val BIOME_FOREST = "#334B3F"
    const val BIOME_DESERT = "#7A684D"
    const val BIOME_HILLS = "#56606A"
    const val BIOME_SETTLEMENT = "#6B5B45"
    const val BIOME_WATER = "#3B5662"

    // Legacy aliases
    const val GOLD = WARNING
    const val SUCCESS = POSITIVE
    const val DANGER = HOSTILE

    // Layout scale
    const val SPACE_1 = 4
    const val SPACE_2 = 8
    const val SPACE_3 = 12
    const val SPACE_4 = 16
    const val SPACE_5 = 24

    // Radius scale
    const val RADIUS_SM = 10f
    const val RADIUS_MD = 16f
    const val RADIUS_LG = 22f

    // Border/shadow
    const val STROKE_WIDTH = 1
    const val HUD_ELEVATION = 4f
}

fun Context.dp(value: Int): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value.toFloat(),
    resources.displayMetrics
).toInt()

fun Button.applyUiButtonStyle(fill: String = UiTheme.SURFACE_ALT, radiusDp: Float = 16f) {
    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
    isAllCaps = false
    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    stateListAnimator = null
    background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusDp
        setColor(Color.parseColor(fill))
        setStroke(1, Color.parseColor(UiTheme.BORDER))
    }
}

fun TextView.applyUiChipStyle(fill: String = UiTheme.SURFACE_ALT, radiusDp: Float = 10f) {
    setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusDp
        setColor(Color.parseColor(fill))
    }
}
