package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.utils.CurrencyUtils
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.DraggableFab
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.AppFloatingActionButton
import androidx.compose.foundation.background
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.*
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemViewModel,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onScanBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState(initial = emptyList())
    val operationState by viewModel.operationState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.item_management)) },
                actions = {
                    IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Default.QrCodeScanner, stringResource(R.string.barcode_scanner))
                    }
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_item))
                    }
                }
            )
        },
        floatingActionButton = {}
        ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (items.isEmpty()) {
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
                            Icons.Default.Category,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.uuid }) { item ->
                        ItemCard(
                            item = item,
                            onEdit = { onEditItem(item.uuid) },
                            onDelete = { viewModel.deleteItem(item) }
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
                AppFloatingActionButton(
                    onClick = onAddItem,
                    backgroundColor = ColorHelpers.getGroup5FabColor(),
                    modifier = fabModifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_item))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToShoppingCart: (() -> Unit)? = null,
    onMoveToContainer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 获取主图路径（原图路径，用于生成缩略图）
    val primaryImagePath = remember(item.imageUris, item.primaryImageIndex, item.imageUri) {
        if (item.imageUris.isNotEmpty() && item.primaryImageIndex < item.imageUris.size) {
            item.imageUris[item.primaryImageIndex]
        } else {
            item.imageUri
        }
    }
    
    // 使用缩略图加载背景图片（异步加载，避免阻塞UI）
    var backgroundBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isImageBright by remember { mutableStateOf(true) }
    
    LaunchedEffect(primaryImagePath) {
        backgroundBitmap = null
        isImageBright = true
        
        if (primaryImagePath != null) {
            scope.launch(Dispatchers.IO) {
                // 加载缩略图（最大400像素，用于列表展示）
                val thumbnail = ImageUtils.loadThumbnail(context, primaryImagePath, maxSize = 400)
                if (thumbnail != null) {
                    backgroundBitmap = thumbnail
                    isImageBright = ImageUtils.calculateImageBrightness(thumbnail)
                }
            }
        }
    }
    
    // 根据图片亮度决定文字颜色
    val textColor = if (isImageBright) {
        Color.Black
    } else {
        Color.White
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        )
    ) {
        Box {
            // 背景图片（使用缩略图）
            backgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize(),
                    contentScale = ContentScale.FillBounds // 使用 FillBounds 填充整个区域
                )
                // 根据图片亮度添加半透明遮罩，确保文字可读
                // 如果图片较亮，使用浅色遮罩；如果较暗，使用深色遮罩
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
            
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (backgroundBitmap != null) textColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (backgroundBitmap != null) {
                                textColor.copy(alpha = 0.9f)
                            } else {
                                ColorHelpers.getGroup4TextColor(0.7f)
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            stringResource(R.string.more_options),
                            tint = if (backgroundBitmap != null) textColor else ColorHelpers.getGroup4IconColor()
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
                        if (onAddToShoppingCart != null) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.add_to_shopping_cart),
                                        maxLines = 2 // 允许最多2行，支持文字换行
                                    ) 
                                },
                                onClick = {
                                    showMenu = false
                                    onAddToShoppingCart()
                                },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, null) },
                                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                            )
                        }
                        if (onMoveToContainer != null) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.move_to_container),
                                        maxLines = 2 // 允许最多2行，支持文字换行
                                    ) 
                                },
                                onClick = {
                                    showMenu = false
                                    onMoveToContainer()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, null) },
                                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                            )
                        }
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

            Spacer(modifier = Modifier.height(12.dp))

            // 统一显示所有标签（标签 + 自定义标签），使用不同浅颜色背景高亮，单行横向滚动
            val expiredTagLabel = stringResource(R.string.status_expired)
            // 到期日结束后（次日00:01起）才算过期
            val isExpired = item.expiryDate?.let { date ->
                val zone = ZoneId.systemDefault()
                val nowZoned = Instant.now().atZone(zone)
                val expiryEnd = Instant.ofEpochMilli(date.time)
                    .atZone(zone)
                    .toLocalDate()
                    .plusDays(1)          // 次日
                    .atStartOfDay(zone)   // 00:00
                    .plusMinutes(1)       // 00:01 后开始算过期
                !nowZoned.isBefore(expiryEnd)
            } ?: false
            val allTagsToShow = if (isExpired) {
                item.tags + expiredTagLabel
            } else {
                item.tags
            }
            
            if (allTagsToShow.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    items(allTagsToShow.size) { index ->
                        val tag = allTagsToShow[index]
                        // 根据标签类型和索引分配不同浅颜色背景（文字统一为黑色）
                        val bgColor = when {
                            tag == expiredTagLabel -> Color(0xFFF3E5F5) // 浅紫
                            else -> {
                                // 自定义标签使用循环颜色（柔和的浅色）
                                val colors = listOf(
                                    Color(0xFFE3F2FD), // 浅蓝
                                    Color(0xFFF1F8E9), // 浅绿
                                    Color(0xFFFFF9C4), // 浅黄
                                    Color(0xFFFCE4EC), // 浅粉
                                    Color(0xFFE0F2F1), // 浅青
                                    Color(0xFFF3E5F5), // 浅紫
                                    Color(0xFFFFE0B2)  // 浅橙
                                )
                                colors[index % colors.size]
                            }
                        }
                        
                        val displayTag = if (tag == expiredTagLabel) {
                            expiredTagLabel
                        } else {
                            tag
                        }
                        // 使用 AssistChip 确保背景色正确显示
                        AssistChip(
                            onClick = { },
                            label = { Text(displayTag, color = Color.Black) },
                            enabled = false,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = bgColor,
                                labelColor = Color.Black,
                                disabledContainerColor = bgColor,
                                disabledLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 其他信息（包括到期日期）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (item.expiryDate != null) {
                        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                        val dateStr = dateFormat.format(item.expiryDate)
                        Text(
                            text = stringResource(R.string.expires_on, dateStr),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (backgroundBitmap != null) {
                                if (isExpired) {
                                    Color(0xFFFF6B6B) // 红色，在背景上更明显
                                } else {
                                    textColor.copy(alpha = 0.9f)
                                }
                            } else {
                                if (isExpired) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            }
                        )
                    }
                if (item.quantity > 1) {
                        if (item.expiryDate != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    Text(
                        text = stringResource(R.string.quantity_with_value, item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                            color = if (backgroundBitmap != null) {
                                textColor.copy(alpha = 0.9f)
                            } else {
                                ColorHelpers.getGroup4TextColor(0.6f)
                            }
                        )
                    }
                }
                if (item.price != null) {
                    Text(
                        text = CurrencyUtils.formatPrice(LocalContext.current, item.price),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (backgroundBitmap != null) {
                            textColor
                        } else {
                            ColorHelpers.getGroup4TextColor()
                        }
                    )
                }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    status: ItemStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 只处理过期状态
    val (labelRes, color, backgroundColor) = when (status) {
        ItemStatus.EXPIRED -> Triple(R.string.status_expired, Color(0xFF7B1FA2), Color(0xFFF3E5F5))
    }

    FilterChip(
        selected = true,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = backgroundColor,
            selectedLabelColor = color
        ),
        modifier = modifier
    )
}

