package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.ButtonAutoSizeText
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.data.model.Item
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReminderFilter {
    ALL, ENABLED, DISABLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    reminderViewModel: ItemReminderViewModel = viewModel(),
    itemViewModel: ItemViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    val allReminders by reminderViewModel.allReminders.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf(ReminderFilter.ALL) }
    
    // 根据筛选条件过滤提醒
    val filteredReminders = remember(allReminders, selectedFilter) {
        when (selectedFilter) {
            ReminderFilter.ALL -> allReminders
            ReminderFilter.ENABLED -> allReminders.filter { it.isEnabled }
            ReminderFilter.DISABLED -> allReminders.filter { !it.isEnabled }
        }
    }
    
    // 用于存储物品信息的Map
    var itemsMap by remember { mutableStateOf<Map<String, Item>>(emptyMap()) }
    
    // 加载所有相关物品信息
    LaunchedEffect(allReminders) {
        val itemUuids = allReminders.map { it.itemUuid }.distinct()
        val items = mutableMapOf<String, Item>()
        itemUuids.forEach { itemUuid ->
            itemViewModel.getItemByUuid(itemUuid)?.let { item ->
                items[itemUuid] = item
            }
        }
        itemsMap = items
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.reminder_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        containerColor = ColorHelpers.getGroup2PageBgColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 筛选按钮
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == ReminderFilter.ALL,
                        onClick = { selectedFilter = ReminderFilter.ALL },
                        label = { 
                            Text(
                                stringResource(R.string.filter_all_reminders),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        leadingIcon = if (selectedFilter == ReminderFilter.ALL) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorHelpers.getGroup2SettingsBtnColor(),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ReminderFilter.ENABLED,
                        onClick = { selectedFilter = ReminderFilter.ENABLED },
                        label = { 
                            Text(
                                stringResource(R.string.filter_enabled_reminders),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        leadingIcon = if (selectedFilter == ReminderFilter.ENABLED) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorHelpers.getGroup2SettingsBtnColor(),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ReminderFilter.DISABLED,
                        onClick = { selectedFilter = ReminderFilter.DISABLED },
                        label = { 
                            Text(
                                stringResource(R.string.filter_disabled_reminders),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        leadingIcon = if (selectedFilter == ReminderFilter.DISABLED) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorHelpers.getGroup2SettingsBtnColor(),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            
            if (filteredReminders.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = ColorHelpers.getGroup4TextColor().copy(alpha = 0.5f)
                        )
                        Text(
                            text = when (selectedFilter) {
                                ReminderFilter.ALL -> stringResource(R.string.no_reminders)
                                ReminderFilter.ENABLED -> stringResource(R.string.no_enabled_reminders)
                                ReminderFilter.DISABLED -> stringResource(R.string.no_disabled_reminders)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColorHelpers.getGroup4TextColor().copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // 提醒列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredReminders, key = { it.uuid }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            item = itemsMap[reminder.itemUuid],
                            onToggleEnabled = { enabled ->
                                scope.launch {
                                    reminderViewModel.updateReminder(reminder.copy(isEnabled = enabled))
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    reminderViewModel.deleteReminder(reminder)
                                }
                            },
                            onNavigateToItem = onNavigateToItem
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ItemReminder,
    item: Item?,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    val contentAlpha = if (reminder.isEnabled) 1f else 0.5f
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item?.let { onNavigateToItem(it.uuid) } },
        shape = RoundedCornerShape(8.dp),
        color = ColorHelpers.getGroup3CardBgColor(),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!reminder.isEnabled) {
                        Modifier.background(
                            ColorHelpers.getGroup2PageBgColor().copy(alpha = 0.5f)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
            // 标题行：物品名称、开关和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item?.name ?: stringResource(R.string.unknown_item),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor().copy(alpha = contentAlpha)
                        )
                        if (!reminder.isEnabled) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = stringResource(R.string.disabled_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorHelpers.getGroup4TextColor().copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (reminder.reason.isNotEmpty()) {
                        Text(
                            text = reminder.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor().copy(alpha = contentAlpha * 0.7f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked = reminder.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ColorHelpers.getGroup5FabColor(),
                            checkedTrackColor = ColorHelpers.getGroup5FabColor().copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            Divider(
                color = ColorHelpers.getDividerColor(),
                thickness = 0.5.dp
            )
            
            // 提醒类型和时间（合并为一行）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        tint = ColorHelpers.getGroup5FabColor().copy(alpha = contentAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = getReminderTypeText(reminder),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor().copy(alpha = contentAlpha)
                    )
                }
                
                // 时间
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup5FabColor().copy(alpha = contentAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = getReminderTimeText(reminder, dateFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor().copy(alpha = contentAlpha)
                    )
                }
            }
        }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete_reminder)) },
            text = { Text(stringResource(R.string.confirm_delete_reminder_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    ButtonAutoSizeText(
                        text = stringResource(R.string.delete)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    ButtonAutoSizeText(
                        text = stringResource(R.string.cancel)
                    )
                }
            }
        )
    }
}

@Composable
private fun getReminderTypeText(reminder: ItemReminder): String {
    return when (reminder.reminderType) {
        ReminderType.ONCE -> stringResource(R.string.reminder_type_once)
        ReminderType.DAILY -> stringResource(R.string.reminder_type_daily)
        ReminderType.MONTHLY -> stringResource(R.string.reminder_type_monthly)
        ReminderType.YEARLY -> stringResource(R.string.reminder_type_yearly)
    }
}

@Composable
private fun getReminderTimeText(reminder: ItemReminder, dateFormat: SimpleDateFormat): String {
    return when (reminder.reminderType) {
        ReminderType.ONCE -> {
            reminder.reminderTime?.let { dateFormat.format(it) } 
                ?: stringResource(R.string.no_time_set)
        }
        ReminderType.DAILY -> {
            reminder.dailyTime ?: stringResource(R.string.no_time_set)
        }
        ReminderType.MONTHLY -> {
            val day = reminder.monthlyDay ?: 1
            val time = reminder.monthlyTime ?: "00:00"
            stringResource(R.string.monthly_reminder_format, day, time)
        }
        ReminderType.YEARLY -> {
            val month = reminder.yearlyMonth ?: 1
            val day = reminder.yearlyDay ?: 1
            val time = reminder.yearlyTime ?: "00:00"
            stringResource(R.string.yearly_reminder_format, month, day, time)
        }
    }
}
