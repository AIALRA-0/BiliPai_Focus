# BliPai Focus v7.9.6-focus.1

## Version
- Based on upstream `BiliPai v7.9.6`
- Focus version: `7.9.6-focus.1 / 164`

## Upstream sync
- Fully syncs the official `7.5.2 -> 7.9.6` releases, including the mainline updates across player, live, danmaku, comments, search, space, downloads, settings, and home navigation.
- Keeps the Focus-specific coexist package name, Focus-only update source, FOLLOW grouping/sorting/auto-sync, and the first-run Focus channel entry points.

## Focus fixes
- Fixes the default multi-follow dynamic timeline stopping too early after filtering; the default page now keeps hydrating until it reaches a healthier scrollable threshold before handing off to bottom load-more.
- Fixes the post-`7.5.x` in-app update checker failures by using the injected Focus repository coordinates first and falling back to the official Focus release repository when needed.

## Release notes
- Release APK only
- Continues to coexist with the official app
