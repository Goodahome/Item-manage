package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehousesScreen(
    viewModel: WarehouseViewModel,
    itemViewModel: ItemViewModel,
    onAddWarehouse: () -> Unit,
    onEditWarehouse: (Long) -> Unit,
    onViewItems: (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 只获取顶层容器
    val topLevelWarehouses by viewModel.topLevelWarehouses.collectAsState(initial = emptyList())
    val allWarehouses by viewModel.warehouses.collectAsState(initial = emptyList())
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    
    // 计算每个容器的物品数量
    val warehouseItemCounts = remember(allWarehouses, items) {
        allWarehouses.associate { warehouse ->
            warehouse.id to items.count { it.warehouseId == warehouse.id }
        }
    }
    
    // 递归计算每个容器的子容器数量（包括所有子容器的子容器）
    val warehouseChildCounts = remember(topLevelWarehouses, allWarehouses) {
        topLevelWarehouses.associate { warehouse ->
            warehouse.id to countAllChildWarehouses(warehouse.id, allWarehouses)
        }
    }
    
    // 递归计算每个容器及其所有子容器中的物品数量
    val warehouseTotalItemCounts = remember(topLevelWarehouses, allWarehouses, items, warehouseItemCounts) {
        topLevelWarehouses.associate { warehouse ->
            warehouse.id to countAllItemsInWarehouse(warehouse.id, allWarehouses, items, warehouseItemCounts)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.warehouse_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWarehouse) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_warehouse))
            }
        }
    ) { paddingValues ->
        if (topLevelWarehouses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warehouse,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        stringResource(R.string.no_warehouses),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Button(onClick = onAddWarehouse) {
                        Text(stringResource(R.string.add_first_warehouse))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(topLevelWarehouses, key = { it.id }) { warehouse ->
                    SquareWarehouseCard(
                        warehouse = warehouse,
                        warehouseCount = warehouseChildCounts[warehouse.id] ?: 0,
                        itemCount = warehouseTotalItemCounts[warehouse.id] ?: 0,
                        onClick = { onViewItems(warehouse.id) }
                    )
                }
            }
        }
    }
}

/**
 * 正方形容器卡片（显示子容器数量和物品数量，使用图标）
 */
@Composable
fun SquareWarehouseCard(
    warehouse: Warehouse,
    warehouseCount: Int,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFEBE9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 统计信息（图标 + 数量）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = null,
                        tint = Color(0xFF5D4037),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$warehouseCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                }
                
                // 物品数量统计
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                        imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                        tint = Color(0xFF5D4037),
                        modifier = Modifier.size(24.dp)
                            )
                            Text(
                        text = "$itemCount",
                                style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                }
            }
        }
    }
}


