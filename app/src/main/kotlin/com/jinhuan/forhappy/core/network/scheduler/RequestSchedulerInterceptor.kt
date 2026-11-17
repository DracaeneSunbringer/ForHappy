package com.jinhuan.forhappy.core.network.scheduler

import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.network.cookie.CookieSyncBridge
import com.jinhuan.forhappy.core.network.cookie.PersistentCookieJar
import com.jinhuan.forhappy.core.network.rust.RustNetBridge
import com.jinhuan.forhappy.core.network.rust.RustNetManager
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Unified OkHttp Application Interceptor that:
 * 1. Resolves HTTP 301/302/303/307/308 redirects in-loop (requires OkHttp `followRedirects(false)`)
 *    so every hop preserves domain-scoped cookies AND routes covered hosts through the Rust gateway.
 * 2. Injects `Cookie` from [PersistentCookieJar] keyed on the true HTTPS host (never `127.0.0.1`).
 * 3. Rewrites `https://<covered-host>/...` to `http://127.0.0.1:$port/...` with `Host`,
 *    `X-ForHappy-Target-Host`, and `X-ForHappy-Target-Port` when Rust gateway is active.
 * 4. Saves `Set-Cookie` headers and calibrates [ServerClock] from HTTP `Date` headers.
 */
class RequestSchedulerInterceptor(
    private val rustNetManager: RustNetManager,
    private val cookieJar: PersistentCookieJar,
) : Interceptor {

    companion object {
        private const val MAX_REDIRECTS = 5
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var logicalRequest = chain.request()
        var hops = 0

        while (true) {
            val origUrl = logicalRequest.url
            val cookiesHeader = cookieJar.buildCookieHeader(origUrl)
            val withCookieBuilder = logicalRequest.newBuilder()
            if (!cookiesHeader.isNullOrBlank() && logicalRequest.header("Cookie") == null) {
                withCookieBuilder.header("Cookie", cookiesHeader)
            }
            if (logicalRequest.header("Origin") == null &&
                logicalRequest.method.uppercase() in setOf("POST", "PUT", "PATCH", "DELETE") &&
                (origUrl.host.equals("52dmeme.com", ignoreCase = true) ||
                    origUrl.host.lowercase().endsWith(".52dmeme.com"))
            ) {
                // Server enforces same-origin CSRF checks on mutating endpoints ("非法来源").
                withCookieBuilder.header("Origin", "https://52dmeme.com")
            }
            if (logicalRequest.header("User-Agent") == null) {
                withCookieBuilder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; ForHappy) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
                )
            }
            val preparedOrig = withCookieBuilder.build()

            val isCoveredHttps = origUrl.scheme.equals("https", ignoreCase = true) &&
                RustNetManager.isCoveredHost(origUrl.host)
            if (isCoveredHttps && rustNetManager.gatewayPort == 0 && RustNetBridge.libraryLoaded) {
                var waited = 0
                while (rustNetManager.gatewayPort == 0 && waited < 800) {
                    Thread.sleep(40)
                    waited += 40
                }
            }

            val gatewayPort = rustNetManager.gatewayPort
            val shouldRouteViaRust = gatewayPort > 0 && isCoveredHttps

            val wireRequest = if (shouldRouteViaRust) {
                rewriteToLoopbackGateway(preparedOrig, origUrl, gatewayPort)
            } else {
                preparedOrig
            }

            val response = try {
                chain.proceed(wireRequest)
            } catch (ioe: IOException) {
                // If loopback gateway failed unexpectedly on initial contact, retry direct once
                if (shouldRouteViaRust) {
                    chain.proceed(preparedOrig)
                } else {
                    throw ioe
                }
            }

            ServerClock.calibrateFromHttpDate(response.header("Date"))

            val setCookies = response.headers("Set-Cookie")
            if (setCookies.isNotEmpty()) {
                cookieJar.saveFromResponse(origUrl, setCookies)
                CookieSyncBridge.syncResponseToWebView(origUrl, setCookies)
            }

            val location = response.header("Location")
            if (!isRedirectCode(response.code) || location.isNullOrBlank() || hops >= MAX_REDIRECTS) {
                return response
            }

            val nextUrl = origUrl.resolve(location) ?: return response
            hops++
            val prevMethod = preparedOrig.method
            val nextMethod = when (response.code) {
                307, 308 -> prevMethod
                303 -> if (prevMethod == "HEAD") "HEAD" else "GET"
                else -> if (prevMethod == "POST" || prevMethod == "PUT" || prevMethod == "DELETE") "GET" else prevMethod
            }
            val nextBody = if (nextMethod == prevMethod) preparedOrig.body else null

            response.close()
            val nextBuilder = preparedOrig.newBuilder()
                .url(nextUrl)
                .method(nextMethod, nextBody)
                .removeHeader("Cookie")
                .removeHeader("X-ForHappy-Target-Host")
                .removeHeader("X-ForHappy-Target-Port")
            if (nextBody == null) {
                nextBuilder.removeHeader("Content-Type")
                nextBuilder.removeHeader("Content-Length")
            }
            if (!nextUrl.host.equals(origUrl.host, ignoreCase = true)) {
                nextBuilder.removeHeader("Authorization")
            }
            logicalRequest = nextBuilder.build()
        }
    }

    private fun rewriteToLoopbackGateway(request: Request, origUrl: HttpUrl, port: Int): Request {
        val gatewayUrl = origUrl.newBuilder()
            .scheme("http")
            .host("127.0.0.1")
            .port(port)
            .build()
        val origPort = if (origUrl.port > 0) origUrl.port else 443
        val hostHeader = if (origPort == 443) origUrl.host else "${origUrl.host}:$origPort"
        return request.newBuilder()
            .url(gatewayUrl)
            .header("Host", hostHeader)
            .header("X-ForHappy-Target-Host", origUrl.host)
            .header("X-ForHappy-Target-Port", origPort.toString())
            .build()
    }

    private fun isRedirectCode(code: Int): Boolean = code in listOf(301, 302, 303, 307, 308)
}
