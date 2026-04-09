package com.warpath.model

/**
 * Canonical player aircraft task states for the campaign map presentation.
 * Cycle 1 currently drives IDLE / ROUTE_PLANNING / TRANSIT / RECON / HOLDING,
 * while the remaining states are reserved for upcoming gameplay cycles.
 */
enum class AirPlayerState {
    IDLE,
    ROUTE_PLANNING,
    TRANSIT,
    RECON,
    INTERCEPT,
    STRIKE,
    RTB,
    HOLDING,
    DAMAGED_EMERGENCY
}

/**
 * Canonical tactical objects rendered/managed on the campaign map.
 */
enum class AirMapObjectType {
    PLAYER_AIRCRAFT,
    ALLIED_AIRCRAFT,
    UNKNOWN_CONTACT,
    HOSTILE_AIRCRAFT,
    HOSTILE_GROUND_THREAT,
    AIRBASE,
    CARRIER,
    FORWARD_STRIP,
    RECON_TARGET,
    SECTOR,
    NO_FLY_ZONE,
    RADAR_COVERAGE_ZONE,
    THREAT_RADIUS,
    OBJECTIVE_MARKER
}
