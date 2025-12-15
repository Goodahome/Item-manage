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
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.BackupRestoreViewModel
import com.example.itemremindertool.utils.DatabaseBackupUtils
import com.example.itemremindertool.utils.NextcloudBackupManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import android.content.Context
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
                            // 先显示成功消息
                            viewModel.showSuccess("数据库恢复成功！应用将自动重启以使更改生效。")
                            // 延迟一下再请求重建，确保成功消息能够显示
                            delay(1000)
                            // 在主线程上请求重建
                            withContext(Dispatchers.Main) {
                                com.example.itemremindertool.utils.AppRefreshManager.requestRecreate(context)
                            }
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
                                text = "离线备份",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            
                            Text(
                                text = "备份文件将保存到设备的Downloads目录，您可以在文件管理器中找到",
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
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorHelpers.getGroup5FabColor()
                                )
                            ) {
                                Icon(Icons.Default.Backup, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("备份到Downloads")
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
                                    text = "云端恢复",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                
                                Text(
                                    text = "从云端下载并恢复自动同步的备份文件（将直接恢复所有数据，请先进行离线备份）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorHelpers.getGroup4TextColor(0.7f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 云端恢复按钮
                                OutlinedButton(
                                    onClick = {
                                        showBackupWarningDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("从云端恢复")
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
                                text = "恢复数据",
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
                            OutlinedButton(
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
                title = { Text("重要提示", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("从云端恢复将直接替换所有现有数据！")
                        Text("强烈建议您先进行离线备份，否则现有数据将丢失。")
                        Text("确定要继续吗？", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBackupWarningDialog = false
                            showCloudRestoreDialog = true
                        }
                    ) {
                        Text("继续恢复", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupWarningDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 云端恢复确认对话框
        if (showCloudRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showCloudRestoreDialog = false },
                title = { Text("确认恢复") },
                text = {
                    Text("确定要从云端恢复备份吗？这将替换所有现有数据。")
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
                                                    // 先显示成功消息
                                                    viewModel.showSuccess("云端恢复成功！应用将自动重启以使更改生效。")
                                                    // 延迟一下再请求重建，确保成功消息能够显示
                                                    delay(1000)
                                                    // 在主线程上请求重建
                                                    withContext(Dispatchers.Main) {
                                                        com.example.itemremindertool.utils.AppRefreshManager.requestRecreate(context)
                                                    }
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
                        Text("确认", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloudRestoreDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    } // 关闭外层 Box
}

