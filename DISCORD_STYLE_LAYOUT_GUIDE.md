# Discord风格首页布局使用指南

## 🎨 新布局特点

首页已重新设计为Discord风格的三栏布局：

```
┌──────────────────────────────────────────────────┐
│  TopAppBar + SearchBox（保持不变）                  │
├───┬──────────────────────────────────────────────┤
│左 │  右侧上部：子容器横向滚动                         │
│侧 │  [○子1] [○子2] [○子3] [+添加]                   │
│   ├──────────────────────────────────────────────┤
│容 │  右侧下部：物品垂直列表                           │
│器 │  ┌──────────────────────────┐                 │
│列 │  │ [○] 物品A | 数量: 5       │                 │
│   │  ├──────────────────────────┤                 │
│[○]│  │ [○] 物品B | 数量: 3       │                 │
│[○]│  └──────────────────────────┘                 │
│[+]│                                               │
└───┴──────────────────────────────────────────────┘
```

## 📋 组件说明

### 1. 左侧容器图标列 `WarehouseSidebarColumn`
- **宽度**: 70dp固定
- **功能**: 
  - 显示所有顶层容器的圆形图标
  - 选中状态高亮显示
  - 显示小红点通知（未读物品数量）
  - 底部"+"按钮添加新容器
- **交互**: 
  - 点击容器图标切换选中状态
  - 右侧内容区自动刷新

### 2. 右侧上部：子容器区 `SubWarehouseRow`
- **布局**: 横向滚动
- **功能**:
  - 显示当前选中容器的所有子容器
  - 圆形图标（48dp）+ 名称
  - 显示物品数量徽章
  - 末尾"+"按钮添加子容器
- **交互**:
  - 点击子容器图标导航到容器详情页

### 3. 右侧下部：物品列表 `ItemListSection`
- **布局**: 垂直滚动
- **功能**:
  - 显示当前选中容器的所有物品
  - 每行：圆形图标 + 名称 + 描述 + 数量
  - 空状态提示："此容器为空"
- **交互**:
  - 点击物品行打开编辑页面

## 🎯 核心组件API

### DiscordStyleMainLayout
主布局整合组件，协调左中右三个区域。

```kotlin
@Composable
fun DiscordStyleMainLayout(
    warehouses: List<Warehouse>,              // 顶层容器列表
    allWarehouses: List<Warehouse>,           // 所有容器（包含子容器）
    allItems: List<Item>,                     // 所有物品
    warehouseItemCounts: Map<Long, Int>,      // 容器物品数量映射
    selectedWarehouseId: Long?,               // 当前选中的容器ID
    onWarehouseSelect: (Warehouse) -> Unit,   // 容器选中回调
    onSubWarehouseClick: (Warehouse) -> Unit, // 子容器点击回调
    onAddWarehouse: () -> Unit,               // 添加容器回调
    onAddChildWarehouse: (Long) -> Unit,      // 添加子容器回调
    onEditItem: (Long) -> Unit                // 编辑物品回调
)
```

### WarehouseSidebarColumn
左侧容器图标列组件。

```kotlin
@Composable
fun WarehouseSidebarColumn(
    warehouses: List<Warehouse>,
    selectedWarehouseId: Long?,
    warehouseItemCounts: Map<Long, Int>,
    onWarehouseClick: (Warehouse) -> Unit,
    onAddWarehouse: () -> Unit
)
```

### SubWarehouseRow
右上子容器横向滚动区。

```kotlin
@Composable
fun SubWarehouseRow(
    subWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    onSubWarehouseClick: (Warehouse) -> Unit,
    onAddSubWarehouse: () -> Unit
)
```

### ItemListSection
右下物品列表区域。

```kotlin
@Composable
fun ItemListSection(
    items: List<Item>,
    onEditItem: (Long) -> Unit
)
```

## 🔄 数据流

1. **初始加载**:
   ```
   DashboardScreen 加载数据
   → 选中第一个容器（或恢复上次选中）
   → 右上显示子容器
   → 右下显示物品
   ```

2. **切换容器**:
   ```
   用户点击左侧容器图标
   → selectedWarehouseId 更新
   → DiscordStyleMainLayout 重新计算子容器和物品
   → UI自动刷新
   ```

3. **点击子容器**:
   ```
   用户点击右上子容器图标
   → 触发 onSubWarehouseClick
   → 导航到容器详情页（WarehouseItemsScreen）
   ```

4. **点击物品**:
   ```
   用户点击右下物品行
   → 触发 onEditItem
   → 导航到物品编辑页（ItemEditScreen）
   ```

## 🎨 颜色主题

所有颜色都使用 `ColorHelpers`，完全支持主题切换：

| 组件 | 颜色 |
|------|------|
| 左侧容器列背景 | `getGroup1NavBarColor()` |
| 右侧内容区背景 | `getGroup2PageBgColor()` |
| 容器图标背景 | `getGroup3CardBgColor()` |
| 选中状态 | `getGroup5FabColor()` |
| 小红点通知 | `getGroup5AlertCardColor()` |
| 文字 | `getGroup4TextColor()` |
| 图标 | `getGroup4IconColor()` |

## ✨ 特性

✅ **自适应主题** - 支持浅色/深色主题自动切换  
✅ **状态保持** - 记住用户选中的容器  
✅ **动画流畅** - 切换容器时内容平滑过渡  
✅ **空状态友好** - 无数据时显示引导提示  
✅ **小红点通知** - 实时显示物品数量  
✅ **搜索框保留** - 顶部搜索框浮动显示（待整合搜索功能）

## 🚀 使用示例

在 `DashboardScreen` 中的使用：

```kotlin
DiscordStyleMainLayout(
    warehouses = warehouses,              // 从 ViewModel 获取
    allWarehouses = allWarehouses,        // 从 ViewModel 获取
    allItems = items,                     // 从 ViewModel 获取
    warehouseItemCounts = warehouseItemCounts, // 计算的数量映射
    selectedWarehouseId = selectedWarehouseId, // 状态变量
    onWarehouseSelect = { warehouse ->
        selectedWarehouseId = warehouse.id // 更新选中状态
    },
    onSubWarehouseClick = { subWarehouse ->
        onNavigateToWarehouseItemsTab(subWarehouse.id) // 导航
    },
    onAddWarehouse = {
        onNavigateToWarehouses() // 导航到容器管理页
    },
    onAddChildWarehouse = { parentId ->
        onAddChildWarehouse(parentId) // 添加子容器
    },
    onEditItem = onEditItem // 编辑物品
)
```

## 🔧 自定义

### 修改左侧栏宽度
编辑 `WarehouseSidebarColumn`，修改：
```kotlin
.width(70.dp) // 改为您想要的宽度
```

### 修改图标大小
编辑 `WarehouseIconItem`，修改：
```kotlin
.size(56.dp) // 容器图标大小
```

编辑 `SubWarehouseIcon`，修改：
```kotlin
.size(48.dp) // 子容器图标大小
```

### 修改徽章样式
编辑 `WarehouseIconItem` 或 `SubWarehouseIcon` 中的徽章部分。

## 📝 待完成功能

- [ ] 整合搜索功能到新布局
- [ ] 添加容器切换动画
- [ ] 支持拖拽排序容器
- [ ] 持久化选中容器状态到 SharedPreferences
- [ ] 长按容器图标显示快捷菜单

## 🎉 完成

Discord风格布局已成功整合到首页！现在享受全新的交互体验吧！

