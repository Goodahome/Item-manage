package com.example.itemremindertool.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
//import com.example.itemremindertool.ui.components.AppDivider
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.itemremindertool.ui.components.blockUserInput
import com.example.itemremindertool.ui.components.rememberScreenInteractionBlocker

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
    val blocker = rememberScreenInteractionBlocker()
    BackHandler { blocker.handleBack(onNavigateBack) }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { blocker.handleBack(onNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                .blockUserInput(blocker.isBlocked)
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
                    .clickable { blocker.handleForward(onNavigateToAppearance) }
                    .padding(vertical = 8.dp)
            )
                    

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
                    .clickable { blocker.handleForward(onNavigateToLanguage) }
                    .padding(vertical = 8.dp)
            )
            

            
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
                    .clickable { blocker.handleForward(onNavigateToWarehouse) }
                    .padding(vertical = 8.dp)
            )
            

            
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
                    .clickable { blocker.handleForward(onNavigateToAlert) }
                    .padding(vertical = 8.dp)
            )
            

            
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
                    .clickable { blocker.handleForward(onNavigateToApp) }
                    .padding(vertical = 8.dp)
            )
                    
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
                    .clickable { blocker.handleForward(onNavigateToCloudStorage) }
                    .padding(vertical = 8.dp)
            )
            

            
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
                    .clickable { blocker.handleForward(onNavigateToBackupRestore) }
                    .padding(vertical = 8.dp)
            )
                }
    }
}

