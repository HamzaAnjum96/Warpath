package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.warpath.engine.CampaignManager
import com.warpath.model.CampaignNode
import com.warpath.model.NodeType
import kotlin.random.Random

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
    private lateinit var travelHintText: TextView
    private lateinit var joystickView: JoystickView
    private lateinit var recenterButton: Button

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

        mapView = CampaignMapView(this).apply {
            nodes = campaignManager.campaignMap
            currentNodeId = campaignManager.gameState.currentNodeId
            enemyParties = campaignManager.gameState.enemyParties
            showPaths = !phaseOnePocMode
            setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
            onMapTapped = { normX, normY ->
                moveWarbandTo(normX, normY)
            }
            onFocusChanged = { focused ->
                recenterButton.visibility = if (focused) View.GONE else View.VISIBLE
                if (!focused) {
                    Toast.makeText(context, "Map unfocused. Drag to scout. Tap Recenter to lock on warband.", Toast.LENGTH_SHORT).show()
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
            textSize = 15f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(24, 10, 24, 10)
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
            text = "◎ Recenter"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setTextColor(Color.parseColor("#F5EED1"))
            setBackgroundColor(Color.parseColor("#4A3F88"))
            setPadding(20, 14, 20, 14)
            stateListAnimator = null
            visibility = View.GONE
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
                topMargin = 110
                rightMargin = 22
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

        travelHintText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#d7def7"))
            setBackgroundColor(Color.parseColor("#aa131d32"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 10, 16, 10)
            text = "Fog-of-war scouting active — move to discover POIs."
        }
        root.addView(
            travelHintText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.START
            ).apply {
                leftMargin = 28
                bottomMargin = 440
            }
        )

        joystickView = JoystickView(this).apply {
            onMove = { x, y ->
                campaignManager.movePlayerBy(x * 0.012f, y * 0.012f)
                mapView.movePlayerBy(x * 0.012f, y * 0.012f)
                mapView.setPlayerLookDirection(x, y)
                checkFogDiscovery()
                showNearbyPoiIfAny()
            }
            onRelease = { _, _ -> }
        }
        root.addView(
            joystickView,
            FrameLayout.LayoutParams(220, 220, Gravity.BOTTOM or Gravity.START).apply {
                leftMargin = 28
                bottomMargin = 220
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
            setPadding(28, 18, 28, 18)
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "SARHAD"
            textSize = 17f
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
            text = "⚔ Warband"
            textSize = 12f
            setTextColor(Color.parseColor("#aaaacc"))
            setBackgroundColor(Color.parseColor("#33334a"))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 8, 16, 8)
            stateListAnimator = null
            setOnClickListener { startActivity(Intent(this@CampaignActivity, WarbandActivity::class.java)) }
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

    private fun buildInfoPanel(): View {
        val container = FrameLayout(this)

        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#f0090914"))
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
            setPadding(36, 20, 36, 20)
        }

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
        headerRow.addView(
            nodeTypeChip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 14 }
        )

        nodeNameText = TextView(this).apply {
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        headerRow.addView(nodeNameText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val dismissX = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.parseColor("#666688"))
            setPadding(8, 0, 4, 0)
            setOnClickListener { infoPanel.visibility = View.GONE }
        }
        headerRow.addView(dismissX)

        content.addView(headerRow)

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
            textSize = 13f
            setTextColor(Color.parseColor("#9999bb"))
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
            setTextColor(Color.parseColor("#6688aa"))
        }
        content.addView(
            nodeStatsText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14 }
        )

        actionButton = Button(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 18, 24, 18)
            stateListAnimator = null
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

    override fun onResume() {
        super.onResume()
        mapView.nodes = campaignManager.campaignMap
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.enemyParties = campaignManager.gameState.enemyParties
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

    private fun onNodeSelected(node: CampaignNode) {
        selectedNode = node
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
                    actionButton.setBackgroundColor(Color.parseColor("#aa2222"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { engageBattle(node) } }
                }

                NodeType.RECOVERY_CAMP -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Cost: 20 supplies  |  Heal 40% HP"
                    actionButton.text = "♥ Rest & Heal"
                    actionButton.setBackgroundColor(Color.parseColor("#225588"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { restAtCamp(node) } }
                }

                NodeType.RESOURCE_CACHE -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Reward: ⚔ +${node.suppliesReward}  ★ +${node.renownReward}"
                    actionButton.text = "◈ Collect Supplies"
                    actionButton.setBackgroundColor(Color.parseColor("#226633"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { animateAndThen(node) { collectResources(node) } }
                }

                NodeType.FACTION_OUTPOST -> {
                    nodeDescText.text = node.description
                    nodeStatsText.text = "Recruit troops and resupply"
                    actionButton.text = "⚑ Visit Outpost"
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
            nodeStatsText.text = "Nearby POI — open interaction menu"
            actionButton.text = "Open Actions"
            actionButton.visibility = View.VISIBLE
            actionButton.setBackgroundColor(Color.parseColor("#225588"))
            actionButton.setOnClickListener {
                suppressAutoPoiSelection = true
                openPoiContextMenu(node)
                suppressAutoPoiSelection = false
                showNearbyPoiIfAny()
            }
            return
        }

        nodeStatsText.text = "Move closer to interact"
        actionButton.visibility = View.GONE
    }

    private fun openPoiContextMenu(node: CampaignNode) {
        val options = when (node.type) {
            NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS ->
                arrayOf("Fight", "Run", "Bribe")

            NodeType.TOWN, NodeType.VILLAGE ->
                arrayOf("Buy Supplies (+25)", "Sell Supplies (-15 for renown)", "Recruit", "Rest")

            NodeType.FACTION_OUTPOST ->
                arrayOf("Recruit", "Buy Supplies (+20)", "Sell Supplies (-10 for renown)")

            NodeType.RESOURCE_CACHE ->
                arrayOf("Scavenge Cache", "Leave")

            NodeType.RECOVERY_CAMP ->
                arrayOf("Rest & Heal", "Leave")

            NodeType.START ->
                arrayOf("Reorganize Warband", "Scout Routes")
        }

        AlertDialog.Builder(this)
            .setTitle("${node.name} — Actions")
            .setItems(options) { _, which ->
                handlePoiAction(node, options[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handlePoiAction(node: CampaignNode, action: String) {
        when {
            action.startsWith("Fight") -> {
                scoutFromNode(node, revealHideoutIntel = node.type == NodeType.ELITE_CHALLENGE)
                Toast.makeText(this, "Skirmish won at ${node.name}.", Toast.LENGTH_SHORT).show()
            }

            action.startsWith("Run") -> {
                handleRunAttempt(node)
            }

            action.startsWith("Bribe") -> {
                handleBribe(node)
            }

            action == "Leave" -> {
                Toast.makeText(this, "You avoid trouble and keep moving.", Toast.LENGTH_SHORT).show()
            }

            action.startsWith("Buy Supplies") -> {
                campaignManager.gameState.supplies += if (node.type == NodeType.TOWN || node.type == NodeType.VILLAGE) 25 else 20
                scoutFromNode(node)
                updateHud()
                Toast.makeText(this, "Supplies stocked.", Toast.LENGTH_SHORT).show()
            }

            action.startsWith("Sell Supplies") -> {
                val sellAmount = if (node.type == NodeType.TOWN || node.type == NodeType.VILLAGE) 15 else 10
                if (campaignManager.gameState.supplies < sellAmount) {
                    Toast.makeText(this, "Not enough supplies to sell.", Toast.LENGTH_SHORT).show()
                    return
                }
                campaignManager.gameState.supplies -= sellAmount
                campaignManager.gameState.renown += 5
                scoutFromNode(node)
                updateHud()
                Toast.makeText(this, "Trade completed: +5 renown.", Toast.LENGTH_SHORT).show()
            }

            action == "Recruit" || action == "Reorganize Warband" -> {
                scoutFromNode(node)
                startActivity(Intent(this, WarbandActivity::class.java).apply {
                    putExtra("can_recruit", true)
                })
            }

            action == "Rest" || action == "Rest & Heal" -> {
                campaignManager.gameState.healWarband(if (node.type == NodeType.TOWN) 1.0f else 0.5f)
                scoutFromNode(node)
                Toast.makeText(this, "Warband recovered.", Toast.LENGTH_SHORT).show()
            }

            action == "Scavenge Cache" || action == "Scout Routes" -> {
                scoutFromNode(node)
            }
        }
    }

    private fun handleRunAttempt(node: CampaignNode) {
        val roll = Random.nextFloat()
        when {
            roll < 0.55f -> {
                Toast.makeText(this, "Clean escape. Your warband slips away.", Toast.LENGTH_SHORT).show()
                return
            }
            roll < 0.85f -> {
                val loss = randomSupplyLoss(6, 15)
                campaignManager.gameState.supplies = (campaignManager.gameState.supplies - loss).coerceAtLeast(0)
                updateHud()
                Toast.makeText(this, "Messy retreat. -$loss supplies.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Escape failed! Forced skirmish at ${node.name}.", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Paid off ${node.name}: -$cost supplies, -2 renown.", Toast.LENGTH_SHORT).show()
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

    private fun moveWarbandTo(normX: Float, normY: Float) {
        mapView.inputEnabled = false
        statusText.text = "▶ Marching…"
        statusText.visibility = View.VISIBLE
        mapView.animatePlayerTo(normX, normY) {
            campaignManager.setPlayerPosition(normX, normY)
            checkFogDiscovery()
            mapView.inputEnabled = true
            statusText.visibility = View.GONE
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
            "Locked on warband — joystick moves, discover POIs by scouting."
        } else {
            "Scouting mode — drag to survey. Tap Recenter to follow."
        }
        if (nearbyNode == null) {
            selectedNode = null
            infoPanel.visibility = View.GONE
            return
        }
        travelHintText.text = "Nearby: ${nearbyNode.name} · Open Actions for options."
        if (selectedNode?.id == nearbyNode.id && infoPanel.visibility == View.VISIBLE) return
        onNodeSelected(nearbyNode)
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

        mapView.animatePlayerTo(targetNode) {
            statusText.visibility = View.GONE
            mapView.inputEnabled = true
            actionButton.isEnabled = true
            actionButton.alpha = 1f
            if (campaignManager.stepEnemyParties()) {
                mapView.enemyParties = campaignManager.gameState.enemyParties
                forceEnemyEngagement()
                return@animatePlayerTo
            }
            mapView.enemyParties = campaignManager.gameState.enemyParties
            action()
        }
    }

    private fun engageBattle(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.setPlayerPosition(campaignManager.gameState.playerMapX, campaignManager.gameState.playerMapY)
        infoPanel.visibility = View.GONE
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
        infoPanel.visibility = View.GONE
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
    }

    private fun forceEnemyEngagement() {
        val enemyNodeIds = campaignManager.gameState.enemyParties.map { it.nodeId }.toSet()
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
