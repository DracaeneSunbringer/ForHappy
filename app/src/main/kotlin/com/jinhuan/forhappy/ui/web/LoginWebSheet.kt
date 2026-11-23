package com.jinhuan.forhappy.ui.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.cookie.CookieSyncBridge
import com.jinhuan.forhappy.ui.components.ForHappyTextField
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun shouldAutoCompleteWebLogin(
    url: String?,
    hasSessionCookie: Boolean,
    cookiesChangedOrSawAuthFlow: Boolean,
): Boolean {
    if (!hasSessionCookie || !cookiesChangedOrSawAuthFlow) return false
    val parsed = url?.toHttpUrlOrNull() ?: return false
    if (!parsed.scheme.equals("https", ignoreCase = true) ||
        !parsed.host.equals("52dmeme.com", ignoreCase = true)
    ) {
        return false
    }
    val path = parsed.encodedPath.ifEmpty { "/" }
    if (path == "/login" ||
        path == "/register" ||
        path == "/reset" ||
        path == "/app" ||
        path == "/app.html" ||
        path == "/welcome" ||
        path == "/i" ||
        path.startsWith("/i/") ||
        path.startsWith("/auth/") ||
        path.startsWith("/me/account")
    ) {
        return false
    }
    return true
}

/**
 * Interactive login sheet supporting:
 * 1. Direct native account-password login via `/api/auth/password-login` over the Rust ECH tunnel.
 * 2. WebView login supporting direct OAuth providers (`/auth/login`, `/auth/github/start`,
 *    `/auth/google/start`) and `https://52dmeme.com/login` (with guard against `/login -> 307 /app`),
 *    synced directly into OkHttp [PersistentCookieJar].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebSheet(
    api: ForHappyApiClient,
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val cookieJar = api.cookieJar
    val scope = rememberCoroutineScope()

    var loginTab by remember { mutableIntStateOf(0) } // 0 = 账号密码直连, 1 = 网页 / OAuth 登录
    var targetWebUrl by remember { mutableStateOf("https://52dmeme.com/auth/login?next=%2F") }
    var currentWebPath by remember { mutableStateOf("") }
    var sawAuthFlow by remember { mutableStateOf(false) }
    val initialCookieSnapshot = remember {
        "https://52dmeme.com/".toHttpUrlOrNull()
            ?.let { u -> cookieJar.loadForUrl(u).associate { it.name to it.value } }
            .orEmpty()
    }
    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun openWebTarget(url: String) {
        targetWebUrl = url
        val parsed = url.toHttpUrlOrNull()
        if (parsed != null && (!parsed.host.equals("52dmeme.com", ignoreCase = true) || parsed.encodedPath.startsWith("/auth/"))) {
            sawAuthFlow = true
        }
        if (loginTab == 1) {
            webViewRef?.loadUrl(url)
        } else {
            loginTab = 1
        }
    }

    fun submitNativeLogin() {
        val trimmedLogin = loginInput.trim()
        if (trimmedLogin.isEmpty() || passwordInput.isEmpty()) {
            loginError = "请输入登录名与密码"
            return
        }
        if (submitting) return
        submitting = true
        loginError = null
        scope.launch {
            val result = api.loginWithPassword(trimmedLogin, passwordInput)
            submitting = false
            result.onSuccess {
                onAuthenticated()
            }.onFailure { err ->
                loginError = err.message ?: "登录失败"
            }
        }
    }

    val hideTopbarScript = remember {
        """
        (function(){
          if(document.getElementById('forhappy-login-clean')) return;
          var s=document.createElement('style');
          s.id='forhappy-login-clean';
          s.textContent='.topbar,.login-top,.back-link,a[href="/"]{display:none!important;}';
          (document.head||document.documentElement).appendChild(s);
        })();
        """.trimIndent()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (webViewRef != null) {
                CookieSyncBridge.pullFromWebViewToJar(cookieJar)
            }
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "登录 52dmeme.com",
                                    color = palette.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "支持账号密码直连与 Turnstile / OAuth 登录",
                                    color = palette.muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (loginTab == 1) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    CookieSyncBridge.pullFromWebViewToJar(cookieJar)
                                    onAuthenticated()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("完成登录", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = loginTab == 0,
                            onClick = {
                                if (loginTab != 0) {
                                    webViewRef?.stopLoading()
                                    webViewRef?.destroy()
                                    webViewRef = null
                                    loginTab = 0
                                }
                            },
                            label = { Text("账号密码直连", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                        FilterChip(
                            selected = loginTab == 1,
                            onClick = {
                                if (loginTab != 1) {
                                    targetWebUrl = "https://52dmeme.com/auth/login?next=%2F"
                                    sawAuthFlow = true
                                    loginTab = 1
                                }
                            },
                            label = { Text("网页 / OAuth 登录", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                    }
                }
            }

            if (loginTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "账号密码登录",
                                color = palette.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "使用在「账号安全」中设置的登录名与密码，经 Rust ECH 隧道直连",
                                color = palette.muted,
                                fontSize = 12.sp,
                            )

                            ForHappyTextField(
                                value = loginInput,
                                onValueChange = {
                                    loginInput = it
                                    loginError = null
                                },
                                label = "登录名",
                                placeholder = "输入用户名或邮箱",
                                singleLine = true,
                                enabled = !submitting,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            ForHappyTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    loginError = null
                                },
                                label = "密码",
                                placeholder = "输入登录密码",
                                singleLine = true,
                                enabled = !submitting,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                        tint = palette.muted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { showPassword = !showPassword },
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitNativeLogin() }),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (!loginError.isNullOrBlank()) {
                                Text(
                                    text = loginError!!,
                                    color = palette.danger,
                                    fontSize = 12.sp,
                                )
                            }

                            Button(
                                onClick = { submitNativeLogin() },
                                enabled = !submitting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                            ) {
                                Text(
                                    text = if (submitting) "正在登录…" else "登录",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            OutlinedButton(
                                onClick = { openWebTarget("https://52dmeme.com/auth/login?next=%2F") },
                                enabled = !submitting,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("使用 Linux.DO 授权登录", fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { openWebTarget("https://52dmeme.com/auth/github/start") },
                                    enabled = !submitting,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("GitHub", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = { openWebTarget("https://52dmeme.com/auth/google/start") },
                                    enabled = !submitting,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Google", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = { openWebTarget("https://52dmeme.com/login?next=%2F") },
                                    enabled = !submitting,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("网页登录页", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val quickProviders = listOf(
                        "Linux.DO" to "https://52dmeme.com/auth/login?next=%2F",
                        "GitHub" to "https://52dmeme.com/auth/github/start",
                        "Google" to "https://52dmeme.com/auth/google/start",
                        "网页登录页" to "https://52dmeme.com/login?next=%2F",
                    )
                    quickProviders.forEach { (label, url) ->
                        FilterChip(
                            selected = targetWebUrl == url,
                            onClick = { openWebTarget(url) },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                    }
                }

                if (currentWebPath == "/app") {
                    Surface(
                        color = palette.surface2,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "站点 /login 当前跳转至 /app，请直接点击上方 Linux.DO / GitHub / Google 按钮登录",
                            color = palette.muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = palette.primary,
                        trackColor = palette.surface2,
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.surface),
                    factory = { ctx ->
                        CookieManager.getInstance().setAcceptCookie(true)
                        WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(false)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                private fun markAuthIfExternalOrAuthRoute(urlStr: String?) {
                                    val u = urlStr?.toHttpUrlOrNull() ?: return
                                    if (!u.host.equals("52dmeme.com", ignoreCase = true) ||
                                        u.encodedPath.startsWith("/auth/")
                                    ) {
                                        sawAuthFlow = true
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    markAuthIfExternalOrAuthRoute(request?.url?.toString())
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    markAuthIfExternalOrAuthRoute(url)
                                    super.onPageStarted(view, url, favicon)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    currentWebPath = url?.toHttpUrlOrNull()
                                        ?.takeIf { it.host.equals("52dmeme.com", ignoreCase = true) }
                                        ?.encodedPath
                                        .orEmpty()
                                    if (url?.contains("52dmeme.com/login") == true) {
                                        view?.evaluateJavascript(hideTopbarScript, null)
                                    }
                                    CookieSyncBridge.pullFromWebViewToJar(cookieJar)
                                    val nowCookies = "https://52dmeme.com/".toHttpUrlOrNull()
                                        ?.let { u -> cookieJar.loadForUrl(u).associate { it.name to it.value } }
                                        .orEmpty()
                                    val cookiesChangedOrSawAuth =
                                        sawAuthFlow || initialCookieSnapshot.isEmpty() || nowCookies != initialCookieSnapshot
                                    if (shouldAutoCompleteWebLogin(
                                            url = url,
                                            hasSessionCookie = cookieJar.hasSessionCookieFor(),
                                            cookiesChangedOrSawAuthFlow = cookiesChangedOrSawAuth,
                                        )
                                    ) {
                                        onAuthenticated()
                                    }
                                }
                            }
                            loadUrl(targetWebUrl)
                            webViewRef = this
                        }
                    },
                )
            }
        }
    }
}
