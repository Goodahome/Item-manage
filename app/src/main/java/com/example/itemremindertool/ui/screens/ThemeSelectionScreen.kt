package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    onNavigateBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.theme)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                        prefs.edit().putString("theme", selectedTheme).apply()
                        onApply()
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val themes = listOf(
                "light" to R.string.theme_light,
                "dark" to R.string.theme_dark,
                "system" to R.string.theme_system
            )
            
            themes.forEach { (themeKey, stringResId) ->
                ListItem(
                    headlineContent = { 
                        Text(
                            text = stringResource(stringResId),
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = selectedTheme == themeKey,
                            onClick = { selectedTheme = themeKey }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        selectedTheme = themeKey
                    }
                )
                if (themeKey != themes.last().first) {
                    HorizontalDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 4.dp
                    )
                }
            }
        }
    }

}

