# ARCHITECTURE

## Architecture goals
- Fast iteration for gameplay tuning.
- Clear separation of campaign, battle, and meta systems.
- Data-driven content with minimal hardcoded balance values.
- Deterministic-enough battle resolution for debugging and player trust.
- Mobile-first performance discipline.

## High-level modules
- `Core` (shared primitives, IDs, stat blocks, utility systems)
- `Data` (ScriptableObject definitions and validation)
- `Campaign` (node graph, progression, event selection)
- `Battle` (encounter sim, squads, abilities, resolution)
- `AI` (decision policies and state machines)
- `Input` (touch translation + command dispatch)
- `UI` (HUD, overlays, rewards, progression)
- `Save` (serialization, versioning, persistence)
- `Telemetry` (lightweight event hooks)

## Folder/project structure
Suggested Unity layout:
- `Assets/_Project/Core`
- `Assets/_Project/Data`
- `Assets/_Project/Campaign`
- `Assets/_Project/Battle`
- `Assets/_Project/AI`
- `Assets/_Project/Input`
- `Assets/_Project/UI`
- `Assets/_Project/Save`
- `Assets/_Project/Tools`

## Code layering rules
- UI never mutates data models directly; it dispatches commands.
- Campaign and Battle communicate via explicit result payloads.
- Data definitions are immutable at runtime (instance state stored separately).
- Avoid cross-module singletons except tightly constrained service locators.

## Core entity model
- `Commander`: hero/profile, abilities, progression hooks.
- `Squad`: runtime group with troop archetype + count + morale + status.
- `Encounter`: battle context (terrain tag, enemy composition, modifiers).
- `Node`: campaign map unit with type, rewards, and connections.
- `FactionStanding`: relationship values unlocking content/perks.

## Data definition model
Use ScriptableObject configs for:
- Unit archetypes
- Ability definitions
- Encounter templates
- Reward tables
- Upgrade branches
- Faction profiles

Each definition uses stable IDs for save compatibility and analytics.

## Campaign system flow
1. Load run state and region graph.
2. Present available connected nodes.
3. Resolve pre-battle event/effects.
4. Build encounter request from node + modifiers.
5. Transition to battle scene/context.
6. Receive outcome payload and apply rewards/losses.
7. Check win/loss/end conditions.

## Battle system flow
1. Instantiate squads from encounter definition.
2. Run auto-battle simulation tick.
3. Open intervention windows on cooldown/event triggers.
4. Apply player command effects.
5. Resolve morale/casualties/objective state.
6. Produce battle summary payload for campaign layer.

## Input/touch handling layer
- Converts raw touch events into semantic commands:
  - `SelectSquadCommand`
  - `FocusTargetCommand`
  - `MovePingCommand`
  - `ActivateAbilityCommand`
- Includes tap target expansion and gesture timeout safeguards.

## AI system split
- **Strategic AI (campaign)**: node choice bias, aggression posture.
- **Tactical AI (battle)**: target selection, spacing, ability timing.
- Shared utility scoring for consistency across factions.

## AI state machine outline
Per-squad tactical states:
1. `Advance`
2. `Engage`
3. `Hold`
4. `Flank`
5. `Retreat`
6. `Rally`

Transitions driven by morale, distance bands, threat score, and commander directives.

## Combat resolution model
- Hybrid real-time tick with deterministic priority ordering.
- Damage pipeline: hit check -> mitigation -> morale impact -> casualty update.
- Morale is a first-class outcome driver to keep encounters readable and fast.

## Event/message system
- Lightweight event bus for decoupled updates:
  - Battle started/ended
  - Squad defeated/rallied
  - Ability activated
  - Node captured
- Keep event schema explicit and versioned where needed.

## Save/load architecture
- Separate files/sections:
  - Meta progression profile
  - Active run snapshot
  - Settings/preferences
- Use version headers + migration handlers for schema evolution.

## Balancing/config pipeline
- Author balance in ScriptableObjects.
- Run validation pass to catch invalid links, out-of-range values, and missing IDs.
- Export lightweight balance snapshots for design review.

## Mobile performance constraints
- Hard cap concurrent units/effects per encounter.
- Object pooling for squads/projectiles/VFX.
- Throttled UI refresh for non-critical stats.
- Avoid per-frame allocations in combat hot paths.

## Testing strategy
- Unit tests for combat math and reward calculations.
- Integration tests for campaign->battle->campaign transitions.
- Device smoke tests on representative Android tiers.
- Regression checklist for touch responsiveness/readability.

## Non-goals / avoid overengineering
- No ECS migration in early phases unless profiling proves necessity.
- No fully generic behavior-tree framework before concrete need.
- No networked architecture assumptions for v1 (single-player only).

## Assumption lock (architecture scope)
- Keep architecture single-player and offline-first for v1.
- Preserve small-encounter battle limits as a hard systems constraint.
- Do not introduce modules for town walking, dynasty, or deep politics.
- Do not introduce multiplayer abstractions unless scope is formally changed.
- Treat readability/performance regressions as architecture bugs, not polish tasks.
