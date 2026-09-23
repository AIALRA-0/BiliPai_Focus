package com.android.purebilibili.feature.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.skeleton.ContentSkeletonBlock
import com.android.purebilibili.core.ui.skeleton.rememberContentSkeletonBlockColor
import com.android.purebilibili.core.ui.skeleton.rememberContentSkeletonPulse
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.feature.home.resolveHomeFeedCardLayout
import kotlin.math.roundToInt

/** UP 空间首屏骨架：资料头、主标签和投稿网格均与真实 SpaceContent 同构。 */
@Composable
internal fun SpaceLoadingSkeleton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by SettingsManager.getHomeSettings(context)
        .collectAsStateWithLifecycle(
            initialValue = HomeSettings(androidNativeLiquidGlassEnabled = false)
        )
    val windowSizeClass = LocalWindowSizeClass.current
    val windowWidthDp = windowSizeClass.widthDp.value.roundToInt().coerceAtLeast(0)
    val adaptiveLayoutSpec = remember(windowWidthDp, windowSizeClass.widthSizeClass) {
        resolveSpaceAdaptiveLayoutSpec(
            widthDp = windowWidthDp,
            widthSizeClass = windowSizeClass.widthSizeClass,
        )
    }
    val columns = resolveSpaceContentGridColumnCount(
        widthDp = windowWidthDp,
        fixedColumnCount = settings.gridColumnCount,
        cardWidthPreset = settings.homeFeedCardWidthPreset,
        contentMaxWidthDp = adaptiveLayoutSpec.contentMaxWidthDp,
        widthSizeClass = windowSizeClass.widthSizeClass,
    )
    val cardLayout = resolveHomeFeedCardLayout(
        style = settings.homeFeedCardStyle,
        gridColumns = columns,
        widthSizeClass = windowSizeClass.widthSizeClass,
    )
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .responsiveContentWidth(maxWidth = adaptiveLayoutSpec.contentMaxWidthDp.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = cardLayout.outerPaddingDp.dp,
            vertical = 8.dp,
        ),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(cardLayout.itemSpacingDp.dp),
        verticalArrangement = Arrangement.spacedBy(cardLayout.verticalItemSpacingDp.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            val bannerMetrics = remember(
                windowWidthDp,
                windowSizeClass.widthDp,
                windowSizeClass.heightDp,
            ) {
                resolveSpaceBannerMetrics(
                    renderedBannerWidthDp = windowWidthDp.toFloat(),
                    windowWidthDp = windowSizeClass.widthDp.value,
                    windowHeightDp = windowSizeClass.heightDp.value,
                )
            }
            Column(Modifier.fillMaxWidth()) {
                ContentSkeletonBlock(
                    blockColor,
                    Modifier.fillMaxWidth().height(bannerMetrics.heightDp.dp),
                    AppShapes.container(ContainerLevel.Card),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContentSkeletonBlock(blockColor, Modifier.size(76.dp), CircleShape)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.36f).height(20.dp))
                        ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.62f).height(13.dp))
                        ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.48f).height(13.dp))
                    }
                    if (adaptiveLayoutSpec.useExpandedHeader) {
                        Spacer(Modifier.width(24.dp))
                        Column(
                            modifier = Modifier
                                .weight(0.8f, fill = false)
                                .widthIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(3) {
                                    ContentSkeletonBlock(
                                        blockColor,
                                        Modifier.weight(1f).height(24.dp),
                                    )
                                }
                            }
                            ContentSkeletonBlock(
                                blockColor,
                                Modifier.fillMaxWidth().height(40.dp),
                                AppShapes.container(ContainerLevel.Pill),
                            )
                        }
                    } else {
                        ContentSkeletonBlock(
                            blockColor,
                            Modifier.width(88.dp).height(40.dp),
                            AppShapes.container(ContainerLevel.Pill),
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ContentSkeletonBlock(
                blockColor,
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                AppShapes.container(ContainerLevel.Pill),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ContentSkeletonBlock(blockColor, Modifier.width(92.dp).height(20.dp))
        }
        items(List(columns * 4) { it }) {
            Column(Modifier.fillMaxWidth()) {
                ContentSkeletonBlock(
                    blockColor,
                    Modifier.fillMaxWidth().aspectRatio(cardLayout.coverAspectRatio),
                    AppShapes.container(ContainerLevel.Card),
                )
                Spacer(Modifier.height(8.dp))
                ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.9f).height(15.dp))
                Spacer(Modifier.height(6.dp))
                ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.52f).height(12.dp))
            }
        }
    }
}
