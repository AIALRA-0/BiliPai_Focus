package com.bytedance.danmaku.render.engine

/** Choreographer owns the cadence: never turn 90/120Hz into uneven 60fps updates. */
internal class RenderFrameCadence {
    private var lastFrameNs: Long? = null

    fun shouldRender(frameTimeNs: Long): Boolean = lastFrameNs?.let { frameTimeNs > it } ?: true

    fun onFrame(frameTimeNs: Long) { lastFrameNs = frameTimeNs }

    fun reset() { lastFrameNs = null }
}
