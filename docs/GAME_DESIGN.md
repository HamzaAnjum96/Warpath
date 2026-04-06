# GAME_DESIGN — Warpath

## One-line summary
**Warpath is a portrait-mode Android strategy RPG where you lead a roaming warband across a living world, recruit troops, manage supplies, take missions, and decide when to fight, flee, or bribe your way through danger.**

## Design goals
1. **Clear decisions on mobile**: every major choice is understandable in seconds.
2. **Strong momentum**: "one more stop, one more mission" pacing.
3. **Tension through survival pressure**: troops, gold, supplies, and time always matter.
4. **Readable tactical action**: real-time battles that stay short and legible.

## Core fantasy
Start vulnerable and gradually become a feared, mobile warband leader.
Power comes from:
- smart movement,
- calculated risks,
- repeated survival,
- and disciplined resource management.

Tone: **rugged, grounded, dangerous frontier**.

## Core loop
1. Travel in real time across overworld.
2. Discover danger/opportunity/destination.
3. Trigger party encounter or enter settlement.
4. Choose response (battle/run/bribe or hub actions).
5. Resolve consequences.
6. Gain/lose troops, gold, supplies, and time.
7. Recover/recruit/restock/take missions.
8. Repeat.

## Overworld travel
- Primary play space.
- Real-time movement and interception.
- Tension between safe routing vs fast routing.

### Portrait controls
- **Left thumb:** virtual movement joystick.
- **Right thumb:** compact actions (party, supplies, missions, time speed).

### Travel decisions
- Where to go next.
- What to avoid.
- Whether to risk another encounter.
- When to return to settlement safety.

## Encounters
Collision/intercept opens a fast decision panel.

### Player options
- **Battle** — high risk/high reward.
- **Run** — chance-based escape outcome (full/partial/fail).
- **Bribe** — spend resources to avoid conflict.

## Battle model
- **Real-time** tactical combat.
- Target duration: **30 seconds to 3 minutes**.
- Must remain readable on phone screens.

### Command set (core)
- Move commander
- Charge
- Hold
- Retreat
- Focus target

## Settlements (menu-based hubs)
No interior walking scenes.

### Towns
- stronger recruits
- broader supplies
- better recovery
- richer missions

### Villages
- basic recruits
- cheaper limited supplies
- local missions
- basic recovery

### Settlement actions
- Heal/Rest (advances time)
- Get Units
- Get Supplies
- Get Missions

## Progression model
Primary growth axes:
- Party Size
- Troop Quality
- Economy (gold flow)
- Mobility
- Warband Stability (healing/supplies/readiness)

## Resource model
- **Gold**: recruits, supplies, bribes, recovery
- **Supplies**: sustain movement/combat readiness
- **Troops**: force strength and survival proxy
- **Time**: hidden strategic cost across all actions

## Risk and consequence
Setbacks include:
- casualties/wounds,
- supply losses,
- gold drain,
- mission failure,
- weak map position,
- forced retreat.

## UX direction
- Minimal, modern, high-contrast UI
- Thumb-first interactions
- Icon-led fast actions
- Low clutter + low text density

## Visual direction
- Low poly
- Clean silhouettes
- Stylized but readable
- Android performance-conscious

## Session targets
- **Short (3–8 min):** quick travel + recruit/restock + mission pickup
- **Medium (10–20 min):** mission run with encounters and recovery
- **Long (30+ min):** chained missions with clear warband growth

## Scope boundaries (initial)
- Android-first
- Portrait orientation
- Single-player
- No deep political/dynasty simulation
- No settlement interior walking
- No large-scale siege-style battles in initial versions
