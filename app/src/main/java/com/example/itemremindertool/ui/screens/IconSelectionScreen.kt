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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import androidx.compose.foundation.background
import com.example.itemremindertool.utils.IconManager
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconSelectionScreen(
    onNavigateBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var selectedIcon by remember { mutableStateOf(IconManager.getCurrentIcon(context)) }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.app_icon)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)
            ) {
                // 使用与侧边栏风格首页一致的悬浮按钮样式
                val fabBackground = ColorHelpers.getGroup2SettingsBtnColor()
                val fabIconColor = ColorHelpers.getContrastColor(fabBackground)
                
                FloatingActionButton(
                    onClick = {
                        showRestartDialog = true
                    },
                    containerColor = fabBackground,
                    contentColor = fabIconColor,
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Check,
                        stringResource(R.string.apply),
                        tint = fabIconColor
                    )
                }
            }
        }
    ) { paddingValues ->
        val iconResIds = remember { IconManager.getIconResIds() }
        val iconNames = remember { IconManager.getIconNames(context) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            iconNames.forEachIndexed { index, name ->
                val iconResId = iconResIds.getOrNull(index) ?: R.mipmap.ic_launcher
                ListItem(
                    leadingContent = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ColorHelpers.getGroup3CardBgColor()
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(6.dp)
                            )
                        }
                    },
                    headlineContent = { 
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = selectedIcon == index,
                            onClick = {
                                selectedIcon = index
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        selectedIcon = index
                    }
                )
                if (index < iconNames.size - 1) {
                    Divider()
                }
            }
        }
    }

    // 重启提示对话框
    if (showRestartDialog) {
        ModernSettingsDialog(
            title = stringResource(R.string.restart_app),
            icon = Icons.Default.Refresh,
            onDismiss = { showRestartDialog = false },
            onDismissAction = {
                IconManager.switchIcon(context, selectedIcon, commit = true)
                showRestartDialog = false
                onApply()
            },
            onConfirm = {
                IconManager.switchIcon(context, selectedIcon, commit = true)
                android.os.Process.killProcess(android.os.Process.myPid())
            },
            confirmText = stringResource(R.string.now),
            dismissText = stringResource(R.string.later)
        ) {
            Text(
                text = stringResource(R.string.icon_changed),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
        }
    }
}

