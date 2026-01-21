package com.example.itemremindertool.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * 颜色辅助函数
 * UI配色分组管理，自动适配浅色/深色主题和所有配色方案
 * 
 * 重要说明：
 * 所有颜色从 MaterialTheme.colorScheme 动态获取
 * 支持以下功能：
 * - ✅ 自动适配浅色/深色主题
 * - ✅ 支持 6 种配色方案切换（红蓝配色、奶油橙、薄荷绿、深空灰、红酒红、圣诞）
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
    
    /**
     * 计算两个颜色之间的对比度比率
     * 使用 WCAG 对比度公式
     * 返回 0.0 到 21.0 之间的值（21.0 是最大对比度，即黑白对比）
     */
    fun calculateContrastRatio(color1: Color, color2: Color): Double {
        fun getLuminance(color: Color): Double {
            fun adjustComponent(component: Float): Double {
                return if (component <= 0.03928) {
                    component / 12.92
                } else {
                    Math.pow((component + 0.055) / 1.055, 2.4)
                }
            }
            val r = adjustComponent(color.red)
            val g = adjustComponent(color.green)
            val b = adjustComponent(color.blue)
            return 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        
        val lum1 = getLuminance(color1)
        val lum2 = getLuminance(color2)
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * 根据背景色和对比度判断，返回对应的文字颜色
     * 如果对比度低于0.5（这里指对比度比率低于某个阈值），使用低对比度颜色
     * 否则使用高对比度颜色
     */
    @Composable
    fun getGroup4TextColorByContrast(backgroundColor: Color): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
        
        // 计算背景色的相对亮度（0.0 = 黑色, 1.0 = 白色）
        // 对透明色进行复合，使用当前主题背景色，避免系统主题影响
        val baseBackground = backgroundColor.compositeOver(MaterialTheme.colorScheme.background)
        val luminance = baseBackground.luminance()
        
        // 根据背景亮度选择文字颜色：
        // - 亮度 > 0.5：背景较亮，使用深色文字（OnSurfaceLowContrast，黑色）
        // - 亮度 <= 0.5：背景较暗，使用浅色文字（OnSurfaceHighContrast，白色）
        val useDarkText = luminance > 0.5f
        
        // 所有配色方案都使用相同的对比度颜色（黑色和白色）
        return if (isDarkTheme) {
            // 深色主题
            if (useDarkText) {
                // 较亮的背景，使用深色文字（低对比度颜色，黑色）
                OnSurfaceDarkLowContrast
            } else {
                // 较暗的背景，使用浅色文字（高对比度颜色，白色）
                OnSurfaceDarkHighContrast
            }
        } else {
            // 浅色主题
            if (useDarkText) {
                // 较亮的背景，使用深色文字（低对比度颜色，黑色）
                OnSurfaceLowContrast
            } else {
                // 较暗的背景，使用浅色文字（高对比度颜色，白色）
                OnSurfaceHighContrast
            }
        }
    }
    
    /**
     * 根据背景色和对比度判断，返回对应的图标颜色
     */
    @Composable
    fun getGroup4IconColorByContrast(backgroundColor: Color): Color {
        return getGroup4TextColorByContrast(backgroundColor)
    }
    
    /**
     * 根据背景色和对比度判断，返回对应的文字颜色（带透明度）
     */
    @Composable
    fun getGroup4TextColorByContrast(backgroundColor: Color, alpha: Float): Color {
        return getGroup4TextColorByContrast(backgroundColor).copy(alpha = alpha)
    }
    
    /**
     * 根据背景色和对比度判断，返回对应的图标颜色（带透明度）
     */
    @Composable
    fun getGroup4IconColorByContrast(backgroundColor: Color, alpha: Float): Color {
        return getGroup4IconColorByContrast(backgroundColor).copy(alpha = alpha)
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
     * 根据背景色对比度自动选择对应的颜色
     * 如果未提供背景色，使用页面背景色
     */
    @Composable
    fun getGroup4TextColor(backgroundColor: Color? = null): Color {
        val bgColor = backgroundColor ?: getGroup2PageBgColor()
        return getGroup4TextColorByContrast(bgColor)
    }
    
    /**
     * 主要文字颜色（带透明度）
     */
    @Composable
    fun getGroup4TextColor(alpha: Float, backgroundColor: Color? = null): Color {
        val bgColor = backgroundColor ?: getGroup2PageBgColor()
        return getGroup4TextColorByContrast(bgColor, alpha)
    }
    
    /**
     * 主要图标颜色
     * 根据背景色对比度自动选择对应的颜色
     * 如果未提供背景色，使用页面背景色
     */
    @Composable
    fun getGroup4IconColor(backgroundColor: Color? = null): Color {
        val bgColor = backgroundColor ?: getGroup2PageBgColor()
        return getGroup4IconColorByContrast(bgColor)
    }
    
    /**
     * 主要图标颜色（带透明度）
     */
    @Composable
    fun getGroup4IconColor(alpha: Float, backgroundColor: Color? = null): Color {
        val bgColor = backgroundColor ?: getGroup2PageBgColor()
        return getGroup4IconColorByContrast(bgColor, alpha)
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
    
    // ==================== 顶部渐变颜色 ====================
    
    /**
     * 获取顶部渐变开始颜色（左侧）
     * 根据当前颜色方案和主题模式返回对应的渐变开始颜色
     */
    @Composable
    fun getTopBarGradientStart(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueGradientStartDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamGradientStartDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintGradientStartDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceGradientStartDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineGradientStartDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasGradientStartDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueGradientStart
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamGradientStart
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintGradientStart
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceGradientStart
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineGradientStart
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasGradientStart
            }
        }
    }
    
    /**
     * 获取顶部渐变结束颜色（右侧）
     * 根据当前颜色方案和主题模式返回对应的渐变结束颜色
     * 如果与开始颜色相同，则无渐变效果
     */
    @Composable
    fun getTopBarGradientEnd(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueGradientEndDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamGradientEndDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintGradientEndDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceGradientEndDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineGradientEndDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasGradientEndDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueGradientEnd
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamGradientEnd
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintGradientEnd
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceGradientEnd
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineGradientEnd
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasGradientEnd
            }
        }
    }
    
    // ==================== 搜索框背景颜色 ====================
    
    /**
     * 获取搜索框背景颜色
     * 根据当前颜色方案和主题模式返回对应的搜索框背景颜色
     */
    @Composable
    fun getSearchBoxBgColor(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSearchBoxBgDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamSearchBoxBgDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintSearchBoxBgDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceSearchBoxBgDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineSearchBoxBgDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasSearchBoxBgDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSearchBoxBg
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamSearchBoxBg
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintSearchBoxBg
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceSearchBoxBg
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineSearchBoxBg
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasSearchBoxBg
            }
        }
    }
    
    // ==================== 搜索框边框颜色 ====================
    
    /**
     * 获取搜索框边框颜色
     * 根据当前颜色方案和主题模式返回对应的搜索框边框颜色
     */
    @Composable
    fun getSearchBoxBorderColor(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSearchBoxBorderDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamSearchBoxBorderDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintSearchBoxBorderDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceSearchBoxBorderDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineSearchBoxBorderDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasSearchBoxBorderDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSearchBoxBorder
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamSearchBoxBorder
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintSearchBoxBorder
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceSearchBoxBorder
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineSearchBoxBorder
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasSearchBoxBorder
            }
        }
    }
    
    // ==================== 面包屑导航颜色 ====================
    
    /**
     * 获取面包屑导航文字颜色
     * 根据当前颜色方案和主题模式返回对应的面包屑文字颜色
     */
    @Composable
    fun getBreadcrumbTextColor(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueBreadcrumbTextDark
                // 其他配色方案暂时使用相同的颜色，后续可以扩展
                else -> RedBlueBreadcrumbTextDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueBreadcrumbText
                // 其他配色方案暂时使用相同的颜色，后续可以扩展
                else -> RedBlueBreadcrumbText
            }
        }
    }
    
    /**
     * 获取面包屑导航图标颜色
     * 根据当前颜色方案和主题模式返回对应的面包屑图标颜色
     */
    @Composable
    fun getBreadcrumbIconColor(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueBreadcrumbIconDark
                // 其他配色方案暂时使用相同的颜色，后续可以扩展
                else -> RedBlueBreadcrumbIconDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueBreadcrumbIcon
                // 其他配色方案暂时使用相同的颜色，后续可以扩展
                else -> RedBlueBreadcrumbIcon
            }
        }
    }
    
    // ==================== 子容器名称颜色 ====================
    
    /**
     * 获取子容器名称颜色
     * 根据当前颜色方案和主题模式返回对应的子容器名称颜色
     */
    @Composable
    fun getSubWarehouseNameColor(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSubWarehouseNameDark
                // 其他配色方案暂时使用相同的颜色，后续可以扩展
                else -> RedBlueSubWarehouseNameDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSubWarehouseName
                // 其他配色方案暂时使用相同的颜色，后续可以扩展
                else -> RedBlueSubWarehouseName
            }
        }
    }
    
    // ==================== Surface Variant 背景颜色 ====================
    
    /**
     * 获取 Surface Variant 背景颜色
     * 根据当前颜色方案和主题模式返回对应的 Surface Variant 颜色
     */
    @Composable
    fun getSurfaceVariantColor(): Color {
        val appSettings = LocalAppSettings.current
        val schemeType = com.example.itemremindertool.ui.theme.ColorSchemeType.fromKey(appSettings.colorScheme)
        
        // 根据应用主题设置判断是否使用深色模式（与 Theme.kt 逻辑一致）
        val isDarkTheme = when (appSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme() // "system" 时跟随系统
        }
        
        return if (isDarkTheme) {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSurfaceVariantDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamSurfaceVariantDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintSurfaceVariantDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceSurfaceVariantDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineSurfaceVariantDark
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasSurfaceVariantDark
            }
        } else {
            when (schemeType) {
                com.example.itemremindertool.ui.theme.ColorSchemeType.RED_BLUE -> RedBlueSurfaceVariant
                com.example.itemremindertool.ui.theme.ColorSchemeType.CREAM -> CreamSurfaceVariant
                com.example.itemremindertool.ui.theme.ColorSchemeType.MINT -> MintSurfaceVariant
                com.example.itemremindertool.ui.theme.ColorSchemeType.SPACE -> SpaceSurfaceVariant
                com.example.itemremindertool.ui.theme.ColorSchemeType.WINE -> WineSurfaceVariant
                com.example.itemremindertool.ui.theme.ColorSchemeType.CHRISTMAS -> ChristmasSurfaceVariant
            }
        }
    }
}

