package com.android.purebilibili.core.ui.lint

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 白名单体量棘轮。
 *
 * StyleLint 的设计意图是「只拦新增违规」，但它漏了一个环节：往
 * [StyleLintAllowlist] 里加一条豁免只需要改一行，**没有任何机制让人注意到总数在涨**。
 * 于是白名单可以无声膨胀，lint 看起来还是绿的。
 *
 * 加上这三个上限后，新增豁免必须同时把数字改大，PR diff 里就会出现
 * 「把上限增加 1」这个刺眼的动作——这正是文件头注释里
 * 「Adding a new path here is a documented exception, not a default」想要的效果。
 *
 * 迁移完一个文件就把对应数字调小，让棘轮只能往一个方向走。
 */
class StyleLintAllowlistRatchetTest {

    @Test
    fun shapeAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.SHAPE_HITS.size <= MAX_SHAPE_HITS,
            "SHAPE_HITS 有 ${StyleLintAllowlist.SHAPE_HITS.size} 条，超过上限 $MAX_SHAPE_HITS。" +
                "迁移到 AppShapes 后请调小上限；确需新增豁免，请在 PR 里写明像素级理由。",
        )
    }

    @Test
    fun motionAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.MOTION_HITS.size <= MAX_MOTION_HITS,
            "MOTION_HITS 有 ${StyleLintAllowlist.MOTION_HITS.size} 条，超过上限 $MAX_MOTION_HITS。" +
                "迁移到 AppMotionTokens 后请调小上限。",
        )
    }

    @Test
    fun surfaceAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.SURFACE_HITS.size <= MAX_SURFACE_HITS,
            "SURFACE_HITS 有 ${StyleLintAllowlist.SURFACE_HITS.size} 条，超过上限 $MAX_SURFACE_HITS。" +
                "MaterialTheme.colorScheme.surface → AppSurfaceTokens 基本是 1:1 机械替换、" +
                "视觉零差异，是最容易削减的一类。",
        )
    }

    @Test
    fun colorAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.COLOR_HITS.size <= MAX_COLOR_HITS,
            "COLOR_HITS 有 ${StyleLintAllowlist.COLOR_HITS.size} 条，超过上限 $MAX_COLOR_HITS。" +
                "迁移到主题色或命名 Palette 后请调小上限。",
        )
    }

    @Test
    fun spacingAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.SPACING_HITS.size <= MAX_SPACING_HITS,
            "SPACING_HITS 有 ${StyleLintAllowlist.SPACING_HITS.size} 条，超过上限 $MAX_SPACING_HITS。" +
                "迁移到 AppSpacingTokens 或命名 Spec 后请调小上限。",
        )
    }

    @Test
    fun typographyAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.TYPOGRAPHY_HITS.size <= MAX_TYPOGRAPHY_HITS,
            "TYPOGRAPHY_HITS 有 ${StyleLintAllowlist.TYPOGRAPHY_HITS.size} 条，超过上限 $MAX_TYPOGRAPHY_HITS。" +
                "迁移到 MaterialTheme.typography 后请调小上限。",
        )
    }

    /**
     * 反方向的棘轮：已纳管的 feature 前缀只能增不能减。
     *
     * 少了这一条，「迁移」可以靠把前缀从名单里删掉来伪造通过。
     */
    @Test
    fun migratedFeaturePrefixesDoNotShrink() {
        assertTrue(
            StyleLintAllowlist.MIGRATED_TOKEN_PREFIXES.size >= MIN_MIGRATED_PREFIXES,
            "MIGRATED_TOKEN_PREFIXES 只剩 ${StyleLintAllowlist.MIGRATED_TOKEN_PREFIXES.size} 条，" +
                "低于下限 $MIN_MIGRATED_PREFIXES。已纳管的 feature 不应退出 spacing/color/" +
                "typography lint 覆盖。",
        )
    }

    @Test
    fun allowlistContentsMatchReviewedSnapshot() {
        assertEquals(
            MIGRATED_PREFIXES_SHA256,
            sha256(StyleLintAllowlist.MIGRATED_TOKEN_PREFIXES),
            "MIGRATED_TOKEN_PREFIXES 内容发生变化。已迁移模块不能被同数量的其他前缀替换。",
        )
        assertEquals(
            SHAPE_HITS_SHA256,
            sha256(StyleLintAllowlist.SHAPE_HITS),
            "SHAPE_HITS 内容发生变化。迁移或新增例外时请审查具体路径并更新摘要。",
        )
        assertEquals(
            MOTION_HITS_SHA256,
            sha256(StyleLintAllowlist.MOTION_HITS),
            "MOTION_HITS 内容发生变化。迁移或新增例外时请审查具体路径并更新摘要。",
        )
        assertEquals(
            SURFACE_HITS_SHA256,
            sha256(StyleLintAllowlist.SURFACE_HITS),
            "SURFACE_HITS 内容发生变化。迁移或新增例外时请审查具体路径并更新摘要。",
        )
        assertEquals(
            COLOR_HITS_SHA256,
            sha256(StyleLintAllowlist.COLOR_HITS),
            "COLOR_HITS 内容发生变化。迁移或新增例外时请审查具体路径并更新摘要。",
        )
        assertEquals(
            SPACING_HITS_SHA256,
            sha256(StyleLintAllowlist.SPACING_HITS),
            "SPACING_HITS 内容发生变化。迁移或新增例外时请审查具体路径并更新摘要。",
        )
        assertEquals(
            TYPOGRAPHY_HITS_SHA256,
            sha256(StyleLintAllowlist.TYPOGRAPHY_HITS),
            "TYPOGRAPHY_HITS 内容发生变化。迁移或新增例外时请审查具体路径并更新摘要。",
        )
    }

    private fun sha256(values: Set<String>): String {
        val bytes = values.sorted().joinToString("\n").toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
    }

    private companion object {
        // 冻结于接入棘轮时的实测值，只能调小。
        //
        // 注意：接入时 HardcodedShape/Motion/SurfaceLintTest 三条本身是**红的**——
        // audio/ListenVideoScreen.kt、video/ui/gesture/GestureLevelOverlay.kt、
        // home/components/cards/HomeStyleSingleColumnVideoCard.kt 等文件在各自的
        // feature commit 里引入了新的硬编码，却没有同步更新白名单，而这些测试从来
        // 没有进过 CI，所以一直没人发现。
        //
        // 因此下面这三个数字是「当前白名单的长度」，不是「当前违规为零」。
        // 修复方向有两条，选哪条要在 PR 里说清楚：
        //   1. 把那些文件迁到 AppShapes / AppMotionTokens / AppSurfaceTokens（推荐）；
        //   2. 确有像素级理由无法迁移，则加进白名单并把这里的上限一并调大。
        // 第 2 条会让上限变大，这正是设计意图——它必须是一个显眼、需要解释的动作。
        //
        // 81 → 85：收纳 4 个接入棘轮前的存量字面圆角文件（MusicPlayerContent、
        // DynamicCard、ProfileLoadingSkeleton、AudioQualitySelectionMenu）。
        // 它们带 preset 缩放（MD3 0.9x / MIUIX 1.15x），换 AppShapes 会改变实际渲染，
        // 且 8dp 无对应 ContainerLevel；迁移到命名 Spec 后调小。
        // 85 → 86：上游 SearchLandingUi（悬浮建议卡片）带入的存量字面圆角，
        // 同样受 preset 缩放约束（16/10/4dp 无对应 ContainerLevel）；迁移后调小。
        const val MAX_SHAPE_HITS = 0
        const val MAX_MOTION_HITS = 15
        const val MAX_SURFACE_HITS = 48

        // 新增的 color/spacing/typography 豁免棘轮：接入时即收纳全部存量违规，
        // 均为非 4dp 刻度尺寸或深色 SuperChat 品牌色等有像素级理由的豁免。
        const val MAX_COLOR_HITS = 2
        // 6 → 7：上游 LiveHomeSelectableChip 的 compact 纵向 5dp（不在 4dp 刻度上），
        // 取整会改变紧凑态像素布局；迁移到命名 Spec 后调小。
        const val MAX_SPACING_HITS = 7
        const val MAX_TYPOGRAPHY_HITS = 1

        // 只能调大。直播与第一轮信息流模块已完成 token 迁移。
        const val MIN_MIGRATED_PREFIXES = 6

        const val MIGRATED_PREFIXES_SHA256 =
            "9eb8920bc5953589f037ba610fc3a1ec74c98a8a6ce69bb3286f8a31e0501a16"
        const val SHAPE_HITS_SHA256 =
            "aaa828f33ef8722244a70faa3602e25546f0e680db06a94503a7a0ec4b174f82"
        const val MOTION_HITS_SHA256 =
            "eb883a77a6e9e2f94733b73408f83d02a551b475b0cfbe119f5ee432a4df4925"
        const val SURFACE_HITS_SHA256 =
            "4655ba41f9fd9c1802650eb2ee526ef9633c9bdb872c0e58a8e90a31306bf6e0"
        const val COLOR_HITS_SHA256 =
            "472bbaea88ea315505d7e5d1cfcc834664bbc0da24b91211bf4c1d3d8f1240d9"
        const val SPACING_HITS_SHA256 =
            "a05ad9d223632622f6841a35d15dc12ec11cd945d297f533606537a344872c55"
        const val TYPOGRAPHY_HITS_SHA256 =
            "9da424c82f8cdb1d3429277b6b8bcb9d8a7a6156f8a2cbfb252eb41a7ab3098d"
    }
}
