# ARCHITECTURE — Warpath

## Architecture goals
- Keep the travel -> encounter -> settlement loop easy to evolve.
- Preserve clear boundaries between overworld, encounters, combat, and progression.
- Maintain mobile readability and performance as first-class constraints.
- Keep systems simple enough for rapid balancing during concept validation.

## High-level modules
- `Core`: shared models, constants, utility functions
- `World`: live overworld movement, parties, destinations, time flow
- `Encounter`: detection/interception and Battle/Run/Bribe decision flow
- `Battle`: short real-time tactical resolution and command handling
- `Settlement`: rest/recruit/supplies/missions menu systems
- `Progression`: warband growth, mission rewards, loss application
- `Economy`: gold/supplies costs, bribe formulas, upkeep pressure
- `State`: save/load, run snapshot, migration/versioning
- `UI`: portrait layouts, joystick/action controls, decision screens

## Core data entities
- `Warband` (troops, readiness, mobility, capacity)
- `TroopStack` (type, count, condition)
- `Party` (hostile/friendly neutral parties in overworld)
- `Settlement` (town/village, recruit pools, missions, pricing)
- `Mission` (objective, constraints, reward, failure consequences)
- `EncounterContext` (opponent, terrain/modifiers, risk state)
- `Resources` (gold, supplies, time)

## Overworld flow
1. Advance world simulation time.
2. Move player warband from joystick input.
3. Update moving parties and nearby threat/opportunity signals.
4. Trigger encounter when collision/interception occurs.
5. Route player to encounter decision or settlement menus.

## Encounter flow
1. Build encounter context.
2. Show Battle/Run/Bribe panel.
3. Resolve chosen option:
   - Battle -> battle subsystem
   - Run -> escape resolver (success/partial/fail)
   - Bribe -> economy transaction + relationship/position effects
4. Apply results to world + progression state.

## Battle flow
1. Spawn forces from encounter context.
2. Run short real-time simulation.
3. Process player command set (move commander, charge, hold, retreat, focus).
4. Resolve victory/defeat/withdrawal.
5. Return summary payload (losses, loot, time cost, effects).

## Settlement flow
1. Enter town/village menu (no interior traversal).
2. Offer context-appropriate actions:
   - Heal/Rest
   - Get Units
   - Get Supplies
   - Get Missions
3. Apply costs, time progression, and availability constraints.
4. Return to overworld with updated state.

## State/update rules
- UI emits intents/commands; gameplay systems own state mutation.
- Keep outcome payloads explicit between systems (encounter -> battle -> progression).
- Avoid hidden cross-module side effects.
- Favor deterministic resolvers for run/bribe outcomes where possible.

## Mobile constraints in architecture
- Portrait-first layout assumptions
- One-handed interaction zones
- Minimal screen-state depth for fast decisions
- Tight caps on active combat entities/effects
- No per-frame allocation spikes in hot paths

## Testing priorities
- Encounter option resolution correctness (battle/run/bribe)
- Resource accounting integrity (gold/supplies/time)
- Settlement action correctness and availability rules
- Overworld-to-encounter-to-settlement transition stability
- Frame pacing and input responsiveness on target devices

## Scope guardrails
- Single-player only
- No deep macro simulation (politics/dynasty/economy)
- No settlement interior scene architecture
- No large-battle/siege infrastructure in initial versions
