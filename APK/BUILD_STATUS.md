# APK build status

## v1.1.0 - POI Interaction Movement Update
**Status: TARGETED IN THIS PASS**

Scope for v1.1.0:
- free joystick movement as direct character control
- no path/edge rendering between map points
- POIs are proximity interactions (no travel button)
- POI popup uses direct interact action when in range

## v1.0.1 - Phase 1 Minimal POC
**Status: COMPLETE**

Scope for v1.0.1 was intentionally minimal:
- app launches to portrait main menu
- `Start` routes directly to overworld
- joystick movement + node travel + exploration reveal

## v1.0.0 - Foundation Menu Build
**Status: COMPLETE**

Scope for v1.0.0:
- app launches to a simplified portrait main menu
- `Start` routes to How To Play

## Available committed artifacts
- `Warpath_v0_0_1.apk` — initial Android scaffold
- `Warpath_v0_5_0.apk` — touch-first prototype
- `Warpath_v0_6_0.apk` — live map movement + UI upgrade

## Naming note
App/runtime branding is now Sarhad. Historical APK filenames remain unchanged for existing artifacts.

## Rebuild command
```bash
./scripts/build_apk.sh
```
