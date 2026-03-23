# BliPai Focus v7.1.1-focus.1 Release Notes

BliPai Focus `v7.1.1-focus.1` is based on upstream `BiliPai v7.1.1`. This release focuses on merging upstream language and immersive-layout fixes while rebuilding Home Follow filtering on top of the official `HOME_FOLLOW` pagination flow.

## ✨ Highlights

- Merges upstream `v7.1.1` app-language switching, localized Home UI, dark-theme style split, and immersive inset fixes.
- Home Follow now uses a two-stage strategy: official `HOME_FOLLOW` pagination first, then continued fetches only until the Focus-filtered visible delta catches up with the current official chunk.
- This avoids the old Focus-filtered behavior where Home Follow could falsely report “no available followed creators” or surface only `1-2` unstable results after refresh.
- When Focus follow filtering is disabled, Home Follow stays on the upstream official refresh path instead of using extra Focus-specific continuation.
- Top insets are aligned back to the upstream `7.1.1` immersive baseline, reducing the small gap some systems showed above the header.
- README / README_EN / CHANGELOG / Focus Changelog are all updated to the `7.1.1-focus.1` release line.

## 🧩 Focus Defaults

- Home `Recommend / Popular / Live / Game / Partition` entries are hidden by default
- Search hot list is disabled by default while keeping suggestions, discovery, results, and history
- Watch history supports one-tap clear-all
- Related videos below video detail are hidden by default and can be restored in Focus settings
- Dynamic and Home Follow share the same local follow-group filtering rules, with a master switch

## 📦 Version Info

- Focus Version: `7.1.1-focus.1`
- Upstream Base: `7.1.1`
- VersionCode: `135`
- Tag: `v7.1.1-focus.1`

## 📱 Artifact Names

- Debug APK: `BliPai-Focus-debug-7.1.1-focus.1-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.1-focus.1.apk`

## 🧪 Testing

- Verified currently on `realme Neo 7` and `Lenovo Y700 2023`
- If you hit device-specific compatibility or behavior issues elsewhere, please open an issue
- Focus tries to stay on the same upstream major version and, for now, will continue shipping Focus sub-versions on top of upstream major releases
