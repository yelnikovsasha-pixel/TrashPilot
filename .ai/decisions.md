# decisions
- Use Compose and Material 3; enable edge-to-edge; select dynamic color on Android 12+ with local light/dark fallback; keep the first release splash-only while future flows follow the Figma system and its offline-first, no-fake-claims privacy stance.
- Splash flow decision: use a 700ms fade-in, hold for 2 seconds, then navigate with NavHost from splash to Home.
- Home decision: show `Not scanned yet` before the first scan, launch Scanner from the 168 dp circular primary action, and show real device totals after a completed scan.
- Home information architecture uses Storage, Trash DNA, Privacy Monitor, and Reports cards; Settings remains a bottom-navigation destination rather than a content card.
- Home evolution keeps SCAN dominant and makes Quick Clean, Trash DNA, Privacy Monitor, and Reports
  equal secondary actions. Storage progress appears only after a real scan; trust statements are
  factual and never imply background scanning or automatic deletion.
- Bottom navigation exposes the planned Home, Privacy, Reports, and Settings information architecture; unfinished destinations use clear placeholder copy.
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
- The scan/results flow has explicit Scanning, Results, Nothing Found, and genuine Error states.
  A successful scan reaches Results only after finalization; zero-attention scans use the positive
  Nothing Found presentation rather than rendering zero-valued category cards.
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
- Scan duration is measured with elapsed realtime around the actual SAF traversal. Legacy rows
  retain explicit missing values, while new scans record duration and scanned-file count.
- Storage trend charts normalize only values from recorded sessions and never seed demo,
  forecast, interpolated, or synthetic points.
- Settings uses private SharedPreferences for theme and Preferences DataStore for language;
  Activity recreation applies locale changes immediately. Backup/restore uses explicit Android document pickers and a versioned
  text format so no storage permission or cloud service is required.
- Privacy Policy and Terms are bundled offline in the app. Feedback is the only Settings action
  that intentionally hands off to an external app; Pro remains disabled with no purchase flow.
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
