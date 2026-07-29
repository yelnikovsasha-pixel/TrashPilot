# TrashPilot Project Instructions
## Project mission
- Build a minimal privacy-first offline-first Android cleaner; avoid fake optimization claims and use one primary action per screen.
## Architecture rules
- Use Kotlin, Compose, and Material 3; keep shared code in core, features in features, visual tokens in ui/theme, navigation in AppNavigation, and setup in MainActivity.
## Kotlin and Jetpack Compose conventions: Use idiomatic Kotlin, immutable state by default, state hoisting where practical, MaterialTheme tokens, Compose Modifiers, and string resources for new user-facing copy.
## Figma workflow: Inspect relevant pages, components, styles, and variables before work; reuse system tokens and components; validate meaningful visual changes with screenshots.
## Default Figma design source
- Use the TrashPilot Design System file for design work: https://www.figma.com/design/GIpl7h9YLIvYnAavxMoGGz/TrashPilot-Design-System?node-id=8-2
- File key: `GIpl7h9YLIvYnAavxMoGGz`; default entry node: `8:2`.
## Coding standards: Prefer readable focused changes, follow existing naming and dependency conventions, avoid unrelated refactors, and handle errors and permissions explicitly.
## Documentation rules: Keep docs and .ai concise and factual. Do not overwrite existing files unless explicitly requested. Always update relevant .ai documentation after major feature, architecture, design, dependency, or workflow changes.
## Git workflow: Inspect the working tree, preserve unrelated changes, keep commits focused, avoid destructive Git commands, and do not commit, push, or change remotes unless explicitly requested.
## Testing rules: Add or update focused unit and Android/UI tests for meaningful behavior. Run the narrowest relevant Gradle checks and report executed tests, failures, and unverified areas honestly.
## AI behavior rules: Read project instructions before substantial work; make the smallest safe change; state assumptions and blockers; preserve existing files unless explicit overwrite approval is given; update .ai documentation after major changes.
