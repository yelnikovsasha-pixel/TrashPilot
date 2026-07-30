# project
- Single-module Android app (com.trashpilot.app) using Kotlin, Jetpack Compose, Material 3, and Navigation Compose; min SDK 24, compile/target SDK 36. MainActivity applies TrashPilotTheme and AppNavigation currently displays SplashScreen.
- Navigation uses Navigation Compose with splash as the start destination, followed by Home, Scanner, Results, Privacy, Reports, and Settings destinations.
- Home is a Material 3 screen sourced only from Figma `XyDczeNnzEw5DmApIXuXTz` node `1:65`,
  with a blue concentric brand lockup, one 200 dp double-ring SCAN action, a real-data phone
  storage card, five vertical feature cards, and persistent bottom navigation.
- The evolved Home keeps the approved blue hero identity and uses a compact real-data Storage card,
  a two-by-two Quick Clean/Trash DNA/Privacy Monitor/Reports action grid, and a concise trust card.
- Compose Material Icons Extended supplies monochrome icons for Home cards and navigation.
- `core/storage` owns read-only document-tree scanning, file categorization, byte formatting, and immutable scan models. Scanner and Results UI live in focused feature packages.
- Scan metadata now retains every readable file URI string, size, category, and last-modified
  timestamp in memory for the active session; no file content is retained or uploaded.
- `features/results` owns explicit loading, empty, success, and error UI states, sorting, and
  the category file-list destination.
- `core/quickclean` owns conservative disposable-file classification and selected-document
  deletion through the Storage Access Framework. `features/quickclean` owns Overview, manual
  Review, Confirmation, and honest Cleaning Report states.
- Room 2.8.4 persists metadata-only Trash DNA sessions in `core/trashdna`; `features/trashdna`
  owns overview, supported local insights, history, and loading/empty/success/error presentation.
- KSP 2.2.10-2.0.2 generates Room code. AGP 9 requires the scoped
  `android.disallowKotlinSourceSets=false` compatibility setting for this KSP version.
- `core/privacy` maps Android package permission declarations into ten factual permission
  categories. `features/privacy` owns overview, category, app, detail, recommendation,
  search, sorting, loading, empty, success, and error presentation.
- `core/reports` summarizes and exports the existing metadata-only Room history.
  `features/reports` owns overview, scan and cleaning histories, real-data charts, local text
  export, and loading/empty/success/error presentation.
- Room schema version 2 adds scan duration, scanned-file count, and aggregate Privacy Monitor
  review counts through a non-destructive `1 -> 2` migration.
- `core/settings` owns persisted theme preferences, DataStore language selection, and a versioned metadata-only
  backup codec. `features/settings` owns the seven Settings destinations and local document
  picker flows for diagnostics, backup, and restore.
- Preferences DataStore 1.2.1 stores the selected locale and migrates the legacy
  `trashpilot-settings` SharedPreferences language key on upgrade.
- Android resources provide complete 344-key catalogs for all 25 supported app languages.
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
