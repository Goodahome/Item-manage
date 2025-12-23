package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit,
    activityEventViewModel: com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    val defaultAppName = context.getString(R.string.app_name)
    var appName by remember { mutableStateOf(prefs.getString("app_name", defaultAppName) ?: defaultAppName) }
    var isPasswordEnabled by remember { mutableStateOf(prefs.getBoolean("password_enabled", false)) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAppNameDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showSuffixDialog by remember { mutableStateOf(false) }
    var showAdUnitIdDialog by remember { mutableStateOf(false) }
    var showClearActivityDataDialog by remember { mutableStateOf(false) }
    
    val defaultSuffix = context.getString(R.string.warehouse_items_suffix)
    var warehouseItemsSuffix by remember { 
        mutableStateOf(prefs.getString("warehouse_items_suffix", defaultSuffix) ?: defaultSuffix) 
    }
    
    // 广告单元 ID
    var adBannerUnitId by remember { 
        mutableStateOf(prefs.getString("ad_banner_unit_id", null) ?: "") 
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.app_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 程序名称
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name_label)) },
                supportingContent = { 
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = { 
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorHelpers.getGroup4TextColor()
                        ),
                        onClick = { showAppNameDialog = true }) {
                        Text(stringResource(R.string.modify))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Divider()
            
            // 自定义容器物品后缀
            ListItem(
                headlineContent = { Text(stringResource(R.string.custom_warehouse_items_suffix)) },
                supportingContent = { 
                    Text(
                        text = stringResource(R.string.custom_warehouse_items_suffix_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = { 
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorHelpers.getGroup4TextColor()
                        ),
                        onClick = { showSuffixDialog = true }) {
                        Text(stringResource(R.string.modify))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Divider()
            
            // 广告横幅单元 ID
            ListItem(
                headlineContent = { Text(stringResource(R.string.ad_banner_unit_id)) },
                supportingContent = { 
                    Text(
                        text = if (adBannerUnitId.isNotEmpty()) {
                            adBannerUnitId
                        } else {
                            stringResource(R.string.ad_banner_unit_id_desc) + " (当前使用测试 ID)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = { 
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorHelpers.getGroup4TextColor()
                        ),
                        onClick = { showAdUnitIdDialog = true }) {
                        Text(stringResource(R.string.modify))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Divider()
            
            // 密码保护
            ListItem(
                headlineContent = { Text(stringResource(R.string.password_protection)) },
                supportingContent = { 
                    Text(
                        text = if (isPasswordEnabled) stringResource(R.string.password_enabled) else stringResource(R.string.password_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isPasswordEnabled,
                        onCheckedChange = {
                            isPasswordEnabled = it
                            prefs.edit().putBoolean("password_enabled", it).commit()
                            if (it) {
                                showPasswordDialog = true
                            } else {
                                // 如果禁用密码，清除密码
                                prefs.edit().putString("app_password", "").commit()
                            }
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Divider()
            
            // 清除动态数据
            if (activityEventViewModel != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.clear_activity_data)) },
                    supportingContent = { 
                        Text(
                            text = stringResource(R.string.clear_activity_data_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.7f)
                        )
                    },
                    trailingContent = { 
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = { showClearActivityDataDialog = true }) {
                            Text(stringResource(R.string.clear))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
    
    // 程序名称对话框
    if (showAppNameDialog) {
        var newAppName by remember { mutableStateOf(appName) }
        ModernSettingsDialog(
            title = stringResource(R.string.modify_app_name),
            icon = Icons.Default.Edit,
            onDismiss = { showAppNameDialog = false },
            onConfirm = {
                appName = newAppName
                prefs.edit().putString("app_name", newAppName).apply()
                prefs.edit().putBoolean("pending_name_change", true).apply()
                showAppNameDialog = false
                showRestartDialog = true
            },
            confirmEnabled = newAppName.isNotEmpty()
        ) {
            OutlinedTextField(
                value = newAppName,
                onValueChange = { newAppName = it },
                label = { Text(stringResource(R.string.app_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
    
    // 密码设置对话框
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        ModernSettingsDialog(
            title = stringResource(R.string.set_password),
            icon = Icons.Default.Lock,
            onDismiss = { 
                showPasswordDialog = false
                if (!isPasswordEnabled) {
                    isPasswordEnabled = false
                    prefs.edit().putBoolean("password_enabled", false).apply()
                }
            },
            onConfirm = {
                if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                    prefs.edit().putString("app_password", newPassword).commit()
                    prefs.edit().putBoolean("password_enabled", true).commit()
                    isPasswordEnabled = true
                    showPasswordDialog = false
                }
            },
            confirmEnabled = newPassword == confirmPassword && newPassword.isNotEmpty()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        }
    }
    
    // 自定义容器物品后缀对话框
    if (showSuffixDialog) {
        var newSuffix by remember { mutableStateOf(warehouseItemsSuffix) }
        ModernSettingsDialog(
            title = stringResource(R.string.custom_warehouse_items_suffix),
            icon = Icons.Default.Description,
            onDismiss = { showSuffixDialog = false },
            onConfirm = {
                warehouseItemsSuffix = newSuffix
                prefs.edit().putString("warehouse_items_suffix", newSuffix).apply()
                showSuffixDialog = false
            }
        ) {
            OutlinedTextField(
                value = newSuffix,
                onValueChange = { newSuffix = it },
                label = { Text(stringResource(R.string.warehouse_items_suffix)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
    
    // 广告单元 ID 设置对话框
    if (showAdUnitIdDialog) {
        var newAdUnitId by remember { mutableStateOf(adBannerUnitId) }
        ModernSettingsDialog(
            title = stringResource(R.string.ad_banner_unit_id),
            icon = Icons.Default.Campaign,
            onDismiss = { showAdUnitIdDialog = false },
            onConfirm = {
                adBannerUnitId = newAdUnitId.trim()
                if (adBannerUnitId.isEmpty()) {
                    // 如果为空，删除保存的值，使用默认测试 ID
                    prefs.edit().remove("ad_banner_unit_id").apply()
                } else {
                    // 保存新的广告单元 ID
                    prefs.edit().putString("ad_banner_unit_id", adBannerUnitId).apply()
                }
                showAdUnitIdDialog = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newAdUnitId,
                    onValueChange = { newAdUnitId = it },
                    label = { Text(stringResource(R.string.ad_banner_unit_id_hint)) },
                    placeholder = { Text(stringResource(R.string.ad_banner_unit_id_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.ad_banner_unit_id_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.7f)
                )
            }
        }
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        ModernSettingsDialog(
            title = stringResource(R.string.restart_app),
            icon = Icons.Default.Refresh,
            onDismiss = { showRestartDialog = false },
            onConfirm = {
                android.os.Process.killProcess(android.os.Process.myPid())
            },
            confirmText = stringResource(R.string.now),
            dismissText = stringResource(R.string.later)
        ) {
            Text(context.getString(R.string.app_name_changed))
        }
    }
    
    // 清除动态数据确认对话框
    if (showClearActivityDataDialog && activityEventViewModel != null) {
        ModernSettingsDialog(
            title = stringResource(R.string.clear_activity_data),
            icon = Icons.Default.Delete,
            onDismiss = { showClearActivityDataDialog = false },
            onConfirm = {
                activityEventViewModel.clearAllEvents()
                showClearActivityDataDialog = false
            },
            confirmText = stringResource(R.string.confirm_button),
            dismissText = stringResource(R.string.cancel_button)
        ) {
            Text(
                text = stringResource(R.string.clear_activity_data_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
        }
    }
}

/**
 * 统一的现代化设置对话框样式
 */
@Composable
fun ModernSettingsDialog(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmText: String = stringResource(R.string.ok),
    dismissText: String = stringResource(R.string.cancel),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorHelpers.getGroup3CardBgColor()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 顶部标题栏 - 现代化设计
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
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
                                icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                }
                
                // 底部按钮栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dismissText)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

