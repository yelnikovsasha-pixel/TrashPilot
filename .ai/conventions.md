# conventions
- Organize code by core, features, and ui/theme; route composables through AppNavigation; apply TrashPilotTheme at the activity root; use MaterialTheme tokens, Compose modifiers, dynamic color when supported, and privacy-conscious offline-first UX copy.
- Splash visuals use a white surface, a Compose-native TP mark, theme typography, and explicit restrained colors; animation and delayed navigation are lifecycle-aware through LaunchedEffect.
- Before real device data exists, Storage must show `Not scanned yet`; never display fake percentages, capacity figures, or optimization results.
- Keep Scan as the only visually dominant Home action; navigation actions remain in the Material 3 bottom bar.
- Home content excludes Motivation, Quick Clean, and Settings cards. Settings is available only through bottom navigation.
- Storage access must use explicit user-selected document trees. Do not request broad storage access or scan in the background.
- Scan results remain local and read-only; deletion requires a separate future flow with explicit per-action confirmation.
- File categories are exclusive. Files inside Download/Downloads are categorized as Downloads before MIME or extension classification.
