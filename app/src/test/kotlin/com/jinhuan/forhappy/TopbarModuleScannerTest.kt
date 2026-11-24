package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.dynamic.TopbarModuleScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopbarModuleScannerTest {

    @Test
    fun parsesDynamicTopbarHtmlMarksNewModulesAndFiltersRetiredRoutes() {
        val sampleHtml = """
            <html><body>
              <header class="topbar">
                <nav class="mainnav">
                  <a href="/">图廊</a>
                  <a href="/chat">群聊</a>
                  <a href="/road">鞭打</a>
                  <a href="/novels">小说</a>
                  <a href="/pets">鱼鱼谷</a>
                  <a href="/puzzles">旧解谜大厅</a>
                  <a href="/tools">旧工具</a>
                  <a href="/farm">旧农场</a>
                  <a href="/manor">肥鱼庄园</a>
                  <a href="/huancai">欢彩</a>
                  <a href="/music">音乐</a>
                  <a href="/nsfw">RZP</a>
                  <a href="/arcade">游戏大厅</a>
                  <a href="/valley">谷地别名</a>
                  <a href="/community-updates.html">社区更新</a>
                  <a href="/event">周年活动</a>
                </nav>
              </header>
            </body></html>
        """.trimIndent()

        val parsed = TopbarModuleScanner.parseTopbarHtml(sampleHtml)
        assertEquals(7, parsed.size)
        val eventMod = parsed.last()
        assertEquals("/event", eventMod.route)
        assertEquals("周年活动", eventMod.label)
        assertFalse(eventMod.isBuiltInNative)
        assertTrue(eventMod.isNewFromTopbar)

        val retired = TopbarModuleScanner.RETIRED_ROUTE_PREFIXES
        assertTrue(parsed.none { mod -> mod.route in retired })
        assertFalse(parsed.any { it.route.startsWith("/arcade") })
        // /valley normalizes to /pets and deduplicates against existing /pets
        assertEquals(1, parsed.count { it.route == "/pets" })
        // /community-updates.html normalizes to /community-updates
        assertTrue(parsed.any { it.route == "/community-updates" })
        assertTrue(TopbarModuleScanner.isRetiredRoute("/arcade/mole"))
        assertEquals("/pets", TopbarModuleScanner.normalizeModuleRoute("https://52dmeme.com/pet-isle/"))
        assertEquals("/community-updates", TopbarModuleScanner.normalizeModuleRoute("/community-updates.html"))

        val merged = TopbarModuleScanner.mergeWithDefaults(parsed)
        assertTrue(merged.none { mod -> TopbarModuleScanner.isRetiredRoute(mod.route) })
        assertTrue(merged.any { it.route == "/moyu" })
        assertTrue(merged.any { it.route == "/event" })
    }
}
