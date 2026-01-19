package com.example.itemremindertool.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.utils.ImageUtils
import java.text.DateFormat
import java.util.*

/**
 * 网格模式物品卡片（正方形，类似游戏背包）
 */
@Composable
fun ItemGridCard(
    item: Item,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取主图路径（优先使用裁剪后的主图）
    val primaryImagePath = remember(item.imageUris, item.primaryImageIndex, item.imageUri) {
        if (item.imageUris.isNotEmpty() && item.primaryImageIndex < item.imageUris.size) {
            val originalPath = item.imageUris[item.primaryImageIndex]
            val croppedPath = ImageUtils.getCroppedImagePath(originalPath)
            if (croppedPath != null) {
                val croppedFile = java.io.File(croppedPath)
                if (croppedFile.exists()) croppedPath else originalPath
            } else {
                originalPath
            }
        } else {
            item.imageUri
        }
    }
    
    // 加载背景图片
    val backgroundBitmap = remember(primaryImagePath) {
        if (primaryImagePath != null) {
            try {
                BitmapFactory.decodeFile(primaryImagePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // 计算背景图片的亮度，决定文字颜色
    val isImageBright = remember(primaryImagePath) {
        if (primaryImagePath != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(primaryImagePath)
                if (bitmap != null) {
                    ImageUtils.calculateImageBrightness(bitmap)
                } else {
                    true
                }
            } catch (e: Exception) {
                true
            }
        } else {
            true
        }
    }
    
    // 根据图片亮度决定文字颜色
    val textColor = if (backgroundBitmap != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4TextColor()
    }
    
    // 使用与左侧容器图标一致的主题色
    val cardBackgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
    
    Card(
        modifier = modifier
            .aspectRatio(1f) // 保持正方形
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBitmap != null) {
                Color.Transparent
            } else {
                cardBackgroundColor
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            if (backgroundBitmap != null) {
                Image(
                    bitmap = backgroundBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 添加半透明遮罩确保文字可读
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isImageBright) {
                                Color.White.copy(alpha = 0.3f)
                            } else {
                                Color.Black.copy(alpha = 0.4f)
                            }
                        )
                )
            }
            
            // 根据是否有图片决定文字颜色
            val displayTextColor = if (backgroundBitmap != null) {
                Color.White // 有图片时统一白色（因为有半透明遮罩）
            } else {
                // 无图片时使用与背景色对比的颜色，与左侧容器图标保持一致
                ColorHelpers.getContrastColor(cardBackgroundColor)
            }
            
            // 物品名称 - 有图片时不显示，无图片时居中显示
            if (backgroundBitmap == null) {
                Text(
                    text = item.name,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    color = displayTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
            
            // 数量（右下角）- 纯数字显示，下移到真正的底部
            if (item.quantity > 0) {
                Text(
                    text = "${item.quantity}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 3.dp),
                    color = displayTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = if (displayTextColor == Color.White) Color.Black else Color.White,
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }
            
            // 选中指示器
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            }
        }
    }
}

/**
 * 网格内嵌详细信息面板（与物品卡片同层级，紧凑版）
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemGridDetailPanel(
    item: Item,
    onQuantityChange: (Int) -> Unit,
    onUse: (Int) -> Unit,
    onViewDetails: () -> Unit,
    onAddToShoppingCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var useQuantity by remember { mutableStateOf(1) }
    var quantityInputText by remember { mutableStateOf("1") }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp),
        shape = RoundedCornerShape(12.dp), // 与物品卡片相同的圆角
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 与物品卡片相同的立体效果
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onAddToShoppingCart,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = stringResource(R.string.add_to_shopping_cart),
                        tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // 物品描述
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.8f),
                    maxLines = 2,
                    fontSize = 10.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 标签列表（横向滚动）
            if (item.tags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(item.tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ColorHelpers.getGroup4TextColor().copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorHelpers.getGroup4TextColor(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            
            // 使用数量滑块（美化版 - 圆角滑块）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 减少按钮
                IconButton(
                    onClick = { 
                        if (useQuantity > 1) {
                            useQuantity--
                            quantityInputText = useQuantity.toString()
                        }
                    },
                    modifier = Modifier.size(32.dp),
                    enabled = useQuantity > 1
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.decrease),
                        tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // 自定义美化滑块
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // 圆角轨道背景
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorHelpers.getGroup4IconColor(0.15f))
                    )
                    
                    // 激活部分的圆角轨道
                    val progress = (useQuantity - 1f) / (item.quantity - 1f).coerceAtLeast(1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    
                    // 标准滑块（透明，只用于交互）
                    Slider(
                        value = useQuantity.toFloat(),
                        onValueChange = { 
                            useQuantity = it.toInt()
                            quantityInputText = useQuantity.toString()
                        },
                        valueRange = 1f..item.quantity.toFloat().coerceAtLeast(1f),
                        steps = 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        thumb = {
                            // 圆角方形滑块按钮
                            Box(
                                modifier = Modifier
                                    .size(18.dp, 24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .then(
                                        Modifier.background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    )
                            )
                        }
                    )
                }
                
                // 增加按钮
                IconButton(
                    onClick = { 
                        if (useQuantity < item.quantity) {
                            useQuantity++
                            quantityInputText = useQuantity.toString()
                        }
                    },
                    modifier = Modifier.size(32.dp),
                    enabled = useQuantity < item.quantity
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.increase),
                        tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // 使用数量显示（末尾）
                Text(
                    text = "$useQuantity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 36.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ColorHelpers.getGroup4TextColor()
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(stringResource(R.string.details), fontSize = 11.sp)
                }
                
                Button(
                    onClick = { 
                        onUse(useQuantity)
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    enabled = item.quantity > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(stringResource(R.string.use_item), fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * 物品详细信息展开面板（显示在网格下方）
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailPanel(
    item: Item,
    onQuantityChange: (Int) -> Unit,
    onUse: (Int) -> Unit,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var useQuantity by remember { mutableStateOf(1) }
    var quantityInputText by remember { mutableStateOf("1") }
    
    Card(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题栏：物品名称 + 关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = ColorHelpers.getGroup4IconColor()
                    )
                }
            }
            
            // 描述
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.8f)
                )
            }
            
            // 标签
            if (item.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(item.tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            // 到期时间
            if (item.expiryDate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup4IconColor(),
                        modifier = Modifier.size(20.dp)
                    )
                    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
                    Text(
                        text = stringResource(R.string.expires, dateFormat.format(item.expiryDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                }
            }
            
            Divider(color = ColorHelpers.getGroup4IconColor(0.2f))
            
            // 当前数量显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.current_quantity),
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorHelpers.getGroup4TextColor()
                )
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 使用数量选择
            Text(
                text = stringResource(R.string.use_quantity),
                style = MaterialTheme.typography.titleSmall,
                color = ColorHelpers.getGroup4TextColor()
            )
            
            // 数量滑块
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
                Slider(
                    value = useQuantity.toFloat(),
                    onValueChange = { 
                        useQuantity = it.toInt()
                        quantityInputText = useQuantity.toString()
                    },
                    valueRange = 1f..item.quantity.toFloat().coerceAtLeast(1f),
                    steps = (item.quantity - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
            }
            
            // 数量输入框
            OutlinedTextField(
                value = quantityInputText,
                onValueChange = { 
                    quantityInputText = it
                    val num = it.toIntOrNull()
                    if (num != null && num in 1..item.quantity) {
                        useQuantity = num
                    }
                },
                label = { Text(stringResource(R.string.input_use_quantity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = ColorHelpers.getGroup4IconColor(0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = ColorHelpers.getGroup4TextColor(0.6f)
                )
            )
            
            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 详细信息按钮
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ColorHelpers.getGroup4TextColor()
                    )
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.detail_info))
                }
                
                // 使用按钮
                Button(
                    onClick = { 
                        val finalQuantity = quantityInputText.toIntOrNull()?.coerceIn(1, item.quantity) ?: useQuantity
                        onUse(finalQuantity)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = item.quantity > 0
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.use_item))
                }
            }
        }
    }
}
