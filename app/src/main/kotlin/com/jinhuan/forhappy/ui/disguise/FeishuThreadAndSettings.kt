package com.jinhuan.forhappy.ui.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.BuildConfig
import com.jinhuan.forhappy.core.compose.ComposeTrayItem
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.ChatReaction
import com.jinhuan.forhappy.core.model.MemeComment
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemeMediaItem
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.ShortcutGestureMode
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.theme.LocalBossKeyActive
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.wall.ImmersiveMediaStage
import com.jinhuan.forhappy.ui.wall.NativeVideoPlayer
import com.jinhuan.forhappy.ui.wall.saveMediaToSystemGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import kotlin.math.abs

internal data class FeishuThemePalette(
    val blue: Color,
    val bg: Color,
    val card: Color,
    val peerBubble: Color,
    val selfBubble: Color,
    val surfaceMuted: Color,
    val reactionBg: Color,
    val reactionMineBg: Color,
    val line: Color,
    val text: Color,
    val subText: Color,
    val muted: Color,
    val unreadRed: Color,
    val onlineGreen: Color,
)

@Composable
internal fun rememberFeishuColors(): FeishuThemePalette {
    val isDark = LocalForHappyPalette.current.isDark
    return remember(isDark) {
        if (isDark) {
            FeishuThemePalette(
                blue = Color(0xFF4C88FF),
                bg = Color(0xFF111215),
                card = Color(0xFF1A1C20),
                peerBubble = Color(0xFF24272E),
                selfBubble = Color(0xFF1D3766),
                surfaceMuted = Color(0xFF25282F),
                reactionBg = Color(0xFF252931),
                reactionMineBg = Color(0xFF1E355E),
                line = Color(0xFF282B32),
                text = Color(0xFFEFF0F2),
                subText = Color(0xFFA6ABB4),
                muted = Color(0xFF7E8590),
                unreadRed = Color(0xFFF54A45),
                onlineGreen = Color(0xFF27C346),
            )
        } else {
            FeishuThemePalette(
                blue = Color(0xFF3370FF),
                bg = Color(0xFFF2F3F5),
                card = Color(0xFFFFFFFF),
                peerBubble = Color(0xFFFFFFFF),
                selfBubble = Color(0xFFD8E6FF),
                surfaceMuted = Color(0xFFF2F3F5),
                reactionBg = Color(0xFFE5E8EE),
                reactionMineBg = Color(0xFFDCE8FF),
                line = Color(0xFFEBEDF0),
                text = Color(0xFF1F2329),
                subText = Color(0xFF646A73),
                muted = Color(0xFF8F959E),
                unreadRed = Color(0xFFF54A45),
                onlineGreen = Color(0xFF00B42A),
            )
        }
    }
}

internal val FEISHU_WORK_QUICK_PHRASES: List<String> = listOf(
    "收到，已阅",
    "同意，按这个推进",
    "辛苦了，点赞",
    "已对齐",
    "+1",
)

/**
 * Official Feishu paper-bird vector mark (`FS_MARK` viewBox `0 0 32 32`).
 */
@Composable
internal fun FeishuBrandMark(
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sx = w / 32f
        val sy = h / 32f
        drawRoundRect(
            color = Color(0xFF3370FF),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(8f * sx, 8f * sy),
        )
        val wingPath = Path().apply {
            moveTo(7f * sx, 15.5f * sy)
            lineTo(24.5f * sx, 8f * sy)
            lineTo(20.3f * sx, 24.3f * sy)
            lineTo(14.9f * sx, 19.9f * sy)
            lineTo(11.5f * sx, 23.0f * sy)
            lineTo(11.8f * sx, 18.0f * sy)
            close()
        }
        drawPath(path = wingPath, color = Color.White)
        val foldPath = Path().apply {
            moveTo(11.8f * sx, 18.0f * sy)
            lineTo(20.5f * sx, 10.7f * sy)
            lineTo(14.5f * sx, 19.1f * sy)
            close()
        }
        drawPath(path = foldPath, color = Color(0xFFC2D4FF))
    }
}

/**
 * Authentic Feishu 7.x Android 2×2 composite group avatar: a soft-tinted rounded square
 * containing 4 deterministic colleague surname tiles.
 */
@Composable
internal fun FeishuGroupMosaicAvatar(
    seedKey: String,
    fc: FeishuThemePalette,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier,
) {
    val members = remember(seedKey) {
        val baseHash = abs(seedKey.hashCode()).toLong()
        (0..3).map { idx ->
            val name = FeishuDisguiseHelpers.imFake(baseHash + idx * 131L + 7L)
            val surname = name.take(1).ifBlank { "同" }
            surname to FeishuDisguiseHelpers.imColor("$seedKey#$idx#$name")
        }
    }
    val outerRadius = size * 0.22f
    val pad = size * 0.07f
    val gap = size * 0.045f
    val cellSize = (size - pad * 2 - gap) / 2f
    val cellRadius = cellSize * 0.24f
    val fontSp = (cellSize.value * 0.54f).coerceIn(8f, 12f).sp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(outerRadius))
            .background(fc.reactionBg)
            .padding(pad),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0..1) {
                        val (ch, bg) = members[row * 2 + col]
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(cellRadius))
                                .background(bg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = ch,
                                color = Color.White,
                                fontSize = fontSp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun applyLauncherIconAlias(context: Context, selected: AppIconAlias) {
    val pm = context.packageManager
    val pkg = context.packageName
    for (alias in AppIconAlias.entries) {
        val state = if (alias == selected) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, alias.aliasClass),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}

internal fun safeIconAliasTitle(alias: AppIconAlias): String = when (alias) {
    AppIconAlias.JINHUAN -> "标准样式"
    AppIconAlias.FEISHU -> "飞书文档"
    AppIconAlias.DOCS -> "协作文档"
}

internal fun feishuSafeAuthor(seedId: Long, rawAuthor: String?, aliasEnabled: Boolean): String {
    val cleanRaw = DisguiseSanitizer.sanitizeSummaryForDisguise(
        rawAuthor.orEmpty().ifBlank { "同事" },
        AppDisguiseMode.FEISHU,
    )
    return DisguiseSanitizer.mapColleagueName(seedId, cleanRaw, aliasEnabled)
}

/**
 * Returns sanitized message text ONLY when the meme actually carries a human caption/body.
 * Media-only memes return `null` so Feishu IM renders a clean standalone image thumbnail
 * instead of repeating fake placeholder sentences on every photo.
 */
internal fun feishuMemeOptionalText(meme: MemeItem): String? {
    val candidates = listOfNotNull(meme.content, meme.text, meme.caption, meme.body, meme.title)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val raw = candidates.firstOrNull() ?: return if (meme.mediaItems.isEmpty()) DisguiseSanitizer.workPlaceholder(meme.id) else null
    if (meme.mediaItems.isNotEmpty() && (
            raw.startsWith("工作同步截图") ||
                raw.startsWith("佬友素材分享") ||
                raw == "分享一张图" ||
                raw == "无题"
            )
    ) {
        return null
    }
    val cleaned = DisguiseSanitizer.sanitizeSummaryForDisguise(raw, AppDisguiseMode.FEISHU).trim()
    return cleaned.takeIf { it.isNotBlank() }
        ?: if (meme.mediaItems.isEmpty()) DisguiseSanitizer.workPlaceholder(meme.id) else null
}

internal fun feishuMemePrimaryText(meme: MemeItem): String {
    return feishuMemeOptionalText(meme) ?: "[图片]"
}

/**
 * Shared media upload + publish pipeline for both Feishu work-group messages and
 * LINUX DO Discourse floors. Supports multi-image merge vs solo posts, masked/original
 * dual uploads (`keepOriginalUnmasked`), and `[spoiler]` flags.
 */
internal suspend fun disguiseUploadAndPublish(
    context: Context,
    api: ForHappyApiClient,
    text: String,
    stagedItems: List<ComposeTrayItem>,
    mergeAll: Boolean,
    spoiler: Boolean,
    tags: List<String>,
    fallbackTitlePrefix: String,
): Result<List<Long>> {
    val trimmed = text.trim()
    if (stagedItems.isEmpty()) {
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入内容或添加图片"))
        }
        return api.createMeme(
            title = trimmed.take(48),
            content = trimmed,
            tags = tags,
            spoiler = spoiler,
            uploadedKey = null,
        ).map { listOf(it) }
    }

    val uploadedPairs = mutableListOf<Pair<String, String?>>()
    for (item in stagedItems) {
        val mainPath = item.maskedFilePath ?: item.localFilePath
        if (mainPath.isNullOrBlank()) continue
        val mainBytes = withContext(Dispatchers.IO) {
            runCatching { File(mainPath).readBytes() }.getOrNull()
        } ?: return Result.failure(IllegalStateException("读取本地媒体失败：${item.fileName}"))
        val mainUpload = api.uploadMediaBytes(
            bytes = mainBytes,
            fileName = item.fileName,
            mimeType = item.mimeType,
            forWhat = "meme",
        )
        val mainKey = mainUpload.getOrElse { return Result.failure(it) }.key
        var origKey: String? = null
        if (item.keepOriginalUnmasked && item.isMasked && !item.localFilePath.isNullOrBlank() && item.localFilePath != mainPath) {
            val origBytes = withContext(Dispatchers.IO) {
                runCatching { File(item.localFilePath).readBytes() }.getOrNull()
            }
            if (origBytes != null) {
                origKey = api.uploadMediaBytes(
                    bytes = origBytes,
                    fileName = "orig_${item.fileName}",
                    mimeType = item.mimeType,
                    forWhat = "meme",
                ).getOrNull()?.key
            }
        }
        uploadedPairs.add(mainKey to origKey)
    }

    if (uploadedPairs.isEmpty()) {
        return Result.failure(IllegalStateException("没有可上传的媒体文件"))
    }

    val title = trimmed.takeIf { it.isNotBlank() }?.take(60)
        ?: "$fallbackTitlePrefix（${uploadedPairs.size}P）"

    return if (mergeAll || uploadedPairs.size == 1) {
        val itemsArray = JsonArray(
            uploadedPairs.map { (k, orig) ->
                buildJsonObject {
                    put("key", JsonPrimitive(k))
                    if (!orig.isNullOrBlank()) put("orig", JsonPrimitive(orig))
                }
            },
        )
        api.createMemeFull(
            kind = null,
            title = title,
            content = trimmed,
            tags = tags,
            spoiler = spoiler,
            aiOff = false,
            layout = "carousel",
            items = itemsArray,
        ).map { listOf(it) }
    } else {
        val ids = mutableListOf<Long>()
        for ((index, pair) in uploadedPairs.withIndex()) {
            val singleArray = JsonArray(
                listOf(
                    buildJsonObject {
                        put("key", JsonPrimitive(pair.first))
                        if (!pair.second.isNullOrBlank()) put("orig", JsonPrimitive(pair.second))
                    },
                ),
            )
            val perTitle = if (trimmed.isNotBlank()) "${trimmed.take(40)} (${index + 1}/${uploadedPairs.size})" else "$fallbackTitlePrefix ${index + 1}"
            val res = api.createMemeFull(
                kind = null,
                title = perTitle,
                content = trimmed,
                tags = tags,
                spoiler = spoiler,
                aiOff = false,
                layout = "carousel",
                items = singleArray,
            )
            val id = res.getOrElse { return Result.failure(it) }
            ids.add(id)
        }
        Result.success(ids)
    }
}

/**
 * Shared neutral media renderer for Feishu & LINUX DO. Handles privacy blur, click-to-reveal,
 * auto-muted disguised video playback, Feishu compact IM thumbnails / 9-grid (`compactImMode`),
 * Discourse Lightbox footer (`discourseLightbox`), and fullscreen `ImmersiveMediaStage`.
 */
@Composable
internal fun DisguiseMediaStrip(
    meme: MemeItem,
    api: ForHappyApiClient,
    blurAll: Boolean,
    accentColor: Color,
    isItemUnblurred: Boolean,
    onRevealItem: () -> Unit,
    onOpenViewer: (List<MemeMediaItem>, Int) -> Unit,
    onMediaLoaded: () -> Unit = {},
    compactImMode: Boolean = false,
    discourseLightbox: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val items = remember(meme.id, meme.images, meme.image, meme.thumb, meme.media) { meme.mediaItems }
    if (items.isEmpty()) return
    val isBlurred = blurAll && !isItemUnblurred

    if (items.size == 1) {
        val single = items.first()
        val thumbUrl = remember(single) { MediaUrlRewriter.rewriteMediaUrl(single.resolvedThumbOrUrl) }
        val videoUrl = remember(single) { single.v?.takeIf { it.isNotBlank() }?.let { MediaUrlRewriter.rewriteMediaUrl(it) } }
        val w = single.w ?: 0
        val h = single.h ?: 0
        val aspect = remember(w, h, compactImMode) {
            if (w > 0 && h > 0) {
                (w.toFloat() / h.toFloat()).coerceIn(if (compactImMode) 0.62f else 0.65f, 1.80f)
            } else {
                if (compactImMode) 1.15f else 1.35f
            }
        }
        val corner = when {
            compactImMode -> 10.dp
            discourseLightbox -> 6.dp
            else -> 8.dp
        }
        val sizeMod = if (compactImMode) {
            val preferredWidth = if (aspect < 0.85f) 168.dp else 216.dp
            Modifier
                .width(preferredWidth)
                .aspectRatio(aspect)
                .heightIn(max = 236.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .heightIn(max = 268.dp)
        }
        Box(
            modifier = modifier
                .then(sizeMod)
                .clip(RoundedCornerShape(corner))
                .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(corner))
                .background(Color(0xFF1F2329).copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (single.isVideo && !videoUrl.isNullOrBlank()) {
                LaunchedEffect(videoUrl) { onMediaLoaded() }
                NativeVideoPlayer(
                    videoUrl = videoUrl,
                    posterUrl = thumbUrl,
                    api = api,
                    inlineMode = true,
                    isBlurred = isBlurred,
                    onExpandFullscreen = { onOpenViewer(items, 0) },
                    modifier = Modifier.fillMaxSize(),
                )
                if (isBlurred) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { onRevealItem() },
                    ) {
                        Text(
                            text = if (compactImMode) "附件已隐藏 · 轻触查看" else "已保护 · 点击查看",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = thumbUrl,
                    imageLoader = api.imageLoader,
                    contentDescription = "附件图片",
                    contentScale = ContentScale.Crop,
                    onSuccess = { onMediaLoaded() },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isBlurred) Modifier.blur(22.dp) else Modifier)
                        .clickable {
                            if (isBlurred) onRevealItem() else onOpenViewer(items, 0)
                        },
                )
                if (isBlurred) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable { onRevealItem() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Text(
                                text = if (compactImMode) "轻触查看图片" else "已保护 · 点击查看",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                } else {
                    if (single.isGif || single.isLive) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.52f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp),
                        ) {
                            Text(
                                text = if (single.isLive) "LIVE" else "GIF",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    if (discourseLightbox) {
                        val dimLabel = if (w > 0 && h > 0) "${w}×${h}" else "原图预览"
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.62f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                                Text(
                                    text = dimLabel,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 10.sp,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.OpenInFull,
                                contentDescription = "展开大图",
                                tint = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    } else if (compactImMode) {
        // Authentic Feishu 2-col / 3-col tight photo grid (up to 9 cells, max width 216dp)
        val shown = remember(items) { items.take(9) }
        val cols = if (shown.size == 2 || shown.size == 4) 2 else 3
        val gridWidth = if (cols == 2) 196.dp else 216.dp
        val extraCount = (items.size - 9).coerceAtLeast(0)
        Column(
            modifier = modifier
                .width(gridWidth)
                .clip(RoundedCornerShape(10.dp))
                .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            verticalArrangement = Arrangement.spacedBy(2.5.dp),
        ) {
            shown.chunked(cols).forEachIndexed { rIdx, rowList ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                ) {
                    rowList.forEachIndexed { cIdx, item ->
                        val flatIdx = rIdx * cols + cIdx
                        val url = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.resolvedThumbOrUrl) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(Color(0xFF1F2329).copy(alpha = 0.07f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = url,
                                imageLoader = api.imageLoader,
                                contentDescription = "附件图片 ${flatIdx + 1}",
                                contentScale = ContentScale.Crop,
                                onSuccess = { if (flatIdx == 0) onMediaLoaded() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (isBlurred) Modifier.blur(18.dp) else Modifier)
                                    .clickable {
                                        if (isBlurred) onRevealItem() else onOpenViewer(items, flatIdx)
                                    },
                            )
                            if (isBlurred && flatIdx == 0) {
                                Text(
                                    text = "${items.size}张 · 查看",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.56f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            } else if (!isBlurred && flatIdx == shown.lastIndex && extraCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.48f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "+$extraCount",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    repeat(cols - rowList.size) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    } else {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(items, key = { idx, item -> "${meme.id}:$idx:${item.u ?: item.t}" }) { idx, item ->
                val url = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.resolvedThumbOrUrl) }
                val cellCorner = if (discourseLightbox) 6.dp else 8.dp
                Box(
                    modifier = Modifier
                        .size(width = 136.dp, height = 118.dp)
                        .clip(RoundedCornerShape(cellCorner))
                        .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(cellCorner))
                        .background(Color(0xFF1F2329).copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = url,
                        imageLoader = api.imageLoader,
                        contentDescription = "附件图片 ${idx + 1}",
                        contentScale = ContentScale.Crop,
                        onSuccess = { if (idx == 0) onMediaLoaded() },
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isBlurred) Modifier.blur(20.dp) else Modifier)
                            .clickable {
                                if (isBlurred) onRevealItem() else onOpenViewer(items, idx)
                            },
                    )
                    if (isBlurred) {
                        Text(
                            text = "点击查看",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    } else if (idx == 0) {
                        Text(
                            text = "${items.size}P",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(
                                    if (discourseLightbox) Color.Black.copy(alpha = 0.65f) else accentColor.copy(alpha = 0.85f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared staged media thumbnail strip + merge/solo + spoiler chips (`ComposeTrayItem`).
 * Tapping any thumbnail opens `ImageMaskEditorScreen`.
 */
@Composable
internal fun DisguiseStagedMediaStrip(
    items: List<ComposeTrayItem>,
    api: ForHappyApiClient,
    accentColor: Color,
    mergeAll: Boolean,
    spoiler: Boolean,
    mergeLabel: String,
    soloLabel: String,
    spoilerLabel: String,
    onToggleMerge: (Boolean) -> Unit,
    onToggleSpoiler: (Boolean) -> Unit,
    onEditMask: (Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                val thumbModel: Any? = when {
                    !item.posterLocalPath.isNullOrBlank() -> File(item.posterLocalPath)
                    !item.maskedFilePath.isNullOrBlank() -> File(item.maskedFilePath)
                    !item.localFilePath.isNullOrBlank() -> File(item.localFilePath)
                    else -> item.keptRemoteUrl
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onEditMask(idx) },
                ) {
                    AsyncImage(
                        model = thumbModel,
                        imageLoader = api.imageLoader,
                        contentDescription = "待发送图片 ${idx + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { onRemoveItem(item.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "移除",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    Surface(
                        color = if (item.isMasked) accentColor else Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(3.dp),
                    ) {
                        Text(
                            text = if (item.isMasked) "已打码" else "点击打码",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (items.size > 1) {
                FilterChip(
                    selected = mergeAll,
                    onClick = { onToggleMerge(true) },
                    label = { Text(mergeLabel, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.14f),
                        selectedLabelColor = accentColor,
                    ),
                )
                FilterChip(
                    selected = !mergeAll,
                    onClick = { onToggleMerge(false) },
                    label = { Text(soloLabel, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.14f),
                        selectedLabelColor = accentColor,
                    ),
                )
            }
            FilterChip(
                selected = spoiler,
                onClick = { onToggleSpoiler(!spoiler) },
                label = { Text(spoilerLabel, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.14f),
                    selectedLabelColor = accentColor,
                ),
            )
        }
    }
}

@Composable
internal fun FeishuWorkGroupInputBar(
    groupTitle: String,
    api: ForHappyApiClient,
    text: String,
    onTextChange: (String) -> Unit,
    stagedItems: List<ComposeTrayItem>,
    mergeAll: Boolean,
    spoiler: Boolean,
    quotedMeme: MemeItem?,
    sending: Boolean,
    showEmojiDrawer: Boolean,
    aliasEnabled: Boolean,
    onToggleEmojiDrawer: () -> Unit,
    onPickPhotos: () -> Unit,
    onEditMask: (Int) -> Unit,
    onRemoveStaged: (String) -> Unit,
    onToggleMerge: (Boolean) -> Unit,
    onToggleSpoiler: (Boolean) -> Unit,
    onClearQuote: () -> Unit,
    onInsertPhrase: (String) -> Unit,
    onSend: () -> Unit,
) {
    val fc = rememberFeishuColors()
    Surface(
        color = fc.card,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = fc.line)
            if (quotedMeme != null) {
                val replyTarget = feishuSafeAuthor(quotedMeme.authorId ?: quotedMeme.id, quotedMeme.authorName ?: quotedMeme.authorUsername, aliasEnabled)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(fc.surfaceMuted)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(16.dp)
                            .background(fc.blue, RoundedCornerShape(2.dp)),
                    )
                    Text(
                        text = "回复 $replyTarget：${feishuMemePrimaryText(quotedMeme)}",
                        color = fc.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "取消回复",
                        tint = fc.muted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onClearQuote() },
                    )
                }
            }

            if (stagedItems.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                DisguiseStagedMediaStrip(
                    items = stagedItems,
                    api = api,
                    accentColor = fc.blue,
                    mergeAll = mergeAll,
                    spoiler = spoiler,
                    mergeLabel = "合并发送",
                    soloLabel = "逐张单发",
                    spoilerLabel = "内容需确认",
                    onToggleMerge = onToggleMerge,
                    onToggleSpoiler = onToggleSpoiler,
                    onEditMask = onEditMask,
                    onRemoveItem = onRemoveStaged,
                )
            }

            if (showEmojiDrawer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(fc.surfaceMuted)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DisguiseSanitizer.SAFE_DISGUISE_EMOJIS.forEach { emoji ->
                            Surface(
                                color = fc.card,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onInsertPhrase(emoji) },
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FEISHU_WORK_QUICK_PHRASES.forEach { phrase ->
                            Surface(
                                color = fc.card,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, fc.line),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onInsertPhrase(phrase) },
                            ) {
                                Text(
                                    text = phrase,
                                    color = fc.text,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.4.dp, fc.subText.copy(alpha = 0.78f), CircleShape)
                        .clickable { onToggleEmojiDrawer() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "快捷语音短语",
                        tint = fc.subText,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fc.surfaceMuted)
                        .padding(horizontal = 11.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (quotedMeme != null) "输入回复内容…" else "发送给 $groupTitle",
                            color = fc.muted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(color = fc.text, fontSize = 14.sp),
                        cursorBrush = SolidColor(fc.blue),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                IconButton(
                    onClick = onToggleEmojiDrawer,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEmotions,
                        contentDescription = "表情",
                        tint = if (showEmojiDrawer) fc.blue else fc.subText,
                        modifier = Modifier.size(22.dp),
                    )
                }

                val canSend = !sending && (text.isNotBlank() || stagedItems.isNotEmpty())
                if (canSend) {
                    Surface(
                        color = fc.blue,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSend() },
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "发送",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onPickPhotos,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = "发送图片或附件",
                            tint = fc.subText,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FeishuMemeMessageActionSheet(
    meme: MemeItem,
    api: ForHappyApiClient,
    onReactEmoji: (String) -> Unit,
    onQuickBoost: (String) -> Unit,
    onReplyQuote: () -> Unit,
    onOpenThread: () -> Unit,
    onToggleFav: () -> Unit,
    onNotify: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (LocalBossKeyActive.current) return
    val fc = rememberFeishuColors()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var boostInput by remember(meme.id) { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = fc.card,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DisguiseSanitizer.SAFE_DISGUISE_EMOJIS.forEach { emoji ->
                    Surface(
                        color = fc.surfaceMuted,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                onReactEmoji(emoji)
                                onDismiss()
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = fc.line)

            Text("快捷补充（群内附加回复）", color = fc.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FEISHU_WORK_QUICK_PHRASES.forEach { phrase ->
                    Surface(
                        color = fc.surfaceMuted,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                onQuickBoost(phrase)
                                onDismiss()
                            },
                    ) {
                        Text(
                            text = phrase,
                            color = fc.text,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = boostInput,
                    onValueChange = { if (it.length <= 24) boostInput = it },
                    placeholder = { Text("自定义短评（不超过24字）", fontSize = 13.sp, color = fc.muted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = fc.blue,
                        unfocusedBorderColor = fc.line,
                        focusedTextColor = fc.text,
                        unfocusedTextColor = fc.text,
                    ),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = boostInput.isNotBlank(),
                    onClick = {
                        onQuickBoost(boostInput.trim())
                        onDismiss()
                    },
                ) {
                    Text("发送", color = fc.blue, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = fc.line)

            FeishuActionSheetRow(
                icon = Icons.Rounded.Reply,
                label = "回复此消息",
                fc = fc,
                onClick = {
                    onReplyQuote()
                    onDismiss()
                },
            )
            FeishuActionSheetRow(
                icon = Icons.Rounded.Forum,
                label = "打开话题详情（${maxOf(meme.comments, meme.commentCount)} 条回复）",
                fc = fc,
                onClick = {
                    onOpenThread()
                    onDismiss()
                },
            )
            FeishuActionSheetRow(
                icon = if (meme.faved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                label = if (meme.faved) "取消收藏消息" else "收藏此消息",
                fc = fc,
                onClick = {
                    onToggleFav()
                    onDismiss()
                },
            )
            val firstMedia = meme.mediaItems.firstOrNull()?.resolvedFullUrl?.takeIf { it.isNotBlank() }
            if (firstMedia != null) {
                FeishuActionSheetRow(
                    icon = Icons.Rounded.Download,
                    label = "保存附件图片到手机相册",
                    fc = fc,
                    onClick = {
                        val urlToSave = MediaUrlRewriter.rewriteMediaUrl(firstMedia)
                        onDismiss()
                        scope.launch {
                            saveMediaToSystemGallery(context, api, urlToSave).fold(
                                onSuccess = { onNotify("已保存附件图片到相册") },
                                onFailure = { onNotify(it.message ?: "保存失败") },
                            )
                        }
                    },
                )
            }
            FeishuActionSheetRow(
                icon = Icons.Rounded.ContentCopy,
                label = "复制消息引用",
                fc = fc,
                onClick = {
                    clipboard.setText(AnnotatedString("#${meme.id} ${feishuMemePrimaryText(meme)}"))
                    onNotify("已复制消息链接")
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun FeishuActionSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    fc: FeishuThemePalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fc.text, modifier = Modifier.size(20.dp))
        Text(label, color = fc.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Full-screen Feishu `‹ 话题` detail screen showing root message, inline media,
 * safe Unicode reactions, boost pills, comment thread (`fetchMemeComments`),
 * and bottom `[ 回复话题… ] ☺ ➤` bar (`postMemeCommentFull`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeishuThreadScreen(
    rootMeme: MemeItem,
    api: ForHappyApiClient,
    blurAll: Boolean,
    aliasEnabled: Boolean,
    unblurredIds: Set<Long>,
    onRevealMeme: (Long) -> Unit,
    onOpenViewer: (List<MemeMediaItem>, Int) -> Unit,
    onReactMeme: (MemeItem, String) -> Unit,
    onMemeCommentAdded: (Long, Int) -> Unit,
    onClose: () -> Unit,
) {
    val fc = rememberFeishuColors()
    val scope = rememberCoroutineScope()
    var comments by remember(rootMeme.id) { mutableStateOf<List<MemeComment>>(emptyList()) }
    var loadingComments by remember(rootMeme.id) { mutableStateOf(true) }
    var draftReply by remember(rootMeme.id) { mutableStateOf("") }
    var replyToComment by remember(rootMeme.id) { mutableStateOf<MemeComment?>(null) }
    var sending by remember(rootMeme.id) { mutableStateOf(false) }
    var showEmojiRow by remember(rootMeme.id) { mutableStateOf(false) }
    var feedbackMsg by remember(rootMeme.id) { mutableStateOf<String?>(null) }

    BackHandler(onBack = onClose)

    fun reloadComments() {
        scope.launch {
            loadingComments = true
            val list = api.fetchMemeComments(rootMeme.id)
            comments = list
            onMemeCommentAdded(rootMeme.id, list.size)
            loadingComments = false
        }
    }

    LaunchedEffect(rootMeme.id) {
        reloadComments()
    }

    val rootAuthor = feishuSafeAuthor(rootMeme.authorId ?: rootMeme.id, rootMeme.authorName ?: rootMeme.authorUsername, aliasEnabled)
    val rootAvatarColor = FeishuDisguiseHelpers.imColor(rootAuthor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fc.bg)
            .statusBarsPadding(),
    ) {
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回群聊", tint = fc.text)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("话题", color = fc.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("${comments.size} 条回复 · 群内同步", color = fc.muted, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(color = fc.line)
            }
        }

        if (!feedbackMsg.isNullOrBlank()) {
            Surface(color = fc.blue.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = feedbackMsg.orEmpty(),
                    color = fc.blue,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "root_${rootMeme.id}") {
                Surface(
                    color = fc.card,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, fc.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(rootAvatarColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    FeishuDisguiseHelpers.avatarChars(rootAuthor),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rootAuthor, color = fc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    FeishuDisguiseHelpers.formatClock(rootMeme.createdAt),
                                    color = fc.muted,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        RichChatBodyView(
                            rawBody = feishuMemePrimaryText(rootMeme),
                            imageLoader = api.imageLoader,
                            textColor = fc.text,
                            showInlineImages = false,
                        )

                        DisguiseMediaStrip(
                            meme = rootMeme,
                            api = api,
                            blurAll = blurAll,
                            accentColor = fc.blue,
                            isItemUnblurred = rootMeme.id in unblurredIds,
                            onRevealItem = { onRevealMeme(rootMeme.id) },
                            onOpenViewer = onOpenViewer,
                        )

                        if (rootMeme.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                rootMeme.tags.take(4).forEach { tag ->
                                    Surface(color = fc.surfaceMuted, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "#${DisguiseSanitizer.sanitizeSummaryForDisguise(tag, AppDisguiseMode.FEISHU)}",
                                            color = fc.blue,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val quickReactSet = rootMeme.reactions.ifEmpty {
                                listOf(ChatReaction(emoji = "👍", count = 0))
                            }
                            quickReactSet.forEach { reaction ->
                                ReactionChip(
                                    emojiKey = reaction.emoji,
                                    count = reaction.count,
                                    mine = reaction.mine,
                                    imageLoader = api.imageLoader,
                                    onClick = { onReactMeme(rootMeme, reaction.emoji) },
                                )
                            }
                            DisguiseSanitizer.SAFE_DISGUISE_EMOJIS.take(3).forEach { emoji ->
                                if (quickReactSet.none { DisguiseSanitizer.mapStickerToSafeEmoji(it.emoji) == emoji }) {
                                    Surface(
                                        color = fc.surfaceMuted,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { onReactMeme(rootMeme, emoji) },
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "hdr") {
                Text(
                    text = if (loadingComments) "正在同步回复…" else "全部回复 (${comments.size})",
                    color = fc.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (!loadingComments && comments.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "暂无回复，在下方输入第一条同事回复",
                        color = fc.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            items(comments, key = { it.id }) { comment ->
                val cmAuthor = feishuSafeAuthor(comment.userId, comment.displayAuthor, aliasEnabled)
                val cmColor = FeishuDisguiseHelpers.imColor(cmAuthor)
                Surface(
                    color = fc.card,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { replyToComment = comment },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(cmColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                FeishuDisguiseHelpers.avatarChars(cmAuthor),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(cmAuthor, color = fc.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(FeishuDisguiseHelpers.formatClock(comment.createdAt), color = fc.muted, fontSize = 10.sp)
                            }
                            comment.replyTo?.let { parent ->
                                val parentText = DisguiseSanitizer.sanitizeSummaryForDisguise(parent.body.orEmpty(), AppDisguiseMode.FEISHU)
                                Text(
                                    text = "回复：${parentText.ifBlank { "上一条消息" }}",
                                    color = fc.muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(fc.surfaceMuted, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            RichChatBodyView(
                                rawBody = DisguiseSanitizer.sanitizeSummaryForDisguise(comment.body, AppDisguiseMode.FEISHU),
                                imageLoader = api.imageLoader,
                                textColor = fc.text,
                                showInlineImages = false,
                            )
                        }
                    }
                }
            }
        }

        Surface(
            color = fc.card,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Column {
                HorizontalDivider(color = fc.line)
                replyToComment?.let { target ->
                    val name = feishuSafeAuthor(target.userId, target.displayAuthor, aliasEnabled)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(fc.surfaceMuted)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "回复 $name",
                            color = fc.blue,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "取消回复",
                            tint = fc.muted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { replyToComment = null },
                        )
                    }
                }
                if (showEmojiRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(fc.surfaceMuted)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DisguiseSanitizer.SAFE_DISGUISE_EMOJIS.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { draftReply += emoji }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(fc.surfaceMuted)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        if (draftReply.isEmpty()) {
                            Text("回复话题…", color = fc.muted, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = draftReply,
                            onValueChange = { draftReply = it },
                            textStyle = TextStyle(color = fc.text, fontSize = 14.sp),
                            cursorBrush = SolidColor(fc.blue),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconButton(onClick = { showEmojiRow = !showEmojiRow }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEmotions,
                            contentDescription = "表情",
                            tint = if (showEmojiRow) fc.blue else fc.muted,
                        )
                    }
                    val canSend = !sending && draftReply.isNotBlank()
                    Surface(
                        color = if (canSend) fc.blue else fc.surfaceMuted,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSend) {
                                sending = true
                                feedbackMsg = null
                                val payloadText = draftReply.trim()
                                val parentId = replyToComment?.id
                                scope.launch {
                                    val result = api.postMemeCommentFull(
                                        memeId = rootMeme.id,
                                        body = payloadText,
                                        replyToId = parentId,
                                    )
                                    sending = false
                                    result.fold(
                                        onSuccess = {
                                            draftReply = ""
                                            replyToComment = null
                                            reloadComments()
                                        },
                                        onFailure = { err ->
                                            feedbackMsg = DisguiseSanitizer.sanitizeSummaryForDisguise(
                                                err.message ?: "回复发送失败，请确认已登录账号",
                                                AppDisguiseMode.FEISHU,
                                            )
                                        },
                                    )
                                }
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "发送回复",
                                tint = if (canSend) Color.White else fc.muted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal enum class FeishuSettingsPage {
    ROOT,
    GENERAL,
    INTERFACE_STYLE,
    PRIVACY,
    NOTIFICATIONS,
    ABOUT,
}

@Composable
internal fun FeishuSettingsOverlay(
    api: ForHappyApiClient,
    initialPage: FeishuSettingsPage = FeishuSettingsPage.ROOT,
    themeMode: AppThemeMode,
    privacyConfig: PrivacyStealthConfig,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    onChangeTheme: (AppThemeMode) -> Unit,
    onExitDisguise: () -> Unit,
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
    onClose: () -> Unit,
) {
    val fc = rememberFeishuColors()
    val context = LocalContext.current
    var page by remember(initialPage) { mutableStateOf(initialPage) }
    var showBossPreview by remember { mutableStateOf(false) }

    fun navigateUp() {
        page = when (page) {
            FeishuSettingsPage.ROOT -> {
                onClose()
                return
            }
            FeishuSettingsPage.INTERFACE_STYLE -> FeishuSettingsPage.GENERAL
            FeishuSettingsPage.GENERAL,
            FeishuSettingsPage.PRIVACY,
            FeishuSettingsPage.NOTIFICATIONS,
            FeishuSettingsPage.ABOUT,
            -> FeishuSettingsPage.ROOT
        }
    }

    BackHandler(enabled = !showBossPreview) { navigateUp() }

    if (showBossPreview) {
        BossDocScreen(
            longPressTitleEnabled = privacyConfig.bossExitLongPressTitle,
            tapCollabFiveEnabled = privacyConfig.bossExitTapCollabFive,
            doubleBackExitEnabled = privacyConfig.bossDoubleBack,
            hapticEnabled = privacyConfig.stealthHapticFeedback,
            api = api,
            isPreviewMode = true,
            onExit = { showBossPreview = false },
        )
        return
    }

    val title = when (page) {
        FeishuSettingsPage.ROOT -> "设置"
        FeishuSettingsPage.GENERAL -> "通用"
        FeishuSettingsPage.INTERFACE_STYLE -> "界面风格"
        FeishuSettingsPage.PRIVACY -> "隐私与安全"
        FeishuSettingsPage.NOTIFICATIONS -> "消息通知"
        FeishuSettingsPage.ABOUT -> "关于"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fc.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::navigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = fc.text)
                    }
                    Text(title, color = fc.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = fc.line)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (page) {
                FeishuSettingsPage.ROOT -> {
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            FeishuSettingsNavRow("通用", "界面风格、外观与显示设置", fc) {
                                page = FeishuSettingsPage.GENERAL
                            }
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSettingsNavRow("隐私与安全", "工作区遮罩、名称规范化与锁屏文档", fc) {
                                page = FeishuSettingsPage.PRIVACY
                            }
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSettingsNavRow("消息通知", "新消息与群组提醒方式", fc) {
                                page = FeishuSettingsPage.NOTIFICATIONS
                            }
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSettingsNavRow("关于", "客户端版本与工作区信息", fc) {
                                page = FeishuSettingsPage.ABOUT
                            }
                        }
                    }
                }

                FeishuSettingsPage.GENERAL -> {
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        FeishuSettingsNavRow(
                            title = "界面风格",
                            subtitle = "办公协作（当前）",
                            fc = fc,
                        ) {
                            page = FeishuSettingsPage.INTERFACE_STYLE
                        }
                    }
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("外观模式", color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("调整工作区亮暗配色方案", color = fc.muted, fontSize = 12.sp)
                            val neutralThemes = listOf(
                                AppThemeMode.FOLLOW_SYSTEM to "跟随系统",
                                AppThemeMode.WARM_PAPER_LIGHT to "浅色模式",
                                AppThemeMode.PINE_NIGHT_DARK to "深色模式",
                            )
                            neutralThemes.forEach { (mode, label) ->
                                val selected = themeMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) fc.blue.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { onChangeTheme(mode) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) fc.blue else fc.text,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = fc.blue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                FeishuSettingsPage.INTERFACE_STYLE -> {
                    Text(
                        text = "切换界面布局风格",
                        color = fc.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            FeishuStyleChoiceRow(
                                title = "默认风格",
                                subtitle = "标准四栏视图，返回完整标准布局",
                                selected = false,
                                fc = fc,
                                onClick = onExitDisguise,
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuStyleChoiceRow(
                                title = "办公协作（当前）",
                                subtitle = "即时消息、通讯录与工作群协同",
                                selected = true,
                                fc = fc,
                                onClick = {},
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuStyleChoiceRow(
                                title = "技术社区",
                                subtitle = "长帖楼层讨论视图，适配快速浏览",
                                selected = false,
                                fc = fc,
                                onClick = { onChangeDisguise(AppDisguiseMode.LINUX_DO) },
                            )
                        }
                    }
                }

                FeishuSettingsPage.PRIVACY -> {
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            FeishuSwitchRow(
                                title = "工作群附件默认保护遮罩",
                                subtitle = "进入工作区与滚动后自动隐藏图片细节，点击可查看",
                                checked = privacyConfig.blurAllEnabled,
                                fc = fc,
                                onCheckedChange = {
                                    onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = it))
                                },
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "联系人显示规范化工作姓名",
                                subtitle = "群内消息流与话题回复统一使用部门标准姓名",
                                checked = privacyConfig.feishuMemberAliasEnabled,
                                fc = fc,
                                onCheckedChange = {
                                    onUpdatePrivacyConfig(privacyConfig.copy(feishuMemberAliasEnabled = it))
                                },
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "快捷双击返回切换锁屏文档",
                                subtitle = "连续两次触发系统返回键时即时切换至只读会议纪要",
                                checked = privacyConfig.bossDoubleBack,
                                fc = fc,
                                onCheckedChange = {
                                    onUpdatePrivacyConfig(privacyConfig.copy(bossDoubleBack = it))
                                },
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "切入后台自动锁定为会议纪要",
                                subtitle = "离开应用再回来时先显示只读会议纪要",
                                checked = privacyConfig.bossOnBackgroundResume,
                                fc = fc,
                                onCheckedChange = {
                                    onUpdatePrivacyConfig(privacyConfig.copy(bossOnBackgroundResume = it))
                                },
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "锁屏文档返回时保留遮罩",
                                subtitle = "从只读会议纪要回到工作区后维持图片遮罩状态",
                                checked = privacyConfig.keepBlurAfterBossExit,
                                fc = fc,
                                onCheckedChange = {
                                    onUpdatePrivacyConfig(privacyConfig.copy(keepBlurAfterBossExit = it))
                                },
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "防截屏与最近任务缩略图保护",
                                subtitle = "开启 FLAG_SECURE，系统后台任务视图隐藏内容缩略图",
                                checked = privacyShieldEnabled,
                                fc = fc,
                                onCheckedChange = onTogglePrivacyShield,
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "手势切换触感反馈",
                                subtitle = "触发隐藏切换手势时提供轻微震动提示",
                                checked = privacyConfig.stealthHapticFeedback,
                                fc = fc,
                                onCheckedChange = {
                                    onUpdatePrivacyConfig(privacyConfig.copy(stealthHapticFeedback = it))
                                },
                            )
                        }
                    }

                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("快捷手势触发锁屏文档", color = fc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ShortcutGestureMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = privacyConfig.bossVolumeShortcut == mode,
                                        onClick = {
                                            onUpdatePrivacyConfig(privacyConfig.copy(bossVolumeShortcut = mode))
                                        },
                                        label = { Text(mode.label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = fc.blue,
                                            selectedLabelColor = Color.White,
                                        ),
                                    )
                                }
                            }
                            HorizontalDivider(color = fc.line)
                            Text("桌面应用图标样式", color = fc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppIconAlias.entries.forEach { alias ->
                                    FilterChip(
                                        selected = activeIconAlias == alias,
                                        onClick = {
                                            applyLauncherIconAlias(context, alias)
                                            onChangeIconAlias(alias)
                                        },
                                        label = { Text(safeIconAliasTitle(alias), fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = fc.blue,
                                            selectedLabelColor = Color.White,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        FeishuSettingsNavRow(
                            title = "锁屏文档样式预览",
                            subtitle = "查看只读会议纪要页面效果并测试长按标题返回手势",
                            fc = fc,
                            onClick = { showBossPreview = true },
                        )
                    }
                    Text(
                        text = "提示：进入只读会议纪要后，长按页面主标题 1.5 秒以上即可无声返回当前工作区。",
                        color = fc.muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                FeishuSettingsPage.NOTIFICATIONS -> {
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            FeishuSwitchRow(
                                title = "新消息弹窗提示",
                                subtitle = "收到同事私信或工作群回复时顶部轻提醒",
                                checked = true,
                                fc = fc,
                                onCheckedChange = {},
                            )
                            HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 16.dp))
                            FeishuSwitchRow(
                                title = "话题 @我 强提醒",
                                subtitle = "工作群内提及或引用消息时突出显示未读角标",
                                checked = true,
                                fc = fc,
                                onCheckedChange = {},
                            )
                        }
                    }
                }

                FeishuSettingsPage.ABOUT -> {
                    Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "企业协同办公客户端",
                                color = fc.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "版本号：v${BuildConfig.VERSION_NAME}",
                                color = fc.muted,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = "当前工作区：产品研发协同中心 · 已接入即时消息与云端话题同步服务",
                                color = fc.muted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeishuSettingsNavRow(
    title: String,
    subtitle: String,
    fc: FeishuThemePalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = fc.muted, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = fc.muted,
        )
    }
}

@Composable
private fun FeishuStyleChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    fc: FeishuThemePalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = if (selected) fc.blue else fc.text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(subtitle, color = fc.muted, fontSize = 12.sp)
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "当前使用", tint = fc.blue, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FeishuSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    fc: FeishuThemePalette,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = fc.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = fc.muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
