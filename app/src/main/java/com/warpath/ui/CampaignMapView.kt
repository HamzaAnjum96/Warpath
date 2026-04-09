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
import android.view.animation.LinearInterpolator
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
        set(value) {
            val old = field
            field = value
            for ((id, pos) in value) {
                val prev = old[id] ?: continue
                val dx = pos.first - prev.first
                val dy = pos.second - prev.second
                if (dx * dx + dy * dy > 0.00001f) {
                    enemyHeadings[id] = atan2(dy, dx)
                }
            }
            invalidate()
        }
    private val enemyHeadings: MutableMap<String, Float> = mutableMapOf()

    var inputEnabled: Boolean = true
    var showPaths: Boolean = false
    var onFocusChanged: ((Boolean) -> Unit)? = null
    var onMapTapped: ((Float, Float) -> Unit)? = null
    var onEnemyPartyTapped: ((EnemyParty) -> Unit)? = null
    var selectedNodeId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    private var playerNormX: Float = 0.1f
    private var playerNormY: Float = 0.5f
    val currentPlayerNormX: Float get() = playerNormX
    val currentPlayerNormY: Float get() = playerNormY
    private var playerLookDirX: Float = 1f
    private var playerLookDirY: Float = 0f
    private var isMoving: Boolean = false
    var isPaused: Boolean = false
        private set
    private var isRedirecting: Boolean = false
    private var travelTargetNorm: Pair<Float, Float>? = null
    private var travelStartNorm: Pair<Float, Float>? = null
    private var travelProgress: Float = 0f
    private var routePreviewTargetNorm: Pair<Float, Float>? = null
    private var routePreviewCommitted: Boolean = false
    private var previewRoutePoints: List<Pair<Float, Float>> = emptyList()
    private var movementRoutePoints: List<Pair<Float, Float>> = emptyList()
    private var activeRouteType: RouteType = RouteType.DIRECT
    private var activeRouteRisk: RouteRisk = RouteRisk.SAFE

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

    var playerSpeed: Float = 0.12f

    private var moveAnimator: ValueAnimator? = null
    private var cameraAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var ambientAnimator: ValueAnimator? = null
    private var pulseValue: Float = 0f
    private var ambientValue: Float = 0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#080E18") }
    private object Palette {
        const val MATTE_BASE = "#0D1820"
        const val STEPPE = UiTheme.BIOME_PLAINS
        const val DRY_GROUND = UiTheme.BIOME_DESERT
        const val SETTLEMENT_SOIL = UiTheme.BIOME_SETTLEMENT
        const val FOREST = UiTheme.BIOME_FOREST
        const val HILLS = UiTheme.BIOME_HILLS
        const val AIRWAY_MARK = "#1A6060"
        const val WATER = UiTheme.BIOME_WATER
        const val HUD_TEXT = UiTheme.TEXT_PRIMARY
        const val HUD_LABEL_BG = "#CC16263A"
        const val GOLD = UiTheme.WARNING
    }
    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        alpha = 28
    }
    private val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1E5A6A")
        strokeWidth = 1.8f
        alpha = 90
    }
    private val landShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0A122026")
    }
    private val riverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor(Palette.WATER)
        alpha = 180
        strokeWidth = 7f
    }
    private val dryRiverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#8C7450")
        pathEffect = DashPathEffect(floatArrayOf(16f, 11f), 0f)
        strokeWidth = 3f
        alpha = 120
    }
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#22162234")
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E4A5A")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(UiTheme.WARNING)
        strokeWidth = 4f
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
        typeface = UiTheme.TYPEFACE_HEADING
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(Palette.HUD_TEXT)
        textSize = 17f
        textAlign = Paint.Align.CENTER
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(Palette.HUD_LABEL_BG)
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val clearedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5FAF7A")
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
        typeface = UiTheme.TYPEFACE_HEADING
    }
    private val nearbyNodeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#6E8E9BB0")
        strokeWidth = 1.7f
        alpha = 80
    }
    private val selectedNodeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor(Palette.GOLD)
        strokeWidth = 3f
    }
    private val playerRouteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B8C2D1")
        textSize = 11f
        textAlign = Paint.Align.CENTER
        typeface = UiTheme.TYPEFACE_BODY
    }
    private val playerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor(Palette.GOLD)
        strokeWidth = 4f
    }
    private val playerLookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(Palette.GOLD)
    }
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val routeUnderlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#45121D2E")
    }
    private val hazardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val routeProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#F2F0EA")
        strokeWidth = 2.8f
        alpha = 185
    }
    private val routeDestinationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        alpha = 210
    }
    private val routeChevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 1.4f
        alpha = 225
    }
    private val routeThreatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1.8f
        color = Color.parseColor("#C65A5A")
    }
    private val routePath = Path()
    private val routeProgressPath = Path()
    private val routeChevronPath = Path()

    private val nodeRadius = 34f
    private val mapLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        color = Color.parseColor("#2A6878")
        textAlign = Paint.Align.CENTER
        typeface = UiTheme.TYPEFACE_TITLE
        letterSpacing = 0.12f
    }
    private val ambiencePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val campfirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B89C6B")
        alpha = 120
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2A4A63")
        strokeWidth = 1.1f
        alpha = 62
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor(UiTheme.RADAR_CYAN)
        strokeWidth = 1.5f
        alpha = 46
    }
    private val scanlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#33A9BED2")
        strokeWidth = 1f
        alpha = 16
    }
    private val sweepOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2F4EC7D9")
    }
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val radarSweepRect = RectF()

    private enum class ZoomState { FAR, MID, CLOSE }
    private enum class RouteType { DIRECT, THREAT_AVOIDANCE }
    private enum class RouteRisk { SAFE, THREATENED, INTERCEPT }

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
        val dist = hypot(dx, dy).coerceAtLeast(0.01f)
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (dist / playerSpeed * 1000f).roundToInt().toLong().coerceIn(200L, 12000L)
            interpolator = LinearInterpolator()

            var prevX = startX
            var prevY = startY
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                playerNormX = startX + (endX - startX) * t
                playerNormY = startY + (endY - startY) * t
                travelProgress = t
                val mdx = playerNormX - prevX
                val mdy = playerNormY - prevY
                if (mdx * mdx + mdy * mdy > 0.00001f) {
                    playerLookDirX = mdx
                    playerLookDirY = mdy
                    val len = hypot(playerLookDirX, playerLookDirY)
                    playerLookDirX /= len
                    playerLookDirY /= len
                }
                prevX = playerNormX
                prevY = playerNormY
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
                    if (isRedirecting) {
                        isRedirecting = false
                        return
                    }
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
        activeRouteRisk = route.risk
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
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (routeLength / playerSpeed * 1000f).roundToInt().toLong().coerceIn(200L, 12000L)
            interpolator = LinearInterpolator()
            var prevX = startX
            var prevY = startY
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val routePoint = pointOnRoute(movementRoutePoints, t)
                playerNormX = routePoint.first
                playerNormY = routePoint.second
                travelProgress = t
                val mdx = playerNormX - prevX
                val mdy = playerNormY - prevY
                if (mdx * mdx + mdy * mdy > 0.00001f) {
                    val len = hypot(mdx, mdy)
                    playerLookDirX = mdx / len
                    playerLookDirY = mdy / len
                }
                prevX = playerNormX
                prevY = playerNormY
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
                    if (isRedirecting) {
                        isRedirecting = false
                        return
                    }
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
        activeRouteRisk = route.risk
        invalidate()
    }

    fun clearRoutePreview() {
        routePreviewTargetNorm = null
        routePreviewCommitted = false
        previewRoutePoints = emptyList()
        activeRouteType = RouteType.DIRECT
        activeRouteRisk = RouteRisk.SAFE
        invalidate()
    }

    fun currentPreviewRouteTypeLabel(): String = when (activeRouteRisk) {
        RouteRisk.INTERCEPT -> "INTERCEPT RISK VECTOR"
        RouteRisk.THREATENED -> "THREATENED FLIGHT PATH"
        RouteRisk.SAFE -> if (activeRouteType == RouteType.THREAT_AVOIDANCE) {
            "THREAT-AVOIDANCE FLIGHT PATH"
        } else {
            "DIRECT FLIGHT PATH"
        }
    }

    fun cancelMovement() {
        if (!isMoving) return
        moveAnimator?.cancel()
    }

    fun pauseMovement() {
        if (!isMoving || isPaused) return
        isPaused = true
        moveAnimator?.pause()
        invalidate()
    }

    fun resumeMovement() {
        if (!isPaused) return
        isPaused = false
        moveAnimator?.resume()
        invalidate()
    }

    fun redirectMovement(
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
            return
        }

        isRedirecting = true
        isPaused = false
        moveAnimator?.cancel()

        trail.clear()
        lastTrailNX = startX
        lastTrailNY = startY

        val route = buildTravelRoute(startX, startY, endX, endY)
        movementRoutePoints = route.points
        activeRouteType = route.type
        activeRouteRisk = route.risk
        val routeLength = routeNormLength(movementRoutePoints).coerceAtLeast(0.04f)

        isMoving = true
        moveWasCancelled = false
        travelStartNorm = Pair(startX, startY)
        travelTargetNorm = Pair(endX, endY)
        travelProgress = 0f
        routePreviewCommitted = true
        routePreviewTargetNorm = Pair(endX, endY)
        previewRoutePoints = movementRoutePoints

        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (routeLength / playerSpeed * 1000f).roundToInt().toLong().coerceIn(200L, 12000L)
            interpolator = LinearInterpolator()
            var prevX = startX
            var prevY = startY
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val routePoint = pointOnRoute(movementRoutePoints, t)
                playerNormX = routePoint.first
                playerNormY = routePoint.second
                travelProgress = t
                val mdx = playerNormX - prevX
                val mdy = playerNormY - prevY
                if (mdx * mdx + mdy * mdy > 0.00001f) {
                    val len = hypot(mdx, mdy)
                    playerLookDirX = mdx / len
                    playerLookDirY = mdy / len
                }
                prevX = playerNormX
                prevY = playerNormY
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
                    if (isRedirecting) {
                        isRedirecting = false
                        return
                    }
                    moveWasCancelled = true
                    finishMove(playerNormX, playerNormY, onComplete, cancelled = true)
                }
            })
            start()
        }
    }

    fun stopMovement() {
        if (!isMoving && !isPaused) return
        if (isPaused) {
            isPaused = false
            val anim = moveAnimator
            if (anim != null && anim.isPaused) {
                anim.resume()
                anim.cancel()
            } else {
                finishMove(playerNormX, playerNormY, {}, cancelled = true)
            }
        } else {
            moveAnimator?.cancel()
        }
    }

    fun isMovementActive(): Boolean = isMoving

    fun currentTravelProgress(): Float = travelProgress

    fun currentRouteLength(): Float = routeNormLength(movementRoutePoints)

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
        activeRouteType = RouteType.DIRECT
        activeRouteRisk = RouteRisk.SAFE
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
        drawTacticalGrid(canvas)
        drawWorldAtmosphere(canvas)
        drawFogOfWar(canvas)
        drawTravelProjection(canvas)
        if (showPaths) drawConnections(canvas)
        drawTrail(canvas)
        drawNodes(canvas)
        drawEnemyParties(canvas)
        drawPlayerMarker(canvas)
        drawRadarSweep(canvas)
        drawScanlines(canvas)
        drawVignette(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        terrainPaint.color = Color.parseColor(Palette.MATTE_BASE)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), terrainPaint)
        drawBiome(canvas, 0.46f, 0.48f, 1.22f, 1.18f, Palette.STEPPE, BiomeType.PLAINS)
        drawBiome(canvas, 0.77f, 0.25f, 0.58f, 0.44f, Palette.DRY_GROUND, BiomeType.DESERT)
        drawBiome(canvas, 0.56f, 0.73f, 0.65f, 0.46f, Palette.FOREST, BiomeType.FOREST)
        drawBiome(canvas, 0.20f, 0.22f, 0.48f, 0.38f, Palette.HILLS, BiomeType.HILLS)
        drawBiome(canvas, 0.31f, 0.56f, 0.46f, 0.34f, Palette.SETTLEMENT_SOIL, BiomeType.PLAINS)

        drawTerrainRelief(canvas)
        drawRiverbeds(canvas)

        drawLandmarks(canvas)
        drawRegionLabels(canvas)
    }

    private fun drawFogOfWar(canvas: Canvas) {
        fogPaint.alpha = 34
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fogPaint)
        fogPaint.alpha = 255
    }

    private fun drawWorldAtmosphere(canvas: Canvas) {
        val parallaxX = (cameraNormX - 0.5f) * 22f
        val parallaxY = (cameraNormY - 0.5f) * 16f

        // Desert dust
        ambiencePaint.color = Color.parseColor("#556D7C5A")
        ambiencePaint.alpha = 24
        repeat(4) { i ->
            val drift = (ambientValue * 2f + i * 0.22f) % 1f
            val x = screenX(0.62f + drift * 0.32f) - parallaxX * 0.5f
            val y = screenY(0.11f + i * 0.06f) + parallaxY * 0.2f
            canvas.drawOval(RectF(x - 42f, y - 14f, x + 42f, y + 14f), ambiencePaint)
        }

        // Forest mist
        ambiencePaint.color = Color.parseColor("#553D5A49")
        ambiencePaint.alpha = 22
        repeat(3) { i ->
            val drift = ((ambientValue + i * 0.27f) % 1f)
            val x = screenX(0.40f + drift * 0.30f) - parallaxX * 0.4f
            val y = screenY(0.60f + i * 0.09f) - parallaxY * 0.3f
            canvas.drawOval(RectF(x - 65f, y - 18f, x + 65f, y + 18f), ambiencePaint)
        }

        // Soft moving cloud shadows
        ambiencePaint.color = Color.parseColor("#330A1220")
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
        ambiencePaint.color = Color.parseColor("#332A4D63")
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
        ambiencePaint.color = Color.parseColor("#66B89C6B")
        ambiencePaint.alpha = 65
        canvas.drawCircle(screenX(0.25f) + 3f, screenY(0.45f) - 10f - pulseValue * 2f, 4.5f, ambiencePaint)
        canvas.drawCircle(screenX(0.66f) + 3f, screenY(0.73f) - 10f - pulseValue * 2f, 4.5f, ambiencePaint)

        // Simple birds over forest
        val birdX = screenX(0.48f + ambientValue * 0.18f)
        val birdY = screenY(0.63f + sin(ambientValue * 6.28f) * 0.01f)
        val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#708694A8")
            alpha = 100
        }
        canvas.drawArc(RectF(birdX - 8f, birdY - 3f, birdX, birdY + 3f), 180f, 170f, false, birdPaint)
        canvas.drawArc(RectF(birdX, birdY - 3f, birdX + 8f, birdY + 3f), 190f, 170f, false, birdPaint)
    }

    private fun drawTacticalGrid(canvas: Canvas) {
        val majorStep = width / 6f
        val minorStep = width / 18f
        var x = 0f
        while (x <= width) {
            val majorLine = (x % majorStep) < 1f
            gridPaint.alpha = if (majorLine) 72 else 38
            gridPaint.strokeWidth = if (majorLine) 1.5f else 1f
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += minorStep
        }
        var y = 0f
        while (y <= height) {
            gridPaint.alpha = if ((y % (height / 6f)) < 1f) 72 else 34
            gridPaint.strokeWidth = if ((y % (height / 6f)) < 1f) 1.5f else 1f
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += height / 18f
        }
        val cx = screenX(playerNormX)
        val cy = screenY(playerNormY)
        for (i in 1..3) {
            ringPaint.alpha = 44 - i * 8
            canvas.drawCircle(cx, cy, i * 90f, ringPaint)
        }
    }

    private fun drawRadarSweep(canvas: Canvas) {
        val cx = screenX(playerNormX)
        val cy = screenY(playerNormY)
        val radius = (width.coerceAtLeast(height) * 0.42f)
        val sweepAngle = ambientValue * 360f
        radarSweepRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        val alphaBase = (16 + pulseValue * 12f).toInt().coerceIn(12, 32)
        sweepOverlayPaint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                Color.argb(alphaBase, 78, 199, 217),
                Color.argb(alphaBase / 2, 78, 199, 217),
                Color.argb(0, 78, 199, 217)
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        sweepOverlayPaint.alpha = 255
        canvas.drawArc(radarSweepRect, sweepAngle - 18f, 36f, true, sweepOverlayPaint)
        sweepOverlayPaint.shader = null
    }

    private fun drawScanlines(canvas: Canvas) {
        var y = 0f
        while (y <= height) {
            scanlinePaint.alpha = if (((y / 4f).toInt() % 2) == 0) 16 else 10
            canvas.drawLine(0f, y, width.toFloat(), y, scanlinePaint)
            y += 4f
        }
    }

    private fun drawVignette(canvas: Canvas) {
        val shader = RadialGradient(
            width / 2f,
            height / 2f,
            width * 0.72f,
            intArrayOf(Color.parseColor("#00000000"), Color.parseColor("#8A050A12")),
            floatArrayOf(0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        vignettePaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
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
            BiomeType.DESERT -> Color.parseColor("#8F7958")
            BiomeType.PLAINS -> Color.parseColor("#63745A")
            BiomeType.FOREST -> Color.parseColor("#435C50")
            BiomeType.HILLS -> Color.parseColor("#6A7480")
        }
        // Inner breakup patches for texture variation
        val patchRadius = when (biome) {
            BiomeType.DESERT -> 36f
            BiomeType.PLAINS -> 42f
            BiomeType.FOREST -> 34f
            BiomeType.HILLS -> 30f
        }
        repeat(18) { i ->
            val fx = (i * 0.17f + centerX * 0.53f) % 1f
            val fy = (i * 0.11f + centerY * 0.67f) % 1f
            val px = left + (right - left) * fx
            val py = top + (bottom - top) * fy
            canvas.drawOval(RectF(px - patchRadius, py - patchRadius * 0.55f, px + patchRadius, py + patchRadius * 0.55f), texturePaint)
        }
        // Biome-specific micro features for breakup
        val microPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        when (biome) {
            BiomeType.DESERT -> {
                // Sand dune ripples
                microPaint.color = Color.parseColor("#7A664A")
                microPaint.alpha = 35
                repeat(10) { i ->
                    val fx = (i * 0.21f + centerX * 0.37f) % 1f
                    val fy = (i * 0.14f + centerY * 0.51f) % 1f
                    val px = left + (right - left) * fx
                    val py = top + (bottom - top) * fy
                    canvas.drawOval(RectF(px - 28f, py - 5f, px + 28f, py + 5f), microPaint)
                }
            }
            BiomeType.PLAINS -> {
                // Grass tufts
                microPaint.color = Color.parseColor("#6A7D5C")
                microPaint.alpha = 40
                repeat(12) { i ->
                    val fx = (i * 0.19f + centerX * 0.42f) % 1f
                    val fy = (i * 0.15f + centerY * 0.56f) % 1f
                    val px = left + (right - left) * fx
                    val py = top + (bottom - top) * fy
                    canvas.drawCircle(px, py, 4f + (i % 3) * 2f, microPaint)
                }
            }
            BiomeType.FOREST -> {
                // Dense canopy blobs
                microPaint.color = Color.parseColor("#2D4338")
                microPaint.alpha = 50
                repeat(14) { i ->
                    val fx = (i * 0.16f + centerX * 0.45f) % 1f
                    val fy = (i * 0.13f + centerY * 0.58f) % 1f
                    val px = left + (right - left) * fx
                    val py = top + (bottom - top) * fy
                    val r = 8f + (i % 4) * 3.5f
                    canvas.drawCircle(px, py, r, microPaint)
                }
            }
            BiomeType.HILLS -> {
                // Rocky scatter
                microPaint.color = Color.parseColor("#505860")
                microPaint.alpha = 40
                repeat(8) { i ->
                    val fx = (i * 0.23f + centerX * 0.35f) % 1f
                    val fy = (i * 0.18f + centerY * 0.43f) % 1f
                    val px = left + (right - left) * fx
                    val py = top + (bottom - top) * fy
                    canvas.drawRect(px - 4f, py - 3f, px + 4f, py + 3f, microPaint)
                }
            }
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
        riverPaint.color = Color.parseColor("#58606A")
        riverPaint.alpha = 90
        canvas.drawPath(riverPath, riverPaint)
        riverPaint.color = Color.parseColor("#2A4D63")
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

        // Settlement surroundings - villages/towns affect nearby terrain
        for (node in nodes) {
            if (!node.isRevealed) continue
            if (node.type != NodeType.TOWN && node.type != NodeType.VILLAGE && node.type != NodeType.FACTION_OUTPOST) continue
            val sx = screenX(node.mapX)
            val sy = screenY(node.mapY)
            if (!inViewport(node.mapX, node.mapY, 0.3f)) continue
            val settlementRadius = if (node.type == NodeType.TOWN) 52f else 36f
            // Cleared ground ring
            landmarkPaint.color = Color.parseColor(Palette.SETTLEMENT_SOIL)
            landmarkPaint.alpha = 65
            canvas.drawOval(RectF(sx - settlementRadius, sy - settlementRadius * 0.6f, sx + settlementRadius, sy + settlementRadius * 0.6f), landmarkPaint)
            // Tiny buildings / field patches
            landmarkPaint.alpha = 80
            landmarkPaint.color = Color.parseColor("#6B6150")
            val fieldCount = if (node.type == NodeType.TOWN) 6 else 3
            repeat(fieldCount) { i ->
                val angle = i * (6.28f / fieldCount) + node.mapX * 10f
                val dist = settlementRadius * (0.55f + (i % 3) * 0.12f)
                val fx = sx + cos(angle) * dist
                val fy = sy + sin(angle) * dist * 0.6f
                canvas.drawRect(fx - 3f, fy - 2f, fx + 3f, fy + 2f, landmarkPaint)
            }
            // Palisade dots for outposts
            if (node.type == NodeType.FACTION_OUTPOST) {
                landmarkPaint.color = Color.parseColor("#8A7A5A")
                landmarkPaint.alpha = 90
                repeat(8) { i ->
                    val angle = i * (6.28f / 8)
                    canvas.drawCircle(sx + cos(angle) * 28f, sy + sin(angle) * 20f, 2f, landmarkPaint)
                }
            }
            landmarkPaint.alpha = 255
        }

        // Ruins
        val ruins = listOf(0.32f to 0.28f, 0.72f to 0.76f, 0.15f to 0.57f)
        landmarkPaint.color = Color.parseColor("#707985")
        ruins.forEach { (nx, ny) ->
            if (!inViewport(nx, ny, 0.2f)) return@forEach
            val sx = screenX(nx)
            val sy = screenY(ny)
            canvas.drawRect(sx - 8f, sy - 7f, sx + 8f, sy + 7f, landmarkPaint)
            // Broken wall fragment
            landmarkPaint.alpha = 120
            canvas.drawRect(sx - 12f, sy - 4f, sx - 8f, sy + 3f, landmarkPaint)
            canvas.drawRect(sx + 8f, sy - 2f, sx + 11f, sy + 5f, landmarkPaint)
            landmarkPaint.alpha = 255
        }

        // Groves
        val groves = listOf(0.48f to 0.67f, 0.64f to 0.62f, 0.57f to 0.81f)
        landmarkPaint.color = Color.parseColor("#3D5A49")
        groves.forEach { (nx, ny) ->
            if (!inViewport(nx, ny, 0.2f)) return@forEach
            val sx = screenX(nx)
            val sy = screenY(ny)
            canvas.drawCircle(sx, sy, 9f, landmarkPaint)
            canvas.drawCircle(sx + 8f, sy - 4f, 7f, landmarkPaint)
            canvas.drawCircle(sx - 5f, sy + 6f, 6f, landmarkPaint)
            // Undergrowth
            landmarkPaint.alpha = 60
            canvas.drawOval(RectF(sx - 18f, sy - 6f, sx + 18f, sy + 10f), landmarkPaint)
            landmarkPaint.alpha = 255
        }

        // Watch posts
        val watchPosts = listOf(0.84f to 0.34f, 0.26f to 0.46f)
        landmarkPaint.color = Color.parseColor("#A48A63")
        watchPosts.forEach { (nx, ny) ->
            if (!inViewport(nx, ny, 0.2f)) return@forEach
            val sx = screenX(nx)
            val sy = screenY(ny)
            val p = Path().apply {
                moveTo(sx, sy - 8f)
                lineTo(sx + 7f, sy + 7f)
                lineTo(sx - 7f, sy + 7f)
                close()
            }
            canvas.drawPath(p, landmarkPaint)
            // Flag pole
            landmarkPaint.alpha = 140
            canvas.drawLine(sx, sy - 8f, sx, sy - 16f, landmarkPaint.apply { style = Paint.Style.STROKE; strokeWidth = 1.5f })
            landmarkPaint.style = Paint.Style.FILL
            landmarkPaint.alpha = 255
        }

        // Scattered props along travel corridors
        landmarkPaint.color = Color.parseColor(Palette.AIRWAY_MARK)
        canvas.drawRect(screenX(0.76f) - 9f, screenY(0.28f) - 4f, screenX(0.76f) + 9f, screenY(0.28f) + 4f, landmarkPaint)
        landmarkPaint.color = Color.parseColor(Palette.FOREST)
        canvas.drawCircle(screenX(0.54f), screenY(0.66f), 6f, landmarkPaint)
        landmarkPaint.color = Color.parseColor(Palette.HILLS)
        canvas.drawRect(screenX(0.24f) - 7f, screenY(0.24f) - 7f, screenX(0.24f) + 7f, screenY(0.24f) + 7f, landmarkPaint)
        landmarkPaint.color = Color.parseColor(Palette.SETTLEMENT_SOIL)
        canvas.drawRect(screenX(0.29f) - 10f, screenY(0.78f) - 3f, screenX(0.29f) + 10f, screenY(0.78f) + 3f, landmarkPaint)

        // Scattered stones along road corridor
        landmarkPaint.color = Color.parseColor("#58606A")
        landmarkPaint.alpha = 80
        repeat(8) { i ->
            val nx = 0.20f + i * 0.08f
            val ny = 0.52f + sin(i.toFloat() * 0.9f) * 0.03f
            canvas.drawCircle(screenX(nx), screenY(ny), 2.2f, landmarkPaint)
        }
        landmarkPaint.alpha = 255

        // Chokepoint labels at mid/close zoom
        if (zoom != ZoomState.FAR) {
            val chokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = if (zoom == ZoomState.CLOSE) 13f else 10f
                color = Color.parseColor("#B8C2D1")
                alpha = if (zoom == ZoomState.CLOSE) 200 else 140
                textAlign = Paint.Align.CENTER
                typeface = UiTheme.TYPEFACE_BODY
                letterSpacing = 0.06f
            }
            canvas.drawText("MOUNTAIN PASS", screenX(0.40f), screenY(0.24f), chokePaint)
            canvas.drawText("RIVER CROSSING", screenX(0.64f), screenY(0.37f), chokePaint)
            canvas.drawText("CANYON PATH", screenX(0.39f), screenY(0.57f), chokePaint)
            canvas.drawText("FOREST EDGE", screenX(0.47f), screenY(0.60f), chokePaint)
        }
    }

    private fun drawRegionLabels(canvas: Canvas) {
        val zoom = zoomState()
        if (zoom == ZoomState.CLOSE) return
        mapLabelPaint.alpha = if (zoom == ZoomState.FAR) 190 else 110
        canvas.drawText("SECTOR NOVEMBER", screenX(0.77f), screenY(0.16f), mapLabelPaint)
        canvas.drawText("SECTOR ALPHA", screenX(0.23f), screenY(0.10f), mapLabelPaint)
        canvas.drawText("SECTOR DELTA", screenX(0.58f), screenY(0.80f), mapLabelPaint)
        canvas.drawText("SECTOR GOLF", screenX(0.22f), screenY(0.86f), mapLabelPaint)
        mapLabelPaint.alpha = 255
    }

    private fun zoomState(): ZoomState = when {
        cameraZoom < 1.18f -> ZoomState.FAR
        cameraZoom < 1.95f -> ZoomState.MID
        else -> ZoomState.CLOSE
    }

    private data class TravelRoute(val points: List<Pair<Float, Float>>, val type: RouteType, val risk: RouteRisk)

    private fun buildTravelRoute(startX: Float, startY: Float, endX: Float, endY: Float): TravelRoute {
        val points = mutableListOf(Pair(startX, startY), Pair(endX, endY))
        val endpointRisk = routeRiskTo(endX, endY)
        val sampledRisk = routeRiskForPath(points)
        val finalRisk = if (sampledRisk.ordinal > endpointRisk.ordinal) sampledRisk else endpointRisk
        val routeType = if (finalRisk == RouteRisk.INTERCEPT) RouteType.THREAT_AVOIDANCE else RouteType.DIRECT
        return TravelRoute(points, routeType, finalRisk)
    }

    private fun routeNormLength(points: List<Pair<Float, Float>>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 0 until points.lastIndex) {
            total += distanceNorm(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second)
        }
        return total
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

        routePaint.strokeWidth = if (activeRouteType == RouteType.THREAT_AVOIDANCE) 3.6f else 3f
        routePaint.color = when (activeRouteType) {
            RouteType.DIRECT -> Color.parseColor("#A3AFBF")
            RouteType.THREAT_AVOIDANCE -> Color.parseColor("#C65A5A")
        }
        routePaint.alpha = if (routePreviewCommitted) 240 else 185
        routePaint.pathEffect = when (activeRouteType) {
            RouteType.DIRECT -> DashPathEffect(floatArrayOf(12f, 8f), pulseValue * 18f)
            RouteType.THREAT_AVOIDANCE -> DashPathEffect(floatArrayOf(7f, 6f), pulseValue * 16f)
        }
        routeUnderlayPaint.strokeWidth = routePaint.strokeWidth + 2.4f
        routePath.reset()
        points.forEachIndexed { index, pt ->
            val sx = screenX(pt.first)
            val sy = screenY(pt.second)
            if (index == 0) routePath.moveTo(sx, sy) else routePath.lineTo(sx, sy)
        }
        canvas.drawPath(routePath, routeUnderlayPaint)
        canvas.drawPath(routePath, routePaint)
        if (activeRouteRisk == RouteRisk.THREATENED || activeRouteRisk == RouteRisk.INTERCEPT) {
            hazardPaint.strokeWidth = if (activeRouteRisk == RouteRisk.INTERCEPT) 2.4f else 1.8f
            hazardPaint.color = if (activeRouteRisk == RouteRisk.INTERCEPT) Color.parseColor("#E48787") else Color.parseColor("#D29A7D")
            hazardPaint.alpha = if (activeRouteRisk == RouteRisk.INTERCEPT) 160 else 125
            hazardPaint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), pulseValue * 20f)
            canvas.drawPath(routePath, hazardPaint)
        }

        if (isMoving) {
            val progressPoint = pointOnRoute(points, travelProgress)
            routeProgressPath.reset()
            routeProgressPath.moveTo(screenX(points.first().first), screenY(points.first().second))
            val sampleCount = 14
            for (i in 1..sampleCount) {
                val t = (travelProgress * i / sampleCount.toFloat()).coerceIn(0f, travelProgress)
                val p = pointOnRoute(points, t)
                routeProgressPath.lineTo(screenX(p.first), screenY(p.second))
            }
            routeProgressPath.lineTo(screenX(progressPoint.first), screenY(progressPoint.second))
            canvas.drawPath(routeProgressPath, routeProgressPaint)
            val headingPoint = pointOnRoute(points, (travelProgress + 0.025f).coerceAtMost(1f))
            drawRouteHeadingChevron(canvas, progressPoint, headingPoint, activeRouteType)
        }

        val destination = points.last()
        val endX = screenX(destination.first)
        val endY = screenY(destination.second)
        routeDestinationPaint.color = when (activeRouteType) {
            RouteType.THREAT_AVOIDANCE -> Color.parseColor("#E48787")
            RouteType.DIRECT -> Color.parseColor("#D0D8E3")
        }
        val pulse = if (routePreviewCommitted) 1f + pulseValue * 1.8f else 1f + pulseValue * 1.1f
        val markerRadius = 8.5f + pulse * 2f
        canvas.drawCircle(endX, endY, markerRadius, routeDestinationPaint)
        canvas.drawLine(endX - 7f, endY, endX + 7f, endY, routeDestinationPaint)
        canvas.drawLine(endX, endY - 7f, endX, endY + 7f, routeDestinationPaint)
        if (activeRouteType == RouteType.THREAT_AVOIDANCE) {
            routeDestinationPaint.alpha = 120
            canvas.drawCircle(endX, endY, markerRadius + 8f + pulseValue * 4f, routeDestinationPaint)
            routeDestinationPaint.alpha = 210
        }

        val targetLabel = when {
            activeRouteRisk == RouteRisk.INTERCEPT -> "INTERCEPT RISK"
            activeRouteRisk == RouteRisk.THREATENED -> "THREAT RANGE"
            else -> if (activeRouteType == RouteType.THREAT_AVOIDANCE) "THREAT-AVOIDANCE LOCKED" else "DIRECT FLIGHT LOCKED"
        }
        drawLabel(canvas, targetLabel, endX, endY - 20f, LabelState.ROUTE_TARGET)
        drawThreatVectors(canvas, points)
    }

    private fun drawRouteHeadingChevron(
        canvas: Canvas,
        from: Pair<Float, Float>,
        to: Pair<Float, Float>,
        type: RouteType
    ) {
        val fx = screenX(from.first)
        val fy = screenY(from.second)
        val tx = screenX(to.first)
        val ty = screenY(to.second)
        val dx = tx - fx
        val dy = ty - fy
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        val nx = dx / len
        val ny = dy / len
        val size = 8f
        routeChevronPaint.color = when (type) {
            RouteType.DIRECT -> Color.parseColor("#D0D8E3")
            RouteType.THREAT_AVOIDANCE -> Color.parseColor("#F3B1B1")
        }
        routeChevronPath.reset()
        routeChevronPath.moveTo(fx + nx * size, fy + ny * size)
        routeChevronPath.lineTo(fx - nx * size * 0.8f - ny * size * 0.7f, fy - ny * size * 0.8f + nx * size * 0.7f)
        routeChevronPath.lineTo(fx - nx * size * 0.8f + ny * size * 0.7f, fy - ny * size * 0.8f - nx * size * 0.7f)
        routeChevronPath.close()
        canvas.drawPath(routeChevronPath, routeChevronPaint)
    }

    private fun drawThreatVectors(canvas: Canvas, routePoints: List<Pair<Float, Float>>) {
        if (routePoints.size < 2) return
        val hostileParties = enemyParties.filter { it.faction == PartyFaction.HOSTILE }
        if (hostileParties.isEmpty()) return
        routeThreatPaint.alpha = if (activeRouteRisk == RouteRisk.INTERCEPT) 170 else 145
        routeThreatPaint.pathEffect = DashPathEffect(floatArrayOf(7f, 7f), pulseValue * 15f)
        var vectorsDrawn = 0
        hostileParties.forEach { party ->
            val pos = enemyDisplayPositions[party.id] ?: (nodes.find { it.id == party.nodeId }?.let { it.mapX to it.mapY } ?: return@forEach)
            val nearest = nearestPointOnPolyline(pos.first, pos.second, routePoints) ?: return@forEach
            val maxThreatDistance = if (activeRouteRisk == RouteRisk.INTERCEPT) 0.16f else 0.13f
            if (nearest.distanceNorm > maxThreatDistance) return@forEach
            if (vectorsDrawn >= 4) return@forEach
            canvas.drawLine(screenX(pos.first), screenY(pos.second), screenX(nearest.x), screenY(nearest.y), routeThreatPaint)
            vectorsDrawn++
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
            nodePaint.color = nodeBaseColor(node)
            nodePaint.alpha = if (node.isCleared) 90 else 255
            nodeStrokePaint.color = lighten(nodeBaseColor(node), 1.4f)
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
                val labelState = when {
                    selectedNodeId == node.id -> LabelState.SELECTED
                    node.id == currentNodeId -> LabelState.NEARBY
                    zoom == ZoomState.FAR -> LabelState.MICRO
                    else -> LabelState.IDLE
                }
                val defaultY = cy + nodeRadius + 24f
                val bottomInset = height * 0.78f
                val labelY = if (defaultY > bottomInset) cy - nodeRadius - 14f else defaultY
                val labelText = if (labelState == LabelState.MICRO) node.name.take(3).uppercase() else label
                drawLabel(canvas, labelText, cx, labelY, labelState)
                placedLabels.add(cx to labelY)
            }
        }
    }

    private fun drawClearedTaskMarker(canvas: Canvas, cx: Float, cy: Float) {
        val markerRadius = 10f
        nodePaint.color = Color.parseColor("#58606A")
        nodePaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, markerRadius, nodePaint)
        nodeStrokePaint.color = Color.parseColor("#B8C2D1")
        nodeStrokePaint.alpha = 170
        canvas.drawCircle(cx, cy, markerRadius, nodeStrokePaint)

        textPaint.textSize = 14f
        textPaint.color = Color.parseColor("#F2F0EA")
        canvas.drawText("⚔", cx, cy + 5f, textPaint)
        textPaint.textSize = 26f
    }

    private fun isTemporaryNode(node: CampaignNode): Boolean = when (node.type) {
        NodeType.ENEMY_PATROL, NodeType.RESOURCE_CACHE, NodeType.ELITE_CHALLENGE, NodeType.RECOVERY_CAMP -> true
        else -> false
    }

    private fun drawNodeGlyph(canvas: Canvas, node: CampaignNode, cx: Float, cy: Float) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#F2F0EA")
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = if (node.isCleared) 140 else 240
        }
        when (node.type) {
            NodeType.TOWN -> {
                canvas.drawLine(cx - 10f, cy + 8f, cx + 10f, cy + 8f, iconPaint)
                canvas.drawLine(cx - 10f, cy + 8f, cx - 10f, cy - 6f, iconPaint)
                canvas.drawLine(cx + 10f, cy + 8f, cx + 10f, cy - 6f, iconPaint)
                canvas.drawLine(cx - 12f, cy - 6f, cx + 12f, cy - 6f, iconPaint)
            }
            NodeType.VILLAGE -> {
                val roof = Path().apply {
                    moveTo(cx - 10f, cy + 3f)
                    lineTo(cx, cy - 9f)
                    lineTo(cx + 10f, cy + 3f)
                }
                canvas.drawPath(roof, iconPaint)
                canvas.drawLine(cx - 8f, cy + 3f, cx - 8f, cy + 10f, iconPaint)
                canvas.drawLine(cx + 8f, cy + 3f, cx + 8f, cy + 10f, iconPaint)
            }
            NodeType.RECOVERY_CAMP -> {
                canvas.drawLine(cx - 10f, cy + 9f, cx, cy - 8f, iconPaint)
                canvas.drawLine(cx, cy - 8f, cx + 10f, cy + 9f, iconPaint)
                canvas.drawLine(cx - 10f, cy + 9f, cx + 10f, cy + 9f, iconPaint)
            }
            NodeType.FACTION_OUTPOST -> {
                canvas.drawLine(cx - 6f, cy + 10f, cx - 6f, cy - 10f, iconPaint)
                canvas.drawLine(cx - 6f, cy - 8f, cx + 9f, cy - 3f, iconPaint)
                canvas.drawLine(cx + 9f, cy - 3f, cx - 6f, cy + 1f, iconPaint)
            }
            NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE -> {
                canvas.drawLine(cx - 9f, cy - 9f, cx + 9f, cy + 9f, iconPaint)
                canvas.drawLine(cx - 9f, cy + 9f, cx + 9f, cy - 9f, iconPaint)
            }
            NodeType.BOSS -> {
                canvas.drawCircle(cx, cy, 9f, iconPaint)
                canvas.drawLine(cx - 6f, cy - 6f, cx + 6f, cy + 6f, iconPaint)
            }
            NodeType.RESOURCE_CACHE -> {
                canvas.drawRect(cx - 8f, cy - 6f, cx + 8f, cy + 8f, iconPaint)
                canvas.drawLine(cx - 8f, cy - 2f, cx + 8f, cy - 2f, iconPaint)
            }
            NodeType.START -> {
                canvas.drawCircle(cx, cy, 8f, iconPaint)
                canvas.drawLine(cx, cy - 12f, cx, cy + 12f, iconPaint)
            }
        }
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
                    moveTo(cx, cy - nodeRadius - 5f)
                    lineTo(cx + nodeRadius * 0.86f, cy - nodeRadius * 0.18f)
                    lineTo(cx + nodeRadius * 0.86f, cy + nodeRadius * 0.76f)
                    lineTo(cx - nodeRadius * 0.86f, cy + nodeRadius * 0.76f)
                    lineTo(cx - nodeRadius * 0.86f, cy - nodeRadius * 0.18f)
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
            NodeType.RESOURCE_CACHE -> {
                val r = nodeRadius + 3f
                val path = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.7f, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.7f, cy)
                    close()
                }
                canvas.drawPath(path, nodePaint)
                canvas.drawPath(path, nodeStrokePaint)
            }
            NodeType.START -> {
                val r = nodeRadius + 4f
                val path = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.35f, cy - r * 0.35f)
                    lineTo(cx + r, cy)
                    lineTo(cx + r * 0.35f, cy + r * 0.35f)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.35f, cy + r * 0.35f)
                    lineTo(cx - r, cy)
                    lineTo(cx - r * 0.35f, cy - r * 0.35f)
                    close()
                }
                canvas.drawPath(path, nodePaint)
                canvas.drawPath(path, nodeStrokePaint)
            }
        }
    }

    private fun nodeBaseColor(node: CampaignNode): Int = when (node.type) {
        NodeType.TOWN, NodeType.VILLAGE, NodeType.FACTION_OUTPOST -> Color.parseColor("#6C83C8")
        NodeType.RECOVERY_CAMP -> Color.parseColor("#5FAF7A")
        NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS -> when (biomeAt(node.mapX, node.mapY)) {
            BiomeType.DESERT -> Color.parseColor("#C56A5D")
            BiomeType.FOREST -> Color.parseColor("#C56A5D")
            BiomeType.HILLS -> Color.parseColor("#C56A5D")
            BiomeType.PLAINS -> Color.parseColor("#C56A5D")
        }
        NodeType.RESOURCE_CACHE -> Color.parseColor("#8FA0B4")
        NodeType.START -> Color.parseColor(Palette.GOLD)
    }

    private enum class LabelState { IDLE, NEARBY, SELECTED, ROUTE_TARGET, MICRO }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float, state: LabelState = LabelState.IDLE) {
        val fillColor = when (state) {
            LabelState.IDLE -> "#CC16263A"
            LabelState.NEARBY -> "#DD20334C"
            LabelState.SELECTED -> "#EE27415E"
            LabelState.ROUTE_TARGET -> "#DD4B2A2A"
            LabelState.MICRO -> "#B316263A"
        }
        val textColor = when (state) {
            LabelState.ROUTE_TARGET -> "#FFE4DF"
            else -> Palette.HUD_TEXT
        }
        labelPaint.color = Color.parseColor(textColor)
        labelPaint.textSize = when (state) {
            LabelState.MICRO -> 13f
            LabelState.SELECTED, LabelState.ROUTE_TARGET -> 18f
            else -> 16f
        }
        labelPaint.typeface = if (state == LabelState.SELECTED || state == LabelState.ROUTE_TARGET) {
            UiTheme.TYPEFACE_LABEL
        } else {
            UiTheme.TYPEFACE_BODY
        }
        labelBgPaint.color = Color.parseColor(fillColor)
        val fm = labelPaint.fontMetrics
        val tw = labelPaint.measureText(text).coerceAtMost(width * 0.54f)
        val safeTop = height * 0.16f
        val safeBottom = height * 0.80f
        val clampedY = cy.coerceIn(safeTop, safeBottom)
        val rect = RectF(cx - tw / 2 - 10, clampedY + fm.ascent - 6, cx + tw / 2 + 10, clampedY + fm.descent + 6)
        canvas.drawRoundRect(rect, 10f, 10f, labelBgPaint)
        if (state == LabelState.SELECTED || state == LabelState.ROUTE_TARGET) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.6f
                color = Color.parseColor(if (state == LabelState.ROUTE_TARGET) "#E48787" else "#B89C6B")
            }
            canvas.drawRoundRect(rect, 10f, 10f, borderPaint)
        }
        canvas.drawText(text, cx, clampedY, labelPaint)
        labelPaint.color = Color.parseColor(Palette.HUD_TEXT)
        labelPaint.textSize = 17f
    }

    private fun drawPlayerMarker(canvas: Canvas) {
        val px = screenX(playerNormX)
        val py = screenY(playerNormY)

        if (previewRoutePoints.size > 1 && !isMoving) {
            val origin = previewRoutePoints.first()
            canvas.drawText(currentPreviewRouteTypeLabel(), screenX(origin.first), screenY(origin.second) - 16f, playerRouteLabelPaint)
        }

        val heading = atan2(playerLookDirY, playerLookDirX)
        val size = 18f

        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(Math.toDegrees(heading.toDouble()).toFloat() + 90f)

        // Arrow body (dark maroon outline)
        val arrowBody = Path().apply {
            moveTo(0f, -size * 1.2f)           // tip
            lineTo(size * 0.85f, size * 0.8f)   // right wing
            lineTo(0f, size * 0.3f)              // notch center
            lineTo(-size * 0.85f, size * 0.8f)  // left wing
            close()
        }
        playerRingPaint.style = Paint.Style.FILL_AND_STROKE
        playerRingPaint.strokeWidth = 3f
        playerPaint.color = Color.parseColor("#5A1018")
        canvas.drawPath(arrowBody, playerPaint)
        playerRingPaint.color = Color.parseColor("#7A1A24")
        playerRingPaint.style = Paint.Style.STROKE
        playerRingPaint.strokeWidth = 3.5f
        canvas.drawPath(arrowBody, playerRingPaint)
        // Reset ring paint
        playerRingPaint.color = Color.parseColor(Palette.GOLD)
        playerRingPaint.strokeWidth = 4f

        // Center line (red accent)
        val linePaintLocal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E03030")
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, -size * 1.0f, 0f, size * 0.2f, linePaintLocal)

        canvas.restore()
    }

    private fun drawEnemyParties(canvas: Canvas) {
        val zoom = zoomState()
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#F2F0EA")
            typeface = UiTheme.TYPEFACE_LABEL
        }
        val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#B8C2D1")
            typeface = UiTheme.TYPEFACE_BODY
        }
        val threatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#C65A5A")
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
                markerPaint.color = Color.parseColor(UiTheme.ALLY)
                pulseRingPaint.color = Color.parseColor(UiTheme.ALLY)
                symbolPaint.color = Color.parseColor("#F2F0EA")
                labelPaint.color = Color.parseColor("#B8C2D1")
            } else {
                markerPaint.color = Color.parseColor(UiTheme.HOSTILE)
                pulseRingPaint.color = Color.parseColor(UiTheme.HOSTILE)
                symbolPaint.color = Color.WHITE
                labelPaint.color = Color.parseColor("#B8C2D1")
            }

            if (selectedNodeId == node.id && zoom != ZoomState.FAR) {
                pulseRingPaint.strokeWidth = 2f
                pulseRingPaint.alpha = 110
                pulseRingPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 8f), pulseValue * 16f)
                canvas.drawCircle(x, y, rangeRadius, pulseRingPaint)
                pulseRingPaint.pathEffect = null
            }

            val partyHeading = enemyHeadings[party.id] ?: (-Math.PI / 2).toFloat()
            pulseRingPaint.strokeWidth = 1.6f
            pulseRingPaint.alpha = 180
            drawPartyMarker(canvas, party.faction, x, y, iconRadius, markerPaint, pulseRingPaint, partyHeading)
            if (zoom != ZoomState.FAR) {
                canvas.drawText(
                    if (party.faction == PartyFaction.FRIENDLY) "ALLY L${profile.level}" else "${enemyFamilyForBiome(biomeAt(node.mapX, node.mapY))} L${profile.level}",
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

    private fun drawPartyMarker(
        canvas: Canvas,
        faction: PartyFaction,
        x: Float,
        y: Float,
        radius: Float,
        fillPaint: Paint,
        outlinePaint: Paint,
        heading: Float = (-Math.PI / 2).toFloat()
    ) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(Math.toDegrees(heading.toDouble()).toFloat() + 90f)

        val r = radius
        val path = Path().apply {
            moveTo(0f, -r * 1.2f)           // tip
            lineTo(r * 0.85f, r * 0.8f)     // right wing
            lineTo(0f, r * 0.3f)             // notch center
            lineTo(-r * 0.85f, r * 0.8f)    // left wing
            close()
        }
        canvas.drawPath(path, fillPaint)
        val prevStyle = outlinePaint.style
        outlinePaint.style = Paint.Style.STROKE
        canvas.drawPath(path, outlinePaint)
        outlinePaint.style = prevStyle

        // Center line accent
        val accentColor = if (faction == PartyFaction.FRIENDLY) "#40C8E8" else "#E05050"
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(accentColor)
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, -r * 0.9f, 0f, r * 0.15f, accent)

        canvas.restore()
    }

    private fun partyMovementState(party: EnemyParty): String {
        val isMovingParty = party.travelFromNodeId != null && party.travelToNodeId != null && party.travelProgress in 0.01f..0.99f
        if (party.faction == PartyFaction.FRIENDLY) {
            return if (isMovingParty) "TRANSIT" else "IDLE"
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
        BiomeType.DESERT -> "PATROL UNIT"
        BiomeType.PLAINS -> "PATROL WING"
        BiomeType.FOREST -> "INTERCEPT UNIT"
        BiomeType.HILLS -> "DEFENCE UNIT"
    }

    private fun drawPartyGlyph(
        canvas: Canvas,
        faction: PartyFaction,
        biome: BiomeType,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        if (faction == PartyFaction.FRIENDLY) {
            // Shield + banner
            canvas.drawLine(x, y - 8f, x, y + 8f, paint)
            canvas.drawLine(x - 5f, y - 3f, x + 5f, y - 3f, paint)
            canvas.drawLine(x - 5f, y + 3f, x + 5f, y + 3f, paint)
            canvas.drawLine(x - 5f, y - 3f, x - 5f, y + 3f, paint)
            canvas.drawLine(x + 5f, y - 3f, x + 5f, y + 3f, paint)
            return
        }
        when (biome) {
            BiomeType.DESERT -> {
                // Curved scimitar
                canvas.drawArc(RectF(x - 8f, y - 6f, x + 4f, y + 6f), -30f, 200f, false, paint)
                canvas.drawLine(x + 2f, y - 4f, x + 6f, y - 7f, paint)
            }
            BiomeType.PLAINS -> {
                // Crossed spears
                canvas.drawLine(x - 6f, y - 6f, x + 6f, y + 6f, paint)
                canvas.drawLine(x + 6f, y - 6f, x - 6f, y + 6f, paint)
                canvas.drawCircle(x, y, 2.5f, paint)
            }
            BiomeType.FOREST -> {
                // Wolf fang / claw marks
                canvas.drawLine(x - 5f, y - 6f, x - 2f, y + 6f, paint)
                canvas.drawLine(x, y - 6f, x, y + 6f, paint)
                canvas.drawLine(x + 5f, y - 6f, x + 2f, y + 6f, paint)
            }
            BiomeType.HILLS -> {
                // Axe head
                canvas.drawLine(x, y - 8f, x, y + 6f, paint)
                val axePath = Path().apply {
                    moveTo(x, y - 5f)
                    lineTo(x + 7f, y - 1f)
                    lineTo(x, y + 3f)
                }
                canvas.drawPath(axePath, paint)
            }
        }
    }

    private data class PartyVisualProfile(
        val level: Int,
        val iconRadiusPx: Float,
        val rangeRadiusNorm: Float,
        val offsetNormX: Float,
        val offsetNormY: Float
    )

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

    private fun routeRiskForPath(points: List<Pair<Float, Float>>): RouteRisk {
        var highest = RouteRisk.SAFE
        val samples = maxOf(6, points.size * 4)
        for (i in 0..samples) {
            val point = pointOnRoute(points, i / samples.toFloat())
            val risk = routeRiskTo(point.first, point.second)
            if (risk.ordinal > highest.ordinal) {
                highest = risk
                if (highest == RouteRisk.INTERCEPT) return highest
            }
        }
        return highest
    }

    private data class RouteNearestPoint(val x: Float, val y: Float, val distanceNorm: Float)

    private fun nearestPointOnPolyline(x: Float, y: Float, points: List<Pair<Float, Float>>): RouteNearestPoint? {
        if (points.size < 2) return null
        var best: RouteNearestPoint? = null
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            val ax = a.first
            val ay = a.second
            val bx = b.first
            val by = b.second
            val abx = bx - ax
            val aby = by - ay
            val abLen2 = (abx * abx + aby * aby).coerceAtLeast(0.000001f)
            val t = (((x - ax) * abx + (y - ay) * aby) / abLen2).coerceIn(0f, 1f)
            val px = ax + abx * t
            val py = ay + aby * t
            val dist = hypot(x - px, y - py)
            if (best == null || dist < (best?.distanceNorm ?: Float.MAX_VALUE)) {
                best = RouteNearestPoint(px, py, dist)
            }
        }
        return best
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

    /**
     * Returns the hostile [EnemyParty] whose screen icon was tapped, or null.
     * Uses a generous hit radius (nodeRadius + 14dp) for easy finger targeting.
     */
    private fun findTappedEnemyParty(screenTapX: Float, screenTapY: Float): EnemyParty? {
        val hitRadius = nodeRadius + 14f
        val hitRadiusSq = hitRadius * hitRadius
        for (party in enemyParties) {
            if (party.faction != PartyFaction.HOSTILE) continue
            val node = nodes.find { it.id == party.nodeId } ?: continue
            if (!node.isRevealed) continue
            val pos = enemyDisplayPositions[party.id] ?: Pair(node.mapX, node.mapY)
            val sx = screenX(pos.first)
            val sy = screenY(pos.second)
            val dx = screenTapX - sx
            val dy = screenTapY - sy
            if (dx * dx + dy * dy <= hitRadiusSq) return party
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled) return false
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
                    // Check if a hostile party was tapped before routing to generic map tap
                    val tappedParty = findTappedEnemyParty(event.x, event.y)
                    if (tappedParty != null) {
                        onEnemyPartyTapped?.invoke(tappedParty)
                    } else {
                        onMapTapped?.invoke(normXFromScreen(event.x), normYFromScreen(event.y))
                    }
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
