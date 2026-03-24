# BliPai Focus v7.1.2-focus.3

Release date: 2026-03-24

## Highlights
- Restored the `Telegram group` shortcut in first-run onboarding and kept the `Focus GitHub` shortcut alongside it.
- Focus now ships with a standalone `applicationId`: `com.android.purebilibili.focus`, so it can coexist with the upstream official build without overwriting it.
- The in-app update channel remains Focus-only and no longer implies that official upstream releases are valid update targets for this build.

## Home Follow fixes
- Pull-to-refresh on `Follow` no longer drops already cached visible items before new results arrive; refreshed content is merged on top of the existing list.
- With Focus follow filtering enabled, the app now keeps filling until it can surface at least `8` newly visible videos per batch instead of stopping after only `2-3`.
- `HOME_FOLLOW` requests now aim for larger visible chunks, which improves bottom pagination stability and reduces cases where loading stalls at the end.
- Filtered follow results now get a stable randomization seed so they do not appear in the exact same order every time.

## Return consistency
- `Refresh / load more / video open / long-press preview / live room open` from top tabs now all sync the active interaction tab first.
- Returning from video detail now restores the tab you came from instead of incorrectly snapping back to `Follow`.
