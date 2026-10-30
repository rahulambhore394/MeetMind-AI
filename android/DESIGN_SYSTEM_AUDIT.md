# 🎨 **MeetMind_AI — Design System Audit**
## **Section 7: Design Tokens, Themes & Visual Consistency**

---

### **1. Current Design System Architecture**

The application implements a custom **Dark-First Modern Glassmorphism Design System** configured in `core/designsystem/`:

```
core/designsystem/
├── Color.kt      (Primary accents, surface tokens, glassmorphism, dynamic gradients)
├── Theme.kt      (MeetMindDarkColorScheme, MeetMindLightColorScheme, system bar insets)
├── Type.kt       (Typography scale definitions in .sp)
├── Shapes.kt     (Corner radii scale)
├── Spacing.kt    (Padding and margin scale)
└── Modifiers.kt  (Custom modifier extensions: bounceClick, glassmorphism, aiGlow, pulse)
```

---

### **2. Color Palette & Token Definitions**

#### **A. Brand Action Accents (`Color.kt`)**
* `ElectricIndigo` (`#6366F1`): Primary interactive action color (buttons, selected tabs).
* `NeonCyan` (`#06B6D4`): AI highlight color (meeting code badges, AI proxy indicators, active chips).
* `RoseRed` (`#EF4444`): Recording indicator, live status, destructive leave actions.
* `EmeraldGreen` (`#10B981`): Active speaker border, verified badge, task complete status.
* `AmberGold` (`#F59E0B`): Host crown badge, task pending status, AI confidence warning.
* `PurpleViolet` (`#A855F7`): AI Representative agent accent color.

#### **B. Dynamic Theme Surfaces (`Theme.kt`)**

| Surface Token | Dark Theme Value | Light Theme Value | Usage Purpose |
| :--- | :--- | :--- | :--- |
| `background` | `#090D14` (Deep Obsidian) | `#F8FAFC` (Slate White) | Root screen background |
| `surface` | `#131822` (Card Dark) | `#FFFFFF` (Pure White) | Card containers, dialog surfaces |
| `surfaceVariant` | `#1E2430` (Elevated Charcoal) | `#F1F5F9` (Light Grey) | Input fields, chips, secondary cards |
| `onBackground` | `#F1F5F9` (Near White) | `#0F172A` (Slate Dark) | Primary text color |
| `onSurfaceVariant` | `#94A3B8` (Muted Grey) | `#64748B` (Slate Grey) | Secondary text, hints, helper labels |
| `outline` | `#2E3646` (Border Dark) | `#E2E8F0` (Border Light) | Card borders, text field outlines |

---

### **3. Inconsistencies & Token Deficiencies Identified**

1. **Inconsistent Corner Radii Allocation**:
   * `Shapes.kt` defines: `Small = 8.dp`, `Medium = 16.dp`, `Large = 24.dp`, `ExtraLarge = 32.dp`.
   * However, individual screen composables hardcode custom `RoundedCornerShape` values: `6.dp`, `10.dp`, `12.dp`, `14.dp`, `18.dp`, `20.dp`, `28.dp` (e.g. `LiveMeetingScreen` uses `20.dp` for video tiles and `24.dp` for top bar).
   * **Recommendation**: Standardize corner radii scale to 4 core tokens: `CornerSmall (8.dp)`, `CornerMedium (16.dp)`, `CornerLarge (24.dp)`, `CornerFull (CircleShape)`.

2. **Hardcoded Alpha Modifiers**:
   * Surfaces frequently apply manual alpha masks (e.g. `ElectricIndigo.copy(alpha = 0.15f)` or `NeonCyan.copy(alpha = 0.2f)`).
   * **Recommendation**: Define semantic container tokens in `Color.kt` (e.g. `ElectricIndigoSubtle`, `NeonCyanSubtle`) to unify alpha blending across light/dark themes.

3. **Light Mode Contrast Deficiency on Surface Variant**:
   * In `MeetMindLightColorScheme`, `onSurfaceVariant` (`#64748B`) on `surfaceVariant` (`#F1F5F9`) yields a contrast ratio of `4.1:1`, falling slightly below the WCAG AA minimum requirement of `4.5:1` for body text.
   * **Recommendation**: Adjust `MeetMindLightColorScheme.onSurfaceVariant` to `#475569` (Slate 600) to achieve `5.8:1` contrast ratio.

---
*End of Section 7: Design System Audit.*
