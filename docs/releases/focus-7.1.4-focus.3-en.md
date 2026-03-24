# BliPai Focus v7.1.4-focus.3

Release date: 2026-03-24

## Highlights
- This is an emergency patch release for the severe `focus.2` issue where tapping any video could crash the release build immediately.

## Root cause
- The adb device crash log confirmed a release-only `VerifyError` when entering `VideoDetailScreen`.
- The failure was not caused by the home/dynamic click handlers themselves; it came from R8 bytecode optimization producing unverifiable dex for the large Compose video-detail screen.

## Fix
- Keep the normal release shrinking and obfuscation pipeline.
- Disable R8 bytecode optimization so `VideoDetailScreen` and similar large Compose screens are no longer optimized into runtime-invalid classes.

## Verification
- Passed `:app:assembleRelease`.
- Installed the fixed release APK on the attached device over adb.
- Opened a standard Bilibili video link over adb and confirmed the app no longer crashes while entering `VideoDetailScreen`.
