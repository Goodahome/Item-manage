package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.AppFloatingActionButton
import com.example.itemremindertool.ui.components.AppDivider
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.components.ButtonAutoSizeText
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.loper7.date_time_picker.dialog.CardDatePickerDialog
import com.loper7.date_time_picker.DateTimeConfig
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemReminderSettingsScreen(
    item: Item,
    viewModel: ItemReminderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.getRemindersByItemUuid(item.uuid).collectAsState(initial = emptyList())
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ItemReminder?>(null) }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.add_reminder_title, item.name)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)) {
                // 使用与侧边栏风格首页一致的悬浮按钮样式
                val fabBackground = ColorHelpers.getGroup5FabColor()
                
                AppFloatingActionButton(
                    onClick = { showAddReminderDialog = true },
                    backgroundColor = fabBackground,
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Add,
                        stringResource(R.string.add_reminder)
                    )
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
    
    // 添加提醒对话框 - 使用统一的现代化弹窗
    if (showAddReminderDialog) {
        ModernReminderDialog(
            item = item,
            reminderViewModel = viewModel,
            onDismiss = {
                showAddReminderDialog = false
            },
            onSuccess = {
                showAddReminderDialog = false
            }
        )
    }
    
    // 编辑提醒对话框 - 使用统一的现代化弹窗
    if (editingReminder != null) {
        ModernReminderDialog(
            item = item,
            reminderViewModel = viewModel,
            existingReminder = editingReminder,
            onDismiss = {
                editingReminder = null
            },
            onSuccess = {
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
                            ReminderType.ONCE -> stringResource(R.string.reminder_type_once_display)
                            ReminderType.DAILY -> stringResource(R.string.reminder_type_daily_display)
                            ReminderType.MONTHLY -> stringResource(R.string.reminder_type_monthly_display)
                            ReminderType.YEARLY -> stringResource(R.string.reminder_type_yearly_display)
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
                        text = stringResource(R.string.reminder_reason, reminder.reason),
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
                        stringResource(R.string.edit),
                        tint = ColorHelpers.getGroup4IconColor()
                    )
                }
                
                // 删除按钮
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun getReminderTimeText(reminder: ItemReminder): String {
    val context = LocalContext.current
    val dateTimeFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }
    val dateFormat = remember {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    }
    val timeFormat = remember {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    }
    
    return when (reminder.reminderType) {
        ReminderType.ONCE -> reminder.reminderTime?.let { 
            context.getString(R.string.reminder_time_prefix, dateTimeFormat.format(it))
        } ?: context.getString(R.string.reminder_time_not_set)
        ReminderType.DAILY -> context.getString(
            R.string.reminder_daily_prefix,
            reminder.dailyTime ?: context.getString(R.string.reminder_time_not_set)
        )
        ReminderType.MONTHLY -> context.getString(
            R.string.reminder_monthly_prefix,
            reminder.monthlyDay ?: 0,
            reminder.monthlyTime ?: context.getString(R.string.reminder_time_not_set)
        )
        ReminderType.YEARLY -> context.getString(
            R.string.reminder_yearly_prefix,
            reminder.yearlyMonth ?: 0,
            reminder.yearlyDay ?: 0,
            reminder.yearlyTime ?: context.getString(R.string.reminder_time_not_set)
        )
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
    val context = LocalContext.current
    val activity = context as? Activity
    
    // 使用系统默认的日期和时间格式
    val dateFormat = remember {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    }
    val timeFormat = remember {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    }
    
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
    var datePickerKey by remember { mutableStateOf(0) }
    var timePickerKey by remember { mutableStateOf(0) }
    
    AppDialogLayout(
        title = if (existingReminder != null) stringResource(R.string.edit_reminder) else stringResource(R.string.add_reminder),
        icon = Icons.Default.Notifications,
        onDismiss = onDismiss,
        contentModifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp),
        footer = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                ButtonAutoSizeText(
                    text = stringResource(R.string.cancel)
                )
            }
            Button(
                onClick = {
                    val newReminder = ItemReminder(
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
                    onConfirm(newReminder)
                },
                modifier = Modifier.weight(1f)
            ) {
                ButtonAutoSizeText(
                    text = stringResource(R.string.confirm_button)
                )
            }
        }
    ) {
        // 提醒类型选择
        Text(stringResource(R.string.reminder_type), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                                ReminderType.ONCE -> stringResource(R.string.reminder_type_once)
                                ReminderType.DAILY -> stringResource(R.string.reminder_type_daily)
                                ReminderType.MONTHLY -> stringResource(R.string.reminder_type_monthly)
                                ReminderType.YEARLY -> stringResource(R.string.reminder_type_yearly)
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AppDivider(
            color = ColorHelpers.getDividerColor(),
            thickness = 2.dp
        )

        // 根据类型显示不同的时间设置
        when (selectedType) {
            ReminderType.ONCE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 日期选择
                    OutlinedTextField(
                        value = reminderTime?.let { dateFormat.format(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.date_label)) },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, null)
                            }
                        }
                    )
                    // 时间选择
                    OutlinedTextField(
                        value = reminderTime?.let { timeFormat.format(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.time_label)) },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (selectedDateMillis == null) {
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
                    label = { Text(stringResource(R.string.daily_reminder_time)) },
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
                    label = { Text(stringResource(R.string.monthly_date_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.reminder_time_hint)) },
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
                        label = { Text(stringResource(R.string.month_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearlyDay.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { day ->
                                yearlyDay = day.coerceIn(1, 31)
                            }
                        },
                        label = { Text(stringResource(R.string.day_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = yearlyTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.reminder_time_hint)) },
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

        AppDivider(
            color = ColorHelpers.getDividerColor(),
            thickness = 2.dp
        )

        // 提醒原因
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text(stringResource(R.string.reminder_reason_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )
    }
    
    // 日期选择器（一次性提醒）- 使用 CardDatePickerDialog
    val selectDateTitle = stringResource(R.string.select_date)
    LaunchedEffect(showDatePicker, datePickerKey) {
        if (showDatePicker && activity != null) {
            val dialog = CardDatePickerDialog.builder(activity)
                .setTitle(selectDateTitle)
                .setDefaultTime(selectedDateMillis ?: System.currentTimeMillis())
                .setDisplayType(DateTimeConfig.YEAR, DateTimeConfig.MONTH, DateTimeConfig.DAY)
                .setOnChoose { millisecond ->
                    selectedDateMillis = millisecond
                        // 如果已有时间，保留时间；否则设置为当前时间
                        val calendar = Calendar.getInstance()
                        if (reminderTime != null) {
                            calendar.time = reminderTime
                        }
                    calendar.timeInMillis = millisecond
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
                    datePickerKey++
                        currentEditingTimeField = "once"
                        showTimePickerDialog = true
                    timePickerKey++
                }
                .setOnCancel {
                    showDatePicker = false
                    datePickerKey++
                }
                .build()
            dialog.show()
                }
    }
    
    // 时间选择器（循环提醒和一次性提醒）- 使用 CardDatePickerDialog
    val selectTimeTitle = stringResource(R.string.select_time)
    LaunchedEffect(showTimePickerDialog, currentEditingTimeField, timePickerKey) {
        if (showTimePickerDialog && currentEditingTimeField != null && activity != null) {
            // 计算初始时间（毫秒）
            val initialTimeMillis = when (currentEditingTimeField) {
                "once" -> {
                    reminderTime?.time ?: run {
                        selectedDateMillis?.let {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it
                            val now = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        } ?: System.currentTimeMillis()
                    }
                }
                "daily" -> {
                val cal = Calendar.getInstance()
                    val parts = dailyTime.split(":")
                    cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 9)
                    cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                "monthly" -> {
                val cal = Calendar.getInstance()
                    val parts = monthlyTime.split(":")
                    cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 9)
                    cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                "yearly" -> {
                val cal = Calendar.getInstance()
                    val parts = yearlyTime.split(":")
                    cal.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 9)
                    cal.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                else -> System.currentTimeMillis()
            }
            
            // 时间选择器 - 使用 CardDatePickerDialog，设置只显示时分
            val dialog = CardDatePickerDialog.builder(activity)
                .setTitle(selectTimeTitle)
                .setDefaultTime(initialTimeMillis)
                .setDisplayType(DateTimeConfig.HOUR, DateTimeConfig.MIN)
                .setOnChoose { millisecond ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = millisecond
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)
                    
                    when (currentEditingTimeField) {
                        "once" -> {
                            // 一次性提醒：组合日期和时间
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
                            val timeStr = String.format("%02d:%02d:00", hour, minute)
                            dailyTime = timeStr
                        }
                        "monthly" -> {
                            val timeStr = String.format("%02d:%02d:00", hour, minute)
                            monthlyTime = timeStr
                        }
                        "yearly" -> {
                            val timeStr = String.format("%02d:%02d:00", hour, minute)
                            yearlyTime = timeStr
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
                }
    }
}



