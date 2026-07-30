# conventions
- Organize code by core, features, and ui/theme; route composables through AppNavigation; apply TrashPilotTheme at the activity root; use MaterialTheme tokens, Compose modifiers, dynamic color when supported, and privacy-conscious offline-first UX copy.
- Splash visuals use a white surface, a Compose-native TP mark, theme typography, and explicit restrained colors; animation and delayed navigation are lifecycle-aware through LaunchedEffect.
- Before real device data exists, Storage must show `Not scanned yet`; never display fake percentages, capacity figures, or optimization results.
- Keep Scan as the only visually dominant Home action; navigation actions remain in the Material 3 bottom bar.
- Home uses Figma file `XyDczeNnzEw5DmApIXuXTz`, node `1:65`, at 412 × 917; older Home nodes
  are obsolete. Home cards use 24 dp horizontal content
  padding, a fixed 52 dp logo, weighted single-line brand copy, and four equal
  single-line navigation destinations in an 80 dp bar.
- Home excludes Motivation and Settings cards. Settings remains in bottom navigation; Quick Clean
  is a secondary Home action and remains honest when no scan data exists.
- Storage access must use explicit user-selected document trees. Do not request broad storage access or scan in the background.
- Scan results remain local. Quick Clean may delete only explicitly selected conservative
  disposable candidates after confirmation; it must never auto-select or delete personal media.
- Quick Clean candidate rules are intentionally narrow: temporary extensions, files directly
  under cache folders, marked leftover APK names, log files, and readable empty folders.
- File categories are exclusive. Files inside Download/Downloads are categorized as Downloads before MIME or extension classification.
- Results category navigation and sorting must operate on current in-memory scan metadata only.
  Keep the largest-files section capped at ten files; sorting changes their presentation and
  does not substitute a different set.
- A missing document last-modified value is shown honestly as `Date unavailable`.
- Trash DNA persistence may contain timestamps, selected-folder display names, aggregate
  disposable category totals, reclaimable/reclaimed byte totals, and outcomes only.
- Never persist file names, file URIs, full paths, media names, or document names in Trash DNA.
  Summary statistics require two scans; pattern insights require at least three scans and must
  satisfy explicit aggregate-history rules.
- Privacy Monitor must display only installed packages and permission declarations returned by
  Android. Do not add threat scores, malware claims, inferred warnings, or fabricated examples.
- Permission review is read-only. Any permission change must be handed off explicitly to Android
  Settings; TrashPilot does not silently grant, revoke, or modify app permissions.
- Reports and charts may use only persisted event values. Missing legacy scan duration or file
  counts must be labeled `Not recorded` and must not be converted into chart points.
- Text exports are created locally through Android's document picker and contain aggregate
  metadata only; exclude file names, URIs, full paths, media/document names, and file contents.
- Settings reset may delete only rows from TrashPilot's Room history table. Cache clearing is
  confined to the app's private `cacheDir`; neither operation may traverse selected user storage.
- Settings backups contain preferences and metadata-only history. Restore replaces only that
  local metadata after validating the versioned TrashPilot backup header.
- Language catalogs keep System language first, show locale names in their native form, and use
  Preferences DataStore as the sole persisted language source.
- Every runtime-visible label, state, dialog, permission explanation, report label, policy,
  diagnostics line, and dynamic status format belongs in Android string resources. Keep every
  locale catalog key-identical and run `node tools/verify_locales.mjs` after base-string edits.
- Current non-English copy is an automated release-candidate draft requiring native review.
  Structural validation is not linguistic approval; preserve format tokens exactly and never
  describe these catalogs as native-reviewed.
- Do not use dynamic device colors for product UI. Use the approved TrashPilot theme and
  `TrashPilotDimensions` tokens; Home remains the geometry and component-style reference.
- Feature screens must use semantic `TrashPilot*` tokens and components. Raw dp, sp, hex colors,
  and direct Material card/button/top-app-bar construction belong only in the design-system layer.
- Home evolution values belong to `TrashPilotHomeTokens`; preserve the approved brand header,
  circular SCAN control, palette, and navigation when refining Home composition.
- Nested feature pages must intercept system back consistently with their app-bar back action.
  Scanner blocks back only while a local traversal is actively running.
- SAF selection requests and persists both read and write grants. Scanning remains read-only;
  the write grant is consumed only by confirmed Quick Clean deletion of explicitly selected
  disposable documents.

## Protected product names

- Never translate these tokens, including inside descriptive sentences: SCAN, TrashPilot, Trash DNA, Quick Clean, Privacy Monitor, Reports, Settings, and Smart Cleaner.
- Exact-name resources use translatable=false; the draft generator masks embedded occurrences and the verifier checks exact occurrence counts in every locale.
- Translate only surrounding descriptions, messages, dialogs, hints, and explanations.
