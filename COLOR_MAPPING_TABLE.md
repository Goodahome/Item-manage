# UI 组件颜色映射表

## 📋 快速查找表

此表格帮助您快速找到每个 UI 组件对应的配置变量。

---

## 🎨 第一组：导航与菜单（#EB8B78）

### `topAppBarBackground`
- ✅ 所有页面的顶部导航栏背景
  - DashboardScreen
  - ItemsScreen
  - WarehousesScreen
  - TagsScreen
  - ShoppingListScreen
  - SettingsScreen 及所有子页面
  - ItemEditScreen
  - ShoppingItemEditScreen

### `drawerBackground`
- ✅ 侧边导航菜单整体背景
  - ModalDrawerSheet 容器背景

### `drawerItemBackground`
- ✅ 侧边导航菜单项选中状态背景
  - NavigationDrawerItem 选中时的背景

---

## 🎨 第二组：页面与按钮（#D96A59）

### `pageBackground`
- ✅ 所有内容页面的主背景
  - DashboardScreen 背景
  - ItemsScreen 背景
  - WarehousesScreen 背景
  - TagsScreen 背景
  - ShoppingListScreen 背景
  - AllItemsScreen 背景
  - SettingsScreen 及所有子页面背景
  - ItemEditScreen 背景
  - ShoppingItemEditScreen 背景

### `settingsButtonBackground`
- ✅ 设置页面的按钮背景
  - TextButton 背景（设置页面）
  - Button 背景（确认、应用等）
  
- ✅ 交互组件的选中/激活状态
  - FilterChip 选中状态背景
  - RadioButton 选中状态
  - Switch 选中状态滑块
  - Slider 激活状态滑块
  
- ✅ 空状态页面的操作按钮
  - "添加第一个物品" 按钮
  - "添加第一个仓库" 按钮
  - "添加第一个标签" 按钮

---

## 🎨 第三组：卡片（#AEBDAE）

### `cardBackground`
- ✅ 所有列表卡片
  - ItemCard（物品卡片）
  - WarehouseCard（仓库卡片）
  - TagCard（标签卡片）
  - ShoppingItemCard（购物清单卡片）
  
- ✅ 所有输入框
  - OutlinedTextField 背景
  - 搜索框背景
  - 标签输入框背景
  
- ✅ 日期时间选择器
  - DatePicker 背景
  - TimePicker 背景
  
- ✅ 交互组件未选中状态
  - Switch 未选中状态滑轨
  - Slider 未激活状态滑轨

---

## 🎨 第四组：文字与图标（#79857B）

### `primaryText`
- ✅ 所有标题文字
  - TopAppBar 标题
  - Card 标题
  - Dialog 标题
  - ListItem 主标题
  
- ✅ 所有正文文字
  - 物品名称、描述
  - 仓库名称、描述
  - 标签名称
  - 设置项标题
  
- ✅ 按钮文字
  - Button 文字
  - TextButton 文字
  - FilterChip 文字
  
- ✅ 输入框文字
  - OutlinedTextField 输入的文字
  - 下拉菜单文字

### `secondaryText`（带透明度）
- ✅ 提示文字
  - OutlinedTextField 的 placeholder
  - OutlinedTextField 的 label
  
- ✅ 辅助说明文字
  - ListItem 的 supportingContent
  - 卡片的副标题
  - 提示性描述文字
  
- ✅ 禁用状态文字
  - 已完成的购物项文字（带删除线）

### `primaryIcon`
- ✅ 所有导航图标
  - TopAppBar 的返回按钮
  - TopAppBar 的操作按钮
  - 侧边菜单的图标
  
- ✅ 所有操作图标
  - 编辑图标
  - 删除图标
  - 添加图标
  - 更多操作图标（三个点）
  
- ✅ 交互组件图标
  - Checkbox
  - RadioButton
  - Switch
  - Slider
  - DatePicker 图标
  - TimePicker 图标

### `secondaryIcon`（带透明度）
- ✅ 未选中状态图标
  - RadioButton 未选中
  - Checkbox 未选中
  
- ✅ 装饰性图标
  - 空状态页面的大图标
  - ListItem 的 trailingContent 箭头

---

## 🎨 第五组：提醒与强调（#454D47）

### `alertCardBackground`
- ✅ 提醒卡片
  - 过期物品的高亮背景
  - 库存不足提醒卡片
  - 重要提示卡片
  
- ✅ 警告标签
  - "已过期" FilterChip 背景

### `fabBackground`
- ✅ 悬浮操作按钮
  - FloatingActionButton（添加物品）
  - FloatingActionButton（添加仓库）
  - FloatingActionButton（添加标签）
  - FloatingActionButton（添加购物项）
  
- ✅ 强调色用途
  - OutlinedTextField 聚焦状态边框
  - Slider 激活状态的滑块和轨道
  - Switch 选中状态的滑块
  - 搜索框聚焦状态边框
  - 标签输入框聚焦状态背景（低透明度）

---

## 🎯 特殊颜色

### `divider`
- ✅ 分隔线
  - Divider 组件
  - 列表项之间的分隔线

### `dialogBackground`
- ✅ 对话框背景
  - AlertDialog 背景
  - DatePickerDialog 背景
  - TimePickerDialog 背景
  - 自定义对话框背景

### `focusedBorder`
- ✅ 输入框聚焦边框
  - OutlinedTextField 获得焦点时的边框

### `unfocusedBorder`
- ✅ 输入框未聚焦边框
  - OutlinedTextField 未获得焦点时的边框

---

## 📱 各页面颜色分布总览

### DashboardScreen（仪表板）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 搜索框：`cardBackground` + `primaryText` + `focusedBorder`/`unfocusedBorder`
- 统计卡片：`cardBackground` + `primaryText` + `primaryIcon`
- 物品卡片：`cardBackground` + `primaryText` + `primaryIcon`

### ItemsScreen（物品列表）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 物品卡片：`cardBackground` + `primaryText` + `primaryIcon`
- 悬浮按钮：`fabBackground`
- 空状态按钮：`settingsButtonBackground` + `primaryText`

### ItemEditScreen（编辑物品）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 保存按钮：`settingsButtonBackground` + `primaryText`
- 输入框：`cardBackground` + `primaryText` + `focusedBorder`/`unfocusedBorder`
- 标签芯片：`settingsButtonBackground` + `primaryText`
- 过期标签：`alertCardBackground` + `primaryText`
- 开关组件：`fabBackground`（选中）+ `cardBackground`（未选中）

### WarehousesScreen（仓库管理）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 仓库卡片：`cardBackground` + `primaryText` + `primaryIcon`
- 悬浮按钮：`fabBackground`

### TagsScreen（标签管理）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 标签卡片：`cardBackground` + `primaryText` + `primaryIcon`
- 悬浮按钮：`fabBackground`
- 对话框：`dialogBackground` + `primaryText`
- 输入框：`cardBackground` + `primaryText`

### ShoppingListScreen（购物清单）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 购物项卡片：`cardBackground` + `primaryText` + `primaryIcon`
- 悬浮按钮：`fabBackground`
- 复选框：`fabBackground`（选中）

### SettingsScreen（设置页面）
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 列表项文字：`primaryText`
- 箭头图标：`primaryIcon`（带透明度）
- 分隔线：`divider`

### 所有设置子页面
- 导航栏：`topAppBarBackground` + `primaryText` + `primaryIcon`
- 页面背景：`pageBackground`
- 按钮：`settingsButtonBackground` + `primaryText`
- 开关：`fabBackground`（选中）+ `cardBackground`（未选中）
- 单选按钮：`fabBackground`（选中）+ `primaryIcon`（未选中）
- 对话框：`dialogBackground` + `primaryText`

---

## 💡 修改建议

### 场景 1：想要整体换个色调
修改所有 5 组颜色，保持对比度关系：
1. 第一组：品牌主色
2. 第二组：比第一组稍淡或稍深
3. 第三组：与第二组有明显区分的浅色
4. 第四组：确保在所有背景上清晰的深色或浅色
5. 第五组：醒目的强调色

### 场景 2：只想改导航栏颜色
只修改 `topAppBarBackground`

### 场景 3：只想改卡片背景
只修改 `cardBackground`

### 场景 4：文字不够清晰
调整 `primaryText` 的颜色，增加与背景的对比度

### 场景 5：按钮不够醒目
调整 `settingsButtonBackground` 和 `fabBackground` 为更醒目的颜色

---

## 🔍 如何找到要修改的颜色？

1. **确定要修改的 UI 组件**（例如：物品卡片背景）
2. **在上表中查找该组件**（找到 `cardBackground`）
3. **打开 `AppColorConfig.kt`**
4. **搜索该变量名**（搜索 `cardBackground`）
5. **修改色号**（修改 `Color(0xFFAEBDAE)` 为您想要的颜色）
6. **重新编译**（Build → Rebuild Project）

---

## ✅ 完成

现在您可以精确控制应用中每个 UI 组件的颜色了！

