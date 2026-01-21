# 盒记 6套配色方案完整参考

## 🎨 配色方案总览

| 方案名称 | 配置键 | 主色 | 适用场景 | 推荐指数 |
|---------|--------|------|----------|---------|
| 🔴🔵 红蓝配色 | `red_blue` | #0066FF | 工具应用、专业场景 | ⭐⭐⭐⭐⭐ |
| 🍊 奶油治愈系 | `cream` | #FFB84D | 家庭使用、温馨场景 | ⭐⭐⭐⭐ |
| 🌿 薄荷冷感 | `mint` | #00C853 | 清新风格、年轻用户 | ⭐⭐⭐⭐ |
| 🌌 深空高级灰 | `space` | #475569 | 高端用户、收藏管理 | ⭐⭐⭐⭐ |
| 🍷 红酒沉稳 | `wine` | #7C1C2C | 成熟用户、专业收藏 | ⭐⭐⭐ |
| 🎄 节日限定·圣诞 | `christmas` | #D32F2F | 节日活动、限时换肤 | ⭐⭐⭐ |

---

## 详细配色信息

### 1. 🔴🔵 红蓝配色（首推）

#### 浅色主题
```kotlin
val RedBlueLightScheme = lightColorScheme(
    primary = Color(0xFF0066FF),           // 鲜亮蓝
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),  // 浅蓝容器
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF0051C7),         // 深蓝
    onSecondary = Color.White,
    tertiary = Color(0xFFFF3B30),          // 到期红
    onTertiary = Color.White,
    error = Color(0xFFFF3B30),
    background = Color(0xFFF8F9FC),        // 浅灰蓝背景
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE0E7FF)     // 淡蓝变体
)
```

#### 深色主题
```kotlin
val RedBlueDarkScheme = darkColorScheme(
    primary = Color(0xFF4D94FF),           // 明亮蓝
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0040B2),
    tertiary = Color(0xFFFF6B6B),          // 柔和红
    background = Color(0xFF0B1120),        // 深蓝黑背景
    surface = Color(0xFF1E293B)
)
```

**使用场景**：
- ✅ 首页统计卡片
- ✅ 重要操作按钮
- ✅ 到期提醒高亮
- ✅ 专业工具界面

---

### 2. 🍊 奶油治愈系

#### 浅色主题
```kotlin
val CreamLightScheme = lightColorScheme(
    primary = Color(0xFFFFB84D),           // 奶油橙
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8D1),  // 浅橙容器
    onPrimaryContainer = Color(0xFF3D2100),
    secondary = Color(0xFFFF9A6A),         // 珊瑚橙
    onSecondary = Color.White,
    tertiary = Color(0xFFFF6B9A),          // 草莓粉
    onTertiary = Color.White,
    background = Color(0xFFFFFBF8),        // 暖白背景
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFF0E0)     // 浅奶油变体
)
```

#### 深色主题
```kotlin
val CreamDarkScheme = darkColorScheme(
    primary = Color(0xFFFFCC80),           // 浅橙
    onPrimary = Color(0xFF3D2100),
    primaryContainer = Color(0xFF8B5A00),
    tertiary = Color(0xFFFF8FB3),          // 浅粉
    background = Color(0xFF1F1B16),        // 暖黑背景
    surface = Color(0xFF2D2520)
)
```

**使用场景**：
- ✅ 家庭用户界面
- ✅ 母婴物品管理
- ✅ 温馨提示卡片
- ✅ 生活类应用

---

### 3. 🌿 薄荷冷感

#### 浅色主题
```kotlin
val MintLightScheme = lightColorScheme(
    primary = Color(0xFF00C853),           // 薄荷绿
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),  // 浅绿容器
    onPrimaryContainer = Color(0xFF003311),
    secondary = Color(0xFF00B140),         // 深绿
    onSecondary = Color.White,
    tertiary = Color(0xFF00E5FF),          // 冰蓝
    onTertiary = Color.Black,
    background = Color(0xFFF1FFFA),        // 清爽白背景
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD4F5E9)     // 淡绿变体
)
```

#### 深色主题
```kotlin
val MintDarkScheme = darkColorScheme(
    primary = Color(0xFF69F0AE),           // 亮绿
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF00842F),
    tertiary = Color(0xFF64F5FF),          // 亮冰蓝
    background = Color(0xFF0A1F16),        // 深绿黑背景
    surface = Color(0xFF1A2F26)
)
```

**使用场景**：
- ✅ 护肤品管理
- ✅ 清新风格界面
- ✅ 租房党用户
- ✅ 年轻用户群

---

### 4. 🌌 深空高级灰

#### 浅色主题
```kotlin
val SpaceLightScheme = lightColorScheme(
    primary = Color(0xFF475569),           // 深空灰
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),  // 浅灰容器
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF64748B),         // 中灰
    onSecondary = Color.White,
    tertiary = Color(0xFF8B5CF6),          // 紫色光
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),        // 极浅灰背景
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9)     // 灰变体
)
```

#### 深色主题
```kotlin
val SpaceDarkScheme = darkColorScheme(
    primary = Color(0xFF94A3B8),           // 亮灰
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFFA78BFA),          // 亮紫
    background = Color(0xFF0F172A),        // 深空黑背景
    surface = Color(0xFF1E293B)
)
```

**使用场景**：
- ✅ 收藏品管理
- ✅ 手办展示
- ✅ 男性用户偏好
- ✅ 高端产品界面

---

### 5. 🍷 红酒沉稳

#### 浅色主题
```kotlin
val WineLightScheme = lightColorScheme(
    primary = Color(0xFF7C1C2C),           // 酒红
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DE),  // 浅粉容器
    onPrimaryContainer = Color(0xFF3D0007),
    secondary = Color(0xFF991B33),         // 深红
    onSecondary = Color.White,
    tertiary = Color(0xFFD4A574),          // 金色
    onTertiary = Color.Black,
    background = Color(0xFFFFFBF8),        // 暖白背景
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFF3F0)     // 浅粉变体
)
```

#### 深色主题
```kotlin
val WineDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB3BA),           // 浅粉红
    onPrimary = Color(0xFF5C0A1A),
    primaryContainer = Color(0xFF5C0A1A),
    tertiary = Color(0xFFE5C4A0),          // 浅金
    background = Color(0xFF201416),        // 深红黑背景
    surface = Color(0xFF2D1F21)
)
```

**使用场景**：
- ✅ 红酒收藏管理
- ✅ 邮票管理
- ✅ 40+成熟用户
- ✅ 高端奢侈品管理

---

### 6. 🎄 节日限定·圣诞

#### 浅色主题
```kotlin
val ChristmasLightScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),           // 圣诞红
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),  // 浅红容器
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF388E3C),         // 圣诞绿
    onSecondary = Color.White,
    tertiary = Color(0xFFFFD700),          // 金色铃铛
    onTertiary = Color.Black,
    background = Color(0xFFFFFBF8),        // 暖白背景
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFF5F3)     // 浅红变体
)
```

#### 深色主题
```kotlin
val ChristmasDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),           // 浅红
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF9A1B1B),
    secondary = Color(0xFF81C784),         // 浅绿
    tertiary = Color(0xFFFFEB3B),          // 亮金
    background = Color(0xFF201A19),        // 深红黑背景
    surface = Color(0xFF2D2524)
)
```

**使用场景**：
- ✅ 12月圣诞节换肤
- ✅ 节日活动界面
- ✅ 特殊节日促销
- ✅ 限时主题活动

---

## 📊 颜色使用映射表

| 用途 | 推荐使用 | 示例场景 |
|-----|---------|---------|
| 主要操作按钮 | `primary` | 添加物品、确认按钮 |
| 次要操作按钮 | `secondary` | 取消、返回按钮 |
| 到期提醒 | `tertiary` | 物品即将过期提示 |
| 错误提示 | `error` | 库存预警、操作失败 |
| 背景色 | `background` | 页面整体背景 |
| 卡片背景 | `surface` | 白色卡片 |
| 容器卡片 | `surfaceVariant` | 略深一级的卡片 |
| 重要信息 | `primaryContainer` | 首页提醒卡片 |
| 柔和提醒 | `secondaryContainer` | 低库存通知 |
| 强调信息 | `tertiaryContainer` | 到期倒计时 |

---

## 🚀 快速切换配色

```kotlin
// 在 MainActivity 或设置页面
val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

// 切换到奶油治愈系
prefs.edit().putString("color_scheme", "cream").apply()

// 切换到薄荷冷感
prefs.edit().putString("color_scheme", "mint").apply()

// 切换到节日圣诞
prefs.edit().putString("color_scheme", "christmas").apply()
```

---

## 💡 配色搭配建议

### 红蓝配色（推荐首发）
- **优势**：专业、可靠、符合工具应用定位
- **搭配**：白色icon + 深色文字
- **典型用户**：效率达人、工程师、数据控

### 奶油治愈系（推荐家庭版）
- **优势**：温暖、亲和、适合家庭场景
- **搭配**：圆角卡片 + 柔和阴影
- **典型用户**：宝妈、主妇、家庭管理者

### 薄荷冷感（推荐年轻版）
- **优势**：清爽、活力、现代感强
- **搭配**：简洁icon + 大留白
- **典型用户**：租房党、学生、年轻白领

### 深空高级灰（推荐收藏版）
- **优势**：高级、低调、科技感
- **搭配**：扁平风格 + 紫色点缀
- **典型用户**：收藏家、极客、男性用户

### 红酒沉稳（推荐专业版）
- **优势**：成熟、优雅、高端
- **搭配**：衬线字体 + 金色装饰
- **典型用户**：资深用户、专业收藏家

### 节日限定·圣诞（推荐节日版）
- **优势**：喜庆、有趣、话题性强
- **搭配**：节日icon + 动画效果
- **典型用户**：全体用户（限时）

---

## 📈 推荐使用策略

1. **默认配色**：首推"红蓝配色"（`red_blue`）
   - 专业可靠，适合大多数用户
   - 红蓝风格符合工具类应用定位

2. **用户画像匹配**：
   - 25岁以下 → 薄荷冷感
   - 25-40岁女性 → 奶油治愈系
   - 25-40岁男性 → 红蓝配色或深空高级灰
   - 40岁以上 → 红酒沉稳

3. **节日营销**：
   - 12月自动切换圣诞主题
   - 增加节日氛围和用户粘性

4. **用户自定义**：
   - 在设置页面提供配色选择器
   - 让用户根据个人喜好切换

---

## 🎯 下一步

1. ✅ **已完成**：所有配色代码已添加到 `Color.kt` 和 `Theme.kt`
2. ⬜ **待实现**：在设置页面添加配色选择器
3. ⬜ **可选**：添加配色预览功能
4. ⬜ **可选**：节日自动切换配色功能

---

**祝开发顺利！🚀**

