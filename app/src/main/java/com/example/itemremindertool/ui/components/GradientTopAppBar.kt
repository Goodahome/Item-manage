package com.example.itemremindertool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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
    // 从 ColorHelpers 获取渐变颜色
    val gradientStartColor = ColorHelpers.getTopBarGradientStart()
    val gradientEndColor = ColorHelpers.getTopBarGradientEnd()
    
    // 判断是否需要渐变：如果开始和结束颜色相同，则使用纯色，否则使用渐变
    val useGradient = gradientStartColor != gradientEndColor
    
    // 创建背景修饰（需要渐变时使用 Brush，否则使用纯色）
    val backgroundModifier = remember(gradientStartColor, gradientEndColor, useGradient) {
        if (useGradient) {
            Modifier.background(
                Brush.horizontalGradient(
                    colors = listOf(
                        gradientStartColor,  // 左侧：渐变开始颜色
                        gradientEndColor     // 右侧：渐变结束颜色
                    )
                )
            )
        } else {
            Modifier.background(gradientStartColor)
        }
    }
    
    // 根据背景颜色和对比度判断，返回对应的文字/图标颜色
    val contrastColor = ColorHelpers.getGroup4TextColorByContrast(gradientStartColor)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(0.dp),
                clip = false
            )
            // 让背景铺满到状态栏区域，避免状态栏出现白色
            .then(backgroundModifier)
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

