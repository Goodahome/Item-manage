package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.ui.screens.ItemCard
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseItemsScreen(
    warehouseId: Long,
    warehouseViewModel: WarehouseViewModel,
    itemViewModel: ItemViewModel,
    onEditItem: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(warehouseId) {
        warehouseViewModel.loadWarehouseItems(warehouseId)
    }

    val warehouseItems by warehouseViewModel.uiState.collectAsState()
    val warehouse by warehouseViewModel.uiState.collectAsState()
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<Item?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(warehouse.selectedWarehouse?.name ?: stringResource(R.string.warehouse_items_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (warehouseItems.warehouseItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        stringResource(R.string.warehouse_empty_hint),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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
                items(warehouseItems.warehouseItems, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onEdit = { onEditItem(item.id) },
                        onDelete = { itemViewModel.deleteItem(item) },
                        onMoveToContainer = {
                            itemToMove = item
                            showMoveDialog = true
                        }
                    )
                }
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
                // 重新加载容器物品列表
                warehouseViewModel.loadWarehouseItems(warehouseId)
                showMoveDialog = false
                itemToMove = null
            }
        )
    }
}

