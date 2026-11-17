package com.jinhuan.forhappy.ui.wall

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircleFilled
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.BOOST_MAX_LEN
import com.jinhuan.forhappy.core.model.BOOST_PRESETS
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeComment
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.PICKED_REACTIONS
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.countReplyUnits
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.ReactionEmojiView
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.components.boostPresetAccent
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
) {
    val palette = LocalForHappyPalette.current
    val seenTracker = LocalSeenTracker.current
    val scope = rememberCoroutineScope()
    var item by remember(memeId) { mutableStateOf<MemeItem?>(null) }
    var comments by remember(memeId) { mutableStateOf<List<MemeComment>>(emptyList()) }
    var loading by remember(memeId) { mutableStateOf(true) }
    var commentDraft by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var puzzleAnswer by remember(memeId) { mutableStateOf("") }
    var puzzleMsg by remember(memeId) { mutableStateOf<String?>(null) }

    // Reaction & 1-line quick reply state
    var showReactionPicker by remember(memeId) { mutableStateOf(false) }
    var showReplyStickers by remember(memeId) { mutableStateOf(false) }
    var stickerPacks by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var activeStickerTab by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember(memeId) { mutableStateOf("") }
    var replyBusy by remember(memeId) { mutableStateOf(false) }
    var replyStatus by remember(memeId) { mutableStateOf<String?>(null) }
    var localVideoPair by remember(memeId) { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(memeId) {
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
                memeDay = resolved?.day,
            )
        } else {
            runCatching { api.recordMemeViews(listOf(memeId)) }
        }
        item = resolved
        comments = if (fetchedComments.isNotEmpty()) fetchedComments else detail?.comments.orEmpty()
        loading = false
    }

    LaunchedEffect(showReactionPicker, showReplyStickers) {
        if ((showReactionPicker || showReplyStickers) && stickerPacks.isEmpty()) {
            val loaded = api.fetchStickerPacks().filter { !it.hidden && it.items.isNotEmpty() }
            stickerPacks = loaded
            if (activeStickerTab == null) activeStickerTab = loaded.firstOrNull()?.tabKey
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "梗图详情 No.$memeId",
                        color = palette.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                    }
                }

                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                } else {
                    val m = item
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                    ) {
                        if (m != null) {
                            item {
                                Spacer(Modifier.height(10.dp))

                                val urls = m.detailImageUrls
                                if (m.images.isNotEmpty()) {
                                    for (media in m.images) {
                                        val rawImg = media.u ?: media.t.orEmpty()
                                        val imgUrl = MediaUrlRewriter.rewriteMediaUrl(rawImg)
                                        val rawVid = media.v
                                        val isVideo = media.k == "v" || !rawVid.isNullOrBlank()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    if (isVideo && !rawVid.isNullOrBlank()) {
                                                        localVideoPair = imgUrl to MediaUrlRewriter.rewriteMediaUrl(rawVid)
                                                    } else if (imgUrl.isNotBlank()) {
                                                        onOpenImageFullscreen(imgUrl)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (imgUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = imgUrl,
                                                    imageLoader = api.imageLoader,
                                                    contentDescription = m.safeLabel,
                                                    contentScale = ContentScale.FillWidth,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                            if (isVideo) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    shape = CircleShape,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PlayCircleFilled,
                                                        contentDescription = "播放视频",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(48.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    for (rawUrl in urls) {
                                        val imgUrl = MediaUrlRewriter.rewriteMediaUrl(rawUrl)
                                        AsyncImage(
                                            model = imgUrl,
                                            imageLoader = api.imageLoader,
                                            contentDescription = m.safeLabel,
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onOpenImageFullscreen(imgUrl) },
                                        )
                                    }
                                }

                                if (!m.title.isNullOrBlank()) {
                                    Text(
                                        text = m.title,
                                        color = palette.text,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }

                                val bodyText = m.content?.takeIf { it.isNotBlank() }
                                    ?: m.text?.takeIf { it.isNotBlank() && it != m.title }
                                    ?: m.caption?.takeIf { it.isNotBlank() && it != m.title }
                                if (bodyText != null) {
                                    RichChatBodyView(
                                        rawBody = bodyText,
                                        imageLoader = api.imageLoader,
                                        textColor = palette.text,
                                        onHashtagClick = { tag ->
                                            onTagClick(tag)
                                            onDismiss()
                                        },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ForHappyAvatar(
                                        username = m.authorDisplay,
                                        avatarUrl = m.authorAvatar,
                                        imageLoader = api.imageLoader,
                                        size = 30.dp,
                                    )
                                    Text(
                                        text = m.authorDisplay,
                                        color = palette.muted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = {
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
                                    }) {
                                        Icon(
                                            imageVector = if (m.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "点赞",
                                            tint = if (m.liked) palette.danger else palette.muted,
                                        )
                                    }
                                    Text("${m.totalLikes}", color = palette.muted, fontSize = 13.sp)
                                    IconButton(onClick = {
                                        scope.launch {
                                            val wasFaved = m.isFaved
                                            if (api.toggleFav(m.id, wasFaved)) {
                                                val updated = m.copy(
                                                    faved = !wasFaved,
                                                    favFolders = if (wasFaved) emptyList() else listOf(1L),
                                                )
                                                item = updated
                                                onStateUpdated(updated)
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (m.isFaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                            contentDescription = "收藏",
                                            tint = if (m.isFaved) palette.gold else palette.muted,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
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
                                                        val updated = api.reactToMeme(m.id, rx.emoji)
                                                        if (updated != null) {
                                                            item = updated
                                                            onStateUpdated(updated)
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    Surface(
                                        color = if (showReactionPicker) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (showReactionPicker) palette.primary else palette.line,
                                                RoundedCornerShape(999.dp),
                                            )
                                            .clickable {
                                                if (me == null) onRequireLogin() else showReactionPicker = !showReactionPicker
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AddReaction,
                                                contentDescription = "表情回应",
                                                tint = palette.primary,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Text("表情回应", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                if (showReactionPicker) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = palette.surface,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text("精选表情回应（点击即表态/取消）", color = palette.muted, fontSize = 11.sp)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                PICKED_REACTIONS.forEach { key ->
                                                    val existing = m.reactions.firstOrNull { it.emoji == key }
                                                    ReactionChip(
                                                        emojiKey = key,
                                                        count = existing?.count ?: 0,
                                                        mine = existing?.mine == true,
                                                        imageLoader = api.imageLoader,
                                                        onClick = {
                                                            scope.launch {
                                                                val updated = api.reactToMeme(m.id, key)
                                                                if (updated != null) {
                                                                    item = updated
                                                                    onStateUpdated(updated)
                                                                }
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                            if (stickerPacks.isNotEmpty()) {
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    items(stickerPacks, key = { it.tabKey }) { pack ->
                                                        FilterChip(
                                                            selected = activeStickerTab == pack.tabKey,
                                                            onClick = { activeStickerTab = pack.tabKey },
                                                            label = { Text(pack.label.removePrefix("L站·"), fontSize = 11.sp) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor = palette.primary,
                                                                selectedLabelColor = palette.onPrimary,
                                                            ),
                                                        )
                                                    }
                                                }
                                                val selPack = stickerPacks.firstOrNull { it.tabKey == activeStickerTab }
                                                if (selPack != null) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                                                        LazyVerticalGrid(
                                                            columns = GridCells.Adaptive(44.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                                        ) {
                                                            items(selPack.items.distinct().take(80), key = { "${selPack.tabKey}_rx_$it" }) { stkName ->
                                                                val emoKey = "${selPack.key}:$stkName"
                                                                val mine = m.reactions.any { it.emoji == emoKey && it.mine }
                                                                Surface(
                                                                    color = if (mine) palette.primary.copy(alpha = 0.2f) else Color.Transparent,
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    modifier = Modifier
                                                                        .size(40.dp)
                                                                        .clickable {
                                                                            scope.launch {
                                                                                val updated = api.reactToMeme(m.id, emoKey)
                                                                                if (updated != null) {
                                                                                    item = updated
                                                                                    onStateUpdated(updated)
                                                                                }
                                                                                showReactionPicker = false
                                                                            }
                                                                        }
                                                                        .padding(3.dp),
                                                                ) {
                                                                    AsyncImage(
                                                                        model = MediaUrlRewriter.stickerUrl(selPack.key, stkName, selPack.ext),
                                                                        imageLoader = api.imageLoader,
                                                                        contentDescription = stkName,
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

                                if (m.tags.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        m.tags.take(8).forEach { tag ->
                                            BadgePill(
                                                text = "#$tag",
                                                modifier = Modifier.clickable {
                                                    onTagClick(tag)
                                                    onDismiss()
                                                },
                                            )
                                        }
                                    }
                                }

                                m.puzzle?.let { pz ->
                                    Spacer(Modifier.height(10.dp))
                                    Surface(
                                        color = palette.surface,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, palette.gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "附加解谜 · ${pz.layerCount} 层",
                                                    color = palette.gold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                )
                                                BadgePill(if (pz.solved) "已解开" else if (pz.status == "open") "进行中" else "已结束")
                                            }
                                            pz.hint?.takeIf { it.isNotBlank() }?.let {
                                                Text("提示：$it", color = palette.muted, fontSize = 12.sp)
                                            }
                                            pz.current?.prompt?.takeIf { it.isNotBlank() }?.let {
                                                Text(it, color = palette.text, fontSize = 13.sp)
                                            }
                                            pz.reward?.takeIf { it.isNotBlank() }?.let {
                                                Text("奖励内容：$it", color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            if (pz.status == "open" && !pz.solved && pz.reward == null) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = puzzleAnswer,
                                                        onValueChange = { puzzleAnswer = it },
                                                        placeholder = { Text("输入谜底", fontSize = 12.sp) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Button(onClick = {
                                                        scope.launch {
                                                            val res = api.solvePuzzle(pz.id, puzzleAnswer.trim())
                                                            val correct = res["correct"]?.jsonPrimitive?.booleanOrNull == true
                                                            puzzleMsg = if (correct) "解密成功！" else res["error"]?.jsonPrimitive?.content ?: "不对，再想想"
                                                            if (correct) {
                                                                item = api.fetchMemeDetail(m.id)?.resolvedMeme ?: item
                                                            }
                                                        }
                                                    }) {
                                                        Text("解密")
                                                    }
                                                }
                                            }
                                            puzzleMsg?.let { Text(it, color = palette.primary, fontSize = 12.sp) }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                val quickReplies = remember(m.replyList, m.myReply, me?.username) {
                                    m.effectiveQuickReplies(me)
                                }
                                val totalQuickCount = maxOf(m.replyCount, quickReplies.size)
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line.copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "一句回应 ($totalQuickCount)",
                                                color = palette.text,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "点他人 +1 同感 · 点自己撤回",
                                                color = palette.muted,
                                                fontSize = 11.sp,
                                            )
                                        }

                                        if (quickReplies.isEmpty()) {
                                            Text(
                                                text = "还没有一句回应，点下方 Boost 词或自己写一句（${BOOST_MAX_LEN}字内）",
                                                color = palette.muted,
                                                fontSize = 12.sp,
                                            )
                                        } else {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                quickReplies.forEach { qr ->
                                                    Surface(
                                                        color = if (qr.mine) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                                                        shape = RoundedCornerShape(999.dp),
                                                        modifier = Modifier
                                                            .border(
                                                                1.dp,
                                                                if (qr.mine) palette.primary.copy(alpha = 0.5f) else palette.line.copy(alpha = 0.65f),
                                                                RoundedCornerShape(999.dp),
                                                            )
                                                            .clickable(enabled = !replyBusy) {
                                                                if (me == null) {
                                                                    onRequireLogin()
                                                                } else {
                                                                    replyBusy = true
                                                                    replyStatus = null
                                                                    scope.launch {
                                                                        val res = if (qr.mine) {
                                                                            api.deleteMemeQuickReply(m.id)
                                                                        } else {
                                                                            api.putMemeQuickReply(m.id, qr.text)
                                                                        }
                                                                        res.fold(
                                                                            onSuccess = { updated ->
                                                                                val fresh = updated ?: api.fetchMemeDetail(m.id)?.resolvedMeme
                                                                                if (fresh != null) {
                                                                                    item = fresh
                                                                                    onStateUpdated(fresh)
                                                                                }
                                                                            },
                                                                            onFailure = { replyStatus = it.message },
                                                                        )
                                                                        replyBusy = false
                                                                    }
                                                                }
                                                            },
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        ) {
                                                            ForHappyAvatar(
                                                                username = qr.displayAuthor,
                                                                avatarUrl = qr.avatar,
                                                                imageLoader = api.imageLoader,
                                                                size = 18.dp,
                                                            )
                                                            BoostPillText(
                                                                text = qr.text,
                                                                imageLoader = api.imageLoader,
                                                                fontSize = 12.sp,
                                                            )
                                                            if (qr.n > 1) {
                                                                Text(
                                                                    text = "×${qr.n}",
                                                                    color = palette.muted,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(BOOST_PRESETS, key = { "qr_preset_$it" }) { preset ->
                                                val accent = remember(preset, palette.isDark) { boostPresetAccent(preset, palette.isDark) }
                                                val isMine = m.myReply == preset
                                                Surface(
                                                    color = if (isMine) palette.primary.copy(alpha = 0.18f) else palette.surface2,
                                                    shape = RoundedCornerShape(999.dp),
                                                    modifier = Modifier
                                                        .border(
                                                            1.dp,
                                                            if (isMine) palette.primary else palette.line.copy(alpha = 0.7f),
                                                            RoundedCornerShape(999.dp),
                                                        )
                                                        .clickable(enabled = !replyBusy) {
                                                            if (me == null) {
                                                                onRequireLogin()
                                                            } else {
                                                                replyBusy = true
                                                                replyStatus = null
                                                                scope.launch {
                                                                    val res = if (isMine) {
                                                                        api.deleteMemeQuickReply(m.id)
                                                                    } else {
                                                                        api.putMemeQuickReply(m.id, preset)
                                                                    }
                                                                    res.fold(
                                                                        onSuccess = { updated ->
                                                                            val fresh = updated ?: api.fetchMemeDetail(m.id)?.resolvedMeme
                                                                            if (fresh != null) {
                                                                                item = fresh
                                                                                onStateUpdated(fresh)
                                                                            }
                                                                        },
                                                                        onFailure = { replyStatus = it.message },
                                                                    )
                                                                    replyBusy = false
                                                                }
                                                            }
                                                        },
                                                ) {
                                                    Text(
                                                        text = preset,
                                                        color = accent ?: palette.text,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    )
                                                }
                                            }
                                        }

                                        val units = remember(replyDraft) { countReplyUnits(replyDraft) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            IconButton(
                                                onClick = { showReplyStickers = !showReplyStickers },
                                                modifier = Modifier.size(34.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.EmojiEmotions,
                                                    contentDescription = "插入表情",
                                                    tint = if (showReplyStickers) palette.primary else palette.muted,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                            OutlinedTextField(
                                                value = replyDraft,
                                                onValueChange = { replyDraft = it },
                                                placeholder = {
                                                    Text(
                                                        text = if (!m.myReply.isNullOrBlank()) "改一句回应（${BOOST_MAX_LEN}字内）" else "自己写一句回应（${BOOST_MAX_LEN}字内）",
                                                        fontSize = 12.sp,
                                                    )
                                                },
                                                singleLine = true,
                                                supportingText = if (units > 0) {
                                                    {
                                                        Text(
                                                            text = "$units/$BOOST_MAX_LEN",
                                                            color = if (units > BOOST_MAX_LEN) palette.danger else palette.muted,
                                                            fontSize = 10.sp,
                                                        )
                                                    }
                                                } else null,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Button(
                                                enabled = !replyBusy && units in 1..BOOST_MAX_LEN,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = palette.primary,
                                                    contentColor = palette.onPrimary,
                                                ),
                                                onClick = {
                                                    if (me == null) {
                                                        onRequireLogin()
                                                    } else {
                                                        val txt = replyDraft.trim()
                                                        replyBusy = true
                                                        replyStatus = null
                                                        scope.launch {
                                                            api.putMemeQuickReply(m.id, txt).fold(
                                                                onSuccess = { updated ->
                                                                    replyDraft = ""
                                                                    showReplyStickers = false
                                                                    val fresh = updated ?: api.fetchMemeDetail(m.id)?.resolvedMeme
                                                                    if (fresh != null) {
                                                                        item = fresh
                                                                        onStateUpdated(fresh)
                                                                    }
                                                                },
                                                                onFailure = { replyStatus = it.message },
                                                            )
                                                            replyBusy = false
                                                        }
                                                    }
                                                },
                                            ) {
                                                Text(if (m.myReply.isNullOrBlank()) "回应" else "改", fontSize = 12.sp)
                                            }
                                        }

                                        if (!m.myReply.isNullOrBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text("我的回应：${m.myReply}", color = palette.primary, fontSize = 11.sp)
                                                TextButton(
                                                    enabled = !replyBusy,
                                                    onClick = {
                                                        replyBusy = true
                                                        scope.launch {
                                                            api.deleteMemeQuickReply(m.id).fold(
                                                                onSuccess = { updated ->
                                                                    val fresh = updated ?: api.fetchMemeDetail(m.id)?.resolvedMeme
                                                                    if (fresh != null) {
                                                                        item = fresh
                                                                        onStateUpdated(fresh)
                                                                    }
                                                                },
                                                                onFailure = { replyStatus = it.message },
                                                            )
                                                            replyBusy = false
                                                        }
                                                    },
                                                ) {
                                                    Text("撤回回应", color = palette.danger, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        if (showReplyStickers && stickerPacks.isNotEmpty()) {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(stickerPacks, key = { "qr_tab_${it.tabKey}" }) { pack ->
                                                    FilterChip(
                                                        selected = activeStickerTab == pack.tabKey,
                                                        onClick = { activeStickerTab = pack.tabKey },
                                                        label = { Text(pack.label.removePrefix("L站·"), fontSize = 11.sp) },
                                                    )
                                                }
                                            }
                                            val selPack = stickerPacks.firstOrNull { it.tabKey == activeStickerTab }
                                            if (selPack != null) {
                                                Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Adaptive(42.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        items(selPack.items.distinct().take(60), key = { "${selPack.tabKey}_qr_$it" }) { stk ->
                                                            AsyncImage(
                                                                model = MediaUrlRewriter.stickerUrl(selPack.key, stk, selPack.ext),
                                                                imageLoader = api.imageLoader,
                                                                contentDescription = stk,
                                                                modifier = Modifier
                                                                    .size(38.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .clickable { replyDraft += "[${selPack.key}:$stk]" }
                                                                    .padding(2.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        replyStatus?.let { err ->
                                            Text(err, color = palette.danger, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = palette.line)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "评论 (${comments.size})",
                                    color = palette.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        if (comments.isEmpty()) {
                            item {
                                Text(
                                    text = "还没有评论，抢个沙发吧",
                                    color = palette.muted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        } else {
                            items(comments, key = { it.id }) { cm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    ForHappyAvatar(
                                        username = cm.displayAuthor,
                                        avatarUrl = cm.avatar,
                                        imageLoader = api.imageLoader,
                                        size = 30.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cm.displayAuthor,
                                            color = palette.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        cm.replyTo?.let { rep ->
                                            Text(
                                                text = "回复 @${rep.resolvedAuthor}：${rep.resolvedText}",
                                                color = palette.muted,
                                                fontSize = 11.sp,
                                            )
                                        }
                                        RichChatBodyView(
                                            rawBody = cm.body,
                                            imageLoader = api.imageLoader,
                                            textColor = palette.text,
                                            onPostRefClick = {},
                                            onHashtagClick = { tag ->
                                                onTagClick(tag)
                                                onDismiss()
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(color = palette.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = commentDraft,
                                onValueChange = { commentDraft = it },
                                placeholder = { Text("留下一句评论…", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = commentDraft.isNotBlank() && !posting,
                                onClick = {
                                    val txt = commentDraft.trim()
                                    posting = true
                                    scope.launch {
                                        if (api.postMemeComment(memeId, txt)) {
                                            commentDraft = ""
                                            comments = api.fetchMemeComments(memeId)
                                        }
                                        posting = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送")
                            }
                        }
                    }
                }
            }
        }

        localVideoPair?.let { (posterUrl, vidUrl) ->
            FullscreenMediaOverlay(
                imageUrl = posterUrl,
                videoUrl = vidUrl,
                api = api,
                onDismiss = { localVideoPair = null },
            )
        }
    }

@Composable
fun FullscreenMediaOverlay(
    imageUrl: String,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    videoUrl: String? = null,
) {
    if (!videoUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            var prepared by remember(videoUrl) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val mc = MediaController(ctx)
                            mc.setAnchorView(this)
                            setMediaController(mc)
                            setVideoURI(Uri.parse(videoUrl))
                            setOnPreparedListener { mp ->
                                prepared = true
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!prepared) {
                    CircularProgressIndicator(color = Color.White)
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(14.dp),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭视频", tint = Color.White)
                    }
                }
            }
        }
        return
    }

    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offsetX by remember(imageUrl) { mutableFloatStateOf(0f) }
    var offsetY by remember(imageUrl) { mutableFloatStateOf(0f) }
    var pullDismissY by remember(imageUrl) { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffsets(newScale: Float, candX: Float, candY: Float, size: IntSize): Pair<Float, Float> {
        val maxX = ((size.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
        val maxY = ((size.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
        return candX.coerceIn(-maxX, maxX) to candY.coerceIn(-maxY, maxY)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (0.95f - (pullDismissY / 900f)).coerceIn(0.45f, 0.95f)))
                .onSizeChanged { viewport = it }
                .pointerInput(imageUrl) {
                    detectTapGestures(
                        onTap = {
                            if (scale <= 1.02f) onDismiss()
                        },
                        onDoubleTap = { tapOffset ->
                            if (scale > 1.15f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val target = 2.6f
                                val rawX = (viewport.width / 2f - tapOffset.x) * (target - 1f)
                                val rawY = (viewport.height / 2f - tapOffset.y) * (target - 1f)
                                val (cx, cy) = clampOffsets(target, rawX, rawY, viewport)
                                scale = target
                                offsetX = cx
                                offsetY = cy
                            }
                        },
                    )
                }
                .pointerInput(imageUrl) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val nextScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = nextScale
                                pullDismissY = 0f
                                if (nextScale <= 1.01f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    val (cx, cy) = clampOffsets(nextScale, offsetX + pan.x, offsetY + pan.y, viewport)
                                    offsetX = cx
                                    offsetY = cy
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pressed == 1) {
                                val pan = event.calculatePan()
                                if (scale > 1.02f) {
                                    val (cx, cy) = clampOffsets(scale, offsetX + pan.x, offsetY + pan.y, viewport)
                                    offsetX = cx
                                    offsetY = cy
                                    if (pan.x != 0f || pan.y != 0f) {
                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (abs(pan.y) >= abs(pan.x) && (pullDismissY > 0f || pan.y > 0f)) {
                                    pullDismissY = (pullDismissY + pan.y).coerceAtLeast(0f)
                                    if (pan.y != 0f) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (pullDismissY > 210f && scale <= 1.02f) {
                            onDismiss()
                        } else {
                            pullDismissY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                imageLoader = api.imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY + pullDismissY
                    },
            )

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(14.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭大图", tint = Color.White)
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 42.dp),
            ) {
                Text(
                    text = if (scale > 1.02f) {
                        "${String.format(Locale.US, "%.1fx", scale)} · 单指平移查看 / 双击复位"
                    } else {
                        "双指捏合放大 · 双击放大 · 轻触或下拉关闭"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
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
