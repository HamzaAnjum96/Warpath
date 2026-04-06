package com.warpath.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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
    var enemyDisplayPositions: Map<String, Pair<Float, Float>> = emptyMap()
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
    private var travelTargetNorm: Pair<Float, Float>? = null
    private var travelStartNorm: Pair<Float, Float>? = null
    private var travelProgress: Float = 0f

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
    private lateinit var gestureDetector: GestureDetector
    private var multiTouchStartTime = 0L
    private var multiTouchStartMidX = 0f
    private var multiTouchStartMidY = 0f

    private val trail = mutableListOf<Pair<Float, Float>>()
    private var lastTrailNX = playerNormX
    private var lastTrailNY = playerNormY

    private var moveAnimator: ValueAnimator? = null
    private var cameraAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var pulseValue: Float = 0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#2A332B") }
    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        alpha = 70
    }
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
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val nodeRadius = 34f
    private val mapLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        color = Color.parseColor("#4A5575")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        letterSpacing = 0.12f
    }

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
            gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val zoomTarget = (cameraZoom + 0.22f).coerceIn(minZoom, maxZoom)
                    if (zoomTarget != cameraZoom) {
                        cameraZoom = zoomTarget
                        if (!followPlayerFocus) {
                            cameraNormX = clampCameraX(normXFromScreen(e.x))
                            cameraNormY = clampCameraY(normYFromScreen(e.y))
                        }
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
        cameraAnimator?.cancel()
    }

    private var moveWasCancelled = false

    fun animatePlayerTo(
        targetNode: CampaignNode,
        onProgress: ((Float, Float, Float) -> Unit)? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val startX = playerNormX
        val startY = playerNormY
        val endX = targetNode.mapX
        val endY = targetNode.mapY

        val dx = endX - startX
        val dy = endY - startY
        if (dx * dx + dy * dy < 0.0001f) {
            onComplete(false)
            return
        }

        isMoving = true
        moveWasCancelled = false
        travelStartNorm = Pair(startX, startY)
        travelTargetNorm = Pair(endX, endY)
        travelProgress = 0f
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
                travelProgress = t
                onProgress?.invoke(t, playerNormX, playerNormY)
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
                    if (!moveWasCancelled) {
                        finishMove(endX, endY, onComplete, cancelled = false)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    moveWasCancelled = true
                    finishMove(playerNormX, playerNormY, onComplete, cancelled = true)
                }
            })
            start()
        }
    }

    fun animatePlayerTo(
        normX: Float,
        normY: Float,
        onProgress: ((Float, Float, Float) -> Unit)? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val endX = normX.coerceIn(0.02f, 0.98f)
        val endY = normY.coerceIn(0.02f, 0.98f)
        val startX = playerNormX
        val startY = playerNormY
        val dx = endX - startX
        val dy = endY - startY
        if (dx * dx + dy * dy < 0.00002f) {
            onComplete(false)
            return
        }

        isMoving = true
        moveWasCancelled = false
        travelStartNorm = Pair(startX, startY)
        travelTargetNorm = Pair(endX, endY)
        travelProgress = 0f
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
                travelProgress = t
                onProgress?.invoke(t, playerNormX, playerNormY)
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
                    if (!moveWasCancelled) {
                        finishMove(endX, endY, onComplete, cancelled = false)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    moveWasCancelled = true
                    finishMove(playerNormX, playerNormY, onComplete, cancelled = true)
                }
            })
            start()
        }
    }

    fun cancelMovement() {
        if (!isMoving) return
        moveAnimator?.cancel()
    }

    fun isMovementActive(): Boolean = isMoving

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
        animateCameraTo(playerNormX, playerNormY)
        onFocusChanged?.invoke(true)
    }

    fun isCenteredOnPlayer(): Boolean = followPlayerFocus

    private fun finishMove(endX: Float, endY: Float, onComplete: (Boolean) -> Unit, cancelled: Boolean) {
        isMoving = false
        playerNormX = endX
        playerNormY = endY
        if (followPlayerFocus) {
            cameraNormX = endX
            cameraNormY = endY
        }
        trail.clear()
        travelStartNorm = null
        travelTargetNorm = null
        travelProgress = 0f
        invalidate()
        post { onComplete(cancelled) }
    }

    private fun animateCameraTo(targetX: Float, targetY: Float) {
        val startX = cameraNormX
        val startY = cameraNormY
        cameraAnimator?.cancel()
        cameraAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                cameraNormX = startX + (targetX - startX) * t
                cameraNormY = startY + (targetY - startY) * t
                invalidate()
            }
            start()
        }
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
        drawTravelProjection(canvas)
        if (showPaths) drawConnections(canvas)
        drawTrail(canvas)
        drawNodes(canvas)
        drawEnemyParties(canvas)
        drawPlayerMarker(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        drawBiome(canvas, 0.50f, 0.50f, 1.32f, 1.24f, "#5A6646", BiomeType.PLAINS)
        drawBiome(canvas, 0.79f, 0.24f, 0.52f, 0.36f, "#8F7450", BiomeType.DESERT)
        drawBiome(canvas, 0.58f, 0.71f, 0.60f, 0.42f, "#355542", BiomeType.FOREST)
        drawBiome(canvas, 0.21f, 0.21f, 0.45f, 0.36f, "#596069", BiomeType.HILLS)
        drawBiome(canvas, 0.20f, 0.74f, 0.34f, 0.24f, "#748159", BiomeType.PLAINS)

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

        drawLandmarks(canvas)
        drawRoads(canvas)
        drawRegionLabels(canvas)
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

    private enum class BiomeType { DESERT, PLAINS, FOREST, HILLS }

    private fun biomeAt(nx: Float, ny: Float): BiomeType {
        return when {
            nx > 0.63f && ny < 0.42f -> BiomeType.DESERT
            ny > 0.56f && nx > 0.38f -> BiomeType.FOREST
            nx < 0.40f && ny < 0.40f -> BiomeType.HILLS
            else -> BiomeType.PLAINS
        }
    }

    private fun drawBiome(canvas: Canvas, centerX: Float, centerY: Float, widthNorm: Float, heightNorm: Float, color: String, biome: BiomeType) {
        terrainPaint.color = Color.parseColor(color)
        val left = screenX(centerX - widthNorm / 2f)
        val top = screenY(centerY - heightNorm / 2f)
        val right = screenX(centerX + widthNorm / 2f)
        val bottom = screenY(centerY + heightNorm / 2f)
        canvas.drawOval(RectF(left, top, right, bottom), terrainPaint)

        texturePaint.color = when (biome) {
            BiomeType.DESERT -> Color.parseColor("#A6875E")
            BiomeType.PLAINS -> Color.parseColor("#79855F")
            BiomeType.FOREST -> Color.parseColor("#436853")
            BiomeType.HILLS -> Color.parseColor("#767D84")
        }
        val step = when (biome) {
            BiomeType.DESERT -> 24f
            BiomeType.PLAINS -> 32f
            BiomeType.FOREST -> 20f
            BiomeType.HILLS -> 22f
        }
        var y = top + 8f
        while (y <= bottom - 8f) {
            canvas.drawLine(left + 10f, y, right - 10f, y + if (biome == BiomeType.HILLS) 5f else 2f, texturePaint)
            y += step
        }
    }

    private fun drawLandmarks(canvas: Canvas) {
        val landmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val ruins = listOf(0.32f to 0.28f, 0.72f to 0.76f, 0.15f to 0.57f)
        landmarkPaint.color = Color.parseColor("#7E8178")
        ruins.forEach { (nx, ny) ->
            val sx = screenX(nx)
            val sy = screenY(ny)
            canvas.drawRect(sx - 8f, sy - 7f, sx + 8f, sy + 7f, landmarkPaint)
        }

        val groves = listOf(0.48f to 0.67f, 0.64f to 0.62f, 0.57f to 0.81f)
        landmarkPaint.color = Color.parseColor("#2F4838")
        groves.forEach { (nx, ny) ->
            val sx = screenX(nx)
            val sy = screenY(ny)
            canvas.drawCircle(sx, sy, 9f, landmarkPaint)
            canvas.drawCircle(sx + 8f, sy - 4f, 7f, landmarkPaint)
        }

        val watchPosts = listOf(0.84f to 0.34f, 0.26f to 0.46f)
        landmarkPaint.color = Color.parseColor("#8E7655")
        watchPosts.forEach { (nx, ny) ->
            val sx = screenX(nx)
            val sy = screenY(ny)
            val p = Path().apply {
                moveTo(sx, sy - 8f)
                lineTo(sx + 7f, sy + 7f)
                lineTo(sx - 7f, sy + 7f)
                close()
            }
            canvas.drawPath(p, landmarkPaint)
        }
    }

    private fun drawRoads(canvas: Canvas) {
        val drawn = mutableSetOf<String>()
        for (node in nodes) {
            for (connId in node.connections) {
                val toNode = nodes.find { it.id == connId } ?: continue
                val key = if (node.id < toNode.id) "${node.id}-${toNode.id}" else "${toNode.id}-${node.id}"
                if (!drawn.add(key)) continue
                roadPaint.color = when (biomeAt((node.mapX + toNode.mapX) / 2f, (node.mapY + toNode.mapY) / 2f)) {
                    BiomeType.DESERT -> Color.parseColor("#A08966")
                    BiomeType.PLAINS -> Color.parseColor("#7C8A6D")
                    BiomeType.FOREST -> Color.parseColor("#5A6F5E")
                    BiomeType.HILLS -> Color.parseColor("#7A8193")
                }
                roadPaint.alpha = if (showPaths) 150 else 105
                roadPaint.strokeWidth = 2.5f
                canvas.drawLine(screenX(node.mapX), screenY(node.mapY), screenX(toNode.mapX), screenY(toNode.mapY), roadPaint)
            }
        }
    }

    private fun drawRegionLabels(canvas: Canvas) {
        canvas.drawText("ASHEN DUNES", screenX(0.77f), screenY(0.16f), mapLabelPaint)
        canvas.drawText("IRON HILLS", screenX(0.23f), screenY(0.10f), mapLabelPaint)
        canvas.drawText("SABLE WOODS", screenX(0.58f), screenY(0.80f), mapLabelPaint)
        canvas.drawText("BROKEN PLAINS", screenX(0.22f), screenY(0.86f), mapLabelPaint)
    }

    private fun drawTravelProjection(canvas: Canvas) {
        val target = travelTargetNorm ?: return
        val start = travelStartNorm ?: Pair(playerNormX, playerNormY)
        val startX = screenX(start.first)
        val startY = screenY(start.second)
        val endX = screenX(target.first)
        val endY = screenY(target.second)
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#A8D6A6")
            strokeWidth = 3.5f
            pathEffect = DashPathEffect(floatArrayOf(16f, 10f), pulseValue * 22f)
            alpha = 220
        }
        canvas.drawLine(startX, startY, endX, endY, routePaint)

        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#DCE9C5")
            strokeWidth = 4.5f
            alpha = 230
        }
        val progressX = startX + (endX - startX) * travelProgress
        val progressY = startY + (endY - startY) * travelProgress
        canvas.drawLine(startX, startY, progressX, progressY, progressPaint)

        val destPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#EAF3CF")
            strokeWidth = 2f
        }
        canvas.drawCircle(endX, endY, 10f + pulseValue * 3f, destPaint)
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

            nodePaint.style = Paint.Style.FILL
            nodePaint.color = node.type.color.toInt()
            nodePaint.alpha = if (node.isCleared) 90 else 255
            nodeStrokePaint.color = lighten(node.type.color.toInt(), 1.5f)
            nodeStrokePaint.alpha = nodePaint.alpha

            drawNodeShape(canvas, node, cx, cy)
            if (node.isCleared) canvas.drawCircle(cx, cy, nodeRadius + 3f, clearedRingPaint)
            if (selectedNodeId == node.id) {
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#D6E0FF")
                    strokeWidth = 3f
                }
                val selRadius = nodeRadius + 8f + pulseValue * 3f
                canvas.drawCircle(cx, cy, selRadius, ringPaint)
            }

            drawNodeGlyph(canvas, node, cx, cy)

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

    private fun drawNodeGlyph(canvas: Canvas, node: CampaignNode, cx: Float, cy: Float) {
        textPaint.color = if (node.isCleared) Color.argb(150, 255, 255, 255) else Color.WHITE
        textPaint.textSize = 18f
        val glyph = when (node.type) {
            NodeType.TOWN -> "♜"
            NodeType.VILLAGE -> "⌂"
            NodeType.RECOVERY_CAMP -> "⛺"
            NodeType.FACTION_OUTPOST -> "⚑"
            NodeType.ENEMY_PATROL -> "⚔"
            NodeType.ELITE_CHALLENGE -> "⛬"
            NodeType.BOSS -> "☗"
            NodeType.RESOURCE_CACHE -> "◉"
            NodeType.START -> "✦"
        }
        canvas.drawText(glyph, cx, cy + 6f, textPaint)
        textPaint.textSize = 26f
        textPaint.color = Color.WHITE
    }

    private fun drawNodeShape(canvas: Canvas, node: CampaignNode, cx: Float, cy: Float) {
        when (node.type) {
            NodeType.TOWN -> {
                val r = nodeRadius + 4f
                canvas.drawRect(cx - r, cy - r * 0.8f, cx + r, cy + r * 0.8f, nodePaint)
                canvas.drawRect(cx - r, cy - r * 0.8f, cx + r, cy + r * 0.8f, nodeStrokePaint)
            }
            NodeType.VILLAGE -> {
                val path = Path().apply {
                    moveTo(cx, cy - nodeRadius - 3f)
                    lineTo(cx + nodeRadius + 4f, cy + nodeRadius * 0.7f)
                    lineTo(cx - nodeRadius - 4f, cy + nodeRadius * 0.7f)
                    close()
                }
                canvas.drawPath(path, nodePaint)
                canvas.drawPath(path, nodeStrokePaint)
            }
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
            NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE -> {
                val r = nodeRadius + 2f
                val path = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.88f, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.88f, cy)
                    close()
                }
                canvas.drawPath(path, nodePaint)
                canvas.drawPath(path, nodeStrokePaint)
            }
            NodeType.FACTION_OUTPOST -> {
                val rect = RectF(cx - nodeRadius, cy - nodeRadius * 0.72f, cx + nodeRadius, cy + nodeRadius * 0.72f)
                canvas.drawRoundRect(rect, 22f, 22f, nodePaint)
                canvas.drawRoundRect(rect, 22f, 22f, nodeStrokePaint)
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

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#E8CB62")
            strokeWidth = 4f
        }
        canvas.drawCircle(px, py, 17f, ringPaint)
        playerPaint.color = Color.parseColor("#D1AF4B")
        canvas.drawCircle(px, py, 11f, playerPaint)
        playerPaint.color = Color.parseColor("#2D2A20")
        canvas.drawCircle(px, py, 5.5f, playerPaint)

        val heading = atan2(playerLookDirY, playerLookDirX)
        val pointer = Path().apply {
            moveTo(px + cos(heading) * 27f, py + sin(heading) * 27f)
            lineTo(px + cos(heading + 2.5f) * 10f, py + sin(heading + 2.5f) * 10f)
            lineTo(px + cos(heading - 2.5f) * 10f, py + sin(heading - 2.5f) * 10f)
            close()
        }
        val lookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F2DE9B") }
        canvas.drawPath(pointer, lookPaint)

        playerIconPaint.color = Color.parseColor("#242219")
        canvas.drawText("✦", px, py + 7f, playerIconPaint)
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
        val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#D5DDEE")
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val threatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#DD7B7B")
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(10f, 8f), pulseValue * 15f)
            alpha = 180
        }

        for (party in enemyParties) {
            val node = nodes.find { it.id == party.nodeId } ?: continue
            val profile = partyVisualProfile(party)
            val partyPos = enemyDisplayPositions[party.id] ?: Pair(node.mapX, node.mapY)
            if (!node.isRevealed) continue
            val isVisibleByNode = inViewport(node.mapX, node.mapY, 0.2f)
            val isVisibleByPartyPos = inViewport(partyPos.first, partyPos.second, 0.2f)
            if (!isVisibleByNode && !isVisibleByPartyPos) continue
            val x = screenX((partyPos.first + profile.offsetNormX).coerceIn(0.02f, 0.98f))
            val y = screenY((partyPos.second + profile.offsetNormY).coerceIn(0.02f, 0.98f))
            val rangeRadius = worldRadiusToPixels(profile.rangeRadiusNorm)

            if (party.faction == PartyFaction.FRIENDLY) {
                markerPaint.color = Color.parseColor("#2d7fd6")
                pulseRingPaint.color = Color.parseColor("#A5D5FF")
                symbolPaint.color = Color.parseColor("#eaf4ff")
                labelPaint.color = Color.parseColor("#8ec9ff")
            } else {
                markerPaint.color = Color.parseColor("#c32626")
                pulseRingPaint.color = Color.parseColor("#FF9C9C")
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

            pulseRingPaint.strokeWidth = 2f
            pulseRingPaint.alpha = 210
            canvas.drawCircle(x, y, profile.iconRadiusPx + 2f, pulseRingPaint)
            canvas.drawCircle(x, y, profile.iconRadiusPx, markerPaint)
            val biome = biomeAt(partyPos.first, partyPos.second)
            val symbol = if (party.faction == PartyFaction.FRIENDLY) "△" else enemySymbolForBiome(biome)
            canvas.drawText(symbol, x, y + 9f, symbolPaint)
            canvas.drawText(
                if (party.faction == PartyFaction.FRIENDLY) "ALLY L${profile.level}" else "${enemyFamilyForBiome(biome)} L${profile.level}",
                x,
                y - (profile.iconRadiusPx + 9f),
                labelPaint
            )
            val state = partyMovementState(party)
            canvas.drawText(state, x, y + profile.iconRadiusPx + 16f, statePaint)

            if (party.faction == PartyFaction.HOSTILE) {
                val dx = partyPos.first - playerNormX
                val dy = partyPos.second - playerNormY
                val dist = hypot(dx, dy)
                if (dist < 0.14f) {
                    canvas.drawLine(x, y, screenX(playerNormX), screenY(playerNormY), threatPaint)
                }
            }
        }
    }

    private fun partyMovementState(party: EnemyParty): String {
        val isMovingParty = party.travelFromNodeId != null && party.travelToNodeId != null && party.travelProgress in 0.01f..0.99f
        if (party.faction == PartyFaction.FRIENDLY) {
            return if (isMovingParty) "TRAVELLING" else "IDLE"
        }
        return if (isMovingParty) "PURSUING" else "IDLE"
    }

    private fun enemyFamilyForBiome(biome: BiomeType): String = when (biome) {
        BiomeType.DESERT -> "DUNE RAIDERS"
        BiomeType.PLAINS -> "HORSE WARBAND"
        BiomeType.FOREST -> "FOREST BANDITS"
        BiomeType.HILLS -> "HILL AMBUSHERS"
    }

    private fun enemySymbolForBiome(biome: BiomeType): String = when (biome) {
        BiomeType.DESERT -> "◇"
        BiomeType.PLAINS -> "✦"
        BiomeType.FOREST -> "◬"
        BiomeType.HILLS -> "◆"
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

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).coerceAtMost(255f).toInt()
        val g = (Color.green(color) * factor).coerceAtMost(255f).toInt()
        val b = (Color.blue(color) * factor).coerceAtMost(255f).toInt()
        return Color.argb(Color.alpha(color), r, g, b)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled || isMoving) return false
        scaleDetector.onTouchEvent(event)
        if (::gestureDetector.isInitialized) {
            gestureDetector.onTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    multiTouchStartTime = event.eventTime
                    multiTouchStartMidX = (event.getX(0) + event.getX(1)) / 2f
                    multiTouchStartMidY = (event.getY(0) + event.getY(1)) / 2f
                }
                return true
            }

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

            MotionEvent.ACTION_POINTER_UP -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 2) {
                    val elapsed = event.eventTime - multiTouchStartTime
                    val activeIndex = if (event.actionIndex == 0) 1 else 0
                    val midX = (event.getX(activeIndex) + event.getX(event.actionIndex)) / 2f
                    val midY = (event.getY(activeIndex) + event.getY(event.actionIndex)) / 2f
                    if (elapsed < 220L && hypot(midX - multiTouchStartMidX, midY - multiTouchStartMidY) < 26f) {
                        cameraZoom = (cameraZoom - 0.18f).coerceIn(minZoom, maxZoom)
                        invalidate()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
