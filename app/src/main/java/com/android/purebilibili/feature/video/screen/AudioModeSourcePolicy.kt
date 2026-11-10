package com.android.purebilibili.feature.video.screen

/** 新入口指定内容时，旧播放器的成功状态不能充当该入口的首屏。 */
internal fun matchesAudioModeInitialSource(
    requestedBvid: String,
    requestedCid: Long,
    loadedBvid: String,
    loadedCid: Long,
): Boolean = requestedBvid.isBlank() ||
    (requestedBvid == loadedBvid && (requestedCid <= 0L || requestedCid == loadedCid))

/**
 * 首屏门控只在“入口内容尚未落地”时生效。
 *
 * 一旦本页发起了加载并经历过一次 Loading，之后到达的成功态就属于本次播放会话：
 * 可能是入口分 P 被仓库归一化成另一个 cid，也可能是用户在队列里切歌。此时若继续
 * 拿入口 bvid/cid 当门槛，切歌后会永远停在“正在加载音频”，看不到新的曲目。
 */
internal fun shouldResolveAudioModeInitialSource(
    requestedBvid: String,
    requestedCid: Long,
    loadedBvid: String,
    loadedCid: Long,
    entryLoadRequested: Boolean,
    sawLoadingSinceEntry: Boolean,
): Boolean = matchesAudioModeInitialSource(
    requestedBvid = requestedBvid,
    requestedCid = requestedCid,
    loadedBvid = loadedBvid,
    loadedCid = loadedCid,
) || (entryLoadRequested && sawLoadingSinceEntry)

/**
 * 入口加载要为已在进行的切歌让路。
 *
 * 本页建立时播放器可能已经在加载队列里的下一首（点击「下一首」后才建立页面，或页面
 * 被重建）。此时再发入口请求会取消并覆盖那次加载，把播放拉回入口曲目，表现为切歌
 * 失败并停在“正在加载音频”。
 */
internal fun shouldSkipAudioModeEntryLoadForInFlightSwitch(
    isLoading: Boolean,
    inFlightBvid: String,
    requestedBvid: String,
): Boolean = isLoading &&
    inFlightBvid.isNotBlank() &&
    inFlightBvid != requestedBvid
