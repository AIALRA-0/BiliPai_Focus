package com.android.purebilibili.danmaku.engine

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.bytedance.danmaku.render.engine.DanmakuView
import com.bytedance.danmaku.render.engine.utils.CMD_SET_TOUCHABLE

/** Android View host that keeps the vendored renderer out of app-layer APIs. */
class DanmakuRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val rendererView = DanmakuView(context)

    val engine: DanmakuEngine = ByteDanceDanmakuEngine(rendererView)

    init {
        clipChildren = false
        clipToPadding = false
        addView(
            rendererView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun setRendererTouchable(touchable: Boolean) {
        rendererView.isClickable = touchable
        rendererView.isLongClickable = touchable
        rendererView.controller.executeCommand(CMD_SET_TOUCHABLE, param = touchable)
    }

    fun releaseRenderer() {
        engine.close()
        removeAllViews()
    }
}
