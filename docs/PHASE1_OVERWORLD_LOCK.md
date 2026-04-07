# Phase 1 Overworld Lock (Passes 1.9 → 1.20)

This document freezes the production-facing overworld presentation contract so future iteration does not regress into prototype styling.

## Locked travel/readability rules

- Route visuals communicate **both** path type (road/off-road) and tactical risk (safe/threatened/intercept).
- Intercept-grade paths are always upgraded to the risky route treatment, even when a road spline is available.
- Preview labels must remain concise and stateful:
  - `ROAD ROUTE`
  - `THREATENED ROAD`
  - `OFF-ROAD ROUTE`
  - `THREATENED OFF-ROAD`
  - `INTERCEPT RISK ROUTE`
- The committed destination lock marker and movement chevron are the only dominant motion cues during travel.
- Threat vectors are advisory only and should never overpower the selected/targeted route stroke.

## HUD / panel consistency lock

- Top HUD, mode strip, alerts, and bottom panel share one spacing/radius/elevation language sourced from `UiTheme`.
- A single dominant map state is shown at a time (`FOLLOW WARBAND`, `SCOUTING`, route preview state, or `TRAVELLING`).
- Alert queue priority and timing remains:
  - Minor: 1400ms
  - Standard: 1900ms
  - High: 2500ms

## Component family freeze

- Buttons/chips use unified rounded gradient shell and border styling.
- Marker labels and status chips use condensed copy and avoid system/default Android affordances.
- Temporary/prototype map helpers are not permitted in production passes unless explicitly feature-flagged.

## Reference screen checklist

Use this as the canonical Phase 1 validation sweep before Phase 2 work:

1. Overworld idle (follow warband).
2. Scouting camera (manual pan, no target).
3. Preview route (safe).
4. Preview route (threatened).
5. Preview route (intercept risk).
6. Travelling state with stop control.
7. Nearby POI selected with bottom panel expanded.
8. Hostile presence with alert banner.
9. Event/decision panel state.
10. Result/summary panel state.
