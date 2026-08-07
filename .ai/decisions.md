# decisions
- Use Compose and Material 3; enable edge-to-edge; select dynamic color on Android 12+ with local light/dark fallback; keep the first release splash-only while future flows follow the Figma system and its offline-first, no-fake-claims privacy stance.
- Splash flow decision: use a 700ms fade-in, hold for 2 seconds, then navigate with NavHost from splash to Home.
- Home decision: show `Not scanned yet` before the first scan, launch Scanner from the 168 dp circular primary action, and show real device totals after a completed scan.
- Top-level information architecture is Home, Privacy, Reports, and Settings. Home keeps SCAN
  dominant, then real storage, Quick Clean, Trash DNA, and Privacy Monitor; Reports and Settings
  are not duplicated as Home feature cards.
- The primary product flow is SCAN -> Results -> Review -> Confirm -> Clean. Every successful
  scan reaches Results, including a zero-candidate scan, because Results also provides truthful
  deeper-review destinations without inventing totals.
- Results groups review destinations as Apps (App cache, Social media), Photos (Screenshots,
  Duplicates, Photo review), and Files (Large files, Downloads, APK installers, Hidden files,
  Empty folders). Internal implementation names and algorithms remain unchanged.
- Keep AndroidX dependencies on API 36.1-compatible versions until the project intentionally adopts compile SDK 37.
- Scanner starts immediately with a foreground MediaStore query of the shared-storage records
  Android exposes to TrashPilot; it does not request `MANAGE_EXTERNAL_STORAGE`.
- Scanner requests only Android's scoped image, video, audio, or legacy read permissions when no
  supported media access exists. A partial grant is scanned as-is and is not repeatedly prompted.
- Storage Access Framework selection is a fallback only when Android blocks direct access to a
  location. The picker retains the exact read flag returned by DocumentsUI and persists that
  grant before the fallback scan.
- Completed scan results are kept in memory for the active app session and contain metadata only.
  Quick Clean is the sole deletion path and operates only on manually selected disposable URIs.
- Results keeps all accessible file metadata for category drill-down, while deriving the ten
  largest files on demand. File URIs are stored as strings to keep sorting logic platform-neutral.
- The scan/results flow has explicit Scanning, Results, and genuine Error states. A finalized scan
  always reaches the grouped Results hub; unavailable totals are omitted and deeper tools use an
  honest Open review or Run analysis affordance.
- Scanner work is owned by a composition coroutine. Both visible and system Back leave the
  foreground scan safely and cancel ongoing traversal instead of trapping the user.
- Quick Clean starts Review with nothing selected, requires a separate confirmation dialog, and
  reports reclaimed bytes, successful counts by category, and every failed deletion honestly.
- Trash DNA uses a private on-device Room database with no account, network, cloud, analytics,
  or tracking integration. Scan and cleanup completion append metadata-only history records.
- Trash DNA requires two completed scans containing version-3 aggregate metrics. Profiles are
  deterministic and default to Balanced unless one recorded growth pattern clearly dominates.
  One insight and one calm recommendation are derived from the latest two qualifying scans.
- Resetting Trash DNA records a local cutoff rather than deleting scan rows, so Trash DNA starts
  fresh while historical scan results remain available to Reports and metadata backup.
- Folder display names are retained to make history understandable; document URIs, full paths,
  and file-level identifiers are deliberately excluded from the Room schema.
- Privacy Monitor uses PackageManager with permission metadata and package visibility so counts
  reflect the installed device rather than a curated or fake app list. Data is processed in
  memory and is not persisted, uploaded, scored, or tracked.
- Privacy Monitor lists every package Android returns and distinguishes runtime grants from
  declared special capabilities. Accessibility and background-related declarations are labeled
  Sensitive because PackageManager cannot establish current usage or enabled state.
- Recommendations are conditional: they appear only when the corresponding permission category
  exists on the device, and permission management opens Android's own privacy settings.
- Reports reuses the Trash DNA Room event table as the single local metadata history. Privacy
  reviews append aggregate app counts only; no app names, package names, or permission lists are
  persisted.
- Reports displays one real point per completed scan and associates only cleanup rows recorded
  after that scan and before the next. Charts use unsmoothed bars and never interpolate points.
- Version-4 scan rows explicitly mark complete Reports aggregates. Older rows remain visible but
  unavailable fields are labeled Not recorded; they are never inferred from unrelated totals.
- Clearing Reports deletes only SCAN and CLEANUP rows from app-private history. It never touches
  user files or Privacy Monitor review rows.
- Scan duration is measured with elapsed realtime around the actual SAF traversal. Legacy rows
  retain explicit missing values, while new scans record duration and scanned-file count.
- Storage trend charts normalize only values from recorded sessions and never seed demo,
  forecast, interpolated, or synthetic points.
- Settings uses private SharedPreferences for theme and Preferences DataStore for language;
  Activity recreation applies locale and theme changes immediately. Backup/restore uses explicit
  Android document pickers and a versioned text format so no storage permission or cloud service
  is required. Existing values and the system-default first-install behavior are preserved.
- Settings presents media, audio, Usage Access, and persisted SAF access as factual Android-owned
  states, never as app-controlled switches. Opening Settings requests no permission; review actions
  hand off only to resolvable public Android settings intents.
- Reports history clearing deletes only report-producing scan/cache-scan/cleanup rows. Trash DNA
  reset records its existing cutoff and preserves shared history rows. TrashPilot cache clearing
  deletes only measured app-private cache children and reports measured bytes.
- View introduction reuses the production onboarding UI through a separate navigation route,
  returns to Settings, and does not mutate first-launch completion.
- About is a dedicated navigation destination under Settings, not an internal Settings page.
  Version values come from installed package metadata. Its Privacy Policy is the existing bundled
  offline document; no production URL, support contact, developer address, legal identity, or
  social account is shown because none is currently defined.
- Open-source notices are kept local and list only the verified AndroidX, Kotlin, and coroutines
  runtime families declared by the build with their Apache License 2.0 identifier. No notice UI
  dependency or external handoff is added.
- Privacy Policy and Terms are bundled offline in the app. Feedback is the only Settings action
  that intentionally hands off to a non-system external app; no purchase or decorative Pro control
  is exposed.
- Stabilization disables Android Auto Backup to preserve the offline/no-cloud contract.
  Metadata restore now replaces Room history transactionally so a failed insert cannot leave a
  partially restored database.
- The language selector exposes 25 requested locale targets plus System language. Locale tags are
  applied immediately. Complete per-locale catalogs prevent missing-resource English fallback.
- Automated translations are accepted only as release-candidate drafts. Native-language review
  remains a localization release gate even when lint, catalog parity, and emulator layout checks
  pass.

## Localization brand strategy

- Feature and product names remain English in all locales. Localization applies only to explanatory and action-supporting copy.
- The Home primary action is always the exact uppercase text SCAN.
- Catalog verification fails on any changed, missing, or duplicated protected token.

## Approved visual implementation

- Use the fixed approved light palette instead of Material dynamic color or dark variants.
- Render one navigation bar from the app shell on every route, including Splash and nested flows.
- Hide system bars immersively so the 412 × 917 app surface matches the approved full-frame design.
- Preserve legacy approved geometry as semantic tokens during architecture refactors; tokenization
  does not authorize normalizing 20/24/26/28 dp shapes into a redesigned shape scale.
- Duplicate recoverable storage is the real byte total of redundant copies after retaining one
  file per SHA-256 group. Confirmed duplicate cleanup is stored as a normal cleanup event so
  existing Reports and Trash DNA consume only actual deletions.
- Per-app cache measurement uses only `StorageStatsManager` with user-granted Usage Access on API
  26+. Automatic cleaning is restricted to TrashPilot's own cache; every other package is handed
  to public Android App Info and is recorded as cleaned only after a measured decrease.
- Large-file discovery reuses MediaStore and the Storage Access Framework. Archives remain a
  presentation type derived from real extensions so the scan-history schema stays compatible.
- Hidden-file management is SAF-only and never traverses protected Android/OS trees. A file is
  hidden only through a dot-prefixed path segment or an accessible `.nomedia` marker.
- Social media attribution uses MediaStore owner-package metadata where exposed and conservative
  public-folder markers otherwise; private directories and inferred cache sizes are excluded.
- The persisted theme preference selects a real light or dark Material scheme; dynamic color
  remains disabled and shared semantic roles remain the UI contract.
- APK metadata uses only `PackageManager.getPackageArchiveInfo` against a temporary private-cache
  copy of a user-accessible file; the copy is removed immediately after parsing.
- Downloads Cleaner reuses the foreground MediaStore and SAF scanners and requests no new
  permission. Download membership comes only from the real Download/Downloads path exposed by
  Android. Reclaimed bytes are the sizes of verified successful deletions only.
- Empty-folder discovery is SAF-only and propagates CONTENT or UNKNOWN status upward through the
  selected tree. The selected root, required top-level media folders, `/data`, `/system`, and
  TrashPilot-private package paths are never candidates. Explicitly granted Android/data or
  Android/obb descendants may be inspected only when DocumentsUI actually exposes them.
- Empty-folder cleanup history stores successful folder count in `emptyFolderCount` and always
  stores zero reclaimable/reclaimed bytes; Reports associates that count without treating it as
  recovered storage.
- Screenshot discovery queries MediaStore Images and requires a recognized screenshot directory
  signal from `RELATIVE_PATH` or bucket metadata. A screenshot-like filename or image dimensions
  alone are insufficient; uncertain images remain excluded.
- Screenshot previews use Android thumbnail APIs and never decode full-resolution list images.
  Android 14 selected-photo access is presented as partial coverage. Deletion uses
  `MediaStore.createDeleteRequest` where required and counts only URIs verified absent afterward.
- Photo Quality Analyzer is a deterministic technical tool, not semantic or AI image judgment.
  Its centralized conservative defaults are: below 1,000,000 pixels or a 720-pixel short side;
  Laplacian variance below 45 for “Possibly blurry”; mean luminance below 35 plus 85% at or below
  55 for “Very dark”; and mean above 225 plus 85% at or above 210 for “Very bright”.
- Pixel metrics use a maximum 256-pixel MediaStore thumbnail. This bounds memory and I/O but can
  produce false positives for intentional night, bright, graphic, low-detail, or soft-focus work.
  The UI explains this and never exposes an overall quality score or automatic recommendation.
- Startup retains Splash, then resolves synchronously from a versioned private onboarding preference
  to either Onboarding or Home. A successful final-step write is required before Home navigation,
  and onboarding is removed from the back stack once complete.
- Onboarding is limited to Welcome, Privacy, and contextual Access. It contains no permission
  launcher: every Android permission remains requested only by the feature the user actively opens.
  Splash and Onboarding intentionally omit the app-wide bottom navigation.
