package com.jinhuan.forhappy

import android.app.Application
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.cookie.PersistentCookieJar
import com.jinhuan.forhappy.core.network.rust.RustNetManager
import com.jinhuan.forhappy.core.update.AppUpdateController

/**
 * Application root container owning the SQLite store, CookieJar, Rust ECH+H2+EdgePin engine,
 * unified API client, update controller, and chat session poller.
 */
class ForHappyApp : Application() {
    lateinit var db: ForHappyDatabase
        private set
    lateinit var cookieJar: PersistentCookieJar
        private set
    lateinit var rustNetManager: RustNetManager
        private set
    lateinit var apiClient: ForHappyApiClient
        private set
    lateinit var chatController: ChatSessionController
        private set
    lateinit var updateController: AppUpdateController
        private set

    override fun onCreate() {
        super.onCreate()
        db = ForHappyDatabase(this)
        cookieJar = PersistentCookieJar(db)
        rustNetManager = RustNetManager(this, db).also { it.start() }
        updateController = AppUpdateController(this)
        apiClient = ForHappyApiClient(
            context = this,
            db = db,
            rustNetManager = rustNetManager,
            cookieJar = cookieJar,
        )
        chatController = ChatSessionController(
            db = db,
            fetchGroupsFn = { apiClient.fetchChatGroups() },
            fetchMessagesFn = { gid, afterId, sinceSec, beforeId ->
                apiClient.fetchChatMessages(gid, afterId, sinceSec, beforeId)
            },
            sendMessageFn = { gid, body, replyToId, retry ->
                apiClient.sendChatMessage(gid, body, replyToId, retry)
            },
            markReadFn = { gid, lastId -> apiClient.markChatRead(gid, lastId) },
            toggleReactionFn = { msgId, emoji -> apiClient.toggleChatReaction(msgId, emoji) },
            editMessageFn = { msgId, body -> apiClient.editChatMessage(msgId, body) },
            deleteMessageFn = { msgId -> apiClient.deleteChatMessage(msgId) },
            postBoostFn = { msgId, text -> apiClient.postChatBoost(msgId, text) },
            postReactionMsgFn = { msgId, emoji -> apiClient.postChatReaction(msgId, emoji) },
        )
    }

    override fun onTerminate() {
        rustNetManager.stop()
        super.onTerminate()
    }
}
