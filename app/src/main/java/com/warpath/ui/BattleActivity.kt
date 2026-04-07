package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.warpath.engine.BattleEngine
import com.warpath.engine.BattleState
import com.warpath.model.BattleCommand
import com.warpath.model.CampaignNode

class BattleActivity : AppCompatActivity() {

    private lateinit var battleView: BattleView
    private lateinit var battleEngine: BattleEngine
    private lateinit var battleState: BattleState
    private lateinit var logText: TextView
    private lateinit var commandBar: LinearLayout
    private lateinit var startBattleButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val tickInterval = 50L // 20 fps
    private var node: CampaignNode? = null
    private val startingPlayerCounts = mutableMapOf<String, Int>()
    private var startingEnemyCount: Int = 0

    private val commandButtons = mutableMapOf<BattleCommand, Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val nodeId = intent.getStringExtra("node_id") ?: return
        node = CampaignActivity.campaignManager.campaignMap.find { it.id == nodeId }
        val currentNode = node ?: return

        // Set up battle
        battleEngine = BattleEngine()
        battleState = battleEngine.createBattle(
            CampaignActivity.campaignManager.gameState.warband,
            currentNode.enemySquads
        )
        startingPlayerCounts.clear()
        CampaignActivity.campaignManager.gameState.warband.forEach { squad ->
            startingPlayerCounts[squad.id] = squad.count
        }
        startingEnemyCount = battleState.enemySquads.sumOf { it.count }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.BASE_BG))
        }

        // Battle view (takes most of the screen)
        battleView = BattleView(this).apply {
            battleState = this@BattleActivity.battleState
            onEnemyTapped = { enemy ->
                battleEngine.issueCommand(BattleCommand.FOCUS_TARGET, this@BattleActivity.battleState, enemy.id)
            }
        }
        // Main container
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Title bar
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(UiTheme.SURFACE))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleText = TextView(this).apply {
            text = "Battle: ${currentNode.name}"
            textSize = 16f
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = Typeface.DEFAULT_BOLD
        }
        titleBar.addView(titleText)
        mainLayout.addView(titleBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Battle view gets weight
        val battleContainer = FrameLayout(this)
        battleContainer.addView(battleView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        startBattleButton = Button(this).apply {
            text = "Start Battle"
            textSize = 14f
            setPadding(dp(18), dp(10), dp(18), dp(10))
            applyUiButtonStyle(UiTheme.GOLD, 12f)
            setOnClickListener {
                visibility = View.GONE
                if (!isRunning) {
                    isRunning = true
                    handler.post(battleTick)
                }
            }
        }
        battleContainer.addView(startBattleButton, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
        mainLayout.addView(battleContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Battle log
        val logScroll = HorizontalScrollView(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.SURFACE_ALT))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        logText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            maxLines = 2
            text = "Ready. Tap Start Battle."
        }
        logScroll.addView(logText)
        mainLayout.addView(logScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Command bar
        commandBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(UiTheme.SURFACE))
            setPadding(dp(8), dp(12), dp(8), dp(12))
            gravity = Gravity.CENTER
        }

        for (cmd in BattleCommand.values()) {
            val btn = Button(this).apply {
                text = cmd.displayName
                textSize = 12f
                setPadding(dp(12), dp(8), dp(12), dp(8))
                applyUiButtonStyle(getCommandColor(cmd), 10f)
                setOnClickListener { onCommand(cmd) }
            }
            commandButtons[cmd] = btn
            commandBar.addView(btn, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = 4; marginEnd = 4 })
        }
        mainLayout.addView(commandBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        root.addView(mainLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)

        // Wait for player to start battle loop
    }

    private fun getCommandColor(cmd: BattleCommand): String {
        return when (cmd) {
            BattleCommand.FOCUS_TARGET -> UiTheme.GOLD
            BattleCommand.PUSH -> UiTheme.DANGER
            BattleCommand.HOLD -> UiTheme.PRIMARY
            BattleCommand.RALLY -> UiTheme.SUCCESS
            BattleCommand.RETREAT -> UiTheme.SURFACE_ALT
        }
    }

    private fun onCommand(cmd: BattleCommand) {
        val target = if (cmd == BattleCommand.FOCUS_TARGET) battleView.selectedEnemyId else null
        battleEngine.issueCommand(cmd, battleState, target)
    }

    private val battleTick = object : Runnable {
        override fun run() {
            if (!isRunning) return

            battleEngine.tick(battleState, tickInterval / 1000f)
            battleView.battleState = battleState
            battleView.invalidate()

            // Update log
            if (battleState.battleLog.isNotEmpty()) {
                logText.text = battleState.battleLog.takeLast(2).joinToString(" | ")
            }

            // Update command cooldowns
            for ((cmd, btn) in commandButtons) {
                val remaining = battleEngine.getCommandCooldownRemaining(cmd)
                if (remaining > 0) {
                    btn.alpha = 0.5f
                    btn.text = "${cmd.displayName}\n${remaining / 1000}s"
                } else {
                    btn.alpha = 1.0f
                    btn.text = cmd.displayName
                }
            }

            if (battleState.isOver) {
                isRunning = false
                onBattleEnd()
                return
            }

            handler.postDelayed(this, tickInterval)
        }
    }

    private fun onBattleEnd() {
        val currentNode = node ?: return
        CampaignActivity.campaignManager.resolveNodeRewards(currentNode, battleState.playerWon)

        // Apply surviving squad state back to warband
        if (battleState.playerWon) {
            val warband = CampaignActivity.campaignManager.gameState.warband
            for (surviving in battleState.playerSquads) {
                val original = warband.find { it.id == surviving.id }
                if (original != null) {
                    original.count = surviving.count
                    original.currentHpPercent = surviving.currentHpPercent
                    original.morale = surviving.morale
                }
            }
        }

        val intent = Intent(this, BattleResultActivity::class.java)
        intent.putExtra("player_won", battleState.playerWon)
        intent.putExtra("node_name", currentNode.name)
        intent.putExtra("supplies_reward", if (battleState.playerWon) currentNode.suppliesReward else 0)
        intent.putExtra("renown_reward", if (battleState.playerWon) currentNode.renownReward else 0)
        intent.putExtra("enemies_killed",
            startingEnemyCount - battleState.enemySquads.sumOf { it.count })
        intent.putExtra("enemies_started", startingEnemyCount)
        intent.putExtra("squads_lost",
            CampaignActivity.campaignManager.gameState.warband.size -
            battleState.playerSquads.count { it.isAlive })
        val casualtyRows = ArrayList<String>()
        for (squad in battleState.playerSquads) {
            val started = startingPlayerCounts[squad.id] ?: squad.count
            val lost = (started - squad.count).coerceAtLeast(0)
            casualtyRows.add("${squad.unitType.name}: -$lost")
        }
        intent.putStringArrayListExtra("casualties_rows", casualtyRows)
        val avgMorale = if (battleState.playerSquads.isEmpty()) 0 else battleState.playerSquads.map { it.morale }.average().toInt()
        intent.putExtra("morale_end", avgMorale)
        intent.putExtra("supplies_lost", if (battleState.playerWon) 0 else (CampaignActivity.campaignManager.gameState.supplies * 0.3f).toInt())
        intent.putExtra("warband_status", "${battleState.playerSquads.count { it.isAlive }}/${battleState.playerSquads.size} squads standing")
        intent.putExtra("node_id", currentNode.id)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}
