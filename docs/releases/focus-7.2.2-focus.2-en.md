# BliPai Focus v7.2.2-focus.2

Release date: 2026-03-28

## Highlights
- This release stays on the upstream `7.2.2` baseline and focuses on fixing manual-only following sync plus the missing linkage between follow-list changes and Home FOLLOW.

## Main changes
- Following data is now unified into a shared snapshot source used by Dynamic, Following List, and Home.
- Following sync is automatic by default: Dynamic restores cache first, then silently refreshes in the background when the snapshot is stale; the settings button is now only a manual fallback.
- Home FOLLOW no longer keeps its own private one-hour `following_mids_*` cache and instead subscribes directly to the shared following snapshot.
- When new creators are followed, Home FOLLOW reloads and includes them in the candidate pool; when creators are unfollowed, their content is removed immediately and the current batch is topped up.
- Pure profile-only changes such as avatar or nickname updates no longer trigger unnecessary Home FOLLOW reshuffles.

## Verification
- Passed `:app:testDebugUnitTest --tests "*DynamicFollowingsRefreshPolicyTest" --tests "*HomeFollowingSyncPolicyTest" --tests "*FollowingBatchSelectionPolicyTest" --tests "*HomeFollowFocusPolicyTest"`.
- Passed `:app:assembleRelease`.
