package com.warpath.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.warpath.engine.BattleState
import com.warpath.model.Squad
import com.warpath.model.SquadState
import com.warpath.model.UnitCategory

class BattleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var battleState: BattleState? = null
    var onEnemyTapped: ((Squad) -> Unit)? = null
    var selectedEnemyId: String? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#1a2a1a") }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#223322")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val enemyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cccccc")
        textSize = 16f
        textAlign = Paint.Align.CENTER
    }
    private val hpBarBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
    }
    private val hpBarFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moralePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4488cc")
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ffcc00")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22ffffff")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    // Battle field is 16x12 units
    private val fieldW = 16f
    private val fieldH = 12f

    private fun toScreenX(fieldX: Float): Float = fieldX / fieldW * width
    private fun toScreenY(fieldY: Float): Float = fieldY / fieldH * height
    private fun toFieldX(screenX: Float): Float = screenX / width * fieldW
    private fun toFieldY(screenY: Float): Float = screenY / height * fieldH

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw terrain grid
        for (i in 0..16) {
            val x = toScreenX(i.toFloat())
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (i in 0..12) {
            val y = toScreenY(i.toFloat())
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        // Draw terrain features (placeholder trees/rocks)
        drawTerrainFeatures(canvas)

        val state = battleState ?: return

        // Draw squads
        for (squad in state.playerSquads) {
            drawSquad(canvas, squad, isPlayer = true)
        }
        for (squad in state.enemySquads) {
            drawSquad(canvas, squad, isPlayer = false)
        }
    }

    private fun drawTerrainFeatures(canvas: Canvas) {
        val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1a3a1a")
            style = Paint.Style.FILL
        }
        // Some decorative trees
        val trees = listOf(5f to 0.5f, 7f to 9.5f, 11f to 2f, 9f to 10f, 3f to 8f)
        for ((tx, ty) in trees) {
            val sx = toScreenX(tx)
            val sy = toScreenY(ty)
            canvas.drawCircle(sx, sy, 20f, treePaint)
        }
    }

    private fun drawSquad(canvas: Canvas, squad: Squad, isPlayer: Boolean) {
        if (!squad.isAlive) return

        val cx = toScreenX(squad.x)
        val cy = toScreenY(squad.y)
        val size = 28f + squad.count * 1.5f

        // Squad shape color
        val paint = if (isPlayer) playerPaint else enemyPaint
        paint.color = getCategoryColor(squad.unitType.category, isPlayer)
        paint.alpha = if (squad.isRouted) 100 else 255

        // Draw squad shape
        when (squad.unitType.category) {
            UnitCategory.FRONTLINE -> {
                // Square for frontline
                canvas.drawRect(cx - size, cy - size, cx + size, cy + size, paint)
            }
            UnitCategory.RANGED, UnitCategory.SKIRMISH -> {
                // Triangle for ranged
                val path = Path()
                path.moveTo(cx, cy - size)
                path.lineTo(cx + size, cy + size)
                path.lineTo(cx - size, cy + size)
                path.close()
                canvas.drawPath(path, paint)
            }
            UnitCategory.CAVALRY -> {
                // Diamond for cavalry
                val path = Path()
                path.moveTo(cx, cy - size * 1.2f)
                path.lineTo(cx + size, cy)
                path.lineTo(cx, cy + size * 1.2f)
                path.lineTo(cx - size, cy)
                path.close()
                canvas.drawPath(path, paint)
            }
            UnitCategory.SUPPORT -> {
                // Circle for support
                canvas.drawCircle(cx, cy, size, paint)
            }
        }

        // Selection highlight
        if (!isPlayer && squad.id == selectedEnemyId) {
            canvas.drawCircle(cx, cy, size + 8f, selectionPaint)
        }

        // Squad count
        canvas.drawText("${squad.count}", cx, cy + 7f, textPaint)

        // HP bar
        val barW = size * 2.2f
        val barH = 5f
        val barY = cy - size - 12f
        hpBarBg.color = Color.parseColor("#333333")
        canvas.drawRect(cx - barW / 2, barY, cx + barW / 2, barY + barH, hpBarBg)
        hpBarFill.color = when {
            squad.currentHpPercent > 0.6f -> Color.parseColor("#33cc33")
            squad.currentHpPercent > 0.3f -> Color.parseColor("#cccc33")
            else -> Color.parseColor("#cc3333")
        }
        canvas.drawRect(cx - barW / 2, barY, cx - barW / 2 + barW * squad.currentHpPercent, barY + barH, hpBarFill)

        // Morale bar (thin blue bar below hp)
        val moraleY = barY + barH + 2f
        moralePaint.alpha = 150
        canvas.drawRect(cx - barW / 2, moraleY, cx - barW / 2 + barW * (squad.morale / 100f), moraleY + 3f, moralePaint)

        // Unit name (small)
        val abbrev = squad.unitType.name.take(8)
        canvas.drawText(abbrev, cx, cy + size + 20f, smallText)

        // State indicator
        if (squad.state == SquadState.RETREAT) {
            val retreatPaint = Paint(smallText).apply { color = Color.parseColor("#ff6666") }
            canvas.drawText("RETREAT", cx, cy + size + 36f, retreatPaint)
        } else if (squad.state == SquadState.RALLY) {
            val rallyPaint = Paint(smallText).apply { color = Color.parseColor("#66ccff") }
            canvas.drawText("RALLY", cx, cy + size + 36f, rallyPaint)
        }
    }

    private fun getCategoryColor(category: UnitCategory, isPlayer: Boolean): Int {
        if (isPlayer) {
            return when (category) {
                UnitCategory.FRONTLINE -> Color.parseColor("#3366aa")
                UnitCategory.SKIRMISH -> Color.parseColor("#6699cc")
                UnitCategory.RANGED -> Color.parseColor("#339966")
                UnitCategory.CAVALRY -> Color.parseColor("#6633aa")
                UnitCategory.SUPPORT -> Color.parseColor("#3399aa")
            }
        } else {
            return when (category) {
                UnitCategory.FRONTLINE -> Color.parseColor("#aa3333")
                UnitCategory.SKIRMISH -> Color.parseColor("#cc6633")
                UnitCategory.RANGED -> Color.parseColor("#993333")
                UnitCategory.CAVALRY -> Color.parseColor("#aa3366")
                UnitCategory.SUPPORT -> Color.parseColor("#996633")
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val state = battleState ?: return false
            val fx = toFieldX(event.x)
            val fy = toFieldY(event.y)

            // Check enemy squads for tap
            for (squad in state.enemySquads) {
                if (!squad.isAlive) continue
                val dx = fx - squad.x
                val dy = fy - squad.y
                if (dx * dx + dy * dy < 4f) {
                    selectedEnemyId = squad.id
                    onEnemyTapped?.invoke(squad)
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
