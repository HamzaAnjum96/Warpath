package com.warpath.model

enum class NodeType(val displayName: String, val color: Long) {
    ENEMY_PATROL("Threat Contact", 0xFFCC3333),
    RESOURCE_CACHE("Fuel Cache", 0xFF33AA33),
    ELITE_CHALLENGE("Elite Wing", 0xFFDD2222),
    RECOVERY_CAMP("Waypoint", 0xFF3388CC),
    FACTION_OUTPOST("Forward Operating Base", 0xFFCC8833),
    TOWN("Airbase", 0xFF8A4DCC),
    VILLAGE("FOB", 0xFF7A8A33),
    BOSS("HVT", 0xFFAA0000),
    START("Home Base", 0xFF4488AA)
}

data class CampaignNode(
    val id: String,
    val type: NodeType,
    val name: String,
    val description: String,
    val mapX: Float,
    val mapY: Float,
    val connections: MutableList<String> = mutableListOf(),
    var isCleared: Boolean = false,
    var isRevealed: Boolean = false,
    val enemySquads: List<EnemyTemplate> = emptyList(),
    val suppliesReward: Int = 0,
    val renownReward: Int = 0
)

data class EnemyTemplate(
    val unitTypeId: String,
    val count: Int
)
