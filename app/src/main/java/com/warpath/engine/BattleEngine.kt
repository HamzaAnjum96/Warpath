package com.warpath.engine

import com.warpath.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class BattleState(
    val playerSquads: MutableList<Squad>,
    val enemySquads: MutableList<Squad>,
    var tickCount: Int = 0,
    var isOver: Boolean = false,
    var playerWon: Boolean = false,
    var battleLog: MutableList<String> = mutableListOf(),
    val activePowerUps: MutableList<ActivePowerUp> = mutableListOf()
)

data class ActivePowerUp(
    val powerUp: PowerUp,
    var remainingTicks: Int = 60
)

class BattleEngine {
    private val commandCooldowns = mutableMapOf<BattleCommand, Long>()
    var focusTargetId: String? = null
    var currentStance: BattleCommand? = null

    fun createBattle(playerWarband: List<Squad>, enemyTemplates: List<EnemyTemplate>): BattleState {
        val playerSquads = playerWarband.map { sq ->
            sq.copy(
                x = 1f + Random.nextFloat() * 2f,
                y = 1f + (playerWarband.indexOf(sq)) * 2.5f,
                state = SquadState.ADVANCE
            )
        }.toMutableList()

        val enemySquads = enemyTemplates.mapIndexed { i, template ->
            Squad(
                id = "enemy_${template.unitTypeId}_$i",
                unitType = UnitType.byId(template.unitTypeId),
                count = template.count,
                isPlayerOwned = false,
                x = 12f + Random.nextFloat() * 2f,
                y = 1f + i * 2.5f,
                state = SquadState.ADVANCE
            )
        }.toMutableList()

        commandCooldowns.clear()
        focusTargetId = null
        currentStance = null

        return BattleState(playerSquads, enemySquads)
    }

    fun tick(state: BattleState, deltaTime: Float) {
        if (state.isOver) return
        state.tickCount++

        // Apply power-ups
        applyPowerUps(state, deltaTime)

        // Move and fight
        processSquads(state.playerSquads, state.enemySquads, deltaTime, state)
        processSquads(state.enemySquads, state.playerSquads, deltaTime, state)

        // Remove dead
        state.playerSquads.removeAll { !it.isAlive }
        state.enemySquads.removeAll { !it.isAlive }

        // Check victory/defeat
        if (state.enemySquads.isEmpty() || state.enemySquads.all { it.isRouted }) {
            state.isOver = true
            state.playerWon = true
            state.battleLog.add("Victory! The enemy is defeated!")
        } else if (state.playerSquads.isEmpty() || state.playerSquads.all { it.isRouted }) {
            state.isOver = true
            state.playerWon = false
            state.battleLog.add("Defeat! Your warband has fallen!")
        }
    }

    fun issueCommand(command: BattleCommand, state: BattleState, targetId: String? = null): Boolean {
        val now = System.currentTimeMillis()
        val lastUsed = commandCooldowns[command] ?: 0L
        if (now - lastUsed < command.cooldownMs) return false

        commandCooldowns[command] = now

        when (command) {
            BattleCommand.FOCUS_TARGET -> {
                focusTargetId = targetId
                state.battleLog.add(">> Focus fire on target!")
            }
            BattleCommand.PUSH -> {
                currentStance = BattleCommand.PUSH
                state.playerSquads.forEach { it.state = SquadState.ADVANCE }
                state.battleLog.add(">> Push forward!")
            }
            BattleCommand.HOLD -> {
                currentStance = BattleCommand.HOLD
                state.playerSquads.forEach { it.state = SquadState.HOLD }
                state.battleLog.add(">> Hold the line!")
            }
            BattleCommand.RALLY -> {
                state.playerSquads.forEach {
                    it.morale = min(100f, it.morale + 25f)
                }
                state.battleLog.add(">> Rally! Morale restored!")
            }
            BattleCommand.RETREAT -> {
                state.playerSquads.forEach { it.state = SquadState.RETREAT }
                state.battleLog.add(">> Fall back!")
            }
        }
        return true
    }

    fun usePowerUp(powerUp: PowerUp, state: BattleState) {
        state.activePowerUps.add(ActivePowerUp(powerUp))
        state.battleLog.add("Used ${powerUp.name}!")

        if (powerUp.effect == PowerUpEffect.SPAWN_REINFORCEMENT) {
            val reinforcement = Squad(
                id = "reinforcement_${state.tickCount}",
                unitType = UnitType.MILITIA_SPEAR,
                count = 4,
                isPlayerOwned = true,
                x = 0f,
                y = 5f,
                state = SquadState.ADVANCE
            )
            state.playerSquads.add(reinforcement)
        }
    }

    fun getCommandCooldownRemaining(command: BattleCommand): Long {
        val now = System.currentTimeMillis()
        val lastUsed = commandCooldowns[command] ?: 0L
        return max(0L, command.cooldownMs - (now - lastUsed))
    }

    private fun applyPowerUps(state: BattleState, deltaTime: Float) {
        val iterator = state.activePowerUps.iterator()
        while (iterator.hasNext()) {
            val active = iterator.next()
            active.remainingTicks--
            when (active.powerUp.effect) {
                PowerUpEffect.HEAL_OVER_TIME -> {
                    state.playerSquads.forEach { it.heal(active.powerUp.magnitude * deltaTime) }
                }
                PowerUpEffect.ATTACK_BOOST -> { /* Applied in damage calc */ }
                PowerUpEffect.ENEMY_ACCURACY_DOWN -> { /* Applied in damage calc */ }
                PowerUpEffect.SPAWN_REINFORCEMENT -> { /* One-time, already applied */ }
            }
            if (active.remainingTicks <= 0) iterator.remove()
        }
    }

    private fun processSquads(
        friendlySquads: List<Squad>,
        enemySquads: List<Squad>,
        deltaTime: Float,
        state: BattleState
    ) {
        for (squad in friendlySquads) {
            if (!squad.isAlive || squad.isRouted) continue

            val target = pickTarget(squad, enemySquads, state)
            if (target == null) continue

            val dist = squad.distanceTo(target)
            val inRange = dist <= squad.unitType.range

            when (squad.state) {
                SquadState.ADVANCE, SquadState.IDLE -> {
                    if (inRange) {
                        squad.state = SquadState.ENGAGE
                        attack(squad, target, state, deltaTime)
                    } else {
                        squad.moveToward(target.x, target.y, deltaTime)
                    }
                }
                SquadState.ENGAGE -> {
                    if (!inRange) {
                        squad.moveToward(target.x, target.y, deltaTime)
                    } else {
                        attack(squad, target, state, deltaTime)
                    }
                }
                SquadState.HOLD -> {
                    if (inRange) {
                        attack(squad, target, state, deltaTime)
                    }
                    // Don't move
                }
                SquadState.RETREAT -> {
                    val retreatX = if (squad.isPlayerOwned) -2f else 16f
                    squad.moveToward(retreatX, squad.y, deltaTime)
                }
                SquadState.FLANK -> {
                    val flankY = if (target.y > 5f) target.y - 3f else target.y + 3f
                    if (!inRange) {
                        squad.moveToward(target.x, flankY, deltaTime)
                    } else {
                        attack(squad, target, state, deltaTime)
                    }
                }
                SquadState.RALLY -> {
                    squad.morale = min(100f, squad.morale + 5f * deltaTime)
                    if (squad.morale > 50f) squad.state = SquadState.ADVANCE
                }
            }

            // Auto-rally if morale drops
            if (squad.morale <= 15f && squad.state != SquadState.RETREAT) {
                squad.state = SquadState.RALLY
            }
        }
    }

    private fun pickTarget(squad: Squad, enemies: List<Squad>, state: BattleState): Squad? {
        val alive = enemies.filter { it.isAlive && !it.isRouted }
        if (alive.isEmpty()) return null

        // Focus target priority
        if (squad.isPlayerOwned && focusTargetId != null) {
            alive.find { it.id == focusTargetId }?.let { return it }
        }

        // Closest target
        return alive.minByOrNull { squad.distanceTo(it) }
    }

    private fun attack(attacker: Squad, target: Squad, state: BattleState, deltaTime: Float) {
        var damage = attacker.effectiveAttack * deltaTime

        // Power-up modifiers
        if (attacker.isPlayerOwned) {
            state.activePowerUps.forEach { active ->
                when (active.powerUp.effect) {
                    PowerUpEffect.ATTACK_BOOST -> damage *= active.powerUp.magnitude
                    else -> {}
                }
            }
        } else {
            state.activePowerUps.forEach { active ->
                when (active.powerUp.effect) {
                    PowerUpEffect.ENEMY_ACCURACY_DOWN -> damage *= active.powerUp.magnitude
                    else -> {}
                }
            }
        }

        // Random variance
        damage *= (0.8f + Random.nextFloat() * 0.4f)

        target.takeDamage(damage)

        if (!target.isAlive && state.tickCount % 10 == 0) {
            state.battleLog.add("${attacker.unitType.name} destroyed ${target.unitType.name}!")
        }
    }
}
