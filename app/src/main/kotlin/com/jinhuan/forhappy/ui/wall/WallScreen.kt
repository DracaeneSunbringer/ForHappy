package com.jinhuan.forhappy.ui.wall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.model.WallFilterState
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mobile-optimized Gallery Waterfall (`图廊`) with:
 * - Instant 1-column / 2-column layout toggle persisted locally
 * - Full sort/unseen/text/NSFW/author/multi-tag filters matching `app.js` `listParams`
 * - Double-tap card to like with tactile haptic burst + safe NSFW tap-to-unblur cover
 * - Direct FAB compose sheet + MemeDetailModal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallScreen(
    api: ForHappyApiClient,
    me: MeUser?,
    initialTag: String? = null,
    initialDetailMemeId: Long? = null,
    onConsumeInitialArgs: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()

    var columns by remember { mutableIntStateOf(2) }
    var filter by remember(initialTag) {
        mutableStateOf(
            WallFilterState(
                tags = if (!initialTag.isNullOrBlank()) setOf(initialTag) else emptySet(),
            ),
        )
    }
    var searchDraft by remember { mutableStateOf("") }
    var anchors by remember { mutableStateOf<List<AnchorTag>>(emptyList()) }
    var popularTags by remember { mutableStateOf<List<TagStatItem>>(emptyList()) }
    var memes by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }

    var selectedMemeId by remember(initialDetailMemeId) { mutableStateOf(initialDetailMemeId) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showCompose by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        columns = api.getPref("ui.wall_columns")?.toIntOrNull()?.coerceIn(1, 2) ?: 2
        if (initialTag != null || initialDetailMemeId != null) {
            onConsumeInitialArgs()
        }
    }

    LaunchedEffect(filter.nsfwMode) {
        val hideNsfw = filter.nsfwMode == "hide"
        val anchorsDef = async { api.fetchAnchors() }
        val tagsDef = async { api.fetchTags(hideNsfw = hideNsfw) }
        anchors = anchorsDef.await()
        popularTags = tagsDef.await()
    }

    fun reload(curFilter: WallFilterState = filter) {
        scope.launch {
            loading = true
            val tagsCsv = curFilter.tags.takeIf { it.isNotEmpty() }?.joinToString(",")
            val q = curFilter.searchQuery.trim().ifBlank { null }
            val author = curFilter.author.trim().ifBlank { null }
            val hideNsfw = curFilter.nsfwMode == "hide"

            val flat = api.fetchMemes(
                limit = 40,
                offset = 0,
                tag = tagsCsv,
                query = q,
                sort = curFilter.sort,
                unseenOnly = curFilter.unseenOnly,
                textFilter = curFilter.textFilter,
                nsfwHide = hideNsfw,
                author = author,
            )
            memes = flat
            hasMore = flat.size >= 30
            loading = false
        }
    }

    fun loadMore() {
        if (loading || loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val tagsCsv = filter.tags.takeIf { it.isNotEmpty() }?.joinToString(",")
            val q = filter.searchQuery.trim().ifBlank { null }
            val author = filter.author.trim().ifBlank { null }
            val hideNsfw = filter.nsfwMode == "hide"
            val existingIds = memes.asSequence().map { it.id }.toHashSet()

            val appended = api.fetchMemes(
                limit = 30,
                offset = memes.size,
                tag = tagsCsv,
                query = q,
                sort = filter.sort,
                unseenOnly = filter.unseenOnly,
                textFilter = filter.textFilter,
                nsfwHide = hideNsfw,
                author = author,
            ).filter { existingIds.add(it.id) }
            if (appended.isNotEmpty()) {
                memes = memes + appended
                hasMore = appended.size >= 25
            } else {
                hasMore = false
            }
            loadingMore = false
        }
    }

    LaunchedEffect(
        filter.sort,
        filter.unseenOnly,
        filter.textFilter,
        filter.nsfwMode,
        filter.tags,
        filter.author,
        filter.searchQuery,
    ) {
        reload(filter)
    }

    // Tag list combining /api/tags and /api/anchors cleanly without duplicates
    val quickTags = remember(popularTags, anchors, filter.tags) {
        val merged = LinkedHashSet<String>()
        merged.addAll(filter.tags)
        popularTags.take(14).forEach { if (it.tag.isNotBlank()) merged.add(it.tag) }
        anchors.take(8).forEach { if (it.name.isNotBlank()) merged.add(it.name) }
        merged.toList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact search + layout columns + filter + refresh bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = palette.muted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (searchDraft.isEmpty()) {
                                Text(
                                    text = "搜梗图 / No.123 / 标签",
                                    color = palette.muted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = searchDraft,
                                onValueChange = { searchDraft = it },
                                singleLine = true,
                                textStyle = TextStyle(color = palette.text, fontSize = 13.sp),
                                cursorBrush = SolidColor(palette.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { filter = filter.copy(searchQuery = searchDraft) },
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (searchDraft.isNotEmpty() || filter.searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "清空搜索",
                                tint = palette.muted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        searchDraft = ""
                                        filter = filter.copy(searchQuery = "")
                                    },
                            )
                        }
                    }
                }

                // Single <-> Dual Column waterfall toggle button
                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .clickable {
                            val next = if (columns == 2) 1 else 2
                            columns = next
                            scope.launch { api.putPref("ui.wall_columns", next.toString()) }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = if (columns == 2) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            contentDescription = if (columns == 2) "切换单栏" else "切换双栏",
                            tint = palette.primary,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = if (columns == 2) "双栏" else "单栏",
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Filter modal toggle button with active count badge
                Surface(
                    color = if (filter.activeFilterCount > 0) palette.primary else palette.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .clickable { showFilterSheet = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "筛选",
                            tint = if (filter.activeFilterCount > 0) palette.onPrimary else palette.text,
                            modifier = Modifier.size(17.dp),
                        )
                        if (filter.activeFilterCount > 0) {
                            Text(
                                text = "${filter.activeFilterCount}",
                                color = palette.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Refresh button
                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            if (searchDraft != filter.searchQuery) {
                                filter = filter.copy(searchQuery = searchDraft)
                            } else {
                                reload(filter)
                            }
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "刷新",
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Compact quick-sort, unseen & tag filter strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(bottom = 7.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    WallPill(
                        label = "最新",
                        selected = filter.sort == "new",
                        onClick = { filter = filter.copy(sort = "new") },
                    )
                }
                item {
                    WallPill(
                        label = "热门",
                        selected = filter.sort == "hot",
                        onClick = { filter = filter.copy(sort = "hot") },
                    )
                }
                item {
                    WallPill(
                        label = "最早",
                        selected = filter.sort == "old",
                        onClick = { filter = filter.copy(sort = "old") },
                    )
                }
                item {
                    WallPill(
                        label = "只看未看",
                        selected = filter.unseenOnly,
                        onClick = { filter = filter.copy(unseenOnly = !filter.unseenOnly) },
                    )
                }
                item {
                    WallPill(
                        label = "文字帖",
                        selected = filter.textFilter == "only",
                        onClick = {
                            filter = filter.copy(textFilter = if (filter.textFilter == "only") "" else "only")
                        },
                    )
                }
                items(quickTags, key = { "qtag_$it" }) { tagName ->
                    val isSel = filter.tags.contains(tagName)
                    WallPill(
                        label = "#$tagName",
                        selected = isSel,
                        onClick = {
                            val nextTags = if (isSel) filter.tags - tagName else setOf(tagName)
                            filter = filter.copy(tags = nextTags)
                        },
                    )
                }
            }

            // Active filter summary conditions bar when non-default filters applied
            if (filter.activeFilterCount > 0) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.bg)
                        .padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (filter.nsfwMode == "hide") {
                        item {
                            RemovableConditionChip("已隐藏敏感") { filter = filter.copy(nsfwMode = "blur") }
                        }
                    }
                    if (filter.textFilter == "0") {
                        item {
                            RemovableConditionChip("不看文字帖") { filter = filter.copy(textFilter = "") }
                        }
                    }
                    if (filter.author.isNotBlank()) {
                        item {
                            RemovableConditionChip("发图人 @${filter.author}") { filter = filter.copy(author = "") }
                        }
                    }
                    items(filter.tags.toList(), key = { "cond_$it" }) { t ->
                        RemovableConditionChip("#$t") { filter = filter.copy(tags = filter.tags - t) }
                    }
                    item {
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable {
                                searchDraft = ""
                                filter = WallFilterState()
                            },
                        ) {
                            Text(
                                text = "全部重置",
                                color = palette.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            if (loading && memes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (memes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前筛选条件下暂无梗图", color = palette.muted, fontSize = 14.sp)
                        if (filter.activeFilterCount > 0 || filter.searchQuery.isNotBlank()) {
                            OutlinedButton(onClick = {
                                searchDraft = ""
                                filter = WallFilterState()
                            }) {
                                Text("清空筛选条件")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(if (columns == 1) 12.dp else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                ) {
                    items(memes, key = { it.id }) { meme ->
                        MemeWaterfallCard(
                            meme = meme,
                            api = api,
                            me = me,
                            isSingleColumn = columns == 1,
                            onRequireLogin = onRequireLogin,
                            onOpenDetail = { selectedMemeId = meme.id },
                            onMemeUpdated = { updated ->
                                memes = memes.map { if (it.id == updated.id) updated else it }
                            },
                            onDoubleTapLike = {
                                if (me == null) {
                                    onRequireLogin()
                                } else {
                                    scope.launch {
                                        api.toggleLike(meme.id)
                                        memes = memes.map { cur ->
                                            if (cur.id == meme.id) {
                                                cur.copy(
                                                    liked = !cur.liked,
                                                    likes = if (cur.liked) maxOf(0, cur.likes - 1) else cur.likes + 1,
                                                    likeCount = if (cur.liked) maxOf(0, cur.likeCount - 1) else cur.likeCount + 1,
                                                )
                                            } else {
                                                cur
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                loadingMore -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = palette.primary)
                                hasMore -> OutlinedButton(onClick = { loadMore() }) {
                                    Text("加载更多梗图（当前 ${memes.size} 条）", fontSize = 12.sp)
                                }
                                else -> Text(
                                    text = "已加载 ${memes.size} 条梗图",
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (me == null) onRequireLogin() else showCompose = true
            },
            containerColor = palette.primary,
            contentColor = palette.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "投稿")
        }
    }

    if (showFilterSheet) {
        WallFilterSheet(
            current = filter,
            availableTags = popularTags.map { it.tag }.ifEmpty { anchors.map { it.name } },
            onDismiss = { showFilterSheet = false },
            onApply = { next ->
                filter = next
                showFilterSheet = false
            },
        )
    }

    selectedMemeId?.let { id ->
        MemeDetailModal(
            memeId = id,
            api = api,
            me = me,
            onRequireLogin = onRequireLogin,
            onDismiss = { selectedMemeId = null },
            onOpenImageFullscreen = { url -> fullscreenImageUrl = url },
            onTagClick = { tag -> filter = filter.copy(tags = setOf(tag)) },
            onStateUpdated = { updated ->
                memes = memes.map { if (it.id == updated.id) updated else it }
            },
        )
    }

    fullscreenImageUrl?.let { url ->
        FullscreenMediaOverlay(
            imageUrl = url,
            api = api,
            onDismiss = { fullscreenImageUrl = null },
        )
    }

    if (showCompose) {
        ComposeMemeModal(
            api = api,
            onDismiss = { showCompose = false },
            onPublished = { reload(filter) },
        )
    }
}

@Composable
private fun WallPill(
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
                width = 1.dp,
                color = if (selected) palette.primary else palette.line,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            color = if (selected) palette.onPrimary else palette.text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun RemovableConditionChip(
    label: String,
    onRemove: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable { onRemove() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(label, color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Rounded.Close, contentDescription = "移除", tint = palette.primary, modifier = Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WallFilterSheet(
    current: WallFilterState,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onApply: (WallFilterState) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(current) { mutableStateOf(current) }
    var tagSearch by remember { mutableStateOf("") }

    val filteredTags = remember(availableTags, draft.tags, tagSearch) {
        val all = (draft.tags + availableTags).distinct()
        val q = tagSearch.trim().removePrefix("#").lowercase()
        if (q.isBlank()) all else all.filter { it.lowercase().contains(q) }
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
                .fillMaxHeight(0.84f)
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
                Text("图廊筛选与排序", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { draft = WallFilterState(searchQuery = current.searchQuery) }) {
                    Text("重置全部")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                item {
                    Text("排序方式", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("new" to "最新发布", "hot" to "热门排行", "old" to "最早发布").forEach { (k, lbl) ->
                            WallPill(label = lbl, selected = draft.sort == k) {
                                draft = draft.copy(sort = k)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("只看未看", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("跳过个人已记录浏览过的梗图", color = palette.muted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = draft.unseenOnly,
                            onCheckedChange = { draft = draft.copy(unseenOnly = it) },
                        )
                    }
                }

                item {
                    Text("帖子类型", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to "全部帖子", "only" to "只看文字帖", "0" to "不看文字帖").forEach { (k, lbl) ->
                            WallPill(label = lbl, selected = draft.textFilter == k) {
                                draft = draft.copy(textFilter = k)
                            }
                        }
                    }
                }

                item {
                    Text("敏感与剧透内容", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("blur" to "默认打码（点开显示）", "hide" to "完全隐藏").forEach { (k, lbl) ->
                            WallPill(label = lbl, selected = draft.nsfwMode == k) {
                                draft = draft.copy(nsfwMode = k)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = draft.author,
                        onValueChange = { draft = draft.copy(author = it) },
                        label = { Text("发图人用户名（可选，多个逗号分隔）", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text("标签筛选（可多选，已选 ${draft.tags.size} 个）", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = tagSearch,
                        onValueChange = { tagSearch = it },
                        placeholder = { Text("搜索 #标签…", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        filteredTags.take(40).forEach { t ->
                            val sel = draft.tags.contains(t)
                            WallPill(label = "#$t", selected = sel) {
                                draft = draft.copy(
                                    tags = if (sel) draft.tags - t else draft.tags + t,
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = { onApply(draft) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary,
                        ),
                        modifier = Modifier.weight(2f),
                    ) {
                        Text("应用筛选")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemeWaterfallCard(
    meme: MemeItem,
    api: ForHappyApiClient,
    me: MeUser?,
    isSingleColumn: Boolean,
    onRequireLogin: () -> Unit,
    onOpenDetail: () -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
    onDoubleTapLike: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showHeartBurst by remember { mutableStateOf(false) }
    var revealNsfw by remember(meme.id) { mutableStateOf(false) }
    var useExtFallback by remember(meme.id) { mutableStateOf(false) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(650L)
            showHeartBurst = false
        }
    }

    val rawUrl = remember(meme.primaryImageUrl, isSingleColumn, useExtFallback) {
        val candidate = if (isSingleColumn) {
            meme.detailImageUrls.firstOrNull() ?: meme.primaryImageUrl
        } else {
            meme.primaryImageUrl
        }
        val primary = MediaUrlRewriter.rewriteMediaUrl(candidate)
        if (useExtFallback && primary.isNotBlank()) MediaUrlRewriter.externalFallbackUrl(primary) else primary
    }

    val knownAspect = remember(meme.effectiveWidth, meme.effectiveHeight, isSingleColumn) {
        val w = meme.effectiveWidth ?: 0
        val h = meme.effectiveHeight ?: 0
        when {
            w > 0 && h > 0 && isSingleColumn -> (w.toFloat() / h.toFloat()).coerceIn(0.30f, 2.4f)
            w > 0 && h > 0 -> (w.toFloat() / h.toFloat()).coerceIn(0.55f, 1.85f)
            isSingleColumn -> null
            else -> 1f
        }
    }

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .pointerInput(meme.id) {
                detectTapGestures(
                    onTap = {
                        if (meme.isRisky && !revealNsfw) {
                            revealNsfw = true
                        } else {
                            onOpenDetail()
                        }
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHeartBurst = true
                        onDoubleTapLike()
                    },
                )
            },
    ) {
        Column {
            if (rawUrl.isNotBlank()) {
                val imgContainerModifier = if (knownAspect != null) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(knownAspect)
                        .background(palette.surface2)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 560.dp)
                        .background(palette.surface2)
                }
                Box(
                    modifier = imgContainerModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = rawUrl,
                        imageLoader = api.imageLoader,
                        contentDescription = meme.safeLabel,
                        contentScale = if (isSingleColumn) ContentScale.FillWidth else ContentScale.Crop,
                        onError = {
                            if (!useExtFallback && !rawUrl.contains("/ext?u=")) {
                                useExtFallback = true
                            }
                        },
                        modifier = (if (knownAspect != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                            .let { mod ->
                                if (meme.isRisky && !revealNsfw) mod.blur(20.dp) else mod
                            },
                    )
                    if (meme.isRisky && !revealNsfw) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("NSFW · 轻触显示", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showHeartBurst,
                        enter = scaleIn(),
                        exit = scaleOut(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            } else {
                // Text-only meme card preview
                val snippet = meme.text?.takeIf { it.isNotBlank() && it != meme.title } ?: meme.content.orEmpty()
                if (snippet.isNotBlank()) {
                    Surface(
                        color = palette.surface2.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = snippet,
                            color = palette.text,
                            fontSize = if (isSingleColumn) 13.sp else 12.sp,
                            maxLines = if (isSingleColumn) 9 else 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = meme.safeLabel,
                    color = palette.text,
                    fontSize = if (isSingleColumn) 14.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (isSingleColumn) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val usedReactions = remember(meme.reactions) {
                    meme.reactions.filter { it.count > 0 }
                        .sortedWith(compareByDescending<com.jinhuan.forhappy.core.model.ChatReaction> { it.count }.thenByDescending { it.mine })
                }
                if (usedReactions.isNotEmpty()) {
                    val maxShown = if (isSingleColumn) 8 else 4
                    val shownRx = usedReactions.take(maxShown)
                    val restRx = usedReactions.size - shownRx.size
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        shownRx.forEach { rx ->
                            ReactionChip(
                                emojiKey = rx.emoji,
                                count = rx.count,
                                mine = rx.mine,
                                imageLoader = api.imageLoader,
                                compact = true,
                                onClick = {
                                    if (me == null) {
                                        onRequireLogin()
                                    } else {
                                        scope.launch {
                                            api.reactToMeme(meme.id, rx.emoji)?.let(onMemeUpdated)
                                        }
                                    }
                                },
                            )
                        }
                        if (restRx > 0) {
                            Surface(
                                color = palette.surface2,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.clickable { onOpenDetail() },
                            ) {
                                Text(
                                    text = "+$restRx",
                                    color = palette.muted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }

                val quickReplies = remember(meme.replyList, meme.myReply, me?.username) {
                    meme.effectiveQuickReplies(me)
                }
                if (quickReplies.isNotEmpty()) {
                    val maxReplies = if (isSingleColumn) 5 else 2
                    val shownReplies = quickReplies.takeLast(maxReplies)
                    val hiddenReplies = maxOf(0, maxOf(meme.replyCount, quickReplies.size) - shownReplies.size)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        shownReplies.forEach { qr ->
                            Surface(
                                color = if (qr.mine) palette.primary.copy(alpha = 0.14f) else palette.surface2,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (qr.mine) palette.primary.copy(alpha = 0.45f) else palette.line.copy(alpha = 0.55f),
                                        RoundedCornerShape(999.dp),
                                    )
                                    .clickable {
                                        if (me == null) {
                                            onRequireLogin()
                                        } else {
                                            scope.launch {
                                                val res = if (qr.mine) {
                                                    api.deleteMemeQuickReply(meme.id)
                                                } else {
                                                    api.putMemeQuickReply(meme.id, qr.text)
                                                }
                                                res.getOrNull()?.let(onMemeUpdated)
                                            }
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    ForHappyAvatar(
                                        username = qr.displayAuthor,
                                        avatarUrl = qr.avatar,
                                        imageLoader = api.imageLoader,
                                        size = 14.dp,
                                    )
                                    BoostPillText(
                                        text = qr.text,
                                        imageLoader = api.imageLoader,
                                        fontSize = 10.sp,
                                    )
                                    if (qr.n > 1) {
                                        Text(
                                            text = "×${qr.n}",
                                            color = palette.muted,
                                            fontSize = 9.sp,
                                        )
                                    }
                                }
                            }
                        }
                        if (hiddenReplies > 0) {
                            Text(
                                text = "还有${hiddenReplies}条回应",
                                color = palette.primary,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clickable { onOpenDetail() },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = meme.authorDisplay.ifBlank { meme.day?.takeLast(5) ?: "No.${meme.id}" },
                        color = palette.muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (meme.replyCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.RocketLaunch,
                                    contentDescription = "一句回应",
                                    tint = palette.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("${meme.replyCount}", color = palette.primary, fontSize = 11.sp)
                            }
                        }
                        if (meme.totalComments > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = "评论",
                                    tint = palette.muted,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("${meme.totalComments}", color = palette.muted, fontSize = 11.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (meme.liked) palette.danger else palette.muted,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = meme.totalLikes.toString(),
                                color = if (meme.liked) palette.danger else palette.muted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
