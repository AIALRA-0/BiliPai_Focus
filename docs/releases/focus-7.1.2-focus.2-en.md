# BliPai Focus v7.1.2-focus.2 Release Notes

BliPai Focus `v7.1.2-focus.2` continues to build on upstream `BiliPai v7.1.2`. This update pulls the Focus home switches back to a smaller, more upstream-aligned surface and keeps refining filtered Home Follow refresh latency and bottom-end continuation behavior.

## ✨ Highlights

- The Focus settings page removes the extra `Show Anime / Show Knowledge / Show Tech` switches. Those tabs now go back to the original top-tab management instead of being controlled separately by Focus.
- Filtered Home Follow refresh is faster: as soon as the current round yields the first visible cards, the foreground fetch stops instead of waiting to match the size of the first official raw chunk.
- Home Follow refresh no longer blocks on user-info fetching, and the extra `100ms` artificial delay inside the filtered continuation loop has been removed.
- Reaching the bottom of Home Follow now keeps pulling forward until at least one new visible card appears, or every visible uploader is truly exhausted, instead of easily stalling until the user switches tabs.
- If every Focus-controlled home title is disabled, Home now safely falls back to a single `Recommend` tab instead of crashing.

## 🧩 Focus Defaults

- Home `Recommend / Popular / Live / Game / Partition` entries are hidden by default
- Home `Follow` stays visible by default and can still be disabled from Focus
- Home `Anime / Knowledge / Tech` now follow the original top-tab management and no longer have separate Focus switches
- Search hot list is disabled by default while keeping suggestions, discovery, results, and history
- Watch history supports one-tap clear-all
- Related videos below video detail are hidden by default and can be restored in Focus settings
- Dynamic and Home Follow share the same local follow-group filtering rules, with a master switch

## 📦 Version Info

- Focus Version: `7.1.2-focus.2`
- Upstream Base: `7.1.2`
- VersionCode: `139`
- Tag: `v7.1.2-focus.2`

## 📱 Artifact Names

- Debug APK: `BliPai-Focus-debug-7.1.2-focus.2-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.2-focus.2.apk`

## 🧪 Testing

- This release passed `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`
- Focus-related manual validation is currently performed on `realme Neo 7` and `Lenovo Y700 2023`
- If you hit compatibility or behavior issues on other devices, please open an issue
- Focus currently tries to stay on the same upstream major version and, for now, only rolls Focus sub-versions on top of upstream major releases
