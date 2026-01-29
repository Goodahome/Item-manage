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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import android.text.format.DateFormat as AndroidDateFormat
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.ui.components.AppDivider
import com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.utils.CurrencyUtils
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.utils.formatQuantityWithUnit
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import android.graphics.BitmapFactory
import android.content.Context
import android.content.ContextWrapper
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.*
import java.util.Calendar
import java.time.Instant
import java.time.ZoneId
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel

private fun firstDisplayChar(text: String): String {
    if (text.isBlank()) return "?"
    val codePoint = text.codePointAt(0)
    val count = Character.charCount(codePoint)
    return text.substring(0, count)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    itemUuid: String,
    itemViewModel: ItemViewModel,
    reminderViewModel: ItemReminderViewModel,
    onNavigateBack: () -> Unit,
    onEditItem: (String) -> Unit,
    onAddAlert: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val activityEventViewModel: ActivityEventViewModel = viewModel()
    LaunchedEffect(itemUuid) {
        itemViewModel.loadItemByUuid(itemUuid)
    }
    val uiState by itemViewModel.uiState.collectAsState()
    val item = uiState.selectedItem
    
    // 加载提醒信息
    val reminders by remember(item?.uuid) {
        if (item?.uuid != null) {
            reminderViewModel.getRemindersByItemUuid(item.uuid)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<ItemReminder>())
        }
    }.collectAsState(initial = emptyList())
    
    // 提醒设置弹窗状态
    var showReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ItemReminder?>(null) }
    
    // 图片查看弹窗状态
    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(item?.uuid) {
        item?.let { loadedItem ->
            activityEventViewModel.logItemViewed(loadedItem.uuid, loadedItem.name)
        }
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEditItem(item.uuid) }) {
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
            val allImages = item.imageUris.ifEmpty {
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
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
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
                        .size(220.dp)
                        .align(Alignment.CenterHorizontally),
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
                        val displayChar = firstDisplayChar(item.name)
                        val displayText = if (displayChar.length == 1) displayChar.uppercase() else displayChar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(itemBackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayText,
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
                    
                    AppDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 2.dp
                    )
                    
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
                        AppDivider(
                            color = ColorHelpers.getDividerColor(),
                            thickness = 2.dp
                        )
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
                                text = formatQuantityWithUnit(item.quantity, item.quantityUnit),
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
                        AppDivider(
                            color = ColorHelpers.getDividerColor(),
                            thickness = 2.dp
                        )
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
                        AppDivider(
                            color = ColorHelpers.getDividerColor(),
                            thickness = 2.dp
                        )
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
                        AppDivider(
                            color = ColorHelpers.getDividerColor(),
                            thickness = 2.dp
                        )
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
                        val outlineEnabled = ColorHelpers.isOutlineEnabled()
                        AppDivider(
                            color = ColorHelpers.getDividerColor(),
                            thickness = 2.dp
                        )
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
                                    val borderColor = if (outlineEnabled) {
                                        tagBgColor
                                    } else {
                                        ColorHelpers.getGroup4TextColor(0.3f)
                                    }
                                    val borderWidth = if (outlineEnabled) 2.dp else 1.dp
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (outlineEnabled) Color.Transparent else tagBgColor,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        border = BorderStroke(borderWidth, borderColor)
                                    ) {
                                        Text(
                                            text = displayTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                outlineEnabled -> tagBgColor
                                                isExpiredTag -> androidx.compose.ui.graphics.Color.White
                                                else -> ColorHelpers.getGroup4TextColorByContrast(tagBgColor)
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
                    AppDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 2.dp
                    )
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
                    
                    AppDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 2.dp
                    )
                    
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
                                ReminderItemCard(
                                    reminder = reminder,
                                    onToggleEnabled = {
                                        reminderViewModel.updateReminder(
                                            reminder.copy(isEnabled = !reminder.isEnabled, updatedAt = Date())
                                        )
                                    },
                                    onEdit = { editingReminder = reminder }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 提醒设置弹窗（新增）
    if (showReminderDialog) {
        ModernReminderDialog(
            item = item,
            reminderViewModel = reminderViewModel,
            onDismiss = { showReminderDialog = false },
            onSuccess = {
                showReminderDialog = false
            }
        )
    }

    // 提醒设置弹窗（编辑）
    if (editingReminder != null) {
        ModernReminderDialog(
            item = item,
            reminderViewModel = reminderViewModel,
            existingReminder = editingReminder,
            onDismiss = { editingReminder = null },
            onSuccess = { editingReminder = null }
        )
    }
    
    // 图片查看弹窗
    if (showImageDialog) {
        val allImages = item.imageUris.ifEmpty {
            item.imageUri?.let { listOf(it) } ?: emptyList()
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
                                contentDescription = item.name,
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

@Composable
fun ReminderItemCard(
    reminder: ItemReminder,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { 
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Switch(
                        checked = reminder.isEnabled,
                        onCheckedChange = { onToggleEnabled() }
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
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    val billingManager = remember {
        if (FeatureFlags.ENABLE_PURCHASE_FEATURE) {
            BillingManager(
                context,
                listOf(
                    BillingManager.PRODUCT_REMOVE_ADS,
                    BillingManager.PRODUCT_PREMIUM_FEATURES,
                    BillingManager.PRODUCT_PREMIUM_LIFETIME
                )
            ).apply {
                initialize()
            }
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "premium_features" || key == "premium_lifetime" || key == "premium_trial_used" || key == "premium_trial_start_time") {
                canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    
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
                // 顶部标题栏 - 使用主题色背景
                val headerColor = MaterialTheme.colorScheme.primary
                val headerContentColor = MaterialTheme.colorScheme.onPrimary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                            tint = headerContentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (existingReminder != null) stringResource(R.string.edit_reminder) else stringResource(R.string.add_reminder),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerContentColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = headerContentColor
                        )
                    }
                }
                AppDivider(color = Color.Transparent, thickness = 0.dp)
                
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
                                    if (!canAccessPremiumFeatures && selectedType != ReminderType.ONCE) {
                                        showPremiumFeatureDialog = true
                                        return@clickable
                                    }
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
                                    if (!canAccessPremiumFeatures && selectedType != ReminderType.ONCE) {
                                        showPremiumFeatureDialog = true
                                        return@IconButton
                                    }
                                    when (selectedType) {
                                        ReminderType.ONCE -> {
                                            currentEditingTimeField = "once"
                                            showDatePicker = true
                                            datePickerKey++
                                        }
                                        ReminderType.DAILY -> {
                                            currentEditingTimeField = "daily"
                                            showTimePickerDialog = true
                                            timePickerKey++
                                        }
                                        ReminderType.MONTHLY -> {
                                            currentEditingTimeField = "monthly"
                                            showDatePicker = true
                                            datePickerKey++
                                        }
                                        ReminderType.YEARLY -> {
                                            currentEditingTimeField = "yearly"
                                            showDatePicker = true
                                            datePickerKey++
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
                            ReminderType.ONCE to stringResource(R.string.reminder_type_once_short),
                            ReminderType.DAILY to stringResource(R.string.reminder_type_daily_short),
                            ReminderType.MONTHLY to stringResource(R.string.reminder_type_monthly_short),
                            ReminderType.YEARLY to stringResource(R.string.reminder_type_yearly_short)
                        )
                            
                            options.forEach { (type, label) ->
                                val isSelected = selectedType == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!canAccessPremiumFeatures && type != ReminderType.ONCE) {
                                                showPremiumFeatureDialog = true
                                            } else {
                                                selectedType = type
                                            }
                                        },
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
                
                // 底部按钮区域 - 透明背景
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
                    val confirmText = stringResource(R.string.confirm_button)
                    val confirmFontSize = when {
                        confirmText.length >= 8 -> 11.sp
                        confirmText.length >= 6 -> 12.sp
                        confirmText.length >= 5 -> 13.sp
                        else -> 15.sp
                    }
                    Button(
                        onClick = {
                            if (!canAccessPremiumFeatures && selectedType != ReminderType.ONCE) {
                                showPremiumFeatureDialog = true
                                return@Button
                            }
                            val reminder = ItemReminder(
                                uuid = existingReminder?.uuid ?: java.util.UUID.randomUUID().toString(),
                                itemUuid = item.uuid,
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
                        Text(
                            text = confirmText,
                            fontSize = confirmFontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
    
    val is24HourFormat = remember { AndroidDateFormat.is24HourFormat(context) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis ?: reminderTime?.time
    )

    LaunchedEffect(showDatePicker, currentEditingTimeField, datePickerKey) {
        if (showDatePicker) {
            val initialMillis = when (currentEditingTimeField) {
                "once" -> selectedDateMillis ?: reminderTime?.time
                "monthly", "yearly" -> System.currentTimeMillis()
                else -> selectedDateMillis
            } ?: System.currentTimeMillis()
            datePickerState.selectedDateMillis = initialMillis
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                currentEditingTimeField = null
            },
            confirmButton = {
                TextButton(onClick = {
                    val pickedMillis = datePickerState.selectedDateMillis
                    if (pickedMillis != null) {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = pickedMillis
                        when (currentEditingTimeField) {
                            "once" -> {
                                selectedDateMillis = pickedMillis
                            }
                            "monthly" -> {
                                monthlyDay = calendar.get(Calendar.DAY_OF_MONTH)
                            }
                            "yearly" -> {
                                yearlyMonth = calendar.get(Calendar.MONTH) + 1
                                yearlyDay = calendar.get(Calendar.DAY_OF_MONTH)
                            }
                        }
                    }
                    showDatePicker = false
                    if (currentEditingTimeField in listOf("once", "monthly", "yearly")) {
                        showTimePickerDialog = true
                        timePickerKey++
                    } else {
                        currentEditingTimeField = null
                    }
                }) {
                    Text(stringResource(R.string.confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    currentEditingTimeField = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                modifier = Modifier
                    .graphicsLayer(scaleX = 0.9f, scaleY = 0.9f)
            )
        }
    }

    val (initialHour, initialMinute) = remember(
        currentEditingTimeField,
        timePickerKey,
        reminderTime,
        dailyTime,
        monthlyTime,
        yearlyTime
    ) {
        when (currentEditingTimeField) {
            "once" -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = reminderTime?.time ?: System.currentTimeMillis()
                cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
            }
            "daily" -> {
                val parts = dailyTime.split(":")
                (parts.getOrNull(0)?.toIntOrNull() ?: 9) to
                    (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            "monthly" -> {
                val parts = monthlyTime.split(":")
                (parts.getOrNull(0)?.toIntOrNull() ?: 9) to
                    (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            "yearly" -> {
                val parts = yearlyTime.split(":")
                (parts.getOrNull(0)?.toIntOrNull() ?: 9) to
                    (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            else -> 9 to 0
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24HourFormat
    )

    if (showTimePickerDialog && currentEditingTimeField != null) {
        AlertDialog(
            onDismissRequest = {
                showTimePickerDialog = false
                currentEditingTimeField = null
            },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    when (currentEditingTimeField) {
                        "once" -> {
                            val dateMillis = selectedDateMillis ?: System.currentTimeMillis()
                            val dateCal = Calendar.getInstance()
                            dateCal.timeInMillis = dateMillis
                            dateCal.set(Calendar.HOUR_OF_DAY, hour)
                            dateCal.set(Calendar.MINUTE, minute)
                            dateCal.set(Calendar.SECOND, 0)
                            dateCal.set(Calendar.MILLISECOND, 0)
                            reminderTime = Date(dateCal.timeInMillis)
                            selectedDateMillis = dateCal.timeInMillis
                        }
                        "daily" -> {
                            dailyTime = String.format("%02d:%02d:00", hour, minute)
                        }
                        "monthly" -> {
                            monthlyTime = String.format("%02d:%02d:00", hour, minute)
                        }
                        "yearly" -> {
                            yearlyTime = String.format("%02d:%02d:00", hour, minute)
                        }
                    }
                    showTimePickerDialog = false
                    timePickerKey++
                    currentEditingTimeField = null
                }) {
                    Text(stringResource(R.string.confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePickerDialog = false
                    timePickerKey++
                    currentEditingTimeField = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = 0.9f, scaleY = 0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}
