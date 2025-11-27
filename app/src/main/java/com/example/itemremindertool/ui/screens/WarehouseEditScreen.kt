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
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseEditScreen(
    warehouseId: Long?,
    viewModel: WarehouseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    LaunchedEffect(warehouseId) {
        if (warehouseId != null) {
            viewModel.loadWarehouse(warehouseId)
        }
    }

    val selectedWarehouse by viewModel.uiState.collectAsState()
    LaunchedEffect(selectedWarehouse.selectedWarehouse) {
        selectedWarehouse.selectedWarehouse?.let { warehouse ->
            name = warehouse.name
            description = warehouse.description
            location = warehouse.location
            capacity = warehouse.capacity?.toString() ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (warehouseId == null) "添加仓库" else "编辑仓库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val warehouse = Warehouse(
                                id = warehouseId ?: 0,
                                name = name,
                                description = description,
                                location = location,
                                capacity = capacity.toIntOrNull()
                            )
                            if (warehouseId == null) {
                                viewModel.insertWarehouse(warehouse)
                            } else {
                                viewModel.updateWarehouse(warehouse.copy(id = warehouseId))
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
                label = { Text("仓库名称 *") },
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
                value = location,
                onValueChange = { location = it },
                label = { Text("位置") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )

            OutlinedTextField(
                value = capacity,
                onValueChange = { capacity = it },
                label = { Text("容量限制（可选）") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Storage, null) }
            )
        }
    }
}

