# decisions
- Use Compose and Material 3; enable edge-to-edge; select dynamic color on Android 12+ with local light/dark fallback; keep the first release splash-only while future flows follow the Figma system and its offline-first, no-fake-claims privacy stance.
- Splash flow decision: use a 700ms fade-in, hold for 2 seconds, then navigate with NavHost from splash to Home.
- Home decision: show `Not scanned yet` before the first scan, launch Scanner from the 168 dp circular primary action, and show real device totals after a completed scan.
- Home information architecture uses Storage, Trash DNA, Privacy Monitor, and Reports cards; Settings remains a bottom-navigation destination rather than a content card.
- Bottom navigation exposes the planned Home, Privacy, Reports, and Settings information architecture; unfinished destinations use clear placeholder copy.
- Keep AndroidX dependencies on API 36.1-compatible versions until the project intentionally adopts compile SDK 37.
- Scanner MVP uses Android Storage Access Framework folder selection instead of `MANAGE_EXTERNAL_STORAGE`; persisted read permission is used only for the selected tree.
- Completed scan results are kept in memory for the active app session and contain metadata only. No automatic deletion or collection is implemented.
