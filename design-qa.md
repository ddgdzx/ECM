**Design QA — Arxan ECM option 3**

**Source and evidence**

- Source visual truth: `/Users/developer/.codex/generated_images/019fb865-a17d-7a73-9540-1c96fdce66b4/exec-f1c7d301-9115-48f9-a9d4-c3da3401956b.png`
- Source pixels: 1686 × 933, three-screen concept board.
- Implementation screenshots:
  - `/Users/developer/.codex/visualizations/2026/07/31/019fb865-a17d-7a73-9540-1c96fdce66b4/arxan-home-v1.png`
  - `/Users/developer/.codex/visualizations/2026/07/31/019fb865-a17d-7a73-9540-1c96fdce66b4/arxan-consume-v2.png`
  - `/Users/developer/.codex/visualizations/2026/07/31/019fb865-a17d-7a73-9540-1c96fdce66b4/arxan-settings-v2.png`
- Implementation pixels: 1170 × 2532 each, iPhone 16e at 390 × 844 points and @3x density. CSS size and deviceScaleFactor: not applicable to this native SwiftUI build.
- Combined comparison: `/Users/developer/.codex/visualizations/2026/07/31/019fb865-a17d-7a73-9540-1c96fdce66b4/arxan-option3-comparison-v2.jpg`
- Density normalization: each native capture was proportionally downsampled to 562 × 1216 and placed directly below its corresponding source screen. Device chrome was retained but excluded from app-content judgments.
- State: light theme; one resistor with 50 units; consumption quantity 5 with required detail entered; Simplified Chinese selected.

**Full-view comparison evidence**

- Information hierarchy matches the selected direction: compact white quick-action card, orange consumption mark, blue text action, four-item bottom dock, one custom consumption sheet, and a dedicated preferences screen.
- The intentional brand change replaces the source title “元件库” with “Arxan ECM”; the inventory tab remains “元件库”.
- Native safe areas and system sheet chrome account for the main vertical differences from the bezel-free concept board.

**Focused comparison evidence**

- Consumption controls were inspected at full @3x resolution because their touch targets, selected states, footer hierarchy, and required-detail state were not legible enough in the combined board alone.
- Settings was inspected at full @3x resolution to verify the blue-tinted preference header, five language rows, selected checkmark, appearance row, and About section.

**Findings**

- No actionable P0, P1, or P2 differences remain.
- Typography uses native SF system styles with comparable weights and hierarchy; long localized labels use native wrapping/truncation behavior.
- Spacing, 12–16 point radii, grouped backgrounds, chip rhythm, and persistent footer placement align with the source direction.
- Colors use system blue for selection/navigation, system orange only for consumption, semantic grouped surfaces, and accessible foreground contrast.
- Image and icon fidelity passes: the supplied app icon remains raster artwork; visible UI icons use SF Symbols/Material Icons rather than generated stand-ins.
- Copy covers the selected flow and all five requested language choices. Arxan ECM remains invariant across languages.

**Comparison history**

- Iteration 1 found P2 drift in the iOS quantity shortcuts and footer: shortcuts rendered as small square controls, and native bordered buttons changed the intended hierarchy. The settings header also lacked the source’s blue tint.
- Fixes: replaced quantity shortcuts with equal-width capsules, replaced footer controls with deterministic neutral/orange rounded buttons, reduced stepper controls, and added the blue-tinted settings header on both platforms.
- Post-fix evidence: `arxan-option3-comparison-v2.jpg`; the three issues are visibly resolved.

**Interaction verification**

- Opened the quick-consumption entry, selected quantity 5, confirmed the required-detail gate, and saved a record.
- Verified inventory changed from 50 to 45 and the component detail shows the individual record with quantity, remaining stock, timestamp, and detail.
- Switched to Español and verified settings and navigation copy updated, then restored Simplified Chinese.

**Follow-up polish**

- P3: the concept board shows several component cards while the simulator fixture contains one. This is an acceptable data-state difference and horizontal scrolling is implemented for additional components.

**Implementation checklist**

- [x] Option 3 home action card
- [x] Single custom consumption flow
- [x] Required per-record detail and live remaining stock
- [x] Four-tab dock with Settings
- [x] Five persistent language choices
- [x] Arxan ECM internal screen branding and external display/product name
- [x] Android APK and iOS simulator builds

final result: passed
