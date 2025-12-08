package com.example.itemremindertool.ui.screens
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import android.content.SharedPreferences

/**
 * 首页布局风格枚举
 */
enum class HomeLayoutStyle(val key: String) {
    DISCORD("discord"),      // Discord风格（左侧容器列）
    CLASSIC("classic")       // 经典风格（卡片式布局）
}

/**
 * 获取首页布局风格偏好
 */
@Composable
fun rememberHomeLayoutStyle(): MutableState<HomeLayoutStyle> {
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) 
    }
    
    return remember {
        val savedStyle = prefs.getString("home_layout_style", HomeLayoutStyle.DISCORD.key)
        mutableStateOf(
            HomeLayoutStyle.values().find { it.key == savedStyle } ?: HomeLayoutStyle.DISCORD
        )
    }
}

/**
 * 保存首页布局风格偏好
 */
fun saveHomeLayoutStyle(context: android.content.Context, style: HomeLayoutStyle) {
    val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    prefs.edit().putString("home_layout_style", style.key).apply()
}

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
    onAddWarehouse: () -> Unit = {}, // 添加容器回调
    onAddChildWarehouse: (Long) -> Unit = {},
    onEditWarehouse: (Long) -> Unit = {}, // 编辑容器回调
    onDeleteWarehouse: (com.example.itemremindertool.data.model.Warehouse) -> Unit = {}, // 删除容器回调
    onDeleteItem: (Item) -> Unit = {}, // 删除物品回调
    onNavigateToWarehouseItemsTab: (Long) -> Unit = {}, // 导航到容器物品页面
    initialSelectedWarehouseId: Long? = null, // 初始选中的容器ID
    onSelectedWarehouseIdChanged: (Long?) -> Unit = {}, // 选中容器ID变化时的回调
    modifier: Modifier = Modifier
) {
    val stats by dashboardViewModel.stats.collectAsState()
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    val warehouses by warehouseViewModel.topLevelWarehouses.collectAsState(initial = emptyList())
    val allWarehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
    
    val context = LocalContext.current
    val alertSettingsManager = remember { AlertSettingsManager(context) }
    
    // 首页布局风格状态
    val homeLayoutStyleState = rememberHomeLayoutStyle()
    var homeLayoutStyle by homeLayoutStyleState

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
    
    // 同步外部传入的 initialSelectedWarehouseId 到内部状态
    // 注意：需要同时监听 initialSelectedWarehouseId 和 homeLayoutStyle，确保 Discord 风格下正确同步
    LaunchedEffect(initialSelectedWarehouseId, homeLayoutStyle) {
        // 总是同步，包括 null 值，确保返回时能正确恢复状态
        selectedWarehouseId = initialSelectedWarehouseId
    }
    
    // 当内部 selectedWarehouseId 变化时，通知外部
    LaunchedEffect(selectedWarehouseId) {
        onSelectedWarehouseIdChanged(selectedWarehouseId)
    }
    
    // 当容器列表变化时，如果选中的容器已被删除，返回首页
    LaunchedEffect(allWarehouses, selectedWarehouseId) {
        // 仓库列表加载完成且不为空时才进行校验，避免列表为空导致误清空选中状态
        if (selectedWarehouseId != null && allWarehouses.isNotEmpty() && allWarehouses.none { it.id == selectedWarehouseId }) {
            selectedWarehouseId = null // 返回首页
        }
    }
    
    // 搜索框显示状态（需要在 Scaffold 之前定义，以便在 topBar 中使用）
    var showSearchBox by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()
    
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
                    // 风格切换按钮
                    IconButton(
                        onClick = {
                            homeLayoutStyle = if (homeLayoutStyle == HomeLayoutStyle.DISCORD) {
                                HomeLayoutStyle.CLASSIC
                            } else {
                                HomeLayoutStyle.DISCORD
                            }
                            saveHomeLayoutStyle(context, homeLayoutStyle)
                        }
                    ) {
                        Icon(
                            if (homeLayoutStyle == HomeLayoutStyle.DISCORD) {
                                Icons.Default.ViewModule // 切换到经典风格图标
                            } else {
                                Icons.Default.ViewList // 切换到Discord风格图标
                            },
                            contentDescription = stringResource(R.string.switch_layout_style)
                        )
                    }
                    // 搜索按钮
                    IconButton(
                        onClick = { showSearchBox = !showSearchBox }
                    ) {
                        Icon(
                            if (showSearchBox) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                    // 首页：显示扫码按钮
                        IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Default.QrCodeScanner, stringResource(R.string.barcode_scanner))
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = ColorHelpers.getGroup4TextColor(),
                    navigationIconContentColor = ColorHelpers.getGroup4IconColor(),
                    actionIconContentColor = ColorHelpers.getGroup4IconColor()
                )
            )
        },
        floatingActionButton = {
            // 直接跳转到添加物品页面
    Column(
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
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
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // 不使用系统 insets，手动控制 padding
    ) { paddingValues ->
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
            // 根据风格切换显示不同的布局
            when (homeLayoutStyle) {
                HomeLayoutStyle.DISCORD -> {
                    // Discord风格主布局（添加顶部padding为搜索框留出空间）
                    DiscordStyleMainLayout(
                        warehouses = warehouses,
                        allWarehouses = allWarehouses,
                        allItems = items,
                        warehouseItemCounts = warehouseItemCounts,
                        selectedWarehouseId = selectedWarehouseId,
                        shoppingItemsCount = activeShoppingItemsCount,
                        alertSettingsManager = alertSettingsManager,
                        shoppingItemViewModel = shoppingItemViewModel,
                        itemViewModel = itemViewModel,
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
                        onAddWarehouse = onAddWarehouse,
                        onAddChildWarehouse = { parentId ->
                            onAddChildWarehouse(parentId)
                        },
                        onEditWarehouse = onEditWarehouse,
                        onDeleteWarehouse = onDeleteWarehouse,
                        onEditItem = onEditItem,
                        onDeleteItem = { item ->
                            itemViewModel.deleteItem(item)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (showSearchBox) 80.dp else 0.dp) // 仅在搜索框显示时为搜索框留出空间
                    )
                }
                HomeLayoutStyle.CLASSIC -> {
                    // 经典风格布局（卡片式）
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
                showSearchBox = showSearchBox, // 传递搜索框显示状态
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // 搜索框浮动在顶部（毛玻璃效果）- 仅在showSearchBox为true时显示
            if (showSearchBox) {
                SearchBoxSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCloseSearch = { 
                        searchQuery = ""
                        showSearchBox = false
                    }
                )
            }
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
                        modifier = Modifier.width(96.dp)
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
            .width(96.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
            // 统计信息（图标 + 数量）- 单行显示
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$warehouseCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = ColorHelpers.getGroup4IconColor()
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
    showSearchBox: Boolean = false, // 搜索框显示状态
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
            top = if (showSearchBox) 80.dp else 0.dp, // 仅在搜索框显示时为搜索框留出空间
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
            .width(96.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.8.dp))
            Text(
                text = stringResource(R.string.items_count_format, count),
                fontSize = 10.8.sp,
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
    
    // 创建 LazyRow 的滚动状态
    val lazyListState = rememberLazyListState()
    
    // 当列表的第一个元素改变时（即最新打开的容器改变），重置滚动位置到开始
    val firstWarehouseId = remember(recentWarehouses) {
        recentWarehouses.firstOrNull()?.id
    }
    
    LaunchedEffect(firstWarehouseId) {
        // 当第一个容器改变时，重置滚动位置到开始（确保列表左对齐）
        if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
            lazyListState.animateScrollToItem(0, scrollOffset = 0)
        }
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
                state = lazyListState,
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
@OptIn(ExperimentalMaterial3Api::class)
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
            .width(96.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
            // 统计信息（图标 + 数量）- 单行显示
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$childWarehouseCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = MaterialTheme.colorScheme.primary
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
@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = Modifier
            .then(modifier)
            .width(96.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称（居中）
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
            // 统计信息（图标 + 数量）- 单行显示
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$childWarehouseCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = MaterialTheme.colorScheme.primary
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WarehouseIconItem(
    warehouse: Warehouse,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
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
                        ColorHelpers.getGroup2SettingsBtnColor(), // 与按钮颜色一致
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }
        
        // 图标圆形容器（居中显示）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
            val textColor = ColorHelpers.getContrastColor(backgroundColor)
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 与右侧子容器大小一致
                        .clip(CircleShape)
                        .background(backgroundColor) // 与右侧容器按钮一致
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = { showMenu = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 显示容器首字母或图标
                    Text(
                        text = warehouse.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall, // 与右侧子容器字体大小一致
                        fontWeight = FontWeight.Bold,
                        color = textColor // 根据背景颜色对比度自动切换
                    )
                }
                
                // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = ColorHelpers.getGroup3CardBgColor(),
            tonalElevation = 8.dp
        ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEditWarehouse(warehouse)
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteWarehouse(warehouse)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
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
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    modifier: Modifier = Modifier
) {
                Column(
        modifier = modifier
            .width(60.dp) // 减小：70dp → 60dp
            .fillMaxHeight()
            .background(Color.Transparent)
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
                            ColorHelpers.getGroup2SettingsBtnColor(), // 与按钮颜色一致
                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                )
            }
            
            // 首页图标圆形容器（居中显示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(40.dp) // 与右侧子容器大小一致
                        .clip(CircleShape)
                        .background(backgroundColor) // 与右侧容器按钮一致
                        .clickable(onClick = onHomeClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "首页",
                        tint = iconColor, // 根据背景颜色对比度自动切换
                        modifier = Modifier.size(20.dp) // 与右侧子容器图标大小一致
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
                    onClick = { onWarehouseClick(warehouse) },
                    onEditWarehouse = onEditWarehouse,
                    onDeleteWarehouse = onDeleteWarehouse
                )
            }
            
            // 添加按钮（跟随在容器图标后面）
            item {
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(40.dp) // 与容器图标大小一致
                        .clip(CircleShape)
                        .background(backgroundColor) // 与容器图标颜色一致
                        .clickable(onClick = onAddWarehouse),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加容器",
                        tint = iconColor, // 根据背景颜色对比度自动切换
                        modifier = Modifier.size(20.dp) // 与容器图标大小一致
                    )
                }
            }
        }
    }
}

/**
 * 右上子容器图标项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubWarehouseIcon(
    warehouse: Warehouse,
    itemCount: Int,
    onClick: () -> Unit,
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 6.dp), // 减小：8dp → 6dp
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp) // 减小：4dp → 3dp
        ) {
            val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
            val textColor = ColorHelpers.getContrastColor(backgroundColor)
            Box(
                modifier = Modifier
                    .size(40.dp) // 与添加按钮大小一致
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = warehouse.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = textColor // 根据背景颜色对比度自动切换
                )
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
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = ColorHelpers.getGroup3CardBgColor(),
            tonalElevation = 8.dp
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    showMenu = false
                    onEditWarehouse(warehouse)
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    showMenu = false
                    onDeleteWarehouse(warehouse)
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }
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
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
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
                    onClick = { onSubWarehouseClick(subWarehouse) },
                    onEditWarehouse = onEditWarehouse,
                    onDeleteWarehouse = onDeleteWarehouse
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
                    val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                    val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                    Box(
                        modifier = Modifier
                            .size(40.dp) // 与子容器大小一致
                            .clip(CircleShape)
                            .background(backgroundColor), // 与子容器颜色一致
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            Icons.Default.Add,
                            contentDescription = "添加子容器",
                            tint = iconColor, // 根据背景颜色对比度自动切换
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "添加容器",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
            }
        }
    }
}

/**
 * 右下物品列表项（滑动展开操作按钮）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemListRow(
    item: Item,
    onClick: () -> Unit,
    onEditItem: (Long) -> Unit = {},
    onAddToShoppingCart: (Item) -> Unit = {},
    onDeleteItem: (Item) -> Unit = {},
    onAddAlert: (Item) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonWidthDp = 38.dp
    val buttonCount = 4
    val maxOffsetPx: Float = remember(density) {
        -with(density) { (buttonWidthDp * buttonCount).toPx() }
    }
    // 滑动偏移状态（像素）
    var offsetX by remember { mutableStateOf(0f) }
    
    // 动画偏移
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "swipe"
    )
    
    // 使用 BoxWithConstraints 来限制滑动范围，不超出父容器
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp) // 与其他卡片对齐
            .clipToBounds() // 防止滑出容器区域（左侧保持对齐）
    ) {
        val containerWidth = maxWidth
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds() // 再裁剪一次，确保卡片不超出自身容器
        ) {
            // 背景操作按钮 - 使用 matchParentSize 来匹配卡片高度
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(40.dp) // 固定高度
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp), // 更紧凑，仅留极小间距
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val circleSize = 30.dp
                    val iconSize = 16.dp

                    @Composable
                    fun ActionButton(
                        color: Color,
                        icon: ImageVector,
                        contentDescription: String,
                        onClick: () -> Unit
                    ) {
                        Box(
                            modifier = Modifier
                                .width(buttonWidthDp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(circleSize)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = contentDescription,
                                    tint = Color.White,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }

                    ActionButton(
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.Edit,
                        contentDescription = "编辑"
                    ) {
                        onEditItem(item.id); offsetX = 0f
                    }

                    ActionButton(
                        color = Color(0xFF2196F3),
                        icon = Icons.Default.ShoppingCart,
                        contentDescription = "购物车"
                    ) {
                        onAddToShoppingCart(item); offsetX = 0f
                    }

                    ActionButton(
                        color = Color(0xFFFF9800),
                        icon = Icons.Default.Notifications,
                        contentDescription = "提醒"
                    ) {
                        onAddAlert(item); offsetX = 0f
                    }

                    ActionButton(
                        color = Color(0xFFF44336),
                        icon = Icons.Default.Delete,
                        contentDescription = "删除"
                    ) {
                        onDeleteItem(item); offsetX = 0f
                    }
                }
            }
            
            // 前景物品卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // 判断是否展开（超过1/3就展开）
                                offsetX = if (offsetX < maxOffsetPx / 3f) {
                                    maxOffsetPx // 完全展开
                                } else {
                                    0f // 收起
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                // 只允许向左滑动，限制滑动范围
                                val newOffset = (offsetX + dragAmount).coerceIn(maxOffsetPx, 0f)
                                // 确保不会滑出左侧边界（留出12dp的margin）
                                offsetX = newOffset
                            }
                        )
                    }
                    .clickable {
                        if (offsetX < 0f) {
                            // 如果已展开，点击收起
                            offsetX = 0f
                        } else {
                            // 否则执行点击事件
                            onClick()
                        }
                    },
                shape = RoundedCornerShape(8.dp),
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
            val itemBackgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
            val itemTextColor = ColorHelpers.getContrastColor(itemBackgroundColor)
            Box(
                modifier = Modifier
                    .size(36.dp) // 减小：40dp → 36dp
                    .clip(CircleShape)
                    .background(itemBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = itemTextColor // 根据背景颜色对比度自动切换
                )
            }
            
            // 中间物品信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp) // 减小间距
            ) {
                // 物品名称（粗体）
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    maxLines = 2, // 允许显示2行
                    overflow = TextOverflow.Ellipsis
                )
                
                // 物品描述
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.labelSmall, // 减小字体
                        color = ColorHelpers.getGroup4TextColor(0.7f),
                        maxLines = 2, // 允许显示2行
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 标签显示
                val isExpired = item.expiryDate?.let { it.before(Date()) } ?: false
                val allTagsToShow = if (isExpired) {
                    item.tags + "过期"
                } else {
                    item.tags
                }
                
                if (allTagsToShow.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(allTagsToShow.size) { index ->
                            val tag = allTagsToShow[index]
                            // 根据标签类型和索引分配不同浅颜色背景
                            val bgColor = when {
                                tag == "正常" -> Color(0xFFE8F5E9) // 浅绿
                                tag == "损坏" -> Color(0xFFFFF3E0) // 浅橙
                                tag == "遗失" -> Color(0xFFFFEBEE) // 浅红
                                tag == "过期" -> Color(0xFFF3E5F5) // 浅紫
                                else -> {
                                    // 自定义标签使用循环颜色（柔和的浅色）
                                    val colors = listOf(
                                        Color(0xFFE3F2FD), // 浅蓝
                                        Color(0xFFF1F8E9), // 浅绿
                                        Color(0xFFFFF9C4), // 浅黄
                                        Color(0xFFFCE4EC), // 浅粉
                                        Color(0xFFE0F2F1), // 浅青
                                        Color(0xFFF3E5F5), // 浅紫
                                        Color(0xFFFFE0B2)  // 浅橙
                                    )
                                    colors[index % colors.size]
                                }
                            }
                            
                            val displayTag = when (tag) {
                                "正常" -> stringResource(R.string.status_normal)
                                "损坏" -> stringResource(R.string.status_damaged)
                                "遗失" -> stringResource(R.string.status_lost)
                                "过期" -> stringResource(R.string.status_expired)
                                else -> tag
                            }
                            
                            // 使用 AssistChip 显示标签（透明背景，使用默认边框）
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        displayTag, 
                                        color = ColorHelpers.getGroup4TextColor(),
                                        fontSize = 9.sp
                                    ) 
                                },
                                enabled = false,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = ColorHelpers.getGroup4TextColor(),
                                    disabledContainerColor = Color.Transparent,
                                    disabledLabelColor = ColorHelpers.getGroup4TextColor()
                                )
                            )
                        }
                    }
                }
                
                // 过期日期或条码信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.expiryDate != null) {
                        val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
                        val dateStr = remember(item.expiryDate) { dateFormat.format(item.expiryDate) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                        contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = ColorHelpers.getGroup4IconColor(0.6f)
                    )
                    Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = ColorHelpers.getGroup4TextColor(0.6f)
                            )
                        }
                    }
                    
                    if (item.barcode != null && item.barcode.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = ColorHelpers.getGroup4IconColor(0.6f)
                            )
                            Text(
                                text = item.barcode.take(8), // 只显示前8位
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = ColorHelpers.getGroup4TextColor(0.6f)
                            )
                        }
                    }
                }
            }
            
            // 右侧数量和价格信息
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 价格显示
                if (item.price != null) {
                    Text(
                        text = stringResource(R.string.price_with_value, item.price),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 数量显示
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
        // 结束 Card 内容
        }
        // 结束内层 Box
        }
    // 结束 BoxWithConstraints
    }
}

/**
 * 右下物品列表区域
 */
@Composable
fun ItemListSection(
    items: List<Item>,
    onEditItem: (Long) -> Unit,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    alertSettingsManager: AlertSettingsManager? = null,
    onDeleteItem: (Item) -> Unit = {},
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
                    onClick = { onEditItem(item.id) },
                    onEditItem = onEditItem,
                    onAddToShoppingCart = { item ->
                        shoppingItemViewModel?.let { vm ->
                            val shoppingItem = com.example.itemremindertool.data.model.ShoppingItem(
                                name = item.name,
                                description = item.description,
                                quantity = item.quantity,
                                priority = com.example.itemremindertool.data.model.Priority.MEDIUM
                            )
                            vm.insertShoppingItem(shoppingItem)
                        }
                    },
                    onDeleteItem = { toDelete ->
                        if (onDeleteItem != {}) {
                            onDeleteItem(toDelete)
                        } else {
                            itemViewModel?.deleteItem(toDelete)
                        }
                    },
                    onAddAlert = { _ -> /* TODO: 接入提醒设置 */ }
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
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    onWarehouseSelect: (Warehouse) -> Unit,
    onHomeClick: () -> Unit, // 新增：点击首页的回调
    onSubWarehouseClick: (Warehouse) -> Unit,
    onAddWarehouse: () -> Unit,
    onAddChildWarehouse: (Long) -> Unit,
    onEditWarehouse: (Long) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    onEditItem: (Long) -> Unit,
    onDeleteItem: (Item) -> Unit = {},
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
            onAddWarehouse = onAddWarehouse,
            onEditWarehouse = { warehouse -> onEditWarehouse(warehouse.id) },
            onDeleteWarehouse = onDeleteWarehouse
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
                        warehousePath = displayPath,
                        onNavigateToWarehouse = { warehouse ->
                            // 点击容器，导航到该容器
                            onWarehouseSelect(warehouse)
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
                        onAddSubWarehouse = { onAddChildWarehouse(selectedWarehouseId) },
                        onEditWarehouse = { warehouse -> onEditWarehouse(warehouse.id) },
                        onDeleteWarehouse = onDeleteWarehouse
                    )
                }
                
                // 右下：物品列表
                ItemListSection(
                    items = warehouseItems,
                    onEditItem = onEditItem,
                    shoppingItemViewModel = shoppingItemViewModel,
                    itemViewModel = itemViewModel,
                    alertSettingsManager = alertSettingsManager,
                    onDeleteItem = onDeleteItem,
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
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val textColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(40.dp) // 圆形大小
                        .clip(CircleShape)
                        .background(backgroundColor), // 使用不同的背景色以区分
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = totalWarehouses.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor // 根据背景颜色对比度自动切换
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
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val textColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(40.dp) // 圆形大小
                        .clip(CircleShape)
                        .background(backgroundColor), // 使用不同的背景色以区分
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = totalItems.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor // 根据背景颜色对比度自动切换
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
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val textColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(40.dp) // 圆形大小
                        .clip(CircleShape)
                        .background(backgroundColor), // 使用不同的背景色以区分
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shoppingItemsCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor // 根据背景颜色对比度自动切换
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
            val alertBackgroundColor = ColorHelpers.getGroup5AlertCardColor()
            val alertIconColor = ColorHelpers.getContrastColor(alertBackgroundColor)
            Box(
                modifier = Modifier
                    .size(36.dp) // 减小：40dp → 36dp
                    .clip(CircleShape)
                    .background(alertBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = alertIconColor, // 根据背景颜色对比度自动切换
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
 * 显示容器路径，一行并排显示多个容器名称，支持左右滑动
 */
@Composable
fun WarehouseLevelHeader(
    warehousePath: List<Warehouse>,
    onNavigateToWarehouse: (Warehouse) -> Unit,
    modifier: Modifier = Modifier
) {
    if (warehousePath.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // 使用 LazyRow 实现横向滚动
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(warehousePath.size) { index ->
                val warehouse = warehousePath[index]
                val isLast = index == warehousePath.size - 1
                
                // 容器名称（可点击）
                Text(
                    text = warehouse.name.ifEmpty { "未命名容器" },
                    style = if (isLast) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) {
                        ColorHelpers.getGroup4TextColor()
                    } else {
                        ColorHelpers.getGroup4TextColor(0.7f)
                    },
                    modifier = Modifier
                        .clickable { onNavigateToWarehouse(warehouse) }
                        .padding(vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 如果不是最后一个，显示箭头分隔符
                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup4IconColor(0.5f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
