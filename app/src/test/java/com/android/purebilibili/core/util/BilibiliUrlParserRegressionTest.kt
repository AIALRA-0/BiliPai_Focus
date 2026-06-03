package com.android.purebilibili.core.util
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BilibiliUrlParserRegressionTest {

    @Test
    fun parse_extractsBvidFromVideoUrl() {
        val result = BilibiliUrlParser.parse("https://www.bilibili.com/video/BV1xx411c7mD")
        assertTrue(result.isValid)
        assertEquals("BV1xx411c7mD", result.getVideoId())
    }

    @Test
    fun extractUrls_findsMultipleLinksInSharedText() {
        val text = "看看这个 https://www.bilibili.com/video/BV1xx411c7mD 还有这个 https://b23.tv/abc123"
        val urls = BilibiliUrlParser.extractUrls(text)
        assertEquals(2, urls.size)
        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD", urls[0])
        assertEquals("https://b23.tv/abc123", urls[1])
    }

    @Test
    fun parse_extractsAidFromBilibiliSchemeVideoPath() {
        val result = BilibiliUrlParser.parse("bilibili://video/170001")

        assertTrue(result.isValid)
        assertEquals("av170001", result.getVideoId())
    }

    @Test
    fun parse_treatsLikelyDynamicIdVideoDeepLinkAsDynamicTarget() {
        val result = BilibiliUrlParser.parse("bilibili://video/1199344045210468386")

        assertTrue(result.isValid)
        assertNull(result.getVideoId())
        assertEquals("1199344045210468386", result.getDynamicTargetId())
    }

    @Test
    fun parse_extractsAidFromHttpsVideoNumericPath() {
        val result = BilibiliUrlParser.parse("https://www.bilibili.com/video/170001")

        assertTrue(result.isValid)
        assertEquals("av170001", result.getVideoId())
    }

    @Test
    fun parse_acceptsLowercaseBvPrefix() {
        val result = BilibiliUrlParser.parse("https://www.bilibili.com/video/bv1xx411c7mD")

        assertTrue(result.isValid)
        assertEquals("BV1xx411c7mD", result.getVideoId())
    }

    @Test
    fun parse_acceptsMobileOpusDynamicLink() {
        val result = BilibiliUrlParser.parse("https://m.bilibili.com/opus/1015637114125025318")

        assertTrue(result.isValid)
        assertNull(result.getVideoId())
        assertEquals("1015637114125025318", result.getDynamicTargetId())
    }

    @Test
    fun parse_acceptsDesktopDynamicLink() {
        val result = BilibiliUrlParser.parse("https://t.bilibili.com/1015637114125025318")

        assertTrue(result.isValid)
        assertNull(result.getVideoId())
        assertEquals("1015637114125025318", result.getDynamicTargetId())
    }

    @Test
    fun parseDeepLink_prefersWrappedOpusUrlOverMisleadingAidQuery() {
        val result = BilibiliUrlParser.parseDeepLink(
            "bilibili://browser?url=https%3A%2F%2Fm.bilibili.com%2Fopus%2F1015637114125025318&aid=170001"
        )

        assertTrue(result.isValid)
        assertNull(result.getVideoId())
        assertEquals("1015637114125025318", result.getDynamicTargetId())
    }

    @Test
    fun parseDeepLink_prefersExplicitOpusIdOverMisleadingAidQuery() {
        val result = BilibiliUrlParser.parseDeepLink(
            "bilibili://opus/detail?opus_id=1015637114125025318&aid=170001"
        )

        assertTrue(result.isValid)
        assertNull(result.getVideoId())
        assertEquals("1015637114125025318", result.getDynamicTargetId())
    }
}
