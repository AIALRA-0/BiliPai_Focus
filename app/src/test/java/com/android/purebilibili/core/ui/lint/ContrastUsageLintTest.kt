package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal val DirectTranslucentSemanticContainerPattern = Regex(
    """(?:\bcolor\s*=|\.background\()\s*(?:MaterialTheme\.)?colorScheme\.""" +
        """(?:primary|secondary|tertiary|error)Container\.copy\(alpha\s*=\s*[^)]+\)""",
    RegexOption.DOT_MATCHES_ALL,
)

internal val CustomMaterialSwitchColorsPattern = Regex(
    """SwitchDefaults\.colors\s*\(""",
)

class ContrastUsageLintTest {

    @Test
    fun feature_surfaces_do_not_use_translucent_semantic_containers_directly() {
        val offenders = StyleLintSupport.findOffenders(
            pattern = DirectTranslucentSemanticContainerPattern,
            allowlist = emptySet(),
        )

        assertTrue(
            offenders.isEmpty(),
            "Flatten translucent semantic containers with the shared contrast policy.\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun feature_switches_use_global_dynamic_roles_unless_contrast_tested() {
        val offenders = StyleLintSupport.findOffenders(
            pattern = CustomMaterialSwitchColorsPattern,
            allowlist = emptySet(),
        )

        assertTrue(
            offenders.isEmpty(),
            "Feature switches must use the global AppSwitch dynamic roles.\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun scanners_match_only_the_guarded_usage_shapes() {
        assertTrue(
            DirectTranslucentSemanticContainerPattern.containsMatchIn(
                "color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)"
            )
        )
        assertTrue(
            DirectTranslucentSemanticContainerPattern.containsMatchIn(
                ".background(colorScheme.tertiaryContainer.copy(alpha = 0.6f))"
            )
        )
        assertFalse(
            DirectTranslucentSemanticContainerPattern.containsMatchIn(
                "containerColor = colorScheme.primaryContainer.copy(alpha = 0.4f)"
            )
        )
        assertTrue(CustomMaterialSwitchColorsPattern.containsMatchIn("SwitchDefaults.colors()"))
        assertFalse(CustomMaterialSwitchColorsPattern.containsMatchIn("SwitchDefaults.IconSize"))
    }
}
