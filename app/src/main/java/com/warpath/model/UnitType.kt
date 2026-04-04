package com.warpath.model

enum class UnitCategory {
    FRONTLINE, SKIRMISH, RANGED, CAVALRY, SUPPORT
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
            id = "militia_spear", name = "Militia Spearmen", category = UnitCategory.FRONTLINE,
            baseHp = 80, baseAttack = 12, baseDefense = 15, baseSpeed = 1.0f, range = 1.5f,
            description = "Sturdy spear infantry. Reliable front line.",
            upgradeTo = listOf("veteran_spear", "shield_wall")
        )
        val SHIELD_INFANTRY = UnitType(
            id = "shield_infantry", name = "Shield Infantry", category = UnitCategory.FRONTLINE,
            baseHp = 100, baseAttack = 10, baseDefense = 22, baseSpeed = 0.8f, range = 1.0f,
            description = "Heavy shields absorb punishment.",
            upgradeTo = listOf("shield_wall")
        )
        val VETERAN_SPEAR = UnitType(
            id = "veteran_spear", name = "Veteran Spearmen", category = UnitCategory.FRONTLINE,
            baseHp = 100, baseAttack = 18, baseDefense = 18, baseSpeed = 1.0f, range = 1.5f,
            description = "Battle-hardened spears. Lethal and disciplined."
        )
        val SHIELD_WALL = UnitType(
            id = "shield_wall", name = "Shield Wall", category = UnitCategory.FRONTLINE,
            baseHp = 130, baseAttack = 14, baseDefense = 28, baseSpeed = 0.6f, range = 1.0f,
            description = "Immovable defensive formation."
        )
        val JAVELIN_SKIRMISHER = UnitType(
            id = "javelin_skirmisher", name = "Javelin Skirmishers", category = UnitCategory.SKIRMISH,
            baseHp = 55, baseAttack = 16, baseDefense = 8, baseSpeed = 1.4f, range = 4.0f,
            description = "Fast skirmishers who harass at range.",
            upgradeTo = listOf("elite_javelin")
        )
        val ELITE_JAVELIN = UnitType(
            id = "elite_javelin", name = "Elite Javelineers", category = UnitCategory.SKIRMISH,
            baseHp = 65, baseAttack = 22, baseDefense = 10, baseSpeed = 1.5f, range = 4.5f,
            description = "Deadly precision javelin throwers."
        )
        val ARCHER = UnitType(
            id = "archer", name = "Archers", category = UnitCategory.RANGED,
            baseHp = 45, baseAttack = 20, baseDefense = 5, baseSpeed = 1.0f, range = 7.0f,
            description = "Long-range bow infantry.",
            upgradeTo = listOf("longbow", "crossbow")
        )
        val LONGBOW = UnitType(
            id = "longbow", name = "Longbowmen", category = UnitCategory.RANGED,
            baseHp = 50, baseAttack = 26, baseDefense = 5, baseSpeed = 0.9f, range = 9.0f,
            description = "Extreme range and devastating volleys."
        )
        val CROSSBOW = UnitType(
            id = "crossbow", name = "Crossbowmen", category = UnitCategory.RANGED,
            baseHp = 55, baseAttack = 28, baseDefense = 8, baseSpeed = 0.8f, range = 6.0f,
            description = "Armor-piercing bolts. Slower but deadlier."
        )
        val LIGHT_CAVALRY = UnitType(
            id = "light_cavalry", name = "Light Cavalry", category = UnitCategory.CAVALRY,
            baseHp = 70, baseAttack = 18, baseDefense = 10, baseSpeed = 2.2f, range = 1.0f,
            description = "Fast flanking riders.",
            upgradeTo = listOf("raiders")
        )
        val RAIDERS = UnitType(
            id = "raiders", name = "Raiders", category = UnitCategory.CAVALRY,
            baseHp = 80, baseAttack = 24, baseDefense = 12, baseSpeed = 2.4f, range = 1.0f,
            description = "Elite mounted raiders. Strike and vanish."
        )
        val BANNER_BEARER = UnitType(
            id = "banner_bearer", name = "Banner Bearer", category = UnitCategory.SUPPORT,
            baseHp = 60, baseAttack = 6, baseDefense = 12, baseSpeed = 1.0f, range = 3.0f,
            description = "Boosts nearby morale and attack.",
            upgradeTo = listOf("war_drummer")
        )
        val WAR_DRUMMER = UnitType(
            id = "war_drummer", name = "War Drummer", category = UnitCategory.SUPPORT,
            baseHp = 65, baseAttack = 8, baseDefense = 14, baseSpeed = 1.0f, range = 4.0f,
            description = "Powerful aura that inspires the warband."
        )
        val FIELD_MEDIC = UnitType(
            id = "field_medic", name = "Field Medic", category = UnitCategory.SUPPORT,
            baseHp = 50, baseAttack = 4, baseDefense = 8, baseSpeed = 1.1f, range = 3.0f,
            description = "Heals nearby squads over time."
        )

        // Enemy-only types
        val BANDIT_THUG = UnitType(
            id = "bandit_thug", name = "Bandit Thugs", category = UnitCategory.FRONTLINE,
            baseHp = 60, baseAttack = 14, baseDefense = 8, baseSpeed = 1.2f, range = 1.0f,
            description = "Aggressive but poorly armored."
        )
        val BANDIT_ARCHER = UnitType(
            id = "bandit_archer", name = "Bandit Archers", category = UnitCategory.RANGED,
            baseHp = 35, baseAttack = 16, baseDefense = 4, baseSpeed = 1.1f, range = 6.0f,
            description = "Ragged bowmen. Dangerous in numbers."
        )
        val MILITIA_GUARD = UnitType(
            id = "militia_guard", name = "Militia Guards", category = UnitCategory.FRONTLINE,
            baseHp = 90, baseAttack = 10, baseDefense = 20, baseSpeed = 0.7f, range = 1.0f,
            description = "Defensive town militia. Hard to break."
        )
        val ELITE_RETAINER = UnitType(
            id = "elite_retainer", name = "Elite Retainers", category = UnitCategory.FRONTLINE,
            baseHp = 120, baseAttack = 22, baseDefense = 20, baseSpeed = 1.0f, range = 1.0f,
            description = "Well-trained elite soldiers. Very dangerous."
        )
        val WOLF_PACK = UnitType(
            id = "wolf_pack", name = "Wolf Pack", category = UnitCategory.CAVALRY,
            baseHp = 50, baseAttack = 20, baseDefense = 4, baseSpeed = 2.5f, range = 1.0f,
            description = "Wild beasts. Fast and ferocious."
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
