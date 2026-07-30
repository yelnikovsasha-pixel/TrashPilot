# TrashPilot Design System

## Scope and authority

This file defines approved visual tokens and component specifications. Use it together with
`UI_BIBLE.md` and `BRAND_GUIDELINES.md`. Existing production exceptions are documented in the
UI Bible audit; they are not additional design tokens.

## Color tokens

| Token | Hex | Approved use |
|---|---:|---|
| Background | `#FBFAFD` | App canvas, navigation bar, base surface |
| Primary | `#3C3467` | Primary actions, icon tint, emphasized text |
| On Primary | `#FFFFFF` | Text/icons on Primary |
| Text | `#1B1824` | Primary text and navigation content |
| Text Secondary | `#524D5D` | Descriptions, metadata, assurance copy |
| Card | `#F5F2F9` | Standard low-emphasis cards |
| Accent Surface | `#E9E2F7` | Icon containers, selected indicator, highlighted cards |
| Outline | `#D8D2E2` | Approved borders and outlines |
| Home Blue | `#0EA5E9` | Home logo, SCAN, progress and feature icon circles |
| Home Ink | `#111827` | Home titles |
| Home Secondary | `#6B7280` | Home supporting copy |
| Home Outline | `#E5E7EB` | Home card and navigation dividers |

Rules:

- Use theme roles or named tokens, never screen-local hex values.
- Dynamic color is disabled.
- The approved system is light-only.
- Do not add status colors unless a real product requirement and an approved design define them.
- Never use color alone to communicate state.

## Typography

Typeface: Android default Roboto family.

| Material role | Weight | Size | Line height | Typical use |
|---|---:|---:|---:|---|
| `headlineSmall` | Semibold | 24 sp | 30 sp | Brand and prominent screen headings |
| `titleLarge` | Semibold | 22 sp | 28 sp | Section/screen emphasis and SCAN base style |
| `titleMedium` | Semibold | 16 sp | 22 sp | Card titles |
| `titleSmall` | Medium | 14 sp | 20 sp | Compact titles |
| `bodyLarge` | Regular | 16 sp | 24 sp | Long body copy |
| `bodyMedium` | Regular | 13 sp | 18 sp | Descriptions |
| `bodySmall` | Regular | 12 sp | 16 sp | Notes and metadata |
| `labelLarge` | Medium | 14 sp | 20 sp | Primary rectangular actions |
| `labelMedium` | Medium | 12 sp | 16 sp | Navigation labels |
| `labelSmall` | Medium | 11 sp | 15 sp | Supporting microcopy |

Use these roles directly. Do not create 18 sp or 19 sp local headings without adding an approved
role first. Weight changes must preserve the role's approved size and line height.

## Spacing and dimensions

### Named production tokens

| Token | Value |
|---|---:|
| Screen padding | 24 dp |
| Content top padding | 20 dp |
| Standard card gap | 12 dp |
| Card radius | 24 dp |
| Home card padding | 14 dp |
| Card icon container | 48 dp |
| Card icon | 24 dp |
| Card icon radius | 16 dp |
| Bottom bar height | 80 dp |
| Bottom bar icon | 22 dp |
| Selected indicator | 64 × 32 dp |
| Primary rectangular action height | 52 dp |
| Primary rectangular action radius | 18 dp |
| Top app bar height | 64 dp |
| Home hero spacing | 20 dp |
| Home storage card height | 92 dp |
| Home quick-action gap | 10 dp |
| Home quick-action height | 84 dp |

### Spacing scale

Approved default spacing should be selected from 4, 8, 12, 16, 20, 24, and 32 dp. The Home
reference also uses intentional 6, 10, and 14 dp values. Values outside this set require an
approved component specification.

## Shapes

- Standard card: 24 dp.
- Home icon container: 16 dp.
- Home brand mark: 18 dp.
- Selected navigation indicator: 16 dp.
- SCAN: circle.
- Primary rectangular action: 18 dp.
- Do not infer new shape tokens from current implementation exceptions.

## Elevation and shadows

- Canvas, navigation, and ordinary cards: zero tonal elevation.
- SCAN is the primary elevated surface and uses a restrained shadow.
- Do not rely on unspecified Material component elevation.
- A future code token should record the approved SCAN shadow before any shadow refactor.

## Layout

- Reference width: 412 dp at 160 dpi.
- Horizontal page padding: 24 dp, leaving 364 dp content width.
- Use the persistent 80 dp bottom bar on every route.
- Nested content must measure within the app-shell content area.
- Use top alignment for information-dense screens; centered layouts are reserved for hero,
  loading, empty, and focused scanner states.
- Use lazy scrolling for dynamic lists and localized content.
- Never overlap content, snackbar, dialog action, or primary action with the bottom bar.

## Component specifications

### Brand mark

- Home mark: 36 dp blue concentric circle with white ring and center.
- The TrashPilot and Smart Cleaner lockup follows Figma node `1:65`.

### Feature card

- Standard: Card background, 24 dp radius, 14 dp padding.
- Icon container: Accent Surface, 48 dp square, 16 dp radius.
- Icon: Primary, 24 dp.
- Title: `titleMedium`.
- Description: `bodyMedium`, Text Secondary.
- Storage status: `bodyMedium`, medium weight, Primary.
- Storage supporting copy: `labelSmall`, Text Secondary.

### Information and metric cards

- Default radius: 24 dp.
- Default surface: Card.
- Highlighted surface: Accent Surface.
- Use 18 or 20 dp internal padding only when the approved screen requires the denser data-card
  pattern.
- Label/value alignment must remain readable under localization and font scaling.

### Primary actions

- Filled Primary background with On Primary content.
- Rectangular action: full content width where specified, 52 dp height.
- Label: `labelLarge`.
- One filled primary action per screen.
- SCAN is a unique 200 dp double-ring circle with a 168 dp blue inner action.

### Secondary actions

- Use approved outlined or text treatment.
- Match primary action height when paired side by side.
- Do not introduce a second visually dominant filled action.

### Inputs and selection controls

- Use theme colors and explicit approved shape.
- Search controls must have a localized hint and accessible semantics.
- Selected state must use text/icon semantics in addition to color.
- Filter and radio controls must retain at least 48 dp touch targets.

### Dialogs and state UI

- Dialog titles, messages, buttons, and error details use string resources.
- Destructive confirmation identifies exactly what will be affected.
- Loading, empty, error, and success states use shared hierarchy and spacing.
- Do not insert sample or fabricated data into runtime state UI.

## Iconography

- Monochrome only.
- Primary tint on light surfaces.
- Use approved exported assets when a Material glyph is not visually identical.
- Use filled icons for bottom navigation and follow the approved asset state elsewhere.
- Avoid Unicode characters as icons.

## Animation

- Splash fade: 700 ms.
- Splash hold/navigation: approximately 2 seconds.
- Progress animation may represent actual local work only.
- No decorative continuous motion.

## Accessibility and validation

- Minimum target: 48 dp.
- Validate TalkBack labels, switch/radio state, traversal, and system back.
- Validate 200% font scale for clipping and overlap.
- Validate English, a long Latin locale, Arabic RTL, and CJK.
- Validate screenshots at 412 × 917.
- Run locale verification, build, lint, and unit tests.

## Code mapping

- Colors: `app/src/main/java/com/trashpilot/app/ui/theme/Color.kt`
- Typography: `app/src/main/java/com/trashpilot/app/ui/theme/Type.kt`
- Dimensions: `app/src/main/java/com/trashpilot/app/ui/theme/DesignTokens.kt`
- Theme mapping: `app/src/main/java/com/trashpilot/app/ui/theme/Theme.kt`
- App shell: `app/src/main/java/com/trashpilot/app/core/navigation/AppNavigation.kt`
- Bottom navigation:
  `app/src/main/java/com/trashpilot/app/core/navigation/TrashPilotBottomBar.kt`
- Shared components:
  `app/src/main/java/com/trashpilot/app/ui/components/TrashPilotComponents.kt`
