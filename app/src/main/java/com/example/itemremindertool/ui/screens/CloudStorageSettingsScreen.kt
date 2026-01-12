package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.itemremindertool.utils.NextcloudBackupManager
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.ui.viewmodel.CloudStorageViewModel
import com.example.itemremindertool.ui.viewmodel.OperationState
import android.app.Activity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudStorageSettingsScreen(
    viewModel: CloudStorageViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    // 确保正确观察状态
    val operationState by viewModel.operationState.collectAsState(initial = OperationState.Idle)
    
    // 调试：监听状态变化
    LaunchedEffect(operationState) {
        android.util.Log.d("CloudStorageSettings", "operationState 变化: $operationState")
    }
    
    // Nextcloud 配置
    var nextcloudServerUrl by remember { 
        mutableStateOf(prefs.getString("nextcloud_server_url", "") ?: "") 
    }
    var nextcloudUsername by remember { 
        mutableStateOf(prefs.getString("nextcloud_username", "") ?: "") 
    }
    var nextcloudPassword by remember { 
        mutableStateOf(prefs.getString("nextcloud_password", "") ?: "") 
    }
    var autoSyncEnabled by remember { 
        mutableStateOf(prefs.getBoolean("auto_sync_enabled", false)) 
    }
    
    // UI 状态
    var showNextcloudConfigDialog by remember { mutableStateOf(false) }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    
    // 检查高级功能访问权限
    val canAccessPremiumFeatures = remember {
        PremiumFeatureManager.canAccessPremiumFeatures(context)
    }
    
    // Billing Manager
    val activity = context as? Activity
    val billingManager = remember {
        BillingManager(context, listOf(BillingManager.PRODUCT_REMOVE_ADS, BillingManager.PRODUCT_PREMIUM_FEATURES)).apply {
            initialize()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GradientTopAppBar(
                    title = { Text(stringResource(R.string.cloud_storage)) },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nextcloud 配置卡片
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.nextcloud_settings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            if (nextcloudServerUrl.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.nextcloud_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorHelpers.getGroup4TextColor(0.7f),
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.nextcloud_not_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorHelpers.getGroup4TextColor(0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(onClick = {
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                            } else {
                                showNextcloudConfigDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Settings, null, tint = ColorHelpers.getGroup4IconColor())
                        }
                    }
                    
                    Divider()
                    
                    // 测试连接按钮
                    val isConfigComplete = nextcloudServerUrl.isNotEmpty() && nextcloudUsername.isNotEmpty() && nextcloudPassword.isNotEmpty()
                    
                    Button(
                        onClick = {
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                                return@Button
                            }
                            android.util.Log.d("CloudStorageSettings", "测试连接按钮被点击")
                            if (!isConfigComplete) {
                                android.util.Log.d("CloudStorageSettings", "配置信息不完整")
                                viewModel.showError("请先配置 Nextcloud 服务器信息")
                                return@Button
                            }
                            
                            android.util.Log.d("CloudStorageSettings", "开始执行连接测试，服务器: $nextcloudServerUrl, 用户: $nextcloudUsername")
                            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                viewModel.showSaving()
                                try {
                                    android.util.Log.d("CloudStorageSettings", "切换到IO线程执行连接测试")
                                    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        android.util.Log.d("CloudStorageSettings", "调用 NextcloudBackupManager.testConnection")
                                        NextcloudBackupManager.testConnection(
                                            nextcloudServerUrl,
                                            nextcloudUsername,
                                            nextcloudPassword
                                        )
                                    }
                                    
                                    android.util.Log.d("CloudStorageSettings", "连接测试结果: success=${result.isSuccess}")
                                    
                                    // 根据结果更新状态（确保在主线程）
                                    if (result.isSuccess) {
                                        android.util.Log.d("CloudStorageSettings", "连接测试成功")
                                        viewModel.showSuccess("连接成功！")
                                    } else {
                                        val error = result.exceptionOrNull()
                                        android.util.Log.e("CloudStorageSettings", "连接测试失败: ${error?.message}", error)
                                        viewModel.showError("连接失败: ${error?.message ?: "未知错误"}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CloudStorageSettings", "连接测试异常", e)
                                    viewModel.showError("连接失败: ${e.message ?: "未知错误"}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isConfigComplete
                    ) {
                        Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.test_connection))
                    }
                    
                    Divider()
                    
                    // 自动同步开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.auto_sync),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = if (autoSyncEnabled) stringResource(R.string.auto_sync_enabled) else stringResource(R.string.auto_sync_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            enabled = canAccessPremiumFeatures && nextcloudServerUrl.isNotEmpty() && nextcloudUsername.isNotEmpty() && nextcloudPassword.isNotEmpty(),
                            onCheckedChange = {
                                if (!canAccessPremiumFeatures) {
                                    showPremiumFeatureDialog = true
                                    return@Switch
                                }
                                autoSyncEnabled = it
                                prefs.edit().putBoolean("auto_sync_enabled", it).apply()
                                
                                // 根据开关状态调度或取消自动同步
                                if (it && nextcloudServerUrl.isNotEmpty() && nextcloudUsername.isNotEmpty() && nextcloudPassword.isNotEmpty()) {
                                    com.example.itemremindertool.utils.CloudSyncScheduler.scheduleSync(context)
                                    viewModel.showSuccess("自动同步已启用")
                                } else {
                                    com.example.itemremindertool.utils.CloudSyncScheduler.cancelSync(context)
                                    viewModel.showSuccess("自动同步已禁用")
                                }
                            }
                        )
                    }
                }
            }
        } // 关闭 Scaffold 的 content lambda
        
        // 底部状态指示器
        BottomOperationStatusIndicator(
            operationState = operationState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    } // 关闭外层 Box
}
    
    // Nextcloud 配置对话框
    if (showNextcloudConfigDialog) {
        NextcloudConfigDialog(
            serverUrl = nextcloudServerUrl,
            username = nextcloudUsername,
            password = nextcloudPassword,
            onDismiss = { showNextcloudConfigDialog = false },
            onSave = { url, user, pass ->
                nextcloudServerUrl = url
                nextcloudUsername = user
                nextcloudPassword = pass
                prefs.edit()
                    .putString("nextcloud_server_url", url)
                    .putString("nextcloud_username", user)
                    .putString("nextcloud_password", pass)
                    .apply()
                if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                    autoSyncEnabled = false
                    prefs.edit().putBoolean("auto_sync_enabled", false).apply()
                    com.example.itemremindertool.utils.CloudSyncScheduler.cancelSync(context)
                } else if (autoSyncEnabled) {
                    // 如果自动同步已启用，重新调度
                    com.example.itemremindertool.utils.CloudSyncScheduler.scheduleSync(context)
                }
                showNextcloudConfigDialog = false
            }
        )
    }
    
    // 高级功能对话框
    if (showPremiumFeatureDialog) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
}

@Composable
private fun NextcloudConfigDialog(
    serverUrl: String,
    username: String,
    password: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var newServerUrl by remember { mutableStateOf(serverUrl) }
    var newUsername by remember { mutableStateOf(username) }
    var newPassword by remember { mutableStateOf(password) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val isDarkTheme = isSystemInDarkTheme()
    val dialogBackgroundColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.7f)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBackgroundColor,
        title = { Text(stringResource(R.string.nextcloud_settings)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newServerUrl,
                    onValueChange = { newServerUrl = it },
                    label = { Text(stringResource(R.string.nextcloud_server_url)) },
                    placeholder = { Text("https://nextcloud.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text(stringResource(R.string.nextcloud_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.nextcloud_password)) },
                    placeholder = { Text(stringResource(R.string.nextcloud_password_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(newServerUrl.trim(), newUsername.trim(), newPassword)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

