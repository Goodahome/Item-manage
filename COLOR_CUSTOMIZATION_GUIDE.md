# 颜色自定义指南

## 📝 简介

所有应用颜色现在统一在一个文件中管理，您可以轻松修改任意 UI 组件的颜色。

## 🎨 如何修改颜色

### 步骤 1：打开配置文件

打开文件：`app/src/main/java/com/example/itemremindertool/ui/theme/AppColorConfig.kt`

### 步骤 2：找到要修改的颜色

该文件包含以下 5 个主要分组：

#### 第一组：导航与菜单
- `topAppBarBackground` - 顶部导航栏背景
- `drawerBackground` - 侧边菜单背景
- `drawerItemBackground` - 侧边菜单项背景

#### 第二组：页面与按钮
- `pageBackground` - 页面背景色
- `settingsButtonBackground` - 按钮背景色

#### 第三组：卡片
- `cardBackground` - 所有卡片背景色

#### 第四组：文字与图标
- `primaryText` - 主要文字颜色
- `secondaryText` - 次要文字颜色
- `primaryIcon` - 主要图标颜色
- `secondaryIcon` - 次要图标颜色

#### 第五组：提醒与强调
- `alertCardBackground` - 提醒卡片背景
- `fabBackground` - 悬浮按钮背景

### 步骤 3：修改颜色代码

找到要修改的颜色变量，修改等号后面的色号：

```kotlin
// 修改前
val topAppBarBackground = Color(0xFFEB8B78)

// 修改后（改为蓝色）
val topAppBarBackground = Color(0xFF1E88E5)
```

### 步骤 4：重新编译

修改完成后，重新编译应用即可看到效果。

## 🎨 颜色格式说明

颜色格式：`Color(0xFFRRGGBB)`

- `0xFF` - 不透明度（FF=完全不透明，00=完全透明）
- `RR` - 红色分量（00-FF，即 0-255）
- `GG` - 绿色分量（00-FF，即 0-255）
- `BB` - 蓝色分量（00-FF，即 0-255）

### 示例

- 红色：`Color(0xFFE53935)`
- 橙色：`Color(0xFFFF9800)`
- 黄色：`Color(0xFFFFC107)`
- 绿色：`Color(0xFF4CAF50)`
- 蓝色：`Color(0xFF2196F3)`
- 紫色：`Color(0xFF9C27B0)`
- 灰色：`Color(0xFF757575)`
- 黑色：`Color(0xFF000000)`
- 白色：`Color(0xFFFFFFFF)`

## 🔍 每个颜色应用在哪里？

每个颜色变量都有详细的注释说明其应用位置，例如：

```kotlin
/**
 * 顶部导航栏背景色
 * 应用位置：所有页面的顶部导航栏
 */
val topAppBarBackground = Color(0xFFEB8B78)
```

## 💡 配色建议

### 1. 对比度
确保文字颜色与背景色有足够对比度，以保证可读性。

**建议：**
- 浅色背景 → 深色文字
- 深色背景 → 浅色文字

### 2. 协调性
相邻颜色组应该有明显区分，但整体保持协调。

**建议：**
- 第一组（导航）：可以使用品牌主色
- 第二组（页面背景）：比导航色稍浅或稍深
- 第三组（卡片）：与页面背景有明显区分
- 第四组（文字/图标）：确保在所有背景上都清晰可见
- 第五组（强调色）：使用醒目的颜色

### 3. 修改顺序建议

1. 先确定品牌主色（通常用于导航栏）
2. 选择合适的页面背景色
3. 选择与背景对比明显的卡片色
4. 确定文字和图标颜色（确保在所有背景上都清晰）
5. 最后选择强调色（用于重要提示和操作按钮）

## 🎯 当前配色方案

当前默认配色：

| 组件 | 色号 | 颜色预览 |
|------|------|---------|
| 导航栏 | #EB8B78 | 🟠 橙色系 |
| 页面背景 | #D96A59 | 🟠 橙红色系 |
| 卡片背景 | #AEBDAE | 🟢 浅绿色系 |
| 文字/图标 | #79857B | ⚫ 深灰色系 |
| 强调色 | #454D47 | ⚫ 深墨色系 |

## 🛠️ 常见问题

### Q: 修改颜色后没有生效？
A: 确保已重新编译应用。在 Android Studio 中点击 Build → Rebuild Project。

### Q: 如何让文字颜色在不同背景上都清晰？
A: 使用中性色（灰色、深灰色、黑色、白色）作为文字颜色通常效果最好。

### Q: 如何选择协调的配色方案？
A: 可以使用在线工具如 [Coolors.co](https://coolors.co/)、[Adobe Color](https://color.adobe.com/) 生成配色方案。

### Q: 透明度是如何控制的？
A: 带透明度的颜色（如次要文字、图标）会在代码中自动添加透明度，您只需修改基础色即可。

## 📚 相关文件

- **颜色配置文件**：`app/src/main/java/com/example/itemremindertool/ui/theme/AppColorConfig.kt`
- **颜色辅助函数**：`app/src/main/java/com/example/itemremindertool/ui/theme/ColorHelpers.kt`（不需要修改此文件）

## ✅ 完成

现在您可以轻松自定义应用的所有颜色了！只需修改 `AppColorConfig.kt` 中的色号，重新编译即可。

