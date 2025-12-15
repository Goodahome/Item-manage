package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.utils.ImageUtils
import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    
    if (item == null) {
        // 加载中或物品不存在
        Scaffold(
            topBar = {
                GradientTopAppBar(
                    title = { Text(stringResource(R.string.item_detail)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
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
            // 物品图片
            if (item.imageUri != null) {
                val context = LocalContext.current
                val bitmap = remember(item.imageUri) {
                    try {
                        BitmapFactory.decodeFile(item.imageUri)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (bitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                        .height(200.dp),
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
                    
                    Divider()
                    
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
                        Divider()
                    }
                    
                    // 数量
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
                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    }
                    
                    // 价格
                    if (item.price != null) {
                        Divider()
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
                                text = stringResource(R.string.price_with_value, item.price),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                    }
                    
                    // 条码
                    if (item.barcode != null && item.barcode.isNotBlank()) {
                        Divider()
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
                        Divider()
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
                            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                            val dateStr = remember(item.expiryDate) { dateFormat.format(item.expiryDate) }
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                        }
                    }
                    
                    // 标签
                    if (item.tags.isNotEmpty()) {
                        Divider()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tags),
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = ColorHelpers.getGroup2SettingsBtnColor(),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorHelpers.getContrastColor(ColorHelpers.getGroup2SettingsBtnColor()),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
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
                        TextButton(onClick = { onAddAlert(item) }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add))
                        }
                    }
                    
                    Divider()
                    
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
                                text = "暂无提醒设置",
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
}

@Composable
fun ReminderItemCard(
    reminder: ItemReminder,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
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
                            ReminderType.ONCE -> "一次性提醒"
                            ReminderType.DAILY -> "每日提醒"
                            ReminderType.MONTHLY -> "每月提醒"
                            ReminderType.YEARLY -> "每年提醒"
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
                        text = if (reminder.isEnabled) "已启用" else "已禁用",
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
                    text = "提醒时间: ${dateFormat.format(reminder.reminderTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

