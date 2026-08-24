package io.github.shuixingqianfeng.morireader.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

data class ParsedEpub(
    val hash: String,
    val identifier: String?,
    val title: String,
    val author: String,
    val description: String,
    val readingDirection: String,
    val file: File,
    val cover: File?,
)

class EpubImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class EpubImporter(private val context: Context) {
    suspend fun hash(uri: Uri): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        } ?: throw EpubImportException("无法读取所选文件")
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun copyAndParse(uri: Uri, hash: String): ParsedEpub = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "books/$hash")
        if (!directory.exists() && !directory.mkdirs()) {
            throw EpubImportException("无法创建书籍存储目录")
        }
        val epubFile = File(directory, "book.epub")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(epubFile).use { output -> input.copyTo(output) }
            } ?: throw EpubImportException("无法读取所选文件")
            parse(epubFile, directory, hash)
        } catch (error: Throwable) {
            directory.deleteRecursively()
            if (error is EpubImportException) throw error
            throw EpubImportException("EPUB 解析失败：${error.message ?: "文件格式无效"}", error)
        }
    }

    private fun parse(file: File, directory: File, hash: String): ParsedEpub {
        ZipFile(file).use { zip ->
            val mimetype = zip.getEntry("mimetype")?.let { entry ->
                zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText().trim() }
            }
            if (mimetype != null && mimetype != "application/epub+zip") {
                throw EpubImportException("所选文件不是有效的 EPUB")
            }
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: throw EpubImportException("EPUB 缺少 META-INF/container.xml")
            val opfPath = zip.getInputStream(containerEntry).use(::findRootFile)
                ?: throw EpubImportException("EPUB 未声明内容清单")
            val opfEntry = zip.getEntry(opfPath) ?: throw EpubImportException("EPUB 内容清单不存在")
            val metadata = zip.getInputStream(opfEntry).use(::parsePackage)

            val opfDirectory = opfPath.substringBeforeLast('/', "")
            val coverHref = metadata.coverHref
            val coverFile = coverHref?.let { href ->
                val normalized = normalizePath(opfDirectory, href)
                val entry = zip.getEntry(normalized) ?: return@let null
                if (entry.size > 20L * 1024 * 1024) return@let null
                val extension = normalized.substringAfterLast('.', "jpg").take(5)
                File(directory, "cover.$extension").also { target ->
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                }
            }

            return ParsedEpub(
                hash = hash,
                identifier = metadata.identifier?.takeIf(String::isNotBlank),
                title = metadata.title?.takeIf(String::isNotBlank) ?: file.nameWithoutExtension,
                author = metadata.author?.takeIf(String::isNotBlank) ?: "未知作者",
                description = metadata.description.orEmpty().trim(),
                readingDirection = metadata.direction ?: "ltr",
                file = file,
                cover = coverFile,
            )
        }
    }

    private fun findRootFile(parserInput: java.io.InputStream): String? {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(parserInput, null)
        }
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
            }
            parser.next()
        }
        return null
    }

    private data class PackageMetadata(
        var title: String? = null,
        var author: String? = null,
        var identifier: String? = null,
        var description: String? = null,
        var direction: String? = null,
        var coverHref: String? = null,
    )

    private fun parsePackage(parserInput: java.io.InputStream): PackageMetadata {
        val result = PackageMetadata()
        val manifest = mutableMapOf<String, Pair<String, String>>()
        var legacyCoverId: String? = null
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(parserInput, null)
        }
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name.lowercase()) {
                    "title" -> if (result.title == null) result.title = parser.nextText()
                    "creator" -> if (result.author == null) result.author = parser.nextText()
                    "identifier" -> if (result.identifier == null) result.identifier = parser.nextText()
                    "description" -> if (result.description == null) result.description = parser.nextText()
                    "meta" -> if (parser.getAttributeValue(null, "name") == "cover") {
                        legacyCoverId = parser.getAttributeValue(null, "content")
                    }
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        val properties = parser.getAttributeValue(null, "properties").orEmpty()
                        if (id != null && href != null) manifest[id] = href to properties
                    }
                    "spine" -> result.direction = parser.getAttributeValue(null, "page-progression-direction")
                }
            }
            parser.next()
        }
        result.coverHref = manifest.values.firstOrNull { (_, props) ->
            props.split(' ').contains("cover-image")
        }?.first ?: legacyCoverId?.let { manifest[it]?.first }
        return result
    }

    private fun normalizePath(base: String, href: String): String {
        val decoded = java.net.URLDecoder.decode(href.replace("+", "%2B"), Charsets.UTF_8.name())
            .substringBefore('#')
        val parts = (if (base.isBlank()) decoded else "$base/$decoded").split('/')
        val stack = ArrayDeque<String>()
        for (part in parts) when (part) {
            "", "." -> Unit
            ".." -> if (stack.isNotEmpty()) stack.removeLast() else throw EpubImportException("EPUB 包含非法路径")
            else -> stack.addLast(part)
        }
        return stack.joinToString("/")
    }
}
