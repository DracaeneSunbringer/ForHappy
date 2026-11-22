package com.jinhuan.forhappy.ui.chat

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChatBubbleOutline
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
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
import com.jinhuan.forhappy.core.model.DecoConfig
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
import com.jinhuan.forhappy.ui.components.ChatCosmeticBoostPill
import com.jinhuan.forhappy.ui.components.ChatCosmeticBubbleBox
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ForHappyTextField
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.components.TitleChipView
import com.jinhuan.forhappy.ui.components.boostPresetAccent
import com.jinhuan.forhappy.ui.components.parseSafeHex
import com.jinhuan.forhappy.ui.compose.ComposeMediaStager
import com.jinhuan.forhappy.ui.disguise.DisguiseSanitizer
import com.jinhuan.forhappy.ui.me.KV_DECO_CLEAN
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    hasFloatingDock: Boolean = false,
    onDockHideChanged: (Boolean) -> Unit = {},
) {
    val palette = LocalForHappyPalette.current
    val stealth = LocalPrivacyStealthConfig.current
    val disguiseMode = stealth.disguiseMode
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state by controller.uiState.collectAsState()
    val cosmeticCatalogRev by BadgeAndCosmeticRegistry.catalogRevision.collectAsState()
    val isImeVisible = WindowInsets.isImeVisible
    val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isDirectEmbedded = hideChannelHeader || forcedChannelMode != null
    val joinedGroups = remember(state.groups) { state.groups.filter { it.joined } }
    val bypassSessionList = state.shouldBypassSessionList(isDirectEmbedded)
    var activePane by remember(isDirectEmbedded, forcedChannelMode) {
        mutableStateOf(if (isDirectEmbedded || bypassSessionList) (forcedChannelMode ?: "group") else "list")
    }
    var cleanRawPref by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(me?.id, me?.deco, state.activeGroupId, activePane) {
        cleanRawPref = api.getPref(KV_DECO_CLEAN)
    }
    LaunchedEffect(isDirectEmbedded, bypassSessionList, state.dmEnabled, disguiseMode) {
        if (!isDirectEmbedded) {
            if (bypassSessionList && activePane != "group") {
                activePane = "group"
            } else if (!state.dmEnabled && disguiseMode != AppDisguiseMode.FEISHU && activePane == "dm") {
                activePane = "list"
            }
        }
    }
    val cleanDecoConfig = remember(me?.deco, cleanRawPref) {
        BadgeAndCosmeticRegistry.parseServerDecoConfig(me?.deco, cleanRawPref)
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
    var sessionClaimedOrEndedPacketIds by remember(state.activeGroupId, me?.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var serverHasAvailableRedpack by remember(state.activeGroupId) { mutableStateOf(false) }
    var seenRedpackMatchTriggerIds by remember(state.activeGroupId, me?.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var autoOlderAttemptedId by remember(state.activeGroupId) { mutableLongStateOf(0L) }
    var historyLoadingOlder by remember { mutableStateOf(false) }
    var historyLoadingNewer by remember { mutableStateOf(false) }

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

    // System Back closes composer edit/trays/jump mode, then returns Level-1 Group/DM to Level-0 Session List
    BackHandler(
        enabled = state.editTarget != null ||
            dmEditTarget != null ||
            showStickerTray ||
            showAttachmentTray ||
            historyMsgs != null ||
            (!isDirectEmbedded && activePane != "list" && !bypassSessionList),
    ) {
        when {
            state.editTarget != null -> controller.setEditTarget(null)
            dmEditTarget != null -> dmEditTarget = null
            showStickerTray || showAttachmentTray -> {
                showStickerTray = false
                showAttachmentTray = false
            }
            historyMsgs != null -> historyMsgs = null
            !isDirectEmbedded && !bypassSessionList -> activePane = "list"
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
            onDockHideChanged(false)
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

    LaunchedEffect(me?.id, state.dmEnabled, disguiseMode) {
        if (me != null && (state.dmEnabled || disguiseMode == AppDisguiseMode.FEISHU)) {
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
                activePane = if (bypassSessionList) "group" else "list"
            }
            "dm" -> {
                if (!state.dmEnabled && disguiseMode != AppDisguiseMode.FEISHU) {
                    activePane = if (bypassSessionList) "group" else "list"
                } else {
                    newDmPeer = null
                    activeDmId = req.dmThreadId ?: state.dmThreads.firstOrNull()?.id
                    activePane = "dm"
                }
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

    suspend fun claimSinglePacketMsg(
        pktMsg: ChatMessage,
        phraseAnswer: String?,
    ): Pair<Boolean, String?> {
        val rp = pktMsg.redpack
        val coin52 = runCatching { rp?.get("coin52")?.jsonObject }.getOrNull()
        val ldc = runCatching { rp?.get("ldc")?.jsonObject }.getOrNull()
        val mode = rp?.get("mode")?.jsonPrimitive?.contentOrNull ?: "open"
        val res = when {
            coin52 != null -> api.claimCoin52Redpack(pktMsg.id, phraseAnswer)
            ldc != null && mode == "open" -> api.postLdcRedpackAction(pktMsg.id, "claim", phraseAnswer)
            else -> api.openRedpack(pktMsg.id, phraseAnswer)
        }
        val err = res["error"]?.jsonPrimitive?.contentOrNull
        if (!err.isNullOrBlank()) {
            if (err.contains("领完") || err.contains("抢光") || err.contains("过期") || err.contains("已经") || err.contains("自己")) {
                sessionClaimedOrEndedPacketIds = sessionClaimedOrEndedPacketIds + pktMsg.id
            }
            return false to err
        }
        var finalMsg = res["msg"]?.jsonPrimitive?.contentOrNull
        val openedReward = runCatching { res["packet"]?.jsonObject?.get("reward")?.jsonObject }.getOrNull()
        if (openedReward != null &&
            openedReward["owner"]?.jsonPrimitive?.booleanOrNull != true &&
            (openedReward["mine"] == null || openedReward["mine"] is JsonNull) &&
            openedReward["state"]?.jsonPrimitive?.contentOrNull == "open" &&
            (openedReward["claimed_count"]?.jsonPrimitive?.intOrNull ?: 0) < (openedReward["count"]?.jsonPrimitive?.intOrNull ?: 1)
        ) {
            val rRes = api.postLdcRedpackAction(pktMsg.id, "claim")
            val rAmt = runCatching { rRes["ldc"]?.jsonObject?.get("mine")?.jsonObject?.get("amount")?.jsonPrimitive?.contentOrNull }.getOrNull()
            if (!rAmt.isNullOrBlank()) finalMsg = "奖励 $rAmt"
        }
        sessionClaimedOrEndedPacketIds = sessionClaimedOrEndedPacketIds + pktMsg.id
        return true to finalMsg
    }

    suspend fun autoMatchSentMessageRedpacks(confirmed: ChatMessage?) {
        if (confirmed == null || confirmed.id <= 0L || confirmed.id in seenRedpackMatchTriggerIds) return
        seenRedpackMatchTriggerIds = seenRedpackMatchTriggerIds + confirmed.id
        val matches = api.fetchRedpackMatches(state.activeGroupId, confirmed.id)
        if (matches.isEmpty()) return
        var opened = 0
        for (pkt in matches) {
            val (ok, _) = claimSinglePacketMsg(pkt, confirmed.body.trim())
            if (ok) opened++
        }
        if (opened > 0) {
            chatStatusBanner = "口令匹配自动拆开 $opened 个红包！"
        }
        controller.pollOnce(state.activeGroupId)
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
                val confirmed = controller.sendOrEditMessage(txt)
                autoMatchSentMessageRedpacks(confirmed)
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

    val shouldHideShellDock = remember(
        isDirectEmbedded,
        activePane,
        bypassSessionList,
        isNearBottom,
        historyMsgs,
        showStickerTray,
        showAttachmentTray,
    ) {
        if (isDirectEmbedded) {
            false
        } else {
            when (activePane) {
                "list" -> false
                "dm" -> true
                "group" -> if (!bypassSessionList) {
                    true
                } else {
                    !isNearBottom || historyMsgs != null || showStickerTray || showAttachmentTray
                }
                else -> false
            }
        }
    }
    LaunchedEffect(shouldHideShellDock) {
        onDockHideChanged(shouldHideShellDock)
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
    val endedRedpackRefIds = remember(state.messages) {
        state.messages.asSequence()
            .filter {
                it.systemKind in setOf(
                    "redpack_result",
                    "redpack_draw",
                    "redpack_due",
                    "ldc_winner",
                    "ldc_cancel",
                    "ldc_expired",
                )
            }
            .mapNotNull { it.systemRef?.takeIf { ref -> ref.isNotBlank() } }
            .toHashSet()
    }
    val unclaimedRedpackMsgs = remember(state.messages, sessionClaimedOrEndedPacketIds, endedRedpackRefIds) {
        state.messages.filter { msg ->
            if (msg.deleted || msg.id <= 0L || msg.redpack == null) return@filter false
            if (msg.id in sessionClaimedOrEndedPacketIds) return@filter false
            if (msg.id.toString() in endedRedpackRefIds) return@filter false
            val meta = msg.parseRedpackMeta() ?: return@filter false
            !meta.isGuessGame &&
                !meta.solved &&
                !meta.snapshotClaimed &&
                meta.snapshotState !in setOf("ended", "settled", "exhausted", "expired", "cancelled", "drawn")
        }
    }
    LaunchedEffect(state.activeGroupId, activePane, state.messages.size, me?.id) {
        serverHasAvailableRedpack = if (activePane == "group" && me != null && state.activeGroupId > 0L) {
            api.fetchRedpacksAvailable(state.activeGroupId)
        } else {
            false
        }
    }
    val hasLiveRedpack = unclaimedRedpackMsgs.isNotEmpty() || serverHasAvailableRedpack

    suspend fun claimPhraseBatch(phrase: String, primaryMsgId: Long): PhraseBatchClaimResult {
        val clean = phrase.trim()
        if (clean.isEmpty()) return PhraseBatchClaimResult(error = "口令不能为空")
        val nowSec = ServerClock.serverNowSec()
        val recentOwnMatch = state.messages.lastOrNull { m ->
            m.mine && !m.deleted && m.id > 0L && m.body.trim() == clean && (nowSec - m.createdAt) <= 120L
        }
        val sentMsg = recentOwnMatch ?: controller.sendOrEditMessage(clean)
        val extraMatches = if (sentMsg != null && sentMsg.id > 0L) {
            seenRedpackMatchTriggerIds = seenRedpackMatchTriggerIds + sentMsg.id
            api.fetchRedpackMatches(state.activeGroupId, sentMsg.id)
        } else {
            emptyList()
        }
        val localSamePhrase = unclaimedRedpackMsgs.filter { m ->
            val rp = m.redpack ?: return@filter false
            val mMode = rp["mode"]?.jsonPrimitive?.contentOrNull
            val mPhrase = rp["phrase"]?.jsonPrimitive?.contentOrNull?.trim()
            mMode == "phrase" && mPhrase == clean
        }
        val richById = (extraMatches + localSamePhrase + listOfNotNull(state.messages.firstOrNull { it.id == primaryMsgId }))
            .associateBy { it.id }
        val resolvedPrimary = richById[primaryMsgId] ?: ChatMessage(id = primaryMsgId, groupId = state.activeGroupId)
        val targets = (listOf(resolvedPrimary) + extraMatches + localSamePhrase).distinctBy { it.id }

        var openedCount = 0
        val rewards = mutableListOf<String>()
        var firstErr: String? = null
        for (pkt in targets) {
            val (ok, msgOrErr) = claimSinglePacketMsg(pkt, clean)
            if (ok) {
                openedCount++
                msgOrErr?.takeIf { it.isNotBlank() }?.let { rewards += it }
            } else if (firstErr == null) {
                firstErr = msgOrErr
            }
        }
        controller.pollOnce(state.activeGroupId)
        return PhraseBatchClaimResult(
            openedCount = openedCount,
            totalTargetCount = targets.size,
            rewardText = rewards.distinct().joinToString(" · ").ifBlank { null },
            error = firstErr,
        )
    }
    val hasLiveGuess = activeRound != null
    val canSwipeSwitchGroups = disguiseMode == AppDisguiseMode.NONE &&
        activePane == "group" &&
        state.swipeSwitchGroups &&
        joinedGroups.size >= 2 &&
        historyMsgs == null
    val activeHistory = historyMsgs
    val isModernCleanMode = palette.isModernUi && disguiseMode == AppDisguiseMode.NONE
    val effectiveDockVisible = hasFloatingDock && !shouldHideShellDock && !isImeVisible
    val targetModernBottomPad = when {
        isImeVisible -> 8.dp
        effectiveDockVisible -> navBottomPad + 76.dp
        isDirectEmbedded -> navBottomPad + 8.dp
        else -> navBottomPad + 10.dp
    }
    val modernBottomComposerPad by animateDpAsState(
        targetValue = targetModernBottomPad,
        animationSpec = tween(durationMillis = 220),
        label = "modernBottomComposerPad",
    )
    var expandedSystemGroupIds by remember(state.activeGroupId) { mutableStateOf<Set<Long>>(emptySet()) }
    val listTopPad = if (isModernCleanMode) 8.dp else 6.dp
    val listBottomPad = if (isModernCleanMode) 10.dp else 6.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        val maxGuessHeightDp = (maxHeight * 0.50f).coerceAtLeast(160.dp)

        @Composable
        fun RenderTopBarAndBanners() {
            // Level-1 Compact 48dp / Modern 52dp Chat / DM Top Bar (hidden when inside FeishuDisguiseScreen)
            if (!hideChannelHeader) {
                Surface(color = if (palette.isModernUi) palette.bg else palette.surface) {
                    Column(
                        modifier = if (palette.isModernUi && !isDirectEmbedded) Modifier.statusBarsPadding() else Modifier,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (palette.isModernUi) 52.dp else ForHappyDimens.topBarHeight)
                                .padding(horizontal = if (palette.isModernUi) 12.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 6.dp else 2.dp),
                        ) {
                            if (!bypassSessionList) {
                                if (palette.isModernUi && disguiseMode == AppDisguiseMode.NONE) {
                                    ModernChatHeaderIconButton(
                                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "返回消息列表",
                                        onClick = { activePane = "list" },
                                    )
                                } else {
                                    IconButton(
                                        onClick = { activePane = "list" },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回消息列表", tint = palette.text)
                                    }
                                }
                            } else {
                                Spacer(Modifier.width(if (palette.isModernUi) 4.dp else 8.dp))
                            }

                            if (activePane == "group") {
                                val showTopGroupPills = disguiseMode == AppDisguiseMode.NONE &&
                                    joinedGroups.size >= 2 &&
                                    (state.swipeSwitchGroups || bypassSessionList)
                                if (showTopGroupPills) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState())
                                            .padding(end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        joinedGroups.forEach { grp ->
                                            val selected = grp.id == state.activeGroupId
                                            val badgeCnt = (if (grp.muted) grp.atMe else grp.unread).coerceAtLeast(0)
                                            val pillBg = when {
                                                palette.isModernUi && selected -> palette.primary
                                                selected -> palette.primaryContainer
                                                else -> palette.surface2
                                            }
                                            val pillFg = when {
                                                palette.isModernUi && selected -> palette.onPrimary
                                                selected -> palette.primary
                                                else -> palette.text
                                            }
                                            Surface(
                                                color = pillBg,
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier
                                                    .then(
                                                        if (palette.isModernUi) {
                                                            Modifier
                                                        } else {
                                                            Modifier.border(
                                                                1.dp,
                                                                if (selected) palette.primary else palette.line,
                                                                RoundedCornerShape(999.dp),
                                                            )
                                                        },
                                                    )
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .clickable {
                                                        if (selected) {
                                                            showGroupInfoDialog = true
                                                        } else {
                                                            historyMsgs = null
                                                            controller.setActiveGroup(grp.id)
                                                        }
                                                    },
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Text(
                                                        text = grp.name,
                                                        color = pillFg,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    if (selected && grp.memberCount > 0) {
                                                        Text(
                                                            text = "(${grp.memberCount})",
                                                            color = pillFg.copy(alpha = 0.78f),
                                                            fontSize = 10.sp,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                    if (!selected && badgeCnt > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(999.dp))
                                                                .background(if (grp.atMe > 0) palette.danger else palette.badge)
                                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                                        ) {
                                                            Text(
                                                                text = when {
                                                                    grp.atMe > 0 -> "@$badgeCnt"
                                                                    badgeCnt > 99 -> "99+"
                                                                    else -> badgeCnt.toString()
                                                                },
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
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
                                            fontSize = if (palette.isModernUi) 17.sp else 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (palette.isModernUi && disguiseMode == AppDisguiseMode.NONE) {
                                    ModernChatHeaderIconButton(
                                        icon = Icons.Rounded.Search,
                                        contentDescription = "搜索记录",
                                        onClick = { showSearchDialog = true },
                                    )
                                    ModernChatHeaderIconButton(
                                        icon = Icons.Rounded.CardGiftcard,
                                        contentDescription = "红包",
                                        tint = if (hasLiveRedpack) palette.redpacket else palette.text.copy(alpha = 0.86f),
                                        containerColor = if (hasLiveRedpack) palette.redpacket.copy(alpha = 0.14f) else palette.surface2.copy(alpha = 0.78f),
                                        showDot = hasLiveRedpack,
                                        dotColor = palette.redpacket,
                                        onClick = {
                                            val targetRp = unclaimedRedpackMsgs.firstOrNull()
                                            if (targetRp != null) {
                                                redpackFallbackMeta = targetRp.parseRedpackMeta()
                                                redpackSenderName = targetRp.displayAuthor
                                                redpackDialogMsgId = targetRp.id
                                            } else {
                                                showRedpackArchiveSheet = true
                                            }
                                        },
                                    )
                                    ModernChatHeaderIconButton(
                                        icon = Icons.Rounded.AutoAwesome,
                                        contentDescription = "欢欢猜词屋",
                                        tint = if (hasLiveGuess) palette.gold else palette.text.copy(alpha = 0.86f),
                                        containerColor = if (hasLiveGuess) palette.gold.copy(alpha = 0.16f) else palette.surface2.copy(alpha = 0.78f),
                                        showDot = hasLiveGuess,
                                        dotColor = palette.gold,
                                        onClick = {
                                            guessInitialMid = activeRound?.messageId
                                                ?: activeRedpackMsg?.takeIf { it.parseRedpackMeta()?.isGuessGame == true }?.id
                                            showGuessSheet = true
                                        },
                                    )
                                    ModernChatHeaderIconButton(
                                        icon = Icons.Rounded.People,
                                        contentDescription = "群信息与成员",
                                        onClick = { if (activeGroup != null) showGroupInfoDialog = true },
                                    )
                                } else {
                                    IconButton(onClick = { showSearchDialog = true }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Rounded.Search, contentDescription = "搜索记录", tint = palette.text, modifier = Modifier.size(19.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val targetRp = unclaimedRedpackMsgs.firstOrNull()
                                            if (targetRp != null) {
                                                redpackFallbackMeta = targetRp.parseRedpackMeta()
                                                redpackSenderName = targetRp.displayAuthor
                                                redpackDialogMsgId = targetRp.id
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
                                        fontSize = if (palette.isModernUi) 17.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (dmMeta != null) {
                                    if (palette.isModernUi && disguiseMode == AppDisguiseMode.NONE) {
                                        ModernChatHeaderIconButton(
                                            icon = if (dmMuted) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                                            contentDescription = "免打扰开关",
                                            tint = if (dmMuted) palette.muted else palette.text.copy(alpha = 0.86f),
                                            onClick = { controller.setDmMutedLocal(dmMeta.id, !dmMuted) },
                                        )
                                    } else {
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
                        }
                        HorizontalDivider(
                            color = if (palette.isModernUi) palette.line else palette.line.copy(alpha = 0.7f),
                            thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                        )
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

            if (activePane == "group" && activeHistory == null) {
                if (activeRaffles.isNotEmpty()) {
                    val raffle = activeRaffles.first()
                    val raffleAccent = if (disguiseMode == AppDisguiseMode.NONE) palette.gold else palette.primary
                    val raffleLabel = if (disguiseMode == AppDisguiseMode.NONE) {
                        "头衔抽奖「${raffle.resolvedTitleName}」· ${raffle.resolvedParticipantCount}人已报名"
                    } else {
                        "群内抽签「${DisguiseSanitizer.sanitizeSummaryForDisguise(raffle.resolvedTitleName, disguiseMode)}」· ${raffle.resolvedParticipantCount}人已登记"
                    }
                    Surface(color = if (isModernCleanMode) palette.surface else raffleAccent.copy(alpha = 0.12f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = raffleAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = raffleLabel,
                                color = palette.text,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                color = if (raffle.joined) palette.surface2 else raffleAccent,
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
                                    text = if (raffle.joined) (if (disguiseMode == AppDisguiseMode.NONE) "已报名" else "已登记") else (if (disguiseMode == AppDisguiseMode.NONE) "一键报名" else "立即登记"),
                                    color = if (raffle.joined) palette.muted else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = if (isModernCleanMode) palette.line else palette.line.copy(alpha = 0.5f), thickness = if (isModernCleanMode) 0.5.dp else 1.dp)
                }

                if (activeRound != null) {
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

                if (!isModernCleanMode) {
                    val boundaryId = state.unreadEntryBoundaryByGroup[state.activeGroupId]
                    if (boundaryId != null && boundaryId > 0L) {
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
                }
            }

            if (activePane == "group" && activeHistory != null) {
                Surface(color = if (isModernCleanMode) palette.surface else palette.gold.copy(alpha = 0.14f)) {
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
                if (isModernCleanMode) {
                    HorizontalDivider(color = palette.line, thickness = 0.5.dp)
                }
            }
        }

        @Composable
        fun RenderMessagesViewport(viewportModifier: Modifier) {
            if (activePane == "group") {
                val swipeAnimHaptic = LocalHapticFeedback.current
                var swipeDragOffsetPx by remember { mutableFloatStateOf(0f) }
                val slideEntryOffsetPx = remember { Animatable(0f) }
                val layoutDir = LocalLayoutDirection.current
                val sysGestures = WindowInsets.systemGestures
                val baseEdgeGuardPx = with(density) { 36.dp.toPx() }
                val leftEdgeGuardPx = maxOf(baseEdgeGuardPx, sysGestures.getLeft(density, layoutDir).toFloat())
                val rightEdgeGuardPx = maxOf(baseEdgeGuardPx, sysGestures.getRight(density, layoutDir).toFloat())
                val swipeThresholdPx = with(density) { 56.dp.toPx() }
                val swipeVelocityThresholdPx = with(density) { 650.dp.toPx() }
                val minVelDistancePx = with(density) { 24.dp.toPx() }
                val entryShiftPx = with(density) { 72.dp.toPx() }
                val latestJoinedGroups by rememberUpdatedState(joinedGroups)
                val latestActiveGroupId by rememberUpdatedState(state.activeGroupId)

                Box(
                    modifier = viewportModifier
                        .clipToBounds()
                    .then(
                        if (canSwipeSwitchGroups) {
                            Modifier.pointerInput(canSwipeSwitchGroups, leftEdgeGuardPx, rightEdgeGuardPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (down.position.x < leftEdgeGuardPx || down.position.x > size.width - rightEdgeGuardPx) {
                                        return@awaitEachGesture
                                    }
                                    var rawDragPx = 0f
                                    val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                        if (!change.isConsumed) {
                                            change.consume()
                                            rawDragPx = over
                                        }
                                    }
                                    if (slop != null) {
                                        val vt = VelocityTracker()
                                        vt.addPointerInputChange(down)
                                        vt.addPointerInputChange(slop)
                                        val groupsSnap = latestJoinedGroups
                                        val curIdx = groupsSnap.indexOfFirst { it.id == latestActiveGroupId }.coerceAtLeast(0)
                                        fun applyDragOffset(rawDx: Float) {
                                            val hasTargetInDir = (rawDx < 0f && curIdx < groupsSnap.lastIndex) ||
                                                (rawDx > 0f && curIdx > 0)
                                            val factor = if (hasTargetInDir) 0.42f else 0.12f
                                            swipeDragOffsetPx = (rawDx * factor).coerceIn(-entryShiftPx * 1.4f, entryShiftPx * 1.4f)
                                        }
                                        applyDragOffset(rawDragPx)
                                        val finished = horizontalDrag(slop.id) { change ->
                                            vt.addPointerInputChange(change)
                                            rawDragPx += change.positionChange().x
                                            applyDragOffset(rawDragPx)
                                            change.consume()
                                        }
                                        if (finished) {
                                            val vx = vt.calculateVelocity().x
                                            val swipeLeft = rawDragPx <= -swipeThresholdPx ||
                                                (vx <= -swipeVelocityThresholdPx && rawDragPx <= -minVelDistancePx)
                                            val swipeRight = rawDragPx >= swipeThresholdPx ||
                                                (vx >= swipeVelocityThresholdPx && rawDragPx >= minVelDistancePx)
                                            val targetIdx = when {
                                                swipeLeft && curIdx < groupsSnap.lastIndex -> curIdx + 1
                                                swipeRight && curIdx > 0 -> curIdx - 1
                                                else -> -1
                                            }
                                            if (targetIdx in groupsSnap.indices) {
                                                val targetGroup = groupsSnap[targetIdx]
                                                swipeAnimHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                historyMsgs = null
                                                controller.setActiveGroup(targetGroup.id)
                                                val startShift = if (targetIdx > curIdx) entryShiftPx else -entryShiftPx
                                                scope.launch {
                                                    slideEntryOffsetPx.snapTo(startShift)
                                                    slideEntryOffsetPx.animateTo(0f, animationSpec = tween(180))
                                                }
                                            }
                                        }
                                        swipeDragOffsetPx = 0f
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = swipeDragOffsetPx + slideEntryOffsetPx.value
                        },
                ) {
                    // History `around` Overlay vs Live `reverseLayout = true` Stream
                    if (activeHistory != null) {
                        val histAtTop by remember {
                            derivedStateOf { historyListState.firstVisibleItemIndex <= 1 }
                        }
                        val histAtBottom by remember {
                            derivedStateOf {
                                val info = historyListState.layoutInfo
                                val total = info.totalItemsCount
                                val lastIdx = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                                total > 0 && lastIdx >= (total - 3).coerceAtLeast(0)
                            }
                        }
                        fun triggerHistOlder() {
                            if (historyLoadingOlder || !historyHasOlder) return
                            val edge = (historyMsgs ?: activeHistory).firstOrNull()?.id ?: return
                            historyLoadingOlder = true
                            scope.launch {
                                try {
                                    val d = api.fetchChatMessagesAround(state.activeGroupId, edge)
                                    if (d != null && historyMsgs != null) {
                                        val cur = historyMsgs ?: return@launch
                                        val have = cur.map { it.id }.toHashSet()
                                        val older = d.items.filter { it.id < edge && !have.contains(it.id) }
                                        historyMsgs = older + cur
                                        historyHasOlder = d.more && older.isNotEmpty()
                                        if (older.isNotEmpty()) {
                                            historyListState.scrollToItem(older.size + 1)
                                        }
                                    }
                                } finally {
                                    historyLoadingOlder = false
                                }
                            }
                        }
                        fun triggerHistNewer() {
                            if (historyLoadingNewer || !historyHasNewer) return
                            val edge = (historyMsgs ?: activeHistory).lastOrNull()?.id ?: return
                            historyLoadingNewer = true
                            scope.launch {
                                try {
                                    val d = api.fetchChatMessagesAround(state.activeGroupId, edge)
                                    if (d != null && historyMsgs != null) {
                                        val cur = historyMsgs ?: return@launch
                                        val have = cur.map { it.id }.toHashSet()
                                        val newer = d.items.filter { it.id > edge && !have.contains(it.id) }
                                        historyMsgs = cur + newer
                                        historyHasNewer = d.moreAfter && newer.isNotEmpty()
                                    }
                                } finally {
                                    historyLoadingNewer = false
                                }
                            }
                        }
                        LaunchedEffect(histAtTop, historyHasOlder, activeHistory.firstOrNull()?.id) {
                            if (histAtTop && historyHasOlder && !historyLoadingOlder) {
                                triggerHistOlder()
                            }
                        }
                        LaunchedEffect(histAtBottom, historyHasNewer, activeHistory.lastOrNull()?.id) {
                            if (histAtBottom && historyHasNewer && !historyLoadingNewer) {
                                triggerHistNewer()
                            }
                        }
                        val latestHistHasOlder by rememberUpdatedState(historyHasOlder)
                        val latestHistLoadingOlder by rememberUpdatedState(historyLoadingOlder)
                        val pullHistTopNestedScroll = remember(historyListState, state.activeGroupId) {
                            var histPullAccumPx = 0f
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (source == NestedScrollSource.UserInput && !historyListState.canScrollBackward) {
                                        if (available.y > 0f) {
                                            histPullAccumPx += available.y
                                            if (histPullAccumPx >= 24f &&
                                                latestHistHasOlder &&
                                                !latestHistLoadingOlder
                                            ) {
                                                histPullAccumPx = 0f
                                                triggerHistOlder()
                                            }
                                        } else if (available.y < 0f) {
                                            histPullAccumPx = 0f
                                        }
                                    } else {
                                        histPullAccumPx = 0f
                                    }
                                    return Offset.Zero
                                }
                            }
                        }
                        LazyColumn(
                            state = historyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(pullHistTopNestedScroll),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = listTopPad,
                                bottom = listBottomPad,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                                if (historyHasOlder) {
                                    item(key = "hist_older_indicator") {
                                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                            if (historyLoadingOlder) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.primary)
                                            } else {
                                                TextButton(onClick = { triggerHistOlder() }) {
                                                    Text("↑ 继续下拉加载更早消息", fontSize = 12.sp, color = palette.muted)
                                                }
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
                                        cleanDecoConfig = cleanDecoConfig,
                                        meId = me?.id ?: 0L,
                                        cosmeticCatalogRev = cosmeticCatalogRev,
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
                        } else {
                            // Live `reverseLayout = true` chat list: prepending older history causes zero viewport jump!
                            val streamEntries = remember(state.messages, isModernCleanMode, expandedSystemGroupIds) {
                                buildReversedChatStreamEntries(
                                    chronologicalMessages = state.messages,
                                    enableSystemFolding = isModernCleanMode,
                                    expandedGroupIds = expandedSystemGroupIds,
                                )
                            }
                            val nearVisualTop by remember {
                                derivedStateOf {
                                    val info = listState.layoutInfo
                                    val total = info.totalItemsCount
                                    val lastIdx = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                                    total > 0 && lastIdx >= (total - 6).coerceAtLeast(0)
                                }
                            }
                            val oldestServerMsgId = remember(state.messages) {
                                state.messages.firstOrNull { it.id > 0L }?.id ?: 0L
                            }
                            val olderLoadFailed = state.olderLoadFailedByGroup[state.activeGroupId] == true
                            val latestHasMoreOlder by rememberUpdatedState(state.hasMoreOlder)
                            val latestIsLoadingHistory by rememberUpdatedState(state.isLoadingHistory)
                            val latestOldestServerMsgId by rememberUpdatedState(oldestServerMsgId)
                            LaunchedEffect(
                                nearVisualTop,
                                state.activeGroupId,
                                state.hasMoreOlder,
                                state.isLoadingHistory,
                                oldestServerMsgId,
                                olderLoadFailed,
                            ) {
                                if (nearVisualTop &&
                                    state.hasMoreOlder &&
                                    !state.isLoadingHistory &&
                                    !olderLoadFailed &&
                                    oldestServerMsgId > 0L &&
                                    autoOlderAttemptedId != oldestServerMsgId
                                ) {
                                    autoOlderAttemptedId = oldestServerMsgId
                                    controller.requestLoadOlderHistory(state.activeGroupId)
                                }
                            }
                            val pullTopHistoryNestedScroll = remember(listState, controller) {
                                var pullDownAccumPx = 0f
                                object : NestedScrollConnection {
                                    override fun onPreScroll(
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset {
                                        if (source == NestedScrollSource.UserInput && !listState.canScrollForward) {
                                            if (available.y > 0f) {
                                                pullDownAccumPx += available.y
                                                if (pullDownAccumPx >= 24f &&
                                                    latestHasMoreOlder &&
                                                    !latestIsLoadingHistory &&
                                                    latestOldestServerMsgId > 0L
                                                ) {
                                                    pullDownAccumPx = 0f
                                                    autoOlderAttemptedId = latestOldestServerMsgId
                                                    controller.requestLoadOlderHistory()
                                                }
                                            } else if (available.y < 0f) {
                                                pullDownAccumPx = 0f
                                            }
                                        } else {
                                            pullDownAccumPx = 0f
                                        }
                                        return Offset.Zero
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    reverseLayout = true,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(pullTopHistoryNestedScroll),
                                    contentPadding = PaddingValues(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = listTopPad,
                                        bottom = listBottomPad,
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                itemsIndexed(
                                    streamEntries,
                                    key = { _, e -> e.stableKey },
                                ) { revIdx, entry ->
                                    when (entry) {
                                        is ChatStreamEntry.FoldedSystemGroup -> {
                                            val allRedpack = entry.messages.all { m ->
                                                m.systemKind in setOf("redpack_result", "redpack_draw", "redpack_due", "ldc_winner", "ldc_cancel", "ldc_expired")
                                            }
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Surface(
                                                    color = palette.surface2.copy(alpha = if (palette.isDark) 0.72f else 0.85f),
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            expandedSystemGroupIds = expandedSystemGroupIds + entry.groupKeyId
                                                        },
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        Icon(
                                                            imageVector = if (allRedpack) Icons.Rounded.CardGiftcard else Icons.Rounded.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = if (allRedpack) palette.redpacket.copy(alpha = 0.85f) else palette.gold,
                                                            modifier = Modifier.size(13.dp),
                                                        )
                                                        Text(
                                                            text = if (allRedpack) {
                                                                "已折叠 ${entry.messages.size} 条红包结算播报"
                                                            } else {
                                                                "已折叠 ${entry.messages.size} 条系统播报"
                                                            },
                                                            color = palette.muted,
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                        )
                                                        Text(
                                                            text = "展开 ▾",
                                                            color = palette.primary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        is ChatStreamEntry.CollapseSystemGroupHandle -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Surface(
                                                    color = palette.surface2.copy(alpha = if (palette.isDark) 0.72f else 0.85f),
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            expandedSystemGroupIds = expandedSystemGroupIds - entry.groupKeyId
                                                        },
                                                ) {
                                                    Text(
                                                        text = "收起 ${entry.count} 条播报 ▴",
                                                        color = palette.primary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                    )
                                                }
                                            }
                                        }
                                        is ChatStreamEntry.MessageItem -> {
                                            val msg = entry.message
                                            val cont = entry.isContinuation
                                            Column {
                                                if (!cont && revIdx < streamEntries.lastIndex) {
                                                    Spacer(Modifier.height(5.dp))
                                                }
                                                RenderChatItemRow(
                                                    msg = msg,
                                                    isContinuation = cont,
                                                    highlighted = msg.id > 0L && highlightedMsgId == msg.id,
                                                    cleanDecoMode = state.cleanDecoMode,
                                                    cleanDecoConfig = cleanDecoConfig,
                                                    meId = me?.id ?: 0L,
                                                    cosmeticCatalogRev = cosmeticCatalogRev,
                                                    api = api,
                                                    enableSwipeReply = !canSwipeSwitchGroups,
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
                                    }
                                }

                                // Visual TOP of reverseLayout LazyColumn
                                if (state.hasMoreOlder) {
                                    item(key = "older_header") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !state.isLoadingHistory) {
                                                    controller.requestLoadOlderHistory(state.activeGroupId)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                when {
                                                    state.isLoadingHistory -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp,
                                                            color = palette.primary,
                                                        )
                                                        Text(
                                                            text = "正在加载更早消息…",
                                                            color = palette.muted,
                                                            fontSize = 12.sp,
                                                        )
                                                    }
                                                    olderLoadFailed -> {
                                                        Text(
                                                            text = "加载更早消息失败 · 下拉或点击重试",
                                                            color = palette.primary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                        )
                                                    }
                                                    else -> {
                                                        Text(
                                                            text = "↑ 继续下拉加载更早消息",
                                                            color = palette.muted,
                                                            fontSize = 11.5.sp,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (state.messages.size >= 8) {
                                    item(key = "older_end_marker") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "— 已加载到最早可见消息 —",
                                                color = palette.muted.copy(alpha = 0.65f),
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }

                                // Subtle bottom gradient fade cleanly into composer deck
                                if (isModernCleanMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, palette.bg),
                                                ),
                                            ),
                                    )
                                }

                                // Floating compact unread pill (Modern UI)
                                val boundaryId = state.unreadEntryBoundaryByGroup[state.activeGroupId]
                                val firstUnreadMsg = if (isModernCleanMode && boundaryId != null && boundaryId > 0L) {
                                    remember(state.messages, boundaryId) {
                                        state.messages.firstOrNull { it.id > boundaryId && !it.mine && !it.deleted }
                                    }
                                } else {
                                    null
                                }
                                if (firstUnreadMsg != null) {
                                    Surface(
                                        color = if (palette.isDark) palette.surface2 else palette.surface,
                                        shape = CircleShape,
                                        shadowElevation = if (palette.isDark) 6.dp else 3.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 8.dp)
                                            .border(1.dp, palette.primary.copy(alpha = 0.35f), CircleShape)
                                            .clip(CircleShape)
                                            .clickable {
                                                controller.clearUnreadBoundary(state.activeGroupId)
                                                jumpToMessage(firstUnreadMsg.id, state.activeGroupId)
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = palette.primary, modifier = Modifier.size(15.dp))
                                            Text(
                                                text = "有新未读消息 · 点击定位",
                                                color = palette.primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "关闭未读提示",
                                                tint = palette.muted,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clip(CircleShape)
                                                    .clickable { controller.clearUnreadBoundary(state.activeGroupId) },
                                            )
                                        }
                                    }
                                }

                                // Floating multi-redpack queue chip when >=2 unclaimed packets are loaded
                                if (unclaimedRedpackMsgs.size >= 2 && disguiseMode == AppDisguiseMode.NONE) {
                                    Surface(
                                        color = Color(0xFFB71C1C),
                                        shape = CircleShape,
                                        shadowElevation = 5.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(
                                                top = if (firstUnreadMsg != null) 44.dp else 8.dp,
                                                end = 12.dp,
                                            )
                                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.85f), CircleShape)
                                            .clip(CircleShape)
                                            .clickable {
                                                val first = unclaimedRedpackMsgs.firstOrNull() ?: return@clickable
                                                redpackFallbackMeta = first.parseRedpackMeta()
                                                redpackSenderName = first.displayAuthor
                                                redpackDialogMsgId = first.id
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.CardGiftcard,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "待拆红包 ×${unclaimedRedpackMsgs.size} · 连拆",
                                                color = Color(0xFFFFF8E1),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
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
                                            .padding(
                                                end = 16.dp,
                                                bottom = if (isModernCleanMode) 12.dp else 10.dp,
                                            )
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
                Box(modifier = viewportModifier) {
                    if (me == null) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(top = listTopPad, bottom = listBottomPad),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(onClick = onRequireLogin) { Text("登录后查看私信") }
                        }
                    } else if (dmMessages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(top = listTopPad, bottom = listBottomPad),
                            contentAlignment = Alignment.Center,
                        ) {
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
                                    val isFeishuDm = disguiseMode == AppDisguiseMode.FEISHU
                                    val dmShape = when {
                                        isFeishuDm && isMine -> RoundedCornerShape(topStart = 10.dp, topEnd = 2.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
                                        isFeishuDm -> RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
                                        palette.isModernUi && isMine -> RoundedCornerShape(topStart = 18.dp, topEnd = 5.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                                        palette.isModernUi -> RoundedCornerShape(topStart = 5.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                                        else -> RoundedCornerShape(15.dp)
                                    }
                                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                                        Surface(
                                            color = if (isMine) palette.bubbleSelf else palette.surface,
                                            shape = dmShape,
                                            shadowElevation = if (palette.isModernUi && !isFeishuDm && !palette.isDark) 0.5.dp else 0.dp,
                                            modifier = Modifier
                                                .widthIn(max = 286.dp)
                                                .then(
                                                    when {
                                                        isFeishuDm -> Modifier
                                                        palette.isModernUi -> if (palette.cardBorder != Color.Transparent) {
                                                            Modifier.border(0.5.dp, palette.cardBorder, dmShape)
                                                        } else Modifier
                                                        else -> Modifier.border(
                                                            1.dp,
                                                            if (isMine) palette.primary.copy(alpha = 0.35f) else palette.line,
                                                            dmShape,
                                                        )
                                                    },
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
                    if (isModernCleanMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, palette.bg),
                                    ),
                                ),
                        )
                    }
            }
        }
        }

        @Composable
        fun RenderBottomComposerOverlay() {
            // Reply / Edit target bar
            val reply = state.replyTarget
            val edit = state.editTarget
            if ((activePane == "group" && (reply != null || edit != null)) || (activePane == "dm" && dmEditTarget != null)) {
                val isModernFloatingReply = palette.isModernUi && disguiseMode == AppDisguiseMode.NONE
                val replyShape = if (isModernFloatingReply) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
                Surface(
                    color = if (isModernFloatingReply) palette.bg else palette.surface2,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        color = if (isModernFloatingReply) {
                            if (palette.isDark) palette.surface2 else palette.surface
                        } else palette.surface2,
                        shape = replyShape,
                        shadowElevation = if (isModernFloatingReply && !palette.isDark) 2.dp else 0.dp,
                        modifier = if (isModernFloatingReply) {
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                                .border(1.dp, palette.glassBorder, replyShape)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = if (isModernFloatingReply) 7.dp else 5.dp),
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
            }

            // `[聊天 | 猜 | 问]` input mode bar when active Guess Room round runs
            if (activePane == "group" && activeRound != null && state.editTarget == null) {
                Surface(color = palette.bg, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = if (isModernCleanMode) {
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        contentAlignment = Alignment.Center,
                    ) {
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
                }
            }

            // Sticker tray (Group & DM!)
            if (showStickerTray) {
                val isModernTray = palette.isModernUi && disguiseMode == AppDisguiseMode.NONE
                val stickerShape = if (isModernTray) RoundedCornerShape(22.dp) else RoundedCornerShape(0.dp)
                Surface(
                    color = if (isModernTray) palette.bg else palette.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        color = palette.surface,
                        shape = stickerShape,
                        shadowElevation = if (isModernTray && !palette.isDark) 4.dp else 0.dp,
                        modifier = if (isModernTray) {
                            Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .widthIn(max = 408.dp)
                                .fillMaxWidth()
                                .height(210.dp)
                                .border(1.dp, palette.glassBorder, stickerShape)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .border(1.dp, palette.line)
                        },
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
            }

            // `[＋]` Attachment Tray (`相册`, `文件`, `发红包`, `欢欢猜词`)
            AnimatedVisibility(visible = showAttachmentTray) {
                val isModernTray = palette.isModernUi && disguiseMode == AppDisguiseMode.NONE
                val trayShape = when {
                    isModernTray -> RoundedCornerShape(22.dp)
                    palette.isModernUi -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Surface(
                    color = if (isModernTray) palette.bg else Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        color = when {
                            isModernTray -> if (palette.isDark) palette.surface2 else palette.surface
                            palette.isModernUi -> palette.surfaceRaised
                            else -> palette.surface
                        },
                        shape = trayShape,
                        shadowElevation = if (isModernTray && !palette.isDark) 4.dp else 0.dp,
                        modifier = Modifier.then(
                            when {
                                isModernTray -> Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .widthIn(max = 408.dp)
                                    .fillMaxWidth()
                                    .border(1.dp, palette.glassBorder, trayShape)
                                palette.isModernUi -> Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, trayShape) else Modifier,
                                    )
                                else -> Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.line.copy(alpha = 0.7f))
                            },
                        ),
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
            }

        // Slim Composer Capsule (`Emoji` + Input + `[＋]` + `Send`; modern floating pill when palette.isModernUi; authentic Feishu 7.x bar in FEISHU mode)
        val isFeishuComposer = disguiseMode == AppDisguiseMode.FEISHU
        val swipeThresholdPx = with(density) { 48.dp.toPx() }
        val composerRowContent = @Composable { rowModifier: Modifier ->
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isFeishuComposer) 8.dp else 5.dp),
            ) {
                if (isFeishuComposer) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.4.dp, palette.muted.copy(alpha = 0.8f), CircleShape)
                            .clickable {
                                showAttachmentTray = false
                                showStickerTray = !showStickerTray
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "快捷语音短语",
                            tint = palette.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
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
                }

                val inputShape = RoundedCornerShape(if (isFeishuComposer) 8.dp else 18.dp)
                val inputBg = if (palette.isModernUi && !isFeishuComposer) Color.Transparent else palette.surface2
                Surface(
                    color = inputBg,
                    shape = inputShape,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isFeishuComposer || palette.isModernUi) {
                                Modifier
                            } else {
                                Modifier.border(1.dp, palette.line.copy(alpha = 0.8f), inputShape)
                            },
                        )
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
                            .padding(horizontal = if (palette.isModernUi && !isFeishuComposer) 6.dp else 12.dp, vertical = 8.dp),
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = when {
                                    me == null -> if (isFeishuComposer) "登录企业账号后发言" else "登录后参与聊天"
                                    activePane == "dm" -> if (isFeishuComposer) "发送给同事…" else "发送私信…"
                                    inputMode == ChatInputMode.GUESS -> if (disguiseMode != AppDisguiseMode.NONE) "输入待提交内容…" else "输入猜词答案（自动加 !）"
                                    inputMode == ChatInputMode.ASK -> if (disguiseMode != AppDisguiseMode.NONE) "输入提问内容…" else "向欢欢提一个是非问题…"
                                    isFeishuComposer -> "发送给当前群组…"
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

                if (isFeishuComposer) {
                    IconButton(
                        onClick = {
                            showAttachmentTray = false
                            showStickerTray = !showStickerTray
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEmotions,
                            contentDescription = "表情",
                            tint = if (showStickerTray) palette.primary else palette.muted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        showStickerTray = false
                        showAttachmentTray = !showAttachmentTray
                    },
                    modifier = Modifier.size(if (isFeishuComposer) 30.dp else 34.dp),
                ) {
                    Icon(
                        imageVector = when {
                            showAttachmentTray -> Icons.Rounded.Close
                            isFeishuComposer -> Icons.Rounded.AddCircleOutline
                            else -> Icons.Rounded.Add
                        },
                        contentDescription = "更多功能",
                        tint = if (showAttachmentTray) palette.primary else palette.muted,
                        modifier = Modifier.size(if (isFeishuComposer) 23.dp else 21.dp),
                    )
                }

                if (!isFeishuComposer || draft.isNotBlank()) {
                    if (isFeishuComposer) {
                        Surface(
                            color = palette.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { submitActiveDraft() },
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "发送",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    } else if (palette.isModernUi) {
                        val canSend = draft.isNotBlank()
                        Surface(
                            color = if (canSend) palette.primary else palette.surface2,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { submitActiveDraft() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "发送",
                                    tint = if (canSend) palette.onPrimary else palette.muted,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    } else {
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
        }

        if (palette.isModernUi && !isFeishuComposer) {
            Surface(
                color = palette.bg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = modernBottomComposerPad,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = if (palette.isDark) palette.surface2 else palette.surface,
                        shape = CircleShape,
                        shadowElevation = if (palette.isDark) 6.dp else 3.dp,
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth()
                            .border(1.dp, palette.glassBorder, CircleShape),
                    ) {
                        composerRowContent(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        } else {
            val composerSurfaceBg = if (isFeishuComposer) palette.surfaceRaised else palette.surface
            Surface(color = composerSurfaceBg) {
                Column {
                    if (isFeishuComposer) {
                        HorizontalDivider(color = palette.line, thickness = 0.5.dp)
                    }
                    composerRowContent(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isFeishuComposer) 10.dp else 8.dp, vertical = if (isFeishuComposer) 7.dp else 5.dp),
                    )
                }
            }
        }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            RenderTopBarAndBanners()
            RenderMessagesViewport(viewportModifier = Modifier.weight(1f).fillMaxWidth())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RenderBottomComposerOverlay()
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
                val remaining = state.groups.filter { it.joined && it.id != activeGroup.id }
                if (bypassSessionList && remaining.isNotEmpty()) {
                    controller.setActiveGroup(remaining.first().id)
                } else {
                    activePane = "list"
                }
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
            onSendDm = if (state.dmEnabled || disguiseMode == AppDisguiseMode.FEISHU) {
                { peerUser ->
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
                }
            } else {
                null
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
            onSelectGuessMsgId = { mid ->
                showRedpackArchiveSheet = false
                guessInitialMid = mid
                showGuessSheet = true
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
                    val sent = controller.sendOrEditMessage(phrase)
                    autoMatchSentMessageRedpacks(sent)
                }
            },
            onOpenArchive = { showRedpackArchiveSheet = true },
            onUpdated = { scope.launch { controller.pollOnce(state.activeGroupId) } },
            queueMessages = unclaimedRedpackMsgs,
            onSwitchPacket = { nextPkt ->
                redpackFallbackMeta = nextPkt.parseRedpackMeta()
                redpackSenderName = nextPkt.displayAuthor
                redpackDialogMsgId = nextPkt.id
            },
            onPacketTerminalOrClaimed = { doneId ->
                sessionClaimedOrEndedPacketIds = sessionClaimedOrEndedPacketIds + doneId
            },
            onClaimPhraseBatch = { phrase, primaryMid ->
                claimPhraseBatch(phrase, primaryMid)
            },
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

internal sealed interface ChatStreamEntry {
    val stableKey: String

    data class MessageItem(
        val message: ChatMessage,
        val isContinuation: Boolean,
    ) : ChatStreamEntry {
        override val stableKey: String =
            if (message.id > 0L) "srv_${message.id}" else "loc_${message.localId ?: message.createdAt}"
    }

    data class FoldedSystemGroup(
        val groupKeyId: Long,
        val messages: List<ChatMessage>,
    ) : ChatStreamEntry {
        override val stableKey: String = "sysfold_$groupKeyId"
    }

    data class CollapseSystemGroupHandle(
        val groupKeyId: Long,
        val count: Int,
    ) : ChatStreamEntry {
        override val stableKey: String = "syscollapse_$groupKeyId"
    }
}

internal fun buildReversedChatStreamEntries(
    chronologicalMessages: List<ChatMessage>,
    enableSystemFolding: Boolean,
    expandedGroupIds: Set<Long>,
): List<ChatStreamEntry> {
    if (chronologicalMessages.isEmpty()) return emptyList()
    val entries = ArrayList<ChatStreamEntry>(chronologicalMessages.size)
    var i = 0
    while (i < chronologicalMessages.size) {
        val m = chronologicalMessages[i]
        if (!m.systemKind.isNullOrBlank()) {
            val startIdx = i
            val run = ArrayList<ChatMessage>()
            while (i < chronologicalMessages.size && !chronologicalMessages[i].systemKind.isNullOrBlank()) {
                run.add(chronologicalMessages[i])
                i++
            }
            if (!enableSystemFolding || run.size < 2) {
                for (sysMsg in run) {
                    entries.add(ChatStreamEntry.MessageItem(sysMsg, isContinuation = false))
                }
            } else {
                val lastItem = run.last()
                val gid = lastItem.id.takeIf { it > 0L }
                    ?: -(lastItem.createdAt.coerceAtLeast(1L) * 1000L + startIdx.toLong())
                if (gid in expandedGroupIds) {
                    for (sysMsg in run) {
                        entries.add(ChatStreamEntry.MessageItem(sysMsg, isContinuation = false))
                    }
                    entries.add(ChatStreamEntry.CollapseSystemGroupHandle(groupKeyId = gid, count = run.size))
                } else {
                    entries.add(ChatStreamEntry.FoldedSystemGroup(groupKeyId = gid, messages = run))
                }
            }
        } else {
            val prevChrono = if (i > 0) chronologicalMessages[i - 1] else null
            entries.add(ChatStreamEntry.MessageItem(m, isContinuation = canGroupContinuation(prevChrono, m)))
            i++
        }
    }
    return entries.asReversed()
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
    cleanDecoConfig: DecoConfig,
    meId: Long,
    cosmeticCatalogRev: Int,
    api: ForHappyApiClient,
    enableSwipeReply: Boolean = true,
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
            cleanDecoConfig = cleanDecoConfig,
            meId = meId,
            cosmeticCatalogRev = cosmeticCatalogRev,
            api = api,
            enableSwipeReply = enableSwipeReply,
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

    val novelShape = if (palette.isModernUi) RoundedCornerShape(16.dp) else RoundedCornerShape(12.dp)
    Surface(
        color = cardBg,
        shape = novelShape,
        shadowElevation = if (palette.isModernUi && !palette.isDark) 1.dp else 0.dp,
        modifier = Modifier
            .widthIn(min = 210.dp, max = 280.dp)
            .then(
                when {
                    !palette.isModernUi -> Modifier.border(1.dp, palette.line.copy(alpha = 0.7f), novelShape)
                    palette.cardBorder != Color.Transparent -> Modifier.border(0.5.dp, palette.cardBorder, novelShape)
                    else -> Modifier
                },
            )
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

    if (palette.isModernUi) {
        val actionPair: Pair<String, () -> Unit>? = if (!msg.deleted && refId != null && refId > 0L) {
            when {
                kind == "title_raffle_open" && msg.body.contains("报名") -> "点我报名" to { onActionRaffle(refId) }
                kind == "ldc_game" -> "对应发言" to { onJumpMessage(refId) }
                kind in setOf("ldc_winner", "ldc_cancel", "ldc_expired") -> "看猜词结果" to { onActionGuess(refId) }
                isRedpackNotice -> "去看看" to { onActionRedpack(refId) }
                else -> null
            }
        } else null
        val compactLine = remember(headline, explanation) {
            val singleHead = headline.replace('\n', ' ').trim()
            if (explanation.isNullOrBlank()) singleHead else "$singleHead · 释义：${explanation.replace('\n', ' ').trim()}"
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = palette.surface2.copy(alpha = if (palette.isDark) 0.72f else 0.84f),
                shape = CircleShape,
                modifier = Modifier
                    .widthIn(max = 336.dp)
                    .clip(CircleShape)
                    .then(
                        if (actionPair != null) {
                            Modifier.clickable { actionPair.second() }
                        } else Modifier,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isRedpackNotice || kind.startsWith("ldc")) {
                        Icon(
                            imageVector = if (isRedpackNotice) Icons.Rounded.CardGiftcard else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (isRedpackNotice) palette.redpacket.copy(alpha = 0.85f) else palette.gold,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = compactLine,
                        color = palette.muted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (actionPair != null) {
                        Text(
                            text = "${actionPair.first} ›",
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        return
    }

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
    cleanDecoConfig: DecoConfig,
    meId: Long,
    cosmeticCatalogRev: Int,
    api: ForHappyApiClient,
    enableSwipeReply: Boolean = true,
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

    val layoutDir = LocalLayoutDirection.current
    val sysGestures = WindowInsets.systemGestures
    val baseEdgeGuardPx = with(density) { 36.dp.toPx() }
    val leftEdgeGuardPx = maxOf(baseEdgeGuardPx, sysGestures.getLeft(density, layoutDir).toFloat())
    val rightEdgeGuardPx = maxOf(baseEdgeGuardPx, sysGestures.getRight(density, layoutDir).toFloat())
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
            .then(
                if (enableSwipeReply) {
                    Modifier.pointerInput(msg.id, msg.localId, leftEdgeGuardPx, rightEdgeGuardPx, quoteTriggerPx) {
                        // Edge-safe swipe-to-quote (`04-gestures.md`: preserve left & right system back edges!)
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.position.x < leftEdgeGuardPx || down.position.x > size.width - rightEdgeGuardPx) {
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
                } else {
                    Modifier
                },
            )
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

        val isDisguised = disguiseMode != AppDisguiseMode.NONE
        val isOwn = msg.mine || (meId > 0L && msg.effectiveUserId == meId)
        val hideAll = isDisguised || cleanDecoMode
        val hideNick = hideAll || (!isOwn && cleanDecoConfig.cleanHideOthersNick)
        val hideTitle = hideAll || (!isOwn && cleanDecoConfig.cleanHideOthersStyles)
        val hideBadge = hideAll || (!isOwn && cleanDecoConfig.cleanHideOthersBadges)
        val hideBubble = hideAll || (!isOwn && cleanDecoConfig.cleanHideBubbles)
        val hideCss = hideTitle || hideBubble

        Column(
            horizontalAlignment = if (msg.mine && !isLinuxDo) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 318.dp),
        ) {
            if (!isContinuation) {
                val chatIdentity = remember(
                    msg.user?.deco,
                    msg.user?.badges,
                    msg.user?.officialTitles,
                    msg.user?.isAdmin,
                    msg.user?.isFancyGranted,
                    msg.user?.cosmeticAccess,
                    hideAll,
                    palette.isDark,
                    cosmeticCatalogRev,
                ) {
                    if (!hideAll) {
                        BadgeAndCosmeticRegistry.resolveEquippedIdentity(
                            decoJson = msg.user?.deco,
                            badgesJson = msg.user?.badges,
                            officialTitles = msg.user?.officialTitles.orEmpty(),
                            where = "chat",
                            isAdmin = msg.user?.isAdmin == true,
                            hasFancyPermission = msg.user?.isFancyGranted,
                            cosmeticAccess = msg.user?.cosmeticAccess,
                            isDarkTheme = palette.isDark,
                        )
                    } else {
                        null
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        5.dp,
                        if (msg.mine && !isLinuxDo) Alignment.End else Alignment.Start,
                    ),
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    if (!msg.mine || isLinuxDo) {
                        val gradColors = remember(chatIdentity?.nickGradientHexes, hideNick, palette.muted) {
                            if (hideNick) {
                                null
                            } else {
                                chatIdentity?.nickGradientHexes?.map { parseSafeHex(it, palette.muted) }?.takeIf { it.size >= 2 }
                            }
                        }
                        val singleColor = remember(chatIdentity?.nickGradientHexes, hideNick, palette.muted) {
                            if (hideNick) {
                                null
                            } else {
                                chatIdentity?.nickGradientHexes?.singleOrNull()?.let { parseSafeHex(it, palette.muted) }
                            }
                        }
                        Text(
                            text = shownAuthor,
                            style = when {
                                gradColors != null -> TextStyle(
                                    brush = Brush.horizontalGradient(gradColors),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                singleColor != null -> TextStyle(
                                    color = singleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                else -> TextStyle(color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!hideTitle) {
                        if (chatIdentity?.title != null) {
                            TitleChipView(title = chatIdentity.title)
                        } else {
                            msg.title?.takeIf { it.isNotBlank() }?.let { titleLabel ->
                                BadgePill(titleLabel, containerColor = palette.gold.copy(alpha = 0.16f), contentColor = palette.gold)
                            }
                        }
                    }
                    if (!hideBadge) {
                        chatIdentity?.badge?.let { badgeChip ->
                            BadgeChipView(badge = badgeChip, imageLoader = api.imageLoader, mini = true)
                        }
                    }
                    Text(shortTimeStr, color = palette.muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    if (disguiseMode != AppDisguiseMode.FEISHU) {
                        msg.floor?.let { fl ->
                            Text("${fl}楼", color = palette.muted.copy(alpha = 0.65f), fontSize = 10.sp)
                        }
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

            val isFeishu = disguiseMode == AppDisguiseMode.FEISHU
            val redpackMeta = if (!msg.deleted) msg.parseRedpackMeta() else null
            val novelExcerpt = remember(msg.body, msg.deleted) {
                if (!msg.deleted) ChatRichTextParser.parseNovelExcerpt(msg.body) else null
            }
            val bubbleBg = when {
                isLinuxDo -> Color.Transparent
                isFeishu && msg.mine -> palette.bubbleSelf
                isFeishu -> palette.surface
                palette.isModernUi && msg.mine -> palette.bubbleSelf
                palette.isModernUi -> palette.surface
                msg.mine -> palette.primary.copy(alpha = 0.14f)
                else -> palette.surface
            }
            val bubbleBorder = when {
                isLinuxDo -> Color.Transparent
                highlighted -> palette.gold
                isFeishu -> Color.Transparent
                palette.isModernUi -> palette.cardBorder
                msg.mine -> palette.primary.copy(alpha = 0.35f)
                else -> palette.line
            }
            val bubbleShape = when {
                isLinuxDo -> RoundedCornerShape(0.dp)
                isFeishu && msg.mine -> RoundedCornerShape(topStart = 10.dp, topEnd = 2.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
                isFeishu -> RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
                palette.isModernUi && msg.mine -> RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = if (isContinuation) 18.dp else 5.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 18.dp,
                )
                palette.isModernUi -> RoundedCornerShape(
                    topStart = if (isContinuation) 18.dp else 5.dp,
                    topEnd = 18.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 18.dp,
                )
                else -> RoundedCornerShape(15.dp)
            }

            @Composable
            fun ReplyPreviewBar(insideBubble: Boolean) {
                msg.replyTo?.let { rep ->
                    val cleanRefText = remember(rep.resolvedText, disguiseMode) {
                        val base = ChatRichTextParser.previewSummary(rep.resolvedText).ifBlank {
                            if (rep.deleted) "消息已撤回" else if (rep.file != null) "[文件] ${rep.file}" else if (rep.hasImage || rep.thumb != null) "[图片]" else "…"
                        }
                        DisguiseSanitizer.sanitizeSummaryForDisguise(base, disguiseMode)
                    }
                    val repAuthor = remember(rep.id, rep.resolvedAuthor, isFeishu, stealth.feishuMemberAliasEnabled) {
                        if (isFeishu && stealth.feishuMemberAliasEnabled) {
                            DisguiseSanitizer.mapColleagueName(rep.id, rep.resolvedAuthor, true)
                        } else {
                            rep.resolvedAuthor
                        }
                    }
                    val repNickHexes = remember(
                        rep.deco,
                        rep.isAdmin,
                        rep.isFancyGranted,
                        hideNick,
                        palette.isDark,
                        cosmeticCatalogRev,
                    ) {
                        if (hideNick || isDisguised) {
                            emptyList()
                        } else {
                            BadgeAndCosmeticRegistry.resolveEquippedIdentity(
                                decoJson = rep.deco,
                                where = "chat",
                                isAdmin = rep.isAdmin,
                                hasFancyPermission = rep.isFancyGranted,
                                isDarkTheme = palette.isDark,
                            ).nickGradientHexes
                        }
                    }
                    val repLineAnnotated = remember(repAuthor, cleanRefText, repNickHexes, palette.muted, palette.primary) {
                        buildAnnotatedString {
                            val grad = repNickHexes.map { parseSafeHex(it, palette.muted) }
                            when {
                                grad.size >= 2 -> withStyle(
                                    SpanStyle(brush = Brush.horizontalGradient(grad), fontWeight = FontWeight.SemiBold),
                                ) {
                                    append(repAuthor)
                                }
                                grad.size == 1 -> withStyle(
                                    SpanStyle(color = grad.first(), fontWeight = FontWeight.SemiBold),
                                ) {
                                    append(repAuthor)
                                }
                                else -> append(repAuthor)
                            }
                            append("：$cleanRefText")
                        }
                    }
                    Surface(
                        color = if (insideBubble) palette.surface2 else palette.surface2.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(if (isFeishu) 6.dp else 9.dp),
                        modifier = Modifier
                            .then(
                                if (!insideBubble) {
                                    Modifier.border(0.8.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .clickable(enabled = rep.id > 0L) { onQuoteClick(rep.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (isFeishu || !insideBubble) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(14.dp)
                                        .background(palette.primary.copy(alpha = 0.7f), RoundedCornerShape(2.dp)),
                                )
                            }
                            Text(
                                text = repLineAnnotated,
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
            }

            if (isDisguised) {
                Surface(
                    color = bubbleBg,
                    shape = bubbleShape,
                    modifier = if (isLinuxDo || (isFeishu && !highlighted)) {
                        Modifier
                    } else {
                        Modifier.border(if (highlighted) 1.5.dp else 1.dp, bubbleBorder, bubbleShape)
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        ReplyPreviewBar(insideBubble = true)

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
                                        collapsible = !msg.deleted,
                                        collapsedMaxLines = 8,
                                        collapseStateKey = "msg_fold_${msg.id}_${msg.localId.orEmpty()}",
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
            } else {
                // Normal 52dmeme mode: reply bar above bubble, bubbleless stickers/solo-images, full cosmetic frames + contrast-safe text, media outside frame below
                Column(
                    horizontalAlignment = if (msg.mine) Alignment.End else Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (msg.replyTo != null) {
                        ReplyPreviewBar(insideBubble = false)
                    }

                    if (msg.deleted) {
                        Surface(
                            color = bubbleBg,
                            shape = bubbleShape,
                            modifier = if (bubbleBorder != Color.Transparent) {
                                Modifier.border(1.dp, bubbleBorder, bubbleShape)
                            } else Modifier,
                        ) {
                            Text(
                                text = "${if (msg.mine) "你" else msg.displayAuthor} 撤回了一条消息",
                                color = palette.muted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            )
                        }
                    } else {
                        if (redpackMeta != null) {
                            RedpackBubbleCard(meta = redpackMeta, api = api, onClick = onOpenRedpack)
                        }
                        if (novelExcerpt != null) {
                            NovelExcerptCard(spec = novelExcerpt, onOpenBook = { onOpenNovelExcerpt(novelExcerpt) })
                        } else {
                            val isEditedMsg = msg.edited || msg.editedAt != null
                            val stkOnly = remember(msg.body) { ChatRichTextParser.isStickerOnly(msg.body) }
                            val bodyImgOnly = remember(msg.body) {
                                val parsed = ChatRichTextParser.parse(msg.body)
                                parsed.bodyImgOnly && parsed.inlineImageUrls.isNotEmpty()
                            }
                            if (msg.body.isNotBlank() && (redpackMeta == null || redpackMeta.shared)) {
                                if (stkOnly || bodyImgOnly) {
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
                                } else {
                                    val bubbleStyle = remember(
                                        msg.user?.deco,
                                        msg.user?.cosmeticAccess,
                                        msg.user?.isAdmin,
                                        msg.id,
                                        hideBubble,
                                        hideCss,
                                        palette.isDark,
                                        cosmeticCatalogRev,
                                    ) {
                                        ChatBubbleCosmetics.resolveChatBubbleStyle(
                                            user = msg.user,
                                            messageId = msg.id.toString(),
                                            isDarkTheme = palette.isDark,
                                            hideBubble = hideBubble,
                                            hideCss = hideCss,
                                        )
                                    }
                                    ChatCosmeticBubbleBox(
                                        style = bubbleStyle,
                                        defaultContainerColor = bubbleBg,
                                        defaultBorderColor = if (highlighted) palette.gold else bubbleBorder,
                                        defaultShape = bubbleShape,
                                        defaultPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                                        imageLoader = api.imageLoader,
                                    ) {
                                        RichChatBodyView(
                                            rawBody = msg.body,
                                            imageLoader = api.imageLoader,
                                            textColor = bubbleStyle?.textColor ?: palette.text,
                                            linkColor = bubbleStyle?.linkColor,
                                            mutedColor = bubbleStyle?.mutedColor,
                                            bodyFontFamily = bubbleStyle?.fontFamily,
                                            bodyFontWeight = bubbleStyle?.fontWeight,
                                            bodyFontStyle = bubbleStyle?.fontStyle,
                                            isEdited = isEditedMsg,
                                            collapsible = !msg.deleted,
                                            collapsedMaxLines = 8,
                                            collapseStateKey = "msg_fold_${msg.id}_${msg.localId.orEmpty()}",
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
                                    }
                                }
                            } else if (isEditedMsg && redpackMeta == null && msg.allMedia.isEmpty()) {
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
                        cleanDecoMode = cleanDecoMode,
                        cleanDecoConfig = cleanDecoConfig,
                        meId = meId,
                        cosmeticCatalogRev = cosmeticCatalogRev,
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
                    if (isFeishu) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenRepliesSheet() }
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.primary.copy(alpha = 0.78f)),
                            )
                            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = palette.primary, modifier = Modifier.size(13.dp))
                            Text("${msg.replyCount} 条回复", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = palette.primary, modifier = Modifier.size(14.dp))
                        }
                    } else {
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
    cleanDecoMode: Boolean,
    cleanDecoConfig: DecoConfig,
    meId: Long,
    cosmeticCatalogRev: Int,
    api: ForHappyApiClient,
    onTapBoost: (ChatBoost) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val stealth = LocalPrivacyStealthConfig.current
    val disguiseMode = stealth.disguiseMode
    val isDisguised = disguiseMode != AppDisguiseMode.NONE
    val aiBoosts = remember(boosts) { boosts.filter { it.ai } }
    val userBoosts = remember(boosts) { boosts.filter { !it.ai } }

    Column(horizontalAlignment = if (mineAlignEnd) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        aiBoosts.forEach { b ->
            val aiTitle = when (disguiseMode) {
                AppDisguiseMode.FEISHU -> "智能助手摘要"
                AppDisguiseMode.LINUX_DO -> "Bot 自动小结"
                AppDisguiseMode.NONE -> "欢欢 AI 回应"
            }
            val aiShort = if (disguiseMode == AppDisguiseMode.NONE) "欢欢" else "助"
            Surface(
                color = palette.primary.copy(alpha = 0.09f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, palette.primary.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            ) {
                Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForHappyAvatar(username = aiShort, avatarUrl = if (disguiseMode == AppDisguiseMode.NONE) b.avatar else null, imageLoader = api.imageLoader, size = 18.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(aiTitle, color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = DisguiseSanitizer.sanitizeSummaryForDisguise(
                                b.text.ifBlank { if (b.status == "thinking") "处理中…" else "…" },
                                disguiseMode,
                            ),
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
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
                    val bAuthor = if (disguiseMode == AppDisguiseMode.FEISHU && stealth.feishuMemberAliasEnabled) {
                        DisguiseSanitizer.mapColleagueName(b.displayAuthor.hashCode().toLong(), b.displayAuthor, true)
                    } else {
                        b.displayAuthor
                    }
                    if (isDisguised) {
                        Surface(
                            color = if (b.mine) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .then(
                                    if (disguiseMode == AppDisguiseMode.FEISHU) {
                                        Modifier
                                    } else {
                                        Modifier.border(1.dp, if (b.mine) palette.primary.copy(alpha = 0.50f) else palette.line.copy(alpha = 0.70f), RoundedCornerShape(999.dp))
                                    },
                                )
                                .clickable { onTapBoost(b) },
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ForHappyAvatar(username = bAuthor, avatarUrl = if (disguiseMode == AppDisguiseMode.FEISHU) null else b.avatar, imageLoader = api.imageLoader, size = 15.dp)
                                BoostPillText(text = DisguiseSanitizer.sanitizeSummaryForDisguise(b.text, disguiseMode), imageLoader = api.imageLoader, fontSize = 11.sp)
                            }
                        }
                    } else {
                        val bOwn = b.mine || (meId > 0L && b.userId == meId)
                        val hideBoostFrame = cleanDecoMode || (!bOwn && cleanDecoConfig.cleanHideBubbles)
                        val boostStyle = remember(
                            b.deco,
                            b.cosmeticAccess,
                            b.userId,
                            hideBoostFrame,
                            palette.isDark,
                            cosmeticCatalogRev,
                        ) {
                            ChatBubbleCosmetics.resolveBoostFrameStyle(
                                boost = b,
                                isDarkTheme = palette.isDark,
                                hideBubble = hideBoostFrame,
                                hideCss = hideBoostFrame,
                            )
                        }
                        ChatCosmeticBoostPill(
                            style = boostStyle,
                            defaultContainerColor = if (b.mine) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                            defaultBorderColor = if (b.mine) palette.primary.copy(alpha = 0.50f) else palette.line.copy(alpha = 0.70f),
                            onClick = { onTapBoost(b) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ForHappyAvatar(username = bAuthor, avatarUrl = b.avatar, imageLoader = api.imageLoader, size = 15.dp)
                                BoostPillText(
                                    text = b.text,
                                    imageLoader = api.imageLoader,
                                    fontSize = 11.sp,
                                    textColor = boostStyle?.textColor,
                                    frameFillColor = boostStyle?.fillColor,
                                )
                            }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (units > BOOST_MAX_LEN) palette.danger.copy(alpha = 0.14f) else palette.surface2,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = "$units/$BOOST_MAX_LEN",
                            color = if (units > BOOST_MAX_LEN) palette.danger else palette.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForHappyTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "自己写一句（$BOOST_MAX_LEN 字内）",
                    singleLine = true,
                    isError = units > BOOST_MAX_LEN,
                    minHeight = 40.dp,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = !busy && units in 1..BOOST_MAX_LEN,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
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

@Composable
private fun ModernChatHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = LocalForHappyPalette.current.text.copy(alpha = 0.86f),
    containerColor: Color = LocalForHappyPalette.current.surface2.copy(alpha = 0.78f),
    showDot: Boolean = false,
    dotColor: Color = LocalForHappyPalette.current.primary,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(34.dp)) {
        Surface(
            color = containerColor,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable { onClick() },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
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
