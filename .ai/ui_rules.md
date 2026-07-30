Global UI Design Rules



\# TrashPilot — Global UI Design Rules



Version: 1.0



This document defines the global UI and UX rules for the entire TrashPilot project.



These rules apply to every screen, every component and every future UI change.



\---



\# 1. Design Foundation



Before implementing or redesigning ANY screen, always review the Figma page:



\## 00 Design Foundation



This page is the single source of truth for the visual language of TrashPilot.



It defines:



\- layout principles

\- visual hierarchy

\- spacing

\- typography

\- colors

\- corner radius

\- shadows

\- icon style

\- component proportions

\- navigation

\- animations

\- overall product personality



Do NOT copy the screen literally.



Instead, use it as the visual foundation for every screen.



Every new screen must immediately feel like part of the same application.



If a design decision is not explicitly defined, follow the style established in "00 Design Foundation" instead of inventing a new pattern.



\---



\# 2. Design Philosophy



TrashPilot is built around simplicity.



The interface must feel:



\- calm

\- modern

\- lightweight

\- trustworthy

\- premium

\- minimalist



Every screen should have one clear primary purpose.



Avoid visual noise.



Remove unnecessary decorations.



Every element must have a reason to exist.



\---



\# 3. Visual Consistency



Maintain consistency across the entire application.



Always reuse the existing design language.



Maintain consistent:



\- spacing

\- margins

\- typography

\- iconography

\- colors

\- shadows

\- corner radius

\- elevation

\- animations



Never introduce a different visual style.



Never mix multiple design styles.



\---



\# 4. Components



Always reuse existing shared components whenever possible.



Avoid duplicate implementations.



Shared components should include:



\- cards

\- buttons

\- app bars

\- navigation

\- dialogs

\- list items

\- progress indicators

\- empty states

\- loading states



If a new component is necessary:



\- make it reusable

\- follow the existing design language

\- document it



\---



\# 5. Navigation



Navigation should always remain consistent.



Bottom navigation must behave identically across every screen.



Icons, spacing and interaction patterns should remain consistent.



Users should never need to relearn navigation.



\---



\# 6. Animations



Animations should feel natural.



Rules:



\- subtle

\- smooth

\- fast

\- never distracting



Avoid unnecessary movement.



Animations should support usability, not decoration.



\---



\# 7. Ambient Messages



Ambient Messages are one of the signature features of TrashPilot.



Purpose:



Create a calm and positive atmosphere without distracting the user.



Ambient Messages are NOT:



\- notifications

\- advertisements

\- tips

\- warnings

\- dialogs



Rules:



\- short

\- motivational

\- calming

\- positive

\- respectful

\- never manipulative

\- never create urgency

\- never pressure the user

\- never interrupt interaction

\- never clickable



Behavior:



\- fade in smoothly

\- remain visible for several seconds

\- fade out smoothly

\- appear naturally

\- change occasionally

\- avoid frequent repetition



Implementation:



Ambient Messages must be implemented as a reusable component.



Localization is required for every supported language.



Examples:



Clean space. Clear mind.



Small steps. Better habits.



Your phone. Your control.



Privacy begins with awareness.



A little cleaner every day.



\---



\# 8. Accessibility



Maintain accessibility.



Ensure:



\- readable typography

\- sufficient contrast

\- touch targets of appropriate size

\- scalable layouts

\- screen reader compatibility



Accessibility improvements must never break the Design Foundation.



\---



\# 9. Performance



UI must remain lightweight.



Avoid:



\- unnecessary recompositions

\- heavy animations

\- excessive nesting

\- duplicated layouts



Every screen should feel responsive.



\---



\# 10. Privacy



The interface should reinforce the privacy-first philosophy.



Never suggest that user data leaves the device unless explicitly implemented.



Avoid misleading claims.



Display only real information.



\---



\# 11. Localization



Every user-facing string must be localizable.



Avoid hardcoded text.



Respect text expansion in longer languages.



Maintain layout quality across all supported languages.



\---



\# 12. Implementation Quality



Prefer:



\- reusable components

\- clean architecture

\- semantic naming

\- maintainable code



Avoid quick fixes that reduce long-term consistency.



\---



\# 13. Screen Evaluation



Before considering a screen complete, verify:



✓ follows Design Foundation



✓ matches TrashPilot visual identity



✓ uses shared components



✓ remains visually consistent



✓ maintains accessibility



✓ supports localization



✓ builds successfully



✓ passes tests



\---



\# 14. Final Report



After completing every UI task, provide:



1\. Summary of implemented changes.



2\. Which Design Foundation principles were applied.



3\. Which shared components were reused.



4\. Any newly created reusable components.



5\. Build status.



6\. Test status.



7\. Before / After comparison.



8\. Any recommendations for future improvements.



\---



End of document.

