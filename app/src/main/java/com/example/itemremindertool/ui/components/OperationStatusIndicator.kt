package com.example.itemremindertool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.OperationState

/**
 * 底部状态指示器组件
 * 显示在屏幕底部居中位置，与FAB位置相似
 */
@Composable
fun BottomOperationStatusIndicator(
    operationState: OperationState,
    modifier: Modifier = Modifier
) {
    // 调试：记录状态变化
    android.util.Log.d("BottomOperationStatusIndicator", "当前状态: $operationState")
    
    when (val state = operationState) {
        is OperationState.Idle -> {
            // 不显示任何内容
        }
        is OperationState.Saving -> {
            BottomStatusBanner(
                message = "正在保存...",
                isLoading = true,
                modifier = modifier
            )
        }
        is OperationState.Deleting -> {
            BottomStatusBanner(
                message = "正在删除...",
                isLoading = true,
                modifier = modifier
            )
        }
        is OperationState.Syncing -> {
            // 不显示同步中的提示，只通过下拉刷新图标显示
        }
        is OperationState.Success -> {
            BottomStatusBanner(
                message = state.message,
                isLoading = false,
                isSuccess = true,
                modifier = modifier
            )
        }
        is OperationState.Error -> {
            BottomStatusBanner(
                message = state.message,
                isLoading = false,
                isSuccess = false,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun BottomStatusBanner(
    message: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(24.dp),
            color = when {
                isSuccess -> ColorHelpers.getGroup3CardBgColor()
                !isSuccess && !isLoading -> MaterialTheme.colorScheme.errorContainer
                else -> ColorHelpers.getGroup3CardBgColor()
            },
            shadowElevation = 8.dp
        ) {
            Text(
                text = message,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (!isSuccess && !isLoading) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    ColorHelpers.getGroup4TextColor()
                },
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

