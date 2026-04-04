# ROADMAP

## Product goal
Ship a polished Android-first v1 that delivers a repeatable campaign-node + fast battle loop with strong readability and performance.

## Success criteria
- Core loop understood by new players within first session.
- Stable performance on target mid-tier Android hardware.
- High replay intent from short-session users.
- Data-driven content pipeline supports rapid balancing.

## Phase 1: touch-first prototype
Focus:
- One region map slice with limited node types.
- Basic auto-battle with 2–3 intervention commands.
- Minimal progression (warband slots + simple upgrades).
- Foundational touch UI and readability standards.

Exit criteria:
- Loop playable end-to-end without editor intervention.
- Session length consistently in target window.
- Touch controls usable one-handed on common phone sizes.

## Phase 2: vertical slice
Focus:
- Refined battle readability and encounter variety.
- Faction identity pass (colors, silhouettes, behavior differences).
- Reward/progression tuning for retention.
- Save/load reliability and run persistence.

Exit criteria:
- 20–40 minute run feels complete and replayable.
- At least one polished region with boss/rival endpoint.
- Performance budgets met under representative load.

## Phase 3: content expansion
Focus:
- Additional units, node events, and encounter templates.
- Expanded upgrade branches and faction perks.
- Better run modifiers and temporary advantages.
- Early analytics hooks for balancing.

Exit criteria:
- Content variety sustains repeated runs without major fatigue.
- No major readability regressions from added content.
- Data validation pipeline catches config errors early.

## Phase 4: polish/release prep
Focus:
- UX polish, onboarding, and tutorialization.
- Audio/VFX polish within performance limits.
- Device compatibility sweep and bug triage.
- Store build readiness and compliance prep.

Exit criteria:
- Crash-free/stability targets achieved in test cohort.
- Onboarding completion and first-run clarity targets met.
- Release checklist completed for Android submission.

## Mobile optimization pass
Perform at end of each phase:
- Profile CPU/GPU/memory.
- Cut overdraw and heavy effects.
- Validate battery/thermal behavior in long sessions.
- Re-check UI legibility on low-density and small screens.

## Cut list if scope grows
If schedule risk increases, cut in this order:
1. Extra biome/theme variants.
2. Non-critical ability VFX complexity.
3. Rare encounter edge-case content.
4. Secondary progression tracks.
5. Any feature that harms performance/readability.

## Milestone review questions
- Is the core loop still fun in <10 minute sessions?
- Did new content reduce readability?
- Are controls still intuitive without tutorial reliance?
- Are performance and battery goals still met?
- Did we add complexity that violates v1 scope cuts?

## Production rules
- One strong loop first, expansion second.
- Any new feature must justify mobile readability and perf cost.
- Prefer data/config additions over system rewrites.
- Keep mechanics explainable in one short tooltip.

## Release definition
v1 release requires:
- Stable Android build pipeline.
- Complete single-region progression loop.
- Reliable save/load.
- Acceptable performance on target devices.
- No blockers in core touch interaction or battle readability.

## Assumption lock (roadmap gates)
- Every milestone is Android-first and validated on target phone hardware.
- Camera/readability constraints are not negotiable within v1.
- Multiplayer, deep simulation, and siege-scale features are post-v1 only.
- Release can slip before violating battery/performance/readability priorities.
