package com.warpath.model

enum class EnemyPartyBehaviorType {
    PATROL
}

data class EnemyParty(
    val id: String,
    var nodeId: String,
    val unitTemplates: List<EnemyTemplate>,
    val behaviorType: EnemyPartyBehaviorType = EnemyPartyBehaviorType.PATROL,
    var patrolForward: Boolean = true
)
