# BliPai Focus v7.1.2-focus.5

Release date: 2026-03-24

## Highlights
- This stability release fixes home top-tab return consistency, speeds up the filtered Home Follow feed, improves refresh-time randomness, and restores both Telegram links in the first-use disclaimer.

## Home top-tab fix
- Home now distinguishes between hydrated and non-hydrated `FocusSettings`, so the current tab is no longer treated as hidden before settings finish loading.
- With only tabs like `Follow / Popular / Live` enabled, returning from a video opened in `Popular` or `Live` now lands back on the original top tab instead of incorrectly falling back to `Follow`.

## Faster Home Follow
- With Focus follow filtering enabled, Home Follow now fetches visible creators' user feeds in parallel, with up to `8` concurrent requests per wave.
- The first `8` newly visible videos are published as soon as they are ready, without clearing the videos already on screen; any remaining items continue hydrating in the background.
- The followed-creator list itself is also synchronized with parallel page fetches after the first page, reducing the wait cost for larger follow counts.

## Randomness and first-use disclaimer
- Home Follow ordering now uses cross-creator interleaving plus a new refresh seed each time, so the first screens vary more noticeably while remaining deduplicated.
- The startup disclaimer now exposes `Official GitHub / Focus GitHub / Telegram Group / Telegram Channel`; the onboarding `Telegram Group` shortcut remains available too.
