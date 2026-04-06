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
import kotlin.math.roundToInt

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
    private var routePreviewTargetNorm: Pair<Float, Float>? = null
    private var routePreviewCommitted: Boolean = false
    private var previewRoutePoints: List<Pair<Float, Float>> = emptyList()
    private var movementRoutePoints: List<Pair<Float, Float>> = emptyList()
    private var activeRouteType: RouteType = RouteType.OFF_ROAD

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
    private var ambientAnimator: ValueAnimator? = null
    private var pulseValue: Float = 0f
    private var ambientValue: Float = 0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#231A18") }
    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        alpha = 28
    }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#5A4A3A")
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    private val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#6F5B4E")
        strokeWidth = 1.8f
        alpha = 65
    }
    private val landShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1b12101A")
    }
    private val riverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#5A6057")
        alpha = 180
        strokeWidth = 7f
    }
    private val dryRiverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#7f6b4f")
        pathEffect = DashPathEffect(floatArrayOf(16f, 11f), 0f)
        strokeWidth = 3f
        alpha = 120
    }
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#d9111624")
    }
    private val fogHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F322C")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7E4E45")
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
        color = Color.parseColor("#C3B5A2")
        textSize = 17f
        textAlign = Paint.Align.CENTER
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#aa0a0a18")
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val clearedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6E8A62")
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
    private val nearbyNodeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#7AB89B84")
        strokeWidth = 1.7f
        alpha = 80
    }
    private val selectedNodeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#D9C8B3")
        strokeWidth = 3f
    }
    private val playerRouteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D6C6B1")
        textSize = 11f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val playerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E8CB62")
        strokeWidth = 4f
    }
    private val playerLookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2DE9B")
    }

    private val nodeRadius = 34f
    private val mapLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        color = Color.parseColor("#8C7A66")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        letterSpacing = 0.12f
    }
    private val ambiencePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val campfirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#A77636")
        alpha = 120
    }

    private enum class ZoomState { FAR, MID, CLOSE }
    private enum class RouteType { ROAD, OFF_ROAD, RISKY }

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
        ambientAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 9000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                ambientValue = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        ambientAnimator?.cancel()
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

        val route = buildTravelRoute(startX, startY, endX, endY)
        movementRoutePoints = route.points
        activeRouteType = route.type
        val routeLength = routeNormLength(movementRoutePoints).coerceAtLeast(0.04f)

        isMoving = true
        moveWasCancelled = false
        travelStartNorm = Pair(startX, startY)
        travelTargetNorm = Pair(endX, endY)
        travelProgress = 0f
        routePreviewCommitted = true
        routePreviewTargetNorm = Pair(endX, endY)
        previewRoutePoints = movementRoutePoints
        trail.clear()
        lastTrailNX = startX
        lastTrailNY = startY

        moveAnimator?.cancel()
        val durationFactor = when (route.type) {
            RouteType.ROAD -> 0.92f
            RouteType.OFF_ROAD -> 1.0f
            RouteType.RISKY -> 1.06f
        }
        val terrainFactor = when (dominantBiomeOnRoute(movementRoutePoints)) {
            BiomeType.FOREST -> 1.10f
            BiomeType.HILLS -> 1.16f
            else -> 0.96f
        }
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (520f + routeLength * 1100f * durationFactor * terrainFactor).roundToInt().toLong()
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val routePoint = pointOnRoute(movementRoutePoints, t)
                playerNormX = routePoint.first
                playerNormY = routePoint.second
                travelProgress = t
                onProgress?.invoke(t, playerNormX, playerNormY)
                if (followPlayerFocus) {
                    cameraNormX = playerNormX
                    cameraNormY = playerNormY
                }
                val tdx = playerNormX - lastTrailNX
                val tdy = playerNormY - lastTrailNY
                if (tdx * tdx + tdy * tdy > 0.0005f) {
                    trail.add(Pair(playerNormX, playerNormY))
                    if (trail.size > 14) trail.removeAt(0)
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

    fun previewRouteTo(normX: Float, normY: Float, committed: Boolean = false) {
        val target = Pair(normX.coerceIn(0.02f, 0.98f), normY.coerceIn(0.02f, 0.98f))
        routePreviewTargetNorm = target
        routePreviewCommitted = committed
        val route = buildTravelRoute(playerNormX, playerNormY, target.first, target.second)
        previewRoutePoints = route.points
        activeRouteType = route.type
        invalidate()
    }

    fun clearRoutePreview() {
        routePreviewTargetNorm = null
        routePreviewCommitted = false
        previewRoutePoints = emptyList()
        invalidate()
    }

    fun currentPreviewRouteTypeLabel(): String = when (activeRouteType) {
        RouteType.ROAD -> "ROAD ROUTE"
        RouteType.OFF_ROAD -> "OFF-ROAD ROUTE"
        RouteType.RISKY -> "THREATENED ROUTE"
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
        routePreviewTargetNorm = null
        routePreviewCommitted = false
        previewRoutePoints = emptyList()
        movementRoutePoints = emptyList()
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
        drawWorldAtmosphere(canvas)
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

        drawBiome(canvas, 0.50f, 0.50f, 1.32f, 1.24f, "#5A4A3F", BiomeType.PLAINS)
        drawBiome(canvas, 0.79f, 0.24f, 0.52f, 0.36f, "#7F6345", BiomeType.DESERT)
        drawBiome(canvas, 0.58f, 0.71f, 0.60f, 0.42f, "#3E4A3F", BiomeType.FOREST)
        drawBiome(canvas, 0.21f, 0.21f, 0.45f, 0.36f, "#5B524D", BiomeType.HILLS)
        drawBiome(canvas, 0.20f, 0.74f, 0.34f, 0.24f, "#6C5E4A", BiomeType.PLAINS)

        drawTerrainRelief(canvas)
        drawRiverbeds(canvas)

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
                intArrayOf(Color.argb(18, 60, 40, 36), Color.argb(158, 28, 18, 20), Color.argb(220, 16, 12, 14)),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(sx, sy, radius, fogHolePaint)
        }
        fogHolePaint.shader = null
    }

    private fun drawWorldAtmosphere(canvas: Canvas) {
        val parallaxX = (cameraNormX - 0.5f) * 22f
        val parallaxY = (cameraNormY - 0.5f) * 16f

        // Desert dust
        ambiencePaint.color = Color.parseColor("#809E7F5F")
        ambiencePaint.alpha = 24
        repeat(4) { i ->
            val drift = (ambientValue * 2f + i * 0.22f) % 1f
            val x = screenX(0.62f + drift * 0.32f) - parallaxX * 0.5f
            val y = screenY(0.11f + i * 0.06f) + parallaxY * 0.2f
            canvas.drawOval(RectF(x - 42f, y - 14f, x + 42f, y + 14f), ambiencePaint)
        }

        // Forest mist
        ambiencePaint.color = Color.parseColor("#706C6E62")
        ambiencePaint.alpha = 22
        repeat(3) { i ->
            val drift = ((ambientValue + i * 0.27f) % 1f)
            val x = screenX(0.40f + drift * 0.30f) - parallaxX * 0.4f
            val y = screenY(0.60f + i * 0.09f) - parallaxY * 0.3f
            canvas.drawOval(RectF(x - 65f, y - 18f, x + 65f, y + 18f), ambiencePaint)
        }

        // Soft moving cloud shadows
        ambiencePaint.color = Color.parseColor("#4D111922")
        ambiencePaint.alpha = 18
        val cloudDrift = ((ambientValue * 0.6f) % 1f)
        canvas.drawOval(
            RectF(
                screenX(0.12f + cloudDrift * 0.74f) - 150f - parallaxX,
                screenY(0.26f) - 34f - parallaxY,
                screenX(0.12f + cloudDrift * 0.74f) + 150f - parallaxX,
                screenY(0.26f) + 34f - parallaxY
            ),
            ambiencePaint
        )

        // River shimmer
        ambiencePaint.color = Color.parseColor("#556E6E67")
        ambiencePaint.alpha = 28
        val shimmer = sin(ambientValue * 6.28f) * 5f
        canvas.drawRect(
            screenX(0.41f), screenY(0.20f) + shimmer,
            screenX(0.82f), screenY(0.40f) + shimmer + 4f, ambiencePaint
        )

        // Distant life markers
        campfirePaint.alpha = 75
        canvas.drawCircle(screenX(0.25f), screenY(0.45f), 3.6f, campfirePaint)
        canvas.drawCircle(screenX(0.66f), screenY(0.73f), 3.6f, campfirePaint)
        ambiencePaint.color = Color.parseColor("#BFAE9A84")
        ambiencePaint.alpha = 65
        canvas.drawCircle(screenX(0.25f) + 3f, screenY(0.45f) - 10f - pulseValue * 2f, 4.5f, ambiencePaint)
        canvas.drawCircle(screenX(0.66f) + 3f, screenY(0.73f) - 10f - pulseValue * 2f, 4.5f, ambiencePaint)

        // Simple birds over forest
        val birdX = screenX(0.48f + ambientValue * 0.18f)
        val birdY = screenY(0.63f + sin(ambientValue * 6.28f) * 0.01f)
        val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#907C776A")
            alpha = 100
        }
        canvas.drawArc(RectF(birdX - 8f, birdY - 3f, birdX, birdY + 3f), 180f, 170f, false, birdPaint)
        canvas.drawArc(RectF(birdX, birdY - 3f, birdX + 8f, birdY + 3f), 190f, 170f, false, birdPaint)
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
            BiomeType.DESERT -> Color.parseColor("#8E734F")
            BiomeType.PLAINS -> Color.parseColor("#7A6C58")
            BiomeType.FOREST -> Color.parseColor("#4F5C4D")
            BiomeType.HILLS -> Color.parseColor("#716761")
        }
        val step = when (biome) {
            BiomeType.DESERT -> 44f
            BiomeType.PLAINS -> 60f
            BiomeType.FOREST -> 58f
            BiomeType.HILLS -> 36f
        }
        var y = top + 8f
        while (y <= bottom - 8f) {
            if (biome == BiomeType.HILLS || biome == BiomeType.DESERT) {
                canvas.drawLine(left + 10f, y, right - 10f, y + if (biome == BiomeType.HILLS) 3f else 1f, texturePaint)
            }
            y += step
        }
    }

    private fun drawTerrainRelief(canvas: Canvas) {
        val reliefShapes = listOf(
            listOf(0.09f to 0.13f, 0.17f to 0.09f, 0.30f to 0.12f, 0.36f to 0.21f, 0.28f to 0.28f, 0.14f to 0.24f), // hill mass
            listOf(0.36f to 0.12f, 0.46f to 0.16f, 0.54f to 0.22f, 0.50f to 0.30f, 0.40f to 0.26f), // ridge toward pass
            listOf(0.47f to 0.28f, 0.58f to 0.30f, 0.67f to 0.36f, 0.64f to 0.45f, 0.52f to 0.42f) // canyon edge
        )
        reliefShapes.forEachIndexed { idx, points ->
            val path = Path()
            points.forEachIndexed { i, point ->
                val x = screenX(point.first)
                val y = screenY(point.second)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            landShadowPaint.alpha = 22 + idx * 12
            canvas.drawPath(path, landShadowPaint)
        }

        contourPaint.alpha = 65
        repeat(7) { i ->
            val t = i / 6f
            val curve = Path().apply {
                moveTo(screenX(0.08f + t * 0.14f), screenY(0.08f + t * 0.16f))
                cubicTo(
                    screenX(0.14f + t * 0.10f), screenY(0.06f + t * 0.19f),
                    screenX(0.28f + t * 0.09f), screenY(0.13f + t * 0.18f),
                    screenX(0.34f + t * 0.08f), screenY(0.22f + t * 0.12f)
                )
            }
            canvas.drawPath(curve, contourPaint)
        }
    }

    private fun drawRiverbeds(canvas: Canvas) {
        val riverPath = Path().apply {
            moveTo(screenX(0.08f), screenY(0.09f))
            cubicTo(screenX(0.19f), screenY(0.12f), screenX(0.35f), screenY(0.20f), screenX(0.44f), screenY(0.23f))
            cubicTo(screenX(0.53f), screenY(0.26f), screenX(0.63f), screenY(0.34f), screenX(0.70f), screenY(0.36f))
            cubicTo(screenX(0.79f), screenY(0.39f), screenX(0.87f), screenY(0.43f), screenX(0.91f), screenY(0.48f))
        }
        riverPaint.strokeWidth = 7.5f
        canvas.drawPath(riverPath, riverPaint)
        riverPaint.strokeWidth = 3.5f
        riverPaint.color = Color.parseColor("#7F7A6A")
        riverPaint.alpha = 90
        canvas.drawPath(riverPath, riverPaint)
        riverPaint.color = Color.parseColor("#5A6057")
        riverPaint.alpha = 180

        val dryBed = Path().apply {
            moveTo(screenX(0.52f), screenY(0.40f))
            cubicTo(screenX(0.46f), screenY(0.48f), screenX(0.40f), screenY(0.55f), screenX(0.34f), screenY(0.66f))
            cubicTo(screenX(0.30f), screenY(0.73f), screenX(0.24f), screenY(0.81f), screenX(0.19f), screenY(0.89f))
        }
        canvas.drawPath(dryBed, dryRiverPaint)
    }

    private fun drawLandmarks(canvas: Canvas) {
        val zoom = zoomState()
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

        // Biome-specific camp props around likely spawn zones.
        landmarkPaint.color = Color.parseColor("#9E7F59") // desert worn cloth / wood
        canvas.drawRect(screenX(0.76f) - 9f, screenY(0.28f) - 4f, screenX(0.76f) + 9f, screenY(0.28f) + 4f, landmarkPaint)
        landmarkPaint.color = Color.parseColor("#3F5A44") // forest traps
        canvas.drawCircle(screenX(0.54f), screenY(0.66f), 6f, landmarkPaint)
        landmarkPaint.color = Color.parseColor("#7A7D82") // hill spikes / stone
        canvas.drawRect(screenX(0.24f) - 7f, screenY(0.24f) - 7f, screenX(0.24f) + 7f, screenY(0.24f) + 7f, landmarkPaint)
        landmarkPaint.color = Color.parseColor("#B29B6F") // plains horse-post feel
        canvas.drawRect(screenX(0.29f) - 10f, screenY(0.78f) - 3f, screenX(0.29f) + 10f, screenY(0.78f) + 3f, landmarkPaint)

        if (zoom != ZoomState.FAR) {
            val chokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = if (zoom == ZoomState.CLOSE) 14f else 11f
            color = Color.parseColor("#D4C5AF")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            canvas.drawText("MOUNTAIN PASS", screenX(0.40f), screenY(0.24f), chokePaint)
            canvas.drawText("RIVER CROSSING", screenX(0.64f), screenY(0.37f), chokePaint)
            canvas.drawText("CANYON PATH", screenX(0.39f), screenY(0.57f), chokePaint)
            canvas.drawText("FOREST EDGE ROUTE", screenX(0.47f), screenY(0.60f), chokePaint)
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
                    BiomeType.PLAINS -> Color.parseColor("#7A6B59")
                    BiomeType.FOREST -> Color.parseColor("#5E5A4A")
                    BiomeType.HILLS -> Color.parseColor("#75695E")
                }
                roadPaint.alpha = if (showPaths) 150 else 105
                roadPaint.strokeWidth = 2.5f
                val sx = screenX(node.mapX)
                val sy = screenY(node.mapY)
                val tx = screenX(toNode.mapX)
                val ty = screenY(toNode.mapY)
                val midNX = (node.mapX + toNode.mapX) / 2f
                val midNY = (node.mapY + toNode.mapY) / 2f
                val terrainDeflection = when (biomeAt(midNX, midNY)) {
                    BiomeType.HILLS -> 0.032f
                    BiomeType.FOREST -> 0.020f
                    else -> 0.012f
                }
                val dx = toNode.mapX - node.mapX
                val dy = toNode.mapY - node.mapY
                val len = hypot(dx, dy).coerceAtLeast(0.001f)
                val nx = -dy / len
                val ny = dx / len
                val cx = screenX((midNX + nx * terrainDeflection).coerceIn(0.02f, 0.98f))
                val cy = screenY((midNY + ny * terrainDeflection).coerceIn(0.02f, 0.98f))
                val roadPath = Path().apply {
                    moveTo(sx, sy)
                    quadTo(cx, cy, tx, ty)
                }
                canvas.drawPath(roadPath, roadPaint)
            }
        }
    }

    private fun drawRegionLabels(canvas: Canvas) {
        val zoom = zoomState()
        if (zoom == ZoomState.CLOSE) return
        mapLabelPaint.alpha = if (zoom == ZoomState.FAR) 190 else 110
        canvas.drawText("ASHEN DUNES", screenX(0.77f), screenY(0.16f), mapLabelPaint)
        canvas.drawText("IRON HILLS", screenX(0.23f), screenY(0.10f), mapLabelPaint)
        canvas.drawText("SABLE WOODS", screenX(0.58f), screenY(0.80f), mapLabelPaint)
        canvas.drawText("BROKEN PLAINS", screenX(0.22f), screenY(0.86f), mapLabelPaint)
        mapLabelPaint.alpha = 255
    }

    private fun zoomState(): ZoomState = when {
        cameraZoom < 1.18f -> ZoomState.FAR
        cameraZoom < 1.95f -> ZoomState.MID
        else -> ZoomState.CLOSE
    }

    private data class TravelRoute(val points: List<Pair<Float, Float>>, val type: RouteType)

    private fun buildTravelRoute(startX: Float, startY: Float, endX: Float, endY: Float): TravelRoute {
        val start = Pair(startX, startY)
        val end = Pair(endX, endY)
        val nearestA = nearestNode(startX, startY)
        val nearestB = nearestNode(endX, endY)
        val nearRoad = nearestA != null && nearestB != null &&
            distanceNorm(startX, startY, nearestA.mapX, nearestA.mapY) < 0.08f &&
            distanceNorm(endX, endY, nearestB.mapX, nearestB.mapY) < 0.08f

        val routeType = when {
            routeRiskTo(endX, endY) == RouteRisk.INTERCEPT -> RouteType.RISKY
            nearRoad -> RouteType.ROAD
            else -> RouteType.OFF_ROAD
        }

        val points = mutableListOf(start)
        if (nearRoad) {
            val ax = nearestA!!.mapX
            val ay = nearestA.mapY
            val bx = nearestB!!.mapX
            val by = nearestB.mapY
            points.add(Pair((startX + ax) / 2f, (startY + ay) / 2f))
            points.add(Pair((ax + bx) / 2f + (ay - by) * 0.05f, (ay + by) / 2f + (bx - ax) * 0.05f))
            points.add(Pair((endX + bx) / 2f, (endY + by) / 2f))
        } else {
            val mx = (startX + endX) / 2f
            val my = (startY + endY) / 2f
            val dx = endX - startX
            val dy = endY - startY
            val len = hypot(dx, dy).coerceAtLeast(0.001f)
            val nx = -dy / len
            val ny = dx / len
            val terrain = biomeAt(mx, my)
            val bend = when (terrain) {
                BiomeType.FOREST -> 0.032f
                BiomeType.HILLS -> 0.028f
                else -> 0.018f
            }
            points.add(Pair((mx + nx * bend).coerceIn(0.02f, 0.98f), (my + ny * bend).coerceIn(0.02f, 0.98f)))
            if (terrain == BiomeType.FOREST) {
                points.add(Pair((mx - nx * (bend * 0.8f)).coerceIn(0.02f, 0.98f), (my - ny * (bend * 0.8f)).coerceIn(0.02f, 0.98f)))
            }
        }
        points.add(end)
        return TravelRoute(points, routeType)
    }

    private fun nearestNode(x: Float, y: Float): CampaignNode? = nodes.minByOrNull { distanceNorm(x, y, it.mapX, it.mapY) }

    private fun routeNormLength(points: List<Pair<Float, Float>>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 0 until points.lastIndex) {
            total += distanceNorm(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second)
        }
        return total
    }

    private fun dominantBiomeOnRoute(points: List<Pair<Float, Float>>): BiomeType {
        if (points.isEmpty()) return BiomeType.PLAINS
        val counts = mutableMapOf(BiomeType.DESERT to 0, BiomeType.PLAINS to 0, BiomeType.FOREST to 0, BiomeType.HILLS to 0)
        points.forEach { (x, y) -> counts[biomeAt(x, y)] = (counts[biomeAt(x, y)] ?: 0) + 1 }
        return counts.maxByOrNull { it.value }?.key ?: BiomeType.PLAINS
    }

    private fun pointOnRoute(points: List<Pair<Float, Float>>, progress: Float): Pair<Float, Float> {
        if (points.size < 2) return points.firstOrNull() ?: Pair(playerNormX, playerNormY)
        val t = progress.coerceIn(0f, 1f)
        val total = routeNormLength(points).coerceAtLeast(0.0001f)
        var traveled = t * total
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            val seg = distanceNorm(a.first, a.second, b.first, b.second)
            if (traveled <= seg || i == points.lastIndex - 1) {
                val st = if (seg <= 0.00001f) 0f else (traveled / seg).coerceIn(0f, 1f)
                return Pair(a.first + (b.first - a.first) * st, a.second + (b.second - a.second) * st)
            }
            traveled -= seg
        }
        return points.last()
    }

    private fun distanceNorm(x1: Float, y1: Float, x2: Float, y2: Float): Float = hypot(x2 - x1, y2 - y1)

    private fun drawTravelProjection(canvas: Canvas) {
        val points = when {
            isMoving && movementRoutePoints.size > 1 -> movementRoutePoints
            previewRoutePoints.size > 1 -> previewRoutePoints
            else -> return
        }

        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 2.2f
            color = when (activeRouteType) {
                RouteType.ROAD -> Color.parseColor("#A38D73")
                RouteType.OFF_ROAD -> Color.parseColor("#7C6D5C")
                RouteType.RISKY -> Color.parseColor("#8E4A44")
            }
            alpha = if (routePreviewCommitted) 230 else 170
            pathEffect = DashPathEffect(floatArrayOf(12f, 10f), pulseValue * 20f)
        }
        val path = Path()
        points.forEachIndexed { index, pt ->
            val sx = screenX(pt.first)
            val sy = screenY(pt.second)
            if (index == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        canvas.drawPath(path, routePaint)

        if (isMoving) {
            val progressPoint = pointOnRoute(points, travelProgress)
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.parseColor("#D5C8B6")
                strokeWidth = 2.8f
                alpha = 185
            }
            val progressPath = Path()
            progressPath.moveTo(screenX(points.first().first), screenY(points.first().second))
            val sampleCount = 14
            for (i in 1..sampleCount) {
                val t = (travelProgress * i / sampleCount.toFloat()).coerceIn(0f, travelProgress)
                val p = pointOnRoute(points, t)
                progressPath.lineTo(screenX(p.first), screenY(p.second))
            }
            progressPath.lineTo(screenX(progressPoint.first), screenY(progressPoint.second))
            canvas.drawPath(progressPath, progressPaint)
        }

        val destination = points.last()
        val endX = screenX(destination.first)
        val endY = screenY(destination.second)
        val destPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#D9CAB8")
            strokeWidth = 1.8f
            alpha = 210
        }
        val pulse = if (routePreviewCommitted) 1f + pulseValue * 1.8f else 1f + pulseValue * 1.1f
        canvas.drawCircle(endX, endY, 8.5f + pulse * 2f, destPaint)
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
            val alpha = (frac * 95).toInt()
            val radius = 2.2f + frac * 3.2f
            trailPaint.color = Color.argb(alpha, 169, 180, 196)
            canvas.drawCircle(px, py, radius, trailPaint)
        }
    }

    private fun drawNodes(canvas: Canvas) {
        val zoom = zoomState()
        val placedLabels = mutableListOf<Pair<Float, Float>>()
        for (node in nodes) {
            if (!node.isRevealed || !inViewport(node.mapX, node.mapY)) continue
            val cx = screenX(node.mapX)
            val cy = screenY(node.mapY)
            if (node.isCleared && isTemporaryNode(node)) {
                drawClearedTaskMarker(canvas, cx, cy)
                continue
            }
            val nearbyDistance = hypot(playerNormX - node.mapX, playerNormY - node.mapY)
            if (nearbyDistance < 0.11f && selectedNodeId != node.id) {
                canvas.drawCircle(cx, cy, nodeRadius + 7f + pulseValue * 2f, nearbyNodeRingPaint)
            }

            nodePaint.style = Paint.Style.FILL
            nodePaint.color = node.type.color.toInt()
            nodePaint.alpha = if (node.isCleared) 90 else 255
            nodeStrokePaint.color = lighten(node.type.color.toInt(), 1.5f)
            nodeStrokePaint.alpha = nodePaint.alpha

            val zoomScale = when (zoom) {
                ZoomState.FAR -> 0.80f
                ZoomState.MID -> 0.94f
                ZoomState.CLOSE -> 1.06f
            }

            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(zoomScale, zoomScale)
            drawNodeShape(canvas, node, 0f, 0f)
            drawNodeGlyph(canvas, node, 0f, 0f)
            canvas.restore()

            if (node.isCleared) canvas.drawCircle(cx, cy, nodeRadius + 3f, clearedRingPaint)
            if (selectedNodeId == node.id) {
                val selRadius = nodeRadius + 8f + pulseValue * 3f
                canvas.drawCircle(cx, cy, selRadius, selectedNodeRingPaint)
            }

            val label = if (node.isCleared) "✓ ${node.name}" else node.name
            val shouldShowLabel = selectedNodeId == node.id ||
                node.id == currentNodeId ||
                (zoom != ZoomState.FAR && hypot(playerNormX - node.mapX, playerNormY - node.mapY) < 0.12f) ||
                zoom == ZoomState.CLOSE
            if (shouldShowLabel) {
                val overlaps = placedLabels.any { hypot(it.first - cx, it.second - cy) < if (zoom == ZoomState.CLOSE) 40f else 58f }
                if (overlaps && selectedNodeId != node.id && node.id != currentNodeId) continue
                labelPaint.alpha = when {
                    selectedNodeId == node.id || node.id == currentNodeId -> 240
                    zoom == ZoomState.CLOSE -> 220
                    else -> 155
                }
                drawLabel(canvas, label, cx, cy + nodeRadius + 24f)
                labelPaint.alpha = 255
                placedLabels.add(cx to cy + nodeRadius + 24f)
            }
        }
    }

    private fun drawClearedTaskMarker(canvas: Canvas, cx: Float, cy: Float) {
        val markerRadius = 10f
        nodePaint.color = Color.parseColor("#6A5A4A")
        nodePaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, markerRadius, nodePaint)
        nodeStrokePaint.color = Color.parseColor("#A48D74")
        nodeStrokePaint.alpha = 170
        canvas.drawCircle(cx, cy, markerRadius, nodeStrokePaint)

        textPaint.textSize = 14f
        textPaint.color = Color.parseColor("#E5D6C2")
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

        if (previewRoutePoints.size > 1 && !isMoving) {
            val mid = pointOnRoute(previewRoutePoints, 0.5f)
            canvas.drawText(currentPreviewRouteTypeLabel(), screenX(mid.first), screenY(mid.second) - 10f, playerRouteLabelPaint)
        }

        canvas.drawCircle(px, py, 17f, playerRingPaint)
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
        canvas.drawPath(pointer, playerLookPaint)

        playerIconPaint.color = Color.parseColor("#242219")
        canvas.drawText("✦", px, py + 7f, playerIconPaint)
    }

    private fun drawEnemyParties(canvas: Canvas) {
        val zoom = zoomState()
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
            color = Color.parseColor("#E4D4BF")
            typeface = Typeface.DEFAULT_BOLD
        }
        val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#CABAA5")
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
            val iconScale = when (zoom) {
                ZoomState.FAR -> 0.86f
                ZoomState.MID -> 1f
                ZoomState.CLOSE -> 1.14f
            }
            val iconRadius = profile.iconRadiusPx * iconScale

            if (party.faction == PartyFaction.FRIENDLY) {
                markerPaint.color = Color.parseColor("#7A6A52")
                pulseRingPaint.color = Color.parseColor("#CCB995")
                symbolPaint.color = Color.parseColor("#F2E5D1")
                labelPaint.color = Color.parseColor("#D9BF96")
            } else {
                markerPaint.color = Color.parseColor("#8A2F34")
                pulseRingPaint.color = Color.parseColor("#CE8084")
                symbolPaint.color = Color.WHITE
                labelPaint.color = Color.parseColor("#D9989C")
            }

            if (selectedNodeId == node.id && zoom != ZoomState.FAR) {
                pulseRingPaint.strokeWidth = 2f
                pulseRingPaint.alpha = 110
                pulseRingPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 8f), pulseValue * 16f)
                canvas.drawCircle(x, y, rangeRadius, pulseRingPaint)
                pulseRingPaint.pathEffect = null
            }

            pulseRingPaint.strokeWidth = 2f
            pulseRingPaint.alpha = 210
            canvas.drawCircle(x, y, iconRadius + 2f, pulseRingPaint)
            canvas.drawCircle(x, y, iconRadius, markerPaint)
            val biome = biomeAt(partyPos.first, partyPos.second)
            val symbol = if (party.faction == PartyFaction.FRIENDLY) "△" else enemySymbolForBiome(biome)
            canvas.drawText(symbol, x, y + 9f, symbolPaint)
            if (zoom != ZoomState.FAR) {
                canvas.drawText(
                    if (party.faction == PartyFaction.FRIENDLY) "ALLY L${profile.level}" else "${enemyFamilyForBiome(biome)} L${profile.level}",
                    x,
                    y - (iconRadius + 9f),
                    labelPaint
                )
            }
            val state = partyMovementState(party)
            if (zoom == ZoomState.CLOSE) {
                canvas.drawText(state, x, y + iconRadius + 16f, statePaint)
            }

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
        val pos = enemyDisplayPositions[party.id] ?: (nodes.find { it.id == party.nodeId }?.let { it.mapX to it.mapY } ?: (0.5f to 0.5f))
        val distToPlayer = hypot(pos.first - playerNormX, pos.second - playerNormY)
        return when {
            isMovingParty && distToPlayer < 0.17f -> "PURSUING"
            !isMovingParty && distToPlayer < 0.12f -> "WATCHING"
            isMovingParty && distToPlayer > 0.30f -> "RETREATING"
            else -> "ROAMING"
        }
    }

    private fun enemyFamilyForBiome(biome: BiomeType): String = when (biome) {
        BiomeType.DESERT -> "CARAVAN THIEVES"
        BiomeType.PLAINS -> "ROAMING WARBAND"
        BiomeType.FOREST -> "POACHER WOLFPACK"
        BiomeType.HILLS -> "HILL CLANS"
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

    private enum class RouteRisk { SAFE, THREATENED, INTERCEPT }

    private fun routeRiskTo(targetX: Float, targetY: Float): RouteRisk {
        var closest = 1f
        for (party in enemyParties) {
            if (party.faction != PartyFaction.HOSTILE) continue
            val pos = enemyDisplayPositions[party.id] ?: (nodes.find { it.id == party.nodeId }?.let { it.mapX to it.mapY } ?: continue)
            val dist = hypot(targetX - pos.first, targetY - pos.second)
            if (dist < closest) closest = dist
        }
        return when {
            closest < 0.08f -> RouteRisk.INTERCEPT
            closest < 0.14f -> RouteRisk.THREATENED
            else -> RouteRisk.SAFE
        }
    }

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
