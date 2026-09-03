package io.github.shuixingqianfeng.morireader

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileOutputStream
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
    fun navigationSupportsTapAndContinuousDrag() {
        composeRule.onNodeWithTag("tab_TAGS").performClick()
        composeRule.onNodeWithTag("tab_TAGS").assertIsSelected()

        composeRule.onNodeWithTag("bottom_navigation").performTouchInput {
            swipe(
                start = Offset(width * 0.12f, height * 0.5f),
                end = Offset(width * 0.90f, height * 0.5f),
                durationMillis = 420,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab_SEARCH").assertIsSelected()
    }

    @Test
    fun capturesRenderedLiquidGlassNavigation() {
        composeRule.waitForIdle()
        val bitmap = composeRule.onNodeWithTag("bottom_navigation")
            .captureToImage()
            .asAndroidBitmap()
        val output = File(composeRule.activity.filesDir, "liquid-navigation.png")
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

}
