package com.warpath.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView

object UiTheme {
    const val BASE_BG = "#0E1726"
    const val SURFACE = "#111C2D"
    const val SURFACE_ALT = "#16263A"
    const val BORDER = "#2C3F57"
    const val TEXT_PRIMARY = "#F2F0EA"
    const val TEXT_MUTED = "#9AA9BC"
    const val GOLD = "#D4B15A"
    const val PRIMARY = "#6C83C8"
    const val SUCCESS = "#5FAF7A"
    const val DANGER = "#C56A5D"
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
