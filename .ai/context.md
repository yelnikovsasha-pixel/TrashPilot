# context

## Product context

- TrashPilot is a minimal, privacy-first Android cleaning app.
- Figma principles: offline first, one primary action per screen, and no fake optimization claims.
- Current UI: Splash routes to a premium Home screen with brand header, circular Scan action, feature cards, and bottom navigation.

## Design scope

- Default Figma implementation target: Home / Compose Sync, file key `GIpl7h9YLIvYnAavxMoGGz`, node `21:2` on page `18:2` (`04_Screens`).
- Source URL: https://www.figma.com/design/GIpl7h9YLIvYnAavxMoGGz/TrashPilot-Design-System?node-id=21-2
- Figma design system covers foundations, components, patterns, screens, and prototype flows.
- Planned screens: Splash, Home, Scanner, Results, Privacy, and Settings.
- Splash implementation: white Material 3 surface with centered TP mark, TrashPilot wordmark, and tagline; fades in over 700ms and routes to Home after 2 seconds.
- Settings is an accessible placeholder route with a top app bar, back navigation, and temporary explanatory copy.
- Home shows Storage as `Not scanned yet` until real on-device scan data exists; no percentages or illustrative storage values are displayed.
- Scan opens an explicit system folder picker, recursively inspects readable files offline, and navigates to Results.
- Results show device total, used, and free storage; category totals; and up to ten largest accessible files.
- Scanning is read-only. TrashPilot does not upload, collect, automatically delete, or modify files.
- Home cards are Storage, Trash DNA, Privacy Monitor, and Reports. Settings remains in bottom navigation only.
- Home bottom navigation links to Home, Privacy, Reports, and Settings; Privacy and Reports currently show honest placeholder destinations.
- Figma Scanner target: node `27:2`; Results target: node `27:3` on `04_Screens`.
