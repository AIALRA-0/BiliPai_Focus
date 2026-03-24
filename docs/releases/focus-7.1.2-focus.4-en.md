# BliPai Focus v7.1.2-focus.4

Release date: 2026-03-24

## Highlights
- This is a hotfix release for the in-app update channel. It fixes incorrect APK selection when a GitHub Release contains both `debug` and `release` assets.

## In-app update fix
- The in-app updater now explicitly prefers the `release` APK instead of accidentally choosing the larger `debug` build.
- `debug / dev` assets are now deprioritized and only used as a fallback when no normal `release` APK is available.
- Public GitHub Releases now ship only the production `release` APK and no longer attach the `debug` build.
- Added regression coverage so future releases keep selecting the production package first.
