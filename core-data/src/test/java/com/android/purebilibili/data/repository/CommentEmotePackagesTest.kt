package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.EmoteData
import com.android.purebilibili.data.model.response.EmoteItem
import com.android.purebilibili.data.model.response.EmotePackage
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentEmotePackagesTest {
    @Test
    fun emptyUserPanel_preservesAllPackagesAndRestoresMissingStandardGroups() {
        val userFaces = pack(1, "小黄脸", "[doge]")
        val custom = pack(99, "用户表情", "[用户_开心]")
        val televisions = pack(2, "tv_小电视", "[tv_doge]")
        val hotWords = pack(53, "热词系列一", "[热词系列_好耶]")
        val kaomojis = pack(4, "颜文字", "(^_^)")
        val result = mergeCommentEmotePackages(
            EmoteData(packages = emptyList(), all_packages = listOf(userFaces, custom)),
            listOf(pack(1, "小黄脸", "[笑哭]"), televisions, hotWords, kaomojis),
        )

        assertEquals(setOf(1L, 2L, 53L, 4L, 99L), result.map { it.id }.toSet())
        assertEquals(5, result.size)
        assertEquals(userFaces, result.single { it.id == 1L })
        assertEquals(custom, result.single { it.id == 99L })
        assertEquals(televisions, result.single { it.id == 2L })
        assertEquals(hotWords, result.single { it.id == 53L })
        assertEquals(kaomojis, result.single { it.id == 4L })
    }

    @Test
    fun missingContents_areReplacedWithoutDuplicatingCategoriesOrOverwritingUserPacks() {
        val faces = pack(1, "小黄脸", "[微笑]", "[doge]")
        val userHotWords = pack(53, "热词系列一", "[热词系列_知识增加]", "[热词系列_好耶]")
        val result = mergeCommentEmotePackages(
            EmoteData(packages = listOf(faces.copy(emote = emptyList()), userHotWords)),
            listOf(faces, pack(53, "热词系列一", "[热词系列_泪目]")),
        )

        assertEquals(listOf(1L, 53L), result.map { it.id })
        assertEquals(faces, result.single { it.id == 1L })
        assertEquals(userHotWords, result.single { it.id == 53L })
    }

    private fun pack(id: Long, name: String, vararg tokens: String) = EmotePackage(
        id = id,
        text = name,
        url = "https://i0.hdslb.com/package/$id.png",
        emote = tokens.mapIndexed { index, token ->
            EmoteItem(id = id * 100 + index, text = token, url = "https://i0.hdslb.com/emote/$id/$index.png")
        },
    )
}
