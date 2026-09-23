# Android 17 verification checklist

Use a `dev` or `release` artifact only. Do not package or install `debug`/`smooth` variants.

## Compatibility toggles

Before and after raising `targetSdk` to 37, enable the new MessageQueue implementation for the dev package and run navigation, playback, danmaku, settings, and WorkManager smoke tests:

```bash
adb shell am compat enable USE_NEW_MESSAGEQUEUE com.android.purebilibili.dev
```

Check native libraries and APK zip alignment with:

```bash
scripts/android17_native_compat_check.sh app/build/outputs/apk/dev/app-dev.apk
```

On an Android 17 16 KB system image, set the page-size compatibility mode to fatal and smoke-test normal and Dolby/FFmpeg playback.

## Device scenarios

- Local network: grant, deny, and revoke Local Network access. DLNA must not open sockets without access; Google Cast must stay usable.
- Background audio: test Home, screen-off, Bluetooth/media keys, brief buffering, permanent stop, playback end, and PiP.
- Large screens: resize at 599/600dp, rotate tablets/foldables, and use split/freeform windows. Fullscreen must remain an in-app layout on large screens.
- Handoff: test VOD, bangumi, and video-audio mode with the app installed and with web fallback. Resume position tolerance is two seconds.
- Profiling: force system-triggered profiling for the dev package, verify at most three private artifacts and 128 MB total, then disable crash diagnostics and verify deletion.
- Memory limiter: run the Android 17 memory-limiter manual test and verify the next launch records `MemoryLimiter:AnonSwap` without content identifiers.
- Audio hardening: enable the Android 17 audio hardening throw mode and verify no playback failure or hardening log appears.

Regression devices: API 26, 31, 33, and 35. Also verify long downloads and scheduled WebDAV backup tolerate JobScheduler quota delays.
