package com.jinhuan.forhappy.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class BadgeFormattingTest {

    @Test
    fun formatUnreadBadgeCountCapsAboveNinetyNine() {
        assertEquals("0", formatUnreadBadgeCount(-5))
        assertEquals("0", formatUnreadBadgeCount(0))
        assertEquals("1", formatUnreadBadgeCount(1))
        assertEquals("19", formatUnreadBadgeCount(19))
        assertEquals("99", formatUnreadBadgeCount(99))
        assertEquals("99+", formatUnreadBadgeCount(100))
    }

    @Test
    fun formatMentionOrCountBadgeHandlesMentionAndCountModes() {
        assertEquals("@我", formatMentionOrCountBadge(0, isMention = true))
        assertEquals("@19", formatMentionOrCountBadge(19, isMention = true))
        assertEquals("@99+", formatMentionOrCountBadge(150, isMention = true))
        assertEquals("8", formatMentionOrCountBadge(8, isMention = false))
    }
}
