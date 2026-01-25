package com.example.itemremindertool.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 浅色模式通用 ====================

// 文字/图标颜色（对比度低于0.5时使用）- 通用颜色，所有配色方案共用
val OnSurfaceLowContrast      = Color(0xFF000000)   // 黑色文字，用于浅色背景
val OnSurfaceVariantLowContrast = Color(0xFF000000)   // 黑色文字，用于次要文字

// 文字/图标颜色（对比度高于0.5时使用）- 通用颜色，所有配色方案共用
val OnSurfaceHighContrast     = Color(0xFFFFFFFF)   // 白色文字，用于深色背景
val OnSurfaceVariantHighContrast = Color(0xFFFFFFFF)   // 白色文字，用于次要文字   

// ==================== 深色模式通用 ====================

// 文字/图标颜色（对比度低于0.5时使用）- 通用颜色，所有配色方案共用
val OnSurfaceDarkLowContrast      = Color(0xFF000000)   // 黑色文字，用于浅色背景（深色主题下）
val OnSurfaceVariantDarkLowContrast = Color(0xFF000000)   // 黑色文字，用于次要文字

// 文字/图标颜色（对比度高于0.5时使用）- 通用颜色，所有配色方案共用
val OnSurfaceDarkHighContrast     = Color(0xFFFFFFFF)   // 白色文字，用于深色背景（深色主题下）
val OnSurfaceVariantDarkHighContrast = Color(0xFFFFFFFF)   // 白色文字，用于次要文字



// ========== 1. 红蓝配色（首推）==========
// 浅色主题
val RedBluePrimary = Color(0xFFBA3801)  // 主色调
val RedBlueTertiary = Color(0xFFFF3B30) // 强调色
val RedBluePrimaryContainer = Color(0xFFBA3801) // 悬浮按钮色
val RedBlueOnPrimaryContainer = Color(0xFF001A41) // 程序文字色
val RedBlueBackground = Color(0xFFFFFFFF)
val RedBlueSurface = Color(0xFFFFFFFF) // 在 Theme.kt 中使用：surface
val RedBlueSurfaceVariant = Color(0xFFFFFFFF)

// 子容器名称颜色（浅色主题）
val RedBlueSubWarehouseName = Color(0xFF6B4F0A) // 子容器名称颜色

// 顶部渐变颜色（浅色主题）
val RedBlueGradientStart = Color(0xFFBA3801) // 渐变开始颜色（左侧）
val RedBlueGradientEnd = Color(0xFFBA3801) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（浅色主题）
val RedBlueSearchBoxBg = Color(0xFF4A69B3) // 搜索框背景色

// 搜索框边框颜色（浅色主题）
val RedBlueSearchBoxBorder = Color(0xFF4A69B3) // 搜索框边框颜色

// 搜索框文字颜色（浅色主题）
val RedBlueSearchBoxText = Color(0xFFFFFFFF) // 搜索框文字颜色


// ==================== 深色模式 - 温暖砖橙主题 ====================

// 主要强调色（按钮、链接、选中态） - 从浅色 #BA3801 衍生，提亮+降饱和
val RedBluePrimaryDark     = Color(0xFFFF8C5E)   // 明亮温暖橙，视觉焦点

// 次要色（辅助按钮、chip等）

// 错误/警告/tertiary（保留原有的红色系，但稍微柔化）
val RedBlueTertiaryDark       = Color(0xFFFF7A70)   // 柔和珊瑚红

// Primary Container（卡片、填充区域）
val RedBluePrimaryContainerDark = Color(0xFF5C2A0F)  // 深暖棕橙

// On Primary Container（在上面的文字）
val RedBlueOnPrimaryContainerDark    = Color(0xFFFFD8C2)   // 暖米白

// 背景（最底层）
val RedBlueBackgroundDark        = Color(0xFF16100A)   // 非常深的暖黑棕

// Surface（卡片、对话框、底部导航等主要层）
val RedBlueSurfaceDark           = Color(0xFF1E150F)   // 比背景略亮一点的暖深灰

// Surface Variant（次级区分，如列表分隔、输入框底色）
val RedBlueSurfaceVariantDark    = Color(0xFF32251C)   // 更明显的暖棕灰

// 面包屑导航文字/图标颜色（深色主题）
val RedBlueBreadcrumbTextDark = Color(0xFFFFD8A3) // 面包屑文字颜色
val RedBlueBreadcrumbIconDark = Color(0xFFFFD8A3) // 面包屑图标颜色

// 子容器名称颜色（深色主题）
val RedBlueSubWarehouseNameDark = Color(0xFFFFE5B8) // 子容器名称颜色

// 顶部渐变（建议从亮到暗，保持温暖过渡）
val RedBlueGradientStartDark     = Color(0xFF2A1A10)   // 与 Primary 一致
val RedBlueGradientEndDark       = Color(0xFF2A1A10)   // 深暖棕

// 搜索框背景（深色模式下建议半透或较暗的暖色）
val RedBlueSearchBoxBgDark       = Color(0xFF2A1F17)   // 深暖灰棕

// 搜索框边框（可以更亮一点突出）
val RedBlueSearchBoxBorderDark   = Color(0xFFFFA270)   // 浅暖橙，微发光感

// 搜索框文字颜色（深色主题）
val RedBlueSearchBoxTextDark     = Color(0xFFFFFFFF)   // 搜索框文字颜色



// ========== 2. 奶油治愈系 ==========
// 浅色主题
val CreamPrimary = Color(0xFFFFB84D)
val CreamTertiary = Color(0xFFFF6B9A)
val CreamPrimaryContainer = Color(0xFFFFB84D)
val CreamOnPrimaryContainer = Color(0xFF3D2100)
val CreamBackground = Color(0xFFFFFBF8)
val CreamSurface = Color(0xFFFFFFFF)
val CreamSurfaceVariant = Color(0xFFFFF0E0)

// 顶部渐变颜色（浅色主题）
val CreamGradientStart = Color(0xFFFFB84D) // 渐变开始颜色（左侧）
val CreamGradientEnd = Color(0xFFFFF0E0) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（浅色主题）
val CreamSearchBoxBg = Color(0xFFFFFFFF) // 搜索框背景色

// 搜索框边框颜色（浅色主题）
val CreamSearchBoxBorder = Color(0xFF000000) // 搜索框边框颜色

// 搜索框文字颜色（浅色主题）
val CreamSearchBoxText = Color(0xFF000000) // 搜索框文字颜色

// 深色主题
val CreamPrimaryDark = Color(0xFFFFCC80)
val CreamTertiaryDark = Color(0xFFFF8FB3)
val CreamPrimaryContainerDark = Color(0xFF8B5A00)
val CreamBackgroundDark = Color(0xFF1F1B16)
val CreamSurfaceDark = Color(0xFF2D2520)
val CreamSurfaceVariantDark = Color(0xFF3D3530)

// 顶部渐变颜色（深色主题）
val CreamGradientStartDark = Color(0xFFFFCC80) // 渐变开始颜色（左侧）
val CreamGradientEndDark = Color(0xFF2D2520) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（深色主题）
val CreamSearchBoxBgDark = Color(0xFF2D2520) // 搜索框背景色

// 搜索框边框颜色（深色主题）
val CreamSearchBoxBorderDark = Color(0xFFFFFFFF) // 搜索框边框颜色

// 搜索框文字颜色（深色主题）
val CreamSearchBoxTextDark = Color(0xFFFFFFFF) // 搜索框文字颜色

// ========== 3. 薄荷冷感 ==========
// 浅色主题
val MintPrimary = Color(0xFF00C853)
val MintTertiary = Color(0xFF00E5FF)
val MintPrimaryContainer = Color(0xFF00C853)
val MintOnPrimaryContainer = Color(0xFF003311)
val MintBackground = Color(0xFFF1FFFA)
val MintSurface = Color(0xFFFFFFFF)
val MintSurfaceVariant = Color(0xFFD4F5E9)

// 顶部渐变颜色（浅色主题）
val MintGradientStart = Color(0xFF00C853) // 渐变开始颜色（左侧）
val MintGradientEnd = Color(0xFFD4F5E9) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（浅色主题）
val MintSearchBoxBg = Color(0xFFFFFFFF) // 搜索框背景色

// 搜索框边框颜色（浅色主题）
val MintSearchBoxBorder = Color(0xFF000000) // 搜索框边框颜色

// 搜索框文字颜色（浅色主题）
val MintSearchBoxText = Color(0xFF000000) // 搜索框文字颜色

// 深色主题
val MintPrimaryDark = Color(0xFF69F0AE)
val MintTertiaryDark = Color(0xFF64F5FF)
val MintPrimaryContainerDark = Color(0xFF00842F)
val MintBackgroundDark = Color(0xFF0A1F16)
val MintSurfaceDark = Color(0xFF1A2F26)
val MintSurfaceVariantDark = Color(0xFF253F36)

// 顶部渐变颜色（深色主题）
val MintGradientStartDark = Color(0xFF69F0AE) // 渐变开始颜色（左侧）
val MintGradientEndDark = Color(0xFF1A2F26) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（深色主题）
val MintSearchBoxBgDark = Color(0xFF1A2F26) // 搜索框背景色

// 搜索框边框颜色（深色主题）
val MintSearchBoxBorderDark = Color(0xFFFFFFFF) // 搜索框边框颜色

// 搜索框文字颜色（深色主题）
val MintSearchBoxTextDark = Color(0xFFFFFFFF) // 搜索框文字颜色

// ========== 4. 深空高级灰 ==========
// 浅色主题
val SpacePrimary = Color(0xFF475569)
val SpaceTertiary = Color(0xFF8B5CF6)
val SpacePrimaryContainer = Color(0xFF475569)
val SpaceOnPrimaryContainer = Color(0xFF0F172A)
val SpaceBackground = Color(0xFFF8FAFC)
val SpaceSurface = Color(0xFFFFFFFF)
val SpaceSurfaceVariant = Color(0xFFF1F5F9)

// 顶部渐变颜色（浅色主题）
val SpaceGradientStart = Color(0xFF475569) // 渐变开始颜色（左侧）
val SpaceGradientEnd = Color(0xFFF1F5F9) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（浅色主题）
val SpaceSearchBoxBg = Color(0xFFFFFFFF) // 搜索框背景色

// 搜索框边框颜色（浅色主题）
val SpaceSearchBoxBorder = Color(0xFF000000) // 搜索框边框颜色

// 搜索框文字颜色（浅色主题）
val SpaceSearchBoxText = Color(0xFF000000) // 搜索框文字颜色

// 深色主题
val SpacePrimaryDark = Color(0xFF94A3B8)
val SpaceTertiaryDark = Color(0xFFA78BFA)
val SpacePrimaryContainerDark = Color(0xFF1E293B)
val SpaceBackgroundDark = Color(0xFF0F172A)
val SpaceSurfaceDark = Color(0xFF1E293B)
val SpaceSurfaceVariantDark = Color(0xFF334155)

// 顶部渐变颜色（深色主题）
val SpaceGradientStartDark = Color(0xFF94A3B8) // 渐变开始颜色（左侧）
val SpaceGradientEndDark = Color(0xFF1E293B) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（深色主题）
val SpaceSearchBoxBgDark = Color(0xFF1E293B) // 搜索框背景色

// 搜索框边框颜色（深色主题）
val SpaceSearchBoxBorderDark = Color(0xFFFFFFFF) // 搜索框边框颜色

// 搜索框文字颜色（深色主题）
val SpaceSearchBoxTextDark = Color(0xFFFFFFFF) // 搜索框文字颜色

// ========== 5. 红酒沉稳 ==========
// 浅色主题
val WinePrimary = Color(0xFF7C1C2C)
val WineTertiary = Color(0xFFD4A574)
val WinePrimaryContainer = Color(0xFF7C1C2C)
val WineOnPrimaryContainer = Color(0xFF3D0007)
val WineBackground = Color(0xFFFFFBF8)
val WineSurface = Color(0xFFFFFFFF)
val WineSurfaceVariant = Color(0xFFFFF3F0)

// 顶部渐变颜色（浅色主题）
val WineGradientStart = Color(0xFF7C1C2C) // 渐变开始颜色（左侧）
val WineGradientEnd = Color(0xFFFFF3F0) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（浅色主题）
val WineSearchBoxBg = Color(0xFFFFFFFF) // 搜索框背景色

// 搜索框边框颜色（浅色主题）
val WineSearchBoxBorder = Color(0xFF000000) // 搜索框边框颜色

// 搜索框文字颜色（浅色主题）
val WineSearchBoxText = Color(0xFF000000) // 搜索框文字颜色

// 深色主题
val WinePrimaryDark = Color(0xFFFFB3BA)
val WineTertiaryDark = Color(0xFFE5C4A0)
val WinePrimaryContainerDark = Color(0xFF5C0A1A)
val WineBackgroundDark = Color(0xFF201416)
val WineSurfaceDark = Color(0xFF2D1F21)
val WineSurfaceVariantDark = Color(0xFF3D2A2C)

// 顶部渐变颜色（深色主题）
val WineGradientStartDark = Color(0xFFFFB3BA) // 渐变开始颜色（左侧）
val WineGradientEndDark = Color(0xFF2D1F21) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（深色主题）
val WineSearchBoxBgDark = Color(0xFF2D1F21) // 搜索框背景色

// 搜索框边框颜色（深色主题）
val WineSearchBoxBorderDark = Color(0xFFFFFFFF) // 搜索框边框颜色

// 搜索框文字颜色（深色主题）
val WineSearchBoxTextDark = Color(0xFFFFFFFF) // 搜索框文字颜色

// ========== 6. 节日限定·圣诞 ==========
// 浅色主题
val ChristmasPrimary = Color(0xFFD32F2F)
val ChristmasTertiary = Color(0xFFFFD700)
val ChristmasPrimaryContainer = Color(0xFFD32F2F)
val ChristmasOnPrimaryContainer = Color(0xFF410002)
val ChristmasBackground = Color(0xFFFFFBF8)
val ChristmasSurface = Color(0xFFFFFFFF)
val ChristmasSurfaceVariant = Color(0xFFFFF5F3)

// 顶部渐变颜色（浅色主题）
val ChristmasGradientStart = Color(0xFFD32F2F) // 渐变开始颜色（左侧）
val ChristmasGradientEnd = Color(0xFFFFF5F3) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（浅色主题）
val ChristmasSearchBoxBg = Color(0xFFFFFFFF) // 搜索框背景色

// 搜索框边框颜色（浅色主题）
val ChristmasSearchBoxBorder = Color(0xFFD32F2F) // 搜索框边框颜色

// 搜索框文字颜色（浅色主题）
val ChristmasSearchBoxText = Color(0xFF000000) // 搜索框文字颜色

// 深色主题
val ChristmasPrimaryDark = Color(0xFFFFB4AB)
val ChristmasTertiaryDark = Color(0xFFFFEB3B)
val ChristmasPrimaryContainerDark = Color(0xFF9A1B1B)
val ChristmasBackgroundDark = Color(0xFF201A19)
val ChristmasSurfaceDark = Color(0xFF2D2524)
val ChristmasSurfaceVariantDark = Color(0xFF3D3230)

// 顶部渐变颜色（深色主题）
val ChristmasGradientStartDark = Color(0xFFFFB4AB) // 渐变开始颜色（左侧）
val ChristmasGradientEndDark = Color(0xFF2D2524) // 渐变结束颜色（右侧），如果与开始颜色相同则无渐变

// 搜索框背景颜色（深色主题）
val ChristmasSearchBoxBgDark = Color(0xFF2D2524) // 搜索框背景色

// 搜索框边框颜色（深色主题）
val ChristmasSearchBoxBorderDark = Color(0xFFFFFFFF) // 搜索框边框颜色

// 搜索框文字颜色（深色主题）
val ChristmasSearchBoxTextDark = Color(0xFFFFFFFF) // 搜索框文字颜色