package com.warpath.model

enum class EnemyPartyBehaviorType {
    PATROL
}

enum class PartyFaction {
    HOSTILE,
    FRIENDLY
}

data class EnemyParty(
    val id: String,
    var nodeId: String,
    val homeNodeId: String,
    val unitTemplates: List<EnemyTemplate>,
    val faction: PartyFaction = PartyFaction.HOSTILE,
    val maxLinkedFromHome: Int = 1,
    val behaviorType: EnemyPartyBehaviorType = EnemyPartyBehaviorType.PATROL,
    var patrolForward: Boolean = true,
    var travelFromNodeId: String? = null,
    var travelToNodeId: String? = null,
    var travelProgress: Float = 0f
)
