# TrashPilot Brand Guidelines

## Brand promise

TrashPilot provides private, understandable, on-device storage care. The product gives users
control; it does not manufacture urgency, fear, performance claims, or cleanup results.

## Voice

- Calm, direct, factual, and respectful.
- Explain what the app reads, stores, changes, and does not change.
- Prefer concrete actions: “Choose folder and scan,” “Review items,” and “Open Android settings.”
- State uncertainty honestly: “Not scanned yet,” “Not recorded,” and “Date unavailable.”
- Avoid hype, gamification, scare language, malware verdicts, and unverifiable percentages.
- Never imply background scanning, cloud intelligence, automatic deletion, or device optimization
  when those behaviors do not exist.

## Product naming

The following protected names remain exactly in English in every locale:

- `SCAN`
- `TrashPilot`
- `Trash DNA`
- `Quick Clean`
- `Privacy Monitor`
- `Reports`
- `Settings`
- `Smart Cleaner`

Rules:

- `SCAN` is always uppercase.
- Preserve capitalization, spacing, and punctuation exactly.
- Exact-name string resources use `translatable="false"`.
- When a protected name appears inside a sentence, translate only the surrounding description.
- Do not abbreviate, pluralize, hyphenate, transliterate, or create alternate localized names.
- `TP` is the approved monogram.

## Approved visual identity

- Home brand color: `#0EA5E9`.
- Home canvas and cards: `#FFFFFF`.
- Home primary text: `#111827`.
- Home secondary text: `#6B7280`.
- Home outline: `#E5E7EB`.
- Logo and icon treatment is monochrome, geometric, and restrained.
- The Home screen is the reference expression of the brand.

## Logo use

- Standard Home mark: 36 dp concentric blue circle with white ring and center.
- Preserve clear space; do not place the mark against a competing colored surface.
- Do not rotate, stretch, recolor, outline, add gradients, or apply decorative effects.
- Splash and About variants must retain the same palette and proportions unless a separately
  approved asset specifies otherwise.

## SCAN action

- SCAN is the principal brand interaction.
- It is a 200 dp double-ring blue action with a 168 dp inner circle and white label.
- Do not add an icon, progress percentage, gradient, alternate label, or competing primary action.
- Pressing SCAN begins an explicit user-controlled storage-selection flow; it must not imply
  background or unrestricted scanning.

## Privacy language

Approved themes include:

- “Your device. Your control.”
- “Private by design.”
- “Scanning stays on your device.”
- “On-device only.”
- “Nothing is deleted automatically.”

Privacy claims must match implementation. Avoid absolute claims if a platform handoff, export, or
user-selected share action can move data outside the app. Describe those exceptions clearly.

## Feature descriptions

- `Trash DNA`: describe local analysis of TrashPilot-created scan and cleanup metadata.
- `Quick Clean`: describe manual review of conservative disposable candidates.
- `Privacy Monitor`: describe Android permission declarations, not malware detection or a security
  score.
- `Reports`: describe recorded local activity only; do not seed examples or forecasts.
- `Settings`: describe on-device preferences, metadata backup/restore, and policies.

## Localization

- Only descriptions and ordinary UI copy are translated.
- Protected product names remain unchanged.
- Native language names in the language picker remain in their native form.
- Preserve XML format tokens and escaping.
- Run `node tools/verify_locales.mjs` after string changes.
- Automated draft translations are release candidates only and require native review before
  linguistic approval.
- Validate RTL layout without changing protected-name spelling.

## Accessibility and inclusivity

- Use plain language and avoid technical blame.
- Do not rely on color, icon, or motion alone.
- Provide localized descriptions for actions and meaningful images.
- Allow text wrapping and font scaling.
- Avoid fear-based permission copy and judgmental cleanup language.

## Brand review checklist

Before approving UI or copy:

1. Are all protected names exact?
2. Is SCAN uppercase and visually dominant only on Home?
3. Does every claim reflect real local behavior and real data?
4. Does the design use only approved colors and components?
5. Is privacy copy precise about export and Android handoffs?
6. Is descriptive copy localized and accessible?
7. Has the result been validated at 412 × 917 and with representative locales?
