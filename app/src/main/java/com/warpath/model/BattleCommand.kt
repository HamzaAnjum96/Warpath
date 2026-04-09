package com.warpath.model

enum class BattleCommand(val displayName: String, val description: String, val cooldownMs: Long) {
    FOCUS_TARGET("Lock Target", "All flights engage selected hostile", 8000),
    PUSH("Engage!", "Flights advance aggressively", 10000),
    HOLD("Hold Pattern", "Flights hold position and defend", 10000),
    RALLY("Reform!", "Restore pilot confidence to nearby flights", 15000),
    RETREAT("Disengage", "Pull flights back to safety", 5000)
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
    PowerUp("rally_horn", "Afterburner", "Temporary attack speed boost", PowerUpEffect.ATTACK_BOOST, 1.3f),
    PowerUp("field_rations", "Repair Bay", "Small hull repair over time", PowerUpEffect.HEAL_OVER_TIME, 0.15f),
    PowerUp("smoke_pots", "ECM Burst", "Enemy targeting accuracy reduced", PowerUpEffect.ENEMY_ACCURACY_DOWN, 0.6f),
    PowerUp("reinforcements", "Air Support", "Spawn temporary allied flight", PowerUpEffect.SPAWN_REINFORCEMENT, 1f)
)
