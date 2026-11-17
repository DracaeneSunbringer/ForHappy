package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.dynamic.TopbarModuleScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopbarModuleScannerTest {

    @Test
    fun parsesDynamicTopbarHtmlAndMarksNewServerModules() {
        val sampleHtml = """
            <html><body>
              <header class="topbar">
                <nav class="mainnav">
                  <a href="/">图廊</a>
                  <a href="/chat">群聊</a>
                  <a href="/road">鞭打</a>
                  <a href="/novels">小说</a>
                  <a href="/pets">鱼鱼谷</a>
                  <a href="/huancai">焕彩新活动</a>
                </nav>
              </header>
            </body></html>
        """.trimIndent()

        val parsed = TopbarModuleScanner.parseTopbarHtml(sampleHtml)
        assertEquals(6, parsed.size)
        val huancai = parsed.last()
        assertEquals("/huancai", huancai.route)
        assertEquals("焕彩新活动", huancai.label)
        assertFalse(huancai.isBuiltInNative)
        assertTrue(huancai.isNewFromTopbar)

        val merged = TopbarModuleScanner.mergeWithDefaults(parsed)
        assertTrue(merged.any { it.route == "/puzzles" })
    }
}
