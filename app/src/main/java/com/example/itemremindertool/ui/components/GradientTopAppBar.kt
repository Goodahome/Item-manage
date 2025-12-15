package com.example.itemremindertool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.R
import com.example.itemremindertool.ui.theme.ColorHelpers

/**
 * 带渐变背景的 TopAppBar 组件
 * 自动根据主题色创建左右渐变，并根据对比度调整文字和图标颜色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = isSystemInDarkTheme()
    
    // 创建左右渐变：根据浅色/深色模式使用不同的渐变策略
    val gradientBrush = remember(primaryColor, primaryContainerColor, surfaceColor, isDarkTheme) {
        if (isDarkTheme) {
            // 深色模式：使用更暗的渐变，从调暗的 primary 色渐变到更暗的颜色
            // 左侧：将 primary 色调暗到 50%，使其在深色背景下更协调
            val darkStartColor = Color(
                red = (primaryColor.red * 0.5f).coerceIn(0f, 1f),
                green = (primaryColor.green * 0.5f).coerceIn(0f, 1f),
                blue = (primaryColor.blue * 0.5f).coerceIn(0f, 1f),
                alpha = primaryColor.alpha
            )
            
            // 右侧：进一步调暗，混合黑色或使用 surface 颜色
            val darkEndColor = if (primaryContainerColor.alpha > 0.1f) {
                // 混合调暗的 primary 和 primaryContainer，并进一步调暗
                val mixedColor = Color(
                    red = (primaryColor.red * 0.2f + primaryContainerColor.red * 0.8f).coerceIn(0f, 1f),
                    green = (primaryColor.green * 0.2f + primaryContainerColor.green * 0.8f).coerceIn(0f, 1f),
                    blue = (primaryColor.blue * 0.2f + primaryContainerColor.blue * 0.8f).coerceIn(0f, 1f),
                    alpha = primaryColor.alpha
                )
                // 再混合一些黑色，进一步降低亮度
                Color(
                    red = (mixedColor.red * 0.7f + Color.Black.red * 0.3f).coerceIn(0f, 1f),
                    green = (mixedColor.green * 0.7f + Color.Black.green * 0.3f).coerceIn(0f, 1f),
                    blue = (mixedColor.blue * 0.7f + Color.Black.blue * 0.3f).coerceIn(0f, 1f),
                    alpha = primaryColor.alpha
                )
            } else {
                // 如果 primaryContainer 不可用，直接调暗 primary 色并混合黑色
                Color(
                    red = (primaryColor.red * 0.4f + Color.Black.red * 0.6f).coerceIn(0f, 1f),
                    green = (primaryColor.green * 0.4f + Color.Black.green * 0.6f).coerceIn(0f, 1f),
                    blue = (primaryColor.blue * 0.4f + Color.Black.blue * 0.6f).coerceIn(0f, 1f),
                    alpha = primaryColor.alpha
                )
            }
            
            Brush.horizontalGradient(
                colors = listOf(
                    darkStartColor,  // 左侧：调暗的 primary 色（50%）
                    darkEndColor     // 右侧：更暗的颜色（混合黑色）
                )
            )
        } else {
            // 浅色模式：从 primary 色渐变到浅色版本（混合白色）
            val mixRatio = 0.6f // 60% 白色混合
            val lightPrimary = Color(
                red = (primaryColor.red * (1 - mixRatio) + Color.White.red * mixRatio).coerceIn(0f, 1f),
                green = (primaryColor.green * (1 - mixRatio) + Color.White.green * mixRatio).coerceIn(0f, 1f),
                blue = (primaryColor.blue * (1 - mixRatio) + Color.White.blue * mixRatio).coerceIn(0f, 1f),
                alpha = primaryColor.alpha
            )
            Brush.horizontalGradient(
                colors = listOf(
                    primaryColor,    // 左侧：完整的 primary 色（深）
                    lightPrimary     // 右侧：混合 60% 白色的浅色版本（浅）
                )
            )
        }
    }
    
    // 根据背景颜色计算对比色
    // 在深色模式下，使用调暗后的起始颜色；在浅色模式下，使用原始的 primary 色
    val contrastColor = remember(primaryColor, isDarkTheme) {
        if (isDarkTheme) {
            // 深色模式：使用调暗后的 primary 色（50%）来计算对比色
            val darkPrimary = Color(
                red = (primaryColor.red * 0.5f).coerceIn(0f, 1f),
                green = (primaryColor.green * 0.5f).coerceIn(0f, 1f),
                blue = (primaryColor.blue * 0.5f).coerceIn(0f, 1f),
                alpha = primaryColor.alpha
            )
            ColorHelpers.getContrastColor(darkPrimary)
        } else {
            // 浅色模式：使用原始的 primary 色
            ColorHelpers.getContrastColor(primaryColor)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 让渐变铺满到状态栏区域，避免状态栏出现白色
            .background(gradientBrush)
            .statusBarsPadding()
    ) {
        if (navigationIcon != null) {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contrastColor,
                    navigationIconContentColor = contrastColor,
                    actionIconContentColor = contrastColor
                )
            )
        } else {
            TopAppBar(
                title = title,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contrastColor,
                    navigationIconContentColor = contrastColor,
                    actionIconContentColor = contrastColor
                )
            )
        }
    }
}

/**
 * 带渐变背景的 TopAppBar 组件（简化版本，带返回按钮）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopAppBarWithBack(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    GradientTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = actions
    )
}

