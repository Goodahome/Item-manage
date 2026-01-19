package com.example.itemremindertool.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.itemremindertool.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit,
    onApply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var selectedLanguage by remember { mutableStateOf(prefs.getString("language", "zh") ?: "zh") }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.language)) },
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
                        // 使用 commit 确保立即写入，避免重启过快导致未持久化
                        prefs.edit().putString("language", selectedLanguage).commit()
                        // 清栈重启，避免华为平板透明旧窗口残留导致黑屏
                        val activity = context as? Activity
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                        activity?.finishAffinity()
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
            modifier = Modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val languages = listOf("zh", "en", "fr", "de")
            languages.forEach { lang ->
                ListItem(
                    headlineContent = { 
                        Text(
                            text = when (lang) {
                                "zh" -> stringResource(R.string.language_zh)
                                "en" -> stringResource(R.string.language_en)
                                "fr" -> stringResource(R.string.language_fr)
                                "de" -> stringResource(R.string.language_de)
                                else -> lang
                            }
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = selectedLanguage == lang,
                            onClick = {
                                selectedLanguage = lang
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        selectedLanguage = lang
                    }
                )
                if (lang != languages.last()) {
                    Divider()
                }
            }
        }
    }
}

