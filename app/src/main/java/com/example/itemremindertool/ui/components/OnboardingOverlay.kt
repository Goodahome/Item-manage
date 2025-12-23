package com.example.itemremindertool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.itemremindertool.R
import com.example.itemremindertool.ui.theme.ColorHelpers

/**
 * 首次使用引导步骤枚举
 */
enum class OnboardingStep {
    WELCOME,           // 欢迎页面
    HOME_AREA,         // 首页区域介绍
    ADD_WAREHOUSE,     // 如何添加容器
    ADD_ITEM,          // 如何添加物品
    SETTINGS,          // 设置功能介绍
    COMPLETE           // 完成
}

/**
 * 首次使用引导覆盖层
 */
@Composable
fun OnboardingOverlay(
    currentStep: OnboardingStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    highlightedArea: HighlightedArea? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onComplete,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // 半透明遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            
            // 高亮区域（如果有）
            highlightedArea?.let { area ->
                HighlightedAreaOverlay(area)
            }
            
            // 引导卡片
            when (currentStep) {
                OnboardingStep.WELCOME -> {
                    WelcomeStep(
                        onNext = onNext,
                        onSkip = onSkip,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                OnboardingStep.HOME_AREA -> {
                    HomeAreaStep(
                        onNext = onNext,
                        onSkip = onSkip,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .padding(top = 100.dp)
                    )
                }
                OnboardingStep.ADD_WAREHOUSE -> {
                    AddWarehouseStep(
                        onNext = onNext,
                        onSkip = onSkip,
                        highlightedArea = highlightedArea,
                        modifier = Modifier.align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 100.dp)
                    )
                }
                OnboardingStep.ADD_ITEM -> {
                    AddItemStep(
                        onNext = onNext,
                        onSkip = onSkip,
                        highlightedArea = highlightedArea,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 100.dp)
                    )
                }
                OnboardingStep.SETTINGS -> {
                    SettingsStep(
                        onNext = onNext,
                        onSkip = onSkip,
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(top = 100.dp, start = 16.dp)
                    )
                }
                OnboardingStep.COMPLETE -> {
                    CompleteStep(
                        onComplete = onComplete,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * 高亮区域数据类
 */
data class HighlightedArea(
    val rect: Rect,
    val shape: HighlightShape = HighlightShape.RECTANGLE
)

enum class HighlightShape {
    RECTANGLE,
    CIRCLE
}

/**
 * 高亮区域覆盖层
 * 注意：当前版本简化实现，未来可以扩展为使用 Canvas 绘制高亮效果
 */
@Composable
fun HighlightedAreaOverlay(area: HighlightedArea) {
    // 未来可以在这里实现高亮区域的视觉效果
    // 例如使用 Canvas 绘制遮罩层，只显示高亮区域
}

/**
 * 欢迎步骤
 */
@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingCard(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_description),
        icon = Icons.Default.Info,
        onNext = onNext,
        onSkip = onSkip,
        showSkip = true,
        modifier = modifier
    )
}

/**
 * 首页区域介绍步骤
 */
@Composable
fun HomeAreaStep(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingCard(
        title = stringResource(R.string.onboarding_home_area_title),
        description = stringResource(R.string.onboarding_home_area_description),
        icon = Icons.Default.Home,
        onNext = onNext,
        onSkip = onSkip,
        showSkip = true,
        modifier = modifier
    )
}

/**
 * 添加容器步骤
 */
@Composable
fun AddWarehouseStep(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    highlightedArea: HighlightedArea?,
    modifier: Modifier = Modifier
) {
    OnboardingCard(
        title = stringResource(R.string.onboarding_add_warehouse_title),
        description = stringResource(R.string.onboarding_add_warehouse_description),
        icon = Icons.Default.Add,
        onNext = onNext,
        onSkip = onSkip,
        showSkip = true,
        modifier = modifier
    )
}

/**
 * 添加物品步骤
 */
@Composable
fun AddItemStep(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    highlightedArea: HighlightedArea?,
    modifier: Modifier = Modifier
) {
    OnboardingCard(
        title = stringResource(R.string.onboarding_add_item_title),
        description = stringResource(R.string.onboarding_add_item_description),
        icon = Icons.Default.AddShoppingCart,
        onNext = onNext,
        onSkip = onSkip,
        showSkip = true,
        modifier = modifier
    )
}

/**
 * 设置功能步骤
 */
@Composable
fun SettingsStep(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingCard(
        title = stringResource(R.string.onboarding_settings_title),
        description = stringResource(R.string.onboarding_settings_description),
        icon = Icons.Default.Settings,
        onNext = onNext,
        onSkip = onSkip,
        showSkip = true,
        modifier = modifier
    )
}

/**
 * 完成步骤
 */
@Composable
fun CompleteStep(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingCard(
        title = stringResource(R.string.onboarding_complete_title),
        description = stringResource(R.string.onboarding_complete_description),
        icon = Icons.Default.CheckCircle,
        onNext = onComplete,
        onSkip = null,
        showSkip = false,
        nextButtonText = stringResource(R.string.onboarding_get_started),
        modifier = modifier
    )
}

/**
 * 引导卡片组件
 */
@Composable
fun OnboardingCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    showSkip: Boolean,
    nextButtonText: String = stringResource(R.string.onboarding_next),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 400.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ColorHelpers.getGroup2SettingsBtnColor()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ColorHelpers.getGroup4TextColor(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 描述
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor(0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showSkip && onSkip != null) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = ColorHelpers.getGroup4TextColor(0.7f)
                        )
                    }
                }
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorHelpers.getGroup2SettingsBtnColor()
                    )
                ) {
                    Text(
                        text = nextButtonText,
                        color = ColorHelpers.getContrastColor(ColorHelpers.getGroup2SettingsBtnColor())
                    )
                }
            }
        }
    }
}

