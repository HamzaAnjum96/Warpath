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

class CampaignActivity : AppCompatActivity() {

    companion object {
        var campaignManager: CampaignManager = CampaignManager()
    }

    private lateinit var mapView: CampaignMapView
    private lateinit var infoPanel: LinearLayout
    private lateinit var suppliesText: TextView
    private lateinit var renownText: TextView
    private lateinit var nodeNameText: TextView
    private lateinit var nodeDescText: TextView
    private lateinit var actionButton: Button

    private var selectedNode: CampaignNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        if (intent.getBooleanExtra("new_game", false)) {
            campaignManager.startNewCampaign()
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0d0d1a"))
        }

        // Map view fills the screen
        mapView = CampaignMapView(this).apply {
            nodes = campaignManager.campaignMap
            currentNodeId = campaignManager.gameState.currentNodeId
            onNodeTapped = { node -> onNodeSelected(node) }
        }
        root.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Top HUD bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#cc1a1a2e"))
            setPadding(30, 16, 30, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "Campaign Map"
            textSize = 18f
            setTextColor(Color.parseColor("#e6c84c"))
            typeface = Typeface.DEFAULT_BOLD
        }
        topBar.addView(titleText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        suppliesText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#33aa33"))
        }
        topBar.addView(suppliesText)

        val spacer = View(this)
        topBar.addView(spacer, LinearLayout.LayoutParams(30, 1))

        renownText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#ccaa33"))
        }
        topBar.addView(renownText)

        root.addView(topBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ))

        // Bottom info panel (hidden initially)
        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#ee1a1a2e"))
            setPadding(40, 30, 40, 30)
            visibility = View.GONE
        }

        nodeNameText = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        infoPanel.addView(nodeNameText)

        nodeDescText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#aaaacc"))
        }
        infoPanel.addView(nodeDescText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8; bottomMargin = 16 })

        actionButton = Button(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#cc3333"))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setPadding(30, 16, 30, 16)
        }
        infoPanel.addView(actionButton)

        val dismissBtn = Button(this).apply {
            text = "Close"
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            setBackgroundColor(Color.TRANSPARENT)
            isAllCaps = false
            setOnClickListener { infoPanel.visibility = View.GONE }
        }
        infoPanel.addView(dismissBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.END; topMargin = 8 })

        root.addView(infoPanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ))

        setContentView(root)
        updateHud()
    }

    override fun onResume() {
        super.onResume()
        updateHud()
        mapView.nodes = campaignManager.campaignMap
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()

        if (campaignManager.isRunOver()) {
            showRunOverDialog()
        }
    }

    private fun updateHud() {
        suppliesText.text = "Supplies: ${campaignManager.gameState.supplies}"
        renownText.text = "Renown: ${campaignManager.gameState.renown}"
    }

    private fun onNodeSelected(node: CampaignNode) {
        selectedNode = node
        infoPanel.visibility = View.VISIBLE
        nodeNameText.text = node.name
        nodeNameText.setTextColor(node.type.color.toInt())

        val accessible = campaignManager.getAccessibleNodes()
        val isAccessible = accessible.any { it.id == node.id }
        val isCurrent = node.id == campaignManager.gameState.currentNodeId

        if (node.isCleared) {
            nodeDescText.text = "${node.description}\n[CLEARED]"
            actionButton.visibility = View.GONE
        } else if (isCurrent || isAccessible) {
            when (node.type) {
                NodeType.ENEMY_PATROL, NodeType.ELITE_CHALLENGE, NodeType.BOSS -> {
                    val enemyCount = node.enemySquads.sumOf { it.count }
                    nodeDescText.text = "${node.description}\nEnemy forces: $enemyCount troops" +
                        "\nReward: ${node.suppliesReward} supplies, ${node.renownReward} renown"
                    actionButton.text = "Attack!"
                    actionButton.setBackgroundColor(Color.parseColor("#cc3333"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { engageBattle(node) }
                }
                NodeType.RECOVERY_CAMP -> {
                    nodeDescText.text = "${node.description}\nCost: 20 supplies to heal warband"
                    actionButton.text = "Rest & Heal"
                    actionButton.setBackgroundColor(Color.parseColor("#3388cc"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { restAtCamp(node) }
                }
                NodeType.RESOURCE_CACHE -> {
                    nodeDescText.text = "${node.description}\nReward: ${node.suppliesReward} supplies, ${node.renownReward} renown"
                    actionButton.text = "Collect"
                    actionButton.setBackgroundColor(Color.parseColor("#33aa33"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { collectResources(node) }
                }
                NodeType.FACTION_OUTPOST -> {
                    nodeDescText.text = "${node.description}\nRecruit troops and resupply."
                    actionButton.text = "Visit Outpost"
                    actionButton.setBackgroundColor(Color.parseColor("#cc8833"))
                    actionButton.visibility = View.VISIBLE
                    actionButton.setOnClickListener { visitOutpost(node) }
                }
                else -> {
                    nodeDescText.text = node.description
                    actionButton.visibility = View.GONE
                }
            }
        } else {
            nodeDescText.text = "${node.description}\n[Not accessible - clear a connected node first]"
            actionButton.visibility = View.GONE
        }
    }

    private fun engageBattle(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        val intent = Intent(this, BattleActivity::class.java)
        intent.putExtra("node_id", node.id)
        startActivity(intent)
    }

    private fun restAtCamp(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        campaignManager.resolveRecoveryCamp(node)
        updateHud()
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        Toast.makeText(this, "Warband healed!", Toast.LENGTH_SHORT).show()
        infoPanel.visibility = View.GONE
    }

    private fun collectResources(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        campaignManager.resolveResourceCache(node)
        updateHud()
        mapView.currentNodeId = campaignManager.gameState.currentNodeId
        mapView.invalidate()
        Toast.makeText(this, "+${node.suppliesReward} supplies!", Toast.LENGTH_SHORT).show()
        infoPanel.visibility = View.GONE
    }

    private fun visitOutpost(node: CampaignNode) {
        campaignManager.moveToNode(node.id)
        node.isCleared = true
        node.connections.forEach { connId ->
            campaignManager.campaignMap.find { it.id == connId }?.isRevealed = true
        }
        val intent = Intent(this, WarbandActivity::class.java)
        intent.putExtra("can_recruit", true)
        startActivity(intent)
    }

    private fun showRunOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Campaign Over")
            .setMessage(campaignManager.getRunSummary())
            .setPositiveButton("New Campaign") { _, _ ->
                campaignManager.startNewCampaign()
                updateHud()
                mapView.nodes = campaignManager.campaignMap
                mapView.currentNodeId = campaignManager.gameState.currentNodeId
                mapView.invalidate()
            }
            .setNegativeButton("Main Menu") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
