package com.Android.purebilibili.feature.dynamic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.imageLoader
import com.android.purebilibili.feature.dynamic.components.ZOOMABLE_IMAGE_TAG
import com.android.purebilibili.feature.dynamic.components.ZoomableImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZoomableImageUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun previewImage_usesContainerBoundsInsteadOfIntrinsicThumbnailSize() {
        lateinit var imageLoader: ImageLoader

        composeTestRule.setContent {
            MaterialTheme {
                imageLoader = LocalContext.current.imageLoader
                Box(
                    modifier = Modifier.size(width = 390.dp, height = 844.dp)
                ) {
                    ZoomableImage(
                        model = android.R.drawable.ic_menu_gallery,
                        imageLoader = imageLoader
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(ZOOMABLE_IMAGE_TAG).assertIsDisplayed()

        val bounds = composeTestRule
            .onNodeWithTag(ZOOMABLE_IMAGE_TAG)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(bounds.width > 300f)
        assertTrue(bounds.height > 700f)
    }
}
