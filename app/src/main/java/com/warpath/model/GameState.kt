package com.warpath.model

data class GameState(
    var commanderName: String = "Commander",
    var supplies: Int = 180,
    var renown: Int = 0,
    var warband: MutableList<Squad> = mutableListOf(),
    var maxWarbandSlots: Int = 5,
    var currentNodeId: String = "start",
    var playerMapX: Float = 0.1f,
    var playerMapY: Float = 0.5f,
    var enemyParties: MutableList<EnemyParty> = mutableListOf(),
    var nodesCleared: Int = 0,
    var battlesWon: Int = 0,
    var battlesLost: Int = 0,
    var isRunActive: Boolean = true
) {
    fun addSquad(squad: Squad): Boolean {
        if (warband.size >= maxWarbandSlots) return false
        warband.add(squad)
        return true
    }

    fun removeSquad(squadId: String) {
        warband.removeAll { it.id == squadId }
    }

    fun healWarband(amount: Float) {
        warband.forEach { it.heal(amount) }
    }

    fun canRecruit(): Boolean = warband.size < maxWarbandSlots

    companion object {
        fun newGame(): GameState {
            val state = GameState()
            state.warband.add(
                Squad(
                    id = "squad_spear_1",
                    unitType = UnitType.MILITIA_SPEAR,
                    count = 24,
                    isPlayerOwned = true
                )
            )
            state.warband.add(
                Squad(
                    id = "squad_archer_1",
                    unitType = UnitType.ARCHER,
                    count = 14,
                    isPlayerOwned = true
                )
            )
            state.warband.add(
                Squad(
                    id = "squad_skirmish_1",
                    unitType = UnitType.JAVELIN_SKIRMISHER,
                    count = 12,
                    isPlayerOwned = true
                )
            )
            return state
        }
    }
}
