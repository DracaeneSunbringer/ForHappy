package com.jinhuan.forhappy.core.network.cookie

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Bidirectional sync between OkHttp [PersistentCookieJar] and Android WebView [CookieManager]
 * so Turnstile/OAuth logins in [LoginWebSheet] immediately authenticate native OkHttp API calls
 * and vice versa.
 */
object CookieSyncBridge {
    private val SYNC_ORIGINS = listOf(
        "https://52dmeme.com/",
        "https://cc.52dmeme.com/",
        "https://img.52dmeme.com/",
    )

    fun syncResponseToWebView(url: HttpUrl, setCookieHeaders: List<String>) {
        if (setCookieHeaders.isEmpty()) return
        runCatching {
            val cm = CookieManager.getInstance()
            val urlStr = url.toString()
            for (header in setCookieHeaders) {
                cm.setCookie(urlStr, header)
            }
            cm.flush()
        }
    }

    fun pushJarToWebView(jar: PersistentCookieJar) {
        runCatching {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            for (origin in SYNC_ORIGINS) {
                val url = origin.toHttpUrlOrNull() ?: continue
                for (ck in jar.loadForUrl(url)) {
                    val str = buildString {
                        append(ck.name).append('=').append(ck.value)
                        append("; Path=").append(ck.path)
                        if (!ck.hostOnly) append("; Domain=").append(ck.domain)
                        if (ck.secure) append("; Secure")
                        if (ck.httpOnly) append("; HttpOnly")
                    }
                    cm.setCookie(origin, str)
                }
            }
            cm.flush()
        }
    }

    fun pullFromWebViewToJar(jar: PersistentCookieJar) {
        runCatching {
            val cm = CookieManager.getInstance()
            for (origin in SYNC_ORIGINS) {
                val url = origin.toHttpUrlOrNull() ?: continue
                val raw = cm.getCookie(origin) ?: continue
                val parsed = mutableListOf<Cookie>()
                for (pair in raw.split(';')) {
                    val trimmed = pair.trim()
                    val eq = trimmed.indexOf('=')
                    if (eq <= 0) continue
                    val name = trimmed.substring(0, eq).trim()
                    val value = trimmed.substring(eq + 1).trim()
                    if (name.isEmpty()) continue
                    parsed += Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(url.host)
                        .path("/")
                        .expiresAt(System.currentTimeMillis() + 30L * 86400L * 1000L)
                        .secure()
                        .build()
                }
                if (parsed.isNotEmpty()) {
                    jar.upsertCookies(url.host, parsed)
                }
            }
        }
    }

    fun clearWebViewCookies() {
        runCatching {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        }
    }
}
