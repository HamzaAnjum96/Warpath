package com.warpath.engine

import com.warpath.model.*
import kotlin.random.Random

class CampaignManager {
    var gameState: GameState = GameState.newGame()
        private set
    var campaignMap: List<CampaignNode> = emptyList()
        private set
    private var partyCounter: Int = 0

    private data class PartySpawnHome(
        val homeNodeId: String,
        val faction: PartyFaction,
        val maxLinked: Int,
        val templateOptions: List<List<EnemyTemplate>>
    )

    fun startNewCampaign(): GameState {
        gameState = GameState.newGame()
        campaignMap = generateMap()
        gameState.currentNodeId = "start"
        gameState.playerMapX = 0.1f
        gameState.playerMapY = 0.5f
        partyCounter = 0
        gameState.enemyParties = mutableListOf()
        refillRoamingParties(forceFill = true)
        revealPoisNearPlayer()
        return gameState
    }

    fun getCurrentNode(): CampaignNode? =
        campaignMap.find { it.id == gameState.currentNodeId }

    fun getAccessibleNodes(): List<CampaignNode> {
        return campaignMap.filter { it.isRevealed && !it.isCleared }
    }

    fun moveToNode(nodeId: String): CampaignNode? {
        val node = campaignMap.find { it.id == nodeId && it.isRevealed } ?: return null
        gameState.currentNodeId = nodeId
        gameState.playerMapX = node.mapX
        gameState.playerMapY = node.mapY
        return node
    }

    fun movePlayerBy(deltaX: Float, deltaY: Float) {
        gameState.playerMapX = (gameState.playerMapX + deltaX).coerceIn(0.02f, 0.98f)
        gameState.playerMapY = (gameState.playerMapY + deltaY).coerceIn(0.02f, 0.98f)
    }

    fun setPlayerPosition(normX: Float, normY: Float) {
        gameState.playerMapX = normX.coerceIn(0.02f, 0.98f)
        gameState.playerMapY = normY.coerceIn(0.02f, 0.98f)
    }

    fun findClosestRevealedNodeInDirection(dirX: Float, dirY: Float): CampaignNode? {
        val px = gameState.playerMapX
        val py = gameState.playerMapY
        val dirLen2 = dirX * dirX + dirY * dirY
        val hasDirection = dirLen2 > 0.001f
        return campaignMap
            .asSequence()
            .filter { it.isRevealed }
            .filter {
                if (!hasDirection) return@filter true
                val nx = it.mapX - px
                val ny = it.mapY - py
                nx * dirX + ny * dirY > 0f
            }
            .minByOrNull {
                val dx = it.mapX - px
                val dy = it.mapY - py
                dx * dx + dy * dy
            }
    }

    fun findNearbyRevealedNode(maxDistance: Float): CampaignNode? {
        val px = gameState.playerMapX
        val py = gameState.playerMapY
        val maxDistance2 = maxDistance * maxDistance
        return campaignMap
            .asSequence()
            .filter { it.isRevealed }
            .map { node ->
                val dx = node.mapX - px
                val dy = node.mapY - py
                node to (dx * dx + dy * dy)
            }
            .filter { (_, distance2) -> distance2 <= maxDistance2 }
            .minByOrNull { it.second }
            ?.first
    }

    fun revealPoisNearPlayer(radius: Float = 0.13f): List<CampaignNode> {
        val px = gameState.playerMapX
        val py = gameState.playerMapY
        val radius2 = radius * radius
        val newlyRevealed = mutableListOf<CampaignNode>()
        campaignMap.forEach { node ->
            if (node.isRevealed) return@forEach
            val dx = node.mapX - px
            val dy = node.mapY - py
            if (dx * dx + dy * dy <= radius2) {
                node.isRevealed = true
                newlyRevealed.add(node)
            }
        }
        return newlyRevealed
    }

    fun revealNode(nodeId: String): CampaignNode? {
        val node = campaignMap.find { it.id == nodeId } ?: return null
        node.isRevealed = true
        return node
    }

    fun stepEnemyParties(): Boolean {
        syncPartiesWithClearedHomes()
        refillRoamingParties()
        var hitPlayer = false
        for (party in gameState.enemyParties) {
            val node = campaignMap.find { it.id == party.nodeId } ?: continue
            if (node.connections.isEmpty()) continue

            val nextId = when (party.behaviorType) {
                EnemyPartyBehaviorType.PATROL -> {
                    val sorted = node.connections.sorted()
                    val choice = if (party.patrolForward) sorted.first() else sorted.last()
                    if (sorted.size > 1) {
                        party.patrolForward = !party.patrolForward
                    }
                    choice
                }
            }
            party.nodeId = nextId
            if (party.faction == PartyFaction.HOSTILE && nextId == gameState.currentNodeId) {
                hitPlayer = true
            }
        }
        resolveFriendlyHostileCollisions()
        return hitPlayer
    }

    fun resolveNodeRewards(node: CampaignNode, battleWon: Boolean) {
        if (battleWon) {
            node.isCleared = true
            gameState.supplies += node.suppliesReward
            gameState.renown += node.renownReward
            gameState.nodesCleared++
            gameState.battlesWon++
            syncPartiesWithClearedHomes()
        } else {
            gameState.battlesLost++
            // Lose some supplies on defeat
            gameState.supplies = (gameState.supplies * 0.7f).toInt()
        }
    }

    fun resolveRecoveryCamp(node: CampaignNode) {
        node.isCleared = true
        val healCost = 20
        if (gameState.supplies >= healCost) {
            gameState.supplies -= healCost
            gameState.healWarband(0.4f)
        }
        syncPartiesWithClearedHomes()
    }

    fun resolveResourceCache(node: CampaignNode) {
        node.isCleared = true
        gameState.supplies += node.suppliesReward
        gameState.renown += node.renownReward
        gameState.nodesCleared++
        syncPartiesWithClearedHomes()
    }

    fun recruitUnit(unitType: UnitType, count: Int, cost: Int): Boolean {
        if (gameState.supplies < cost) return false
        if (!gameState.canRecruit()) return false
        gameState.supplies -= cost
        val squad = Squad(
            id = "squad_${unitType.id}_${System.currentTimeMillis()}",
            unitType = unitType,
            count = count,
            isPlayerOwned = true
        )
        val recruited = gameState.addSquad(squad)
        if (recruited) {
            refillRoamingParties()
        }
        return recruited
    }

    fun isRunOver(): Boolean {
        if (gameState.warband.isEmpty()) return true
        // Check if boss is cleared
        val boss = campaignMap.find { it.type == NodeType.BOSS }
        if (boss?.isCleared == true) return true
        return !gameState.isRunActive
    }

    fun getRunSummary(): String {
        val boss = campaignMap.find { it.type == NodeType.BOSS }
        val won = boss?.isCleared == true
        return if (won) {
            "VICTORY! Region conquered!\nBattles: ${gameState.battlesWon}W/${gameState.battlesLost}L\n" +
            "Nodes Cleared: ${gameState.nodesCleared}\nRenown Earned: ${gameState.renown}"
        } else {
            "DEFEAT. Your warband has fallen.\nBattles: ${gameState.battlesWon}W/${gameState.battlesLost}L\n" +
            "Nodes Cleared: ${gameState.nodesCleared}\nRenown Earned: ${gameState.renown}"
        }
    }

    private fun generateMap(): List<CampaignNode> {
        val nodes = mutableListOf<CampaignNode>()

        // Start node
        nodes.add(CampaignNode(
            id = "start", type = NodeType.START, name = "War Camp",
            description = "Your warband's staging ground.",
            mapX = 0.1f, mapY = 0.5f, isCleared = true, isRevealed = true
        ))

        // Layer 1 (3 nodes)
        nodes.add(CampaignNode(
            id = "patrol_1", type = NodeType.ENEMY_PATROL, name = "Bandit Roadblock",
            description = "A small group of bandits blocking the road.",
            mapX = 0.25f, mapY = 0.2f,
            enemySquads = listOf(EnemyTemplate("bandit_thug", 6), EnemyTemplate("bandit_archer", 3)),
            suppliesReward = 30, renownReward = 10
        ))
        nodes.add(CampaignNode(
            id = "resource_1", type = NodeType.RESOURCE_CACHE, name = "Abandoned Supply Wagon",
            description = "An unguarded supply wagon on the road.",
            mapX = 0.25f, mapY = 0.5f,
            suppliesReward = 50, renownReward = 5
        ))
        nodes.add(CampaignNode(
            id = "patrol_2", type = NodeType.ENEMY_PATROL, name = "Wolf Den",
            description = "Wild wolves have made a den near the path.",
            mapX = 0.25f, mapY = 0.8f,
            enemySquads = listOf(EnemyTemplate("wolf_pack", 5)),
            suppliesReward = 15, renownReward = 15
        ))

        // Layer 2 (3 nodes)
        nodes.add(CampaignNode(
            id = "camp_1", type = NodeType.RECOVERY_CAMP, name = "Roadside Inn",
            description = "A safe place to rest and recover.",
            mapX = 0.45f, mapY = 0.15f,
            suppliesReward = 0, renownReward = 0
        ))
        nodes.add(CampaignNode(
            id = "outpost_1", type = NodeType.FACTION_OUTPOST, name = "Merchant's Guild Post",
            description = "A trading outpost. Recruit new troops here.",
            mapX = 0.45f, mapY = 0.5f,
            suppliesReward = 20, renownReward = 10
        ))
        nodes.add(CampaignNode(
            id = "patrol_3", type = NodeType.ENEMY_PATROL, name = "Militia Checkpoint",
            description = "Local militia demands a toll... or a fight.",
            mapX = 0.45f, mapY = 0.85f,
            enemySquads = listOf(
                EnemyTemplate("militia_guard", 6),
                EnemyTemplate("militia_spear", 4)
            ),
            suppliesReward = 40, renownReward = 20
        ))

        nodes.add(CampaignNode(
            id = "town_1", type = NodeType.TOWN, name = "Stonewatch Town",
            description = "A fortified town with skilled healers and recruiters.",
            mapX = 0.58f, mapY = 0.5f,
            suppliesReward = 0, renownReward = 0
        ))

        nodes.add(CampaignNode(
            id = "village_1", type = NodeType.VILLAGE, name = "Elderfield Village",
            description = "A quiet village that can patch your warband for fewer supplies.",
            mapX = 0.72f, mapY = 0.15f,
            suppliesReward = 0, renownReward = 0
        ))

        // Layer 3 (2 nodes)
        nodes.add(CampaignNode(
            id = "elite_1", type = NodeType.ELITE_CHALLENGE, name = "Retainer Ambush",
            description = "Elite soldiers lie in wait. High risk, high reward.",
            mapX = 0.65f, mapY = 0.3f,
            enemySquads = listOf(
                EnemyTemplate("elite_retainer", 4),
                EnemyTemplate("bandit_archer", 5)
            ),
            suppliesReward = 60, renownReward = 30
        ))
        nodes.add(CampaignNode(
            id = "camp_2", type = NodeType.RECOVERY_CAMP, name = "Forest Clearing",
            description = "A peaceful clearing to tend wounds.",
            mapX = 0.65f, mapY = 0.7f,
            suppliesReward = 0, renownReward = 0
        ))

        // Layer 4 (Boss)
        nodes.add(CampaignNode(
            id = "boss_1", type = NodeType.BOSS, name = "Warlord's Stronghold",
            description = "The regional warlord awaits. Defeat him to conquer the region.",
            mapX = 0.85f, mapY = 0.5f,
            enemySquads = listOf(
                EnemyTemplate("elite_retainer", 5),
                EnemyTemplate("militia_guard", 6),
                EnemyTemplate("bandit_archer", 4),
                EnemyTemplate("bandit_thug", 4)
            ),
            suppliesReward = 100, renownReward = 50
        ))

        // Connect nodes
        connect(nodes, "start", "patrol_1")
        connect(nodes, "start", "resource_1")
        connect(nodes, "start", "patrol_2")

        connect(nodes, "patrol_1", "camp_1")
        connect(nodes, "patrol_1", "outpost_1")
        connect(nodes, "resource_1", "outpost_1")
        connect(nodes, "resource_1", "patrol_3")
        connect(nodes, "patrol_2", "patrol_3")

        connect(nodes, "camp_1", "elite_1")
        connect(nodes, "outpost_1", "elite_1")
        connect(nodes, "outpost_1", "camp_2")
        connect(nodes, "outpost_1", "town_1")
        connect(nodes, "patrol_3", "camp_2")
        connect(nodes, "camp_1", "village_1")
        connect(nodes, "village_1", "elite_1")
        connect(nodes, "town_1", "camp_2")

        connect(nodes, "elite_1", "boss_1")
        connect(nodes, "camp_2", "boss_1")

        return nodes
    }

    private fun connect(nodes: List<CampaignNode>, fromId: String, toId: String) {
        nodes.find { it.id == fromId }?.connections?.add(toId)
    }

    private fun refillRoamingParties(forceFill: Boolean = false) {
        val homes = buildSpawnHomes()
        if (homes.isEmpty()) return
        for (home in homes) {
            val activeForHome = gameState.enemyParties.count { it.homeNodeId == home.homeNodeId && it.faction == home.faction }
            if (activeForHome >= home.maxLinked) continue
            val chance = if (forceFill) 1f else 0.35f
            if (kotlin.random.Random.nextFloat() > chance) continue
            val template = home.templateOptions.randomOrNull() ?: continue
            gameState.enemyParties.add(
                EnemyParty(
                    id = "${home.faction.name.lowercase()}_${home.homeNodeId}_${partyCounter++}",
                    nodeId = home.homeNodeId,
                    homeNodeId = home.homeNodeId,
                    unitTemplates = template,
                    faction = home.faction,
                    maxLinkedFromHome = home.maxLinked
                )
            )
        }
    }

    private fun buildSpawnHomes(): List<PartySpawnHome> {
        val homes = mutableListOf<PartySpawnHome>()
        for (node in campaignMap) {
            if (!node.isRevealed) continue
            if (node.isCleared && isTemporarySpawnHome(node)) continue
            when (node.type) {
                NodeType.ENEMY_PATROL -> {
                    val max = if (node.enemySquads.any { it.unitTypeId == "wolf_pack" }) 3 else 2
                    val templates = listOf(node.enemySquads.ifEmpty { listOf(EnemyTemplate("bandit_thug", 3)) })
                    homes.add(PartySpawnHome(node.id, PartyFaction.HOSTILE, max, templates))
                }
                NodeType.ELITE_CHALLENGE -> {
                    val templates = listOf(node.enemySquads.ifEmpty { listOf(EnemyTemplate("elite_retainer", 2)) })
                    homes.add(PartySpawnHome(node.id, PartyFaction.HOSTILE, 1, templates))
                }
                NodeType.BOSS -> {
                    val templates = listOf(
                        listOf(EnemyTemplate("elite_retainer", 3), EnemyTemplate("militia_guard", 2)),
                        listOf(EnemyTemplate("bandit_archer", 3), EnemyTemplate("militia_guard", 2))
                    )
                    homes.add(PartySpawnHome(node.id, PartyFaction.HOSTILE, 2, templates))
                }
                NodeType.START, NodeType.FACTION_OUTPOST, NodeType.TOWN, NodeType.VILLAGE -> {
                    val friendlyTemplates = friendlyTemplatePool()
                    if (friendlyTemplates.isNotEmpty()) {
                        val max = when (node.type) {
                            NodeType.START -> 2
                            NodeType.FACTION_OUTPOST -> 2
                            else -> 1
                        }
                        homes.add(PartySpawnHome(node.id, PartyFaction.FRIENDLY, max, friendlyTemplates))
                    }
                }
                else -> Unit
            }
        }
        return homes
    }

    private fun friendlyTemplatePool(): List<List<EnemyTemplate>> {
        val mapped = gameState.warband
            .filter { it.count > 0 }
            .map { squad ->
                val roamingCount = (squad.count / 2).coerceIn(2, 6)
                listOf(EnemyTemplate(squad.unitType.id, roamingCount))
            }
        return if (mapped.isNotEmpty()) mapped else listOf(listOf(EnemyTemplate("militia_spear", 3)))
    }

    private fun resolveFriendlyHostileCollisions() {
        val hostileByNode = gameState.enemyParties
            .filter { it.faction == PartyFaction.HOSTILE }
            .groupBy { it.nodeId }
        val friendlyByNode = gameState.enemyParties
            .filter { it.faction == PartyFaction.FRIENDLY }
            .groupBy { it.nodeId }

        if (hostileByNode.isEmpty() || friendlyByNode.isEmpty()) return

        val toRemove = mutableSetOf<String>()
        for (nodeId in hostileByNode.keys.intersect(friendlyByNode.keys)) {
            val hostile = hostileByNode[nodeId].orEmpty()
            val friendly = friendlyByNode[nodeId].orEmpty()
            if (hostile.isNotEmpty() && friendly.isNotEmpty()) {
                toRemove.add(hostile.first().id)
                toRemove.add(friendly.first().id)
            }
        }
        if (toRemove.isNotEmpty()) {
            gameState.enemyParties.removeAll { toRemove.contains(it.id) }
        }
    }

    private fun syncPartiesWithClearedHomes() {
        val clearedHomeIds = campaignMap
            .filter { it.isCleared && isTemporarySpawnHome(it) }
            .map { it.id }
            .toSet()
        if (clearedHomeIds.isEmpty()) return
        gameState.enemyParties.removeAll { party ->
            party.faction == PartyFaction.HOSTILE && clearedHomeIds.contains(party.homeNodeId)
        }
    }

    private fun isTemporarySpawnHome(node: CampaignNode): Boolean = when (node.type) {
        NodeType.ENEMY_PATROL, NodeType.RESOURCE_CACHE, NodeType.ELITE_CHALLENGE, NodeType.RECOVERY_CAMP -> true
        else -> false
    }
}
