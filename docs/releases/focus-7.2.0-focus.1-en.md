# BliPai Focus v7.2.0-focus.1

Release date: 2026-03-25

## Highlights
- This release rebases the Focus line onto upstream `BiliPai v7.2.0` while keeping the Focus coexist package, Focus-only update channel, and Home FOLLOW custom flow.

## Main changes
- Merges upstream article models, article detail UI, article image preview/shared transition, and article navigation from history/search.
- Merges upstream updates around home cards, dynamic cards, image preview, WebView, search, history, and video detail behavior.
- Keeps `applicationId = com.android.purebilibili.focus`, and in-app update still tracks only release assets from the Focus repository.
- Keeps Home FOLLOW filtering, the 16-card first batch, random/time/cluster sorting, the pure-random mode, and the stabilized refresh behavior.

## Verification
- Passed `:app:testDebugUnitTest`.
- Passed `:app:assembleRelease`.
