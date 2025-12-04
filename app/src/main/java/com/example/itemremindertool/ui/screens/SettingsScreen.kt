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
    modifier: Modifier = Modifier
) {
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
        ) {
            // 外观设置
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.appearance_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
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
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
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
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
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
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
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
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
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
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                modifier = Modifier
                    .clickable { onNavigateToCloudStorage() }
                    .padding(vertical = 8.dp)
            )
                }
    }
}

