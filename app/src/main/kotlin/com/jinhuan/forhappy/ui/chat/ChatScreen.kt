package com.jinhuan.forhappy.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.BOOST_MAX_LEN
import com.jinhuan.forhappy.core.model.BOOST_PRESETS
import com.jinhuan.forhappy.core.model.ChatBoost
import com.jinhuan.forhappy.core.model.ChatMedia
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatReplyItem
import com.jinhuan.forhappy.core.model.DmMessage
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.PICKED_REACTIONS
import com.jinhuan.forhappy.core.model.RedpackMeta
import com.jinhuan.forhappy.core.model.StickerEntry
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.countReplyUnits
import com.jinhuan.forhappy.core.model.parseRedpackMeta
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.ReactionEmojiView
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.components.boostPresetAccent
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ChatNotificationJump(
    val groupId: Long,
    val messageId: Long? = null,
    val triggerSeq: Long = 0L,
)

/**
 * Group chat + 1:1 Direct Message screen driven by HTTP cursor polling ([ChatSessionController]).
 * - Compact single-line header maximizing viewport ratio for message content.
 * - Full unread synchronization (`POST /api/chat/groups/{gid}/read` & `PUT /api/dm/threads/{id}/read`).
 * - Consecutive-message collapsing, unclipped multi-line system notices, rich redpack/guess cards,
 *   native `GuessRoomSheet`, message search + history around jump view, and stable scroll anchoring.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    api: ForHappyApiClient,
    controller: ChatSessionController,
    me: MeUser?,
    initialJump: ChatNotificationJump? = null,
    onConsumeInitialJump: () -> Unit = {},
    onNavigateToMeme: (Long) -> Unit,
    onNavigateToTag: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRequireLogin: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val state by controller.uiState.collectAsState()
    val isImeVisible = WindowInsets.isImeVisible

    var channelMode by remember { mutableStateOf("group") } // "group" | "dm"
    var draft by remember { mutableStateOf("") }
    var showStickerTray by remember { mutableStateOf(false) }
    var stickerPacks by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var savedStickers by remember { mutableStateOf<List<StickerEntry>>(emptyList()) }
    var activePackKey by remember { mutableStateOf<String?>(null) }
    var fullscreenImg by remember { mutableStateOf<String?>(null) }
    var fullscreenVideoPair by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Message action / boost / replies / inline post modal state
    var actionSheetMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var boostComposerMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var repliesSheetMsgId by remember { mutableStateOf<Long?>(null) }
    var inlinePostMemeId by remember { mutableStateOf<Long?>(null) }

    // Redpack / Guess room state
    var redpackDialogMsgId by remember { mutableStateOf<Long?>(null) }
    var redpackFallbackMeta by remember { mutableStateOf<RedpackMeta?>(null) }
    var redpackSenderName by remember { mutableStateOf<String?>(null) }
    var showGuessSheet by remember { mutableStateOf(false) }
    var guessInitialMid by remember { mutableStateOf<Long?>(null) }

    // Search & history context layer state
    var showSearchSheet by remember { mutableStateOf(false) }
    var historyMsgs by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var historyHasOlder by remember { mutableStateOf(false) }
    var historyHasNewer by remember { mutableStateOf(false) }
    var highlightedMsgId by remember { mutableStateOf<Long?>(null) }

    // DM state
    var dmThreads by remember { mutableStateOf<List<DmThread>>(emptyList()) }
    var activeDmId by remember { mutableStateOf<Long?>(null) }
    var dmMessages by remember { mutableStateOf<List<DmMessage>>(emptyList()) }
    val dmListState = rememberLazyListState()

    val listState = rememberLazyListState()
    val historyListState = rememberLazyListState()

    var didInitialScrollGroupId by remember { mutableStateOf<Long?>(null) }
    var lastObservedTailId by remember { mutableStateOf(0L) }
    var lastObservedLocalCount by remember { mutableIntStateOf(0) }
    var pendingNewCount by remember { mutableIntStateOf(0) }
    var pendingPrependAnchorId by remember { mutableStateOf<Long?>(null) }
    var wasNearBottomBeforeIme by remember { mutableStateOf(true) }

    val isNearBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) {
                true
            } else {
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 3
            }
        }
    }

    val activeRedpackMsg = remember(state.messages) {
        state.messages.lastOrNull { it.redpack != null && !it.deleted }
    }
    val guessSignalHash = remember(state.messages) {
        state.messages.count { m ->
            val meta = m.parseRedpackMeta()
            meta?.isGuessGame == true || m.systemKind in setOf("ldc_winner", "ldc_cancel", "ldc_expired", "guess_level")
        }
    }

    DisposableEffect(Unit) {
        controller.startPolling()
        onDispose {
            controller.isChatForegroundAtBottom = false
            controller.stopPolling()
        }
    }

    // Keep controller informed whether user is actively viewing the bottom of this group
    LaunchedEffect(
        channelMode,
        isNearBottom,
        fullscreenImg,
        fullscreenVideoPair,
        showSearchSheet,
        historyMsgs,
        showGuessSheet,
        redpackDialogMsgId,
        inlinePostMemeId,
        actionSheetMsg,
        boostComposerMsg,
        repliesSheetMsgId,
    ) {
        val foregroundBottom = channelMode == "group" &&
            isNearBottom &&
            fullscreenImg == null &&
            fullscreenVideoPair == null &&
            !showSearchSheet &&
            historyMsgs == null &&
            !showGuessSheet &&
            redpackDialogMsgId == null &&
            inlinePostMemeId == null &&
            actionSheetMsg == null &&
            boostComposerMsg == null &&
            repliesSheetMsgId == null
        controller.isChatForegroundAtBottom = foregroundBottom
        if (foregroundBottom && state.messages.isNotEmpty()) {
            pendingNewCount = 0
            controller.acknowledgeGroupRead(state.activeGroupId)
        }
    }

    // Reset per-group scroll markers when switching groups
    LaunchedEffect(state.activeGroupId) {
        historyMsgs = null
        pendingNewCount = 0
        pendingPrependAnchorId = null
        lastObservedTailId = 0L
        lastObservedLocalCount = 0
        didInitialScrollGroupId = null
        controller.acknowledgeGroupRead(state.activeGroupId)
    }

    // Wait until LazyColumn has recomposed with newly prepended older messages, then anchor viewport
    LaunchedEffect(state.messages, state.hasMoreOlder, pendingPrependAnchorId) {
        val anchorId = pendingPrependAnchorId ?: return@LaunchedEffect
        val idx = state.messages.indexOfFirst { it.id == anchorId }
        if (idx >= 0) {
            val expectedTotal = (if (state.hasMoreOlder) 1 else 0) + state.messages.size
            var frames = 0
            while (listState.layoutInfo.totalItemsCount < expectedTotal && frames < 15) {
                withFrameNanos { }
                frames++
            }
            val targetIdx = (if (state.hasMoreOlder) 1 else 0) + idx
            listState.scrollToItem(targetIdx, 0)
            pendingPrependAnchorId = null
        }
    }

    // Scroll to bottom ONCE when entering a group and receiving initial messages
    LaunchedEffect(state.activeGroupId, state.messages.size) {
        if (channelMode == "group" &&
            initialJump?.messageId == null &&
            state.messages.isNotEmpty() &&
            didInitialScrollGroupId != state.activeGroupId
        ) {
            didInitialScrollGroupId = state.activeGroupId
            val lastIdx = (if (state.hasMoreOlder) 1 else 0) + state.messages.size - 1
            if (lastIdx >= 0) {
                listState.scrollToItem(lastIdx)
            }
            lastObservedTailId = state.messages.lastOrNull { it.id > 0L }?.id ?: 0L
            lastObservedLocalCount = state.messages.count { it.id <= 0L }
            controller.acknowledgeGroupRead(state.activeGroupId, lastObservedTailId)
        }
    }

    // Smart tail tracking: ONLY scroll on OWN newly sent message or when ALREADY at bottom & not in viewer
    val newestServerId = remember(state.messages) { state.messages.lastOrNull { it.id > 0L }?.id ?: 0L }
    val localMsgCount = remember(state.messages) { state.messages.count { it.id <= 0L } }
    LaunchedEffect(newestServerId, localMsgCount) {
        if (channelMode != "group" ||
            didInitialScrollGroupId != state.activeGroupId ||
            state.messages.isEmpty() ||
            state.isLoadingHistory ||
            pendingPrependAnchorId != null
        ) {
            return@LaunchedEffect
        }

        val sentOwnMessage = localMsgCount > lastObservedLocalCount
        val arrivedNewServerMsg = newestServerId > lastObservedTailId && lastObservedTailId > 0L
        if (sentOwnMessage) {
            val lastIdx = (if (state.hasMoreOlder) 1 else 0) + state.messages.size - 1
            if (lastIdx >= 0) listState.animateScrollToItem(lastIdx)
            pendingNewCount = 0
        } else if (arrivedNewServerMsg) {
            if (isNearBottom &&
                fullscreenImg == null &&
                fullscreenVideoPair == null &&
                inlinePostMemeId == null &&
                historyMsgs == null &&
                !showSearchSheet
            ) {
                val lastIdx = (if (state.hasMoreOlder) 1 else 0) + state.messages.size - 1
                if (lastIdx >= 0) listState.animateScrollToItem(lastIdx)
                pendingNewCount = 0
                controller.acknowledgeGroupRead(state.activeGroupId, newestServerId)
            } else {
                val newly = state.messages.count { it.id in (lastObservedTailId + 1)..newestServerId && !it.mine && !it.deleted }
                if (newly > 0) pendingNewCount += newly
            }
        }
        lastObservedTailId = newestServerId
        lastObservedLocalCount = localMsgCount
    }

    LaunchedEffect(isNearBottom, isImeVisible) {
        if (!isImeVisible) {
            wasNearBottomBeforeIme = isNearBottom
        }
    }

    // When IME opens while user was near bottom, scroll right as IME starts and again once Gboard finishes animating
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && channelMode == "group" && wasNearBottomBeforeIme && fullscreenImg == null && state.messages.isNotEmpty()) {
            delay(80L)
            val lastIdx = (if (state.hasMoreOlder) 1 else 0) + state.messages.size - 1
            if (lastIdx >= 0) listState.scrollToItem(lastIdx)
            delay(200L)
            if (lastIdx >= 0) listState.scrollToItem(lastIdx)
        }
    }

    // Clear highlight pulse after 2.6 seconds
    LaunchedEffect(highlightedMsgId) {
        if (highlightedMsgId != null) {
            delay(2600L)
            highlightedMsgId = null
        }
    }

    LaunchedEffect(state.editTarget) {
        state.editTarget?.let { draft = it.body }
    }

    LaunchedEffect(showStickerTray) {
        if (showStickerTray && stickerPacks.isEmpty() && savedStickers.isEmpty()) {
            val packsDef = async { api.fetchStickerPacks().filter { !it.hidden && it.items.isNotEmpty() } }
            val favDef = async { api.fetchStickers() }
            stickerPacks = packsDef.await()
            savedStickers = favDef.await()
            activePackKey = stickerPacks.firstOrNull()?.tabKey ?: if (savedStickers.isNotEmpty()) "__fav__" else null
        }
    }

    // Load DM threads proactively on entering chat so unread count stays accurate, and load messages + mark read when DM selected
    LaunchedEffect(me?.id) {
        if (me != null) {
            dmThreads = api.fetchDmThreads()
        }
    }

    LaunchedEffect(channelMode, activeDmId) {
        if (channelMode == "dm" && me != null) {
            if (dmThreads.isEmpty()) dmThreads = api.fetchDmThreads()
            val id = activeDmId ?: dmThreads.firstOrNull()?.id
            activeDmId = id
            if (id != null) {
                val fetched = api.fetchDmMessages(id)
                dmMessages = fetched
                val newestDm = fetched.lastOrNull { it.id > 0L }?.id ?: 0L
                if (newestDm > 0L) {
                    if (api.markDmRead(id, newestDm)) {
                        dmThreads = dmThreads.map { if (it.id == id) it.copy(unread = 0) else it }
                    }
                    dmListState.scrollToItem((fetched.size - 1).coerceAtLeast(0))
                }
            }
        }
    }

    fun jumpToMessage(targetId: Long, groupId: Long = state.activeGroupId) {
        if (targetId <= 0L) return
        val localIdx = if (groupId == state.activeGroupId) {
            state.messages.indexOfFirst { it.id == targetId }
        } else {
            -1
        }
        if (localIdx >= 0) {
            historyMsgs = null
            highlightedMsgId = targetId
            val lazyIdx = (if (state.hasMoreOlder) 1 else 0) + localIdx
            scope.launch { listState.animateScrollToItem(lazyIdx) }
            return
        }
        scope.launch {
            val resp = api.fetchChatMessagesAround(groupId, targetId)
            if (resp != null && resp.items.isNotEmpty()) {
                historyMsgs = resp.items
                historyHasOlder = resp.more
                historyHasNewer = resp.moreAfter
                highlightedMsgId = targetId
                val idxInHist = resp.items.indexOfFirst { it.id == targetId }
                if (idxInHist >= 0) {
                    val lazyIdx = (if (resp.more) 1 else 0) + idxInHist
                    delay(40L)
                    historyListState.scrollToItem(lazyIdx)
                }
            }
        }
    }

    LaunchedEffect(initialJump?.triggerSeq) {
        val jump = initialJump ?: return@LaunchedEffect
        channelMode = "group"
        showSearchSheet = false
        historyMsgs = null
        val targetGroupId = if (jump.groupId > 0L) jump.groupId else state.activeGroupId
        if (jump.groupId > 0L && state.activeGroupId != jump.groupId) {
            controller.setActiveGroup(jump.groupId)
            var waited = 0L
            while (waited < 1600L) {
                if (state.activeGroupId == jump.groupId && state.messages.isNotEmpty()) break
                delay(40L)
                waited += 40L
            }
            withFrameNanos { }
        }
        val targetMsgId = jump.messageId
        if (targetMsgId != null && targetMsgId > 0L) {
            didInitialScrollGroupId = targetGroupId
            jumpToMessage(targetMsgId, targetGroupId)
        }
        onConsumeInitialJump()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        // Single sleek compact ~42dp chat header maximizing message area
        Surface(color = palette.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(state.groups, key = { "grp_${it.id}" }) { g ->
                        val selected = channelMode == "group" && g.id == state.activeGroupId
                        val label = if (g.unread > 0) "${g.name} (${g.unread})" else g.name
                        ChatHeaderPill(
                            label = label,
                            selected = selected,
                            onClick = {
                                channelMode = "group"
                                historyMsgs = null
                                controller.setActiveGroup(g.id)
                            },
                        )
                    }
                    item {
                        val dmUnread = dmThreads.sumOf { it.unread }
                        ChatHeaderPill(
                            label = if (dmUnread > 0) "私信 ($dmUnread)" else "私信",
                            selected = channelMode == "dm",
                            onClick = {
                                channelMode = "dm"
                                historyMsgs = null
                            },
                        )
                    }
                }

                if (channelMode == "group") {
                    // Search button
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { showSearchSheet = true },
                    ) {
                        Box(modifier = Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, contentDescription = "搜索消息", tint = palette.text, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Guess Room button
                    Surface(
                        color = if (activeRedpackMsg?.parseRedpackMeta()?.isGuessGame == true) {
                            palette.gold.copy(alpha = 0.2f)
                        } else {
                            palette.surface2
                        },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable {
                            guessInitialMid = activeRedpackMsg?.takeIf { it.parseRedpackMeta()?.isGuessGame == true }?.id
                            showGuessSheet = true
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = "欢欢猜词屋",
                                tint = palette.gold,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "猜词",
                                color = palette.gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Active non-guess redpack shortcut icon when present
                    if (activeRedpackMsg != null && activeRedpackMsg.parseRedpackMeta()?.isGuessGame != true) {
                        Surface(
                            color = Color(0xFFC84B31).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable {
                                redpackFallbackMeta = activeRedpackMsg.parseRedpackMeta()
                                redpackSenderName = activeRedpackMsg.displayAuthor
                                redpackDialogMsgId = activeRedpackMsg.id
                            },
                        ) {
                            Box(modifier = Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.CardGiftcard,
                                    contentDescription = "最新红包",
                                    tint = Color(0xFFC84B31),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Secondary DM peer strip only when viewing DMs
        if (channelMode == "dm" && dmThreads.isNotEmpty()) {
            Surface(color = palette.surface.copy(alpha = 0.92f)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(dmThreads, key = { it.id }) { thread ->
                        val label = (thread.peerName ?: thread.peerUsername ?: "私信") +
                            if (thread.unread > 0) " (${thread.unread})" else ""
                        ChatHeaderPill(
                            label = label,
                            selected = thread.id == activeDmId,
                            onClick = { activeDmId = thread.id },
                        )
                    }
                }
            }
        }

        // Message Viewport (Group or History Around Overlay or DM)
        if (channelMode == "group") {
            val activeHistory = historyMsgs
            if (activeHistory != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = palette.gold.copy(alpha = 0.12f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("正在查看搜索/引用定位消息", color = palette.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Surface(
                                color = palette.primary,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.clickable { historyMsgs = null },
                            ) {
                                Text(
                                    text = "回到最新聊天",
                                    color = palette.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    LazyColumn(
                        state = historyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (historyHasOlder) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    TextButton(onClick = {
                                        val edge = activeHistory.firstOrNull()?.id ?: return@TextButton
                                        scope.launch {
                                            val d = api.fetchChatMessagesAround(state.activeGroupId, edge)
                                            if (d != null) {
                                                val have = activeHistory.map { it.id }.toHashSet()
                                                val older = d.items.filter { it.id < edge && !have.contains(it.id) }
                                                historyMsgs = older + activeHistory
                                                historyHasOlder = d.more
                                                if (older.isNotEmpty()) historyListState.scrollToItem(older.size + 1)
                                            }
                                        }
                                    }) {
                                        Text("↑ 更早消息")
                                    }
                                }
                            }
                        }

                        itemsIndexed(activeHistory, key = { _, m -> "hist_${m.id}" }) { idx, msg ->
                            val prev = if (idx > 0) activeHistory[idx - 1] else null
                            val cont = canGroupContinuation(prev, msg)
                            if (!msg.systemKind.isNullOrBlank()) {
                                SystemNoticeRow(
                                    msg = msg,
                                    onActionRedpack = { refId ->
                                        redpackFallbackMeta = null
                                        redpackSenderName = null
                                        redpackDialogMsgId = refId
                                    },
                                    onActionGuess = { refId ->
                                        guessInitialMid = refId
                                        showGuessSheet = true
                                    },
                                    onActionRaffle = { refId ->
                                        scope.launch { api.joinActiveTitleRaffle(refId) }
                                    },
                                    onJumpMessage = { jumpToMessage(it) },
                                )
                            } else {
                                SwipeableChatRow(
                                    msg = msg,
                                    isContinuation = cont,
                                    highlighted = highlightedMsgId == msg.id,
                                    api = api,
                                    onSwipeReply = { controller.setReplyTarget(msg); historyMsgs = null },
                                    onOpenActionSheet = { actionSheetMsg = msg },
                                    onOpenBoostComposer = {
                                        if (me == null) onRequireLogin() else boostComposerMsg = msg
                                    },
                                    onQuickBoost = { txt ->
                                        if (me == null) onRequireLogin() else scope.launch { controller.sendBoost(msg.id, txt) }
                                    },
                                    onReaction = { emoji ->
                                        if (me == null) onRequireLogin() else scope.launch { controller.toggleReaction(msg.id, emoji) }
                                    },
                                    onRetrySend = {},
                                    onOpenImage = { url -> fullscreenImg = url },
                                    onOpenVideo = { poster, vid -> fullscreenVideoPair = poster to vid },
                                    onOpenRedpack = {
                                        val meta = msg.parseRedpackMeta()
                                        if (meta?.isGuessGame == true) {
                                            guessInitialMid = msg.id
                                            showGuessSheet = true
                                        } else {
                                            redpackFallbackMeta = meta
                                            redpackSenderName = msg.displayAuthor
                                            redpackDialogMsgId = msg.id
                                        }
                                    },
                                    onQuoteClick = { qid -> jumpToMessage(qid) },
                                    onOpenRepliesSheet = { repliesSheetMsgId = msg.id },
                                    onPostRefClick = { mid -> inlinePostMemeId = mid },
                                    onHashtagClick = onNavigateToTag,
                                    onLinkClick = onOpenUrl,
                                )
                            }
                        }

                        if (historyHasNewer) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    TextButton(onClick = {
                                        val edge = activeHistory.lastOrNull()?.id ?: return@TextButton
                                        scope.launch {
                                            val d = api.fetchChatMessagesAround(state.activeGroupId, edge)
                                            if (d != null) {
                                                val have = activeHistory.map { it.id }.toHashSet()
                                                val newer = d.items.filter { it.id > edge && !have.contains(it.id) }
                                                historyMsgs = activeHistory + newer
                                                historyHasNewer = d.moreAfter
                                            }
                                        }
                                    }) {
                                        Text("↓ 更晚消息")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        if (state.hasMoreOlder) {
                            item(key = "older_header") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TextButton(
                                        enabled = !state.isLoadingHistory,
                                        onClick = {
                                            val anchorId = state.messages.firstOrNull { it.id > 0L }?.id
                                            scope.launch {
                                                val prepended = controller.loadOlderHistory()
                                                if (prepended > 0 && anchorId != null) {
                                                    pendingPrependAnchorId = anchorId
                                                }
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                                        Text(if (state.isLoadingHistory) "加载更早消息中…" else "查看更早消息", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        itemsIndexed(
                            state.messages,
                            key = { _, m ->
                                if (m.id > 0L) "srv_${m.id}" else "loc_${m.localId ?: m.createdAt}"
                            },
                        ) { idx, msg ->
                            val prev = if (idx > 0) state.messages[idx - 1] else null
                            val cont = canGroupContinuation(prev, msg)
                            Column {
                                if (!cont && idx > 0) {
                                    Spacer(Modifier.height(5.dp))
                                }
                                if (!msg.systemKind.isNullOrBlank()) {
                                    SystemNoticeRow(
                                        msg = msg,
                                        onActionRedpack = { refId ->
                                            redpackFallbackMeta = null
                                            redpackSenderName = null
                                            redpackDialogMsgId = refId
                                        },
                                        onActionGuess = { refId ->
                                            guessInitialMid = refId
                                            showGuessSheet = true
                                        },
                                        onActionRaffle = { refId ->
                                            scope.launch { api.joinActiveTitleRaffle(refId) }
                                        },
                                        onJumpMessage = { jumpToMessage(it) },
                                    )
                                } else {
                                    SwipeableChatRow(
                                        msg = msg,
                                        isContinuation = cont,
                                        highlighted = msg.id > 0L && highlightedMsgId == msg.id,
                                        api = api,
                                        onSwipeReply = { controller.setReplyTarget(msg) },
                                        onOpenActionSheet = { actionSheetMsg = msg },
                                        onOpenBoostComposer = {
                                            if (me == null) onRequireLogin() else boostComposerMsg = msg
                                        },
                                        onQuickBoost = { txt ->
                                            if (me == null) onRequireLogin() else scope.launch { controller.sendBoost(msg.id, txt) }
                                        },
                                        onReaction = { emoji ->
                                            if (me == null) onRequireLogin() else scope.launch { controller.toggleReaction(msg.id, emoji) }
                                        },
                                        onRetrySend = { msg.localId?.let { lid -> scope.launch { controller.retryFailedMessage(lid) } } },
                                        onOpenImage = { url -> fullscreenImg = url },
                                        onOpenVideo = { poster, vid -> fullscreenVideoPair = poster to vid },
                                        onOpenRedpack = {
                                            val meta = msg.parseRedpackMeta()
                                            if (meta?.isGuessGame == true) {
                                                guessInitialMid = msg.id
                                                showGuessSheet = true
                                            } else {
                                                redpackFallbackMeta = meta
                                                redpackSenderName = msg.displayAuthor
                                                redpackDialogMsgId = msg.id
                                            }
                                        },
                                        onQuoteClick = { qid -> jumpToMessage(qid) },
                                        onOpenRepliesSheet = { repliesSheetMsgId = msg.id },
                                        onPostRefClick = { mid -> inlinePostMemeId = mid },
                                        onHashtagClick = onNavigateToTag,
                                        onLinkClick = onOpenUrl,
                                    )
                                }
                            }
                        }
                    }

                    // Floating jump-to-latest pill when browsing history
                    if (!isNearBottom && state.messages.isNotEmpty()) {
                        Surface(
                            color = palette.primary,
                            shape = RoundedCornerShape(999.dp),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 10.dp)
                                .clickable {
                                    val lastIdx = (if (state.hasMoreOlder) 1 else 0) + state.messages.size - 1
                                    if (lastIdx >= 0) {
                                        scope.launch {
                                            listState.animateScrollToItem(lastIdx)
                                            pendingNewCount = 0
                                            controller.acknowledgeGroupRead(state.activeGroupId)
                                        }
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (pendingNewCount > 0) "$pendingNewCount 条新消息" else "回最新",
                                    color = palette.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (me == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = onRequireLogin) { Text("登录后查看私信") }
                    }
                } else {
                    LazyColumn(
                        state = dmListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(dmMessages, key = { it.id }) { dm ->
                            val isMine = dm.senderId == me.id
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                            ) {
                                Surface(
                                    color = if (isMine) palette.primary else palette.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.widthIn(max = 300.dp),
                                ) {
                                    Text(
                                        text = if (dm.deleted) "消息已撤回" else dm.body,
                                        color = if (isMine) palette.onPrimary else palette.text,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val reply = state.replyTarget
        val edit = state.editTarget
        if (channelMode == "group" && (reply != null || edit != null)) {
            Surface(color = palette.surface2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (edit != null) "编辑消息：${edit.body}" else "回复 ${reply?.displayAuthor}：${reply?.body}",
                        color = palette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            controller.setReplyTarget(null)
                            controller.setEditTarget(null)
                            if (edit != null) draft = ""
                        },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "取消", tint = palette.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (showStickerTray && channelMode == "group") {
            Surface(
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .border(1.dp, palette.line),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(stickerPacks, key = { it.tabKey }) { pack ->
                            FilterChip(
                                selected = activePackKey == pack.tabKey,
                                onClick = { activePackKey = pack.tabKey },
                                label = { Text(pack.label.removePrefix("L站·"), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = palette.onPrimary,
                                ),
                            )
                        }
                        if (savedStickers.isNotEmpty()) {
                            item {
                                FilterChip(
                                    selected = activePackKey == "__fav__",
                                    onClick = { activePackKey = "__fav__" },
                                    label = { Text("收藏 (${savedStickers.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.primary,
                                        selectedLabelColor = palette.onPrimary,
                                    ),
                                )
                            }
                        }
                    }

                    val selectedPack = stickerPacks.firstOrNull { it.tabKey == activePackKey }
                    if (stickerPacks.isEmpty() && savedStickers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("正在加载平台表情…", color = palette.muted, fontSize = 12.sp)
                        }
                    } else if (activePackKey == "__fav__") {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(52.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(
                                savedStickers,
                                key = { idx, st -> "fav_${idx}_${st.messageId}_${st.index}" },
                            ) { _, st ->
                                val url = st.url?.let { MediaUrlRewriter.rewriteMediaUrl(it) }
                                    ?: MediaUrlRewriter.stickerUrl(st.pack, st.name, st.ext)
                                AsyncImage(
                                    model = url,
                                    imageLoader = api.imageLoader,
                                    contentDescription = st.name,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (st.pack.isNotBlank() && st.name.isNotBlank()) {
                                                draft += "[${st.pack}:${st.name}]"
                                            }
                                        }
                                        .padding(2.dp),
                                )
                            }
                        }
                    } else if (selectedPack != null) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(52.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(
                                selectedPack.items.distinct().take(120),
                                key = { "${selectedPack.tabKey}:$it" },
                            ) { itemName ->
                                val url = MediaUrlRewriter.stickerUrl(selectedPack.key, itemName, selectedPack.ext)
                                AsyncImage(
                                    model = url,
                                    imageLoader = api.imageLoader,
                                    contentDescription = itemName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            draft += "[${selectedPack.key}:$itemName]"
                                        }
                                        .padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sleek ~48dp slim composer capsule flush above IME or bottom bar
        Surface(color = palette.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (channelMode == "group") {
                    IconButton(
                        onClick = { showStickerTray = !showStickerTray },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEmotions,
                            contentDescription = "表情",
                            tint = if (showStickerTray) palette.primary else palette.muted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, palette.line.copy(alpha = 0.8f), RoundedCornerShape(18.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = when {
                                    me == null -> "登录后参与群聊"
                                    channelMode == "dm" -> "发送私信…"
                                    else -> "发消息…"
                                },
                                color = palette.muted,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            maxLines = 4,
                            textStyle = TextStyle(color = palette.text, fontSize = 14.sp),
                            cursorBrush = SolidColor(palette.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (me == null) {
                            onRequireLogin()
                            return@IconButton
                        }
                        val txt = draft.trim()
                        if (txt.isEmpty()) return@IconButton
                        draft = ""
                        showStickerTray = false
                        scope.launch {
                            if (channelMode == "group") {
                                controller.sendOrEditMessage(txt)
                            } else {
                                val tid = activeDmId ?: return@launch
                                if (api.sendDmMessage(tid, txt)) {
                                    val fetched = api.fetchDmMessages(tid)
                                    dmMessages = fetched
                                    val newestDm = fetched.lastOrNull { it.id > 0L }?.id ?: 0L
                                    if (newestDm > 0L) api.markDmRead(tid, newestDm)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "发送",
                        tint = if (draft.isNotBlank()) palette.primary else palette.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    fullscreenImg?.let { url ->
        FullscreenMediaOverlay(imageUrl = url, api = api, onDismiss = { fullscreenImg = null })
    }

    fullscreenVideoPair?.let { (posterUrl, videoUrl) ->
        FullscreenMediaOverlay(
            imageUrl = posterUrl,
            videoUrl = videoUrl,
            api = api,
            onDismiss = { fullscreenVideoPair = null },
        )
    }

    inlinePostMemeId?.let { mid ->
        MemeDetailModal(
            memeId = mid,
            api = api,
            me = me,
            onRequireLogin = onRequireLogin,
            onDismiss = { inlinePostMemeId = null },
            onOpenImageFullscreen = { url -> fullscreenImg = url },
            onTagClick = { tag ->
                inlinePostMemeId = null
                onNavigateToTag(tag)
            },
            onStateUpdated = {},
        )
    }

    actionSheetMsg?.let { targetMsg ->
        ChatMessageActionSheet(
            msg = targetMsg,
            api = api,
            me = me,
            onDismiss = { actionSheetMsg = null },
            onReact = { emoji ->
                actionSheetMsg = null
                if (me == null) onRequireLogin() else scope.launch { controller.toggleReaction(targetMsg.id, emoji) }
            },
            onQuickBoost = { txt ->
                actionSheetMsg = null
                if (me == null) onRequireLogin() else scope.launch { controller.sendBoost(targetMsg.id, txt) }
            },
            onOpenBoostComposer = {
                actionSheetMsg = null
                if (me == null) onRequireLogin() else boostComposerMsg = targetMsg
            },
            onReply = {
                actionSheetMsg = null
                historyMsgs = null
                controller.setReplyTarget(targetMsg)
            },
            onQuote = {
                actionSheetMsg = null
                historyMsgs = null
                controller.setReplyTarget(targetMsg)
                val qSummary = ChatRichTextParser.previewSummary(targetMsg.body).ifBlank { "[图片]" }.take(180)
                val qBlock = "[quote=\"${targetMsg.displayAuthor}, msg:${targetMsg.id}, g:${state.activeGroupId}\"]\n$qSummary\n[/quote]\n"
                draft = qBlock + draft
            },
            onEchoPlusOne = {
                actionSheetMsg = null
                if (me == null) onRequireLogin() else if (targetMsg.body.isNotBlank()) {
                    scope.launch { controller.sendOrEditMessage(targetMsg.body) }
                }
            },
            onCopy = {
                actionSheetMsg = null
                val clipTxt = targetMsg.body.ifBlank {
                    targetMsg.allMedia.firstOrNull()?.resolvedUrl?.let { MediaUrlRewriter.rewriteMediaUrl(it) }.orEmpty()
                }
                if (clipTxt.isNotBlank()) {
                    clipboard.setText(AnnotatedString(clipTxt))
                }
            },
            onEdit = {
                actionSheetMsg = null
                historyMsgs = null
                controller.setEditTarget(targetMsg)
            },
            onRecall = {
                actionSheetMsg = null
                scope.launch { controller.recallMessage(targetMsg.id) }
            },
        )
    }

    boostComposerMsg?.let { targetMsg ->
        BoostComposerSheet(
            msg = targetMsg,
            onDismiss = { boostComposerMsg = null },
            onSubmit = { txt ->
                val ok = controller.sendBoost(targetMsg.id, txt).isSuccess
                if (ok) boostComposerMsg = null
                ok
            },
        )
    }

    repliesSheetMsgId?.let { parentId ->
        ChatRepliesSheet(
            messageId = parentId,
            api = api,
            onDismiss = { repliesSheetMsgId = null },
            onJumpMessage = { jumpId ->
                repliesSheetMsgId = null
                jumpToMessage(jumpId)
            },
        )
    }

    if (showSearchSheet) {
        ChatSearchSheet(
            groupId = state.activeGroupId,
            api = api,
            onDismiss = { showSearchSheet = false },
            onSelectMessage = { mid ->
                showSearchSheet = false
                jumpToMessage(mid)
            },
        )
    }

    if (showGuessSheet) {
        GuessRoomSheet(
            groupId = state.activeGroupId,
            initialMessageId = guessInitialMid,
            refreshSignal = guessSignalHash,
            api = api,
            me = me,
            onRequireLogin = onRequireLogin,
            onDismiss = { showGuessSheet = false },
        )
    }

    redpackDialogMsgId?.let { rMsgId ->
        RedpackDetailSheet(
            messageId = rMsgId,
            senderName = redpackSenderName,
            fallbackMeta = redpackFallbackMeta,
            api = api,
            onDismiss = { redpackDialogMsgId = null },
            onUpdated = { scope.launch { controller.pollOnce(state.activeGroupId) } },
            onOpenGuessRoom = { mid ->
                redpackDialogMsgId = null
                guessInitialMid = mid
                showGuessSheet = true
            },
        )
    }
}

internal fun canGroupContinuation(prev: ChatMessage?, cur: ChatMessage): Boolean {
    if (prev == null) return false
    if (!prev.systemKind.isNullOrBlank() || !cur.systemKind.isNullOrBlank()) return false
    if (prev.deleted || cur.deleted) return false
    if (prev.mine != cur.mine) return false
    if (cur.replyTo != null || cur.redpack != null || prev.redpack != null) return false

    val prevUid = prev.effectiveUserId
    val curUid = cur.effectiveUserId
    val sameAuthor = when {
        prevUid > 0L && curUid > 0L -> prevUid == curUid
        prevUid > 0L || curUid > 0L -> false
        else -> prev.resolvedUsername.isNotBlank() && prev.resolvedUsername == cur.resolvedUsername
    }
    if (!sameAuthor) return false

    val prevDay = (prev.createdAt + 28_800L).floorDiv(86_400L)
    val curDay = (cur.createdAt + 28_800L).floorDiv(86_400L)
    if (prevDay != curDay) return false

    return (cur.createdAt - prev.createdAt) in 0L until 300L
}

@Composable
private fun ChatHeaderPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = if (selected) palette.primary else palette.surface2,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .border(
                1.dp,
                if (selected) palette.primary else palette.line,
                RoundedCornerShape(999.dp),
            )
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            color = if (selected) palette.onPrimary else palette.text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * Renders system notices without clipping corners on multi-line settlements:
 * short 1-line `join` stays pill-shaped, while multi-line settlements/raffles use a `12.dp` card.
 */
@Composable
private fun SystemNoticeRow(
    msg: ChatMessage,
    onActionRedpack: (Long) -> Unit,
    onActionGuess: (Long) -> Unit,
    onActionRaffle: (Long) -> Unit,
    onJumpMessage: (Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val kind = msg.systemKind.orEmpty()
    val refId = msg.systemRef?.toLongOrNull()

    if (kind == "join") {
        val label = when {
            msg.deleted -> "入群提醒已撤回"
            msg.body.isBlank() -> "欢迎 ${msg.displayAuthor} 加入群聊"
            msg.body.contains(msg.displayAuthor) -> msg.body
            else -> "欢迎 ${msg.displayAuthor} ${msg.body}"
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = palette.surface2.copy(alpha = 0.85f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "✦ $label",
                    color = palette.muted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        return
    }

    val isRedpackNotice = kind in setOf("redpack_result", "redpack_draw", "redpack_due", "ldc_winner", "ldc_cancel", "ldc_expired")
    val parts = remember(msg.body, msg.deleted) {
        if (msg.deleted) {
            listOf("系统提醒已撤回")
        } else {
            msg.body.ifBlank { kind }.split("\n答案释义：")
        }
    }
    val headline = if (kind == "redpack_due" && !msg.deleted) "红包到点啦！" else parts.firstOrNull().orEmpty()
    val explanation = if (!msg.deleted && parts.size > 1) parts.drop(1).joinToString("；") else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = if (isRedpackNotice) palette.gold.copy(alpha = 0.10f) else palette.surface2.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(
                    1.dp,
                    if (isRedpackNotice) palette.gold.copy(alpha = 0.28f) else palette.line.copy(alpha = 0.7f),
                    RoundedCornerShape(12.dp),
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = headline,
                    color = palette.text,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = if (isRedpackNotice) FontWeight.Medium else FontWeight.Normal,
                )
                if (!explanation.isNullOrBlank()) {
                    Text(
                        text = "答案释义：$explanation",
                        color = palette.primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
                if (!msg.deleted && refId != null && refId > 0L) {
                    val actionLabel = when {
                        kind == "title_raffle_open" && msg.body.contains("报名") -> "点我报名"
                        kind == "ldc_game" -> "对应发言"
                        kind in setOf("ldc_winner", "ldc_cancel", "ldc_expired") -> "看猜词结果"
                        isRedpackNotice -> "去看看"
                        else -> null
                    }
                    if (actionLabel != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Surface(
                                color = palette.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.clickable {
                                    when {
                                        kind == "title_raffle_open" -> onActionRaffle(refId)
                                        kind == "ldc_game" -> onJumpMessage(refId)
                                        kind in setOf("ldc_winner", "ldc_cancel", "ldc_expired") -> onActionGuess(refId)
                                        else -> onActionRedpack(refId)
                                    }
                                },
                            ) {
                                Text(
                                    text = actionLabel,
                                    color = palette.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun SwipeableChatRow(
    msg: ChatMessage,
    isContinuation: Boolean,
    highlighted: Boolean,
    api: ForHappyApiClient,
    onSwipeReply: () -> Unit,
    onOpenActionSheet: () -> Unit,
    onOpenBoostComposer: () -> Unit,
    onQuickBoost: (String) -> Unit,
    onReaction: (String) -> Unit,
    onRetrySend: () -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    onOpenRedpack: () -> Unit,
    onQuoteClick: (Long) -> Unit,
    onOpenRepliesSheet: () -> Unit,
    onPostRefClick: (Long) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val haptic = LocalHapticFeedback.current
    var dragX by remember { mutableFloatStateOf(0f) }

    val timeStr = remember(msg.createdAt) {
        runCatching {
            SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(msg.createdAt * 1000L))
        }.getOrDefault("")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.gold.copy(alpha = 0.16f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier
                },
            )
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .pointerInput(msg.id, msg.localId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragX < -72f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwipeReply()
                        }
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                    onHorizontalDrag = { _, delta ->
                        dragX = (dragX + delta).coerceIn(-100f, 0f)
                    },
                )
            }
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (!msg.deleted) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenActionSheet()
                    }
                },
            ),
        horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!msg.mine) {
            if (!isContinuation) {
                ForHappyAvatar(
                    username = msg.displayAuthor,
                    avatarUrl = msg.resolvedAvatar,
                    imageLoader = api.imageLoader,
                    size = 32.dp,
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(40.dp))
            }
        }

        Column(
            horizontalAlignment = if (msg.mine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 318.dp),
        ) {
            if (!isContinuation) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (!msg.mine) {
                        Text(
                            text = msg.displayAuthor,
                            color = palette.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    msg.resolvedTitle?.let { titleLabel ->
                        BadgePill(titleLabel, containerColor = palette.gold.copy(alpha = 0.16f), contentColor = palette.gold)
                    }
                    Text(timeStr, color = palette.muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    msg.floor?.let { fl ->
                        Text("${fl}楼", color = palette.muted.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                    if (!msg.deleted) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "消息操作",
                            tint = palette.muted.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onOpenActionSheet() },
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }

            val redpackMeta = if (!msg.deleted) msg.parseRedpackMeta() else null
            val bubbleBg = if (msg.mine) palette.primary.copy(alpha = 0.14f) else palette.surface
            val bubbleBorder = when {
                highlighted -> palette.gold
                msg.mine -> palette.primary.copy(alpha = 0.35f)
                else -> palette.line
            }

            Surface(
                color = bubbleBg,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.border(
                    width = if (highlighted) 1.5.dp else 1.dp,
                    color = bubbleBorder,
                    shape = RoundedCornerShape(15.dp),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    msg.replyTo?.let { rep ->
                        val cleanRefText = remember(rep.resolvedText) {
                            ChatRichTextParser.previewSummary(rep.resolvedText).ifBlank {
                                if (rep.deleted) "消息已撤回" else if (rep.file != null) "[文件] ${rep.file}" else if (rep.hasImage || rep.thumb != null) "[图片]" else "…"
                            }
                        }
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = rep.id > 0L) {
                                onQuoteClick(rep.id)
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "${rep.resolvedAuthor}：$cleanRefText",
                                    color = palette.muted,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (!rep.deleted && !rep.thumb.isNullOrBlank()) {
                                    AsyncImage(
                                        model = MediaUrlRewriter.rewriteWithExtProxy(rep.thumb),
                                        imageLoader = api.imageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(4.dp)),
                                    )
                                }
                            }
                        }
                    }

                    if (msg.deleted) {
                        Text(
                            text = "${if (msg.mine) "你" else msg.displayAuthor} 撤回了一条消息",
                            color = palette.muted,
                            fontSize = 13.sp,
                        )
                    } else {
                        if (redpackMeta != null) {
                            RedpackBubbleCard(
                                meta = redpackMeta,
                                api = api,
                                onClick = onOpenRedpack,
                            )
                        }

                        val isEditedMsg = msg.edited || msg.editedAt != null
                        if (msg.body.isNotBlank() && (redpackMeta == null || redpackMeta.shared)) {
                            RichChatBodyView(
                                rawBody = msg.body,
                                imageLoader = api.imageLoader,
                                textColor = palette.text,
                                isEdited = isEditedMsg,
                                onPostRefClick = onPostRefClick,
                                onHashtagClick = onHashtagClick,
                                onLinkClick = onLinkClick,
                                onQuoteClick = onQuoteClick,
                                onInlineImageClick = onOpenImage,
                                onLongPress = {
                                    if (!msg.deleted) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenActionSheet()
                                    }
                                },
                            )
                        } else if (isEditedMsg && redpackMeta == null) {
                            Text("（已编辑）", color = palette.muted, fontSize = 11.sp)
                        }

                        if (msg.mediaExpired) {
                            Surface(
                                color = palette.surface2,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "附件已超过七天，无法查看",
                                    color = palette.muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        if (redpackMeta == null || redpackMeta.shared) {
                            val all = msg.allMedia
                            val files = remember(all) { all.filter { it.k == "f" } }
                            val visuals = remember(all) { all.filter { it.k != "f" } }

                            if (visuals.size == 1) {
                                ChatVisualMediaCell(
                                    msgId = msg.id,
                                    media = visuals.first(),
                                    solo = true,
                                    api = api,
                                    onOpenImage = onOpenImage,
                                    onOpenVideo = onOpenVideo,
                                )
                            } else if (visuals.size > 1) {
                                val cols = if (visuals.size == 2 || visuals.size == 4) 2 else 3
                                val rows = visuals.chunked(cols)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.width(222.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            rowItems.forEach { media ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    ChatVisualMediaCell(
                                                        msgId = msg.id,
                                                        media = media,
                                                        solo = false,
                                                        api = api,
                                                        onOpenImage = onOpenImage,
                                                        onOpenVideo = onOpenVideo,
                                                    )
                                                }
                                            }
                                            repeat(cols - rowItems.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            files.forEach { f ->
                                val dlUrl = MediaUrlRewriter.rewriteWithExtProxy(f.resolvedUrl)
                                Surface(
                                    color = palette.surface2,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                        .clickable(enabled = dlUrl.isNotBlank()) { onLinkClick(dlUrl) },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = f.resolvedName ?: "附件文件",
                                                color = palette.text,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if ((f.s ?: 0L) > 0L) {
                                                Text(formatFileBytes(f.s!!), color = palette.muted, fontSize = 10.sp)
                                            }
                                        }
                                        Icon(Icons.Rounded.Download, contentDescription = "下载", tint = palette.muted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!msg.deleted) {
                if (msg.boosts.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    ChatBoostsBlock(
                        boosts = msg.boosts,
                        mineAlignEnd = msg.mine,
                        api = api,
                        onTapBoost = { b ->
                            if (b.ai) return@ChatBoostsBlock
                            if (b.mine) {
                                onOpenBoostComposer()
                            } else if (b.text.isNotBlank()) {
                                onQuickBoost(b.text)
                            }
                        },
                    )
                }

                if (msg.reactions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, if (msg.mine) Alignment.End else Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        msg.reactions.filter { it.count > 0 }.forEach { rx ->
                            ReactionChip(
                                emojiKey = rx.emoji,
                                count = rx.count,
                                mine = rx.mine,
                                imageLoader = api.imageLoader,
                                compact = true,
                                onClick = { onReaction(rx.emoji) },
                            )
                        }
                    }
                }

                if (msg.replyCount > 0 && msg.id > 0L) {
                    Spacer(Modifier.height(3.dp))
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(1.dp, palette.line.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                            .clickable { onOpenRepliesSheet() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, tint = palette.primary, modifier = Modifier.size(12.dp))
                            Text("${msg.replyCount} 条回复", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (msg.deliveryState == "sending") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = palette.muted)
                    Spacer(Modifier.width(4.dp))
                    Text("发送中…", color = palette.muted, fontSize = 10.sp)
                }
            } else if (msg.deliveryState == "failed") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable { onRetrySend() },
                ) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = palette.danger, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("发送失败 · 点此携带 retry:true 重发", color = palette.danger, fontSize = 10.sp)
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = palette.danger, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatVisualMediaCell(
    msgId: Long,
    media: ChatMedia,
    solo: Boolean,
    api: ForHappyApiClient,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val rawUrl = media.resolvedUrl
    val imgUrl = MediaUrlRewriter.rewriteWithExtProxy(rawUrl)
    val rawVid = media.v
    val isVideo = media.k == "v" || !rawVid.isNullOrBlank()
    val isLive = media.k == "l"
    val videoUrl = if (!rawVid.isNullOrBlank()) MediaUrlRewriter.rewriteWithExtProxy(rawVid) else ""
    var unblurred by remember(msgId, rawUrl) { mutableStateOf(false) }

    val mw = media.w ?: media.width ?: 0
    val mh = media.h ?: media.height ?: 0
    val ratio = when {
        !solo -> 1f
        mw > 0 && mh > 0 -> (mw.toFloat() / mh.toFloat()).coerceIn(0.65f, 1.85f)
        else -> 4f / 3f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = (if (solo) Modifier.width(210.dp) else Modifier.fillMaxWidth())
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface2)
            .clickable {
                if (media.b && !unblurred) {
                    unblurred = true
                } else if (isVideo && videoUrl.isNotBlank()) {
                    onOpenVideo(imgUrl, videoUrl)
                } else if (imgUrl.isNotBlank()) {
                    onOpenImage(imgUrl)
                }
            },
    ) {
        if (imgUrl.isNotBlank()) {
            AsyncImage(
                model = imgUrl,
                imageLoader = api.imageLoader,
                contentDescription = media.resolvedName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (media.b && !unblurred) it.blur(20.dp) else it },
            )
        }
        if (media.b && !unblurred) {
            Surface(
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("已模糊 · 轻触显示", color = Color.White, fontSize = 10.sp)
                }
            }
        } else if (isVideo) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircleFilled,
                    contentDescription = "播放视频",
                    tint = Color.White,
                    modifier = Modifier.size(if (solo) 38.dp else 28.dp),
                )
            }
        }
        if (isLive) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            ) {
                Text("实况", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatBoostsBlock(
    boosts: List<ChatBoost>,
    mineAlignEnd: Boolean,
    api: ForHappyApiClient,
    onTapBoost: (ChatBoost) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val aiBoosts = remember(boosts) { boosts.filter { it.ai } }
    val userBoosts = remember(boosts) { boosts.filter { !it.ai } }

    Column(
        horizontalAlignment = if (mineAlignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        aiBoosts.forEach { b ->
            Surface(
                color = palette.primary.copy(alpha = 0.09f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .border(1.dp, palette.primary.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ForHappyAvatar(
                        username = "欢欢",
                        avatarUrl = b.avatar,
                        imageLoader = api.imageLoader,
                        size = 18.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("欢欢 AI 回应", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = b.text.ifBlank { if (b.status == "thinking") "思考中…" else "…" },
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                        b.score?.let { sc ->
                            LinearProgressIndicator(
                                progress = { sc.coerceIn(0, 100) / 100f },
                                color = palette.primary,
                                trackColor = palette.surface2,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                            )
                        }
                    }
                }
            }
        }

        if (userBoosts.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, if (mineAlignEnd) Alignment.End else Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                userBoosts.forEach { b ->
                    Surface(
                        color = if (b.mine) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (b.mine) palette.primary.copy(alpha = 0.50f) else palette.line.copy(alpha = 0.70f),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { onTapBoost(b) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ForHappyAvatar(
                                username = b.displayAuthor,
                                avatarUrl = b.avatar,
                                imageLoader = api.imageLoader,
                                size = 15.dp,
                            )
                            BoostPillText(
                                text = b.text,
                                imageLoader = api.imageLoader,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageActionSheet(
    msg: ChatMessage,
    api: ForHappyApiClient,
    me: MeUser?,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onQuickBoost: (String) -> Unit,
    onOpenBoostComposer: () -> Unit,
    onReply: () -> Unit,
    onQuote: () -> Unit,
    onEchoPlusOne: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRecall: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMoreStickers by remember { mutableStateOf(false) }
    var packs by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var activeTab by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showMoreStickers) {
        if (showMoreStickers && packs.isEmpty()) {
            packs = api.fetchStickerPacks().filter { !it.hidden && it.items.isNotEmpty() }
            activeTab = packs.firstOrNull()?.tabKey
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Quick reactions row (10 official PICKED emojis + expand sticker packs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("表情回应", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showMoreStickers = !showMoreStickers }) {
                    Text(if (showMoreStickers) "收起表情包" else "更多表情包", fontSize = 12.sp)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PICKED_REACTIONS.forEach { key ->
                    val existing = msg.reactions.firstOrNull { it.emoji == key }
                    ReactionChip(
                        emojiKey = key,
                        count = existing?.count ?: 0,
                        mine = existing?.mine == true,
                        imageLoader = api.imageLoader,
                        onClick = { onReact(key) },
                    )
                }
            }

            if (showMoreStickers && packs.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(packs, key = { it.tabKey }) { pack ->
                        FilterChip(
                            selected = activeTab == pack.tabKey,
                            onClick = { activeTab = pack.tabKey },
                            label = { Text(pack.label.removePrefix("L站·"), fontSize = 11.sp) },
                        )
                    }
                }
                val selPack = packs.firstOrNull { it.tabKey == activeTab }
                if (selPack != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(44.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(selPack.items.distinct().take(80), key = { "${selPack.tabKey}_act_$it" }) { stk ->
                                val key = "${selPack.key}:$stk"
                                AsyncImage(
                                    model = MediaUrlRewriter.stickerUrl(selPack.key, stk, selPack.ext),
                                    imageLoader = api.imageLoader,
                                    contentDescription = stk,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onReact(key) }
                                        .padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            // Quick Boost strip + Boost composer entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Boost 回应（≤12字）", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Surface(
                    color = palette.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable { onOpenBoostComposer() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = palette.primary, modifier = Modifier.size(14.dp))
                        Text("写一句 Boost…", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(BOOST_PRESETS, key = { "act_bst_$it" }) { preset ->
                    val accent = remember(preset, palette.isDark) { boostPresetAccent(preset, palette.isDark) }
                    val isMine = msg.boosts.any { it.mine && it.text == preset }
                    Surface(
                        color = if (isMine) palette.primary.copy(alpha = 0.18f) else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isMine) palette.primary else palette.line,
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { onQuickBoost(if (isMine) "" else preset) },
                    ) {
                        Text(
                            text = preset,
                            color = accent ?: palette.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            // Message operations grid
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionMenuTile(icon = Icons.AutoMirrored.Rounded.Reply, label = "回复", onClick = onReply)
                ActionMenuTile(icon = Icons.Rounded.FormatQuote, label = "引用", onClick = onQuote)
                if (msg.body.isNotBlank()) {
                    ActionMenuTile(icon = Icons.Rounded.Repeat, label = "+1 复读", onClick = onEchoPlusOne)
                }
                ActionMenuTile(icon = Icons.Rounded.ContentCopy, label = "复制", onClick = onCopy)
                if ((msg.canEdit || (msg.mine && msg.id > 0L)) && msg.body.isNotBlank()) {
                    ActionMenuTile(icon = Icons.Rounded.Edit, label = "编辑", onClick = onEdit)
                }
                if (msg.canDelete || (msg.mine && msg.id > 0L) || me?.isAdmin == true) {
                    ActionMenuTile(icon = Icons.Rounded.Delete, label = if (msg.mine) "撤回" else "删除", danger = true, onClick = onRecall)
                }
            }
        }
    }
}

@Composable
private fun ActionMenuTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val tint = if (danger) palette.danger else palette.text
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BoostComposerSheet(
    msg: ChatMessage,
    onDismiss: () -> Unit,
    onSubmit: suspend (String) -> Boolean,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val oldMine = remember(msg) { msg.boosts.firstOrNull { !it.ai && it.mine }?.text.orEmpty() }
    var text by remember(msg.id) { mutableStateOf(oldMine) }
    var busy by remember { mutableStateOf(false) }

    val units = remember(text) { countReplyUnits(text) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("给这条消息挂一句 Boost（${BOOST_MAX_LEN}字内）", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("自己写一句（$BOOST_MAX_LEN 字内）", fontSize = 13.sp) },
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "$units/$BOOST_MAX_LEN",
                            color = if (units > BOOST_MAX_LEN) palette.danger else palette.muted,
                            fontSize = 11.sp,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = !busy && units in 1..BOOST_MAX_LEN,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    onClick = {
                        busy = true
                        scope.launch {
                            onSubmit(text.trim())
                            busy = false
                        }
                    },
                ) {
                    Text(if (oldMine.isNotBlank()) "改" else "挂上")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BOOST_PRESETS.forEach { preset ->
                    val accent = remember(preset, palette.isDark) { boostPresetAccent(preset, palette.isDark) }
                    val selected = oldMine == preset
                    Surface(
                        color = if (selected) palette.primary.copy(alpha = 0.18f) else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) palette.primary else palette.line,
                                RoundedCornerShape(999.dp),
                            )
                            .clickable(enabled = !busy) {
                                busy = true
                                scope.launch {
                                    onSubmit(preset)
                                    busy = false
                                }
                            },
                    ) {
                        Text(
                            text = preset,
                            color = accent ?: palette.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            if (oldMine.isNotBlank()) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            onSubmit("")
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("撤掉我的回应「$oldMine」", color = palette.danger)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatRepliesSheet(
    messageId: Long,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onJumpMessage: (Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loading by remember(messageId) { mutableStateOf(true) }
    var items by remember(messageId) { mutableStateOf<List<ChatReplyItem>>(emptyList()) }

    LaunchedEffect(messageId) {
        loading = true
        items = api.fetchChatMessageReplies(messageId)
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${items.size} 条回复", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                }
            }
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("回复都撤回了", color = palette.muted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { rep ->
                        val ts = remember(rep.at) {
                            runCatching {
                                SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(rep.at * 1000L))
                            }.getOrDefault("")
                        }
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { onJumpMessage(rep.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ForHappyAvatar(
                                    username = rep.displayAuthor,
                                    avatarUrl = rep.avatar,
                                    imageLoader = api.imageLoader,
                                    size = 28.dp,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(rep.displayAuthor, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(ts, color = palette.muted, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = rep.text?.takeIf { it.isNotBlank() } ?: if (rep.hasImage) "[图片]" else "…",
                                        color = palette.muted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

@Composable
private fun RedpackBubbleCard(
    meta: RedpackMeta,
    api: ForHappyApiClient,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val isGuess = meta.isGuessGame
    val headerBg = when {
        meta.solved -> Color(0xFFD98A6C)
        isGuess -> palette.primary
        meta.isDcoin -> Color(0xFFD48806)
        else -> Color(0xFFC84B31)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        modifier = Modifier
            .widthIn(min = 210.dp, max = 270.dp)
            .border(1.dp, headerBg.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGuess) Icons.Rounded.AutoAwesome else Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meta.headline,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = meta.subline,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                meta.cover?.let { coverUrl ->
                    AsyncImage(
                        model = MediaUrlRewriter.rewriteMediaUrl(coverUrl),
                        imageLoader = api.imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface2.copy(alpha = 0.55f))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = meta.actionLabel,
                    color = headerBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = meta.typeBadge,
                    color = palette.muted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSearchSheet(
    groupId: Long,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onSelectMessage: (Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var queryText by remember { mutableStateOf("") }
    var onlyImages by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }

    fun runSearch(beforeId: Long? = null) {
        val raw = queryText.trim()
        val atMatch = Regex("(?:^|\\s)@(\\S+)").find(raw)
        val user = atMatch?.groupValues?.getOrNull(1).orEmpty()
        val kw = raw.replace(Regex("(?:^|\\s)@\\S+"), "").trim()
        if (kw.isBlank() && user.isBlank() && !onlyImages) {
            results = emptyList()
            hasMore = false
            return
        }
        scope.launch {
            loading = true
            val res = api.searchChatMessages(
                groupId = groupId,
                q = kw,
                user = user,
                onlyImages = onlyImages,
                beforeId = beforeId,
            )
            results = if (beforeId == null) res.items else results + res.items
            hasMore = res.more
            loading = false
        }
    }

    LaunchedEffect(queryText, onlyImages) {
        delay(320L)
        runSearch(null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.primary)
                Text("搜索本群聊天记录", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("输入关键词，或 @用户名 只看某人", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("只看包含图片的消息", color = palette.muted, fontSize = 12.sp)
                    Switch(checked = onlyImages, onCheckedChange = { onlyImages = it })
                }
            }

            HorizontalDivider(color = palette.line)

            if (loading && results.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (queryText.isBlank() && !onlyImages) "输入关键词或勾选只看图开始搜索" else "没有找到相关消息",
                        color = palette.muted,
                        fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { "srch_${it.id}" }) { m ->
                        val ts = remember(m.createdAt) {
                            runCatching {
                                SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(m.createdAt * 1000L))
                            }.getOrDefault("")
                        }
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { onSelectMessage(m.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ForHappyAvatar(
                                    username = m.displayAuthor,
                                    avatarUrl = m.resolvedAvatar,
                                    imageLoader = api.imageLoader,
                                    size = 30.dp,
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(m.displayAuthor, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(ts, color = palette.muted, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = m.body.ifBlank { if (m.allMedia.isNotEmpty()) "[图片]" else "[消息]" },
                                        color = palette.muted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                m.allMedia.firstOrNull()?.resolvedUrl?.let { thumb ->
                                    AsyncImage(
                                        model = MediaUrlRewriter.rewriteWithExtProxy(thumb),
                                        imageLoader = api.imageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(onClick = { runSearch(results.lastOrNull()?.id) }) {
                                    Text("加载更多结果")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedpackDetailSheet(
    messageId: Long,
    senderName: String?,
    fallbackMeta: RedpackMeta?,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
    onOpenGuessRoom: (Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loading by remember(messageId) { mutableStateOf(true) }
    var packetObj by remember(messageId) { mutableStateOf<JsonObject?>(null) }
    var answerInput by remember(messageId) { mutableStateOf("") }
    var statusMsg by remember(messageId) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    suspend fun reloadPacket() {
        loading = true
        val res = api.fetchRedpackDetail(messageId)
        packetObj = res?.get("packet")?.let { runCatching { it.jsonObject }.getOrNull() } ?: res
        loading = false
    }

    LaunchedEffect(messageId) {
        reloadPacket()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC84B31))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = fallbackMeta?.headline ?: "尽欢红包详情",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!senderName.isNullOrBlank()) {
                        Text("来自：$senderName", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                }
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else {
                val p = packetObj
                val ldc = runCatching { p?.get("ldc")?.jsonObject }.getOrNull()
                val ldcMode = ldc?.get("mode")?.jsonPrimitive?.contentOrNull
                val coin52 = runCatching { p?.get("coin52")?.jsonObject }.getOrNull()
                val external = runCatching { p?.get("external")?.jsonObject }.getOrNull()
                val mode = p?.get("mode")?.jsonPrimitive?.contentOrNull ?: fallbackMeta?.mode ?: "open"
                val solved = p?.get("solved")?.jsonPrimitive?.booleanOrNull == true
                val openedCount = p?.get("opened")?.jsonPrimitive?.intOrNull ?: fallbackMeta?.opened ?: 0
                val prompt = p?.get("prompt")?.jsonPrimitive?.contentOrNull ?: fallbackMeta?.headline ?: ""
                val phrase = p?.get("phrase")?.jsonPrimitive?.contentOrNull
                val secret = p?.get("secret")?.jsonPrimitive?.contentOrNull
                val clueText = runCatching { p?.get("clue")?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull }.getOrNull()
                val clueImage = runCatching { p?.get("image")?.jsonObject?.get("u")?.jsonPrimitive?.contentOrNull }.getOrNull()
                val mineReward = ldc?.get("mine")?.jsonObject ?: coin52?.get("mine")?.jsonObject
                val recipients = runCatching { ldc?.get("recipients")?.jsonArray ?: coin52?.get("recipients")?.jsonArray }.getOrNull()

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (prompt.isNotBlank()) {
                        item {
                            Text(prompt, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (ldcMode in setOf("guess", "clues")) {
                        item {
                            Button(
                                onClick = { onOpenGuessRoom(messageId) },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("进入欢欢猜词屋")
                            }
                        }
                    } else if (external != null) {
                        val extUrl = external["url"]?.jsonPrimitive?.contentOrNull
                        val refCode = external["ref"]?.jsonPrimitive?.contentOrNull
                        item {
                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("站外平台红包", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    if (!refCode.isNullOrBlank()) Text("口令/标识：$refCode", color = palette.muted, fontSize = 12.sp)
                                    if (!extUrl.isNullOrBlank()) Text("链接：$extUrl", color = palette.primary, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        if (!phrase.isNullOrBlank()) {
                            item {
                                Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("复读口令：$phrase", color = palette.text, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                        if (!clueText.isNullOrBlank()) {
                            item {
                                Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("线索：$clueText", color = palette.text, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                        if (!clueImage.isNullOrBlank()) {
                            item {
                                AsyncImage(
                                    model = MediaUrlRewriter.rewriteMediaUrl(clueImage),
                                    imageLoader = api.imageLoader,
                                    contentDescription = "红包图片",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                )
                            }
                        }
                        if (solved) {
                            item {
                                Surface(
                                    color = palette.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("已拆封 · $openedCount 人拆过", color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (!secret.isNullOrBlank()) {
                                            Text("内容：$secret", color = palette.text, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                        mineReward?.let { mine ->
                            item {
                                Text(
                                    text = "我的领取：${mine["amount"]?.jsonPrimitive?.contentOrNull.orEmpty()}",
                                    color = palette.gold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (!solved && (mode == "cipher" || mode == "image")) {
                            item {
                                OutlinedTextField(
                                    value = answerInput,
                                    onValueChange = { answerInput = it },
                                    label = { Text("输入谜底 / 口令") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (!recipients.isNullOrEmpty()) {
                            item {
                                Text("领取记录 (${recipients.size})", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            items(recipients.size) { idx ->
                                val r = recipients[idx].jsonObject
                                val who = r["name"]?.jsonPrimitive?.contentOrNull ?: r["username"]?.jsonPrimitive?.contentOrNull ?: "群友"
                                val amt = r["amount"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(who, color = palette.text, fontSize = 13.sp)
                                    Text(amt, color = palette.gold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (!statusMsg.isNullOrBlank()) {
                            item {
                                Text(statusMsg!!, color = palette.primary, fontSize = 13.sp)
                            }
                        }
                        if (!solved && mineReward == null) {
                            item {
                                Button(
                                    enabled = !busy,
                                    onClick = {
                                        busy = true
                                        scope.launch {
                                            val res = if (ldc != null && coin52 == null && ldcMode in setOf("fixed", "random")) {
                                                api.claimLdcRedpack(messageId)
                                            } else {
                                                api.openRedpack(messageId, answerInput.takeIf { it.isNotBlank() })
                                            }
                                            statusMsg = res["msg"]?.jsonPrimitive?.contentOrNull
                                                ?: res["error"]?.jsonPrimitive?.contentOrNull
                                                ?: if (res.isNotEmpty()) "操作已提交" else "开包失败"
                                            reloadPacket()
                                            onUpdated()
                                            busy = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC84B31), contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (busy) "正在开包…" else "拆红包")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
