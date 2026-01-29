package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.ui.components.AppDivider
import android.app.Activity
import android.content.SharedPreferences
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit,
    activityEventViewModel: com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel? = null,
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
    var pendingPasswordEnable by remember { mutableStateOf(false) }
    var showAppNameDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showAdUnitIdDialog by remember { mutableStateOf(false) }
    var showClearActivityDataDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    
    // 物品展示模式管理器
    val displayModeManager = remember { com.example.itemremindertool.config.ItemDisplayModeManager.getInstance(context) }
    val displayMode by displayModeManager.displayMode.collectAsState()
    
    val defaultCurrencySymbol = context.getString(R.string.default_currency_symbol)
    var currencySymbol by remember {
        mutableStateOf(prefs.getString("currency_symbol", defaultCurrencySymbol) ?: defaultCurrencySymbol)
    }
    
    // 服务器地址
    val defaultServerUrl = "http://localhost:3000"
    var serverUrl by remember {
        mutableStateOf(prefs.getString("server_url", defaultServerUrl) ?: defaultServerUrl)
    }
    var showServerUrlDialog by remember { mutableStateOf(false) }
    
    // 广告单元 ID
    var adBannerUnitId by remember { 
        mutableStateOf(prefs.getString("ad_banner_unit_id", null) ?: "")
    }
    
    // Billing Manager（仅在启用购买功能时初始化）
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val billingManager = remember {
        if (FeatureFlags.ENABLE_PURCHASE_FEATURE) {
            BillingManager(
                context,
                listOf(
                    BillingManager.PRODUCT_REMOVE_ADS,
                    BillingManager.PRODUCT_PREMIUM_FEATURES,
                    BillingManager.PRODUCT_PREMIUM_LIFETIME
                )
            ).apply {
                initialize()
            }
        } else {
            null
        }
    }
    
    // 高级功能对话框状态
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    
    // 检查高级功能访问权限
    var canAccessPremiumFeatures by remember {
        mutableStateOf(com.example.itemremindertool.billing.PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    
    // 购买状态（仅在启用购买功能时获取）
    val purchaseState by if (FeatureFlags.ENABLE_PURCHASE_FEATURE && billingManager != null) {
        billingManager.purchaseState.collectAsState()
    } else {
        remember { mutableStateOf(com.example.itemremindertool.billing.PurchaseState.NotPurchased) }
    }
    val productDetails by if (FeatureFlags.ENABLE_PURCHASE_FEATURE && billingManager != null) {
        billingManager.productDetails.collectAsState()
    } else {
        remember { mutableStateOf<com.android.billingclient.api.ProductDetails?>(null) }
    }
    val isReady by if (FeatureFlags.ENABLE_PURCHASE_FEATURE && billingManager != null) {
        billingManager.isReady.collectAsState()
    } else {
        remember { mutableStateOf(false) }
    }
    
    // 监听购买状态和本地存储
    var isAdsRemoved by remember { mutableStateOf(prefs.getBoolean("ads_removed", false)) }
    
    // 当购买状态变化时更新
    LaunchedEffect(purchaseState) {
        isAdsRemoved = purchaseState is com.example.itemremindertool.billing.PurchaseState.Purchased || 
                       prefs.getBoolean("ads_removed", false)
    }
    
    // 监听 SharedPreferences 变化
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs: SharedPreferences, key: String? ->
            if (key == "ads_removed") {
                isAdsRemoved = prefs.getBoolean("ads_removed", false)
            }
            if (key == "premium_features" || key == "premium_lifetime" || key == "premium_trial_used" || key == "premium_trial_start_time") {
                canAccessPremiumFeatures = com.example.itemremindertool.billing.PremiumFeatureManager.canAccessPremiumFeatures(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // 生命周期管理（仅在启用购买功能时）
    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && billingManager != null) {
        DisposableEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.addObserver(billingManager)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(billingManager)
            }
        }
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.app_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        ) {
            // 程序名称
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name_hint)) },
                supportingContent = { 
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = { 
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                            } else {
                                showAppNameDialog = true
                            }
                        }) {
                        Text(stringResource(R.string.modify))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
            // 服务器地址设置
            ListItem(
                headlineContent = { Text(stringResource(R.string.server_url_setting)) },
                supportingContent = { 
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = { 
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = { showServerUrlDialog = true }
                    ) {
                        Text(stringResource(R.string.modify))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
            // 币种符号设置
            ListItem(
                headlineContent = { Text(stringResource(R.string.currency_symbol_setting)) },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.currency_symbol_setting_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                },
                trailingContent = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = { showCurrencyDialog = true }
                    ) {
                        Text(currencySymbol)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
            // 广告横幅单元 ID
//            ListItem(
//                headlineContent = { Text(stringResource(R.string.ad_banner_unit_id)) },
//                supportingContent = {
//                    Text(
//                        text = if (adBannerUnitId.isNotEmpty()) {
//                            adBannerUnitId
//                        } else {
//                            stringResource(R.string.ad_banner_unit_id_desc) + " (当前使用测试 ID)"
//                        },
//                        style = MaterialTheme.typography.bodySmall,
//                        color = ColorHelpers.getGroup4TextColor(0.7f)
//                    )
//                },
//                trailingContent = {
//                    TextButton(
//                        colors = ButtonDefaults.textButtonColors(
//                            contentColor = ColorHelpers.getGroup4TextColor()
//                        ),
//                        onClick = { showAdUnitIdDialog = true }) {
//                        Text(stringResource(R.string.modify))
//                    }
//                },
//                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
//            )
        
            
            // 移除广告（仅在启用购买功能时显示）
            if (FeatureFlags.ENABLE_PURCHASE_FEATURE) {
                ListItem(
                    headlineContent = { 
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.remove_ads),
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            if (isAdsRemoved) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.purchased),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    },
                    supportingContent = { 
                        Text(
                            text = if (isAdsRemoved) {
                                stringResource(R.string.ads_removed_desc)
                            } else {
                                val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                                if (price != null) {
                                    stringResource(R.string.remove_ads_desc, price)
                                } else {
                                    stringResource(R.string.remove_ads_desc_no_price)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.7f)
                        )
                    },
                    trailingContent = {
                        if (isAdsRemoved) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (isReady && activity != null && billingManager != null) {
                                        val success = billingManager.launchPurchaseFlow(activity)
                                        if (!success) {
                                            // 如果产品未找到，显示提示
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.product_not_available),
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else if (!isReady) {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.billing_not_ready),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = isReady && activity != null && billingManager != null
                            ) {
                                val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                                if (price != null) {
                                    Text(price)
                                } else {
                                    Text(stringResource(R.string.purchase))
                                }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                AppDivider(
                    color = ColorHelpers.getDividerColor(),
                    thickness = 2.dp
                )
            }
            
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
                        enabled = canAccessPremiumFeatures,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = ColorHelpers.getGroup4IconColor(0.6f),
                            uncheckedTrackColor = ColorHelpers.getGroup4IconColor(0.2f)
                        ),
                        onCheckedChange = {
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                                return@Switch
                            }
                            if (it) {
                                pendingPasswordEnable = true
                                isPasswordEnabled = true
                                showPasswordDialog = true
                            } else {
                                pendingPasswordEnable = false
                                isPasswordEnabled = false
                                // 如果禁用密码，清除密码
                                prefs.edit().putString("app_password", "").commit()
                                prefs.edit().putBoolean("password_enabled", false).commit()
                                // 显示重启提醒
                                showRestartDialog = true
                            }
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
            // 清除动态数据
            if (activityEventViewModel != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.clear_activity_data)) },
                    supportingContent = { 
                        Text(
                            text = stringResource(R.string.clear_activity_data_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.7f)
                        )
                    },
                    trailingContent = { 
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            onClick = { showClearActivityDataDialog = true }) {
                            Text(stringResource(R.string.clear))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
    
    // 高级功能对话框（仅在启用购买功能时显示）
    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
    
    // 程序名称对话框
    if (showAppNameDialog) {
        var newAppName by remember { mutableStateOf(appName) }
        ModernSettingsDialog(
            title = stringResource(R.string.modify_app_name),
            icon = Icons.Default.Edit,
            onDismiss = { showAppNameDialog = false },
            onConfirm = {
                appName = newAppName
                prefs.edit().putString("app_name", newAppName).apply()
                showAppNameDialog = false
            },
            confirmEnabled = newAppName.isNotEmpty()
        ) {
            OutlinedTextField(
                value = newAppName,
                onValueChange = { newAppName = it },
                label = { Text(stringResource(R.string.app_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
    
    // 密码设置对话框
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        ModernSettingsDialog(
            title = stringResource(R.string.set_password),
            icon = Icons.Default.Lock,
            onDismiss = { 
                showPasswordDialog = false
                if (pendingPasswordEnable) {
                    pendingPasswordEnable = false
                    isPasswordEnabled = false
                    prefs.edit().putBoolean("password_enabled", false).apply()
                }
            },
            onConfirm = {
                if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                    prefs.edit().putString("app_password", newPassword).commit()
                    prefs.edit().putBoolean("password_enabled", true).commit()
                    isPasswordEnabled = true
                    pendingPasswordEnable = false
                    showPasswordDialog = false
                    // 显示重启提醒
                    showRestartDialog = true
                }
            },
            confirmEnabled = newPassword == confirmPassword && newPassword.isNotEmpty()
        ) {
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
        }
    }
    
    // 币种符号对话框
    if (showCurrencyDialog) {
        var newSymbol by remember { mutableStateOf(currencySymbol) }
        ModernSettingsDialog(
            title = stringResource(R.string.currency_symbol_setting),
            icon = Icons.Default.AttachMoney,
            onDismiss = { showCurrencyDialog = false },
            onConfirm = {
                val finalSymbol = newSymbol.trim().ifBlank { defaultCurrencySymbol }
                currencySymbol = finalSymbol
                prefs.edit().putString("currency_symbol", finalSymbol).apply()
                showCurrencyDialog = false
            }
        ) {
            OutlinedTextField(
                value = newSymbol,
                onValueChange = { newSymbol = it },
                label = { Text(stringResource(R.string.currency_symbol_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
    
    // 服务器地址设置对话框
    if (showServerUrlDialog) {
        var newServerUrl by remember { mutableStateOf(serverUrl) }
        var urlError by remember { mutableStateOf<String?>(null) }
        
        // URL 验证函数
        fun validateUrl(url: String): String? {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) {
                return context.getString(R.string.server_url_empty_error)
            }
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                return context.getString(R.string.server_url_format_error)
            }
            try {
                val uri = java.net.URI(trimmed)
                if (uri.host.isNullOrEmpty()) {
                    return context.getString(R.string.server_url_invalid_error)
                }
            } catch (e: Exception) {
                return context.getString(R.string.server_url_invalid_error)
            }
            return null
        }
        
        ModernSettingsDialog(
            title = stringResource(R.string.server_url_setting),
            icon = Icons.Default.Cloud,
            onDismiss = { 
                showServerUrlDialog = false
                urlError = null
            },
            onConfirm = {
                val trimmed = newServerUrl.trim()
                val error = validateUrl(trimmed)
                if (error == null) {
                    // 确保 URL 不以斜杠结尾（RetrofitClient 会自动添加）
                    val finalUrl = trimmed.trimEnd('/')
                    serverUrl = finalUrl
                    prefs.edit().putString("server_url", finalUrl).apply()
                    
                    // 重置 Retrofit 客户端以使用新地址
                    com.example.itemremindertool.network.RetrofitClient.reset()
                    
                    showServerUrlDialog = false
                    urlError = null
                    
                    // 显示成功提示
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.server_url_saved),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    urlError = error
                }
            },
            confirmEnabled = newServerUrl.trim().isNotEmpty()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newServerUrl,
                    onValueChange = { 
                        newServerUrl = it
                        urlError = null // 清除错误提示
                    },
                    label = { Text(stringResource(R.string.server_url_hint)) },
                    placeholder = { Text(stringResource(R.string.server_url_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = if (urlError != null) {
                        { Text(urlError!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        { Text(stringResource(R.string.server_url_desc), style = MaterialTheme.typography.bodySmall) }
                    }
                )
            }
        }
    }
    
    // 广告单元 ID 设置对话框
    if (showAdUnitIdDialog) {
        var newAdUnitId by remember { mutableStateOf(adBannerUnitId) }
        ModernSettingsDialog(
            title = stringResource(R.string.ad_banner_unit_id),
            icon = Icons.Default.Campaign,
            onDismiss = { showAdUnitIdDialog = false },
            onConfirm = {
                adBannerUnitId = newAdUnitId.trim()
                if (adBannerUnitId.isEmpty()) {
                    // 如果为空，删除保存的值，使用默认测试 ID
                    prefs.edit().remove("ad_banner_unit_id").apply()
                } else {
                    // 保存新的广告单元 ID
                    prefs.edit().putString("ad_banner_unit_id", adBannerUnitId).apply()
                }
                showAdUnitIdDialog = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newAdUnitId,
                    onValueChange = { newAdUnitId = it },
                    label = { Text(stringResource(R.string.ad_banner_unit_id_hint)) },
                    placeholder = { Text(stringResource(R.string.ad_banner_unit_id_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.ad_banner_unit_id_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.7f)
                )
            }
        }
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        ModernSettingsDialog(
            title = stringResource(R.string.restart_app),
            icon = Icons.Default.Refresh,
            onDismiss = { showRestartDialog = false },
            onConfirm = {
                android.os.Process.killProcess(android.os.Process.myPid())
            },
            confirmText = stringResource(R.string.now),
            dismissText = stringResource(R.string.later)
        ) {
            Text(
                if (isPasswordEnabled) {
                    context.getString(R.string.enabled_restart_message)
                } else {
                    context.getString(R.string.disabled_restart_message)
                }
            )
        }
    }
    
    // 清除动态数据确认对话框
    if (showClearActivityDataDialog && activityEventViewModel != null) {
        ModernSettingsDialog(
            title = stringResource(R.string.clear_activity_data),
            icon = Icons.Default.Delete,
            onDismiss = { showClearActivityDataDialog = false },
            onConfirm = {
                activityEventViewModel.clearAllEvents()
                showClearActivityDataDialog = false
            },
            confirmText = stringResource(R.string.confirm_button),
            dismissText = stringResource(R.string.cancel)
        ) {
            Text(
                text = stringResource(R.string.clear_activity_data_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
        }
    }
}

/**
 * 统一的现代化设置对话框样式
 */
@Composable
fun ModernSettingsDialog(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDismissAction: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    confirmText: String = stringResource(R.string.ok),
    dismissText: String = stringResource(R.string.cancel),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorHelpers.getGroup3CardBgColor()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 顶部标题栏 - 使用主题色背景
                val headerColor = MaterialTheme.colorScheme.primary
                val headerContentColor = MaterialTheme.colorScheme.onPrimary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = headerContentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerContentColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = headerContentColor
                        )
                    }
                }
                AppDivider(color = Color.Transparent, thickness = 0.dp)
                
                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                }
                
                // 底部按钮栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onDismissAction?.invoke() ?: onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dismissText)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

