package com.example.itemremindertool.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.itemremindertool.ui.components.AppFloatingActionButton
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.components.ButtonAutoSizeText
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDetailScreen(
    warehouseUuid: String,
    warehouseViewModel: WarehouseViewModel,
    itemViewModel: ItemViewModel,
    shoppingItemViewModel: ShoppingItemViewModel,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    onAddItem: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onViewItem: (String) -> Unit = {},
    onAddChildWarehouse: (String) -> Unit,
    onEditWarehouse: (String) -> Unit,
    onDeleteWarehouse: (com.example.itemremindertool.data.model.Warehouse) -> Unit,
    onNavigateToWarehouseItemsTab: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToParentWarehouse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultSuffix = stringResource(R.string.warehouse_items_suffix)

    LaunchedEffect(warehouseUuid) {
        accessHistoryManager.recordAccess(warehouseUuid)
    }

    LaunchedEffect(warehouseUuid) {
        warehouseViewModel.loadWarehouse(warehouseUuid)
        warehouseViewModel.loadWarehouseItems(warehouseUuid)
    }

    LaunchedEffect(defaultSuffix) {
        warehouseViewModel.migrateWarehouseSuffixIfNeeded(defaultSuffix)
    }

    val warehouseItemsState by warehouseViewModel.uiState.collectAsState()
    val warehouse = warehouseItemsState.selectedWarehouse
    val warehousePath = warehouseItemsState.warehousePath
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    val allWarehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())

    val parentWarehouseUuid = remember(warehousePath) {
        if (warehousePath.size > 1) warehousePath[warehousePath.size - 2].uuid else null
    }

    val warehouseItemCounts = remember(allWarehouses, items) {
        allWarehouses.associate { w -> w.uuid to items.count { it.warehouseUuid == w.uuid } }
    }
    val currentItemCount = warehouseItemCounts[warehouseUuid] ?: 0
    val hasCapacityLimit = warehouse?.capacity != null
    val isCapacityFull = hasCapacityLimit && currentItemCount >= (warehouse?.capacity ?: 0)
    
    // 顶部菜单状态
    var showTopMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
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
                    Text(warehouse?.name ?: stringResource(R.string.warehouse_items_title))
                },
                navigationIcon = {
                    // 如果有父容器，显示返回按钮；否则不显示
                    if (parentWarehouseUuid != null) {
                        IconButton(onClick = {
                            onNavigateToParentWarehouse(parentWarehouseUuid)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    } else {
                        // 没有父容器时，也显示返回按钮用于返回首页
                        IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                                    warehouse?.let { onEditWarehouse(it.uuid) }
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
                                        stringResource(R.string.warehouse_settings),
                                        color = ColorHelpers.getGroup4TextColor(),
                                        maxLines = 2
                                    ) 
                                },
                                onClick = {
                                    showTopMenu = false
                                    showSettingsDialog = true
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Settings, 
                                        null,
                                        tint = ColorHelpers.getGroup4IconColor()
                                    ) 
                                },
                                modifier = Modifier.heightIn(min = 36.dp)
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
                    
                    if (showSettingsDialog && warehouse != null) {
                        var suffixInput by remember(warehouse.uuid) {
                            mutableStateOf(warehouse.itemsSuffix ?: "")
                        }
                        var hideUseButton by remember(warehouse.uuid) {
                            mutableStateOf(warehouse.hideUseButton)
                        }
                        var hideDetailsButton by remember(warehouse.uuid) {
                            mutableStateOf(warehouse.hideDetailsButton)
                        }
                        var hideQuantity by remember(warehouse.uuid) {
                            mutableStateOf(warehouse.hideQuantity)
                        }
                        var hideQuantitySlider by remember(warehouse.uuid) {
                            mutableStateOf(warehouse.hideQuantitySlider)
                        }
                        ModernSettingsDialog(
                            title = stringResource(R.string.warehouse_settings),
                            icon = Icons.Default.Settings,
                            onDismiss = { showSettingsDialog = false },
                            onConfirm = {
                                val updatedWarehouse = warehouse.copy(
                                    itemsSuffix = suffixInput.trim().ifBlank { null },
                                    hideUseButton = hideUseButton,
                                    hideDetailsButton = hideDetailsButton,
                                    hideQuantity = hideQuantity,
                                    hideQuantitySlider = hideQuantitySlider
                                )
                                warehouseViewModel.updateWarehouse(updatedWarehouse)
                                warehouseViewModel.loadWarehouse(warehouse.uuid)
                                showSettingsDialog = false
                            }
                        ) {
                            OutlinedTextField(
                                value = suffixInput,
                                onValueChange = { suffixInput = it },
                                label = { Text(stringResource(R.string.warehouse_items_suffix)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    Text(
                                        text = stringResource(R.string.custom_warehouse_items_suffix_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorHelpers.getGroup4TextColor(0.7f)
                                    )
                                }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.hide_use_button)) },
                                trailingContent = {
                                    Switch(
                                        checked = hideUseButton,
                                        onCheckedChange = { hideUseButton = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.hide_details_button)) },
                                trailingContent = {
                                    Switch(
                                        checked = hideDetailsButton,
                                        onCheckedChange = { hideDetailsButton = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.hide_quantity_display)) },
                                trailingContent = {
                                    Switch(
                                        checked = hideQuantity,
                                        onCheckedChange = { hideQuantity = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.hide_quantity_slider)) },
                                trailingContent = {
                                    Switch(
                                        checked = hideQuantitySlider,
                                        onCheckedChange = { hideQuantitySlider = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
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
                    AppFloatingActionButton(
                        onClick = {
                            if (isCapacityFull) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.capacity_limit_reached),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                fabExpanded = false
                                onAddItem(warehouseUuid)
                            }
                        },
                        backgroundColor = ColorHelpers.getGroup5FabColor(),
                        modifier = Modifier.size(UIConstants.FAB_SIZE)
                    ) {
                        Icon(
                            Icons.Default.Category,
                            stringResource(R.string.add_item)
                        )
                    }
                    
                    // 添加子容器按钮
                    val path = warehouseItemsState.warehousePath
                    val currentDepth = if (path.isNotEmpty()) path.size else 1
                    val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                    val canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
                    val unlimitedContainers = prefs.getBoolean("unlimited_containers", false) && canAccessPremiumFeatures
                    
                    if (unlimitedContainers || currentDepth < 5) {
                        AppFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                onAddChildWarehouse(warehouseUuid)
                            },
                            backgroundColor = ColorHelpers.getGroup5FabColor(),
                            modifier = Modifier.size(UIConstants.FAB_SIZE)
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                stringResource(R.string.add_warehouse)
                            )
                        }
                    }
                }
                
                // 主 FAB 按钮（最下方）
                AppFloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    backgroundColor = ColorHelpers.getGroup5FabColor(),
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
                itemCount = warehouseItemCounts[warehouse.uuid] ?: 0,
                childWarehouses = warehouseItemsState.childWarehouses,
                warehouseItemCounts = warehouseItemCounts,
                allWarehouses = allWarehouses,
                allItems = items,
                warehouseViewModel = warehouseViewModel,
                itemViewModel = itemViewModel,
                shoppingItemViewModel = shoppingItemViewModel,
                onWarehouseClick = { childWarehouseUuid ->
                    // 导航到子容器详情页面
                    onNavigateToWarehouseItemsTab(childWarehouseUuid)
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
        AppDialogLayout(
            title = stringResource(R.string.delete_warehouse_title),
            icon = Icons.Default.Delete,
            onDismiss = { showDeleteDialog = false },
            footer = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    ButtonAutoSizeText(
                        text = stringResource(R.string.cancel)
                    )
                }
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteWarehouse(warehouse)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    ButtonAutoSizeText(
                        text = stringResource(R.string.delete)
                    )
                }
            }
        ) {
            Text(
                text = stringResource(R.string.delete_warehouse_confirm, warehouse.name),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
        }
    }
}


