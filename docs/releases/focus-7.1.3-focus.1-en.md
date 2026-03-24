# BliPai Focus v7.1.3-focus.1

Release date: 2026-03-24

## Highlights
- This release syncs upstream `BiliPai v7.1.3` and tightens Home Follow around three areas: a strict first batch, a unified post-refresh scroll-to-top, and a guard against premature auto-pagination right after refresh.

## Upstream `7.1.3` sync
- Pulls in the upstream `7.1.3` updates for Space search, Dynamic top-tab persistence, player cover/manual-start behavior, background audio focus, and the Home avatar action split.
- Focus still keeps its separate package id, co-install support with the official app, and a Focus-only in-app update channel that only accepts the Focus `release` APK.

## Strict `16`-card Home Follow window
- Home Follow now treats the presentation window as the source of truth: initial entry and pull-to-refresh completion both expose only the first `16` visible cards.
- Larger cached or filtered pools can still exist in memory, but they no longer leak several dozen cards onto the first screen.

## Unified refresh completion reset
- After a filtered FOLLOW refresh completes, the UI first commits the new first `16` cards and then resets the list back to the top.
- This removes the old-anchor behavior that made refreshes feel jumpy and visually inconsistent.

## Post-refresh load-more guard
- FOLLOW now temporarily blocks automatic `loadMore` while the refresh-complete top reset is still pending.
- The next `16`-card batch becomes eligible only after that reset has been handled, preventing the common “refresh finished and it instantly paged again” failure mode.
