package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveMatchBracketNode

internal data class LiveBracketPlacement(val node: LiveMatchBracketNode, val column: Int, val row: Float, val key: String)
internal data class LiveBracketEdge(val child: Int, val parent: Int)
internal data class LiveBracketLayout(val nodes: List<LiveBracketPlacement>, val edges: List<LiveBracketEdge>, val columns: Int, val rows: Int, val truncated: Boolean = false)

/** 左侧前轮、右侧后轮。连线仅来自 children，限制节点与深度，避免异常数据撑满界面。 */
internal fun liveMatchBracketLayout(roots: List<LiveMatchBracketNode>): LiveBracketLayout {
    val nodes = mutableListOf<LiveBracketPlacement>()
    val edges = mutableListOf<LiveBracketEdge>()
    var nextLeaf = 0
    var maxDepth = 0
    var truncated = false
    fun visit(node: LiveMatchBracketNode, depth: Int, key: String): Int? {
        if (nodes.size >= 64 || depth > 8) {
            truncated = true
            return null
        }
        val index = nodes.size
        maxDepth = maxOf(maxDepth, depth)
        nodes.add(LiveBracketPlacement(node, depth, 0f, key))
        val children = node.children.mapIndexedNotNull { childIndex, child -> visit(child, depth + 1, "$key.$childIndex") }
        val row = if (children.isEmpty()) nextLeaf++.toFloat() else children.map { nodes[it].row }.average().toFloat()
        nodes[index] = nodes[index].copy(row = row)
        children.forEach { edges.add(LiveBracketEdge(it, index)) }
        return index
    }
    roots.forEachIndexed { index, node -> visit(node, 0, index.toString()) }
    return LiveBracketLayout(nodes.map { it.copy(column = maxDepth - it.column) }, edges, if (nodes.isEmpty()) 0 else maxDepth + 1, nextLeaf, truncated)
}

internal fun liveMatchGradeLabel(average: String?, users: Long?): String {
    if (users == 0L) return "暂无评分"
    val value = average?.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..10f } ?: return "—"
    return String.format(java.util.Locale.ROOT, "%.1f", value)
}
