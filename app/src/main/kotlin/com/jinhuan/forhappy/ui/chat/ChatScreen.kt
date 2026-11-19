package com.jinhuan.forhappy.ui.chat

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.BOOST_MAX_LEN
import com.jinhuan.forhappy.core.model.BOOST_PRESETS
import com.jinhuan.forhappy.core.model.ChatBoost
import com.jinhuan.forhappy.core.model.ChatInputMode
import com.jinhuan.forhappy.core.model.ChatMedia
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatOpenSessionRequest
import com.jinhuan.forhappy.core.model.ChatReplyItem
import com.jinhuan.forhappy.core.model.ChatScrollPolicy
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.core.model.DmMessage
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.GroupMemberItem
import com.jinhuan.forhappy.core.model.GuessRoomOverviewResponse
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.NovelExcerptSpec
import com.jinhuan.forhappy.core.model.PICKED_REACTIONS
import com.jinhuan.forhappy.core.model.RedpackMeta
import com.jinhuan.forhappy.core.model.StickerEntry
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.TitleRaffleItem
import com.jinhuan.forhappy.core.model.countReplyUnits
import com.jinhuan.forhappy.core.model.parseRedpackMeta
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgeChipView
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.components.TitleChipView
import com.jinhuan.forhappy.ui.components.boostPresetAccent
import com.jinhuan.forhappy.ui.components.parseSafeHex
import com.jinhuan.forhappy.ui.compose.ComposeMediaStager
import com.jinhuan.forhappy.ui.disguise.DisguiseSanitizer
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import com.jinhuan.forhappy.ui.wall.saveMediaToSystemGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class ChatNotificationJump(
    val groupId: Long,
    val messageId: Long? = null,
    val triggerSeq: Long = 0L,
)

/**
 * 2-Level Unified `消息` Tab Screen (`02c`):
 * - Level-0 (`activePane == "list"`): Unified Groups & DMs list ([MessagesListView]), bypassed when hosted
 *   inside [com.jinhuan.forhappy.ui.disguise.FeishuDisguiseScreen] (`hideChannelHeader || forcedChannelMode != null`).
 * - Level-1 (`activePane == "group"` | `"dm"`):
 *   - `reverseLayout = true` live chat viewport anchoring with smart/auto/manual scroll policies
 *   - Co-screen squeezed Guess Room intel board + `[聊天 | 猜 | 问]` swipeable composer
 *   - Scheme 3A Red-Packet creation & 3D flip/cipher/9-grid opening dialogs
 *   - `[novel-excerpt:v1:...]` warm-paper cards, Title Raffles, `[＋]` media/file tray, right-edge-safe quote swipe
 *   - Full DM date dividers (`UTC+8`), read receipts (`已读/未读`), media sending & edit/recall
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    hideChannelHeader: Boolean = false,
    forcedChannelMode: String? = null,
    forcedDmThreadId: Long? = null,
    onConvertMessageToPost: ((ChatMessage) -> Unit)? = null,
    notifUnreadCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    openSessionRequest: ChatOpenSessionRequest? = null,
    onConsumeOpenSessionRequest: () -> Unit = {},
    onNavigateToAuthor: (String) -> Unit = {},
    onOpenDiscoverRoute: ((String) -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val stealth = LocalPrivacyStealthConfig.current
    val disguiseMode = stealth.disguiseMode
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state by controller.uiState.collectAsState()
    val isImeVisible = WindowInsets.isImeVisible

    val isDirectEmbedded = hideChannelHeader || forcedChannelMode != null
    var activePane by remember(isDirectEmbedded, forcedChannelMode) {
        mutableStateOf(if (isDirectEmbedded) (forcedChannelMode ?: "group") else "list")
    }
    var listFilter by remember { mutableStateOf("all") }

    // Group Composer State (per-group 3-mode independent drafts backed by controller)
    var inputMode by remember { mutableStateOf(ChatInputMode.CHAT) }
    var draft by remember { mutableStateOf("") }
    var showStickerTray by remember { mutableStateOf(false) }
    var showAttachmentTray by remember { mutableStateOf(false) }
    var mediaUploading by remember { mutableStateOf(false) }
    var stickerPacks by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var savedStickers by remember { mutableStateOf<List<StickerEntry>>(emptyList()) }
    var activePackKey by remember { mutableStateOf<String?>(null) }
    var fullscreenImg by remember { mutableStateOf<String?>(null) }
    var fullscreenVideoPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var chatStatusBanner by remember { mutableStateOf<String?>(null) }

    // Co-screen Squeezed Guess Room state
    var guessOverview by remember { mutableStateOf<GuessRoomOverviewResponse?>(null) }
    var isGuessExpanded by remember { mutableStateOf(false) }
    var wasGuessExpandedBeforeIme by remember { mutableStateOf(false) }
    var guessBoardHeightDp by remember { mutableStateOf(210.dp) }
    var guessReceiptBanner by remember { mutableStateOf<String?>(null) }
    var guessHintBusy by remember { mutableStateOf(false) }
    var guessCloseBusy by remember { mutableStateOf(false) }
    var guessNowTickSec by remember { mutableLongStateOf(ServerClock.serverNowSec()) }

    // Active Title Raffles
    var activeRaffles by remember { mutableStateOf<List<TitleRaffleItem>>(emptyList()) }

    // Message Action / Boost / Replies / Member / GroupInfo / Search Modals
    var actionSheetMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var boostComposerMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var boostHistoryMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var repliesSheetMsgId by remember { mutableStateOf<Long?>(null) }
    var inlinePostMemeId by remember { mutableStateOf<Long?>(null) }
    var selectedMemberProfile by remember { mutableStateOf<GroupMemberItem?>(null) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    // Redpack / Guess Full Dialogs
    var redpackDialogMsgId by remember { mutableStateOf<Long?>(null) }
    var redpackFallbackMeta by remember { mutableStateOf<RedpackMeta?>(null) }
    var redpackSenderName by remember { mutableStateOf<String?>(null) }
    var showCreateRedpackDialog by remember { mutableStateOf(false) }
    var showRedpackArchiveSheet by remember { mutableStateOf(false) }
    var showGuessSheet by remember { mutableStateOf(false) }
    var guessInitialMid by remember { mutableStateOf<Long?>(null) }

    // History `around` jump mode state
    var historyMsgs by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var historyHasOlder by remember { mutableStateOf(false) }
    var historyHasNewer by remember { mutableStateOf(false) }
    var highlightedMsgId by remember { mutableStateOf<Long?>(null) }

    // DM state
    var dmThreads by remember { mutableStateOf(state.dmThreads) }
    var activeDmId by remember { mutableStateOf<Long?>(null) }
    var newDmPeer by remember { mutableStateOf<ChatUser?>(null) }
    var activeDmMeta by remember { mutableStateOf<DmThread?>(null) }
    var dmMessages by remember { mutableStateOf<List<DmMessage>>(emptyList()) }
    var dmActionTarget by remember { mutableStateOf<DmMessage?>(null) }
    var dmEditTarget by remember { mutableStateOf<DmMessage?>(null) }
    val dmListState = rememberLazyListState()

    val listState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    var lastObservedTailId by remember { mutableStateOf(0L) }
    var lastObservedLocalCount by remember { mutableIntStateOf(0) }
    var pendingNewCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.dmThreads) {
        dmThreads = state.dmThreads
        activeDmId?.let { id ->
            state.dmThreads.firstOrNull { it.id == id }?.let { activeDmMeta = it }
        }
    }

    // Load draft for active group + mode or active DM
    LaunchedEffect(activePane, state.activeGroupId, inputMode, activeDmId, state.editTarget, dmEditTarget) {
        when {
            activePane == "group" && state.editTarget != null -> draft = state.editTarget!!.body
            activePane == "group" -> draft = controller.draftForGroup(state.activeGroupId, inputMode)
            activePane == "dm" && dmEditTarget != null -> draft = dmEditTarget!!.body
        }
    }

    // System Back inside Level-1 Group/DM returns to Level-0 Unified Session List
    BackHandler(enabled = !isDirectEmbedded && activePane != "list") {
        if (showStickerTray || showAttachmentTray) {
            showStickerTray = false
            showAttachmentTray = false
        } else if (historyMsgs != null) {
            historyMsgs = null
        } else {
            activePane = "list"
        }
    }

    val isNearBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || listState.firstVisibleItemIndex <= 2
        }
    }

    val activeRound = guessOverview?.round?.takeIf { it.state == "open" }
    val guessSignalHash = remember(state.messages) {
        state.messages.count { m ->
            val meta = m.parseRedpackMeta()
            meta?.isGuessGame == true || m.systemKind in setOf("ldc_winner", "ldc_cancel", "ldc_expired", "guess_level")
        }
    }

    // Photo / video picker + Document picker
    val mediaUploadPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        if (me == null) {
            onRequireLogin()
            return@rememberLauncherForActivityResult
        }
        mediaUploading = true
        showAttachmentTray = false
        scope.launch {
            val mediaObjs = mutableListOf<kotlinx.serialization.json.JsonObject>()
            val fallbackUrls = mutableListOf<String>()
            for (u in uris.take(6)) {
                val staged = ComposeMediaStager.stageUri(context, u) ?: continue
                val mainFile = File(staged.localFilePath ?: continue)
                if (!mainFile.exists()) continue
                val bytes = withContext(Dispatchers.IO) { mainFile.readBytes() }
                val up = api.uploadMediaBytes(bytes, staged.fileName, staged.mimeType, forWhat = "chat").getOrNull() ?: continue
                var posterKey: String? = null
                if (staged.isVideo && staged.posterLocalPath != null) {
                    val pf = File(staged.posterLocalPath)
                    if (pf.exists()) {
                        val pBytes = withContext(Dispatchers.IO) { pf.readBytes() }
                        posterKey = api.uploadMediaBytes(pBytes, pf.name, "image/jpeg", forWhat = "chat").getOrNull()?.key
                    }
                }
                fallbackUrls += MediaUrlRewriter.rewriteMediaUrl(up.url.ifBlank { "/img/${up.key}" })
                mediaObjs += buildJsonObject {
                    put("key", JsonPrimitive(up.key))
                    if (posterKey != null) put("poster", JsonPrimitive(posterKey))
                    if (staged.width > 0) put("w", JsonPrimitive(staged.width))
                    if (staged.height > 0) put("h", JsonPrimitive(staged.height))
                    put("name", JsonPrimitive(staged.fileName))
                }
            }
            if (mediaObjs.isNotEmpty()) {
                val arr = JsonArray(mediaObjs)
                if (activePane == "dm") {
                    val res = api.sendDmToThreadOrUser(
                        threadId = activeDmId,
                        peerUserId = newDmPeer?.id,
                        body = draft.trim().ifBlank { fallbackUrls.joinToString("\n") },
                        media = arr,
                    )
                    res.onSuccess { (tid, _) ->
                        draft = ""
                        if (activeDmId == null && tid > 0L) {
                            activeDmId = tid
                            newDmPeer = null
                        }
                        controller.refreshDmThreads()
                        if (tid > 0L) {
                            api.fetchDmMessagesFull(tid)?.let { dmResp ->
                                dmMessages = dmResp.resolvedMessages
                                dmResp.thread?.let { activeDmMeta = it }
                            }
                        }
                    }.onFailure { chatStatusBanner = it.message ?: "私信媒体发送失败" }
                } else {
                    val res = api.sendChatMessageWithMedia(state.activeGroupId, draft.trim(), arr, state.replyTarget?.id)
                    res.onSuccess {
                        draft = ""
                        controller.updateGroupDraft(state.activeGroupId, ChatInputMode.CHAT, "")
                        controller.setReplyTarget(null)
                        controller.pollOnce(state.activeGroupId)
                    }.onFailure { chatStatusBanner = it.message ?: "发送图片失败" }
                }
            } else {
                chatStatusBanner = "图片上传失败"
            }
            mediaUploading = false
        }
    }

    val fileUploadPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (me == null) {
            onRequireLogin()
            return@rememberLauncherForActivityResult
        }
        mediaUploading = true
        showAttachmentTray = false
        scope.launch {
            val (name, mime, bytes) = withContext(Dispatchers.IO) {
                val resolvedName = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                } ?: "file_${System.currentTimeMillis()}"
                val resolvedMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                Triple(resolvedName, resolvedMime, data)
            }
            if (bytes.isEmpty() || bytes.size > 25 * 1024 * 1024) {
                mediaUploading = false
                chatStatusBanner = if (bytes.isEmpty()) "无法读取该文件" else "文件大小超过 25MB 限制"
                return@launch
            }
            val upRes = api.uploadMediaBytes(bytes, name, mime, forWhat = "chat")
            val up = upRes.getOrNull()
            if (up != null) {
                val arr = JsonArray(
                    listOf(
                        buildJsonObject {
                            put("key", JsonPrimitive(up.key))
                            put("u", JsonPrimitive(up.url))
                            put("k", JsonPrimitive("f"))
                            put("n", JsonPrimitive(name))
                            put("s", JsonPrimitive(bytes.size.toLong()))
                        },
                    ),
                )
                if (activePane == "dm") {
                    api.sendDmToThreadOrUser(
                        threadId = activeDmId,
                        peerUserId = newDmPeer?.id,
                        body = draft.trim(),
                        media = arr,
                    ).onSuccess { (tid, _) ->
                        draft = ""
                        if (activeDmId == null && tid > 0L) {
                            activeDmId = tid
                            newDmPeer = null
                        }
                        controller.refreshDmThreads()
                        if (tid > 0L) {
                            api.fetchDmMessagesFull(tid)?.let { dmMessages = it.resolvedMessages }
                        }
                    }
                } else {
                    api.sendChatMessageWithMedia(state.activeGroupId, draft.trim(), arr, state.replyTarget?.id).onSuccess {
                        draft = ""
                        controller.updateGroupDraft(state.activeGroupId, ChatInputMode.CHAT, "")
                        controller.setReplyTarget(null)
                        controller.pollOnce(state.activeGroupId)
                    }
                }
            } else {
                chatStatusBanner = upRes.exceptionOrNull()?.message ?: "文件上传失败"
            }
            mediaUploading = false
        }
    }

    // Lifecycle & polling
    DisposableEffect(Unit) {
        controller.startPolling()
        onDispose {
            controller.isChatForegroundAtBottom = false
            controller.stopPolling()
        }
    }

    LaunchedEffect(
        activePane,
        isNearBottom,
        fullscreenImg,
        fullscreenVideoPair,
        showSearchDialog,
        showGroupInfoDialog,
        historyMsgs,
        showGuessSheet,
        redpackDialogMsgId,
        showCreateRedpackDialog,
        inlinePostMemeId,
        actionSheetMsg,
        boostComposerMsg,
        repliesSheetMsgId,
    ) {
        val foregroundBottom = activePane == "group" &&
            isNearBottom &&
            fullscreenImg == null &&
            fullscreenVideoPair == null &&
            !showSearchDialog &&
            !showGroupInfoDialog &&
            historyMsgs == null &&
            !showGuessSheet &&
            redpackDialogMsgId == null &&
            !showCreateRedpackDialog &&
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

    // Reset per-group local states & fetch guess overview + title raffles when active group changes
    LaunchedEffect(activePane, state.activeGroupId, guessSignalHash) {
        if (activePane == "group") {
            val ov = api.fetchGuessOverview(state.activeGroupId)
            guessOverview = ov
            controller.updateActiveGuessRound(state.activeGroupId, ov?.round)
            if (ov?.round?.state != "open" && inputMode != ChatInputMode.CHAT) {
                inputMode = ChatInputMode.CHAT
            }
            activeRaffles = api.fetchActiveTitleRaffles(state.activeGroupId)
        }
    }

    LaunchedEffect(activeRound?.messageId, activePane) {
        while (isActive && activePane == "group" && activeRound != null) {
            guessNowTickSec = ServerClock.serverNowSec()
            delay(1_000L)
        }
    }

    // Auto-collapse squeezed Guess Intel Board when IME opens, restore on close
    LaunchedEffect(isImeVisible) {
        if (activePane == "group" && activeRound != null) {
            if (isImeVisible) {
                wasGuessExpandedBeforeIme = isGuessExpanded
                isGuessExpanded = false
            } else if (wasGuessExpandedBeforeIme) {
                isGuessExpanded = true
            }
        }
    }

    // Clear banner feedback after 2.2s
    LaunchedEffect(guessReceiptBanner) {
        if (guessReceiptBanner != null) {
            delay(2200L)
            guessReceiptBanner = null
        }
    }
    LaunchedEffect(chatStatusBanner) {
        if (chatStatusBanner != null) {
            delay(2600L)
            chatStatusBanner = null
        }
    }
    LaunchedEffect(highlightedMsgId) {
        if (highlightedMsgId != null) {
            delay(2000L)
            highlightedMsgId = null
        }
    }

    // Reset tail counters when switching groups
    LaunchedEffect(state.activeGroupId, activePane) {
        historyMsgs = null
        pendingNewCount = 0
        lastObservedTailId = 0L
        lastObservedLocalCount = 0
        if (activePane == "group" && state.messages.isNotEmpty()) {
            listState.scrollToItem(0)
            lastObservedTailId = state.messages.lastOrNull { it.id > 0L }?.id ?: 0L
            lastObservedLocalCount = state.messages.count { it.id <= 0L }
        }
    }

    // reverseLayout = true tail scroll policy
    val newestServerId = remember(state.messages) { state.messages.lastOrNull { it.id > 0L }?.id ?: 0L }
    val localMsgCount = remember(state.messages) { state.messages.count { it.id <= 0L } }
    LaunchedEffect(newestServerId, localMsgCount, activePane, state.scrollPolicy) {
        if (activePane != "group" || state.messages.isEmpty() || state.isLoadingHistory) return@LaunchedEffect
        val sentOwn = localMsgCount > lastObservedLocalCount
        val arrivedNew = newestServerId > lastObservedTailId && lastObservedTailId > 0L
        when {
            sentOwn -> {
                listState.animateScrollToItem(0)
                pendingNewCount = 0
            }
            arrivedNew -> {
                val noModalOpen = fullscreenImg == null && fullscreenVideoPair == null &&
                    inlinePostMemeId == null && historyMsgs == null && !showSearchDialog
                val shouldFollow = when (state.scrollPolicy) {
                    ChatScrollPolicy.AUTO_FOLLOW -> noModalOpen
                    ChatScrollPolicy.SMART -> noModalOpen && isNearBottom
                    ChatScrollPolicy.MANUAL -> noModalOpen && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 8
                }
                if (shouldFollow) {
                    listState.animateScrollToItem(0)
                    pendingNewCount = 0
                    controller.acknowledgeGroupRead(state.activeGroupId, newestServerId)
                } else {
                    val newly = state.messages.count { it.id in (lastObservedTailId + 1)..newestServerId && !it.mine && !it.deleted }
                    if (newly > 0) pendingNewCount += newly
                }
            }
        }
        lastObservedTailId = newestServerId
        lastObservedLocalCount = localMsgCount
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

    LaunchedEffect(me?.id) {
        if (me != null) {
            controller.refreshDmThreads()
        }
    }

    LaunchedEffect(forcedChannelMode, forcedDmThreadId) {
        if (forcedChannelMode != null) activePane = forcedChannelMode
        if (forcedDmThreadId != null) {
            newDmPeer = null
            activeDmId = forcedDmThreadId
        }
    }

    // Active DM polling + read ack
    LaunchedEffect(activePane, activeDmId, me?.id) {
        if (activePane == "dm" && me != null) {
            val id = activeDmId ?: return@LaunchedEffect
            while (isActive) {
                val resp = api.fetchDmMessagesFull(id)
                if (resp != null) {
                    dmMessages = resp.resolvedMessages
                    resp.thread?.let { activeDmMeta = it }
                    val newestDm = resp.resolvedMessages.lastOrNull { it.id > 0L }?.id ?: 0L
                    if (newestDm > 0L && api.markDmRead(id, newestDm)) {
                        val updated = state.dmThreads.map { if (it.id == id) it.copy(unread = 0) else it }
                        controller.updateDmThreadsLocal(updated)
                    }
                }
                delay(4000L)
            }
        }
    }

    fun jumpToMessage(targetId: Long, groupId: Long = state.activeGroupId) {
        if (targetId <= 0L) return
        activePane = "group"
        val localIdx = if (groupId == state.activeGroupId) {
            state.messages.indexOfFirst { it.id == targetId }
        } else -1
        if (localIdx >= 0) {
            historyMsgs = null
            highlightedMsgId = targetId
            val revIdx = (state.messages.size - 1 - localIdx).coerceAtLeast(0)
            scope.launch { listState.animateScrollToItem(revIdx) }
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
        activePane = "group"
        showSearchDialog = false
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
        jump.messageId?.takeIf { it > 0L }?.let { mid -> jumpToMessage(mid, targetGroupId) }
        onConsumeInitialJump()
    }

    LaunchedEffect(openSessionRequest?.requestId) {
        val req = openSessionRequest ?: return@LaunchedEffect
        when (req.mode) {
            "list" -> {
                req.filter?.let { listFilter = it }
                activePane = "list"
            }
            "dm" -> {
                newDmPeer = null
                activeDmId = req.dmThreadId ?: state.dmThreads.firstOrNull()?.id
                activePane = "dm"
            }
            else -> {
                activePane = "group"
                if (req.groupId > 0L && state.activeGroupId != req.groupId) {
                    controller.setActiveGroup(req.groupId)
                }
                req.jumpMessageId?.takeIf { it > 0L }?.let { mid ->
                    jumpToMessage(mid, req.groupId)
                }
            }
        }
        onConsumeOpenSessionRequest()
    }

    fun submitActiveDraft() {
        if (me == null) {
            onRequireLogin()
            return
        }
        val txt = draft.trim()
        if (txt.isEmpty()) return

        if (activePane == "group" && activeRound != null && inputMode != ChatInputMode.CHAT && state.editTarget == null) {
            val normalized = if (txt.startsWith("!") || txt.startsWith("！") ||
                txt.startsWith("?") || txt.startsWith("？") ||
                txt.startsWith("@欢欢") || txt.startsWith("＠欢欢")
            ) {
                txt
            } else {
                (if (inputMode == ChatInputMode.GUESS) "!" else "?") + txt
            }
            draft = ""
            controller.updateGroupDraft(state.activeGroupId, inputMode, "")
            showStickerTray = false
            showAttachmentTray = false
            scope.launch {
                val res = api.submitGuessInput(activeRound.messageId, normalized)
                res.onSuccess {
                    guessReceiptBanner = if (disguiseMode != AppDisguiseMode.NONE) {
                        if (inputMode == ChatInputMode.GUESS) "已提交 · 正在处理…" else "已提问 · 等待回复…"
                    } else {
                        if (inputMode == ChatInputMode.GUESS) "已提交猜测 · 欢欢 AI 判词中…" else "已向欢欢提问 · 等待回复…"
                    }
                    val ov = api.fetchGuessOverview(state.activeGroupId, messageId = activeRound.messageId, receiptId = it)
                    guessOverview = ov
                    controller.updateActiveGuessRound(state.activeGroupId, ov?.round)
                    ov?.receipt?.reply?.text?.takeIf { s -> s.isNotBlank() }?.let { replyText ->
                        guessReceiptBanner = if (disguiseMode != AppDisguiseMode.NONE) {
                            "回复：$replyText"
                        } else {
                            "欢欢：$replyText"
                        }
                    }
                }.onFailure { err ->
                    guessReceiptBanner = err.message ?: "提交失败"
                }
            }
            return
        }

        if (activePane == "group") {
            draft = ""
            controller.updateGroupDraft(state.activeGroupId, ChatInputMode.CHAT, "")
            showStickerTray = false
            showAttachmentTray = false
            scope.launch {
                controller.sendOrEditMessage(txt)
            }
        } else if (activePane == "dm") {
            val editing = dmEditTarget
            draft = ""
            showStickerTray = false
            showAttachmentTray = false
            scope.launch {
                if (editing != null) {
                    dmEditTarget = null
                    if (api.editDmMessage(editing.id, txt)) {
                        activeDmId?.let { tid ->
                            api.fetchDmMessagesFull(tid)?.let { dmMessages = it.resolvedMessages }
                        }
                    } else {
                        chatStatusBanner = "编辑私信失败"
                    }
                } else {
                    val res = api.sendDmToThreadOrUser(
                        threadId = activeDmId,
                        peerUserId = newDmPeer?.id,
                        body = txt,
                    )
                    res.onSuccess { (resolvedTid, _) ->
                        if (activeDmId == null && resolvedTid > 0L) {
                            activeDmId = resolvedTid
                            newDmPeer = null
                        }
                        controller.refreshDmThreads()
                        val targetTid = if (resolvedTid > 0L) resolvedTid else activeDmId
                        if (targetTid != null && targetTid > 0L) {
                            api.fetchDmMessagesFull(targetTid)?.let { resp ->
                                dmMessages = resp.resolvedMessages
                                resp.thread?.let { activeDmMeta = it }
                            }
                        }
                    }.onFailure { err ->
                        chatStatusBanner = err.message ?: "发送私信失败"
                    }
                }
            }
        }
    }

    // Level-0 Unified `消息` Session List
    if (activePane == "list" && !isDirectEmbedded) {
        MessagesListView(
            state = state,
            controller = controller,
            api = api,
            me = me,
            notifUnreadCount = notifUnreadCount,
            selectedFilter = listFilter,
            onSelectFilter = { listFilter = it },
            onOpenGroup = { gid ->
                historyMsgs = null
                controller.setActiveGroup(gid)
                activePane = "group"
            },
            onOpenDmThread = { thread ->
                newDmPeer = null
                activeDmId = thread.id
                activeDmMeta = thread
                dmMessages = emptyList()
                activePane = "dm"
            },
            onStartNewDmUser = { peer ->
                newDmPeer = peer
                activeDmId = null
                activeDmMeta = null
                dmMessages = emptyList()
                activePane = "dm"
            },
            onOpenNotifications = onOpenNotifications,
            onOpenGroupSearchJump = { gid, mid ->
                if (state.activeGroupId != gid) controller.setActiveGroup(gid)
                jumpToMessage(mid, gid)
            },
            onRequireLogin = onRequireLogin,
        )
        return
    }

    val activeGroup = state.groups.firstOrNull { it.id == state.activeGroupId }
    val activeRedpackMsg = remember(state.messages) { state.messages.lastOrNull { it.redpack != null && !it.deleted } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        // Level-1 Compact 48dp Chat / DM Top Bar (hidden when inside FeishuDisguiseScreen)
        if (!hideChannelHeader) {
            Surface(color = palette.surface) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ForHappyDimens.topBarHeight)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        IconButton(
                            onClick = { activePane = "list" },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回消息列表", tint = palette.text)
                        }

                        if (activePane == "group") {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { if (activeGroup != null) showGroupInfoDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = buildString {
                                        append(activeGroup?.name ?: "群聊")
                                        if ((activeGroup?.memberCount ?: 0) > 0) append(" (${activeGroup!!.memberCount})")
                                    },
                                    color = palette.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { showSearchDialog = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.Search, contentDescription = "搜索记录", tint = palette.text, modifier = Modifier.size(19.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (activeRedpackMsg != null && activeRedpackMsg.parseRedpackMeta()?.isGuessGame != true) {
                                        redpackFallbackMeta = activeRedpackMsg.parseRedpackMeta()
                                        redpackSenderName = activeRedpackMsg.displayAuthor
                                        redpackDialogMsgId = activeRedpackMsg.id
                                    } else {
                                        showRedpackArchiveSheet = true
                                    }
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.CardGiftcard, contentDescription = "红包", tint = palette.redpacket, modifier = Modifier.size(19.dp))
                            }
                            IconButton(
                                onClick = {
                                    guessInitialMid = activeRound?.messageId
                                        ?: activeRedpackMsg?.takeIf { it.parseRedpackMeta()?.isGuessGame == true }?.id
                                    showGuessSheet = true
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = "欢欢猜词屋", tint = palette.gold, modifier = Modifier.size(19.dp))
                            }
                            IconButton(
                                onClick = { if (activeGroup != null) showGroupInfoDialog = true },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.People, contentDescription = "群信息与成员", tint = palette.text, modifier = Modifier.size(19.dp))
                            }
                        } else {
                            val dmMeta = activeDmMeta ?: state.dmThreads.firstOrNull { it.id == activeDmId }
                            val dmTitle = dmMeta?.peerName?.takeIf { it.isNotBlank() }
                                ?: dmMeta?.peerUsername
                                ?: newDmPeer?.displayName
                                ?: "私信"
                            val dmMuted = dmMeta != null && (dmMeta.muted || dmMeta.id in state.mutedDmIds)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ForHappyAvatar(
                                    username = dmTitle,
                                    avatarUrl = dmMeta?.peerAvatar ?: newDmPeer?.avatar,
                                    imageLoader = api.imageLoader,
                                    size = 28.dp,
                                )
                                Text(
                                    text = dmTitle,
                                    color = palette.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (dmMeta != null) {
                                IconButton(
                                    onClick = { controller.setDmMutedLocal(dmMeta.id, !dmMuted) },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(
                                        imageVector = if (dmMuted) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                                        contentDescription = "免打扰开关",
                                        tint = if (dmMuted) palette.muted else palette.text,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = palette.line.copy(alpha = 0.7f))
                }
            }
        }

        chatStatusBanner?.let { banner ->
            Surface(color = palette.primaryContainer) {
                Text(
                    text = banner,
                    color = palette.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        // Main Chat / DM Viewport
        if (activePane == "group") {
            val activeHistory = historyMsgs
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val maxGuessHeightDp = (maxHeight * 0.50f).coerceAtLeast(160.dp)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Active Title Raffle banner (top of group)
                    if (activeHistory == null && activeRaffles.isNotEmpty()) {
                        val raffle = activeRaffles.first()
                        Surface(color = palette.gold.copy(alpha = 0.14f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = palette.gold, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "头衔抽奖「${raffle.resolvedTitleName}」· ${raffle.resolvedParticipantCount}人已报名",
                                    color = palette.text,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    color = if (raffle.joined) palette.surface2 else palette.gold,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.clickable(enabled = !raffle.joined) {
                                        if (me == null) {
                                            onRequireLogin()
                                        } else {
                                            scope.launch {
                                                chatStatusBanner = api.joinActiveTitleRaffle(raffle.id)
                                                activeRaffles = api.fetchActiveTitleRaffles(state.activeGroupId)
                                            }
                                        }
                                    },
                                ) {
                                    Text(
                                        text = if (raffle.joined) "已报名" else "一键报名",
                                        color = if (raffle.joined) palette.muted else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = palette.line.copy(alpha = 0.5f))
                    }

                    // Co-Screen Squeezed Guess Room Strip / Board
                    if (activeRound != null && activeHistory == null) {
                        if (!isGuessExpanded || isImeVisible) {
                            GuessStatusStrip(
                                round = activeRound,
                                nowTickSec = guessNowTickSec,
                                onExpand = {
                                    isGuessExpanded = true
                                    wasGuessExpandedBeforeIme = true
                                },
                                onOpenFullPanel = {
                                    guessInitialMid = activeRound.messageId
                                    showGuessSheet = true
                                },
                            )
                        } else {
                            GuessIntelBoard(
                                round = activeRound,
                                overview = guessOverview,
                                nowTickSec = guessNowTickSec,
                                boardHeightDp = guessBoardHeightDp,
                                maxHeightDp = maxGuessHeightDp,
                                onHeightChangeDp = { guessBoardHeightDp = it },
                                hintBusy = guessHintBusy,
                                closeVoteBusy = guessCloseBusy,
                                onCollapse = {
                                    isGuessExpanded = false
                                    wasGuessExpandedBeforeIme = false
                                },
                                onOpenFullPanel = {
                                    guessInitialMid = activeRound.messageId
                                    showGuessSheet = true
                                },
                                onPrefillGuessWord = { word ->
                                    inputMode = ChatInputMode.GUESS
                                    draft = word
                                    controller.updateGroupDraft(state.activeGroupId, ChatInputMode.GUESS, word)
                                },
                                onRequestAiHint = {
                                    if (me == null) {
                                        onRequireLogin()
                                        return@GuessIntelBoard
                                    }
                                    guessHintBusy = true
                                    scope.launch {
                                        val res = api.submitGuessInput(activeRound.messageId, "@欢欢 线索")
                                        guessHintBusy = false
                                        res.onSuccess { eid ->
                                            guessReceiptBanner = "已向欢欢要线索…"
                                            val ov = api.fetchGuessOverview(state.activeGroupId, activeRound.messageId, receiptId = eid)
                                            guessOverview = ov
                                        }.onFailure { err ->
                                            guessReceiptBanner = err.message ?: "要线索失败"
                                        }
                                    }
                                },
                                onVoteCloseRound = { actionKey ->
                                    if (me == null) {
                                        onRequireLogin()
                                        return@GuessIntelBoard
                                    }
                                    guessCloseBusy = true
                                    scope.launch {
                                        val res = api.postGuessCloseAction(activeRound.messageId, actionKey)
                                        guessCloseBusy = false
                                        res.onSuccess {
                                            guessReceiptBanner = "已提交表决"
                                            val ov = api.fetchGuessOverview(state.activeGroupId, activeRound.messageId)
                                            guessOverview = ov
                                            controller.updateActiveGuessRound(state.activeGroupId, ov?.round)
                                        }.onFailure { err ->
                                            guessReceiptBanner = err.message ?: "表决失败"
                                        }
                                    }
                                },
                            )
                        }
                    }

                    // Unread entry boundary banner (`>= 5` unread messages when entering)
                    val boundaryId = state.unreadEntryBoundaryByGroup[state.activeGroupId]
                    if (activeHistory == null && boundaryId != null && boundaryId > 0L) {
                        val firstUnreadMsg = remember(state.messages, boundaryId) {
                            state.messages.firstOrNull { it.id > boundaryId && !it.mine && !it.deleted }
                        }
                        if (firstUnreadMsg != null) {
                            Surface(color = palette.primaryContainer) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            controller.clearUnreadBoundary(state.activeGroupId)
                                            jumpToMessage(firstUnreadMsg.id, state.activeGroupId)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "── 离开期间新增未读消息 · 跳到第一条未读 ──",
                                        color = palette.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    // History `around` Overlay vs Live `reverseLayout = true` Stream
                    if (activeHistory != null) {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Surface(color = palette.gold.copy(alpha = 0.14f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("正在查看定位上下文", color = palette.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Surface(
                                        color = palette.primary,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable { historyMsgs = null },
                                    ) {
                                        Text(
                                            text = "↓ 回到最新",
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
                                                Text("↑ 加载更早消息")
                                            }
                                        }
                                    }
                                }
                                itemsIndexed(activeHistory, key = { _, m -> "hist_${m.id}" }) { idx, msg ->
                                    val prev = if (idx > 0) activeHistory[idx - 1] else null
                                    val cont = canGroupContinuation(prev, msg)
                                    RenderChatItemRow(
                                        msg = msg,
                                        isContinuation = cont,
                                        highlighted = highlightedMsgId == msg.id,
                                        cleanDecoMode = state.cleanDecoMode,
                                        api = api,
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
                                            scope.launch { chatStatusBanner = api.joinActiveTitleRaffle(refId) }
                                        },
                                        onJumpMessage = { jumpToMessage(it) },
                                        onSwipeReply = {
                                            controller.setReplyTarget(msg)
                                            historyMsgs = null
                                        },
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
                                        onOpenRepliesSheet = { repliesSheetMsgId = msg.id },
                                        onPostRefClick = { mid -> inlinePostMemeId = mid },
                                        onHashtagClick = onNavigateToTag,
                                        onLinkClick = onOpenUrl,
                                        onOpenNovelExcerpt = { spec ->
                                            val route = "/novels/${spec.bookId}?chapter=${spec.chapter}"
                                            if (onOpenDiscoverRoute != null) onOpenDiscoverRoute(route) else onOpenUrl("https://52dmeme.com$route")
                                        },
                                        onTapAvatar = { m ->
                                            selectedMemberProfile = GroupMemberItem(
                                                id = m.effectiveUserId,
                                                username = m.resolvedUsername,
                                                name = m.displayAuthor,
                                                avatar = m.resolvedAvatar,
                                                title = m.resolvedTitle,
                                                deco = m.user?.deco,
                                            )
                                        },
                                        onLongPressAvatar = { m ->
                                            val handle = m.resolvedUsername.ifBlank { m.displayAuthor }
                                            draft += "@$handle "
                                            controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
                                        },
                                    )
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
                                                Text("↓ 加载更晚消息")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Live `reverseLayout = true` chat list: prepending older history causes zero viewport jump!
                        val reversedMsgs = remember(state.messages) { state.messages.asReversed() }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = listState,
                                reverseLayout = true,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                itemsIndexed(
                                    reversedMsgs,
                                    key = { _, m -> if (m.id > 0L) "srv_${m.id}" else "loc_${m.localId ?: m.createdAt}" },
                                ) { revIdx, msg ->
                                    val prevChrono = reversedMsgs.getOrNull(revIdx + 1)
                                    val cont = canGroupContinuation(prevChrono, msg)
                                    Column {
                                        if (!cont && revIdx < reversedMsgs.lastIndex) {
                                            Spacer(Modifier.height(5.dp))
                                        }
                                        RenderChatItemRow(
                                            msg = msg,
                                            isContinuation = cont,
                                            highlighted = msg.id > 0L && highlightedMsgId == msg.id,
                                            cleanDecoMode = state.cleanDecoMode,
                                            api = api,
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
                                                scope.launch { chatStatusBanner = api.joinActiveTitleRaffle(refId) }
                                            },
                                            onJumpMessage = { jumpToMessage(it) },
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
                                            onRetrySend = {
                                                msg.localId?.let { lid -> scope.launch { controller.retryFailedMessage(lid) } }
                                            },
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
                                            onOpenRepliesSheet = { repliesSheetMsgId = msg.id },
                                            onPostRefClick = { mid -> inlinePostMemeId = mid },
                                            onHashtagClick = onNavigateToTag,
                                            onLinkClick = onOpenUrl,
                                            onOpenNovelExcerpt = { spec ->
                                                val route = "/novels/${spec.bookId}?chapter=${spec.chapter}"
                                                if (onOpenDiscoverRoute != null) onOpenDiscoverRoute(route) else onOpenUrl("https://52dmeme.com$route")
                                            },
                                            onTapAvatar = { m ->
                                                selectedMemberProfile = GroupMemberItem(
                                                    id = m.effectiveUserId,
                                                    username = m.resolvedUsername,
                                                    name = m.displayAuthor,
                                                    avatar = m.resolvedAvatar,
                                                    title = m.resolvedTitle,
                                                    deco = m.user?.deco,
                                                )
                                            },
                                            onLongPressAvatar = { m ->
                                                val handle = m.resolvedUsername.ifBlank { m.displayAuthor }
                                                draft += "@$handle "
                                                controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
                                            },
                                        )
                                    }
                                }

                                // Visual TOP of reverseLayout LazyColumn
                                if (state.hasMoreOlder) {
                                    item(key = "older_header") {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            TextButton(
                                                enabled = !state.isLoadingHistory,
                                                onClick = { scope.launch { controller.loadOlderHistory() } },
                                            ) {
                                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                                                Text(if (state.isLoadingHistory) "加载更早消息中…" else "查看更早消息", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Floating jump-to-latest pill
                            if (!isNearBottom && state.messages.isNotEmpty()) {
                                Surface(
                                    color = palette.primary,
                                    shape = RoundedCornerShape(999.dp),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 12.dp, bottom = 10.dp)
                                        .clickable {
                                            scope.launch {
                                                listState.animateScrollToItem(0)
                                                pendingNewCount = 0
                                                controller.acknowledgeGroupRead(state.activeGroupId)
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
                                            text = if (pendingNewCount > 0) "↓ $pendingNewCount 回到最新" else "回到最新",
                                            color = palette.onPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Active 1:1 Direct Message Pane (`UTC+8` date dividers + read receipts + edit/recall)
            val dmMeta = activeDmMeta ?: state.dmThreads.firstOrNull { it.id == activeDmId }
            val peerLastReadId = dmMeta?.peerLastRead ?: 0L
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (me == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = onRequireLogin) { Text("登录后查看私信") }
                    }
                } else if (dmMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (newDmPeer != null) "给 ${newDmPeer!!.displayName} 发一句打招呼吧" else "暂无私信消息",
                            color = palette.muted,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    val reversedDms = remember(dmMessages) { dmMessages.asReversed() }
                    LazyColumn(
                        state = dmListState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(reversedDms, key = { _, d -> "dm_${d.id}" }) { revIdx, dm ->
                            val isMine = dm.senderId == me.id
                            val olderDm = reversedDms.getOrNull(revIdx + 1)
                            val showDateDivider = olderDm == null || !isSameShanghaiDay(olderDm.createdAt, dm.createdAt)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (showDateDivider && dm.createdAt > 0L) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    ) {
                                        Surface(color = palette.surface2, shape = RoundedCornerShape(999.dp)) {
                                            Text(
                                                text = formatShanghaiDateDivider(dm.createdAt),
                                                color = palette.muted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                                ) {
                                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                                        Surface(
                                            color = if (isMine) palette.primaryContainer else palette.surface,
                                            shape = RoundedCornerShape(15.dp),
                                            modifier = Modifier
                                                .widthIn(max = 300.dp)
                                                .border(
                                                    1.dp,
                                                    if (isMine) palette.primary.copy(alpha = 0.35f) else palette.line,
                                                    RoundedCornerShape(15.dp),
                                                )
                                                .combinedClickable(
                                                    onClick = {},
                                                    onLongClick = { if (!dm.deleted) dmActionTarget = dm },
                                                ),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                if (dm.deleted) {
                                                    Text(
                                                        text = if (isMine) "你撤回了一条私信" else "对方撤回了一条私信",
                                                        color = palette.muted,
                                                        fontSize = 13.sp,
                                                    )
                                                } else {
                                                    if (dm.body.isNotBlank()) {
                                                        RichChatBodyView(
                                                            rawBody = dm.body,
                                                            imageLoader = api.imageLoader,
                                                            textColor = palette.text,
                                                            isEdited = dm.edited,
                                                            onPostRefClick = { mid -> inlinePostMemeId = mid },
                                                            onHashtagClick = onNavigateToTag,
                                                            onLinkClick = onOpenUrl,
                                                            onInlineImageClick = { url -> fullscreenImg = url },
                                                            onLongPress = { dmActionTarget = dm },
                                                        )
                                                    }
                                                    dm.media.filter { it.k != "f" }.forEach { med ->
                                                        ChatVisualMediaCell(
                                                            msgId = dm.id,
                                                            media = med,
                                                            solo = true,
                                                            api = api,
                                                            onOpenImage = { url -> fullscreenImg = url },
                                                            onOpenVideo = { p, v -> fullscreenVideoPair = p to v },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                                        ) {
                                            Text(
                                                text = formatShanghaiClock(dm.createdAt),
                                                color = palette.muted.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                            )
                                            if (isMine && !dm.deleted && peerLastReadId > 0L) {
                                                val readByPeer = peerLastReadId >= dm.id
                                                Text(
                                                    text = if (readByPeer) "已读" else "未读",
                                                    color = if (readByPeer) palette.primary else palette.muted,
                                                    fontSize = 10.sp,
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
        }

        // Reply / Edit target bar
        val reply = state.replyTarget
        val edit = state.editTarget
        if ((activePane == "group" && (reply != null || edit != null)) || (activePane == "dm" && dmEditTarget != null)) {
            Surface(color = palette.surface2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            dmEditTarget != null -> "编辑私信：${dmEditTarget!!.body}"
                            edit != null -> "编辑消息：${edit.body}"
                            else -> "回复 ${reply?.displayAuthor}：${reply?.body}"
                        },
                        color = palette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (dmEditTarget != null) {
                                dmEditTarget = null
                                draft = ""
                            } else {
                                controller.setReplyTarget(null)
                                controller.setEditTarget(null)
                                if (edit != null) draft = ""
                            }
                        },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "取消", tint = palette.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // `[聊天 | 猜 | 问]` input mode bar when active Guess Room round runs
        if (activePane == "group" && activeRound != null && state.editTarget == null) {
            GuessInputModeBar(
                activeMode = inputMode,
                canPlay = activeRound.canPlay,
                receiptBanner = guessReceiptBanner,
                onSelectMode = { next ->
                    controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
                    inputMode = next
                    draft = controller.draftForGroup(state.activeGroupId, next)
                },
            )
        }

        // Sticker tray (Group & DM!)
        if (showStickerTray) {
            Surface(
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .border(1.dp, palette.line),
            ) {
                if (disguiseMode != AppDisguiseMode.NONE) {
                    val safeEmojis = remember {
                        listOf("👍", "😂", "✅", "🎉", "👏", "🤔", "❤️", "🔥", "👌", "🙌", "💡", "📌", "🤝", "☕", "🚀", "👀", "😊", "🙏")
                    }
                    val quickPhrases = remember {
                        listOf("收到", "没问题", "好的", "辛苦了", "赞同", "我看看")
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickPhrases, key = { it }) { phrase ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        draft += phrase
                                        if (activePane == "group") controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
                                    },
                                    label = { Text(phrase, fontSize = 11.sp) },
                                )
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(44.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(safeEmojis, key = { it }) { emoji ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            draft += emoji
                                            if (activePane == "group") controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
                                        },
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                } else {
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
                            itemsIndexed(savedStickers, key = { idx, st -> "fav_${idx}_${st.messageId}_${st.index}" }) { _, st ->
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
                                                if (activePane == "group") controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
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
                            items(selectedPack.items.distinct().take(120), key = { "${selectedPack.tabKey}:$it" }) { itemName ->
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
                                            if (activePane == "group") controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
                                        }
                                        .padding(2.dp),
                                )
                            }
                        }
                    }
                }
                }
            }
        }

        // `[＋]` Attachment Tray (`相册`, `文件`, `发红包`, `欢欢猜词`)
        AnimatedVisibility(visible = showAttachmentTray) {
            Surface(
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.line.copy(alpha = 0.7f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ComposerAttachmentTile(
                        icon = Icons.Rounded.Image,
                        label = if (mediaUploading) "上传中…" else "相册",
                        tint = palette.primary,
                        enabled = !mediaUploading,
                        onClick = {
                            StealthExemptionGate.markInternalJump()
                            mediaUploadPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                    )
                    ComposerAttachmentTile(
                        icon = Icons.Rounded.AttachFile,
                        label = "文件",
                        tint = palette.accent,
                        enabled = !mediaUploading,
                        onClick = {
                            StealthExemptionGate.markInternalJump()
                            fileUploadPicker.launch("*/*")
                        },
                    )
                    if (activePane == "group") {
                        ComposerAttachmentTile(
                            icon = Icons.Rounded.CardGiftcard,
                            label = if (disguiseMode == AppDisguiseMode.LINUX_DO) "互动卡片" else "发红包",
                            tint = palette.redpacket,
                            enabled = !mediaUploading,
                            onClick = {
                                if (me == null) {
                                    onRequireLogin()
                                } else {
                                    showAttachmentTray = false
                                    showCreateRedpackDialog = true
                                }
                            },
                        )
                        ComposerAttachmentTile(
                            icon = Icons.Rounded.AutoAwesome,
                            label = when (disguiseMode) {
                                AppDisguiseMode.FEISHU -> "群待办"
                                AppDisguiseMode.LINUX_DO -> "发起投票"
                                AppDisguiseMode.NONE -> "猜词屋"
                            },
                            tint = palette.gold,
                            enabled = !mediaUploading,
                            onClick = {
                                showAttachmentTray = false
                                guessInitialMid = activeRound?.messageId
                                showGuessSheet = true
                            },
                        )
                    }
                }
            }
        }

        // Slim Composer Capsule (`Emoji` + Input + `[＋]` + `Send`)
        Surface(color = palette.surface) {
            val swipeThresholdPx = with(density) { 48.dp.toPx() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                IconButton(
                    onClick = {
                        showAttachmentTray = false
                        showStickerTray = !showStickerTray
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        Icons.Rounded.EmojiEmotions,
                        contentDescription = "表情",
                        tint = if (showStickerTray) palette.primary else palette.muted,
                        modifier = Modifier.size(21.dp),
                    )
                }

                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, palette.line.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                        .pointerInput(activePane, activeRound?.messageId, draft, inputMode) {
                            // Empty input horizontal swipe >= 48dp cycles 聊天 <-> 猜 <-> 问
                            if (activePane == "group" && activeRound != null && draft.isEmpty()) {
                                var totalDx = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDx = 0f },
                                    onDragEnd = {
                                        if (kotlin.math.abs(totalDx) >= swipeThresholdPx) {
                                            val modes = if (activeRound.canPlay) {
                                                listOf(ChatInputMode.CHAT, ChatInputMode.GUESS, ChatInputMode.ASK)
                                            } else {
                                                listOf(ChatInputMode.CHAT)
                                            }
                                            if (modes.size > 1) {
                                                val idx = modes.indexOf(inputMode)
                                                val nextIdx = if (totalDx < 0f) (idx + 1) % modes.size else (idx - 1 + modes.size) % modes.size
                                                inputMode = modes[nextIdx]
                                                draft = controller.draftForGroup(state.activeGroupId, inputMode)
                                            }
                                        }
                                        totalDx = 0f
                                    },
                                    onDragCancel = { totalDx = 0f },
                                    onHorizontalDrag = { _, delta -> totalDx += delta },
                                )
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = when {
                                    me == null -> "登录后参与聊天"
                                    activePane == "dm" -> "发送私信…"
                                    inputMode == ChatInputMode.GUESS -> if (disguiseMode != AppDisguiseMode.NONE) "输入待提交内容…" else "输入猜词答案（自动加 !）"
                                    inputMode == ChatInputMode.ASK -> if (disguiseMode != AppDisguiseMode.NONE) "输入提问内容…" else "向欢欢提一个是非问题…"
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
                            onValueChange = { next ->
                                draft = next
                                if (activePane == "group" && state.editTarget == null) {
                                    controller.updateGroupDraft(state.activeGroupId, inputMode, next)
                                }
                            },
                            maxLines = 4,
                            textStyle = TextStyle(color = palette.text, fontSize = 14.sp),
                            cursorBrush = SolidColor(palette.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        showStickerTray = false
                        showAttachmentTray = !showAttachmentTray
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = if (showAttachmentTray) Icons.Rounded.Close else Icons.Rounded.Add,
                        contentDescription = "更多功能",
                        tint = if (showAttachmentTray) palette.primary else palette.muted,
                        modifier = Modifier.size(21.dp),
                    )
                }

                IconButton(
                    onClick = { submitActiveDraft() },
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

    // Fullscreen viewers & secondary dialogs/sheets
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
            onOpenBoostHistory = {
                actionSheetMsg = null
                boostHistoryMsg = targetMsg
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
                controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
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
                    chatStatusBanner = "已复制消息内容"
                }
            },
            onSaveMedia = targetMsg.allMedia.firstOrNull { it.k != "f" && it.k != "v" && it.v.isNullOrBlank() }?.resolvedUrl?.takeIf { it.isNotBlank() }?.let { rawUrl ->
                {
                    actionSheetMsg = null
                    scope.launch {
                        val rewritten = MediaUrlRewriter.rewriteWithExtProxy(rawUrl)
                        saveMediaToSystemGallery(context, api, rewritten).fold(
                            onSuccess = { chatStatusBanner = "已保存图片到系统相册" },
                            onFailure = { chatStatusBanner = it.message ?: "保存图片失败" },
                        )
                    }
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
            onConvertToPost = if (targetMsg.allMedia.isNotEmpty() && onConvertMessageToPost != null) {
                {
                    actionSheetMsg = null
                    historyMsgs = null
                    onConvertMessageToPost(targetMsg)
                }
            } else null,
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

    boostHistoryMsg?.let { targetMsg ->
        BoostHistoryDialog(
            msg = targetMsg,
            api = api,
            onDismiss = { boostHistoryMsg = null },
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

    if (showSearchDialog) {
        ChatHistorySearchDialog(
            groupId = state.activeGroupId,
            groupName = activeGroup?.name ?: "群聊",
            api = api,
            onDismiss = { showSearchDialog = false },
            onSelectMessage = { mid ->
                showSearchDialog = false
                jumpToMessage(mid)
            },
        )
    }

    if (showGroupInfoDialog && activeGroup != null) {
        GroupInfoDialog(
            group = activeGroup,
            state = state,
            controller = controller,
            api = api,
            me = me,
            onDismiss = { showGroupInfoDialog = false },
            onOpenRedpackArchive = {
                showGroupInfoDialog = false
                showRedpackArchiveSheet = true
            },
            onSelectMember = { member ->
                selectedMemberProfile = member
            },
            onLeftGroup = {
                showGroupInfoDialog = false
                activePane = "list"
            },
        )
    }

    selectedMemberProfile?.let { member ->
        MemberProfileSheet(
            member = member,
            groupId = state.activeGroupId,
            canKick = activeGroup?.canManage == true || me?.isAdmin == true,
            api = api,
            me = me,
            onDismiss = { selectedMemberProfile = null },
            onMentionUser = { mention ->
                selectedMemberProfile = null
                draft += mention
                if (activePane == "group") controller.updateGroupDraft(state.activeGroupId, inputMode, draft)
            },
            onSendDm = { peerUser ->
                selectedMemberProfile = null
                showGroupInfoDialog = false
                val existing = state.dmThreads.firstOrNull { it.peerId == peerUser.id }
                if (existing != null) {
                    newDmPeer = null
                    activeDmId = existing.id
                    activeDmMeta = existing
                } else {
                    newDmPeer = peerUser
                    activeDmId = null
                    activeDmMeta = null
                    dmMessages = emptyList()
                }
                activePane = "dm"
            },
            onViewWall = { username ->
                selectedMemberProfile = null
                showGroupInfoDialog = false
                onNavigateToAuthor(username)
            },
            onKicked = {
                selectedMemberProfile = null
                chatStatusBanner = "已将该成员移出本群"
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
            onRoundCreatedOrUpdated = {
                scope.launch {
                    controller.pollOnce(state.activeGroupId)
                    val ov = api.fetchGuessOverview(state.activeGroupId)
                    guessOverview = ov
                    controller.updateActiveGuessRound(state.activeGroupId, ov?.round)
                }
            },
        )
    }

    if (showCreateRedpackDialog) {
        CreateRedpackDialog(
            groupId = state.activeGroupId,
            groupName = activeGroup?.name ?: "群聊",
            api = api,
            me = me,
            onDismiss = { showCreateRedpackDialog = false },
            onOpenUrl = onOpenUrl,
            onSendExternalTextToChat = { text ->
                api.sendChatMessage(state.activeGroupId, text, replyToId = null, retry = false).isSuccess
            },
            onCreated = {
                showCreateRedpackDialog = false
                scope.launch { controller.pollOnce(state.activeGroupId) }
            },
        )
    }

    if (showRedpackArchiveSheet) {
        GroupRedpackArchiveSheet(
            groupId = state.activeGroupId,
            api = api,
            onDismiss = { showRedpackArchiveSheet = false },
            onSelectPacketMsgId = { mid ->
                showRedpackArchiveSheet = false
                redpackFallbackMeta = null
                redpackSenderName = null
                redpackDialogMsgId = mid
            },
        )
    }

    redpackDialogMsgId?.let { rMsgId ->
        OpenRedpackCenteredDialog(
            messageId = rMsgId,
            senderName = redpackSenderName,
            fallbackMeta = redpackFallbackMeta,
            api = api,
            onDismiss = { redpackDialogMsgId = null },
            onOpenUrl = onOpenUrl,
            onOpenGuessRoom = { mid ->
                redpackDialogMsgId = null
                guessInitialMid = mid
                showGuessSheet = true
            },
            onOpenFullscreenImage = { url -> fullscreenImg = url },
            onSendPhraseToChat = { phrase ->
                if (me != null && phrase.isNotBlank()) {
                    api.sendChatMessage(state.activeGroupId, phrase, replyToId = null, retry = false)
                }
            },
            onOpenArchive = { showRedpackArchiveSheet = true },
            onUpdated = { scope.launch { controller.pollOnce(state.activeGroupId) } },
        )
    }

    // DM message long-press sheet (`复制 / 编辑 / 撤回`)
    dmActionTarget?.let { targetDm ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isMine = targetDm.senderId == me?.id
        ModalBottomSheet(
            onDismissRequest = { dmActionTarget = null },
            sheetState = sheetState,
            containerColor = palette.bg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("私信操作", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = palette.line)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionMenuTile(icon = Icons.Rounded.ContentCopy, label = "复制", onClick = {
                        clipboard.setText(AnnotatedString(targetDm.body))
                        chatStatusBanner = "已复制私信"
                        dmActionTarget = null
                    })
                    if (isMine && targetDm.body.isNotBlank()) {
                        ActionMenuTile(icon = Icons.Rounded.Edit, label = "编辑", onClick = {
                            dmEditTarget = targetDm
                            draft = targetDm.body
                            dmActionTarget = null
                        })
                    }
                    if (isMine && targetDm.id > 0L) {
                        ActionMenuTile(icon = Icons.Rounded.Delete, label = "撤回", danger = true, onClick = {
                            val mid = targetDm.id
                            dmActionTarget = null
                            scope.launch {
                                if (api.deleteDmMessage(mid)) {
                                    activeDmId?.let { tid ->
                                        api.fetchDmMessagesFull(tid)?.let { dmMessages = it.resolvedMessages }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
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

    return (cur.createdAt - prev.createdAt) in 0L until 180L
}

@Composable
private fun RenderChatItemRow(
    msg: ChatMessage,
    isContinuation: Boolean,
    highlighted: Boolean,
    cleanDecoMode: Boolean,
    api: ForHappyApiClient,
    onActionRedpack: (Long) -> Unit,
    onActionGuess: (Long) -> Unit,
    onActionRaffle: (Long) -> Unit,
    onJumpMessage: (Long) -> Unit,
    onSwipeReply: () -> Unit,
    onOpenActionSheet: () -> Unit,
    onOpenBoostComposer: () -> Unit,
    onQuickBoost: (String) -> Unit,
    onReaction: (String) -> Unit,
    onRetrySend: () -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    onOpenRedpack: () -> Unit,
    onOpenRepliesSheet: () -> Unit,
    onPostRefClick: (Long) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onOpenNovelExcerpt: (NovelExcerptSpec) -> Unit,
    onTapAvatar: (ChatMessage) -> Unit,
    onLongPressAvatar: (ChatMessage) -> Unit,
) {
    if (!msg.systemKind.isNullOrBlank()) {
        SystemNoticeRow(
            msg = msg,
            onActionRedpack = onActionRedpack,
            onActionGuess = onActionGuess,
            onActionRaffle = onActionRaffle,
            onJumpMessage = onJumpMessage,
        )
    } else {
        SwipeableChatRow(
            msg = msg,
            isContinuation = isContinuation,
            highlighted = highlighted,
            cleanDecoMode = cleanDecoMode,
            api = api,
            onSwipeReply = onSwipeReply,
            onOpenActionSheet = onOpenActionSheet,
            onOpenBoostComposer = onOpenBoostComposer,
            onQuickBoost = onQuickBoost,
            onReaction = onReaction,
            onRetrySend = onRetrySend,
            onOpenImage = onOpenImage,
            onOpenVideo = onOpenVideo,
            onOpenRedpack = onOpenRedpack,
            onQuoteClick = onJumpMessage,
            onOpenRepliesSheet = onOpenRepliesSheet,
            onPostRefClick = onPostRefClick,
            onHashtagClick = onHashtagClick,
            onLinkClick = onLinkClick,
            onOpenNovelExcerpt = onOpenNovelExcerpt,
            onTapAvatar = { onTapAvatar(msg) },
            onLongPressAvatar = { onLongPressAvatar(msg) },
        )
    }
}

@Composable
private fun ComposerAttachmentTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Surface(
            color = tint.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Text(label, color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NovelExcerptCard(
    spec: NovelExcerptSpec,
    onOpenBook: () -> Unit,
) {
    val cardBg = when (spec.theme) {
        "night" -> Color(0xFF1F2421)
        "green" -> Color(0xFFEDF5EC)
        "white" -> Color(0xFFFFFFFF)
        else -> Color(0xFFFBF8F1)
    }
    val cardText = if (spec.theme == "night") Color(0xFFE7ECE8) else Color(0xFF2B2A26)
    val cardMuted = if (spec.theme == "night") Color(0xFF98A39C) else Color(0xFF7A7468)
    val palette = LocalForHappyPalette.current

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .widthIn(min = 210.dp, max = 280.dp)
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable { onOpenBook() },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = "《${spec.title}》 · ${spec.chapterTitle}",
                    color = cardMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = spec.excerpt,
                color = cardText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("去书房连读 ›", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

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
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
            Surface(color = palette.surface2.copy(alpha = 0.85f), shape = RoundedCornerShape(999.dp)) {
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
        if (msg.deleted) listOf("系统提醒已撤回") else msg.body.ifBlank { kind }.split("\n答案释义：")
    }
    val headline = if (kind == "redpack_due" && !msg.deleted) "红包到点啦！" else parts.firstOrNull().orEmpty()
    val explanation = if (!msg.deleted && parts.size > 1) parts.drop(1).joinToString("；") else null

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = if (isRedpackNotice) palette.gold.copy(alpha = 0.10f) else palette.surface2.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(1.dp, if (isRedpackNotice) palette.gold.copy(alpha = 0.28f) else palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(headline, color = palette.text, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = if (isRedpackNotice) FontWeight.Medium else FontWeight.Normal)
                if (!explanation.isNullOrBlank()) {
                    Text("答案释义：$explanation", color = palette.primary, fontSize = 11.sp, lineHeight = 16.sp)
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
                                Text(actionLabel, color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
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
    cleanDecoMode: Boolean,
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
    onOpenNovelExcerpt: (NovelExcerptSpec) -> Unit,
    onTapAvatar: () -> Unit,
    onLongPressAvatar: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val stealth = LocalPrivacyStealthConfig.current
    val disguiseMode = stealth.disguiseMode
    val isLinuxDo = disguiseMode == AppDisguiseMode.LINUX_DO
    val shownAuthor = remember(msg.id, msg.user?.id, msg.displayAuthor, msg.mine, disguiseMode, stealth.feishuMemberAliasEnabled) {
        if (disguiseMode == AppDisguiseMode.FEISHU && stealth.feishuMemberAliasEnabled && !msg.mine) {
            DisguiseSanitizer.mapColleagueName(msg.user?.id ?: msg.id, msg.displayAuthor, true)
        } else {
            msg.displayAuthor
        }
    }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragX by remember { mutableFloatStateOf(0f) }
    var showDetailMeta by remember(msg.id) { mutableStateOf(false) }

    val shortTimeStr = remember(msg.createdAt) {
        formatShanghaiClock(msg.createdAt)
    }
    val exactTimeStr = remember(msg.createdAt) {
        runCatching {
            SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            }.format(Date(msg.createdAt * 1000L))
        }.getOrDefault(shortTimeStr)
    }

    val rightEdgeGuardPx = with(density) { 32.dp.toPx() }
    val quoteTriggerPx = with(density) { 44.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.gold.copy(alpha = 0.16f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else Modifier,
            )
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .pointerInput(msg.id, msg.localId, rightEdgeGuardPx, quoteTriggerPx) {
                // Right-edge-safe swipe-to-quote (`04-gestures.md`: preserve >= 32dp system back edge!)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > size.width - rightEdgeGuardPx) {
                        return@awaitEachGesture
                    }
                    val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over < 0f) change.consume()
                    }
                    if (slop != null) {
                        dragX = 0f
                        horizontalDrag(slop.id) { change ->
                            val dx = change.positionChange().x
                            dragX = (dragX + dx).coerceIn(-quoteTriggerPx * 1.4f, 0f)
                            change.consume()
                        }
                        if (dragX <= -quoteTriggerPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwipeReply()
                        }
                        dragX = 0f
                    }
                }
            }
            .combinedClickable(
                onClick = { showDetailMeta = !showDetailMeta },
                onLongClick = {
                    if (!msg.deleted) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenActionSheet()
                    }
                },
            ),
        horizontalArrangement = if (msg.mine && !isLinuxDo) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!msg.mine || isLinuxDo) {
            if (!isContinuation) {
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = onTapAvatar,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressAvatar()
                        },
                    ),
                ) {
                    ForHappyAvatar(
                        username = shownAuthor,
                        avatarUrl = if (disguiseMode == AppDisguiseMode.FEISHU) null else msg.resolvedAvatar,
                        imageLoader = api.imageLoader,
                        size = 32.dp,
                    )
                }
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(40.dp))
            }
        }

        Column(
            horizontalAlignment = if (msg.mine && !isLinuxDo) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 318.dp),
        ) {
            if (!isContinuation) {
                val chatIdentity = remember(msg.user?.deco, cleanDecoMode, disguiseMode) {
                    if (!cleanDecoMode && disguiseMode == AppDisguiseMode.NONE) {
                        BadgeAndCosmeticRegistry.resolveEquippedIdentity(msg.user?.deco, where = "chat")
                    } else {
                        null
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (!msg.mine || isLinuxDo) {
                        val gradColors = remember(chatIdentity?.nickGradientHexes, palette.muted) {
                            chatIdentity?.nickGradientHexes?.map { parseSafeHex(it, palette.muted) }?.takeIf { it.size >= 2 }
                        }
                        Text(
                            text = shownAuthor,
                            style = if (gradColors != null) {
                                TextStyle(brush = Brush.horizontalGradient(gradColors), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                TextStyle(color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!cleanDecoMode && disguiseMode == AppDisguiseMode.NONE) {
                        if (chatIdentity?.title != null) {
                            TitleChipView(title = chatIdentity.title)
                        } else {
                            msg.title?.takeIf { it.isNotBlank() }?.let { titleLabel ->
                                BadgePill(titleLabel, containerColor = palette.gold.copy(alpha = 0.16f), contentColor = palette.gold)
                            }
                        }
                        chatIdentity?.badge?.let { badgeChip ->
                            BadgeChipView(badge = badgeChip, imageLoader = api.imageLoader, mini = true)
                        }
                    }
                    Text(shortTimeStr, color = palette.muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    msg.floor?.let { fl ->
                        Text("${fl}楼", color = palette.muted.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                    if (!msg.deleted) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "消息操作",
                            tint = palette.muted.copy(alpha = 0.65f),
                            modifier = Modifier.size(15.dp).clickable { onOpenActionSheet() },
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }

            val redpackMeta = if (!msg.deleted) msg.parseRedpackMeta() else null
            val novelExcerpt = remember(msg.body, msg.deleted) {
                if (!msg.deleted) ChatRichTextParser.parseNovelExcerpt(msg.body) else null
            }
            val bubbleBg = when {
                isLinuxDo -> Color.Transparent
                msg.mine -> palette.primary.copy(alpha = 0.14f)
                else -> palette.surface
            }
            val bubbleBorder = when {
                isLinuxDo -> Color.Transparent
                highlighted -> palette.gold
                msg.mine -> palette.primary.copy(alpha = 0.35f)
                else -> palette.line
            }

            Surface(
                color = bubbleBg,
                shape = RoundedCornerShape(if (isLinuxDo) 0.dp else 15.dp),
                modifier = if (isLinuxDo) {
                    Modifier
                } else {
                    Modifier.border(if (highlighted) 1.5.dp else 1.dp, bubbleBorder, RoundedCornerShape(15.dp))
                },
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
                            modifier = Modifier.clickable(enabled = rep.id > 0L) { onQuoteClick(rep.id) },
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
                            RedpackBubbleCard(meta = redpackMeta, api = api, onClick = onOpenRedpack)
                        }
                        if (novelExcerpt != null) {
                            NovelExcerptCard(spec = novelExcerpt, onOpenBook = { onOpenNovelExcerpt(novelExcerpt) })
                        } else {
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
                        }

                        if (msg.mediaExpired) {
                            Surface(color = palette.surface2, shape = RoundedCornerShape(8.dp)) {
                                Text("附件已超过七天，无法查看", color = palette.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    visuals.chunked(cols).forEach { rowItems ->
                                        Row(modifier = Modifier.width(222.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
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
                                            Text(f.resolvedName ?: "附件文件", color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if ((f.s ?: 0L) > 0L) Text(formatFileBytes(f.s!!), color = palette.muted, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Rounded.Download, contentDescription = "下载", tint = palette.muted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDetailMeta && !msg.deleted) {
                Text(
                    text = buildString {
                        msg.floor?.let { append("#$it · ") }
                        append(exactTimeStr)
                        if (msg.edited || msg.editedAt != null) append(" · 已编辑")
                    },
                    color = palette.muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                )
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
                            if (b.mine) onOpenBoostComposer() else if (b.text.isNotBlank()) onQuickBoost(b.text)
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = palette.muted)
                    Spacer(Modifier.width(4.dp))
                    Text("发送中…", color = palette.muted, fontSize = 10.sp)
                }
            } else if (msg.deliveryState == "failed") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp).clickable { onRetrySend() },
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
    val stealthConfig = LocalPrivacyStealthConfig.current
    val rawUrl = media.resolvedUrl
    val imgUrl = MediaUrlRewriter.rewriteWithExtProxy(rawUrl)
    val rawVid = media.v
    val isVideo = media.k == "v" || !rawVid.isNullOrBlank()
    val isLive = media.k == "l"
    val videoUrl = if (!rawVid.isNullOrBlank()) MediaUrlRewriter.rewriteWithExtProxy(rawVid) else ""
    var unblurred by remember(msgId, rawUrl, stealthConfig.blurAllEnabled) { mutableStateOf(false) }

    if (stealthConfig.autoReblurOnScrollOff) {
        DisposableEffect(msgId, rawUrl) {
            onDispose { unblurred = false }
        }
    }

    val isBlurred = (media.b || stealthConfig.blurAllEnabled) && !unblurred
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
                if (isBlurred) unblurred = true
                else if (isVideo && videoUrl.isNotBlank()) onOpenVideo(imgUrl, videoUrl)
                else if (imgUrl.isNotBlank()) onOpenImage(imgUrl)
            },
    ) {
        if (imgUrl.isNotBlank()) {
            AsyncImage(
                model = imgUrl,
                imageLoader = api.imageLoader,
                contentDescription = media.resolvedName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().let { if (isBlurred) it.blur(20.dp) else it },
            )
        }
        if (isBlurred) {
            Surface(color = Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(999.dp)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("已模糊 · 轻触显示", color = Color.White, fontSize = 10.sp)
                }
            }
        } else if (isVideo) {
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = CircleShape) {
                Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "播放视频", tint = Color.White, modifier = Modifier.size(if (solo) 38.dp else 28.dp))
            }
        }
        if (isLive) {
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
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

    Column(horizontalAlignment = if (mineAlignEnd) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        aiBoosts.forEach { b ->
            Surface(
                color = palette.primary.copy(alpha = 0.09f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, palette.primary.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            ) {
                Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForHappyAvatar(username = "欢欢", avatarUrl = b.avatar, imageLoader = api.imageLoader, size = 18.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("欢欢 AI 回应", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(b.text.ifBlank { if (b.status == "thinking") "思考中…" else "…" }, color = palette.text, fontSize = 12.sp, lineHeight = 17.sp)
                        b.score?.let { sc ->
                            LinearProgressIndicator(
                                progress = { sc.coerceIn(0, 100) / 100f },
                                color = palette.primary,
                                trackColor = palette.surface2,
                                modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(999.dp)),
                            )
                        }
                    }
                }
            }
        }

        if (userBoosts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, if (mineAlignEnd) Alignment.End else Alignment.Start), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                userBoosts.forEach { b ->
                    Surface(
                        color = if (b.mine) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(1.dp, if (b.mine) palette.primary.copy(alpha = 0.50f) else palette.line.copy(alpha = 0.70f), RoundedCornerShape(999.dp))
                            .clickable { onTapBoost(b) },
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ForHappyAvatar(username = b.displayAuthor, avatarUrl = b.avatar, imageLoader = api.imageLoader, size = 15.dp)
                            BoostPillText(text = b.text, imageLoader = api.imageLoader, fontSize = 11.sp)
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
    onOpenBoostHistory: () -> Unit,
    onReply: () -> Unit,
    onQuote: () -> Unit,
    onEchoPlusOne: () -> Unit,
    onCopy: () -> Unit,
    onSaveMedia: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onRecall: () -> Unit,
    onConvertToPost: (() -> Unit)? = null,
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
        val actionDisguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("表情回应", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (actionDisguiseMode == AppDisguiseMode.NONE) {
                    TextButton(onClick = { showMoreStickers = !showMoreStickers }) {
                        Text(if (showMoreStickers) "收起表情包" else "更多表情包", fontSize = 12.sp)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            if (actionDisguiseMode == AppDisguiseMode.NONE && showMoreStickers && packs.isNotEmpty()) {
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
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).clickable { onReact(key) }.padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Boost 回应（≤12字）", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Surface(color = palette.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp), modifier = Modifier.clickable { onOpenBoostComposer() }) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            .border(1.dp, if (isMine) palette.primary else palette.line, RoundedCornerShape(999.dp))
                            .clickable { onQuickBoost(if (isMine) "" else preset) },
                    ) {
                        Text(preset, color = accent ?: palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionMenuTile(icon = Icons.AutoMirrored.Rounded.Reply, label = "回复", onClick = onReply)
                ActionMenuTile(icon = Icons.Rounded.FormatQuote, label = "引用", onClick = onQuote)
                if (msg.body.isNotBlank()) ActionMenuTile(icon = Icons.Rounded.Repeat, label = "+1 复读", onClick = onEchoPlusOne)
                ActionMenuTile(icon = Icons.Rounded.ContentCopy, label = "复制", onClick = onCopy)
                if (onSaveMedia != null) ActionMenuTile(icon = Icons.Rounded.Download, label = "保存图片", onClick = onSaveMedia)
                if (msg.boosts.isNotEmpty()) ActionMenuTile(icon = Icons.Rounded.Info, label = "助推记录", onClick = onOpenBoostHistory)
                if (onConvertToPost != null) ActionMenuTile(icon = Icons.Rounded.Image, label = "转为图廊帖子", onClick = onConvertToPost)
                if ((msg.canEdit || (msg.mine && msg.id > 0L)) && msg.body.isNotBlank()) ActionMenuTile(icon = Icons.Rounded.Edit, label = "编辑", onClick = onEdit)
                if (msg.canDelete || (msg.mine && msg.id > 0L) || me?.isAdmin == true) {
                    ActionMenuTile(icon = Icons.Rounded.Delete, label = if (msg.mine) "撤回" else "删除", danger = true, onClick = onRecall)
                }
            }
        }
    }
}

@Composable
private fun BoostHistoryDialog(
    msg: ChatMessage,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("助推 / Boost 记录 (${msg.boosts.size})") },
        text = {
            LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(msg.boosts.size) { idx ->
                    val b = msg.boosts[idx]
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ForHappyAvatar(username = b.displayAuthor, avatarUrl = b.avatar, imageLoader = api.imageLoader, size = 26.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.displayAuthor, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(b.text, color = palette.muted, fontSize = 12.sp)
                        }
                        Text(formatShanghaiClock(b.at), color = palette.muted, fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
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
        modifier = Modifier.border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp)).clickable { onClick() },
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
        Column(modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("给这条消息挂一句 Boost（${BOOST_MAX_LEN}字内）", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("自己写一句（$BOOST_MAX_LEN 字内）", fontSize = 13.sp) },
                    singleLine = true,
                    supportingText = { Text("$units/$BOOST_MAX_LEN", color = if (units > BOOST_MAX_LEN) palette.danger else palette.muted, fontSize = 11.sp) },
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
                ) { Text(if (oldMine.isNotBlank()) "改" else "挂上") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BOOST_PRESETS.forEach { preset ->
                    val accent = remember(preset, palette.isDark) { boostPresetAccent(preset, palette.isDark) }
                    val selected = oldMine == preset
                    Surface(
                        color = if (selected) palette.primary.copy(alpha = 0.18f) else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.border(1.dp, if (selected) palette.primary else palette.line, RoundedCornerShape(999.dp)).clickable(enabled = !busy) {
                            busy = true
                            scope.launch {
                                onSubmit(preset)
                                busy = false
                            }
                        },
                    ) {
                        Text(preset, color = accent ?: palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
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
                ) { Text("撤掉我的回应「$oldMine」", color = palette.danger) }
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
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().background(palette.surface).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${items.size} 条回复", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                }
            }
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = palette.primary) }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("回复都撤回了", color = palette.muted, fontSize = 13.sp) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { rep ->
                        val ts = remember(rep.at) { formatShanghaiClock(rep.at) }
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp)).clickable { onJumpMessage(rep.id) },
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                ForHappyAvatar(username = rep.displayAuthor, avatarUrl = rep.avatar, imageLoader = api.imageLoader, size = 28.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(rep.displayAuthor, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(ts, color = palette.muted, fontSize = 10.sp)
                                    }
                                    Text(rep.text?.takeIf { it.isNotBlank() } ?: if (rep.hasImage) "[图片]" else "…", color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedpackBubbleCard(
    meta: RedpackMeta,
    api: ForHappyApiClient,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
    val isGuess = meta.isGuessGame
    val headerBg = when {
        meta.solved -> Color(0xFFD98A6C)
        isGuess -> palette.primary
        meta.isDcoin -> Color(0xFFD48806)
        else -> Color(0xFFC84B31)
    }

    val shownHeadline = DisguiseSanitizer.sanitizeSummaryForDisguise(meta.headline, disguiseMode)
    val shownSubline = when (disguiseMode) {
        AppDisguiseMode.FEISHU -> if (isGuess) "群待办事项" else if (meta.ldcMode == "random") "拼手气红包" else "红包"
        AppDisguiseMode.LINUX_DO -> if (isGuess) "社区投票卡片" else "互动卡片"
        AppDisguiseMode.NONE -> meta.subline
    }
    val shownAction = if (disguiseMode != AppDisguiseMode.NONE && isGuess) "查看待办详情" else meta.actionLabel
    val shownBadge = if (disguiseMode != AppDisguiseMode.NONE) "${meta.opened} 人" else meta.typeBadge

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        modifier = Modifier.widthIn(min = 210.dp, max = 270.dp).border(1.dp, headerBg.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).clickable { onClick() },
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(color = Color.White.copy(alpha = 0.22f), shape = CircleShape, modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isGuess) Icons.Rounded.AutoAwesome else Icons.Rounded.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(shownHeadline, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(shownSubline, color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (disguiseMode == AppDisguiseMode.NONE) {
                    meta.cover?.let { coverUrl ->
                        AsyncImage(
                            model = MediaUrlRewriter.rewriteMediaUrl(coverUrl),
                            imageLoader = api.imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().background(palette.surface2.copy(alpha = 0.55f)).padding(horizontal = 11.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(shownAction, color = headerBg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(shownBadge, color = palette.muted, fontSize = 10.sp)
            }
        }
    }
}

private fun formatFileBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

private fun formatShanghaiClock(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    return runCatching {
        SimpleDateFormat("HH:mm", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date(epochSec * 1000L))
    }.getOrDefault("")
}

private fun isSameShanghaiDay(aSec: Long, bSec: Long): Boolean {
    if (aSec <= 0L || bSec <= 0L) return true
    return (aSec + 28_800L).floorDiv(86_400L) == (bSec + 28_800L).floorDiv(86_400L)
}

private fun formatShanghaiDateDivider(epochSec: Long): String {
    val today = (ServerClock.serverNowSec() + 28_800L).floorDiv(86_400L)
    val day = (epochSec + 28_800L).floorDiv(86_400L)
    return when {
        day == today -> "今天"
        today - day == 1L -> "昨天"
        else -> runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            }.format(Date(epochSec * 1000L))
        }.getOrDefault("")
    }
}
