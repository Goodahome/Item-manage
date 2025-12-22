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
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.AutoSizeButton
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.layout.onSizeChanged

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
            GradientTopAppBar(
                title = { Text(stringResource(R.string.warehouse_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)
            ) {
                FloatingActionButton(
                    onClick = onAddWarehouse,
                    containerColor = ColorHelpers.getGroup5FabColor(),
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_warehouse))
                }
            }
        }
    ) { paddingValues ->
        if (topLevelWarehouses.isEmpty()) {
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
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ColorHelpers.getGroup4IconColor(0.6f)
                    )
                    Text(
                        stringResource(R.string.no_warehouses),
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                    AutoSizeButton(
                        onClick = onAddWarehouse,
                        text = stringResource(R.string.add_first_warehouse)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
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
            .width(96.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
            // 统计信息（图标 + 数量）- 单行显示
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$warehouseCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = ColorHelpers.getGroup4IconColor()
                    )
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                        tint = ColorHelpers.getGroup4IconColor()
                    )
                }
            }
        }
    }
}

/**
 * 滚动文本组件（支持手动滚动）
 */
@Composable
fun ScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified
) {
    val scrollState = rememberScrollState()
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    val needsScroll = textWidth > containerWidth && containerWidth > 0
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = if (!needsScroll && textAlign == TextAlign.Center) {
            Alignment.Center
        } else {
            Alignment.CenterStart
        }
    ) {
        if (needsScroll) {
            // 文本较长，需要滚动
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Start,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { textLayoutResult ->
                        textWidth = textLayoutResult.size.width
                    }
                )
            }
        } else {
            // 文本较短，居中显示
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    textWidth = textLayoutResult.size.width
                }
            )
        }
    }
}


