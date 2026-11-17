package com.jinhuan.forhappy.core.seen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

val LocalSeenTracker = staticCompositionLocalOf<SeenTracker?> { null }

/**
 * Observes a composed meme item while it stays visible in viewport with its media ready and
 * unblurred, automatically marking it as seen after [SeenTracker.DWELL_THRESHOLD_MS] (1s).
 * No badge or visual indicator is rendered on cards.
 */
@Composable
fun ObserveMemeSeenDwell(
    memeId: Long,
    seenAt: Long?,
    memeDay: String?,
    isMediaReady: Boolean,
    isBlurred: Boolean,
) {
    val tracker = LocalSeenTracker.current ?: return
    LaunchedEffect(memeId, isMediaReady, isBlurred) {
        if (memeId > 0L && isMediaReady && !isBlurred) {
            delay(SeenTracker.DWELL_THRESHOLD_MS)
            tracker.markSeen(
                memeId = memeId,
                existingSeenAt = seenAt,
                memeDay = memeDay,
            )
        }
    }
}
