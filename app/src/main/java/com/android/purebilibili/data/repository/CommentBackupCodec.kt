package com.android.purebilibili.data.repository

import com.android.purebilibili.core.database.entity.CommentFraudRecord
import kotlinx.serialization.json.*

/** 评论备份支持本地记录数组及 ReplyInfo Proto3 JSON 数组，64 位 ID 按十进制整数读取。 */
internal fun decodeCommentBackup(content: String): List<CommentFraudRecord> {
    val json = Json { ignoreUnknownKeys = true }
    val array = json.parseToJsonElement(content) as? JsonArray ?: error("备份必须是评论数组")
    require(array.size <= 20000) { "单次最多导入 20000 条评论" }
    return array.map { element ->
        val item = element as? JsonObject ?: error("评论记录格式无效")
        if ("rpid" in item) {
            json.decodeFromJsonElement<CommentFraudRecord>(item)
        } else {
            fun number(key: String, default: Long? = null): Long {
                val raw = item[key] ?: return default ?: error("缺少评论字段 $key")
                return (raw as? JsonPrimitive)?.content?.toLongOrNull() ?: error("评论字段 $key 必须为整数")
            }
            val seconds = number("ctime", 0L)
            require(seconds in 0..(Long.MAX_VALUE / 1000L)) { "评论时间无效" }
            val type = number("type")
            require(type in 1..Int.MAX_VALUE.toLong()) { "评论区类型无效" }
            val message = (item["content"] as? JsonObject)?.get("message")?.jsonPrimitive?.content.orEmpty()
            val directMid = number("mid", 0L)
            require(directMid >= 0L) { "评论账号 ID 无效" }
            val memberMid = (item["member"] as? JsonObject)?.get("mid")?.let {
                (it as? JsonPrimitive)?.content?.toLongOrNull() ?: error("评论账号 ID 必须为整数")
            } ?: 0L
            val mid = directMid.takeIf { it > 0L } ?: memberMid
            CommentFraudRecord(
                rpid = number("id"), oid = number("oid"), type = type.toInt(), uid = mid,
                root = number("root", 0L), parent = number("parent", 0L), message = message,
                post_time = seconds * 1000L, timestamp = seconds * 1000L, proto3_payload = item.toString(),
            )
        }
    }.also(::validateCommentArchiveImport)
}

/** 导入时的附加字段原样保留；本地身份和正文作为权威值，检查状态只写入原生格式。 */
internal fun encodeProto3CommentBackup(records: List<CommentFraudRecord>): String =
    Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), JsonArray(records.map { record ->
        val original = record.proto3_payload?.let {
            runCatching { Json.parseToJsonElement(it) as? JsonObject }.getOrNull()
        }
        buildJsonObject {
            original?.forEach { (key, value) -> put(key, value) }
            put("id", record.rpid.toString())
            put("oid", record.oid.toString())
            put("type", record.type.toString())
            put("mid", record.uid.toString())
            put("root", record.root.toString())
            put("parent", record.parent.toString())
            put("ctime", (record.post_time / 1000L).toString())
            putJsonObject("content") {
                (original?.get("content") as? JsonObject)?.forEach { (key, value) -> put(key, value) }
                put("message", record.message)
            }
        }
    }))
