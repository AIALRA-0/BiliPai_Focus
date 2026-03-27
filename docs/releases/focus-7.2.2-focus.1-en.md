# BliPai Focus v7.2.2-focus.1

Release date: 2026-03-27

## Highlights
- This release rebases the Focus line onto upstream `BiliPai v7.2.2` while keeping the Focus coexist package, Focus-only update channel, and Home FOLLOW custom flow.

## Main changes
- Merges upstream `7.2.1/7.2.2` message center work, including feeds for `@ me`, replies, likes, and system notices, plus message preview parsing and the related navigation paths.
- Merges upstream updates around chat, profile, space, bottom-bar behavior, and the supporting message models, reducing divergence on social flows.
- Merges upstream player, video detail, and comment improvements, including the seek preview bubble, mini player, portrait pager, fullscreen overlay, comment appearance, and playback policy fixes.
- Keeps `applicationId = com.android.purebilibili.focus`, and in-app update still tracks only release assets from the Focus repository.
- Keeps Home FOLLOW filtering, the 16-card first batch, random/time/cluster sorting, the pure-random mode, and the stabilized refresh behavior.

## Verification
- Passed `:app:testDebugUnitTest`.
- Passed `:app:assembleRelease`.
