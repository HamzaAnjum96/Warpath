# APK build status

## v1.3.0 - Fog-of-War Discovery + UI Pass
**Status: TARGETED IN THIS PASS**

Scope for v1.3.0:
- POIs reveal when the warband scouts near them (fog-of-war style)
- route-chain gating removed for most POIs
- elite fight intel can reveal hideout location
- updated campaign visuals and interaction copy for clarity

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
