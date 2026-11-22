package com.jinhuan.forhappy.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import com.jinhuan.forhappy.core.compose.MaskBrushStyle
import com.jinhuan.forhappy.core.compose.MaskOperation
import com.jinhuan.forhappy.core.compose.MaskPixelEngine
import com.jinhuan.forhappy.core.compose.MaskToolKind
import com.jinhuan.forhappy.core.compose.NormPoint
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class BoxDragMode {
    NONE,
    NEW_BOX,
    MOVE,
    RESIZE_TL,
    RESIZE_TR,
    RESIZE_BL,
    RESIZE_BR,
}

private data class DecodedMaskSource(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
)

@Composable
fun ImageMaskEditorScreen(
    context: Context,
    imageItems: List<com.jinhuan.forhappy.core.compose.ComposeTrayItem>,
    initialIndex: Int,
    onItemUpdated: (com.jinhuan.forhappy.core.compose.ComposeTrayItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hostView = LocalView.current

    var currentIndex by remember(imageItems) {
        mutableIntStateOf(initialIndex.coerceIn(0, maxOf(0, imageItems.lastIndex)))
    }
    val currentItem = imageItems.getOrNull(currentIndex) ?: run {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var tool by remember { mutableStateOf(MaskToolKind.BOX) }
    var brushStyle by remember { mutableStateOf(MaskBrushStyle.MOSAIC) }
    var brushRadiusNorm by remember { mutableFloatStateOf(0.035f) }

    var ops by remember(currentItem.id) { mutableStateOf(currentItem.maskOps) }
    var undoStack by remember(currentItem.id) { mutableStateOf<List<List<MaskOperation>>>(emptyList()) }
    var redoStack by remember(currentItem.id) { mutableStateOf<List<List<MaskOperation>>>(emptyList()) }
    var keepOriginal by remember(currentItem.id) {
        mutableStateOf(
            currentItem.keepOriginalUnmasked ||
                imageItems.getOrNull((currentIndex - 1).coerceAtLeast(0))?.keepOriginalUnmasked == true,
        )
    }
    var selectedBoxId by remember(currentItem.id) { mutableStateOf<String?>(null) }

    var sourceData by remember(currentItem.id) { mutableStateOf<DecodedMaskSource?>(null) }
    var previewBitmap by remember(currentItem.id) { mutableStateOf<ImageBitmap?>(null) }
    var rendering by remember(currentItem.id) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomScale by remember(currentItem.id) { mutableFloatStateOf(1f) }
    var panOffset by remember(currentItem.id) { mutableStateOf(Offset.Zero) }

    // Live in-flight gesture visuals
    var draftBox by remember { mutableStateOf<MaskOperation.BoxOp?>(null) }
    var activeStrokePoints by remember { mutableStateOf<List<NormPoint>>(emptyList()) }

    val hasUnsavedChanges = ops != currentItem.maskOps || keepOriginal != currentItem.keepOriginalUnmasked

    // Decode unmasked source bitmap downsampled to max edge 2048px
    LaunchedEffect(currentItem.id, currentItem.localFilePath) {
        rendering = true
        sourceData = null
        previewBitmap = null
        val path = currentItem.localFilePath
        if (path != null) {
            val decoded = withContext(Dispatchers.IO) {
                decodeDownsampledSource(path, maxEdgePx = 2048)
            }
            sourceData = decoded
        }
        rendering = false
    }

    // Re-synthesize preview bitmap whenever committed ops or source changes
    LaunchedEffect(sourceData, ops) {
        val src = sourceData ?: return@LaunchedEffect
        val outBmp = withContext(Dispatchers.Default) {
            val pixels = MaskPixelEngine.applyMaskOperations(src.pixels, src.width, src.height, ops)
            Bitmap.createBitmap(pixels, src.width, src.height, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        previewBitmap = outBmp
    }

    // Register bottom 200dp Android gesture exclusion rect over canvas container
    DisposableEffect(hostView) {
        onDispose {
            ViewCompat.setSystemGestureExclusionRects(hostView, emptyList())
        }
    }

    fun commitOps(next: List<MaskOperation>) {
        undoStack = (undoStack + listOf(ops)).takeLast(30)
        redoStack = emptyList()
        ops = next
    }

    fun undo() {
        val prev = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = redoStack + listOf(ops)
        ops = prev
        if (selectedBoxId != null && prev.none { it.id == selectedBoxId }) {
            selectedBoxId = null
        }
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = undoStack + listOf(ops)
        ops = next
    }

    suspend fun exportAndPersistCurrent(): com.jinhuan.forhappy.core.compose.ComposeTrayItem {
        val src = sourceData
        val dirty = ops != currentItem.maskOps || keepOriginal != currentItem.keepOriginalUnmasked
        if (!dirty && (ops.isEmpty() || currentItem.maskedFilePath != null)) {
            return currentItem
        }
        if (ops.isEmpty()) {
            val origLen = currentItem.localFilePath?.let { File(it).takeIf { f -> f.exists() }?.length() }
                ?: currentItem.originalBytes
            return currentItem.copy(
                maskedFilePath = null,
                maskOps = emptyList(),
                keepOriginalUnmasked = keepOriginal,
                compressedBytes = origLen,
                uploadedKey = null,
                uploadedOrigKey = null,
                uploadState = "idle",
            )
        }
        if (src == null) return currentItem
        val outFile = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "compose_staging").apply { mkdirs() }
            val pixels = MaskPixelEngine.applyMaskOperations(src.pixels, src.width, src.height, ops)
            val bmp = Bitmap.createBitmap(pixels, src.width, src.height, Bitmap.Config.ARGB_8888)
            val target = File(dir, "masked_${currentItem.id}_${System.currentTimeMillis()}.jpg")
            target.outputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bmp.recycle()
            target
        }
        return currentItem.copy(
            maskedFilePath = outFile.absolutePath,
            maskOps = ops,
            keepOriginalUnmasked = keepOriginal,
            compressedBytes = outFile.length(),
            width = src.width,
            height = src.height,
            uploadedKey = null,
            uploadedOrigKey = null,
            uploadState = "idle",
        )
    }

    fun attemptClose() {
        if (saving) return
        if (hasUnsavedChanges) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { attemptClose() },
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = false)
        DialogBackOwnerScope {
            BackHandler { attemptClose() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top Bar: ✕ | 打码 1/4 | 完成
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { attemptClose() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                    }
                    Text(
                        text = if (imageItems.size > 1) "打码 ${currentIndex + 1}/${imageItems.size}" else "打码",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = imageItems.size > 1 && !saving) {
                                scope.launch {
                                    saving = true
                                    val updated = exportAndPersistCurrent()
                                    onItemUpdated(updated)
                                    saving = false
                                    currentIndex = (currentIndex + 1) % imageItems.size
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }

                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable(enabled = !saving) {
                        scope.launch {
                            saving = true
                            val updated = exportAndPersistCurrent()
                            onItemUpdated(updated)
                            saving = false
                            onDismiss()
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (saving) "处理中…" else "完成",
                            color = palette.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Canvas Stage with 24dp black gutters left/right + bottom 200dp gesture exclusion
            val bottomExclusionPx = with(density) { 200.dp.toPx() }
            val fitRect = remember(canvasSize, sourceData) {
                computeFitRect(canvasSize, sourceData?.width ?: 0, sourceData?.height ?: 0)
            }

            val latestTool by rememberUpdatedState(tool)
            val latestStyle by rememberUpdatedState(brushStyle)
            val latestRadius by rememberUpdatedState(brushRadiusNorm)
            val latestOps by rememberUpdatedState(ops)
            val latestSelectedBoxId by rememberUpdatedState(selectedBoxId)
            val latestFitRect by rememberUpdatedState(fitRect)
            val latestCanvasSize by rememberUpdatedState(canvasSize)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clipToBounds()
                    .onSizeChanged { canvasSize = it }
                    .onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        val exclTop = maxOf(b.top, b.bottom - bottomExclusionPx)
                        if (b.right > b.left && b.bottom > exclTop) {
                            ViewCompat.setSystemGestureExclusionRects(
                                hostView,
                                listOf(Rect(b.left.roundToInt(), exclTop.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())),
                            )
                        }
                    }
                    .pointerInput(currentItem.id) {
                        var lastTapMs = 0L
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            var isMultiTouch = false
                            var movedBeyondSlop = false
                            var dragMode = BoxDragMode.NONE
                            var activeBoxOriginal: MaskOperation.BoxOp? = null
                            var dragStartNorm = screenToNorm(
                                firstDown.position,
                                latestCanvasSize,
                                latestFitRect,
                                zoomScale,
                                panOffset,
                            )
                            val strokePts = mutableListOf(dragStartNorm)

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) {
                                    // Finger(s) lifted
                                    if (!isMultiTouch && movedBeyondSlop) {
                                        when (dragMode) {
                                            BoxDragMode.NEW_BOX -> {
                                                val box = draftBox
                                                if (box != null && abs(box.right - box.left) > 0.015f && abs(box.bottom - box.top) > 0.015f) {
                                                    val normalized = box.copy(
                                                        left = minOf(box.left, box.right),
                                                        top = minOf(box.top, box.bottom),
                                                        right = maxOf(box.left, box.right),
                                                        bottom = maxOf(box.top, box.bottom),
                                                    )
                                                    commitOps(latestOps + normalized)
                                                    selectedBoxId = normalized.id
                                                }
                                            }
                                            BoxDragMode.MOVE,
                                            BoxDragMode.RESIZE_TL,
                                            BoxDragMode.RESIZE_TR,
                                            BoxDragMode.RESIZE_BL,
                                            BoxDragMode.RESIZE_BR,
                                            -> {
                                                val edited = draftBox
                                                val orig = activeBoxOriginal
                                                if (edited != null && orig != null) {
                                                    val normalized = edited.copy(
                                                        left = minOf(edited.left, edited.right).coerceIn(0f, 1f),
                                                        top = minOf(edited.top, edited.bottom).coerceIn(0f, 1f),
                                                        right = maxOf(edited.left, edited.right).coerceIn(0f, 1f),
                                                        bottom = maxOf(edited.top, edited.bottom).coerceIn(0f, 1f),
                                                    )
                                                    if (normalized.right - normalized.left > 0.015f &&
                                                        normalized.bottom - normalized.top > 0.015f
                                                    ) {
                                                        commitOps(latestOps.map { if (it.id == orig.id) normalized else it })
                                                    }
                                                }
                                            }
                                            BoxDragMode.NONE -> {
                                                if (latestTool == MaskToolKind.STROKE && strokePts.size >= 2) {
                                                    val op = MaskOperation.StrokeOp(
                                                        id = UUID.randomUUID().toString(),
                                                        points = strokePts.toList(),
                                                        radiusNorm = latestRadius,
                                                        style = latestStyle,
                                                    )
                                                    commitOps(latestOps + op)
                                                }
                                            }
                                        }
                                        draftBox = null
                                        activeStrokePoints = emptyList()
                                    } else if (!isMultiTouch && !movedBeyondSlop) {
                                        // Tap (single or double)
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapMs < 280L) {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                            lastTapMs = 0L
                                        } else {
                                            lastTapMs = now
                                            if (latestTool == MaskToolKind.BOX) {
                                                val hitBox = latestOps
                                                    .filterIsInstance<MaskOperation.BoxOp>()
                                                    .lastOrNull { box ->
                                                        dragStartNorm.x in box.left..box.right &&
                                                            dragStartNorm.y in box.top..box.bottom
                                                    }
                                                selectedBoxId = hitBox?.id
                                            } else {
                                                selectedBoxId = null
                                            }
                                        }
                                    }
                                    break
                                }

                                if (pressed.size >= 2 || isMultiTouch) {
                                    isMultiTouch = true
                                    draftBox = null
                                    activeStrokePoints = emptyList()
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val nextScale = (zoomScale * zoomChange).coerceIn(1f, 5f)
                                    val maxPanX = (latestCanvasSize.width * (nextScale - 1f)) / 2f
                                    val maxPanY = (latestCanvasSize.height * (nextScale - 1f)) / 2f
                                    val center = Offset(latestCanvasSize.width / 2f, latestCanvasSize.height / 2f)
                                    val centroidDelta = if (centroid != Offset.Unspecified) {
                                        (centroid - center - panOffset) * (nextScale / zoomScale - 1f)
                                    } else Offset.Zero
                                    zoomScale = nextScale
                                    panOffset = if (nextScale <= 1.01f) {
                                        Offset.Zero
                                    } else {
                                        Offset(
                                            x = (panOffset.x + panChange.x - centroidDelta.x).coerceIn(-maxPanX, maxPanX),
                                            y = (panOffset.y + panChange.y - centroidDelta.y).coerceIn(-maxPanY, maxPanY),
                                        )
                                    }
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                } else {
                                    val ch = pressed.first()
                                    val dist = (ch.position - firstDown.position).getDistance()
                                    if (!movedBeyondSlop && dist > touchSlop) {
                                        movedBeyondSlop = true
                                        if (latestTool == MaskToolKind.BOX) {
                                            val selBox = latestOps
                                                .filterIsInstance<MaskOperation.BoxOp>()
                                                .firstOrNull { it.id == latestSelectedBoxId }
                                            if (selBox != null) {
                                                val hit = hitTestBoxHandle(
                                                    pt = firstDown.position,
                                                    box = selBox,
                                                    canvasSize = latestCanvasSize,
                                                    fit = latestFitRect,
                                                    scale = zoomScale,
                                                    pan = panOffset,
                                                    handleRadiusPx = with(density) { 22.dp.toPx() },
                                                )
                                                dragMode = hit
                                                if (hit != BoxDragMode.NEW_BOX) {
                                                    activeBoxOriginal = selBox
                                                    draftBox = selBox
                                                } else {
                                                    selectedBoxId = null
                                                    draftBox = MaskOperation.BoxOp(
                                                        id = UUID.randomUUID().toString(),
                                                        left = dragStartNorm.x,
                                                        top = dragStartNorm.y,
                                                        right = dragStartNorm.x,
                                                        bottom = dragStartNorm.y,
                                                        style = latestStyle,
                                                    )
                                                }
                                            } else {
                                                dragMode = BoxDragMode.NEW_BOX
                                                draftBox = MaskOperation.BoxOp(
                                                    id = UUID.randomUUID().toString(),
                                                    left = dragStartNorm.x,
                                                    top = dragStartNorm.y,
                                                    right = dragStartNorm.x,
                                                    bottom = dragStartNorm.y,
                                                    style = latestStyle,
                                                )
                                            }
                                        } else {
                                            selectedBoxId = null
                                            activeStrokePoints = strokePts.toList()
                                        }
                                    }

                                    if (movedBeyondSlop) {
                                        val curNorm = screenToNorm(
                                            ch.position,
                                            latestCanvasSize,
                                            latestFitRect,
                                            zoomScale,
                                            panOffset,
                                        )
                                        when (dragMode) {
                                            BoxDragMode.NEW_BOX -> {
                                                draftBox = draftBox?.copy(right = curNorm.x, bottom = curNorm.y)
                                            }
                                            BoxDragMode.MOVE -> {
                                                val orig = activeBoxOriginal
                                                if (orig != null) {
                                                    val dx = curNorm.x - dragStartNorm.x
                                                    val dy = curNorm.y - dragStartNorm.y
                                                    val w = orig.right - orig.left
                                                    val h = orig.bottom - orig.top
                                                    val newL = (orig.left + dx).coerceIn(0f, (1f - w).coerceAtLeast(0f))
                                                    val newT = (orig.top + dy).coerceIn(0f, (1f - h).coerceAtLeast(0f))
                                                    draftBox = orig.copy(left = newL, top = newT, right = newL + w, bottom = newT + h)
                                                }
                                            }
                                            BoxDragMode.RESIZE_TL -> {
                                                draftBox = draftBox?.copy(left = curNorm.x, top = curNorm.y)
                                            }
                                            BoxDragMode.RESIZE_TR -> {
                                                draftBox = draftBox?.copy(right = curNorm.x, top = curNorm.y)
                                            }
                                            BoxDragMode.RESIZE_BL -> {
                                                draftBox = draftBox?.copy(left = curNorm.x, bottom = curNorm.y)
                                            }
                                            BoxDragMode.RESIZE_BR -> {
                                                draftBox = draftBox?.copy(right = curNorm.x, bottom = curNorm.y)
                                            }
                                            BoxDragMode.NONE -> {
                                                if (latestTool == MaskToolKind.STROKE) {
                                                    val last = strokePts.lastOrNull()
                                                    if (last == null || hypot((curNorm.x - last.x).toDouble(), (curNorm.y - last.y).toDouble()) > 0.004) {
                                                        strokePts += curNorm
                                                        activeStrokePoints = strokePts.toList()
                                                    }
                                                }
                                            }
                                        }
                                        ch.consume()
                                    }
                                    if (downTime == 0L) Unit
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val bmp = previewBitmap
                if (rendering || bmp == null) {
                    CircularProgressIndicator(color = palette.primary)
                } else {
                    Image(
                        bitmap = bmp,
                        contentDescription = "局部打码画布",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val activeBoxes = ops.filterIsInstance<MaskOperation.BoxOp>()
                        activeBoxes.forEach { rawBox ->
                            val box = if (draftBox?.id == rawBox.id) draftBox!! else rawBox
                            val tl = normToScreen(NormPoint(box.left, box.top), canvasSize, fitRect, zoomScale, panOffset)
                            val br = normToScreen(NormPoint(box.right, box.bottom), canvasSize, fitRect, zoomScale, panOffset)
                            val isSel = box.id == selectedBoxId
                            drawRect(
                                color = if (isSel) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.45f),
                                topLeft = tl,
                                size = Size((br.x - tl.x).coerceAtLeast(1f), (br.y - tl.y).coerceAtLeast(1f)),
                                style = Stroke(
                                    width = if (isSel) 2.dp.toPx() else 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                                ),
                            )
                            if (isSel) {
                                listOf(
                                    tl,
                                    Offset(br.x, tl.y),
                                    Offset(tl.x, br.y),
                                    br,
                                ).forEach { handlePt ->
                                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = handlePt)
                                    drawCircle(color = Color(0xFF4ADE80), radius = 4.dp.toPx(), center = handlePt)
                                }
                            }
                        }

                        if (draftBox != null && ops.none { it.id == draftBox?.id }) {
                            val d = draftBox!!
                            val tl = normToScreen(NormPoint(minOf(d.left, d.right), minOf(d.top, d.bottom)), canvasSize, fitRect, zoomScale, panOffset)
                            val br = normToScreen(NormPoint(maxOf(d.left, d.right), maxOf(d.top, d.bottom)), canvasSize, fitRect, zoomScale, panOffset)
                            drawRect(
                                color = Color.White.copy(alpha = 0.28f),
                                topLeft = tl,
                                size = Size((br.x - tl.x).coerceAtLeast(1f), (br.y - tl.y).coerceAtLeast(1f)),
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = tl,
                                size = Size((br.x - tl.x).coerceAtLeast(1f), (br.y - tl.y).coerceAtLeast(1f)),
                                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                            )
                        }

                        if (activeStrokePoints.size >= 2) {
                            val path = Path()
                            activeStrokePoints.forEachIndexed { i, np ->
                                val sp = normToScreen(np, canvasSize, fitRect, zoomScale, panOffset)
                                if (i == 0) path.moveTo(sp.x, sp.y) else path.lineTo(sp.x, sp.y)
                            }
                            val strokePx = (brushRadiusNorm * minOf(fitRect.size.width, fitRect.size.height) * zoomScale * 2f)
                                .coerceAtLeast(12f)
                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.55f),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }

                    // Floating delete chip above selected box
                    val selBox = ops.filterIsInstance<MaskOperation.BoxOp>().firstOrNull { it.id == selectedBoxId }
                    if (selBox != null && draftBox == null) {
                        val topCenter = normToScreen(
                            NormPoint((selBox.left + selBox.right) / 2f, selBox.top),
                            canvasSize,
                            fitRect,
                            zoomScale,
                            panOffset,
                        )
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        x = (topCenter.x - with(density) { 36.dp.toPx() }).roundToInt().coerceIn(8, maxOf(8, canvasSize.width - 180)),
                                        y = (topCenter.y - with(density) { 40.dp.toPx() }).roundToInt().coerceAtLeast(8),
                                    )
                                }
                                .clickable {
                                    commitOps(ops.filterNot { it.id == selBox.id })
                                    selectedBoxId = null
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("删除", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Bottom Control Panel
            Surface(color = Color(0xFF141715)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Tools & Undo/Redo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MaskPill(
                                label = "框选",
                                icon = Icons.Rounded.CropSquare,
                                selected = tool == MaskToolKind.BOX,
                                onClick = { tool = MaskToolKind.BOX },
                            )
                            MaskPill(
                                label = "涂抹",
                                icon = Icons.Rounded.Brush,
                                selected = tool == MaskToolKind.STROKE,
                                onClick = {
                                    tool = MaskToolKind.STROKE
                                    selectedBoxId = null
                                },
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { undo() },
                                enabled = undoStack.isNotEmpty(),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = "撤销",
                                    tint = if (undoStack.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.35f),
                                )
                            }
                            IconButton(
                                onClick = { redo() },
                                enabled = redoStack.isNotEmpty(),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Redo,
                                    contentDescription = "重做",
                                    tint = if (redoStack.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.35f),
                                )
                            }
                        }
                    }

                    // Brush styles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("笔触", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                        MaskStylePill("马赛克", brushStyle == MaskBrushStyle.MOSAIC) {
                            brushStyle = MaskBrushStyle.MOSAIC
                        }
                        MaskStylePill("模糊", brushStyle == MaskBrushStyle.BLUR) {
                            brushStyle = MaskBrushStyle.BLUR
                        }
                        MaskStylePill("涂黑", brushStyle == MaskBrushStyle.BLACKOUT) {
                            brushStyle = MaskBrushStyle.BLACKOUT
                        }
                    }

                    if (tool == MaskToolKind.STROKE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("粗细", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                            Slider(
                                value = brushRadiusNorm,
                                onValueChange = { brushRadiusNorm = it },
                                valueRange = 0.015f..0.08f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primary,
                                    activeTrackColor = palette.primary,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Per-image Keep Original toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { keepOriginal = !keepOriginal }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = keepOriginal,
                            onCheckedChange = { keepOriginal = it },
                            colors = CheckboxDefaults.colors(checkedColor = palette.primary, checkmarkColor = palette.onPrimary),
                            modifier = Modifier.size(22.dp),
                        )
                        Column {
                            Text(
                                text = "同时保留原图（看帖人可切换查看原图）",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "不勾选：仅上传打码后成品，原图不上传",
                                color = Color.White.copy(alpha = 0.56f),
                                fontSize = 11.sp,
                            )
                        }
                    }

                    // Explicit prev/next navigation buttons (no horizontal swipe to prevent drawing conflict)
                    if (imageItems.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                enabled = currentIndex > 0 && !saving,
                                onClick = {
                                    scope.launch {
                                        saving = true
                                        val updated = exportAndPersistCurrent()
                                        onItemUpdated(updated)
                                        saving = false
                                        currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                    }
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                                Text("上一张")
                            }
                            Text(
                                text = "${currentIndex + 1} / ${imageItems.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            TextButton(
                                enabled = currentIndex < imageItems.lastIndex && !saving,
                                onClick = {
                                    scope.launch {
                                        saving = true
                                        val updated = exportAndPersistCurrent()
                                        onItemUpdated(updated)
                                        saving = false
                                        currentIndex = (currentIndex + 1).coerceAtMost(imageItems.lastIndex)
                                    }
                                },
                            ) {
                                Text("下一张")
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            containerColor = palette.surface,
            title = { Text("放弃本张修改？", color = palette.text, fontWeight = FontWeight.Bold) },
            text = { Text("当前局部打码或原图保留选项尚未保存。", color = palette.muted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDismiss()
                }) {
                    Text("放弃", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("继续编辑")
                }
            },
        )
    }
}

@Composable
private fun MaskPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = if (selected) palette.primary else Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.onPrimary else Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                color = if (selected) palette.onPrimary else Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MaskStylePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = if (selected) palette.primary.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) palette.primary else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            color = if (selected) palette.primary else Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

private data class FitRect(val topLeft: Offset, val size: Size)

private fun computeFitRect(canvas: IntSize, imgW: Int, imgH: Int): FitRect {
    if (canvas.width <= 0 || canvas.height <= 0 || imgW <= 0 || imgH <= 0) {
        return FitRect(Offset.Zero, Size(canvas.width.toFloat().coerceAtLeast(1f), canvas.height.toFloat().coerceAtLeast(1f)))
    }
    val cw = canvas.width.toFloat()
    val ch = canvas.height.toFloat()
    val scale = minOf(cw / imgW.toFloat(), ch / imgH.toFloat())
    val dw = imgW * scale
    val dh = imgH * scale
    return FitRect(
        topLeft = Offset((cw - dw) / 2f, (ch - dh) / 2f),
        size = Size(dw.coerceAtLeast(1f), dh.coerceAtLeast(1f)),
    )
}

private fun screenToNorm(
    screenPos: Offset,
    canvasSize: IntSize,
    fit: FitRect,
    scale: Float,
    pan: Offset,
): NormPoint {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val baseX = (screenPos.x - cx - pan.x) / scale + cx
    val baseY = (screenPos.y - cy - pan.y) / scale + cy
    val nx = ((baseX - fit.topLeft.x) / fit.size.width).coerceIn(0f, 1f)
    val ny = ((baseY - fit.topLeft.y) / fit.size.height).coerceIn(0f, 1f)
    return NormPoint(nx, ny)
}

private fun normToScreen(
    norm: NormPoint,
    canvasSize: IntSize,
    fit: FitRect,
    scale: Float,
    pan: Offset,
): Offset {
    val baseX = fit.topLeft.x + norm.x * fit.size.width
    val baseY = fit.topLeft.y + norm.y * fit.size.height
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    return Offset(
        x = (baseX - cx) * scale + cx + pan.x,
        y = (baseY - cy) * scale + cy + pan.y,
    )
}

private fun hitTestBoxHandle(
    pt: Offset,
    box: MaskOperation.BoxOp,
    canvasSize: IntSize,
    fit: FitRect,
    scale: Float,
    pan: Offset,
    handleRadiusPx: Float,
): BoxDragMode {
    val tl = normToScreen(NormPoint(box.left, box.top), canvasSize, fit, scale, pan)
    val tr = normToScreen(NormPoint(box.right, box.top), canvasSize, fit, scale, pan)
    val bl = normToScreen(NormPoint(box.left, box.bottom), canvasSize, fit, scale, pan)
    val br = normToScreen(NormPoint(box.right, box.bottom), canvasSize, fit, scale, pan)
    return when {
        (pt - tl).getDistance() <= handleRadiusPx -> BoxDragMode.RESIZE_TL
        (pt - tr).getDistance() <= handleRadiusPx -> BoxDragMode.RESIZE_TR
        (pt - bl).getDistance() <= handleRadiusPx -> BoxDragMode.RESIZE_BL
        (pt - br).getDistance() <= handleRadiusPx -> BoxDragMode.RESIZE_BR
        pt.x in tl.x..br.x && pt.y in tl.y..br.y -> BoxDragMode.MOVE
        else -> BoxDragMode.NEW_BOX
    }
}

private fun decodeDownsampledSource(path: String, maxEdgePx: Int): DecodedMaskSource? {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while ((bounds.outWidth / sampleSize) > maxEdgePx * 2 || (bounds.outHeight / sampleSize) > maxEdgePx * 2) {
        sampleSize *= 2
    }
    val raw = BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    ) ?: return null
    val maxDim = maxOf(raw.width, raw.height)
    val scaled = if (maxDim > maxEdgePx) {
        val ratio = maxEdgePx.toFloat() / maxDim.toFloat()
        val w = (raw.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (raw.height * ratio).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(raw, w, h, true).also {
            if (it !== raw) raw.recycle()
        }
    } else raw
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    val res = DecodedMaskSource(scaled.width, scaled.height, pixels)
    scaled.recycle()
    return res
}
