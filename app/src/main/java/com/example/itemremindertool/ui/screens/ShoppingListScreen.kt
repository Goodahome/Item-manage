package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import com.example.itemremindertool.utils.ImageUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.data.model.Priority
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingItemViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 显示所有购物项（包括已完成的），而不是只显示未完成的
    val shoppingItems by viewModel.shoppingItems.collectAsState(initial = emptyList())
    val operationState by viewModel.operationState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.shopping_basket)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_shopping_item))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)
            ) {
                FloatingActionButton(
                    onClick = onAddItem,
                    containerColor = ColorHelpers.getGroup5FabColor(),
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_shopping_item))
                }
            }
        }
    ) { paddingValues ->
        if (shoppingItems.isEmpty()) {
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
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ColorHelpers.getGroup4IconColor(0.6f)
                    )
                    Text(
                        stringResource(R.string.no_shopping_items),
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                    Button(
                        onClick = onAddItem,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorHelpers.getGroup2SettingsBtnColor()
                        )
                    ) {
                        Text(stringResource(R.string.add_first_shopping_item), color = ColorHelpers.getGroup4TextColor())
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 排序：未完成的在前，已完成的在后
                val sortedItems = shoppingItems.sortedBy { it.isCompleted }
                items(sortedItems, key = { it.id }) { item ->
                    ShoppingItemCard(
                        item = item,
                        onEdit = { onEditItem(item.id) },
                        onDelete = { viewModel.deleteShoppingItem(item) },
                        onToggleComplete = { viewModel.toggleComplete(item) },
                        onQuantityChange = { newQuantity ->
                            viewModel.updateShoppingItem(item.copy(quantity = newQuantity))
                        }
                    )
                }
            }
        }
        } // 关闭 Scaffold 的 content lambda
        
        // 底部状态指示器
        BottomOperationStatusIndicator(
            operationState = operationState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    } // 关闭外层 Box
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf(item.quantity.toString()) }
    val context = LocalContext.current
    
    // 加载背景图片（从ShoppingItem的imageUri字段）
    val backgroundBitmap = remember(item.imageUri) {
        if (item.imageUri != null) {
            try {
                BitmapFactory.decodeFile(item.imageUri)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // 计算背景图片的亮度，决定文字颜色
    val isImageBright = remember(item.imageUri) {
        if (item.imageUri != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(item.imageUri)
                if (bitmap != null) {
                    ImageUtils.calculateImageBrightness(bitmap)
                } else {
                    true // 默认使用深色文字
                }
            } catch (e: Exception) {
                true // 默认使用深色文字
            }
        } else {
            true // 默认使用深色文字
        }
    }
    
    // 根据图片亮度决定文字颜色
    val textColor = if (isImageBright) {
        Color.Black
    } else {
        Color.White
    }
    
    // 当物品数量改变时，更新本地状态
    LaunchedEffect(item.quantity) {
        quantityText = item.quantity.toString()
    }

    val priorityColor = when (item.priority) {
        Priority.HIGH -> Color(0xFFD32F2F)
        Priority.MEDIUM -> Color(0xFFF57C00)
        Priority.LOW -> Color(0xFF388E3C)
    }

    // 统一的灰色背景色（用于已完成状态）
    val completedBackgroundColor = ColorHelpers.getGroup3CardBgColor().copy(alpha = 0.6f)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (item.isCompleted) 1.dp else 1.dp // 已完成的不显示阴影，避免边框效果
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) completedBackgroundColor else ColorHelpers.getGroup3CardBgColor()
        )
    ) {
        Box {
            // 未完成状态：显示背景图片和文字遮罩（如果有背景图片）
            if (!item.isCompleted) {
                if (backgroundBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = backgroundBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize(),
                        contentScale = ContentScale.FillBounds // 使用 FillBounds 填充整个区域
                    )
                    // 根据图片亮度添加半透明遮罩，确保文字可读
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .background(
                                if (isImageBright) {
                                    Color.White.copy(alpha = 0.4f) // 亮图用浅色遮罩
                                } else {
                                    Color.Black.copy(alpha = 0.5f) // 暗图用深色遮罩
                                }
                            )
                    )
                }
                // 如果没有背景图片，Card 的白色背景会显示出来
            }
            
            // 已完成状态：只显示灰色遮罩层覆盖整个卡片（仅在已完成时显示）
            if (item.isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .background(completedBackgroundColor)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggleComplete() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) {
                            if (backgroundBitmap != null) {
                                textColor.copy(alpha = 0.5f)
                            } else {
                                ColorHelpers.getGroup4TextColor(0.5f)
                            }
                        } else {
                            if (backgroundBitmap != null) {
                                textColor
                            } else {
                                ColorHelpers.getGroup4TextColor()
                            }
                        }
                    )
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (backgroundBitmap != null) {
                                textColor.copy(alpha = 0.9f)
                            } else {
                                ColorHelpers.getGroup4TextColor().copy(alpha = 0.7f)
                            }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(getPriorityLabel(item.priority)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = priorityColor.copy(alpha = 0.2f),
                                labelColor = priorityColor
                            )
                        )
                        // 直接显示为可编辑的数量输入框（使用BasicTextField自定义内边距）
                        var isFocused by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = quantityText,
                            onValueChange = { newValue: String ->
                                quantityText = newValue.filter { char: Char -> char.isDigit() }
                            },
                            modifier = Modifier
                                .width(60.dp)
                                .heightIn(min = 30.dp) // 使用 heightIn 而不是固定 height，允许内容自适应
                                .border(
                                    width = 1.dp,
                                    color = if (isFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp) // 自定义内边距
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                    if (!focusState.isFocused) {
                                        // 失去焦点时保存数量
                                        val newQuantity = quantityText.toIntOrNull() ?: 1
                                        if (newQuantity > 0) {
                                            onQuantityChange(newQuantity)
                                        } else {
                                            quantityText = item.quantity.toString()
                                        }
                                    }
                                },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = if (backgroundBitmap != null) {
                                    if (isFocused) textColor else textColor.copy(alpha = 0.8f)
                                } else {
                                    if (isFocused) {
                                        ColorHelpers.getGroup4TextColor()
                                    } else {
                                        ColorHelpers.getGroup4TextColor().copy(alpha = 0.8f)
                                    }
                                }
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val newQuantity = quantityText.toIntOrNull() ?: 1
                                    if (newQuantity > 0) {
                                        onQuantityChange(newQuantity)
                                    } else {
                                        quantityText = item.quantity.toString()
                                    }
                                }
                            )
                        )
                    }
                }
                }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        stringResource(R.string.more_options),
                        tint = if (backgroundBitmap != null && !item.isCompleted) {
                            textColor
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                stringResource(R.string.edit),
                                maxLines = 2 // 允许最多2行，支持文字换行
                            ) 
                        },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                stringResource(R.string.delete),
                                maxLines = 2 // 允许最多2行，支持文字换行
                            ) 
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                    )
                }
            }
            }
        }
    }
}

@Composable
fun getPriorityLabel(priority: Priority): String {
    return when (priority) {
        Priority.HIGH -> stringResource(R.string.priority_high)
        Priority.MEDIUM -> stringResource(R.string.priority_medium)
        Priority.LOW -> stringResource(R.string.priority_low)
    }
}

