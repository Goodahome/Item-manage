package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import com.example.itemremindertool.data.AlertSettingsManager
import com.example.itemremindertool.ui.theme.ColorHelpers
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.itemremindertool.notification.NotificationScheduler
import com.example.itemremindertool.ui.components.GradientTopAppBar
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alertSettingsManager = remember { AlertSettingsManager(context) }
    
    var expiryReminderDays by remember { mutableStateOf(alertSettingsManager.getExpiryReminderDays()) }
    var lowStockThreshold by remember { mutableStateOf(alertSettingsManager.getLowStockThreshold()) }
    var forgetProtectionEnabled by remember { mutableStateOf(alertSettingsManager.isForgetProtectionEnabled()) }
    var systemNotificationEnabled by remember { mutableStateOf(alertSettingsManager.isSystemNotificationEnabled()) }
    var notificationHour by remember { mutableStateOf(alertSettingsManager.getNotificationHour()) }
    var notificationMinute by remember { mutableStateOf(alertSettingsManager.getNotificationMinute()) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.alert_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
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
            // 到期提醒期限
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.expiry_reminder_days),
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = stringResource(R.string.expiry_reminder_days_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.6f)
                            )
                        }
                        Text(
                            text = stringResource(R.string.days_format, expiryReminderDays),
                            style = MaterialTheme.typography.titleLarge,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    }
                    
                    Slider(
                        value = expiryReminderDays.toFloat(),
                        onValueChange = { expiryReminderDays = it.toInt() },
                        valueRange = 1f..30f,
                        steps = 28,
                        onValueChangeFinished = {
                            alertSettingsManager.setExpiryReminderDays(expiryReminderDays)
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.6f)
                        )
                        Text(
                            text = "30",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.6f)
                        )
                    }
                }
            }
            
            // 库存提醒阈值
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.low_stock_threshold),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.low_stock_threshold_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    OutlinedTextField(
                        value = lowStockThreshold.toString(),
                        onValueChange = { 
                            val value = it.toIntOrNull()
                            if (value != null && value > 0) {
                                lowStockThreshold = value
                                alertSettingsManager.setLowStockThreshold(value)
                            } else if (it.isEmpty()) {
                                // 允许清空，但不保存
                            }
                        },
                        label = { Text(stringResource(R.string.threshold_value)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val newValue = (lowStockThreshold - 1).coerceAtLeast(1)
                                    lowStockThreshold = newValue
                                    alertSettingsManager.setLowStockThreshold(newValue)
                                }
                            ) {
                                Icon(Icons.Default.Remove, null)
                            }
                        },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    val newValue = lowStockThreshold + 1
                                    lowStockThreshold = newValue
                                    alertSettingsManager.setLowStockThreshold(newValue)
                                }
                            ) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    )
                }
            }
            
            // 防遗忘提醒
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.forget_protection),
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        Text(
                            text = stringResource(R.string.forget_protection_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.6f)
                        )
                    }
                    Switch(
                        checked = forgetProtectionEnabled,
                        onCheckedChange = {
                            forgetProtectionEnabled = it
                            alertSettingsManager.setForgetProtectionEnabled(it)
                        }
                    )
                }
            }
            
            // 系统通知开关
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.system_notification),
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = stringResource(R.string.system_notification_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.6f)
                            )
                        }
                        Switch(
                            checked = systemNotificationEnabled,
                            onCheckedChange = {
                                systemNotificationEnabled = it
                                alertSettingsManager.setSystemNotificationEnabled(it)
                                // 重新调度通知
                                NotificationScheduler.scheduleNotifications(context)
                            }
                        )
                    }
                    
                    // 如果系统通知已启用，显示时间选择器
                    if (systemNotificationEnabled) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.notification_time),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                Text(
                                    text = String.format("%02d:%02d", notificationHour, notificationMinute),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ColorHelpers.getGroup4IconColor()
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 时间选择器对话框
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationHour)
            set(Calendar.MINUTE, notificationMinute)
        }
        var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
        var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
        
        ModernSettingsDialog(
            title = stringResource(R.string.select_notification_time),
            icon = Icons.Default.Schedule,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                notificationHour = selectedHour
                notificationMinute = selectedMinute
                alertSettingsManager.setNotificationHour(selectedHour)
                alertSettingsManager.setNotificationMinute(selectedMinute)
                // 重新调度通知以应用新的时间
                NotificationScheduler.scheduleNotifications(context)
                showTimePicker = false
            },
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 使用简单的数字输入框来选择时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.hour),
                            style = MaterialTheme.typography.labelMedium
                        )
                        OutlinedTextField(
                            value = selectedHour.toString(),
                            onValueChange = { 
                                val value = it.toIntOrNull()?.coerceIn(0, 23) ?: selectedHour
                                selectedHour = value
                            },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.minute),
                            style = MaterialTheme.typography.labelMedium
                        )
                        OutlinedTextField(
                            value = selectedMinute.toString(),
                            onValueChange = { 
                                val value = it.toIntOrNull()?.coerceIn(0, 59) ?: selectedMinute
                                selectedMinute = value
                            },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                }
            }
        }
    }
}

