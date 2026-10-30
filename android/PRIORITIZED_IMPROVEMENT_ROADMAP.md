# 🚀 **MeetMind_AI — Prioritized Improvement Roadmap**
## **Section 13: 5-Phase Implementation Plan**

---

### **Phase 1: High-Impact & Accessibility Defects (Immediate Priority)**
*Focus: Resolving non-compliant touch target sizes, missing content descriptions, and form input keyboard occlusions.*

| Issue ID | Description | Source File | Fix Complexity | Dependencies | Risk Assessment |
| :---: | :--- | :--- | :---: | :--- | :--- |
| **ISS-02** | Expand top bar icon button touch targets to `48dp x 48dp` | `LiveMeetingScreen.kt` | **Low** | None | Low |
| **ISS-03** | Expand task checkbox touch target to `48dp x 48dp` | `ComprehensiveReportScreen.kt` | **Low** | None | Low |
| **ISS-04** | Add `Modifier.imePadding()` to prevent keyboard CTA occlusion | `CreateMeetingScreen.kt` | **Low** | None | Low |
| **ISS-05** | Add meaningful `contentDescription` labels to text fields & buttons | `CoreComponents.kt` | **Low** | None | Low |
| **ISS-06** | Darken Light Mode `onSurfaceVariant` to `#475569` for WCAG AA contrast | `Theme.kt` | **Low** | None | Low |

---

### **Phase 2: String Externalization & Multi-Language Localization**
*Focus: Refactoring hardcoded string literals into `strings.xml` resource keys to enable localization.*

| Issue ID | Description | Source File | Fix Complexity | Dependencies | Risk Assessment |
| :---: | :--- | :--- | :---: | :--- | :--- |
| **ISS-01** | Extract all 285+ user-facing strings into `res/values/strings.xml` | Project-wide | **Medium** | None | Low (Refactoring string literals) |
| **ISS-13** | Parse ISO-8601 timestamps using `DateTimeFormatter` in Chat | `ChatScreen.kt` | **Low** | None | Low |

---

### **Phase 3: Design System Consistency & Token Consolidation**
*Focus: Unifying corner radii scale, glassmorphism tokens, and theme surface colors.*

| Issue ID | Description | Source File | Fix Complexity | Dependencies | Risk Assessment |
| :---: | :--- | :--- | :---: | :--- | :--- |
| **ISS-07** | Consolidate corner radii into 4 design tokens (`CornerSmall/Medium/Large`) | `Shapes.kt`, `Color.kt` | **Medium** | Phase 2 | Low |
| **ISS-17** | Replace hardcoded `BorderColor` with dynamic `GlassBorder` token | `CoreComponents.kt` | **Low** | Phase 3 | Low |
| **ISS-18** | Standardize `StatusChip` padding using design `Spacing` tokens | `CoreComponents.kt` | **Low** | Phase 3 | Low |
| **ISS-19** | Standardize segmented mode switcher height to `48.dp` | `CreateMeetingScreen.kt` | **Low** | Phase 3 | Low |

---

### **Phase 4: Usability, Empty States & State Invalidation**
*Focus: Enhancing empty state Call-to-Actions, state cleanup on logout, and keyboard navigation.*

| Issue ID | Description | Source File | Fix Complexity | Dependencies | Risk Assessment |
| :---: | :--- | :--- | :---: | :--- | :--- |
| **ISS-08** | Add interactive "Schedule Meeting" CTA inside empty meeting list card | `DashboardScreen.kt` | **Low** | None | Low |
| **ISS-09** | Add "Refresh Notifications" CTA inside empty notifications card | `NotificationsScreen.kt` | **Low** | None | Low |
| **ISS-10** | Add explanatory illustration card inside empty transcript view | `TranscriptScreen.kt` | **Low** | None | Low |
| **ISS-11** | Invalidate cached meeting details in ViewModel on logout | `MeetingViewModel.kt` | **Low** | None | Low |
| **ISS-12** | Invalidate cached user profile state in ViewModel on logout | `ProfileViewModel.kt` | **Low** | None | Low |
| **ISS-14** | Add screen-reader state description to unread notification badges | `NotificationsScreen.kt` | **Low** | None | Low |
| **ISS-15** | Apply `TextOverflow.Ellipsis` and adaptive sizing to 2x2 stats cards | `DashboardScreen.kt` | **Low** | None | Low |
| **ISS-16** | Add `WindowInsets.navigationBars` padding to bottom glass dock | `LiveMeetingScreen.kt` | **Low** | None | Low |
| **ISS-25** | Retain search meeting query across device rotations (`rememberSaveable`) | `SearchMeetingsScreen.kt` | **Low** | None | Low |

---

### **Phase 5: Performance Polish & Micro-Animations**
*Focus: Micro-interactions, visual polish, and animation enhancements.*

| Issue ID | Description | Source File | Fix Complexity | Dependencies | Risk Assessment |
| :---: | :--- | :--- | :---: | :--- | :--- |
| **ISS-21** | Add camera edit icon overlay badge to profile avatar | `ProfileScreen.kt` | **Low** | None | Low |
| **ISS-22** | Add subtle `HorizontalDivider` between settings categories | `SettingsScreen.kt` | **Low** | None | Low |
| **ISS-26** | Apply `.aiGlow(color = PurpleViolet)` to active AI representative cards | `AiRepDashboardScreen.kt` | **Low** | None | Low |
| **ISS-28** | Add entrance scale & alpha fade animation to Splash screen logo | `SplashScreen.kt` | **Low** | None | Low |
| **ISS-30** | Apply spring slide animation to AI Proxy Speech banner entrance | `LiveMeetingScreen.kt` | **Low** | None | Low |
| **ISS-33** | Add animated checkmark transition on profile save success | `EditProfileScreen.kt` | **Low** | None | Low |

---
*End of Section 13: Prioritized Improvement Roadmap.*
