package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.warpath.engine.CampaignManager
import com.warpath.model.AirPlayerState
import com.warpath.model.CampaignNode
import com.warpath.model.EnemyParty
import com.warpath.model.NodeType
import com.warpath.model.PartyFaction
import java.util.ArrayDeque
import kotlin.random.Random
import kotlin.math.hypot

class CampaignActivity : AppCompatActivity() {

    private enum class PanelType { SETTLEMENT, ROAMING, EVENT, RESULT }

    companion object {
        var campaignManager: CampaignManager = CampaignManager()
    }

    private lateinit var mapView: CampaignMapView
    private lateinit var infoPanel: LinearLayout
    private lateinit var suppliesText: TextView
    private lateinit var renownText: TextView
    private lateinit var warbandText: TextView
    private lateinit var nodeNameText: TextView
    private lateinit var nodeTypeChip: TextView
    private lateinit var nodeDescText: TextView
    private lateinit var nodeStatsText: TextView
    private lateinit var threatRowText: TextView
    private lateinit var strengthRowText: TextView
    private lateinit var suppliesRowText: TextView
    private lateinit var reputationRowText: TextView
    private lateinit var nodeRangeTagText: TextView
    private lateinit var distanceText: TextView
    private lateinit var actionSecondaryText: TextView
    private lateinit var actionButton: Button
    private lateinit var panelAccentBar: View
    private lateinit var statusText: TextView
    private lateinit var travelHintText: TextView
    private lateinit var recenterButton: Button
    private lateinit var stopMovementButton: Button
    private lateinit var movementPanel: LinearLayout
    private lateinit var movementPanelInfoText: TextView
    private lateinit var pauseButton: Button
    private lateinit var movementStopButton: Button
    private lateinit var mapStateText: TextView
    private lateinit var modeStripText: TextView
    private lateinit var controlCluster: LinearLayout
    private lateinit var alertBanner: LinearLayout
    private lateinit var alertAccent: View
    private lateinit var alertIconText: TextView
    private lateinit var alertMessageText: TextView
    private lateinit var alertEdgeTint: View
    private var controlClusterTopMarginPx: Int = 0
    private var hasShownScoutingHint: Boolean = false
    private var hintHideRunnable: Runnable? = null

    private val phaseOnePocMode = false
    private val poiInteractionDistance = 0.07f
    private val playerMetersPerMoveAction = 50f
    private var selectedNode: CampaignNode? = null
    private var suppressAutoPoiSelection = false
    private var pendingRedirectTarget: Pair<Float, Float>? = null
    private var activeEnemySnapshot: com.warpath.engine.CampaignManager.EnemyMovementSnapshot? = null
    private var activeTravelDistanceNorm: Float = 0f
    private var activeTravelStartX: Float = 0f
    private var activeTravelStartY: Float = 0f
    private var detailsExpanded: Boolean = false
    private var activeWorldAlert: WorldAlert? = null
    private var isAlertShowing: Boolean = false
    private var alertHideRunnable: Runnable? = null
    private val alertQueue = ArrayDeque<WorldAlert>()
    private val recentlyShownAlerts = mutableMapOf<String, Long>()
    private val uiHandler = Handler(Looper.getMainLooper())

    private val density by lazy { resources.displayMetrics.density }
    private val screenWidthDp by lazy { resources.displayMetrics.widthPixels / density }
    private val compactUi by lazy { screenWidthDp < 420f }
    private val duplicateAlertWindowMs = 2500L
    private val recentAlertRetentionMs = 12000L
    private val maxQueuedAlerts = 6

    private object Palette {
        val BASE_BG = UiTheme.BASE_BG
        val HUD_SURFACE = "#EE${UiTheme.SURFACE.removePrefix("#")}"
        val HUD_SURFACE_ALT = "#DD${UiTheme.SURFACE_ALT.removePrefix("#")}"
        val HUD_BORDER = UiTheme.BORDER
        val HUD_TEXT = UiTheme.TEXT_PRIMARY
        val HUD_TEXT_MUTED = UiTheme.TEXT_MUTED
        val GOLD = UiTheme.WARNING
        val PRIMARY = UiTheme.PRIMARY
        val SUCCESS = UiTheme.POSITIVE
        val DANGER = UiTheme.HOSTILE
    }

    private enum class AlertPriority(val holdMs: Long) {
        MINOR(1400L),
        STANDARD(1900L),
        HIGH(2500L)
    }

    private enum class AlertCategory(val accentHex: String, val icon: String) {
        DANGER("#C65A5A", "⚠"),
        DISCOVERY("#B89C6B", "◈"),
        TRAVEL("#A86973", "➤"),
        GAIN("#5FAF7A", "✦"),
        EVENT("#7B1E2B", "✧")
    }

    private data class WorldAlert(
        val message: String,
        val category: AlertCategory,
        val priority: AlertPriority,
        val key: String = message
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        if (intent.getBooleanExtra("new_game", false)) {
            campaignManager.startNewCampaign()
        }

        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor(Palette.BASE_BG)) }

        mapView = CampaignMapView(this).apply {
            nodes = campaignManager.campaignMap
            currentNodeId = campaignManager.gameState.currentNodeId
            enemyParties = campaignManager.gameState.enemyParties
            enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
                party.id to campaignManager.getEnemyPartyPosition(party)
            }
            showPaths = false
            setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
            onMapTapped = { normX, normY ->
                handleMapTap(normX, normY)
            }
            onEnemyPartyTapped = { party ->
                handleEnemyPartyTap(party)
            }
            onFocusChanged = { focused ->
                updateControlClusterVisibility()
                if (!focused) {
                    enqueueWorldAlert("Recon View", AlertCategory.TRAVEL, AlertPriority.MINOR, "map_unfocused")
                }
                updateMapStateText()
            }
        }
        root.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            buildTopHud(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
        )
        root.addView(
            buildModeStrip(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = dp(92) }
        )

        root.addView(
            buildAlertBanner(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            ).apply {
                topMargin = dp(96)
                marginStart = dp(UiTheme.SPACE_4)
                marginEnd = dp(UiTheme.SPACE_4)
            }
        )

        alertEdgeTint = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 0f
            visibility = View.GONE
        }
        root.addView(
            alertEdgeTint,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        statusText = TextView(this).apply {
            textSize = if (compactUi) 12f else 13f
            setTextColor(Color.parseColor(Palette.GOLD))
            typeface = UiTheme.TYPEFACE_LABEL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(Color.parseColor(Palette.HUD_SURFACE))
            visibility = View.GONE
        }
        root.addView(
            statusText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            )
        )

        recenterButton = Button(this).apply {
            text = "⌖"
            contentDescription = "Recenter map on flight"
            textSize = if (compactUi) 9f else 10f
            typeface = UiTheme.TYPEFACE_HEADING
            isAllCaps = false
            minWidth = dp(32)
            minimumWidth = dp(32)
            minHeight = dp(32)
            minimumHeight = dp(32)
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            setPadding(0, 0, 0, 0)
            stateListAnimator = null
            visibility = View.GONE
            applyHudButtonStyle(cornerRadius = 16f)
            setOnClickListener {
                mapView.recenterOnPlayer()
                enqueueWorldAlert("Squadron Radar Restored", AlertCategory.TRAVEL, AlertPriority.MINOR, "camera_recentered")
            }
        }

        mapStateText = TextView(this).apply {
            textSize = if (compactUi) 9f else 10f
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            letterSpacing = 0.08f
        }
        styleChip(mapStateText, Palette.HUD_SURFACE_ALT, 10f)

        controlCluster = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        controlCluster.addView(recenterButton)
        controlCluster.addView(
            mapStateText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        )
        controlClusterTopMarginPx = dp(142)
        root.addView(
            controlCluster,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = controlClusterTopMarginPx
                rightMargin = dp(UiTheme.SPACE_4)
            }
        )

        root.addView(
            buildInfoPanel(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        stopMovementButton = Button(this).apply { visibility = View.GONE }

        movementPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(dpF(16f), dpF(16f), dpF(16f), dpF(16f), 0f, 0f, 0f, 0f)
                setColor(Color.parseColor("#F116263A"))
                setStroke(1, Color.parseColor(Palette.HUD_BORDER))
            }
            elevation = UiTheme.HUD_ELEVATION
            visibility = View.GONE
        }

        movementPanelInfoText = TextView(this).apply {
            textSize = if (compactUi) 10f else 12f
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(Palette.GOLD))
            text = "➤ ROUTE · 0m · 0%"
            setPadding(0, 0, 0, dp(10))
        }
        movementPanel.addView(movementPanelInfoText)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        pauseButton = Button(this).apply {
            text = "⏸ Pause"
            contentDescription = "Pause movement"
            textSize = if (compactUi) 10f else 11f
            typeface = UiTheme.TYPEFACE_HEADING
            isAllCaps = false
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            minHeight = dp(38)
            minimumHeight = dp(38)
            applyHudButtonStyle(cornerRadius = 10f)
            setOnClickListener {
                if (mapView.isPaused) {
                    val redirect = pendingRedirectTarget
                    if (redirect != null) {
                        pendingRedirectTarget = null
                        redirectMovement(redirect.first, redirect.second)
                    } else {
                        mapView.resumeMovement()
                    }
                    text = "⏸ Pause"
                    enqueueWorldAlert("Sortie Resumed", AlertCategory.TRAVEL, AlertPriority.MINOR, "movement_resumed")
                } else {
                    mapView.pauseMovement()
                    text = "▶ Resume"
                    enqueueWorldAlert("Sortie Paused", AlertCategory.TRAVEL, AlertPriority.MINOR, "movement_paused")
                }
                updateMapStateText()
            }
        }
        buttonRow.addView(pauseButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })

        movementStopButton = Button(this).apply {
            text = "■ Stop"
            contentDescription = "Stop and cancel movement"
            textSize = if (compactUi) 10f else 11f
            typeface = UiTheme.TYPEFACE_HEADING
            isAllCaps = false
            setTextColor(Color.parseColor(Palette.DANGER))
            minHeight = dp(38)
            minimumHeight = dp(38)
            applyHudButtonStyle(cornerRadius = 10f)
            setOnClickListener {
                mapView.stopMovement()
            }
        }
        buttonRow.addView(movementStopButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        movementPanel.addView(buttonRow)

        root.addView(
            movementPanel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        travelHintText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_LABEL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            text = "◉ RECON MODE  ·  Drag to survey. Tap airspace to set flight path."
            background = GradientDrawable().apply {
                cornerRadius = dpF(16f)
                setColor(Color.parseColor(Palette.HUD_SURFACE))
                setStroke(dp(1), Color.parseColor(Palette.HUD_BORDER))
            }
            visibility = View.GONE
            alpha = 0f
        }
        root.addView(
            travelHintText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                marginStart = dp(UiTheme.SPACE_5)
                marginEnd = dp(UiTheme.SPACE_5)
                bottomMargin = dp(if (compactUi) 180 else 192)
            }
        )

        setContentView(root)
        updateHud()
        updateMapStateText()
        checkFogDiscovery(showToast = false)
        showNearbyPoiIfAny()
    }

    private fun buildTopHud(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                Color.parseColor(Palette.HUD_SURFACE),
                Color.parseColor("#DD0E1726")
            )).apply { cornerRadius = 0f }
            setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(UiTheme.TOP_BAR_HEIGHT + 36)
        }

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleCol.addView(TextView(this).apply {
            text = "AIR OPS"
            textSize = if (compactUi) 14f else 16f
            setTextColor(Color.parseColor(Palette.GOLD))
            typeface = UiTheme.TYPEFACE_TITLE
            letterSpacing = 0.12f
        })
        bar.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.85f))

        suppliesText = hudStatChip()
        renownText = hudStatChip()
        warbandText = hudStatChip()

        val resourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        resourceRow.addView(suppliesText)
        resourceRow.addView(hudSpacer())
        resourceRow.addView(renownText)
        resourceRow.addView(hudSpacer())
        resourceRow.addView(warbandText)
        bar.addView(resourceRow, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val warbandBtn = Button(this).apply {
            text = "⚑"
            textSize = if (compactUi) 10f else 11f
            minWidth = dp(UiTheme.ICON_BUTTON_SIZE)
            minimumWidth = dp(UiTheme.ICON_BUTTON_SIZE)
            minHeight = dp(UiTheme.ICON_BUTTON_SIZE)
            minimumHeight = dp(UiTheme.ICON_BUTTON_SIZE)
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            isAllCaps = false
            typeface = UiTheme.TYPEFACE_HEADING
            setPadding(0, 0, 0, 0)
            stateListAnimator = null
            applyHudButtonStyle(cornerRadius = UiTheme.RADIUS_SM)
            setOnClickListener { startActivity(Intent(this@CampaignActivity, WarbandActivity::class.java)) }
        }
        bar.addView(warbandBtn)

        val divider = View(this).apply { setBackgroundColor(Color.parseColor("#324660")) }
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        shell.addView(bar)
        shell.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
        return shell
    }

    private fun hudStatChip() = TextView(this).apply {
        textSize = if (compactUi) UiTheme.TEXT_MICRO + 2f else UiTheme.TEXT_SECONDARY
        setTextColor(Color.parseColor(Palette.HUD_TEXT))
        typeface = UiTheme.TYPEFACE_LABEL
        background = UiTheme.roundedRect(Palette.HUD_SURFACE_ALT, null, UiTheme.RADIUS_XS)
        setPadding(dp(UiTheme.SPACE_2), dp(2), dp(UiTheme.SPACE_2), dp(2))
    }

    private fun hudSpacer(): View = View(this).also {
        it.layoutParams = LinearLayout.LayoutParams(dp(16), 1)
    }

    private fun buildAlertBanner(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        alertBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(16), dp(8))
            alpha = 0f
            translationY = -dp(12).toFloat()
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpF(UiTheme.RADIUS_MD)
                setColor(Color.parseColor(Palette.HUD_SURFACE))
                setStroke(dp(1), Color.parseColor(Palette.HUD_BORDER))
            }
            elevation = dpF(3f)
        }

        alertAccent = View(this).apply { setBackgroundColor(Color.parseColor(AlertCategory.TRAVEL.accentHex)) }
        alertBanner.addView(
            alertAccent,
            LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginEnd = dp(8) }
        )

        alertIconText = TextView(this).apply {
            text = AlertCategory.EVENT.icon
            textSize = if (compactUi) 11f else 12f
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            setPadding(0, 0, dp(8), 0)
        }
        alertBanner.addView(alertIconText)

        alertMessageText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            letterSpacing = 0.02f
        }
        alertBanner.addView(alertMessageText)
        row.addView(alertBanner, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Space(this), LinearLayout.LayoutParams(dp(8), 1))
        return row
    }

    private fun buildModeStrip(): View {
        modeStripText = TextView(this).apply {
            textSize = if (compactUi) 9f else 10f
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            setPadding(dp(12), dp(5), dp(12), dp(5))
            letterSpacing = 0.08f
            alpha = 0.95f
        }
        styleChip(modeStripText, Palette.HUD_SURFACE_ALT, 10f)
        return modeStripText
    }

    private fun updateControlClusterVisibility() {
        if (!::controlCluster.isInitialized) return
        recenterButton.visibility = if (mapView.isCenteredOnPlayer()) View.GONE else View.VISIBLE
    }

    private fun compactRowText() = TextView(this).apply {
        textSize = if (compactUi) 10f else 11f
        setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
        setPadding(0, dp(1), 0, dp(1))
        typeface = UiTheme.TYPEFACE_LABEL
    }

    private fun styledStat(label: String, value: String): CharSequence {
        val combined = "$label  $value"
        return SpannableString(combined).apply {
            setSpan(ForegroundColorSpan(Color.parseColor(Palette.HUD_TEXT_MUTED)), 0, label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(Color.parseColor(Palette.HUD_TEXT)), label.length + 2, combined.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun enqueueWorldAlert(message: String, category: AlertCategory, priority: AlertPriority, key: String = message) {
        val now = System.currentTimeMillis()
        pruneRecentAlerts(now)
        if ((recentlyShownAlerts[key] ?: 0L) + duplicateAlertWindowMs > now) return
        if (activeWorldAlert?.key == key) return
        if (alertQueue.any { it.key == key }) return
        val incomingAlert = WorldAlert(message = message, category = category, priority = priority, key = key)
        if (priority == AlertPriority.HIGH && isAlertShowing && activeWorldAlert?.priority != AlertPriority.HIGH) {
            alertQueue.addFirst(incomingAlert)
            forceDismissCurrentAlert()
            return
        }
        if (alertQueue.size >= maxQueuedAlerts) {
            alertQueue.removeFirst()
        }
        when (priority) {
            AlertPriority.HIGH -> alertQueue.addFirst(incomingAlert)
            AlertPriority.STANDARD, AlertPriority.MINOR -> alertQueue.addLast(incomingAlert)
        }
        showNextWorldAlertIfIdle()
    }

    private fun pruneRecentAlerts(now: Long = System.currentTimeMillis()) {
        recentlyShownAlerts.entries.removeAll { (_, shownAt) -> now - shownAt > recentAlertRetentionMs }
    }

    private fun showNextWorldAlertIfIdle() {
        if (isAlertShowing || alertQueue.isEmpty()) return
        val alert = alertQueue.removeFirst()
        activeWorldAlert = alert
        isAlertShowing = true
        recentlyShownAlerts[alert.key] = System.currentTimeMillis()

        val accentColor = Color.parseColor(alert.category.accentHex)
        alertAccent.setBackgroundColor(accentColor)
        alertIconText.text = alert.category.icon
        alertIconText.setTextColor(accentColor)
        alertMessageText.text = alert.message
        val borderColor = when (alert.priority) {
            AlertPriority.MINOR -> Palette.HUD_BORDER
            AlertPriority.STANDARD -> alert.category.accentHex
            AlertPriority.HIGH -> Palette.GOLD
        }
        alertBanner.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpF(UiTheme.RADIUS_MD)
            setColor(Color.parseColor(Palette.HUD_SURFACE))
            setStroke(dp(1), Color.parseColor(borderColor))
        }

        alertBanner.visibility = View.VISIBLE
        alertBanner.alpha = 0f
        alertBanner.translationY = -dp(10).toFloat()
        alertBanner.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(190L)
            .withEndAction {
                if (alert.priority == AlertPriority.HIGH) {
                    pulseEdgeTint(alert.category)
                }
                alertHideRunnable?.let { uiHandler.removeCallbacks(it) }
                alertHideRunnable = Runnable { hideCurrentWorldAlert() }
                uiHandler.postDelayed(alertHideRunnable!!, alert.priority.holdMs)
            }
            .start()
    }

    private fun forceDismissCurrentAlert() {
        alertHideRunnable?.let { uiHandler.removeCallbacks(it) }
        alertHideRunnable = null
        hintHideRunnable?.let { uiHandler.removeCallbacks(it) }
        hintHideRunnable = null
        hideCurrentWorldAlert(durationMs = 90L)
    }

    private fun hideCurrentWorldAlert(durationMs: Long = 170L) {
        alertHideRunnable?.let { uiHandler.removeCallbacks(it) }
        alertHideRunnable = null
        hintHideRunnable?.let { uiHandler.removeCallbacks(it) }
        hintHideRunnable = null
        alertBanner.animate()
            .alpha(0f)
            .translationY(-dp(8).toFloat())
            .setDuration(durationMs)
            .withEndAction {
                alertBanner.visibility = View.GONE
                isAlertShowing = false
                activeWorldAlert = null
                showNextWorldAlertIfIdle()
            }
            .start()
    }

    private fun pulseEdgeTint(category: AlertCategory) {
        val tint = when (category) {
            AlertCategory.DANGER -> "#30C65A5A"
            AlertCategory.DISCOVERY -> "#30B89C6B"
            AlertCategory.TRAVEL -> "#287B1E2B"
            AlertCategory.GAIN -> "#205FAF7A"
            AlertCategory.EVENT -> "#2527415E"
        }
        alertEdgeTint.setBackgroundColor(Color.parseColor(tint))
        alertEdgeTint.visibility = View.VISIBLE
        alertEdgeTint.alpha = 0f
        alertEdgeTint.animate().alpha(1f).setDuration(90L).withEndAction {
            alertEdgeTint.animate().alpha(0f).setDuration(220L).withEndAction {
                alertEdgeTint.visibility = View.GONE
            }.start()
        }.start()
    }


    private fun buildInfoPanel(): View {
        val container = FrameLayout(this)

        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpF(UiTheme.RADIUS_LG)
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                colors = intArrayOf(Color.parseColor("#F116263A"), Color.parseColor("#EF111C2D"))
                setStroke(dp(1), Color.parseColor(Palette.HUD_BORDER))
            }
            visibility = View.GONE
            elevation = dpF(UiTheme.HUD_ELEVATION)
        }

        panelAccentBar = View(this).apply { visibility = View.GONE }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nodeTypeChip = TextView(this).apply {
            textSize = if (compactUi) 9f else 10f
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            typeface = UiTheme.TYPEFACE_LABEL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            letterSpacing = 0.08f
        }
        headerRow.addView(
            nodeTypeChip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
        )

        nodeNameText = TextView(this).apply {
            textSize = if (compactUi) 18f else 20f
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            typeface = UiTheme.TYPEFACE_LABEL
            letterSpacing = 0.02f
        }
        headerRow.addView(nodeNameText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val dismissX = TextView(this).apply {
            text = "✕"
            textSize = if (compactUi) 13f else 14f
            setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dpF(10f)
                setColor(Color.parseColor(Palette.HUD_SURFACE_ALT))
            }
            setOnClickListener {
                infoPanel.visibility = View.GONE
                selectedNode = null
                mapView.selectedNodeId = null
                updateMapStateText()
                if (mapView.isMovementActive() || mapView.isPaused) {
                    showMovementPanel()
                }
            }
        }
        headerRow.addView(dismissX)

        content.addView(headerRow)

        nodeRangeTagText = TextView(this).apply {
            textSize = if (compactUi) 9f else 10f
            typeface = UiTheme.TYPEFACE_LABEL
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            letterSpacing = 0.06f
            visibility = View.GONE
        }
        val statusChipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statusChipRow.addView(nodeRangeTagText)

        distanceText = TextView(this).apply {
            textSize = if (compactUi) 10f else 11f
            typeface = UiTheme.TYPEFACE_BODY
            setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
            setPadding(dp(8), dp(8), dp(8), dp(0))
        }
        statusChipRow.addView(distanceText)
        content.addView(statusChipRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })

        nodeDescText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            setLineSpacing(3f, 1f)
            maxLines = 2
            setOnClickListener {
                detailsExpanded = !detailsExpanded
                maxLines = if (detailsExpanded) 6 else 2
            }
        }
        content.addView(
            nodeDescText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16); bottomMargin = dp(8) }
        )

        val statsGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpF(16f)
                setColor(Color.parseColor(Palette.HUD_SURFACE_ALT))
                setStroke(dp(1), Color.parseColor(Palette.HUD_BORDER))
            }
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        threatRowText = compactRowText()
        strengthRowText = compactRowText()
        suppliesRowText = compactRowText()
        reputationRowText = compactRowText()
        val statsRowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statsRowOne.addView(threatRowText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statsRowOne.addView(strengthRowText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val statsRowTwo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statsRowTwo.addView(suppliesRowText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statsRowTwo.addView(reputationRowText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statsGrid.addView(statsRowOne)
        statsGrid.addView(statsRowTwo, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        content.addView(statsGrid)

        nodeStatsText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            background = GradientDrawable().apply {
                cornerRadius = dpF(10f)
                setColor(Color.parseColor(Palette.HUD_SURFACE_ALT))
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        content.addView(
            nodeStatsText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(16) }
        )

        actionButton = Button(this).apply {
            textSize = if (compactUi) 13f else 14f
            setTextColor(Color.parseColor(Palette.HUD_TEXT))
            isAllCaps = false
            typeface = UiTheme.TYPEFACE_LABEL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            stateListAnimator = null
            applyPrimaryButtonStyle()
        }
        content.addView(
            actionButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        actionSecondaryText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor(Palette.HUD_TEXT_MUTED))
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(0))
            visibility = View.GONE
        }
        content.addView(actionSecondaryText)

        infoPanel.addView(content)
        container.addView(
            infoPanel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(16)
                marginEnd = dp(16)
                bottomMargin = dp(16)
            }
        )

        return container
    }

    private fun Button.applyRoundedStyle(
        backgroundColor: String,
        borderColor: String = Palette.HUD_BORDER,
        topEdgeColor: String = backgroundColor,
        cornerRadius: Float = 16f
    ) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(Color.parseColor(topEdgeColor), Color.parseColor(backgroundColor))
            setStroke(dp(1), Color.parseColor(borderColor))
            this.cornerRadius = cornerRadius
        }
    }

    private fun showMovementPanel() {
        if (!::movementPanel.isInitialized) return
        infoPanel.visibility = View.GONE
        pauseButton.text = "⏸ Pause"
        updateMovementPanel()
        movementPanel.visibility = View.VISIBLE
        movementPanel.alpha = 0f
        movementPanel.translationY = dpF(60f)
        movementPanel.animate().alpha(1f).translationY(0f).setDuration(220L).start()
        statusText.visibility = View.GONE
    }

    private fun hideMovementPanel() {
        if (!::movementPanel.isInitialized) return
        movementPanel.animate().alpha(0f).translationY(dpF(60f)).setDuration(180L)
            .withEndAction { movementPanel.visibility = View.GONE }
            .start()
        statusText.visibility = View.GONE
    }

    private fun updateMovementPanel() {
        if (!::movementPanelInfoText.isInitialized) return
        val routeLabel = mapView.currentPreviewRouteTypeLabel()
        val progress = (mapView.currentTravelProgress() * 100f).toInt()
        val distMeters = (mapView.currentRouteLength() * 1000f).toInt()
        movementPanelInfoText.text = "➤ $routeLabel · ${distMeters}m · $progress%"
    }

    private fun Button.applyHudButtonStyle(cornerRadius: Float = 11f) {
        applyRoundedStyle(
            backgroundColor = Palette.HUD_SURFACE_ALT,
            borderColor = Palette.HUD_BORDER,
            topEdgeColor = Palette.HUD_SURFACE_ALT,
            cornerRadius = cornerRadius
        )
    }

    private fun Button.applyPrimaryButtonStyle() {
        applyRoundedStyle(
            backgroundColor = Palette.PRIMARY,
            borderColor = Palette.PRIMARY,
            topEdgeColor = Palette.PRIMARY,
            cornerRadius = 16f
        )
    }

    private fun styleChip(chip: TextView, fillHex: String, radiusDp: Float = 10f) {
        chip.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpF(radiusDp)
            setColor(Color.parseColor(fillHex))
        }
    }

    private fun formatDistanceToNode(node: CampaignNode): String {
        val dx = campaignManager.gameState.playerMapX - node.mapX
        val dy = campaignManager.gameState.playerMapY - node.mapY
        val meters = hypot(dx, dy) * 1000f
        return "${meters.toInt()}m"
    }

    private fun updateMapStateText() {
        if (!::mapStateText.isInitialized) return
        val moving = mapView.isMovementActive()
        val paused = mapView.isPaused
        val hasTarget = selectedNode != null
        val centered = mapView.isCenteredOnPlayer()
        val airState = when {
            moving && paused -> AirPlayerState.HOLDING
            moving -> AirPlayerState.TRANSIT
            hasTarget -> AirPlayerState.ROUTE_PLANNING
            !centered -> AirPlayerState.RECON
            else -> AirPlayerState.IDLE
        }
        val (label, color) = when (airState) {
            AirPlayerState.HOLDING -> "HOLDING" to Palette.GOLD
            AirPlayerState.TRANSIT -> "TRANSIT" to Palette.SUCCESS
            AirPlayerState.ROUTE_PLANNING -> "ROUTE PLANNING" to Palette.PRIMARY
            AirPlayerState.RECON -> "RECON" to Palette.HUD_SURFACE_ALT
            AirPlayerState.IDLE -> "IDLE" to Palette.HUD_SURFACE_ALT
            AirPlayerState.INTERCEPT -> "INTERCEPT" to Palette.HUD_SURFACE_ALT
            AirPlayerState.STRIKE -> "STRIKE" to Palette.HUD_SURFACE_ALT
            AirPlayerState.RTB -> "RTB" to Palette.HUD_SURFACE_ALT
            AirPlayerState.DAMAGED_EMERGENCY -> "DAMAGED / EMERGENCY" to Palette.DANGER
        }
        mapStateText.text = label
        styleChip(mapStateText, color, 10f)
        if (::modeStripText.isInitialized) {
            modeStripText.text = "MODE · $label"
            styleChip(modeStripText, color, 10f)
        }
        maybeShowScoutingHint(centered = centered, moving = moving)
    }


    private fun maybeShowScoutingHint(centered: Boolean, moving: Boolean) {
        if (!::travelHintText.isInitialized) return
        if (centered || moving) {
            hasShownScoutingHint = false
            hideTravelHint()
            return
        }
        if (hasShownScoutingHint) return
        hasShownScoutingHint = true
        hintHideRunnable?.let { uiHandler.removeCallbacks(it) }
        travelHintText.visibility = View.VISIBLE
        travelHintText.alpha = 0f
        travelHintText.animate().alpha(1f).setDuration(140L).start()
        hintHideRunnable = Runnable { hideTravelHint() }
        uiHandler.postDelayed(hintHideRunnable!!, 3200L)
    }

    private fun hideTravelHint() {
        if (!::travelHintText.isInitialized || travelHintText.visibility != View.VISIBLE) return
        travelHintText.animate().alpha(0f).setDuration(120L).withEndAction {
            travelHintText.visibility = View.GONE
        }.start()
    }

    private fun dp(value: Int): Int = (value * density).toInt()
    private fun dpF(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    override fun onResume() {
        super.onResume()
        mapView.nodes = campaignManager.campaignMap
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.enemyParties = campaignManager.gameState.enemyParties
        mapView.enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
            party.id to campaignManager.getEnemyPartyPosition(party)
        }
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        mapView.inputEnabled = true
        mapView.showPaths = false
        mapView.invalidate()
        recenterButton.visibility = if (mapView.isCenteredOnPlayer()) View.GONE else View.VISIBLE
        updateMapStateText()
        updateHud()
        checkFogDiscovery(showToast = false)
        showNearbyPoiIfAny()
        if (isAlertShowing && activeWorldAlert != null && alertHideRunnable == null) {
            alertHideRunnable = Runnable { hideCurrentWorldAlert() }
            uiHandler.postDelayed(alertHideRunnable!!, 1400L)
        }

        if (campaignManager.isRunOver()) {
            showRunOverDialog()
        }
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        alertHideRunnable = null
        hintHideRunnable = null
        alertQueue.clear()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        alertHideRunnable?.let { uiHandler.removeCallbacks(it) }
        alertHideRunnable = null
        hintHideRunnable?.let { uiHandler.removeCallbacks(it) }
        hintHideRunnable = null
    }

    private fun updateHud() {
        val gs = campaignManager.gameState
        suppliesText.text = "FUEL  ${gs.supplies}"
        renownText.text = "RDNS  ${gs.renown}"
        val livingTroops = gs.warband.sumOf { it.count.coerceAtLeast(0) }
        warbandText.text = "SQDN  $livingTroops"
    }

    private fun handleMapTap(normX: Float, normY: Float) {
        val tappedNode = findNodeNear(normX, normY)
        if (tappedNode != null) {
            onNodeSelected(tappedNode)
            return
        }
        selectedNode = null
        mapView.selectedNodeId = null
        infoPanel.visibility = View.GONE
        if ((mapView.isMovementActive() || mapView.isPaused) && ::movementPanel.isInitialized && movementPanel.visibility != View.VISIBLE) {
            showMovementPanel()
        }
        if (mapView.isMovementActive() && !mapView.isPaused) {
            redirectMovement(normX, normY)
        } else if (mapView.isPaused) {
            pendingRedirectTarget = Pair(normX, normY)
            mapView.previewRouteTo(normX, normY, committed = true)
            updateMovementPanel()
            enqueueWorldAlert("Route Updated", AlertCategory.TRAVEL, AlertPriority.MINOR, "route_redirect_paused")
            updateMapStateText()
        } else {
            mapView.previewRouteTo(normX, normY, committed = true)
            moveWarbandTo(normX, normY)
        }
    }

    private fun findNodeNear(normX: Float, normY: Float): CampaignNode? {
        return campaignManager.campaignMap
            .filter { it.isRevealed }
            .minByOrNull { node ->
                val dx = node.mapX - normX
                val dy = node.mapY - normY
                dx * dx + dy * dy
            }
            ?.takeIf {
                val dx = it.mapX - normX
                val dy = it.mapY - normY
                dx * dx + dy * dy <= 0.0028f
            }
    }

    private fun onNodeSelected(node: CampaignNode) {
        selectedNode = node
        detailsExpanded = false
        mapView.selectedNodeId = node.id
        if (::movementPanel.isInitialized) movementPanel.visibility = View.GONE
        infoPanel.visibility = View.VISIBLE
        updateMapStateText()

        panelAccentBar.setBackgroundColor(Color.parseColor(Palette.PRIMARY))

        nodeTypeChip.text = node.type.displayName.uppercase()
        styleChip(nodeTypeChip, Palette.HUD_SURFACE_ALT, 10f)
        nodeNameText.text = node.name
        nodeNameText.setTextColor(Color.parseColor(Palette.HUD_TEXT))

        val isAccessible = node.isRevealed
        val isCurrent = node.id == campaignManager.gameState.currentNodeId
        val isNearby = isNodeNearby(node)
        distanceText.text = "Range: ${formatDistanceToNode(node)}"
        actionSecondaryText.visibility = View.GONE
        applyPanelType(if (node.type == NodeType.TOWN || node.type == NodeType.VILLAGE || node.type == NodeType.FACTION_OUTPOST) PanelType.SETTLEMENT else PanelType.ROAMING)
        bindCompactRows(node)

        updateRangeTag(node, isNearby)

        actionButton.alpha = 1f
        actionButton.isEnabled = true

        if (phaseOnePocMode) {
            showPocNodeState(node)
            return
        }

        if (node.isCleared) {
            nodeDescText.text = node.description
            nodeStatsText.text = "✓ Objective cleared"
            nodeStatsText.setTextColor(Color.parseColor("#5FAF7A"))
            actionButton.visibility = View.GONE

        } else if (isCurrent || isAccessible) {
            nodeStatsText.setTextColor(Color.parseColor(Palette.GOLD))
            when (node.type) {
                NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS -> {
                    val enemyCount = node.enemySquads.sumOf { it.count }
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Hostiles: $enemyCount  |  FUEL +${node.suppliesReward}  RDNS +${node.renownReward}"
                    actionButton.text = if (node.type == NodeType.BOSS) "✈ Strike the Target!" else "✈ Engage!"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { engageBattle(node) } }
                }

                NodeType.RECOVERY_CAMP -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 20 fuel  |  Repair 40% hull"
                    actionButton.text = "⚙ Refuel & Repair"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtCamp(node) } }
                }

                NodeType.RESOURCE_CACHE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Reward: FUEL +${node.suppliesReward}  RDNS +${node.renownReward}"
                    actionButton.text = "⛽ Collect Fuel"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { collectResources(node) } }
                }

                NodeType.FACTION_OUTPOST -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Scramble new flights and resupply"
                    actionButton.text = "✈ Land at FOB"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { visitOutpost(node) } }
                }

                NodeType.TOWN -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 35 fuel  |  Full repair + scramble support"
                    actionButton.text = "⚙ Dock at Airbase"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, true) } }
                }

                NodeType.VILLAGE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 15 fuel  |  Repair 50%"
                    actionButton.text = "⚙ Land at FOB"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, false) } }
                }

                NodeType.START -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Your staging airbase"
                    actionButton.visibility = View.GONE
                }
            }
        } else {
            nodeDescText.text = node.description
            nodeStatsText.text = "Out of range — move closer to interact."
            nodeStatsText.setTextColor(Color.parseColor(Palette.GOLD))
            actionButton.text = "➤ Transit To"
            actionButton.visibility = View.VISIBLE
            actionButton.applyPrimaryButtonStyle()
            actionButton.setOnClickListener {
                infoPanel.visibility = View.GONE
                selectedNode = null
                mapView.selectedNodeId = null
                if (mapView.isMovementActive()) {
                    redirectMovement(node.mapX, node.mapY)
                } else {
                    mapView.previewRouteTo(node.mapX, node.mapY, committed = true)
                    moveWarbandTo(node.mapX, node.mapY)
                }
            }
        }
    }

    private fun showPocNodeState(node: CampaignNode) {
        val isAccessible = node.isRevealed
        val isNearby = isNodeNearby(node)
        nodeDescText.text = node.description
        nodeStatsText.setTextColor(Color.parseColor("#B89C6B"))

        if (!isAccessible) {
            nodeStatsText.text = "⚠ Not discovered yet"
            actionButton.visibility = View.GONE
            return
        }

        if (isNearby) {
            nodeStatsText.text = "Within interaction range."
            actionButton.text = "Open Actions"
            actionButton.visibility = View.VISIBLE
            actionButton.applyPrimaryButtonStyle()
            actionSecondaryText.text = "More details"
            actionSecondaryText.visibility = View.VISIBLE
            actionButton.setOnClickListener {
                suppressAutoPoiSelection = true
                openPoiContextMenu(node)
                suppressAutoPoiSelection = false
                showNearbyPoiIfAny()
            }
            return
        }

        nodeStatsText.text = "Out of range. Move to this location."
        actionButton.text = "Transit"
        actionButton.visibility = View.VISIBLE
        actionButton.applyPrimaryButtonStyle()
        actionButton.setOnClickListener { moveWarbandTo(node.mapX, node.mapY) }
    }

    private fun updateRangeTag(node: CampaignNode, isNearby: Boolean) {
        val (label, bg) = when {
            node.type == NodeType.ENEMY_PATROL || node.type == NodeType.ELITE_CHALLENGE || node.type == NodeType.BOSS ->
                if (isNearby) "HOSTILE · NEARBY" to Palette.DANGER else "HOSTILE" to Palette.DANGER
            node.type == NodeType.TOWN || node.type == NodeType.VILLAGE -> if (isNearby) "ALLY · NEARBY" to Palette.PRIMARY else "ALLY" to Palette.PRIMARY
            node.type == NodeType.RESOURCE_CACHE -> if (isNearby) "NEARBY" to Palette.HUD_BORDER else "NEARBY" to Palette.HUD_BORDER
            else -> if (isNearby) "NEUTRAL · NEARBY" to Palette.HUD_BORDER else "NEUTRAL" to Palette.HUD_BORDER
        }
        nodeRangeTagText.text = label
        nodeRangeTagText.visibility = View.VISIBLE
        styleChip(nodeRangeTagText, bg, 10f)
    }

    private fun bindCompactRows(node: CampaignNode) {
        val enemyStrength = node.enemySquads.sumOf { it.count }
        val threatText = when (node.type) {
            NodeType.BOSS -> "High"
            NodeType.ELITE_CHALLENGE, NodeType.ENEMY_PATROL -> "Medium"
            else -> "Low"
        }
        val supplies = node.suppliesReward.coerceAtLeast(0)
        val reputation = node.renownReward.coerceAtLeast(0)
        threatRowText.text = styledStat("THREAT", threatText)
        strengthRowText.text = styledStat("STRENGTH", enemyStrength.coerceAtLeast(0).toString())
        suppliesRowText.text = styledStat("FUEL", if (supplies == 0) "—" else supplies.toString())
        reputationRowText.text = styledStat("RDNS", if (reputation == 0) "—" else "+$reputation")
    }

    private fun applyPanelType(type: PanelType) {
        val accent = when (type) {
            PanelType.SETTLEMENT -> "#5E8FD6"
            PanelType.ROAMING -> "#27415E"
            PanelType.EVENT -> "#B89C6B"
            PanelType.RESULT -> "#5FAF7A"
        }
        panelAccentBar.setBackgroundColor(Color.parseColor(accent))
        infoPanel.alpha = 0f
        infoPanel.translationY = dp(12).toFloat()
        infoPanel.animate().alpha(1f).translationY(0f).setDuration(
            when (type) {
                PanelType.SETTLEMENT -> 240L
                PanelType.ROAMING -> 150L
                PanelType.EVENT -> 210L
                PanelType.RESULT -> 170L
            }
        ).start()
    }

    private fun openPoiContextMenu(node: CampaignNode) {
        val options = when (node.type) {
            NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS ->
                arrayOf("Attack", "Ambush", "Bribe", "Observe")

            NodeType.TOWN, NodeType.VILLAGE ->
                arrayOf("Recruit", "Heal", "Trade Fuel", "Rest", "Gather Intel")

            NodeType.FACTION_OUTPOST ->
                arrayOf("Recruit", "Trade Fuel", "Take Contract")

            NodeType.RESOURCE_CACHE ->
                arrayOf("Investigate", "Spend Fuel", "Ignore")

            NodeType.RECOVERY_CAMP ->
                arrayOf("Heal", "Rest", "Ignore")

            NodeType.START ->
                arrayOf("Reorganize Squadron", "Scout Routes")
        }

        showThemedSheet(node.name, "Choose an action", options) { label ->
            handlePoiAction(node, label)
        }
    }

    private fun handlePoiAction(node: CampaignNode, action: String) {
        when {
            action == "Attack" || action == "Ambush" -> {
                scoutFromNode(node, revealHideoutIntel = node.type == NodeType.ELITE_CHALLENGE)
                showOutcomeSheet(
                    title = "Victory at ${node.name}",
                    summary = "Your squadron broke the enemy formation.",
                    details = listOf(
                        "+${node.suppliesReward.coerceAtLeast(4)} fuel secured",
                        "RDNS +${node.renownReward.coerceAtLeast(1)}",
                        "Nearby airspace pressure reduced"
                    )
                )
            }

            action == "Flee" -> {
                handleRunAttempt(node)
            }

            action.startsWith("Bribe") -> {
                handleBribe(node)
            }

            action == "Ignore" || action == "Observe" || action == "Gather Intel" -> {
                enqueueWorldAlert("Vector Maintained", AlertCategory.TRAVEL, AlertPriority.MINOR, "avoid_trouble")
            }

            action == "Trade Fuel" -> {
                campaignManager.gameState.supplies += if (node.type == NodeType.TOWN || node.type == NodeType.VILLAGE) 20 else 14
                scoutFromNode(node)
                updateHud()
                enqueueWorldAlert("Fuel Acquired", AlertCategory.GAIN, AlertPriority.MINOR, "supplies_stocked")
            }

            action == "Spend Fuel" -> {
                if (campaignManager.gameState.supplies < 8) {
                    enqueueWorldAlert("Need 8 Fuel", AlertCategory.EVENT, AlertPriority.MINOR, "need_8_supplies")
                    return
                }
                campaignManager.gameState.supplies -= 8
                campaignManager.gameState.renown += 3
                scoutFromNode(node)
                updateHud()
                enqueueWorldAlert("RDNS Earned", AlertCategory.GAIN, AlertPriority.STANDARD, "expedition_renown")
            }

            action == "Recruit" || action == "Reorganize Squadron" || action == "Take Contract" -> {
                scoutFromNode(node)
                startActivity(Intent(this, WarbandActivity::class.java).apply {
                    putExtra("can_recruit", true)
                })
            }

            action == "Rest" || action == "Heal" -> {
                campaignManager.gameState.healWarband(if (node.type == NodeType.TOWN) 1.0f else 0.55f)
                scoutFromNode(node)
                enqueueWorldAlert("Squadron Repaired", AlertCategory.GAIN, AlertPriority.STANDARD, "warband_recovered")
            }

            action == "Investigate" || action == "Scout Routes" -> {
                scoutFromNode(node)
            }
        }
    }

    private fun handleRunAttempt(node: CampaignNode) {
        val roll = Random.nextFloat()
        when {
            roll < 0.55f -> {
                showOutcomeSheet(
                    title = "Narrow Escape",
                    summary = "Your recon found a gap and the squadron disengaged.",
                    details = listOf("No losses", "Enemy contact broken")
                )
                return
            }
            roll < 0.85f -> {
                val loss = randomSupplyLoss(6, 15)
                campaignManager.gameState.supplies = (campaignManager.gameState.supplies - loss).coerceAtLeast(0)
                updateHud()
                showOutcomeSheet(
                    title = "Retreat Under Pressure",
                    summary = "You broke contact but lost some fuel in the retreat.",
                    details = listOf("-$loss fuel", "Enemy remains active")
                )
            }
            else -> {
                showOutcomeSheet(
                    title = "Escape Failed",
                    summary = "The enemy cut you off and forced a close skirmish.",
                    details = listOf("Forced engagement at ${node.name}")
                )
                scoutFromNode(node)
                return
            }
        }
    }

    private fun handleBribe(node: CampaignNode) {
        val cost = getBribeCost(node)
        val gs = campaignManager.gameState
        if (gs.supplies < cost) {
            enqueueWorldAlert("Need $cost Fuel", AlertCategory.EVENT, AlertPriority.MINOR, "bribe_need_$cost")
            return
        }
        gs.supplies -= cost
        gs.renown = (gs.renown - 2).coerceAtLeast(0)
        updateHud()
        showOutcomeSheet(
            title = "Negotiated Passage",
            summary = "The opposing party accepted payment and stood down.",
            details = listOf("-$cost fuel", "RDNS -2")
        )
    }

    private fun getBribeCost(node: CampaignNode): Int {
        return when (node.type) {
            NodeType.BOSS -> 40
            NodeType.ELITE_CHALLENGE -> 26
            else -> 16
        }
    }

    private fun randomSupplyLoss(min: Int, max: Int): Int {
        if (max <= min) return min
        return min + Random.nextInt(max - min + 1)
    }

    private fun showOutcomeSheet(title: String, summary: String, details: List<String>) {
        val isDefeatTone = title.contains("Failed", ignoreCase = true) || title.contains("Retreat", ignoreCase = true)
        val isDiscoveryTone = title.contains("Intel", ignoreCase = true) || title.contains("Discovered", ignoreCase = true)
        val accentHex = when {
            isDefeatTone -> Palette.DANGER
            isDiscoveryTone -> Palette.GOLD
            else -> Palette.SUCCESS
        }
        val header = when {
            isDefeatTone -> "RESOLUTION"
            isDiscoveryTone -> "DISCOVERY"
            else -> "VICTORY"
        }

        showThemedOverlay { dismiss ->
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = UiTheme.gradientSurface(
                    topHex = UiTheme.SURFACE_ELEVATED,
                    bottomHex = UiTheme.SURFACE,
                    borderHex = UiTheme.BORDER,
                    radius = UiTheme.RADIUS_LG
                )
                setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5))
                elevation = dpF(UiTheme.SHEET_ELEVATION)

                // Accent bar
                addView(View(this@CampaignActivity).apply {
                    setBackgroundColor(Color.parseColor(accentHex))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply {
                    bottomMargin = dp(UiTheme.SPACE_4)
                })

                addView(TextView(this@CampaignActivity).apply {
                    text = header
                    textSize = UiTheme.TEXT_CHIP
                    letterSpacing = 0.12f
                    typeface = UiTheme.TYPEFACE_LABEL
                    setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
                })
                addView(TextView(this@CampaignActivity).apply {
                    text = title
                    textSize = UiTheme.TEXT_CARD_TITLE
                    typeface = UiTheme.TYPEFACE_HEADING
                    setTextColor(Color.parseColor(Palette.HUD_TEXT))
                    setPadding(0, dp(UiTheme.SPACE_1), 0, dp(UiTheme.SPACE_1))
                })
                addView(TextView(this@CampaignActivity).apply {
                    text = summary
                    textSize = UiTheme.TEXT_SECONDARY
                    typeface = UiTheme.TYPEFACE_BODY
                    setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
                    setPadding(0, 0, 0, dp(UiTheme.SPACE_3))
                })

                // Detail rows
                val detailBlock = LinearLayout(this@CampaignActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = UiTheme.roundedRect(Palette.HUD_SURFACE_ALT, Palette.HUD_BORDER, UiTheme.RADIUS_SM)
                    setPadding(dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_2))
                }
                details.forEach { line ->
                    detailBlock.addView(TextView(this@CampaignActivity).apply {
                        text = line
                        textSize = UiTheme.TEXT_SECONDARY
                        typeface = UiTheme.TYPEFACE_BODY
                        setTextColor(Color.parseColor(Palette.HUD_TEXT))
                        setPadding(0, dp(UiTheme.SPACE_1), 0, dp(UiTheme.SPACE_1))
                    })
                }
                addView(detailBlock, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ))

                addView(Button(this@CampaignActivity).apply {
                    text = "Continue"
                    applyPrimaryButtonStyle()
                    textSize = UiTheme.TEXT_BUTTON
                    minHeight = dp(UiTheme.BUTTON_HEIGHT)
                    minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
                    setOnClickListener { dismiss() }
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(UiTheme.SPACE_4) })
            }
        }
    }

    /** Full-screen themed overlay with a centered panel. Returns dismiss callback. */
    private fun showThemedOverlay(buildContent: (dismiss: () -> Unit) -> View) {
        val rootFrame = window.decorView.findViewById<FrameLayout>(android.R.id.content)
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC0A0F1A"))
            isClickable = true
            isFocusable = true
        }
        val dismiss = {
            overlay.animate().alpha(0f).setDuration(150L).withEndAction {
                rootFrame.removeView(overlay)
            }.start()
        }
        val panel = buildContent(dismiss)
        val panelLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            marginStart = dp(UiTheme.SPACE_5)
            marginEnd = dp(UiTheme.SPACE_5)
        }
        overlay.addView(panel, panelLp)
        overlay.alpha = 0f
        rootFrame.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        overlay.animate().alpha(1f).setDuration(180L).start()
    }

    /** Themed bottom-anchored action sheet (replaces BottomSheetDialog). */
    private fun showThemedSheet(
        title: String,
        subtitle: String,
        options: Array<String>,
        onSelected: (String) -> Unit
    ) {
        val rootFrame = window.decorView.findViewById<FrameLayout>(android.R.id.content)
        val scrim = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#AA0A0F1A"))
            isClickable = true
        }
        val dismiss = {
            scrim.animate().alpha(0f).setDuration(140L).withEndAction {
                rootFrame.removeView(scrim)
            }.start()
        }
        scrim.setOnClickListener { dismiss() }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_6))
            background = UiTheme.gradientSurface(
                topHex = UiTheme.SURFACE_ELEVATED,
                bottomHex = UiTheme.SURFACE,
                borderHex = UiTheme.BORDER,
                radius = UiTheme.RADIUS_XL
            ).apply {
                cornerRadii = floatArrayOf(
                    dpF(UiTheme.RADIUS_XL), dpF(UiTheme.RADIUS_XL),
                    dpF(UiTheme.RADIUS_XL), dpF(UiTheme.RADIUS_XL),
                    0f, 0f, 0f, 0f
                )
            }
            elevation = dpF(UiTheme.SHEET_ELEVATION)
            isClickable = true
        }

        // Handle bar
        val handle = View(this).apply {
            background = UiTheme.roundedRect(UiTheme.BORDER_LIGHT, null, 3f)
        }
        container.addView(handle, LinearLayout.LayoutParams(dp(UiTheme.SHEET_HANDLE_WIDTH), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(UiTheme.SPACE_4)
        })

        container.addView(TextView(this).apply {
            text = "$title · Actions"
            textSize = UiTheme.TEXT_CARD_TITLE
            typeface = UiTheme.TYPEFACE_TITLE
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
        })
        container.addView(TextView(this).apply {
            text = subtitle
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
            setPadding(0, dp(UiTheme.SPACE_1), 0, dp(UiTheme.SPACE_3))
        })

        options.forEach { label ->
            val row = TextView(this).apply {
                text = label
                textSize = UiTheme.TEXT_BODY_SM
                typeface = UiTheme.TYPEFACE_BODY
                setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
                setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
                background = UiTheme.rippleDrawable(UiTheme.SURFACE_ALT, UiTheme.BORDER, UiTheme.RADIUS_SM)
                setOnClickListener {
                    dismiss()
                    onSelected(label)
                }
            }
            container.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(UiTheme.SPACE_2) })
        }

        scrim.addView(container, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ))
        scrim.alpha = 0f
        rootFrame.addView(scrim, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        scrim.animate().alpha(1f).setDuration(180L).start()
        container.translationY = dp(60).toFloat()
        container.animate().translationY(0f).setDuration(220L).start()
    }

    private fun moveWarbandTo(normX: Float, normY: Float) {
        pendingRedirectTarget = null
        infoPanel.visibility = View.GONE
        val startX = campaignManager.gameState.playerMapX
        val startY = campaignManager.gameState.playerMapY
        val totalDistanceNorm = hypot(normX - startX, normY - startY)
        if (totalDistanceNorm < 0.00002f) return
        val enemySnapshot = campaignManager.createEnemyMovementSnapshot()
        activeEnemySnapshot = enemySnapshot
        activeTravelDistanceNorm = totalDistanceNorm
        activeTravelStartX = startX
        activeTravelStartY = startY
        val routeLabel = mapView.currentPreviewRouteTypeLabel()
        when {
            routeLabel.contains("INTERCEPT") -> enqueueWorldAlert("INTERCEPT RISK ACTIVE", AlertCategory.DANGER, AlertPriority.HIGH, "route_risky")
            routeLabel.contains("THREATENED") -> enqueueWorldAlert("THREAT ENVELOPE AHEAD", AlertCategory.DANGER, AlertPriority.STANDARD, "route_threatened")
            else -> enqueueWorldAlert("FLIGHT PATH CLEAR", AlertCategory.TRAVEL, AlertPriority.MINOR, "route_clear")
        }
        showMovementPanel()
        updateMapStateText()
        mapView.animatePlayerTo(
            normX,
            normY,
            onProgress = { t, x, y ->
                val progressNorm = totalDistanceNorm * t
                mapView.enemyDisplayPositions = campaignManager.enemyPreviewPositions(
                    snapshot = enemySnapshot,
                    playerMovedNorm = progressNorm,
                    totalPlayerTravelNorm = totalDistanceNorm,
                    playerMetersPerAction = playerMetersPerMoveAction
                )
                updateMovementPanel()
            }
        ) { cancelled ->
            handleMovementComplete(enemySnapshot, totalDistanceNorm, cancelled)
        }
    }

    private fun redirectMovement(normX: Float, normY: Float) {
        val currentSnapshot = activeEnemySnapshot
        val currentTravelDist = activeTravelDistanceNorm
        if (currentSnapshot != null && currentTravelDist > 0f) {
            val partialProgress = mapView.currentTravelProgress()
            val movedNorm = currentTravelDist * partialProgress
            campaignManager.setPlayerPosition(mapView.currentPlayerNormX, mapView.currentPlayerNormY)
            val playerHit = campaignManager.applyReactiveEnemyMovement(
                snapshot = currentSnapshot,
                playerMovedNorm = movedNorm,
                totalPlayerTravelNorm = currentTravelDist,
                playerMetersPerAction = playerMetersPerMoveAction
            )
            mapView.enemyParties = campaignManager.gameState.enemyParties
            mapView.enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
                party.id to campaignManager.getEnemyPartyPosition(party)
            }
            if (playerHit) {
                mapView.stopMovement()
                handleMovementComplete(currentSnapshot, currentTravelDist, cancelled = true)
                forceEnemyEngagement()
                return
            }
        } else {
            campaignManager.setPlayerPosition(mapView.currentPlayerNormX, mapView.currentPlayerNormY)
        }

        val startX = mapView.currentPlayerNormX
        val startY = mapView.currentPlayerNormY
        val totalDistanceNorm = hypot(normX - startX, normY - startY)
        if (totalDistanceNorm < 0.00002f) return
        val enemySnapshot = campaignManager.createEnemyMovementSnapshot()
        activeEnemySnapshot = enemySnapshot
        activeTravelDistanceNorm = totalDistanceNorm
        activeTravelStartX = startX
        activeTravelStartY = startY

        mapView.redirectMovement(
            normX,
            normY,
            onProgress = { t, x, y ->
                val progressNorm = totalDistanceNorm * t
                mapView.enemyDisplayPositions = campaignManager.enemyPreviewPositions(
                    snapshot = enemySnapshot,
                    playerMovedNorm = progressNorm,
                    totalPlayerTravelNorm = totalDistanceNorm,
                    playerMetersPerAction = playerMetersPerMoveAction
                )
                updateMovementPanel()
            }
        ) { cancelled ->
            handleMovementComplete(enemySnapshot, totalDistanceNorm, cancelled)
        }

        enqueueWorldAlert("Route Changed", AlertCategory.TRAVEL, AlertPriority.STANDARD, "route_redirect")
        updateMovementPanel()
        updateMapStateText()
    }

    private fun handleMovementComplete(
        enemySnapshot: com.warpath.engine.CampaignManager.EnemyMovementSnapshot,
        totalDistanceNorm: Float,
        cancelled: Boolean
    ) {
        // travelProgress is reset to 0 in finishMove() before onComplete fires, so we
        // compute the actual moved distance from the start position we recorded.
        val movedNorm = if (!cancelled) {
            totalDistanceNorm
        } else {
            hypot(
                mapView.currentPlayerNormX - activeTravelStartX,
                mapView.currentPlayerNormY - activeTravelStartY
            ).coerceIn(0f, totalDistanceNorm)
        }
        campaignManager.setPlayerPosition(mapView.currentPlayerNormX, mapView.currentPlayerNormY)
        val playerHit = campaignManager.applyReactiveEnemyMovement(
            snapshot = enemySnapshot,
            playerMovedNorm = movedNorm,
            totalPlayerTravelNorm = totalDistanceNorm,
            playerMetersPerAction = playerMetersPerMoveAction
        )
        mapView.enemyParties = campaignManager.gameState.enemyParties
        mapView.enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
            party.id to campaignManager.getEnemyPartyPosition(party)
        }
        checkFogDiscovery()
        activeEnemySnapshot = null
        activeTravelDistanceNorm = 0f
        pendingRedirectTarget = null
        hideMovementPanel()
        updateMapStateText()
        if (cancelled) {
            enqueueWorldAlert("Movement Halted", AlertCategory.TRAVEL, AlertPriority.MINOR, "movement_abort")
        }
        if (playerHit) {
            forceEnemyEngagement()
            return
        }
        showNearbyPoiIfAny()
    }

    private fun scoutFromNode(node: CampaignNode, revealHideoutIntel: Boolean = false) {
        campaignManager.moveToNode(node.id)
        node.isCleared = true
        if (revealHideoutIntel) {
            campaignManager.revealNode("boss_1")
            enqueueWorldAlert("Hideout Intel Acquired", AlertCategory.DISCOVERY, AlertPriority.HIGH, "hideout_intel")
        }
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(
            campaignManager.gameState.playerMapX,
            campaignManager.gameState.playerMapY
        )
        mapView.recenterOnPlayer()
        mapView.invalidate()
        updateMapStateText()
        updateHud()
        enqueueWorldAlert("${node.name} Scouted", AlertCategory.DISCOVERY, AlertPriority.STANDARD, "scouted_${node.id}")
        infoPanel.visibility = View.GONE
    }

    private fun isNodeNearby(node: CampaignNode): Boolean {
        val px = campaignManager.gameState.playerMapX
        val py = campaignManager.gameState.playerMapY
        val dx = node.mapX - px
        val dy = node.mapY - py
        return dx * dx + dy * dy <= poiInteractionDistance * poiInteractionDistance
    }

    private fun showNearbyPoiIfAny() {
        if (!phaseOnePocMode || suppressAutoPoiSelection) return
        val nearbyNode = campaignManager.findNearbyRevealedNode(poiInteractionDistance)
        travelHintText.text = if (mapView.isCenteredOnPlayer()) {
            "RECON  ·  Tap POIs to inspect. Tap airspace to set flight path."
        } else {
            "SURVEY MODE  ·  Drag to scout. Tap ⌖ to follow flight."
        }
        val filteredNearbyNode = nearbyNode?.takeUnless { it.isCleared && isTemporaryNode(it) }
        if (filteredNearbyNode == null) {
            selectedNode = null
            mapView.selectedNodeId = null
            infoPanel.visibility = View.GONE
            return
        }
        travelHintText.text = "NEARBY TARGET  ·  ${filteredNearbyNode.name}"
        if (
            selectedNode?.id == filteredNearbyNode.id &&
            infoPanel.visibility == View.VISIBLE &&
            isNodeNearby(filteredNearbyNode)
        ) {
            val isAlreadyShowingActions = actionButton.visibility == View.VISIBLE &&
                actionButton.text?.toString() == "Open Actions"
            if (isAlreadyShowingActions) return
        }
        onNodeSelected(filteredNearbyNode)
    }

    private fun isTemporaryNode(node: CampaignNode): Boolean = when (node.type) {
        NodeType.ENEMY_PATROL, NodeType.RESOURCE_CACHE, NodeType.ELITE_CHALLENGE, NodeType.RECOVERY_CAMP -> true
        else -> false
    }

    private fun checkFogDiscovery(showToast: Boolean = true) {
        val newlyRevealed = campaignManager.revealPoisNearPlayer()
        if (newlyRevealed.isNotEmpty()) {
            mapView.nodes = campaignManager.campaignMap
            if (showToast) {
                val names = newlyRevealed.take(2).joinToString { it.name }
                val more = if (newlyRevealed.size > 2) " +${newlyRevealed.size - 2} more" else ""
                enqueueWorldAlert("$names Discovered$more", AlertCategory.DISCOVERY, AlertPriority.STANDARD, "poi_discovery_${newlyRevealed.size}")
            }
        }
    }

    private fun animateAndThen(targetNode: CampaignNode, action: () -> Unit) {
        val current = campaignManager.getCurrentNode()
        if (current == null || current.id == targetNode.id) {
            action()
            return
        }

        mapView.inputEnabled = false
        showPreEventState(targetNode)
        actionButton.text = "  Vectoring…"
        actionButton.isEnabled = false
        actionButton.alpha = 0.5f
        statusText.text = "▶ Vectoring to ${targetNode.name}…"
        statusText.visibility = View.VISIBLE

        mapView.animatePlayerTo(targetNode, onComplete = {
            statusText.visibility = View.GONE
            mapView.inputEnabled = true
            actionButton.isEnabled = true
            actionButton.alpha = 1f
            if (campaignManager.stepEnemyParties()) {
                mapView.enemyParties = campaignManager.gameState.enemyParties
                mapView.enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
                    party.id to campaignManager.getEnemyPartyPosition(party)
                }
                forceEnemyEngagement()
                return@animatePlayerTo
            }
            mapView.enemyParties = campaignManager.gameState.enemyParties
            mapView.enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
                party.id to campaignManager.getEnemyPartyPosition(party)
            }
            action()
        })
    }


    private fun showPreEventState(node: CampaignNode) {
        val alert = when (node.type) {
            NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS ->
                WorldAlert("Enemy Sighted", AlertCategory.DANGER, AlertPriority.HIGH, "pre_event_enemy")
            NodeType.TOWN, NodeType.VILLAGE ->
                WorldAlert("Airbase Reached", AlertCategory.TRAVEL, AlertPriority.STANDARD, "pre_event_settlement")
            NodeType.RESOURCE_CACHE ->
                WorldAlert("Fuel Cache Located", AlertCategory.DISCOVERY, AlertPriority.STANDARD, "pre_event_ruins")
            else ->
                WorldAlert("Ambush Detected", AlertCategory.EVENT, AlertPriority.HIGH, "pre_event_ambush")
        }
        enqueueWorldAlert(alert.message, alert.category, alert.priority, alert.key)
    }
    private fun engageBattle(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
        startActivity(Intent(this, BattleActivity::class.java).apply {
            putExtra("node_id", node.id)
        })
    }

    private fun restAtCamp(node: CampaignNode) {
        val gs = campaignManager.gameState
        if (gs.supplies < 20) {
            enqueueWorldAlert("Need 20 Fuel", AlertCategory.EVENT, AlertPriority.MINOR, "rest_need_20")
            infoPanel.visibility = View.GONE
            return
        }
        campaignManager.moveToNode(node.id)
        campaignManager.resolveRecoveryCamp(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        enqueueWorldAlert("Squadron Repaired", AlertCategory.GAIN, AlertPriority.STANDARD, "warband_healed")
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
    }

    private fun collectResources(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        campaignManager.resolveResourceCache(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        enqueueWorldAlert("Fuel +${node.suppliesReward}", AlertCategory.GAIN, AlertPriority.STANDARD, "supplies_collected_${node.id}")
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
    }

    private fun visitOutpost(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        node.isCleared = true
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
        startActivity(Intent(this, WarbandActivity::class.java).apply {
            putExtra("can_recruit", true)
        })
    }

    private fun restAtSettlement(node: CampaignNode, town: Boolean) {
        val gs = campaignManager.gameState
        val cost = if (town) 35 else 15
        val heal = if (town) 1.0f else 0.5f
        if (gs.supplies < cost) {
            enqueueWorldAlert("Need $cost Fuel", AlertCategory.EVENT, AlertPriority.MINOR, "rest_need_$cost")
            infoPanel.visibility = View.GONE
            return
        }
        campaignManager.moveToNode(node.id)
        gs.supplies -= cost
        gs.healWarband(heal)
        node.isCleared = true
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        updateHud()
        enqueueWorldAlert(
            if (town) "Squadron Restored" else "Squadron Partially Restored",
            AlertCategory.GAIN,
            AlertPriority.STANDARD,
            if (town) "town_restore" else "village_restore"
        )
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
    }

    // ── Tap-to-chase / intercept traveling enemy parties ────────────────────────

    private fun handleEnemyPartyTap(party: EnemyParty) {
        val pos = campaignManager.getEnemyPartyPosition(party)
        val px = campaignManager.gameState.playerMapX
        val py = campaignManager.gameState.playerMapY
        val dist = hypot(pos.first - px, pos.second - py)
        val canIntercept = dist <= 0.09f  // within intercept radius

        val options = if (canIntercept) {
            arrayOf("⚔ Attack Now", "↝ Chase", "✕ Cancel")
        } else {
            arrayOf("↝ Chase", "✕ Cancel")
        }

        showThemedSheet(
            title = "Hostile Party Spotted",
            subtitle = if (canIntercept) "Enemy within striking range!" else "Enemy is moving — intercept them?",
            options = options
        ) { label ->
            when {
                label.startsWith("⚔") -> interceptEnemyParty(party)
                label.startsWith("↝") -> chaseEnemyParty(party)
            }
        }
    }

    /**
     * Launch an immediate battle against a roaming party using its unit templates.
     * On win, the party is removed from the campaign.
     */
    private fun interceptEnemyParty(party: EnemyParty) {
        // Store the party's position as the player's current position so onBattleEnd works correctly
        val pos = campaignManager.getEnemyPartyPosition(party)
        campaignManager.setPlayerPosition(pos.first, pos.second)
        mapView.setPlayerPosition(pos.first, pos.second)
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
        val intent = Intent(this, BattleActivity::class.java).apply {
            putExtra("party_id", party.id)
        }
        startActivity(intent)
    }

    /**
     * Move the warband toward the enemy party's current position to catch them.
     */
    private fun chaseEnemyParty(party: EnemyParty) {
        val pos = campaignManager.getEnemyPartyPosition(party)
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
        if (mapView.isMovementActive()) {
            redirectMovement(pos.first, pos.second)
        } else {
            mapView.previewRouteTo(pos.first, pos.second, committed = true)
            moveWarbandTo(pos.first, pos.second)
        }
        enqueueWorldAlert("INTERCEPT VECTOR ACTIVE", AlertCategory.DANGER, AlertPriority.STANDARD, "chase_enemy_${party.id}")
    }

    // ────────────────────────────────────────────────────────────────────────────

    private fun forceEnemyEngagement() {
        val enemyNodeIds = campaignManager.gameState.enemyParties
            .filter { it.faction == PartyFaction.HOSTILE }
            .map { it.nodeId }
            .toSet()
        val current = campaignManager.getCurrentNode()
        if (current != null && enemyNodeIds.contains(current.id)) {
            enqueueWorldAlert("Enemy Interception", AlertCategory.DANGER, AlertPriority.HIGH, "enemy_interception")
            engageBattle(current)
        }
    }

    private fun showRunOverDialog() {
        showThemedOverlay { dismiss ->
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = UiTheme.gradientSurface(
                    topHex = UiTheme.SURFACE_ELEVATED,
                    bottomHex = UiTheme.SURFACE,
                    borderHex = UiTheme.BORDER,
                    radius = UiTheme.RADIUS_LG
                )
                setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_5))
                elevation = dpF(UiTheme.SHEET_ELEVATION)

                addView(View(this@CampaignActivity).apply {
                    setBackgroundColor(Color.parseColor(Palette.GOLD))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply {
                    bottomMargin = dp(UiTheme.SPACE_4)
                })

                addView(TextView(this@CampaignActivity).apply {
                    text = "CAMPAIGN OVER"
                    textSize = UiTheme.TEXT_CHIP
                    letterSpacing = 0.12f
                    typeface = UiTheme.TYPEFACE_LABEL
                    setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
                })
                addView(TextView(this@CampaignActivity).apply {
                    text = "Final Report"
                    textSize = UiTheme.TEXT_SECTION
                    typeface = UiTheme.TYPEFACE_HEADING
                    setTextColor(Color.parseColor(Palette.HUD_TEXT))
                    setPadding(0, dp(UiTheme.SPACE_1), 0, dp(UiTheme.SPACE_3))
                })
                addView(TextView(this@CampaignActivity).apply {
                    text = campaignManager.getRunSummary()
                    textSize = UiTheme.TEXT_SECONDARY
                    typeface = UiTheme.TYPEFACE_BODY
                    setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
                    setLineSpacing(4f, 1f)
                    setPadding(0, 0, 0, dp(UiTheme.SPACE_4))
                })

                addView(Button(this@CampaignActivity).apply {
                    text = "New Campaign"
                    applyPrimaryButtonStyle()
                    textSize = UiTheme.TEXT_BUTTON
                    minHeight = dp(UiTheme.BUTTON_HEIGHT)
                    minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
                    setOnClickListener {
                        dismiss()
                        campaignManager.startNewCampaign()
                        mapView.nodes = campaignManager.campaignMap
                        mapView.currentNodeId = campaignManager.gameState.currentNodeId
                        mapView.recenterOnPlayer()
                        mapView.invalidate()
                        infoPanel.visibility = View.GONE
                        updateHud()
                    }
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ))
                addView(Button(this@CampaignActivity).apply {
                    text = "Main Menu"
                    applySecondaryStyle()
                    minHeight = dp(UiTheme.BUTTON_HEIGHT)
                    minimumHeight = dp(UiTheme.BUTTON_HEIGHT)
                    setOnClickListener { dismiss(); finish() }
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(UiTheme.SPACE_2) })
            }
        }
    }
}
