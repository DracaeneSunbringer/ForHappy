package com.jinhuan.forhappy.core.seen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

val LocalSeenTracker = staticCompositionLocalOf<SeenTracker?> { null }

/**
 * Computes what fraction `[0f..1f]` of a card's clipped阅读窗口 is inside the visible viewport,
 * aligned with `kourou25/meme-hall-frontend/seen-visibility.js` (`expectedHeight = min(itemHeight, viewportHeight)`),
 * so tall single-column cards (`itemHeight > viewportHeight`) still reach `>= 0.60f` when filling the screen.
 */
fun computeVisibleAreaRatio(
    itemTopPx: Float,
    itemBottomPx: Float,
    viewportTopPx: Float,
    viewportBottomPx: Float,
): Float {
    val itemHeight = itemBottomPx - itemTopPx
    val viewportHeight = viewportBottomPx - viewportTopPx
    if (itemHeight <= 0f || viewportHeight <= 0f) return 0f
    val expectedHeight = minOf(itemHeight, viewportHeight)
    val inter = (minOf(itemBottomPx, viewportBottomPx) - maxOf(itemTopPx, viewportTopPx)).coerceAtLeast(0f)
    return (inter / expectedHeight).coerceIn(0f, 1f)
}

/**
 * Dual-gated seen eligibility rule (`02a §6`):
 * requires loaded media, unblurred state, visible fraction >= 60%, and scroll speed <= 1500 dp/s.
 */
fun shouldCountSeenDwell(
    memeId: Long,
    isMediaReady: Boolean,
    isBlurred: Boolean,
    visibleRatio: Float,
    scrollSpeedDpPerSec: Float,
): Boolean = memeId > 0L &&
    isMediaReady &&
    !isBlurred &&
    visibleRatio >= 0.60f &&
    scrollSpeedDpPerSec <= 1500f

/**
 * Auto-reblur trigger (`02a §7`): once a blurred card has been revealed in-memory,
 * scrolling it below 50% viewport visibility immediately snaps the mask back on.
 */
fun shouldAutoReblurRevealed(
    wasRevealed: Boolean,
    visibleRatio: Float,
): Boolean = wasRevealed && visibleRatio < 0.50f

/**
 * Observes a composed meme item while it stays >= 60% visible in viewport with its media ready,
 * unblurred, and scroll velocity slow enough, automatically marking it as seen after
 * [SeenTracker.DWELL_THRESHOLD_MS] (1s). No badge or visual indicator is rendered on cards.
 */
@Composable
fun ObserveMemeSeenDwell(
    memeId: Long,
    seenAt: Long?,
    memeDay: String?,
    isMediaReady: Boolean,
    isBlurred: Boolean,
    visibleRatio: Float = 1f,
    scrollSpeedDpPerSec: Float = 0f,
) {
    val tracker = LocalSeenTracker.current ?: return
    val eligible = shouldCountSeenDwell(
        memeId = memeId,
        isMediaReady = isMediaReady,
        isBlurred = isBlurred,
        visibleRatio = visibleRatio,
        scrollSpeedDpPerSec = scrollSpeedDpPerSec,
    )
    LaunchedEffect(memeId, eligible) {
        if (eligible) {
            delay(SeenTracker.DWELL_THRESHOLD_MS)
            tracker.markSeen(
                memeId = memeId,
                existingSeenAt = seenAt,
                memeDay = memeDay,
            )
        }
    }
}
