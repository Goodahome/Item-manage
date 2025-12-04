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
import android.graphics.BitmapFactory
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onScanBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, stringResource(R.string.add_item))
            }
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onEdit = { onEditItem(item.id) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCard(
    item: Item,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToShoppingCart: (() -> Unit)? = null,
    onMoveToContainer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // 加载背景图片
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            // 背景图片
            if (backgroundBitmap != null) {
                Image(
                    bitmap = backgroundBitmap,
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
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                            tint = if (backgroundBitmap != null) textColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        if (onAddToShoppingCart != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_shopping_cart)) },
                                onClick = {
                                    showMenu = false
                                    onAddToShoppingCart()
                                },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                            )
                        }
                        if (onMoveToContainer != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_to_container)) },
                                onClick = {
                                    showMenu = false
                                    onMoveToContainer()
                                },
                                leadingIcon = { Icon(Icons.Default.CompareArrows, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 统一显示所有标签（状态标签 + 自定义标签），使用不同浅颜色背景高亮，单行横向滚动
            val isExpired = item.expiryDate?.let { it.before(Date()) } ?: false
            val allTagsToShow = if (isExpired) {
                item.tags + "过期"
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
                            tag == "正常" -> Color(0xFFE8F5E9) // 浅绿
                            tag == "损坏" -> Color(0xFFFFF3E0) // 浅橙
                            tag == "遗失" -> Color(0xFFFFEBEE) // 浅红
                            tag == "过期" -> Color(0xFFF3E5F5) // 浅紫
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
                        
                        val displayTag = when (tag) {
                            "正常" -> stringResource(R.string.status_normal)
                            "损坏" -> stringResource(R.string.status_damaged)
                            "遗失" -> stringResource(R.string.status_lost)
                            "过期" -> stringResource(R.string.status_expired)
                            else -> tag
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
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
                if (item.price != null) {
                    Text(
                        text = stringResource(R.string.price_with_value, item.price),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (backgroundBitmap != null) {
                            textColor
                        } else {
                            MaterialTheme.colorScheme.primary
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
    val (labelRes, color, backgroundColor) = when (status) {
        ItemStatus.NORMAL -> Triple(R.string.status_normal, Color(0xFF388E3C), Color(0xFFE8F5E9))
        ItemStatus.DAMAGED -> Triple(R.string.status_damaged, Color(0xFFF57C00), Color(0xFFFFF3E0))
        ItemStatus.LOST -> Triple(R.string.status_lost, Color(0xFFD32F2F), Color(0xFFFFEBEE))
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

