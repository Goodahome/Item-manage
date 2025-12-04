package com.example.itemremindertool.ui.screens
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.itemremindertool.ui.screens.ItemSearchByImageDialog
import com.example.itemremindertool.ui.theme.LocalAppSettings
import com.example.itemremindertool.R

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    itemViewModel: ItemViewModel,
    warehouseViewModel: com.example.itemremindertool.ui.viewmodel.WarehouseViewModel,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel,
    onAddItem: (Long?) -> Unit, // 传递当前选中的容器ID
    onEditItem: (Long) -> Unit,
    onScanBarcode: () -> Unit,
    onItemRecognition: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToItems: () -> Unit = {},
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

    // 容器物品数量映射（包含所有容器，不仅仅是顶层容器）
    var warehouseItemCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }

    // Tab设置：只有首页和所有物品两个标签页
    val homeTabTitle = stringResource(R.string.nav_home)
    val allItemsTabTitle = stringResource(R.string.nav_all_items)

    val tabs = listOf(homeTabTitle, allItemsTabTitle)

    // 必须在这里声明！！
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    
    // 移除自动导航逻辑，用户应该主动点击容器卡片来导航
    // 这样可以避免返回时意外导航
    
    // 计算每个容器的物品数量（包含所有容器，不仅仅是顶层容器）
    LaunchedEffect(allWarehouses, items) {
        val counts = allWarehouses.associate { warehouse ->
            warehouse.id to items.count { it.warehouseId == warehouse.id }
        }
        warehouseItemCounts = counts
    }
    
    
    // 搜索相关状态
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Item>>(emptyList()) }
    var showImageSearchDialog by remember { mutableStateOf(false) }
    
    // 名称搜索
    val searchResultsFlow = if (searchQuery.isNotEmpty()) {
        itemViewModel.searchItemsByName(searchQuery)
    } else {
        kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            searchResultsFlow.collect { results ->
                searchResults = results
            }
        } else {
            searchResults = emptyList()
        }
    }
    

    val context = LocalContext.current
    
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
                    if (pagerState.currentPage == 0) {
                        // 首页：显示查找按钮
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Default.Search, stringResource(R.string.search_items))
                        }
                    } else if (pagerState.currentPage == 1) {
                        // 所有物品页面：显示扫码、识别和添加按钮
                        IconButton(onClick = onScanBarcode) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                stringResource(R.string.barcode_scanner)
                            )
                        }
                        IconButton(onClick = onItemRecognition) {
                            Icon(
                                Icons.Default.ImageSearch,
                                stringResource(R.string.item_recognition)
                            )
                        }
                        IconButton(onClick = { onAddItem(null) }) {
                            Icon(Icons.Default.Add, stringResource(R.string.add_item))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
    Column(
        modifier = modifier
            .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab切换 - 移到标题下方（仅在未选中容器或选中容器时都显示）
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }
            
            // 搜索框（在首页且显示搜索时显示，且未选中容器）
            if (pagerState.currentPage == 0 && showSearch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, stringResource(R.string.cancel))
                                }
                            }
                        },
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            showImageSearchDialog = true
                        }
                    ) {
                        Icon(Icons.Default.ImageSearch, stringResource(R.string.search_by_image))
                    }
                }
            }
            
            // 图片识别搜索对话框
            if (showImageSearchDialog) {
                ItemSearchByImageDialog(
                    itemViewModel = itemViewModel,
                    onItemFound = { item ->
                        onEditItem(item.id)
                        showImageSearchDialog = false
                    },
                    onDismiss = { showImageSearchDialog = false }
                )
            }
            
            // 使用HorizontalPager支持左右滑动
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // 首页 - 显示统计卡片、容器卡片或搜索结果
                        if (showSearch && searchQuery.isNotEmpty()) {
                            // 显示搜索结果
                            if (searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.no_matching_items))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
                                    items(searchResults) { item ->
                                        ItemCard(
                                            item = item,
                                            onEdit = { onEditItem(item.id) },
                                            onDelete = { itemViewModel.deleteItem(item) },
                                            onAddToShoppingCart = {
                                                // 将物品添加到购物篮（只使用名称和图片URI，不携带其他数据）
                                                val shoppingItem =
                                                    com.example.itemremindertool.data.model.ShoppingItem(
                                                    name = item.name,
                                                    description = "",
                                                    quantity = 1,
                                                    isCompleted = false,
                                                    priority = com.example.itemremindertool.data.model.Priority.MEDIUM,
                                                    createdAt = java.util.Date(),
                                                    imageUri = item.imageUri // 传递图片URI
                                                )
                                                shoppingItemViewModel.insertShoppingItem(
                                                    shoppingItem
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // 显示统计卡片和容器卡片
                            val totalItemsTitle = stringResource(R.string.total_items)
                            val numWarehousesTitle = stringResource(R.string.number_of_warehouses)
                            val shoppingItemsTitle = stringResource(R.string.shopping_items)
                            val statCardItems = remember(
                                stats,
                                totalItemsTitle,
                                numWarehousesTitle,
                                shoppingItemsTitle
                            ) {
                                listOf(
                                    StatCardData(
                                        title = totalItemsTitle,
                                        value = stats.totalItems.toString(),
                                        icon = Icons.Default.Inventory,
                                        color = Color(0xFF1976D2),
                                        backgroundColor = Color(0xFFE3F2FD),
                                        onClick = onNavigateToItems
                                    ),
                                    StatCardData(
                                        title = numWarehousesTitle,
                                        value = stats.totalWarehouses.toString(),
                                        icon = Icons.Default.Warehouse,
                                        color = Color(0xFF5D4037),
                                        backgroundColor = Color(0xFFEFEBE9),
                                        onClick = onNavigateToWarehouses
                                    ),
                                    StatCardData(
                                        title = shoppingItemsTitle,
                                        value = stats.activeShoppingItems.toString(),
                                        icon = Icons.Default.ShoppingCart,
                                        color = Color(0xFFE91E63),
                                        backgroundColor = Color(0xFFFCE4EC),
                                        onClick = onNavigateToShoppingList
                                    )
                                )
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // 第一部分：固定的统计卡片
                                items(
                                    items = statCardItems,
                                    key = { it.title.hashCode() }
                                ) { statCard ->
                                    StatCard(statCard)
                                }

                                // 第二部分：动态容器卡片
                                items(
                                    items = warehouses,
                                    key = { it.id }
                                ) { warehouse ->
                                    WarehouseStatCard(
                                        warehouse = warehouse,
                                        itemCount = warehouseItemCounts[warehouse.id] ?: 0,
                                        onClick = {
                                            onNavigateToWarehouseItemsTab(warehouse.id)
                                        }
                                    )
                                }
                            }
                        }
                        }
                    1 -> {
                        // 所有物品页面：使用独立的 AllItemsScreen
                        AllItemsScreen(
                            itemViewModel = itemViewModel,
                            shoppingItemViewModel = shoppingItemViewModel,
                            warehouseViewModel = warehouseViewModel,
                            onAddItem = { onAddItem(null) },
                            onEditItem = onEditItem
                        )
                    }
                    }
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
    onWarehouseClick: (Long) -> Unit = {},
    onEditWarehouse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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

        // 子容器网格显示（使用 FlowRow 代替 LazyVerticalGrid，避免嵌套滚动问题）
        if (childWarehouses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.child_containers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // 使用普通的 Column + Row 布局，避免嵌套 Lazy 组件
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                childWarehouses.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { childWarehouse ->
                            val childItemCount = warehouseItemCounts[childWarehouse.id] ?: 0
                            Box(modifier = Modifier.weight(1f)) {
                                ChildWarehouseCard(
                                    warehouse = childWarehouse,
                                    itemCount = childItemCount,
                                    onClick = { onWarehouseClick(childWarehouse.id) }
                                )
                            }
                        }
                        // 如果行不满，添加空白占位
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 子容器卡片（方形样式）
 */
@Composable
fun ChildWarehouseCard(
    warehouse: Warehouse,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = null,
                tint = Color(0xFF5D4037),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.items_count, itemCount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D4037).copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}


