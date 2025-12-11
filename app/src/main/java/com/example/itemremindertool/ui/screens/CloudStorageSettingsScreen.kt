package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudStorageSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var cloudServerUrl by remember { mutableStateOf(prefs.getString("cloud_server_url", "") ?: "") }
    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", false)) }
    var showCloudServerDialog by remember { mutableStateOf(false) }
    
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
        ) {
            // 服务器地址
            ListItem(
                headlineContent = { Text(stringResource(R.string.cloud_server_url)) },
                supportingContent = { 
                    Text(
                        text = cloudServerUrl.ifEmpty { stringResource(R.string.cloud_server_not_set) },
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f),
                        maxLines = 1
                    )
                },
                trailingContent = { 
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorHelpers.getGroup4TextColor()
                        ),
                        onClick = { showCloudServerDialog = true }) {
                        Text(stringResource(R.string.settings))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Divider()
            
            // 自动同步
            ListItem(
                headlineContent = { Text(stringResource(R.string.auto_sync)) },
                supportingContent = { 
                    Text(
                        text = if (autoSyncEnabled) stringResource(R.string.auto_sync_enabled) else stringResource(R.string.auto_sync_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = {
                            autoSyncEnabled = it
                            prefs.edit().putBoolean("auto_sync_enabled", it).apply()
                        },
                        enabled = cloudServerUrl.isNotEmpty()
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
    
    val isDarkTheme = isSystemInDarkTheme()
    val dialogBackgroundColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.7f) // 深色模式：半透明黑色毛玻璃
    } else {
        Color.White.copy(alpha = 0.7f) // 浅色模式：半透明白色毛玻璃
    }
    
    // 云端服务器设置对话框
    if (showCloudServerDialog) {
        var newServerUrl by remember { mutableStateOf(cloudServerUrl) }
        AlertDialog(
            onDismissRequest = { showCloudServerDialog = false },
            containerColor = dialogBackgroundColor,
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
}

