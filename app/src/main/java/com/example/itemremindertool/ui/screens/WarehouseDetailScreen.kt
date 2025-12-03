package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDetailScreen(
    warehouseId: Long,
    warehouseViewModel: WarehouseViewModel,
    itemViewModel: ItemViewModel,
    shoppingItemViewModel: ShoppingItemViewModel,
    onAddItem: (Long) -> Unit,
    onEditItem: (Long) -> Unit,
    onScanBarcode: () -> Unit,
    onItemRecognition: () -> Unit,
    onAddChildWarehouse: (Long) -> Unit,
    onNavigateToWarehouseItemsTab: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToParentWarehouse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 加载容器数据
    LaunchedEffect(warehouseId) {
        warehouseViewModel.loadWarehouse(warehouseId)
        warehouseViewModel.loadWarehouseItems(warehouseId)
    }

    val warehouseItemsState by warehouseViewModel.uiState.collectAsState()
    val warehouse = warehouseItemsState.selectedWarehouse
    val warehousePath = warehouseItemsState.warehousePath
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    val allWarehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
    
    // 获取父容器ID（路径中倒数第二个，如果存在）
    val parentWarehouseId = remember(warehousePath) {
        if (warehousePath.size > 1) {
            warehousePath[warehousePath.size - 2].id
        } else {
            null
        }
    }
    
    // 计算容器物品数量
    val warehouseItemCounts = remember(allWarehouses, items) {
        allWarehouses.associate { w ->
            w.id to items.count { it.warehouseId == w.id }
        }
    }
    
    // Tab设置：容器信息和容器物品两个标签页
    val infoTabTitle = stringResource(R.string.nav_warehouse_info)
    val itemsTabTitle = stringResource(R.string.nav_items)
    
    val tabs = listOf(infoTabTitle, itemsTabTitle)
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // 如果有路径，显示面包屑；否则显示容器名称
                    if (warehousePath.size > 1) {
                        BreadcrumbNavigation(
                            path = warehousePath,
                            onWarehouseClick = { clickedWarehouseId ->
                                if (clickedWarehouseId != warehouseId) {
                                    onNavigateToParentWarehouse(clickedWarehouseId)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(warehouse?.name ?: stringResource(R.string.warehouse_items_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // 如果有父容器，导航到父容器；否则使用默认返回
                        if (parentWarehouseId != null) {
                            onNavigateToParentWarehouse(parentWarehouseId)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (pagerState.currentPage == 0) {
                        // 容器信息页面：显示添加子容器按钮
                        val path = warehouseItemsState.warehousePath
                        val currentDepth = if (path.isNotEmpty()) path.size else 1
                        val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                        val unlimitedContainers = prefs.getBoolean("unlimited_containers", false)

                        if (unlimitedContainers || currentDepth < 5) {
                            IconButton(onClick = { onAddChildWarehouse(warehouseId) }) {
                                Icon(Icons.Default.Add, stringResource(R.string.add_warehouse))
                            }
                        }
                    } else if (pagerState.currentPage == 1) {
                        // 容器物品页面：显示扫码、识别和添加物品按钮
                        IconButton(onClick = onScanBarcode) {
                            Icon(Icons.Default.QrCodeScanner, stringResource(R.string.barcode_scanner))
                        }
                        IconButton(onClick = onItemRecognition) {
                            Icon(Icons.Default.ImageSearch, stringResource(R.string.item_recognition))
                        }
                        IconButton(onClick = { onAddItem(warehouseId) }) {
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
            // Tab切换
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
            
            // 使用HorizontalPager支持左右滑动
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // 容器信息页面
                        if (warehouse != null) {
                            WarehouseInfoScreen(
                                warehouse = warehouse,
                                itemCount = warehouseItemCounts[warehouse.id] ?: 0,
                                childWarehouses = warehouseItemsState.childWarehouses,
                                warehouseItemCounts = warehouseItemCounts,
                                onWarehouseClick = { childWarehouseId ->
                                    // 导航到子容器详情页面
                                    onNavigateToWarehouseItemsTab(childWarehouseId)
                                },
                                onEditWarehouse = { /* 可以编辑容器 */ }
                            )
                        }
                    }
                    1 -> {
                        // 容器物品页面
                        WarehouseItemsTabScreen(
                            warehouseId = warehouseId,
                            warehouseViewModel = warehouseViewModel,
                            itemViewModel = itemViewModel,
                            shoppingItemViewModel = shoppingItemViewModel,
                            onAddItem = { onAddItem(warehouseId) },
                            onEditItem = onEditItem
                        )
                    }
                }
            }
        }
    }
}

/**
 * 面包屑导航组件（显示在TopAppBar的title位置）
 */
@Composable
fun BreadcrumbNavigation(
    path: List<com.example.itemremindertool.data.model.Warehouse>,
    onWarehouseClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        path.forEachIndexed { index, warehouse ->
            if (index > 0) {
                // 分隔符
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(horizontal = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            // 容器名称（可点击）
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (index == path.size - 1) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = index < path.size - 1) {
                        onWarehouseClick(warehouse.id)
                    }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            )
        }
    }
}

