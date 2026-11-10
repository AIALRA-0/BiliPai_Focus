/*
 * Copyright (C) 2022 ByteDance Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytedance.danmaku.render.engine.render.cache

import com.bytedance.danmaku.render.engine.control.DanmakuConfig
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.DrawItem
import java.util.*

/**
 * Created by dss886 on 2019/9/22.
 * Buffer for render layers.
 */
class LayerBuffer(private val mConfig: DanmakuConfig,
                  private val mCachePool: IDrawCachePool,
                  private var mBufferSize: Int,
                  private var mBufferMaxTime: Long) {

    private val mBufferItems = LinkedList<DrawItem<DanmakuData>>()

    fun addItems(list: List<DrawItem<DanmakuData>>) {
        mBufferItems.addAll(list)
    }

    /**
     * Read and deal with items from the buffer.
     * action: return true if you have processed the item successfully and want to remove it from buffer.
     */
    fun forEach(action: (DrawItem<DanmakuData>) -> Boolean) {
        val iterator = mBufferItems.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (action(next)) {
                iterator.remove()
            }
        }
    }

    /**
     * Trim Buffer down to the mBufferSize.
     * Using [DanmakuConfig.CommonConfig.bufferDiscardRule] to discard items.
     */
    fun trimBuffer(playTime: Long) {
        if (mBufferItems.isEmpty() || mBufferItems.size <= mBufferSize) {
            return
        }
        val retained = mBufferItems.toList()
        val decisions = admissions(playTime, retained.size) { retained[it].data }
        val iterator = mBufferItems.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val item = iterator.next()
            val reason = decisions[index++]
            if (reason != 0) {
                mConfig.common.discardListener?.invoke(item.data, reason)
                iterator.remove()
                mCachePool.release(item)
            }
        }
    }

    /** Select from old + new arrivals before acquiring Paints or measuring incoming text. */
    fun addData(
        playTime: Long,
        list: List<DanmakuData>,
        createItem: (DanmakuData) -> DrawItem<DanmakuData>,
    ) {
        val previous = mBufferItems.toList()
        val decisions = admissions(playTime, previous.size + list.size) { index ->
            if (index < previous.size) previous[index].data else list[index - previous.size]
        }
        val iterator = mBufferItems.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val item = iterator.next()
            val reason = decisions[index++]
            if (reason != 0) {
                mConfig.common.discardListener?.invoke(item.data, reason)
                iterator.remove()
                mCachePool.release(item)
            }
        }
        list.forEachIndexed { incomingIndex, data ->
            val reason = decisions[previous.size + incomingIndex]
            if (reason == 0) mBufferItems.add(createItem(data))
            else mConfig.common.discardListener?.invoke(data, reason)
        }
    }

    private inline fun admissions(
        playTime: Long,
        count: Int,
        crossinline dataAt: (Int) -> DanmakuData?,
    ): IntArray = selectBufferAdmissions(
        count = count,
        capacity = mBufferSize,
        isExpired = { index ->
            val data = dataAt(index)
            mConfig.common.bufferExpireRule?.invoke(data, playTime)
                ?: (playTime - (data?.showAtTime ?: 0L) > mBufferMaxTime)
        },
        scoreAt = { mConfig.common.bufferDiscardRule(dataAt(it)) },
    )

    /**
     * When need to measure (config changed, or measure items manually),
     * items in the buffer should be measured too.
     */
    fun measureItems() {
        mBufferItems.forEach {
            it.measure(mConfig)
        }
    }

    fun clear() {
        mBufferItems.forEach(mCachePool::release)
        mBufferItems.clear()
    }

    /**
     * Changing the buffer size will clear items in the buffer.
     */
    fun onBufferChanged(newSize: Int, maxTime: Long) {
        mBufferItems.forEach {
            mCachePool.release(it)
        }
        mBufferItems.clear()
        mBufferSize = newSize
        mBufferMaxTime = maxTime
    }

}
