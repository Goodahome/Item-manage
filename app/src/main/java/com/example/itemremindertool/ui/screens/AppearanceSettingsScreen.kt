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
import com.example.itemremindertool.ui.theme.ColorSchemeType
import com.example.itemremindertool.ui.theme.ColorHelpers
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var selectedColorScheme by remember { mutableStateOf(prefs.getString("color_scheme", "cold_blue") ?: "cold_blue") }
    var selectedIcon by remember { mutableStateOf(IconManager.getCurrentIcon(context)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var showIconDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartReason by remember { mutableStateOf("") }
    
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
            TopAppBar(
                title = { Text(stringResource(R.string.appearance_settings)) },
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
                modifier = Modifier.clickable { showThemeDialog = true }
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
                modifier = Modifier.clickable { showColorSchemeDialog = true }
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
                modifier = Modifier.clickable { showIconDialog = true }
            )
        }
    }
    
    // 主题模式选择对话框（浅色/深色/跟随系统）
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = listOf("light", "dark", "system")
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = theme
                                    prefs.edit().putString("theme", theme).apply()
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == theme,
                                onClick = {
                                    selectedTheme = theme
                                    prefs.edit().putString("theme", theme).apply()
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (theme) {
                                    "light" -> stringResource(R.string.theme_light)
                                    "dark" -> stringResource(R.string.theme_dark)
                                    else -> stringResource(R.string.theme_system)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // 配色主题选择对话框（自动跟随主题模式）
    if (showColorSchemeDialog) {
        AlertDialog(
            onDismissRequest = { showColorSchemeDialog = false },
            title = { Text(stringResource(R.string.color_scheme)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colorSchemes = listOf(
                        "cold_blue" to R.string.color_scheme_cold_blue,
                        "cream" to R.string.color_scheme_cream,
                        "mint" to R.string.color_scheme_mint,
                        "space" to R.string.color_scheme_space,
                        "wine" to R.string.color_scheme_wine,
                        "christmas" to R.string.color_scheme_christmas
                    )
                    
                    colorSchemes.forEach { (schemeKey, stringResId) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedColorScheme = schemeKey
                                    prefs.edit().putString("color_scheme", schemeKey).apply()
                                    showColorSchemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedColorScheme == schemeKey,
                                onClick = {
                                    selectedColorScheme = schemeKey
                                    prefs.edit().putString("color_scheme", schemeKey).apply()
                                    showColorSchemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(stringResId),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorSchemeDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // 图标选择对话框
    if (showIconDialog) {
        AlertDialog(
            onDismissRequest = { showIconDialog = false },
            title = { Text(stringResource(R.string.app_icon)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconManager.getIconNames(context).forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIcon = index
                                    IconManager.switchIcon(context, index)
                                    restartReason = context.getString(R.string.icon_changed)
                                    showIconDialog = false
                                    showRestartDialog = true
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIcon == index,
                                onClick = {
                                    selectedIcon = index
                                    IconManager.switchIcon(context, index)
                                    restartReason = context.getString(R.string.icon_changed)
                                    showIconDialog = false
                                    showRestartDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_app)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(restartReason)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                ) {
                    Text(stringResource(R.string.now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}

