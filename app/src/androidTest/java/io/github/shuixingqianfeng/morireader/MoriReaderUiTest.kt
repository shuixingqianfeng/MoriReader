package io.github.shuixingqianfeng.morireader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class MoriReaderUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyLibraryShowsImportAndNavigation() {
        composeRule.onNodeWithTag("library_screen_title").assertIsDisplayed()
        composeRule.onNodeWithText("书架还是空的").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("导入 EPUB").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_LIBRARY").assertIsSelected()
    }

    @Test
    fun appShellPaintsVisiblePixels() {
        composeRule.onNodeWithTag("library_screen_title").assertIsDisplayed()
        composeRule.waitForIdle()

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val darkPixelCount = countSampledDarkPixels(pixels)
        if (darkPixelCount < 20) {
            fail("App semantics were present but the OEM-compatible shell painted blank; dark pixels=$darkPixelCount")
        }
    }

    @Test
    fun bottomNavigationTracksDragIntoSearchOrb() {
        val node = composeRule.onNodeWithTag("bottom_navigation")
        node.performTouchInput {
            swipe(
                start = Offset(width * 0.10f, height * 0.5f),
                end = Offset(width * 0.94f, height * 0.5f),
                durationMillis = 220,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab_SEARCH").assertIsSelected()
        composeRule.onNodeWithText("搜索", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun countSampledDarkPixels(pixels: PixelMap): Int {
        var count = 0
        for (y in pixels.height / 10 until pixels.height * 9 / 10 step 6) {
            for (x in 0 until pixels.width step 6) {
                val pixel = pixels[x, y]
                if (pixel.red < 0.75f && pixel.green < 0.75f && pixel.blue < 0.75f) count++
            }
        }
        return count
    }
}
