package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.DraggableFab
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
        floatingActionButton = {}
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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

            val fabBoundsPadding = PaddingValues(
                start = 12.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                end = 12.dp,
                bottom = UIConstants.FAB_BOTTOM_PADDING + paddingValues.calculateBottomPadding()
            )
            DraggableFab(
                modifier = Modifier.fillMaxSize(),
                boundsPadding = fabBoundsPadding
            ) { fabModifier ->
                FloatingActionButton(
                    onClick = onAddItem,
                    containerColor = ColorHelpers.getGroup5FabColor(),
                    modifier = fabModifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_shopping_item))
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

@OptIn(ExperimentalFoundationApi::class)
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fabBackground = ColorHelpers.getGroup2SettingsBtnColor()
                val deleteIconColor = ColorHelpers.getGroup4IconColorByContrast(fabBackground)
                val nameColor = if (item.isCompleted) {
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
                val descColor = if (backgroundBitmap != null) {
                    textColor.copy(alpha = 0.85f)
                } else {
                    ColorHelpers.getGroup4TextColor().copy(alpha = 0.7f)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onToggleComplete() },
                            onLongClick = { onEdit() }
                        )
                ) {
                    val minNameFontSize = 12.sp
                    var nameFontSize by remember(item.name) { mutableStateOf(16.sp) }
                    Text(
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = nameFontSize,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = nameColor,
                        onTextLayout = { result ->
                            if (result.hasVisualOverflow && nameFontSize > minNameFontSize) {
                                nameFontSize = (nameFontSize.value - 1f).sp
                            }
                        }
                    )
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = descColor
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        val quantityWidth = when (quantityText.length) {
                            0, 1 -> 24.dp
                            2 -> 30.dp
                            3 -> 36.dp
                            else -> 42.dp
                        }
                        var isFocused by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val current = quantityText.toIntOrNull() ?: item.quantity
                                    val newQuantity = (current - 1).coerceAtLeast(1)
                                    quantityText = newQuantity.toString()
                                    onQuantityChange(newQuantity)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            BasicTextField(
                                value = quantityText,
                                onValueChange = { newValue: String ->
                                    quantityText = newValue.filter { char: Char -> char.isDigit() }
                                },
                                modifier = Modifier
                                    .widthIn(min = quantityWidth, max = 56.dp)
                                    .heightIn(min = 24.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isFocused) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                        if (!focusState.isFocused) {
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
                            IconButton(
                                onClick = {
                                    val current = quantityText.toIntOrNull() ?: item.quantity
                                    val newQuantity = (current + 1).coerceAtLeast(1)
                                    quantityText = newQuantity.toString()
                                    onQuantityChange(newQuantity)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(ColorHelpers.getGroup4TextColor().copy(alpha = 0.2f))
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = deleteIconColor, modifier = Modifier.size(18.dp))
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

