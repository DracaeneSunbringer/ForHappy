package com.jinhuan.forhappy.ui.wall

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreHoriz
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.BOOST_MAX_LEN
import com.jinhuan.forhappy.core.model.BOOST_PRESETS
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeComment
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemeMediaItem
import com.jinhuan.forhappy.core.model.PICKED_REACTIONS
import com.jinhuan.forhappy.core.model.PuzzleSpec
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.WhoReactedResponse
import com.jinhuan.forhappy.core.model.countReplyUnits
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.components.boostPresetAccent
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

/**
 * Full-screen L1 Post Detail Page (`02b §1–4`):
 * - Shared-element style entrance (`280ms`), hides bottom bar, translucent -> solid topBar.
 * - Multi-form media stage (`max 70% height`): Carousel / Grid / Long-image / Native Video / LIVE / Masked⇄Original.
 * - Strict 4-layer interactions: `♡ Like` + `一句话` (above stickers) + `贴纸` + Nested 1-level `评论` (AI badges, @mention, reply, image upload).
 * - Embedded collapsible multi-layer Puzzle card with error-shake feedback.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemeDetailModal(
    memeId: Long,
    api: ForHappyApiClient,
    me: MeUser? = null,
    onRequireLogin: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenImageFullscreen: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onStateUpdated: (MemeItem) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onDeleted: (Long) -> Unit = {},
    onAnchorsUpdated: (List<AnchorTag>) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onOpenEditWorkbench: ((MemeItem) -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val privacyConfig = LocalPrivacyStealthConfig.current
    val seenTracker = LocalSeenTracker.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var entered by remember(memeId) { mutableStateOf(false) }
    var item by remember(memeId) { mutableStateOf<MemeItem?>(null) }
    var comments by remember(memeId) { mutableStateOf<List<MemeComment>>(emptyList()) }
    var loading by remember(memeId) { mutableStateOf(true) }

    // Media / blur / original states (memory-only, reset on background or blur)
    var detailRevealed by remember(memeId, privacyConfig.blurAllEnabled) { mutableStateOf(false) }
    var detailShowOriginal by remember(memeId, privacyConfig.blurAllEnabled) { mutableStateOf(false) }
    var immersiveStartIndex by remember(memeId) { mutableStateOf<Int?>(null) }

    // Sheets & pickers
    var showMoreSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showBoostSheet by remember { mutableStateOf(false) }
    var whoReactedData by remember { mutableStateOf<WhoReactedResponse?>(null) }
    var stickerPacks by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var activeStickerTab by remember { mutableStateOf<String?>(null) }

    // Comments state
    var commentSort by remember { mutableStateOf("latest") } // "latest" | "hot"
    var commentEditing by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var commentReplyTarget by remember { mutableStateOf<MemeComment?>(null) }
    var showCommentStickers by remember { mutableStateOf(false) }
    var showMentionSuggestions by remember { mutableStateOf(false) }
    var uploadingCommentImg by remember { mutableStateOf(false) }
    var postingComment by remember { mutableStateOf(false) }
    var expandedThreads by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var commentActionTarget by remember { mutableStateOf<MemeComment?>(null) }
    val commentFocusRequester = remember { FocusRequester() }
    val commentThreads = remember(comments, commentSort) {
        buildDetailCommentThreads(comments, commentSort)
    }

    // Puzzle state
    var puzzleExpanded by remember(memeId) { mutableStateOf(false) }
    var puzzleInputFocused by remember { mutableStateOf(false) }

    // Inline feedback toast (likes breakdown / copy / errors)
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(2_400L)
            statusBanner = null
        }
    }

    LaunchedEffect(memeId) {
        entered = true
        loading = true
        val detailDef = async { api.fetchMemeDetail(memeId) }
        val commentsDef = async { api.fetchMemeComments(memeId) }
        val detail = detailDef.await()
        val fetchedComments = commentsDef.await()
        val resolved = detail?.resolvedMeme
        if (seenTracker != null) {
            seenTracker.markSeen(
                memeId = memeId,
                existingSeenAt = resolved?.seenAt,
                memeDay = resolved?.resolvedDay,
            )
        } else {
            runCatching { api.recordMemeViews(listOf(memeId)) }
        }
        item = resolved
        if (resolved?.puzzle != null) {
            puzzleExpanded = !resolved.puzzle.solved
        }
        comments = if (fetchedComments.isNotEmpty()) fetchedComments else detail?.comments.orEmpty()
        loading = false
    }

    LaunchedEffect(privacyConfig.blurAllEnabled) {
        if (privacyConfig.blurAllEnabled) {
            detailShowOriginal = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                detailRevealed = false
                detailShowOriginal = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showStickerSheet, showBoostSheet, showCommentStickers) {
        if ((showStickerSheet || showBoostSheet || showCommentStickers) && stickerPacks.isEmpty()) {
            val loaded = api.fetchStickerPacks().filter { !it.hidden && it.items.isNotEmpty() }
            stickerPacks = loaded
            if (activeStickerTab == null) activeStickerTab = loaded.firstOrNull()?.tabKey
        }
    }

    LaunchedEffect(commentEditing) {
        if (commentEditing) {
            delay(60L)
            runCatching { commentFocusRequester.requestFocus() }
        }
    }

    val commentImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                uploadingCommentImg = true
                val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                if (bytes != null) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val name = "cmt_${System.currentTimeMillis()}.${if (mime.contains("png")) "png" else "jpg"}"
                    api.uploadImageBytes(bytes, name, mime).fold(
                        onSuccess = { uploaded ->
                            val imgUrl = MediaUrlRewriter.rewriteMediaUrl(uploaded.url.ifBlank { "/img/${uploaded.key}" })
                            commentDraft = if (commentDraft.isBlank()) imgUrl else "${commentDraft.trimEnd()} $imgUrl"
                        },
                        onFailure = { statusBanner = it.message ?: "配图上传失败" },
                    )
                }
                uploadingCommentImg = false
            }
        }
    }

    fun performLike() {
        val m = item ?: return
        if (me == null) {
            onRequireLogin()
            return
        }
        scope.launch {
            api.toggleLike(m.id)
            val updated = m.copy(
                liked = !m.liked,
                likes = if (m.liked) maxOf(0, m.likes - 1) else m.likes + 1,
                likeCount = if (m.liked) maxOf(0, m.likeCount - 1) else m.likeCount + 1,
            )
            item = updated
            onStateUpdated(updated)
        }
    }

    val topBarSolid by remember {
        derivedStateOf {
            item?.mediaItems.isNullOrEmpty() ||
                listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 140
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = topBarSolid && !palette.isDark)
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(280)) + scaleIn(initialScale = 0.94f, animationSpec = tween(280)),
            exit = fadeOut(tween(180)),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.bg),
            ) {
                val maxMediaHeight = maxHeight * 0.70f

                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                } else {
                    val m = item
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 18.dp),
                        ) {
                            if (m != null) {
                                val mediaItems = m.mediaItems
                                val isStageBlurred = mediaItems.isNotEmpty() &&
                                    (privacyConfig.blurAllEnabled || m.isRisky) &&
                                    !detailRevealed

                                if (mediaItems.isNotEmpty()) {
                                    item(key = "media_stage") {
                                        DetailMediaStage(
                                            meme = m,
                                            mediaItems = mediaItems,
                                            api = api,
                                            maxStageHeight = maxMediaHeight,
                                            isBlurred = isStageBlurred,
                                            showOriginal = detailShowOriginal,
                                            blurAllActive = privacyConfig.blurAllEnabled,
                                            onRevealBlur = { detailRevealed = true },
                                            onToggleOriginal = {
                                                if (isStageBlurred) {
                                                    statusBanner = "请先点击图片揭开模糊，再查看原图"
                                                } else {
                                                    detailShowOriginal = !detailShowOriginal
                                                }
                                            },
                                            onResetOriginal = { detailShowOriginal = false },
                                            onOpenImmersive = { idx ->
                                                if (isStageBlurred) {
                                                    detailRevealed = true
                                                } else {
                                                    immersiveStartIndex = idx
                                                }
                                            },
                                        )
                                    }
                                } else {
                                    item(key = "text_top_spacer") {
                                        Spacer(
                                            modifier = Modifier
                                                .statusBarsPadding()
                                                .height(ForHappyDimens.topBarHeight + 8.dp),
                                        )
                                    }
                                }

                                // Title, clickable author, tags & rich body
                                item(key = "meta_and_body") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        if (!m.title.isNullOrBlank()) {
                                            Text(
                                                text = m.title,
                                                color = palette.text,
                                                fontSize = 18.sp,
                                                lineHeight = 25.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val uname = m.authorUsername?.takeIf { it.isNotBlank() }
                                                    if (uname != null) {
                                                        onAuthorClick(uname)
                                                        onDismiss()
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            ForHappyAvatar(
                                                username = m.authorDisplay,
                                                avatarUrl = m.authorAvatar,
                                                imageLoader = api.imageLoader,
                                                size = 32.dp,
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = m.authorDisplay,
                                                    color = palette.text,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                val stamp = formatRelativeTimeSec(m.createdAt)
                                                Text(
                                                    text = if (stamp.isNotBlank()) "发布于 $stamp" else "No.${m.id}",
                                                    color = palette.muted,
                                                    fontSize = 11.sp,
                                                )
                                            }
                                        }

                                        if (m.tags.isNotEmpty()) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp),
                                            ) {
                                                items(m.tags, key = { "dtag_$it" }) { tag ->
                                                    BadgePill(
                                                        text = "#$tag",
                                                        containerColor = palette.surface2,
                                                        contentColor = palette.primary,
                                                        modifier = Modifier.clickable {
                                                            onTagClick(tag)
                                                            onDismiss()
                                                        },
                                                    )
                                                }
                                            }
                                        }

                                        val bodyText = m.content?.takeIf { it.isNotBlank() }
                                            ?: m.text?.takeIf { it.isNotBlank() && it != m.title }
                                            ?: m.caption?.takeIf { it.isNotBlank() && it != m.title }
                                            ?: m.body?.takeIf { it.isNotBlank() && it != m.title }
                                        if (bodyText != null) {
                                            RichChatBodyView(
                                                rawBody = bodyText,
                                                imageLoader = api.imageLoader,
                                                textColor = palette.text,
                                                onHashtagClick = { tag ->
                                                    onTagClick(tag)
                                                    onDismiss()
                                                },
                                                onInlineImageClick = onOpenImageFullscreen,
                                                onLinkClick = onOpenUrl,
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = if (palette.isModernUi) palette.glassBorder else palette.line,
                                        thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                                    )
                                }

                                // Strict 4-layer hierarchy: Tier 3 Quick Replies ABOVE Tier 2 Stickers (`02b §1.1`)
                                item(key = "reactions_layers") {
                                    val quickReplies = remember(m.replyList, m.replies, m.myReply, me?.username) {
                                        m.effectiveQuickReplies(me)
                                    }
                                    val totalQuickCount = maxOf(m.replyCount, quickReplies.size)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        // ③ 一句话助推 (more prominent book-quote pills in primaryContainer)
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "一句话 ($totalQuickCount)",
                                                    color = palette.text,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                if (totalQuickCount > 0) {
                                                    Text(
                                                        text = "查看全部 ›",
                                                        color = palette.primary,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.clickable {
                                                            scope.launch {
                                                                whoReactedData = api.fetchMemeWhoReacted(m.id)
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                quickReplies.forEach { qr ->
                                                    Surface(
                                                        color = if (qr.mine) palette.primaryContainer else palette.primary.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(999.dp),
                                                        modifier = Modifier
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (qr.mine) palette.primary else palette.primary.copy(alpha = 0.25f),
                                                                shape = RoundedCornerShape(999.dp),
                                                            )
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (me == null) {
                                                                        onRequireLogin()
                                                                    } else if (!qr.mine) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                        scope.launch {
                                                                            api.putMemeQuickReply(m.id, qr.text).getOrNull()?.let { updated ->
                                                                                item = updated
                                                                                onStateUpdated(updated)
                                                                            }
                                                                        }
                                                                    } else {
                                                                        scope.launch {
                                                                            whoReactedData = api.fetchMemeWhoReacted(m.id)
                                                                        }
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    if (qr.mine && me != null) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        scope.launch {
                                                                            api.deleteMemeQuickReply(m.id).getOrNull()?.let { updated ->
                                                                                item = updated
                                                                                onStateUpdated(updated)
                                                                                statusBanner = "已撤回一句回应"
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                            ),
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        ) {
                                                            Text("「", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            BoostPillText(text = qr.text, imageLoader = api.imageLoader, fontSize = 12.sp)
                                                            Text("」", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            if (qr.n > 1) {
                                                                Text(
                                                                    text = "×${qr.n}",
                                                                    color = palette.primary,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                                                    modifier = Modifier
                                                                        .padding(start = 2.dp)
                                                                        .clickable {
                                                                            scope.launch {
                                                                                whoReactedData = api.fetchMemeWhoReacted(m.id)
                                                                            }
                                                                        },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    color = palette.surface2,
                                                    shape = RoundedCornerShape(999.dp),
                                                    modifier = Modifier
                                                        .then(
                                                            if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                                                Modifier
                                                            } else {
                                                                Modifier.border(
                                                                    1.dp,
                                                                    if (palette.isModernUi) palette.cardBorder else palette.line,
                                                                    RoundedCornerShape(999.dp),
                                                                )
                                                            },
                                                        )
                                                        .clickable {
                                                            if (me == null) onRequireLogin() else showBoostSheet = true
                                                        },
                                                ) {
                                                    Text(
                                                        text = "＋ 助推",
                                                        color = palette.primary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    )
                                                }
                                            }
                                        }

                                        // ② 贴纸回应
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("贴纸回应", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            val usedReactions = m.reactions.filter { it.count > 0 }
                                                .sortedWith(compareByDescending<com.jinhuan.forhappy.core.model.ChatReaction> { it.count }.thenByDescending { it.mine })
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                usedReactions.forEach { rx ->
                                                    ReactionChip(
                                                        emojiKey = rx.emoji,
                                                        count = rx.count,
                                                        mine = rx.mine,
                                                        imageLoader = api.imageLoader,
                                                        onClick = {
                                                            if (me == null) {
                                                                onRequireLogin()
                                                            } else {
                                                                scope.launch {
                                                                    api.reactToMeme(m.id, rx.emoji)?.let { updated ->
                                                                        item = updated
                                                                        onStateUpdated(updated)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    )
                                                }
                                                Surface(
                                                    color = palette.surface2,
                                                    shape = RoundedCornerShape(999.dp),
                                                    modifier = Modifier
                                                        .then(
                                                            if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                                                Modifier
                                                            } else {
                                                                Modifier.border(
                                                                    1.dp,
                                                                    if (palette.isModernUi) palette.cardBorder else palette.line,
                                                                    RoundedCornerShape(999.dp),
                                                                )
                                                            },
                                                        )
                                                        .clickable {
                                                            if (me == null) onRequireLogin() else showStickerSheet = true
                                                        },
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    ) {
                                                        Icon(Icons.Rounded.AddReaction, contentDescription = "添加贴纸", tint = palette.muted, modifier = Modifier.size(16.dp))
                                                        Text("＋", color = palette.muted, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(
                                        color = if (palette.isModernUi) palette.glassBorder else palette.line,
                                        thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                                    )
                                }

                                // Embedded Multi-Layer Puzzle Card (`02b §3`)
                                m.puzzle?.let { pz ->
                                    item(key = "puzzle_card") {
                                        EmbeddedPuzzleCard(
                                            puzzle = pz,
                                            api = api,
                                            me = me,
                                            expanded = puzzleExpanded,
                                            onToggleExpand = { puzzleExpanded = !puzzleExpanded },
                                            onRequireLogin = onRequireLogin,
                                            onOpenImage = onOpenImageFullscreen,
                                            onInputFocusChanged = { puzzleInputFocused = it },
                                            onSolvedRefresh = {
                                                scope.launch {
                                                    api.fetchMemeDetail(m.id)?.resolvedMeme?.let { fresh ->
                                                        item = fresh
                                                        onStateUpdated(fresh)
                                                    }
                                                }
                                            },
                                            onCopyReward = { rewardText ->
                                                clipboard.setText(AnnotatedString(rewardText))
                                                statusBanner = "已复制奖励内容"
                                            },
                                        )
                                        HorizontalDivider(
                                        color = if (palette.isModernUi) palette.glassBorder else palette.line,
                                        thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                                    )
                                    }
                                }

                                // Comments Header (`最热` / `最新`)
                                item(key = "comments_header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "评论 (${comments.size})",
                                            color = palette.text,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            WallSheetPill(label = "最热", selected = commentSort == "hot") {
                                                commentSort = "hot"
                                            }
                                            WallSheetPill(label = "最新", selected = commentSort == "latest") {
                                                commentSort = "latest"
                                            }
                                        }
                                    }
                                }

                                if (comments.isEmpty()) {
                                    item(key = "comments_empty") {
                                        Text(
                                            text = "还没有评论，点击下方「说点什么…」抢个沙发",
                                            color = palette.muted,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        )
                                    }
                                } else {
                                    items(commentThreads, key = { it.root.id }) { thread ->
                                        DetailCommentThreadBlock(
                                            thread = thread,
                                            api = api,
                                            expanded = expandedThreads.contains(thread.root.id),
                                            onExpand = { expandedThreads = expandedThreads + thread.root.id },
                                            onReply = { target ->
                                                if (me == null) {
                                                    onRequireLogin()
                                                } else {
                                                    commentReplyTarget = target
                                                    commentEditing = true
                                                }
                                            },
                                            onLongPress = { target -> commentActionTarget = target },
                                            onOpenImage = onOpenImageFullscreen,
                                            onTagClick = { tag ->
                                                onTagClick(tag)
                                                onDismiss()
                                            },
                                            onOpenUrl = onOpenUrl,
                                        )
                                    }
                                }
                            }
                        }

                        // Fixed bottom thumb bar or Comment Editor (`02b §1.1 & §1.3`)
                        if (m != null && !puzzleInputFocused) {
                            Surface(
                                color = palette.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding(),
                            ) {
                                Column {
                                    HorizontalDivider(
                                        color = if (palette.isModernUi) palette.glassBorder else palette.line,
                                        thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                                    )
                                    if (!commentEditing) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Surface(
                                                color = palette.surface2,
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .clickable {
                                                        if (me == null) {
                                                            onRequireLogin()
                                                        } else {
                                                            commentEditing = true
                                                        }
                                                    },
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 14.dp),
                                                    contentAlignment = Alignment.CenterStart,
                                                ) {
                                                    Text("说点什么…", color = palette.muted, fontSize = 13.sp)
                                                }
                                            }

                                            // ① Like button (tap = toggle, long press = station + L-site composition)
                                            Surface(
                                                color = palette.surface,
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier.combinedClickable(
                                                    onClick = { performLike() },
                                                    onLongClick = {
                                                        val localLikes = if (m.likeCount > 0) m.likeCount else maxOf(0, m.likes - m.ldLikes)
                                                        statusBanner = "赞组成：站内 $localLikes · L 站 ${m.ldLikes}"
                                                    },
                                                ),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = if (m.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                        contentDescription = "赞",
                                                        tint = if (m.liked) palette.like else palette.text,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        text = "${m.totalLikes}",
                                                        color = if (m.liked) palette.like else palette.text,
                                                        fontSize = 12.sp,
                                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { if (me == null) onRequireLogin() else showStickerSheet = true },
                                                modifier = Modifier.size(36.dp),
                                            ) {
                                                Icon(Icons.Rounded.EmojiEmotions, contentDescription = "贴纸回应", tint = palette.text, modifier = Modifier.size(20.dp))
                                            }

                                            Surface(
                                                color = palette.surface2,
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clickable { if (me == null) onRequireLogin() else showBoostSheet = true },
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 9.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text("「」", color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            IconButton(
                                                onClick = { showMoreSheet = true },
                                                modifier = Modifier.size(36.dp),
                                            ) {
                                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多操作", tint = palette.text, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    } else {
                                        // Expanded Comment Editor `[📷] [☺] [@] 输入框 [发送]`
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            commentReplyTarget?.let { target ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(palette.surface2)
                                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        text = "回复 @${target.displayAuthor}：${target.body}",
                                                        color = palette.muted,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "取消回复",
                                                        tint = palette.muted,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clickable { commentReplyTarget = null },
                                                    )
                                                }
                                            }

                                            if (showMentionSuggestions) {
                                                val candidates = remember(comments, m) {
                                                    (listOfNotNull(m.authorUsername) + comments.map { it.username })
                                                        .filter { it.isNotBlank() }
                                                        .distinct()
                                                        .take(10)
                                                }
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    items(candidates + listOf("欢欢")) { uname ->
                                                        WallSheetPill(label = "@$uname", selected = false) {
                                                            commentDraft += "@$uname "
                                                            showMentionSuggestions = false
                                                        }
                                                    }
                                                }
                                            }

                                            if (showCommentStickers && stickerPacks.isNotEmpty()) {
                                                StickerGridPicker(
                                                    packs = stickerPacks,
                                                    activeTabKey = activeStickerTab,
                                                    api = api,
                                                    onSelectTab = { activeStickerTab = it },
                                                    onSelectSticker = { code, _, _ ->
                                                        commentDraft += "[$code]"
                                                    },
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                IconButton(
                                                    enabled = !uploadingCommentImg,
                                                    onClick = {
                                                        StealthExemptionGate.markInternalJump()
                                                        commentImagePicker.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                                        )
                                                    },
                                                    modifier = Modifier.size(34.dp),
                                                ) {
                                                    if (uploadingCommentImg) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    } else {
                                                        Icon(Icons.Rounded.Image, contentDescription = "配图", tint = palette.muted, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { showCommentStickers = !showCommentStickers },
                                                    modifier = Modifier.size(34.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.EmojiEmotions,
                                                        contentDescription = "表情",
                                                        tint = if (showCommentStickers) palette.primary else palette.muted,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showMentionSuggestions = !showMentionSuggestions },
                                                    modifier = Modifier.size(34.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.AlternateEmail,
                                                        contentDescription = "@提及",
                                                        tint = if (showMentionSuggestions) palette.primary else palette.muted,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }

                                                OutlinedTextField(
                                                    value = commentDraft,
                                                    onValueChange = { commentDraft = it },
                                                    placeholder = { Text("写评论…", fontSize = 13.sp) },
                                                    singleLine = false,
                                                    maxLines = 3,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .focusRequester(commentFocusRequester),
                                                )

                                                Button(
                                                    enabled = commentDraft.isNotBlank() && !postingComment,
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = palette.primary,
                                                        contentColor = palette.onPrimary,
                                                    ),
                                                    onClick = {
                                                        val txt = commentDraft.trim()
                                                        val rtId = commentReplyTarget?.id
                                                        postingComment = true
                                                        scope.launch {
                                                            api.postMemeCommentFull(memeId, txt, rtId).fold(
                                                                onSuccess = {
                                                                    commentDraft = ""
                                                                    commentReplyTarget = null
                                                                    showCommentStickers = false
                                                                    showMentionSuggestions = false
                                                                    commentEditing = false
                                                                    comments = api.fetchMemeComments(memeId)
                                                                },
                                                                onFailure = { statusBanner = it.message ?: "评论发送失败" },
                                                            )
                                                            postingComment = false
                                                        }
                                                    },
                                                ) {
                                                    Text("发送", fontSize = 12.sp)
                                                }
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                Text(
                                                    text = "收起编辑器",
                                                    color = palette.muted,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier
                                                        .clickable {
                                                            commentEditing = false
                                                            showCommentStickers = false
                                                            showMentionSuggestions = false
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating translucent -> solid TopBar (`‹  No.8812  ⋯`)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            if (topBarSolid) palette.surface
                            else Color.Black.copy(alpha = 0.28f),
                        )
                        .statusBarsPadding()
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val fg = if (topBarSolid) palette.text else Color.White
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = fg)
                        }
                        Text(
                            text = "No.$memeId",
                            color = fg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                        )
                    }
                    if (item != null) {
                        IconButton(onClick = { showMoreSheet = true }) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多操作", tint = fg)
                        }
                    }
                }

                statusBanner?.let { msg ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 56.dp, start = 20.dp, end = 20.dp),
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }

    val m = item
    if (showMoreSheet && m != null) {
        MemeQuickActionsSheet(
            meme = m,
            api = api,
            me = me,
            blurThumbnail = (privacyConfig.blurAllEnabled || m.isRisky) && !detailRevealed,
            showQuickInteractions = false,
            onRequireLogin = onRequireLogin,
            onMemeUpdated = { updated ->
                item = updated
                onStateUpdated(updated)
            },
            onDeleted = { deletedId ->
                onDeleted(deletedId)
                onDismiss()
            },
            onAnchorsUpdated = onAnchorsUpdated,
            onOpenUrl = onOpenUrl,
            onStatusToast = { statusBanner = it },
            onOpenEditWorkbench = onOpenEditWorkbench,
            onDismiss = { showMoreSheet = false },
        )
    }

    if (showStickerSheet && m != null) {
        ModalBottomSheet(
            onDismissRequest = { showStickerSheet = false },
            containerColor = palette.bg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("贴纸回应", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PICKED_REACTIONS.forEach { key ->
                        val ex = m.reactions.firstOrNull { it.emoji == key }
                        ReactionChip(
                            emojiKey = key,
                            count = ex?.count ?: 0,
                            mine = ex?.mine == true,
                            imageLoader = api.imageLoader,
                            onClick = {
                                scope.launch {
                                    api.reactToMeme(m.id, key)?.let { updated ->
                                        item = updated
                                        onStateUpdated(updated)
                                    }
                                    showStickerSheet = false
                                }
                            },
                        )
                    }
                }
                if (stickerPacks.isNotEmpty()) {
                    StickerGridPicker(
                        packs = stickerPacks,
                        activeTabKey = activeStickerTab,
                        api = api,
                        onSelectTab = { activeStickerTab = it },
                        onSelectSticker = { emoKey, _, _ ->
                            scope.launch {
                                api.reactToMeme(m.id, emoKey)?.let { updated ->
                                    item = updated
                                    onStateUpdated(updated)
                                }
                                showStickerSheet = false
                            }
                        },
                    )
                }
            }
        }
    }

    if (showBoostSheet && m != null) {
        QuickBoostComposerSheet(
            meme = m,
            api = api,
            stickerPacks = stickerPacks,
            activeStickerTab = activeStickerTab,
            onSelectStickerTab = { activeStickerTab = it },
            onMemeUpdated = { updated ->
                item = updated
                onStateUpdated(updated)
            },
            onStatus = { statusBanner = it },
            onDismiss = { showBoostSheet = false },
        )
    }

    whoReactedData?.let { who ->
        ModalBottomSheet(
            onDismissRequest = { whoReactedData = null },
            containerColor = palette.bg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("助推与回应名单", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(who.replies) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ForHappyAvatar(entry.displayName, entry.avatar, api.imageLoader, size = 28.dp)
                            Text(entry.displayName, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            entry.text?.let { BoostPillText(text = it, imageLoader = api.imageLoader, fontSize = 12.sp) }
                        }
                    }
                    items(who.resolvedReactions) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ForHappyAvatar(entry.displayName, entry.avatar, api.imageLoader, size = 28.dp)
                            Text(entry.displayName, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            entry.emoji?.let { Text(it, fontSize = 13.sp, color = palette.muted) }
                        }
                    }
                }
            }
        }
    }

    commentActionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { commentActionTarget = null },
            containerColor = palette.surface,
            title = { Text("评论操作", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text(target.body, color = palette.muted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(target.body))
                            statusBanner = "已复制评论"
                            commentActionTarget = null
                        },
                    ) { Text("复制") }
                    TextButton(
                        onClick = {
                            commentActionTarget = null
                            if (me == null) {
                                onRequireLogin()
                            } else {
                                commentReplyTarget = target
                                commentEditing = true
                            }
                        },
                    ) { Text("回复") }
                    if (target.canDelete) {
                        TextButton(
                            onClick = {
                                val cid = target.id
                                commentActionTarget = null
                                scope.launch {
                                    if (api.deleteComment(cid)) {
                                        comments = comments.filterNot { it.id == cid }
                                        statusBanner = "已删除评论"
                                    }
                                }
                            },
                        ) { Text("删除", color = palette.danger) }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { commentActionTarget = null }) { Text("关闭") }
            },
        )
    }

    val startIdx = immersiveStartIndex
    if (startIdx != null && m != null) {
        ImmersiveMediaStage(
            mediaItems = m.mediaItems,
            initialIndex = startIdx,
            api = api,
            meme = m,
            forceBlurAll = privacyConfig.blurAllEnabled,
            onLikeToggle = { performLike() },
            onOpenReactions = {
                immersiveStartIndex = null
                if (me == null) onRequireLogin() else showStickerSheet = true
            },
            onOpenQuickReplies = {
                immersiveStartIndex = null
                if (me == null) onRequireLogin() else showBoostSheet = true
            },
            onOpenComments = {
                immersiveStartIndex = null
                scope.launch {
                    val idx = if (m.mediaItems.isNotEmpty()) 2 else 1
                    listState.animateScrollToItem(idx)
                }
            },
            onDismiss = {
                detailShowOriginal = false
                immersiveStartIndex = null
            },
        )
    }
}

@Composable
private fun DetailMediaStage(
    meme: MemeItem,
    mediaItems: List<MemeMediaItem>,
    api: ForHappyApiClient,
    maxStageHeight: androidx.compose.ui.unit.Dp,
    isBlurred: Boolean,
    showOriginal: Boolean,
    blurAllActive: Boolean,
    onRevealBlur: () -> Unit,
    onToggleOriginal: () -> Unit,
    onResetOriginal: () -> Unit,
    onOpenImmersive: (Int) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    LaunchedEffect(pagerState.currentPage) {
        onResetOriginal()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = maxStageHeight)
            .background(Color.Black.copy(alpha = 0.06f)),
    ) {
        if (meme.layout == "grid" && mediaItems.size > 1) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxStageHeight)
                    .padding(top = ForHappyDimens.topBarHeight + 18.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(mediaItems.size) { idx ->
                    val item = mediaItems[idx]
                    val url = MediaUrlRewriter.rewriteMediaUrl(item.resolvedThumbOrUrl)
                    AsyncImage(
                        model = url,
                        imageLoader = api.imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .let { if (isBlurred) it.blur(26.dp) else it }
                            .clickable { onOpenImmersive(idx) },
                    )
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val item = mediaItems[page]
                val isCurrent = page == pagerState.currentPage
                val maskedUrl = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.resolvedFullUrl) }
                val origUrl = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.o) }
                val activeUrl = if (isCurrent && showOriginal && origUrl.isNotBlank()) origUrl else maskedUrl
                val w = item.w ?: meme.effectiveWidth ?: 0
                val h = item.h ?: meme.effectiveHeight ?: 0
                val isLong = w > 0 && h.toFloat() / w.toFloat() >= 2.2f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = maxStageHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        item.isVideo && !item.v.isNullOrBlank() -> {
                            NativeVideoPlayer(
                                videoUrl = MediaUrlRewriter.rewriteMediaUrl(item.v),
                                posterUrl = activeUrl,
                                api = api,
                                inlineMode = true,
                                isBlurred = isBlurred,
                                onExpandFullscreen = { onOpenImmersive(page) },
                            )
                        }
                        item.isLive && !item.v.isNullOrBlank() -> {
                            LivePhotoStageBox(
                                imageUrl = activeUrl,
                                videoUrl = MediaUrlRewriter.rewriteMediaUrl(item.v),
                                api = api,
                                isBlurred = isBlurred,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxStageHeight)
                                    .let { if (isBlurred) it.blur(28.dp) else it },
                                onTap = { onOpenImmersive(page) },
                            )
                        }
                        else -> {
                            val ratio = if (w > 0 && h > 0 && !isLong) (w.toFloat() / h.toFloat()).coerceIn(0.45f, 2.2f) else null
                            AsyncImage(
                                model = activeUrl,
                                imageLoader = api.imageLoader,
                                contentDescription = meme.safeLabel,
                                contentScale = if (isLong) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let { mod ->
                                        when {
                                            isLong -> mod.height(420.dp)
                                            ratio != null -> mod.aspectRatio(ratio)
                                            else -> mod.heightIn(min = 240.dp, max = maxStageHeight)
                                        }
                                    }
                                    .let { if (isBlurred) it.blur(28.dp) else it }
                                    .clickable { onOpenImmersive(page) },
                            )
                            if (isLong && !isBlurred) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.68f),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable { onOpenImmersive(page) },
                                    ) {
                                        Text(
                                            text = "展开长图 ▤",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isBlurred) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(palette.blurOverlay)
                    .clickable { onRevealBlur() },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.66f),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (blurAllActive) "全图模糊中 · 点按查看" else "敏感内容 · 点按查看",
                            color = Color.White,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // Bottom-left Masked <-> Original switch & bottom-right carousel indicator
        val currentMedia = mediaItems.getOrElse(pagerState.currentPage) { mediaItems.first() }
        if (!isBlurred && currentMedia.hasOriginalUnmasked) {
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .border(
                        width = if (showOriginal) 1.5.dp else 1.dp,
                        color = if (showOriginal) palette.danger else Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable { onToggleOriginal() },
            ) {
                Text(
                    text = if (showOriginal) "原图 ⇄ 打码版" else "打码版 ⇄ 原图",
                    color = if (showOriginal) Color(0xFFFCA5A5) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        if (mediaItems.size > 1 && meme.layout != "grid") {
            Surface(
                color = Color.Black.copy(alpha = 0.56f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                    color = Color.White,
                    fontSize = 11.sp,
                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmbeddedPuzzleCard(
    puzzle: PuzzleSpec,
    api: ForHappyApiClient,
    me: MeUser?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenImage: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    onSolvedRefresh: () -> Unit,
    onCopyReward: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var answerDraft by remember(puzzle.id, puzzle.layer) { mutableStateOf("") }
    var showHint by remember(puzzle.id) { mutableStateOf(false) }
    var feedback by remember(puzzle.id, puzzle.layer) { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerWrongFeedback(msg: String) {
        feedback = msg
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        for (x in listOf(-10f, 10f, -8f, 8f, -4f, 0f)) {
            shakeOffset.animateTo(x, animationSpec = tween(35))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (puzzle.solved) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (puzzle.solved) palette.success else palette.gold,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (puzzle.solved) {
                        "已通关 · 解密挑战 (${puzzle.layerCount} 层)"
                    } else {
                        "解密挑战 · 第 ${(puzzle.layer + 1).coerceAtMost(puzzle.layerCount)} / ${puzzle.layerCount} 层"
                    },
                    color = palette.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = palette.muted,
            )
        }

        if (expanded) {
            if (puzzle.layerCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(puzzle.layerCount) { idx ->
                        val lit = puzzle.solved || idx <= puzzle.layer
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (lit) palette.primary else palette.line),
                        )
                        if (idx < puzzle.layerCount - 1) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(if (idx < puzzle.layer || puzzle.solved) palette.primary else palette.line),
                            )
                        }
                    }
                }
            }

            puzzle.current?.let { layer ->
                layer.prompt?.takeIf { it.isNotBlank() }?.let { prompt ->
                    Text(prompt, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                val ld = layer.data
                if (layer.kind == "cipher" && !ld?.text.isNullOrBlank()) {
                    Surface(color = palette.surface2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = ld!!.text!!,
                            color = palette.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
                if (!ld?.img.isNullOrBlank()) {
                    val imgUrl = MediaUrlRewriter.rewriteMediaUrl(ld!!.img)
                    AsyncImage(
                        model = imgUrl,
                        imageLoader = api.imageLoader,
                        contentDescription = "线索图片",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenImage(imgUrl) },
                    )
                }
                if (layer.kind == "quiz" && ld?.options?.isNotEmpty() == true && puzzle.status == "open" && !puzzle.solved) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ld.options.forEachIndexed { idx, opt ->
                            OutlinedButton(
                                enabled = !submitting,
                                onClick = {
                                    if (me == null) {
                                        onRequireLogin()
                                    } else {
                                        submitting = true
                                        scope.launch {
                                            val res = api.solvePuzzle(puzzle.id, opt)
                                            val ok = res["correct"]?.jsonPrimitive?.booleanOrNull == true
                                            if (ok) {
                                                feedback = "这一层过了！"
                                                onSolvedRefresh()
                                            } else {
                                                triggerWrongFeedback(res["error"]?.jsonPrimitive?.content ?: "不对，再想想")
                                            }
                                            submitting = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${'A' + idx}. $opt", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            puzzle.hint?.takeIf { it.isNotBlank() }?.let { h ->
                Text(
                    text = if (showHint) "💡 提示：$h" else "💡 点按揭开提示",
                    color = palette.gold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { showHint = true },
                )
            }

            puzzle.reward?.takeIf { it.isNotBlank() }?.let { rw ->
                Surface(color = palette.primaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🎁 奖励：$rw", color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onCopyReward(rw) }) { Text("复制") }
                    }
                }
            }

            if (puzzle.status == "open" && !puzzle.solved && puzzle.current?.kind != "quiz") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = answerDraft,
                        onValueChange = { answerDraft = it },
                        placeholder = { Text("输入谜底", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { onInputFocusChanged(it.isFocused) },
                    )
                    Button(
                        enabled = !submitting && answerDraft.isNotBlank(),
                        onClick = {
                            if (me == null) {
                                onRequireLogin()
                            } else {
                                submitting = true
                                scope.launch {
                                    val res = api.solvePuzzle(puzzle.id, answerDraft.trim())
                                    val ok = res["correct"]?.jsonPrimitive?.booleanOrNull == true
                                    if (ok) {
                                        answerDraft = ""
                                        feedback = "解锁成功！"
                                        onSolvedRefresh()
                                    } else {
                                        triggerWrongFeedback(res["error"]?.jsonPrimitive?.content ?: "不对，再想想")
                                    }
                                    submitting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    ) {
                        Text("解锁")
                    }
                }
            }

            feedback?.let { msg ->
                Text(msg, color = palette.primary, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickBoostComposerSheet(
    meme: MemeItem,
    api: ForHappyApiClient,
    stickerPacks: List<StickerPack>,
    activeStickerTab: String?,
    onSelectStickerTab: (String) -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
    onStatus: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var draftFocused by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val units = remember(draft) { countReplyUnits(draft) }

    fun submitBoost(text: String) {
        if (busy) return
        busy = true
        scope.launch {
            api.putMemeQuickReply(meme.id, text.trim()).fold(
                onSuccess = { updated ->
                    if (updated != null) {
                        onMemeUpdated(updated)
                    } else {
                        api.fetchMemeDetail(meme.id)?.resolvedMeme?.let(onMemeUpdated)
                    }
                    onStatus("已助推「${text.trim()}」")
                    onDismiss()
                },
                onFailure = { onStatus(it.message ?: "发送失败") },
            )
            busy = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("一句话助推", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$units / $BOOST_MAX_LEN",
                    color = if (units > BOOST_MAX_LEN) palette.danger else palette.muted,
                    fontSize = 12.sp,
                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("自定义一句，点下方热梗直接发出", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { draftFocused = it.isFocused },
                )
                Button(
                    enabled = !busy && units in 1..BOOST_MAX_LEN,
                    onClick = { submitBoost(draft) },
                ) {
                    Text("发送")
                }
            }

            Text(
                text = if (draftFocused) "热梗（点击插入编辑框）" else "热梗（点击即发，无需二次确认）",
                color = palette.muted,
                fontSize = 12.sp,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BOOST_PRESETS.forEach { preset ->
                    val accent = remember(preset, palette.isDark) { boostPresetAccent(preset, palette.isDark) }
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(1.dp, palette.line, RoundedCornerShape(999.dp))
                            .clickable(enabled = !busy) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (draftFocused || draft.isNotBlank()) {
                                    draft += preset
                                } else {
                                    submitBoost(preset)
                                }
                            },
                    ) {
                        Text(
                            text = preset,
                            color = accent ?: palette.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            if (stickerPacks.isNotEmpty()) {
                StickerGridPicker(
                    packs = stickerPacks,
                    activeTabKey = activeStickerTab,
                    api = api,
                    onSelectTab = onSelectStickerTab,
                    onSelectSticker = { emoKey, _, _ ->
                        draft += "[$emoKey]"
                    },
                )
            }
        }
    }
}

@Composable
private fun StickerGridPicker(
    packs: List<StickerPack>,
    activeTabKey: String?,
    api: ForHappyApiClient,
    onSelectTab: (String) -> Unit,
    onSelectSticker: (fullCode: String, packKey: String, name: String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(packs, key = { it.tabKey }) { pack ->
                FilterChip(
                    selected = activeTabKey == pack.tabKey,
                    onClick = { onSelectTab(pack.tabKey) },
                    label = { Text(pack.label.removePrefix("L站·"), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary,
                        selectedLabelColor = palette.onPrimary,
                    ),
                )
            }
        }
        val selPack = packs.firstOrNull { it.tabKey == activeTabKey } ?: packs.firstOrNull()
        if (selPack != null) {
            Box(modifier = Modifier.fillMaxWidth().height(136.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(selPack.items.distinct().take(80), key = { "${selPack.tabKey}_$it" }) { stk ->
                        val fullCode = "${selPack.key}:$stk"
                        AsyncImage(
                            model = MediaUrlRewriter.stickerUrl(selPack.key, stk, selPack.ext),
                            imageLoader = api.imageLoader,
                            contentDescription = stk,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectSticker(fullCode, selPack.key, stk) }
                                .padding(2.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class DetailCommentThread(
    val root: MemeComment,
    val children: List<MemeComment>,
)

private fun buildDetailCommentThreads(
    comments: List<MemeComment>,
    sortMode: String,
): List<DetailCommentThread> {
    if (comments.isEmpty()) return emptyList()
    val sortedChrono = comments.sortedBy { it.id }
    val byId = sortedChrono.associateBy { it.id }
    val childrenByRoot = LinkedHashMap<Long, MutableList<MemeComment>>()
    val roots = ArrayList<MemeComment>()

    fun findRootId(cm: MemeComment): Long? {
        var cur = cm.replyTo?.id
        var guard = 0
        while (cur != null && cur > 0L && guard++ < 8) {
            val parent = byId[cur] ?: return null
            if (parent.replyTo?.id == null || parent.replyTo.id <= 0L || byId[parent.replyTo.id] == null) {
                return parent.id
            }
            cur = parent.replyTo.id
        }
        return null
    }

    for (cm in sortedChrono) {
        val rootId = findRootId(cm)
        if (rootId == null) {
            roots.add(cm)
            childrenByRoot.putIfAbsent(cm.id, ArrayList())
        } else {
            childrenByRoot.getOrPut(rootId) { ArrayList() }.add(cm)
        }
    }

    val threads = roots.map { r -> DetailCommentThread(root = r, children = childrenByRoot[r.id].orEmpty()) }
    return if (sortMode == "hot") {
        threads.sortedWith(compareByDescending<DetailCommentThread> { it.children.size }.thenByDescending { it.root.id })
    } else {
        threads.sortedByDescending { it.root.id }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailCommentThreadBlock(
    thread: DetailCommentThread,
    api: ForHappyApiClient,
    expanded: Boolean,
    onExpand: () -> Unit,
    onReply: (MemeComment) -> Unit,
    onLongPress: (MemeComment) -> Unit,
    onOpenImage: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DetailSingleCommentRow(
            cm = thread.root,
            api = api,
            isChild = false,
            onReply = onReply,
            onLongPress = onLongPress,
            onOpenImage = onOpenImage,
            onTagClick = onTagClick,
            onOpenUrl = onOpenUrl,
        )
        val shownChildren = if (expanded || thread.children.size <= 2) thread.children else thread.children.take(2)
        if (shownChildren.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 36.dp)
                    .border(1.dp, palette.line.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surface2.copy(alpha = 0.55f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shownChildren.forEach { child ->
                    DetailSingleCommentRow(
                        cm = child,
                        api = api,
                        isChild = true,
                        onReply = onReply,
                        onLongPress = onLongPress,
                        onOpenImage = onOpenImage,
                        onTagClick = onTagClick,
                        onOpenUrl = onOpenUrl,
                    )
                }
                if (!expanded && thread.children.size > 2) {
                    Text(
                        text = "展开其余 ${thread.children.size - 2} 条回复 ▾",
                        color = palette.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onExpand() }
                            .padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailSingleCommentRow(
    cm: MemeComment,
    api: ForHappyApiClient,
    isChild: Boolean,
    onReply: (MemeComment) -> Unit,
    onLongPress: (MemeComment) -> Unit,
    onOpenImage: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onReply(cm) },
                onLongClick = { onLongPress(cm) },
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ForHappyAvatar(
            username = cm.displayAuthor,
            avatarUrl = cm.avatar,
            imageLoader = api.imageLoader,
            size = if (isChild) 24.dp else 30.dp,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = cm.displayAuthor,
                    color = palette.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (cm.kind == "bot") {
                    BadgePill("AI", containerColor = palette.primaryContainer, contentColor = palette.primary)
                }
                val ts = formatRelativeTimeSec(cm.createdAt)
                if (ts.isNotBlank()) {
                    Text(ts, color = palette.muted, fontSize = 10.sp)
                }
            }
            cm.replyTo?.let { rep ->
                if (isChild) {
                    Text(
                        text = "回复 @${rep.resolvedAuthor}",
                        color = palette.muted,
                        fontSize = 11.sp,
                    )
                }
            }
            RichChatBodyView(
                rawBody = cm.body,
                imageLoader = api.imageLoader,
                textColor = palette.text,
                onHashtagClick = onTagClick,
                onInlineImageClick = onOpenImage,
                onLinkClick = onOpenUrl,
            )
        }
    }
}

private fun formatRelativeTimeSec(sec: Long?): String {
    if (sec == null || sec <= 0L) return ""
    val diff = (System.currentTimeMillis() / 1000L - sec).coerceAtLeast(0L)
    return when {
        diff < 60L -> "刚刚"
        diff < 3600L -> "${diff / 60} 分钟前"
        diff < 86400L -> "${diff / 3600} 小时前"
        diff < 86400L * 30 -> "${diff / 86400} 天前"
        else -> ""
    }
}

@Composable
fun ComposeMemeModal(
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var spoiler by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageName by remember { mutableStateOf("meme.jpg") }
    var selectedImageMime by remember { mutableStateOf("image/jpeg") }
    var statusText by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()?.let { bytes ->
                selectedImageBytes = bytes
                selectedImageMime = context.contentResolver.getType(uri) ?: "image/jpeg"
                selectedImageName = "upload_${System.currentTimeMillis()}.${if (selectedImageMime.contains("png")) "png" else "jpg"}"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = palette.bg,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.line, RoundedCornerShape(22.dp)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("投稿新梗图", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                    }
                }

                OutlinedButton(
                    onClick = {
                        StealthExemptionGate.markInternalJump()
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedImageBytes == null) "从相册选择图片"
                        else "已选 ${selectedImageBytes!!.size / 1024} KB ($selectedImageMime)",
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题 / 一句话配文（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("正文 / 出处背景") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tagsRaw,
                    onValueChange = { tagsRaw = it },
                    label = { Text("标签（空格或逗号分隔）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("标为慎点 / NSFW（默认模糊）", color = palette.muted, fontSize = 13.sp)
                    Switch(checked = spoiler, onCheckedChange = { spoiler = it })
                }

                if (!statusText.isNullOrBlank()) {
                    Text(statusText!!, color = palette.danger, fontSize = 12.sp)
                }

                Button(
                    enabled = !submitting && (selectedImageBytes != null || content.isNotBlank() || title.isNotBlank()),
                    onClick = {
                        submitting = true
                        statusText = null
                        scope.launch {
                            val uploadedKey = selectedImageBytes?.let { bytes ->
                                val up = api.uploadImageBytes(bytes, selectedImageName, selectedImageMime)
                                up.getOrElse {
                                    statusText = it.message ?: "图片上传失败"
                                    submitting = false
                                    return@launch
                                }.key
                            }
                            val tags = tagsRaw.split(',', ' ', '，', '#')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            val res = api.createMeme(
                                title = title.trim(),
                                content = content.trim(),
                                tags = tags,
                                spoiler = spoiler,
                                uploadedKey = uploadedKey,
                            )
                            res.fold(
                                onSuccess = {
                                    submitting = false
                                    onPublished()
                                    onDismiss()
                                },
                                onFailure = {
                                    statusText = it.message ?: "发布失败"
                                    submitting = false
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary,
                    ),
                ) {
                    Text(if (submitting) "正在上传并发布…" else "发布到图廊")
                }
            }
        }
    }
}
