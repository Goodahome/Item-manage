package com.example.itemremindertool.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

/**
 * 颜色辅助函数
 * UI配色分组管理，自动适配浅色/深色主题和所有配色方案
 * 
 * 重要说明：
 * 所有颜色从 MaterialTheme.colorScheme 动态获取
 * 支持以下功能：
 * - ✅ 自动适配浅色/深色主题
 * - ✅ 支持 6 种配色方案切换（冷冽蓝、奶油橙、薄荷绿、深空灰、红酒红、圣诞）
 * - ✅ 跟随系统主题
 * 
 * 颜色分组映射到 MaterialTheme：
 * 第一组（导航）→ surface
 * 第二组（页面背景）→ background；（按钮）→ primary
 * 第三组（卡片）→ surfaceVariant
 * 第四组（文字图标）→ onSurface
 * 第五组（提醒）→ error；（FAB）→ primaryContainer
 */
object ColorHelpers {
    
    // ==================== 第一组：导航与菜单 ====================
    
    /**
     * 顶部导航栏背景色
     * 映射到 MaterialTheme.colorScheme.surface
     */
    @Composable
    fun getGroup1NavBarColor(): Color {
        return MaterialTheme.colorScheme.surface
    }
    
    /**
     * 侧边抽屉菜单背景色
     * 映射到 MaterialTheme.colorScheme.surface
     */
    @Composable
    fun getGroup1DrawerColor(): Color {
        return MaterialTheme.colorScheme.surface
    }
    
    /**
     * 侧边抽屉菜单项背景色
     * 映射到 MaterialTheme.colorScheme.surface
     */
    @Composable
    fun getGroup1DrawerItemColor(): Color {
        return MaterialTheme.colorScheme.surface
    }
    
    
    // ==================== 第二组：页面与按钮 ====================
    
    /**
     * 页面背景色
     * 映射到 MaterialTheme.colorScheme.background
     */
    @Composable
    fun getGroup2PageBgColor(): Color {
        return MaterialTheme.colorScheme.background
    }
    
    /**
     * 设置按钮背景色
     * 映射到 MaterialTheme.colorScheme.primary
     */
    @Composable
    fun getGroup2SettingsBtnColor(): Color {
        return MaterialTheme.colorScheme.primary
    }
    
    /**
     * 根据背景颜色计算对比度，返回黑色或白色
     * 使用相对亮度公式 (L = 0.299*R + 0.587*G + 0.114*B)
     * 如果背景较亮（亮度 > 0.5），返回黑色；否则返回白色
     */
    fun getContrastColor(backgroundColor: Color): Color {
        // 计算相对亮度
        val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
        // 如果背景较亮，返回黑色；否则返回白色
        return if (luminance > 0.5f) {
            Color.Black
        } else {
            Color.White
        }
    }
    
    
    // ==================== 第三组：卡片 ====================
    
    /**
     * 卡片背景色
     * 映射到 MaterialTheme.colorScheme.surfaceVariant
     */
    @Composable
    fun getGroup3CardBgColor(): Color {
        return MaterialTheme.colorScheme.surfaceVariant
    }
    
    
    // ==================== 第四组：文字与图标 ====================
    
    /**
     * 主要文字颜色
     * 映射到 MaterialTheme.colorScheme.onSurface
     */
    @Composable
    fun getGroup4TextColor(): Color {
        return MaterialTheme.colorScheme.onSurface
    }
    
    /**
     * 主要文字颜色（带透明度）
     */
    @Composable
    fun getGroup4TextColor(alpha: Float): Color {
        return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    }
    
    /**
     * 主要图标颜色
     * 映射到 MaterialTheme.colorScheme.onSurface
     */
    @Composable
    fun getGroup4IconColor(): Color {
        return MaterialTheme.colorScheme.onSurface
    }
    
    /**
     * 主要图标颜色（带透明度）
     */
    @Composable
    fun getGroup4IconColor(alpha: Float): Color {
        return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    }
    
    
    // ==================== 第五组：提醒与强调 ====================
    
    /**
     * 提醒卡片背景色
     * 映射到 MaterialTheme.colorScheme.error（醒目的提醒颜色）
     */
    @Composable
    fun getGroup5AlertCardColor(): Color {
        return MaterialTheme.colorScheme.error
    }
    
    /**
     * 悬浮操作按钮（FAB）背景色
     * 映射到 MaterialTheme.colorScheme.primaryContainer
     */
    @Composable
    fun getGroup5FabColor(): Color {
        return MaterialTheme.colorScheme.primaryContainer
    }
    
    
    // ==================== 特殊用途颜色 ====================
    
    /**
     * 分隔线颜色
     */
    @Composable
    fun getDividerColor(): Color {
        return MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    
    /**
     * 对话框背景色
     */
    @Composable
    fun getDialogBackgroundColor(): Color {
        return MaterialTheme.colorScheme.surface
    }
    
    /**
     * 输入框聚焦边框颜色
     */
    @Composable
    fun getFocusedBorderColor(): Color {
        return MaterialTheme.colorScheme.primary
    }
    
    /**
     * 输入框未聚焦边框颜色
     */
    @Composable
    fun getUnfocusedBorderColor(): Color {
        return MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
}

