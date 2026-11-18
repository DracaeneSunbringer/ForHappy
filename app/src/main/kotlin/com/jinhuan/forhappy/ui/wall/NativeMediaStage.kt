package com.jinhuan.forhappy.ui.wall

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemeMediaItem
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.theme.LocalBossKeyActive
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Configures an edge-to-edge Compose [Dialog] window so Android 15 display cutouts and
 * system bars do not shift the dialog root down while measuring full screen height.
 */
@Composable
fun ConfigureFullscreenDialogWindow(darkStatusIcons: Boolean) {
    val view = LocalView.current
    LaunchedEffect(view, darkStatusIcons) {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        )
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val newAttrs = WindowManager.LayoutParams().apply {
            copyFrom(window.attributes)
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        window.attributes = newAttrs
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkStatusIcons
        controller.isAppearanceLightNavigationBars = darkStatusIcons
        ViewCompat.requestApplyInsets(window.decorView)
    }
}

/**
 * Downloads (if needed) via [ForHappyApiClient.fetchCachedMediaFile] through RustNet and writes
 * the image/GIF/video into the Android MediaStore system album (`Pictures/ForHappy` or `Movies/ForHappy`).
 */
suspend fun saveMediaToSystemGallery(
    context: Context,
    api: ForHappyApiClient,
    mediaUrl: String,
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val file = api.fetchCachedMediaFile(mediaUrl)
            ?: throw IllegalStateException("媒体下载失败，无法保存")
        val ext = file.extension.lowercase()
        val isVideo = ext in setOf("mp4", "webm", "mov")
        val mime = when (ext) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "png" -> "image/png"
            else -> "image/jpeg"
        }
        val outExt = if (ext.isBlank() || ext == "bin") "jpg" else ext
        writeGalleryMedia(
            context = context,
            isVideo = isVideo,
            mime = mime,
            ext = outExt,
        ) { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
        "已保存到系统相册"
    }
}

/**
 * Writes raw image bytes (e.g. generated Moyu daily report PNG) into `Pictures/ForHappy`.
 */
suspend fun saveImageBytesToSystemGallery(
    context: Context,
    bytes: ByteArray,
    ext: String = "png",
    mime: String = "image/png",
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        writeGalleryMedia(
            context = context,
            isVideo = false,
            mime = mime,
            ext = ext,
        ) { out ->
            out.write(bytes)
        }
        "已保存到系统相册"
    }
}

private fun writeGalleryMedia(
    context: Context,
    isVideo: Boolean,
    mime: String,
    ext: String,
    writeTo: (java.io.OutputStream) -> Unit,
) {
    val cleanExt = ext.trimStart('.').ifBlank { "jpg" }
    val displayName = "ForHappy_${System.currentTimeMillis()}.$cleanExt"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relPath = if (isVideo) {
            "${Environment.DIRECTORY_MOVIES}/ForHappy"
        } else {
            "${Environment.DIRECTORY_PICTURES}/ForHappy"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("创建相册文件失败")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                writeTo(out)
            } ?: throw IllegalStateException("无法写入系统相册")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    } else {
        @Suppress("DEPRECATION")
        val dir = File(
            Environment.getExternalStoragePublicDirectory(
                if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
            ),
            "ForHappy",
        ).apply { mkdirs() }
        val dst = File(dir, displayName)
        dst.outputStream().use { out -> writeTo(out) }
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            @Suppress("DEPRECATION")
            put(MediaStore.MediaColumns.DATA, dst.absolutePath)
        }
        context.contentResolver.insert(collection, values)
    }
}

private val VIDEO_SPEED_STEPS = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

/**
 * Zero-dependency native Android `MediaPlayer + TextureView` video player (`07 §二 Phase 2`).
 * Downloads/reads local cache via [ForHappyApiClient.fetchCachedMediaFile] so playback traffic
 * always routes through RustNet ECH + EdgePin rather than direct cleartext/blocked OS sockets.
 */
@Composable
fun NativeVideoPlayer(
    videoUrl: String,
    posterUrl: String?,
    api: ForHappyApiClient,
    inlineMode: Boolean,
    modifier: Modifier = Modifier,
    isBlurred: Boolean = false,
    onExpandFullscreen: (() -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val bossActive = LocalBossKeyActive.current
    val disguised = LocalPrivacyStealthConfig.current.disguiseMode != AppDisguiseMode.NONE
    val lifecycleOwner = LocalLifecycleOwner.current

    var userActivated by remember(videoUrl) { mutableStateOf(!inlineMode) }
    var cachedFile by remember(videoUrl) { mutableStateOf<File?>(null) }
    var loadingFile by remember(videoUrl) { mutableStateOf(false) }
    var loadError by remember(videoUrl) { mutableStateOf<String?>(null) }
    var prepared by remember(videoUrl) { mutableStateOf(false) }
    var playing by remember(videoUrl) { mutableStateOf(false) }
    var muted by remember(videoUrl, disguised) { mutableStateOf(inlineMode || disguised) }
    var speedIdx by remember(videoUrl) { mutableIntStateOf(1) } // 1.0x
    var durationMs by remember(videoUrl) { mutableIntStateOf(0) }
    var positionMs by remember(videoUrl) { mutableIntStateOf(0) }
    var videoWidth by remember(videoUrl) { mutableIntStateOf(0) }
    var videoHeight by remember(videoUrl) { mutableIntStateOf(0) }
    var showHud by remember(videoUrl) { mutableStateOf(true) }
    var hideRotateHint by remember(videoUrl) { mutableStateOf(false) }

    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }
    val surfaceRef = remember { mutableStateOf<Surface?>(null) }

    LaunchedEffect(userActivated, videoUrl, isBlurred, bossActive) {
        if (!userActivated || isBlurred || bossActive) return@LaunchedEffect
        if (cachedFile == null) {
            loadingFile = true
            loadError = null
            val f = api.fetchCachedMediaFile(videoUrl)
            loadingFile = false
            if (f == null) {
                loadError = "视频加载失败"
            } else {
                cachedFile = f
            }
        }
    }

    LaunchedEffect(isBlurred, bossActive) {
        if (isBlurred || bossActive) {
            mediaPlayerRef.value?.let { mp ->
                runCatching { mp.setVolume(0f, 0f) }
                if (runCatching { mp.isPlaying }.getOrDefault(false)) {
                    runCatching { mp.pause() }
                }
            }
            playing = false
        } else if (prepared) {
            val vol = if (muted) 0f else 1f
            mediaPlayerRef.value?.let { runCatching { it.setVolume(vol, vol) } }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                mediaPlayerRef.value?.let { mp ->
                    if (runCatching { mp.isPlaying }.getOrDefault(false)) {
                        runCatching { mp.pause() }
                    }
                }
                playing = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(cachedFile, surfaceRef.value) {
        val file = cachedFile ?: return@LaunchedEffect
        val surface = surfaceRef.value ?: return@LaunchedEffect
        if (mediaPlayerRef.value != null) return@LaunchedEffect

        val mp = MediaPlayer()
        mediaPlayerRef.value = mp
        runCatching {
            FileInputStream(file).use { fis ->
                mp.setDataSource(fis.fd)
            }
            mp.setSurface(surface)
            mp.isLooping = true
            mp.setOnVideoSizeChangedListener { _, w, h ->
                videoWidth = w
                videoHeight = h
            }
            mp.setOnPreparedListener { readyMp ->
                prepared = true
                durationMs = readyMp.duration.coerceAtLeast(0)
                val vol = if (muted) 0f else 1f
                readyMp.setVolume(vol, vol)
                val spd = VIDEO_SPEED_STEPS[speedIdx]
                if (spd != 1.0f) {
                    runCatching { readyMp.playbackParams = readyMp.playbackParams.setSpeed(spd) }
                }
                if (!isBlurred && !bossActive) {
                    readyMp.start()
                    playing = true
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                loadError = "视频解码失败"
                prepared = false
                playing = false
                true
            }
            mp.prepareAsync()
        }.onFailure {
            loadError = "无法播放该视频"
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            mediaPlayerRef.value?.let { mp ->
                runCatching { mp.stop() }
                runCatching { mp.release() }
            }
            mediaPlayerRef.value = null
            surfaceRef.value?.release()
            surfaceRef.value = null
        }
    }

    LaunchedEffect(playing, prepared) {
        while (playing && prepared) {
            mediaPlayerRef.value?.let { mp ->
                positionMs = runCatching { mp.currentPosition }.getOrDefault(positionMs)
            }
            delay(250L)
        }
    }

    fun toggleMute() {
        muted = !muted
        val v = if (muted) 0f else 1f
        mediaPlayerRef.value?.let { runCatching { it.setVolume(v, v) } }
    }

    fun togglePlayPause() {
        val mp = mediaPlayerRef.value ?: return
        if (!prepared) return
        if (playing) {
            runCatching { mp.pause() }
            playing = false
        } else {
            runCatching { mp.start() }
            playing = true
        }
    }

    fun cycleSpeed() {
        val nextIdx = (speedIdx + 1) % VIDEO_SPEED_STEPS.size
        speedIdx = nextIdx
        val mp = mediaPlayerRef.value ?: return
        if (!prepared) return
        val wasPlaying = runCatching { mp.isPlaying }.getOrDefault(false)
        runCatching {
            mp.playbackParams = mp.playbackParams.setSpeed(VIDEO_SPEED_STEPS[nextIdx])
            if (!wasPlaying) mp.pause()
        }
    }

    val aspectRatio = remember(videoWidth, videoHeight, inlineMode) {
        if (videoWidth > 0 && videoHeight > 0) {
            (videoWidth.toFloat() / videoHeight.toFloat()).coerceIn(0.4f, 2.4f)
        } else {
            16f / 9f
        }
    }

    if (inlineMode && (!userActivated || isBlurred)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color.Black)
                .clickable(enabled = !isBlurred) { userActivated = true },
            contentAlignment = Alignment.Center,
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    imageLoader = api.imageLoader,
                    contentDescription = "视频封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (!isBlurred) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "播放视频",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(34.dp),
                    )
                }
                if (onExpandFullscreen != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clickable { onExpandFullscreen() },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "全屏播放",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp),
                        )
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .let { mod ->
                if (inlineMode) {
                    mod
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                } else {
                    mod.fillMaxSize()
                }
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {
                if (inlineMode) togglePlayPause() else showHud = !showHud
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            surfaceRef.value = Surface(st)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            mediaPlayerRef.value?.let { mp ->
                                runCatching { mp.setSurface(null) }
                            }
                            surfaceRef.value?.release()
                            surfaceRef.value = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            },
            modifier = if (inlineMode) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(aspectRatio),
        )

        if (loadingFile || (!prepared && loadError == null)) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
        }

        loadError?.let { err ->
            Surface(color = Color.Black.copy(alpha = 0.72f), shape = RoundedCornerShape(10.dp)) {
                Text(
                    text = err,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (inlineMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.clickable { toggleMute() },
                ) {
                    Icon(
                        imageVector = if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (muted) "取消静音" else "静音",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp),
                    )
                }
                if (onExpandFullscreen != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier.clickable {
                            mediaPlayerRef.value?.let { mp ->
                                if (runCatching { mp.isPlaying }.getOrDefault(false)) {
                                    runCatching { mp.pause() }
                                }
                            }
                            playing = false
                            onExpandFullscreen()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "全屏播放",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp),
                        )
                    }
                }
            }
        } else if (showHud) {
            // Non-intrusive landscape suggestion when natural dimensions are horizontal (`02b §2`)
            if (videoWidth > videoHeight && !hideRotateHint) {
                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 14.dp, top = 64.dp)
                        .clickable { hideRotateHint = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("横屏视频 · 可旋转手机横屏观看", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            if (prepared && !playing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.clickable { togglePlayPause() },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(40.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.56f))
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Slider(
                    value = if (durationMs > 0) positionMs.coerceIn(0, durationMs).toFloat() else 0f,
                    onValueChange = { target ->
                        positionMs = target.toInt()
                    },
                    onValueChangeFinished = {
                        mediaPlayerRef.value?.let { mp ->
                            if (prepared) runCatching { mp.seekTo(positionMs) }
                        }
                    },
                    valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = palette.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { togglePlayPause() }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "暂停" else "播放",
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = "${formatDurationMs(positionMs)} / ${formatDurationMs(durationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable { cycleSpeed() },
                        ) {
                            Text(
                                text = "${VIDEO_SPEED_STEPS[speedIdx]}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        IconButton(onClick = { toggleMute() }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = if (muted) "取消静音" else "静音",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)
}

/**
 * Live Photo viewer (`02b §2` + `04 §3.2`): renders cover image with top-left `LIVE` pill;
 * pressing & holding plays the live video stream silently in place without popping any context menu.
 */
@Composable
fun LivePhotoStageBox(
    imageUrl: String,
    videoUrl: String,
    api: ForHappyApiClient,
    isBlurred: Boolean = false,
    contentScale: ContentScale = ContentScale.FillWidth,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    var holding by remember { mutableStateOf(false) }
    var cachedFile by remember(videoUrl) { mutableStateOf<File?>(null) }
    val surfaceRef = remember { mutableStateOf<Surface?>(null) }
    val mpRef = remember { mutableStateOf<MediaPlayer?>(null) }
    val curBlurred by rememberUpdatedState(isBlurred)
    val curTap by rememberUpdatedState(onTap)

    LaunchedEffect(holding, videoUrl, curBlurred) {
        if (holding && !curBlurred && cachedFile == null) {
            cachedFile = api.fetchCachedMediaFile(videoUrl)
        }
    }

    LaunchedEffect(holding, cachedFile, surfaceRef.value, curBlurred) {
        val file = cachedFile
        val surface = surfaceRef.value
        if (holding && !curBlurred && file != null && surface != null) {
            val mp = MediaPlayer()
            mpRef.value = mp
            runCatching {
                FileInputStream(file).use { mp.setDataSource(it.fd) }
                mp.setSurface(surface)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setOnPreparedListener { it.start() }
                mp.prepareAsync()
            }
        } else {
            mpRef.value?.let { mp ->
                runCatching { mp.stop() }
                runCatching { mp.release() }
            }
            mpRef.value = null
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            mpRef.value?.let { mp ->
                runCatching { mp.stop() }
                runCatching { mp.release() }
            }
            mpRef.value = null
            surfaceRef.value?.release()
            surfaceRef.value = null
        }
    }

    Box(
        modifier = modifier.pointerInput(videoUrl) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                if (curBlurred) {
                    waitForUpOrCancellation()
                    curTap?.invoke()
                    return@awaitEachGesture
                }
                val startMs = System.currentTimeMillis()
                holding = true
                waitForUpOrCancellation()
                holding = false
                if (System.currentTimeMillis() - startMs < 200L) {
                    curTap?.invoke()
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            imageLoader = api.imageLoader,
            contentDescription = "实况图片",
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )

        if (holding && !isBlurred) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                surfaceRef.value = Surface(st)
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                mpRef.value?.let { runCatching { it.setSurface(null) } }
                                surfaceRef.value?.release()
                                surfaceRef.value = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Surface(
            color = if (holding) Color(0xFF1F6B4F) else Color.Black.copy(alpha = 0.58f),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        ) {
            Text(
                text = if (holding) "LIVE 播放中" else "LIVE · 长按播放",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * Immersive full-screen stage (`02b §2.1`) supporting:
 * - Multi-item `HorizontalPager` (disabled when zoomed > 1.02x)
 * - Long-image vertical reading stage with right-side draggable scrollbar (`28.dp` from right edge)
 * - Pinch zoom (`1f..5f`), double-tap `2.5x`, pull-down to dismiss (`> 120dp`)
 * - Masked <-> Original toggle capsule with automatic reset on swipe / close / blur / background
 * - Save to system album (`⤓`) & optional synchronized bottom action bar (`♡ / ☺ / 「」 / 💬`)
 */
@Composable
fun ImmersiveMediaStage(
    mediaItems: List<MemeMediaItem>,
    initialIndex: Int = 0,
    api: ForHappyApiClient,
    meme: MemeItem? = null,
    forceBlurAll: Boolean = false,
    onLikeToggle: (() -> Unit)? = null,
    onOpenReactions: (() -> Unit)? = null,
    onOpenQuickReplies: (() -> Unit)? = null,
    onOpenComments: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    if (mediaItems.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val safeInitial = initialIndex.coerceIn(0, mediaItems.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { mediaItems.size })
    var showHud by remember { mutableStateOf(true) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var showOriginal by remember { mutableStateOf(false) }
    var pendingBlurAllOrigConfirm by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.currentPage, forceBlurAll) {
        showOriginal = false
        pendingBlurAllOrigConfirm = false
        currentZoom = 1f
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                showOriginal = false
                pendingBlurAllOrigConfirm = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2_200L)
            toastMsg = null
        }
    }

    val currentItem = mediaItems.getOrElse(pagerState.currentPage) { mediaItems.first() }
    val maskedUrl = remember(currentItem) { MediaUrlRewriter.rewriteMediaUrl(currentItem.resolvedFullUrl) }
    val originalUrl = remember(currentItem) { MediaUrlRewriter.rewriteMediaUrl(currentItem.o) }
    val activeSaveUrl = if (showOriginal && originalUrl.isNotBlank()) originalUrl else maskedUrl

    if (LocalBossKeyActive.current) return

    Dialog(
        onDismissRequest = {
            showOriginal = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = false)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentZoom <= 1.02f,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val item = mediaItems[page]
                val isCurrent = page == pagerState.currentPage
                val pageMaskedUrl = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.resolvedFullUrl) }
                val pageOrigUrl = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.o) }
                val pageUrl = if (isCurrent && showOriginal && pageOrigUrl.isNotBlank()) pageOrigUrl else pageMaskedUrl
                val isLong = (item.w ?: 0) > 0 && ((item.h ?: 0).toFloat() / item.w!!.toFloat()) >= 2.2f

                when {
                    item.isVideo && !item.v.isNullOrBlank() -> {
                        NativeVideoPlayer(
                            videoUrl = MediaUrlRewriter.rewriteMediaUrl(item.v),
                            posterUrl = pageUrl,
                            api = api,
                            inlineMode = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    item.isLive && !item.v.isNullOrBlank() -> {
                        LivePhotoStageBox(
                            imageUrl = pageUrl,
                            videoUrl = MediaUrlRewriter.rewriteMediaUrl(item.v),
                            api = api,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            onTap = { showHud = !showHud },
                        )
                    }
                    isLong -> {
                        LongImageReaderPage(
                            imageUrl = pageUrl,
                            api = api,
                            onTap = { showHud = !showHud },
                        )
                    }
                    else -> {
                        ZoomableImagePage(
                            imageUrl = pageUrl,
                            api = api,
                            onTap = { showHud = !showHud },
                            onZoomChanged = { if (isCurrent) currentZoom = it },
                            onDismiss = {
                                showOriginal = false
                                onDismiss()
                            },
                        )
                    }
                }
            }

            // Top HUD bar (`02b §2.1`: `✕`, `3 / 4`, `⤓`)
            if (showHud) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = CircleShape,
                    ) {
                        IconButton(
                            onClick = {
                                showOriginal = false
                                onDismiss()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                        }
                    }

                    if (mediaItems.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.56f),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${mediaItems.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = CircleShape,
                    ) {
                        IconButton(
                            onClick = {
                                if (activeSaveUrl.isBlank()) return@IconButton
                                scope.launch {
                                    saveMediaToSystemGallery(context, api, activeSaveUrl).fold(
                                        onSuccess = { toastMsg = it },
                                        onFailure = { toastMsg = it.message ?: "保存失败" },
                                    )
                                }
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "保存到相册", tint = Color.White)
                        }
                    }
                }
            }

            // Bottom HUD: masked/original capsule + optional synchronized meme interaction bar
            if (showHud) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (currentItem.hasOriginalUnmasked) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.68f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .border(
                                    width = if (showOriginal || pendingBlurAllOrigConfirm) 1.5.dp else 1.dp,
                                    color = if (showOriginal || pendingBlurAllOrigConfirm) palette.danger else Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(999.dp),
                                )
                                .clickable {
                                    if (showOriginal) {
                                        showOriginal = false
                                        pendingBlurAllOrigConfirm = false
                                    } else if (forceBlurAll && !pendingBlurAllOrigConfirm) {
                                        pendingBlurAllOrigConfirm = true
                                    } else {
                                        pendingBlurAllOrigConfirm = false
                                        showOriginal = true
                                    }
                                },
                        ) {
                            Text(
                                text = when {
                                    showOriginal -> "原图 ⇄ 打码版"
                                    pendingBlurAllOrigConfirm -> "已开全图模糊 · 再点确认看原图"
                                    else -> "打码版 ⇄ 原图"
                                },
                                color = if (showOriginal || pendingBlurAllOrigConfirm) Color(0xFFFCA5A5) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    if (meme != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable(enabled = onLikeToggle != null) { onLikeToggle?.invoke() },
                                ) {
                                    Icon(
                                        imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "赞",
                                        tint = if (meme.liked) palette.like else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = "${meme.totalLikes}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable(enabled = onOpenReactions != null) { onOpenReactions?.invoke() },
                                ) {
                                    Icon(Icons.Rounded.AddReaction, contentDescription = "贴纸回应", tint = Color.White, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "${meme.reactions.sumOf { it.count }}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable(enabled = onOpenQuickReplies != null) { onOpenQuickReplies?.invoke() },
                                ) {
                                    Text("「」", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${meme.replyCount}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable(enabled = onOpenComments != null) { onOpenComments?.invoke() },
                                ) {
                                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "评论", tint = Color.White, modifier = Modifier.size(19.dp))
                                    Text(
                                        text = "${meme.totalComments}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            toastMsg?.let { msg ->
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImagePage(
    imageUrl: String,
    api: ForHappyApiClient,
    onTap: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val pullThresholdPx = with(density) { 120.dp.toPx() }
    val edgeZonePx = with(density) { 24.dp.toPx() }

    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offsetX by remember(imageUrl) { mutableFloatStateOf(0f) }
    var offsetY by remember(imageUrl) { mutableFloatStateOf(0f) }
    var pullDismissY by remember(imageUrl) { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale) { onZoomChanged(scale) }

    fun clampOffsets(newScale: Float, candX: Float, candY: Float, size: IntSize): Pair<Float, Float> {
        val maxX = ((size.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
        val maxY = ((size.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
        return candX.coerceIn(-maxX, maxX) to candY.coerceIn(-maxY, maxY)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (1f - (pullDismissY / (pullThresholdPx * 3f))).coerceIn(0.45f, 1f)))
            .onSizeChanged { viewport = it }
            .pointerInput(imageUrl) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.15f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            val target = 2.5f
                            val rawX = (viewport.width / 2f - tapOffset.x) * (target - 1f)
                            val rawY = (viewport.height / 2f - tapOffset.y) * (target - 1f)
                            val (cx, cy) = clampOffsets(target, rawX, rawY, viewport)
                            scale = target
                            offsetX = cx
                            offsetY = cy
                        }
                    },
                )
            }
            .pointerInput(imageUrl) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startedNearEdge = down.position.x < edgeZonePx || down.position.x > (viewport.width - edgeZonePx)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = nextScale
                            pullDismissY = 0f
                            if (nextScale <= 1.01f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val (cx, cy) = clampOffsets(nextScale, offsetX + pan.x, offsetY + pan.y, viewport)
                                offsetX = cx
                                offsetY = cy
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pressed == 1) {
                            val pan = event.calculatePan()
                            if (scale > 1.02f && !startedNearEdge) {
                                val (cx, cy) = clampOffsets(scale, offsetX + pan.x, offsetY + pan.y, viewport)
                                offsetX = cx
                                offsetY = cy
                                if (pan.x != 0f || pan.y != 0f) {
                                    event.changes.forEach { it.consume() }
                                }
                            } else if (scale <= 1.02f && !startedNearEdge && abs(pan.y) > abs(pan.x) * 1.2f && (pullDismissY > 0f || pan.y > 0f)) {
                                pullDismissY = (pullDismissY + pan.y).coerceAtLeast(0f)
                                if (pan.y != 0f) {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (pullDismissY > pullThresholdPx && scale <= 1.02f) {
                        onDismiss()
                    } else {
                        pullDismissY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            imageLoader = api.imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY + pullDismissY
                },
        )
    }
}

@Composable
private fun LongImageReaderPage(
    imageUrl: String,
    api: ForHappyApiClient,
    onTap: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var zoomed by remember(imageUrl) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val trackHeightPx = with(LocalDensity.current) { (maxHeight - 180.dp).coerceAtLeast(120.dp).toPx() }
        val thumbHeightPx = with(LocalDensity.current) { 44.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .pointerInput(imageUrl) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { zoomed = !zoomed },
                    )
                },
        ) {
            AsyncImage(
                model = imageUrl,
                imageLoader = api.imageLoader,
                contentDescription = "长图",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = if (zoomed) 1.8f else 1f
                        scaleX = s
                        scaleY = s
                    },
            )
        }

        // Right-side draggable progress bar placed 28dp inset from edge (`04 §3.2`)
        if (scrollState.maxValue > 0) {
            val fraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            val usablePx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
            val thumbY = (fraction * usablePx).roundToInt()

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
                    .width(24.dp)
                    .height(with(LocalDensity.current) { trackHeightPx.toDp() })
                    .pointerInput(scrollState.maxValue) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaFrac = dragAmount / usablePx
                            val target = (scrollState.value + (deltaFrac * scrollState.maxValue)).roundToInt()
                                .coerceIn(0, scrollState.maxValue)
                            scope.launch { scrollState.scrollTo(target) }
                        }
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbY) }
                        .width(10.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.85f)),
                )
            }
        }
    }
}

/**
 * Backward-compatible entrypoint used by Chat, Disguise & Shell callers.
 */
@Composable
fun FullscreenMediaOverlay(
    imageUrl: String,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    videoUrl: String? = null,
) {
    val item = remember(imageUrl, videoUrl) {
        MemeMediaItem(
            u = imageUrl,
            t = imageUrl,
            v = videoUrl,
            k = if (!videoUrl.isNullOrBlank()) "v" else null,
        )
    }
    ImmersiveMediaStage(
        mediaItems = listOf(item),
        initialIndex = 0,
        api = api,
        onDismiss = onDismiss,
    )
}
