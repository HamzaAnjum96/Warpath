package com.warpath.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class JoystickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((Float, Float) -> Unit)? = null
    var onRelease: ((Float, Float) -> Unit)? = null

    private var stickX = 0f
    private var stickY = 0f

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66333344")
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88aaaacc")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cce6c84c")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseR = width.coerceAtMost(height) * 0.44f
        val knobR = baseR * 0.35f
        canvas.drawCircle(cx, cy, baseR, basePaint)
        canvas.drawCircle(cx, cy, baseR, ringPaint)
        canvas.drawCircle(cx + stickX * baseR, cy + stickY * baseR, knobR, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val dx = (event.x - cx) / (width * 0.44f)
        val dy = (event.y - cy) / (height * 0.44f)
        val len = hypot(dx, dy)
        val nx = if (len > 1f) dx / len else dx
        val ny = if (len > 1f) dy / len else dy
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                stickX = nx
                stickY = ny
                onMove?.invoke(stickX, stickY)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val rx = stickX
                val ry = stickY
                stickX = 0f
                stickY = 0f
                onRelease?.invoke(rx, ry)
                invalidate()
            }
        }
        return true
    }
}
