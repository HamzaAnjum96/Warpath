# TECH_STACK

## Final engine recommendation: Unity
Unity is the recommended engine for v1 due to mature Android tooling, strong 2D/3D hybrid workflows, and rapid iteration support for small teams.

## Why Unity fits Android-first development
- Stable Android export pipeline.
- Robust profiling tools for CPU/GPU/memory tuning.
- Large ecosystem for UI, tooling, and mobile optimization patterns.
- Fast prototyping with prefabs and ScriptableObject-driven content.

## Language decision: C#
- Native Unity workflow language.
- Good balance between developer speed and maintainability.
- Strong support for data-driven patterns and editor tooling.

## Rendering decision: low-poly 3D isometric
- 3D low-poly assets with fixed top-down isometric camera.
- Simple lighting and restrained post-processing for clarity and battery.
- Prioritize silhouette readability over material complexity.

## Platform target decision: Android first
- Primary target: mid-range Android devices.
- Initial optimization budgets defined by phone-first constraints.
- iOS/PC ports considered only after v1 loop proves retention/performance.

## Input decision: touch-first UI/control layer
- UI built for thumb zones and large targets.
- Interaction model centered on tap/select/drag.
- No dependence on controller/keyboard for core gameplay.

## Data format decision
- **Primary authoring**: Unity ScriptableObjects for units/factions/encounters/upgrades.
- **Runtime serialization**: JSON (or binary wrapper later) for saves and telemetry snapshots.
- **Balancing tables**: CSV import pipeline optional after prototype.

## Save system approach
- Local save slots with lightweight run-state and meta-state separation.
- Versioned save schema for safe iteration.
- Autosave at node transitions and post-battle reward screens.

## Asset pipeline decision
- Low-poly modular kit pieces.
- Shared materials/atlases to reduce draw calls.
- Animation scope kept small and readable (short cycles, clear anticipation frames).

## Tooling/editor decision
- Unity Editor custom inspectors for data validation.
- In-editor debug overlays for AI state, aggro, and DPS contribution.
- Automated config validation scripts in CI later.

## Build/export approach
- Start with local Android APK/AAB builds.
- Standardized local build profiles (debug/perf/release candidate).
- CI/CD introduced after core loop stabilizes.

## Mobile optimization constraints
- Consistent frame pacing prioritized over peak fidelity.
- Strict spawn/unit count caps in battle.
- Bounded shader complexity and overdraw.
- Memory budgets enforced for low/mid-tier Android devices.

## CI/CD plan later
- Phase 1/2: local builds and manual smoke checks.
- Phase 3+: cloud CI for automated builds/tests/config validation.
- Phase 4: release pipeline hardening with signing/version automation.

## Alternatives considered and rejected
- **Unreal Engine**: strong visuals but heavier iteration overhead for this scope.
- **Godot**: promising, but Unity mobile pipeline/tool familiarity better fits fast delivery.
- **Custom engine**: rejected due to high risk and low leverage for v1.

## Main technical risks
1. Battle readability degrading as content expands.
2. Performance drops from uncontrolled content complexity.
3. Data drift without strict validation tooling.
4. Touch UX regressions when desktop assumptions leak into design.

## Assumption lock (technical guardrails)
- Android is the only release target for v1 quality gates.
- Art/rendering remains low-poly with fixed isometric camera framing.
- Touch-first interaction is mandatory for all core actions.
- Single-player only in v1 (no netcode investment).
- Economy/politics simulations remain intentionally shallow.
- No first-person/town-walking scene stack in v1 architecture.
- Siege-scale systems are out of scope before post-v1 review.
- Unit counts are intentionally constrained for readability and battery.
