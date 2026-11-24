package com.jinhuan.forhappy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.BOOST_PRESETS
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.disguise.DisguiseSanitizer
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Circular avatar: uses user's real avatar URL when present, falling back to `app.js` hueOf
 * hash-tinted colored circle + first initial character if absent or if image loading fails.
 */
@Composable
fun ForHappyAvatar(
    username: String,
    avatarUrl: String?,
    imageLoader: ImageLoader,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val stealth = LocalPrivacyStealthConfig.current
    val hideAvatarForFeishu = stealth.disguiseMode == AppDisguiseMode.FEISHU && stealth.feishuMemberAliasEnabled
    val display = username.ifBlank { "?" }
    val initial = remember(display) { display.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
    val bgTint = remember(display, palette.isDark) {
        val hue = display.fold(7) { acc, ch -> (acc * 31 + ch.code) % 360 }.toFloat()
        Color.hsv(hue, if (palette.isDark) 0.45f else 0.35f, if (palette.isDark) 0.42f else 0.90f)
    }
    var imageLoaded by remember(avatarUrl, hideAvatarForFeishu) { mutableStateOf(false) }
    var loadFailed by remember(avatarUrl, hideAvatarForFeishu) { mutableStateOf(false) }
    val avatarSpec = remember(avatarUrl) { MediaUrlRewriter.parseAvatarViewSpec(avatarUrl) }
    val effectiveUrl = remember(avatarUrl, avatarSpec) {
        avatarSpec?.cleanUrl ?: MediaUrlRewriter.rewriteWithExtProxy(avatarUrl)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgTint)
            .border(
                width = if (palette.isModernUi) 0.5.dp else 1.dp,
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                shape = CircleShape,
            ),
    ) {
        if (!imageLoaded || hideAvatarForFeishu) {
            Text(
                text = initial,
                color = palette.text,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (!hideAvatarForFeishu && effectiveUrl.isNotBlank() && !loadFailed) {
            val imageModifier = if (avatarSpec != null) {
                Modifier
                    .requiredSize(
                        width = size * (avatarSpec.width * avatarSpec.scale),
                        height = size * (avatarSpec.height * avatarSpec.scale),
                    )
                    .graphicsLayer {
                        val boxPx = size.toPx()
                        translationX = avatarSpec.tx * boxPx
                        translationY = avatarSpec.ty * boxPx
                        rotationZ = avatarSpec.rotationDeg.toFloat()
                    }
            } else {
                Modifier.fillMaxSize()
            }
            AsyncImage(
                model = effectiveUrl,
                imageLoader = imageLoader,
                contentDescription = display,
                contentScale = if (avatarSpec != null) ContentScale.FillBounds else ContentScale.Crop,
                onSuccess = { imageLoaded = true },
                onError = {
                    loadFailed = true
                    imageLoaded = false
                },
                modifier = imageModifier,
            )
        }
    }
}

fun formatUnreadBadgeCount(count: Int): String =
    if (count > 99) "99+" else count.coerceAtLeast(0).toString()

fun formatMentionOrCountBadge(count: Int, isMention: Boolean): String = when {
    isMention && count > 0 -> "@${formatUnreadBadgeCount(count)}"
    isMention -> "@我"
    else -> formatUnreadBadgeCount(count)
}

/**
 * Unclipped, vertically-centered pill/circle numeric or status badge.
 * Uses tight `lineHeight` + `includeFontPadding = false` + `LineHeightStyle.Alignment.Center`
 * so digits do not shift upward inside compact capsules.
 */
@Composable
fun ForHappyCountBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color = Color.White,
    minSize: Dp = 16.dp,
    fontSize: TextUnit = 9.5.sp,
    horizontalPadding: Dp = if (text.length <= 1) 0.dp else 4.5.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
) {
    val palette = LocalForHappyPalette.current
    val bg = containerColor ?: palette.badge
    val shape = RoundedCornerShape(999.dp)
    val outerModifier = if (borderWidth > 0.dp && borderColor != Color.Transparent) {
        modifier
            .background(borderColor, shape)
            .padding(borderWidth)
    } else {
        modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = outerModifier
            .defaultMinSize(minWidth = minSize, minHeight = minSize)
            .clip(shape)
            .background(bg)
            .padding(horizontal = horizontalPadding),
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize,
            lineHeight = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                fontFeatureSettings = TABULAR_FONT_FEATURE,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
            ),
        )
    }
}

/**
 * Top-bar circular notification bell button whose badge sits outside the clipped circle background
 * so multi-digit unread pills are never sliced at the button boundary.
 */
@Composable
fun ForHappyBellIconButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp? = null,
    icon: ImageVector = Icons.Rounded.NotificationsNone,
    iconSize: Dp? = null,
    iconTint: Color? = null,
    contentDescription: String = "通知",
) {
    val palette = LocalForHappyPalette.current
    val isModern = palette.isModernUi
    val resolvedButtonSize = buttonSize ?: if (isModern) 36.dp else 40.dp
    val resolvedIconSize = iconSize ?: if (isModern) 19.dp else 21.dp
    val resolvedTint = iconTint ?: if (unreadCount > 0) palette.text else palette.muted
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(resolvedButtonSize),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .let { if (isModern) it.background(palette.surface2.copy(alpha = 0.78f)) else it }
                .clickable(onClick = onClick),
        )
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier.size(resolvedIconSize),
            )
            if (unreadCount > 0) {
                ForHappyCountBadge(
                    text = formatUnreadBadgeCount(unreadCount),
                    containerColor = palette.badge,
                    minSize = 15.dp,
                    fontSize = 9.sp,
                    horizontalPadding = if (unreadCount < 10) 0.dp else 4.dp,
                    modifier = Modifier.offset(x = 6.dp, y = (-4).dp),
                )
            }
        }
    }
}

@Composable
fun BadgePill(
    text: String,
    containerColor: Color? = null,
    contentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = containerColor ?: palette.surface2,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = contentColor ?: palette.muted,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/**
 * Renders either a Unicode reaction (`❤️`) or a platform colon-key sticker (`ld:distorted_face`,
 * `heartgif:heartgif_001`) as an actual sticker image from `/stickers/{pack}/{name}.{ext}`.
 */
@Composable
fun ReactionEmojiView(
    emojiKey: String,
    imageLoader: ImageLoader,
    size: Dp = 18.dp,
    fontSize: TextUnit = 13.sp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val disguised = LocalPrivacyStealthConfig.current.disguiseMode != AppDisguiseMode.NONE
    val colonIdx = emojiKey.indexOf(':')
    when {
        disguised && colonIdx > 0 -> {
            Text(
                text = DisguiseSanitizer.mapStickerToSafeEmoji(emojiKey),
                fontSize = fontSize,
                color = palette.text,
                modifier = modifier,
            )
        }
        emojiKey == "+1" -> {
            Text(
                text = "+1",
                color = palette.primary,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = modifier,
            )
        }
        colonIdx > 0 && colonIdx < emojiKey.lastIndex -> {
            val pack = emojiKey.substring(0, colonIdx)
            val name = emojiKey.substring(colonIdx + 1)
            val baseUrl = remember(emojiKey) { MediaUrlRewriter.stickerUrl(pack, name) }
            var retryAttempt by remember(emojiKey) { mutableIntStateOf(0) }
            var pendingRetry by remember(emojiKey) { mutableStateOf(false) }
            var failed by remember(emojiKey) { mutableStateOf(false) }
            LaunchedEffect(emojiKey, pendingRetry) {
                if (pendingRetry) {
                    delay(1800L)
                    retryAttempt++
                    pendingRetry = false
                }
            }
            if (failed) {
                Text(
                    text = name,
                    color = palette.text,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier,
                )
            } else {
                val reqUrl = if (retryAttempt == 0) baseUrl else "$baseUrl?retry=$retryAttempt"
                AsyncImage(
                    model = reqUrl,
                    imageLoader = imageLoader,
                    contentDescription = emojiKey,
                    contentScale = ContentScale.Fit,
                    onError = {
                        if (!pendingRetry) {
                            if (retryAttempt < 1) {
                                pendingRetry = true
                            } else {
                                failed = true
                            }
                        }
                    },
                    modifier = modifier.size(size),
                )
            }
        }
        else -> {
            Text(
                text = emojiKey,
                fontSize = fontSize,
                color = palette.text,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun ReactionChip(
    emojiKey: String,
    count: Int,
    mine: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = when {
            palette.isModernUi && mine -> palette.primaryContainer
            mine -> palette.primary.copy(alpha = 0.18f)
            else -> palette.surface2
        },
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
            .then(
                when {
                    palette.isModernUi && mine -> Modifier.border(
                        width = 1.dp,
                        color = palette.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    palette.isModernUi -> Modifier
                    else -> Modifier.border(
                        width = 1.dp,
                        color = if (mine) palette.primary.copy(alpha = 0.50f) else palette.line.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(999.dp),
                    )
                },
            )
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 2.5.dp else 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            ReactionEmojiView(
                emojiKey = emojiKey,
                imageLoader = imageLoader,
                size = if (compact) 15.dp else 18.dp,
                fontSize = if (compact) 12.sp else 13.sp,
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    color = if (mine) palette.primary else palette.text,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = if (mine) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

fun boostPresetAccent(text: String, isDark: Boolean): Color? {
    val idx = BOOST_PRESETS.indexOf(text)
    if (idx < 0) return null
    val lightPalette = listOf(
        Color(0xFF1B7F4B), // 善
        Color(0xFF2B6CB0), // 甚好
        Color(0xFFD97706), // 欢欢
        Color(0xFFDB2777), // 妈妈
        Color(0xFF7C3AED), // 666
        Color(0xFFDC2626), // 太地狱了
        Color(0xFFB45309), // 神！
        Color(0xFF0F766E), // 挂！
        Color(0xFF2563EB), // 我去~
        Color(0xFF9333EA), // 大大大
        Color(0xFFE11D48), // 电！！
        Color(0xFF4F46E5), // 不讲不讲
        Color(0xFFC2410C), // 魔了
    )
    val darkPalette = listOf(
        Color(0xFF6EE7B7),
        Color(0xFF93C5FD),
        Color(0xFFFCD34D),
        Color(0xFFF9A8D4),
        Color(0xFFC4B5FD),
        Color(0xFFFCA5A5),
        Color(0xFFFDE68A),
        Color(0xFF5EEAD4),
        Color(0xFF93C5FD),
        Color(0xFFD8B4FE),
        Color(0xFFFDA4AF),
        Color(0xFFA5B4FC),
        Color(0xFFFDBA74),
    )
    val list = if (isDark) darkPalette else lightPalette
    return list[idx % list.size]
}

/**
 * Renders short boost / 1-line quick reply text, supporting both official `BOOST_PRESETS` accents
 * and inline `[pack:name]` stickers or `:shortcode:` Twemoji.
 */
@Composable
fun BoostPillText(
    text: String,
    imageLoader: ImageLoader,
    fontSize: TextUnit = 11.sp,
    textColor: Color? = null,
    frameFillColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val disguised = LocalPrivacyStealthConfig.current.disguiseMode != AppDisguiseMode.NONE
    val presetAccent = remember(text, palette.isDark, disguised, frameFillColor) {
        val accent = boostPresetAccent(text, palette.isDark)
        when {
            accent == null -> null
            disguised -> palette.primary
            frameFillColor != null -> ChatBubbleCosmetics.ensureReadableForeground(accent, frameFillColor, minContrast = 4.0f)
            else -> accent
        }
    }
    if (presetAccent != null) {
        Text(
            text = text,
            color = presetAccent,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    val tokens = remember(text) { ChatRichTextParser.tokenizeInline(text) }
    val hasRichMedia = remember(tokens, disguised) {
        !disguised && tokens.any { it is ChatRichTextParser.Token.Sticker || it is ChatRichTextParser.Token.Twemoji }
    }
    if (!hasRichMedia && !tokens.any { it is ChatRichTextParser.Token.Sticker || it is ChatRichTextParser.Token.Twemoji }) {
        Text(
            text = text,
            color = textColor ?: palette.text,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    val iconDp = if (fontSize.value <= 11f) 15.dp else 18.dp
    val iconSp = if (fontSize.value <= 11f) 16.sp else 19.sp
    var failedInlineKeys by remember(tokens) { mutableStateOf(emptySet<String>()) }
    val inlineContent = remember(tokens, failedInlineKeys, iconDp, iconSp, palette.muted, disguised) {
        if (disguised) {
            emptyMap()
        } else {
            buildInlineMediaMap(
                tokens = tokens,
                keyPrefix = "bst",
                failedKeys = failedInlineKeys,
                stickerDp = iconDp,
                stickerSp = iconSp,
                twemojiDp = iconDp,
                twemojiSp = iconSp,
                imageLoader = imageLoader,
                onPermanentError = { key -> failedInlineKeys = failedInlineKeys + key },
            )
        }
    }
    val annotated = remember(tokens, failedInlineKeys, textColor, palette, disguised) {
        buildAnnotatedString {
            tokens.forEachIndexed { idx, tk ->
                when (tk) {
                    is ChatRichTextParser.Token.Sticker -> {
                        if (disguised) {
                            append(DisguiseSanitizer.mapStickerToSafeEmoji("[${tk.pack}:${tk.name}]"))
                        } else {
                            val key = "bst_stk_$idx"
                            if (key in failedInlineKeys) {
                                withStyle(SpanStyle(color = palette.muted)) { append("[${tk.pack}:${tk.name}]") }
                            } else {
                                appendInlineContent(key, "[${tk.pack}:${tk.name}]")
                            }
                        }
                    }
                    is ChatRichTextParser.Token.Twemoji -> {
                        if (disguised) {
                            append(DisguiseSanitizer.mapStickerToSafeEmoji(":${tk.code}:"))
                        } else {
                            val key = "bst_twe_$idx"
                            if (key in failedInlineKeys) {
                                withStyle(SpanStyle(color = palette.muted)) { append(":${tk.code}:") }
                            } else {
                                appendInlineContent(key, ":${tk.code}:")
                            }
                        }
                    }
                    is ChatRichTextParser.Token.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(tk.text) }
                    is ChatRichTextParser.Token.PlainText -> append(tk.text)
                    is ChatRichTextParser.Token.PostRef -> append(tk.label)
                    is ChatRichTextParser.Token.Hashtag -> append("#${tk.tag}")
                    is ChatRichTextParser.Token.Mention -> append("@${tk.username}")
                    is ChatRichTextParser.Token.InlineCode -> append(tk.code)
                    is ChatRichTextParser.Token.Strike -> append(tk.text)
                    is ChatRichTextParser.Token.Spoiler -> append(tk.text)
                    is ChatRichTextParser.Token.MarkdownLink -> append(tk.label)
                    is ChatRichTextParser.Token.WebLink -> append(tk.url)
                }
            }
        }
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        color = textColor ?: palette.text,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Pull-down-to-dismiss wrapper for modal image viewer & sheets (matches modern thumb ergonomics).
 */
@Composable
fun PullToDismissBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var triggeredHaptic by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.roundToInt().coerceAtLeast(0)) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 190f) {
                            onDismiss()
                        } else {
                            dragOffset = 0f
                            triggeredHaptic = 0f
                        }
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        triggeredHaptic = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val next = (dragOffset + dragAmount).coerceAtLeast(0f)
                        dragOffset = next
                        if (next > 190f && triggeredHaptic == 0f) {
                            triggeredHaptic = 1f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                )
            },
    ) {
        content()
    }
}

private fun estimateInlineTokensLines(tokens: List<ChatRichTextParser.Token>): Int {
    val sb = StringBuilder()
    tokens.forEach { t ->
        when (t) {
            is ChatRichTextParser.Token.PlainText -> sb.append(t.text)
            is ChatRichTextParser.Token.Bold -> sb.append(t.text)
            is ChatRichTextParser.Token.Strike -> sb.append(t.text)
            is ChatRichTextParser.Token.InlineCode -> sb.append(t.code)
            is ChatRichTextParser.Token.Spoiler -> sb.append(t.text)
            is ChatRichTextParser.Token.Mention -> sb.append("@").append(t.username)
            is ChatRichTextParser.Token.PostRef -> sb.append(t.label)
            is ChatRichTextParser.Token.Hashtag -> sb.append("#").append(t.tag)
            is ChatRichTextParser.Token.MarkdownLink -> sb.append(t.label)
            is ChatRichTextParser.Token.WebLink -> sb.append(t.url)
            is ChatRichTextParser.Token.Sticker,
            is ChatRichTextParser.Token.Twemoji -> sb.append("□□")
        }
    }
    val text = sb.toString()
    if (text.isBlank()) return 0
    return text.split('\n').sumOf { seg ->
        if (seg.isEmpty()) 1 else ((seg.length - 1) / 18) + 1
    }
}

/** Estimates visual lines of a parsed chat body inside ~296dp group chat bubbles. */
fun estimateChatBodyVisualLines(parsed: ChatRichTextParser.ParsedBody): Int {
    if (parsed.blocks.isEmpty()) return 0
    return parsed.blocks.sumOf { block ->
        when (block) {
            is ChatRichTextParser.Block.Paragraph -> estimateInlineTokensLines(block.tokens).coerceAtLeast(1)
            is ChatRichTextParser.Block.BlockQuote -> estimateInlineTokensLines(block.tokens).coerceAtLeast(1) + 1
            is ChatRichTextParser.Block.CodeBlock -> block.code.lines().sumOf { line ->
                if (line.isEmpty()) 1 else ((line.length - 1) / 28) + 1
            }.coerceAtLeast(1) + 1
            is ChatRichTextParser.Block.QuoteBlock -> 2 + estimateInlineTokensLines(block.bodyTokens).coerceAtMost(3)
            is ChatRichTextParser.Block.DetailsBlock -> 2
        }
    }
}

/** Returns true when a chat message is long enough to merit default folding in group chat. */
fun shouldCollapseChatBody(
    parsed: ChatRichTextParser.ParsedBody,
    maxLines: Int = 8,
): Boolean {
    if (parsed.bodyImgOnly) return false
    if (parsed.blocks.size == 1) {
        val single = parsed.blocks.first()
        if (single is ChatRichTextParser.Block.Paragraph &&
            single.tokens.count { it is ChatRichTextParser.Token.Sticker } in 1..6 &&
            single.tokens.all {
                it is ChatRichTextParser.Token.Sticker ||
                    (it is ChatRichTextParser.Token.PlainText && it.text.isBlank())
            }
        ) {
            return false
        }
    }
    return estimateChatBodyVisualLines(parsed) > maxLines
}

/**
 * Full rich text & lite-Markdown renderer for `52dmeme.com` messages, comments, and descriptions.
 */
@Composable
fun RichChatBodyView(
    rawBody: String,
    imageLoader: ImageLoader,
    textColor: Color? = null,
    linkColor: Color? = null,
    mutedColor: Color? = null,
    bodyFontFamily: FontFamily? = null,
    bodyFontWeight: FontWeight? = null,
    bodyFontStyle: FontStyle? = null,
    isEdited: Boolean = false,
    showInlineImages: Boolean = true,
    collapsible: Boolean = false,
    collapsedMaxLines: Int = 8,
    collapseStateKey: Any? = null,
    onPostRefClick: (Long) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onQuoteClick: ((Long) -> Unit)? = null,
    onInlineImageClick: ((String) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val effectiveTextColor = textColor ?: palette.text
    val effectiveLinkColor = linkColor ?: palette.primary
    val effectiveMutedColor = mutedColor ?: palette.muted
    val parsed = remember(rawBody) { ChatRichTextParser.parse(rawBody) }

    val onlyInlineImage = parsed.bodyImgOnly && showInlineImages && parsed.inlineImageUrls.isNotEmpty()
    val onlyParagraphIdx = remember(parsed.blocks) {
        parsed.blocks.indexOfLast { it is ChatRichTextParser.Block.Paragraph }
    }
    val foldKey = collapseStateKey?.toString() ?: rawBody
    var isExpanded by rememberSaveable(foldKey) { mutableStateOf(false) }
    var observedOverflow by remember(foldKey, rawBody) { mutableStateOf(false) }
    val estimatedOverBudget = remember(parsed, collapsible, collapsedMaxLines) {
        collapsible && !onlyInlineImage && shouldCollapseChatBody(parsed, collapsedMaxLines)
    }

    var hiddenByBudget = false

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (!onlyInlineImage) {
            var remainingLines = if (collapsible && !isExpanded) collapsedMaxLines.coerceAtLeast(3) else Int.MAX_VALUE
            for ((bIdx, block) in parsed.blocks.withIndex()) {
                if (collapsible && !isExpanded && remainingLines <= 0) {
                    hiddenByBudget = true
                    break
                }
                when (block) {
                    is ChatRichTextParser.Block.QuoteBlock -> {
                        val jumpMemeId = block.memeId
                        val jumpMsgId = block.messageId
                        val canJump = (jumpMsgId != null && onQuoteClick != null) || (jumpMemeId != null)
                        Surface(
                            color = palette.surface2.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .clickable(enabled = canJump) {
                                    when {
                                        jumpMemeId != null -> onPostRefClick(jumpMemeId)
                                        jumpMsgId != null -> onQuoteClick?.invoke(jumpMsgId)
                                    }
                                },
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .heightIn(min = 18.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(palette.primary.copy(alpha = 0.75f)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = block.author?.takeIf { it.isNotBlank() }?.let { "$it：" } ?: "引用：",
                                            color = palette.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (canJump) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = "跳到原文",
                                                tint = palette.primary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                    if (block.bodyTokens.isNotEmpty()) {
                                        InlineTokensText(
                                            tokens = block.bodyTokens,
                                            imageLoader = imageLoader,
                                            textColor = palette.muted,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            stickerSize = 18.dp,
                                            stickerSp = 20.sp,
                                            linkColor = palette.primary,
                                            mutedColor = palette.muted,
                                            maxLines = if (collapsible && !isExpanded) 3 else Int.MAX_VALUE,
                                            overflow = TextOverflow.Ellipsis,
                                            onPostRefClick = onPostRefClick,
                                            onHashtagClick = onHashtagClick,
                                            onLinkClick = { url ->
                                                val mid = ChatRichTextParser.extractInternalMemeId(url)
                                                if (mid != null) onPostRefClick(mid) else onLinkClick(url)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        if (remainingLines < Int.MAX_VALUE) {
                            remainingLines -= (2 + estimateInlineTokensLines(block.bodyTokens).coerceAtMost(3))
                        }
                    }

                    is ChatRichTextParser.Block.DetailsBlock -> {
                        var expanded by remember(rawBody, bIdx) { mutableStateOf(false) }
                        Surface(
                            color = palette.surface2.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = !expanded },
                                ) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = block.summary,
                                        color = palette.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (expanded && block.bodyTokens.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    InlineTokensText(
                                        tokens = block.bodyTokens,
                                        imageLoader = imageLoader,
                                        textColor = effectiveTextColor,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        stickerSize = 20.dp,
                                        stickerSp = 22.sp,
                                        linkColor = effectiveLinkColor,
                                        mutedColor = effectiveMutedColor,
                                        fontFamily = bodyFontFamily,
                                        fontWeight = bodyFontWeight,
                                        fontStyle = bodyFontStyle,
                                        onPostRefClick = onPostRefClick,
                                        onHashtagClick = onHashtagClick,
                                        onLinkClick = { url ->
                                            val mid = ChatRichTextParser.extractInternalMemeId(url)
                                            if (mid != null) onPostRefClick(mid) else onLinkClick(url)
                                        },
                                    )
                                }
                            }
                        }
                        if (remainingLines < Int.MAX_VALUE) remainingLines -= 2
                    }

                    is ChatRichTextParser.Block.CodeBlock -> {
                        val allLines = remember(block.code) { block.code.lines() }
                        val capLines = if (collapsible && !isExpanded) remainingLines.coerceAtLeast(3) else allLines.size
                        val codeText = if (collapsible && !isExpanded && allLines.size > capLines) {
                            hiddenByBudget = true
                            allLines.take(capLines).joinToString("\n") + "\n…"
                        } else {
                            block.code
                        }
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                        ) {
                            Text(
                                text = codeText,
                                color = effectiveTextColor,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 17.sp,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                        if (remainingLines < Int.MAX_VALUE) {
                            remainingLines -= minOf(allLines.size, capLines) + 1
                        }
                    }

                    is ChatRichTextParser.Block.BlockQuote -> {
                        val bqMax = if (collapsible && !isExpanded) remainingLines.coerceAtLeast(2) else Int.MAX_VALUE
                        val estLines = estimateInlineTokensLines(block.tokens).coerceAtLeast(1)
                        if (collapsible && !isExpanded && estLines > bqMax) hiddenByBudget = true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surface2.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .heightIn(min = 16.dp)
                                    .background(effectiveMutedColor.copy(alpha = 0.55f), RoundedCornerShape(999.dp)),
                            )
                            Spacer(Modifier.width(7.dp))
                            InlineTokensText(
                                tokens = block.tokens,
                                imageLoader = imageLoader,
                                textColor = effectiveMutedColor,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                stickerSize = 20.dp,
                                stickerSp = 22.sp,
                                linkColor = effectiveLinkColor,
                                mutedColor = effectiveMutedColor,
                                fontFamily = bodyFontFamily,
                                fontWeight = bodyFontWeight,
                                fontStyle = bodyFontStyle,
                                maxLines = bqMax,
                                overflow = if (bqMax < Int.MAX_VALUE) TextOverflow.Ellipsis else TextOverflow.Clip,
                                onVisualOverflow = { over -> if (over) observedOverflow = true },
                                onPostRefClick = onPostRefClick,
                                onHashtagClick = onHashtagClick,
                                onLinkClick = { url ->
                                    val mid = ChatRichTextParser.extractInternalMemeId(url)
                                    if (mid != null) onPostRefClick(mid) else onLinkClick(url)
                                },
                            )
                        }
                        if (remainingLines < Int.MAX_VALUE) {
                            remainingLines -= minOf(estLines, bqMax) + 1
                        }
                    }

                    is ChatRichTextParser.Block.Paragraph -> {
                        val stickerCount = block.tokens.count { it is ChatRichTextParser.Token.Sticker }
                        val isSoloStickers = parsed.blocks.size == 1 &&
                            stickerCount in 1..6 &&
                            block.tokens.all {
                                it is ChatRichTextParser.Token.Sticker ||
                                    (it is ChatRichTextParser.Token.PlainText && it.text.isBlank())
                            }
                        val soloSizeDp = if (stickerCount == 1) 64.dp else if (stickerCount <= 3) 48.dp else 38.dp
                        val soloSp = if (stickerCount == 1) 66.sp else if (stickerCount <= 3) 50.sp else 40.sp
                        val paraMax = if (collapsible && !isExpanded && !isSoloStickers) remainingLines.coerceAtLeast(1) else Int.MAX_VALUE
                        val estLines = estimateInlineTokensLines(block.tokens).coerceAtLeast(1)
                        if (collapsible && !isExpanded && !isSoloStickers && estLines > paraMax) hiddenByBudget = true
                        InlineTokensText(
                            tokens = block.tokens,
                            imageLoader = imageLoader,
                            textColor = effectiveTextColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            stickerSize = if (isSoloStickers) soloSizeDp else 22.dp,
                            stickerSp = if (isSoloStickers) soloSp else 24.sp,
                            appendEditedBadge = isEdited && bIdx == onlyParagraphIdx,
                            linkColor = effectiveLinkColor,
                            mutedColor = effectiveMutedColor,
                            fontFamily = bodyFontFamily,
                            fontWeight = bodyFontWeight,
                            fontStyle = bodyFontStyle,
                            maxLines = paraMax,
                            overflow = if (paraMax < Int.MAX_VALUE) TextOverflow.Ellipsis else TextOverflow.Clip,
                            onVisualOverflow = { over -> if (over) observedOverflow = true },
                            onPostRefClick = onPostRefClick,
                            onHashtagClick = onHashtagClick,
                            onLongPress = onLongPress,
                            onLinkClick = { url ->
                                val mid = ChatRichTextParser.extractInternalMemeId(url)
                                if (mid != null) onPostRefClick(mid) else onLinkClick(url)
                            },
                        )
                        if (remainingLines < Int.MAX_VALUE) {
                            remainingLines -= minOf(estLines, paraMax)
                        }
                    }
                }
            }
        }

        if (isEdited && onlyParagraphIdx < 0) {
            Text(
                text = "（已编辑）",
                color = effectiveMutedColor,
                fontSize = 11.sp,
            )
        }

        val showFoldToggle = collapsible && !onlyInlineImage &&
            (observedOverflow || hiddenByBudget || estimatedOverBudget)
        if (showFoldToggle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (isExpanded) "收起长消息" else "展开全文",
                    color = effectiveLinkColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "收起长消息" else "展开全文",
                    tint = effectiveLinkColor,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        if (showInlineImages && parsed.inlineImageUrls.isNotEmpty()) {
            val stealthConfig = LocalPrivacyStealthConfig.current
            parsed.inlineImageUrls.forEach { rawUrl ->
                val imgUrl = MediaUrlRewriter.rewriteWithExtProxy(rawUrl)
                var unblurred by remember(rawUrl, stealthConfig.blurAllEnabled) { mutableStateOf(false) }
                if (stealthConfig.autoReblurOnScrollOff) {
                    DisposableEffect(rawUrl) {
                        onDispose { unblurred = false }
                    }
                }
                val isBlurred = stealthConfig.blurAllEnabled && !unblurred
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface2)
                        .then(
                            if (isBlurred) {
                                Modifier.border(0.5.dp, palette.line.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            },
                        )
                        .clickable {
                            if (isBlurred) unblurred = true
                            else onInlineImageClick?.invoke(imgUrl)
                        },
                ) {
                    AsyncImage(
                        model = imgUrl,
                        imageLoader = imageLoader,
                        contentDescription = "正文图片链接",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .heightIn(max = 240.dp)
                            .let { if (isBlurred) it.blur(20.dp) else it },
                    )
                    if (isBlurred) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.06f)),
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.padding(horizontal = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "已模糊 · 轻触显示",
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineTokensText(
    tokens: List<ChatRichTextParser.Token>,
    imageLoader: ImageLoader,
    textColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    stickerSize: Dp,
    stickerSp: TextUnit,
    appendEditedBadge: Boolean = false,
    linkColor: Color? = null,
    mutedColor: Color? = null,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onVisualOverflow: ((Boolean) -> Unit)? = null,
    onPostRefClick: (Long) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
    val disguised = disguiseMode != AppDisguiseMode.NONE
    val effectiveLink = linkColor ?: palette.primary
    val effectiveMuted = mutedColor ?: palette.muted
    var revealedSpoilers by remember(tokens) { mutableStateOf(emptySet<Int>()) }
    var failedInlineKeys by remember(tokens) { mutableStateOf(emptySet<String>()) }

    val inlineContent = remember(tokens, failedInlineKeys, stickerSize, stickerSp, palette.muted, disguised) {
        if (disguised) {
            emptyMap()
        } else {
            buildInlineMediaMap(
                tokens = tokens,
                keyPrefix = "msg",
                failedKeys = failedInlineKeys,
                stickerDp = stickerSize,
                stickerSp = stickerSp,
                twemojiDp = 19.dp,
                twemojiSp = 20.sp,
                imageLoader = imageLoader,
                onPermanentError = { key -> failedInlineKeys = failedInlineKeys + key },
            )
        }
    }

    val annotated = remember(tokens, failedInlineKeys, textColor, effectiveLink, effectiveMuted, palette, revealedSpoilers, appendEditedBadge, disguiseMode) {
        buildAnnotatedString {
            tokens.forEachIndexed { idx, token ->
                when (token) {
                    is ChatRichTextParser.Token.PlainText -> append(token.text)
                    is ChatRichTextParser.Token.Sticker -> {
                        if (disguised) {
                            append(DisguiseSanitizer.mapStickerToSafeEmoji("[${token.pack}:${token.name}]"))
                        } else {
                            val key = "msg_stk_$idx"
                            if (key in failedInlineKeys) {
                                withStyle(SpanStyle(color = effectiveMuted)) {
                                    append("[${token.pack}:${token.name}]")
                                }
                            } else {
                                appendInlineContent(key, "[${token.pack}:${token.name}]")
                            }
                        }
                    }
                    is ChatRichTextParser.Token.Twemoji -> {
                        if (disguised) {
                            append(DisguiseSanitizer.mapStickerToSafeEmoji(":${token.code}:"))
                        } else {
                            val key = "msg_twe_$idx"
                            if (key in failedInlineKeys) {
                                withStyle(SpanStyle(color = effectiveMuted)) {
                                    append(":${token.code}:")
                                }
                            } else {
                                appendInlineContent(key, ":${token.code}:")
                            }
                        }
                    }
                    is ChatRichTextParser.Token.Bold -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(token.text)
                        }
                    }
                    is ChatRichTextParser.Token.Strike -> {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = effectiveMuted)) {
                            append(token.text)
                        }
                    }
                    is ChatRichTextParser.Token.InlineCode -> {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = palette.surface2,
                                color = palette.primary,
                            ),
                        ) {
                            append(" ${token.code} ")
                        }
                    }
                    is ChatRichTextParser.Token.Spoiler -> {
                        val shown = revealedSpoilers.contains(idx)
                        pushStringAnnotation("SPOILER", idx.toString())
                        withStyle(
                            if (shown) {
                                SpanStyle(background = effectiveLink.copy(alpha = 0.14f), color = textColor)
                            } else {
                                SpanStyle(background = effectiveMuted, color = effectiveMuted)
                            },
                        ) {
                            append(token.text)
                        }
                        pop()
                    }
                    is ChatRichTextParser.Token.Mention -> {
                        withStyle(SpanStyle(color = effectiveLink, fontWeight = FontWeight.SemiBold)) {
                            append("@${token.username}")
                        }
                    }
                    is ChatRichTextParser.Token.PostRef -> {
                        pushStringAnnotation("POST_REF", token.memeId.toString())
                        val refColor = if (disguised) palette.primary else (linkColor ?: palette.gold)
                        val refLabel = when (disguiseMode) {
                            AppDisguiseMode.FEISHU -> "[消息链接 #${token.memeId}]"
                            AppDisguiseMode.LINUX_DO -> "#${token.memeId}"
                            AppDisguiseMode.NONE -> token.label
                        }
                        withStyle(
                            SpanStyle(
                                color = refColor,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append(refLabel)
                        }
                        pop()
                    }
                    is ChatRichTextParser.Token.Hashtag -> {
                        pushStringAnnotation("HASHTAG", token.tag)
                        withStyle(
                            SpanStyle(
                                color = effectiveLink,
                                fontWeight = FontWeight.Medium,
                            ),
                        ) {
                            append("#${token.tag}")
                        }
                        pop()
                    }
                    is ChatRichTextParser.Token.MarkdownLink -> {
                        pushStringAnnotation("URL", token.url)
                        withStyle(
                            SpanStyle(
                                color = effectiveLink,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append(token.label)
                        }
                        pop()
                    }
                    is ChatRichTextParser.Token.WebLink -> {
                        val internalId = ChatRichTextParser.extractInternalMemeId(token.url)
                        if (internalId != null) {
                            pushStringAnnotation("POST_REF", internalId.toString())
                            val refColor = if (disguised) palette.primary else (linkColor ?: palette.gold)
                            val refLabel = when (disguiseMode) {
                                AppDisguiseMode.FEISHU -> "[消息链接 #$internalId]"
                                AppDisguiseMode.LINUX_DO -> "#$internalId"
                                AppDisguiseMode.NONE -> "No.$internalId"
                            }
                            withStyle(
                                SpanStyle(
                                    color = refColor,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) {
                                append(refLabel)
                            }
                            pop()
                        } else {
                            pushStringAnnotation("URL", token.url)
                            withStyle(
                                SpanStyle(
                                    color = effectiveLink,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) {
                                append(token.url)
                            }
                            pop()
                        }
                    }
                }
            }
            if (appendEditedBadge) {
                withStyle(SpanStyle(color = effectiveMuted, fontSize = 11.sp)) {
                    append("（已编辑）")
                }
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated,
        style = TextStyle(
            color = textColor,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
        ),
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
        onTextLayout = { res ->
            layoutResult = res
            onVisualOverflow?.invoke(res.hasVisualOverflow)
        },
        modifier = Modifier.pointerInput(annotated, onLongPress) {
            detectTapGestures(
                onLongPress = if (onLongPress != null) {
                    { onLongPress() }
                } else {
                    null
                },
                onTap = { pos ->
                    val offset = layoutResult?.getOffsetForPosition(pos) ?: return@detectTapGestures
                    annotated.getStringAnnotations(offset, offset).firstOrNull()?.let { ann ->
                        when (ann.tag) {
                            "SPOILER" -> ann.item.toIntOrNull()?.let { idx ->
                                revealedSpoilers = revealedSpoilers + idx
                            }
                            "POST_REF" -> ann.item.toLongOrNull()?.let(onPostRefClick)
                            "HASHTAG" -> onHashtagClick(ann.item)
                            "URL" -> onLinkClick(ann.item)
                        }
                    }
                },
            )
        },
    )
}

private fun buildInlineMediaMap(
    tokens: List<ChatRichTextParser.Token>,
    keyPrefix: String,
    failedKeys: Set<String>,
    stickerDp: Dp,
    stickerSp: TextUnit,
    twemojiDp: Dp,
    twemojiSp: TextUnit,
    imageLoader: ImageLoader,
    onPermanentError: (String) -> Unit,
): Map<String, InlineTextContent> = buildMap {
    tokens.forEachIndexed { idx, token ->
        when (token) {
            is ChatRichTextParser.Token.Sticker -> {
                val key = "${keyPrefix}_stk_$idx"
                if (key !in failedKeys) {
                    put(
                        key,
                        stickerInlineContent(
                            url = MediaUrlRewriter.stickerUrl(token.pack, token.name),
                            fallbackText = "[${token.pack}:${token.name}]",
                            stickerDp = stickerDp,
                            stickerSp = stickerSp,
                            imageLoader = imageLoader,
                            onPermanentError = { onPermanentError(key) },
                        ),
                    )
                }
            }
            is ChatRichTextParser.Token.Twemoji -> {
                val key = "${keyPrefix}_twe_$idx"
                if (key !in failedKeys) {
                    val url = MediaUrlRewriter.rewriteWithExtProxy("https://cdn.ldstatic.com/images/emoji/twemoji/${token.code}.png")
                    put(
                        key,
                        stickerInlineContent(
                            url = url,
                            fallbackText = ":${token.code}:",
                            stickerDp = twemojiDp,
                            stickerSp = twemojiSp,
                            imageLoader = imageLoader,
                            onPermanentError = { onPermanentError(key) },
                        ),
                    )
                }
            }
            else -> Unit
        }
    }
}

private fun stickerInlineContent(
    url: String,
    fallbackText: String,
    stickerDp: Dp,
    stickerSp: TextUnit,
    imageLoader: ImageLoader,
    onPermanentError: () -> Unit,
): InlineTextContent {
    val slotSize = if (stickerSp.value > 28f) stickerSp else 1.4.em
    return InlineTextContent(
        Placeholder(
            width = slotSize,
            height = slotSize,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        ),
    ) {
        val palette = LocalForHappyPalette.current
        var retryAttempt by remember(url) { mutableIntStateOf(0) }
        var isLoading by remember(url) { mutableStateOf(true) }
        var failed by remember(url) { mutableStateOf(false) }

        val reqUrl = if (retryAttempt == 0) url else "$url?retry=$retryAttempt"
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (failed) {
                        Modifier
                            .background(palette.surface2)
                            .clickable {
                                failed = false
                                isLoading = true
                                retryAttempt++
                            }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (isLoading && !failed) {
                Box(
                    modifier = Modifier
                        .size(stickerDp.coerceAtMost(24.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.surface2.copy(alpha = 0.85f)),
                )
            }
            if (failed) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "点击重试加载 $fallbackText",
                    tint = palette.muted,
                    modifier = Modifier.size((stickerDp * 0.72f).coerceAtLeast(12.dp)),
                )
            } else {
                AsyncImage(
                    model = reqUrl,
                    imageLoader = imageLoader,
                    contentDescription = fallbackText,
                    contentScale = ContentScale.Fit,
                    onLoading = { isLoading = true },
                    onSuccess = {
                        isLoading = false
                        failed = false
                    },
                    onError = {
                        isLoading = false
                        if (retryAttempt < 1) {
                            retryAttempt++
                        } else {
                            // Keep fixed inline slot active so tap-to-retry icon stays in-place without shifting line height.
                            failed = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}
