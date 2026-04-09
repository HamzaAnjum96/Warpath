package com.warpath.model

enum class UnitCategory {
    FIGHTER, INTERCEPTOR, BOMBER, RECON, SUPPORT
}

data class UnitType(
    val id: String,
    val name: String,
    val category: UnitCategory,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Float,
    val range: Float,
    val description: String,
    val upgradeTo: List<String> = emptyList()
) {
    companion object {
        val MILITIA_SPEAR = UnitType(
            id = "militia_spear", name = "Fighter Flight", category = UnitCategory.FIGHTER,
            baseHp = 80, baseAttack = 12, baseDefense = 15, baseSpeed = 1.0f, range = 1.5f,
            description = "Standard air superiority flight. Reliable frontline.",
            upgradeTo = listOf("veteran_spear", "shield_wall")
        )
        val SHIELD_INFANTRY = UnitType(
            id = "shield_infantry", name = "Escort Wing", category = UnitCategory.FIGHTER,
            baseHp = 100, baseAttack = 10, baseDefense = 22, baseSpeed = 0.8f, range = 1.0f,
            description = "Heavily armed escort. Absorbs incoming fire.",
            upgradeTo = listOf("shield_wall")
        )
        val VETERAN_SPEAR = UnitType(
            id = "veteran_spear", name = "Veteran Fighter Wing", category = UnitCategory.FIGHTER,
            baseHp = 100, baseAttack = 18, baseDefense = 18, baseSpeed = 1.0f, range = 1.5f,
            description = "Battle-hardened pilots. Lethal and disciplined."
        )
        val SHIELD_WALL = UnitType(
            id = "shield_wall", name = "Air Defense Wing", category = UnitCategory.FIGHTER,
            baseHp = 130, baseAttack = 14, baseDefense = 28, baseSpeed = 0.6f, range = 1.0f,
            description = "Immovable defensive formation. Hard to break through."
        )
        val JAVELIN_SKIRMISHER = UnitType(
            id = "javelin_skirmisher", name = "Interceptor Flight", category = UnitCategory.INTERCEPTOR,
            baseHp = 55, baseAttack = 16, baseDefense = 8, baseSpeed = 1.4f, range = 4.0f,
            description = "Fast interceptors who harass at range.",
            upgradeTo = listOf("elite_javelin")
        )
        val ELITE_JAVELIN = UnitType(
            id = "elite_javelin", name = "Elite Strike Wing", category = UnitCategory.INTERCEPTOR,
            baseHp = 65, baseAttack = 22, baseDefense = 10, baseSpeed = 1.5f, range = 4.5f,
            description = "Precision strike aircraft. Deadly at standoff range."
        )
        val ARCHER = UnitType(
            id = "archer", name = "Bomber Flight", category = UnitCategory.BOMBER,
            baseHp = 45, baseAttack = 20, baseDefense = 5, baseSpeed = 1.0f, range = 7.0f,
            description = "Long-range strike aircraft. High payload.",
            upgradeTo = listOf("longbow", "crossbow")
        )
        val LONGBOW = UnitType(
            id = "longbow", name = "Strategic Bomber Wing", category = UnitCategory.BOMBER,
            baseHp = 50, baseAttack = 26, baseDefense = 5, baseSpeed = 0.9f, range = 9.0f,
            description = "Extreme range and devastating payload."
        )
        val CROSSBOW = UnitType(
            id = "crossbow", name = "Precision Strike Wing", category = UnitCategory.BOMBER,
            baseHp = 55, baseAttack = 28, baseDefense = 8, baseSpeed = 0.8f, range = 6.0f,
            description = "Armor-piercing ordnance. Slower but deadlier."
        )
        val LIGHT_CAVALRY = UnitType(
            id = "light_cavalry", name = "Recon Flight", category = UnitCategory.RECON,
            baseHp = 70, baseAttack = 18, baseDefense = 10, baseSpeed = 2.2f, range = 1.0f,
            description = "Fast flanking recon aircraft.",
            upgradeTo = listOf("raiders")
        )
        val RAIDERS = UnitType(
            id = "raiders", name = "Fast Attack Wing", category = UnitCategory.RECON,
            baseHp = 80, baseAttack = 24, baseDefense = 12, baseSpeed = 2.4f, range = 1.0f,
            description = "Elite fast attack. Strike and disengage."
        )
        val BANNER_BEARER = UnitType(
            id = "banner_bearer", name = "AWACS Flight", category = UnitCategory.SUPPORT,
            baseHp = 60, baseAttack = 6, baseDefense = 12, baseSpeed = 1.0f, range = 3.0f,
            description = "Airborne command. Boosts nearby pilot confidence.",
            upgradeTo = listOf("war_drummer")
        )
        val WAR_DRUMMER = UnitType(
            id = "war_drummer", name = "ECM Wing", category = UnitCategory.SUPPORT,
            baseHp = 65, baseAttack = 8, baseDefense = 14, baseSpeed = 1.0f, range = 4.0f,
            description = "Electronic warfare. Disrupts enemy targeting across the squadron."
        )
        val FIELD_MEDIC = UnitType(
            id = "field_medic", name = "Tanker Flight", category = UnitCategory.SUPPORT,
            baseHp = 50, baseAttack = 4, baseDefense = 8, baseSpeed = 1.1f, range = 3.0f,
            description = "Aerial refueling. Restores nearby aircraft over time."
        )

        // Enemy-only types
        val BANDIT_THUG = UnitType(
            id = "bandit_thug", name = "Hostile Fighter Flight", category = UnitCategory.FIGHTER,
            baseHp = 60, baseAttack = 14, baseDefense = 8, baseSpeed = 1.2f, range = 1.0f,
            description = "Aggressive but lightly armored enemy fighters."
        )
        val BANDIT_ARCHER = UnitType(
            id = "bandit_archer", name = "Hostile Bomber Wing", category = UnitCategory.BOMBER,
            baseHp = 35, baseAttack = 16, baseDefense = 4, baseSpeed = 1.1f, range = 6.0f,
            description = "Enemy strike aircraft. Dangerous in numbers."
        )
        val MILITIA_GUARD = UnitType(
            id = "militia_guard", name = "Defense Wing", category = UnitCategory.FIGHTER,
            baseHp = 90, baseAttack = 10, baseDefense = 20, baseSpeed = 0.7f, range = 1.0f,
            description = "Area defense flight. Hard to break through."
        )
        val ELITE_RETAINER = UnitType(
            id = "elite_retainer", name = "Elite Fighter Wing", category = UnitCategory.FIGHTER,
            baseHp = 120, baseAttack = 22, baseDefense = 20, baseSpeed = 1.0f, range = 1.0f,
            description = "Veteran enemy pilots. Well-trained and very dangerous."
        )
        val WOLF_PACK = UnitType(
            id = "wolf_pack", name = "Hunter Drone Wing", category = UnitCategory.RECON,
            baseHp = 50, baseAttack = 20, baseDefense = 4, baseSpeed = 2.5f, range = 1.0f,
            description = "Autonomous attack drones. Fast and ferocious."
        )

        val ALL = listOf(
            MILITIA_SPEAR, SHIELD_INFANTRY, VETERAN_SPEAR, SHIELD_WALL,
            JAVELIN_SKIRMISHER, ELITE_JAVELIN,
            ARCHER, LONGBOW, CROSSBOW,
            LIGHT_CAVALRY, RAIDERS,
            BANNER_BEARER, WAR_DRUMMER, FIELD_MEDIC,
            BANDIT_THUG, BANDIT_ARCHER, MILITIA_GUARD, ELITE_RETAINER, WOLF_PACK
        )

        fun byId(id: String): UnitType = ALL.first { it.id == id }
    }
}
