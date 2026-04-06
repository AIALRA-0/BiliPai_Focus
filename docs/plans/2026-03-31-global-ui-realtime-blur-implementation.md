# Global UI Realtime Blur Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a global realtime blur master switch for shared UI components while preserving local blur controls and preparing the blur pipeline for future liquid glass expansion.

**Architecture:** Persist a new global blur setting in `SettingsManager`, thread it through aggregated settings state, and enforce it inside the shared blur infrastructure so most existing call sites inherit the behavior automatically. Keep bottom bar blur and liquid glass logic local, but make final blur enablement depend on both the global switch and per-surface local eligibility.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, Haze blur utilities, kotlin.test

---

### Task 1: Persist the global blur setting

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt`
- Test: `app/src/test/java/com/android/purebilibili/core/store/HomeSettingsMappingPolicyTest.kt`

**Step 1: Write the failing test**

Add a test that verifies:
- empty preferences map `globalUiRealtimeBlurEnabled` to `true`
- populated preferences with `global_ui_realtime_blur_enabled = false` map to `false`

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.android.purebilibili.core.store.HomeSettingsMappingPolicyTest`
Expected: FAIL because the new `HomeSettings` field and mapping do not exist yet.

**Step 3: Write minimal implementation**

Update `SettingsManager.kt` to:
- add `globalUiRealtimeBlurEnabled: Boolean = true` to `HomeSettings`
- add a new preference key `global_ui_realtime_blur_enabled`
- map the new key in `mapHomeSettingsFromPreferences(...)`
- expose `getGlobalUiRealtimeBlurEnabled(context)` and `setGlobalUiRealtimeBlurEnabled(context, value)`

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.android.purebilibili.core.store.HomeSettingsMappingPolicyTest`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt app/src/test/java/com/android/purebilibili/core/store/HomeSettingsMappingPolicyTest.kt
git commit -m "feat: add global UI realtime blur setting"
```

### Task 2: Surface the setting in settings state and UI

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt`

**Step 1: Write the failing test**

If there is an existing small settings aggregation test pattern, add a test that proves the new boolean participates in UI state aggregation. If there is no lightweight test seam, skip directly to UI wiring and rely on the dedicated policy tests in later tasks.

**Step 2: Run test to verify it fails**

Run the chosen targeted test command, or note that no suitable unit seam exists for this screen-only wiring.

**Step 3: Write minimal implementation**

Update `SettingsViewModel.kt` to:
- add `globalUiRealtimeBlurEnabled` to `SettingsUiState`
- add it to `ExtraSettings` / `BaseSettings`
- include `SettingsManager.getGlobalUiRealtimeBlurEnabled(context)` in the aggregation flow
- expose `toggleGlobalUiRealtimeBlur(value)`

Update `AnimationSettingsScreen.kt` to:
- add a new `IOSSwitchItem`
- place it in the blur/effects area near header blur and bottom bar blur
- use copy that makes the master-switch behavior clear

**Step 4: Run test to verify it passes**

Run the targeted test if one exists, otherwise rely on compilation and later shared policy tests.

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt
git commit -m "feat: expose global UI blur toggle in settings"
```

### Task 3: Gate shared blur infrastructure with the global switch

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/core/ui/blur/UnifiedBlur.kt`
- Modify: `app/src/main/java/com/android/purebilibili/core/ui/blur/RecoverableVisualEffects.kt`
- Test: `app/src/test/java/com/android/purebilibili/core/ui/blur/GlobalUiRealtimeBlurPolicyTest.kt`

**Step 1: Write the failing test**

Create `GlobalUiRealtimeBlurPolicyTest.kt` with minimal tests for:
- global on + local on => blur enabled
- global off + local on => blur disabled
- global on + local off => blur disabled

Prefer extracting a small pure function for final blur enablement so the test stays cheap and deterministic.

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.android.purebilibili.core.ui.blur.GlobalUiRealtimeBlurPolicyTest`
Expected: FAIL because the policy function does not exist yet.

**Step 3: Write minimal implementation**

In shared blur infrastructure:
- add a pure helper such as `resolveEffectiveRealtimeBlurEnabled(globalEnabled, localEnabled)`
- use it in `Modifier.unifiedBlur(...)`
- use it in `rememberRecoverableHazeState(...)` so `HazeState.blurEnabled` respects the global master switch

Keep defaults backward-compatible by treating unspecified values as enabled.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.android.purebilibili.core.ui.blur.GlobalUiRealtimeBlurPolicyTest`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/core/ui/blur/UnifiedBlur.kt app/src/main/java/com/android/purebilibili/core/ui/blur/RecoverableVisualEffects.kt app/src/test/java/com/android/purebilibili/core/ui/blur/GlobalUiRealtimeBlurPolicyTest.kt
git commit -m "feat: gate shared blur with global realtime setting"
```

### Task 4: Normalize representative runtime call sites

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/MainActivity.kt`
- Modify: `app/src/main/java/com/android/purebilibili/navigation/AppNavigationAppearancePolicy.kt`
- Modify: `app/src/test/java/com/android/purebilibili/navigation/AppNavigationAppearancePolicyTest.kt`

**Step 1: Write the failing test**

Add or extend a navigation appearance test to verify the new global blur flag is exposed where runtime composition needs it.

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.android.purebilibili.navigation.AppNavigationAppearancePolicyTest`
Expected: FAIL because the new appearance field is not yet mapped.

**Step 3: Write minimal implementation**

Patch representative runtime composition points so the global blur state is available where shared haze state is created or passed down:
- include the new flag in `AppNavigationAppearance`
- map it from `HomeSettings`
- make `MainActivity` or root composition pass the correct user-enabled value into global haze state creation where needed

Avoid broad refactors; only patch the root/runtime seam necessary for consistent behavior.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.android.purebilibili.navigation.AppNavigationAppearancePolicyTest`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/MainActivity.kt app/src/main/java/com/android/purebilibili/navigation/AppNavigationAppearancePolicy.kt app/src/test/java/com/android/purebilibili/navigation/AppNavigationAppearancePolicyTest.kt
git commit -m "feat: thread global blur through root navigation state"
```

### Task 5: Verify shared behavior and guard against regressions

**Files:**
- Modify: any directly affected call site files discovered during implementation
- Test: targeted existing tests plus newly added blur policy tests

**Step 1: Write the failing test**

For any outlier surface discovered during manual audit that bypasses the shared gate, add the smallest targeted test around its policy or mapping before patching it.

**Step 2: Run test to verify it fails**

Use a narrow test command for the affected file.

**Step 3: Write minimal implementation**

Patch only real outliers that still blur when the global switch is off. Prefer reusing shared helpers instead of introducing screen-specific settings reads.

**Step 4: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.android.purebilibili.core.store.HomeSettingsMappingPolicyTest
./gradlew testDebugUnitTest --tests com.android.purebilibili.navigation.AppNavigationAppearancePolicyTest
./gradlew testDebugUnitTest --tests com.android.purebilibili.core.ui.blur.GlobalUiRealtimeBlurPolicyTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add <affected files>
git commit -m "test: verify global UI realtime blur behavior"
```

### Task 6: Final verification

**Files:**
- No new files required

**Step 1: Run targeted unit tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.android.purebilibili.core.store.HomeSettingsMappingPolicyTest
./gradlew testDebugUnitTest --tests com.android.purebilibili.navigation.AppNavigationAppearancePolicyTest
./gradlew testDebugUnitTest --tests com.android.purebilibili.core.ui.blur.GlobalUiRealtimeBlurPolicyTest
```

Expected: all PASS

**Step 2: Run compile verification if needed**

Run:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

**Step 3: Manual smoke checklist**

- Open settings and confirm `全局 UI 实时模糊` is visible.
- Turn it off and verify representative shared UI surfaces no longer blur.
- Turn it back on and verify bottom bar, sheets, overlays, and search/common surfaces regain blur when locally allowed.
- Verify bottom bar liquid glass toggle logic still behaves as before.

