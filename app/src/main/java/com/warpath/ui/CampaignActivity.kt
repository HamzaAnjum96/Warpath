package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.warpath.engine.CampaignManager
import com.warpath.model.CampaignNode
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
    private lateinit var mapStateText: TextView
    private lateinit var alertBanner: LinearLayout
    private lateinit var alertAccent: View
    private lateinit var alertIconText: TextView
    private lateinit var alertMessageText: TextView
    private lateinit var alertEdgeTint: View

    private val phaseOnePocMode = true
    private val poiInteractionDistance = 0.07f
    private val playerMetersPerMoveAction = 50f
    private var selectedNode: CampaignNode? = null
    private var suppressAutoPoiSelection = false
    private var pendingTravelTarget: Pair<Float, Float>? = null
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

    private enum class AlertPriority(val holdMs: Long) {
        MINOR(1400L),
        STANDARD(1900L),
        HIGH(2500L)
    }

    private enum class AlertCategory(val accentHex: String, val icon: String) {
        DANGER("#C65A5A", "⚠"),
        DISCOVERY("#B89C6B", "◈"),
        TRAVEL("#8E9BB0", "➤"),
        GAIN("#5FAF7A", "✦"),
        EVENT("#5C6BC0", "✧")
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

        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#0E1726")) }

        mapView = CampaignMapView(this).apply {
            nodes = campaignManager.campaignMap
            currentNodeId = campaignManager.gameState.currentNodeId
            enemyParties = campaignManager.gameState.enemyParties
            enemyDisplayPositions = campaignManager.gameState.enemyParties.associate { party ->
                party.id to campaignManager.getEnemyPartyPosition(party)
            }
            showPaths = !phaseOnePocMode
            setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
            onMapTapped = { normX, normY ->
                handleMapTap(normX, normY)
            }
            onFocusChanged = { focused ->
                recenterButton.visibility = if (focused) View.GONE else View.VISIBLE
                if (!focused) {
                    enqueueWorldAlert("Scouting View", AlertCategory.TRAVEL, AlertPriority.MINOR, "map_unfocused")
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
            buildAlertBanner(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = dp(68)
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
            textSize = if (compactUi) 13f else 15f
            setTextColor(Color.parseColor("#D4B15A"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(Color.parseColor("#C0111C2D"))
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
            contentDescription = "Recenter map on warband"
            textSize = if (compactUi) 11f else 12f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setTextColor(Color.parseColor("#F2F0EA"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            stateListAnimator = null
            visibility = View.GONE
            applyHudButtonStyle()
            setOnClickListener {
                mapView.recenterOnPlayer()
                enqueueWorldAlert("Warband Focus Restored", AlertCategory.TRAVEL, AlertPriority.MINOR, "camera_recentered")
            }
        }
        root.addView(
            recenterButton,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = dp(78)
                rightMargin = dp(16)
            }
        )

        mapStateText = TextView(this).apply {
            textSize = if (compactUi) 9f else 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
            setPadding(dp(10), dp(5), dp(10), dp(5))
            letterSpacing = 0.08f
        }
        styleChip(mapStateText, "#132033")
        root.addView(
            mapStateText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = dp(124)
                rightMargin = dp(16)
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

        stopMovementButton = Button(this).apply {
            text = "Stop"
            contentDescription = "Stop current movement"
            textSize = if (compactUi) 10f else 11f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setTextColor(Color.parseColor("#F2F0EA"))
            minHeight = dp(34)
            minimumHeight = dp(34)
            applyHudButtonStyle(cornerRadius = 10f)
            visibility = View.GONE
            setOnClickListener { mapView.cancelMovement() }
        }
        root.addView(
            stopMovementButton,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.BOTTOM
            ).apply {
                rightMargin = dp(16)
                bottomMargin = dp(if (compactUi) 126 else 140)
            }
        )

        travelHintText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor("#B8C2D1"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(12), dp(6))
            text = "◉ SCOUTING MODE · Tap to select, tap terrain to travel"
            background = GradientDrawable().apply {
                cornerRadius = dpF(9f)
                setColor(Color.parseColor("#DD111C2D"))
                setStroke(dp(1), Color.parseColor("#27415E"))
            }
        }
        root.addView(
            travelHintText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.START
            ).apply {
                leftMargin = dp(18)
                bottomMargin = dp(if (compactUi) 360 else 390)
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
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpF(0f)
                setColor(Color.parseColor("#E00E1726"))
                setStroke(dp(1), Color.parseColor("#27415E"))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "WARPATH"
            textSize = if (compactUi) 15f else 17f
            setTextColor(Color.parseColor("#D4B15A"))
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            letterSpacing = 0.08f
        }
        bar.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        suppliesText = hudStatText()
        renownText = hudStatText()
        warbandText = hudStatText()

        bar.addView(suppliesText)
        bar.addView(hudSpacer())
        bar.addView(renownText)
        bar.addView(hudSpacer())
        bar.addView(warbandText)
        bar.addView(hudSpacer())

        val warbandBtn = Button(this).apply {
            text = "⚑"
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor("#F2F0EA"))
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            stateListAnimator = null
            applyHudButtonStyle(cornerRadius = 9f)
            setOnClickListener { startActivity(Intent(this@CampaignActivity, WarbandActivity::class.java)) }
        }
        bar.addView(warbandBtn)

        return bar
    }

    private fun hudStatText() = TextView(this).apply {
        textSize = if (compactUi) 12f else 13f
        setTextColor(Color.parseColor("#B8C2D1"))
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private fun hudSpacer(): View = View(this).also {
        it.layoutParams = LinearLayout.LayoutParams(dp(14), 1)
    }

    private fun buildAlertBanner(): View {
        alertBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(14), dp(8))
            alpha = 0f
            translationY = -dp(12).toFloat()
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpF(8f)
                setColor(Color.parseColor("#EE111C2D"))
                setStroke(dp(1), Color.parseColor("#27415E"))
            }
            elevation = dpF(3f)
        }

        alertAccent = View(this).apply {
            setBackgroundColor(Color.parseColor(AlertCategory.TRAVEL.accentHex))
        }
        alertBanner.addView(
            alertAccent,
            LinearLayout.LayoutParams(dp(3), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(8)
            }
        )

        alertIconText = TextView(this).apply {
            text = AlertCategory.EVENT.icon
            textSize = if (compactUi) 11f else 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
            setPadding(0, 0, dp(8), 0)
        }
        alertBanner.addView(alertIconText)

        alertMessageText = TextView(this).apply {
            textSize = if (compactUi) 12f else 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
            letterSpacing = 0.02f
        }
        alertBanner.addView(alertMessageText)
        return alertBanner
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
        hideCurrentWorldAlert(durationMs = 90L)
    }

    private fun hideCurrentWorldAlert(durationMs: Long = 170L) {
        alertHideRunnable?.let { uiHandler.removeCallbacks(it) }
        alertHideRunnable = null
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
            AlertCategory.TRAVEL -> "#255E8FD6"
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

    private fun compactRowText() = TextView(this).apply {
        textSize = if (compactUi) 10f else 11f
        setTextColor(Color.parseColor("#B8C2D1"))
        setPadding(0, dp(1), 0, dp(1))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun buildInfoPanel(): View {
        val container = FrameLayout(this)

        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpF(12f)
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                colors = intArrayOf(Color.parseColor("#F0111C2D"), Color.parseColor("#F00E1726"))
                setStroke(dp(1), Color.parseColor("#27415E"))
            }
            visibility = View.GONE
        }

        panelAccentBar = View(this)
        infoPanel.addView(
            panelAccentBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                3
            )
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nodeTypeChip = TextView(this).apply {
            textSize = if (compactUi) 10f else 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            letterSpacing = 0.08f
        }
        headerRow.addView(
            nodeTypeChip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 14 }
        )

        nodeNameText = TextView(this).apply {
            textSize = if (compactUi) 17f else 19f
            setTextColor(Color.parseColor("#F2F0EA"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.02f
        }
        headerRow.addView(nodeNameText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val dismissX = TextView(this).apply {
            text = "✕"
            textSize = if (compactUi) 16f else 18f
            setTextColor(Color.parseColor("#8694A8"))
            setPadding(8, 0, 4, 0)
            setOnClickListener {
                infoPanel.visibility = View.GONE
                selectedNode = null
                mapView.selectedNodeId = null
                updateMapStateText()
            }
        }
        headerRow.addView(dismissX)

        content.addView(headerRow)

        nodeRangeTagText = TextView(this).apply {
            textSize = if (compactUi) 10f else 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
            setPadding(dp(10), dp(5), dp(10), dp(5))
            letterSpacing = 0.06f
            visibility = View.GONE
        }
        content.addView(
            nodeRangeTagText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        )

        distanceText = TextView(this).apply {
            textSize = if (compactUi) 10f else 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.parseColor("#B8C2D1"))
            setPadding(dp(0), dp(8), dp(0), dp(0))
        }
        content.addView(distanceText)

        content.addView(
            View(this).apply { setBackgroundColor(Color.parseColor("#22344B")) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = 10
                bottomMargin = 10
            }
        )

        nodeDescText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor("#B8C2D1"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(2f, 1f)
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
            ).apply { bottomMargin = 6 }
        )

        threatRowText = compactRowText()
        strengthRowText = compactRowText()
        suppliesRowText = compactRowText()
        reputationRowText = compactRowText()
        content.addView(threatRowText)
        content.addView(strengthRowText)
        content.addView(suppliesRowText)
        content.addView(reputationRowText)

        nodeStatsText = TextView(this).apply {
            textSize = if (compactUi) 11f else 12f
            setTextColor(Color.parseColor("#8694A8"))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        content.addView(
            nodeStatsText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14 }
        )

        actionButton = Button(this).apply {
            textSize = if (compactUi) 13f else 15f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
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
            setTextColor(Color.parseColor("#8694A8"))
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(2))
            visibility = View.GONE
        }
        content.addView(actionSecondaryText)

        infoPanel.addView(content)
        container.addView(
            infoPanel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return container
    }

    private fun Button.applyRoundedStyle(
        backgroundColor: String,
        borderColor: String = "#7A514F",
        topEdgeColor: String = "#9C706C",
        cornerRadius: Float = 28f
    ) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(Color.parseColor(topEdgeColor), Color.parseColor(backgroundColor))
            setStroke(dp(1), Color.parseColor(borderColor))
            this.cornerRadius = cornerRadius
        }
    }

    private fun Button.applyHudButtonStyle(cornerRadius: Float = 11f) {
        applyRoundedStyle(
            backgroundColor = "#132033",
            borderColor = "#27415E",
            topEdgeColor = "#22344B",
            cornerRadius = cornerRadius
        )
    }

    private fun Button.applyPrimaryButtonStyle() {
        applyRoundedStyle(
            backgroundColor = "#5C6BC0",
            borderColor = "#5C6BC0",
            topEdgeColor = "#5C6BC0",
            cornerRadius = 12f
        )
    }

    private fun styleChip(chip: TextView, fillHex: String) {
        chip.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpF(12f)
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
        val hasTarget = selectedNode != null
        val hasPreview = pendingTravelTarget != null
        val centered = mapView.isCenteredOnPlayer()
        val (label, color) = when {
            moving -> "TRAVELLING" to "#22344B"
            hasPreview -> mapView.currentPreviewRouteTypeLabel() to "#27415E"
            hasTarget -> "TARGET LOCKED" to "#27415E"
            !centered -> "SCOUTING" to "#27415E"
            else -> "FOLLOW WARBAND" to "#22344B"
        }
        mapStateText.text = label
        styleChip(mapStateText, color)
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
        mapView.showPaths = !phaseOnePocMode
        mapView.invalidate()
        recenterButton.visibility = if (mapView.isCenteredOnPlayer()) View.GONE else View.VISIBLE
        updateMapStateText()
        updateHud()
        checkFogDiscovery(showToast = false)
        showNearbyPoiIfAny()

        if (campaignManager.isRunOver()) {
            showRunOverDialog()
        }
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        alertHideRunnable = null
        alertQueue.clear()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        alertHideRunnable?.let { uiHandler.removeCallbacks(it) }
        alertHideRunnable = null
    }

    private fun updateHud() {
        val gs = campaignManager.gameState
        suppliesText.text = "◈ ${gs.supplies}"
        renownText.text = "★ ${gs.renown}"
        val livingTroops = gs.warband.sumOf { it.count.coerceAtLeast(0) }
        warbandText.text = "⚔ $livingTroops"
    }

    private fun handleMapTap(normX: Float, normY: Float) {
        val tappedNode = findNodeNear(normX, normY)
        if (tappedNode != null) {
            pendingTravelTarget = null
            mapView.clearRoutePreview()
            onNodeSelected(tappedNode)
            return
        }
        selectedNode = null
        mapView.selectedNodeId = null
        infoPanel.visibility = View.GONE
        val pending = pendingTravelTarget
        if (pending != null && hypot(pending.first - normX, pending.second - normY) < 0.03f) {
            pendingTravelTarget = null
            mapView.previewRouteTo(normX, normY, committed = true)
            moveWarbandTo(normX, normY)
        } else {
            pendingTravelTarget = Pair(normX, normY)
            mapView.previewRouteTo(normX, normY, committed = false)
            enqueueWorldAlert("Route Marked", AlertCategory.TRAVEL, AlertPriority.MINOR, "route_preview")
            updateMapStateText()
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
        infoPanel.visibility = View.VISIBLE
        updateMapStateText()

        panelAccentBar.setBackgroundColor(node.type.color.toInt())

        nodeTypeChip.text = node.type.displayName.uppercase()
        styleChip(nodeTypeChip, "#22344B")
        nodeNameText.text = node.name
        nodeNameText.setTextColor(Color.parseColor("#F2F0EA"))

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
            nodeStatsText.text = "✓ Location cleared"
            nodeStatsText.setTextColor(Color.parseColor("#5FAF7A"))
            actionButton.visibility = View.GONE

        } else if (isCurrent || isAccessible) {
            nodeStatsText.setTextColor(Color.parseColor("#B89C6B"))
            when (node.type) {
                NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS -> {
                    val enemyCount = node.enemySquads.sumOf { it.count }
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Enemies: $enemyCount  |  ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text = if (node.type == NodeType.BOSS) "⚔ Storm the Stronghold!" else "⚔ Attack!"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { engageBattle(node) } }
                }

                NodeType.RECOVERY_CAMP -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 20 supplies  |  Heal 40% HP"
                    actionButton.text = "♥ Rest & Heal"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtCamp(node) } }
                }

                NodeType.RESOURCE_CACHE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Reward: ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text = "◈ Collect Supplies"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { collectResources(node) } }
                }

                NodeType.FACTION_OUTPOST -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Recruit troops and resupply"
                    actionButton.text = "⚑ Visit Outpost"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { visitOutpost(node) } }
                }

                NodeType.TOWN -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 35 supplies  |  Full heal + recruit support"
                    actionButton.text = "♜ Rest in Town"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, true) } }
                }

                NodeType.VILLAGE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 15 supplies  |  Heal 50%"
                    actionButton.text = "⌂ Rest in Village"
                    actionButton.applyPrimaryButtonStyle()
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, false) } }
                }

                NodeType.START -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Your staging ground"
                    actionButton.visibility = View.GONE
                }
            }
        } else {
            nodeDescText.text = node.description
            nodeStatsText.text = "⚠ Not accessible — scout closer to discover this POI"
            nodeStatsText.setTextColor(Color.parseColor("#C65A5A"))
            actionButton.visibility = View.GONE
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

        nodeStatsText.text = "Out of range. Travel to this location."
        actionButton.text = "Travel"
        actionButton.visibility = View.VISIBLE
        actionButton.applyPrimaryButtonStyle()
        actionButton.setOnClickListener { moveWarbandTo(node.mapX, node.mapY) }
    }

    private fun updateRangeTag(node: CampaignNode, isNearby: Boolean) {
        val (label, bg) = when {
            node.type == NodeType.ENEMY_PATROL || node.type == NodeType.ELITE_CHALLENGE || node.type == NodeType.BOSS ->
                if (isNearby) "HOSTILE · NEARBY" to "#C65A5A" else "HOSTILE" to "#C65A5A"
            node.type == NodeType.TOWN || node.type == NodeType.VILLAGE -> if (isNearby) "ALLY · NEARBY" to "#5E8FD6" else "ALLY" to "#5E8FD6"
            node.type == NodeType.RESOURCE_CACHE -> if (isNearby) "NEARBY" to "#8E9BB0" else "NEARBY" to "#8E9BB0"
            else -> if (isNearby) "NEUTRAL · NEARBY" to "#8E9BB0" else "NEUTRAL" to "#8E9BB0"
        }
        nodeRangeTagText.text = label
        nodeRangeTagText.visibility = View.VISIBLE
        styleChip(nodeRangeTagText, bg)
    }

    private fun bindCompactRows(node: CampaignNode) {
        val enemyStrength = node.enemySquads.sumOf { it.count }
        val threatText = when (node.type) {
            NodeType.BOSS -> "High"
            NodeType.ELITE_CHALLENGE, NodeType.ENEMY_PATROL -> "Medium"
            else -> "Low"
        }
        threatRowText.text = "Threat: $threatText"
        strengthRowText.text = "Strength: ${enemyStrength.coerceAtLeast(0)}"
        suppliesRowText.text = "Supplies: ${node.suppliesReward.coerceAtLeast(0)}"
        reputationRowText.text = "Reputation: +${node.renownReward.coerceAtLeast(0)}"
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
                arrayOf("Recruit", "Heal", "Trade Supplies", "Rest", "Gather Rumours")

            NodeType.FACTION_OUTPOST ->
                arrayOf("Recruit", "Trade Supplies", "Take Contract")

            NodeType.RESOURCE_CACHE ->
                arrayOf("Investigate", "Spend Supplies", "Ignore")

            NodeType.RECOVERY_CAMP ->
                arrayOf("Heal", "Rest", "Ignore")

            NodeType.START ->
                arrayOf("Reorganize Warband", "Scout Routes")
        }

        val dialog = BottomSheetDialog(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(26))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(dpF(20f), dpF(20f), dpF(20f), dpF(20f), 0f, 0f, 0f, 0f)
                setColor(Color.parseColor("#111C2D"))
                setStroke(dp(1), Color.parseColor("#27415E"))
            }
        }
        val title = TextView(this).apply {
            text = "${node.name} · Actions"
            textSize = 18f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
        }
        container.addView(title)
        val subtitle = TextView(this).apply {
            text = "Choose a contextual option"
            textSize = 12f
            setTextColor(Color.parseColor("#8694A8"))
            setPadding(0, dp(4), 0, dp(12))
        }
        container.addView(subtitle)

        options.forEach { label ->
            val row = TextView(this).apply {
                text = label
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(Color.parseColor("#F2F0EA"))
                setPadding(dp(12), dp(9), dp(12), dp(9))
                background = GradientDrawable().apply {
                    cornerRadius = dpF(9f)
                    setColor(Color.parseColor("#132033"))
                    setStroke(dp(1), Color.parseColor("#27415E"))
                }
                setOnClickListener {
                    dialog.dismiss()
                    handlePoiAction(node, label)
                }
            }
            container.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            })
        }
        dialog.setContentView(container)
        dialog.show()
    }

    private fun handlePoiAction(node: CampaignNode, action: String) {
        when {
            action == "Attack" || action == "Ambush" -> {
                scoutFromNode(node, revealHideoutIntel = node.type == NodeType.ELITE_CHALLENGE)
                showOutcomeSheet(
                    title = "Victory at ${node.name}",
                    summary = "Your warband forced the enemy line to break.",
                    details = listOf(
                        "+${node.suppliesReward.coerceAtLeast(4)} supplies secured",
                        "Renown +${node.renownReward.coerceAtLeast(1)}",
                        "Nearby route pressure reduced"
                    )
                )
            }

            action == "Flee" -> {
                handleRunAttempt(node)
            }

            action.startsWith("Bribe") -> {
                handleBribe(node)
            }

            action == "Ignore" || action == "Observe" || action == "Gather Rumours" -> {
                enqueueWorldAlert("Route Maintained", AlertCategory.TRAVEL, AlertPriority.MINOR, "avoid_trouble")
            }

            action == "Trade Supplies" -> {
                campaignManager.gameState.supplies += if (node.type == NodeType.TOWN || node.type == NodeType.VILLAGE) 20 else 14
                scoutFromNode(node)
                updateHud()
                enqueueWorldAlert("Supplies Gained", AlertCategory.GAIN, AlertPriority.MINOR, "supplies_stocked")
            }

            action == "Spend Supplies" -> {
                if (campaignManager.gameState.supplies < 8) {
                    enqueueWorldAlert("Need 8 Supplies", AlertCategory.EVENT, AlertPriority.MINOR, "need_8_supplies")
                    return
                }
                campaignManager.gameState.supplies -= 8
                campaignManager.gameState.renown += 3
                scoutFromNode(node)
                updateHud()
                enqueueWorldAlert("Reputation Gained", AlertCategory.GAIN, AlertPriority.STANDARD, "expedition_renown")
            }

            action == "Recruit" || action == "Reorganize Warband" || action == "Take Contract" -> {
                scoutFromNode(node)
                startActivity(Intent(this, WarbandActivity::class.java).apply {
                    putExtra("can_recruit", true)
                })
            }

            action == "Rest" || action == "Heal" -> {
                campaignManager.gameState.healWarband(if (node.type == NodeType.TOWN) 1.0f else 0.55f)
                scoutFromNode(node)
                enqueueWorldAlert("Warband Recovered", AlertCategory.GAIN, AlertPriority.STANDARD, "warband_recovered")
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
                    summary = "Your scouts found a gap and the warband disengaged.",
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
                    summary = "You escaped but had to abandon part of the pack train.",
                    details = listOf("-$loss supplies", "Enemy remains active")
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
            enqueueWorldAlert("Need $cost Supplies", AlertCategory.EVENT, AlertPriority.MINOR, "bribe_need_$cost")
            return
        }
        gs.supplies -= cost
        gs.renown = (gs.renown - 2).coerceAtLeast(0)
        updateHud()
        showOutcomeSheet(
            title = "Negotiated Passage",
            summary = "The opposing party accepted payment and stood down.",
            details = listOf("-$cost supplies", "Renown -2")
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
        val dialog = BottomSheetDialog(this)
        val isDefeatTone = title.contains("Failed", ignoreCase = true) || title.contains("Retreat", ignoreCase = true)
        val isDiscoveryTone = title.contains("Intel", ignoreCase = true) || title.contains("Discovered", ignoreCase = true)
        val background = when {
            isDefeatTone -> "#24191A"
            isDiscoveryTone -> "#2A201F"
            else -> "#2A1D1D"
        }
        val header = when {
            isDefeatTone -> "RESOLUTION"
            isDiscoveryTone -> "DISCOVERY"
            else -> "VICTORY"
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(20))
            setBackgroundColor(Color.parseColor(background))
        }
        content.addView(TextView(this).apply {
            text = header
            textSize = 10f
            letterSpacing = 0.1f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#8694A8"))
        })
        content.addView(TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
            setPadding(0, dp(4), 0, dp(4))
        })
        content.addView(TextView(this).apply {
            text = summary
            textSize = 12f
            setTextColor(Color.parseColor("#B8C2D1"))
            setPadding(0, 0, 0, dp(10))
        })
        details.forEach { line ->
            content.addView(TextView(this).apply {
                text = line
                textSize = 12f
                setTextColor(Color.parseColor("#F2F0EA"))
                setPadding(0, dp(2), 0, dp(2))
            })
        }
        content.addView(Button(this).apply {
            text = "Next Action"
            isAllCaps = false
            minHeight = dp(38)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#F2F0EA"))
            applyPrimaryButtonStyle()
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
        dialog.setContentView(content)
        dialog.show()
    }

    private fun moveWarbandTo(normX: Float, normY: Float) {
        pendingTravelTarget = null
        val startX = campaignManager.gameState.playerMapX
        val startY = campaignManager.gameState.playerMapY
        val totalDistanceNorm = hypot(normX - startX, normY - startY)
        if (totalDistanceNorm < 0.00002f) return
        val enemySnapshot = campaignManager.createEnemyMovementSnapshot()
        var latestProgressNorm = 0f
        var latestPlayerX = startX
        var latestPlayerY = startY
        mapView.inputEnabled = false
        statusText.text = "▶ ${mapView.currentPreviewRouteTypeLabel()} · 0%"
        statusText.visibility = View.VISIBLE
        stopMovementButton.visibility = View.VISIBLE
        updateMapStateText()
        mapView.animatePlayerTo(
            normX,
            normY,
            onProgress = { t, x, y ->
                latestProgressNorm = totalDistanceNorm * t
                latestPlayerX = x
                latestPlayerY = y
                mapView.enemyDisplayPositions = campaignManager.enemyPreviewPositions(
                    snapshot = enemySnapshot,
                    playerMovedNorm = latestProgressNorm,
                    totalPlayerTravelNorm = totalDistanceNorm,
                    playerMetersPerAction = playerMetersPerMoveAction
                )
                val meters = (latestProgressNorm * 1000f).toInt()
                statusText.text = "▶ Travelling · ${(t * 100f).toInt()}% · ${meters}m"
            }
        ) { cancelled ->
            val movedNorm = latestProgressNorm
            campaignManager.setPlayerPosition(latestPlayerX, latestPlayerY)
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
            mapView.inputEnabled = true
            statusText.visibility = View.GONE
            stopMovementButton.visibility = View.GONE
            updateMapStateText()
            if (cancelled) {
                enqueueWorldAlert("Movement Halted", AlertCategory.TRAVEL, AlertPriority.MINOR, "movement_abort")
            }
            if (playerHit) {
                forceEnemyEngagement()
                return@animatePlayerTo
            }
            showNearbyPoiIfAny()
        }
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
            "Tap POIs to inspect. Tap terrain to travel. Pinch to zoom."
        } else {
            "Scouting mode — drag to survey. Tap ⌖ to follow warband."
        }
        val filteredNearbyNode = nearbyNode?.takeUnless { it.isCleared && isTemporaryNode(it) }
        if (filteredNearbyNode == null) {
            selectedNode = null
            mapView.selectedNodeId = null
            infoPanel.visibility = View.GONE
            return
        }
        travelHintText.text = "Nearby: ${filteredNearbyNode.name}"
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
        actionButton.text = "  Traveling…"
        actionButton.isEnabled = false
        actionButton.alpha = 0.5f
        statusText.text = "▶ Marching to ${targetNode.name}…"
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
                WorldAlert("Settlement Reached", AlertCategory.TRAVEL, AlertPriority.STANDARD, "pre_event_settlement")
            NodeType.RESOURCE_CACHE ->
                WorldAlert("Ruins Found", AlertCategory.DISCOVERY, AlertPriority.STANDARD, "pre_event_ruins")
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
            enqueueWorldAlert("Need 20 Supplies", AlertCategory.EVENT, AlertPriority.MINOR, "rest_need_20")
            infoPanel.visibility = View.GONE
            return
        }
        campaignManager.moveToNode(node.id)
        campaignManager.resolveRecoveryCamp(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        enqueueWorldAlert("Warband Healed", AlertCategory.GAIN, AlertPriority.STANDARD, "warband_healed")
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
    }

    private fun collectResources(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        campaignManager.resolveResourceCache(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        enqueueWorldAlert("Supplies +${node.suppliesReward}", AlertCategory.GAIN, AlertPriority.STANDARD, "supplies_collected_${node.id}")
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
            enqueueWorldAlert("Need $cost Supplies", AlertCategory.EVENT, AlertPriority.MINOR, "rest_need_$cost")
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
            if (town) "Warband Restored" else "Warband Partially Restored",
            AlertCategory.GAIN,
            AlertPriority.STANDARD,
            if (town) "town_restore" else "village_restore"
        )
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
    }

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
        AlertDialog.Builder(this)
            .setTitle("Campaign Over")
            .setMessage(campaignManager.getRunSummary())
            .setPositiveButton("New Campaign") { _, _ ->
                campaignManager.startNewCampaign()
                mapView.nodes = campaignManager.campaignMap
                mapView.currentNodeId = campaignManager.gameState.currentNodeId
                mapView.recenterOnPlayer()
                mapView.invalidate()
                infoPanel.visibility = View.GONE
                updateHud()
            }
            .setNegativeButton("Main Menu") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
