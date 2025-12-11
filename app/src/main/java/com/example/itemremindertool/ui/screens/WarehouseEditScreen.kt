package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseEditScreen(
    warehouseId: Long?,
    viewModel: WarehouseViewModel,
    onNavigateBack: () -> Unit,
    initialParentId: Long? = null, // 预设的父容器ID
    onSaveSuccess: ((Long?) -> Unit)? = null, // 保存成功后的回调，传递父容器ID
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(initialParentId) }
    var showParentDropdown by remember { mutableStateOf(false) }

    // 当 initialParentId 变化时更新 selectedParentId（仅在添加模式下）
    LaunchedEffect(initialParentId) {
        if (warehouseId == null) {
            selectedParentId = initialParentId
        }
    }

    val allWarehouses by viewModel.warehouses.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 过滤可用的父容器（排除当前容器和层级>=5的容器）
    val availableParents = remember(allWarehouses, warehouseId) {
        allWarehouses.filter { 
            it.id != warehouseId && it.level < 5
        }
    }

    LaunchedEffect(warehouseId) {
        if (warehouseId != null) {
            viewModel.loadWarehouse(warehouseId)
        } else {
            // 添加新容器时，清空表单
            name = ""
            description = ""
            location = ""
            capacity = ""
            // 保持 initialParentId 的设置
            selectedParentId = initialParentId
        }
    }

    val selectedWarehouse by viewModel.uiState.collectAsState()
    LaunchedEffect(warehouseId, selectedWarehouse.selectedWarehouse) {
        // 只有在编辑模式下（warehouseId 不为 null）才填充表单
        if (warehouseId != null) {
            selectedWarehouse.selectedWarehouse?.let { warehouse ->
                name = warehouse.name
                description = warehouse.description
                location = warehouse.location
                capacity = warehouse.capacity?.toString() ?: ""
                selectedParentId = warehouse.parentId
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(if (warehouseId == null) stringResource(R.string.add_warehouse) else stringResource(R.string.edit_warehouse)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                                val unlimitedContainers = prefs.getBoolean("unlimited_containers", false)
                                
                                val level = if (selectedParentId != null) {
                                    viewModel.calculateLevel(selectedParentId)
                                } else {
                                    1
                                }
                                
                                // 检查层级限制（除非开启无限容器模式）
                                if (!unlimitedContainers && level > 5) {
                                    // 显示提示：已达到最大层级限制
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.max_level_reached),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                                
                                val warehouse = Warehouse(
                                    id = warehouseId ?: 0,
                                    name = name,
                                    description = description,
                                    location = location,
                                    capacity = capacity.toIntOrNull(),
                                    parentId = selectedParentId,
                                    level = level
                                )
                                if (warehouseId == null) {
                                    viewModel.insertWarehouse(warehouse)
                                    // 如果是添加新容器，保存成功后调用回调
                                    onSaveSuccess?.invoke(selectedParentId)
                                } else {
                                    viewModel.updateWarehouse(warehouse.copy(id = warehouseId))
                                }
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save))
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
                label = { Text(stringResource(R.string.warehouse_name_required_field)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.warehouse_location)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )

            OutlinedTextField(
                value = capacity,
                onValueChange = { capacity = it },
                label = { Text(stringResource(R.string.capacity_limit_optional)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Storage, null) }
            )

            // 父容器选择
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedParentId?.let { parentId ->
                        availableParents.find { it.id == parentId }?.name ?: ""
                    } ?: "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.parent_container)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showParentDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, null) },
                    placeholder = { Text(stringResource(R.string.no_warehouse_option)) }
                )
                DropdownMenu(
                    expanded = showParentDropdown,
                    onDismissRequest = { showParentDropdown = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_warehouse_option)) },
                        onClick = {
                            selectedParentId = null
                            showParentDropdown = false
                        }
                    )
                    availableParents.forEach { parent ->
                        DropdownMenuItem(
                            text = { Text(parent.name) },
                            onClick = {
                                selectedParentId = parent.id
                                showParentDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

