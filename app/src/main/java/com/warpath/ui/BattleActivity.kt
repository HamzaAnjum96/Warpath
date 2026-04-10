package com.warpath.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.warpath.R
import com.warpath.engine.BattleEngine
import com.warpath.engine.BattleState
import com.warpath.model.BattleCommand
import com.warpath.model.CampaignNode
import com.warpath.model.EnemyParty

class BattleActivity : AppCompatActivity() {

    private lateinit var battleView: BattleView
    private lateinit var battleEngine: BattleEngine
    private lateinit var battleState: BattleState
    private lateinit var logText: TextView
    private lateinit var commandBar: LinearLayout
    private lateinit var startBattleButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val tickInterval = 50L
    private var node: CampaignNode? = null
    private var roamingParty: EnemyParty? = null
    private val startingPlayerCounts = mutableMapOf<String, Int>()
    private var startingEnemyCount: Int = 0

    private val commandButtons = mutableMapOf<BattleCommand, Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val nodeId = intent.getStringExtra("node_id")
        val partyId = intent.getStringExtra("party_id")

        battleEngine = BattleEngine()

        val battleTitle: String
        val enemyTemplates: List<com.warpath.model.EnemyTemplate>

        if (partyId != null) {
            // Intercept battle against a roaming party
            roamingParty = CampaignActivity.campaignManager.gameState.enemyParties.find { it.id == partyId }
            val party = roamingParty ?: run { finish(); return }
            battleTitle = "Airspace Intercept"
            enemyTemplates = party.unitTemplates
        } else {
            // Node-based battle
            if (nodeId == null) { finish(); return }
            node = CampaignActivity.campaignManager.campaignMap.find { it.id == nodeId }
            val currentNode = node ?: run { finish(); return }
            battleTitle = currentNode.name
            enemyTemplates = currentNode.enemySquads
        }

        battleState = battleEngine.createBattle(
            CampaignActivity.campaignManager.gameState.warband,
            enemyTemplates
        )
        startingPlayerCounts.clear()
        CampaignActivity.campaignManager.gameState.warband.forEach { squad ->
            startingPlayerCounts[squad.id] = squad.count
        }
        startingEnemyCount = battleState.enemySquads.sumOf { it.count }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.BASE_BG))
        }

        battleView = BattleView(this).apply {
            battleState = this@BattleActivity.battleState
            onEnemyTapped = { enemy ->
                battleEngine.issueCommand(BattleCommand.FOCUS_TARGET, this@BattleActivity.battleState, enemy.id)
            }
        }

        val mainLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Title bar with gradient
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                Color.parseColor(UiTheme.SURFACE),
                Color.parseColor(UiTheme.SURFACE_ALT)
            ))
            setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_3))
            gravity = Gravity.CENTER_VERTICAL
            elevation = dpF(UiTheme.HUD_ELEVATION)
        }
        // Accent bar
        titleBar.addView(View(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.HOSTILE))
        }, LinearLayout.LayoutParams(dp(3), LinearLayout.LayoutParams.MATCH_PARENT).apply {
            marginEnd = dp(UiTheme.SPACE_3)
        })
        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleCol.addView(TextView(this).apply {
            text = "AIR ENGAGEMENT"
            textSize = UiTheme.TEXT_CHIP
            setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
            typeface = UiTheme.TYPEFACE_LABEL
            letterSpacing = 0.1f
        })
        titleCol.addView(TextView(this).apply {
            text = battleTitle
            textSize = UiTheme.TEXT_BODY
            setTextColor(Color.parseColor(UiTheme.GOLD))
            typeface = UiTheme.TYPEFACE_HEADING
        })
        titleBar.addView(titleCol)
        mainLayout.addView(titleBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Battle view
        val battleContainer = FrameLayout(this)
        battleContainer.addView(battleView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        startBattleButton = Button(this).apply {
            text = "SCRAMBLE!"
            textSize = UiTheme.TEXT_BUTTON
            typeface = UiTheme.TYPEFACE_HEADING
            isAllCaps = false
            setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
            setPadding(dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_5), dp(UiTheme.SPACE_3))
            stateListAnimator = null
            background = UiTheme.rippleDrawable(UiTheme.WARNING, null, UiTheme.RADIUS_MD)
            elevation = dpF(UiTheme.SHEET_ELEVATION)
            setOnClickListener {
                visibility = View.GONE
                if (!isRunning) {
                    isRunning = true
                    handler.post(battleTick)
                }
            }
        }
        battleContainer.addView(startBattleButton, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ))
        mainLayout.addView(battleContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Battle log
        val logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                Color.parseColor(UiTheme.SURFACE_ALT),
                Color.parseColor(UiTheme.SURFACE)
            ))
            setPadding(dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_4), dp(UiTheme.SPACE_2))
            gravity = Gravity.CENTER_VERTICAL
        }
        logContainer.addView(TextView(this).apply {
            text = "◈"
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_SUBTLE))
            setPadding(0, 0, dp(UiTheme.SPACE_2), 0)
        })
        logText = TextView(this).apply {
            textSize = UiTheme.TEXT_SECONDARY
            setTextColor(Color.parseColor(UiTheme.TEXT_MUTED))
            typeface = UiTheme.TYPEFACE_BODY
            maxLines = 2
            text = "Awaiting orders."
        }
        logContainer.addView(logText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        mainLayout.addView(logContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Divider
        mainLayout.addView(View(this).apply {
            setBackgroundColor(Color.parseColor(UiTheme.DIVIDER))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))

        // Command bar
        commandBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(UiTheme.SURFACE))
            setPadding(dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_3), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_3))
            gravity = Gravity.CENTER
        }

        for (cmd in BattleCommand.values()) {
            val btn = Button(this).apply {
                text = cmd.displayName
                textSize = UiTheme.TEXT_BUTTON_SM
                typeface = UiTheme.TYPEFACE_HEADING
                isAllCaps = false
                setTextColor(Color.parseColor(UiTheme.TEXT_PRIMARY))
                setPadding(dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_2), dp(UiTheme.SPACE_2))
                stateListAnimator = null
                minHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
                minimumHeight = dp(UiTheme.BUTTON_HEIGHT_SM)
                background = UiTheme.rippleDrawable(getCommandColor(cmd), UiTheme.BORDER, UiTheme.RADIUS_SM)
                setOnClickListener { onCommand(cmd) }
                // Add icon above text
                AppCompatResources.getDrawable(context, getCommandIcon(cmd))?.let { d ->
                    DrawableCompat.setTint(d.mutate(), Color.parseColor(UiTheme.TEXT_PRIMARY))
                    d.setBounds(0, 0, dp(14), dp(14))
                    setCompoundDrawables(null, d, null, null)
                    compoundDrawablePadding = dp(2)
                }
            }
            commandButtons[cmd] = btn
            commandBar.addView(btn, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(2); marginEnd = dp(2) })
        }
        mainLayout.addView(commandBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        root.addView(mainLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)
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

    private fun getCommandIcon(cmd: BattleCommand): Int = when (cmd) {
        BattleCommand.FOCUS_TARGET -> R.drawable.ic_lucide_crosshair
        BattleCommand.PUSH         -> R.drawable.ic_lucide_zap
        BattleCommand.HOLD         -> R.drawable.ic_lucide_shield
        BattleCommand.RALLY        -> R.drawable.ic_lucide_flag
        BattleCommand.RETREAT      -> R.drawable.ic_lucide_arrow_left
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
        val party = roamingParty
        val currentNode = node

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

        val battleName: String
        val suppliesReward: Int
        val renownReward: Int

        if (party != null) {
            // Roaming party battle
            battleName = "Airspace Intercept"
            suppliesReward = if (battleState.playerWon) 20 else 0
            renownReward = if (battleState.playerWon) 8 else 0
            if (battleState.playerWon) {
                CampaignActivity.campaignManager.gameState.supplies += suppliesReward
                CampaignActivity.campaignManager.gameState.renown += renownReward
                CampaignActivity.campaignManager.gameState.enemyParties.removeAll { it.id == party.id }
                CampaignActivity.campaignManager.gameState.battlesWon++
            } else {
                CampaignActivity.campaignManager.gameState.battlesLost++
                CampaignActivity.campaignManager.gameState.supplies =
                    (CampaignActivity.campaignManager.gameState.supplies * 0.7f).toInt()
            }
        } else if (currentNode != null) {
            battleName = currentNode.name
            suppliesReward = if (battleState.playerWon) currentNode.suppliesReward else 0
            renownReward = if (battleState.playerWon) currentNode.renownReward else 0
            CampaignActivity.campaignManager.resolveNodeRewards(currentNode, battleState.playerWon)
        } else {
            finish(); return
        }

        val intent = Intent(this, BattleResultActivity::class.java)
        intent.putExtra("player_won", battleState.playerWon)
        intent.putExtra("node_name", battleName)
        intent.putExtra("supplies_reward", suppliesReward)
        intent.putExtra("renown_reward", renownReward)
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
        intent.putExtra("warband_status", "${battleState.playerSquads.count { it.isAlive }}/${battleState.playerSquads.size} flights standing")
        intent.putExtra("node_id", currentNode?.id ?: "")
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}
