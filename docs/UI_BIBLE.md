# TrashPilot UI Bible

## Authority

This document, `DESIGN_SYSTEM.md`, and `BRAND_GUIDELINES.md` are the single source of truth for
TrashPilot UI work. Read all three before changing UI. They describe the approved current design;
they do not authorize redesign. When implementation and documentation differ, record the
difference and obtain approval before changing production code.

The approved Home screen is the visual reference for every other screen. Validation target:
412 × 917 px at 160 dpi. The configured Figma source is the TrashPilot Design System file
`GIpl7h9YLIvYnAavxMoGGz`; Home is node `21:2`.

## Design philosophy

- Minimal, calm, privacy-first, and offline-first.
- One visually dominant primary action per screen.
- Never claim optimization, danger, savings, or cleanup results without real device data.
- Use generous whitespace, soft rounded surfaces, restrained elevation, and one blue-violet
  palette.
- Preserve a light, near-white canvas. Do not introduce dark mode, dynamic device colors,
  gradients, decorative colors, or alternate layouts without approval.
- Keep navigation predictable and always visible.

## Reference layout

- App surface: 412 × 917 design frame.
- System bars: immersive in the approved implementation so app coordinates map to the full frame.
- Page gutter: 24 dp.
- Home content begins 20 dp from the top.
- Bottom navigation begins at y = 837 and is 80 dp high.
- Content must not render under or over the bottom navigation.
- Scrolling is allowed when localized copy or dynamic data exceeds the reference viewport.
- Keep leading alignment for content; center only intentional hero, empty-state, and assurance
  content.

## Home reference geometry

- Brand logo: 52 × 52 dp, 18 dp corner radius.
- Brand header gap: 14 dp.
- Space from header to SCAN region: 20 dp.
- SCAN button: 168 × 168 dp circular action.
- Space after SCAN: 20 dp.
- Storage card: 92 dp high; real scan progress appears only when real totals exist.
- Home sections use 20 dp separation and a 10 dp title-to-content gap.
- Quick actions use a two-column grid with 10 dp gaps and 84 dp cards.
- Trust is a single compact accent-surface card with three factual assurances.
- Card padding: 14 dp.
- Card icon container: 48 × 48 dp, 16 dp radius.
- Card icon: 24 × 24 dp.
- Card corner radius: 24 dp.

## Component rules

### App shell

`AppNavigation` owns the shared app scaffold. `TrashPilotBottomBar` is the only bottom-navigation
implementation. Do not add screen-local bottom bars.

### Top app bars

- Nominal height: 64 dp.
- Use a leading back action on nested destinations.
- Title style follows the approved title hierarchy.
- App-bar back and system back must produce the same result.
- Do not use text as an icon substitute when an approved icon exists.

### Cards

- Default container: card surface token.
- Highlight container: accent-surface token.
- Default radius: 24 dp.
- Default Home padding: 14 dp; dense data cards currently use 18 or 20 dp where specified by the
  approved screen.
- Avoid implicit Material elevation. Elevation or shadow must be explicitly approved.
- Clickable cards require a meaningful semantic role, visible pressed feedback, and a minimum
  48 dp touch target.

### Buttons

- Primary rectangular action: 52 dp high, 18 dp radius in the token set.
- Current feature flows commonly render 52 dp actions with a 26 dp pill radius. Treat this as an
  implementation exception pending approval, not a new token.
- Home SCAN is the only 168 dp circular primary action.
- Use one dominant filled action per screen.
- Secondary actions may be outlined or text actions.
- Destructive actions require explicit confirmation and descriptive labels.

### Bottom navigation

- Present on every screen and always visible.
- Height: 80 dp.
- Four equal destinations: Home, Privacy, Reports, Settings.
- Icon: 22 dp.
- Selected indicator: 64 × 32 dp with a 16 dp radius.
- Unselected icon slot: 32 × 32 dp.
- Label: 12 sp/16 sp; medium weight when selected, normal when unselected.
- Bar color: background token; zero tonal elevation.
- Selected indicator: accent-surface token.
- Icons and labels: primary text token.
- Root navigation should be single-top and avoid stacking duplicate root destinations.

### Icons

- Use approved monochrome assets or a visually identical Material glyph.
- Home card icons: 24 dp inside 48 dp containers.
- Bottom-navigation icons: 22 dp.
- Standard toolbar/action icons: 24 dp unless a screen specification says otherwise.
- Do not mix outlined, filled, text-symbol, emoji, or custom-drawn styles within the same
  component family.
- Decorative icons have no content description. Action icons require localized descriptions.

## Motion

- Motion is restrained and functional.
- Splash content fades in over 700 ms and routes after approximately 2 seconds.
- Use standard Material state transitions only when they do not alter approved geometry.
- Avoid looping decoration, parallax, large movement, or motion that implies scanning progress
  not backed by real work.
- Respect the system animator-duration setting and avoid making information dependent on motion.

## Accessibility

- Touch targets must be at least 48 × 48 dp.
- Preserve logical traversal order and system-back behavior.
- Action icons require localized content descriptions.
- Decorative icons should be excluded from accessibility semantics.
- Never communicate state with color alone.
- Text must support font scaling without clipping; localized copy may wrap.
- Do not force single-line labels unless the approved component has sufficient measured width in
  every supported locale.
- Maintain readable contrast against the approved palette.
- Loading, empty, error, success, selected, disabled, and destructive states must be announced
  through visible text or semantics.

## Localization and naming

- All user-facing descriptions, statuses, dialogs, labels, and dynamic formats belong in string
  resources.
- Keep format tokens identical in every locale.
- Locale catalogs must remain key-identical and pass `node tools/verify_locales.mjs`.
- Exact product and feature names remain English. See `BRAND_GUIDELINES.md`.
- Descriptions are translated; protected names are not.
- Test long Latin text, Arabic RTL, CJK, and large font scale before approving layout changes.
- Existing non-English catalogs are automated release-candidate drafts, not native-reviewed copy.

## Implementation guidance

- Kotlin, Jetpack Compose, and Material 3 only.
- Reuse `MaterialTheme`, `TrashPilotDimensions`, and shared navigation.
- State is immutable by default and hoisted where practical.
- Keep shared UI outside feature packages once two or more features need it.
- Do not embed new colors, dimensions, shapes, or text styles in a screen when an approved token
  exists.
- New visual values require design approval and documentation before production use.
- Preserve offline behavior and use real state; never add fabricated preview data to runtime UI.
- Validate meaningful changes with screenshots at 412 × 917.
- Run locale verification, lint, unit tests, and APK build after UI work.

## Current implementation audit

Audit date: 2026-07-29. Scope: every Kotlin file containing `@Composable`, the theme files,
`AppNavigation`, and `TrashPilotBottomBar`.

### 1. UI inconsistencies

- Home and bottom navigation consume shared dimension tokens; feature screens mostly repeat local
  literals.
- Home uses fixed reference geometry; Scanner centers content vertically, while most other
  features use top-aligned `LazyColumn` content.
- Screen-level state presentations differ: `StateMessage`, `MessageState`, `StateCard`,
  `ResultsStateMessage`, and placeholder UI implement similar roles differently.
- Feature screens use different card padding, gaps, radii, and title sizes for comparable content.
- Settings uses several Material defaults without explicit approved shapes or colors.

### 2. Different spacing values

Observed raw spacing values include 3, 4, 6, 8, 10, 12, 14, 16, 18, 20, 24, and 32 dp.
Common page gutters are consistently 24 dp, but vertical list gaps vary among 8, 10, 12, 14, and
16 dp. Card internals vary among 12, 14, 18, 20, and 24 dp. These differences are sometimes
screen-specific, but most are not represented as named tokens.

### 3. Different typography styles

- The global type scale defines 24/30, 22/28, 16/22, 14/20, 13/18, 12/16, and 11/15 styles.
- Home repeats the approved sizes locally instead of using the matching global styles directly.
- Reports uses hardcoded 18 sp and 19 sp semibold headings.
- Privacy Monitor uses a hardcoded 18 sp semibold heading.
- Default `Text` styles occur in several feature helpers, allowing context-dependent Material
  typography.

### 4. Different card implementations

- Home `FeatureCard`: 24 dp radius, 14 dp padding, fixed height.
- Quick Clean and Trash DNA each define separate but nearly identical `HighlightCard`, `LowCard`,
  and `MetricRow` helpers.
- Reports and Privacy Monitor each define separate `MetricCard`, `InfoCard`, and state-card
  helpers.
- Results uses `ImprovedStorageCard` and `ImprovedListCard`, with 24 and 20 dp radii.
- Settings uses `SettingRow` with 20 dp radius, plus cards that rely on Material default shape.
- Scanner has a unique 24 dp privacy-assurance card.

### 5. Different button implementations

- Home uses a 168 dp `ElevatedButton`.
- Scanner uses a default-height Material `Button` without the shared primary-action token.
- Results uses separate 52 dp `Button` and `OutlinedButton` instances with 26 dp radius.
- Quick Clean defines a local 52 dp/26 dp `PrimaryAction`, plus inline secondary actions.
- Trash DNA defines inline 52 dp/26 dp buttons.
- Settings uses default Material buttons, text buttons, radio buttons, and disabled actions with
  no shared TrashPilot button wrapper.

### 6. Different icon styles

- Bottom navigation uses filled Material icons.
- Home cards use outlined Material icons.
- Most feature actions use outlined icons.
- Splash uses a text-based TP mark.
- Settings uses a mixture of outlined icons, plain text marks, and card-contained icons.
- Figma-exported icon assets are not represented as a shared approved icon set in production.

### 7. Different navigation implementations

- Root navigation and the persistent bottom bar are centralized and reusable.
- Each feature independently builds a Material `TopAppBar`.
- Settings, Privacy Monitor, Reports, Trash DNA, and Quick Clean manage nested pages inside one
  route with local enums and local back handling rather than child navigation destinations.
- Placeholder, Results, Category Files, and Scanner repeat top-app-bar/back patterns.

### 8. Duplicate components

- `HighlightCard`, `LowCard`, `MetricRow`, `SectionTitle`, and body-text helpers are duplicated
  between Quick Clean and Trash DNA.
- `MetricCard`, `InfoCard`, and message-state patterns are duplicated across Reports and Privacy.
- Back top bars are repeated across nearly every feature.
- Screen list scaffolding with 24 dp gutters is repeated across features.
- Navigation-card rows are separately implemented in Reports and Settings.

### 9. Hardcoded colors

Production feature hardcoded colors occur in Splash:

- `#17212B` for the mark and title.
- `#66727D` for the tagline.

These conflict with the approved palette. Other screens generally use `MaterialTheme`.

### 10. Hardcoded dimensions

Most feature layouts directly use dp values rather than `TrashPilotDimensions`. Repeated values
include 24 dp gutters/radii, 20/18/14 dp card padding, 16/14/12/10/8 dp gaps, 52 dp buttons,
26 dp pill radii, 20/24 dp icons, and several state-specific fixed heights. Home is the primary
consumer of the shared tokens.

### 11. Hardcoded typography

Home repeats six approved font-size/line-height pairs. Reports adds 18 and 19 sp sizes; Privacy
adds 18 sp. Several components specify only `fontWeight`, inheriting a default size from context.

### 12. Components that should be reusable

- TrashPilot top app bar with standard back behavior.
- Primary 52 dp action and secondary 52 dp action.
- Standard low card and highlighted card.
- Metric row and metric card.
- Information card and message-state card.
- 24 dp-gutter screen column.
- Navigation/setting row with icon, title, body, and chevron.
- Search field and sort-chip row.
- Brand mark and brand header.
- Standard confirmation dialog treatment.

### 13. Components already reusable

- `TrashPilotTheme`.
- `TrashPilotDimensions`.
- `TrashPilotBottomBar`.
- App-level scaffold and route coordination in `AppNavigation`.
- Feature-local reuse exists within Home (`FeatureCard`), Results (`ImprovedListCard`),
  Quick Clean, Trash DNA, Privacy Monitor, Reports, and Settings, but these helpers are private
  and cannot provide cross-feature consistency.

### 14. Accessibility issues

- Bottom-navigation labels are forced to one line and may truncate or crowd in long locales.
- Home brand and assurance copy use fixed single-line behavior.
- Some clickable `Card` implementations use `Modifier.clickable` without an explicit button role.
- Several action icons pass `null` content descriptions; some are decorative, but intent is not
  consistently documented.
- Settings sometimes uses text glyphs as visual marks, producing inconsistent screen-reader and
  font rendering behavior.
- Fixed heights may clip at large font scale.
- Full-screen immersive mode reduces persistent access to system bars; Android provides transient
  reveal by swipe, but this should be explicitly tested with accessibility services.
- Automated Compose accessibility tests are not present.

### 15. Material defaults conflicting with the approved design

- Default `TopAppBar`, `Button`, `OutlinedButton`, `Card`, `OutlinedTextField`, `AlertDialog`,
  `RadioButton`, filter chips, and progress indicators can introduce default padding, shape,
  elevation, state colors, or sizing not captured by TrashPilot tokens.
- `Card` calls without explicit colors/shapes occur in Settings.
- Default top-app-bar insets and typography are repeated rather than fixed by a shared component.
- Home `ElevatedButton` uses Material elevation values rather than a named approved shadow token.

### 16. Other visual inconsistencies

- Card radii include 12, 16, 18, 20, 24, 26, and 28 dp.
- Comparable headings vary between theme styles and hardcoded 18/19 sp text.
- Search fields use 18 or 28 dp radii depending on feature.
- Settings icon containers use 12 dp radius and 20 dp icons, differing from Home's 16 dp/24 dp
  standard.
- Scanner's primary button does not explicitly use the approved 52 dp height.
- Splash uses a different dark gray palette and an 88 dp/28 dp mark instead of the Home brand
  system.

## Refactoring plan — do not execute without approval

### HIGH

1. Create shared top app bar, primary/secondary actions, base cards, screen column, and state card.
   Affected: approximately 10 feature files plus 2–4 new shared UI files. Complexity: high.
   Risk: high because geometry, scrolling, nested back behavior, and state layouts can regress.
2. Replace production hardcoded colors and uncontrolled Material defaults with approved tokens.
   Affected: Splash, Settings, Scanner, Results, Quick Clean, Trash DNA, Privacy, Reports.
   Complexity: medium. Risk: medium-high because it changes visible output.
3. Add screenshot and accessibility regression coverage at 412 × 917 for every route and nested
   state. Affected: Android-test infrastructure and all screen fixtures. Complexity: high.
   Risk: low to production, medium to CI stability.

### MEDIUM

1. Expand named spacing, shape, icon, elevation, and component-size tokens after design approval.
   Affected: theme tokens plus approximately 9 feature files. Complexity: medium. Risk: medium.
2. Consolidate Quick Clean/Trash DNA and Reports/Privacy duplicate card and metric components.
   Affected: 4 feature files plus shared components. Complexity: medium. Risk: medium.
3. Standardize icon sources and outlined/filled rules against approved assets.
   Affected: Home, bottom navigation, Settings, feature app bars and actions. Complexity: medium.
   Risk: medium.
4. Replace fixed-height or forced-single-line text where localization/font-scale testing proves
   clipping. Affected: Home, bottom navigation, Settings rows, Results rows. Complexity: medium.
   Risk: medium because geometry may change.

### LOW

1. Normalize helper naming and remove obsolete `Improved` prefixes after behavior is stable.
   Affected: Results files. Complexity: low. Risk: low.
2. Add previews for shared components and representative long/RTL copy.
   Affected: shared UI preview files. Complexity: low-medium. Risk: low.
3. Add KDoc explaining decorative versus actionable icon semantics.
   Affected: shared components. Complexity: low. Risk: low.

## Verification baseline

Run on 2026-07-29:

- Build: `assembleDebug` passed.
- Lint: `lintDebug` passed with 0 errors and 327 warnings.
- Unit tests: `testDebugUnitTest` passed; 23 tests in 10 suites, 0 failures, 0 errors,
  0 skipped.
- Gradle emitted one configuration warning: `android.disallowKotlinSourceSets=false` is an
  experimental option.

Lint warning inventory:

| Count | Lint ID | Summary |
|---:|---|---|
| 288 | `Untranslatable` | Protected resources marked non-translatable are repeated in localized catalogs |
| 9 | `PluralsCandidate` | Quantity strings should be reviewed for plural resources |
| 8 | `InlinedApi` | Permission constants were introduced after min SDK 24 |
| 6 | `GradleDependency` | Newer SDK/dependency versions are available |
| 5 | `TypographyEllipsis` | Localized strings use three periods instead of an ellipsis |
| 3 | `UseKtx` | KTX alternatives exist for URI parsing and preferences editing |
| 2 | `UnusedResources` | `scan_supporting_copy` and `results_scan_summary` are unused |
| 2 | `StringFormatCount` | French and Italian `trash_dna_logs_low_body` interpret `10%` as a format token |
| 1 | `OldTargetApi` | Lint considers target SDK 36 older than the installed latest SDK |
| 1 | `NewerVersionAvailable` | Newer Kotlin Compose plugin version is available |
| 1 | `AppBundleLocaleChanges` | Runtime locale changes need bundle-language split configuration or Play Core handling |
| 1 | `DataExtractionRules` | `allowBackup` is deprecated without Android 12 data-extraction rules |

The build also reports that compile SDK 37 and newer Core, Lifecycle, Activity Compose, Compose
BOM, and Navigation Compose versions are available. Version upgrades are not automatically
approved: current project documentation intentionally pins API 36.1-compatible versions.
