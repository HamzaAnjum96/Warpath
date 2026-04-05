package com.warpath.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.warpath.model.CampaignNode
import com.warpath.model.EnemyParty
import com.warpath.model.NodeType
import com.warpath.model.PartyFaction
import com.warpath.model.UnitType
import kotlin.math.abs
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
                    if (followPlayerFocus) {
                        cameraNormX = it.mapX
                        cameraNormY = it.mapY
                    }
                }
            }
            invalidate()
        }

    var enemyParties: List<EnemyParty> = emptyList()
        set(value) { field = value; invalidate() }

    var inputEnabled: Boolean = true
    var showPaths: Boolean = false
    var onFocusChanged: ((Boolean) -> Unit)? = null
    var onMapTapped: ((Float, Float) -> Unit)? = null
    var selectedNodeId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    private var playerNormX: Float = 0.1f
    private var playerNormY: Float = 0.5f
    private var playerLookDirX: Float = 1f
    private var playerLookDirY: Float = 0f
    private var isMoving: Boolean = false

    private var cameraZoom = 1.6f
    private val minZoom = 0.85f
    private val maxZoom = 3.2f
    private val baseViewportSpanX = 0.34f
    private val baseViewportSpanY = 0.46f
    private var cameraNormX: Float = playerNormX
    private var cameraNormY: Float = playerNormY
    private var followPlayerFocus: Boolean = true

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isPanning = false
    private lateinit var scaleDetector: ScaleGestureDetector

    private val trail = mutableListOf<Pair<Float, Float>>()
    private var lastTrailNX = playerNormX
    private var lastTrailNY = playerNormY

    private var moveAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var pulseValue: Float = 0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#070b16") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1b2140")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2a3551")
        strokeWidth = 3f
    }
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#d9111624")
    }
    private val fogHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
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
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val clearedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44cc44")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val accessibleRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val playerOuterGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val nodeRadius = 34f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!::scaleDetector.isInitialized) {
            scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val newZoom = (cameraZoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
                    if (newZoom != cameraZoom) {
                        cameraZoom = newZoom
                        invalidate()
                    }
                    return true
                }
            })
        }
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

    fun animatePlayerTo(targetNode: CampaignNode, onComplete: () -> Unit) {
        val startX = playerNormX
        val startY = playerNormY
        val endX = targetNode.mapX
        val endY = targetNode.mapY

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
                if (followPlayerFocus) {
                    cameraNormX = playerNormX
                    cameraNormY = playerNormY
                }

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

    fun animatePlayerTo(normX: Float, normY: Float, onComplete: () -> Unit = {}) {
        val endX = normX.coerceIn(0.02f, 0.98f)
        val endY = normY.coerceIn(0.02f, 0.98f)
        val startX = playerNormX
        val startY = playerNormY
        val dx = endX - startX
        val dy = endY - startY
        if (dx * dx + dy * dy < 0.00002f) {
            onComplete()
            return
        }

        isMoving = true
        trail.clear()
        lastTrailNX = startX
        lastTrailNY = startY

        moveAnimator?.cancel()
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                playerNormX = startX + (endX - startX) * t
                playerNormY = startY + (endY - startY) * t
                if (followPlayerFocus) {
                    cameraNormX = playerNormX
                    cameraNormY = playerNormY
                }
                val tdx = playerNormX - lastTrailNX
                val tdy = playerNormY - lastTrailNY
                if (tdx * tdx + tdy * tdy > 0.0006f) {
                    trail.add(Pair(playerNormX, playerNormY))
                    if (trail.size > 10) trail.removeAt(0)
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
        if (followPlayerFocus) {
            cameraNormX = normX
            cameraNormY = normY
        }
        if (!isMoving) {
            trail.clear()
        }
        invalidate()
    }

    fun setPlayerLookDirection(dirX: Float, dirY: Float) {
        if (abs(dirX) + abs(dirY) < 0.05f) return
        val len = hypot(dirX, dirY)
        playerLookDirX = dirX / len
        playerLookDirY = dirY / len
        invalidate()
    }

    fun movePlayerBy(deltaX: Float, deltaY: Float) {
        if (isMoving) return
        playerNormX = (playerNormX + deltaX).coerceIn(0.02f, 0.98f)
        playerNormY = (playerNormY + deltaY).coerceIn(0.02f, 0.98f)
        if (followPlayerFocus) {
            cameraNormX = playerNormX
            cameraNormY = playerNormY
        }
        invalidate()
    }

    fun recenterOnPlayer() {
        followPlayerFocus = true
        cameraNormX = playerNormX
        cameraNormY = playerNormY
        onFocusChanged?.invoke(true)
        invalidate()
    }

    fun isCenteredOnPlayer(): Boolean = followPlayerFocus
    fun zoomIn() {
        cameraZoom = (cameraZoom + 0.18f).coerceIn(minZoom, maxZoom)
        invalidate()
    }

    fun zoomOut() {
        cameraZoom = (cameraZoom - 0.18f).coerceIn(minZoom, maxZoom)
        invalidate()
    }

    private fun finishMove(endX: Float, endY: Float, onComplete: () -> Unit) {
        isMoving = false
        playerNormX = endX
        playerNormY = endY
        if (followPlayerFocus) {
            cameraNormX = endX
            cameraNormY = endY
        }
        trail.clear()
        invalidate()
        post { onComplete() }
    }

    private fun visibleSpanX() = (baseViewportSpanX / cameraZoom).coerceIn(0.08f, 1f)
    private fun visibleSpanY() = (baseViewportSpanY / cameraZoom).coerceIn(0.10f, 1f)

    private fun clampCameraX(value: Float): Float {
        val half = visibleSpanX() / 2f
        return value.coerceIn(half, 1f - half)
    }

    private fun clampCameraY(value: Float): Float {
        val half = visibleSpanY() / 2f
        return value.coerceIn(half, 1f - half)
    }

    private fun screenX(normX: Float): Float {
        val span = visibleSpanX()
        val left = clampCameraX(cameraNormX) - span / 2f
        return ((normX - left) / span) * width
    }

    private fun screenY(normY: Float): Float {
        val span = visibleSpanY()
        val top = clampCameraY(cameraNormY) - span / 2f
        return ((normY - top) / span) * height
    }

    private fun normXFromScreen(screenX: Float): Float {
        val span = visibleSpanX()
        val left = clampCameraX(cameraNormX) - span / 2f
        return (left + (screenX / width) * span).coerceIn(0f, 1f)
    }

    private fun normYFromScreen(screenY: Float): Float {
        val span = visibleSpanY()
        val top = clampCameraY(cameraNormY) - span / 2f
        return (top + (screenY / height) * span).coerceIn(0f, 1f)
    }

    private fun inViewport(normX: Float, normY: Float, margin: Float = 0.1f): Boolean {
        val sx = screenX(normX)
        val sy = screenY(normY)
        return sx in (-width * margin)..(width * (1f + margin)) && sy in (-height * margin)..(height * (1f + margin))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        cameraNormX = clampCameraX(cameraNormX)
        cameraNormY = clampCameraY(cameraNormY)

        drawBackground(canvas)
        drawFogOfWar(canvas)
        if (showPaths) drawConnections(canvas)
        drawTrail(canvas)
        drawNodes(canvas)
        drawEnemyParties(canvas)
        drawPlayerMarker(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val worldStep = 0.05f
        var wx = 0f
        while (wx <= 1.001f) {
            val sx = screenX(wx)
            canvas.drawLine(sx, 0f, sx, height.toFloat(), gridPaint)
            wx += worldStep
        }
        var wy = 0f
        while (wy <= 1.001f) {
            val sy = screenY(wy)
            canvas.drawLine(0f, sy, width.toFloat(), sy, gridPaint)
            wy += worldStep
        }

        val terrainZones = listOf(
            Triple(0.22f, 0.18f, Pair(0.22f, 0.16f) to "#17243D"), // hills
            Triple(0.57f, 0.69f, Pair(0.30f, 0.20f) to "#132A2A"), // forests
            Triple(0.80f, 0.22f, Pair(0.22f, 0.15f) to "#352C1D"), // drylands
            Triple(0.24f, 0.74f, Pair(0.20f, 0.22f) to "#1D2538")  // plains
        )
        for ((bx, by, zone) in terrainZones) {
            val (size, color) = zone
            val (bw, bh) = size
            terrainPaint.color = Color.parseColor(color)
            val left = screenX(bx - bw / 2f)
            val top = screenY(by - bh / 2f)
            val right = screenX(bx + bw / 2f)
            val bottom = screenY(by + bh / 2f)
            canvas.drawOval(RectF(left, top, right, bottom), terrainPaint)
        }

        val rivers = listOf(
            Pair(0.08f to 0.10f, 0.44f to 0.22f),
            Pair(0.44f to 0.22f, 0.70f to 0.34f),
            Pair(0.70f to 0.34f, 0.90f to 0.45f)
        )
        terrainPaint.color = Color.parseColor("#223B60")
        terrainPaint.style = Paint.Style.STROKE
        terrainPaint.strokeWidth = 8f
        rivers.forEach { (from, to) ->
            canvas.drawLine(screenX(from.first), screenY(from.second), screenX(to.first), screenY(to.second), terrainPaint)
        }
        terrainPaint.style = Paint.Style.FILL

        for (node in nodes) {
            if (!node.isRevealed) continue
            for (connId in node.connections) {
                val toNode = nodes.find { it.id == connId } ?: continue
                if (!toNode.isRevealed) continue
                roadPaint.alpha = if (showPaths) 140 else 90
                canvas.drawLine(screenX(node.mapX), screenY(node.mapY), screenX(toNode.mapX), screenY(toNode.mapY), roadPaint)
            }
        }

        val blobs = listOf(
            Triple(0.32f, 0.12f, 0.18f to 0.2f),
            Triple(0.62f, 0.72f, 0.22f to 0.22f)
        )
        for ((bx, by, size) in blobs) {
            val (bw, bh) = size
            val left = screenX(bx - bw / 2f)
            val top = screenY(by - bh / 2f)
            val right = screenX(bx + bw / 2f)
            val bottom = screenY(by + bh / 2f)
            terrainPaint.color = Color.parseColor("#111733")
            canvas.drawOval(RectF(left, top, right, bottom), terrainPaint)
        }
    }

    private fun drawFogOfWar(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fogPaint)
        val revealPoints = nodes.filter { it.isRevealed }.map { Pair(it.mapX, it.mapY) } + Pair(playerNormX, playerNormY)
        revealPoints.forEach { (nx, ny) ->
            val sx = screenX(nx)
            val sy = screenY(ny)
            val radius = width.coerceAtLeast(height) * 0.12f
            fogHolePaint.shader = RadialGradient(
                sx,
                sy,
                radius,
                intArrayOf(Color.argb(20, 30, 44, 72), Color.argb(160, 16, 20, 36), Color.argb(220, 12, 14, 26)),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(sx, sy, radius, fogHolePaint)
        }
        fogHolePaint.shader = null
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
                if (!inViewport(node.mapX, node.mapY, 0.2f) && !inViewport(toNode.mapX, toNode.mapY, 0.2f)) continue

                val isActive = node.id == currentNodeId || toNode.id == currentNodeId
                val paint = if (isActive) activeLinePaint else linePaint
                canvas.drawLine(fx, fy, tx, ty, paint)

                if (isActive) drawMidArrow(canvas, fx, fy, tx, ty)
            }
        }
    }

    private fun drawMidArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
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
            if (!node.isRevealed || !inViewport(node.mapX, node.mapY)) continue
            val cx = screenX(node.mapX)
            val cy = screenY(node.mapY)
            if (node.isCleared && isTemporaryNode(node)) {
                drawClearedTaskMarker(canvas, cx, cy)
                continue
            }

            if (node.id == currentNodeId) {
                val glowR = nodeRadius + 5f + pulseValue * 10f
                val glowAlpha = (80 + (pulseValue * 120).toInt())
                glowPaint.color = Color.argb(glowAlpha, 230, 200, 76)
                glowPaint.strokeWidth = 5f + pulseValue * 3f
                canvas.drawCircle(cx, cy, glowR, glowPaint)
            }

            if (selectedNodeId == node.id) {
                glowPaint.color = Color.parseColor("#ccb78646")
                glowPaint.strokeWidth = 3.5f
                canvas.drawCircle(cx, cy, nodeRadius + 13f + pulseValue * 3f, glowPaint)
            }

            if (isAccessibleFromCurrent(node) && !node.isCleared) {
                val ringAlpha = (50 + (pulseValue * 80).toInt())
                accessibleRingPaint.color = Color.argb(ringAlpha, 200, 200, 255)
                canvas.drawCircle(cx, cy, nodeRadius + 5f, accessibleRingPaint)
            }

            nodePaint.style = Paint.Style.FILL
            nodePaint.color = node.type.color.toInt()
            nodePaint.alpha = if (node.isCleared) 90 else 255
            nodeStrokePaint.color = lighten(node.type.color.toInt(), 1.5f)
            nodeStrokePaint.alpha = nodePaint.alpha

            drawNodeShape(canvas, node, cx, cy)
            if (node.isCleared) canvas.drawCircle(cx, cy, nodeRadius + 3f, clearedRingPaint)

            textPaint.color = if (node.isCleared) Color.argb(160, 255, 255, 255) else Color.WHITE
            textPaint.textSize = 24f
            canvas.drawText(nodeIcon(node.type), cx, cy + 9f, textPaint)
            textPaint.textSize = 26f
            textPaint.color = Color.WHITE

            val label = if (node.isCleared) "✓ ${node.name}" else node.name
            val shouldShowLabel = selectedNodeId == node.id ||
                node.id == currentNodeId ||
                cameraZoom > 1.45f ||
                hypot(playerNormX - node.mapX, playerNormY - node.mapY) < 0.12f
            if (shouldShowLabel) {
                drawLabel(canvas, label, cx, cy + nodeRadius + 24f)
            }
        }
    }

    private fun drawClearedTaskMarker(canvas: Canvas, cx: Float, cy: Float) {
        val markerRadius = 10f
        nodePaint.color = Color.parseColor("#556070")
        nodePaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, markerRadius, nodePaint)
        nodeStrokePaint.color = Color.parseColor("#88a0b2")
        nodeStrokePaint.alpha = 170
        canvas.drawCircle(cx, cy, markerRadius, nodeStrokePaint)

        textPaint.textSize = 14f
        textPaint.color = Color.parseColor("#dbe5f3")
        canvas.drawText("⚔", cx, cy + 5f, textPaint)
        textPaint.textSize = 26f
    }

    private fun isTemporaryNode(node: CampaignNode): Boolean = when (node.type) {
        NodeType.ENEMY_PATROL, NodeType.RESOURCE_CACHE, NodeType.ELITE_CHALLENGE, NodeType.RECOVERY_CAMP -> true
        else -> false
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

        selectedNodeId?.let { id ->
            val node = nodes.find { it.id == id }
            if (node != null) {
                val tx = screenX(node.mapX)
                val ty = screenY(node.mapY)
                val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#88e6c84c")
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(14f, 10f), pulseValue * 20f)
                }
                canvas.drawLine(px, py, tx, ty, routePaint)
            }
        }

        val glowRadius = 24f + pulseValue * 8f
        val glowAlpha = (100 + (pulseValue * 100).toInt())
        playerOuterGlowPaint.color = Color.argb(glowAlpha, 220, 190, 90)
        playerOuterGlowPaint.strokeWidth = 4f + pulseValue * 2f
        canvas.drawCircle(px, py, glowRadius, playerOuterGlowPaint)

        playerPaint.color = Color.parseColor("#e6c84c")
        canvas.drawCircle(px, py, 16f, playerPaint)

        val lookLine = 28f
        val lx = px + playerLookDirX * lookLine
        val ly = py + playerLookDirY * lookLine
        val lookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#f5e4ab")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(px, py, lx, ly, lookPaint)
        canvas.drawCircle(lx, ly, 4f, lookPaint)

        playerIconPaint.color = Color.parseColor("#2c2c2c")
        canvas.drawText("▲", px, py + 8f, playerIconPaint)
    }

    private fun drawEnemyParties(canvas: Canvas) {
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#dce6ff")
            typeface = Typeface.DEFAULT_BOLD
        }

        for (party in enemyParties) {
            val node = nodes.find { it.id == party.nodeId } ?: continue
            if (!node.isRevealed || !inViewport(node.mapX, node.mapY, 0.2f)) continue

            val profile = partyVisualProfile(party)
            val x = screenX((node.mapX + profile.offsetNormX).coerceIn(0.02f, 0.98f))
            val y = screenY((node.mapY + profile.offsetNormY).coerceIn(0.02f, 0.98f))
            val ringRadius = profile.iconRadiusPx + pulseValue * 6f
            val ringAlpha = (70 + pulseValue * 140f).toInt()
            val rangeRadius = worldRadiusToPixels(profile.rangeRadiusNorm)

            if (party.faction == PartyFaction.FRIENDLY) {
                markerPaint.color = Color.parseColor("#2d7fd6")
                pulseRingPaint.color = Color.argb(ringAlpha, 90, 170, 255)
                symbolPaint.color = Color.parseColor("#eaf4ff")
                labelPaint.color = Color.parseColor("#8ec9ff")
            } else {
                markerPaint.color = Color.parseColor("#c32626")
                pulseRingPaint.color = Color.argb(ringAlpha, 255, 90, 90)
                symbolPaint.color = Color.WHITE
                labelPaint.color = Color.parseColor("#ff9b9b")
            }

            if (selectedNodeId == node.id) {
                pulseRingPaint.strokeWidth = 2f
                pulseRingPaint.alpha = 110
                pulseRingPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 8f), pulseValue * 16f)
                canvas.drawCircle(x, y, rangeRadius, pulseRingPaint)
                pulseRingPaint.pathEffect = null
            }

            pulseRingPaint.strokeWidth = 2.5f
            pulseRingPaint.alpha = 255
            canvas.drawCircle(x, y, ringRadius, pulseRingPaint)
            canvas.drawCircle(x, y, profile.iconRadiusPx, markerPaint)
            canvas.drawText(if (party.faction == PartyFaction.FRIENDLY) "🛡" else "☠", x, y + 9f, symbolPaint)
            canvas.drawText(
                if (party.faction == PartyFaction.FRIENDLY) "ALLY L${profile.level}" else "ROAMING L${profile.level}",
                x,
                y - (profile.iconRadiusPx + 9f),
                labelPaint
            )
        }
    }

    private data class PartyVisualProfile(
        val level: Int,
        val iconRadiusPx: Float,
        val rangeRadiusNorm: Float,
        val offsetNormX: Float,
        val offsetNormY: Float
    )

    private fun partyVisualProfile(party: EnemyParty): PartyVisualProfile {
        val strength = party.unitTemplates.fold(0f) { acc, template ->
            val unit = UnitType.byId(template.unitTypeId)
            val unitPower = unit.baseHp / 6f + unit.baseAttack + unit.baseDefense + unit.baseSpeed * 10f
            acc + unitPower * template.count
        }
        val level = when {
            strength >= 320f -> 4
            strength >= 220f -> 3
            strength >= 120f -> 2
            else -> 1
        }
        val rangeBase = if (party.faction == PartyFaction.FRIENDLY) 0.050f else 0.060f
        val rangeRadiusNorm = rangeBase + (level - 1) * 0.012f
        val iconRadius = 14f + level * 1.8f
        val xOffset = if (party.faction == PartyFaction.FRIENDLY) -0.02f else 0.02f
        val yOffset = -0.02f
        return PartyVisualProfile(level, iconRadius, rangeRadiusNorm, xOffset, yOffset)
    }

    private fun worldRadiusToPixels(radiusNorm: Float): Float {
        val pxPerNormX = width / visibleSpanX()
        val pxPerNormY = height / visibleSpanY()
        return radiusNorm * minOf(pxPerNormX, pxPerNormY)
    }

    private fun isAccessibleFromCurrent(node: CampaignNode): Boolean {
        val current = nodes.find { it.id == currentNodeId } ?: return false
        return current.connections.contains(node.id)
    }

    private fun nodeIcon(type: NodeType): String = when (type) {
        NodeType.ENEMY_PATROL -> "⚔"
        NodeType.RESOURCE_CACHE -> "◈"
        NodeType.ELITE_CHALLENGE -> "☠"
        NodeType.RECOVERY_CAMP -> "♥"
        NodeType.FACTION_OUTPOST -> "⚑"
        NodeType.TOWN -> "♜"
        NodeType.VILLAGE -> "⌂"
        NodeType.BOSS -> "♛"
        NodeType.START -> "⌂"
    }

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).coerceAtMost(255f).toInt()
        val g = (Color.green(color) * factor).coerceAtMost(255f).toInt()
        val b = (Color.blue(color) * factor).coerceAtMost(255f).toInt()
        return Color.argb(Color.alpha(color), r, g, b)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled || isMoving) return false
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isPanning = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) return true
                val dxPx = event.x - lastTouchX
                val dyPx = event.y - lastTouchY

                if (!isPanning && (abs(dxPx) > 6f || abs(dyPx) > 6f)) {
                    isPanning = true
                    if (followPlayerFocus) {
                        followPlayerFocus = false
                        onFocusChanged?.invoke(false)
                    }
                }

                if (isPanning) {
                    val dxNorm = -dxPx / width * visibleSpanX()
                    val dyNorm = -dyPx / height * visibleSpanY()
                    cameraNormX = clampCameraX(cameraNormX + dxNorm)
                    cameraNormY = clampCameraY(cameraNormY + dyNorm)
                    invalidate()
                }

                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP && !isPanning) {
                    onMapTapped?.invoke(normXFromScreen(event.x), normYFromScreen(event.y))
                }
                isPanning = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
