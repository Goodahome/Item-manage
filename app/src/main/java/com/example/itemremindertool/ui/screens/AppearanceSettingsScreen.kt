package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.utils.IconManager
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToColorScheme: () -> Unit,
    onNavigateToIcon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var selectedColorScheme by remember { mutableStateOf(prefs.getString("color_scheme", "cold_blue") ?: "cold_blue") }
    var selectedIcon by remember { mutableStateOf(IconManager.getCurrentIcon(context)) }
    
    // 监听 SharedPreferences 变化
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "theme" -> selectedTheme = prefs.getString("theme", "system") ?: "system"
                "color_scheme" -> selectedColorScheme = prefs.getString("color_scheme", "cold_blue") ?: "cold_blue"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.appearance_settings)) },
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
            // 主题模式设置（浅色/深色/跟随系统）
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = { 
                    Text(
                        text = when (selectedTheme) {
                            "light" -> stringResource(R.string.theme_light)
                            "dark" -> stringResource(R.string.theme_dark)
                            else -> stringResource(R.string.theme_system)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onNavigateToTheme() }
            )
            
            Divider()
            
            // 配色主题设置（自动跟随主题模式）
            ListItem(
                headlineContent = { Text(stringResource(R.string.color_scheme)) },
                supportingContent = { 
                    Text(
                        text = when (selectedColorScheme) {
                            "cold_blue" -> stringResource(R.string.color_scheme_cold_blue)
                            "cream" -> stringResource(R.string.color_scheme_cream)
                            "mint" -> stringResource(R.string.color_scheme_mint)
                            "space" -> stringResource(R.string.color_scheme_space)
                            "wine" -> stringResource(R.string.color_scheme_wine)
                            "christmas" -> stringResource(R.string.color_scheme_christmas)
                            else -> stringResource(R.string.color_scheme_cold_blue)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onNavigateToColorScheme() }
            )
            
            Divider()
            
            // 应用图标设置
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_icon)) },
                supportingContent = { 
                    Text(
                        text = IconManager.getIconNames(context)[selectedIcon],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onNavigateToIcon() }
            )
        }
    }
}

