package com.android.purebilibili.feature.settings

import com.android.purebilibili.data.repository.decodeCommentBackup
import com.android.purebilibili.data.repository.encodeProto3CommentBackup
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class CommentBackupCodecTest {
    private val source = """[{"id":"9007199254740993","oid":"123","type":"1","mid":"77","root":"4","parent":"5","ctime":"1700000000","content":{"message":"评论😀","pictures":[{"imgSrc":"https://example.com/image.png"}]},"replyControl":{"invisible":true}}]"""

    @Test fun importsProto3Int64AndSecondsWithoutPrecisionLossOrInventingVisibility() {
        val record = decodeCommentBackup(source).single()
        assertEquals(9007199254740993L, record.rpid)
        assertEquals(1700000000000L, record.post_time)
        assertEquals(77L, record.uid)
        assertEquals("评论😀", record.message)
        assertEquals("UNKNOWN", record.status)
        assertEquals(4L, record.root)
        assertEquals(5L, record.parent)
    }

    @Test fun compatibleExportPreservesImportedPicturesAndControlMetadata() {
        val records = decodeCommentBackup(source)
        val exported = Json.parseToJsonElement(encodeProto3CommentBackup(records)).jsonArray.single().jsonObject
        assertEquals("9007199254740993", exported["id"]!!.jsonPrimitive.content)
        assertTrue(exported["id"]!!.jsonPrimitive.isString)
        assertEquals("1700000000", exported["ctime"]!!.jsonPrimitive.content)
        assertEquals(1, exported["content"]!!.jsonObject["pictures"]!!.jsonArray.size)
        assertEquals(true, exported["replyControl"]!!.jsonObject["invisible"]!!.jsonPrimitive.boolean)
        assertEquals(records.single().message, decodeCommentBackup(encodeProto3CommentBackup(records)).single().message)
    }

    @Test fun nativeBackupStillImportsAndMissingOwnerRemainsUnknown() {
        assertEquals(99L, decodeCommentBackup("""[{"rpid":99,"oid":123,"type":1}]""").single().rpid)
        assertEquals(0L, decodeCommentBackup("""[{"id":"99","oid":"123","type":"1"}]""").single().uid)
    }

    @Test fun rejectsFractionalIdsTimeOverflowAndUnknownFormatBeforeSaving() {
        listOf("{}", "[{}]", """[{"id":1.5,"oid":"2","type":"1"}]""",
            """[{"id":"1","oid":"2","type":"2147483648"}]""",
            """[{"id":"1","oid":"2","type":"1","ctime":"9223372036854775807"}]""").forEach {
            assertFails { decodeCommentBackup(it) }
        }
    }
}
