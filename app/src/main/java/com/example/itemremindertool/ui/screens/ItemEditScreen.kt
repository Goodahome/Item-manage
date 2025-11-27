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
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    itemId: Long?,
    viewModel: ItemViewModel,
    categories: List<com.example.itemremindertool.data.model.Category>,
    warehouses: List<com.example.itemremindertool.data.model.Warehouse>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedWarehouseId by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf(ItemStatus.NORMAL) }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var barcode by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<Date?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.loadItem(itemId)
        }
    }

    val selectedItem by viewModel.uiState.collectAsState()
    LaunchedEffect(selectedItem.selectedItem) {
        selectedItem.selectedItem?.let { item ->
            name = item.name
            description = item.description
            selectedCategoryId = item.categoryId
            selectedWarehouseId = item.warehouseId
            status = item.status
            price = item.price?.toString() ?: ""
            quantity = item.quantity.toString()
            barcode = item.barcode ?: ""
            expiryDate = item.expiryDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "添加物品" else "编辑物品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val item = Item(
                                id = itemId ?: 0,
                                name = name,
                                description = description,
                                categoryId = selectedCategoryId,
                                warehouseId = selectedWarehouseId,
                                status = status,
                                price = price.toDoubleOrNull(),
                                quantity = quantity.toIntOrNull() ?: 1,
                                barcode = barcode.ifEmpty { null },
                                expiryDate = expiryDate
                            )
                            if (itemId == null) {
                                viewModel.insertItem(item)
                            } else {
                                viewModel.updateItem(item.copy(id = itemId))
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

            // 分类选择
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = categories.find { it.id == selectedCategoryId }?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("分类") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("无分类") },
                        onClick = {
                            selectedCategoryId = null
                            expandedCategory = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategoryId = category.id
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // 仓库选择
            var expandedWarehouse by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWarehouse,
                onExpandedChange = { expandedWarehouse = !expandedWarehouse }
            ) {
                OutlinedTextField(
                    value = warehouses.find { it.id == selectedWarehouseId }?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("仓库") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWarehouse) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedWarehouse,
                    onDismissRequest = { expandedWarehouse = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("无仓库") },
                        onClick = {
                            selectedWarehouseId = null
                            expandedWarehouse = false
                        }
                    )
                    warehouses.forEach { warehouse ->
                        DropdownMenuItem(
                            text = { Text(warehouse.name) },
                            onClick = {
                                selectedWarehouseId = warehouse.id
                                expandedWarehouse = false
                            }
                        )
                    }
                }
            }

            // 状态选择
            Text("状态", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemStatus.values().forEach { itemStatus ->
                    FilterChip(
                        selected = status == itemStatus,
                        onClick = { status = itemStatus },
                        label = { Text(getStatusLabel(itemStatus)) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("价格") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    leadingIcon = { Text("¥") }
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("数量") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("条形码") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.QrCode, null) }
            )

            // 到期日期
            OutlinedTextField(
                value = expiryDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("到期日期") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, null)
                    }
                }
            )

            if (showDatePicker) {
                // 简单的日期选择器实现
                // 实际应用中可以使用更复杂的日期选择器
            }
        }
    }
}

fun getStatusLabel(status: ItemStatus): String {
    return when (status) {
        ItemStatus.NORMAL -> "正常"
        ItemStatus.DAMAGED -> "损坏"
        ItemStatus.LOST -> "遗失"
        ItemStatus.EXPIRED -> "过期"
    }
}

