package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ArchiveMajor

internal fun resolveDynamicArchiveBadgeLabel(archive: ArchiveMajor): String? {
    archive.badge?.text?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    return if (archive.isChargingArc ||
        archive.elecArcType == 1 ||
        archive.isUgcpay ||
        archive.ugcPay > 0 ||
        archive.ugcPayPreview > 0
    ) {
        "充电专属"
    } else {
        null
    }
}
