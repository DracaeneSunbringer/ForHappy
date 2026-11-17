package com.jinhuan.forhappy.core.network.cookie

import com.jinhuan.forhappy.core.db.ForHappyDatabase
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Persistent Cookie store keyed strictly by the original HTTPS host/url rather than the
 * `127.0.0.1` local Rust gateway address.
 */
class PersistentCookieJar(
    private val db: ForHappyDatabase,
) {
    private val memory = CopyOnWriteArrayList<CookieEntry>()

    private data class CookieEntry(
        val originHost: String,
        val cookie: Cookie,
    )

    init {
        reloadFromDb()
    }

    fun reloadFromDb() {
        memory.clear()
        for (row in db.loadAllCookiesBlocking()) {
            val builder = Cookie.Builder()
                .name(row.name)
                .value(row.value)
                .expiresAt(row.expiresAtMs)
                .path(row.path)
            if (row.hostOnly) builder.hostOnlyDomain(row.domain) else builder.domain(row.domain)
            if (row.secure) builder.secure()
            if (row.httpOnly) builder.httpOnly()
            memory += CookieEntry(row.host, builder.build())
        }
    }

    fun saveFromResponse(url: HttpUrl, setCookieHeaders: List<String>) {
        if (setCookieHeaders.isEmpty()) return
        val parsed = setCookieHeaders.mapNotNull { Cookie.parse(url, it) }
        if (parsed.isEmpty()) return
        upsertCookies(url.host.lowercase(), parsed)
    }

    fun upsertCookies(originHost: String, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        val toPersist = mutableListOf<ForHappyDatabase.PersistedCookie>()

        for (ck in cookies) {
            memory.removeAll {
                it.cookie.name == ck.name &&
                    it.cookie.domain.equals(ck.domain, ignoreCase = true) &&
                    it.cookie.path == ck.path
            }
            val expired = ck.expiresAt <= now || ck.value.isEmpty()
            if (!expired) {
                memory += CookieEntry(originHost, ck)
            }
            val effExpiry = if (ck.persistent) ck.expiresAt else now + 30L * 86400L * 1000L
            toPersist += ForHappyDatabase.PersistedCookie(
                host = originHost,
                name = ck.name,
                value = if (expired) "" else ck.value,
                domain = ck.domain,
                path = ck.path,
                expiresAtMs = if (expired) 0L else effExpiry,
                secure = ck.secure,
                httpOnly = ck.httpOnly,
                hostOnly = ck.hostOnly,
            )
        }
        db.upsertCookiesBlocking(toPersist)
    }

    fun loadForUrl(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return memory
            .asSequence()
            .map { it.cookie }
            .filter { it.expiresAt > now && it.matches(url) }
            .toList()
    }

    fun buildCookieHeader(url: HttpUrl): String? {
        val list = loadForUrl(url)
        if (list.isEmpty()) return null
        return list.joinToString("; ") { "${it.name}=${it.value}" }
    }

    fun hasSessionCookieFor(host: String = "52dmeme.com"): Boolean {
        val now = System.currentTimeMillis()
        return memory.any {
            it.cookie.expiresAt > now &&
                (host == it.cookie.domain || host.endsWith(".${it.cookie.domain}")) &&
                it.cookie.value.isNotBlank()
        }
    }

    fun clearAll() {
        memory.clear()
        db.clearAllCookiesBlocking()
    }
}
