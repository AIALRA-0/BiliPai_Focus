# DLNA Cast Plugin Project

This document records the implementation context and verification history for the BiliPai DLNA cast plugin.

## Existing Context

- Native plugins are registered in `app/src/main/java/com/android/purebilibili/app/PureApplication.kt`.
- The plugin framework lives in `app/src/main/java/com/android/purebilibili/core/plugin/`.
- Built-in plugins live in `app/src/main/java/com/android/purebilibili/feature/plugin/`.
- Existing DLNA casting internals live in `app/src/main/java/com/android/purebilibili/feature/cast/`.
- Existing player cast UI is wired in `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt`.
- Google Cast has its own project document: `docs/GOOGLE_CAST_PLUGIN.md`.

## Architecture Decision

Move DLNA behind the same `CastPluginApi` boundary used by Google Cast, while keeping existing DLNA device discovery, labels, and cast behavior intact:

- Add a built-in native `DlnaCastPlugin` entry so DLNA can be enabled or disabled independently.
- Keep `DlnaManager`, `SsdpDiscovery`, and `SsdpCastClient` as DLNA internals owned by the plugin adapter.
- Keep `DeviceListDialog` protocol-agnostic: it starts/stops/refreshes enabled `CastPluginApi` implementations and renders generic plugin routes.
- Keep DLNA playback controls unsupported for this slice because the existing DLNA path does not provide reliable remote playback state.

### Slice 1: Refactor DLNA Into Cast Plugin

Goal: make DLNA a built-in native `CastPluginApi` implementation at the same architectural level as Google Cast, while keeping current DLNA behavior and UI unchanged.

Status: completed on branch `feature/dlna-cast-plugin`, stacked on `feature/google-cast-plugin`.

Result:

- Added `DlnaCastPlugin` under `feature/plugin/dlna/`; it owns existing `DlnaManager`, `SsdpDiscovery`, and `SsdpCastClient` interactions.
- Represented Cling devices and SSDP fallback devices as plugin routes with stable IDs `cling:<udn>` and `ssdp:<location>`.
- Moved DLNA/SSDP discovery state out of `DeviceListDialog`; the dialog now starts/stops/refreshes enabled `CastPluginApi` implementations and renders generic plugin routes.
- Registered `DlnaCastPlugin` as a built-in plugin so DLNA can be enabled/disabled independently from Google Cast.
- Removed direct DLNA lifecycle management from `VideoPlayerOverlay` and `MainActivity`; the app shell now reaches DLNA through plugin registration and the generic cast dialog.
- Kept DLNA playback controls unsupported for this slice; successful DLNA casts do not activate the app's remote playback-control UI, while Google Cast still can when its playback state is active.
- Kept focused behavior tests for DLNA route mapping/cache selection and overlay cast-state policy. Metadata-only plugin shell tests were removed as low-value coverage.

Verification on 2026-05-26:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon
.\gradlew.bat :app:compileDebugKotlin --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
git diff --check
```

All Gradle commands completed with `BUILD SUCCESSFUL`; `git diff --check` reported no whitespace errors.

## Progress Log

- 2026-05-26: Started `feature/dlna-cast-plugin` to move DLNA behind `CastPluginApi` as a built-in native plugin.
- 2026-05-26: Completed Slice 1: DLNA now lives behind `CastPluginApi` with generic cast dialog discovery and route rendering.
