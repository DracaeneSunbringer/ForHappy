package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.ShortcutGestureMode
import com.jinhuan.forhappy.core.navigation.DeepLinkRouter
import com.jinhuan.forhappy.core.navigation.DeepLinkTarget
import com.jinhuan.forhappy.core.navigation.StealthExemptionTracker
import com.jinhuan.forhappy.ui.disguise.VolumeDoubleTapDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1FoundationTest {

    @Test
    fun deepLinkRouterParsesRoutesFiltersRetiredAndRejectsUntrustedHosts() {
        assertEquals(
            DeepLinkTarget.MemeDetail(42L),
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/m/42"),
        )
        assertEquals(
            DeepLinkTarget.TagFeed("猫猫"),
            DeepLinkRouter.parseDeepLinkUri("https://cc.52dmeme.com/t/%E7%8C%AB%E7%8C%AB"),
        )
        assertEquals(
            DeepLinkTarget.GroupChat(groupId = 2L, aroundMessageId = 99L),
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/chat/2?around=99"),
        )
        assertEquals(
            DeepLinkTarget.DirectMessage(threadOrUserId = 15L, isNewPeer = false),
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/dm/15"),
        )
        assertEquals(
            DeepLinkTarget.DirectMessage(threadOrUserId = 8L, isNewPeer = true),
            DeepLinkRouter.parseDeepLinkUri("https://www.52dmeme.com/dm/new/8"),
        )
        assertEquals(
            DeepLinkTarget.DiscoverSubApp("pets", "/pets", "鱼鱼谷"),
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/pets"),
        )
        assertEquals(
            DeepLinkTarget.DiscoverSubApp("novel_studio", "/novels/studio", "我的创作"),
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/novels/studio"),
        )
        assertEquals(
            DeepLinkTarget.Road,
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/road"),
        )
        assertEquals(
            DeepLinkTarget.Me,
            DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/me"),
        )

        // Retired modules always fall back safely to WallRoot
        assertEquals(DeepLinkTarget.WallRoot, DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/puzzles"))
        assertEquals(DeepLinkTarget.WallRoot, DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/huancai"))
        assertEquals(DeepLinkTarget.WallRoot, DeepLinkRouter.parseDeepLinkUri("https://52dmeme.com/nsfw"))

        // Untrusted hosts or schemes are rejected
        assertNull(DeepLinkRouter.parseDeepLinkUri("https://evil.example.com/m/42"))
        assertNull(DeepLinkRouter.parseDeepLinkUri("javascript:alert(1)"))
        assertNull(DeepLinkRouter.parseDeepLinkUri("ftp://52dmeme.com/m/42"))
        assertNull(DeepLinkRouter.parseDeepLinkUri("  "))
    }

    @Test
    fun volumeDoubleTapDetectorRequiresValidWindowAndResetsOppositeKey() {
        val detector = VolumeDoubleTapDetector(minDeltaMs = 1L, maxDeltaMs = 420L)

        // First press always passes through to OS (null)
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 1_000L))
        // Zero-delta duplicate does not fire
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 1_000L))
        // Second press within 1..420ms fires
        assertEquals(
            ShortcutGestureMode.VOL_DOUBLE_DOWN,
            detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 1_280L),
        )

        // Firing resets sequence so next tap needs a pair
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 1_400L))
        // Exceeding 420ms starts a new sequence instead of firing
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 1_821L))
        // Valid pair after reset fires
        assertEquals(
            ShortcutGestureMode.VOL_DOUBLE_DOWN,
            detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 2_241L),
        )

        // Switching between UP and DOWN resets the opposite key's pending timestamp
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 3_000L))
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_UP, 3_120L))
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_DOWN, 3_220L))

        // UP pair fires normally
        assertNull(detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_UP, 4_000L))
        assertEquals(
            ShortcutGestureMode.VOL_DOUBLE_UP,
            detector.onVolumeKey(ShortcutGestureMode.VOL_DOUBLE_UP, 4_250L),
        )

        // Non-volume modes return null
        assertNull(detector.onVolumeKey(ShortcutGestureMode.NONE, 5_000L))
        assertNull(detector.onVolumeKey(ShortcutGestureMode.SHAKE, 5_100L))
    }

    @Test
    fun stealthExemptionTrackerConsumesOnceRespectsTtlAndClear() {
        val tracker = StealthExemptionTracker(ttlMs = 180_000L)

        assertFalse(tracker.consumeShouldSuppress(1_000L))

        // Consume once within TTL
        tracker.markInternalJump(1_000L)
        assertTrue(tracker.consumeShouldSuppress(2_500L))
        assertFalse(tracker.consumeShouldSuppress(2_600L))

        // Expired TTL does not suppress
        tracker.markInternalJump(10_000L)
        assertFalse(tracker.consumeShouldSuppress(10_000L + 180_001L))

        // Explicit clear disarms
        tracker.markInternalJump(20_000L)
        tracker.clear()
        assertFalse(tracker.consumeShouldSuppress(20_100L))
    }

    @Test
    fun chatSessionControllerAggregatesGroupDmUnreadAndMentions() = runBlocking {
        var dmList = listOf(
            DmThread(id = 10L, peerId = 101L, peerName = "小欢", unread = 2),
            DmThread(id = 11L, peerId = 102L, peerName = "测试", unread = 3),
        )
        val controller = ChatSessionController(
            fetchGroupsFn = {
                listOf(
                    ChatGroup(id = 1L, name = "大厅", unread = 4, atMe = 1),
                    ChatGroup(id = 2L, name = "闲聊", unread = 1, atMe = 0),
                )
            },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse() },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
            fetchDmThreadsFn = { dmList },
        )

        controller.refreshGroups()
        val stateAfterRefresh = controller.uiState.value
        assertEquals(5, stateAfterRefresh.groupUnreadCount)
        assertEquals(5, stateAfterRefresh.dmUnreadCount)
        assertEquals(10, stateAfterRefresh.totalUnreadCount)
        assertEquals(1, stateAfterRefresh.totalAtMeCount)
        assertTrue(stateAfterRefresh.hasUnreadMention)

        // Updating DMs locally updates totalUnread immediately without losing groups or mentions
        dmList = listOf(DmThread(id = 10L, peerId = 101L, peerName = "小欢", unread = 1))
        controller.updateDmThreadsLocal(dmList)
        val stateAfterLocal = controller.uiState.value
        assertEquals(5, stateAfterLocal.groupUnreadCount)
        assertEquals(1, stateAfterLocal.dmUnreadCount)
        assertEquals(6, stateAfterLocal.totalUnreadCount)
        assertTrue(stateAfterLocal.hasUnreadMention)
    }

    @Test
    fun privacyStealthConfigPersistsBossReturnAndCovertExitFields() {
        val cfg = PrivacyStealthConfig(
            disguiseMode = AppDisguiseMode.FEISHU,
            isBossPersisted = true,
            bossReturnDisguise = AppDisguiseMode.LINUX_DO,
            keepBlurAfterBossExit = false,
            bossExitLongPressTitle = true,
            bossExitTapCollabFive = true,
            stealthHapticFeedback = false,
            feishuMemberAliasEnabled = false,
        )
        val json = ForHappyJson.encodeToString(PrivacyStealthConfig.serializer(), cfg)
        val decoded = ForHappyJson.decodeFromString(PrivacyStealthConfig.serializer(), json)
        assertEquals(cfg, decoded)
    }
}
