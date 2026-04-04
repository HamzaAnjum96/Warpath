# GAME_DESIGN

## Game pillars
1. **Readable mobile tactics**: Every decision should be understandable at a glance on a phone screen.
2. **Short, replayable sessions**: Runs should feel meaningful in 5–12 minutes.
3. **Strategic progression with simple inputs**: Depth comes from choices, not control complexity.
4. **Performance-first combat fantasy**: Capture warband command fantasy without large-scale simulation.

## Player fantasy
You are a rising warband commander expanding influence across a compact region. You recruit, upgrade, and direct small troop groups, winning quick encounters and shaping faction control.

## Core gameplay loop
1. Choose next node on campaign map.
2. Resolve event (battle, ambush, caravan raid, reinforcement opportunity, etc.).
3. Enter small battle encounter (mostly auto-battle + light commands).
4. Earn rewards (troops, upgrades, standing, control).
5. Spend and optimize warband.
6. Push toward regional control objective.

## Session structure
- **Session length target**: 5–12 minutes.
- **Run length target**: 20–40 minutes split across sessions.
- **Micro-flow**:
  - 30–60s campaign decision
  - 60–180s battle
  - 15–45s reward/upgrades

## Campaign layer overview
- Compact region map made of connected nodes.
- Node types:
  - Enemy patrol
  - Resource cache
  - Elite challenge
  - Recovery camp
  - Faction outpost
- Player selects routes to balance risk/reward and faction influence.
- Control of key nodes contributes to regional dominance progress.

## Battle layer overview
- Small-scale encounters (roughly 8–30 active units total in early phases).
- Core model: **auto-engagement with intervention windows**.
- Player can issue lightweight commands:
  - Focus target
  - Push/hold stance
  - Timed ability trigger
  - Retreat/reposition ping
- Encounters must resolve quickly and read clearly.

## Touch controls overview
- Tap to select squad/hero.
- Tap enemy to set focus target.
- Drag path arrow for reposition command.
- Large contextual action buttons for abilities.
- Long-press for unit info card.
- Input budget: no gesture-heavy complexity, no precision micro required.

## Unit/troop categories
- **Frontline**: shield infantry, spears.
- **Skirmish**: javelin/light ranged.
- **Ranged**: bow/crossbow archetypes.
- **Cavalry-lite**: fast flanking units (kept limited for readability).
- **Support**: banner/medic/buffer style specialists.

## Enemy categories
- Bandit raiders (aggressive, low armor).
- Militia blocks (defensive, high numbers).
- Elite retainers (small but high threat).
- Beast/rogue encounter variants (unpredictable tempo events).

## Power-ups / temporary battle advantages
- Pre-battle consumables and encounter buffs:
  - Rally Horn (temporary attack speed boost)
  - Field Rations (small heal over time)
  - Smoke Pots (enemy accuracy reduction)
  - Reinforcement Call (spawn 1 temporary allied squad)
- Designed as run-based tactical spikes, not permanent progression replacement.

## Progression systems
- **Warband growth**: unlock squad slots and composition flexibility.
- **Troop upgrades**: branch-based upgrades with simple tradeoffs.
- **Faction standing**: unlock faction-specific recruits/perks.
- **Map control**: persistent region control bonuses and win progression.

## Rewards / economy-lite
- Soft currencies only in v1:
  - Supplies (healing/recovery/recruitment)
  - Renown (meta progression unlocks)
- Rewards granted after nodes and milestone captures.
- No deep market simulation, no dynamic commodity pricing.

## Loss / failure rules
- If core commander unit is defeated or warband morale collapses, encounter is lost.
- On run failure:
  - Keep partial renown/meta progress.
  - Lose temporary run-only buffs/resources.
- Failure should sting but encourage immediate replay.

## Win state / end goal
- Capture/secure required strategic nodes and defeat regional rival commander.
- Full release can add multiple regions; early versions focus on one polished region loop.

## UX rules for mobile readability
- Strong silhouettes and bold faction colors for instant differentiation.
- Minimal UI clutter; hide secondary stats behind long-press panels.
- Large tap zones; avoid tiny icons and precision drag requirements.
- Combat readability > visual effects density.
- Preserve frame consistency on mid-tier Android devices.

## Explicit scope cuts / non-goals
- No multiplayer in v1.
- No open world exploration.
- No town walking/first-person interaction scenes.
- No marriage/dynasty/political simulation depth.
- No deep economy sim.
- No large siege systems in early phases.
- No Bannerlord-sized battles.

## Assumption lock (must remain true in v1)
- Android-first development and UX validation.
- Low-poly art only; no realistic asset direction.
- Fixed top-down isometric combat camera only.
- No multiplayer in v1.
- No deep economy/politics simulation layers.
- No town walking/first-person scenes.
- No marriage/dynasty complexity.
- No large siege systems in early phases.
- No Bannerlord-scale battle counts.
- Prioritize readability, battery life, and performance over realism.
