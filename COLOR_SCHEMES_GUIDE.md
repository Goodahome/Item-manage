# 盒记配色方案使用指南

## 📌 6套精选配色方案

已为「盒记」量身定制6套完整配色方案，全部基于 **Material You 动态取色规范**，支持 Android 12+ 自动深浅色切换。

### 1. 🔴🔵 红蓝配色（首推）
- **气质**：冷静、掌控感、靠谱
- **推荐用户群**：囤货狂魔、工程师、极简党
- **主色**：鲜亮蓝 (#0066FF)
- **配置键**：`red_blue`

### 2. 🍊 奶油治愈系
- **气质**：温暖、柔软、生活感
- **推荐用户群**：母婴家庭、主妇、95后
- **主色**：奶油橙 (#FFB84D)
- **配置键**：`cream`

### 3. 🌿 薄荷冷感
- **气质**：清爽、干净、轻盈
- **推荐用户群**：租房党、护肤品管理
- **主色**：薄荷绿 (#00C853)
- **配置键**：`mint`

### 4. 🌌 深空高级灰
- **气质**：低调、奢华、科技感
- **推荐用户群**：收藏家、手办党、男性用户
- **主色**：深空灰 (#475569)
- **配置键**：`space`

### 5. 🍷 红酒沉稳
- **气质**：成熟、可靠、老钱感
- **推荐用户群**：40+用户、红酒/邮票收藏
- **主色**：酒红 (#7C1C2C)
- **配置键**：`wine`

### 6. 🎄 节日限定·圣诞
- **气质**：喜庆、节日氛围
- **推荐用途**：每年12月换皮
- **主色**：圣诞红 (#D32F2F)
- **配置键**：`christmas`

---

## 🎨 如何切换配色方案

### 方法一：通过代码设置默认配色

在 `SharedPreferences` 中设置：

```kotlin
val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
prefs.edit().putString("color_scheme", "red_blue").apply()
```

### 方法二：在设置界面添加配色选择器

```kotlin
// 在 SettingsScreen 中添加配色选择
val colorSchemes = remember {
    listOf(
        ColorSchemeType.RED_BLUE,
        ColorSchemeType.CREAM,
        ColorSchemeType.MINT,
        ColorSchemeType.SPACE,
        ColorSchemeType.WINE,
        ColorSchemeType.CHRISTMAS
    )
}

LazyColumn {
    items(colorSchemes) { scheme ->
        ListItem(
            headlineContent = { Text(scheme.displayName) },
            trailingContent = {
                RadioButton(
                    selected = currentScheme == scheme.key,
                    onClick = {
                        prefs.edit()
                            .putString("color_scheme", scheme.key)
                            .apply()
                    }
                )
            },
            modifier = Modifier.clickable {
                prefs.edit()
                    .putString("color_scheme", scheme.key)
                    .apply()
            }
        )
    }
}
```

---

## 💡 配色使用建议（提升逼格小技巧）

### 1. 到期提醒
```kotlin
// 统一使用 tertiary（每套都配了高对比强调色）
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
    )
) {
    Text(
        text = "即将到期",
        color = MaterialTheme.colorScheme.tertiary
    )
}
```

### 2. 库存预警
```kotlin
// 使用 error（默认红）
Icon(
    imageVector = Icons.Default.Warning,
    tint = MaterialTheme.colorScheme.error
)
```

### 3. 低库存提醒
```kotlin
// 使用 secondaryContainer（柔和提醒）
Surface(
    color = MaterialTheme.colorScheme.secondaryContainer
) {
    Text(
        text = "库存不足",
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
}
```

### 4. 首页提醒卡片
```kotlin
// primaryContainer + 80% 透明度
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    )
) {
    // 内容
}
```

### 5. 容器卡片
```kotlin
// surfaceVariant（比背景略深一级，层次感拉满）
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    // 容器内容
}
```

---

## 🚀 快速开始

### 步骤1：已完成！
所有配色代码已添加到：
- `Color.kt` - 所有颜色定义
- `Theme.kt` - 6套完整的 ColorScheme

### 步骤2：设置默认配色
在 `MainActivity.onCreate()` 中添加：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 首次启动时设置默认配色为"红蓝配色"
    val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    if (!prefs.contains("color_scheme")) {
        prefs.edit()
            .putString("color_scheme", "red_blue")
            .apply()
    }
    
    setContent {
        ItemReminderToolTheme {
            // ...
        }
    }
}
```

### 步骤3：（可选）添加配色切换功能
在设置页面添加配色选择器，让用户自由切换。

---

## 🎯 配色特点

### Material You 规范
- ✅ 完整的浅色/深色主题
- ✅ 自动适配系统深浅色设置
- ✅ 所有颜色都有对应的 `on` 颜色（确保文字可读性）
- ✅ 支持容器颜色变体（增强层次感）

### 动态取色支持
如果用户设备支持 Android 12+ 动态取色，可以在 `ItemReminderToolTheme` 中设置 `dynamicColor = true` 来启用壁纸取色。

---

## 📱 预览效果

建议在以下场景测试配色效果：
1. 首页卡片和统计数据
2. 容器列表和详情
3. 物品卡片和到期提醒
4. 购物清单
5. 设置页面

---

## 🔧 自定义配色

如果需要新增配色方案，请按以下步骤操作：

1. 在 `Color.kt` 中定义新颜色
2. 在 `Theme.kt` 中创建新的 `lightColorScheme` 和 `darkColorScheme`
3. 在 `ColorSchemeType` 枚举中添加新选项
4. 在 `ItemReminderToolTheme` 的 `when` 分支中添加新方案

---

## 📞 技术支持

如有问题或需要调整配色，请参考：
- Material Design 3 配色指南：https://m3.material.io/styles/color
- Material Theme Builder：https://material-foundation.github.io/material-theme-builder/

---

**享受你的新配色方案吧！🎨**

