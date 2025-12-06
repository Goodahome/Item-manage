package com.example.itemremindertool.ui.screens
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.viewmodel.DashboardViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.ui.screens.ItemSearchByImageDialog
import com.example.itemremindertool.ui.theme.LocalAppSettings
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.QuickAddBottomSheet
import com.example.itemremindertool.R
import com.example.itemremindertool.data.AlertSettingsManager

import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    itemViewModel: ItemViewModel,
    warehouseViewModel: com.example.itemremindertool.ui.viewmodel.WarehouseViewModel,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    onAddItem: (Long?) -> Unit, // 传递当前选中的容器ID
    onEditItem: (Long) -> Unit,
    onScanBarcode: () -> Unit,
    onItemRecognition: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToItems: (String?) -> Unit = {}, // 接收筛选类型参数，null 表示显示所有物品
    onNavigateToShoppingList: () -> Unit = {},
    onNavigateToWarehouses: () -> Unit = {},
    onAddChildWarehouse: (Long) -> Unit = {},
    onNavigateToWarehouseItemsTab: (Long) -> Unit = {}, // 导航到容器物品页面
    initialSelectedWarehouseId: Long? = null, // 初始选中的容器ID
    modifier: Modifier = Modifier
) {
    val stats by dashboardViewModel.stats.collectAsState()
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    val warehouses by warehouseViewModel.topLevelWarehouses.collectAsState(initial = emptyList())
    val allWarehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
    
    val context = LocalContext.current
    val alertSettingsManager = remember { AlertSettingsManager(context) }

    // 容器物品数量映射（包含所有容器，不仅仅是顶层容器）
    var warehouseItemCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    
    // 计算每个容器的物品数量（包含所有容器，不仅仅是顶层容器）
    LaunchedEffect(allWarehouses, items) {
        val counts = allWarehouses.associate { warehouse ->
            warehouse.id to items.count { it.warehouseId == warehouse.id }
        }
        warehouseItemCounts = counts
    }
    
    // 选中的容器ID状态（null表示显示首页）
    var selectedWarehouseId by remember { 
        mutableStateOf<Long?>(initialSelectedWarehouseId) // 默认为null，显示首页
    }
    
    // 当容器列表变化时，如果选中的容器已被删除，返回首页
    LaunchedEffect(allWarehouses, selectedWarehouseId) {
        if (selectedWarehouseId != null && allWarehouses.none { it.id == selectedWarehouseId }) {
            selectedWarehouseId = null // 返回首页
        }
    }
    
    
    Scaffold(
        topBar = {
            val appSettings = LocalAppSettings.current
            TopAppBar(
                title = { 
                    Text(appSettings.appName)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, stringResource(R.string.settings))
                    }
                },
                actions = {
                    // 首页：显示扫码按钮
                        IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Default.QrCodeScanner, stringResource(R.string.barcode_scanner))
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorHelpers.getGroup1NavBarColor(),
                    titleContentColor = ColorHelpers.getGroup4TextColor(),
                    navigationIconContentColor = ColorHelpers.getGroup4IconColor(),
                    actionIconContentColor = ColorHelpers.getGroup4IconColor()
                )
            )
        },
        floatingActionButton = {
            // 直接跳转到添加物品页面
                        FloatingActionButton(
                        onClick = {
                    // 如果当前选中了容器，则带入容器ID
                    onAddItem(selectedWarehouseId)
                            },
                modifier = Modifier.size(56.dp),
                containerColor = ColorHelpers.getGroup5FabColor()
                ) {
                    Icon(
                        Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_item)
                    )
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // 不使用系统 insets，手动控制 padding
    ) { paddingValues ->
        // 搜索框固定在顶部
        var searchQuery by remember { mutableStateOf("") }
        val isSearching = searchQuery.isNotBlank()
        
        // 获取待购物品数量
        val shoppingItems by shoppingItemViewModel.shoppingItems.collectAsState(initial = emptyList())
        val activeShoppingItemsCount = remember(shoppingItems) {
            shoppingItems.count { !it.isCompleted }
        }
        
        Box(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Discord风格主布局（添加顶部padding为搜索框留出空间）
            DiscordStyleMainLayout(
                warehouses = warehouses,
                allWarehouses = allWarehouses,
                allItems = items,
                warehouseItemCounts = warehouseItemCounts,
                selectedWarehouseId = selectedWarehouseId,
                shoppingItemsCount = activeShoppingItemsCount,
                alertSettingsManager = alertSettingsManager,
                onWarehouseSelect = { warehouse ->
                    selectedWarehouseId = warehouse.id
                },
                onHomeClick = {
                    // 点击首页图标，取消容器选中，显示统计和提醒
                    selectedWarehouseId = null
                },
                onSubWarehouseClick = { subWarehouse ->
                    // 点击子容器，切换到该子容器显示其物品
                    selectedWarehouseId = subWarehouse.id
                },
                onAddWarehouse = {
                    onNavigateToWarehouses()
                },
                onAddChildWarehouse = { parentId ->
                    onAddChildWarehouse(parentId)
                },
                onEditItem = onEditItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp) // 为搜索框留出空间（56dp输入框 + 12dp*2上下padding）
            )
            
            // 搜索框浮动在顶部（毛玻璃效果）
            SearchBoxSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = { searchQuery = "" }
            )
                    }
                    }
                }

data class StatCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color,
    val onClick: (() -> Unit)? = null
)

@Composable
fun StatCard(statCard: StatCardData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .then(
                if (statCard.onClick != null) {
                    Modifier.clickable(onClick = statCard.onClick!!)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statCard.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = statCard.icon,
                contentDescription = statCard.title,
                tint = statCard.color,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statCard.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = statCard.color,
                    maxLines = 1
                )
                Text(
                    text = statCard.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statCard.color.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * 容器统计卡片
 */
@Composable
fun WarehouseStatCard(
    warehouse: Warehouse,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = warehouse.name,
                tint = ColorHelpers.getGroup4IconColor(),
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.items_count, itemCount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    maxLines = 1
                )
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 容器信息页面
 */
@Composable
fun WarehouseInfoScreen(
    warehouse: Warehouse,
    itemCount: Int,
    childWarehouses: List<Warehouse> = emptyList(),
    warehouseItemCounts: Map<Long, Int> = emptyMap(),
    allWarehouses: List<Warehouse> = emptyList(),
    allItems: List<Item> = emptyList(),
    warehouseViewModel: WarehouseViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    onWarehouseClick: (Long) -> Unit = {},
    onEditWarehouse: () -> Unit = {},
    onEditItem: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 递归计算子容器数量（包括所有子容器的子容器）
    val childWarehouseCounts = remember(childWarehouses, allWarehouses) {
        childWarehouses.associate { child ->
            child.id to countAllChildWarehouses(child.id, allWarehouses)
        }
    }
    
    // 递归计算子容器物品数量（包括所有子容器的物品）
    val childItemCounts = remember(childWarehouses, allWarehouses, allItems) {
        childWarehouses.associate { child ->
            child.id to countAllItemsInWarehouse(child.id, allWarehouses, allItems, warehouseItemCounts)
        }
    }
    
    // 加载当前容器的物品
    LaunchedEffect(warehouse.id) {
        warehouseViewModel?.loadWarehouseItems(warehouse.id)
    }
    
    // 安全地获取容器物品状态
    val warehouseItemsState = if (warehouseViewModel != null) {
        val state by warehouseViewModel.uiState.collectAsState()
        state
    } else {
        remember { com.example.itemremindertool.ui.viewmodel.WarehouseUiState() }
    }
    val warehouseItems = warehouseItemsState.warehouseItems
    
    // 移动物品对话框状态
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<Item?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 整合的容器信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 容器图标和名称
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warehouse,
                    contentDescription = null,
                    tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(48.dp)
                )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                Text(
                    text = warehouse.name,
                            style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor()
                )
                Text(
                    text = stringResource(R.string.items_count, itemCount),
                            style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor().copy(alpha = 0.7f)
                )
            }
        }
                
                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
        
        // 容器描述
        if (warehouse.description.isNotEmpty()) {
                    Row(
                modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = ColorHelpers.getGroup4IconColor(),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        Text(
                            stringResource(R.string.description),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                        )
                    Text(
                        text = warehouse.description,
                                style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // 容器位置
        if (warehouse.location.isNotEmpty()) {
                    Row(
                modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup4IconColor(),
                            modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.warehouse_location),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = warehouse.location,
                                style = MaterialTheme.typography.bodyMedium
                        )
                }
            }
        }
        
        // 容器容量
        if (warehouse.capacity != null) {
                    Row(
                modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup4IconColor(),
                            modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.capacity),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${warehouse.capacity}",
                                style = MaterialTheme.typography.bodyMedium
                        )
                        // 显示容量使用情况
                        LinearProgressIndicator(
                            progress = if (warehouse.capacity > 0) {
                                (itemCount.toFloat() / warehouse.capacity).coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.used_capacity,
                                itemCount,
                                warehouse.capacity
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        }
                    }
                }
            }
        }

        // 子容器横向滚动显示
        if (childWarehouses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.child_containers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // 横向滚动的子容器列表
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                items(childWarehouses, key = { it.id }) { childWarehouse ->
                    val childWarehouseCount = childWarehouseCounts[childWarehouse.id] ?: 0
                    val childItemCount = childItemCounts[childWarehouse.id] ?: 0
                                ChildWarehouseCard(
                                    warehouse = childWarehouse,
                        warehouseCount = childWarehouseCount,
                                    itemCount = childItemCount,
                        onClick = { onWarehouseClick(childWarehouse.id) },
                        modifier = Modifier.width(140.dp)
                                )
                            }
                        }
        }
        
        // 容器物品列表
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.warehouse_items),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        if (warehouseItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        stringResource(R.string.warehouse_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // 使用 Column 而不是 LazyColumn，因为外层已经有 verticalScroll
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                warehouseItems.forEach { item ->
                    ItemCard(
                        item = item,
                        onEdit = { 
                            onEditItem?.invoke(item.id)
                        },
                        onDelete = { itemViewModel?.deleteItem(item) },
                        onAddToShoppingCart = if (shoppingItemViewModel != null) {
                            {
                                val shoppingItem = com.example.itemremindertool.data.model.ShoppingItem(
                                    name = item.name,
                                    description = "",
                                    quantity = 1,
                                    isCompleted = false,
                                    priority = com.example.itemremindertool.data.model.Priority.MEDIUM,
                                    createdAt = Date(),
                                    imageUri = item.imageUri
                                )
                                shoppingItemViewModel.insertShoppingItem(shoppingItem)
                        }
                        } else null,
                        onMoveToContainer = if (warehouseViewModel != null && itemViewModel != null) {
                            {
                                itemToMove = item
                                showMoveDialog = true
                }
                        } else null
                    )
                }
            }
        }
    }
    
    // 移动物品对话框
    if (showMoveDialog && itemToMove != null) {
        warehouseViewModel?.let { vm ->
            MoveItemDialog(
                itemName = itemToMove!!.name,
                currentWarehouseId = itemToMove!!.warehouseId,
                warehouseViewModel = vm,
                onDismiss = {
                    showMoveDialog = false
                    itemToMove = null
                },
                onConfirm = { targetWarehouseId ->
                    val updatedItem = itemToMove!!.copy(
                        warehouseId = targetWarehouseId,
                        updatedAt = Date()
                    )
                    itemViewModel?.updateItem(updatedItem)
                    // 重新加载容器物品列表
                    vm.loadWarehouseItems(warehouse.id)
                    showMoveDialog = false
                    itemToMove = null
                }
            )
        }
    }
}

/**
 * 子容器卡片（横向滚动样式，显示统计信息）
 */
@Composable
fun ChildWarehouseCard(
    warehouse: Warehouse,
    warehouseCount: Int,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 容器名称
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            
            // 统计信息（图标 + 数量）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 容器数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
            )
            Text(
                        text = "$warehouseCount",
                        style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
                
                // 物品数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
            }
        }
    }
}

/**
 * 递归计算所有子容器数量（包括子容器的子容器）
 */
fun countAllChildWarehouses(warehouseId: Long, allWarehouses: List<Warehouse>): Int {
    val directChildren = allWarehouses.filter { it.parentId == warehouseId }
    var count = directChildren.size
    directChildren.forEach { child ->
        count += countAllChildWarehouses(child.id, allWarehouses)
    }
    return count
}

/**
 * 递归计算容器及其所有子容器中的物品数量
 */
fun countAllItemsInWarehouse(
    warehouseId: Long,
    allWarehouses: List<Warehouse>,
    allItems: List<Item>,
    warehouseItemCounts: Map<Long, Int>
): Int {
    // 当前容器的物品数量
    var count = warehouseItemCounts[warehouseId] ?: 0
    
    // 递归计算所有子容器的物品数量
    val directChildren = allWarehouses.filter { it.parentId == warehouseId }
    directChildren.forEach { child ->
        count += countAllItemsInWarehouse(child.id, allWarehouses, allItems, warehouseItemCounts)
    }
    
    return count
}

/**
 * 新的首页内容布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreenContent(
    items: List<Item>,
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    stats: com.example.itemremindertool.ui.viewmodel.DashboardStats,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    alertSettingsManager: AlertSettingsManager,
    onNavigateToItems: (String?) -> Unit, // 接收筛选类型参数
    onNavigateToShoppingList: () -> Unit,
    onNavigateToWarehouse: (Long) -> Unit,
    onEditItem: (Long) -> Unit,
    searchQuery: String = "",
    isSearching: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 获取提醒设置
    val expiryReminderDays = remember { alertSettingsManager.getExpiryReminderDays() }
    val lowStockThreshold = remember { alertSettingsManager.getLowStockThreshold() }
    
    // 搜索过滤
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            items.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                (item.barcode?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }
    
    // 根据设置计算到期提醒物品
    val calendar = Calendar.getInstance()
    val currentTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, expiryReminderDays)
    val reminderEndTime = calendar.timeInMillis
    
    val expiringItems = remember(items, currentTime, reminderEndTime, expiryReminderDays) {
        items.filter { item ->
            item.expiryDate != null && 
            item.expiryDate.time >= currentTime && 
            item.expiryDate.time <= reminderEndTime
        }
    }
    
    // 根据设置计算库存不足的物品（只包含启用了库存提醒的物品）
    val lowStockItems = remember(items, lowStockThreshold) {
        items.filter { item ->
            item.enableStockAlert && item.quantity <= lowStockThreshold
        }
    }
    
    val alertCount = expiringItems.size + lowStockItems.size
    
    // 格式化日期
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { dateFormat.format(Date()) }
    val greeting = stringResource(R.string.greeting_today, todayStr)
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 80.dp, // 为搜索框留出空间
            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp
        )
    ) {
        // 搜索模式下显示搜索结果
        if (isSearching) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.search_results, filteredItems.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(filteredItems, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onEdit = { onEditItem(item.id) },
                        onDelete = { /* 搜索模式下不提供删除功能 */ },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            // 正常首页内容
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // 问候语
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (alertCount > 0) {
                item {
                    // 提醒卡片
                    val filterTypeAllAlerts = stringResource(R.string.filter_type_all_alerts)
                    AlertCard(
                        count = alertCount,
                        expiringCount = expiringItems.size,
                        lowStockCount = lowStockItems.size,
                        expiryReminderDays = expiryReminderDays,
                        onClick = { 
                            // 点击后跳转到筛选后的物品列表
                            onNavigateToItems(filterTypeAllAlerts)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            item {
                // 快捷入口（只显示购物篮）
                QuickAccessSection(
                    shoppingItemCount = stats.activeShoppingItems,
                    onNavigateToShoppingList = onNavigateToShoppingList
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // 最近打开
                RecentlyOpenedSection(
                    allWarehouses = allWarehouses,
                    warehouseItemCounts = warehouseItemCounts,
                    items = items,
                    accessHistoryManager = accessHistoryManager,
                    onNavigateToWarehouse = onNavigateToWarehouse
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // 所有容器列表
                AllLocationsSection(
                    warehouses = warehouses,
                    allWarehouses = allWarehouses,
                    items = items,
                    warehouseItemCounts = warehouseItemCounts,
                    onNavigateToWarehouse = onNavigateToWarehouse
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 搜索框部分
 */
@Composable
fun SearchBoxSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    // 搜索框外层完全透明，只有输入框是毛玻璃效果
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    stringResource(R.string.search_all_items),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ) 
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ) 
            },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = onCloseSearch) {
                        Icon(
                            Icons.Default.Close, 
                            null, 
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorHelpers.getGroup3CardBgColor().copy(alpha = 1f),
                unfocusedContainerColor = ColorHelpers.getGroup3CardBgColor().copy(alpha = 0.9f),
                focusedTextColor = ColorHelpers.getGroup4TextColor(),
                unfocusedTextColor = ColorHelpers.getGroup4TextColor(),
                focusedBorderColor = ColorHelpers.getGroup4TextColor().copy(alpha = 0.8f),
                unfocusedBorderColor = ColorHelpers.getGroup4TextColor().copy(alpha = 0.5f),
                focusedPlaceholderColor = ColorHelpers.getGroup4TextColor(0.5f),
                unfocusedPlaceholderColor = ColorHelpers.getGroup4TextColor(0.5f),
                focusedLeadingIconColor = ColorHelpers.getGroup4IconColor(0.7f),
                unfocusedLeadingIconColor = ColorHelpers.getGroup4IconColor(0.6f),
                focusedTrailingIconColor = ColorHelpers.getGroup4IconColor(0.7f),
                unfocusedTrailingIconColor = ColorHelpers.getGroup4IconColor(0.6f)
        ),
        singleLine = true
    )
    }
}

/**
 * 提醒卡片
 */
@Composable
fun AlertCard(
    count: Int,
    expiringCount: Int,
    lowStockCount: Int,
    expiryReminderDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expiringCount > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                ColorHelpers.getGroup5AlertCardColor()
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.expiring_soon_alert, expiryReminderDays, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (expiringCount > 0) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 快捷入口部分（只显示购物篮）
 */
@Composable
fun QuickAccessSection(
    shoppingItemCount: Int,
    onNavigateToShoppingList: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            // 待购清单
            item {
                CommonEntryCard(
                    title = stringResource(R.string.shopping_list_entry),
                    count = shoppingItemCount,
                    onClick = onNavigateToShoppingList
                )
            }
        }
    }
}

/**
 * 常用入口卡片（正方形）
 */
@Composable
fun CommonEntryCard(
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.items_count_format, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 最近打开部分
 */
@Composable
fun RecentlyOpenedSection(
    allWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    items: List<Item>,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    onNavigateToWarehouse: (Long) -> Unit
) {
    // 获取最近访问的容器ID列表（按时间降序，最新的在前）
    val recentAccessHistory by accessHistoryManager.accessHistory.collectAsState()
    val recentWarehouses = remember(recentAccessHistory, allWarehouses) {
        recentAccessHistory
            .mapNotNull { access ->
                allWarehouses.find { it.id == access.warehouseId }
            }
            .take(10) // 最多显示10个
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.recently_opened),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (recentWarehouses.isNotEmpty()) {
            // 横向滚动的正方形卡片（最新的在左边第一个）
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(recentWarehouses, key = { it.id }) { warehouse ->
                    val itemCount = countAllItemsInWarehouse(
                        warehouse.id,
                        allWarehouses,
                        items,
                        warehouseItemCounts
                    )
                    val childWarehouseCount = countAllChildWarehouses(warehouse.id, allWarehouses)
                    RecentlyOpenedItem(
                        warehouse = warehouse,
                        itemCount = itemCount,
                        childWarehouseCount = childWarehouseCount,
                        onClick = { onNavigateToWarehouse(warehouse.id) }
                    )
                }
            }
        } else {
            // 空状态占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_recently_opened),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 最近打开项（正方形卡片）
 */
@Composable
fun RecentlyOpenedItem(
    warehouse: Warehouse,
    itemCount: Int,
    childWarehouseCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 统计信息（图标 + 数量）
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$childWarehouseCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 物品数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
    }
}
        }
    }
}

/**
 * 所有容器部分
 */
@Composable
fun AllLocationsSection(
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    items: List<Item>,
    warehouseItemCounts: Map<Long, Int>,
    onNavigateToWarehouse: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.all_locations),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 使用横向滚动显示正方形卡片（与最近打开的卡片保持一致）
        if (warehouses.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(warehouses, key = { it.id }) { warehouse ->
                    val itemCount = countAllItemsInWarehouse(
                        warehouse.id,
                        allWarehouses,
                        items,
                        warehouseItemCounts
                    )
                    val childWarehouseCount = countAllChildWarehouses(warehouse.id, allWarehouses)
                    LocationCard(
                        warehouse = warehouse,
                        itemCount = itemCount,
                        childWarehouseCount = childWarehouseCount,
                        onClick = { onNavigateToWarehouse(warehouse.id) }
                    )
                }
            }
        }
    }
}

/**
 * 容器卡片（正方形）
 */
@Composable
fun LocationCard(
    warehouse: Warehouse,
    itemCount: Int,
    childWarehouseCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(140.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称（居中）
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 统计信息（图标 + 数量）
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$childWarehouseCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 物品数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================================
// Discord风格新布局组件
// ============================================================================

/**
 * 左侧容器图标列表项
 */
@Composable
fun WarehouseIconItem(
    warehouse: Warehouse,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        ColorHelpers.getGroup5FabColor()
    } else {
        ColorHelpers.getGroup3CardBgColor()
    }
    
    // 外层容器，用于显示选中指示器
    Box(
        modifier = modifier
            .fillMaxWidth() // 确保容器占满宽度，让指示器能正确显示
    ) {
        // 选中指示器（左侧竖条）
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp) // 与图标保持一定距离
                    .width(3.dp)
                    .height(24.dp)
                    .background(
                        ColorHelpers.getGroup5FabColor(),
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }
        
        // 图标圆形容器（居中显示）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp) // 减小：56dp → 44dp
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
        // 显示容器首字母或图标
        Text(
            text = warehouse.name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge, // 减小字体
            fontWeight = FontWeight.Bold,
            color = ColorHelpers.getGroup4TextColor()
        )
        }
        }
    }
}

/**
 * 左侧容器图标列
 */
@Composable
fun WarehouseSidebarColumn(
    warehouses: List<Warehouse>,
    selectedWarehouseId: Long?,
    warehouseItemCounts: Map<Long, Int>,
    onWarehouseClick: (Warehouse) -> Unit,
    onHomeClick: () -> Unit, // 新增：点击首页图标的回调
    onAddWarehouse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(60.dp) // 减小：70dp → 60dp
            .fillMaxHeight()
            .background(ColorHelpers.getGroup1NavBarColor())
            .padding(vertical = 6.dp), // 减小：8dp → 6dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // 减小：12dp → 8dp
    ) {
        // 首页图标（固定在顶部）
        val isHomeSelected = selectedWarehouseId == null
        Box(
            modifier = Modifier.fillMaxWidth() // 确保容器占满宽度，让指示器能正确显示
        ) {
            // 选中指示器（左侧竖条）
            if (isHomeSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 4.dp) // 与图标保持一定距离
                        .width(3.dp)
                        .height(24.dp)
                        .background(
                            ColorHelpers.getGroup5FabColor(),
                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                )
            }
            
            // 首页图标圆形容器（居中显示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp) // 减小：56dp → 44dp
                        .clip(CircleShape)
                        .background(
                            if (isHomeSelected) ColorHelpers.getGroup5FabColor()
                            else ColorHelpers.getGroup3CardBgColor()
                        )
                        .clickable(onClick = onHomeClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "首页",
                        tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(22.dp) // 减小：28dp → 22dp
                    )
                }
            }
        }
        
        // 分隔线
        Box(
            modifier = Modifier
                .width(32.dp) // 减小：40dp → 32dp
                .height(1.5.dp) // 减小：2dp → 1.5dp
                .background(ColorHelpers.getGroup4IconColor(0.3f))
        )
        
        // 容器列表（可滚动）
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // 减小：12dp → 8dp
        ) {
            items(warehouses) { warehouse ->
                WarehouseIconItem(
                    warehouse = warehouse,
                    isSelected = warehouse.id == selectedWarehouseId,
                    itemCount = warehouseItemCounts[warehouse.id] ?: 0,
                    onClick = { onWarehouseClick(warehouse) }
                )
            }
            
            // 添加按钮（跟随在容器图标后面）
            item {
                Box(
                    modifier = Modifier
                        .size(44.dp) // 减小：56dp → 44dp
                        .clip(CircleShape)
                        .background(ColorHelpers.getGroup5FabColor())
                        .clickable(onClick = onAddWarehouse),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加容器",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp) // 减小：24dp → 20dp
                    )
                }
            }
        }
    }
}

/**
 * 右上子容器图标项
 */
@Composable
fun SubWarehouseIcon(
    warehouse: Warehouse,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp), // 减小：8dp → 6dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp) // 减小：4dp → 3dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp) // 减小：48dp → 40dp
                    .clip(CircleShape)
                    .background(ColorHelpers.getGroup2SettingsBtnColor()), // 使用不同的背景色以区分
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = warehouse.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor()
                )
            }
            
            // 徽章（物品数量）
            if (itemCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-2).dp) // 调整偏移
                        .size(14.dp) // 减小：16dp → 14dp
                        .clip(CircleShape)
                        .background(ColorHelpers.getGroup5AlertCardColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (itemCount > 99) "99+" else itemCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 8.sp // 减小：9sp → 8sp
                    )
                }
            }
        }
        
        // 子容器名称
        Text(
            text = warehouse.name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp, // 减小字体
            color = ColorHelpers.getGroup4TextColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 50.dp) // 减小：60dp → 50dp
        )
    }
}

/**
 * 右上子容器横向滚动区
 */
@Composable
fun SubWarehouseRow(
    subWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    onSubWarehouseClick: (Warehouse) -> Unit,
    onAddSubWarehouse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp), // 减小：16dp,8dp → 12dp,6dp
        shape = RoundedCornerShape(8.dp), // 减小：12dp → 8dp
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 减小：2dp → 1dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp), // 减小：12dp,8dp → 8dp,6dp
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subWarehouses) { subWarehouse ->
                SubWarehouseIcon(
                    warehouse = subWarehouse,
                    itemCount = warehouseItemCounts[subWarehouse.id] ?: 0,
                    onClick = { onSubWarehouseClick(subWarehouse) }
                )
            }
            
            // 添加子容器按钮
            item {
                Column(
                    modifier = Modifier
                        .clickable(onClick = onAddSubWarehouse)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ColorHelpers.getGroup5FabColor()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加子容器",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "添加",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
            }
        }
    }
}

/**
 * 右下物品列表项
 */
@Composable
fun ItemListRow(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp) // 减小：16dp,4dp → 12dp,3dp
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp), // 减小：12dp → 8dp
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp), // 减小：12dp → 10dp
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 减小：12dp → 10dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧圆形物品图标
            Box(
                modifier = Modifier
                    .size(36.dp) // 减小：40dp → 36dp
                    .clip(CircleShape)
                    .background(ColorHelpers.getGroup2SettingsBtnColor()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // 中间物品信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp) // 减小：4dp → 3dp
            ) {
                // 物品名称（粗体）
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 物品描述或状态
                Text(
                    text = if (item.description.isNotBlank()) {
                        "描述：${item.description}"
                    } else {
                        "暂无描述"
                    },
                    style = MaterialTheme.typography.labelSmall, // 减小字体
                    color = ColorHelpers.getGroup4TextColor(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 右侧数量信息
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "数量",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp, // 进一步减小
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleSmall, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor()
                )
            }
        }
    }
}

/**
 * 右下物品列表区域
 */
@Composable
fun ItemListSection(
    items: List<Item>,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        // 空状态
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    tint = ColorHelpers.getGroup4IconColor(0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "此容器为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp), // 减小：4dp → 3dp
            contentPadding = PaddingValues(bottom = 80.dp) // 为 FAB 留出空间
        ) {
            items(items) { item ->
                ItemListRow(
                    item = item,
                    onClick = { onEditItem(item.id) }
                )
            }
        }
    }
}

/**
 * Discord风格的主布局 - 整合左侧容器列和右侧内容区
 */
@Composable
fun DiscordStyleMainLayout(
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    allItems: List<Item>,
    warehouseItemCounts: Map<Long, Int>,
    selectedWarehouseId: Long?,
    shoppingItemsCount: Int, // 新增：待购物品数量
    alertSettingsManager: AlertSettingsManager, // 新增：提醒设置管理器
    onWarehouseSelect: (Warehouse) -> Unit,
    onHomeClick: () -> Unit, // 新增：点击首页的回调
    onSubWarehouseClick: (Warehouse) -> Unit,
    onAddWarehouse: () -> Unit,
    onAddChildWarehouse: (Long) -> Unit,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 计算当前容器的根容器（顶级容器）ID，用于侧边栏选中状态
    val rootWarehouseId = remember(selectedWarehouseId, allWarehouses, warehouses) {
        if (selectedWarehouseId == null) {
            null
        } else {
            // 递归查找根容器
            var currentId = selectedWarehouseId
            var currentWarehouse = allWarehouses.find { it.id == currentId }
            while (currentWarehouse != null && currentWarehouse.parentId != null) {
                currentId = currentWarehouse.parentId
                currentWarehouse = allWarehouses.find { it.id == currentId }
            }
            currentId
        }
    }
    
    // 计算当前容器的层级路径（面包屑导航）
    val breadcrumbPath = remember(selectedWarehouseId, allWarehouses, warehouses) {
        if (selectedWarehouseId == null) {
            emptyList<Warehouse>()
        } else {
            val path = mutableListOf<Warehouse>()
            var currentId = selectedWarehouseId
            
            // 合并所有容器列表，确保能找到容器
            val allContainers = (allWarehouses + warehouses).distinctBy { it.id }
            
            // 查找当前容器
            var currentWarehouse = allContainers.find { it.id == currentId }
            
            // 如果找不到当前容器，返回空列表
            if (currentWarehouse == null) {
                return@remember emptyList<Warehouse>()
            }
            
            // 从当前容器向上查找，直到根容器
            while (currentWarehouse != null) {
                path.add(0, currentWarehouse) // 添加到开头，保持从根到当前的顺序
                if (currentWarehouse.parentId != null) {
                    currentId = currentWarehouse.parentId
                    // 查找父容器
                    currentWarehouse = allContainers.find { it.id == currentId }
                } else {
                    break
                }
            }
            path
        }
    }
    
    // 获取选中容器的子容器
    val childWarehouses = remember(selectedWarehouseId, allWarehouses) {
        if (selectedWarehouseId != null) {
            allWarehouses.filter { it.parentId == selectedWarehouseId }
        } else {
            emptyList()
        }
    }
    
    // 获取选中容器的物品
    val warehouseItems = remember(selectedWarehouseId, allItems) {
        if (selectedWarehouseId != null) {
            allItems.filter { it.warehouseId == selectedWarehouseId }
        } else {
            emptyList()
        }
    }
    
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // 左侧容器图标列
        WarehouseSidebarColumn(
            warehouses = warehouses,
            selectedWarehouseId = rootWarehouseId, // 使用根容器ID保持选中状态
            warehouseItemCounts = warehouseItemCounts,
            onWarehouseClick = onWarehouseSelect,
            onHomeClick = onHomeClick,
            onAddWarehouse = onAddWarehouse
        )
        
        // 右侧内容区
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ColorHelpers.getGroup2PageBgColor())
        ) {
            // 首页状态（selectedWarehouseId == null）
            if (selectedWarehouseId == null) {
                // 右上：统计卡片
                HomeStatisticCards(
                    totalWarehouses = warehouses.size,
                    totalItems = allItems.size,
                    shoppingItemsCount = shoppingItemsCount
                )
                
                // 右下：提醒列表
                AlertListSection(
                    items = allItems,
                    alertSettingsManager = alertSettingsManager,
                    onEditItem = onEditItem,
                    modifier = Modifier.weight(1f)
                )
            }
            // 如果有选中的容器，显示子容器和物品
            else if (selectedWarehouseId != null) {
                // 面包屑导航（显示层级路径）- 只要选中容器就显示
                // 合并所有容器列表，确保能找到容器
                val allContainers = remember(allWarehouses, warehouses) {
                    (allWarehouses + warehouses).distinctBy { it.id }
                }
                
                // 计算显示路径：优先使用计算好的路径，如果为空则至少显示当前容器
                val displayPath = remember(selectedWarehouseId, breadcrumbPath, allContainers) {
                    if (breadcrumbPath.isNotEmpty()) {
                        breadcrumbPath
                    } else {
                        // 如果路径为空，从合并的容器列表中查找当前容器
                        allContainers.find { it.id == selectedWarehouseId }
                            ?.let { listOf(it) } ?: emptyList()
                    }
                }
                
                // 显示容器层级信息（替代面包屑）
                if (displayPath.isNotEmpty()) {
                    WarehouseLevelHeader(
                        currentWarehouse = displayPath.lastOrNull(),
                        parentWarehouse = displayPath.getOrNull(displayPath.size - 2),
                        onNavigateToParent = { parent ->
                            // 点击返回父容器
                            onWarehouseSelect(parent)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                
                // 右上：子容器横向滚动区
                if (childWarehouses.isNotEmpty() || true) { // 总是显示，即使为空也显示添加按钮
                    SubWarehouseRow(
                        subWarehouses = childWarehouses,
                        warehouseItemCounts = warehouseItemCounts,
                        onSubWarehouseClick = onSubWarehouseClick,
                        onAddSubWarehouse = { onAddChildWarehouse(selectedWarehouseId) }
                    )
                }
                
                // 右下：物品列表
                ItemListSection(
                    items = warehouseItems,
                    onEditItem = onEditItem,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 首页统计卡片（右上）
 */
@Composable
fun HomeStatisticCards(
    totalWarehouses: Int,
    totalItems: Int,
    shoppingItemsCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp), // 与子容器卡片一致
        shape = RoundedCornerShape(8.dp), // 与子容器卡片一致
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 与子容器卡片一致
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp), // 与子容器卡片内部padding一致
            horizontalArrangement = Arrangement.spacedBy(6.dp), // 改为统一间距
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 容器数量 - 圆形样式
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 圆形大小
                        .clip(CircleShape)
                        .background(ColorHelpers.getGroup2SettingsBtnColor()), // 使用不同的背景色以区分
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = totalWarehouses.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
                Text(
                    text = "容器",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = ColorHelpers.getGroup4TextColor(0.7f)
                )
            }
            
            // 物品数量 - 圆形样式
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 圆形大小
                        .clip(CircleShape)
                        .background(ColorHelpers.getGroup2SettingsBtnColor()), // 使用不同的背景色以区分
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = totalItems.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
                Text(
                    text = "物品",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = ColorHelpers.getGroup4TextColor(0.7f)
                )
            }
            
            // 待购物品 - 圆形样式
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 圆形大小
                        .clip(CircleShape)
                        .background(ColorHelpers.getGroup2SettingsBtnColor()), // 使用不同的背景色以区分
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shoppingItemsCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
                Text(
                    text = "待购",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = ColorHelpers.getGroup4TextColor(0.7f)
                )
            }
        }
    }
}

/**
 * 提醒列表项
 */
@Composable
fun AlertListItem(
    title: String,
    description: String,
    icon: ImageVector,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp) // 减小：16dp,4dp → 12dp,3dp
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp), // 减小：12dp → 8dp
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp), // 减小：12dp → 10dp
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 减小：12dp → 10dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Box(
                modifier = Modifier
                    .size(36.dp) // 减小：40dp → 36dp
                    .clip(CircleShape)
                    .background(ColorHelpers.getGroup5AlertCardColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp) // 减小：24dp → 20dp
                )
            }
            
            // 中间信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp) // 减小：4dp → 3dp
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall, // 减小字体
                    color = ColorHelpers.getGroup4TextColor(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 右侧时间
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = ColorHelpers.getGroup4TextColor(0.5f)
            )
        }
    }
}

/**
 * 提醒列表（右下）
 */
@Composable
fun AlertListSection(
    items: List<Item>,
    alertSettingsManager: AlertSettingsManager,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 获取提醒设置
    val expiryReminderDays = remember { alertSettingsManager.getExpiryReminderDays() }
    val lowStockThreshold = remember { alertSettingsManager.getLowStockThreshold() }
    
    // 计算即将过期物品
    val calendar = Calendar.getInstance()
    val currentTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, expiryReminderDays)
    val reminderEndTime = calendar.timeInMillis
    
    val expiringItems = remember(items, currentTime, reminderEndTime) {
        items.filter { item ->
            item.expiryDate != null && 
            item.expiryDate.time >= currentTime && 
            item.expiryDate.time <= reminderEndTime
        }
    }
    
    // 计算库存不足物品
    val lowStockItems = remember(items, lowStockThreshold) {
        items.filter { item ->
            item.enableStockAlert && item.quantity <= lowStockThreshold
        }
    }
    
    // 合并所有提醒，按时间排序
    data class AlertItem(
        val item: Item,
        val type: String, // "expiring" or "lowstock"
        val time: Long
    )
    
    val allAlerts = remember(expiringItems, lowStockItems) {
        val alerts = mutableListOf<AlertItem>()
        
        // 添加即将过期提醒
        expiringItems.forEach { item ->
            alerts.add(AlertItem(item, "expiring", item.expiryDate?.time ?: 0))
        }
        
        // 添加库存不足提醒
        lowStockItems.forEach { item ->
            alerts.add(AlertItem(item, "lowstock", item.updatedAt.time))
        }
        
        // 按时间降序排序（最新的在前）
        alerts.sortedByDescending { it.time }
    }
    
    if (allAlerts.isEmpty()) {
        // 空状态
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ColorHelpers.getGroup4IconColor(0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "暂无提醒",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
                Text(
                    text = "一切正常！",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.5f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp), // 减小：4dp → 3dp
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(allAlerts) { alert ->
                val timeStr = remember(alert.time) {
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(alert.time))
                }
                
                when (alert.type) {
                    "expiring" -> {
                        val daysUntilExpiry = remember(alert.item.expiryDate) {
                            if (alert.item.expiryDate != null) {
                                val days = (alert.item.expiryDate.time - currentTime) / (1000 * 60 * 60 * 24)
                                days.toInt()
                            } else {
                                0
                            }
                        }
                        
                        AlertListItem(
                            title = alert.item.name,
                            description = if (daysUntilExpiry <= 0) "已过期" else "还剩 $daysUntilExpiry 天过期",
                            icon = Icons.Default.Warning,
                            time = timeStr,
                            onClick = { onEditItem(alert.item.id) }
                        )
                    }
                    "lowstock" -> {
                        AlertListItem(
                            title = alert.item.name,
                            description = "库存不足：剩余 ${alert.item.quantity} 件",
                            icon = Icons.Default.Inventory2,
                            time = timeStr,
                            onClick = { onEditItem(alert.item.id) }
                        )
                    }
                }
            }
        }
    }
}


/**
 * 容器层级头部组件（替代面包屑导航）
 * 显示当前容器信息，如果有父容器则显示返回按钮
 */
@Composable
fun WarehouseLevelHeader(
    currentWarehouse: Warehouse?,
    parentWarehouse: Warehouse?,
    onNavigateToParent: (Warehouse) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentWarehouse == null) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：层级信息和容器名称
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 如果有父容器，显示返回按钮
                if (parentWarehouse != null) {
                    IconButton(
                        onClick = { onNavigateToParent(parentWarehouse) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回上级容器",
                            tint = ColorHelpers.getGroup4IconColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // 容器名称和层级指示
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 如果有父容器，显示层级路径
                    if (parentWarehouse != null) {
                        Text(
                            text = parentWarehouse.name.ifEmpty { "未命名容器" },
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = ColorHelpers.getGroup4IconColor(0.5f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(90f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentWarehouse.name.ifEmpty { "未命名容器" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // 没有父容器，只显示当前容器名称
                        Text(
                            text = currentWarehouse.name.ifEmpty { "未命名容器" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 右侧：层级深度指示器（可选）
            if (parentWarehouse != null) {
                Surface(
                    shape = CircleShape,
                    color = ColorHelpers.getGroup5FabColor().copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "L${parentWarehouse.level + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorHelpers.getGroup5FabColor(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
