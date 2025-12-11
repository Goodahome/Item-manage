package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Priority
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import com.example.itemremindertool.data.AlertSettingsManager
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllItemsScreen(
    itemViewModel: ItemViewModel,
    shoppingItemViewModel: ShoppingItemViewModel,
    warehouseViewModel: WarehouseViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    filterType: String? = null, // 筛选类型：expiring, low_stock, all_alerts, null表示所有物品
    modifier: Modifier = Modifier
) {
    val allItems by itemViewModel.items.collectAsState(initial = emptyList())
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<Item?>(null) }
    
    val context = LocalContext.current
    val alertSettingsManager = remember { AlertSettingsManager(context) }
    
    // 获取筛选类型字符串资源（在 remember 外部获取）
    val filterTypeExpiring = stringResource(R.string.filter_type_expiring)
    val filterTypeLowStock = stringResource(R.string.filter_type_low_stock)
    val filterTypeAllAlerts = stringResource(R.string.filter_type_all_alerts)
    
    // 根据筛选类型过滤物品
    val items = remember(allItems, filterType, alertSettingsManager) {
        if (filterType == null) {
            allItems
        } else {
            when (filterType) {
                filterTypeExpiring -> {
                    // 筛选即将到期的物品
                    val expiryReminderDays = alertSettingsManager.getExpiryReminderDays()
                    val calendar = Calendar.getInstance()
                    val currentTime = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, expiryReminderDays)
                    val reminderEndTime = calendar.timeInMillis
                    
                    allItems.filter { item ->
                        item.expiryDate != null &&
                        item.expiryDate.time >= currentTime &&
                        item.expiryDate.time <= reminderEndTime
                    }
                }
                filterTypeLowStock -> {
                    // 筛选库存不足的物品（只包含启用了库存提醒的物品）
                    val threshold = alertSettingsManager.getLowStockThreshold()
                    allItems.filter { item ->
                        item.enableStockAlert && item.quantity <= threshold
                    }
                }
                filterTypeAllAlerts -> {
                    // 筛选所有提醒物品（即将到期 + 库存不足）
                    val expiryReminderDays = alertSettingsManager.getExpiryReminderDays()
                    val threshold = alertSettingsManager.getLowStockThreshold()
                    val calendar = Calendar.getInstance()
                    val currentTime = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, expiryReminderDays)
                    val reminderEndTime = calendar.timeInMillis
                    
                    allItems.filter { item ->
                        val isExpiring = item.expiryDate != null &&
                                item.expiryDate.time >= currentTime &&
                                item.expiryDate.time <= reminderEndTime
                        val isLowStock = item.enableStockAlert && item.quantity <= threshold
                        isExpiring || isLowStock
                    }
                }
                else -> allItems
            }
        }
    }
    
    // 根据筛选类型设置标题
    val title = when (filterType) {
        filterTypeExpiring -> stringResource(R.string.expiring_items)
        filterTypeLowStock -> stringResource(R.string.low_stock_items)
        filterTypeAllAlerts -> stringResource(R.string.alert_items)
        else -> stringResource(R.string.nav_all_items)
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_item))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    stringResource(R.string.no_items),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Button(onClick = onAddItem) {
                    Text(stringResource(R.string.add_first_item))
                }
            }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onEdit = { onEditItem(item.id) },
                        onDelete = { itemViewModel.deleteItem(item) },
                        onAddToShoppingCart = {
                            val shoppingItem = ShoppingItem(
                                name = item.name,
                                description = "",
                                quantity = 1,
                                isCompleted = false,
                                priority = Priority.MEDIUM,
                                createdAt = Date(),
                                imageUri = item.imageUri
                            )
                            shoppingItemViewModel.insertShoppingItem(shoppingItem)
                        },
                        onMoveToContainer = {
                            itemToMove = item
                            showMoveDialog = true
                        }
                    )
                }
            }
        }
        
        // 移动物品对话框
        if (showMoveDialog && itemToMove != null) {
            MoveItemDialog(
                itemName = itemToMove!!.name,
                currentWarehouseId = itemToMove!!.warehouseId,
                warehouseViewModel = warehouseViewModel,
                onDismiss = {
                    showMoveDialog = false
                    itemToMove = null
                },
                onConfirm = { targetWarehouseId ->
                    val updatedItem = itemToMove!!.copy(
                        warehouseId = targetWarehouseId,
                        updatedAt = Date()
                    )
                    itemViewModel.updateItem(updatedItem)
                    showMoveDialog = false
                    itemToMove = null
                }
            )
        }
    }
}

