# Google Cast Plugin Project

This document records the implementation context and verification history for the BiliPai Google Cast plugin.

## Existing Context

- Native plugins are registered in `app/src/main/java/com/android/purebilibili/app/PureApplication.kt`.
- The plugin framework lives in `app/src/main/java/com/android/purebilibili/core/plugin/`.
- Built-in plugins live in `app/src/main/java/com/android/purebilibili/feature/plugin/`.
- Existing DLNA casting lives in `app/src/main/java/com/android/purebilibili/feature/cast/`.
- Existing player cast UI is wired in `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt`.
- Existing BiliBili TV cast URL policy is in `app/src/main/java/com/android/purebilibili/data/repository/VideoCastPolicy.kt`.

## Architecture Decision

Implement Google Cast as a native plugin accessed through the CastPluginApi boundary, so the player overlay discovers and loads cast routes generically without hosting Google Cast specifics directly:

- Add a native plugin entry so the feature appears in the plugin center and can be enabled or disabled.
- Add a small cast-provider abstraction so the player overlay can offer DLNA and Google Cast without mixing protocol details into UI code.
- Use Google Cast CAF for Chromecast discovery/session/media loading, and reuse the current BiliBili TV cast URL fallback path where possible.
- Keep DLNA behavior intact and avoid replacing the existing DLNA manager in this project.

Official references checked on 2026-05-26:

- Google Cast Android sender setup recommends `play-services-cast-framework:22.3.1` and a Cast options provider.
- AndroidX MediaRouter latest stable is `androidx.mediarouter:mediarouter:1.8.1`.
- Media3 CastPlayer docs exist, but this slice starts with CAF session/media loading to avoid a broad player-service rewrite.

## Slices

### Slice 0: Baseline And Scaffolding

Goal: confirm the worktree, baseline focused tests, and add this documentation entry.

Status: complete.

Verification target:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*"
```

Result on 2026-05-26: passed with `BUILD SUCCESSFUL`.

### Slice 1: Google Cast Plugin Shell

Goal: add build/manifest wiring, a native plugin class, and small policy tests proving enablement metadata and route visibility behavior.

Status: complete.

Likely files:

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/android/purebilibili/app/PureApplication.kt`
- `app/src/main/java/com/android/purebilibili/feature/plugin/googlecast/GoogleCastPlugin.kt`
- New focused tests under `app/src/test/java/com/android/purebilibili/feature/cast/`

Result on 2026-05-26:

- Added the native `GoogleCastPlugin` shell and registered it as an eighth built-in plugin.
- Added Google Cast CAF and MediaRouter dependencies.
- Added the Cast options provider manifest metadata and default receiver policy.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*GoogleCast*" --no-daemon`.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon`.

### Slice 2: Chromecast Discovery And Selection

Goal: discover Google Cast routes, present them alongside existing DLNA entries, and honor plugin enabled state.

UI requirement: Google Cast device rows must match the existing DLNA cast list layout, spacing, selection behavior, dialog structure, and loading/empty states. Only the route-specific icon and concise Google Cast label should differ.

Status: complete.

Likely files:

- `app/src/main/java/com/android/purebilibili/core/plugin/CastPluginApi.kt`
- Google Cast manager/provider files under `feature/plugin/googlecast/`
- `DeviceListDialog.kt`
- Focused discovery/presentation policy tests

Result on 2026-05-26:

- Added the generic `CastPluginApi` boundary so the host cast dialog handles plugin cast providers without importing Google Cast protocol classes.
- Added MediaRouter-based Google Cast route discovery inside the native Google Cast plugin implementation.
- Added route filtering/presentation policy tests for default/Bluetooth routes, cast-category support, fallback names, and route mapping.
- Added plugin-provided cast routes to the existing device dialog using the same DLNA row structure and interaction pattern; only the icon and plugin label differ.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*GoogleCast*" --no-daemon`.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon`.

### Slice 3: Chromecast Media Load

Goal: when a Chromecast device is selected, resolve the current playable cast URL and load it through the active Google Cast session.

Status: complete.

- `feature/plugin/googlecast/GoogleCastMediaLoader.kt`
- `VideoPlayerOverlay.kt`
- Focused tests for media metadata/request construction and URL fallback decisions

Result on 2026-05-26:

- `VideoPlayerOverlay` now calls enabled cast plugins through `CastPluginApi` after resolving the same cast URL path used by DLNA/SSDP.
- The Google Cast plugin builds CAF `MediaLoadRequestData` with buffered stream type, `video/mp4` default content type, autoplay, and title/subtitle metadata policy.
- MediaRouter, CastContext, session polling, and RemoteMediaClient load calls run on the main dispatcher with a bounded session wait.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*GoogleCast*" --no-daemon`.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon`.

### Slice 4: Review, Verification, And Cleanup

Goal: run targeted tests and compile checks, remove dead notes/files, update this document, and prepare commits.

Verification ladder:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*"
.\gradlew.bat :app:compileDebugKotlin
```

### Slice 5: Bugfix — SSDP Isolation And Cast Route Preservation

Goal: fix two runtime bugs — (A) SSDP device profile fetch failures crashing the cast dialog, and (B) Google Cast route selection failing after dialog disposal clears discovery state.

Status: verified and APK built.

Result on 2026-05-26:

- SSDP profile fetch now catches per-device exceptions (SocketTimeoutException, parse failures) so one broken device never crashes the dialog or blocks other results.
- `DeviceListDialog.refreshSsdpDevices` wraps discovery in try/finally for `isSearching`; `associateNotNullBy` internally isolates per-device exceptions so one broken device never crashes the dialog.
- `GoogleCastRouteManager` caches `MediaRouter.RouteInfo` in `routeCache` so `GoogleCastMediaLoader` can select the route even if the dialog disposes before `loadMedia` returns.
- `VideoPlayerOverlay` plugin cast flow now dismisses the dialog AFTER `plugin.cast()` returns instead of before; URL-resolution failure still dismisses immediately.
- Added regression tests: `parseDeviceProfile` blank/invalid XML null returns, `associateNotNullBy` with mixed successes/exceptions, and `shouldDismissCastDialogOnUrlFailure` policy wired into the plugin cast flow.
- Verified with targeted regression tests for `SsdpDevicePresentationPolicyTest` and `VideoPlayerOverlayCastPolicyTest`, plus `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon`, `.\gradlew.bat :app:compileDebugKotlin --no-daemon`, and `.\gradlew.bat :app:assembleDebug --no-daemon`; all returned BUILD SUCCESSFUL.
- Debug APK for device testing: `app/build/outputs/apk/debug/BiliPai-debug-8.4.1-debug.apk`.

### Slice 6: Bugfix — DLNA Parsing, First-Tap RemoteMediaClient Wait, And Cast Playback Control

Goal: fix three runtime bugs — (A) benign DOCTYPE descriptions rejected by SSDP XML parser, (B) Chromecast first-tap failing because `remoteMediaClient` not ready after session connect, and (C) app overlay unable to control Chromecast playback after media loads.

Status: verified and APK built.

Result on 2026-05-26:

**(A) DLNA/SSDP profile parsing:**
- `SsdpCastClient.parseDeviceProfile` no longer rejects all DOCTYPE declarations; removed `disallow-doctype-decl` feature and added `load-external-dtd=false`. External entity expansion and external parameter entities remain disabled.
- Parse failure log changed from `Logger.e` to `Logger.w` since malformed non-DLNA descriptions are expected noise.
- Added tests: DOCTYPE + AVTransport parses successfully; DOCTYPE + no AVTransport returns profile with null endpoint; invalid XML with partial tags still returns null.

**(B) Chromecast first-tap media loading:**
- `GoogleCastMediaLoader.loadMedia` now waits for both `currentCastSession != null` AND `session.remoteMediaClient != null` before proceeding.
- Session timeout increased from 5s to 10s to accommodate device connection time.
- Added pure `shouldContinueWaitingForSession` helper with tests covering: no-session/no-client, session/no-client, both-ready, timeout-exceeded.

**(C) App UI playback/progress controls for active cast sessions:**
- Added `CastPluginPlaybackState` data class (`isActive`, `deviceLabel`, `title`, `isPlaying`, `isBuffering`, `currentPositionMs`, `durationMs`, `bufferedPositionMs`, `canSeek`) to the generic `CastPluginApi` boundary.
- Extended `CastPluginApi` with `playbackState: StateFlow<CastPluginPlaybackState>` (defaults to inactive), `play()`, `pause()`, `seek(positionMs)` with default unsupported-override implementations — source-compatible for all plugins.
- Added `GoogleCastPlaybackController` using `RemoteMediaClient` polling (500ms interval) for playback state and delegating `play`/`pause`/`seek` through CAF.
- `GoogleCastPlugin` wires the playback controller: attaches after successful `cast()` using `CastContext.sessionManager.currentCastSession`, detaches on disable.
- `VideoPlayerOverlay` tracks `activeCastPlugin` after successful plugin cast; when a plugin reports `isActive` in its playback state, the overlay uses plugin progress/play state and routes play/pause/seek through the plugin. When no session is active, existing local player behavior is unchanged.
- After successful plugin `cast()`, the local player is paused so local playback does not continue independently.
- Added pure helper tests: `resolveEffectivePlayerProgress` and `resolveEffectivePlayingState` with local/plugin/inactive/active combinations.

**Verification:**
- `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon` → BUILD SUCCESSFUL
- Targeted regression tests for `SsdpCastClientTest`, `GoogleCastPlaybackControllerTest`, `CastPluginApiTest`, and `VideoPlayerOverlayCastPolicyTest` → BUILD SUCCESSFUL
- `.\gradlew.bat :app:compileDebugKotlin --no-daemon` → BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL
- Debug APK for device testing: `app/build/outputs/apk/debug/BiliPai-debug-8.4.1-debug.apk`.

### Slice 7: Active Cast Quality/Source Reload

Goal: when the user changes video quality or the playable source changes during an active Google Cast session, reload the same cast route with the new media URL while preserving remote position and play/pause intent.

Status: verified.

Result on 2026-05-26:

- Extended `CastPluginMediaRequest` with source-compatible `startPositionMs` and `autoplay` fields.
- `GoogleCastMediaLoader` now maps those fields to CAF `MediaLoadRequestData.currentTime` and `autoplay`.
- `VideoPlayerOverlay` tracks the active plugin route, last cast source signature, and last cast URL for active cast sessions.
- Active Google Cast sessions automatically reload when `aid`, `cid`, `currentQuality`, or `currentVideoUrl` changes; same-URL updates only refresh the signature to avoid no-op reload loops.
- DLNA remains one-shot/manual because its plugin playback state is inactive and does not provide remote position or play/pause state.

Verification:

```powershell
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:testDebugUnitTest --tests "*Cast*" --no-daemon
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:compileDebugKotlin --no-daemon
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:assembleDebug --no-daemon
```

All commands completed with `BUILD SUCCESSFUL`.

### Slice 8: Bugfix — DASH-Only Cast Audio On Legacy Chromecast

Goal: fix videos that show picture but have no sound on Chromecast 2 when the BiliBili TV cast API returns DASH-only data.

Root cause:

- `extractTvCastPlayableUrl` previously fell back to `dash.video.first().getValidUrl()` when no `durl` URL existed.
- DASH video URLs are video-only `.m4s` resources, so loading that URL directly into Google Cast can produce video without audio.
- Chromecast 2 also needs conservative codec selection, so Cast DASH fallback should prefer AVC/H.264 video and AAC audio.

Result on 2026-05-27:

- `extractTvCastPlayableUrl` now returns only complete `durl` primary/backup URLs and no longer leaks video-only DASH URLs.
- DASH-only TV cast payloads are converted into a local MPD manifest containing one proxied video representation and one proxied AAC audio representation.
- `LocalProxyServer` now serves registered `/dash/*.mpd` manifests as `application/dash+xml` and adds CORS headers for manifest/proxy requests.
- DASH video selection prefers AVC/H.264 across qualities before HEVC/HVC1, AV1, or unknown codecs; within a codec family it still prefers exact quality, then closest lower, then closest higher quality.
- Active Cast source signatures now include `currentAudioUrl` so audio-only source changes can trigger a Cast reload.

Verification:

```powershell
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:testDebugUnitTest --tests "com.android.purebilibili.data.repository.VideoCastPolicyTest" --tests "com.android.purebilibili.feature.cast.LocalProxyServerPolicyTest" --tests "com.android.purebilibili.feature.plugin.googlecast.GoogleCastMediaLoaderTest" --tests "com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayCastPolicyTest" --no-daemon
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:testDebugUnitTest --tests "*Cast*" --no-daemon
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:compileDebugKotlin --no-daemon
.\gradlew.bat "-Pkotlin.compiler.execution.strategy=in-process" :app:assembleDebug --no-daemon
git diff --check
```

## Progress Log

- 2026-05-26: Created isolated worktree `feature/google-cast-plugin`.
- 2026-05-26: Confirmed existing casting is DLNA/SSDP based; Google Cast/Chromecast support is not implemented yet.
- 2026-05-26: Completed Slice 0 documentation and focused Cast baseline.
- 2026-05-26: Completed Slice 1 Google Cast plugin shell, CAF wiring, and focused policy tests.
- 2026-05-26: Completed Slice 2 Chromecast route discovery and device-list selection UI aligned with existing DLNA rows.
- 2026-05-26: Corrected the plugin boundary so Google Cast discovery/loading lives behind `CastPluginApi`; completed Slice 3 Chromecast media loading.
- 2026-05-26: Completed Slice 5 bugfix — SSDP per-device exception isolation, Google Cast route cache preservation, dialog dismissal timing fix, and debug APK build.
- 2026-05-26: Completed Slice 6 bugfix — DLNA DOCTYPE parsing, first-tap RemoteMediaClient wait, and cast playback control.
- 2026-05-26: Updated Google Cast plugin author metadata from 'BiliPai项目组' to 'Leko (lekoOwO)' before opening upstream PR.
- 2026-05-26: Completed Slice 7 active Google Cast quality/source reload while keeping DLNA one-shot/manual.
- 2026-05-27: Completed Slice 8 Chromecast 2 DASH-only audio fix by replacing video-only DASH URL fallback with a local DASH manifest and AVC/AAC selection policy.
