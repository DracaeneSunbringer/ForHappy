package com.jinhuan.forhappy.core.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ForHappyJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

/**
 * Lightweight SQLiteOpenHelper database storing:
 * - Key-value app preferences & dynamic module registry
 * - Persisted Cloudflare EdgePin `select_state`
 * - Host-scoped HTTP cookies
 * - Sticker extension resolution cache (`[pack:name]` -> `.png`/`.gif`/`.webp`)
 * - Cached chat messages and offline inflight message queue
 * - Web asset cache index (`asset_cache_meta`)
 */
class ForHappyDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "forhappy.db"
        private const val DB_VERSION = 3

        private const val TBL_KV = "kv_store"
        private const val TBL_COOKIES = "cookie_store"
        private const val TBL_STICKER_EXT = "sticker_ext_index"
        private const val TBL_CHAT_MESSAGES = "chat_messages"
        private const val TBL_ASSET_META = "web_asset_meta"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_KV (
                k TEXT PRIMARY KEY,
                v TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_COOKIES (
                host TEXT NOT NULL,
                name TEXT NOT NULL,
                value TEXT NOT NULL,
                domain TEXT NOT NULL,
                path TEXT NOT NULL,
                expires_at_ms INTEGER NOT NULL,
                secure INTEGER NOT NULL,
                http_only INTEGER NOT NULL,
                host_only INTEGER NOT NULL,
                PRIMARY KEY (host, domain, path, name)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_STICKER_EXT (
                sticker_key TEXT PRIMARY KEY,
                ext TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_CHAT_MESSAGES (
                group_id INTEGER NOT NULL,
                msg_key TEXT NOT NULL,
                server_id INTEGER NOT NULL,
                local_id TEXT,
                delivery_state TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                json TEXT NOT NULL,
                PRIMARY KEY (group_id, msg_key)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_chat_gid_srv
            ON $TBL_CHAT_MESSAGES (group_id, server_id DESC, created_at DESC)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_ASSET_META (
                cache_key TEXT PRIMARY KEY,
                url TEXT NOT NULL,
                mime TEXT NOT NULL,
                size_bytes INTEGER NOT NULL,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Purge cached chat windows persisted with reversed-limit / out-of-window `changed` gaps.
            db.execSQL("DELETE FROM $TBL_CHAT_MESSAGES")
        }
        if (oldVersion < 3) {
            // Purge sticker extensions potentially poisoned by SPA HTTP 200 text/html responses.
            db.execSQL("DELETE FROM $TBL_STICKER_EXT")
        }
    }

    suspend fun getKv(key: String): String? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT v FROM $TBL_KV WHERE k = ?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    suspend fun putKv(key: String, value: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("k", key)
            put("v", value)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(TBL_KV, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    fun getKvBlocking(key: String): String? =
        readableDatabase.rawQuery("SELECT v FROM $TBL_KV WHERE k = ?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    fun putKvBlocking(key: String, value: String) {
        val cv = ContentValues().apply {
            put("k", key)
            put("v", value)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(TBL_KV, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getStickerExt(pack: String, name: String): String? = withContext(Dispatchers.IO) {
        val key = "$pack:$name"
        readableDatabase.rawQuery("SELECT ext FROM $TBL_STICKER_EXT WHERE sticker_key = ?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    suspend fun putStickerExt(pack: String, name: String, ext: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("sticker_key", "$pack:$name")
            put("ext", ext)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(TBL_STICKER_EXT, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    suspend fun deleteStickerExt(pack: String, name: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TBL_STICKER_EXT, "sticker_key = ?", arrayOf("$pack:$name"))
        Unit
    }

    suspend fun saveChatMessages(groupId: Long, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            upsertChatMessagesLocked(db, groupId, messages)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun replaceGroupServerMessages(groupId: Long, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TBL_CHAT_MESSAGES, "group_id = ? AND server_id > 0", arrayOf(groupId.toString()))
            if (messages.isNotEmpty()) {
                upsertChatMessagesLocked(db, groupId, messages)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun upsertChatMessagesLocked(db: SQLiteDatabase, groupId: Long, messages: List<ChatMessage>) {
        for (m in messages) {
            val msgKey = if (m.id > 0L) "srv:${m.id}" else "loc:${m.localId ?: System.nanoTime()}"
            val cv = ContentValues().apply {
                put("group_id", groupId)
                put("msg_key", msgKey)
                put("server_id", m.id)
                put("local_id", m.localId)
                put("delivery_state", m.deliveryState)
                put("created_at", m.createdAt)
                put("json", ForHappyJson.encodeToString(m))
            }
            db.insertWithOnConflict(TBL_CHAT_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
        // Trim cached rows per group to latest 400
        db.execSQL(
            """
            DELETE FROM $TBL_CHAT_MESSAGES
            WHERE group_id = ? AND msg_key NOT IN (
                SELECT msg_key FROM $TBL_CHAT_MESSAGES
                WHERE group_id = ?
                ORDER BY CASE WHEN server_id > 0 THEN server_id ELSE 9223372036854775807 END DESC,
                         created_at DESC
                LIMIT 400
            )
            """.trimIndent(),
            arrayOf(groupId.toString(), groupId.toString()),
        )
    }

    suspend fun deleteLocalChatMessage(groupId: Long, localId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(
            TBL_CHAT_MESSAGES,
            "group_id = ? AND local_id = ?",
            arrayOf(groupId.toString(), localId),
        )
        Unit
    }

    suspend fun loadChatMessages(groupId: Long, limit: Int = 200): List<ChatMessage> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ChatMessage>()
        readableDatabase.rawQuery(
            """
            SELECT json FROM (
                SELECT json, server_id, created_at FROM $TBL_CHAT_MESSAGES
                WHERE group_id = ?
                ORDER BY CASE WHEN server_id > 0 THEN server_id ELSE 9223372036854775807 END DESC,
                         created_at DESC
                LIMIT ?
            )
            ORDER BY CASE WHEN server_id > 0 THEN server_id ELSE 9223372036854775807 END ASC,
                     created_at ASC
            """.trimIndent(),
            arrayOf(groupId.toString(), limit.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                runCatching { ForHappyJson.decodeFromString<ChatMessage>(c.getString(0)) }
                    .onSuccess { list += it }
            }
        }
        list
    }

    suspend fun recordAssetCacheMeta(cacheKey: String, url: String, mime: String, sizeBytes: Long) =
        withContext(Dispatchers.IO) {
            val cv = ContentValues().apply {
                put("cache_key", cacheKey)
                put("url", url)
                put("mime", mime)
                put("size_bytes", sizeBytes)
                put("cached_at", System.currentTimeMillis())
            }
            writableDatabase.insertWithOnConflict(TBL_ASSET_META, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            Unit
        }

    suspend fun clearAssetCacheMeta() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TBL_ASSET_META, null, null)
        Unit
    }

    data class PersistedCookie(
        val host: String,
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val expiresAtMs: Long,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean,
    )

    fun loadAllCookiesBlocking(): List<PersistedCookie> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<PersistedCookie>()
        readableDatabase.rawQuery(
            "SELECT host, name, value, domain, path, expires_at_ms, secure, http_only, host_only FROM $TBL_COOKIES WHERE expires_at_ms > ?",
            arrayOf(now.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                list += PersistedCookie(
                    host = c.getString(0),
                    name = c.getString(1),
                    value = c.getString(2),
                    domain = c.getString(3),
                    path = c.getString(4),
                    expiresAtMs = c.getLong(5),
                    secure = c.getInt(6) != 0,
                    httpOnly = c.getInt(7) != 0,
                    hostOnly = c.getInt(8) != 0,
                )
            }
        }
        return list
    }

    fun upsertCookiesBlocking(cookies: List<PersistedCookie>) {
        if (cookies.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (ck in cookies) {
                if (ck.expiresAtMs <= System.currentTimeMillis() || ck.value.isEmpty()) {
                    db.delete(
                        TBL_COOKIES,
                        "host = ? AND domain = ? AND path = ? AND name = ?",
                        arrayOf(ck.host, ck.domain, ck.path, ck.name),
                    )
                } else {
                    val cv = ContentValues().apply {
                        put("host", ck.host)
                        put("name", ck.name)
                        put("value", ck.value)
                        put("domain", ck.domain)
                        put("path", ck.path)
                        put("expires_at_ms", ck.expiresAtMs)
                        put("secure", if (ck.secure) 1 else 0)
                        put("http_only", if (ck.httpOnly) 1 else 0)
                        put("host_only", if (ck.hostOnly) 1 else 0)
                    }
                    db.insertWithOnConflict(TBL_COOKIES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAllCookiesBlocking() {
        writableDatabase.delete(TBL_COOKIES, null, null)
    }
}
