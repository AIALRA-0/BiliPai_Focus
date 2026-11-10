package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled

/** Kyant interactive buttons use their light instead of a second ripple indication. */
private object ElasticLightIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = LightOnlyNode()
    override fun equals(other: Any?) = other === this
    override fun hashCode() = javaClass.hashCode()
}

private class LightOnlyNode : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() = drawContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvideAppElasticFeedback(enabled: Boolean = true, content: @Composable () -> Unit) {
    if (enabled && LocalComponentMotionEnabled.current) {
        CompositionLocalProvider(
            LocalIndication provides ElasticLightIndication,
            LocalRippleConfiguration provides null,
            content = content,
        )
    } else {
        content()
    }
}
