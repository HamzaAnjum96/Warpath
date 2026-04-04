package com.warpath.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.warpath.model.CampaignNode
import com.warpath.model.NodeType

class CampaignMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var nodes: List<CampaignNode> = emptyList()
        set(value) { field = value; invalidate() }
    var currentNodeId: String = ""
        set(value) { field = value; invalidate() }
    var onNodeTapped: ((CampaignNode) -> Unit)? = null

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444466")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#aaaacc")
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#0d0d1a")
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val clearedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33aa33")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val nodeRadius = 36f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val padX = 60f
        val padY = 80f
        val drawW = width - padX * 2
        val drawH = height - padY * 2

        // Draw connections first
        for (node in nodes) {
            if (!node.isRevealed) continue
            val fromX = padX + node.mapX * drawW
            val fromY = padY + node.mapY * drawH
            for (connId in node.connections) {
                val toNode = nodes.find { it.id == connId } ?: continue
                if (!toNode.isRevealed) continue
                val toX = padX + toNode.mapX * drawW
                val toY = padY + toNode.mapY * drawH

                val connLinePaint = Paint(linePaint)
                if (node.id == currentNodeId || toNode.id == currentNodeId) {
                    connLinePaint.color = Color.parseColor("#6666aa")
                    connLinePaint.strokeWidth = 4f
                }
                canvas.drawLine(fromX, fromY, toX, toY, connLinePaint)
            }
        }

        // Draw nodes
        for (node in nodes) {
            if (!node.isRevealed) continue
            val cx = padX + node.mapX * drawW
            val cy = padY + node.mapY * drawH

            // Node background
            nodePaint.color = node.type.color.toInt()
            nodePaint.style = Paint.Style.FILL

            if (node.isCleared) {
                nodePaint.alpha = 120
            } else {
                nodePaint.alpha = 255
            }

            // Glow for current node
            if (node.id == currentNodeId) {
                glowPaint.color = Color.parseColor("#e6c84c")
                canvas.drawCircle(cx, cy, nodeRadius + 8f, glowPaint)
            }

            // Draw node shape based on type
            when (node.type) {
                NodeType.BOSS -> {
                    // Diamond shape for boss
                    val path = Path()
                    path.moveTo(cx, cy - nodeRadius - 6)
                    path.lineTo(cx + nodeRadius + 6, cy)
                    path.lineTo(cx, cy + nodeRadius + 6)
                    path.lineTo(cx - nodeRadius - 6, cy)
                    path.close()
                    canvas.drawPath(path, nodePaint)
                }
                NodeType.RECOVERY_CAMP -> {
                    // Rounded square for camps
                    val rect = RectF(cx - nodeRadius, cy - nodeRadius, cx + nodeRadius, cy + nodeRadius)
                    canvas.drawRoundRect(rect, 12f, 12f, nodePaint)
                }
                else -> {
                    canvas.drawCircle(cx, cy, nodeRadius, nodePaint)
                }
            }

            // Cleared checkmark
            if (node.isCleared) {
                canvas.drawCircle(cx, cy, nodeRadius + 2f, clearedPaint)
            }

            // Node icon text
            val icon = when (node.type) {
                NodeType.ENEMY_PATROL -> "\u2694"  // swords
                NodeType.RESOURCE_CACHE -> "\u25C8" // diamond
                NodeType.ELITE_CHALLENGE -> "\u2620" // skull
                NodeType.RECOVERY_CAMP -> "\u2665"  // heart
                NodeType.FACTION_OUTPOST -> "\u2691" // flag
                NodeType.BOSS -> "\u2655"           // crown
                NodeType.START -> "\u2302"          // house
            }
            canvas.drawText(icon, cx, cy + 10f, textPaint)

            // Node name below
            canvas.drawText(node.name, cx, cy + nodeRadius + 28f, smallTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val padX = 60f
            val padY = 80f
            val drawW = width - padX * 2
            val drawH = height - padY * 2

            for (node in nodes) {
                if (!node.isRevealed) continue
                val cx = padX + node.mapX * drawW
                val cy = padY + node.mapY * drawH
                val dx = event.x - cx
                val dy = event.y - cy
                if (dx * dx + dy * dy <= (nodeRadius + 20f) * (nodeRadius + 20f)) {
                    onNodeTapped?.invoke(node)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
