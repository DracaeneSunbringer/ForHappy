package com.jinhuan.forhappy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette

/**
 * Compact capsule search bar aligned with ForHappy's native Modern Airy / Classic palettes,
 * replacing oversized Material3 OutlinedTextField boxes in toolbars and sheets.
 */
@Composable
fun ForHappySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    containerColor: Color? = null,
    onSearch: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    autoFocus: Boolean = false,
) {
    val palette = LocalForHappyPalette.current
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(if (palette.isModernUi) 999.dp else 10.dp)
    val bgColor = containerColor ?: palette.surface2.copy(alpha = if (palette.isDark) 0.9f else 0.78f)
    val borderColor = when {
        isFocused -> palette.primary.copy(alpha = 0.72f)
        palette.isModernUi && palette.cardBorder != Color.Transparent -> palette.cardBorder
        else -> palette.line.copy(alpha = 0.65f)
    }
    val textSize = if (height <= 34.dp) 12.5.sp else 13.5.sp

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = if (height <= 34.dp) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = if (isFocused) palette.primary else palette.muted,
            modifier = Modifier.size(if (height <= 34.dp) 15.dp else 17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = palette.muted,
                    fontSize = textSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.text, fontSize = textSize),
                cursorBrush = SolidColor(palette.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch?.invoke() },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }
        if (value.isNotEmpty() || onClear != null) {
            Spacer(Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (onClear != null) {
                            onClear()
                        } else {
                            onValueChange("")
                        }
                    },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "清空",
                    tint = palette.muted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Compact rounded text field built on BasicTextField with optional top caption label,
 * focus-highlighted border, and 40.dp default single-line height matching adjacent Buttons.
 */
@Composable
fun ForHappyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minHeight: Dp = if (singleLine) 40.dp else (minLines * 20 + 18).dp,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    containerColor: Color? = null,
    contentColor: Color? = null,
    placeholderColor: Color? = null,
    shape: Shape? = null,
) {
    val palette = LocalForHappyPalette.current
    var isFocused by remember { mutableStateOf(false) }
    val resolvedShape = shape ?: RoundedCornerShape(12.dp)
    val bgColor = containerColor ?: palette.surface2.copy(alpha = if (palette.isDark) 0.85f else 0.72f)
    val textColor = (contentColor ?: palette.text).let { if (enabled) it else it.copy(alpha = 0.55f) }
    val hintColor = placeholderColor ?: palette.muted.copy(alpha = 0.85f)
    val borderColor = when {
        isError -> palette.danger
        isFocused && enabled -> palette.primary.copy(alpha = 0.85f)
        else -> palette.line.copy(alpha = 0.75f)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = when {
                    isError -> palette.danger
                    isFocused -> palette.primary
                    else -> palette.muted
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(resolvedShape)
                .background(bgColor)
                .border(if (isFocused || isError) 1.2.dp else 1.dp, borderColor, resolvedShape)
                .padding(
                    horizontal = 12.dp,
                    vertical = if (singleLine) 8.dp else 10.dp,
                ),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leadingIcon != null) {
                Box(contentAlignment = Alignment.Center) {
                    leadingIcon()
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                    Text(
                        text = placeholder,
                        color = hintColor,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        maxLines = if (singleLine) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = maxLines,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                    ),
                    cursorBrush = SolidColor(palette.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                )
            }
            if (trailingIcon != null) {
                Box(contentAlignment = Alignment.Center) {
                    trailingIcon()
                }
            }
        }

        if (supportingText != null) {
            Box(modifier = Modifier.padding(start = 2.dp)) {
                supportingText()
            }
        }
    }
}

/**
 * TextFieldValue overload preserving cursor & selection ranges (used by ComposeWorkbench rich editor).
 */
@Composable
fun ForHappyTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minHeight: Dp = if (singleLine) 40.dp else (minLines * 20 + 18).dp,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    containerColor: Color? = null,
    contentColor: Color? = null,
    placeholderColor: Color? = null,
    shape: Shape? = null,
) {
    val palette = LocalForHappyPalette.current
    var isFocused by remember { mutableStateOf(false) }
    val resolvedShape = shape ?: RoundedCornerShape(12.dp)
    val bgColor = containerColor ?: palette.surface2.copy(alpha = if (palette.isDark) 0.85f else 0.72f)
    val textColor = (contentColor ?: palette.text).let { if (enabled) it else it.copy(alpha = 0.55f) }
    val hintColor = placeholderColor ?: palette.muted.copy(alpha = 0.85f)
    val borderColor = when {
        isError -> palette.danger
        isFocused && enabled -> palette.primary.copy(alpha = 0.85f)
        else -> palette.line.copy(alpha = 0.75f)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = when {
                    isError -> palette.danger
                    isFocused -> palette.primary
                    else -> palette.muted
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(resolvedShape)
                .background(bgColor)
                .border(if (isFocused || isError) 1.2.dp else 1.dp, borderColor, resolvedShape)
                .padding(
                    horizontal = 12.dp,
                    vertical = if (singleLine) 8.dp else 10.dp,
                ),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leadingIcon != null) {
                Box(contentAlignment = Alignment.Center) {
                    leadingIcon()
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.text.isEmpty() && !placeholder.isNullOrBlank()) {
                    Text(
                        text = placeholder,
                        color = hintColor,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        maxLines = if (singleLine) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = maxLines,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                    ),
                    cursorBrush = SolidColor(palette.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                )
            }
            if (trailingIcon != null) {
                Box(contentAlignment = Alignment.Center) {
                    trailingIcon()
                }
            }
        }

        if (supportingText != null) {
            Box(modifier = Modifier.padding(start = 2.dp)) {
                supportingText()
            }
        }
    }
}
