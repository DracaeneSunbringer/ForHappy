package com.jinhuan.forhappy.core.dynamic

import com.jinhuan.forhappy.core.model.DynamicModule

/**
 * Parses live topbar `<nav class="mainnav">` and `.mobile-nav` entries from `/` HTML
 * to dynamically discover newly launched or retired server modules without app updates.
 */
object TopbarModuleScanner {
    private val NAV_BLOCK_REGEX = Regex("""<nav\b[^>]*class\s*=\s*["'][^"']*mainnav[^"']*["'][^>]*>([\s\S]*?)</nav>""", RegexOption.IGNORE_CASE)
    private val MOBILE_NAV_REGEX = Regex("""<div\b[^>]*class\s*=\s*["'][^"']*mobile-nav[^"']*["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
    private val ANCHOR_REGEX = Regex("""<a\b([^>]*)>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
    private val HREF_REGEX = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val STRIP_TAGS_REGEX = Regex("""<[^>]+>""")

    val DEFAULT_BUILT_IN_MODULES = listOf(
        DynamicModule("wall", "图廊", "/", iconKey = "gallery", isBuiltInNative = true),
        DynamicModule("chat", "群聊", "/chat", iconKey = "chat", isBuiltInNative = true),
        DynamicModule("road", "鞭打", "/road", iconKey = "road", isBuiltInNative = true),
        DynamicModule("novels", "小说", "/novels", iconKey = "book", isBuiltInNative = true),
        DynamicModule("pets", "鱼鱼谷", "/pets", iconKey = "pets", isBuiltInNative = true),
        DynamicModule("puzzles", "解谜", "/puzzles", iconKey = "puzzle", isBuiltInNative = true, visibleInNav = true),
        DynamicModule("moyu", "摸鱼工具", "/moyu", iconKey = "tools", isBuiltInNative = true, visibleInNav = true),
    )

    private val KNOWN_ROUTE_META = mapOf(
        "/" to Triple("wall", "gallery", true),
        "/chat" to Triple("chat", "chat", true),
        "/dm" to Triple("dm", "chat", true),
        "/road" to Triple("road", "road", true),
        "/novels" to Triple("novels", "book", true),
        "/pets" to Triple("pets", "pets", true),
        "/pet-isle" to Triple("pets", "pets", true),
        "/puzzles" to Triple("puzzles", "puzzle", true),
        "/farm" to Triple("farm", "farm", false),
        "/manor" to Triple("manor", "manor", false),
        "/huancai" to Triple("huancai", "sparkle", false),
    )

    fun parseTopbarHtml(html: String): List<DynamicModule> {
        if (html.isBlank()) return emptyList()
        val candidates = buildString {
            NAV_BLOCK_REGEX.findAll(html).forEach { append(it.groupValues[1]).append('\n') }
            MOBILE_NAV_REGEX.findAll(html).forEach { append(it.groupValues[1]).append('\n') }
        }
        if (candidates.isBlank()) return emptyList()

        val seenRoutes = linkedMapOf<String, DynamicModule>()
        ANCHOR_REGEX.findAll(candidates).forEach { match ->
            val attrs = match.groupValues[1]
            val rawLabel = STRIP_TAGS_REGEX.replace(match.groupValues[2], "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace(Regex("""\s+"""), " ")
                .trim()
            val href = HREF_REGEX.find(attrs)?.groupValues?.get(1)?.trim().orEmpty()
            if (href.isBlank() || href.startsWith("#") || href.startsWith("javascript:", ignoreCase = true)) {
                return@forEach
            }
            val normalizedRoute = normalizeRoute(href)
            if (normalizedRoute == "/me" || normalizedRoute == "/login") return@forEach

            val label = rawLabel.ifBlank { fallbackLabelForRoute(normalizedRoute) }
            val known = KNOWN_ROUTE_META.entries.firstOrNull { (prefix, _) ->
                if (prefix == "/") normalizedRoute == "/" else normalizedRoute == prefix || normalizedRoute.startsWith("$prefix/")
            }?.value

            val id = known?.first ?: normalizedRoute.trim('/').replace('/', '_').ifBlank { "mod_${seenRoutes.size}" }
            val iconKey = known?.second ?: "web"
            val builtIn = known?.third ?: false

            seenRoutes.putIfAbsent(
                normalizedRoute,
                DynamicModule(
                    id = id,
                    label = label,
                    route = normalizedRoute,
                    iconKey = iconKey,
                    isBuiltInNative = builtIn,
                    isNewFromTopbar = !builtIn,
                    visibleInNav = true,
                ),
            )
        }
        return seenRoutes.values.toList()
    }

    /**
     * Merges parsed server topbar items with offline defaults so core tools stay accessible even offline
     * while new server topbar modules surface with a `NEW` badge.
     */
    fun mergeWithDefaults(parsed: List<DynamicModule>): List<DynamicModule> {
        if (parsed.isEmpty()) return DEFAULT_BUILT_IN_MODULES
        val merged = linkedMapOf<String, DynamicModule>()
        for (mod in parsed) {
            merged[mod.route] = mod
        }
        for (fallback in DEFAULT_BUILT_IN_MODULES) {
            if (!merged.containsKey(fallback.route)) {
                merged[fallback.route] = fallback
            }
        }
        return merged.values.toList()
    }

    private fun normalizeRoute(href: String): String {
        val withoutOrigin = when {
            href.startsWith("https://52dmeme.com", ignoreCase = true) -> href.removePrefix("https://52dmeme.com")
            href.startsWith("http://52dmeme.com", ignoreCase = true) -> href.removePrefix("http://52dmeme.com")
            else -> href
        }
        val pathOnly = withoutOrigin.substringBefore('?').substringBefore('#')
        return if (pathOnly.isBlank()) "/" else if (pathOnly.startsWith('/')) pathOnly else "/$pathOnly"
    }

    private fun fallbackLabelForRoute(route: String): String = when {
        route == "/" -> "图廊"
        route.startsWith("/chat") -> "群聊"
        route.startsWith("/road") -> "鞭打"
        route.startsWith("/novels") -> "小说"
        route.startsWith("/pets") -> "鱼鱼谷"
        else -> route.trim('/').uppercase().ifBlank { "扩展模块" }
    }
}
