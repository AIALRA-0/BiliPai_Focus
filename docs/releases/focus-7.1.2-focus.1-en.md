# BliPai Focus v7.1.2-focus.1 Release Notes

BliPai Focus `v7.1.2-focus.1` is based on upstream `BiliPai v7.1.2`. This release first pulls in the upstream playback-regression and settings-localization fixes, then expands Focus home-title switches to the full eight-tab surface and adds a proper empty state when every home title is disabled.

## ✨ Highlights

- Syncs upstream `v7.1.2` playback-regression fixes, settings localization cleanup, sub-reply preview regression coverage, and related search / history / navigation policy updates.
- Focus home title switches now cover `Recommend / Follow / Popular / Live / Anime / Game / Knowledge / Tech`.
- When every home title is disabled, Home now shows a dedicated empty state with a shortcut to `Settings -> General -> Focus`, instead of forcing `Follow` back into view.
- Final home-top ordering still follows the original `HomeTopTabSettings` first and only then applies the Focus visibility filter, keeping the implementation as close to upstream as possible.
- This release does not bundle extra performance refactors; it only records the remaining divergence audit items for later decisions.

## 🧩 Focus Defaults

- Home `Recommend / Popular / Live / Game / Partition` entries are hidden by default
- Home `Follow / Anime / Knowledge / Tech` stay visible by default, but every title can now be disabled from Focus settings
- Search hot list is disabled by default while keeping suggestions, discovery, results, and history
- Watch history supports one-tap clear-all
- Related videos below video detail are hidden by default and can be restored in Focus settings
- Dynamic and Home Follow share the same local follow-group filtering rules, with a master switch

## 📦 Version Info

- Focus Version: `7.1.2-focus.1`
- Upstream Base: `7.1.2`
- VersionCode: `138`
- Tag: `v7.1.2-focus.1`

## 📱 Artifact Names

- Debug APK: `BliPai-Focus-debug-7.1.2-focus.1-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.2-focus.1.apk`

## 🧪 Testing

- This release passed `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`
- Focus-related manual validation is currently performed on `realme Neo 7` and `Lenovo Y700 2023`
- If you hit compatibility or behavior issues on other devices, please open an issue
- Focus currently tries to stay on the same upstream major version and, for now, only rolls Focus sub-versions on top of upstream major releases
