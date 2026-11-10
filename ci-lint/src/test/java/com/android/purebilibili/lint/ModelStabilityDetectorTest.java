package com.android.purebilibili.lint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.util.List;
import org.junit.Test;

public final class ModelStabilityDetectorTest extends LintDetectorTest {
    @Override
    protected Detector getDetector() {
        return new ModelStabilityDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return List.of(ModelStabilityDetector.ISSUE);
    }

    @Test
    public void testConstructorAndMemberVarsIncludingPrivateCaches() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                class RenamedModel(var renamed: Int, private var cache: Int) {
                    var exposed = 0
                    private var neverChangedCache = 0
                    var withPrivateSetter = 0
                        private set
                    val safe = 0
                }
                """)).run().expectErrorCount(5);
    }

    @Test
    public void testGetterFunctionInitializerAndConstructorLocalsAreNotProperties() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                class Model(seed: Int, ignored: MutableList<Int>) {
                    val score: Int
                        get() {
                            var local = 1
                            val values = mutableListOf(2)
                            local += values.size
                            return local
                        }
                    init {
                        var initial = seed
                        val values = mutableListOf(initial)
                        initial += values.size
                    }
                    constructor() : this(0, mutableListOf()) {
                        var local = 0
                        local++
                    }
                    fun compute(input: MutableList<Int>): Int {
                        var local = 0
                        input.forEach { var nested = it; local += nested }
                        return local
                    }
                    val text = "var fake: MutableList<Int>"
                    // var commented: ArrayList<Int>
                    /* val hidden = mutableListOf(1) */
                }
                fun outside() { var local = 1; local++ }
                """)).run().expectClean();
    }

    @Test
    public void testResolvedMutableInterfacesAndInferredTypes() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                class Model(
                    val list: MutableList < String >,
                    val map: MutableMap < String, Int >,
                    val set: MutableSet < Int >,
                    val collection: MutableCollection < Int >,
                    val iterator: MutableIterator < Int >,
                    val listIterator: MutableListIterator < Int >
                ) {
                    private val cache = mutableListOf(1)
                    val inferredMap = mutableMapOf("a" to 1)
                    val inferredSet = mutableSetOf(1)
                    val computed: MutableList<Int> get() = mutableListOf(1)
                }
                """)).run().expectErrorCount(10);
    }

    @Test
    public void testReadOnlyInterfacesAndImmutableLookingSameNamesAreAllowed() {
        lint().allowMissingSdk().files(
                kotlin("""
                        package example
                        class MutableList<T>
                        class MutableMap<K, V>
                        class MutableSet<T>
                        class ArrayList<T>
                        class IntArray
                        """),
                kotlin("""
                        package com.android.purebilibili.data.model
                        class Model(
                            val list: List < String >,
                            val map: Map < String, Int >,
                            val set: Set < Int >,
                            val collection: Collection<Int>,
                            val iterator: Iterator<Int>,
                            val listIterator: ListIterator<Int>,
                            val fakeList: example.MutableList<Int>,
                            val fakeMap: example.MutableMap<String, Int>,
                            val fakeSet: example.MutableSet<Int>,
                            val fakeImplementation: example.ArrayList<Int>,
                            val fakeArray: example.IntArray
                        ) {
                            val readOnly: List<Int> = mutableListOf(1)
                        }
                        """)).run().expectClean();
    }

    @Test
    public void testTypeAliasesNestedTypeArgumentsAndJavaImplementations() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                import java.util.ArrayList as RenamedImplementation
                import kotlin.collections.MutableList as RenamedInterface
                typealias Cache = RenamedInterface<Int>
                class Model(
                    val alias: Cache,
                    val nested: List < MutableMap < String, Int > >,
                    val nullable: MutableSet<Int>?,
                    val implementation: RenamedImplementation<Int>,
                    val hashMap: java.util.HashMap<String, Int>,
                    val hashSet: java.util.HashSet<Int>,
                    val linkedMap: java.util.LinkedHashMap<String, Int>,
                    val linkedSet: java.util.LinkedHashSet<Int>
                )
                """)).run().expectErrorCount(8);
    }

    @Test
    public void testResolvedMutableSubclassIsNotHiddenByRenaming() {
        lint().allowMissingSdk().files(
                kotlin("""
                        package example
                        class RenamedCache : java.util.ArrayList<Int>()
                        """),
                kotlin("""
                        package com.android.purebilibili.data.model
                        class Model(val contents: example.RenamedCache)
                        """)).run().expectErrorCount(1);
    }

    @Test
    public void testObjectPrimitiveNullableAndNestedArrays() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                class Arrays(
                    val objects: Array<String>,
                    val booleans: BooleanArray,
                    val bytes: ByteArray,
                    val chars: CharArray,
                    val shorts: ShortArray,
                    val ints: IntArray,
                    val longs: LongArray,
                    val floats: FloatArray,
                    val doubles: DoubleArray,
                    val nullable: Array<Int>?,
                    val nested: List<IntArray>
                ) {
                    val inferred = intArrayOf(1)
                }
                """)).run().expectErrorCount(12);
    }

    @Test
    public void testUnsignedArraysAndConstructorVarargProperties() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                @OptIn(ExperimentalUnsignedTypes::class)
                class UnsignedArrays(
                    val bytes: UByteArray,
                    val shorts: UShortArray,
                    val ints: UIntArray,
                    val longs: ULongArray
                )
                class PrimitiveVararg(vararg val values: Int)
                class ObjectVararg(vararg val values: String)
                class NotAProperty(vararg values: Int)
                """)).run().expectErrorCount(6);
    }

    @Test
    public void testJavaFieldsCollectionsAndArrays() {
        lint().allowMissingSdk().files(java("""
                package com.android.purebilibili.data.model;
                public class JavaModel {
                    public int mutable = 0;
                    private int privateCache = 0;
                    public final java.util.List<String> list = new java.util.ArrayList<>();
                    private final java.util.HashMap<String, Integer> cache = new java.util.HashMap<>();
                    public final int[] primitive = new int[0];
                    public final String[][] nested = new String[0][0];
                    public final String safe = "MutableList var Array";
                    public int compute() { int local = 0; return ++local; }
                }
                """)).run().expectErrorCount(6);
    }

    @Test
    public void testOnlyThePromisedPackageAndItsSubpackages() {
        lint().allowMissingSdk().files(
                kotlin("""
                        package com.android.purebilibili.data.model.nested
                        class Nested(var value: Int)
                        """),
                kotlin("""
                        package com.android.purebilibili.data.models
                        class Unpromised(var value: Int, val cache: MutableList<Int>)
                        """),
                kotlin("""
                        package com.android.purebilibili.feature.model
                        class Other(var value: Int, val bytes: ByteArray)
                        """)).run().expectErrorCount(1);
    }

    @Test
    public void testNestedClassAndObjectProperties() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                class Outer {
                    class Nested(var state: Int)
                    companion object { private var cache = 0 }
                }
                object Singleton { val cache = mutableListOf(1) }
                """)).run().expectErrorCount(3);
    }

    @Test
    public void testStandardLintSuppression() {
        lint().allowMissingSdk().files(kotlin("""
                package com.android.purebilibili.data.model
                @Suppress("BiliPaiModelStability")
                class Suppressed(var value: Int, val cache: MutableList<Int>)
                class Model {
                    @Suppress("BiliPaiModelStability")
                    private var cache = 0
                }
                """)).run().expectClean();
    }

    @Test
    public void testTestSourcesAreExcludedEvenWhenLintVisitsThem() {
        lint().allowMissingSdk().configureDriver(driver -> driver.setCheckTestSources(true)).files(
                gradle("""
                        plugins { id 'com.android.library' }
                        android {
                            namespace 'test.models'
                            compileSdk 37
                        }
                        """),
                manifest().minSdk(26),
                kotlin("src/test/java/com/android/purebilibili/data/model/TestModel.kt", """
                        package com.android.purebilibili.data.model
                        class TestModel(var value: Int, val cache: MutableList<Int>)
                        """),
                kotlin("src/androidTest/java/com/android/purebilibili/data/model/DeviceModel.kt", """
                        package com.android.purebilibili.data.model
                        class DeviceModel(var value: Int, val bytes: ByteArray)
                        """)).run().expectClean();
    }

    @Test
    public void testModelsInDependentAndroidLibraryAreChecked() {
        ProjectDescription coreData = project(
                manifest().minSdk(23),
                gradle("""
                        plugins { id 'com.android.library' }
                        android { namespace 'example.core'; compileSdk 36 }
                        """),
                kotlin("src/main/java/com/android/purebilibili/data/model/RemoteModel.kt", """
                        package com.android.purebilibili.data.model
                        class RemoteModel(var title: String, val cache: MutableList<Int>)
                        """))
                .name("core-data").type(ProjectDescription.Type.LIBRARY);
        ProjectDescription app = project(
                manifest().minSdk(23),
                gradle("""
                        plugins { id 'com.android.application' }
                        android {
                            namespace 'example.app'
                            compileSdk 36
                            lint { checkDependencies true }
                        }
                        dependencies { implementation project(':core-data') }
                        """))
                .name("app").dependsOn(coreData);
        lint().allowMissingSdk().projects(app, coreData)
                .configureDriver(driver -> driver.setCheckDependencies(true))
                .run().expectErrorCount(2);
    }
}
