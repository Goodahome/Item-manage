package com.example.itemremindertool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * 自动调整文字大小的Text组件
 * 确保文字不会超过容器宽度或被迫换行
 * 
 * @param text 要显示的文本
 * @param modifier 修饰符
 * @param maxFontSize 最大字体大小（默认16sp）
 * @param minFontSize 最小字体大小（默认10sp）
 * @param style 文本样式
 * @param color 文字颜色
 * @param fontWeight 字体粗细
 * @param textAlign 文本对齐方式
 * @param maxLines 最大行数（默认1）
 * @param overflow 溢出处理方式
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 10.sp,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    var containerWidth by remember { mutableStateOf(0) }
    var textWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    // 计算合适的字体大小
    val fontSize: TextUnit = remember(text, containerWidth, textWidth, maxFontSize, minFontSize, density) {
        if (containerWidth > 0 && textWidth > 0 && text.isNotEmpty()) {
            val maxSizePx = with(density) { maxFontSize.toPx() }
            val minSizePx = with(density) { minFontSize.toPx() }
            
            // 如果文本宽度小于容器宽度，使用最大字体
            if (textWidth <= containerWidth) {
                maxFontSize
            } else {
                // 计算缩放比例
                val scale = containerWidth.toFloat() / textWidth.toFloat()
                val scaledSizePx = maxSizePx * scale
                
                // 确保不小于最小字体
                val finalSizePx = scaledSizePx.coerceAtLeast(minSizePx)
                with(density) { finalSizePx.toSp() }
            }
        } else {
            maxFontSize
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            style = style?.copy(
                fontSize = fontSize,
                fontWeight = fontWeight ?: style.fontWeight,
                color = if (color != Color.Unspecified) color else style.color
            ) ?: TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color
            ),
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                textWidth = textLayoutResult.size.width
            }
        )
    }
}

/**
 * 自动调整文字大小的Button组件
 * 确保按钮文字不会超过容器宽度
 */
@Composable
fun AutoSizeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 10.sp,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit = {}
) {
    var buttonWidth by remember { mutableStateOf(0) }
    var textWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    // 计算合适的字体大小
    val fontSize: TextUnit = remember(text, buttonWidth, textWidth, maxFontSize, minFontSize, density) {
        if (buttonWidth > 0 && textWidth > 0 && text.isNotEmpty()) {
            val maxSizePx = with(density) { maxFontSize.toPx() }
            val minSizePx = with(density) { minFontSize.toPx() }
            
            if (textWidth <= buttonWidth) {
                maxFontSize
            } else {
                val scale = buttonWidth.toFloat() / textWidth.toFloat()
                val scaledSizePx = maxSizePx * scale
                val finalSizePx = scaledSizePx.coerceAtLeast(minSizePx)
                with(density) { finalSizePx.toSp() }
            }
        } else {
            maxFontSize
        }
    }
    
    val textColor = MaterialTheme.colorScheme.onPrimary
    
    Button(
        onClick = onClick,
        modifier = modifier.onSizeChanged { buttonWidth = it.width },
        enabled = enabled,
        colors = colors
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                textWidth = textLayoutResult.size.width
            },
            modifier = Modifier.fillMaxWidth()
        )
        content()
    }
}

/**
 * 自动调整文字大小的TextButton组件
 * 确保按钮文字不会超过容器宽度
 */
@Composable
fun AutoSizeTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 10.sp,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit = {}
) {
    var buttonWidth by remember { mutableStateOf(0) }
    var textWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    // 计算合适的字体大小
    val fontSize: TextUnit = remember(text, buttonWidth, textWidth, maxFontSize, minFontSize, density) {
        if (buttonWidth > 0 && textWidth > 0 && text.isNotEmpty()) {
            val maxSizePx = with(density) { maxFontSize.toPx() }
            val minSizePx = with(density) { minFontSize.toPx() }
            
            if (textWidth <= buttonWidth) {
                maxFontSize
            } else {
                val scale = buttonWidth.toFloat() / textWidth.toFloat()
                val scaledSizePx = maxSizePx * scale
                val finalSizePx = scaledSizePx.coerceAtLeast(minSizePx)
                with(density) { finalSizePx.toSp() }
            }
        } else {
            maxFontSize
        }
    }
    
    val textColor = MaterialTheme.colorScheme.primary
    
    TextButton(
        onClick = onClick,
        modifier = modifier.onSizeChanged { buttonWidth = it.width },
        enabled = enabled,
        colors = colors
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                textWidth = textLayoutResult.size.width
            },
            modifier = Modifier.fillMaxWidth()
        )
        content()
    }
}
