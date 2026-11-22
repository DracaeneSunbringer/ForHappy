package com.jinhuan.forhappy.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.ChatOpenSessionRequest
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.NovelExcerptSpec
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.cookie.CookieSyncBridge
import com.jinhuan.forhappy.core.network.cookie.PersistentCookieJar
import com.jinhuan.forhappy.core.network.media.WebAssetCacheManager
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.json.JSONObject

/**
 * Shellless WebView container routed through [WebAssetCacheManager] (Rust ECH+H2 gateway):
 * - Injects CSS hiding website chrome (`.topbar`, `.mobile-nav`, `.site-footer`, `.pet-dock`)
 * - Optional auto-fading floating capsule bar (`3s`) with pull/tap reveal at top edge
 * - Canvas bottom-200dp edge gesture exclusion for game canvases (`/pets`, `/arcade`, `/slime`)
 * - Immediate `onPause` + audio silence when backgrounded or covered by Boss Cloud Doc
 * - Novel Reader native bottom collaboration bar (`💬 群聊` half-sheet, `✂ 摘录分享`, `☰ 目录`)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShelllessWebScreen(
    route: String,
    title: String,
    cookieJar: PersistentCookieJar,
    assetCache: WebAssetCacheManager,
    modifier: Modifier = Modifier,
    showCompactBar: Boolean = true,
    onCookiesSynced: (() -> Unit)? = null,
    floatingCapsule: Boolean = false,
    pausedOverlay: Boolean = false,
    isPinnedToHub: Boolean = false,
    onTogglePinToHub: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null,
    onOpenChatDrawer: (() -> Unit)? = null,
    api: ForHappyApiClient? = null,
    chatController: ChatSessionController? = null,
    me: MeUser? = null,
    onRequireLogin: () -> Unit = {},
    onJumpToGroupChat: ((groupId: Long) -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cleanRoute = remember(route) { route.trim() }
    val isPetsCanvas = remember(cleanRoute) {
        cleanRoute.startsWith("/pets") || cleanRoute.startsWith("/arcade") || cleanRoute.startsWith("/slime")
    }
    val isNovelReader = remember(cleanRoute) {
        (cleanRoute.startsWith("/novels") || cleanRoute.contains("/novels/")) && !cleanRoute.startsWith("/novels/studio")
    }
    val targetUrl = remember(cleanRoute) {
        if (cleanRoute.startsWith("http")) cleanRoute else "https://52dmeme.com${if (cleanRoute.startsWith("/")) cleanRoute else "/$cleanRoute"}"
    }

    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    var capsuleVisible by remember { mutableStateOf(true) }
    var lastInteractMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showNovelChatDrawer by remember { mutableStateOf(false) }
    var pendingExcerpt by remember { mutableStateOf<NovelExcerptSpec?>(null) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    // Auto-fade floating capsule after 3s idle when enabled
    LaunchedEffect(floatingCapsule, lastInteractMs, capsuleVisible, showOverflowMenu) {
        if (floatingCapsule && capsuleVisible && !showOverflowMenu) {
            delay(3_000L)
            capsuleVisible = false
        }
    }

    LaunchedEffect(statusNotice) {
        if (statusNotice != null) {
            delay(2500L)
            statusNotice = null
        }
    }

    fun handleBackNavigation() {
        when {
            showOverflowMenu -> showOverflowMenu = false
            showNovelChatDrawer -> showNovelChatDrawer = false
            pendingExcerpt != null -> pendingExcerpt = null
            else -> {
                val wv = webViewRef
                if (wv == null) {
                    onExit?.invoke()
                } else {
                    wv.evaluateJavascript(
                        "(function(){var ds=Array.prototype.slice.call(document.querySelectorAll('dialog[open]'));if(ds.length){var d=ds[ds.length-1];try{d.close();}catch(e){d.removeAttribute('open');}return 'closed';}return 'none';})()",
                    ) { res ->
                        if (res != null && res.contains("closed")) {
                            return@evaluateJavascript
                        }
                        if (wv.canGoBack()) {
                            wv.goBack()
                        } else {
                            onExit?.invoke()
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = showOverflowMenu || showNovelChatDrawer || pendingExcerpt != null || canGoBack || onExit != null) {
        handleBackNavigation()
    }

    // Pause WebView + HTML5/WebAudio immediately when Boss Key activates or activity backgrounds
    fun pauseWebMediaAndTimers(wv: WebView?) {
        wv ?: return
        wv.onPause()
        wv.pauseTimers()
        wv.evaluateJavascript(
            "try{document.querySelectorAll('audio,video').forEach(function(e){e.pause();});}catch(err){}",
            null,
        )
    }

    LaunchedEffect(pausedOverlay, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (pausedOverlay) {
            pauseWebMediaAndTimers(wv)
        } else {
            wv.onResume()
            wv.resumeTimers()
        }
    }

    DisposableEffect(lifecycleOwner, pausedOverlay) {
        val observer = LifecycleEventObserver { _, event ->
            val wv = webViewRef
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> pauseWebMediaAndTimers(wv)
                Lifecycle.Event.ON_RESUME -> if (!pausedOverlay) {
                    wv?.onResume()
                    wv?.resumeTimers()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            CookieSyncBridge.pullFromWebViewToJar(cookieJar)
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()
    val navBarBotDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()

    val shelllessCss = remember(isPetsCanvas, floatingCapsule, showCompactBar, isNovelReader, statusBarTopDp, navBarBotDp) {
        if (isPetsCanvas) {
            ".topbar,.mobile-nav,.site-footer,.pet-dock,header.site-header{display:none!important;} html,body,#main{margin:0!important;padding:0!important;width:100%!important;height:100dvh!important;min-height:100%!important;max-width:none!important;overflow:hidden!important;}"
        } else {
            val topPadPx = when {
                floatingCapsule -> statusBarTopDp + 44
                !showCompactBar -> statusBarTopDp + 8
                else -> 4
            }
            val botPadPx = if (isNovelReader) 10 else navBarBotDp + 12
            ".topbar,.mobile-nav,.site-footer,.pet-dock,header.site-header{display:none!important;} html,body{min-height:100dvh!important;} body{padding-top:${topPadPx}px!important;padding-bottom:${botPadPx}px!important;box-sizing:border-box!important;}"
        }
    }

    val headInjectionHtml = remember(shelllessCss) {
        """
        <style id="forhappy-shellless-css">$shelllessCss</style>
        <script>
        (function(){
          if(window.__fhBridgeInstalled) return;
          window.__fhBridgeInstalled = true;
          var pending = {};
          var seq = 0;
          window.__fhResolve = function(id, jsonStr){
            var cb = pending[id];
            if(!cb) return;
            delete pending[id];
            try{
              var res = JSON.parse(jsonStr);
              cb.resolve(new Response(res.body || '', {
                status: res.status || 200,
                headers: { 'content-type': res.contentType || 'application/json' }
              }));
            }catch(err){ cb.reject(err); }
          };
          var origFetch = window.fetch.bind(window);
          window.fetch = function(input, init){
            try{
              var url = typeof input === 'string' ? input : (input && input.url) || '';
              var abs = new URL(url, location.href);
              var method = ((init && init.method) || (typeof input === 'object' && input && input.method) || 'GET').toUpperCase();
              var isFormOrBlob = (typeof FormData !== 'undefined' && init && init.body instanceof FormData) ||
                                 (typeof Blob !== 'undefined' && init && init.body instanceof Blob);
              if((abs.hostname === '52dmeme.com' || abs.hostname === 'cc.52dmeme.com' || abs.hostname === 'img.52dmeme.com') &&
                 method !== 'GET' && method !== 'HEAD' && !isFormOrBlob && window.ForHappyWebBridge){
                var id = String(++seq);
                var bodyStr = null;
                if(init && init.body != null){
                  bodyStr = typeof init.body === 'string' ? init.body :
                    (init.body instanceof URLSearchParams ? init.body.toString() : String(init.body));
                }
                var ct = 'application/json';
                var hdr = (init && init.headers) || (typeof input === 'object' && input && input.headers);
                if(hdr){
                  if(typeof hdr.get === 'function'){ ct = hdr.get('content-type') || hdr.get('Content-Type') || ct; }
                  else{ ct = hdr['content-type'] || hdr['Content-Type'] || ct; }
                }
                return new Promise(function(resolve, reject){
                  pending[id] = { resolve: resolve, reject: reject };
                  window.ForHappyWebBridge.requestAsync(id, method, abs.href, ct, bodyStr || '');
                });
              }
            }catch(e){}
            return origFetch(input, init);
          };
        })();
        </script>
        """.trimIndent()
    }

    val injectFallbackScript = remember(shelllessCss) {
        val quotedCss = JSONObject.quote(shelllessCss)
        """
        (function(){
          var st=document.getElementById('forhappy-shellless-css');
          if(!st){
            st=document.createElement('style');
            st.id='forhappy-shellless-css';
            (document.head||document.documentElement).appendChild(st);
          }
          st.textContent=$quotedCss;
          setTimeout(function(){ window.dispatchEvent(new Event('resize')); }, 80);
        })();
        """.trimIndent()
    }

    val latestHeadInjectionHtml by rememberUpdatedState(headInjectionHtml)
    val latestInjectFallbackScript by rememberUpdatedState(injectFallbackScript)

    LaunchedEffect(shelllessCss, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        val qCss = JSONObject.quote(shelllessCss)
        wv.evaluateJavascript(
            "(function(){var el=document.getElementById('forhappy-shellless-css');if(el){el.textContent=$qCss;}})();",
            null,
        )
    }

    fun triggerExcerptCapture() {
        if (me == null) {
            onRequireLogin()
            return
        }
        val wv = webViewRef ?: return
        val script = """
            (function(){
              try {
                var sel = (window.getSelection && window.getSelection().toString() || '').trim();
                if (!sel) {
                  var p = document.querySelector('.novel-reader p, .reader-body p, article p, #reader p, main p');
                  if (p) sel = (p.innerText || p.textContent || '').trim();
                }
                sel = sel.slice(0, 1600);
                var path = location.pathname || '';
                var bm = path.match(/\/novels\/([a-z0-9-]{1,80})/i);
                var bookId = bm ? bm[1].toLowerCase() : 'novel';
                var usp = new URLSearchParams(location.search || '');
                var ch = parseInt(usp.get('chapter') || usp.get('ch') || '1', 10);
                if (!ch || ch < 1) ch = 1;
                var bookTitle = (document.querySelector('h1, .novel-title, .book-title')?.textContent || document.title || '站内书房').replace(/\s+/g,' ').trim().slice(0,100);
                var chTitle = (document.querySelector('h2, .chapter-title')?.textContent || ('第 ' + ch + ' 章')).replace(/\s+/g,' ').trim().slice(0,120);
                var themeAttr = (document.documentElement.dataset.readerTheme || document.body.dataset.theme || 'paper').toLowerCase();
                var theme = ['paper','white','green','night'].includes(themeAttr) ? themeAttr : 'paper';
                return JSON.stringify({bookId:bookId,chapter:ch,theme:theme,title:bookTitle,chapterTitle:chTitle,excerpt:sel});
              } catch(e) { return ''; }
            })();
        """.trimIndent()
        wv.evaluateJavascript(script) { rawResult ->
            val unquoted = runCatching {
                val decoded = JSONObject("{\"v\":$rawResult}").optString("v")
                JSONObject(decoded)
            }.getOrNull()
            val excerpt = unquoted?.optString("excerpt")?.trim().orEmpty()
            if (excerpt.isBlank()) {
                statusNotice = "请先在正文中长按选中一段文字再点击摘录"
            } else {
                pendingExcerpt = NovelExcerptSpec(
                    bookId = unquoted?.optString("bookId").orEmpty().ifBlank { "novel" },
                    chapter = (unquoted?.optInt("chapter", 1) ?: 1).coerceAtLeast(1),
                    theme = unquoted?.optString("theme").orEmpty().ifBlank { "paper" },
                    title = unquoted?.optString("title").orEmpty().ifBlank { "站内书房" },
                    chapterTitle = unquoted?.optString("chapterTitle").orEmpty().ifBlank { "第 1 章" },
                    excerpt = excerpt,
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        if (showCompactBar && !floatingCapsule) {
            Surface(color = palette.surface, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("经本地 Rust + ECH/H2 无壳加速容器渲染", color = palette.muted, fontSize = 11.sp)
                    }
                    Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp)) {
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "刷新", tint = palette.primary)
                        }
                    }
                }
            }
        }

        if (isLoading && progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = palette.primary,
                trackColor = palette.surface2,
            )
        }

        statusNotice?.let { note ->
            Surface(color = palette.primaryContainer) {
                Text(
                    text = note,
                    color = palette.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val exclHeightPx = with(density) { 200.dp.roundToPx() }
            val exclWidthPx = with(density) { 44.dp.roundToPx() }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CookieSyncBridge.pushJarToWebView(cookieJar)
                    CookieManager.getInstance().setAcceptCookie(true)
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        if (isPetsCanvas) {
                            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                                val w = v.width
                                val h = v.height
                                if (w > exclWidthPx * 2 && h > exclHeightPx) {
                                    val top = (h - exclHeightPx).coerceAtLeast(0)
                                    ViewCompat.setSystemGestureExclusionRects(
                                        v,
                                        listOf(
                                            Rect(0, top, exclWidthPx, h),
                                            Rect(w - exclWidthPx, top, w, h),
                                        ),
                                    )
                                }
                            }
                        }

                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.databaseEnabled = true
                        settings.useWideViewPort = !isPetsCanvas
                        settings.loadWithOverviewMode = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                        val wv = this
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun requestAsync(reqId: String, method: String, url: String, contentType: String, body: String) {
                                    scope.launch {
                                        val res = assetCache.executeBridgedMutation(
                                            method = method,
                                            urlString = url,
                                            contentType = contentType,
                                            bodyText = body,
                                        )
                                        val payloadJson = ForHappyJson.encodeToString(res)
                                        val quotedId = JSONObject.quote(reqId)
                                        val quotedPayload = JSONObject.quote(payloadJson)
                                        wv.post {
                                            wv.evaluateJavascript("window.__fhResolve && window.__fhResolve($quotedId, $quotedPayload);", null)
                                        }
                                    }
                                }
                            },
                            "ForHappyWebBridge",
                        )

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                                if (cm != null) {
                                    Log.d("ForHappyWeb", "[${cm.messageLevel()}] ${cm.sourceId()}:${cm.lineNumber()} ${cm.message()}")
                                }
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest,
                            ): WebResourceResponse? {
                                return assetCache.interceptShelllessGet(
                                    request = request,
                                    htmlHeadInjection = if (request.isForMainFrame) latestHeadInjectionHtml else null,
                                )
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                view?.evaluateJavascript(latestInjectFallbackScript, null)
                                onCookiesSynced?.invoke()
                            }
                        }
                        loadUrl(targetUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    if (view.url.isNullOrBlank()) {
                        view.loadUrl(targetUrl)
                    }
                },
            )

            // Floating auto-fading capsule & 28dp top pull/tap reveal trigger
            if (floatingCapsule) {
                if (!capsuleVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(28.dp)
                            .align(Alignment.TopCenter)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 12f) {
                                        capsuleVisible = true
                                        lastInteractMs = System.currentTimeMillis()
                                    }
                                }
                            }
                            .clickable {
                                capsuleVisible = true
                                lastInteractMs = System.currentTimeMillis()
                            },
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = capsuleVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Surface(
                        color = palette.surface.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(999.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier.border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(999.dp)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    lastInteractMs = System.currentTimeMillis()
                                    handleBackNavigation()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = title,
                                color = palette.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(120.dp),
                            )
                            IconButton(
                                onClick = {
                                    lastInteractMs = System.currentTimeMillis()
                                    webViewRef?.reload()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = palette.primary, modifier = Modifier.size(17.dp))
                            }
                            Box {
                                IconButton(
                                    onClick = {
                                        lastInteractMs = System.currentTimeMillis()
                                        showOverflowMenu = true
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多", tint = palette.text, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = {
                                        showOverflowMenu = false
                                        lastInteractMs = System.currentTimeMillis()
                                    },
                                ) {
                                    if (onTogglePinToHub != null) {
                                        DropdownMenuItem(
                                            text = { Text(if (isPinnedToHub) "从「常用」取消固定" else "固定到发现「常用」") },
                                            onClick = {
                                                showOverflowMenu = false
                                                onTogglePinToHub()
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("复制当前链接") },
                                        onClick = {
                                            showOverflowMenu = false
                                            clipboard.setText(AnnotatedString(webViewRef?.url ?: targetUrl))
                                            statusNotice = "已复制链接"
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("在系统浏览器打开") },
                                        onClick = {
                                            showOverflowMenu = false
                                            StealthExemptionGate.markInternalJump()
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(webViewRef?.url ?: targetUrl))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                                )
                                            }
                                        },
                                    )
                                    if (api != null && chatController != null || onOpenChatDrawer != null) {
                                        DropdownMenuItem(
                                            text = { Text("💬 打开群聊抽屉") },
                                            onClick = {
                                                showOverflowMenu = false
                                                if (api != null && chatController != null) showNovelChatDrawer = true
                                                else onOpenChatDrawer?.invoke()
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Novel Reader Native Bottom Collaboration Bar
        if (isNovelReader) {
            Surface(color = palette.surface) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = palette.line.copy(alpha = 0.7f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NovelCollabBarButton(
                            icon = Icons.AutoMirrored.Rounded.Chat,
                            label = "群聊",
                            onClick = {
                                if (api != null && chatController != null) showNovelChatDrawer = true
                                else onOpenChatDrawer?.invoke()
                            },
                        )
                        NovelCollabBarButton(
                            icon = Icons.Rounded.ContentCut,
                            label = "摘录分享",
                            onClick = { triggerExcerptCapture() },
                        )
                        NovelCollabBarButton(
                            icon = Icons.Rounded.Menu,
                            label = "目录",
                            onClick = {
                                webViewRef?.evaluateJavascript(
                                    "try{document.querySelector('[data-toc], .toc-btn, .novel-toc-toggle, button[aria-label*=\"目录\"]')?.click();}catch(e){}",
                                    null,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    // Half-screen native Chat drawer preserving WebView scroll position
    if (showNovelChatDrawer && api != null && chatController != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showNovelChatDrawer = false },
            sheetState = sheetState,
            containerColor = palette.bg,
            dragHandle = null,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f)
                    .navigationBarsPadding(),
            ) {
                ChatScreen(
                    api = api,
                    controller = chatController,
                    me = me,
                    onNavigateToMeme = {},
                    onNavigateToTag = {},
                    onOpenUrl = {},
                    onRequireLogin = onRequireLogin,
                    forcedChannelMode = "group",
                )
            }
        }
    }

    // Excerpt Share Preview Sheet
    pendingExcerpt?.let { spec ->
        if (api != null && chatController != null) {
            val chatState by chatController.uiState.collectAsState()
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var editableExcerpt by remember(spec) { mutableStateOf(spec.excerpt) }
            var selectedGroupId by remember(chatState.groups) {
                mutableLongStateOf(chatState.groups.firstOrNull { it.joined }?.id ?: 1L)
            }
            var sending by remember { mutableStateOf(false) }

            ModalBottomSheet(
                onDismissRequest = { pendingExcerpt = null },
                sheetState = sheetState,
                containerColor = palette.bg,
                dragHandle = null,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("分享书房摘录卡", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        color = Color(0xFFFBF8F1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("《${spec.title}》 · ${spec.chapterTitle}", color = Color(0xFF7A7468), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = editableExcerpt,
                                onValueChange = { if (it.length <= 1600) editableExcerpt = it },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Text("发送到群聊：", color = palette.muted, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chatState.groups.filter { it.joined }.forEach { grp ->
                            val sel = grp.id == selectedGroupId
                            Surface(
                                color = if (sel) palette.primary else palette.surface2,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.clickable { selectedGroupId = grp.id },
                            ) {
                                Text(
                                    text = grp.name,
                                    color = if (sel) palette.onPrimary else palette.text,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { pendingExcerpt = null }, modifier = Modifier.weight(1f)) {
                            Text("取消")
                        }
                        Button(
                            enabled = editableExcerpt.isNotBlank() && !sending,
                            onClick = {
                                sending = true
                                scope.launch {
                                    val wire = ChatRichTextParser.serializeNovelExcerpt(spec.copy(excerpt = editableExcerpt.trim()))
                                    val res = api.sendChatMessage(selectedGroupId, wire, replyToId = null, retry = false)
                                    sending = false
                                    res.onSuccess {
                                        pendingExcerpt = null
                                        statusNotice = "摘录已发送到群聊"
                                        onJumpToGroupChat?.invoke(selectedGroupId)
                                    }.onFailure { err ->
                                        statusNotice = err.message ?: "发送摘录失败"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (sending) "发送中…" else "发送至群聊")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelCollabBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = palette.primary, modifier = Modifier.size(16.dp))
        Text(label, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
