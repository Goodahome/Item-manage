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
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    val defaultAppName = context.getString(R.string.app_name)
    var appName by remember { mutableStateOf(prefs.getString("app_name", defaultAppName) ?: defaultAppName) }
    var isPasswordEnabled by remember { mutableStateOf(prefs.getBoolean("password_enabled", false)) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAppNameDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.app_settings)) },
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
            // 程序名称
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name_label)) },
                supportingContent = { 
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = { 
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorHelpers.getGroup4TextColor()
                        ),
                        onClick = { showAppNameDialog = true }) {
                        Text(stringResource(R.string.modify))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Divider()
            
            // 密码保护
            ListItem(
                headlineContent = { Text(stringResource(R.string.password_protection)) },
                supportingContent = { 
                    Text(
                        text = if (isPasswordEnabled) stringResource(R.string.password_enabled) else stringResource(R.string.password_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isPasswordEnabled,
                        onCheckedChange = {
                            isPasswordEnabled = it
                            prefs.edit().putBoolean("password_enabled", it).apply()
                            if (it) {
                                showPasswordDialog = true
                            }
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
    
    val isDarkTheme = isSystemInDarkTheme()
    val dialogBackgroundColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.7f) // 深色模式：半透明黑色毛玻璃
    } else {
        Color.White.copy(alpha = 0.7f) // 浅色模式：半透明白色毛玻璃
    }
    
    // 程序名称对话框
    if (showAppNameDialog) {
        var newAppName by remember { mutableStateOf(appName) }
        AlertDialog(
            onDismissRequest = { showAppNameDialog = false },
            containerColor = dialogBackgroundColor,
            title = { Text(stringResource(R.string.modify_app_name)) },
            text = {
                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text(stringResource(R.string.app_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val appNameChangedMessage = context.getString(R.string.app_name_changed)
                        appName = newAppName
                        prefs.edit().putString("app_name", newAppName).apply()
                        prefs.edit().putBoolean("pending_name_change", true).apply()
                        showAppNameDialog = false
                        showRestartDialog = true
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 密码设置对话框
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false
                if (!isPasswordEnabled) {
                    isPasswordEnabled = false
                    prefs.edit().putBoolean("password_enabled", false).apply()
                }
            },
            containerColor = dialogBackgroundColor,
            title = { Text(stringResource(R.string.set_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                            prefs.edit().putString("app_password", newPassword).apply()
                            prefs.edit().putBoolean("password_enabled", true).apply()
                            showPasswordDialog = false
                        }
                    },
                    enabled = newPassword == confirmPassword && newPassword.isNotEmpty()
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPasswordDialog = false
                    if (!isPasswordEnabled) {
                        isPasswordEnabled = false
                        prefs.edit().putBoolean("password_enabled", false).apply()
                    }
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            containerColor = dialogBackgroundColor,
            title = { Text(stringResource(R.string.restart_app)) },
            text = {
                Text(context.getString(R.string.app_name_changed))
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

