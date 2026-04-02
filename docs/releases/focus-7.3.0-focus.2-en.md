# BliPai Focus v7.3.0-focus.2

Release date: 2026-04-02

## Highlights
- This release stays on the upstream `7.3.0` baseline and fixes two high-priority regressions: some creator space pages incorrectly showing “No videos yet”, and the squashed layout that could remain after returning from portrait-video external navigation.

## Main changes
- Search-result creator entries now use flexible numeric parsing for `mid / fans / videos / level / is_senior_member`, so space navigation keeps working even when the API returns those numbers as strings.
- Space video paging and card fields now use the same flexible parsing, preventing some creator spaces from collapsing into a false empty list because of inconsistent numeric payloads.
- The first creator-space video load now retries once with a refreshed `WBI key` when it sees the suspicious combination of “default video tab + valid user info + unexpectedly empty first page”, instead of immediately treating that response as a true empty state.
- Real zero-video creators still show the real empty state; transient failures now surface as a retryable “failed to load videos” state.
- Before leaving portrait video for a creator page, search, or another screen, the inline portrait collapse offset and viewport transform are reset; on return, the layout is restored before playback re-sync runs, preventing the compressed/squashed detail page.
- Focus keeps its coexist package name, Focus-only update source, Home FOLLOW grouping/sorting/auto-sync flow, and first-use entry points.

## Verification
- Passed `:app:testDebugUnitTest`.
- Passed `:app:testDebugUnitTest --tests "*SpaceLoadPolicyTest" --tests "*SpaceSearchSerializationPolicyTest" --tests "*VideoDetailPlayerCollapsePolicyTest" --tests "*PortraitMainPlayerSyncPolicyTest" --tests "*PortraitVideoPagerPolicyTest"`.
- Passed `:app:assembleRelease`.
