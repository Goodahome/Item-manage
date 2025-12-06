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
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var unlimitedContainers by remember { mutableStateOf(prefs.getBoolean("unlimited_containers", false)) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.warehouse_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorHelpers.getGroup1NavBarColor(),
                    titleContentColor = ColorHelpers.getGroup4TextColor(),
                    navigationIconContentColor = ColorHelpers.getGroup4IconColor(),
                    actionIconContentColor = ColorHelpers.getGroup4IconColor()
                )
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
            // 无限容器模式开关
            ListItem(
                headlineContent = { Text(stringResource(R.string.unlimited_containers)) },
                supportingContent = { 
                    Text(
                        text = stringResource(R.string.unlimited_containers_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = unlimitedContainers,
                        onCheckedChange = { enabled ->
                            unlimitedContainers = enabled
                            prefs.edit().putBoolean("unlimited_containers", enabled).apply()
                        }
                    )
                }
            )
        }
    }
}

