package com.android.purebilibili.lint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.TestFile;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.util.List;
import org.junit.Test;

public final class PerformanceDetectorTest extends LintDetectorTest {
    @Override
    protected Detector getDetector() {
        return new PerformanceDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return List.of(PerformanceDetector.ISSUE);
    }

    private static final TestFile ANDROID_EFFECT = java("""
            package android.graphics;
            public class RenderEffect {
                public static RenderEffect createBlurEffect(float x, float y, Shader.TileMode mode) {
                    return new RenderEffect();
                }
            }
            """).indented();

    private static final TestFile SHADER = java("""
            package android.graphics;
            public class Shader {
                public enum TileMode { CLAMP, DECAL }
            }
            """).indented();

    private static final TestFile MODIFIER = kotlin("""
            package androidx.compose.ui
            interface Modifier {
                companion object : Modifier
            }
            """).indented();

    private static final TestFile GRAPHICS = kotlin("""
            @file:JvmName("GraphicsLayerModifierKt")
            package androidx.compose.ui.graphics
            import androidx.compose.ui.Modifier
            class RenderEffect(val androidEffect: android.graphics.RenderEffect)
            interface GraphicsLayerScope { var renderEffect: RenderEffect? }
            fun android.graphics.RenderEffect.asComposeRenderEffect(): RenderEffect = RenderEffect(this)
            fun Modifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): Modifier = this
            fun Modifier.graphicsLayer(alpha: Float): Modifier = this
            """).indented();

    private static final TestFile REMEMBER = kotlin("""
            package androidx.compose.runtime
            fun <T> remember(calculation: () -> T): T = calculation()
            """).indented();

    private TestFile[] filesWithApis(TestFile source) {
        return new TestFile[] {ANDROID_EFFECT, SHADER, MODIFIER, GRAPHICS, REMEMBER, source};
    }

    @Test
    public void testFourLayerUpdateHotspotsWithAliasAndConditionalAllocation() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/Hotspots.kt", """
                package sample
                import android.graphics.RenderEffect
                import android.graphics.RenderEffect as AndroidRenderEffect
                import android.graphics.Shader
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.asComposeRenderEffect
                import androidx.compose.ui.graphics.graphicsLayer
                fun commentThread(radius: Float) = Modifier.graphicsLayer {
                    if (radius > 0.5f) {
                        renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    }
                }
                fun commentHost(radius: Float, enabled: Boolean) = Modifier.graphicsLayer {
                    renderEffect = if (enabled && radius > 0.5f) {
                        AndroidRenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    } else null
                }
                fun dynamicDetail(radius: Float) = Modifier.graphicsLayer {
                    renderEffect = AndroidRenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }
                fun dynamicSheet(radius: Float) = Modifier.graphicsLayer {
                    renderEffect = AndroidRenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL).asComposeRenderEffect()
                }
                """).indented())).allowMissingSdk().run()
                .expectErrorCount(4);
    }

    @Test
    public void testThreeRetainedProductionPatternsAreClean() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/Cached.kt", """
                package sample
                import android.graphics.RenderEffect as AndroidRenderEffect
                import android.graphics.Shader
                import androidx.compose.runtime.remember
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.RenderEffect
                import androidx.compose.ui.graphics.asComposeRenderEffect
                import androidx.compose.ui.graphics.graphicsLayer
                class BlurRenderEffectCache {
                    private var radiusPx = Float.NaN
                    private var effect: RenderEffect? = null
                    fun resolve(input: Float): RenderEffect? {
                        if (input != radiusPx) {
                            effect = AndroidRenderEffect.createBlurEffect(input, input, Shader.TileMode.CLAMP).asComposeRenderEffect()
                            radiusPx = input
                        }
                        return effect
                    }
                }
                class MiuixCoveredBlurRenderEffectCache {
                    private var previous = Float.NaN
                    private var saved: RenderEffect? = null
                    fun resolve(radiusPx: Float): RenderEffect? {
                        val quantized = kotlin.math.round(radiusPx * 2f) / 2f
                        if (quantized != previous) {
                            previous = quantized
                            saved = AndroidRenderEffect.createBlurEffect(quantized, quantized, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                        return saved
                    }
                }
                fun helper(radius: Float): Modifier {
                    val cache = remember { BlurRenderEffectCache() }
                    return Modifier.graphicsLayer { renderEffect = cache.resolve(radius) }
                }
                fun quantizedHelper(radius: Float): Modifier {
                    val cache = remember { MiuixCoveredBlurRenderEffectCache() }
                    return Modifier.graphicsLayer { renderEffect = cache.resolve(radius) }
                }
                fun prebuilt(index: Int): Modifier {
                    val effects = remember {
                        List(6) { n ->
                            val r = (n + 1) * 0.25f
                            AndroidRenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL).asComposeRenderEffect()
                        }
                    }
                    return Modifier.graphicsLayer { renderEffect = effects[index] }
                }
                """).indented())).allowMissingSdk().run().expectClean();
    }

    @Test
    public void testRealInlineRadiusCacheUsesResolvedSymbolsNotNames() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/InlineCache.kt", """
                package sample
                import android.graphics.RenderEffect as PlatformEffect
                import android.graphics.Shader
                import androidx.compose.runtime.remember
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.RenderEffect
                import androidx.compose.ui.graphics.asComposeRenderEffect
                import androidx.compose.ui.graphics.graphicsLayer
                class Storage {
                    var token = Float.NaN
                    var payload: RenderEffect? = null
                }
                fun inlineCache(input: Float): Modifier {
                    val box = remember { Storage() }
                    return Modifier.graphicsLayer {
                        val amount = input
                        if (amount != box.token) {
                            box.payload = PlatformEffect.createBlurEffect(amount, amount, Shader.TileMode.CLAMP).asComposeRenderEffect()
                            box.token = amount
                        }
                        renderEffect = box.payload
                    }
                }
                fun capturedCache(input: Float): Modifier {
                    var token = Float.NaN
                    var payload: RenderEffect? = null
                    return Modifier.graphicsLayer {
                        if (token != input) {
                            token = input
                            payload = PlatformEffect.createBlurEffect(input, input, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                        renderEffect = payload
                    }
                }
                fun directLayerStorage(input: Float): Modifier {
                    var token = Float.NaN
                    return Modifier.graphicsLayer {
                        if (input != token && input > 0.5f) {
                            token = input
                            renderEffect = PlatformEffect.createBlurEffect(input, input, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                    }
                }
                """).indented())).allowMissingSdk().run().expectClean();
    }

    @Test
    public void testWhenCacheBranchesKeepGuardAndWritebackBoundaries() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/WhenCache.kt", """
                package sample
                import android.graphics.RenderEffect as PlatformEffect
                import android.graphics.Shader
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.RenderEffect
                import androidx.compose.ui.graphics.asComposeRenderEffect
                import androidx.compose.ui.graphics.graphicsLayer
                class Storage {
                    var token = Float.NaN
                    var payload: RenderEffect? = null
                }
                fun cached(radius: Float, cache: Storage) = Modifier.graphicsLayer {
                    when {
                        radius != cache.token && radius > 0.5f -> {
                            cache.token = radius
                            cache.payload = PlatformEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                    }
                    renderEffect = cache.payload
                }
                fun wrongRadius(radius: Float, other: Float, cache: Storage) = Modifier.graphicsLayer {
                    when {
                        other != cache.token -> {
                            cache.payload = PlatformEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                            cache.token = radius
                        }
                    }
                    renderEffect = cache.payload
                }
                fun unguardedElse(radius: Float, cache: Storage) = Modifier.graphicsLayer {
                    when {
                        radius != cache.token -> cache.token = radius
                        else -> {
                            cache.payload = PlatformEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                    }
                    renderEffect = cache.payload
                }
                fun writeInAnotherBranch(radius: Float, cache: Storage) = Modifier.graphicsLayer {
                    when {
                        radius != cache.token -> {
                            cache.payload = PlatformEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                        else -> cache.token = radius
                    }
                    renderEffect = cache.payload
                }
                """).indented())).allowMissingSdk().run().expectErrorCount(3);
    }

    @Test
    public void testImportedFunctionAliasesKeepSymbolIdentity() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/Aliases.kt", """
                package sample
                import android.graphics.RenderEffect as PlatformEffect
                import android.graphics.Shader
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.RenderEffect
                import androidx.compose.ui.graphics.asComposeRenderEffect as convert
                import androidx.compose.ui.graphics.graphicsLayer as updateLayer
                fun hotspot(input: Float) = Modifier.updateLayer {
                    renderEffect = PlatformEffect.createBlurEffect(input, input, Shader.TileMode.CLAMP).convert()
                }
                fun cached(input: Float): Modifier {
                    var token = Float.NaN
                    var effect: RenderEffect? = null
                    return Modifier.updateLayer {
                        if (input != token) {
                            effect = PlatformEffect.createBlurEffect(input, input, Shader.TileMode.CLAMP).convert()
                            token = input
                        }
                        renderEffect = effect
                    }
                }
                """).indented())).allowMissingSdk().run().expectErrorCount(1);
    }

    @Test
    public void testUnrelatedAndIncompleteGuardsCannotHideCreation() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/FalseGuards.kt", """
                package sample
                import android.graphics.RenderEffect
                import android.graphics.Shader
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.asComposeRenderEffect
                import androidx.compose.ui.graphics.graphicsLayer
                class Storage {
                    var lastRadius = Float.NaN
                    var effect: androidx.compose.ui.graphics.RenderEffect? = null
                }
                fun unrelated(radius: Float, other: Float, cache: Storage) = Modifier.graphicsLayer {
                    if (other != cache.lastRadius) { cache.lastRadius = other }
                    renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }
                fun wrongKey(radius: Float, other: Float, cache: Storage) = Modifier.graphicsLayer {
                    if (other != cache.lastRadius) {
                        cache.effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        cache.lastRadius = other
                    }
                    renderEffect = cache.effect
                }
                fun missingWriteback(radius: Float, cache: Storage) = Modifier.graphicsLayer {
                    if (radius != cache.lastRadius) {
                        cache.effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    }
                    renderEffect = cache.effect
                }
                fun wrongReceiver(radius: Float, first: Storage, second: Storage) = Modifier.graphicsLayer {
                    if (radius != first.lastRadius) {
                        first.effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        second.lastRadius = radius
                    }
                    renderEffect = first.effect
                }
                fun resetEveryUpdate(radius: Float, cache: Storage) = Modifier.graphicsLayer {
                    cache.lastRadius = Float.NaN
                    if (radius != cache.lastRadius) {
                        cache.effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        cache.lastRadius = radius
                    }
                    renderEffect = cache.effect
                }
                fun onlyOneAxis(x: Float, y: Float, cache: Storage) = Modifier.graphicsLayer {
                    if (x != cache.lastRadius) {
                        cache.effect = RenderEffect.createBlurEffect(x, y, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        cache.lastRadius = x
                    }
                    renderEffect = cache.effect
                }
                fun notRetained(radius: Float) = Modifier.graphicsLayer {
                    val local = Storage()
                    if (radius != local.lastRadius) {
                        local.effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        local.lastRadius = radius
                    }
                    renderEffect = local.effect
                }
                """).indented())).allowMissingSdk().run().expectErrorCount(7);
    }

    @Test
    public void testFakeApisCommentsStringsAndExtensionDeclarationsAreClean() {
        lint().files(filesWithApis(kotlin("src/main/java/sample/Names.kt", """
                package sample
                import android.graphics.RenderEffect
                import android.graphics.Shader
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.asComposeRenderEffect
                import androidx.compose.ui.graphics.graphicsLayer
                object FakeEffect {
                    fun createBlurEffect(x: Float, y: Float, mode: Shader.TileMode) =
                        RenderEffect.createBlurEffect(x, y, mode).asComposeRenderEffect()
                }
                class FakeLayer { fun graphicsLayer(block: () -> Unit) {} }
                fun fakeEffect(radius: Float) = Modifier.graphicsLayer {
                    // RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                    val note = "RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)"
                    renderEffect = FakeEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                }
                fun fakeLayer(radius: Float) = FakeLayer().graphicsLayer {
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                }
                fun Modifier.createBlurEffect(): Modifier = this
                fun floatOverload(radius: Float) = Modifier.graphicsLayer(
                    alpha = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).hashCode().toFloat()
                )
                """).indented())).allowMissingSdk().run().expectClean();
    }

    @Test
    public void testStandardSuppressionAndTestSourcesAreRespected() {
        lint().files(
                manifest().minSdk(26),
                gradle("""
                        plugins { id 'com.android.library' }
                        android {
                            namespace 'test.performance'
                            compileSdk 37
                            lint { checkTestSources true }
                        }
                        """),
                ANDROID_EFFECT, SHADER, MODIFIER, GRAPHICS,
                kotlin("src/main/java/sample/Suppressed.kt", """
                        package sample
                        import android.graphics.RenderEffect
                        import android.graphics.Shader
                        import androidx.compose.ui.Modifier
                        import androidx.compose.ui.graphics.asComposeRenderEffect
                        import androidx.compose.ui.graphics.graphicsLayer
                        @Suppress("BiliPaiFrameBlurAllocation")
                        fun reviewed(radius: Float) = Modifier.graphicsLayer {
                            renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                        """).indented(),
                kotlin("src/test/java/sample/OnlyInTests.kt", """
                        package sample
                        import android.graphics.RenderEffect
                        import android.graphics.Shader
                        import androidx.compose.ui.Modifier
                        import androidx.compose.ui.graphics.asComposeRenderEffect
                        import androidx.compose.ui.graphics.graphicsLayer
                        fun onlyInTests(radius: Float) = Modifier.graphicsLayer {
                            renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                        """).indented(),
                kotlin("src/androidTest/java/sample/OnlyInAndroidTests.kt", """
                        package sample
                        import android.graphics.RenderEffect
                        import android.graphics.Shader
                        import androidx.compose.ui.Modifier
                        import androidx.compose.ui.graphics.asComposeRenderEffect
                        import androidx.compose.ui.graphics.graphicsLayer
                        fun onlyInAndroidTests(radius: Float) = Modifier.graphicsLayer {
                            renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                        }
                        """).indented()).allowMissingSdk().run().expectClean();
    }
}
