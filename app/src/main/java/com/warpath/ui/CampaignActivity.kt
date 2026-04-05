package com.warpath.ui

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.warpath.engine.CampaignManager
import com.warpath.model.CampaignNode
import com.warpath.model.NodeType

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
    private lateinit var actionButton: Button
    private lateinit var panelAccentBar: View
    private lateinit var statusText: TextView
    private lateinit var joystickView: JoystickView

    private val phaseOnePocMode = true
    private val poiInteractionDistance = 0.07f
    private var selectedNode: CampaignNode? = null
    private var suppressAutoPoiSelection = false

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

        // ── Map view ──────────────────────────────────────────────────────────
        mapView = CampaignMapView(this).apply {
            nodes = campaignManager.campaignMap
            currentNodeId = campaignManager.gameState.currentNodeId
            enemyParties = campaignManager.gameState.enemyParties
            showPaths = !phaseOnePocMode
            setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        }
        root.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // ── Top HUD ───────────────────────────────────────────────────────────
        root.addView(buildTopHud(), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ))

        // ── Status bar (shows "Traveling...", centered over map) ──────────────
        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(24, 10, 24, 10)
            setBackgroundColor(Color.parseColor("#cc0a0a18"))
            visibility = View.GONE
        }
        val statusParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        )
        root.addView(statusText, statusParams)

        // ── Bottom info panel ─────────────────────────────────────────────────
        root.addView(buildInfoPanel(), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ))

        joystickView = JoystickView(this).apply {
            onMove = { x, y ->
                campaignManager.movePlayerBy(x * 0.012f, y * 0.012f)
                mapView.movePlayerBy(x * 0.012f, y * 0.012f)
                showNearbyPoiIfAny()
            }
            onRelease = { _, _ -> }
        }
        root.addView(joystickView, FrameLayout.LayoutParams(220, 220, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = 28
            bottomMargin = 220
        })

        setContentView(root)
        updateHud()
        showNearbyPoiIfAny()
    }

    // ── HUD construction ──────────────────────────────────────────────────────

    private fun buildTopHud(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#dd0a0a18"))
            setPadding(28, 18, 28, 18)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Left: title
        val title = TextView(this).apply {
            text = "SARHAD"
            textSize = 17f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 1f, 1f, Color.parseColor("#80000000"))
        }
        bar.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Resources row
        suppliesText = hudStatText()
        renownText   = hudStatText()
        warbandText  = hudStatText()

        bar.addView(suppliesText)
        bar.addView(hudSpacer())
        bar.addView(renownText)
        bar.addView(hudSpacer())
        bar.addView(warbandText)
        bar.addView(hudSpacer())

        // Warband button
        val warbandBtn = Button(this).apply {
            text = "⚔ Warband"
            textSize = 12f
            setTextColor(Color.parseColor("#aaaacc"))
            setBackgroundColor(Color.parseColor("#33334a"))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 8, 16, 8)
            stateListAnimator = null
            setOnClickListener {
                startActivity(Intent(this@CampaignActivity, WarbandActivity::class.java))
            }
        }
        bar.addView(warbandBtn)

        return bar
    }

    private fun hudStatText() = TextView(this).apply {
        textSize = 13f
        setTextColor(Color.parseColor("#aaaacc"))
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun hudSpacer(): View = View(this).also {
        it.layoutParams = LinearLayout.LayoutParams(20, 1)
    }

    // ── Info panel construction ───────────────────────────────────────────────

    private fun buildInfoPanel(): View {
        val container = FrameLayout(this)

        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#f0090914"))
            visibility = View.GONE
        }

        // Top accent bar (coloured by node type)
        panelAccentBar = View(this)
        infoPanel.addView(panelAccentBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 5
        ))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }

        // Node type chip + name row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nodeTypeChip = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(12, 5, 12, 5)
        }
        headerRow.addView(nodeTypeChip, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = 14 })

        nodeNameText = TextView(this).apply {
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        headerRow.addView(nodeNameText, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))

        // Dismiss X button
        val dismissX = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.parseColor("#666688"))
            setPadding(8, 0, 4, 0)
            setOnClickListener { infoPanel.visibility = View.GONE }
        }
        headerRow.addView(dismissX)

        content.addView(headerRow)

        // Divider
        content.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#222244"))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1
        ).apply { topMargin = 10; bottomMargin = 10 })

        // Description
        nodeDescText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#9999bb"))
            setLineSpacing(2f, 1f)
        }
        content.addView(nodeDescText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8 })

        // Stats row (enemies / rewards)
        nodeStatsText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#6688aa"))
        }
        content.addView(nodeStatsText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 14 })

        // Action button
        actionButton = Button(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 18, 24, 18)
            stateListAnimator = null
        }
        content.addView(actionButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        infoPanel.addView(content)
        container.addView(infoPanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        return container
    }

    // ── Resume & HUD update ───────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        mapView.nodes = campaignManager.campaignMap
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.enemyParties = campaignManager.gameState.enemyParties
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        mapView.inputEnabled = true
        mapView.showPaths = !phaseOnePocMode
        mapView.invalidate()
        updateHud()
        showNearbyPoiIfAny()

        if (campaignManager.isRunOver()) {
            showRunOverDialog()
        }
    }

    private fun updateHud() {
        val gs = campaignManager.gameState
        suppliesText.text = "⚔ ${gs.supplies}"
        renownText.text   = "★ ${gs.renown}"
        val wSize = gs.warband.size
        val wMax  = gs.maxWarbandSlots
        warbandText.text  = "⚔ $wSize/$wMax"
    }

    // ── Node selection ────────────────────────────────────────────────────────

    private fun onNodeSelected(node: CampaignNode) {
        selectedNode = node
        infoPanel.visibility = View.VISIBLE

        // Accent bar color
        panelAccentBar.setBackgroundColor(node.type.color.toInt())

        // Type chip
        nodeTypeChip.text = node.type.displayName.uppercase()
        nodeTypeChip.setBackgroundColor(
            Color.argb(180, Color.red(node.type.color.toInt()),
                Color.green(node.type.color.toInt()),
                Color.blue(node.type.color.toInt()))
        )

        nodeNameText.text = node.name
        nodeNameText.setTextColor(node.type.color.toInt())

        val isAccessible = node.isRevealed
        val isCurrent    = node.id == campaignManager.gameState.currentNodeId

        // Reset
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
                    nodeDescText.text  = node.description
                    nodeStatsText.text = "Enemies: $enemyCount  |  ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text  = if (node.type == NodeType.BOSS) "⚔ Storm the Stronghold!" else "⚔ Attack!"
                    actionButton.setBackgroundColor(Color.parseColor("#aa2222"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { engageBattle(node) } }
                }
                NodeType.RECOVERY_CAMP -> {
                    nodeDescText.text  = node.description
                    nodeStatsText.text = "Cost: 20 supplies  |  Heal 40% HP"
                    actionButton.text  = "♥ Rest & Heal"
                    actionButton.setBackgroundColor(Color.parseColor("#225588"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtCamp(node) } }
                }
                NodeType.RESOURCE_CACHE -> {
                    nodeDescText.text  = node.description
                    nodeStatsText.text = "Reward: ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text  = "◈ Collect Supplies"
                    actionButton.setBackgroundColor(Color.parseColor("#226633"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { collectResources(node) } }
                }
                NodeType.FACTION_OUTPOST -> {
                    nodeDescText.text  = node.description
                    nodeStatsText.text = "Recruit troops and resupply"
                    actionButton.text  = "⚑ Visit Outpost"
                    actionButton.setBackgroundColor(Color.parseColor("#885522"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { visitOutpost(node) } }
                }
                NodeType.TOWN -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 35 supplies  |  Full heal + recruit support"
                    actionButton.text = "♜ Rest in Town"
                    actionButton.setBackgroundColor(Color.parseColor("#6b3ca8"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, true) } }
                }
                NodeType.VILLAGE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 15 supplies  |  Heal 50%"
                    actionButton.text = "⌂ Rest in Village"
                    actionButton.setBackgroundColor(Color.parseColor("#667733"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtSettlement(node, false) } }
                }
                NodeType.START -> {
                    nodeDescText.text  = node.description
                    nodeStatsText.text = "Your staging ground"
                    actionButton.visibility = View.GONE
                }
            }
        } else {
            nodeDescText.text  = node.description
            nodeStatsText.text = "⚠ Not accessible — clear a connected node first"
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
            nodeStatsText.text = "⚠ Not accessible yet"
            actionButton.visibility = View.GONE
            return
        }

        if (isNearby) {
            nodeStatsText.text = "Nearby POI"
            actionButton.text = "Interact"
            actionButton.visibility = View.VISIBLE
            actionButton.setBackgroundColor(Color.parseColor("#225588"))
            actionButton.setOnClickListener {
                suppressAutoPoiSelection = true
                scoutFromNode(node)
                suppressAutoPoiSelection = false
                showNearbyPoiIfAny()
            }
            return
        }

        nodeStatsText.text = "Move closer to interact"
        actionButton.visibility = View.GONE
    }

    private fun scoutFromNode(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        node.isCleared = true
        node.connections.forEach { connId ->
            campaignManager.campaignMap.find { it.id == connId }?.isRevealed = true
        }
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(
            campaignManager.gameState.playerMapX,
            campaignManager.gameState.playerMapY
        )
        mapView.invalidate()
        updateHud()
        Toast.makeText(this, "Explored ${node.name}. Nearby routes revealed.", Toast.LENGTH_SHORT).show()
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
        if (nearbyNode == null) {
            selectedNode = null
            infoPanel.visibility = View.GONE
            return
        }
        if (selectedNode?.id == nearbyNode.id && infoPanel.visibility == View.VISIBLE) {
            return
        }
        onNodeSelected(nearbyNode)
    }

    // ── Animation wrapper ─────────────────────────────────────────────────────

    /**
     * Animates the player marker to [targetNode], then runs [action].
     * If the player is already at that node the animation is skipped.
     */
    private fun animateAndThen(targetNode: CampaignNode, action: () -> Unit) {
        val current = campaignManager.getCurrentNode()
        if (current == null || current.id == targetNode.id) {
            action()
            return
        }

        // Disable input and show traveling state
        mapView.inputEnabled = false
        actionButton.text    = "  Traveling…"
        actionButton.isEnabled = false
        actionButton.alpha   = 0.5f
        statusText.text      = "▶ Marching to ${targetNode.name}…"
        statusText.visibility = View.VISIBLE

        mapView.animatePlayerTo(targetNode) {
            statusText.visibility = View.GONE
            mapView.inputEnabled  = true
            actionButton.isEnabled = true
            actionButton.alpha     = 1f
            if (campaignManager.stepEnemyParties()) {
                mapView.enemyParties = campaignManager.gameState.enemyParties
                forceEnemyEngagement()
                return@animatePlayerTo
            }
            mapView.enemyParties = campaignManager.gameState.enemyParties
            action()
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun engageBattle(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        infoPanel.visibility = View.GONE
        val intent = Intent(this, BattleActivity::class.java)
        intent.putExtra("node_id", node.id)
        startActivity(intent)
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
    }

    private fun collectResources(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        campaignManager.resolveResourceCache(node)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        updateHud()
        Toast.makeText(this, "◈ +${node.suppliesReward} supplies collected!", Toast.LENGTH_SHORT).show()
        infoPanel.visibility = View.GONE
    }

    private fun visitOutpost(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        node.isCleared = true
        node.connections.forEach { connId ->
            campaignManager.campaignMap.find { it.id == connId }?.isRevealed = true
        }
        infoPanel.visibility = View.GONE
        val intent = Intent(this, WarbandActivity::class.java)
        intent.putExtra("can_recruit", true)
        startActivity(intent)
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
        node.connections.forEach { connId ->
            campaignManager.campaignMap.find { it.id == connId }?.isRevealed = true
        }
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        updateHud()
        Toast.makeText(this, if (town) "Warband fully restored!" else "Warband partially restored!", Toast.LENGTH_SHORT).show()
        infoPanel.visibility = View.GONE
    }

    private fun forceEnemyEngagement() {
        val enemyNodeIds = campaignManager.gameState.enemyParties.map { it.nodeId }.toSet()
        val current = campaignManager.getCurrentNode()
        if (current != null && enemyNodeIds.contains(current.id)) {
            Toast.makeText(this, "☠ Enemy party intercepted your warband!", Toast.LENGTH_LONG).show()
            engageBattle(current)
        }
    }

    // ── End-of-run dialog ─────────────────────────────────────────────────────

    private fun showRunOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Campaign Over")
            .setMessage(campaignManager.getRunSummary())
            .setPositiveButton("New Campaign") { _, _ ->
                campaignManager.startNewCampaign()
                mapView.nodes         = campaignManager.campaignMap
                mapView.currentNodeId = campaignManager.gameState.currentNodeId
                mapView.invalidate()
                infoPanel.visibility  = View.GONE
                updateHud()
            }
            .setNegativeButton("Main Menu") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
