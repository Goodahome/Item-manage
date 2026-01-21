package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import com.loper7.date_time_picker.dialog.CardDatePickerDialog
import com.loper7.date_time_picker.DateTimeConfig
import android.app.Activity
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.utils.CurrencyUtils
import com.example.itemremindertool.utils.ImageUtils
import android.graphics.BitmapFactory
import android.content.Context
import android.content.ContextWrapper
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.*
import java.util.Calendar
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    itemViewModel: ItemViewModel,
    reminderViewModel: ItemReminderViewModel,
    onNavigateBack: () -> Unit,
    onEditItem: (Long) -> Unit,
    onAddAlert: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    // 加载物品信息
    LaunchedEffect(itemId) {
        itemViewModel.loadItem(itemId)
    }
    val uiState by itemViewModel.uiState.collectAsState()
    val item = uiState.selectedItem
    
    // 加载提醒信息
    val reminders by reminderViewModel.getRemindersByItemId(itemId).collectAsState(initial = emptyList())
    
    // 提醒设置弹窗状态
    var showReminderDialog by remember { mutableStateOf(false) }
    
    // 图片查看弹窗状态
    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    
    if (item == null) {
        // 加载中或物品不存在
        Scaffold(
            topBar = {
                GradientTopAppBar(
                    title = { Text(stringResource(R.string.item_detail)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(item.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEditItem(item.id) }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 物品图片 - 主图
            val allImages = if (item.imageUris.isNotEmpty()) {
                item.imageUris
            } else {
                item.imageUri?.let { listOf(it) } ?: emptyList()
            }
            val primaryImageIndex = if (item.imageUris.isNotEmpty()) {
                item.primaryImageIndex.coerceIn(0, item.imageUris.size - 1)
            } else {
                0
            }
            val primaryImagePath = allImages.getOrNull(primaryImageIndex)
            
            if (primaryImagePath != null) {
                val context = LocalContext.current
                // 主图显示裁剪后的图片，如果裁剪图不存在则显示原图
                val croppedPath = ImageUtils.getCroppedImagePath(primaryImagePath)
                val displayPath = if (croppedPath != null) {
                    val croppedFile = java.io.File(croppedPath)
                    if (croppedFile.exists()) croppedPath else primaryImagePath
                } else {
                    primaryImagePath
                }
                
                val bitmap = remember(displayPath) {
                    ImageUtils.loadBitmapFromPath(displayPath)
                }
                
                if (bitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f) // 改为正方形
                            .clickable {
                                if (allImages.isNotEmpty()) {
                                    selectedImageIndex = primaryImageIndex
                                    showImageDialog = true
                                }
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                // 没有图片时显示占位符
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f), // 改为正方形
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorHelpers.getGroup3CardBgColor()
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val itemBackgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                        val itemTextColor = ColorHelpers.getContrastColor(itemBackgroundColor)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(itemBackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = itemTextColor
                            )
                        }
                    }
                }
            }
            
            // 小图列表（如果有多张图片）
            if (allImages.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(allImages) { index, imagePath ->
                        // 小图列表：主图显示裁剪图，非主图显示原图
                        val displayPath = if (index == primaryImageIndex) {
                            val croppedPath = ImageUtils.getCroppedImagePath(imagePath)
                            if (croppedPath != null) {
                                val croppedFile = java.io.File(croppedPath)
                                if (croppedFile.exists()) croppedPath else imagePath
                            } else {
                                imagePath
                            }
                        } else {
                            imagePath // 非主图显示原图
                        }
                        
                        val thumbnailBitmap = remember(displayPath) {
                            ImageUtils.loadBitmapFromPath(displayPath)
                        }
                        if (thumbnailBitmap != null) {
                            Card(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable {
                                        selectedImageIndex = index
                                        showImageDialog = true
                                    },
                                shape = RoundedCornerShape(8.dp),
                                border = if (index == primaryImageIndex) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                }
                            ) {
                                Image(
                                    bitmap = thumbnailBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
            
            // 基本信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 物品名称
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.item_name),
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorHelpers.getGroup4TextColor(0.7f)
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor(),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 描述
                    if (item.description.isNotBlank()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.description),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                        HorizontalDivider()
                    }
                    
                    // 数量（带快速调整按钮）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.quantity),
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorHelpers.getGroup4TextColor(0.7f)
                        )
                        
                        // 数量调整控件
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 减少按钮
                            IconButton(
                                onClick = {
                                    if (item.quantity > 0) {
                                        itemViewModel.updateItem(item.copy(quantity = item.quantity - 1))
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                enabled = item.quantity > 0
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "减少数量",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (item.quantity > 0)
                                        ColorHelpers.getGroup4IconColor()
                                    else
                                        ColorHelpers.getGroup4IconColor(0.3f)
                                )
                            }
                            
                            // 数量显示
                            Text(
                                text = item.quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor(),
                                modifier = Modifier.widthIn(min = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            // 增加按钮
                            IconButton(
                                onClick = {
                                    itemViewModel.updateItem(item.copy(quantity = item.quantity + 1))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "增加数量",
                                    modifier = Modifier.size(18.dp),
                                    tint = ColorHelpers.getGroup4IconColor()
                                )
                            }
                        }
                    }
                    
                    // 价格
                    if (item.price != null) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.price),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            Text(
                                text = CurrencyUtils.formatPrice(LocalContext.current, item.price),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                    }
                    
                    // 条码
                    if (item.barcode != null && item.barcode.isNotBlank()) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ColorHelpers.getGroup4IconColor(0.7f)
                                )
                                Text(
                                    text = stringResource(R.string.barcode),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColorHelpers.getGroup4TextColor(0.7f)
                                )
                            }
                            Text(
                                text = item.barcode,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor(),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    
                    // 到期日期
                    if (item.expiryDate != null) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ColorHelpers.getGroup4IconColor(0.7f)
                                )
                                Text(
                                    text = stringResource(R.string.expiry_date),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColorHelpers.getGroup4TextColor(0.7f)
                                )
                            }
                            val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
                            val dateStr = remember(item.expiryDate) { dateFormat.format(item.expiryDate) }
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                    }
                    
                    // 标签（含过期标签）
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
                        HorizontalDivider()
                        val pageBgColor = ColorHelpers.getGroup2PageBgColor()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tags),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorHelpers.getGroup4TextColorByContrast(pageBgColor, 0.7f)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allTagsToShow.forEach { tag ->
                                    val isExpiredTag = tag == "过期"
                                    val tagBgColor = if (isExpiredTag) {
                                        androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                    } else {
                                        ColorHelpers.getGroup2SettingsBtnColor()
                                    }
                                    val displayTag = if (isExpiredTag) {
                                        stringResource(R.string.status_expired)
                                    } else {
                                        tag
                                    }
                                    val borderColor = ColorHelpers.getGroup4TextColor(0.3f)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = tagBgColor,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        border = BorderStroke(1.dp, borderColor)
                                    ) {
                                        Text(
                                            text = displayTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isExpiredTag) {
                                                androidx.compose.ui.graphics.Color.White
                                            } else {
                                                ColorHelpers.getGroup4TextColorByContrast(tagBgColor)
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            maxLines = 1, // 标签内的文字不换行
                                            // overflow = TextOverflow.Ellipsis  如果文字太长，显示省略号
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 创建时间
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ColorHelpers.getGroup4IconColor(0.7f)
                            )
                            Text(
                                text = stringResource(R.string.created_at),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                        }
                        val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()) }
                        val createdAtStr = remember(item.createdAt) { dateFormat.format(item.createdAt) }
                        Text(
                            text = createdAtStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    }
                }
            }
            
            // 提醒设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.alert_settings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        TextButton(onClick = { showReminderDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add))
                        }
                    }
                    
                    HorizontalDivider()
                    
                    if (reminders.isEmpty()) {
                        // 无提醒
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = ColorHelpers.getGroup4IconColor(0.5f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.no_reminder_settings),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor(0.6f)
                            )
                        }
                    } else {
                        // 提醒列表
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            reminders.forEach { reminder ->
                                ReminderItemCard(reminder = reminder)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 提醒设置弹窗
    if (showReminderDialog && item != null) {
        ModernReminderDialog(
            item = item,
            reminderViewModel = reminderViewModel,
            onDismiss = { showReminderDialog = false },
            onSuccess = {
                showReminderDialog = false
            }
        )
    }
    
    // 图片查看弹窗
    if (showImageDialog) {
        item?.let { currentItem ->
            val allImages = if (currentItem.imageUris.isNotEmpty()) {
                currentItem.imageUris
            } else {
                currentItem.imageUri?.let { listOf(it) } ?: emptyList()
            }
            val currentImagePath = allImages.getOrNull(selectedImageIndex)
            
            if (currentImagePath != null) {
                Dialog(
                    onDismissRequest = { showImageDialog = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    // 图片查看弹窗：始终显示原图（完整大小）
                    val bitmap = remember(currentImagePath) {
                        ImageUtils.loadBitmapFromPath(currentImagePath)
                    }
                    
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f))
                                .clickable { showImageDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = currentItem.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItemCard(
    reminder: ItemReminder,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { 
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ColorHelpers.getGroup2PageBgColor(),
        border = BorderStroke(1.dp, ColorHelpers.getGroup4IconColor(0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 提醒类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (reminder.reminderType) {
                            ReminderType.ONCE -> Icons.Default.Event
                            ReminderType.DAILY -> Icons.Default.Today
                            ReminderType.MONTHLY -> Icons.Default.CalendarMonth
                            ReminderType.YEARLY -> Icons.Default.CalendarToday
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = ColorHelpers.getGroup4IconColor()
                    )
                    Text(
                        text = when (reminder.reminderType) {
                            ReminderType.ONCE -> stringResource(R.string.reminder_type_once_display)
                            ReminderType.DAILY -> stringResource(R.string.reminder_type_daily_display)
                            ReminderType.MONTHLY -> stringResource(R.string.reminder_type_monthly_display)
                            ReminderType.YEARLY -> stringResource(R.string.reminder_type_yearly_display)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
                
                // 启用状态
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (reminder.isEnabled) {
                        ColorHelpers.getGroup2SettingsBtnColor()
                    } else {
                        ColorHelpers.getGroup4IconColor(0.1f)
                    }
                ) {
                    Text(
                        text = if (reminder.isEnabled) stringResource(R.string.reminder_enabled) else stringResource(R.string.reminder_disabled),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminder.isEnabled) {
                            ColorHelpers.getContrastColor(ColorHelpers.getGroup2SettingsBtnColor())
                        } else {
                            ColorHelpers.getGroup4TextColor(0.6f)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // 提醒详情
            if (reminder.reason.isNotBlank()) {
                Text(
                    text = reminder.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.8f)
                )
            }
            
            // 提醒时间
            if (reminder.reminderTime != null) {
                Text(
                    text = stringResource(R.string.reminder_time_label, dateFormat.format(reminder.reminderTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 现代化的提醒设置弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernReminderDialog(
    item: Item,
    reminderViewModel: ItemReminderViewModel,
    existingReminder: ItemReminder? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val activity = LocalContext.current.findActivity()
    
    var selectedType by remember { mutableStateOf(existingReminder?.reminderType ?: ReminderType.ONCE) }
    var reminderTime by remember { mutableStateOf<Date?>(existingReminder?.reminderTime) }
    var dailyTime by remember { mutableStateOf(existingReminder?.dailyTime ?: "09:00") }
    var monthlyDay by remember { mutableStateOf(existingReminder?.monthlyDay ?: 1) }
    var monthlyTime by remember { mutableStateOf(existingReminder?.monthlyTime ?: "09:00") }
    var yearlyMonth by remember { mutableStateOf(existingReminder?.yearlyMonth ?: 1) }
    var yearlyDay by remember { mutableStateOf(existingReminder?.yearlyDay ?: 1) }
    var yearlyTime by remember { mutableStateOf(existingReminder?.yearlyTime ?: "09:00") }
    var reason by remember { mutableStateOf(existingReminder?.reason ?: "") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var currentEditingTimeField by remember { mutableStateOf<String?>(null) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(reminderTime?.time) }
    var datePickerKey by remember { mutableStateOf(0) }
    var timePickerKey by remember { mutableStateOf(0) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorHelpers.getGroup3CardBgColor()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部标题栏 - 现代化设计
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (existingReminder != null) stringResource(R.string.edit_reminder) else stringResource(R.string.add_reminder),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                
                // 内容区域（可滚动）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 提醒内容 - 紧凑版
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.reminder_content),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            placeholder = { Text(stringResource(R.string.reminder_content_placeholder), fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorHelpers.getGroup3CardBgColor(),
                                unfocusedContainerColor = ColorHelpers.getGroup3CardBgColor(),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = ColorHelpers.getGroup4TextColor(0.2f)
                            ),
                            minLines = 1,
                            maxLines = 2
                        )
                    }
                    
                    // 提醒时间 - 紧凑版
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.reminder_time),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                        OutlinedTextField(
                                value = when (selectedType) {
                                    ReminderType.ONCE -> reminderTime?.let { 
                                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(it)
                                    } ?: ""
                                    ReminderType.DAILY -> dailyTime
                                    ReminderType.MONTHLY -> "${monthlyDay}日 ${monthlyTime}"
                                    ReminderType.YEARLY -> "${yearlyMonth}月${yearlyDay}日 ${yearlyTime}"
                                },
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(stringResource(R.string.reminder_time_placeholder), fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (selectedType) {
                                        ReminderType.ONCE -> {
                                            showDatePicker = false
                                            datePickerKey++
                                            showDatePicker = true
                                        }
                                        ReminderType.DAILY -> {
                                            currentEditingTimeField = "daily"
                                            showTimePickerDialog = false
                                            timePickerKey++
                                            showTimePickerDialog = true
                                        }
                                        ReminderType.MONTHLY -> {
                                            currentEditingTimeField = "monthly"
                                            showTimePickerDialog = false
                                            timePickerKey++
                                            showTimePickerDialog = true
                                        }
                                        ReminderType.YEARLY -> {
                                            currentEditingTimeField = "yearly"
                                            showTimePickerDialog = false
                                            timePickerKey++
                                            showTimePickerDialog = true
                                    }
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    when (selectedType) {
                                        ReminderType.ONCE -> {
                                            showDatePicker = false
                                            datePickerKey++
                                            showDatePicker = true
                                        }
                                        ReminderType.DAILY -> {
                                            currentEditingTimeField = "daily"
                                            showTimePickerDialog = false
                                            timePickerKey++
                                            showTimePickerDialog = true
                                        }
                                        ReminderType.MONTHLY -> {
                                            currentEditingTimeField = "monthly"
                                            showTimePickerDialog = false
                                            timePickerKey++
                                            showTimePickerDialog = true
                                        }
                                        ReminderType.YEARLY -> {
                                            currentEditingTimeField = "yearly"
                                            showTimePickerDialog = false
                                            timePickerKey++
                                            showTimePickerDialog = true
                                        }
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ColorHelpers.getGroup3CardBgColor(),
                                unfocusedContainerColor = ColorHelpers.getGroup3CardBgColor(),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = ColorHelpers.getGroup4TextColor(0.2f),
                                disabledBorderColor = ColorHelpers.getGroup4TextColor(0.2f),
                                disabledContainerColor = ColorHelpers.getGroup3CardBgColor()
                            )
                        )
                    }
                    
                    // 提醒类型 - 横向紧凑布局
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.reminder_type),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                        
                        // 横向紧凑选项按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val options = listOf(
                                ReminderType.ONCE to "单次",
                                ReminderType.DAILY to "每日",
                                ReminderType.MONTHLY to "每月",
                                ReminderType.YEARLY to "每年"
                            )
                            
                            options.forEach { (type, label) ->
                                val isSelected = selectedType == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedType = type },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        ColorHelpers.getGroup3CardBgColor(),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            ColorHelpers.getGroup4TextColor(0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 图标
                                        Icon(
                                            when (type) {
                                                ReminderType.ONCE -> Icons.Default.Event
                                                ReminderType.DAILY -> Icons.Default.Today
                                                ReminderType.MONTHLY -> Icons.Default.CalendarMonth
                                                ReminderType.YEARLY -> Icons.Default.CalendarToday
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isSelected) 
                                                androidx.compose.ui.graphics.Color.White 
                                            else 
                                                MaterialTheme.colorScheme.primary
                                        )
                                        // 文字
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) 
                                                androidx.compose.ui.graphics.Color.White 
                                            else 
                                                ColorHelpers.getGroup4TextColor(),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 底部按钮区域 - 固定在底部
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorHelpers.getGroup3CardBgColor(),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ColorHelpers.getGroup4TextColor()
                            ),
                            border = BorderStroke(1.5.dp, ColorHelpers.getGroup4TextColor(0.3f))
                        ) {
                            Text(stringResource(R.string.cancel), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                val reminder = ItemReminder(
                                    id = existingReminder?.id ?: 0,
                                    itemId = item.id,
                                    reminderType = selectedType,
                                    reminderTime = if (selectedType == ReminderType.ONCE) reminderTime else null,
                                    dailyTime = if (selectedType == ReminderType.DAILY) dailyTime else null,
                                    monthlyDay = if (selectedType == ReminderType.MONTHLY) monthlyDay else null,
                                    monthlyTime = if (selectedType == ReminderType.MONTHLY) monthlyTime else null,
                                    yearlyMonth = if (selectedType == ReminderType.YEARLY) yearlyMonth else null,
                                    yearlyDay = if (selectedType == ReminderType.YEARLY) yearlyDay else null,
                                    yearlyTime = if (selectedType == ReminderType.YEARLY) yearlyTime else null,
                                    reason = reason,
                                    isEnabled = existingReminder?.isEnabled ?: true,
                                    createdAt = existingReminder?.createdAt ?: Date(),
                                    updatedAt = Date()
                                )
                                if (existingReminder != null) {
                                    reminderViewModel.updateReminder(reminder)
                                } else {
                                    reminderViewModel.insertReminder(reminder)
                                }
                                onSuccess()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.confirm_button), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
    // 日期时间选择器 - 使用 DateTimePicker 库（一次性提醒）
    val selectDateTimeTitle = stringResource(R.string.select_date_time)
    LaunchedEffect(showDatePicker, datePickerKey) {
        if (showDatePicker) {
            val currentActivity = activity
            if (currentActivity != null && !currentActivity.isFinishing) {
                val defaultTime = reminderTime?.time ?: selectedDateMillis ?: System.currentTimeMillis()
                try {
                    val dialog = CardDatePickerDialog.builder(currentActivity)
                        .setTitle(selectDateTimeTitle)
                        .setDefaultTime(defaultTime)
                        .setDisplayType(
                            DateTimeConfig.YEAR,
                            DateTimeConfig.MONTH,
                            DateTimeConfig.DAY,
                            DateTimeConfig.HOUR,
                            DateTimeConfig.MIN
                        )
                        .setOnChoose { millisecond ->
                            selectedDateMillis = millisecond
                            reminderTime = Date(millisecond)
                            showDatePicker = false
                            datePickerKey++
                        }
                        .setOnCancel {
                            showDatePicker = false
                            datePickerKey++
                        }
                        .build()
                    dialog.show()
                } catch (e: Exception) {
                    android.util.Log.e("ModernReminderDialog", "Failed to show date picker", e)
                    showDatePicker = false
                    datePickerKey++
                }
            } else {
                showDatePicker = false
                datePickerKey++
            }
        }
    }
    
    // 时间/循环选择器 - 使用 DateTimePicker 库
    val selectTimeTitle = stringResource(R.string.select_time)
    LaunchedEffect(showTimePickerDialog, currentEditingTimeField, timePickerKey) {
        if (showTimePickerDialog && currentEditingTimeField != null) {
            val currentActivity = activity
            if (currentActivity != null && !currentActivity.isFinishing) {
                val cal = Calendar.getInstance()
                // 计算初始时间（毫秒），并确定显示类型
                val (initialTimeMillis, displayType) = when (currentEditingTimeField) {
                "once" -> {
                    val t = reminderTime?.time ?: System.currentTimeMillis()
                    t to arrayOf(DateTimeConfig.HOUR, DateTimeConfig.MIN)
                }
                "daily" -> {
                    val parts = dailyTime.split(":")
                    cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 9)
                    cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis to arrayOf(DateTimeConfig.HOUR, DateTimeConfig.MIN)
                }
                "monthly" -> {
                    val parts = monthlyTime.split(":")
                    cal.set(Calendar.DAY_OF_MONTH, monthlyDay.coerceIn(1, 31))
                    cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 9)
                    cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis to arrayOf(DateTimeConfig.DAY, DateTimeConfig.HOUR, DateTimeConfig.MIN)
                }
                "yearly" -> {
                    val parts = yearlyTime.split(":")
                    cal.set(Calendar.MONTH, (yearlyMonth - 1).coerceIn(0, 11))
                    cal.set(Calendar.DAY_OF_MONTH, yearlyDay.coerceIn(1, 31))
                    cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 9)
                    cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis to arrayOf(DateTimeConfig.MONTH, DateTimeConfig.DAY, DateTimeConfig.HOUR, DateTimeConfig.MIN)
                }
                else -> System.currentTimeMillis() to arrayOf(DateTimeConfig.HOUR, DateTimeConfig.MIN)
            }
            
            // 时间/循环选择器 - 使用 CardDatePickerDialog
            try {
                val dialog = CardDatePickerDialog.builder(activity)
                    .setTitle(selectTimeTitle)
                    .setDefaultTime(initialTimeMillis)
                    .setDisplayType(*displayType.toIntArray())
                    .setOnChoose { millisecond: Long ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = millisecond
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)
                    
                    when (currentEditingTimeField) {
                        "once" -> {
                            selectedDateMillis?.let { dateMillis ->
                                val dateCal = Calendar.getInstance()
                                dateCal.timeInMillis = dateMillis
                                dateCal.set(Calendar.HOUR_OF_DAY, hour)
                                dateCal.set(Calendar.MINUTE, minute)
                                dateCal.set(Calendar.SECOND, 0)
                                dateCal.set(Calendar.MILLISECOND, 0)
                                reminderTime = Date(dateCal.timeInMillis)
                            }
                        }
                        "daily" -> {
                            dailyTime = String.format("%02d:%02d:00", hour, minute)
                        }
                        "monthly" -> {
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            monthlyDay = day
                            monthlyTime = String.format("%02d:%02d:00", hour, minute)
                        }
                        "yearly" -> {
                            val month = calendar.get(Calendar.MONTH) + 1
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            yearlyMonth = month
                            yearlyDay = day
                            yearlyTime = String.format("%02d:%02d:00", hour, minute)
                        }
                    }
                    showTimePickerDialog = false
                    timePickerKey++
                    currentEditingTimeField = null
                }
                .setOnCancel {
                    showTimePickerDialog = false
                    timePickerKey++
                    currentEditingTimeField = null
                }
                .build()
                    dialog.show()
                } catch (e: Exception) {
                    android.util.Log.e("ModernReminderDialog", "Failed to show time picker", e)
                    showTimePickerDialog = false
                    currentEditingTimeField = null
                }
            } else {
                showTimePickerDialog = false
                currentEditingTimeField = null
            }
        }
    }
}
