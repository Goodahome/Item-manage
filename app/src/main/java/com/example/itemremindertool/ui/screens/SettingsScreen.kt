package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    // 设置状态 - 使用 remember 和 LaunchedEffect 来响应设置变化
    val defaultAppName = context.getString(R.string.app_name)
    var appName by remember { mutableStateOf(prefs.getString("app_name", defaultAppName) ?: defaultAppName) }
    var isPasswordEnabled by remember { mutableStateOf(prefs.getBoolean("password_enabled", false)) }
    var password by remember { mutableStateOf("") }
    var unlimitedContainers by remember { mutableStateOf(prefs.getBoolean("unlimited_containers", false)) }
    var cloudServerUrl by remember { mutableStateOf(prefs.getString("cloud_server_url", "") ?: "") }
    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", false)) }
    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("language", "zh") ?: "zh") }
    
    // 监听设置变化并更新状态
    LaunchedEffect(Unit) {
        // 定期检查设置变化（用于从其他页面返回时更新）
        // 注意：主题和语言的变化需要重启应用才能完全生效
    }
    
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAppNameDialog by remember { mutableStateOf(false) }
    var showCloudServerDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartReason by remember { mutableStateOf("") }
    
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 外观设置 ====================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.appearance_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // 主题设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.theme),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (selectedTheme) {
                                    "light" -> stringResource(R.string.theme_light)
                                    "dark" -> stringResource(R.string.theme_dark)
                                    else -> stringResource(R.string.theme_system)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedTheme == "light",
                                onClick = {
                                    selectedTheme = "light"
                                    prefs.edit().putString("theme", "light").apply()
                                },
                                label = { Text(stringResource(R.string.theme_light)) }
                            )
                            FilterChip(
                                selected = selectedTheme == "dark",
                                onClick = {
                                    selectedTheme = "dark"
                                    prefs.edit().putString("theme", "dark").apply()
                                },
                                label = { Text(stringResource(R.string.theme_dark)) }
                            )
                            FilterChip(
                                selected = selectedTheme == "system",
                                onClick = {
                                    selectedTheme = "system"
                                    prefs.edit().putString("theme", "system").apply()
                                },
                                label = { Text(stringResource(R.string.theme_system)) }
                            )
                        }
                    }
                    
                    Divider()
                    
                    // 语言设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (selectedLanguage) {
                                    "zh" -> stringResource(R.string.language_zh)
                                    "en" -> stringResource(R.string.language_en)
                                    "fr" -> stringResource(R.string.language_fr)
                                    "de" -> stringResource(R.string.language_de)
                                    "es" -> stringResource(R.string.language_es)
                                    "it" -> stringResource(R.string.language_it)
                                    "pt" -> stringResource(R.string.language_pt)
                                    else -> stringResource(R.string.language_zh)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val languages = listOf("zh", "en", "fr", "de", "es", "it", "pt")
                            val restartPrompt = context.getString(R.string.restart_app)
                            languages.forEach { lang ->
                                val label = when (lang) {
                                    "zh" -> stringResource(R.string.language_zh)
                                    "en" -> stringResource(R.string.language_en)
                                    "fr" -> stringResource(R.string.language_fr)
                                    "de" -> stringResource(R.string.language_de)
                                    "es" -> stringResource(R.string.language_es)
                                    "it" -> stringResource(R.string.language_it)
                                    "pt" -> stringResource(R.string.language_pt)
                                    else -> lang
                                }
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = {
                                        selectedLanguage = lang
                                        prefs.edit().putString("language", lang).apply()
                                        restartReason = restartPrompt
                                        showRestartDialog = true
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
            
            // ==================== 容器设置 ====================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.warehouse_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // 无限容器模式开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.unlimited_containers),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.unlimited_containers_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = unlimitedContainers,
                            onCheckedChange = { enabled ->
                                unlimitedContainers = enabled
                                prefs.edit().putBoolean("unlimited_containers", enabled).apply()
                            }
                        )
                    }
                }
            }
            
            // ==================== 应用设置 ====================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // 程序名称
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name_label),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        TextButton(onClick = { showAppNameDialog = true }) {
                            Text(stringResource(R.string.modify))
                        }
                    }
                    
                    Divider()
                    
                    // 密码保护
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.password_protection),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (isPasswordEnabled) stringResource(R.string.password_enabled) else stringResource(R.string.password_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = isPasswordEnabled,
                            onCheckedChange = {
                                isPasswordEnabled = it
                                prefs.edit().putBoolean("password_enabled", it).apply()
                                if (it) {
                                    showPasswordDialog = true
                                }
                            }
                        )
                    }
                }
            }
            
            // ==================== 云端存储设置 ====================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cloud_storage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // 服务器地址
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cloud_server_url),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = cloudServerUrl.ifEmpty { stringResource(R.string.cloud_server_not_set) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                        TextButton(onClick = { showCloudServerDialog = true }) {
                            Text(stringResource(R.string.settings))
                        }
                    }
                    
                    Divider()
                    
                    // 自动同步
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.auto_sync),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (autoSyncEnabled) stringResource(R.string.auto_sync_enabled) else stringResource(R.string.auto_sync_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = {
                                autoSyncEnabled = it
                                prefs.edit().putBoolean("auto_sync_enabled", it).apply()
                            },
                            enabled = cloudServerUrl.isNotEmpty()
                        )
                    }
                }
            }
        }
    }
    
    // ==================== 对话框 ====================
    
    // 程序名称对话框
    if (showAppNameDialog) {
        var newAppName by remember { mutableStateOf(appName) }
        AlertDialog(
            onDismissRequest = { showAppNameDialog = false },
            title = { Text(stringResource(R.string.modify_app_name)) },
            text = {
                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text(stringResource(R.string.app_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val appNameChangedMessage = context.getString(R.string.app_name_changed)
                        appName = newAppName
                        prefs.edit().putString("app_name", newAppName).apply()
                        // 标记需要重启应用才能应用系统级别的更改
                        prefs.edit().putBoolean("pending_name_change", true).apply()
                        showAppNameDialog = false
                        // 提示需要重启应用才能更新系统显示的名称
                        restartReason = appNameChangedMessage
                        showRestartDialog = true
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 密码设置对话框
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false
                if (!isPasswordEnabled) {
                    isPasswordEnabled = false
                    prefs.edit().putBoolean("password_enabled", false).apply()
                }
            },
            title = { Text(stringResource(R.string.set_password)) },
            text = {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                            prefs.edit().putString("app_password", newPassword).apply()
                            prefs.edit().putBoolean("password_enabled", true).apply()
                            showPasswordDialog = false
                        }
                    },
                    enabled = newPassword == confirmPassword && newPassword.isNotEmpty()
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPasswordDialog = false
                    if (!isPasswordEnabled) {
                        isPasswordEnabled = false
                        prefs.edit().putBoolean("password_enabled", false).apply()
                    }
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 云端服务器设置对话框
    if (showCloudServerDialog) {
        var newServerUrl by remember { mutableStateOf(cloudServerUrl) }
        AlertDialog(
            onDismissRequest = { showCloudServerDialog = false },
            title = { Text(stringResource(R.string.set_cloud_server)) },
            text = {
                OutlinedTextField(
                    value = newServerUrl,
                    onValueChange = { newServerUrl = it },
                    label = { Text(stringResource(R.string.server_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.server_url_hint)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cloudServerUrl = newServerUrl
                        prefs.edit().putString("cloud_server_url", newServerUrl).apply()
                        if (newServerUrl.isEmpty()) {
                            autoSyncEnabled = false
                            prefs.edit().putBoolean("auto_sync_enabled", false).apply()
                        }
                        showCloudServerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudServerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_app)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(restartReason)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 重启应用
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                ) {
                    Text(stringResource(R.string.now))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRestartDialog = false
                    // 选择"稍后"时，设置会在下次重启时自动应用
                }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}

