package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionItem

internal object InboxUserInfoResolver {

    fun resolveDisplayName(cached: UserBasicInfo?, session: SessionItem): String {
        val cachedName = cached?.name.cleanValue()
        if (cachedName.isNotEmpty()) return cachedName

        val sessionName = session.account_info?.name.cleanValue()
        if (sessionName.isNotEmpty()) return sessionName

        return "用户${session.talker_id}"
    }

    fun resolveDisplayAvatar(cached: UserBasicInfo?, session: SessionItem): String {
        val cachedAvatar = normalizeAvatarUrl(cached?.face)
        if (cachedAvatar.isNotEmpty()) return cachedAvatar

        return normalizeAvatarUrl(session.account_info?.avatarUrl)
    }

    fun shouldFetchUserInfo(mid: Long, cache: Map<Long, UserBasicInfo>): Boolean {
        val cached = cache[mid] ?: return true
        return !hasCompleteUserInfo(cached)
    }

    fun selectMissingUserInfoMids(
        mids: List<Long>,
        cache: Map<Long, UserBasicInfo>
    ): List<Long> {
        return mids
            .distinct()
            .filter { shouldFetchUserInfo(it, cache) }
    }

    fun shouldFetchSessionUserInfo(session: SessionItem, cache: Map<Long, UserBasicInfo>): Boolean {
        if (session.session_type != 1 || session.talker_id <= 0L) return false
        if (hasCompleteSessionAccountInfo(session)) return false
        return shouldFetchUserInfo(session.talker_id, cache)
    }

    fun hasCompleteUserInfo(info: UserBasicInfo?): Boolean {
        if (info == null) return false
        return info.name.cleanValue().isNotEmpty() && normalizeAvatarUrl(info.face).isNotEmpty()
    }

    fun mergeFetchedUserInfo(existing: UserBasicInfo?, fetched: UserBasicInfo?): UserBasicInfo? {
        if (fetched == null) return existing

        val mergedMid = if (fetched.mid > 0) fetched.mid else existing?.mid ?: 0L
        if (mergedMid <= 0L) return existing

        val mergedName = fetched.name.cleanValue().ifEmpty { existing?.name.cleanValue() }
        val mergedFace = normalizeAvatarUrl(fetched.face).ifEmpty { normalizeAvatarUrl(existing?.face) }

        if (mergedName.isEmpty() && mergedFace.isEmpty()) {
            return existing
        }

        return UserBasicInfo(
            mid = mergedMid,
            name = mergedName,
            face = mergedFace
        )
    }

    private fun hasCompleteSessionAccountInfo(session: SessionItem): Boolean {
        val accountInfo = session.account_info ?: return false
        return accountInfo.name.cleanValue().isNotEmpty() &&
            normalizeAvatarUrl(accountInfo.avatarUrl).isNotEmpty()
    }

    private fun String?.cleanValue(): String = this?.trim().orEmpty()

    private fun normalizeAvatarUrl(raw: String?): String {
        val value = raw.cleanValue()
        if (value.isEmpty()) return ""
        return when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("http://") -> value.replaceFirst("http://", "https://")
            else -> value
        }
    }
}
