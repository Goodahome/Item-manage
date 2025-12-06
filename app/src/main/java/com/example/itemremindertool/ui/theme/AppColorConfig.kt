package com.example.itemremindertool.ui.theme

/**
 * 应用颜色配置指南
 * 
 * ⚠️ 重要提示：
 * 本文件仅作为配置指南使用，不包含实际的颜色定义代码。
 * 应用现已完全支持浅色/深色主题和多配色方案切换！
 * 
 * ✅ 已支持的功能：
 * - 浅色/深色主题切换
 * - 6 种预设配色方案（冷冽蓝、奶油橙、薄荷绿、深空灰、红酒红、圣诞）
 * - 跟随系统主题
 * - 所有 UI 元素（导航栏、页面背景、卡片、按钮、文字、图标、FAB）统一响应主题变化
 * 
 * =============================================================================
 * 🎨 如何自定义现有配色方案？
 * =============================================================================
 * 
 * 步骤 1：打开颜色定义文件
 * 文件路径：app/src/main/java/com/example/itemremindertool/ui/theme/Color.kt
 * 
 * 步骤 2：找到要修改的配色方案
 * Color.kt 中定义了 6 种配色方案，每种都有浅色和深色版本：
 * 
 * 1. 冷冽蓝（ColdBlue）
 *    - 浅色：ColdBluePrimary, ColdBlueBackground, ColdBlueSurface 等
 *    - 深色：ColdBluePrimaryDark, ColdBlueBackgroundDark, ColdBlueSurfaceDark 等
 * 
 * 2. 奶油橙（Cream）
 *    - 浅色：CreamPrimary, CreamBackground, CreamSurface 等
 *    - 深色：CreamPrimaryDark, CreamBackgroundDark, CreamSurfaceDark 等
 * 
 * 3. 薄荷绿（Mint）
 *    - 浅色：MintPrimary, MintBackground, MintSurface 等
 *    - 深色：MintPrimaryDark, MintBackgroundDark, MintSurfaceDark 等
 * 
 * 4. 深空灰（Space）
 *    - 浅色：SpacePrimary, SpaceBackground, SpaceSurface 等
 *    - 深色：SpacePrimaryDark, SpaceBackgroundDark, SpaceSurfaceDark 等
 * 
 * 5. 红酒红（Wine）
 *    - 浅色：WinePrimary, WineBackground, WineSurface 等
 *    - 深色：WinePrimaryDark, WineBackgroundDark, WineSurfaceDark 等
 * 
 * 6. 圣诞（Christmas）
 *    - 浅色：ChristmasPrimary, ChristmasBackground, ChristmasSurface 等
 *    - 深色：ChristmasPrimaryDark, ChristmasBackgroundDark, ChristmasSurfaceDark 等
 * 
 * 步骤 3：修改色号
 * 例如，要修改"冷冽蓝"配色方案的导航栏颜色：
 * 
 * // 浅色主题的导航栏
 * val ColdBlueSurface = Color(0xFFFFFFFF)  // 原始白色
 * 改为：
 * val ColdBlueSurface = Color(0xFFEB8B78)  // 您想要的橙色
 * 
 * // 深色主题的导航栏
 * val ColdBlueSurfaceDark = Color(0xFF1E293B)  // 原始深色
 * 改为：
 * val ColdBlueSurfaceDark = Color(0xFF2D2520)  // 您想要的深色
 * 
 * 步骤 4：保存并重新编译
 * 修改完成后，重新编译应用即可看到效果。
 * 
 * =============================================================================
 * 📋 UI 元素与 MaterialTheme 颜色映射关系
 * =============================================================================
 * 
 * 以下是 UI 元素如何映射到 MaterialTheme.colorScheme 的详细说明：
 * 
 * 第一组：导航栏、侧边菜单
 * ----------------------------
 * UI 元素：
 * - 顶部导航栏背景（TopAppBar）
 * - 侧边抽屉菜单背景（ModalDrawerSheet）
 * - 侧边菜单项背景（NavigationDrawerItem）
 * 
 * 映射到：MaterialTheme.colorScheme.surface
 * Color.kt 中的变量：
 * - 浅色：*Surface（如 ColdBlueSurface）
 * - 深色：*SurfaceDark（如 ColdBlueSurfaceDark）
 * 
 * 
 * 第二组：页面背景
 * ----------------------------
 * UI 元素：
 * - 所有页面的主背景色
 * - Scaffold 背景
 * 
 * 映射到：MaterialTheme.colorScheme.background
 * Color.kt 中的变量：
 * - 浅色：*Background（如 ColdBlueBackground）
 * - 深色：*BackgroundDark（如 ColdBlueBackgroundDark）
 * 
 * 
 * 第二组：按钮背景
 * ----------------------------
 * UI 元素：
 * - 设置页面按钮（Button、TextButton）
 * - FilterChip 选中状态
 * - RadioButton 选中状态
 * - Switch 选中状态
 * 
 * 映射到：MaterialTheme.colorScheme.primary
 * Color.kt 中的变量：
 * - 浅色：*Primary（如 ColdBluePrimary）
 * - 深色：*PrimaryDark（如 ColdBluePrimaryDark）
 * 
 * 
 * 第三组：卡片背景
 * ----------------------------
 * UI 元素：
 * - 物品卡片（ItemCard）
 * - 仓库卡片（WarehouseCard）
 * - 标签卡片（TagCard）
 * - 购物清单卡片（ShoppingItemCard）
 * - OutlinedTextField 输入框背景
 * - DatePicker、TimePicker 组件背景
 * 
 * 映射到：MaterialTheme.colorScheme.surfaceVariant
 * Color.kt 中的变量：
 * - 浅色：*SurfaceVariant（如 ColdBlueSurfaceVariant）
 * - 深色：*SurfaceVariantDark（如 ColdBlueSurfaceVariantDark）
 * 
 * 
 * 第四组：文字、图标
 * ----------------------------
 * UI 元素：
 * - 所有标题文字
 * - 所有正文文字
 * - 所有描述文字
 * - 按钮文字
 * - 所有图标（导航、操作、状态等）
 * 
 * 映射到：MaterialTheme.colorScheme.onSurface
 * Theme.kt 中定义，例如：
 * - 浅色：Color(0xFF1A1C1E)
 * - 深色：Color(0xFFE2E2E5)
 * 
 * 
 * 第五组：提醒卡片
 * ----------------------------
 * UI 元素：
 * - 过期物品提醒卡片
 * - 库存不足提醒卡片
 * - 重要提示卡片
 * - 过期标签（FilterChip）
 * 
 * 映射到：MaterialTheme.colorScheme.error
 * Color.kt 中的变量：
 * - 浅色：*Tertiary（如 ColdBlueTertiary）用作 error
 * - 深色：*TertiaryDark（如 ColdBlueTertiaryDark）用作 error
 * 
 * 
 * 第五组：悬浮操作按钮（FAB）
 * ----------------------------
 * UI 元素：
 * - FloatingActionButton（添加物品、仓库、标签等）
 * - Slider 激活状态滑块
 * - Switch 选中状态滑块
 * - 输入框聚焦边框
 * 
 * 映射到：MaterialTheme.colorScheme.primaryContainer
 * Color.kt 中的变量：
 * - 浅色：*PrimaryContainer（如 ColdBluePrimaryContainer）
 * - 深色：*PrimaryContainerDark（如 ColdBluePrimaryContainerDark）
 * 
 * =============================================================================
 * 💡 快速修改示例
 * =============================================================================
 * 
 * 示例 1：修改"冷冽蓝"配色方案的导航栏颜色
 * ----------------------------------------------
 * 文件：Color.kt
 * 
 * 找到：
 * val ColdBlueSurface = Color(0xFFFFFFFF)  // 浅色导航栏（白色）
 * val ColdBlueSurfaceDark = Color(0xFF1E293B)  // 深色导航栏（深蓝灰）
 * 
 * 修改为：
 * val ColdBlueSurface = Color(0xFFEB8B78)  // 浅色导航栏（橙色）
 * val ColdBlueSurfaceDark = Color(0xFF2D2520)  // 深色导航栏（深棕色）
 * 
 * 
 * 示例 2：修改"奶油橙"配色方案的卡片背景
 * ----------------------------------------------
 * 文件：Color.kt
 * 
 * 找到：
 * val CreamSurfaceVariant = Color(0xFFFFF0E0)  // 浅色卡片
 * val CreamSurfaceVariantDark = Color(0xFF3D3530)  // 深色卡片
 * 
 * 修改为您想要的颜色。
 * 
 * 
 * 示例 3：修改"薄荷绿"配色方案的按钮颜色
 * ----------------------------------------------
 * 文件：Color.kt
 * 
 * 找到：
 * val MintPrimary = Color(0xFF00C853)  // 浅色按钮
 * val MintPrimaryDark = Color(0xFF69F0AE)  // 深色按钮
 * 
 * 修改为您想要的颜色。
 * 
 * =============================================================================
 * 🆕 如何创建新的配色方案？
 * =============================================================================
 * 
 * 步骤 1：在 Color.kt 中定义新颜色
 * ----------------------------------------------
 * 添加浅色版本的颜色：
 * 
 * // ========== 7. 您的新配色方案 ==========
 * // 浅色主题
 * val YourSchemePrimary = Color(0xFF________)  // 主色（按钮）
 * val YourSchemeSecondary = Color(0xFF________)  // 次要色
 * val YourSchemeTertiary = Color(0xFF________)  // 第三色（提醒）
 * val YourSchemePrimaryContainer = Color(0xFF________)  // 主色容器（FAB）
 * val YourSchemeOnPrimaryContainer = Color(0xFF________)  // 主色容器上的文字
 * val YourSchemeBackground = Color(0xFF________)  // 页面背景
 * val YourSchemeSurface = Color(0xFF________)  // 导航栏背景
 * val YourSchemeSurfaceVariant = Color(0xFF________)  // 卡片背景
 * 
 * // 深色主题
 * val YourSchemePrimaryDark = Color(0xFF________)
 * val YourSchemeSecondaryDark = Color(0xFF________)
 * val YourSchemeTertiaryDark = Color(0xFF________)
 * val YourSchemePrimaryContainerDark = Color(0xFF________)
 * val YourSchemeBackgroundDark = Color(0xFF________)
 * val YourSchemeSurfaceDark = Color(0xFF________)
 * val YourSchemeSurfaceVariantDark = Color(0xFF________)
 * 
 * 
 * 步骤 2：在 Theme.kt 中创建 ColorScheme
 * ----------------------------------------------
 * 添加浅色和深色的 ColorScheme：
 * 
 * private val YourSchemeLightScheme = lightColorScheme(
 *     primary = YourSchemePrimary,
 *     onPrimary = Color.White,
 *     primaryContainer = YourSchemePrimaryContainer,
 *     onPrimaryContainer = YourSchemeOnPrimaryContainer,
 *     secondary = YourSchemeSecondary,
 *     onSecondary = Color.White,
 *     tertiary = YourSchemeTertiary,
 *     onTertiary = Color.White,
 *     error = YourSchemeTertiary,
 *     onError = Color.White,
 *     background = YourSchemeBackground,
 *     onBackground = Color(0xFF1A1C1E),
 *     surface = YourSchemeSurface,
 *     onSurface = Color(0xFF1A1C1E),
 *     surfaceVariant = YourSchemeSurfaceVariant,
 *     onSurfaceVariant = Color(0xFF42474E)
 * )
 * 
 * private val YourSchemeDarkScheme = darkColorScheme(
 *     primary = YourSchemePrimaryDark,
 *     onPrimary = Color.Black,
 *     primaryContainer = YourSchemePrimaryContainerDark,
 *     onPrimaryContainer = Color(0xFFD8E2FF),
 *     secondary = YourSchemeSecondaryDark,
 *     onSecondary = Color.Black,
 *     tertiary = YourSchemeTertiaryDark,
 *     onTertiary = Color.White,
 *     error = YourSchemeTertiaryDark,
 *     onError = Color.White,
 *     background = YourSchemeBackgroundDark,
 *     onBackground = Color(0xFFE2E2E5),
 *     surface = YourSchemeSurfaceDark,
 *     onSurface = Color(0xFFE2E2E5),
 *     surfaceVariant = YourSchemeSurfaceVariantDark,
 *     onSurfaceVariant = Color(0xFFC2C7CE)
 * )
 * 
 * 
 * 步骤 3：在 ColorSchemeType 枚举中添加新方案
 * ----------------------------------------------
 * 在 Theme.kt 的 ColorSchemeType 枚举中添加：
 * 
 * enum class ColorSchemeType(val key: String, val displayName: String) {
 *     COLD_BLUE("cold_blue", "冷冽囤老板"),
 *     CREAM("cream", "奶油治愈系"),
 *     MINT("mint", "薄荷冷感"),
 *     SPACE("space", "深空高级灰"),
 *     WINE("wine", "红酒沉稳"),
 *     CHRISTMAS("christmas", "节日限定·圣诞"),
 *     YOUR_SCHEME("your_scheme", "您的配色方案名称");  // 新增
 *     ...
 * }
 * 
 * 
 * 步骤 4：在 ItemReminderToolTheme 函数中添加分支
 * ----------------------------------------------
 * 在 Theme.kt 的 ItemReminderToolTheme 函数中添加：
 * 
 * if (shouldUseDarkTheme) {
 *     when (schemeType) {
 *         ...
 *         ColorSchemeType.YOUR_SCHEME -> YourSchemeDarkScheme  // 新增
 *     }
 * } else {
 *     when (schemeType) {
 *         ...
 *         ColorSchemeType.YOUR_SCHEME -> YourSchemeLightScheme  // 新增
 *     }
 * }
 * 
 * 
 * 步骤 5：在 strings.xml 中添加显示名称
 * ----------------------------------------------
 * 在 app/src/main/res/values/strings.xml 中添加：
 * 
 * <string name="color_scheme_your_scheme">您的配色方案名称</string>
 * 
 * 同时在 values-en/strings.xml 和 values-de/strings.xml 中添加相应翻译。
 * 
 * 
 * 步骤 6：在 AppearanceSettingsScreen.kt 中添加选项
 * ----------------------------------------------
 * 在配色方案选择对话框中添加新选项。
 * 
 * =============================================================================
 * 📚 相关文件路径
 * =============================================================================
 * 
 * 1. Color.kt
 *    路径：app/src/main/java/com/example/itemremindertool/ui/theme/Color.kt
 *    用途：定义所有配色方案的颜色值
 * 
 * 2. Theme.kt
 *    路径：app/src/main/java/com/example/itemremindertool/ui/theme/Theme.kt
 *    用途：组装 ColorScheme，控制主题切换逻辑
 * 
 * 3. ColorHelpers.kt
 *    路径：app/src/main/java/com/example/itemremindertool/ui/theme/ColorHelpers.kt
 *    用途：提供颜色辅助函数（通常不需要修改）
 * 
 * 4. AppearanceSettingsScreen.kt
 *    路径：app/src/main/java/com/example/itemremindertool/ui/screens/AppearanceSettingsScreen.kt
 *    用途：主题和配色方案设置页面
 * 
 * 5. strings.xml
 *    路径：app/src/main/res/values/strings.xml
 *    用途：字符串资源（配色方案显示名称）
 * 
 * =============================================================================
 * ⚠️ 注意事项
 * =============================================================================
 * 
 * 1. 确保浅色和深色版本的颜色有足够对比度
 *    - 浅色主题：文字应该深色，背景应该浅色
 *    - 深色主题：文字应该浅色，背景应该深色
 * 
 * 2. 使用在线工具检查对比度
 *    - WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/
 *    - WCAG 标准：正常文字至少 4.5:1，大号文字至少 3:1
 * 
 * 3. 保持颜色协调统一
 *    - 主色、次要色、第三色应该和谐搭配
 *    - 建议使用色轮工具辅助选色
 * 
 * 4. 测试所有页面
 *    - 修改完成后，在所有主要页面测试效果
 *    - 确保在浅色和深色主题下都清晰可见
 * 
 * =============================================================================
 * 
 * 祝您设计出完美的配色方案！🎨
 */
object AppColorConfig {
    // 此对象仅用作配置指南占位符
    // 实际颜色定义在 Color.kt 和 Theme.kt 中
}
