package io.github.shuixingqianfeng.morireader

import android.graphics.Color
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
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

        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val darkPixelCount = countSampledDarkPixels(screenshot)
        screenshot.recycle()
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
                durationMillis = 450,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab_SEARCH").assertIsSelected()
        composeRule.onNodeWithText("搜索", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun countSampledDarkPixels(bitmap: Bitmap): Int {
        val row = IntArray(bitmap.width)
        var count = 0
        for (y in bitmap.height / 10 until bitmap.height * 9 / 10 step 6) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in row.indices step 6) {
                val pixel = row[x]
                if (Color.red(pixel) < 192 && Color.green(pixel) < 192 && Color.blue(pixel) < 192) count++
            }
        }
        return count
    }
}
