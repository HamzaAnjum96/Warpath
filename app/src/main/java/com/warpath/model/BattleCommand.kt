package com.warpath.model

enum class BattleCommand(val displayName: String, val description: String, val cooldownMs: Long) {
    FOCUS_TARGET("Focus", "All squads attack selected enemy", 8000),
    PUSH("Push!", "Squads advance aggressively", 10000),
    HOLD("Hold!", "Squads hold position and defend", 10000),
    RALLY("Rally!", "Restore morale to nearby squads", 15000),
    RETREAT("Retreat", "Pull squads back to safety", 5000)
}

data class PowerUp(
    val id: String,
    val name: String,
    val description: String,
    val effect: PowerUpEffect,
    val magnitude: Float
)

enum class PowerUpEffect {
    ATTACK_BOOST, HEAL_OVER_TIME, ENEMY_ACCURACY_DOWN, SPAWN_REINFORCEMENT
}

val AVAILABLE_POWERUPS = listOf(
    PowerUp("rally_horn", "Rally Horn", "Temporary attack speed boost", PowerUpEffect.ATTACK_BOOST, 1.3f),
    PowerUp("field_rations", "Field Rations", "Small heal over time", PowerUpEffect.HEAL_OVER_TIME, 0.15f),
    PowerUp("smoke_pots", "Smoke Pots", "Enemy accuracy reduced", PowerUpEffect.ENEMY_ACCURACY_DOWN, 0.6f),
    PowerUp("reinforcements", "Reinforcements", "Spawn temporary allied squad", PowerUpEffect.SPAWN_REINFORCEMENT, 1f)
)
