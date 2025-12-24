package com.example.itemremindertool.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.ui.components.WarehouseQRCodeDialog
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDetailScreen(
    warehouseId: Long,
    warehouseViewModel: WarehouseViewModel,
    itemViewModel: ItemViewModel,
    shoppingItemViewModel: ShoppingItemViewModel,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    onAddItem: (Long) -> Unit,
    onEditItem: (Long) -> Unit,
    onViewItem: (Long) -> Unit = {},
    onAddChildWarehouse: (Long) -> Unit,
    onEditWarehouse: (Long) -> Unit,
    onDeleteWarehouse: (com.example.itemremindertool.data.model.Warehouse) -> Unit,
    onNavigateToWarehouseItemsTab: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToParentWarehouse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 获取当前布局风格
    val homeLayoutStyle = rememberHomeLayoutStyle()
    
    // 记录访问历史
    LaunchedEffect(warehouseId) {
        accessHistoryManager.recordAccess(warehouseId)
    }
    
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
    
    // 顶部菜单状态
    var showTopMenu by remember { mutableStateOf(false) }
    
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // FAB 展开菜单状态
    var fabExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (fabExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300), label = ""
    )
    
    Scaffold(
        topBar = {
            var showQRCodeDialog by remember { mutableStateOf(false) }
            GradientTopAppBar(
                title = {
                    // 经典模式下显示面包屑导航，Discord模式下只显示当前容器名称
                    if (homeLayoutStyle.value == HomeLayoutStyle.CLASSIC && warehousePath.isNotEmpty()) {
                        BreadcrumbNavigation(
                            path = warehousePath,
                            onWarehouseClick = { targetWarehouseId ->
                                onNavigateToParentWarehouse(targetWarehouseId)
                            }
                        )
                    } else {
                        Text(warehouse?.name ?: stringResource(R.string.warehouse_items_title))
                    }
                },
                navigationIcon = {
                    // 如果有父容器，显示返回按钮；否则不显示
                    if (parentWarehouseId != null) {
                        IconButton(onClick = {
                            onNavigateToParentWarehouse(parentWarehouseId)
                        }) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                        }
                    } else {
                        // 没有父容器时，也显示返回按钮用于返回首页
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    // 三个点的操作菜单
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.more_options))
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            containerColor = ColorHelpers.getGroup3CardBgColor()
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.edit),
                                        color = ColorHelpers.getGroup4TextColor(),
                                        maxLines = 2 // 允许最多2行，支持文字换行
                                    ) 
                                },
                                onClick = {
                                    showTopMenu = false
                                    warehouse?.let { onEditWarehouse(it.id) }
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Edit, 
                                        null,
                                        tint = ColorHelpers.getGroup4IconColor()
                                    ) 
                                },
                                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.generate_qr_code),
                                        color = ColorHelpers.getGroup4TextColor(),
                                        maxLines = 2 // 允许最多2行，支持文字换行
                                    ) 
                                },
                                onClick = {
                                    showTopMenu = false
                                    showQRCodeDialog = true
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.QrCode, 
                                        null,
                                        tint = ColorHelpers.getGroup4IconColor()
                                    ) 
                                },
                                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.delete),
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 2 // 允许最多2行，支持文字换行
                                    ) 
                                },
                                onClick = {
                                    showTopMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Delete, 
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    ) 
                                },
                                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                            )
                        }
                    }
                    
                    // 二维码对话框
                    if (showQRCodeDialog && warehouse != null) {
                        WarehouseQRCodeDialog(
                            warehouse = warehouse,
                            onDismiss = { showQRCodeDialog = false }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // 展开的 FAB 菜单
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (fabExpanded) {
                    // 添加物品按钮（最上方）
                    FloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            onAddItem(warehouseId)
                        },
                        containerColor = ColorHelpers.getGroup5FabColor(),
                        modifier = Modifier.size(UIConstants.FAB_SIZE)
                    ) {
                        Icon(Icons.Default.Category, stringResource(R.string.add_item))
                    }
                    
                    // 添加子容器按钮
                    val path = warehouseItemsState.warehousePath
                    val currentDepth = if (path.isNotEmpty()) path.size else 1
                    val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                    val unlimitedContainers = prefs.getBoolean("unlimited_containers", false)
                    
                    if (unlimitedContainers || currentDepth < 5) {
                        FloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                onAddChildWarehouse(warehouseId)
                            },
                            containerColor = ColorHelpers.getGroup5FabColor(),
                            modifier = Modifier.size(UIConstants.FAB_SIZE)
                        ) {
                            Icon(Icons.Default.Inventory2, stringResource(R.string.add_warehouse))
                        }
                    }
                }
                
                // 主 FAB 按钮（最下方）
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = ColorHelpers.getGroup5FabColor(),
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (fabExpanded) stringResource(R.string.close) else stringResource(R.string.add),
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
        }
    ) { paddingValues ->
        // 容器信息页面（已整合容器物品列表）
        if (warehouse != null) {
            WarehouseInfoScreen(
                warehouse = warehouse,
                itemCount = warehouseItemCounts[warehouse.id] ?: 0,
                childWarehouses = warehouseItemsState.childWarehouses,
                warehouseItemCounts = warehouseItemCounts,
                allWarehouses = allWarehouses,
                allItems = items,
                warehouseViewModel = warehouseViewModel,
                itemViewModel = itemViewModel,
                shoppingItemViewModel = shoppingItemViewModel,
                onWarehouseClick = { childWarehouseId ->
                    // 导航到子容器详情页面
                    onNavigateToWarehouseItemsTab(childWarehouseId)
                },
                onEditWarehouse = { /* 已移到顶部菜单 */ },
                onEditItem = onEditItem,
                onViewItem = onViewItem,
                modifier = modifier.padding(paddingValues)
            )
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && warehouse != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_warehouse_title)) },
            text = { 
                Text(stringResource(R.string.delete_warehouse_confirm, warehouse.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteWarehouse(warehouse)
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                    tint = ColorHelpers.getGroup4IconColor(0.6f)
                )
            }
            
            // 容器名称（可点击）
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (index == path.size - 1) {
                    ColorHelpers.getGroup4TextColor()
                } else {
                    ColorHelpers.getGroup2SettingsBtnColor()
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

