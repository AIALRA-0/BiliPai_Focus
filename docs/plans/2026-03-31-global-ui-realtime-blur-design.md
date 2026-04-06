# Global UI Realtime Blur Design

**Problem**

The app currently mixes several blur entry points:
- local component toggles such as home header blur and bottom bar blur
- direct `rememberRecoverableHazeState()` calls spread across feature screens
- `Modifier.unifiedBlur(...)` as the shared blur rendering path

This works for isolated surfaces, but it does not provide one global capability switch for realtime blur. As a result, "global UI blur" cannot be disabled consistently, and future liquid glass work would have to keep re-implementing the same enablement checks across screens.

**Goals**

- Add a single global toggle for realtime blur on shared UI components.
- Keep existing local controls such as bottom bar blur working as fine-grained switches.
- Ensure shared surfaces receive appropriate blur behavior by default.
- Prepare a stable capability layer for future liquid glass expansion.

**Non-Goals**

- Replace existing bottom bar blur or liquid glass controls.
- Redesign blur intensity, blur budget, or component visuals in this change.
- Convert every visual effect into one new framework in a single pass.

**Proposed UX**

Add a new toggle named `全局 UI 实时模糊` in the blur/effects area of the settings flow. It acts as a global master switch for realtime blur across shared UI components.

Behavior:
- When enabled, shared UI surfaces may apply blur if their local component policy also allows it.
- When disabled, shared UI surfaces must not apply realtime blur, regardless of local blur eligibility.
- Existing bottom bar blur and liquid glass mutual exclusion remains unchanged.

Suggested layout:

```text
Animation & Effects
|- Liquid Glass
|- Top Bar Blur
|- Bottom Bar Blur
|- Global UI Realtime Blur
`- Blur Intensity
```

**Architecture**

Introduce a global blur capability flag in persistent settings, then consume it in the shared blur infrastructure instead of wiring the decision independently in every feature screen.

The implementation should flow through three layers:

1. Settings state
- Add `globalUiRealtimeBlurEnabled` to persisted settings with default `true`.
- Surface it through `HomeSettings`, `SettingsUiState`, and `SettingsViewModel`.

2. Shared visual effect gate
- Update shared blur helpers so final blur enablement becomes:
  `globalUiRealtimeBlurEnabled && localEnabled`
- This gate should live close to `unifiedBlur(...)` and `rememberRecoverableHazeState(...)`, not inside individual screens.

3. Screen/component adoption
- Existing call sites that already use shared helpers should inherit the new behavior automatically.
- Only special cases that bypass shared helpers should be patched explicitly.

**Why This Design**

This keeps the current local controls intact while giving the app one consistent blur capability switch. It minimizes the chance of missed screens, reduces repeated settings reads in feature code, and creates a clean future seam for liquid glass:

- today: `global blur enabled && local blur enabled`
- later: `global visual effects enabled && surface effect mode allows blur/liquid glass`

That means future liquid glass rollout can reuse the same capability path instead of duplicating per-screen state checks.

**Data Model Changes**

Add a new boolean preference key:
- `global_ui_realtime_blur_enabled`

Add corresponding fields to:
- `HomeSettings`
- `SettingsUiState`
- `ExtraSettings` / `BaseSettings` aggregation in `SettingsViewModel`

Default:
- `true`, to preserve current runtime behavior for existing users

**Runtime Rules**

- Global off always wins over local on.
- Global on does not force blur everywhere; components still need a local blur-eligible path.
- Bottom bar local blur toggle still only controls bottom navigation blur.
- Liquid glass exclusivity rules for bottom bar remain local to that visual mode.

**Expected Coverage**

The global switch should cover shared UI surfaces that already depend on shared blur helpers, including common cases such as:
- top bars using `unifiedBlur(...)`
- drawers and sheets
- overlays
- search surfaces
- common list and profile blur surfaces

Feature-specific direct blur paths should be reviewed and normalized only where needed.

**Risks**

1. Some screens create `HazeState` directly and may bypass the global gate.
Mitigation: audit `rememberRecoverableHazeState()` and direct `unifiedBlur(...)` usage, then patch only true outliers.

2. Multiple settings flows can increase recomposition churn.
Mitigation: piggyback on existing aggregated settings flows instead of adding ad hoc `collectAsState` calls in feature screens.

3. Future liquid glass work may need a broader "global visual effects" concept.
Mitigation: keep the new flag focused on realtime blur now, but place the effective enablement logic in shared infrastructure so it can be generalized later.

**Testing Strategy**

- Add mapping tests for the new persisted setting in `HomeSettingsMappingPolicyTest`.
- Add policy tests for the new global blur gate in shared blur helpers.
- Add a settings/view-model level test if there is an existing pattern for new boolean settings aggregation.
- Smoke-check one or two representative call sites that should stop blurring when the global switch is off.

**Rollout Notes**

- Defaulting the new flag to `true` keeps existing users visually unchanged after update.
- The toggle name should communicate that it is a master switch, not a replacement for bottom bar-only blur.

