package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import kotlin.random.Random
import kotlin.math.hypot

class CampaignActivity : AppCompatActivity() {

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
    private lateinit var nodeRangeTagText: TextView
    private lateinit var actionButton: Button
    private lateinit var panelAccentBar: View
    private lateinit var statusText: TextView
    private lateinit var travelHintText: TextView
    private lateinit var recenterButton: Button
    private lateinit var stopMovementButton: Button

    private val phaseOnePocMode = true
    private val poiInteractionDistance = 0.07f
    private val playerMetersPerMoveAction = 50f
    private var selectedNode: CampaignNode? = null
    private var suppressAutoPoiSelection = false

    private val density by lazy { resources.displayMetrics.density }
    private val screenWidthDp by lazy { resources.displayMetrics.widthPixels / density }
    private val compactUi by lazy { screenWidthDp < 420f }

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

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0a0a18"))
        }

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
                    Toast.makeText(context, "Map unfocused. Drag to scout. Tap ⌖ to lock on warband.", Toast.LENGTH_SHORT).show()
                }
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

        statusText = TextView(this).apply {
            textSize = if (compactUi) 13f else 15f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(Color.parseColor("#cc0a0a18"))
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
            textSize = if (compactUi) 11f else 12f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setTextColor(Color.parseColor("#F5EED1"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            stateListAnimator = null
            visibility = View.GONE
            applyRoundedStyle(backgroundColor = "#4A3F88")
            setOnClickListener {
                mapView.recenterOnPlayer()
                Toast.makeText(this@CampaignActivity, "Camera recentered on warband.", Toast.LENGTH_SHORT).show()
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

        root.addView(
            buildMapControls(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.BOTTOM
            ).apply {
                rightMargin = dp(16)
                bottomMargin = dp(if (compactUi) 172 else 186)
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
            text = "✕ Stop"
            textSize = if (compactUi) 11f else 12f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setTextColor(Color.WHITE)
            applyRoundedStyle(backgroundColor = "#8A2A2A")
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
            setTextColor(Color.parseColor("#d7def7"))
            setBackgroundColor(Color.parseColor("#aa131d32"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), dp(8), dp(12), dp(8))
            text = "Tap to select. Tap terrain to travel. Pinch to zoom."
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
        checkFogDiscovery(showToast = false)
        showNearbyPoiIfAny()
    }

    private fun buildTopHud(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#dd0a0a18"))
            setPadding(dp(18), dp(12), dp(18), dp(12))
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "SARHAD"
            textSize = if (compactUi) 15f else 17f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 1f, 1f, Color.parseColor("#80000000"))
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
            setTextColor(Color.parseColor("#aaaacc"))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 8, 16, 8)
            stateListAnimator = null
            applyRoundedStyle(backgroundColor = "#33334a")
            setOnClickListener { startActivity(Intent(this@CampaignActivity, WarbandActivity::class.java)) }
        }
        bar.addView(warbandBtn)

        return bar
    }

    private fun hudStatText() = TextView(this).apply {
        textSize = if (compactUi) 12f else 13f
        setTextColor(Color.parseColor("#aaaacc"))
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun hudSpacer(): View = View(this).also {
        it.layoutParams = LinearLayout.LayoutParams(dp(14), 1)
    }

    private fun buildInfoPanel(): View {
        val container = FrameLayout(this)

        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpF(16f)
                setColor(Color.parseColor("#E10C1223"))
                setStroke(dp(1), Color.parseColor("#334A6AA3"))
            }
            visibility = View.GONE
        }

        panelAccentBar = View(this)
        infoPanel.addView(
            panelAccentBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                5
            )
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(16))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nodeTypeChip = TextView(this).apply {
            textSize = if (compactUi) 10f else 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(4), dp(10), dp(4))
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
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        headerRow.addView(nodeNameText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val dismissX = TextView(this).apply {
            text = "✕"
            textSize = if (compactUi) 16f else 18f
            setTextColor(Color.parseColor("#666688"))
            setPadding(8, 0, 4, 0)
            setOnClickListener { infoPanel.visibility = View.GONE }
        }
        headerRow.addView(dismissX)

        content.addView(headerRow)

        nodeRangeTagText = TextView(this).apply {
            textSize = if (compactUi) 10f else 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0E1220"))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            visibility = View.GONE
        }
        content.addView(
            nodeRangeTagText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        )

        content.addView(
            View(this).apply { setBackgroundColor(Color.parseColor("#222244")) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = 10
                bottomMargin = 10
            }
        )

        nodeDescText = TextView(this).apply {
            textSize = if (compactUi) 12f else 13f
            setTextColor(Color.parseColor("#C3CAE2"))
            setLineSpacing(2f, 1f)
        }
        content.addView(
            nodeDescText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        )

        nodeStatsText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#8EA2C6"))
        }
        content.addView(
            nodeStatsText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14 }
        )

        actionButton = Button(this).apply {
            textSize = if (compactUi) 14f else 16f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(12), dp(16), dp(12))
            stateListAnimator = null
            applyRoundedStyle(backgroundColor = "#225588")
        }
        content.addView(
            actionButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

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

    private fun buildMapControls(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val zoomIn = mapIconButton("＋") { mapView.zoomIn() }
        val zoomOut = mapIconButton("－") { mapView.zoomOut() }
        container.addView(zoomIn)
        container.addView(
            zoomOut,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 }
        )
        return container
    }

    private fun mapIconButton(symbol: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = symbol
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#D7DCEF"))
            isAllCaps = false
            setPadding(dp(10), dp(6), dp(10), dp(6))
            stateListAnimator = null
            alpha = 0.82f
            applyRoundedStyle(backgroundColor = "#2B2F45")
            setOnClickListener { onClick() }
        }
    }

    private fun Button.applyRoundedStyle(backgroundColor: String, cornerRadius: Float = 28f) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(backgroundColor))
            this.cornerRadius = cornerRadius
        }
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
        updateHud()
        checkFogDiscovery(showToast = false)
        showNearbyPoiIfAny()

        if (campaignManager.isRunOver()) {
            showRunOverDialog()
        }
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
            onNodeSelected(tappedNode)
            return
        }
        selectedNode = null
        mapView.selectedNodeId = null
        infoPanel.visibility = View.GONE
        moveWarbandTo(normX, normY)
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
        mapView.selectedNodeId = node.id
        infoPanel.visibility = View.VISIBLE

        panelAccentBar.setBackgroundColor(node.type.color.toInt())

        nodeTypeChip.text = node.type.displayName.uppercase()
        nodeTypeChip.setBackgroundColor(
            Color.argb(
                180,
                Color.red(node.type.color.toInt()),
                Color.green(node.type.color.toInt()),
                Color.blue(node.type.color.toInt())
            )
        )

        nodeNameText.text = node.name
        nodeNameText.setTextColor(node.type.color.toInt())

        val isAccessible = node.isRevealed
        val isCurrent = node.id == campaignManager.gameState.currentNodeId
        val isNearby = isNodeNearby(node)

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
            nodeStatsText.setTextColor(Color.parseColor("#44aa44"))
            actionButton.visibility = View.GONE

        } else if (isCurrent || isAccessible) {
            nodeStatsText.setTextColor(Color.parseColor("#6688aa"))
            when (node.type) {
                NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS -> {
                    val enemyCount = node.enemySquads.sumOf { it.count }
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Enemies: $enemyCount  |  ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text = if (node.type == NodeType.BOSS) "⚔ Storm the Stronghold!" else "⚔ Attack!"
                    actionButton.applyRoundedStyle(backgroundColor = "#aa2222")
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { engageBattle(node) } }
                }

                NodeType.RECOVERY_CAMP -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 20 supplies  |  Heal 40% HP"
                    actionButton.text = "♥ Rest & Heal"
                    actionButton.applyRoundedStyle(backgroundColor = "#225588")
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtCamp(node) } }
                }

                NodeType.RESOURCE_CACHE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Reward: ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text = "◈ Collect Supplies"
                    actionButton.applyRoundedStyle(backgroundColor = "#226633")
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { collectResources(node) } }
                }

                NodeType.FACTION_OUTPOST -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Recruit troops and resupply"
                    actionButton.text = "⚑ Visit Outpost"
                    actionButton.applyRoundedStyle(backgroundColor = "#885522")
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { visitOutpost(node) } }
                }

                NodeType.TOWN -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 35 supplies  |  Full heal + recruit support"
                    actionButton.text = "♜ Rest in Town"
                    actionButton.applyRoundedStyle(backgroundColor = "#6b3ca8")
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, true) } }
                }

                NodeType.VILLAGE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 15 supplies  |  Heal 50%"
                    actionButton.text = "⌂ Rest in Village"
                    actionButton.applyRoundedStyle(backgroundColor = "#667733")
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
            nodeStatsText.setTextColor(Color.parseColor("#996633"))
            actionButton.visibility = View.GONE
        }
    }

    private fun showPocNodeState(node: CampaignNode) {
        val isAccessible = node.isRevealed
        val isNearby = isNodeNearby(node)
        nodeDescText.text = node.description
        nodeStatsText.setTextColor(Color.parseColor("#6688aa"))

        if (!isAccessible) {
            nodeStatsText.text = "⚠ Not discovered yet"
            actionButton.visibility = View.GONE
            return
        }

        if (isNearby) {
            nodeStatsText.text = "Within interaction range."
            actionButton.text = "Open Actions"
            actionButton.visibility = View.VISIBLE
            actionButton.applyRoundedStyle(backgroundColor = "#4A3F88")
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
        actionButton.applyRoundedStyle(backgroundColor = "#225588")
        actionButton.setOnClickListener { moveWarbandTo(node.mapX, node.mapY) }
    }

    private fun updateRangeTag(node: CampaignNode, isNearby: Boolean) {
        val (label, bg) = when {
            node.type == NodeType.ENEMY_PATROL || node.type == NodeType.ELITE_CHALLENGE || node.type == NodeType.BOSS ->
                if (isNearby) "HOSTILE · NEARBY" to "#D45353" else "HOSTILE · OUT OF RANGE" to "#803636"
            node.type == NodeType.TOWN || node.type == NodeType.VILLAGE -> if (isNearby) "SAFE SETTLEMENT · NEARBY" to "#71AF8E" else "SAFE SETTLEMENT" to "#4D7D63"
            node.type == NodeType.RESOURCE_CACHE -> if (isNearby) "OPPORTUNITY · NEARBY" to "#C7A252" else "OPPORTUNITY" to "#806B3B"
            else -> if (isNearby) "NEUTRAL · NEARBY" to "#8892B8" else "NEUTRAL" to "#586186"
        }
        nodeRangeTagText.text = label
        nodeRangeTagText.visibility = View.VISIBLE
        nodeRangeTagText.setBackgroundColor(Color.parseColor(bg))
    }

    private fun openPoiContextMenu(node: CampaignNode) {
        val options = when (node.type) {
            NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS ->
                arrayOf("Attack", "Ambush", "Bribe", "Observe", "Flee")

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
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#141C31"))
        }
        val title = TextView(this).apply {
            text = "${node.name} · Actions"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F2F5FF"))
        }
        container.addView(title)
        val subtitle = TextView(this).apply {
            text = "Choose a contextual option"
            textSize = 12f
            setTextColor(Color.parseColor("#8EA2C6"))
            setPadding(0, dp(4), 0, dp(12))
        }
        container.addView(subtitle)

        options.forEach { label ->
            val row = TextView(this).apply {
                text = label
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#E0E8FF"))
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    cornerRadius = dpF(10f)
                    setColor(Color.parseColor("#1E2945"))
                    setStroke(dp(1), Color.parseColor("#304266"))
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
                Toast.makeText(this, "You avoid trouble and keep moving.", Toast.LENGTH_SHORT).show()
            }

            action == "Trade Supplies" -> {
                campaignManager.gameState.supplies += if (node.type == NodeType.TOWN || node.type == NodeType.VILLAGE) 20 else 14
                scoutFromNode(node)
                updateHud()
                Toast.makeText(this, "Supplies stocked.", Toast.LENGTH_SHORT).show()
            }

            action == "Spend Supplies" -> {
                if (campaignManager.gameState.supplies < 8) {
                    Toast.makeText(this, "Need 8 supplies for this option.", Toast.LENGTH_SHORT).show()
                    return
                }
                campaignManager.gameState.supplies -= 8
                campaignManager.gameState.renown += 3
                scoutFromNode(node)
                updateHud()
                Toast.makeText(this, "Expedition paid off: +3 renown.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Warband recovered.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Not enough supplies to bribe (need $cost).", Toast.LENGTH_SHORT).show()
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
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#131B2F"))
        }
        content.addView(TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F4F7FF"))
        })
        content.addView(TextView(this).apply {
            text = summary
            textSize = 13f
            setTextColor(Color.parseColor("#B9C5E2"))
            setPadding(0, dp(6), 0, dp(12))
        })
        details.forEach { line ->
            content.addView(TextView(this).apply {
                text = "• $line"
                textSize = 13f
                setTextColor(Color.parseColor("#DCE5FF"))
                setPadding(0, dp(2), 0, dp(2))
            })
        }
        content.addView(Button(this).apply {
            text = "Continue"
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            applyRoundedStyle(backgroundColor = "#4A3F88")
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })
        dialog.setContentView(content)
        dialog.show()
    }

    private fun moveWarbandTo(normX: Float, normY: Float) {
        val startX = campaignManager.gameState.playerMapX
        val startY = campaignManager.gameState.playerMapY
        val totalDistanceNorm = hypot(normX - startX, normY - startY)
        if (totalDistanceNorm < 0.00002f) return
        val enemySnapshot = campaignManager.createEnemyMovementSnapshot()
        var latestProgressNorm = 0f
        var latestPlayerX = startX
        var latestPlayerY = startY
        mapView.inputEnabled = false
        statusText.text = "▶ Marching…"
        statusText.visibility = View.VISIBLE
        stopMovementButton.visibility = View.VISIBLE
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
            if (cancelled) {
                Toast.makeText(this, "Movement cancelled.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Intel gained: hideout location marked on your map.", Toast.LENGTH_LONG).show()
        }
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(
            campaignManager.gameState.playerMapX,
            campaignManager.gameState.playerMapY
        )
        mapView.recenterOnPlayer()
        mapView.invalidate()
        updateHud()
        Toast.makeText(this, "Explored ${node.name}.", Toast.LENGTH_SHORT).show()
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
        if (selectedNode?.id == filteredNearbyNode.id && infoPanel.visibility == View.VISIBLE) return
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
                Toast.makeText(this, "New POI discovered: $names$more", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Not enough supplies to rest! (Need 20)", Toast.LENGTH_SHORT).show()
            infoPanel.visibility = View.GONE
            return
        }
        campaignManager.moveToNode(node.id)
        campaignManager.resolveRecoveryCamp(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        Toast.makeText(this, "♥ Warband healed!", Toast.LENGTH_SHORT).show()
        infoPanel.visibility = View.GONE
        mapView.selectedNodeId = null
    }

    private fun collectResources(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        campaignManager.resolveResourceCache(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        Toast.makeText(this, "◈ +${node.suppliesReward} supplies collected!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Not enough supplies! (Need $cost)", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, if (town) "Warband fully restored!" else "Warband partially restored!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "☠ Enemy party intercepted your warband!", Toast.LENGTH_LONG).show()
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
