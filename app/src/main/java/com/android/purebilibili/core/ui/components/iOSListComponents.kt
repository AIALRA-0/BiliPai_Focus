package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalDynamicColorActive
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.ui.common.copyOnLongPress
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ═══════════════════════════════════════════════════
//  Common iOS List Components (Reused across Settings, Profile, etc.)
// ═══════════════════════════════════════════════════

internal data class AdaptiveListComponentVisualSpec(
    val sectionStartPaddingDp: Int,
    val groupCornerRadiusDp: Int,
    val groupTonalElevationDp: Int,
    val iconCornerRadiusDp: Int,
    val iconContainerSizeDp: Int,
    val iconGlyphSizeDp: Int,
    val iconBackgroundAlpha: Float,
    val gridCornerRadiusDp: Int,
    val searchBarCornerRadiusDp: Int,
    val searchBarHeightDp: Int,
    val dividerThicknessDp: Float,
    val dividerStartIndentDp: Int
)

internal data class AdaptiveSwitchVisualSpec(
    val usePlatformDefaults: Boolean,
    val checkedThumbColor: Color,
    val checkedTrackColor: Color,
    val uncheckedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedBorderColor: Color
)

internal data class AdaptiveListRowVisualSpec(
    val insideHorizontalPaddingDp: Int,
    val insideVerticalPaddingDp: Int,
    val trailingIconSizeDp: Int,
    val trailingSpacingDp: Int
)

internal fun resolveAdaptiveListComponentVisualSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AdaptiveListComponentVisualSpec {
    return if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
        AdaptiveListComponentVisualSpec(
            sectionStartPaddingDp = 18,
            groupCornerRadiusDp = 20,
            groupTonalElevationDp = 0,
            iconCornerRadiusDp = 10,
            iconContainerSizeDp = 38,
            iconGlyphSizeDp = 20,
            iconBackgroundAlpha = 0.12f,
            gridCornerRadiusDp = 20,
            searchBarCornerRadiusDp = 22,
            searchBarHeightDp = 52,
            dividerThicknessDp = 0f,
            dividerStartIndentDp = 18
        )
    } else if (uiPreset == UiPreset.MD3) {
        AdaptiveListComponentVisualSpec(
            sectionStartPaddingDp = 20,
            groupCornerRadiusDp = 24,
            groupTonalElevationDp = 3,
            iconCornerRadiusDp = 12,
            iconContainerSizeDp = 40,
            iconGlyphSizeDp = 22,
            iconBackgroundAlpha = 0.14f,
            gridCornerRadiusDp = 24,
            searchBarCornerRadiusDp = 28,
            searchBarHeightDp = 56,
            dividerThicknessDp = 0f,
            dividerStartIndentDp = 20
        )
    } else {
        AdaptiveListComponentVisualSpec(
            sectionStartPaddingDp = 32,
            groupCornerRadiusDp = 20,
            groupTonalElevationDp = 1,
            iconCornerRadiusDp = 10,
            iconContainerSizeDp = 36,
            iconGlyphSizeDp = 20,
            iconBackgroundAlpha = 0.12f,
            gridCornerRadiusDp = 20,
            searchBarCornerRadiusDp = 10,
            searchBarHeightDp = 40,
            dividerThicknessDp = 0.5f,
            dividerStartIndentDp = 66
        )
    }
}

internal fun resolveAdaptiveListRowVisualSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AdaptiveListRowVisualSpec {
    return if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
        AdaptiveListRowVisualSpec(
            insideHorizontalPaddingDp = 16,
            insideVerticalPaddingDp = 14,
            trailingIconSizeDp = 14,
            trailingSpacingDp = 6
        )
    } else if (uiPreset == UiPreset.MD3) {
        AdaptiveListRowVisualSpec(
            insideHorizontalPaddingDp = 18,
            insideVerticalPaddingDp = 16,
            trailingIconSizeDp = 16,
            trailingSpacingDp = 8
        )
    } else {
        AdaptiveListRowVisualSpec(
            insideHorizontalPaddingDp = 16,
            insideVerticalPaddingDp = 14,
            trailingIconSizeDp = 20,
            trailingSpacingDp = 6
        )
    }
}

internal fun resolveAdaptiveGroupContainerColor(
    uiPreset: UiPreset,
    colorScheme: ColorScheme,
    fallbackColor: Color,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): Color {
    return if (uiPreset == UiPreset.MD3) {
        if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
            colorScheme.surfaceContainer
        } else {
            colorScheme.surfaceContainerLow
        }
    } else {
        fallbackColor
    }
}

internal fun resolveAdaptiveSearchBarContainerColor(
    uiPreset: UiPreset,
    colorScheme: ColorScheme,
    fallbackColor: Color,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): Color {
    return if (uiPreset == UiPreset.MD3) {
        if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
            colorScheme.surfaceContainer
        } else {
            colorScheme.surfaceContainerHigh
        }
    } else {
        fallbackColor
    }
}

internal fun shouldUseNativeMiuixSearchBar(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Boolean = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX

private val miuixAdaptiveSearchShape = RoundedCornerShape(percent = 50)
private val miuixAdaptiveSearchLeadingStartPadding = 16.dp
private val miuixAdaptiveSearchLeadingEndPadding = 8.dp
private val miuixAdaptiveSearchTrailingStartPadding = 8.dp
private val miuixAdaptiveSearchTrailingEndPadding = 16.dp
private val miuixAdaptiveSearchMinHeight = 45.dp

internal fun resolveAdaptiveSemanticIconTint(
    iconTint: Color,
    uiPreset: UiPreset,
    colorScheme: ColorScheme,
    useSemanticAccentRoles: Boolean = true
): Color {
    if (uiPreset != UiPreset.MD3 || iconTint == Color.Unspecified) {
        return iconTint
    }
    val unifiedAccent = colorScheme.primary
    return when (iconTint) {
        iOSGreen -> unifiedAccent
        iOSBlue, iOSTeal -> if (useSemanticAccentRoles) colorScheme.secondary else unifiedAccent
        iOSPurple, iOSPink, iOSOrange, iOSYellow -> if (useSemanticAccentRoles) colorScheme.tertiary else unifiedAccent
        iOSRed -> colorScheme.error
        iOSSystemGray -> colorScheme.onSurfaceVariant
        else -> iconTint
    }
}

internal fun resolveAdaptiveSwitchVisualSpec(
    uiPreset: UiPreset,
    colorScheme: ColorScheme
): AdaptiveSwitchVisualSpec {
    return if (uiPreset == UiPreset.MD3) {
        AdaptiveSwitchVisualSpec(
            usePlatformDefaults = true,
            checkedThumbColor = colorScheme.onPrimary,
            checkedTrackColor = colorScheme.primary,
            uncheckedThumbColor = colorScheme.surface,
            uncheckedTrackColor = colorScheme.surfaceVariant,
            uncheckedBorderColor = colorScheme.outline.copy(alpha = 0.55f)
        )
    } else {
        AdaptiveSwitchVisualSpec(
            usePlatformDefaults = false,
            checkedThumbColor = Color.White,
            checkedTrackColor = colorScheme.primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFE9E9EA),
            uncheckedBorderColor = Color.Transparent
        )
    }
}

@Composable
internal fun rememberAdaptiveSemanticIconTint(
    iconTint: Color,
    uiPreset: UiPreset = LocalUiPreset.current,
    dynamicColorActive: Boolean = LocalDynamicColorActive.current
): Color {
    val colorScheme = MaterialTheme.colorScheme
    return remember(iconTint, uiPreset, dynamicColorActive, colorScheme) {
        resolveAdaptiveSemanticIconTint(
            iconTint = iconTint,
            uiPreset = uiPreset,
            colorScheme = colorScheme,
            useSemanticAccentRoles = dynamicColorActive
        )
    }
}

@Composable
fun AppAdaptiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val uiPreset = LocalUiPreset.current
    val colorScheme = MaterialTheme.colorScheme
    val switchSpec = remember(uiPreset, colorScheme) {
        resolveAdaptiveSwitchVisualSpec(
            uiPreset = uiPreset,
            colorScheme = colorScheme
        )
    }
    if (uiPreset == UiPreset.MD3) {
        if (switchSpec.usePlatformDefaults) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = modifier
            )
        } else {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = modifier,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = switchSpec.checkedThumbColor,
                    checkedTrackColor = switchSpec.checkedTrackColor,
                    checkedBorderColor = switchSpec.checkedTrackColor,
                    uncheckedThumbColor = switchSpec.uncheckedThumbColor,
                    uncheckedTrackColor = switchSpec.uncheckedTrackColor,
                    uncheckedBorderColor = switchSpec.uncheckedBorderColor
                )
            )
        }
    } else {
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = modifier,
            colors = CupertinoSwitchDefaults.colors(
                thumbColor = switchSpec.checkedThumbColor,
                checkedTrackColor = switchSpec.checkedTrackColor,
                uncheckedTrackColor = switchSpec.uncheckedTrackColor
            )
        )
    }
}

@Composable
fun IOSSectionTitle(title: String) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val visualSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListComponentVisualSpec(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant
        )
    }
    Text(
        text = if (uiPreset == UiPreset.MD3) title else title.uppercase(),
        style = if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
            MaterialTheme.typography.labelLarge
        } else if (uiPreset == UiPreset.MD3) {
            MaterialTheme.typography.titleSmall
        } else {
            MaterialTheme.typography.labelMedium
        },
        color = if (uiPreset == UiPreset.MD3) {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        letterSpacing = if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
            0.15.sp
        } else if (uiPreset == UiPreset.MD3) {
            0.sp
        } else {
            0.5.sp
        },
        modifier = Modifier.padding(
            start = if (uiPreset == UiPreset.MD3) visualSpec.sectionStartPaddingDp.dp else visualSpec.sectionStartPaddingDp.dp,
            top = if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) 24.dp else if (uiPreset == UiPreset.MD3) 28.dp else 24.dp,
            bottom = if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) 8.dp else if (uiPreset == UiPreset.MD3) 10.dp else 8.dp
        )
    )
}

@Composable
fun IOSGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: androidx.compose.ui.graphics.Shape? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val visualSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListComponentVisualSpec(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant
        )
    }
    val groupCornerRadius = iOSCornerRadius.Medium * cornerRadiusScale
    val colorScheme = MaterialTheme.colorScheme
    val appliedShape = shape ?: RoundedCornerShape(
        if (uiPreset == UiPreset.MD3) visualSpec.groupCornerRadiusDp.dp else groupCornerRadius
    )
    val resolvedContainerColor = resolveAdaptiveGroupContainerColor(
        uiPreset = uiPreset,
        colorScheme = colorScheme,
        fallbackColor = containerColor,
        androidNativeVariant = androidNativeVariant
    )
    
    Surface(
        modifier = modifier
            .padding(
                horizontal = if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
                    14.dp
                } else if (uiPreset == UiPreset.MD3) {
                    12.dp
                } else {
                    16.dp
                }
            )
            .clip(appliedShape),
        color = resolvedContainerColor,
        shadowElevation = if (uiPreset == UiPreset.MD3) 0.dp else 0.dp,
        tonalElevation = if (uiPreset == UiPreset.MD3) 0.dp else visualSpec.groupTonalElevationDp.dp,
        border = if (uiPreset == UiPreset.MD3) {
            androidx.compose.foundation.BorderStroke(
                0.8.dp,
                if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
                    colorScheme.outline.copy(alpha = 0.22f)
                } else {
                    colorScheme.outlineVariant.copy(alpha = 0.6f)
                }
            )
        } else {
            border
        }
    ) {
        Column(content = content)
    }
}

@Composable
fun IOSSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val visualSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListComponentVisualSpec(uiPreset, androidNativeVariant)
    }
    val rowSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListRowVisualSpec(uiPreset, androidNativeVariant)
    }
    val effectiveIconTint = rememberAdaptiveSemanticIconTint(iconTint, uiPreset)
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val iconCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.iconCornerRadiusDp.dp else iOSCornerRadius.Small * cornerRadiusScale
    if (uiPreset == UiPreset.MD3) {
        BasicComponent(
            title = title,
            summary = subtitle,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) },
            insideMargin = PaddingValues(
                horizontal = rowSpec.insideHorizontalPaddingDp.dp,
                vertical = rowSpec.insideVerticalPaddingDp.dp
            ),
            startAction = {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(visualSpec.iconContainerSizeDp.dp)
                            .clip(RoundedCornerShape(visualSpec.iconCornerRadiusDp.dp))
                            .background(effectiveIconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = effectiveIconTint,
                            modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                        )
                    }
                }
            },
            endActions = {
                AppAdaptiveSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            }
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(visualSpec.iconContainerSizeDp.dp)
                    .clip(RoundedCornerShape(iconCornerRadius))
                    .background(effectiveIconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = effectiveIconTint,
                    modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
        }
        AppAdaptiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun IOSClickableItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    copyValue: String? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevronTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    centered: Boolean = false,
    enableCopy: Boolean = false,
    showChevron: Boolean = true
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val visualSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListComponentVisualSpec(uiPreset, androidNativeVariant)
    }
    val rowSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListRowVisualSpec(uiPreset, androidNativeVariant)
    }
    val effectiveIconTint = rememberAdaptiveSemanticIconTint(iconTint, uiPreset)
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val iconCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.iconCornerRadiusDp.dp else iOSCornerRadius.Small * cornerRadiusScale
    if (uiPreset == UiPreset.MD3) {
        BasicComponent(
            title = title,
            summary = subtitle,
            onClick = onClick,
            insideMargin = PaddingValues(
                horizontal = rowSpec.insideHorizontalPaddingDp.dp,
                vertical = rowSpec.insideVerticalPaddingDp.dp
            ),
            startAction = {
                when {
                    icon != null -> {
                        Box(
                            modifier = Modifier
                                .size(visualSpec.iconContainerSizeDp.dp)
                                .clip(RoundedCornerShape(visualSpec.iconCornerRadiusDp.dp))
                                .background(effectiveIconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = effectiveIconTint,
                                modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                            )
                        }
                    }

                    iconPainter != null -> {
                        Box(
                            modifier = Modifier
                                .size(visualSpec.iconContainerSizeDp.dp)
                                .clip(RoundedCornerShape(visualSpec.iconCornerRadiusDp.dp))
                                .background(
                                    if (effectiveIconTint == Color.Unspecified) {
                                        Color.Transparent
                                    } else {
                                        effectiveIconTint.copy(alpha = visualSpec.iconBackgroundAlpha)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = effectiveIconTint,
                                modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                            )
                        }
                    }
                }
            },
            endActions = {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        style = if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = if (enableCopy) {
                            Modifier.copyOnLongPress(copyValue ?: value, title)
                        } else {
                            Modifier
                        }
                    )
                    Spacer(modifier = Modifier.width(rowSpec.trailingSpacingDp.dp))
                }
                if (onClick != null && showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(rowSpec.trailingIconSizeDp.dp)
                    )
                }
            }
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
    ) {
        if (!centered) {
            if (icon != null || iconPainter != null) {
                if (effectiveIconTint != Color.Unspecified) {
                    Box(
                        modifier = Modifier
                            .size(visualSpec.iconContainerSizeDp.dp)
                            .clip(RoundedCornerShape(iconCornerRadius))
                            .background(effectiveIconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = effectiveIconTint,
                                modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                            )
                        } else if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = effectiveIconTint,
                                modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.size(visualSpec.iconContainerSizeDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(visualSpec.iconContainerSizeDp.dp)
                            )
                        } else if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(visualSpec.iconContainerSizeDp.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        
        if (centered) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    maxLines = if (subtitle != null) 2 else 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        if (!centered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = valueColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = if (enableCopy) {
                            Modifier.copyOnLongPress(copyValue ?: value, title)
                        } else {
                            Modifier
                        }
                    )
                }
                if (onClick != null && showChevron) {
                    Spacer(modifier = Modifier.width(6.dp))
                    if (uiPreset == UiPreset.MD3) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = chevronTint,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(CupertinoIcons.Default.ChevronForward, null, tint = chevronTint, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IOSDivider(
    modifier: Modifier = Modifier,
    startIndent: androidx.compose.ui.unit.Dp = 66.dp
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    if (visualSpec.dividerThicknessDp <= 0f) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (uiPreset == UiPreset.MD3) visualSpec.dividerStartIndentDp.dp else startIndent)
            .height(visualSpec.dividerThicknessDp.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // Subtle separator
    )
}


@Composable
fun IOSGridItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    val effectiveIconTint = rememberAdaptiveSemanticIconTint(iconTint, uiPreset)
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val itemCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.gridCornerRadiusDp.dp else iOSCornerRadius.Medium * cornerRadiusScale

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(itemCornerRadius))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(iOSCornerRadius.Small * cornerRadiusScale))
                .background(effectiveIconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = effectiveIconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    heightOverride: Dp? = null
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val colorScheme = MaterialTheme.colorScheme
    val visualSpec = remember(uiPreset, androidNativeVariant) {
        resolveAdaptiveListComponentVisualSpec(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant
        )
    }
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val searchBarCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.searchBarCornerRadiusDp.dp else iOSCornerRadius.Small * cornerRadiusScale
    val resolvedContainerColor = resolveAdaptiveSearchBarContainerColor(
        uiPreset = uiPreset,
        colorScheme = colorScheme,
        fallbackColor = containerColor,
        androidNativeVariant = androidNativeVariant
    )
    val resolvedHeight = heightOverride ?: visualSpec.searchBarHeightDp.dp

    if (shouldUseNativeMiuixSearchBar(uiPreset, androidNativeVariant)) {
        MiuixAdaptiveSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = modifier,
            placeholder = placeholder,
            containerColor = resolvedContainerColor,
            height = resolvedHeight
        )
        return
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(resolvedHeight)
            .clip(RoundedCornerShape(searchBarCornerRadius))
            .background(resolvedContainerColor),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                if (uiPreset == UiPreset.MD3) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = CupertinoIcons.Default.MagnifyingGlass,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        val clearIcon = if (uiPreset == UiPreset.MD3) {
                            Icons.Default.Clear
                        } else {
                            CupertinoIcons.Default.XmarkCircle
                        }
                        Icon(
                            imageVector = clearIcon,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun MiuixAdaptiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    containerColor: Color,
    height: androidx.compose.ui.unit.Dp
) {
    val capsuleShape = miuixAdaptiveSearchShape
    val inputTextStyle = MiuixTheme.textStyles.main
        .copy(fontWeight = FontWeight.Medium)
        .merge(MaterialTheme.typography.bodyMedium)
        .copy(color = MiuixTheme.colorScheme.onSurface)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(capsuleShape)
            .background(containerColor),
        singleLine = true,
        textStyle = inputTextStyle,
        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(
                        start = miuixAdaptiveSearchLeadingStartPadding,
                        end = miuixAdaptiveSearchLeadingEndPadding
                    ),
                    imageVector = Icons.Default.Search,
                    tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                    contentDescription = null
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = miuixAdaptiveSearchMinHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiuixTheme.colorScheme.onSurfaceContainerHigh
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier.padding(
                            start = miuixAdaptiveSearchTrailingStartPadding,
                            end = miuixAdaptiveSearchTrailingEndPadding
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .clip(capsuleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onQueryChange("") },
                            imageVector = Icons.Default.Clear,
                            tint = MiuixTheme.colorScheme.onSurfaceContainerHighest,
                            contentDescription = "Clear"
                        )
                    }
                }
            }
        }
    )
}
