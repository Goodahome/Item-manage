package com.example.itemremindertool.ui.screens
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
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
    
    // FAB 展开状态
    var fabExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (fabExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fab_rotation"
    )
    
    // 处理返回键：当 FAB 展开时，按返回键关闭 FAB
    BackHandler(enabled = fabExpanded) {
        fabExpanded = false
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
                }
            )
        },
        floatingActionButton = {
            // 展开的 FAB 菜单
    Column(
                modifier = Modifier.padding(bottom = 50.dp), // 往上移16dp
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = fabExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // 添加物品按钮（最上方）
                        FloatingActionButton(
                        onClick = {
                                fabExpanded = false
                                onAddItem(null)
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Inventory, stringResource(R.string.add_item))
                                }
                        
                        // 添加容器按钮
                        FloatingActionButton(
                        onClick = {
                                fabExpanded = false
                                onNavigateToWarehouses()
                            },
                            modifier = Modifier.size(56.dp)
                    ) {
                            Icon(Icons.Default.Warehouse, stringResource(R.string.add_warehouse))
                    }
                }
            }
            
                // 主 FAB 按钮（最下方）
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (fabExpanded) stringResource(R.string.close) else stringResource(R.string.add),
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // 不使用系统 insets，手动控制 padding
    ) { paddingValues ->
        // 搜索框固定在顶部
        var searchQuery by remember { mutableStateOf("") }
        val isSearching = searchQuery.isNotBlank()
        
        Column(
            modifier = modifier.padding(paddingValues)
        ) {
            // 固定的搜索框
            SearchBoxSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = { searchQuery = "" }
            )
            
            // 可滚动的内容
            NewHomeScreenContent(
                items = items,
                warehouses = warehouses,
                allWarehouses = allWarehouses,
                warehouseItemCounts = warehouseItemCounts,
                stats = stats,
                shoppingItemViewModel = shoppingItemViewModel,
                accessHistoryManager = accessHistoryManager,
                alertSettingsManager = alertSettingsManager,
                onNavigateToItems = onNavigateToItems,
                onNavigateToShoppingList = onNavigateToShoppingList,
                onNavigateToWarehouse = onNavigateToWarehouseItemsTab,
                onEditItem = onEditItem,
                searchQuery = searchQuery,
                isSearching = isSearching,
                modifier = Modifier.weight(1f)
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
            containerColor = Color(0xFFEFEBE9)
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
                tint = Color(0xFF5D4037),
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
                    color = Color(0xFF5D4037),
                    maxLines = 1
                )
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D4037).copy(alpha = 0.7f),
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
                containerColor = Color(0xFFEFEBE9)
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
                    tint = Color(0xFF5D4037),
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
                    color = Color(0xFF5D4037)
                )
                Text(
                    text = stringResource(R.string.items_count, itemCount),
                            style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D4037).copy(alpha = 0.7f)
                )
            }
        }
                
                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color(0xFF5D4037).copy(alpha = 0.2f)
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
                            tint = MaterialTheme.colorScheme.primary,
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
                        tint = MaterialTheme.colorScheme.primary,
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
                        tint = MaterialTheme.colorScheme.primary,
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
            containerColor = Color(0xFFEFEBE9)
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
                color = Color(0xFF5D4037),
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
                tint = Color(0xFF5D4037),
                        modifier = Modifier.size(20.dp)
            )
            Text(
                        text = "$warehouseCount",
                        style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
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
                        tint = Color(0xFF5D4037),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
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
        contentPadding = PaddingValues(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp)
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
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(stringResource(R.string.search_all_items)) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true
    )
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
                MaterialTheme.colorScheme.tertiaryContainer
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
