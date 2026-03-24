# BliPai Focus v7.1.4-focus.1

Release date: 2026-03-24

## Highlights
- This release syncs upstream `BiliPai v7.1.4` and further tightens Home Follow around three areas: refresh timing, freshness priority, and load-more gating.

## Upstream `7.1.4` sync
- Pulls in the upstream `v7.1.4` fixes for portrait-video cover ratio handoff, cover-to-first-frame transitions, and portrait seek/progress stability.
- Focus still keeps its separate package id, co-install support with the official app, and a Focus-only in-app update channel that only accepts the Focus `release` APK.

## Manual refresh now swaps after completion
- Manual pull-to-refresh on Home Follow now keeps the currently visible cards on screen while the refresh is in progress.
- The refreshed order is committed only after the refresh finishes, instead of changing the list while the pull gesture is still returning to rest.

## Fresh content is biased toward the front
- Newly fetched follow videos are now prioritized ahead of older cached items during refresh, while still preserving the cross-creator interleaving and reshuffle behavior.
- That makes it much more likely for just-updated content to appear near the front after a refresh.

## Strict `16`-card windows and safer pagination
- Initial entry, refresh completion, and every later reveal continue to use strict `16`-card presentation windows.
- FOLLOW now loads the next batch only when auto-pagination has been re-enabled, a real downward scroll has been observed, and the list has actually reached the tail, reducing both runaway first-load expansion and intermittent tail stalls.
