# context

## Product context

- TrashPilot is a minimal, privacy-first Android cleaning app.
- Figma principles: offline first, one primary action per screen, and no fake optimization claims.
- Current UI: Splash routes to a premium Home screen with brand header, circular Scan action, feature cards, and bottom navigation.

## Design scope

- Home visual source of truth: file key `XyDczeNnzEw5DmApIXuXTz`, node `1:65`
  (`Android Compact - 1`). Ignore all older Home files, nodes, and purple references.
- Figma design system covers foundations, components, patterns, screens, and prototype flows.
- Planned screens: Splash, Home, Scanner, Results, Privacy, and Settings.
- Splash implementation: white Material 3 surface with centered TP mark, TrashPilot wordmark, and tagline; fades in over 700ms and routes to Home after 2 seconds.
- Settings is an accessible placeholder route with a top app bar, back navigation, and temporary explanatory copy.
- Home shows Storage as `Not scanned yet` until real on-device scan data exists; no percentages or illustrative storage values are displayed.
- Scan immediately analyzes shared-storage records Android exposes through MediaStore, shows
  foreground progress, and navigates to Results. The system folder picker remains only as a
  fallback when Android requires explicit access to a location.
- Results show device total, used, and free storage; category totals; and up to ten largest accessible files.
- Scanning is read-only. TrashPilot does not upload, collect, automatically delete, or modify files.
- Home cards are Storage, Trash DNA, Privacy Monitor, and Reports. Settings remains in bottom navigation only.
- Home bottom navigation links to Home, Privacy, Reports, and Settings; Privacy and Reports currently show honest placeholder destinations.
- Figma Scanner target: node `27:2`; Results target: node `27:3` on `04_Screens`.
- Results follows Figma node `27:3`: a scrollable 412-wide layout with 24 dp margins,
  a lavender device-storage card, rounded category/file surfaces, and a 52 dp Scan again action.
- Results displays only the active in-memory scan session: selected folder, file count, real
  device totals, category totals, and the ten largest readable files.
- Category rows open read-only lists backed by the same session. The ten-largest set can be
  reordered by size, name, or last-modified date.
- Quick Clean Figma Overview: node `39:2`; Review Items: `39:5`; Confirmation: `39:8`;
  Cleaning Report: `39:11`, all on page `18:2` and validated as 412 × 917 frames.
- Quick Clean uses only conservative disposable candidates discovered in the current in-memory
  scan. Personal photos, videos, audio, documents, and Downloads are never candidates.
- Trash DNA Figma Overview: node `46:328`; Habit Insights: `46:329`; History: `46:330`,
  all on page `18:2` and validated as 412 × 917 frames.
- Trash DNA analyzes only locally persisted scan and cleanup metadata generated inside
  TrashPilot. Its overview and insights remain honestly empty until enough history exists.
- Trash DNA persists aggregate used-storage, media, download, messenger, screenshot, large-file,
  hidden-file, and cache totals for completed scans. It shows one deterministic profile, latest
  storage/category trends, one insight, one recommendation, and chronological scan history after
  two qualifying scans; earlier schema rows remain stored but are not treated as complete metrics.
- Privacy Monitor Figma Overview: node `51:2`; Permission Categories: `51:3`;
  Apps With Sensitive Permissions: `51:4`; Permission Details: `51:5`;
  Recommendations: `51:6`, all on page `18:2` and validated as 412 × 917 frames.
- Privacy Monitor reads current installed-package permission declarations from Android only.
  Counts, app rows, grant status, and recommendations are derived from the current device.
- Privacy Monitor now opens directly to a sortable installed-app list. Every row shows the real
  application icon and Camera, Microphone, Location, Contacts, Photos / Storage, Notifications,
  Accessibility, and Background activity status; tapping a row opens factual app details.
- Reports Figma Overview: node `56:2`; Scan History: `56:3`; Cleaning History: `56:4`;
  Storage Trends: `56:5`; Export Report: `56:6`, all on page `18:2` and validated
  as 412 × 917 frames.
- Reports combines real locally recorded scan, Quick Clean, Trash DNA, and Privacy Monitor
  metadata. Charts remain empty until recorded sessions provide actual points.
- Settings Figma Overview: node `60:2`; Appearance: `60:3`; Language: `60:4`;
  Data & Storage: `60:5`; Privacy: `60:6`; About: `60:7`; Pro Upgrade: `60:8`,
  all on page `18:2` and validated as 412 × 917 frames.
- Settings provides local theme and language preferences, app-only cache/history controls,
  metadata-only backup/restore and diagnostics, bundled policy/terms copy, feedback handoff,
  version information, and an honest unavailable Pro preview.
- Settings Language node `60:4` supports 25 locales plus System language, native-name search,
  immediate locale application, and DataStore-backed selection.
- The complete UI string catalog now contains 381 identical keys in English plus 24 localized
  resource directories: Spanish, Brazilian Portuguese, French, German, Italian, Polish,
  Ukrainian, Russian, Turkish, Arabic, Hindi, Bengali, Indonesian, Vietnamese, Thai, Japanese,
  Korean, Simplified Chinese, Traditional Chinese, Dutch, Swedish, Czech, Romanian, and Greek.
- Non-English translations are automated release-candidate drafts. They have passed structural,
  formatting-token, build, lint, and selected-device layout checks, but they have not been
  reviewed or approved by native-language translators and must not be represented as such.
- Locale validation on the 412 × 917 emulator covered English, Ukrainian, Polish, German,
  Spanish, Arabic, Simplified Chinese, and Japanese. All captured Settings hierarchies stayed
  within screen bounds; Arabic correctly mirrored top navigation and row/action alignment.

## Stabilization status

- Release-candidate stabilization completed on 2026-07-29 against a 412 × 917 emulator.
- Main navigation, nested system-back behavior, real storage scanning, Results, Quick Clean,
  Trash DNA, Privacy Monitor, Reports, Settings, and light/dark/system theme persistence were
  exercised without an observed runtime crash.
- A user-selected fallback SAF tree retains read and write grants across app restart. Scan results remain
  intentionally in memory and require a new scan after process recreation.
- Remaining release blocker: automated Compose navigation/UI coverage is not yet present;
  the current screen and back-stack validation is manual. Platform-induced database, package
  manager, and document-provider error states were code-reviewed but not fault-injected on-device.

## Protected localization validation

- Immutable names across all locales: SCAN, TrashPilot, Trash DNA, Quick Clean, Privacy Monitor, Reports, Settings, and Smart Cleaner.
- Home was revalidated at 412 x 917 in English, Polish, Ukrainian, German, Spanish, Arabic, Simplified Chinese, and Japanese; it exposes seven protected names together. Quick Clean is enforced by exact catalog verification.
- The non-English catalogs remain automated release-candidate drafts requiring native review.

## Approved visual baseline

- The approved 412 × 917 Home frame is the visual baseline for the complete app.
- Shared UI uses a fixed light palette, 24 dp page gutters, 24 dp card corners, and an
  always-visible 80 dp bottom navigation bar.
- System bars are immersive so the approved frame coordinates map directly to emulator pixels.
- Phase 1 centralized the current visual implementation without changing approved output.
  Deterministic 412 × 917 Home, Scanner, and Settings screenshots remained byte-identical.
- Home visual evolution keeps the approved header, 168 dp SCAN hero, palette, rounded surfaces,
  whitespace, and persistent navigation while presenting Storage compactly, four secondary quick
  actions, and three factual local-control assurances.



## Home Ambient Message modernization

- Home uses the approved blue concentric logo, 200 dp double-ring SCAN hero, real-data phone
  storage card, five vertical feature cards, and Home-only icon navigation treatment.
- A reusable non-interactive Ambient Message sits beneath SCAN and crossfades localized phrases.
- Compact validation passed at 412 × 917 for English, German, and Arabic; German remains
  scrollable without clipped controls at 200% font scale.

## Scan Results Home-language implementation

- Scan Results reuses `TrashPilotBrandHeader`, `TrashPilotHomeCard`,
  `TrashPilotFeatureCard`, Home color roles, and the Home bottom-navigation treatment.
- Total scanned bytes, accessible cache, files at least 100 MB, hidden-path bytes, and readable
  empty-folder count are derived from the active scan only.
- Social & Messenger Media uses accessible media paths for supported messaging/social apps and
  opens a read-only detail list. Duplicate hashing is a separate explicit local operation.
- Results selection is limited to discovered removable cache and empty-folder candidates;
  `Clean Selected` stays disabled until a candidate category is selected and carries that
  selection into the existing Quick Clean review.
- `Clean Selected` is the sole primary action and enters the existing manual Quick Clean review.
- The foreground scanner reports only stages when their real local analysis begins. Leaving the
  scanner cancels its composition-bound coroutine; Back is never blocked.
- The default scanner requests Android scoped media-read permissions when no supported media
  access has been granted. Partial grants are respected without repeated prompting; broad
  all-files access is never requested.
- A completed scan with no removable candidates, large files, hidden paths, or supported social
  media uses the positive `Your storage is already clean` state instead of zero-valued cards.
