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
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var selectedLanguage by remember { mutableStateOf(prefs.getString("language", "zh") ?: "zh") }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language)) },
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
            val languages = listOf("zh", "en", "fr", "de", "es", "it", "pt")
            languages.forEach { lang ->
                ListItem(
                    headlineContent = { 
                        Text(
                            text = when (lang) {
                                "zh" -> stringResource(R.string.language_zh)
                                "en" -> stringResource(R.string.language_en)
                                "fr" -> stringResource(R.string.language_fr)
                                "de" -> stringResource(R.string.language_de)
                                "es" -> stringResource(R.string.language_es)
                                "it" -> stringResource(R.string.language_it)
                                "pt" -> stringResource(R.string.language_pt)
                                else -> lang
                            }
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = selectedLanguage == lang,
                            onClick = {
                                selectedLanguage = lang
                                prefs.edit().putString("language", lang).apply()
                                showRestartDialog = true
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        selectedLanguage = lang
                        prefs.edit().putString("language", lang).apply()
                        showRestartDialog = true
                    }
                )
                if (lang != languages.last()) {
                    Divider()
                }
            }
        }
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_app)) },
            text = {
                Text(stringResource(R.string.restart_app))
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

