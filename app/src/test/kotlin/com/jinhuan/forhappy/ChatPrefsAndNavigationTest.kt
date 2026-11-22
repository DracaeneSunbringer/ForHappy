package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatGroupsResponse
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatScrollPolicy
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.UnifiedSessionItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPrefsAndNavigationTest {

    private fun newController(
        groups: List<ChatGroup> = listOf(ChatGroup(id = 1L, name = "大厅", unread = 3, atMe = 0, joined = true)),
        dmThreads: List<DmThread> = listOf(DmThread(id = 10L, peerId = 101L, peerName = "虹夏", unread = 4)),
        serverDmUnread: Int = 4,
    ): ChatSessionController {
        return ChatSessionController(
            fetchGroupsFn = { groups },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse() },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
            fetchDmThreadsFn = { dmThreads },
            fetchGroupsEnvelopeFn = { ChatGroupsResponse(groups = groups, dmUnread = serverDmUnread) },
        )
    }

    @Test
    fun defaultPreferencesKeepDmEnabledAndSwipeSwitchDisabled() {
        val controller = newController()
        val state = controller.uiState.value
        assertTrue(state.dmEnabled)
        assertFalse(state.swipeSwitchGroups)
    }

    @Test
    fun disablingDmHidesUnreadCountsAndFiltersUnifiedConversationList() = runBlocking {
        val controller = newController()
        controller.refreshGroups()

        assertEquals(3, controller.uiState.value.groupUnreadCount)
        assertEquals(4, controller.uiState.value.dmUnreadCount)
        assertEquals(7, controller.uiState.value.totalUnreadCount)
        assertTrue(controller.buildUnifiedConversationItems("all").any { it is UnifiedSessionItem.Dm })

        controller.setDmEnabled(false)

        val disabledState = controller.uiState.value
        assertFalse(disabledState.dmEnabled)
        assertEquals(0, disabledState.dmUnreadCount)
        assertEquals(disabledState.groupUnreadCount, disabledState.totalUnreadCount)
        assertFalse(controller.buildUnifiedConversationItems("all").any { it is UnifiedSessionItem.Dm })
        assertFalse(controller.buildUnifiedConversationItems("unread").any { it is UnifiedSessionItem.Dm })
        assertEquals(1, controller.buildUnifiedConversationItems("all").size)

        controller.setDmEnabled(true)

        val restoredState = controller.uiState.value
        assertTrue(restoredState.dmEnabled)
        assertEquals(4, restoredState.dmUnreadCount)
        assertEquals(7, restoredState.totalUnreadCount)
        assertTrue(controller.buildUnifiedConversationItems("all").any { it is UnifiedSessionItem.Dm })
    }

    @Test
    fun togglingSwipeSwitchGroupsUpdatesUiStateImmediately() {
        val controller = newController()
        assertFalse(controller.uiState.value.swipeSwitchGroups)
        controller.setSwipeSwitchGroups(true)
        assertTrue(controller.uiState.value.swipeSwitchGroups)
        controller.setSwipeSwitchGroups(false)
        assertFalse(controller.uiState.value.swipeSwitchGroups)
    }

    @Test
    fun shouldBypassSessionListFollowsDmToggleGroupCountAndSwipeSettingTruthTable() {
        val oneJoined = listOf(ChatGroup(id = 1L, name = "大厅", joined = true))
        val twoJoined = listOf(
            ChatGroup(id = 1L, name = "大厅", joined = true),
            ChatGroup(id = 2L, name = "虹夏后援", joined = true),
        )

        // dmEnabled == true never bypasses session list
        assertFalse(ChatSessionController.ChatUiState(groups = oneJoined, dmEnabled = true, swipeSwitchGroups = false).shouldBypassSessionList())
        assertFalse(ChatSessionController.ChatUiState(groups = oneJoined, dmEnabled = true, swipeSwitchGroups = true).shouldBypassSessionList())
        assertFalse(ChatSessionController.ChatUiState(groups = twoJoined, dmEnabled = true, swipeSwitchGroups = false).shouldBypassSessionList())
        assertFalse(ChatSessionController.ChatUiState(groups = twoJoined, dmEnabled = true, swipeSwitchGroups = true).shouldBypassSessionList())

        // dmEnabled == false with 0 joined groups does not bypass (lets user see joinable groups)
        assertFalse(ChatSessionController.ChatUiState(groups = emptyList(), dmEnabled = false, swipeSwitchGroups = false).shouldBypassSessionList())
        assertFalse(ChatSessionController.ChatUiState(groups = emptyList(), dmEnabled = false, swipeSwitchGroups = true).shouldBypassSessionList())

        // dmEnabled == false with 1 joined group bypasses session list regardless of swipeSwitchGroups
        assertTrue(ChatSessionController.ChatUiState(groups = oneJoined, dmEnabled = false, swipeSwitchGroups = false).shouldBypassSessionList())
        assertTrue(ChatSessionController.ChatUiState(groups = oneJoined, dmEnabled = false, swipeSwitchGroups = true).shouldBypassSessionList())

        // dmEnabled == false with 2 joined groups bypasses ONLY when swipeSwitchGroups == true
        assertFalse(ChatSessionController.ChatUiState(groups = twoJoined, dmEnabled = false, swipeSwitchGroups = false).shouldBypassSessionList())
        assertTrue(ChatSessionController.ChatUiState(groups = twoJoined, dmEnabled = false, swipeSwitchGroups = true).shouldBypassSessionList())

        // Direct embedded surfaces always return false for bypassSessionList
        assertFalse(ChatSessionController.ChatUiState(groups = oneJoined, dmEnabled = false, swipeSwitchGroups = true).shouldBypassSessionList(isDirectEmbedded = true))
    }

    @Test
    fun chatScrollPolicyFromKeyHandlesLegacyAndCanonicalKeys() {
        assertEquals(ChatScrollPolicy.AUTO_FOLLOW, ChatScrollPolicy.fromKey("auto"))
        assertEquals(ChatScrollPolicy.AUTO_FOLLOW, ChatScrollPolicy.fromKey("ALWAYS"))
        assertEquals(ChatScrollPolicy.MANUAL, ChatScrollPolicy.fromKey("manual"))
        assertEquals(ChatScrollPolicy.MANUAL, ChatScrollPolicy.fromKey("NEVER"))
        assertEquals(ChatScrollPolicy.SMART, ChatScrollPolicy.fromKey("smart"))
        assertEquals(ChatScrollPolicy.SMART, ChatScrollPolicy.fromKey("SMART"))
        assertEquals(ChatScrollPolicy.SMART, ChatScrollPolicy.fromKey(null))
    }
}
