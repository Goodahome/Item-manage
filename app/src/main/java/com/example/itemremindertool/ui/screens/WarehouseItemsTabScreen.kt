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
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseItemsTabScreen(
    warehouseId: String,
    warehouseViewModel: WarehouseViewModel,
    itemViewModel: ItemViewModel,
    shoppingItemViewModel: ShoppingItemViewModel,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 加载容器物品
    LaunchedEffect(warehouseId) {
        warehouseViewModel.loadWarehouseItems(warehouseId)
    }

    val warehouseItemsState by warehouseViewModel.uiState.collectAsState()
    val warehouseItems = warehouseItemsState.warehouseItems

    if (warehouseItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                    stringResource(R.string.warehouse_empty_hint),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(warehouseItems, key = { it.uuid }) { item ->
                ItemCard(
                    item = item,
                    onEdit = { onEditItem(item.uuid) },
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
                    }
                )
            }
        }
    }
}
