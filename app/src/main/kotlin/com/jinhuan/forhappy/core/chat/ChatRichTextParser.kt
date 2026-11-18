package com.jinhuan.forhappy.core.chat

import com.jinhuan.forhappy.core.model.NovelExcerptSpec
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Rich text & lite-Markdown parser for `52dmeme.com` chat, quotes, comments, and quick replies:
 * - `[quote="name, msg:id, g:gid"]quoted text[/quote]` -> [Block.QuoteBlock] (plus unclosed leading fallback)
 * - `[details="title"]...[/details]` -> collapsible [Block.DetailsBlock]
 * - Fenced code blocks and `> ` quotes -> [Block.CodeBlock] / [Block.BlockQuote]
 * - Inline tokens: `[pack:name]`, `:shortcode:`, `` `code` ``, `[text](url)`, `**bold**`,
 *   `*italic*`, `~~strike~~`, `[spoiler]...[/spoiler]`, `@username`, `No.1234`, `#tag`, `https://...`
 * - Direct image links in body (`bodyImgUrl`) -> [ParsedBody.inlineImageUrls]
 */
object ChatRichTextParser {
    private val QUOTE_BLOCK_REGEX = Regex(
        """\[quote(?:="([^"]*)")?]([\s\S]*?)\[/quote]""",
    )
    private val UNCLOSED_LEADING_QUOTE_REGEX = Regex(
        """^\[quote="([^"]*)"]\s*\n?""",
    )
    private val DETAILS_BLOCK_REGEX = Regex(
        """\[details(?:="([^"]*)")?]([\s\S]*?)\[/details]""",
    )
    private val FENCED_CODE_REGEX = Regex(
        """(?m)^```[^\n]*\n([\s\S]*?)\n?```[ \t]*$""",
    )

    // Order matters: URLs, Markdown links, inline code, spoilers, stickers, twemoji, styling, mentions, post refs, hashtags
    private val INLINE_TOKEN_REGEX = Regex(
        """`([^`\n]+)`|""" +
            """\[([^\]\n]{1,120})]\((https?://[^\s)]+|/(?!/)[^\s)]*)\)|""" +
            """~~([^~\n]{1,300})~~|""" +
            """(?<!\*)\*\*(?!\s)([^*\n]{1,300}?)(?<!\s)\*\*(?!\*)|""" +
            """\[spoiler]([^\n]*?)\[/spoiler]|""" +
            """\[([a-z][a-z0-9_-]{0,31}):([a-z0-9_-]{1,48})]|""" +
            """(?<![\w]):([a-z0-9_+-]{1,40}):(?![\w])|""" +
            """https?://[^\s<>"'\])]+|""" +
            """(?<![\w@.])@([A-Za-z0-9_][\w.-]{0,30}[\w])|""" +
            """(?<![\w.])(?:No\.|no\.|NO\.)(\d{1,8})(?!\d)|""" +
            """(?<![\w#&/=])#([^\s#<>\[\]，。、,.!！?？:：;；()（）"'*`~]{1,24})""",
    )

    private val LINK_IMG_REGEX = Regex(
        """https?://[^\s<>"')\]\n]+|(?<![\w/.])/img/[^\s<>"')\]\n]+""",
        RegexOption.IGNORE_CASE,
    )
    private val MEME_URL_REGEX = Regex(
        """^https?://(?:cc\.)?52dmeme\.com/m/(\d+)/?(?:\?.*)?$""",
        RegexOption.IGNORE_CASE,
    )

    sealed interface Token {
        data class PlainText(val text: String) : Token
        data class Sticker(val pack: String, val name: String) : Token
        data class Twemoji(val code: String) : Token
        data class PostRef(val memeId: Long, val label: String) : Token
        data class Hashtag(val tag: String) : Token
        data class WebLink(val url: String) : Token
        data class MarkdownLink(val label: String, val url: String) : Token
        data class Mention(val username: String) : Token
        data class Bold(val text: String) : Token
        data class Strike(val text: String) : Token
        data class InlineCode(val code: String) : Token
        data class Spoiler(val text: String) : Token
    }

    sealed interface Block {
        data class QuoteBlock(
            val author: String? = null,
            val messageId: Long? = null,
            val memeId: Long? = null,
            val groupId: Long? = null,
            val bodyTokens: List<Token> = emptyList(),
        ) : Block

        data class CodeBlock(val code: String) : Block
        data class BlockQuote(val tokens: List<Token>) : Block
        data class DetailsBlock(val summary: String, val bodyTokens: List<Token>) : Block
        data class Paragraph(val tokens: List<Token>) : Block
    }

    data class ParsedBody(
        val quoteAuthor: String? = null,
        val quoteMessageId: Long? = null,
        val tokens: List<Token>,
        val blocks: List<Block> = emptyList(),
        val inlineImageUrls: List<String> = emptyList(),
        val bodyImgOnly: Boolean = false,
    )

    fun parse(raw: String): ParsedBody {
        if (raw.isEmpty()) return ParsedBody(tokens = emptyList(), blocks = emptyList())

        val blocks = mutableListOf<Block>()
        val flatTokens = mutableListOf<Token>()
        var firstQuoteAuthor: String? = null
        var firstQuoteMsgId: Long? = null

        var remaining = raw
        val quoteMatches = QUOTE_BLOCK_REGEX.findAll(remaining).toList()
        if (quoteMatches.isEmpty()) {
            UNCLOSED_LEADING_QUOTE_REGEX.find(remaining)?.let { uq ->
                val meta = parseQuoteMeta(uq.groupValues[1])
                firstQuoteAuthor = meta.author
                firstQuoteMsgId = meta.messageId
                blocks += Block.QuoteBlock(
                    author = meta.author,
                    messageId = meta.messageId,
                    memeId = meta.memeId,
                    groupId = meta.groupId,
                    bodyTokens = emptyList(),
                )
                remaining = remaining.removeRange(uq.range)
            }
            val textNoQuote = remaining.trim('\n')
            if (textNoQuote.isNotEmpty()) {
                parseRichSegmentsInto(textNoQuote, blocks, flatTokens)
            }
        } else {
            var cursor = 0
            for (qm in quoteMatches) {
                if (qm.range.first > cursor) {
                    val before = remaining.substring(cursor, qm.range.first).trim('\n')
                    if (before.isNotEmpty()) {
                        parseRichSegmentsInto(before, blocks, flatTokens)
                    }
                }
                val meta = parseQuoteMeta(qm.groupValues[1])
                if (firstQuoteAuthor == null) {
                    firstQuoteAuthor = meta.author
                    firstQuoteMsgId = meta.messageId
                }
                val innerTokens = tokenizeInline(qm.groupValues[2].trim())
                blocks += Block.QuoteBlock(
                    author = meta.author,
                    messageId = meta.messageId,
                    memeId = meta.memeId,
                    groupId = meta.groupId,
                    bodyTokens = innerTokens,
                )
                cursor = qm.range.last + 1
            }
            if (cursor < remaining.length) {
                val tail = remaining.substring(cursor).trimStart('\n')
                if (tail.isNotEmpty()) {
                    parseRichSegmentsInto(tail, blocks, flatTokens)
                }
            }
        }

        val strippedForImgs = QUOTE_BLOCK_REGEX.replace(raw, "").trim()
        val imgMatch = extractInlineImage(strippedForImgs)
        val inlineImages = listOfNotNull(imgMatch?.first)
        val imgOnly = imgMatch?.second == true

        return ParsedBody(
            quoteAuthor = firstQuoteAuthor,
            quoteMessageId = firstQuoteMsgId,
            tokens = flatTokens,
            blocks = blocks,
            inlineImageUrls = inlineImages,
            bodyImgOnly = imgOnly,
        )
    }

    private data class QuoteMeta(
        val author: String? = null,
        val messageId: Long? = null,
        val memeId: Long? = null,
        val groupId: Long? = null,
    )

    private fun parseQuoteMeta(attr: String): QuoteMeta {
        val parts = attr.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return QuoteMeta()
        var author: String? = null
        var msgId: Long? = null
        var memeId: Long? = null
        var groupId: Long? = null
        parts.forEachIndexed { index, part ->
            when {
                part.startsWith("msg:") -> msgId = part.removePrefix("msg:").toLongOrNull()
                part.startsWith("m:") -> memeId = part.removePrefix("m:").toLongOrNull()
                part.startsWith("g:") -> groupId = part.removePrefix("g:").toLongOrNull()
                index == 0 -> author = part
            }
        }
        return QuoteMeta(author = author, messageId = msgId, memeId = memeId, groupId = groupId)
    }

    private fun parseRichSegmentsInto(
        segment: String,
        blocks: MutableList<Block>,
        flatTokens: MutableList<Token>,
    ) {
        val detailsMatches = DETAILS_BLOCK_REGEX.findAll(segment).toList()
        if (detailsMatches.isEmpty()) {
            parseMarkdownBlocksInto(segment, blocks, flatTokens)
            return
        }
        var cursor = 0
        for (dm in detailsMatches) {
            if (dm.range.first > cursor) {
                val before = segment.substring(cursor, dm.range.first).trim('\n')
                if (before.isNotEmpty()) parseMarkdownBlocksInto(before, blocks, flatTokens)
            }
            val summary = dm.groupValues[1].trim().ifBlank { "展开" }
            val innerTokens = tokenizeInline(dm.groupValues[2].trim())
            blocks += Block.DetailsBlock(summary = summary, bodyTokens = innerTokens)
            flatTokens += innerTokens
            cursor = dm.range.last + 1
        }
        if (cursor < segment.length) {
            val tail = segment.substring(cursor).trim('\n')
            if (tail.isNotEmpty()) parseMarkdownBlocksInto(tail, blocks, flatTokens)
        }
    }

    private fun parseMarkdownBlocksInto(
        segment: String,
        blocks: MutableList<Block>,
        flatTokens: MutableList<Token>,
    ) {
        val codeMatches = FENCED_CODE_REGEX.findAll(segment).toList()
        if (codeMatches.isEmpty()) {
            parseLinesInto(segment, blocks, flatTokens)
            return
        }
        var cursor = 0
        for (cm in codeMatches) {
            if (cm.range.first > cursor) {
                val before = segment.substring(cursor, cm.range.first).trim('\n')
                if (before.isNotEmpty()) parseLinesInto(before, blocks, flatTokens)
            }
            val codeText = cm.groupValues[1]
            blocks += Block.CodeBlock(codeText)
            flatTokens += Token.InlineCode(codeText)
            cursor = cm.range.last + 1
        }
        if (cursor < segment.length) {
            val tail = segment.substring(cursor).trim('\n')
            if (tail.isNotEmpty()) parseLinesInto(tail, blocks, flatTokens)
        }
    }

    private fun parseLinesInto(
        text: String,
        blocks: MutableList<Block>,
        flatTokens: MutableList<Token>,
    ) {
        val lines = text.replace("\r", "").split('\n')
        val quoteBuf = mutableListOf<String>()
        val paraBuf = mutableListOf<String>()

        fun flushQuote() {
            if (quoteBuf.isEmpty()) return
            val tks = tokenizeInline(quoteBuf.joinToString("\n"))
            blocks += Block.BlockQuote(tks)
            flatTokens += tks
            quoteBuf.clear()
        }

        fun flushPara() {
            if (paraBuf.isEmpty()) return
            val tks = tokenizeInline(paraBuf.joinToString("\n"))
            blocks += Block.Paragraph(tks)
            flatTokens += tks
            paraBuf.clear()
        }

        for (line in lines) {
            if (line.startsWith("> ") || line == ">") {
                flushPara()
                quoteBuf += line.removePrefix(">").trimStart()
            } else {
                flushQuote()
                paraBuf += line
            }
        }
        flushQuote()
        flushPara()
    }

    fun tokenizeInline(input: String): List<Token> {
        if (input.isEmpty()) return emptyList()
        val tokens = mutableListOf<Token>()
        var cursor = 0
        for (m in INLINE_TOKEN_REGEX.findAll(input)) {
            if (m.range.first > cursor) {
                tokens += Token.PlainText(input.substring(cursor, m.range.first))
            }
            when {
                m.groupValues[1].isNotEmpty() -> {
                    tokens += Token.InlineCode(m.groupValues[1])
                }
                m.groupValues[2].isNotEmpty() && m.groupValues[3].isNotEmpty() -> {
                    tokens += Token.MarkdownLink(m.groupValues[2], m.groupValues[3])
                }
                m.groupValues[4].isNotEmpty() -> {
                    tokens += Token.Strike(m.groupValues[4])
                }
                m.groupValues[5].isNotEmpty() -> {
                    tokens += Token.Bold(m.groupValues[5])
                }
                m.groupValues[6].isNotEmpty() -> {
                    tokens += Token.Spoiler(m.groupValues[6])
                }
                m.groupValues[7].isNotEmpty() && m.groupValues[8].isNotEmpty() -> {
                    tokens += Token.Sticker(m.groupValues[7], m.groupValues[8])
                }
                m.groupValues[9].isNotEmpty() -> {
                    tokens += Token.Twemoji(m.groupValues[9])
                }
                m.groupValues[10].isNotEmpty() -> {
                    tokens += Token.Mention(m.groupValues[10])
                }
                m.groupValues[11].isNotEmpty() -> {
                    val id = m.groupValues[11].toLongOrNull()
                    if (id != null && id > 0L) {
                        tokens += Token.PostRef(id, m.value)
                    } else {
                        tokens += Token.PlainText(m.value)
                    }
                }
                m.groupValues[12].isNotEmpty() -> {
                    tokens += Token.Hashtag(m.groupValues[12])
                }
                else -> {
                    tokens += Token.WebLink(m.value)
                }
            }
            cursor = m.range.last + 1
        }
        if (cursor < input.length) {
            tokens += Token.PlainText(input.substring(cursor))
        }
        return tokens
    }

    fun extractInternalMemeId(url: String): Long? =
        MEME_URL_REGEX.find(url.trim())?.groupValues?.getOrNull(1)?.toLongOrNull()

    private fun extractInlineImage(strippedBody: String): Pair<String, Boolean>? {
        for (m in LINK_IMG_REGEX.findAll(strippedBody)) {
            val rawCandidate = m.value.replace(Regex("""[.,，。!！?？;；:：]+$"""), "")
            val abs = if (rawCandidate.startsWith("/")) "https://52dmeme.com$rawCandidate" else rawCandidate
            val parsed = abs.toHttpUrlOrNull() ?: continue
            val host = parsed.host.lowercase()
            val path = parsed.encodedPath.lowercase()
            val sameOrigin = host == "52dmeme.com" || host == "cc.52dmeme.com" || host == "img.52dmeme.com"
            val isLdHost = host == "linux.do" || host.endsWith(".linux.do") ||
                host == "ldstatic.com" || host.endsWith(".ldstatic.com")
            val isImg = path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") ||
                (sameOrigin && path.startsWith("/img/")) ||
                (isLdHost && (path.startsWith("/uploads/") || path.startsWith("/original/") || path.startsWith("/optimized/")))
            if (isImg) {
                val only = strippedBody.trim() == m.value.trim()
                return rawCandidate to only
            }
        }
        return null
    }

    fun stripQuotes(raw: String): String = QUOTE_BLOCK_REGEX.replace(raw, "").trim()

    private val PREVIEW_STICKER_REGEX = Regex("""\[([a-z][a-z0-9_-]{0,31}):([a-z0-9_-]{1,48})]""")
    private val NOVEL_EXCERPT_REGEX = Regex(
        """^\[novel-excerpt:v1:([a-z0-9-]{1,80}):([1-9]\d{0,5}):(paper|white|green|night)]\r?\n([^\r\n]{1,100})\r?\n([^\r\n]{1,120})\r?\n([\s\S]{1,1600})$""",
    )

    fun parseNovelExcerpt(raw: String): NovelExcerptSpec? {
        val m = NOVEL_EXCERPT_REGEX.matchEntire(raw.trim()) ?: return null
        val bookId = m.groupValues[1]
        val chapter = m.groupValues[2].toIntOrNull() ?: return null
        val theme = m.groupValues[3]
        val title = m.groupValues[4].trim()
        val chapterTitle = m.groupValues[5].trim()
        val excerpt = m.groupValues[6].trim()
        if (title.isBlank() || excerpt.isBlank()) return null
        return NovelExcerptSpec(
            bookId = bookId,
            chapter = chapter,
            theme = theme,
            title = title,
            chapterTitle = chapterTitle,
            excerpt = excerpt,
        )
    }

    fun serializeNovelExcerpt(spec: NovelExcerptSpec): String {
        val safeTheme = when (spec.theme) {
            "paper", "white", "green", "night" -> spec.theme
            else -> "paper"
        }
        val safeTitle = spec.title.replace("\r", " ").replace("\n", " ").trim().take(100)
        val safeChapterTitle = spec.chapterTitle.replace("\r", " ").replace("\n", " ").trim().ifBlank {
            "第 ${spec.chapter.coerceAtLeast(1)} 章"
        }.take(120)
        val safeExcerpt = spec.excerpt.replace("\r\n", "\n").trim().take(1600)
        return "[novel-excerpt:v1:${spec.bookId.lowercase()}:${spec.chapter.coerceAtLeast(1)}:$safeTheme]\n$safeTitle\n$safeChapterTitle\n$safeExcerpt"
    }

    fun previewSummary(raw: String): String {
        parseNovelExcerpt(raw)?.let { spec ->
            return "[书房摘录] 《${spec.title}》"
        }
        val noQuotes = UNCLOSED_LEADING_QUOTE_REGEX.replace(stripQuotes(raw), "")
        return noQuotes
            .replace(PREVIEW_STICKER_REGEX, "[表情]")
            .replace(Regex("""\[details(?:="[^"]*")?]|\[/details]|\[spoiler]|\[/spoiler]"""), "")
            .replace(Regex("""```[^\n]*\n?|```"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun normalizeForMatch(text: String): String = text.trim()
}
