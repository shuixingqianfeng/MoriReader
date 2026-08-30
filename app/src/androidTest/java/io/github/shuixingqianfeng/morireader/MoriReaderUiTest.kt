package io.github.shuixingqianfeng.morireader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

}
