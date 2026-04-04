# Warpath

Warpath is a mobile-first strategy/action game concept inspired by the high-level fantasy of Bannerlord, re-scoped into a tight loop built for short Android play sessions.

## Vision
**"Polytopia to Civ, but for Bannerlord."**

Design priorities:
- Mobile readability over realism
- Fast tactical clarity over large simulation depth
- Performance and battery life over feature sprawl

## Planning Documents
- [Game Design](planning/GAME_DESIGN.md)
- [Tech Stack](planning/TECH_STACK.md)
- [Architecture](planning/ARCHITECTURE.md)
- [Roadmap](planning/ROADMAP.md)

## Core Constraints (v1)
- Android first
- Unity + C#
- Top-down isometric camera
- Low-poly art style only
- No multiplayer
- No deep political/economy simulation
- No town-walking or first-person scenes
- No Bannerlord-scale battles in early phases


## Scope Lock
These constraints are repeated across planning docs and are treated as non-negotiable for v1 unless explicitly re-approved.

## Android Prototype (Hello World)
A minimal native Android app scaffold has been added under `app/`.

### Build target
- Output path: `APK/Warpath_v0_0_1.apk`

### Build command
```bash
./scripts/build_apk.sh
```

> Note: this environment currently cannot download Android SDK/Gradle dependencies due network proxy restrictions, so `APK/Warpath_v0_0_1.apk` must be generated on a machine with Android network access.
