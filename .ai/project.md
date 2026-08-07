# project
- Single-module Android app (com.trashpilot.app) using Kotlin, Jetpack Compose, Material 3, and Navigation Compose; min SDK 24, compile/target SDK 36. MainActivity applies TrashPilotTheme and AppNavigation currently displays SplashScreen.
- Navigation uses Navigation Compose with splash as the start destination. Splash routes first-time
  installs to the three-step Onboarding flow and completed installs to Home.
- Home is a Material 3 screen sourced from Figma `XyDczeNnzEw5DmApIXuXTz` node `1:65`,
  preserving its blue concentric brand lockup and SCAN hero while reducing secondary navigation
  to real storage, Quick Clean, Trash DNA, and Privacy Monitor.
- Compose Material Icons Extended supplies monochrome icons for Home cards and navigation.
- `core/storage` owns immediate read-only MediaStore scanning, fallback document-tree scanning,
  file categorization, byte formatting, and immutable scan models. Scanner and Results UI live
  in focused feature packages.
- Scan metadata now retains every readable file URI string, size, category, and last-modified
  timestamp in memory for the active session; no file content is retained or uploaded.
- `features/results` owns explicit loading, empty, success, and error UI states, sorting, and
  the category file-list destination.
- `core/quickclean` owns conservative disposable-file classification and selected-document
  deletion through the Storage Access Framework. `features/quickclean` owns Overview, manual
  Review, Confirmation, and honest Cleaning Report states.
- Room 2.8.4 persists metadata-only Trash DNA scan aggregates in `core/trashdna`; focused trend,
  profile, insight, and recommendation analyzers produce one honest local behavior summary.
  `features/trashdna` owns the empty, analysis, chronological history, reset, loading, and error UI.
- KSP 2.2.10-2.0.2 generates Room code. AGP 9 requires the scoped
  `android.disallowKotlinSourceSets=false` compatibility setting for this KSP version.
- `core/privacy` maps Android package declarations and runtime grant states into the eight
  Privacy Monitor categories. `features/privacy` owns the sortable installed-app list,
  app icons, status chips, app details, loading, and error presentation.
- `core/reports` associates recorded cleanup outcomes with completed scans and produces factual
  summary, chart, timeline, and detail models. `features/reports` owns the single Reports screen,
  empty/loading/error states, saved detail selection, and scan-history confirmation reset.
- Room schema version 4 adds explicit report-metric availability, scanned bytes, APK/Other totals,
  and large/social file counts through non-destructive `1 -> 2 -> 3 -> 4` migrations.
- `core/settings` owns persisted theme preferences, DataStore language selection, truthful
  permission-state mapping, reset-action rules, and a versioned metadata-only backup codec.
  `features/settings` owns the six production Settings destinations, Android settings handoffs,
  separate Reports/Trash DNA/cache controls, and local document picker flows.
- Preferences DataStore 1.2.1 stores the selected locale and migrates the legacy
  `trashpilot-settings` SharedPreferences language key on upgrade.
- Android resources provide complete 987-key catalogs for all 25 supported app languages.
  `tools/generate_locale_drafts.mjs` reproduces the automated draft catalogs and
  `tools/verify_locales.mjs` checks XML structure, exact key parity, formatting-token parity,
  and probable long English fallbacks.
- Android automatic/cloud backup is disabled. User-directed Settings export and backup are the
  only supported ways to copy TrashPilot metadata outside app-private storage.
- The superseded first Results implementation and unused Android template resources were removed
  during stabilization; `ImprovedResultsScreen.kt` is the single Results implementation.
- AndroidX Core 1.18.0, Lifecycle 2.10.0, and Activity Compose 1.12.2 are pinned to remain compatible with the installed compile SDK 36.1.

- tools/verify_locales.mjs validates XML/key parity, formatting tokens, probable long English fallback, and immutable product-name occurrence parity across all 25 catalogs.
- `ui/theme/DesignTokens.kt` and the fixed light color scheme own the approved shared visual
  values. `core/navigation/TrashPilotBottomBar.kt` owns the persistent app-wide navigation bar.
- `ui/components/TrashPilotComponents.kt` owns shared buttons, cards, top bars, icon containers,
  section headers, and loading/empty/error state primitives.

- `ui/components/TrashPilotComponents.kt` owns the reusable `TrashPilotAmbientMessage`; timing and Home spacing remain centralized in semantic motion/Home tokens.
- Ambient Message copy is present in all 25 key-identical locale catalogs as automated release-candidate draft localization.
- `ui/components/TrashPilotComponents.kt` also owns the shared Home-family brand header, outlined
  card shell, and feature card used by Home and Scan Results.
- Scan Results displays only active-scan metrics, classifies accessible social/messenger media
  from stored relative paths, and runs SHA-256 duplicate comparison only on explicit user action.
- `core/storage/DuplicateAnalyzer.kt` owns size-first SHA-256 grouping and progress models;
  `DuplicateCleaner.kt` owns explicit URI deletion reports; `features/duplicates` owns UI only.
- `core/cache` owns public-API cache capability checks, installed-app cache snapshots, sorting,
  and TrashPilot-private cache deletion. `features/cache` owns Usage Access, search, selection,
  App Info handoff, progress, and factual state UI.
- `core/largefiles` owns file-type mapping, thresholds, filtering, searching, and sorting.
  Existing storage scanners stream accessible files to the Large Files feature UI.
- `core/hiddenfiles` owns hidden-path and `.nomedia` detection plus protected-tree guards.
  `features/hiddenfiles` owns SAF selection, progressive presentation, and confirmed deletion.
- `core/socialcleaner` owns installed-package discovery, file attribution, typing, summaries,
  filtering, and sorting. `features/socialcleaner` owns scan and deletion UI.
- `TrashPilotTheme` now honors the existing System/Light/Dark preference with role-based light and
  dark color schemes; feature UI continues to consume `MaterialTheme` roles.
- `core/apkmanager` owns APK detection, metadata parsing, models, filtering, and sorting.
  `features/apkmanager` owns progressive scan state, details, selection, and deletion.
- `core/downloads` owns Downloads membership, extension-based presentation categories, summaries,
  search, sorting, selection totals, and successful-deletion accounting. `features/downloads`
  streams real MediaStore or user-granted SAF metadata and owns explicit Android-confirmed deletion.
- `core/emptyfolders` owns post-order SAF verification, protected-path policy, sorting/search,
  and verified deletion accounting. `features/emptyfolders` owns explicit tree access, progressive
  scan states, manual selection, confirmation, and refresh after deletion.
- `core/screenshots` owns conservative MediaStore screenshot classification, sorting, grouping,
  selection totals, and verified deletion accounting. `features/screenshots` owns permission-aware
  progressive discovery, thumbnail review, details, selection, and Android-confirmed deletion.
- `core/photoquality` owns conservative thresholds, sampled luminance/Laplacian metrics,
  objective classification, filtering, sorting, selection, and deletion accounting.
  `features/photoquality` owns permission-aware analysis, transparent review, and confirmed deletion.
- `core/onboarding` owns versioned local completion state and deterministic startup routing.
  `features/onboarding` owns the short Welcome, Privacy, and contextual Access presentation.
- `core/about` owns runtime version mapping, local destination availability, and deterministic
  About navigation models. `features/about` owns the dedicated product/privacy screen, bundled
  policy presentation, verified runtime-library notices, and links back to existing Settings and
  onboarding destinations.
- `core/navigation/InformationArchitecture.kt` is the single map for four top-level destinations,
  the Apps/Photos/Files review hierarchy, deep-route parent selection, and honest Quick Clean
  startup routing. Mature cleaner implementations and their deletion rules remain unchanged.
