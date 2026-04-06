# TECH_STACK — Warpath

## Current implementation direction
Warpath is being built as an Android-first portrait game prototype in this repository.

## Platform target
- **Primary:** Android phones
- **Orientation:** Portrait-first UX
- **Performance target:** Mid-tier Android devices

## Language/runtime
- **Kotlin** for Android app/gameplay prototype logic
- **Android SDK** rendering/UI stack for early iteration

## Why this stack fits current phase
- Fast loop iteration for mobile UI and control feel
- Direct control over Android-specific performance constraints
- Lower integration overhead for early gameplay validation

## Input model
- Touch-first controls
- Left-thumb movement joystick
- Right-thumb compact action buttons
- Large touch targets and low text density

## Data and state model (initial)
- Lightweight in-app data structures for troops, parties, encounters, missions
- Save-state/versioning can remain minimal in early slices, then formalize as the loop stabilizes

## Build and packaging
- Gradle-based Android build pipeline
- Debug APK output for rapid device testing
- CI pipeline artifacts for sharing release/versioned APK builds

## Performance guardrails
- Keep battles short and unit counts constrained
- Avoid heavy visual effects that hurt readability/frame pacing
- Prioritize smooth input + deterministic responsiveness over visual complexity

## Explicit non-goals in current phase
- No multiplayer/network stack
- No deep economy/politics simulation systems
- No settlement interior scene stack
- No large-scale siege/battle simulation

## Future stack reassessment trigger
Reassess engine/runtime choices only if:
- team/content scale exceeds native prototype maintainability,
- tooling needs outgrow current workflow,
- or performance/readability goals cannot be met with current architecture.
