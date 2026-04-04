package com.warpath.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.warpath.model.CampaignNode
import com.warpath.model.EnemyParty
import com.warpath.model.NodeType
import kotlin.math.hypot

class CampaignMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var nodes: List<CampaignNode> = emptyList()
        set(value) { field = value; invalidate() }

    var currentNodeId: String = ""
        set(value) {
            field = value
            if (!isMoving) {
                nodes.find { it.id == value }?.let {
                    playerNormX = it.mapX
                    playerNormY = it.mapY
                }
            }
            invalidate()
        }

    var enemyParties: List<EnemyParty> = emptyList()
        set(value) { field = value; invalidate() }

    var onNodeTapped: ((CampaignNode) -> Unit)? = null
    var inputEnabled: Boolean = true

    // Player position in normalized [0..1] space (matches mapX/mapY)
    private var playerNormX: Float = 0.1f
    private var playerNormY: Float = 0.5f
    private var isMoving: Boolean = false

    // Trail: list of normalized positions left behind during movement
    private val trail = mutableListOf<Pair<Float, Float>>()
    private var lastTrailNX = playerNormX
    private var lastTrailNY = playerNormY

    // Animators
    private var moveAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var pulseValue: Float = 0f

    // ── Paints ────────────────────────────────────────────────────────────────

    private val bgPaint = Paint().apply { color = Color.parseColor("#0a0a18") }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14142a")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#252550")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5555bb")
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#aaaacc")
        textSize = 17f
        textAlign = Paint.Align.CENTER
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#aa0a0a18")
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val clearedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44cc44")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val accessibleRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val playerOuterGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val nodeRadius = 34f
    private val padX = 64f
    private val padY = 88f

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulseValue = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        moveAnimator?.cancel()
    }

    // ── Animation API ─────────────────────────────────────────────────────────

    /**
     * Smoothly moves the player marker from its current position to [targetNode].
     * Calls [onComplete] on the main thread when finished (or on cancel).
     */
    fun animatePlayerTo(targetNode: CampaignNode, onComplete: () -> Unit) {
        val startX = playerNormX
        val startY = playerNormY
        val endX = targetNode.mapX
        val endY = targetNode.mapY

        // If already there, skip animation
        val dx = endX - startX
        val dy = endY - startY
        if (dx * dx + dy * dy < 0.0001f) {
            onComplete()
            return
        }

        isMoving = true
        trail.clear()
        lastTrailNX = startX
        lastTrailNY = startY

        moveAnimator?.cancel()
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 850
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                playerNormX = startX + (endX - startX) * t
                playerNormY = startY + (endY - startY) * t

                // Accumulate trail points
                val tdx = playerNormX - lastTrailNX
                val tdy = playerNormY - lastTrailNY
                if (tdx * tdx + tdy * tdy > 0.0008f) {
                    trail.add(Pair(playerNormX, playerNormY))
                    if (trail.size > 9) trail.removeAt(0)
                    lastTrailNX = playerNormX
                    lastTrailNY = playerNormY
                }
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishMove(endX, endY, onComplete)
                }
                override fun onAnimationCancel(animation: Animator) {
                    finishMove(endX, endY, onComplete)
                }
            })
            start()
        }
    }

    fun setPlayerPosition(normX: Float, normY: Float) {
        playerNormX = normX
        playerNormY = normY
        if (!isMoving) {
            trail.clear()
        }
        invalidate()
    }

    fun movePlayerBy(deltaX: Float, deltaY: Float) {
        if (isMoving) return
        playerNormX = (playerNormX + deltaX).coerceIn(0.02f, 0.98f)
        playerNormY = (playerNormY + deltaY).coerceIn(0.02f, 0.98f)
        invalidate()
    }

    private fun finishMove(endX: Float, endY: Float, onComplete: () -> Unit) {
        isMoving = false
        playerNormX = endX
        playerNormY = endY
        trail.clear()
        invalidate()
        post { onComplete() }
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private fun screenX(normX: Float) = padX + normX * (width - padX * 2)
    private fun screenY(normY: Float) = padY + normY * (height - padY * 2)

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        drawConnections(canvas)
        drawTrail(canvas)
        drawNodes(canvas)
        drawEnemyParties(canvas)
        drawPlayerMarker(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle grid
        val step = 56f
        var x = 0f
        while (x <= width) { canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint); x += step }
        var y = 0f
        while (y <= height) { canvas.drawLine(0f, y, width.toFloat(), y, gridPaint); y += step }

        // Terrain blobs for depth
        val blobs = listOf(
            Triple(width * 0.32f, height * 0.12f, width * 0.18f to height * 0.2f),
            Triple(width * 0.62f, height * 0.72f, width * 0.22f to height * 0.22f),
            Triple(width * 0.18f, height * 0.72f, width * 0.16f to height * 0.24f),
            Triple(width * 0.78f, height * 0.2f,  width * 0.14f to height * 0.18f)
        )
        for ((bx, by, size) in blobs) {
            val (bw, bh) = size
            terrainPaint.color = Color.parseColor("#0f0f22")
            canvas.drawOval(RectF(bx - bw / 2, by - bh / 2, bx + bw / 2, by + bh / 2), terrainPaint)
        }
    }

    private fun drawConnections(canvas: Canvas) {
        val drawn = mutableSetOf<String>()
        for (node in nodes) {
            if (!node.isRevealed) continue
            val fx = screenX(node.mapX)
            val fy = screenY(node.mapY)
            for (connId in node.connections) {
                val key = if (node.id < connId) "${node.id}-$connId" else "$connId-${node.id}"
                if (drawn.contains(key)) continue
                drawn.add(key)

                val toNode = nodes.find { it.id == connId } ?: continue
                if (!toNode.isRevealed) continue
                val tx = screenX(toNode.mapX)
                val ty = screenY(toNode.mapY)

                val isActive = node.id == currentNodeId || toNode.id == currentNodeId
                val paint = if (isActive) activeLinePaint else linePaint
                canvas.drawLine(fx, fy, tx, ty, paint)

                // Midpoint arrow only on active connections
                if (isActive) {
                    drawMidArrow(canvas, fx, fy, tx, ty)
                }
            }
        }
    }

    private fun drawMidArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        // Arrow points from x1,y1 toward x2,y2 at midpoint (showing direction of travel)
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        val len = hypot(x2 - x1, y2 - y1)
        if (len < 1f) return
        val nx = (x2 - x1) / len
        val ny = (y2 - y1) / len
        val sz = 7f
        val path = Path().apply {
            moveTo(mx + nx * sz, my + ny * sz)
            lineTo(mx - nx * sz - ny * sz, my - ny * sz + nx * sz)
            lineTo(mx - nx * sz + ny * sz, my - ny * sz - nx * sz)
            close()
        }
        arrowPaint.color = Color.argb(150, 100, 100, 200)
        canvas.drawPath(path, arrowPaint)
    }

    private fun drawTrail(canvas: Canvas) {
        if (trail.isEmpty()) return
        for (i in trail.indices) {
            val (nx, ny) = trail[i]
            val px = screenX(nx)
            val py = screenY(ny)
            val frac = (i + 1f) / trail.size
            val alpha = (frac * 140).toInt()
            val radius = 4f + frac * 5f
            trailPaint.color = Color.argb(alpha, 230, 200, 70)
            canvas.drawCircle(px, py, radius, trailPaint)
        }
    }

    private fun drawNodes(canvas: Canvas) {
        for (node in nodes) {
            if (!node.isRevealed) continue
            val cx = screenX(node.mapX)
            val cy = screenY(node.mapY)

            // Pulsing glow for current node
            if (node.id == currentNodeId) {
                val glowR = nodeRadius + 5f + pulseValue * 10f
                val glowAlpha = (80 + (pulseValue * 120).toInt())
                glowPaint.color = Color.argb(glowAlpha, 230, 200, 76)
                glowPaint.strokeWidth = 5f + pulseValue * 3f
                canvas.drawCircle(cx, cy, glowR, glowPaint)
            }

            // Accessible hint ring (pulsing white outline)
            if (isAccessibleFromCurrent(node) && !node.isCleared) {
                val ringAlpha = (50 + (pulseValue * 80).toInt())
                accessibleRingPaint.color = Color.argb(ringAlpha, 200, 200, 255)
                canvas.drawCircle(cx, cy, nodeRadius + 5f, accessibleRingPaint)
            }

            // Node fill
            nodePaint.style = Paint.Style.FILL
            nodePaint.color = node.type.color.toInt()
            nodePaint.alpha = if (node.isCleared) 90 else 255

            nodeStrokePaint.color = lighten(node.type.color.toInt(), 1.5f)
            nodeStrokePaint.alpha = nodePaint.alpha

            drawNodeShape(canvas, node, cx, cy)

            // Cleared ring
            if (node.isCleared) {
                canvas.drawCircle(cx, cy, nodeRadius + 3f, clearedRingPaint)
            }

            // Icon
            textPaint.color = if (node.isCleared) Color.argb(160, 255, 255, 255) else Color.WHITE
            textPaint.textSize = 24f
            canvas.drawText(nodeIcon(node.type), cx, cy + 9f, textPaint)
            textPaint.textSize = 26f
            textPaint.color = Color.WHITE

            // Label
            val label = if (node.isCleared) "✓ ${node.name}" else node.name
            drawLabel(canvas, label, cx, cy + nodeRadius + 24f)
        }
    }

    private fun drawNodeShape(canvas: Canvas, node: CampaignNode, cx: Float, cy: Float) {
        when (node.type) {
            NodeType.BOSS -> {
                val r = nodeRadius + 8f
                val path = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                canvas.drawPath(path, nodePaint)
                canvas.drawPath(path, nodeStrokePaint)
            }
            NodeType.RECOVERY_CAMP -> {
                val rect = RectF(cx - nodeRadius, cy - nodeRadius, cx + nodeRadius, cy + nodeRadius)
                canvas.drawRoundRect(rect, 14f, 14f, nodePaint)
                canvas.drawRoundRect(rect, 14f, 14f, nodeStrokePaint)
            }
            else -> {
                canvas.drawCircle(cx, cy, nodeRadius, nodePaint)
                canvas.drawCircle(cx, cy, nodeRadius, nodeStrokePaint)
            }
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float) {
        val fm = labelPaint.fontMetrics
        val tw = labelPaint.measureText(text)
        val rect = RectF(cx - tw / 2 - 7, cy + fm.ascent - 3, cx + tw / 2 + 7, cy + fm.descent + 3)
        canvas.drawRoundRect(rect, 5f, 5f, labelBgPaint)
        canvas.drawText(text, cx, cy, labelPaint)
    }

    private fun drawPlayerMarker(canvas: Canvas) {
        val px = screenX(playerNormX)
        val py = screenY(playerNormY)

        // Outer pulse glow
        val glowRadius = 24f + pulseValue * 8f
        val glowAlpha = (100 + (pulseValue * 100).toInt())
        playerOuterGlowPaint.color = Color.argb(glowAlpha, 255, 220, 50)
        playerOuterGlowPaint.strokeWidth = 4f + pulseValue * 2f
        canvas.drawCircle(px, py, glowRadius, playerOuterGlowPaint)

        // Drop shadow
        playerPaint.color = Color.argb(70, 0, 0, 0)
        canvas.drawCircle(px + 2.5f, py + 3f, 17f, playerPaint)

        // Main body
        playerPaint.color = Color.parseColor("#e6c84c")
        canvas.drawCircle(px, py, 17f, playerPaint)

        // Inner ring
        playerPaint.color = Color.parseColor("#c8a030")
        playerPaint.style = Paint.Style.STROKE
        playerPaint.strokeWidth = 2f
        canvas.drawCircle(px, py, 13f, playerPaint)
        playerPaint.style = Paint.Style.FILL

        // Inner dark core
        playerPaint.color = Color.parseColor("#2a1800")
        canvas.drawCircle(px, py, 10f, playerPaint)

        // Sword icon
        playerIconPaint.color = Color.parseColor("#e6c84c")
        canvas.drawText("⚔", px, py + 7f, playerIconPaint)
    }

    private fun drawEnemyParties(canvas: Canvas) {
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#bb2222")
            style = Paint.Style.FILL
        }
        val skullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        for (party in enemyParties) {
            val node = nodes.find { it.id == party.nodeId } ?: continue
            if (!node.isRevealed) continue
            val x = screenX(node.mapX) + 20f
            val y = screenY(node.mapY) - 20f
            canvas.drawCircle(x, y, 16f, markerPaint)
            canvas.drawText("☠", x, y + 8f, skullPaint)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isAccessibleFromCurrent(node: CampaignNode): Boolean {
        val current = nodes.find { it.id == currentNodeId } ?: return false
        return current.connections.contains(node.id)
    }

    private fun nodeIcon(type: NodeType): String = when (type) {
        NodeType.ENEMY_PATROL    -> "⚔"
        NodeType.RESOURCE_CACHE  -> "◈"
        NodeType.ELITE_CHALLENGE -> "☠"
        NodeType.RECOVERY_CAMP   -> "♥"
        NodeType.FACTION_OUTPOST -> "⚑"
        NodeType.TOWN            -> "♜"
        NodeType.VILLAGE         -> "⌂"
        NodeType.BOSS            -> "♛"
        NodeType.START           -> "⌂"
    }

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color)   * factor).coerceAtMost(255f).toInt()
        val g = (Color.green(color) * factor).coerceAtMost(255f).toInt()
        val b = (Color.blue(color)  * factor).coerceAtMost(255f).toInt()
        return Color.argb(Color.alpha(color), r, g, b)
    }

    // ── Touch ─────────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled || isMoving) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            for (node in nodes) {
                if (!node.isRevealed) continue
                val cx = screenX(node.mapX)
                val cy = screenY(node.mapY)
                val dx = event.x - cx
                val dy = event.y - cy
                if (dx * dx + dy * dy <= (nodeRadius + 22f) * (nodeRadius + 22f)) {
                    onNodeTapped?.invoke(node)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
