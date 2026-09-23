package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfficialUserIdentityStructureTest {

    @Test
    fun `compact official identity uses bolt instead of text capsule`() {
        val source = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/OfficialVerifyBadgePolicy.kt"
        )

        assertTrue(source.contains("fun OfficialVerifyAvatarBadge("))
        assertTrue(source.contains("Icons.Outlined.Bolt"))
        assertTrue(source.contains("Color(0xFFFFCC00)"))
        assertTrue(source.contains("Color(0xFF40C4FF)"))
        assertTrue(source.contains("if (compact) {\n        OfficialVerifyAvatarBadge"))
    }

    @Test
    fun `search and following users place official identity on avatar`() {
        val search = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt"
        )
        val following = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt"
        )

        assertTrue(search.contains("OfficialVerifyAvatarBadge("))
        assertTrue(search.contains("modifier = Modifier.align(Alignment.BottomEnd)"))
        assertTrue(search.contains("UserLevelBadge("))
        assertTrue(search.contains("text = verifyBadge.text"))
        assertFalse(search.contains("compact = true\n                        )"))
        assertTrue(following.contains("OfficialVerifyAvatarBadge("))
        assertFalse(following.contains("FollowingOfficialVerifyBadgeView("))
    }

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(File(relativePath), File("../$relativePath"))
        return candidates.firstOrNull(File::exists)?.readText()
            ?: error("Unable to locate $relativePath")
    }
}
