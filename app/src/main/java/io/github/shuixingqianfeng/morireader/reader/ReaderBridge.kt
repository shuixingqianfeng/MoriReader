package io.github.shuixingqianfeng.morireader.reader

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.ReaderPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

data class ReaderLocation(
    val cfi: String?,
    val fraction: Double,
    val chapterIndex: Int,
    val chapterTitle: String,
)

data class TocItem(val label: String, val href: String, val depth: Int)

sealed interface ReaderEvent {
    data object Ready : ReaderEvent
    data object CenterTap : ReaderEvent
    data object Opened : ReaderEvent
    data class Stage(val name: String) : ReaderEvent
    data class Relocated(val location: ReaderLocation) : ReaderEvent
    data class Toc(val items: List<TocItem>) : ReaderEvent
    data class Error(val message: String) : ReaderEvent
}

@SuppressLint("RequiresFeature")
class ReaderController {
    private var replyProxy: JavaScriptReplyProxy? = null
    private val pending = ArrayDeque<String>()

    internal fun attach(proxy: JavaScriptReplyProxy) {
        replyProxy = proxy
        while (pending.isNotEmpty()) proxy.postMessage(pending.removeFirst())
    }

    internal fun detach() {
        replyProxy = null
        pending.clear()
    }

    fun send(message: JSONObject) {
        val text = message.toString()
        replyProxy?.postMessage(text) ?: pending.addLast(text)
    }

    fun next() = send(JSONObject().put("type", "next"))
    fun previous() = send(JSONObject().put("type", "previous"))
    fun goToHref(href: String) = send(JSONObject().put("type", "goToHref").put("href", href))
    fun goToSection(index: Int) = send(JSONObject().put("type", "goToSection").put("index", index))
    fun setAppearance(preferences: ReaderPreferences) = send(
        JSONObject().put("type", "setAppearance").put("appearance", preferences.toJson()),
    )
}

private fun ReaderPreferences.toJson() = JSONObject()
    .put("dailyGoalMinutes", dailyGoalMinutes)
    .put("fontSizeSp", fontSizeSp)
    .put("lineHeight", lineHeight)
    .put("paragraphSpacingEm", paragraphSpacingEm)
    .put("horizontalMarginDp", horizontalMarginDp)
    .put("theme", theme.name)
    .put("mode", mode.name)
    .put("swipeEnabled", swipeEnabled)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderWebView(
    book: BookEntity,
    preferences: ReaderPreferences,
    controller: ReaderController,
    modifier: Modifier = Modifier,
    onEvent: (ReaderEvent) -> Unit,
) {
    val currentEvent = androidx.compose.runtime.rememberUpdatedState(onEvent)
    val currentPreferences = androidx.compose.runtime.rememberUpdatedState(preferences)
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val bookFile = File(book.filePath)
            val loader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .addPathHandler("/books/", WebViewAssetLoader.PathHandler { path ->
                    if (path != "book.epub" || !bookFile.isFile) return@PathHandler null
                    WebResourceResponse(
                        "application/epub+zip",
                        null,
                        FileInputStream(bookFile),
                    ).apply {
                        responseHeaders = mapOf(
                            "Cache-Control" to "no-store",
                            "Content-Security-Policy" to "default-src 'none'",
                        )
                    }
                })
                .build()

            WebView(context).apply {
                webViewHolder[0] = this
                setBackgroundColor(Color.WHITE)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.blockNetworkLoads = true
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
                    WebViewCompat.startSafeBrowsing(context) { }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                        loader.shouldInterceptRequest(request.url)

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        request.url.host != WebViewAssetLoader.DEFAULT_DOMAIN
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    WebViewCompat.addWebMessageListener(
                        this,
                        "MoriNative",
                        setOf("https://${WebViewAssetLoader.DEFAULT_DOMAIN}"),
                    ) { _, message, sourceOrigin, isMainFrame, replyProxy ->
                        if (!isMainFrame || sourceOrigin.host != WebViewAssetLoader.DEFAULT_DOMAIN) return@addWebMessageListener
                        controller.attach(replyProxy)
                        handleReaderMessage(message.data.orEmpty(), currentEvent.value)
                        if (runCatching { JSONObject(message.data.orEmpty()).optString("type") }.getOrNull() == "ready") {
                            controller.send(
                                JSONObject()
                                    .put("type", "open")
                                    .put("url", "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/books/book.epub")
                                    .put("lastCfi", book.currentCfi)
                                    .put("appearance", currentPreferences.value.toJson()),
                            )
                        }
                    }
                } else {
                    currentEvent.value(ReaderEvent.Error("当前 Android WebView 不支持安全消息桥"))
                }
                loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/reader/index.html")
            }
        },
        update = { controller.setAppearance(preferences) },
    )

    DisposableEffect(book.id) {
        onDispose {
            controller.send(JSONObject().put("type", "close"))
            controller.detach()
            webViewHolder[0]?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewHolder[0] = null
        }
    }
}

private fun handleReaderMessage(text: String, onEvent: (ReaderEvent) -> Unit) {
    val json = runCatching { JSONObject(text) }.getOrElse {
        onEvent(ReaderEvent.Error("阅读器消息格式错误"))
        return
    }
    when (json.optString("type")) {
        "ready" -> onEvent(ReaderEvent.Ready)
        "opened" -> onEvent(ReaderEvent.Opened)
        "stage" -> onEvent(ReaderEvent.Stage(json.optString("name")))
        "centerTap" -> onEvent(ReaderEvent.CenterTap)
        "error" -> onEvent(ReaderEvent.Error(json.optString("message", "阅读器发生错误")))
        "relocated" -> onEvent(
            ReaderEvent.Relocated(
                ReaderLocation(
                    cfi = json.optString("cfi").takeIf { it.isNotBlank() && it != "null" },
                    fraction = json.optDouble("fraction", 0.0),
                    chapterIndex = json.optInt("sectionIndex", 0),
                    chapterTitle = json.optString("chapterTitle"),
                ),
            ),
        )
        "toc" -> {
            val array = json.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(TocItem(item.optString("label"), item.optString("href"), item.optInt("depth")))
                }
            }
            onEvent(ReaderEvent.Toc(items))
        }
    }
}
