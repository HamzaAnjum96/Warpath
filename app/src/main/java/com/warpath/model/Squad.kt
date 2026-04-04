package com.warpath.model

import kotlin.math.max
import kotlin.math.min

data class Squad(
    val id: String,
    val unitType: UnitType,
    var count: Int,
    var morale: Float = 100f,
    var x: Float = 0f,
    var y: Float = 0f,
    val isPlayerOwned: Boolean = true,
    var currentHpPercent: Float = 1.0f,
    var state: SquadState = SquadState.IDLE
) {
    val isAlive: Boolean get() = count > 0 && currentHpPercent > 0f
    val isRouted: Boolean get() = morale <= 0f

    val effectiveAttack: Float
        get() = unitType.baseAttack * count * currentHpPercent * moraleMultiplier

    val effectiveDefense: Float
        get() = unitType.baseDefense * currentHpPercent

    val effectiveSpeed: Float
        get() = unitType.baseSpeed * if (state == SquadState.RETREAT) 1.5f else 1.0f

    private val moraleMultiplier: Float
        get() = when {
            morale > 80f -> 1.1f
            morale > 50f -> 1.0f
            morale > 20f -> 0.7f
            else -> 0.4f
        }

    fun takeDamage(damage: Float) {
        val mitigated = max(1f, damage - effectiveDefense * 0.5f)
        val hpLoss = mitigated / (unitType.baseHp * count)
        currentHpPercent = max(0f, currentHpPercent - hpLoss)
        // Lose troops as HP drops
        val expectedCount = max(0, (count * currentHpPercent).toInt())
        if (expectedCount < count) {
            val losses = count - expectedCount
            count = max(0, count - max(1, losses))
            if (count > 0) currentHpPercent = min(1f, currentHpPercent + 0.1f)
        }
        morale = max(0f, morale - mitigated * 0.15f)
    }

    fun heal(amount: Float) {
        currentHpPercent = min(1.0f, currentHpPercent + amount)
        morale = min(100f, morale + amount * 20f)
    }

    fun distanceTo(other: Squad): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun moveToward(targetX: Float, targetY: Float, delta: Float) {
        val dx = targetX - x
        val dy = targetY - y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist > 0.1f) {
            val step = effectiveSpeed * delta * 30f
            val ratio = min(1f, step / dist)
            x += dx * ratio
            y += dy * ratio
        }
    }
}

enum class SquadState {
    IDLE, ADVANCE, ENGAGE, HOLD, FLANK, RETREAT, RALLY
}
