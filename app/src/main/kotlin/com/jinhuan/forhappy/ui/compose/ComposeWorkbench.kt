package com.jinhuan.forhappy.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.compose.COMPOSE_DRAFT_KV_KEY
import com.jinhuan.forhappy.core.compose.ComposeDraftSnapshot
import com.jinhuan.forhappy.core.compose.ComposeGenre
import com.jinhuan.forhappy.core.compose.ComposeLaunchRequest
import com.jinhuan.forhappy.core.compose.ComposeTrayItem
import com.jinhuan.forhappy.core.compose.MAX_COMPOSE_FILES
import com.jinhuan.forhappy.core.compose.PublishStrategy
import com.jinhuan.forhappy.core.compose.PuzzleDraftSpec
import com.jinhuan.forhappy.core.compose.PuzzleLayerDraft
import com.jinhuan.forhappy.core.compose.SocialLinkDetector
import com.jinhuan.forhappy.core.compose.SocialPreviewData
import com.jinhuan.forhappy.core.compose.computePublishButtonLabel
import com.jinhuan.forhappy.core.compose.pendingBatchesToPublish
import com.jinhuan.forhappy.core.compose.planPublishBatches
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.navigation.SharedIncomingPayload
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Stages `content://` URIs and local files into `cacheDir/compose_staging/`, extracting
 * video metadata/posters and downscaling large static images (>4MB or >4096px) while preserving
 * legible long-image widths (`02d §1.1`).
 */
object ComposeMediaStager {

    private const val SHRINK_THRESHOLD_BYTES = 4L * 1024L * 1024L
    private const val SHRINK_MAX_EDGE = 4096

    suspend fun stageUri(
        context: Context,
        uri: Uri,
        existingId: String = UUID.randomUUID().toString(),
    ): ComposeTrayItem? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "compose_staging").apply { mkdirs() }
        val mime = context.contentResolver.getType(uri)?.lowercase() ?: "image/jpeg"
        val displayName = resolveDisplayName(context, uri)
            ?: "media_${System.currentTimeMillis()}.${guessExtension(mime, null)}"
        val ext = guessExtension(mime, displayName)
        val rawFile = File(dir, "raw_${existingId}.$ext")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                rawFile.outputStream().use { output -> input.copyTo(output) }
            }
            rawFile.exists() && rawFile.length() > 0L
        }.getOrDefault(false)
        if (!copied) return@withContext null
        stagePreparedFile(
            stagingDir = dir,
            rawFile = rawFile,
            mime = mime,
            displayName = displayName,
            id = existingId,
        )
    }

    suspend fun stageFile(
        context: Context,
        sourceFile: File,
        existingId: String = UUID.randomUUID().toString(),
    ): ComposeTrayItem? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) return@withContext null
        val dir = File(context.cacheDir, "compose_staging").apply { mkdirs() }
        val ext = sourceFile.extension.lowercase().ifBlank { "jpg" }
        val mime = when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> "image/jpeg"
        }
        val rawFile = File(dir, "raw_${existingId}.$ext")
        if (sourceFile.absolutePath != rawFile.absolutePath) {
            sourceFile.copyTo(rawFile, overwrite = true)
        }
        stagePreparedFile(
            stagingDir = dir,
            rawFile = rawFile,
            mime = mime,
            displayName = sourceFile.name.ifBlank { rawFile.name },
            id = existingId,
        )
    }

    private fun stagePreparedFile(
        stagingDir: File,
        rawFile: File,
        mime: String,
        displayName: String,
        id: String,
    ): ComposeTrayItem {
        val ext = rawFile.extension.lowercase()
        val isVideo = mime.startsWith("video/") || ext in setOf("mp4", "mov", "webm")
        val isGif = mime == "image/gif" || ext == "gif"
        val origBytes = rawFile.length()

        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            var w = 0
            var h = 0
            var durSec = 0
            var posterFile: File? = null
            runCatching {
                retriever.setDataSource(rawFile.absolutePath)
                w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                durSec = (durMs / 1000L).toInt().coerceAtLeast(0)
                val frame = retriever.getFrameAtTime(0L)
                if (frame != null) {
                    val target = File(stagingDir, "poster_${id}.jpg")
                    target.outputStream().use { out ->
                        frame.compress(Bitmap.CompressFormat.JPEG, 84, out)
                    }
                    frame.recycle()
                    if (target.exists() && target.length() > 0L) posterFile = target
                }
            }
            runCatching { retriever.release() }
            return ComposeTrayItem(
                id = id,
                localFilePath = rawFile.absolutePath,
                posterLocalPath = posterFile?.absolutePath,
                mimeType = if (mime.startsWith("video/")) mime else "video/mp4",
                fileName = displayName,
                originalBytes = origBytes,
                compressedBytes = origBytes,
                width = w,
                height = h,
                durationSec = durSec,
                isVideo = true,
            )
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(rawFile.absolutePath, bounds)
        val w = bounds.outWidth.coerceAtLeast(0)
        val h = bounds.outHeight.coerceAtLeast(0)

        if (isGif || w <= 0 || h <= 0) {
            return ComposeTrayItem(
                id = id,
                localFilePath = rawFile.absolutePath,
                mimeType = mime,
                fileName = displayName,
                originalBytes = origBytes,
                compressedBytes = origBytes,
                width = w,
                height = h,
                isGif = isGif,
            )
        }

        val isTall = (h.toFloat() / w.toFloat() > 3f) || (w.toFloat() / h.toFloat() > 3f)
        val needsScale = if (isTall) {
            minOf(w, h) > 2048
        } else {
            maxOf(w, h) > SHRINK_MAX_EDGE
        }
        val needsShrink = needsScale || origBytes > SHRINK_THRESHOLD_BYTES
        if (!needsShrink) {
            return ComposeTrayItem(
                id = id,
                localFilePath = rawFile.absolutePath,
                mimeType = mime,
                fileName = displayName,
                originalBytes = origBytes,
                compressedBytes = origBytes,
                width = w,
                height = h,
            )
        }

        val scale = if (isTall) {
            minOf(1f, 2048f / minOf(w, h).toFloat())
        } else {
            minOf(1f, SHRINK_MAX_EDGE.toFloat() / maxOf(w, h).toFloat())
        }
        var sample = 1
        while (w / (sample * 2) >= (w * scale) && h / (sample * 2) >= (h * scale)) {
            sample *= 2
        }
        val decoded = runCatching {
            BitmapFactory.decodeFile(
                rawFile.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        }.getOrNull() ?: return ComposeTrayItem(
            id = id,
            localFilePath = rawFile.absolutePath,
            mimeType = mime,
            fileName = displayName,
            originalBytes = origBytes,
            compressedBytes = origBytes,
            width = w,
            height = h,
        )

        val targetW = (w * scale).roundToInt().coerceAtLeast(1)
        val targetH = (h * scale).roundToInt().coerceAtLeast(1)
        val finalBmp = if (decoded.width > targetW || decoded.height > targetH) {
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else decoded

        val compressedFile = File(stagingDir, "shrunk_${id}.jpg")
        compressedFile.outputStream().use { out ->
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val outW = finalBmp.width
        val outH = finalBmp.height
        finalBmp.recycle()

        val useCompressed = compressedFile.exists() &&
            compressedFile.length() > 0L &&
            (needsScale || compressedFile.length() < origBytes)

        return if (useCompressed) {
            ComposeTrayItem(
                id = id,
                localFilePath = compressedFile.absolutePath,
                mimeType = "image/jpeg",
                fileName = displayName.substringBeforeLast('.') + ".jpg",
                originalBytes = origBytes,
                compressedBytes = compressedFile.length(),
                width = outW,
                height = outH,
            )
        } else {
            if (compressedFile.exists()) compressedFile.delete()
            ComposeTrayItem(
                id = id,
                localFilePath = rawFile.absolutePath,
                mimeType = mime,
                fileName = displayName,
                originalBytes = origBytes,
                compressedBytes = origBytes,
                width = w,
                height = h,
            )
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() } else null
        }
    }.getOrNull()

    private fun guessExtension(mime: String, name: String?): String {
        val extFromName = name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.length in 2..5 }
        if (extFromName != null) return extFromName
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            mime.contains("mp4") -> "mp4"
            mime.contains("quicktime") -> "mov"
            mime.contains("webm") -> "webm"
            else -> "jpg"
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComposeWorkbench(
    db: ForHappyDatabase,
    api: ForHappyApiClient,
    me: MeUser?,
    launchRequest: ComposeLaunchRequest,
    onPublished: (List<Long>) -> Unit,
    onEdited: (MemeItem?) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val editingMeme = launchRequest.editingMeme
    val isEditing = editingMeme != null

    var genre by remember(launchRequest.triggerSeq) {
        mutableStateOf(
            when {
                editingMeme?.kind == "text" -> ComposeGenre.TEXT
                editingMeme != null -> ComposeGenre.IMAGE
                else -> launchRequest.initialGenre
            },
        )
    }
    var strategy by remember(launchRequest.triggerSeq) { mutableStateOf(PublishStrategy.MERGE_ONE) }
    var trayItems by remember(launchRequest.triggerSeq) {
        mutableStateOf(
            editingMeme?.mediaItems?.mapIndexed { idx, media ->
                ComposeTrayItem(
                    id = "kept_${editingMeme.id}_$idx",
                    keptRemoteUrl = MediaUrlRewriter.rewriteMediaUrl(media.resolvedFullUrl),
                    keptRemoteOrigUrl = media.o?.takeIf { it.isNotBlank() }?.let { MediaUrlRewriter.rewriteMediaUrl(it) },
                    width = media.w ?: 0,
                    height = media.h ?: 0,
                    isVideo = media.isVideo,
                    isGif = media.isGif,
                    isLive = media.isLive,
                    uploadState = "ok",
                )
            }.orEmpty(),
        )
    }
    var title by remember(launchRequest.triggerSeq) {
        mutableStateOf(editingMeme?.title ?: launchRequest.initialTitle)
    }
    var contentField by remember(launchRequest.triggerSeq) {
        val initBody = editingMeme?.content ?: launchRequest.initialContent
        mutableStateOf(TextFieldValue(initBody, TextRange(initBody.length)))
    }
    var selectedTags by remember(launchRequest.triggerSeq) {
        mutableStateOf(editingMeme?.tags ?: launchRequest.initialTags)
    }
    var spoiler by remember(launchRequest.triggerSeq) { mutableStateOf(editingMeme?.spoiler == true) }
    var aiOff by remember(launchRequest.triggerSeq) { mutableStateOf(false) }
    var aiStatusNote by remember { mutableStateOf("仅公开非敏感图文") }
    var aiToggleAllowed by remember { mutableStateOf(true) }

    var shareRawUrl by remember(launchRequest.triggerSeq) { mutableStateOf(launchRequest.initialShareUrl) }
    var linkPreview by remember(launchRequest.triggerSeq) { mutableStateOf<SocialPreviewData?>(null) }
    var linkLoadedRaw by remember(launchRequest.triggerSeq) { mutableStateOf("") }
    var loadingLink by remember { mutableStateOf(false) }
    var linkStatusText by remember { mutableStateOf<String?>(null) }

    var puzzleDraft by remember(launchRequest.triggerSeq) { mutableStateOf(PuzzleDraftSpec()) }
    var showPuzzleSheet by remember { mutableStateOf(false) }

    var showTextMediaTray by remember(launchRequest.triggerSeq) {
        mutableStateOf(editingMeme?.kind == "text" && editingMeme.mediaItems.isNotEmpty())
    }
    var textPreviewMode by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var stickerPacks by remember { mutableStateOf<List<StickerPack>>(emptyList()) }

    var popularTags by remember { mutableStateOf<List<String>>(listOf("梗图", "日常", "猫猫", "职场", "ACG", "视频")) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var customTagDraft by remember { mutableStateOf("") }

    var clipboardBanner by remember { mutableStateOf<com.jinhuan.forhappy.core.compose.DetectedSocialLink?>(null) }
    var pendingDraftBanner by remember { mutableStateOf<ComposeDraftSnapshot?>(null) }

    var maskEditorTargetIndex by remember { mutableStateOf<Int?>(null) }
    var activeCardMenuId by remember { mutableStateOf<String?>(null) }
    var replaceTargetId by remember { mutableStateOf<String?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var draggingAccumX by remember { mutableFloatStateOf(0f) }

    var publishing by remember { mutableStateOf(false) }
    var publishStatusText by remember { mutableStateOf<String?>(null) }
    var showExitGuardDialog by remember { mutableStateOf(false) }
    var pendingGenreSwitch by remember { mutableStateOf<ComposeGenre?>(null) }

    val multiMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_COMPOSE_FILES),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val remaining = (MAX_COMPOSE_FILES - trayItems.size).coerceAtLeast(0)
                val staged = uris.take(remaining).mapNotNull { u -> ComposeMediaStager.stageUri(context, u) }
                if (staged.isNotEmpty()) {
                    trayItems = (trayItems + staged).take(MAX_COMPOSE_FILES)
                }
            }
        }
    }

    val singleReplacePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val targetId = replaceTargetId
        replaceTargetId = null
        if (uri != null && targetId != null) {
            scope.launch {
                val staged = ComposeMediaStager.stageUri(context, uri) ?: return@launch
                trayItems = trayItems.map { old -> if (old.id == targetId) staged else old }
            }
        }
    }

    fun launchMultiPicker() {
        StealthExemptionGate.markInternalJump()
        multiMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    fun loadSocialLink(rawInput: String) {
        val clean = rawInput.trim()
        if (clean.isBlank()) return
        loadingLink = true
        linkStatusText = "正在读取内容…"
        linkPreview = null
        linkLoadedRaw = ""
        scope.launch {
            api.fetchSocialPreview(clean).fold(
                onSuccess = { preview ->
                    loadingLink = false
                    if (!preview.available) {
                        linkStatusText = "暂未取得公开内容，请稍后重试"
                    } else {
                        linkPreview = preview
                        linkLoadedRaw = clean
                        if (contentField.text.isBlank() && preview.description.isNotBlank()) {
                            contentField = TextFieldValue(preview.description, TextRange(preview.description.length))
                        }
                        if (preview.sensitive) spoiler = true
                        linkStatusText = if (preview.embedUrl != null) "内容已就绪，可补充配文后发布" else "内容已读取，可编辑后发布"
                    }
                },
                onFailure = { err ->
                    loadingLink = false
                    linkStatusText = err.message ?: "链接读取失败"
                },
            )
        }
    }

    // Initialization: tags, AI status, incoming shared URIs/paths, clipboard link, draft check, auto picker
    LaunchedEffect(launchRequest.triggerSeq) {
        launch {
            val fetched = api.fetchTags(hideNsfw = false).map { it.tag }.filter { it.isNotBlank() }
            if (fetched.isNotEmpty()) popularTags = (popularTags + fetched).distinct().take(18)
        }
        launch {
            val (cfgImg, prefsImg) = api.fetchAiComposeStatus()
            aiToggleAllowed = cfgImg && prefsImg
            aiStatusNote = when {
                !cfgImg -> "全站 AI 识图暂未开启"
                !prefsImg -> "已在个人偏好中关闭"
                else -> "仅公开非敏感图文"
            }
            if (!aiToggleAllowed) aiOff = true
        }

        if (!isEditing && launchRequest.sharedUris.isNotEmpty()) {
            val staged = launchRequest.sharedUris.take(MAX_COMPOSE_FILES).mapNotNull { uriStr ->
                runCatching { ComposeMediaStager.stageUri(context, Uri.parse(uriStr)) }.getOrNull()
            }
            if (staged.isNotEmpty()) {
                trayItems = staged
            }
        } else if (!isEditing && launchRequest.preStagedPaths.isNotEmpty()) {
            val staged = launchRequest.preStagedPaths.take(MAX_COMPOSE_FILES).mapNotNull { path ->
                ComposeMediaStager.stageFile(context, File(path))
            }
            if (staged.isNotEmpty()) {
                trayItems = staged
            }
        } else if (!isEditing && launchRequest.initialShareUrl.isNotBlank()) {
            genre = ComposeGenre.LINK
            loadSocialLink(launchRequest.initialShareUrl)
        } else if (!isEditing && launchRequest.autoOpenPicker) {
            launchMultiPicker()
        }

        if (!isEditing &&
            launchRequest.sharedUris.isEmpty() &&
            launchRequest.preStagedPaths.isEmpty() &&
            launchRequest.initialShareUrl.isBlank() &&
            launchRequest.initialContent.isBlank()
        ) {
            clipboardBanner = SocialLinkDetector.detectFirstSocialLink(clipboard.getText()?.text)
            val savedRaw = db.getKv(COMPOSE_DRAFT_KV_KEY)
            if (!savedRaw.isNullOrBlank()) {
                val snap = runCatching { ForHappyJson.decodeFromString(ComposeDraftSnapshot.serializer(), savedRaw) }.getOrNull()
                if (snap != null && (snap.trayItems.isNotEmpty() || snap.title.isNotBlank() || snap.content.isNotBlank() || snap.shareRawUrl.isNotBlank())) {
                    pendingDraftBanner = snap
                }
            }
        }
    }

    val isDirty = !isEditing && (
        trayItems.isNotEmpty() ||
            title.isNotBlank() ||
            contentField.text.isNotBlank() ||
            selectedTags.isNotEmpty() ||
            shareRawUrl.isNotBlank() ||
            puzzleDraft.layers.isNotEmpty()
        )

    fun saveDraftAndDismiss() {
        scope.launch {
            val snap = ComposeDraftSnapshot(
                genre = genre,
                strategy = strategy,
                trayItems = trayItems.filter { it.postedMemeId == null },
                title = title,
                content = contentField.text,
                tags = selectedTags,
                spoiler = spoiler,
                aiOff = aiOff,
                shareRawUrl = shareRawUrl,
                puzzleEnabled = puzzleDraft.layers.isNotEmpty(),
                puzzle = puzzleDraft,
                savedAtMs = System.currentTimeMillis(),
            )
            db.putKv(COMPOSE_DRAFT_KV_KEY, ForHappyJson.encodeToString(snap))
            onDismiss()
        }
    }

    fun discardDraftAndDismiss() {
        scope.launch {
            db.putKv(COMPOSE_DRAFT_KV_KEY, "")
            onDismiss()
        }
    }

    fun attemptClose() {
        if (publishing) return
        if (isDirty) {
            showExitGuardDialog = true
        } else {
            onDismiss()
        }
    }

    suspend fun uploadSingleTrayItem(item: ComposeTrayItem): ComposeTrayItem? {
        if (item.isOverLimit) {
            publishStatusText = "${item.fileName} 超过 20MB，请压缩或移除"
            return null
        }
        if (item.keptRemoteUrl != null) return item

        // If already uploaded (same content)
        val mainNeedsUpload = item.uploadedKey.isNullOrBlank()
        var next = item.copy(uploadState = "uploading", uploadError = null)

        if (mainNeedsUpload) {
            val filePath = item.maskedFilePath ?: item.localFilePath ?: return null
            val mainFile = File(filePath)
            if (!mainFile.exists()) return null
            val bytes = withContext(Dispatchers.IO) { mainFile.readBytes() }
            val res = api.uploadMediaBytes(bytes, item.fileName, item.mimeType, forWhat = "meme")
            val uploaded = res.getOrElse { err ->
                publishStatusText = err.message ?: "上传失败"
                return next.copy(uploadState = "failed", uploadError = err.message)
            }
            next = next.copy(uploadedKey = uploaded.key)
        }

        if (next.isVideo && next.posterLocalPath != null && next.uploadedPosterKey.isNullOrBlank()) {
            val posterFile = File(next.posterLocalPath)
            if (posterFile.exists()) {
                val pBytes = withContext(Dispatchers.IO) { posterFile.readBytes() }
                val pRes = api.uploadMediaBytes(pBytes, posterFile.name, "image/jpeg", forWhat = "meme")
                pRes.getOrNull()?.let { next = next.copy(uploadedPosterKey = it.key) }
            }
        }

        if (next.isMasked && next.keepOriginalUnmasked && next.localFilePath != null && next.uploadedOrigKey.isNullOrBlank()) {
            val origFile = File(next.localFilePath)
            if (origFile.exists()) {
                val oBytes = withContext(Dispatchers.IO) { origFile.readBytes() }
                val oRes = api.uploadMediaBytes(oBytes, origFile.name, "image/jpeg", forWhat = "meme")
                oRes.getOrElse { err ->
                    publishStatusText = err.message ?: "原图上传失败"
                    return next.copy(uploadState = "failed", uploadError = err.message)
                }.let { next = next.copy(uploadedOrigKey = it.key) }
            }
        }
        return next.copy(uploadState = "ok")
    }

    fun trayItemsToJsonArray(items: List<ComposeTrayItem>): JsonArray = JsonArray(
        items.map { it ->
            buildJsonObject {
                if (it.keptRemoteUrl != null && !it.isMasked) {
                    put("keep", JsonPrimitive(it.keptRemoteUrl))
                } else {
                    put("key", JsonPrimitive(it.uploadedKey.orEmpty()))
                    if (it.uploadedPosterKey != null) put("poster", JsonPrimitive(it.uploadedPosterKey))
                    if (it.width > 0) put("w", JsonPrimitive(it.width))
                    if (it.height > 0) put("h", JsonPrimitive(it.height))
                    if (it.isLive) put("kind", JsonPrimitive("l"))
                    if (it.uploadedOrigKey != null) put("orig", JsonPrimitive(it.uploadedOrigKey))
                }
            }
        },
    )

    fun submitPublish() {
        if (publishing) return
        if (me == null) {
            publishStatusText = "请先登录后再发布"
            return
        }

        scope.launch {
            publishing = true
            publishStatusText = "准备发布…"

            // Edit mode
            if (isEditing && editingMeme != null) {
                if (genre != ComposeGenre.TEXT && trayItems.isEmpty()) {
                    publishStatusText = "请至少保留一张媒体"
                    publishing = false
                    return@launch
                }
                val updatedTray = trayItems.toMutableList()
                for (i in updatedTray.indices) {
                    val cur = updatedTray[i]
                    if (cur.keptRemoteUrl == null || cur.isMasked) {
                        val up = uploadSingleTrayItem(cur)
                        if (up == null || up.uploadState == "failed") {
                            if (up != null) updatedTray[i] = up
                            trayItems = updatedTray
                            publishing = false
                            return@launch
                        }
                        updatedTray[i] = up
                    }
                }
                trayItems = updatedTray
                val itemsChanged = updatedTray.any { it.keptRemoteUrl == null || it.isMasked } ||
                    updatedTray.mapNotNull { it.keptRemoteUrl } != editingMeme.mediaItems.map { MediaUrlRewriter.rewriteMediaUrl(it.resolvedFullUrl) }

                api.patchMemeFull(
                    memeId = editingMeme.id,
                    title = title.trim(),
                    content = contentField.text.trim(),
                    tags = selectedTags,
                    spoiler = spoiler,
                    aiOff = if (aiToggleAllowed) aiOff else null,
                    items = if (itemsChanged) trayItemsToJsonArray(updatedTray) else null,
                ).fold(
                    onSuccess = { updated ->
                        publishing = false
                        onEdited(updated)
                        onDismiss()
                    },
                    onFailure = { err ->
                        publishStatusText = err.message ?: "保存修改失败"
                        publishing = false
                    },
                )
                return@launch
            }

            // Create mode: LINK
            if (genre == ComposeGenre.LINK) {
                if (linkPreview == null || linkLoadedRaw != shareRawUrl.trim()) {
                    publishStatusText = "请先点击「读取内容」获取链接预览"
                    publishing = false
                    return@launch
                }
                api.createMemeFull(
                    title = title.trim(),
                    content = contentField.text.trim(),
                    tags = selectedTags,
                    spoiler = spoiler,
                    shareUrl = linkLoadedRaw,
                ).fold(
                    onSuccess = { id ->
                        db.putKv(COMPOSE_DRAFT_KV_KEY, "")
                        publishing = false
                        onPublished(listOf(id))
                        onDismiss()
                    },
                    onFailure = { err ->
                        publishStatusText = err.message ?: "发布失败"
                        publishing = false
                    },
                )
                return@launch
            }

            // Create mode: TEXT
            if (genre == ComposeGenre.TEXT) {
                if (title.isBlank() && contentField.text.isBlank()) {
                    publishStatusText = "请写点标题或正文吧"
                    publishing = false
                    return@launch
                }
                val updatedTray = trayItems.toMutableList()
                for (i in updatedTray.indices) {
                    val up = uploadSingleTrayItem(updatedTray[i])
                    if (up == null || up.uploadState == "failed") {
                        if (up != null) updatedTray[i] = up
                        trayItems = updatedTray
                        publishing = false
                        return@launch
                    }
                    updatedTray[i] = up
                }
                trayItems = updatedTray
                api.createMemeFull(
                    kind = "text",
                    title = title.trim(),
                    content = contentField.text.trim(),
                    tags = selectedTags,
                    spoiler = spoiler,
                    layout = "grid",
                    items = trayItemsToJsonArray(updatedTray),
                ).fold(
                    onSuccess = { id ->
                        db.putKv(COMPOSE_DRAFT_KV_KEY, "")
                        publishing = false
                        onPublished(listOf(id))
                        onDismiss()
                    },
                    onFailure = { err ->
                        publishStatusText = err.message ?: "发布失败"
                        publishing = false
                    },
                )
                return@launch
            }

            // Create mode: IMAGE (single, carousel, or split batches with breakpoint retry)
            if (trayItems.isEmpty()) {
                publishStatusText = "请先选择图片或视频"
                publishing = false
                return@launch
            }
            if (trayItems.any { it.isOverLimit }) {
                publishStatusText = "存在超过 20MB 的文件，请移除或替换"
                publishing = false
                return@launch
            }

            // 1. Upload un-uploaded tray items first
            val uploadedTray = trayItems.toMutableList()
            for (i in uploadedTray.indices) {
                if (uploadedTray[i].postedMemeId != null) continue
                publishStatusText = "正在上传 (${i + 1}/${uploadedTray.size})…"
                val up = uploadSingleTrayItem(uploadedTray[i])
                if (up == null || up.uploadState == "failed") {
                    if (up != null) uploadedTray[i] = up
                    trayItems = uploadedTray
                    publishing = false
                    return@launch
                }
                uploadedTray[i] = up
                trayItems = uploadedTray.toList()
            }

            // 2. Plan batches and publish only pending ones (`postedMemeId == null`)
            val batches = planPublishBatches(uploadedTray, strategy)
            val pending = pendingBatchesToPublish(batches)
            val publishedIds = batches.mapNotNull { it.alreadyPostedId }.toMutableList()

            for ((pIdx, batch) in pending.withIndex()) {
                publishStatusText = if (pending.size > 1) {
                    "正在发布 ${pIdx + 1}/${pending.size}…"
                } else {
                    "正在发布…"
                }
                val batchItemIds = batch.items.map { it.id }.toSet()
                val res = api.createMemeFull(
                    title = title.trim(),
                    content = contentField.text.trim(),
                    tags = selectedTags,
                    spoiler = spoiler,
                    aiOff = if (aiToggleAllowed) aiOff else false,
                    layout = "carousel",
                    items = trayItemsToJsonArray(batch.items),
                )
                val newId = res.getOrElse { err ->
                    publishStatusText = if (publishedIds.isNotEmpty()) {
                        "已发出 ${publishedIds.size} 帖，剩余 ${pending.size - pIdx} 帖失败：${err.message}"
                    } else {
                        err.message ?: "发布失败"
                    }
                    publishing = false
                    return@launch
                }
                publishedIds += newId
                trayItems = trayItems.map { item ->
                    if (item.id in batchItemIds) item.copy(postedMemeId = newId) else item
                }
            }

            // 3. Attach puzzle if configured on first published post
            if (puzzleDraft.layers.isNotEmpty() && publishedIds.isNotEmpty()) {
                publishStatusText = "正在挂载解密谜题…"
                val resolvedLayers = puzzleDraft.layers.map { l ->
                    if ((l.kind == "image" || l.kind == "stego") && l.clueLocalPath != null && l.uploadedImgUrl.isNullOrBlank()) {
                        val f = File(l.clueLocalPath)
                        if (f.exists()) {
                            val bytes = withContext(Dispatchers.IO) { f.readBytes() }
                            val up = api.uploadMediaBytes(bytes, f.name, "image/jpeg", forWhat = "meme").getOrNull()
                            if (up != null) l.copy(uploadedImgUrl = up.url) else l
                        } else l
                    } else l
                }
                val pzRes = api.attachMemePuzzle(publishedIds.first(), puzzleDraft.copy(layers = resolvedLayers))
                if (pzRes.isFailure) {
                    publishStatusText = "帖子已发（No.${publishedIds.first()}），谜题挂载失败：${pzRes.exceptionOrNull()?.message}"
                }
            }

            db.putKv(COMPOSE_DRAFT_KV_KEY, "")
            publishing = false
            onPublished(publishedIds)
            onDismiss()
        }
    }

    val currentBatches = remember(trayItems, strategy) { planPublishBatches(trayItems, strategy) }
    val publishLabel = remember(genre, currentBatches, isEditing) {
        if (isEditing) "保存修改" else computePublishButtonLabel(genre, currentBatches)
    }

    Dialog(
        onDismissRequest = { attemptClose() },
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler { attemptClose() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
        ) {
            // Top Bar: ✕  发帖 / 编辑  (●图文 | 文字 | 链接)
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { attemptClose() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                        }
                        Text(
                            text = if (isEditing) "编辑 No.${editingMeme?.id}" else "发帖",
                            color = palette.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (!isEditing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            listOf(
                                ComposeGenre.IMAGE to "图文",
                                ComposeGenre.TEXT to "文字",
                                ComposeGenre.LINK to "链接",
                            ).forEach { (g, label) ->
                                val selected = genre == g
                                Surface(
                                    color = if (selected) palette.primaryContainer else palette.surface2,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (selected) palette.primary else palette.line,
                                            RoundedCornerShape(999.dp),
                                        )
                                        .clickable(enabled = !publishing) {
                                            if (g != genre) {
                                                if (trayItems.isNotEmpty() && g == ComposeGenre.LINK) {
                                                    pendingGenreSwitch = g
                                                } else {
                                                    genre = g
                                                }
                                            }
                                        },
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) palette.primary else palette.muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = palette.line)

            // Main scrollable workspace
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Pending draft restore banner
                pendingDraftBanner?.let { snap ->
                    item(key = "draft_banner") {
                        Surface(
                            color = palette.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "有 1 份草稿 · 继续编辑",
                                    color = palette.text,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        genre = snap.genre
                                        strategy = snap.strategy
                                        trayItems = snap.trayItems
                                        title = snap.title
                                        contentField = TextFieldValue(snap.content, TextRange(snap.content.length))
                                        selectedTags = snap.tags
                                        spoiler = snap.spoiler
                                        aiOff = snap.aiOff
                                        shareRawUrl = snap.shareRawUrl
                                        puzzleDraft = snap.puzzle
                                        pendingDraftBanner = null
                                    }) {
                                        Text("恢复", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    TextButton(onClick = {
                                        pendingDraftBanner = null
                                        scope.launch { db.putKv(COMPOSE_DRAFT_KV_KEY, "") }
                                    }) {
                                        Text("丢弃", color = palette.muted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Clipboard social link banner
                clipboardBanner?.let { detected ->
                    item(key = "clip_banner") {
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "检测到剪贴板链接 ${detected.hostLabel}",
                                        color = palette.text,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = detected.url,
                                        color = palette.muted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Surface(
                                    color = palette.primary,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.clickable {
                                        genre = ComposeGenre.LINK
                                        shareRawUrl = detected.url
                                        clipboardBanner = null
                                        loadSocialLink(detected.url)
                                    },
                                ) {
                                    Text(
                                        text = "生成分享帖",
                                        color = palette.onPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { clipboardBanner = null },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "忽略", tint = palette.muted, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }

                // LINK MODE
                if (genre == ComposeGenre.LINK) {
                    item(key = "link_editor") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = shareRawUrl,
                                onValueChange = {
                                    shareRawUrl = it
                                    if (it.trim() != linkLoadedRaw) {
                                        linkPreview = null
                                    }
                                },
                                label = { Text("粘贴 B站 / 小红书 / X / 抖音等分享链接") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = linkStatusText ?: "点击读取内容后预览发布",
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    enabled = !loadingLink && shareRawUrl.isNotBlank(),
                                    onClick = { loadSocialLink(shareRawUrl) },
                                ) {
                                    if (loadingLink) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = palette.onPrimary)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(if (loadingLink) "读取中" else "读取内容")
                                }
                            }

                            linkPreview?.let { pv ->
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.Link, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                            Text("内容预览就绪", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (pv.description.isNotBlank()) {
                                            Text(pv.description, color = palette.text, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (pv.media.isNotEmpty()) {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(pv.media) { m ->
                                                    AsyncImage(
                                                        model = MediaUrlRewriter.rewriteMediaUrl(m.poster ?: m.url),
                                                        imageLoader = api.imageLoader,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(84.dp)
                                                            .clip(RoundedCornerShape(10.dp)),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MEDIA TRAY (IMAGE mode, or TEXT mode when user chose "+ 配图")
                if (genre == ComposeGenre.IMAGE || (genre == ComposeGenre.TEXT && showTextMediaTray)) {
                    item(key = "media_tray_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (genre == ComposeGenre.IMAGE && trayItems.size >= 2 && !isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("发布方式", color = palette.muted, fontSize = 12.sp)
                                    listOf(
                                        PublishStrategy.MERGE_ONE to "合并一帖",
                                        PublishStrategy.SOLO_EACH to "每张单发",
                                        PublishStrategy.CUSTOM to "自定义",
                                    ).forEach { (st, label) ->
                                        FilterChip(
                                            selected = strategy == st,
                                            onClick = { strategy = st },
                                            label = { Text(label, fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = palette.primaryContainer,
                                                selectedLabelColor = palette.primary,
                                            ),
                                        )
                                    }
                                }
                            }

                            if (trayItems.isEmpty() && genre == ComposeGenre.IMAGE) {
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line, RoundedCornerShape(16.dp))
                                        .clickable { launchMultiPicker() },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 28.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Rounded.Image, contentDescription = null, tint = palette.primary, modifier = Modifier.size(32.dp))
                                        Text("点击从相册选择图片 / 视频 / GIF", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text("最多 $MAX_COMPOSE_FILES 个 · 单个 ≤ 20MB · 大图自动压缩", color = palette.muted, fontSize = 11.5.sp)
                                    }
                                }
                            } else {
                                val stepPx = with(LocalDensity.current) { 82.dp.toPx() }
                                val latestTray by rememberUpdatedState(trayItems)

                                LazyRow(
                                    userScrollEnabled = draggingId == null,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        if (genre == ComposeGenre.IMAGE && strategy == PublishStrategy.SOLO_EACH) 14.dp else 4.dp,
                                    ),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    itemsIndexed(trayItems, key = { _, it -> it.id }) { idx, item ->
                                        val isBeingDragged = draggingId == item.id
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .offset {
                                                        IntOffset(if (isBeingDragged) draggingAccumX.roundToInt() else 0, 0)
                                                    }
                                                    .pointerInput(item.id) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                draggingId = item.id
                                                                draggingAccumX = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                draggingAccumX += dragAmount.x
                                                                val curList = latestTray
                                                                val curIdx = curList.indexOfFirst { it.id == item.id }
                                                                if (curIdx >= 0) {
                                                                    if (draggingAccumX > stepPx && curIdx < curList.lastIndex) {
                                                                        val swapped = curList.toMutableList()
                                                                        val tmp = swapped[curIdx]
                                                                        swapped[curIdx] = swapped[curIdx + 1]
                                                                        swapped[curIdx + 1] = tmp
                                                                        trayItems = swapped
                                                                        draggingAccumX -= stepPx
                                                                    } else if (draggingAccumX < -stepPx && curIdx > 0) {
                                                                        val swapped = curList.toMutableList()
                                                                        val tmp = swapped[curIdx]
                                                                        swapped[curIdx] = swapped[curIdx - 1]
                                                                        swapped[curIdx - 1] = tmp
                                                                        trayItems = swapped
                                                                        draggingAccumX += stepPx
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                draggingId = null
                                                                draggingAccumX = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggingId = null
                                                                draggingAccumX = 0f
                                                            },
                                                        )
                                                    },
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(78.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(
                                                            width = if (item.isOverLimit || item.uploadState == "failed") 2.dp else 1.dp,
                                                            color = when {
                                                                item.isOverLimit || item.uploadState == "failed" -> palette.danger
                                                                item.postedMemeId != null -> palette.primary
                                                                else -> palette.line
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                        )
                                                        .clickable(enabled = item.postedMemeId == null && !publishing) {
                                                            activeCardMenuId = item.id
                                                        },
                                                ) {
                                                    val thumbModel: Any? = when {
                                                        item.posterLocalPath != null -> File(item.posterLocalPath)
                                                        item.maskedFilePath != null -> File(item.maskedFilePath)
                                                        item.localFilePath != null -> File(item.localFilePath)
                                                        else -> item.keptRemoteUrl
                                                    }
                                                    AsyncImage(
                                                        model = thumbModel,
                                                        imageLoader = api.imageLoader,
                                                        contentDescription = "媒体 ${idx + 1}",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )

                                                    // Top-left index badge
                                                    Surface(
                                                        color = Color.Black.copy(alpha = 0.64f),
                                                        shape = CircleShape,
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp),
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        )
                                                    }

                                                    // Top-right media indicator
                                                    when {
                                                        item.isMasked -> {
                                                            Surface(
                                                                color = palette.primary,
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp),
                                                            ) {
                                                                Text("打码", color = palette.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                            }
                                                        }
                                                        item.isVideo -> {
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.72f),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp),
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                ) {
                                                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                                    Text(formatSec(item.durationSec), color = Color.White, fontSize = 9.sp)
                                                                }
                                                            }
                                                        }
                                                        item.isGif -> {
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.68f),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp),
                                                            ) {
                                                                Text("GIF", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                            }
                                                        }
                                                    }

                                                    // Bottom status badge (over limit / posted / failed)
                                                    if (item.isOverLimit) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .fillMaxWidth()
                                                                .background(palette.danger.copy(alpha = 0.9f))
                                                                .padding(vertical = 2.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text("超过 20MB", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else if (item.postedMemeId != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .fillMaxWidth()
                                                                .background(palette.primary.copy(alpha = 0.88f))
                                                                .padding(vertical = 2.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text("✓ No.${item.postedMemeId}", color = palette.onPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else if (item.uploadState == "failed") {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .fillMaxWidth()
                                                                .background(palette.danger.copy(alpha = 0.88f))
                                                                .padding(vertical = 2.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text("⚠ 失败", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    DropdownMenu(
                                                        expanded = activeCardMenuId == item.id,
                                                        onDismissRequest = { activeCardMenuId = null },
                                                    ) {
                                                        if (!item.isVideo && !item.isGif) {
                                                            DropdownMenuItem(
                                                                text = { Text(if (item.isMasked) "重新打码" else "局部打码") },
                                                                onClick = {
                                                                    activeCardMenuId = null
                                                                    val imageIdx = trayItems.indexOfFirst { it.id == item.id }
                                                                    if (imageIdx >= 0) {
                                                                        if (item.localFilePath == null && item.keptRemoteUrl != null) {
                                                                            scope.launch {
                                                                                val f = api.fetchCachedMediaFile(item.keptRemoteOrigUrl ?: item.keptRemoteUrl)
                                                                                if (f != null) {
                                                                                    val staged = ComposeMediaStager.stageFile(context, f, existingId = item.id)
                                                                                    if (staged != null) {
                                                                                        trayItems = trayItems.map { cur ->
                                                                                            if (cur.id == item.id) {
                                                                                                cur.copy(localFilePath = staged.localFilePath, width = staged.width, height = staged.height)
                                                                                            } else cur
                                                                                        }
                                                                                        maskEditorTargetIndex = imageIdx
                                                                                    }
                                                                                }
                                                                            }
                                                                        } else {
                                                                            maskEditorTargetIndex = imageIdx
                                                                        }
                                                                    }
                                                                },
                                                            )
                                                        }
                                                        if (idx > 0) {
                                                            DropdownMenuItem(
                                                                text = { Text("设为封面") },
                                                                onClick = {
                                                                    activeCardMenuId = null
                                                                    val list = trayItems.toMutableList()
                                                                    val moved = list.removeAt(idx)
                                                                    list.add(0, moved)
                                                                    trayItems = list
                                                                },
                                                            )
                                                        }
                                                        DropdownMenuItem(
                                                            text = { Text("替换") },
                                                            onClick = {
                                                                activeCardMenuId = null
                                                                replaceTargetId = item.id
                                                                StealthExemptionGate.markInternalJump()
                                                                singleReplacePicker.launch(
                                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                                                )
                                                            },
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("移除", color = palette.danger) },
                                                            onClick = {
                                                                activeCardMenuId = null
                                                                trayItems = trayItems.filterNot { it.id == item.id }
                                                            },
                                                        )
                                                    }
                                                }

                                                if (genre == ComposeGenre.IMAGE && strategy == PublishStrategy.CUSTOM && !isEditing) {
                                                    val solo = item.soloInCustom
                                                    Surface(
                                                        color = if (solo) palette.primaryContainer else palette.surface2,
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .border(1.dp, if (solo) palette.primary else palette.line, RoundedCornerShape(8.dp))
                                                            .clickable(enabled = item.postedMemeId == null && !publishing) {
                                                                trayItems = trayItems.map { cur ->
                                                                    if (cur.id == item.id) cur.copy(soloInCustom = !cur.soloInCustom) else cur
                                                                }
                                                            },
                                                    ) {
                                                        Text(
                                                            text = if (solo) "单 ▾" else "合 ▾",
                                                            color = if (solo) palette.primary else palette.muted,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                            }

                                            // Thin connector line between merged cards
                                            if (genre == ComposeGenre.IMAGE && strategy == PublishStrategy.MERGE_ONE && idx < trayItems.lastIndex) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .height(2.dp)
                                                        .background(palette.primary.copy(alpha = 0.45f)),
                                                )
                                            }
                                        }
                                    }

                                    if (trayItems.size < MAX_COMPOSE_FILES) {
                                        item(key = "tray_add") {
                                            Box(
                                                modifier = Modifier
                                                    .size(78.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.5.dp, palette.line, RoundedCornerShape(12.dp))
                                                    .clickable(enabled = !publishing) { launchMultiPicker() },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "继续添加", tint = palette.muted, modifier = Modifier.size(26.dp))
                                            }
                                        }
                                    }
                                }

                                // Volume stats line
                                val localItems = trayItems.filter { it.keptRemoteUrl == null }
                                if (localItems.isNotEmpty()) {
                                    val origTotal = localItems.sumOf { it.originalBytes }
                                    val effTotal = localItems.sumOf { it.effectiveBytes }
                                    val infoText = buildString {
                                        append("共 ${trayItems.size} 项")
                                        if (origTotal > effTotal) {
                                            append(" · ${formatByteSize(origTotal)} → 压缩后约 ${formatByteSize(effTotal)}")
                                        } else {
                                            append(" · ${formatByteSize(effTotal)}")
                                        }
                                        if (genre == ComposeGenre.IMAGE && currentBatches.size > 1) {
                                            append(" · 将发布 ${currentBatches.size} 帖")
                                        }
                                    }
                                    Text(
                                        text = infoText,
                                        color = palette.muted,
                                        fontSize = 11.5.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                }
                            }
                        }
                    }
                }

                // TEXT mode optional image attachment trigger
                if (genre == ComposeGenre.TEXT && !showTextMediaTray) {
                    item(key = "text_attach_btn") {
                        OutlinedButton(
                            onClick = {
                                showTextMediaTray = true
                                launchMultiPicker()
                            },
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加配图（可选）", fontSize = 12.5.sp)
                        }
                    }
                }

                // Title & Body Editor / Preview
                item(key = "title_and_body") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(80) },
                            label = { Text("标题（选填，80 字）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (genre == ComposeGenre.TEXT && textPreviewMode) {
                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp)
                                    .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    RichChatBodyView(
                                        rawBody = contentField.text.ifBlank { "_暂无正文内容_" },
                                        imageLoader = api.imageLoader,
                                        textColor = palette.text,
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = contentField,
                                onValueChange = { contentField = it },
                                label = {
                                    Text(
                                        when (genre) {
                                            ComposeGenre.TEXT -> "说点什么…（支持 Markdown）"
                                            ComposeGenre.LINK -> "补充说明（选填）"
                                            ComposeGenre.IMAGE -> "说点什么…（支持 Markdown，选填）"
                                        },
                                    )
                                },
                                minLines = if (genre == ComposeGenre.TEXT) 8 else 3,
                                maxLines = if (genre == ComposeGenre.TEXT) 16 else 6,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Tags Section
                item(key = "tags_picker") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("标签", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "＋ 自定义",
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { showAddTagDialog = true },
                            )
                        }

                        if (selectedTags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                selectedTags.forEach { tag ->
                                    Surface(
                                        color = palette.primaryContainer,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .border(1.dp, palette.primary, RoundedCornerShape(999.dp))
                                            .clickable { selectedTags = selectedTags - tag },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text("#$tag", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Icon(Icons.Rounded.Close, contentDescription = "移除", tint = palette.primary, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            popularTags.filterNot { it in selectedTags }.take(12).forEach { tag ->
                                Surface(
                                    color = palette.surface2,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .border(1.dp, palette.line, RoundedCornerShape(999.dp))
                                        .clickable { selectedTags = selectedTags + tag },
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = palette.muted,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Toggles: Spoiler + AI comment off + Puzzle entry
                item(key = "switches_section") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = palette.danger, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("敏感 / 慎点", color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                    Text("其他用户浏览时默认模糊", color = palette.muted, fontSize = 11.sp)
                                }
                            }
                            Switch(checked = spoiler, onCheckedChange = { spoiler = it })
                        }

                        if (genre == ComposeGenre.IMAGE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("关闭本帖 AI 小伙伴评论", color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                        Text(aiStatusNote, color = palette.muted, fontSize = 11.sp)
                                    }
                                }
                                Switch(
                                    checked = aiOff,
                                    enabled = aiToggleAllowed,
                                    onCheckedChange = { aiOff = it },
                                )
                            }

                            if (!isEditing) {
                                HorizontalDivider(color = palette.line, modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (puzzleDraft.layers.isEmpty()) {
                                                puzzleDraft = puzzleDraft.copy(
                                                    layers = listOf(PuzzleLayerDraft(id = UUID.randomUUID().toString())),
                                                )
                                            }
                                            showPuzzleSheet = true
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                        Text("挂载解密谜题", color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        val activeCount = puzzleDraft.layers.count { it.prompt.isNotBlank() || it.answer.isNotBlank() }
                                        Text(
                                            text = if (activeCount > 0) "已设 $activeCount 层" else "未设置",
                                            color = if (activeCount > 0) palette.primary else palette.muted,
                                            fontSize = 12.5.sp,
                                        )
                                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Area: Markdown Bar (TEXT mode) + Publish Bar
            Surface(
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column {
                    if (genre == ComposeGenre.TEXT) {
                        HorizontalDivider(color = palette.line)
                        if (showStickerPicker) {
                            LaunchedEffect(Unit) {
                                if (stickerPacks.isEmpty()) {
                                    stickerPacks = api.fetchStickerPacks().filter { !it.hidden && it.items.isNotEmpty() }
                                }
                            }
                            val firstPack = stickerPacks.firstOrNull()
                            if (firstPack != null) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.surface2)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(firstPack.items.take(32), key = { it }) { itemName ->
                                        AsyncImage(
                                            model = MediaUrlRewriter.stickerUrl(firstPack.key, itemName, firstPack.ext),
                                            imageLoader = api.imageLoader,
                                            contentDescription = itemName,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clickable {
                                                    contentField = insertAtSelection(contentField, "[${firstPack.key}:$itemName]")
                                                    showStickerPicker = false
                                                },
                                        )
                                    }
                                }
                            }
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            item {
                                MarkdownShortcutChip("B") {
                                    contentField = wrapSelection(contentField, "**", "**", "粗体")
                                }
                            }
                            item {
                                MarkdownShortcutChip("I") {
                                    contentField = wrapSelection(contentField, "*", "*", "斜体")
                                }
                            }
                            item {
                                MarkdownShortcutChip("H") {
                                    contentField = insertAtSelection(contentField, "\n## ")
                                }
                            }
                            item {
                                MarkdownShortcutChip("❝") {
                                    contentField = insertAtSelection(contentField, "\n> ")
                                }
                            }
                            item {
                                MarkdownShortcutChip("</>") {
                                    contentField = wrapSelection(contentField, "\n```\n", "\n```\n", "代码")
                                }
                            }
                            item {
                                MarkdownShortcutChip("▸折叠") {
                                    contentField = wrapSelection(contentField, "[details=\"展开\"]\n", "\n[/details]", "内容")
                                }
                            }
                            item {
                                MarkdownShortcutChip("▓spoiler") {
                                    contentField = wrapSelection(contentField, "||", "||", "剧透")
                                }
                            }
                            item {
                                MarkdownShortcutChip("☺贴纸") {
                                    showStickerPicker = !showStickerPicker
                                }
                            }
                            item {
                                MarkdownShortcutChip(
                                    label = if (textPreviewMode) "编辑" else "预览",
                                    highlighted = textPreviewMode,
                                ) {
                                    textPreviewMode = !textPreviewMode
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = palette.line)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = publishStatusText ?: if (isEditing) "修改将同步至原帖" else "离开未发布可保存草稿",
                                color = if (publishStatusText?.contains("失败") == true || publishStatusText?.contains("超过") == true) {
                                    palette.danger
                                } else {
                                    palette.muted
                                },
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        val canSubmit = !publishing && when {
                            isEditing -> genre == ComposeGenre.TEXT || trayItems.isNotEmpty()
                            genre == ComposeGenre.LINK -> linkPreview != null && linkLoadedRaw == shareRawUrl.trim()
                            genre == ComposeGenre.TEXT -> title.isNotBlank() || contentField.text.isNotBlank()
                            else -> trayItems.isNotEmpty() && trayItems.none { it.isOverLimit }
                        }

                        Button(
                            enabled = canSubmit,
                            onClick = { submitPublish() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                            ),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            if (publishing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = palette.onPrimary)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = if (publishing) "发布中…" else publishLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        }
    }

    // Full-screen Partial Mask Editor launched from tray item
    val maskStartIdx = maskEditorTargetIndex
    if (maskStartIdx != null) {
        val staticImageItems = remember(trayItems) { trayItems.filter { !it.isVideo && !it.isGif && it.localFilePath != null } }
        val targetItem = trayItems.getOrNull(maskStartIdx)
        val staticIdx = staticImageItems.indexOfFirst { it.id == targetItem?.id }.coerceAtLeast(0)
        if (staticImageItems.isNotEmpty()) {
            ImageMaskEditorScreen(
                context = context,
                imageItems = staticImageItems,
                initialIndex = staticIdx,
                onItemUpdated = { updated ->
                    trayItems = trayItems.map { cur -> if (cur.id == updated.id) updated else cur }
                    if (updated.isMasked && "打码" !in selectedTags) {
                        selectedTags = selectedTags + "打码"
                    }
                },
                onDismiss = { maskEditorTargetIndex = null },
            )
        } else {
            LaunchedEffect(maskStartIdx) { maskEditorTargetIndex = null }
        }
    }

    if (showPuzzleSheet) {
        PuzzleEditorSheet(
            initial = puzzleDraft,
            trayImages = trayItems.filter { !it.isVideo },
            onSave = { saved ->
                puzzleDraft = saved
                showPuzzleSheet = false
            },
            onDismiss = { showPuzzleSheet = false },
        )
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            containerColor = palette.surface,
            title = { Text("添加自定义标签", color = palette.text, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customTagDraft,
                    onValueChange = { customTagDraft = it.take(24) },
                    label = { Text("标签名（不含 #）") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = customTagDraft.trim().removePrefix("#")
                    if (clean.isNotBlank() && clean !in selectedTags) {
                        selectedTags = selectedTags + clean
                    }
                    customTagDraft = ""
                    showAddTagDialog = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text("取消") }
            },
        )
    }

    if (showExitGuardDialog) {
        AlertDialog(
            onDismissRequest = { showExitGuardDialog = false },
            containerColor = palette.surface,
            title = { Text("保留发帖草稿？", color = palette.text, fontWeight = FontWeight.Bold) },
            text = { Text("当前媒体与编辑内容可保存在本地，下次点击发帖时可恢复。", color = palette.muted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showExitGuardDialog = false
                    saveDraftAndDismiss()
                }) { Text("保留草稿") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitGuardDialog = false
                        discardDraftAndDismiss()
                    }) { Text("丢弃", color = palette.danger) }
                    TextButton(onClick = { showExitGuardDialog = false }) {
                        Text("继续编辑")
                    }
                }
            },
        )
    }

    pendingGenreSwitch?.let { nextGenre ->
        AlertDialog(
            onDismissRequest = { pendingGenreSwitch = null },
            containerColor = palette.surface,
            title = { Text("切换到链接发帖？", color = palette.text, fontWeight = FontWeight.Bold) },
            text = { Text("链接模式不会上传当前托盘中的本地媒体。", color = palette.muted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    genre = nextGenre
                    pendingGenreSwitch = null
                }) { Text("切换") }
            },
            dismissButton = {
                TextButton(onClick = { pendingGenreSwitch = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleEditorSheet(
    initial: PuzzleDraftSpec,
    trayImages: List<ComposeTrayItem>,
    onSave: (PuzzleDraftSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var layers by remember(initial) {
        mutableStateOf(initial.layers.ifEmpty { listOf(PuzzleLayerDraft(id = UUID.randomUUID().toString())) })
    }
    var expandedIdx by remember { mutableIntStateOf(0) }
    var hint by remember(initial) { mutableStateOf(initial.hint) }
    var rewardKind by remember(initial) { mutableStateOf(initial.rewardKind) }
    var rewardType by remember(initial) { mutableStateOf(initial.rewardType) }
    var rewardText by remember(initial) { mutableStateOf(initial.rewardText) }
    var codes by remember(initial) { mutableStateOf(initial.codes) }
    var maxWinnersRaw by remember(initial) { mutableStateOf(initial.maxWinners.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var cluePickLayerId by remember { mutableStateOf<String?>(null) }
    val cluePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val tid = cluePickLayerId
        cluePickLayerId = null
        if (uri != null && tid != null) {
            val dir = File(context.cacheDir, "compose_staging").apply { mkdirs() }
            val outFile = File(dir, "clue_${tid}.jpg")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    outFile.outputStream().use { out -> inp.copyTo(out) }
                }
            }
            if (outFile.exists() && outFile.length() > 0L) {
                layers = layers.map { l -> if (l.id == tid) l.copy(clueLocalPath = outFile.absolutePath, uploadedImgUrl = null) else l }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("解密谜题（最多 5 层）", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (layers.size < 5) {
                    TextButton(onClick = {
                        layers = layers + PuzzleLayerDraft(id = UUID.randomUUID().toString())
                        expandedIdx = layers.lastIndex
                    }) {
                        Text("＋ 添加一层")
                    }
                }
            }

            layers.forEachIndexed { idx, layer ->
                val expanded = idx == expandedIdx
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedIdx = if (expanded) -1 else idx },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "第 ${idx + 1} 层 · ${
                                    when (layer.kind) {
                                        "quiz" -> "选择题"
                                        "image" -> "看图回答"
                                        else -> "问答口令"
                                    }
                                }",
                                color = palette.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (layers.size > 1) {
                                    IconButton(
                                        onClick = {
                                            layers = layers.filterNot { it.id == layer.id }
                                            expandedIdx = 0
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除此层", tint = palette.danger, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.muted,
                                )
                            }
                        }

                        if (expanded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("pass" to "问答", "quiz" to "选择", "image" to "看图").forEach { (k, lbl) ->
                                    FilterChip(
                                        selected = layer.kind == k,
                                        onClick = {
                                            layers = layers.map { if (it.id == layer.id) it.copy(kind = k) else it }
                                        },
                                        label = { Text(lbl, fontSize = 11.5.sp) },
                                    )
                                }
                            }

                            if (layer.kind == "image") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(onClick = {
                                        cluePickLayerId = layer.id
                                        StealthExemptionGate.markInternalJump()
                                        cluePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) {
                                        Text(if (layer.clueLocalPath == null && layer.uploadedImgUrl == null) "上传线索图" else "更换线索图", fontSize = 12.sp)
                                    }
                                    if (trayImages.isNotEmpty() && layer.clueLocalPath == null) {
                                        OutlinedButton(onClick = {
                                            val firstTray = trayImages.first()
                                            layers = layers.map { cur ->
                                                if (cur.id == layer.id) {
                                                    cur.copy(clueLocalPath = firstTray.maskedFilePath ?: firstTray.localFilePath, uploadedImgUrl = firstTray.keptRemoteUrl)
                                                } else cur
                                            }
                                        }) {
                                            Text("用托盘首图", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = layer.prompt,
                                onValueChange = { v -> layers = layers.map { if (it.id == layer.id) it.copy(prompt = v) else it } },
                                label = { Text("问题 / 题面") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (layer.kind == "quiz") {
                                OutlinedTextField(
                                    value = layer.optionsRaw,
                                    onValueChange = { v -> layers = layers.map { if (it.id == layer.id) it.copy(optionsRaw = v) else it } },
                                    label = { Text("选项（每行一个，2~6 项）") },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                val opts = layer.optionsRaw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                                if (opts.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        opts.take(4).forEach { opt ->
                                            FilterChip(
                                                selected = layer.answer == opt,
                                                onClick = {
                                                    layers = layers.map { if (it.id == layer.id) it.copy(answer = opt) else it }
                                                },
                                                label = { Text(opt, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = layer.answer,
                                    onValueChange = { v -> layers = layers.map { if (it.id == layer.id) it.copy(answer = v) else it } },
                                    label = { Text("答案（多答案用 | 分隔）") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            OutlinedTextField(
                                value = layer.note,
                                onValueChange = { v -> layers = layers.map { if (it.id == layer.id) it.copy(note = v) else it } },
                                label = { Text("过关说明 / 下一层线索（选填）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = hint,
                onValueChange = { hint = it.take(300) },
                label = { Text("总提示（选填，展示在卡片下方）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("通关奖励", color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "secret" to "隐藏图文",
                    "credit" to "Credit 红包",
                    "cdk" to "兑换码",
                ).forEach { (k, lbl) ->
                    FilterChip(
                        selected = rewardKind == k,
                        onClick = {
                            rewardKind = k
                            rewardType = when (k) {
                                "secret" -> "text"
                                "credit" -> if (rewardType in setOf("text", "link")) rewardType else "text"
                                else -> if (rewardType in setOf("codes", "link")) rewardType else "codes"
                            }
                        },
                        label = { Text(lbl, fontSize = 12.sp) },
                    )
                }
            }

            if (rewardKind == "credit") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = rewardType == "text", onClick = { rewardType = "text" }, label = { Text("红包口令", fontSize = 11.5.sp) })
                    FilterChip(selected = rewardType == "link", onClick = { rewardType = "link" }, label = { Text("红包链接", fontSize = 11.5.sp) })
                }
            } else if (rewardKind == "cdk") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = rewardType == "codes", onClick = { rewardType = "codes" }, label = { Text("码池分发", fontSize = 11.5.sp) })
                    FilterChip(selected = rewardType == "link", onClick = { rewardType = "link" }, label = { Text("CDK 链接", fontSize = 11.5.sp) })
                }
            }

            if (rewardType == "codes") {
                OutlinedTextField(
                    value = codes,
                    onValueChange = { codes = it },
                    label = { Text("兑换码（一行一个）") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = rewardText,
                    onValueChange = { rewardText = it },
                    label = { Text(if (rewardType == "link") "奖励链接 (https://...)" else "奖励内容 / 口令") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = maxWinnersRaw,
                    onValueChange = { maxWinnersRaw = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("获奖名额（0 表示不限）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg!!, color = palette.danger, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onSave(PuzzleDraftSpec()) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("清空谜题")
                }
                Button(
                    onClick = {
                        val invalid = layers.indexOfFirst { l ->
                            if (l.kind == "quiz") {
                                val opts = l.optionsRaw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                                opts.size < 2 || l.answer.isBlank()
                            } else {
                                l.answer.isBlank()
                            }
                        }
                        if (invalid >= 0) {
                            expandedIdx = invalid
                            errorMsg = "请完善第 ${invalid + 1} 层的题目与答案"
                            return@Button
                        }
                        if (rewardType == "codes" && codes.isBlank()) {
                            errorMsg = "请填写至少一个兑换码"
                            return@Button
                        }
                        if (rewardType != "codes" && rewardText.isBlank()) {
                            errorMsg = "请填写通关奖励内容"
                            return@Button
                        }
                        onSave(
                            PuzzleDraftSpec(
                                layers = layers,
                                hint = hint,
                                rewardKind = rewardKind,
                                rewardType = rewardType,
                                rewardText = rewardText,
                                codes = codes,
                                maxWinners = maxWinnersRaw.toIntOrNull() ?: 0,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("完成")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemShareTargetSheet(
    context: Context,
    api: ForHappyApiClient,
    me: MeUser?,
    payload: SharedIncomingPayload,
    disguiseMode: AppDisguiseMode,
    groups: List<ChatGroup>,
    onRequireLogin: () -> Unit,
    onOpenComposeForShare: (ComposeLaunchRequest) -> Unit,
    onSentToGroup: (Long) -> Unit,
    onSentToDm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDisguised = disguiseMode != AppDisguiseMode.NONE

    var step by remember(payload) { mutableStateOf("root") } // "root" | "pick_group" | "pick_dm"
    var dmThreads by remember { mutableStateOf<List<DmThread>>(emptyList()) }
    var loadingDms by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(step) {
        if (step == "pick_dm" && dmThreads.isEmpty()) {
            loadingDms = true
            dmThreads = api.fetchDmThreads()
            loadingDms = false
        }
    }

    fun openGalleryComposeFromShare() {
        val text = payload.sharedText.orEmpty().trim()
        val social = SocialLinkDetector.detectFirstSocialLink(text)
        val hasMedia = payload.uris.isNotEmpty()
        val request = when {
            !hasMedia && social != null -> ComposeLaunchRequest(
                triggerSeq = System.currentTimeMillis(),
                initialGenre = ComposeGenre.LINK,
                initialShareUrl = social.url,
            )
            !hasMedia -> ComposeLaunchRequest(
                triggerSeq = System.currentTimeMillis(),
                initialGenre = ComposeGenre.TEXT,
                initialContent = text,
            )
            else -> ComposeLaunchRequest(
                triggerSeq = System.currentTimeMillis(),
                initialGenre = ComposeGenre.IMAGE,
                sharedUris = payload.uris,
                initialContent = text,
            )
        }
        onDismiss()
        onOpenComposeForShare(request)
    }

    fun sendToGroup(groupId: Long) {
        if (me == null) {
            onRequireLogin()
            return
        }
        sending = true
        statusText = "正在上传并发送…"
        scope.launch {
            val uploadedMediaList = mutableListOf<kotlinx.serialization.json.JsonObject>()
            for (uStr in payload.uris.take(12)) {
                val staged = ComposeMediaStager.stageUri(context, Uri.parse(uStr)) ?: continue
                val mainFile = File(staged.localFilePath ?: continue)
                if (!mainFile.exists()) continue
                val bytes = withContext(Dispatchers.IO) { mainFile.readBytes() }
                val up = api.uploadMediaBytes(bytes, staged.fileName, staged.mimeType, forWhat = "chat").getOrNull() ?: continue
                var posterKey: String? = null
                if (staged.isVideo && staged.posterLocalPath != null) {
                    val pf = File(staged.posterLocalPath)
                    if (pf.exists()) {
                        val pBytes = withContext(Dispatchers.IO) { pf.readBytes() }
                        posterKey = api.uploadMediaBytes(pBytes, pf.name, "image/jpeg", forWhat = "chat").getOrNull()?.key
                    }
                }
                uploadedMediaList += buildJsonObject {
                    put("key", JsonPrimitive(up.key))
                    if (posterKey != null) put("poster", JsonPrimitive(posterKey))
                    if (staged.width > 0) put("w", JsonPrimitive(staged.width))
                    if (staged.height > 0) put("h", JsonPrimitive(staged.height))
                    put("name", JsonPrimitive(staged.fileName))
                }
            }
            val bodyText = payload.sharedText.orEmpty().trim()
            val res = if (uploadedMediaList.isNotEmpty()) {
                api.sendChatMessageWithMedia(groupId, bodyText, JsonArray(uploadedMediaList))
            } else {
                api.sendChatMessage(groupId, bodyText, replyToId = null, retry = false)
            }
            sending = false
            res.fold(
                onSuccess = {
                    onSentToGroup(groupId)
                    onDismiss()
                },
                onFailure = { err ->
                    statusText = err.message ?: "发送失败"
                },
            )
        }
    }

    fun sendToDm(threadId: Long) {
        if (me == null) {
            onRequireLogin()
            return
        }
        sending = true
        statusText = "正在发送…"
        scope.launch {
            val mediaObjs = mutableListOf<kotlinx.serialization.json.JsonObject>()
            val fallbackUrls = mutableListOf<String>()
            for (uStr in payload.uris.take(12)) {
                val staged = ComposeMediaStager.stageUri(context, Uri.parse(uStr)) ?: continue
                val mainFile = File(staged.localFilePath ?: continue)
                if (!mainFile.exists()) continue
                val bytes = withContext(Dispatchers.IO) { mainFile.readBytes() }
                val up = api.uploadMediaBytes(bytes, staged.fileName, staged.mimeType, forWhat = "chat").getOrNull() ?: continue
                fallbackUrls += MediaUrlRewriter.rewriteMediaUrl(up.url.ifBlank { "/img/${up.key}" })
                mediaObjs += buildJsonObject {
                    put("key", JsonPrimitive(up.key))
                    if (staged.width > 0) put("w", JsonPrimitive(staged.width))
                    if (staged.height > 0) put("h", JsonPrimitive(staged.height))
                }
            }
            val bodyText = payload.sharedText.orEmpty().trim()
            val ok = api.sendDmMessageWithMedia(
                threadId = threadId,
                body = bodyText,
                media = JsonArray(mediaObjs),
                fallbackImageUrls = fallbackUrls,
            )
            sending = false
            if (ok) {
                onSentToDm(threadId)
                onDismiss()
            } else {
                statusText = "私信发送失败"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isDisguised) {
                // Disguised work-style picker (NO gallery/meme mentions)
                LaunchedEffect(Unit) {
                    if (dmThreads.isEmpty()) dmThreads = api.fetchDmThreads()
                }
                Text("发送给会话", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (payload.uris.isNotEmpty()) "${payload.uris.size} 份本地素材" else payload.sharedText.orEmpty().take(80),
                    color = palette.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item(key = "work_archive") {
                        ShareActionRow(
                            icon = Icons.Rounded.Forum,
                            title = "产品资料归档群",
                            subtitle = "归档图文素材到部门共享区",
                            enabled = !sending,
                            onClick = { openGalleryComposeFromShare() },
                        )
                    }
                    items(groups.filter { it.joined }, key = { "dg_${it.id}" }) { g ->
                        ShareActionRow(
                            icon = Icons.Rounded.Forum,
                            title = "项目协作群 #${g.id}",
                            subtitle = "${g.memberCount} 名成员",
                            enabled = !sending,
                            onClick = { sendToGroup(g.id) },
                        )
                    }
                    items(dmThreads, key = { "dd_${it.id}" }) { dm ->
                        ShareActionRow(
                            icon = Icons.Rounded.Email,
                            title = dm.peerName ?: dm.peerUsername ?: "同事 ${dm.id}",
                            subtitle = "一对一会话",
                            enabled = !sending,
                            onClick = { sendToDm(dm.id) },
                        )
                    }
                }
            } else {
                when (step) {
                    "root" -> {
                        val headerCount = if (payload.uris.isNotEmpty()) "${payload.uris.size} 个媒体" else "文本/链接"
                        Text("分享$headerCount 到…", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (!payload.sharedText.isNullOrBlank()) {
                            Text(
                                text = payload.sharedText,
                                color = palette.muted,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ShareActionRow(
                            icon = Icons.Rounded.Image,
                            title = "发到图廊",
                            subtitle = "打开发帖工作台，预填所选内容",
                            enabled = !sending,
                            onClick = { openGalleryComposeFromShare() },
                        )
                        ShareActionRow(
                            icon = Icons.Rounded.Forum,
                            title = "发到群聊…",
                            subtitle = "选择一个已加入的群聊直接发送",
                            enabled = !sending,
                            onClick = { step = "pick_group" },
                        )
                        ShareActionRow(
                            icon = Icons.Rounded.Email,
                            title = "发私信…",
                            subtitle = "选择一位好友私信发送",
                            enabled = !sending,
                            onClick = { step = "pick_dm" },
                        )
                    }

                    "pick_group" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { step = "root" }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                            }
                            Text("选择群聊", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        val joinedGroups = groups.filter { it.joined }
                        if (joinedGroups.isEmpty()) {
                            Text("暂无已加入群聊", color = palette.muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(joinedGroups, key = { it.id }) { g ->
                                    ShareActionRow(
                                        icon = Icons.Rounded.Forum,
                                        title = g.name,
                                        subtitle = "${g.memberCount} 人",
                                        enabled = !sending,
                                        onClick = { sendToGroup(g.id) },
                                    )
                                }
                            }
                        }
                    }

                    "pick_dm" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { step = "root" }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                            }
                            Text("选择私信对象", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        if (loadingDms) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = palette.primary)
                            }
                        } else if (dmThreads.isEmpty()) {
                            Text("暂无私信会话", color = palette.muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(dmThreads, key = { it.id }) { dm ->
                                    ShareActionRow(
                                        icon = Icons.AutoMirrored.Rounded.Send,
                                        title = dm.peerName ?: dm.peerUsername ?: "私信 #${dm.id}",
                                        subtitle = dm.lastBody?.take(36) ?: "点击直接发送",
                                        enabled = !sending,
                                        onClick = { sendToDm(dm.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!statusText.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (sending) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = palette.primary)
                    } else {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = palette.danger, modifier = Modifier.size(16.dp))
                    }
                    Text(statusText!!, color = if (sending) palette.primary else palette.danger, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ShareActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = palette.muted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MarkdownShortcutChip(
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = if (highlighted) palette.primaryContainer else palette.surface2,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(1.dp, if (highlighted) palette.primary else palette.line, RoundedCornerShape(8.dp))
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            color = if (highlighted) palette.primary else palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun wrapSelection(
    field: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    val text = field.text
    val start = field.selection.min.coerceIn(0, text.length)
    val end = field.selection.max.coerceIn(start, text.length)
    val inner = if (start < end) text.substring(start, end) else placeholder
    val inserted = prefix + inner + suffix
    val newText = text.substring(0, start) + inserted + text.substring(end)
    val selStart = start + prefix.length
    val selEnd = selStart + inner.length
    return TextFieldValue(newText, TextRange(selStart, selEnd))
}

private fun insertAtSelection(
    field: TextFieldValue,
    token: String,
): TextFieldValue {
    val text = field.text
    val start = field.selection.min.coerceIn(0, text.length)
    val end = field.selection.max.coerceIn(start, text.length)
    val newText = text.substring(0, start) + token + text.substring(end)
    val cursor = start + token.length
    return TextFieldValue(newText, TextRange(cursor))
}

private fun formatByteSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1fMB", bytes / (1024f * 1024f))
    bytes >= 1024L -> "${maxOf(1L, bytes / 1024L)}KB"
    else -> "${bytes}B"
}

private fun formatSec(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}
