# context

## Product context

- TrashPilot is a minimal, privacy-first Android cleaning app.
- Figma principles: offline first, one primary action per screen, and no fake optimization claims.
- Current UI: Splash routes through onboarding state to a focused Home screen whose dominant action is SCAN.

## Design scope

- Home visual source of truth: file key `XyDczeNnzEw5DmApIXuXTz`, node `1:65`
  (`Android Compact - 1`). Ignore all older Home files, nodes, and purple references.
- Figma design system covers foundations, components, patterns, screens, and prototype flows.
- Planned screens: Splash, Home, Scanner, Results, Privacy, and Settings.
- Splash implementation: white Material 3 surface with centered TP mark, TrashPilot wordmark, and tagline; fades in over 700ms and routes to Home after 2 seconds.
- Settings is a production route with grouped Appearance, Language, Privacy & Permissions,
  Data & History, Introduction, and About destinations.
- Home shows Storage as `Not scanned yet` until real on-device scan data exists; no percentages or illustrative storage values are displayed.
- Scan immediately analyzes shared-storage records Android exposes through MediaStore, shows
  foreground progress, and navigates to Results. The system folder picker remains only as a
  fallback when Android requires explicit access to a location.
- Results is the cleaning hub. It shows only real active-scan facts, then groups review destinations
  as Apps, Photos, and Files instead of exposing a flat list of implementation modules.
- Scanning is read-only. TrashPilot does not upload, collect, automatically delete, or modify files.
- Home contains the brand header, dominant SCAN action, ambient message, real phone-storage card,
  and Quick Clean, Trash DNA, and Privacy Monitor actions. Reports and Settings are not duplicated.
- Bottom navigation has four stable top-level destinations: Home, Privacy, Reports, and Settings.
  Nested cleaner routes retain Home as their selected parent; About retains Settings.
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
- Reports combines real locally recorded scan and Quick Clean metadata. Charts remain empty until
  recorded scan sessions provide actual points.
- Reports now presents cleaning history as one summary, an unsmoothed storage-cleaned-per-scan
  chart, tappable scan timeline, and exact aggregate detail screen. Detail selection survives
  Activity recreation, and the empty state routes directly to a new foreground scan.
- Settings Figma Overview: node `60:2`; Appearance: `60:3`; Language: `60:4`;
  Data & Storage: `60:5`; Privacy: `60:6`; About: `60:7`; Pro Upgrade: `60:8`,
  all on page `18:2` and validated as 412 × 917 frames.
- The recorded legacy About node `60:7` is no longer present in that Figma file. The current
  Design System integration exposes only its cover page, so Production About deliberately reuses
  the implemented Settings cards, top bar, tokens, and icon treatment rather than guessing a
  missing screen design.
- Settings provides persistent System/Light/Dark theme and app-language selection, truthful
  Android-owned access states, separate Reports/Trash DNA/app-cache controls, metadata-only
  backup/restore and diagnostics, onboarding preview, bundled policy/terms copy, feedback
  handoff, and version information. It contains no fake toggle or purchase preview.
- About TrashPilot is a dedicated Settings child destination. It displays the installed package
  version name/code, concise factual control and privacy statements, the bundled offline policy,
  local AndroidX/Kotlin runtime notices, and routes to existing permissions and introduction UI.
  Back returns to Settings and Settings remains selected in bottom navigation.
  Release preparation still needs a defined legal identity and support contact, plus a decision
  on whether the target store requires a public Privacy Policy URL in addition to the bundled copy.
- Settings Language node `60:4` supports 25 locales plus System language, native-name search,
  immediate locale application, and DataStore-backed selection.
- The complete UI string catalog now contains 964 identical keys in English plus 24 localized
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

## Duplicate Scanner

- The Results duplicate card opens a dedicated scanner over the active real shared-storage scan.
- Detection groups equal sizes before streaming SHA-256 hashes, reports incremental progress, and
  is cancelled with its Compose scope. Only accessible image, video, document, and audio files are
  eligible; Android private/system paths and TrashPilot-owned paths are excluded.
- Groups retain the oldest known file by default. Confirmed deletion updates the active scan and
  records actual reclaimed bytes as local cleanup history for Reports and Trash DNA.

## Real Cache Analyzer

- Home's existing Quick Clean entry opens an installed-application cache analyzer; Results keeps
  the existing selected-file Quick Clean flow.
- On Android 8+, public `StorageStatsManager` values are read only after the user grants Usage
  Access. Values Android withholds remain unavailable and Android 7 receives an honest unsupported
  state; no accessibility, root, broad storage, or hidden API is used.
- TrashPilot can automatically remove only its own private cache. Other selected apps use an
  explicit App Info handoff, and reclaimed bytes are recorded only from actual deletion or a
  measured before/after cache decrease.

## Large Files Manager

- Results opens a real MediaStore/Storage Access Framework scan with incremental file delivery,
  cancellation, exact thresholds, search, filters, and deterministic sorting.
- Android confirms deletion. Only successful deletions update Results and cleanup history used by
  Reports and Trash DNA.

## Hidden Files Manager

- Results opens a Storage Access Framework picker and progressively identifies dot-prefixed paths
  and files hidden by a real `.nomedia` marker.
- Protected Android/OS trees are skipped. Only confirmed successful SAF deletions update history.

## Social Media Cleaner

- Only installed supported packages are shown. Public files stream from MediaStore or explicit SAF.
- Owner-package metadata is preferred; conservative public folder markers are the fallback.
- Cache is reported unavailable, and only actual confirmed deletions update recovered history.

## APK Manager

- Results opens an APK Manager backed by the existing MediaStore scanner and explicit SAF folder
  access. Only accessible files whose real names end in `.apk` are included.
- Metadata is parsed with public `PackageManager` archive APIs from a cancellable temporary
  app-cache copy. Parse failures remain visible as unreadable APKs and never block discovery.
- Deletion uses Android confirmation/SAF APIs and records only successful deleted bytes.

## Downloads Cleaner

- Scan Results opens Downloads Cleaner even when the active scan has no cleanup candidates.
- Accessible Download/Downloads entries stream from MediaStore or a user-selected SAF tree.
  Search, type filtering, five deterministic sorts, selection, and category totals use only
  discovered metadata; no file starts selected.
- MediaStore deletion uses Android's system confirmation and verifies the affected URIs afterward.
  SAF deletion uses per-document results. Only verified successes update the active scan and the
  shared Reports/Trash DNA cleanup history.

## Empty Folders Cleaner

- Scan Results opens a dedicated SAF-only Empty Folders Cleaner. The user chooses the exact tree;
  no broad storage, root, hidden API, or `MANAGE_EXTERNAL_STORAGE` access is used.
- A directory is eligible only when its child query succeeds and every descendant is verified
  empty. Unreadable, protected, cyclic, stale, or content-bearing branches are excluded. Wholly
  empty subtrees collapse to the highest safe directory so deletion requests never overlap.
- Deletion requires selection and confirmation, uses `DocumentsContract.deleteDocument`, verifies
  absence, rescans the granted tree, records the successful folder count, and records zero bytes.

## Screenshots Cleaner

- Scan Results opens a MediaStore-backed Screenshots Cleaner for accessible images in confidently
  recognized screenshot folders. Filename and dimensions alone never qualify an image.
- Discovery is cancellable and progressive. The screen presents real thumbnails and metadata,
  search, deterministic sorting, manual selection, exact selected bytes, and a detail preview.
- Full and Android 14 selected-photo access are distinguished. Android-confirmed deletion is
  verified against MediaStore, and only confirmed missing records update Results and shared history.

## Photo Quality Analyzer

- Results opens an offline MediaStore image analyzer. Images are sampled through Android thumbnail
  APIs at no more than 256 pixels per side; originals are never copied, uploaded, or fully retained.
- A photo is flagged only for recorded dimensions below one megapixel or a 720-pixel short side,
  sampled Laplacian variance below 45, sampled mean luminance below 35 with at least 85% of pixels
  at or below 55, or mean luminance above 225 with at least 85% at or above 210.
- Blur, darkness, and brightness are explicitly labeled as technical heuristics. Intentional night,
  high-key, graphic, or soft-focus images can be false positives and always require manual review.
- No item starts selected. Android-confirmed deletion is verified URI-by-URI; only verified missing
  records and their recorded bytes update Results and shared cleanup history.

## Production Onboarding

- After the existing Splash, first launch shows exactly three lightweight steps: Welcome, Privacy,
  and Access only when needed. No permissions are requested and no bottom navigation is shown.
- Start writes a versioned flag to private SharedPreferences before navigating to Home. Completion
  survives process death, app restart, and updates; clearing app data naturally resets it.
- Onboarding is removed from the back stack after completion. A non-UI reset method exists only for
  development/tests, leaving room for a future intentional “View introduction” entry point.
- Permissions remain contextual to the selected feature and under Android control; onboarding never
  requests root, broad storage access, or unrelated permissions.
