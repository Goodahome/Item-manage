package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemReminderSettingsScreen(
    item: Item,
    viewModel: ItemReminderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.getRemindersByItemId(item.id).collectAsState(initial = emptyList())
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ItemReminder?>(null) }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text("${item.name} - 提醒设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(modifier = Modifier.padding(bottom = 70.dp)) {
                FloatingActionButton(
                    onClick = { showAddReminderDialog = true },
                    containerColor = ColorHelpers.getGroup5FabColor(),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, "添加提醒")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
        ) {
            if (reminders.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ColorHelpers.getGroup4IconColor(0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "暂无提醒",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击右下角添加按钮设置提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor(0.5f)
                    )
                }
            } else {
                // 提醒列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onEdit = { editingReminder = reminder },
                            onDelete = { viewModel.deleteReminder(reminder) },
                            onToggleEnabled = {
                                viewModel.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 添加/编辑提醒对话框
    if (showAddReminderDialog || editingReminder != null) {
        ReminderEditDialog(
            item = item,
            existingReminder = editingReminder,
            onDismiss = {
                showAddReminderDialog = false
                editingReminder = null
            },
            onConfirm = { reminder ->
                if (editingReminder != null) {
                    viewModel.updateReminder(reminder)
                } else {
                    viewModel.insertReminder(reminder)
                }
                showAddReminderDialog = false
                editingReminder = null
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: ItemReminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 提醒类型标题
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (reminder.reminderType) {
                            ReminderType.ONCE -> Icons.Default.Event
                            ReminderType.DAILY -> Icons.Default.Today
                            ReminderType.MONTHLY -> Icons.Default.CalendarMonth
                            ReminderType.YEARLY -> Icons.Default.CalendarToday
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when (reminder.reminderType) {
                            ReminderType.ONCE -> "一次性提醒"
                            ReminderType.DAILY -> "每日提醒"
                            ReminderType.MONTHLY -> "每月提醒"
                            ReminderType.YEARLY -> "每年提醒"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
                
                // 提醒时间详情
                Text(
                    text = getReminderTimeText(reminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.8f)
                )
                
                // 提醒原因
                if (reminder.reason.isNotBlank()) {
                    Text(
                        text = "原因：${reminder.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                }
            }
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 开关
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
                
                // 编辑按钮
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        "编辑",
                        tint = ColorHelpers.getGroup4IconColor()
                    )
                }
                
                // 删除按钮
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun getReminderTimeText(reminder: ItemReminder): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return when (reminder.reminderType) {
        ReminderType.ONCE -> reminder.reminderTime?.let { "时间：${dateFormat.format(it)}" } ?: "未设置时间"
        ReminderType.DAILY -> "每天 ${reminder.dailyTime ?: "未设置时间"}"
        ReminderType.MONTHLY -> "每月 ${reminder.monthlyDay ?: "?"} 号 ${reminder.monthlyTime ?: "未设置时间"}"
        ReminderType.YEARLY -> "${reminder.yearlyMonth ?: "?"}月${reminder.yearlyDay ?: "?"}号 ${reminder.yearlyTime ?: "未设置时间"}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditDialog(
    item: Item,
    existingReminder: ItemReminder?,
    onDismiss: () -> Unit,
    onConfirm: (ItemReminder) -> Unit
) {
    var selectedType by remember { mutableStateOf(existingReminder?.reminderType ?: ReminderType.ONCE) }
    var reminderTime by remember { mutableStateOf(existingReminder?.reminderTime) }
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingReminder != null) "编辑提醒" else "添加提醒") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 提醒类型选择
                Text("提醒类型", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    when (type) {
                                        ReminderType.ONCE -> "一次性"
                                        ReminderType.DAILY -> "每日"
                                        ReminderType.MONTHLY -> "每月"
                                        ReminderType.YEARLY -> "每年"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Divider()
                
                // 根据类型显示不同的时间设置
                when (selectedType) {
                    ReminderType.ONCE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 日期选择
                            OutlinedTextField(
                                value = reminderTime?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("日期") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.CalendarToday, null)
                                    }
                                }
                            )
                            // 时间选择
                            OutlinedTextField(
                                value = reminderTime?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("时间") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (selectedDateMillis == null) {
                                            // 如果还没选择日期，先选择日期
                                            showDatePicker = true
                                        } else {
                                            currentEditingTimeField = "once"
                                            showTimePickerDialog = true
                                        }
                                    }) {
                                        Icon(Icons.Default.Schedule, null)
                                    }
                                }
                            )
                        }
                    }
                    ReminderType.DAILY -> {
                        OutlinedTextField(
                            value = dailyTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("每日提醒时间") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    currentEditingTimeField = "daily"
                                    showTimePickerDialog = true
                                }) {
                                    Icon(Icons.Default.Schedule, null)
                                }
                            }
                        )
                    }
                    ReminderType.MONTHLY -> {
                        OutlinedTextField(
                            value = monthlyDay.toString(),
                            onValueChange = { 
                                it.toIntOrNull()?.let { day ->
                                    monthlyDay = day.coerceIn(1, 31)
                                }
                            },
                            label = { Text("每月日期（1-31）") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = monthlyTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("提醒时间") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    currentEditingTimeField = "monthly"
                                    showTimePickerDialog = true
                                }) {
                                    Icon(Icons.Default.Schedule, null)
                                }
                            }
                        )
                    }
                    ReminderType.YEARLY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = yearlyMonth.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { month ->
                                        yearlyMonth = month.coerceIn(1, 12)
                                    }
                                },
                                label = { Text("月份（1-12）") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = yearlyDay.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { day ->
                                        yearlyDay = day.coerceIn(1, 31)
                                    }
                                },
                                label = { Text("日期（1-31）") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = yearlyTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("提醒时间") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    currentEditingTimeField = "yearly"
                                    showTimePickerDialog = true
                                }) {
                                    Icon(Icons.Default.Schedule, null)
                                }
                            }
                        )
                    }
                }
                
                Divider()
                
                // 提醒原因
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("提醒原因（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newReminder = ItemReminder(
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
                    onConfirm(newReminder)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 日期选择器（一次性提醒）
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                        // 如果已有时间，保留时间；否则设置为当前时间
                        val calendar = Calendar.getInstance()
                        if (reminderTime != null) {
                            calendar.time = reminderTime
                        }
                        calendar.timeInMillis = it
                        // 如果没有设置过时间，使用当前时间
                        if (reminderTime == null) {
                            val now = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            calendar.set(Calendar.SECOND, now.get(Calendar.SECOND))
                        }
                        reminderTime = Date(calendar.timeInMillis)
                        // 选择日期后自动打开时间选择器
                        showDatePicker = false
                        currentEditingTimeField = "once"
                        showTimePickerDialog = true
                    } ?: run {
                        showDatePicker = false
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // 时间选择器（循环提醒和一次性提醒）
    if (showTimePickerDialog && currentEditingTimeField != null) {
        // 获取初始时间
        val initialHour = when (currentEditingTimeField) {
            "once" -> reminderTime?.let {
                val cal = Calendar.getInstance()
                cal.time = it
                cal.get(Calendar.HOUR_OF_DAY)
            } ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            "daily" -> dailyTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
            "monthly" -> monthlyTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
            "yearly" -> yearlyTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
            else -> 9
        }
        val initialMinute = when (currentEditingTimeField) {
            "once" -> reminderTime?.let {
                val cal = Calendar.getInstance()
                cal.time = it
                cal.get(Calendar.MINUTE)
            } ?: Calendar.getInstance().get(Calendar.MINUTE)
            "daily" -> dailyTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
            "monthly" -> monthlyTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
            "yearly" -> yearlyTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
            else -> 0
        }
        val initialSecond = when (currentEditingTimeField) {
            "once" -> reminderTime?.let {
                val cal = Calendar.getInstance()
                cal.time = it
                cal.get(Calendar.SECOND)
            } ?: 0
            else -> 0
        }
        
        var second by remember { mutableStateOf(initialSecond) }
        
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )
        
        AlertDialog(
            onDismissRequest = { 
                showTimePickerDialog = false
                currentEditingTimeField = null
            },
            title = { Text("选择时间") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TimePicker(state = timePickerState)
                    // 如果是一次性提醒，添加秒的选择
                    if (currentEditingTimeField == "once") {
                        OutlinedTextField(
                            value = second.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { s ->
                                    second = s.coerceIn(0, 59)
                                }
                            },
                            label = { Text("秒（0-59）") },
                            modifier = Modifier.fillMaxWidth(),
                            suffix = { Text("秒") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (currentEditingTimeField) {
                        "once" -> {
                            // 一次性提醒：组合日期和时间
                            selectedDateMillis?.let { dateMillis ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = dateMillis
                                calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                calendar.set(Calendar.MINUTE, timePickerState.minute)
                                calendar.set(Calendar.SECOND, second)
                                calendar.set(Calendar.MILLISECOND, 0)
                                reminderTime = Date(calendar.timeInMillis)
                            }
                        }
                        "daily" -> {
                            val timeStr = String.format("%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                            dailyTime = timeStr
                        }
                        "monthly" -> {
                            val timeStr = String.format("%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                            monthlyTime = timeStr
                        }
                        "yearly" -> {
                            val timeStr = String.format("%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                            yearlyTime = timeStr
                        }
                    }
                    showTimePickerDialog = false
                    currentEditingTimeField = null
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePickerDialog = false
                    currentEditingTimeField = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}



