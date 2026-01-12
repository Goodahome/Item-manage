package com.example.itemremindertool.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.itemremindertool.R
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.BackupRestoreViewModel
import com.example.itemremindertool.utils.DatabaseBackupUtils
import com.example.itemremindertool.utils.NextcloudBackupManager
import android.app.Activity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.content.Intent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    val operationState by viewModel.operationState.collectAsState()
    
    // Nextcloud 配置（用于云端恢复）
    val nextcloudServerUrl = remember { prefs.getString("nextcloud_server_url", "") ?: "" }
    val nextcloudUsername = remember { prefs.getString("nextcloud_username", "") ?: "" }
    val nextcloudPassword = remember { prefs.getString("nextcloud_password", "") ?: "" }
    val isNextcloudConfigured = nextcloudServerUrl.isNotEmpty() && 
                                 nextcloudUsername.isNotEmpty() && 
                                 nextcloudPassword.isNotEmpty()
    
    // 云端恢复确认对话框状态
    var showCloudRestoreDialog by remember { mutableStateOf(false) }
    var showBackupWarningDialog by remember { mutableStateOf(false) }
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
    
    // 权限相关
    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要 WRITE_EXTERNAL_STORAGE 权限，MediaStore API 已经足够
            true
        } else {
            // Android 9 及以下需要 WRITE_EXTERNAL_STORAGE 权限
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // 权限请求 Launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限授予后执行备份
            scope.launch {
                viewModel.showSaving()
                try {
                    val result = DatabaseBackupUtils.backupToDownloads(context)
                    result.fold(
                        onSuccess = {
                            viewModel.showSuccess("备份成功！文件已保存到Downloads目录")
                        },
                        onFailure = { e ->
                            viewModel.showError("备份失败: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    viewModel.showError("备份失败: ${e.message}")
                }
            }
        } else {
            viewModel.showError("需要存储权限才能保存备份文件到Downloads目录")
        }
    }
    
    // 文件选择器（用于恢复）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                viewModel.showSaving()
                try {
                    val result = DatabaseBackupUtils.restoreDatabase(context, it)
                    result.fold(
                        onSuccess = {
                            // 处理恢复成功后的逻辑（立即重启应用）
                            handleRestoreSuccess(context, viewModel, "数据恢复成功！程序将立即重启...")
                        },
                        onFailure = { e ->
                            viewModel.showError("恢复失败: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    viewModel.showError("恢复失败: ${e.message}")
                }
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GradientTopAppBar(
                    title = { Text(stringResource(R.string.backup_restore)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 离线备份卡片
                item {
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
                            Text(
                                text = stringResource(R.string.offline_backup),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            
                            Text(
                                text = stringResource(R.string.backup_file_location),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 离线备份按钮
                            Button(
                                onClick = {
                                    if (hasStoragePermission) {
                                        // 已有权限，直接执行备份
                                        scope.launch {
                                            viewModel.showSaving()
                                            try {
                                                val result = DatabaseBackupUtils.backupToDownloads(context)
                                                result.fold(
                                                    onSuccess = {
                                                        viewModel.showSuccess("备份成功！文件已保存到Downloads目录")
                                                    },
                                                    onFailure = { e ->
                                                        viewModel.showError("备份失败: ${e.message}")
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                viewModel.showError("备份失败: ${e.message}")
                                            }
                                        }
                                    } else {
                                        // 请求权限（仅 Android 9 及以下需要）
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        } else {
                                            // Android 10+ 理论上不需要权限，如果失败可能是其他原因
                                            scope.launch {
                                                viewModel.showSaving()
                                                try {
                                                    val result = DatabaseBackupUtils.backupToDownloads(context)
                                                    result.fold(
                                                        onSuccess = {
                                                            viewModel.showSuccess("备份成功！文件已保存到Downloads目录")
                                                        },
                                                        onFailure = { e ->
                                                            viewModel.showError("备份失败: ${e.message}")
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    viewModel.showError("备份失败: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Backup, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_to_downloads))    // 修改
                            }
                        }
                    }
                }
                
                // 云端恢复卡片（如果已配置）
                if (isNextcloudConfigured) {
                    item {
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
                                Text(
                                    text = stringResource(R.string.cloud_restore),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                
                                Text(
                                    text = stringResource(R.string.cloud_restore_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorHelpers.getGroup4TextColor(0.7f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 云端恢复按钮
                                Button(
                                    onClick = {
                                        if (!canAccessPremiumFeatures) {
                                            showPremiumFeatureDialog = true
                                        } else {
                                            showBackupWarningDialog = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.restore_from_cloud))
                                }
                            }
                        }
                    }
                }
                
                // 恢复数据卡片
                item {
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
                            Text(
                                text = stringResource(R.string.restore_data_action),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            
                            Text(
                                text = stringResource(R.string.backup_restore_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 恢复按钮
                            Button(
                                onClick = {
                                    filePickerLauncher.launch("application/zip")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.restore_data))
                            }
                        }
                    }
                }
                
            }
        } // 关闭 Scaffold 的 content lambda
        
        // 底部状态指示器
        BottomOperationStatusIndicator(
            operationState = operationState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // 备份警告对话框
        if (showBackupWarningDialog) {
            AlertDialog(
                onDismissRequest = { showBackupWarningDialog = false },
                title = { Text(stringResource(R.string.important_notice), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cloud_restore_warning))
                        Text(stringResource(R.string.cloud_restore_suggestion))
                        Text(stringResource(R.string.confirm_continue), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBackupWarningDialog = false
                            showCloudRestoreDialog = true
                        }
                    ) {
                        Text(stringResource(R.string.continue_restore), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupWarningDialog = false }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }
        
        // 云端恢复确认对话框
        if (showCloudRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showCloudRestoreDialog = false },
                title = { Text(stringResource(R.string.confirm_restore)) },
                text = {
                    Text(stringResource(R.string.confirm_restore_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCloudRestoreDialog = false
                            scope.launch {
                                viewModel.showSaving()
                                try {
                                    // 1. 列出云端备份文件
                                    val backupsResult = NextcloudBackupManager.listBackups(
                                        nextcloudServerUrl,
                                        nextcloudUsername,
                                        nextcloudPassword
                                    )
                                    
                                    val backups = backupsResult.getOrNull()
                                    if (backups.isNullOrEmpty()) {
                                        viewModel.showError("云端没有找到备份文件")
                                        return@launch
                                    }
                                    
                                    // 2. 找到最新的备份文件（通常是 item_reminder_backup_latest.zip）
                                    val latestBackup = backups.firstOrNull { 
                                        it.endsWith("item_reminder_backup_latest.zip")
                                    } ?: backups.maxByOrNull { backupPath ->
                                        try {
                                            val fileName = backupPath.substringAfterLast("/")
                                            if (fileName.startsWith("item_reminder_backup_") && fileName.endsWith(".zip")) {
                                                val timestampStr = fileName
                                                    .removePrefix("item_reminder_backup_")
                                                    .removeSuffix(".zip")
                                                val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                                                dateFormat.parse(timestampStr)?.time ?: 0L
                                            } else {
                                                0L
                                            }
                                        } catch (e: Exception) {
                                            0L
                                        }
                                    }
                                    
                                    if (latestBackup == null) {
                                        viewModel.showError("无法确定最新的备份文件")
                                        return@launch
                                    }
                                    
                                    // 3. 创建临时目录和文件
                                    val tempDir = File(context.cacheDir, "cloud_restore_temp")
                                    if (tempDir.exists()) {
                                        tempDir.deleteRecursively()
                                    }
                                    tempDir.mkdirs()
                                    
                                    val tempBackupFile = File(tempDir, latestBackup.substringAfterLast("/"))
                                    
                                    // 4. 下载备份文件
                                    val downloadResult = NextcloudBackupManager.downloadBackup(
                                        context,
                                        latestBackup,
                                        tempBackupFile,
                                        nextcloudServerUrl,
                                        nextcloudUsername,
                                        nextcloudPassword
                                    )
                                    
                                    downloadResult.fold(
                                        onSuccess = { downloadedFile ->
                                            // 5. 恢复数据库（完整恢复，替换所有数据）
                                            val restoreResult = DatabaseBackupUtils.restoreDatabaseFromFile(
                                                context,
                                                downloadedFile
                                            )
                                            
                                            restoreResult.fold(
                                                onSuccess = {
                                                    // 清理临时文件
                                                    tempDir.deleteRecursively()
                                                    // 处理恢复成功后的逻辑（立即重启应用）
                                                    handleRestoreSuccess(context, viewModel, "云端恢复成功！程序将立即重启...")
                                                },
                                                onFailure = { e ->
                                                    tempDir.deleteRecursively()
                                                    viewModel.showError("恢复失败: ${e.message}")
                                                }
                                            )
                                        },
                                        onFailure = { e ->
                                            tempDir.deleteRecursively()
                                            viewModel.showError("下载备份失败: ${e.message}")
                                        }
                                    )
                                } catch (e: Exception) {
                                    viewModel.showError("云端恢复失败: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm_button), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloudRestoreDialog = false }) {
                        Text(stringResource(R.string.cancel_button))
                    }
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
    } // 关闭外层 Box
}

/**
 * 处理恢复成功后的逻辑：立即重启应用
 */
private suspend fun handleRestoreSuccess(
    context: Context,
    viewModel: BackupRestoreViewModel,
    message: String = "数据恢复成功！程序将立即重启..."
) {
    // 显示成功消息（不等待，立即重启）
    withContext(Dispatchers.Main) {
        viewModel.showSuccess(message)
        // 立即重启整个应用，不延迟
        restartApplication(context)
    }
}

/**
 * 重启整个应用
 */
private fun restartApplication(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
    // 结束当前进程
    android.os.Process.killProcess(android.os.Process.myPid())
    System.exit(0)
}

