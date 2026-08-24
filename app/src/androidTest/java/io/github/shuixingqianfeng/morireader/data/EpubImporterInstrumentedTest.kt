package io.github.shuixingqianfeng.morireader.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class EpubImporterInstrumentedTest {
    @Test
    fun parsesMetadataAndCopiesToPrivateStorage() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val source = File(context.cacheDir, "fixture-${System.nanoTime()}.epub")
            createFixture(source)
            val importer = EpubImporter(context)
            val hash = importer.hash(Uri.fromFile(source))
            val parsed = importer.copyAndParse(Uri.fromFile(source), hash)

            assertEquals("双 BR 测试书", parsed.title)
            assertEquals("MoriReader 测试", parsed.author)
            assertEquals("fixture-double-br", parsed.identifier)
            assertEquals("ltr", parsed.readingDirection)
            assertTrue(parsed.file.isFile)

            parsed.file.parentFile?.deleteRecursively()
            source.delete()
        }
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
                """<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container" version="1.0"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""",
            )
            entry(
                "OEBPS/content.opf",
                """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier>fixture-double-br</dc:identifier><dc:title>双 BR 测试书</dc:title><dc:creator>MoriReader 测试</dc:creator><dc:description>无版权合成测试内容</dc:description></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/></manifest><spine page-progression-direction="ltr"><itemref idref="chapter"/></spine></package>""",
            )
            entry(
                "OEBPS/nav.xhtml",
                """<html xmlns="http://www.w3.org/1999/xhtml"><body><nav epub:type="toc" xmlns:epub="http://www.idpf.org/2007/ops"><ol><li><a href="chapter.xhtml">第一章</a></li></ol></nav></body></html>""",
            )
            entry(
                "OEBPS/chapter.xhtml",
                """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>第一章</title></head><body>第一段。<br/><br/>第二段。<br/>单换行。<br/><br/>第三段。</body></html>""",
            )
        }
    }
}
