package com.example.itemremindertool.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.AppDivider
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.utils.CurrencyUtils
import com.example.itemremindertool.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

/**
 * 网格模式物品卡片（正方形，类似游戏背包）
 */
@Composable
fun ItemGridCard(
    item: Item,
    isSelected: Boolean = false,
    useOutlineIcon: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
    
    // 使用与左侧容器图标一致的主题色
    val cardBackgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
    
    // 根据图片亮度决定文字颜色
    val isOutlineActive = useOutlineIcon && backgroundBitmap == null
    val textColor = if (backgroundBitmap != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        if (isOutlineActive) {
            cardBackgroundColor
        } else {
            ColorHelpers.getGroup4TextColor()
        }
    }
    
    // 根据背景色和对比度判断，返回对应的边框颜色
    val selectedBorderColor = cardBackgroundColor
    
    val cardShape = if (isSelected) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .aspectRatio(1f) // 保持正方形
            .clickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBitmap != null || isOutlineActive || isSelected) {
                Color.Transparent
            } else {
                cardBackgroundColor
            }
        ),
        elevation = if (isOutlineActive || isSelected) {
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            )
        } else {
            CardDefaults.cardElevation(
                defaultElevation = 6.dp, // 选中和未选中时使用相同的 elevation
                pressedElevation = 8.dp,
                hoveredElevation = 7.dp,
                focusedElevation = 7.dp
            )
        },
        border = null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val contentPadding = if (isSelected) 6.dp else 0.dp
            val contentShape = RoundedCornerShape(12.dp)
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clip(contentShape)
            val contentShadowModifier = if (isSelected && !isOutlineActive) {
                contentModifier.shadow(
                    elevation = 6.dp,
                    shape = contentShape,
                    clip = false
                )
            } else {
                contentModifier
            }

            Box(modifier = contentShadowModifier) {
                if (backgroundBitmap == null && isSelected && !isOutlineActive) {
                    Box(modifier = Modifier.matchParentSize().background(cardBackgroundColor))
                }

                // 背景图片（使用缩略图）
                backgroundBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 添加半透明遮罩确保文字可读
                    Box(
                        modifier = Modifier.matchParentSize()
                            .background(
                                if (isImageBright) {
                                    Color.White.copy(alpha = 0.18f)
                                } else {
                                    Color.Black.copy(alpha = 0f)
                                }
                            )
                    )
                }
            
            // 根据是否有图片决定文字颜色
            val displayTextColor = if (backgroundBitmap != null) {
                // 有图片时，根据图片亮度创建一个代表背景的颜色来计算对比度
                val imageBgColor = if (isImageBright) {
                    Color.White.copy(alpha = 0.18f) // 亮图片，使用浅色背景
                } else {
                    Color.Black.copy(alpha = 0.28f) // 暗图片，使用深色背景
                }
                ColorHelpers.getGroup4TextColorByContrast(imageBgColor)
            } else {
                // 无图片时使用与背景色对比的颜色
                if (isOutlineActive) {
                    cardBackgroundColor
                } else {
                    ColorHelpers.getGroup4TextColorByContrast(cardBackgroundColor)
                }
            }

                // 物品名称 - 有图片时不显示，无图片时居中显示
                if (backgroundBitmap == null) {
                    Text(
                        text = item.name,
                        modifier = Modifier
                            .align(Alignment.Center)
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
            }

            if (isOutlineActive) {
                Box(
                    modifier = if (isSelected) {
                        contentModifier.border(2.dp, cardBackgroundColor, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                            .fillMaxSize()
                            .border(2.dp, cardBackgroundColor, RoundedCornerShape(12.dp))
                    }
                )
            }

            if (isSelected) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val cornerLength = 14.dp.toPx()
                    val inset = 3.dp.toPx()
                    val maxX = size.width - inset
                    val maxY = size.height - inset

                    // 左上角
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(inset, inset),
                        end = Offset(inset + cornerLength, inset),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(inset, inset),
                        end = Offset(inset, inset + cornerLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    // 右上角
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(maxX - cornerLength, inset),
                        end = Offset(maxX, inset),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(maxX, inset),
                        end = Offset(maxX, inset + cornerLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    // 左下角
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(inset, maxY - cornerLength),
                        end = Offset(inset, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(inset, maxY),
                        end = Offset(inset + cornerLength, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    // 右下角
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(maxX - cornerLength, maxY),
                        end = Offset(maxX, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = selectedBorderColor,
                        start = Offset(maxX, maxY - cornerLength),
                        end = Offset(maxX, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // 选中时只显示边框，不显示背景遮罩
        }
    }
}

/**
 * 网格内嵌详细信息面板（与物品卡片同层级，紧凑版）
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemGridDetailPanel(
    item: Item,
    onQuantityChange: (Int) -> Unit,
    onUse: (Int) -> Unit,
    onViewDetails: () -> Unit,
    onAddToShoppingCart: () -> Unit,
    onMoveToContainer: (() -> Unit)? = null,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useQuantity by remember { mutableStateOf(1) }
    var quantityInputText by remember { mutableStateOf("1") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp),
        shape = RoundedCornerShape(12.dp), // 与物品卡片相同的圆角
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3GridInfoCardBgColor()
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
                val priceText = item.price?.let { CurrencyUtils.formatPrice(context, it) }
                val titleText = buildAnnotatedString {
                    append(item.name)
                    if (priceText != null) {
                        append(" ")
                        withStyle(
                            SpanStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorHelpers.getGroup4TextColor(0.85f)
                            )
                        ) {
                            append(priceText)
                        }
                    }
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    if (onMoveToContainer != null) {
                        IconButton(
                            onClick = onMoveToContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DriveFileMove,
                                contentDescription = stringResource(R.string.move_to_container),
                                tint = ColorHelpers.getGroup4IconColor(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_item),
                            tint = ColorHelpers.getGroup4IconColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
            val isExpired = item.expiryDate?.let { date ->
                val zone = ZoneId.systemDefault()
                val nowZoned = Instant.now().atZone(zone)
                val expiryEnd = Instant.ofEpochMilli(date.time)
                    .atZone(zone)
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .plusMinutes(1)
                !nowZoned.isBefore(expiryEnd)
            } ?: false
            val allTagsToShow = if (isExpired) {
                item.tags + "过期"
            } else {
                item.tags
            }
            if (allTagsToShow.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allTagsToShow) { tag ->
                        val isExpiredTag = tag == "过期"
                        val tagBgColor = if (isExpiredTag) {
                            Color(0xFFD32F2F)
                        } else {
                            Color.Transparent
                        }
                        val borderColor = ColorHelpers.getGroup4TextColor().copy(alpha = 0.6f)
                        val displayTag = if (isExpiredTag) {
                            stringResource(R.string.status_expired)
                        } else {
                            tag
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tagBgColor,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
                        ) {
                            Text(
                                text = displayTag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isExpiredTag) Color.White else ColorHelpers.getGroup4TextColor(),
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
            val outlineEnabled = ColorHelpers.isOutlineEnabled()
            val buttonBgColor = ColorHelpers.getGroup2SettingsBtnColor()
            val buttonTextColor = if (outlineEnabled) buttonBgColor else ColorHelpers.getGroup4TextColorByContrast(buttonBgColor)
            val buttonIconColor = if (outlineEnabled) buttonBgColor else ColorHelpers.getGroup4IconColorByContrast(buttonBgColor)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 详情按钮（改为和使用按钮一致的样式）
                if (outlineEnabled) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier.weight(1f).height(36.dp),
                        border = BorderStroke(2.dp, buttonBgColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = buttonTextColor,
                            disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(stringResource(R.string.details), fontSize = 11.sp, color = buttonTextColor)
                    }
                } else {
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBgColor,
                            contentColor = buttonTextColor,
                            disabledContainerColor = buttonBgColor.copy(alpha = 0.5f),
                            disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(stringResource(R.string.details), fontSize = 11.sp, color = buttonTextColor)
                    }
                }
                
                // 使用按钮
                if (outlineEnabled) {
                    OutlinedButton(
                        onClick = { onUse(useQuantity) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        enabled = item.quantity > 0,
                        border = BorderStroke(2.dp, buttonBgColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = buttonTextColor,
                            disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.RemoveCircle,
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(stringResource(R.string.use_item), fontSize = 11.sp, color = buttonTextColor)
                    }
                } else {
                    Button(
                        onClick = { 
                            onUse(useQuantity)
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        enabled = item.quantity > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBgColor,
                            contentColor = buttonTextColor,
                            disabledContainerColor = buttonBgColor.copy(alpha = 0.5f),
                            disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.RemoveCircle,
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(stringResource(R.string.use_item), fontSize = 11.sp, color = buttonTextColor)
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AppDialogLayout(
            title = stringResource(R.string.delete_item),
            icon = Icons.Default.Delete,
            onDismiss = { showDeleteDialog = false },
            footer = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        ) {
            Text(
                text = stringResource(R.string.delete_item_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
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
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
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
            val buttonBgColor = ColorHelpers.getGroup2SettingsBtnColor()
            val buttonTextColor = ColorHelpers.getGroup4TextColorByContrast(buttonBgColor)
            val buttonIconColor = ColorHelpers.getGroup4IconColorByContrast(buttonBgColor)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 详细信息按钮（改为和使用按钮一致的样式）
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = buttonTextColor,
                        disabledContainerColor = buttonBgColor.copy(alpha = 0.5f),
                        disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = buttonIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.detail_info), color = buttonTextColor)
                }
                
                // 使用按钮
                Button(
                    onClick = { 
                        val finalQuantity = quantityInputText.toIntOrNull()?.coerceIn(1, item.quantity) ?: useQuantity
                        onUse(finalQuantity)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = item.quantity > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = buttonTextColor,
                        disabledContainerColor = buttonBgColor.copy(alpha = 0.5f),
                        disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = null,
                        tint = buttonIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.use_item), color = buttonTextColor)
                }
            }
        }
    }
}
