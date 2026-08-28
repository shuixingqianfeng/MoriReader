package io.github.shuixingqianfeng.morireader.reader

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.ReaderPreferences
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ReaderWebViewInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ReaderTestActivity>()

    @Test
    fun opensEpubWithNestedTocAtFirstReadableChapter() {
        val fixture = File(composeRule.activity.filesDir, "nested-toc-${System.nanoTime()}.epub")
        createFixture(fixture)
        val terminal = CountDownLatch(1)
        val relocatedToChapter = CountDownLatch(1)
        val error = AtomicReference<String?>(null)
        val lastStage = AtomicReference("not-started")
        val controller = ReaderController()
        val book = BookEntity(
            id = "reader-fixture",
            epubIdentifier = "reader-fixture",
            title = "Reader fixture",
            author = "MoriReader",
            description = "",
            coverPath = null,
            filePath = fixture.absolutePath,
            readingDirection = "ltr",
            importedAt = System.currentTimeMillis(),
        )

        composeRule.setContent {
            ReaderWebView(
                book = book,
                preferences = ReaderPreferences(),
                controller = controller,
                modifier = Modifier.fillMaxSize().testTag("reader_webview"),
            ) { event ->
                when (event) {
                    ReaderEvent.Opened -> terminal.countDown()
                    is ReaderEvent.Stage -> lastStage.set(event.name)
                    is ReaderEvent.Error -> {
                        error.set(event.message)
                        terminal.countDown()
                    }
                    is ReaderEvent.Relocated -> if (event.location.chapterIndex == 1) relocatedToChapter.countDown()
                    else -> Unit
                }
            }
        }

        try {
            composeRule.waitUntil(timeoutMillis = 20_000) { terminal.count == 0L }
        } catch (_: Throwable) {
            fail("Reader did not finish opening; last stage=${lastStage.get()}")
        }
        assertNull("Reader failed to open: ${error.get()}", error.get())
        try {
            composeRule.waitUntil(timeoutMillis = 10_000) { relocatedToChapter.count == 0L }
        } catch (_: Throwable) {
            fail("Reader did not initially open the first readable chapter; last stage=${lastStage.get()}")
        }
        lastStage.set("gesture-start")
        repeat(4) {
            composeRule.onNodeWithTag("reader_webview").performTouchInput {
                swipe(
                    start = Offset(width * 0.86f, height * 0.5f),
                    end = Offset(width * 0.14f, height * 0.5f),
                    durationMillis = 55,
                )
            }
        }
        SystemClock.sleep(450)
        lastStage.set("reverse-gesture-start")
        repeat(4) {
            composeRule.onNodeWithTag("reader_webview").performTouchInput {
                swipe(
                    start = Offset(width * 0.14f, height * 0.5f),
                    end = Offset(width * 0.86f, height * 0.5f),
                    durationMillis = 55,
                )
            }
        }
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) { lastStage.get().startsWith("page:aligned:") }
        } catch (_: Throwable) {
            fail("Reader did not snap to a whole page after rapid swipes; last stage=${lastStage.get()}")
        }
        composeRule.waitForIdle()
        SystemClock.sleep(250)
    }

    private fun createFixture(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun entry(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
            entry("mimetype", "application/epub+zip")
            entry(
                "META-INF/container.xml",
                """<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container" version="1.0"><rootfiles><rootfile full-path="EPUB/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""",
            )
            entry(
                "EPUB/content.opf",
                """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">reader-fixture</dc:identifier><dc:title>Reader fixture</dc:title><dc:language>zh</dc:language></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="intro" href="intro.xhtml" media-type="application/xhtml+xml"/><item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="intro"/><itemref idref="chapter"/></spine></package>""",
            )
            entry(
                "EPUB/nav.xhtml",
                """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><span>第一卷</span><ol><li><a href="chapter.xhtml">第一章</a></li></ol></li></ol></nav></body></html>""",
            )
            entry(
                "EPUB/intro.xhtml",
                """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>扉页</title></head><body><p>扉页</p></body></html>""",
            )
            entry(
                "EPUB/chapter.xhtml",
                """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>第一章</title></head><body>${
                    (1..90).joinToString("") { index ->
                        "<p>无版权测试正文第${index}段，用于验证快速连续翻页后仍然完整对齐并保持可见。</p>"
                    }
                }</body></html>""",
            )
        }
    }
}
