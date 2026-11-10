package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.ReplyPageControl

/** 评论主体的服务端提示属于列表内容，不随排序栏材质或浮动状态重复绘制。 */
internal fun LazyListScope.commentSectionNotice(control: ReplyPageControl?) {
    val notice = control?.sectionNotice().orEmpty()
    if (notice.isBlank()) return
    item(key = "comment_section_notice", contentType = "comment_section_notice") {
        AppText(
            text = notice,
            style = MaterialTheme.typography.bodySmall,
            color = AppContentColors.secondaryText(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
