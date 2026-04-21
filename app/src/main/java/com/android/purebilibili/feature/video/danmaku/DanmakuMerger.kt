package com.android.purebilibili.feature.video.danmaku

import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import android.util.Log

/**
 * 弹幕合并工具
 *
 * 用于合并短时间内出现的重复弹幕，减少屏幕遮挡
 */
object DanmakuMerger {
    private const val TAG = "DanmakuMerger"

    /**
     * 合并重复弹幕
     *
     * @param list 原始弹幕列表 (必须已按时间排序)
     * @param intervalMs 合并的时间窗口 (毫秒)，默认 500ms
     * @return 合并后的弹幕列表
     */
    /**
     * 合并重复弹幕
     *
     * @param list 原始弹幕列表 (必须已按时间排序)
     * @param intervalMs 合并的时间窗口 (毫秒)，默认 500ms
     * @return Pair(标准弹幕列表, 高级合并弹幕列表)
     *         标准弹幕继续在原层渲染；不再将重复弹幕转换为黄色高级弹幕。
     */
    fun merge(list: List<DanmakuData>, intervalMs: Long = 500): Pair<List<DanmakuData>, List<AdvancedDanmakuData>> {
        if (list.isEmpty()) return Pair(list, emptyList())

        val standardList = mutableListOf<DanmakuData>()
        
        // 1. 分离 TextData 和其他数据
        val textDanmakus = mutableListOf<TextData>()
        
        list.forEach { 
            if (it is TextData) textDanmakus.add(it)
            else standardList.add(it) // 非文本弹幕直接保留在标准列表
        }
        
        // 2. 对 TextData 按内容分组
        val groupedByContent = textDanmakus.groupBy { it.text }
        
        var mergedCount = 0
        
        groupedByContent.forEach { (text, items) ->
            if (items.size == 1) {
                // 没有重复的，保持为标准弹幕
                standardList.add(items[0])
            } else {
                // 对同一内容的弹幕进行时间聚类
                var currentBatch = mutableListOf<TextData>()

                for (item in items) {
                    if (currentBatch.isEmpty()) {
                        currentBatch.add(item)
                    } else {
                        val lastItem = currentBatch.last()
                        if (item.showAtTime - lastItem.showAtTime <= intervalMs) {
                            currentBatch.add(item)
                        } else {
                            // 结算上一批
                            processBatch(currentBatch, standardList)
                            if (currentBatch.size > 1) mergedCount += (currentBatch.size - 1)
                            
                            // 开启新的一批
                            currentBatch = mutableListOf()
                            currentBatch.add(item)
                        }
                    }
                }
                
                // 结算最后一批
                if (currentBatch.isNotEmpty()) {
                    processBatch(currentBatch, standardList)
                    if (currentBatch.size > 1) mergedCount += (currentBatch.size - 1)
                }
            }
        }
        
        // 3. 重新按时间排序
        standardList.sortBy { it.showAtTime }

        Log.d(TAG, "Merged result: ${standardList.size} standard. Reduced $mergedCount items.")

        return Pair(standardList, emptyList())
    }
    
    /**
     * 处理一批重复弹幕
     */
    private fun processBatch(
        batch: List<TextData>, 
        standardOut: MutableList<DanmakuData>
    ) {
        if (batch.isEmpty()) return
        
        if (batch.size == 1) {
            standardOut.add(batch[0])
        } else {
            // 继续以标准弹幕形式存在，但带上 xN 标记，不再升级为中央黄色高级弹幕。
            standardOut.add(combineBatch(batch))
        }
    }
    /**
     * 将一批 TextData 合并为一个
     */
    private fun combineBatch(batch: List<TextData>): TextData {
        if (batch.size == 1) return batch[0]
        
        // 取第一个作为基准
        val base = batch[0]
        val count = batch.size
        
        val mergedText = if (base is WeightedTextData) {
            WeightedTextData().also {
                it.danmakuId = base.danmakuId
                it.userHash = base.userHash
                it.weight = base.weight
                it.pool = base.pool
                it.likeCount = base.likeCount
                it.isVipGradualColor = base.isVipGradualColor
                it.duplicateCount = count
                it.isSelf = base.isSelf
            }
        } else {
            TextData()
        }
        mergedText.text = "${base.text} x$count"
        mergedText.showAtTime = base.showAtTime
        mergedText.textColor = base.textColor
        mergedText.textSize = base.textSize
        mergedText.layerType = base.layerType
        mergedText.typeface = base.typeface
            
        return mergedText
    }
}
