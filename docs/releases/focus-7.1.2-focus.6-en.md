# BliPai Focus v7.1.2-focus.6

Release date: 2026-03-24

## Highlights
- This release refines Home Follow refresh and pagination behavior, focusing on full-page reshuffling, grouped reveals, and lower rendering pressure on weaker devices.

## Home Follow refresh reshuffle
- Pull-to-refresh now reshuffles the entire currently visible Home Follow page instead of merely prepending a few new cards.
- That makes each refresh feel more noticeably different while keeping deduplication and visibility filtering intact.

## Grouped reveals and cache-first paging
- Reaching the bottom now reveals the next group of `16` cards instead of dumping every already-fetched item onto the page at once.
- If there are already visible-but-hidden cards in cache, the next `16` are revealed immediately without waiting for more network requests.

## Fast path preserved
- The Focus-filtered Home Follow feed still uses concurrent per-creator user-feed fetching and does not fall back to the old long serial completion loop.
- Fetching and presentation are now handled separately, preserving fast first results while reducing the amount of content rendered in a single step.
