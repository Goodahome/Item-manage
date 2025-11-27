package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Priority
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemEditScreen(
    itemId: Long?,
    viewModel: ShoppingItemViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.loadShoppingItem(itemId)
        }
    }

    val selectedItem by viewModel.uiState.collectAsState()
    LaunchedEffect(selectedItem.selectedItem) {
        selectedItem.selectedItem?.let { item ->
            name = item.name
            description = item.description
            quantity = item.quantity.toString()
            priority = item.priority
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "添加购物项" else "编辑购物项") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val item = ShoppingItem(
                                id = itemId ?: 0,
                                name = name,
                                description = description,
                                quantity = quantity.toIntOrNull() ?: 1,
                                priority = priority
                            )
                            if (itemId == null) {
                                viewModel.insertShoppingItem(item)
                            } else {
                                viewModel.updateShoppingItem(item.copy(id = itemId))
                            }
                            onNavigateBack()
                        }
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("物品名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("数量") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Text("优先级", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.values().forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(getPriorityLabel(p)) }
                    )
                }
            }
        }
    }
}

