# BliPai Focus v7.1.2-focus.7

Release date: 2026-03-24

## Highlights
- This release keeps refining Home Follow: the first batch is now consistently capped, pull-to-refresh finishes faster, and load-more is more reliable after a refresh.

## Fixed `16`-card first batch
- Home Follow now starts with a fixed `16`-card first batch instead of occasionally dumping several dozen cards before switching to `16 / 16` paging.
- Pull-to-refresh also resets back to the same `16`-card presentation window so the pacing stays consistent.

## Faster refresh path
- Pull-to-refresh now reshuffles the currently visible cached pool first and shows the first `16` cards immediately before continuing with fresh network results.
- Refresh completion now targets only the cards needed for the current first batch instead of waiting for `16` completely new cards to arrive.
- The first fetch wave also rises from `8` to `16` concurrent creators, reducing the number of waves needed to fill the first screen.

## More reliable pagination
- FOLLOW load-more no longer depends solely on the last UI `hasMore` flag; if cached cards remain hidden or the underlying per-creator cursor can continue, the next batch can still load.
- A FOLLOW-specific paging fallback was added to reduce the intermittent “reached bottom but nothing else loads” case after refresh.
