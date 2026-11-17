package com.jinhuan.forhappy.ui.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.network.cookie.CookieSyncBridge
import com.jinhuan.forhappy.core.network.cookie.PersistentCookieJar
import com.jinhuan.forhappy.core.network.media.WebAssetCacheManager
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.json.JSONObject

/**
 * Shellless WebView container routed through [WebAssetCacheManager] (Rust ECH+H2 gateway):
 * - Intercepts same-origin GETs (HTML, JS modules, CSS, API endpoints, assets) via OkHttp+Rust ECH.
 * - Bridges same-origin mutating `fetch` (`POST`/`PUT`/`PATCH`/`DELETE`) via async `@JavascriptInterface`.
 * - Hides web site chrome (`.topbar`, `.mobile-nav`, `.site-footer`, `.pet-dock`) and ensures `/pets`
 *   canvas uses full viewport height (`MATCH_PARENT` + `fit()` resize).
 */
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
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val isPetsCanvas = remember(route) { route.trim().startsWith("/pets") }
    val targetUrl = remember(route) {
        if (route.startsWith("http")) route else "https://52dmeme.com${if (route.startsWith("/")) route else "/$route"}"
    }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieSyncBridge.pullFromWebViewToJar(cookieJar)
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    val shelllessCss = remember(isPetsCanvas) {
        if (isPetsCanvas) {
            ".topbar,.mobile-nav,.site-footer,.pet-dock{display:none!important;} html,body,#main{margin:0!important;padding:0!important;width:100%!important;height:100%!important;max-width:none!important;}"
        } else {
            ".topbar,.mobile-nav,.site-footer,.pet-dock{display:none!important;} body{padding-top:4px!important;padding-bottom:8px!important;}"
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
          if(!document.getElementById('forhappy-shellless-css')){
            var st=document.createElement('style');
            st.id='forhappy-shellless-css';
            st.textContent=$quotedCss;
            (document.head||document.documentElement).appendChild(st);
          }
          setTimeout(function(){ window.dispatchEvent(new Event('resize')); }, 80);
        })();
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        if (showCompactBar) {
            Surface(
                color = palette.surface,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = title,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "经本地 Rust + ECH/H2 无壳加速容器渲染",
                            color = palette.muted,
                            fontSize = 11.sp,
                        )
                    }
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "刷新",
                                tint = palette.primary,
                            )
                        }
                    }
                }
            }
        }

        if (isLoading && progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = palette.primary,
                trackColor = palette.surface2,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
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
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
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
                                    htmlHeadInjection = if (request.isForMainFrame) headInjectionHtml else null,
                                )
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                view?.evaluateJavascript(injectFallbackScript, null)
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
        }
    }
}
