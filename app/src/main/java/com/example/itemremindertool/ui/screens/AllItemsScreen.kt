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
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllItemsScreen(
    itemViewModel: ItemViewModel,
    shoppingItemViewModel: ShoppingItemViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by itemViewModel.items.collectAsState(initial = emptyList())

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Inventory,
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
            modifier = Modifier.fillMaxSize(),
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
                    }
                )
            }
        }
    }
}

