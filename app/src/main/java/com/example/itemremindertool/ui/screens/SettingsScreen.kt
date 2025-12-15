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
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToWarehouse: () -> Unit = {},
    onNavigateToApp: () -> Unit = {},
    onNavigateToCloudStorage: () -> Unit = {},
    onNavigateToAlert: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            GradientTopAppBar(
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
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 外观设置
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.appearance_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToAppearance() }
                    .padding(vertical = 8.dp)
            )
                    
                    Divider()
                    
                    // 语言设置
            ListItem(
                headlineContent = { 
                            Text(
                                text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToLanguage() }
                    .padding(vertical = 8.dp)
            )
            
            Divider()
            
            // 容器设置
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.warehouse_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToWarehouse() }
                    .padding(vertical = 8.dp)
            )
            
            Divider()
            
            // 提醒设置
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.alert_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToAlert() }
                    .padding(vertical = 8.dp)
            )
            
            Divider()
            
            // 应用设置
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToApp() }
                    .padding(vertical = 8.dp)
            )
                    
                    Divider()
                    
            // 云端存储设置
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.cloud_storage),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToCloudStorage() }
                    .padding(vertical = 8.dp)
            )
            
            Divider()
            
            // 数据备份和恢复
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.backup_restore),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = ColorHelpers.getGroup4IconColor(0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clickable { onNavigateToBackupRestore() }
                    .padding(vertical = 8.dp)
            )
                }
    }
}

