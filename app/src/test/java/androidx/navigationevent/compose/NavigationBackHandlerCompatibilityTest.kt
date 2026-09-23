package androidx.navigationevent.compose

import kotlin.test.Test
import kotlin.test.assertTrue

class NavigationBackHandlerCompatibilityTest {

    @Test
    fun retainsLegacyNavigation3BackHandlerAbi() {
        val legacyParameterTypes = listOf(
            "androidx.navigationevent.compose.NavigationEventState",
            "kotlin.jvm.functions.Function0",
            "kotlin.jvm.functions.Function0",
            "androidx.compose.runtime.Composer",
            "int",
            "int",
        )

        val hasLegacyBridge = Class.forName(
            "androidx.navigationevent.compose.NavigationEventHandlerKt"
        ).declaredMethods.any { method ->
            method.name == "NavigationBackHandler" &&
                method.parameterTypes.map { it.name } == legacyParameterTypes
        }

        assertTrue(hasLegacyBridge)
    }

    @Test
    fun retainsMiuixShortCallbackBackHandlerAbi() {
        val miuixParameterTypes = listOf(
            "androidx.navigationevent.compose.NavigationEventState",
            "boolean",
            "kotlin.jvm.functions.Function0",
            "kotlin.jvm.functions.Function0",
            "androidx.compose.runtime.Composer",
            "int",
            "int",
        )

        val hasMiuixBridge = Class.forName(
            "androidx.navigationevent.compose.NavigationEventHandlerKt"
        ).declaredMethods.any { method ->
            method.name == "NavigationBackHandler" &&
                method.parameterTypes.map { it.name } == miuixParameterTypes
        }

        assertTrue(hasMiuixBridge)
    }

}
