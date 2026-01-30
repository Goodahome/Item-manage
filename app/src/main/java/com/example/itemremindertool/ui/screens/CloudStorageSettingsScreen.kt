package com.example.itemremindertool.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.AppDivider
import androidx.compose.foundation.background
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.ui.viewmodel.CloudStorageViewModel
import com.example.itemremindertool.ui.viewmodel.OperationState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import com.example.itemremindertool.utils.cloud.CloudProviderRegistry
import com.example.itemremindertool.utils.cloud.auth.AppAuthManager
import net.openid.appauth.AuthorizationServiceConfiguration
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.blockUserInput
import com.example.itemremindertool.ui.components.rememberScreenInteractionBlocker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudStorageSettingsScreen(
    viewModel: CloudStorageViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocker = rememberScreenInteractionBlocker()
    BackHandler { blocker.handleBack(onNavigateBack) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    // 确保正确观察状态
    val operationState by viewModel.operationState.collectAsState(initial = OperationState.Idle)
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 调试：监听状态变化
    LaunchedEffect(operationState) {
        android.util.Log.d("CloudStorageSettings", "operationState 变化: $operationState")
        when (val state = operationState) {
            is OperationState.Success -> {
                snackbarHostState.showSnackbar(state.message)
            }
            is OperationState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> Unit
        }
    }
    
    // Nextcloud 配置
    var nextcloudServerUrl by remember { 
        mutableStateOf(prefs.getString("nextcloud_server_url", "") ?: "") 
    }
    var nextcloudUsername by remember { 
        mutableStateOf(prefs.getString("nextcloud_username", "") ?: "") 
    }
    var nextcloudPassword by remember { 
        mutableStateOf(prefs.getString("nextcloud_password", "") ?: "") 
    }
    var autoSyncEnabled by remember { 
        mutableStateOf(prefs.getBoolean("auto_sync_enabled", false)) 
    }

    // 云盘选择与 OAuth
    var selectedProviderId by remember {
        mutableStateOf(prefs.getString("cloud_provider_id", "nextcloud") ?: "nextcloud")
    }
    var googleClientId by remember {
        mutableStateOf(prefs.getString("google_drive_client_id", "") ?: "")
    }
    var dropboxClientId by remember {
        mutableStateOf(prefs.getString("dropbox_client_id", "") ?: "")
    }
    var aliyunClientId by remember {
        mutableStateOf(prefs.getString("aliyun_drive_client_id", "") ?: "")
    }
    var aliyunClientSecret by remember {
        mutableStateOf(prefs.getString("aliyun_drive_client_secret", "") ?: "")
    }
    var baiduClientId by remember {
        mutableStateOf(prefs.getString("baidu_netdisk_client_id", "") ?: "")
    }
    var baiduClientSecret by remember {
        mutableStateOf(prefs.getString("baidu_netdisk_client_secret", "") ?: "")
    }
    var showOAuthConfigDialog by remember { mutableStateOf(false) }
    var oauthConfigProviderId by remember { mutableStateOf<String?>(null) }
    var oauthClientIdInput by remember { mutableStateOf("") }
    var oauthClientSecretInput by remember { mutableStateOf("") }
    var pendingAuthProviderId by remember { mutableStateOf<String?>(null) }
    
    // UI 状态
    var showNextcloudConfigDialog by remember { mutableStateOf(false) }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    
    // 检查高级功能访问权限
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    
    // Billing Manager
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

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "premium_features" || key == "premium_lifetime" || key == "premium_trial_used" || key == "premium_trial_start_time") {
                canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val provider = CloudProviderRegistry.getProvider(pendingAuthProviderId)
        val oauthConfig = provider.getOAuthConfig(context)
        if (oauthConfig == null) return@rememberLauncherForActivityResult
        val secret = oauthConfig.clientSecretPrefKey?.let { key ->
            prefs.getString(key, "") ?: ""
        }
        scope.launch {
            viewModel.showSaving()
            val authResult = AppAuthManager.handleAuthorizationResult(
                context = context,
                authStateKey = oauthConfig.authStatePrefKey,
                data = result.data,
                clientSecret = secret
            )
            authResult.fold(
                onSuccess = { viewModel.showSuccess("连接成功！") },
                onFailure = { e -> viewModel.showError("授权失败: ${e.message}") }
            )
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                GradientTopAppBar(
                    title = { Text(stringResource(R.string.cloud_storage)) },
                    navigationIcon = {
                        IconButton(onClick = { blocker.handleBack(onNavigateBack) }) {
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
                    .padding(16.dp)
                    .blockUserInput(blocker.isBlocked),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val googleProvider = CloudProviderRegistry.getProvider("google_drive")
                val dropboxProvider = CloudProviderRegistry.getProvider("dropbox")
                val aliyunProvider = CloudProviderRegistry.getProvider("aliyun_drive")
                val baiduProvider = CloudProviderRegistry.getProvider("baidu_netdisk")

                val googleConfig = googleProvider.getOAuthConfig(context)
                val dropboxConfig = dropboxProvider.getOAuthConfig(context)
                val aliyunConfig = aliyunProvider.getOAuthConfig(context)
                val baiduConfig = baiduProvider.getOAuthConfig(context)

                // 云端存储配置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cloud_provider),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                    Text(
                        text = stringResource(R.string.cloud_provider_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CloudProviderRegistry.providers.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProviderId = provider.id
                                        prefs.edit().putString("cloud_provider_id", provider.id).apply()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProviderId == provider.id,
                                    onClick = {
                                        selectedProviderId = provider.id
                                        prefs.edit().putString("cloud_provider_id", provider.id).apply()
                                    }
                                )
                                Text(
                                    text = provider.displayName,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                            }
                        }
                    }
                    
                    AppDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 2.dp
                    )

                    val selectedProvider = CloudProviderRegistry.getProvider(selectedProviderId)
                    val isConfigComplete =
                        nextcloudServerUrl.isNotEmpty() && nextcloudUsername.isNotEmpty() && nextcloudPassword.isNotEmpty()
                    val isProviderReady =
                        selectedProvider.isConfigured(context) && selectedProvider.isAuthenticated(context)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.auto_backup),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = if (autoSyncEnabled) {
                                    stringResource(R.string.auto_backup_enabled)
                                } else {
                                    stringResource(R.string.auto_backup_disabled)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            enabled = canAccessPremiumFeatures && isProviderReady,
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
                                autoSyncEnabled = it
                                prefs.edit().putBoolean("auto_sync_enabled", it).apply()
                                if (it && isProviderReady) {
                                    com.example.itemremindertool.utils.CloudSyncScheduler.scheduleSync(context)
                                    viewModel.showSuccess(context.getString(R.string.auto_backup_toast_enabled))
                                } else {
                                    com.example.itemremindertool.utils.CloudSyncScheduler.cancelSync(context)
                                    viewModel.showSuccess(context.getString(R.string.auto_backup_toast_disabled))
                                }
                            }
                        )
                    }

                    AppDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 2.dp
                    )

                    when (selectedProviderId) {
                        "google_drive" -> if (googleConfig != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.google_drive),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorHelpers.getGroup4TextColor()
                                    )
                                    Text(
                                        text = if (googleProvider.isAuthenticated(context)) {
                                            stringResource(R.string.connected)
                                        } else {
                                            stringResource(R.string.not_connected)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorHelpers.getGroup4TextColor(0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                                if (googleProvider.isAuthenticated(context)) {
                                    TextButton(onClick = {
                                        AppAuthManager.clearAuthState(context, googleConfig.authStatePrefKey)
                                        viewModel.showSuccess(context.getString(R.string.disconnected))
                                    }) {
                                        Text(stringResource(R.string.disconnect))
                                    }
                                } else {
                                    TextButton(onClick = {
                                        if (!canAccessPremiumFeatures) {
                                            showPremiumFeatureDialog = true
                                            return@TextButton
                                        }
                                        if (googleClientId.isBlank()) {
                                            oauthConfigProviderId = googleProvider.id
                                            oauthClientIdInput = googleClientId
                                            oauthClientSecretInput = ""
                                            showOAuthConfigDialog = true
                                            return@TextButton
                                        }
                                        val serviceConfig = AuthorizationServiceConfiguration(
                                            googleConfig.authEndpoint,
                                            googleConfig.tokenEndpoint
                                        )
                                        pendingAuthProviderId = googleProvider.id
                                        val intent = AppAuthManager.getAuthorizationRequestIntent(
                                            context,
                                            serviceConfig,
                                            googleClientId,
                                            googleConfig.redirectUri,
                                            googleConfig.scopes,
                                            googleConfig.additionalParameters
                                        )
                                        authLauncher.launch(intent)
                                    }) {
                                        Text(stringResource(R.string.connect))
                                    }
                                }
                            }
                        }
                        "dropbox" -> if (dropboxConfig != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.dropbox),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorHelpers.getGroup4TextColor()
                                    )
                                    Text(
                                        text = if (dropboxProvider.isAuthenticated(context)) {
                                            stringResource(R.string.connected)
                                        } else {
                                            stringResource(R.string.not_connected)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorHelpers.getGroup4TextColor(0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                                if (dropboxProvider.isAuthenticated(context)) {
                                    TextButton(onClick = {
                                        AppAuthManager.clearAuthState(context, dropboxConfig.authStatePrefKey)
                                        viewModel.showSuccess(context.getString(R.string.disconnected))
                                    }) {
                                        Text(stringResource(R.string.disconnect))
                                    }
                                } else {
                                    TextButton(onClick = {
                                        if (!canAccessPremiumFeatures) {
                                            showPremiumFeatureDialog = true
                                            return@TextButton
                                        }
                                        if (dropboxClientId.isBlank()) {
                                            oauthConfigProviderId = dropboxProvider.id
                                            oauthClientIdInput = dropboxClientId
                                            oauthClientSecretInput = ""
                                            showOAuthConfigDialog = true
                                            return@TextButton
                                        }
                                        val serviceConfig = AuthorizationServiceConfiguration(
                                            dropboxConfig.authEndpoint,
                                            dropboxConfig.tokenEndpoint
                                        )
                                        pendingAuthProviderId = dropboxProvider.id
                                        val intent = AppAuthManager.getAuthorizationRequestIntent(
                                            context,
                                            serviceConfig,
                                            dropboxClientId,
                                            dropboxConfig.redirectUri,
                                            dropboxConfig.scopes,
                                            dropboxConfig.additionalParameters
                                        )
                                        authLauncher.launch(intent)
                                    }) {
                                        Text(stringResource(R.string.connect))
                                    }
                                }
                            }
                        }
                        "aliyun_drive" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.aliyun_drive),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                OutlinedTextField(
                                    value = aliyunClientId,
                                    onValueChange = { aliyunClientId = it },
                                    label = { Text(stringResource(R.string.oauth_client_id)) },
                                    placeholder = { Text(stringResource(R.string.oauth_client_id_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = aliyunClientSecret,
                                    onValueChange = { aliyunClientSecret = it },
                                    label = { Text(stringResource(R.string.oauth_client_secret)) },
                                    placeholder = { Text(stringResource(R.string.oauth_client_secret_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            prefs.edit()
                                                .putString("aliyun_drive_client_id", aliyunClientId.trim())
                                                .putString("aliyun_drive_client_secret", aliyunClientSecret.trim())
                                                .apply()
                                            val config = aliyunProvider.getOAuthConfig(context)
                                            if (config == null) return@Button
                                            val serviceConfig = AuthorizationServiceConfiguration(
                                                config.authEndpoint,
                                                config.tokenEndpoint
                                            )
                                            pendingAuthProviderId = aliyunProvider.id
                                            val intent = AppAuthManager.getAuthorizationRequestIntent(
                                                context,
                                                serviceConfig,
                                                aliyunClientId.trim(),
                                                config.redirectUri,
                                                config.scopes,
                                                config.additionalParameters
                                            )
                                            authLauncher.launch(intent)
                                        },
                                        enabled = aliyunClientId.isNotBlank() && aliyunClientSecret.isNotBlank()
                                    ) {
                                        Text(stringResource(R.string.connect))
                                    }
                                    if (aliyunProvider.isAuthenticated(context)) {
                                        OutlinedButton(onClick = {
                                            val config = aliyunProvider.getOAuthConfig(context)
                                            if (config != null) {
                                                AppAuthManager.clearAuthState(context, config.authStatePrefKey)
                                            }
                                            viewModel.showSuccess(context.getString(R.string.disconnected))
                                        }) {
                                            Text(stringResource(R.string.disconnect))
                                        }
                                    }
                                }
                            }
                        }
                        "baidu_netdisk" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.baidu_netdisk),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                OutlinedTextField(
                                    value = baiduClientId,
                                    onValueChange = { baiduClientId = it },
                                    label = { Text(stringResource(R.string.oauth_client_id)) },
                                    placeholder = { Text(stringResource(R.string.oauth_client_id_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = baiduClientSecret,
                                    onValueChange = { baiduClientSecret = it },
                                    label = { Text(stringResource(R.string.oauth_client_secret)) },
                                    placeholder = { Text(stringResource(R.string.oauth_client_secret_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            prefs.edit()
                                                .putString("baidu_netdisk_client_id", baiduClientId.trim())
                                                .putString("baidu_netdisk_client_secret", baiduClientSecret.trim())
                                                .apply()
                                            val config = baiduProvider.getOAuthConfig(context)
                                            if (config == null) return@Button
                                            val serviceConfig = AuthorizationServiceConfiguration(
                                                config.authEndpoint,
                                                config.tokenEndpoint
                                            )
                                            pendingAuthProviderId = baiduProvider.id
                                            val intent = AppAuthManager.getAuthorizationRequestIntent(
                                                context,
                                                serviceConfig,
                                                baiduClientId.trim(),
                                                config.redirectUri,
                                                config.scopes,
                                                config.additionalParameters
                                            )
                                            authLauncher.launch(intent)
                                        },
                                        enabled = baiduClientId.isNotBlank() && baiduClientSecret.isNotBlank()
                                    ) {
                                        Text(stringResource(R.string.connect))
                                    }
                                    if (baiduProvider.isAuthenticated(context)) {
                                        OutlinedButton(onClick = {
                                            val config = baiduProvider.getOAuthConfig(context)
                                            if (config != null) {
                                                AppAuthManager.clearAuthState(context, config.authStatePrefKey)
                                            }
                                            viewModel.showSuccess(context.getString(R.string.disconnected))
                                        }) {
                                            Text(stringResource(R.string.disconnect))
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.nextcloud_settings),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorHelpers.getGroup4TextColor()
                                    )
                                    if (nextcloudServerUrl.isNotEmpty()) {
                                        Text(
                                            text = stringResource(R.string.nextcloud_configured),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorHelpers.getGroup4TextColor(0.7f),
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.nextcloud_not_configured),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorHelpers.getGroup4TextColor(0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    if (!canAccessPremiumFeatures) {
                                        showPremiumFeatureDialog = true
                                    } else {
                                        showNextcloudConfigDialog = true
                                    }
                                }) {
                                    Icon(Icons.Default.Settings, null, tint = ColorHelpers.getGroup4IconColor())
                                }
                            }

                            AppDivider(
                                color = ColorHelpers.getDividerColor(),
                                thickness = 2.dp
                            )

                            Button(
                                onClick = {
                                    if (!canAccessPremiumFeatures) {
                                        showPremiumFeatureDialog = true
                                        return@Button
                                    }
                                    if (!isConfigComplete) {
                                        viewModel.showError("请先配置 Nextcloud 服务器信息")
                                        return@Button
                                    }
                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        viewModel.showSaving()
                                        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            com.example.itemremindertool.utils.NextcloudBackupManager.testConnection(
                                                nextcloudServerUrl,
                                                nextcloudUsername,
                                                nextcloudPassword
                                            )
                                        }
                                        if (result.isSuccess) {
                                            viewModel.showSuccess("连接成功！")
                                        } else {
                                            val error = result.exceptionOrNull()
                                            viewModel.showError("连接失败: ${error?.message ?: "未知错误"}")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isConfigComplete
                            ) {
                                Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.test_connection))
                            }
                        }
                    }
                }
            }
        } // 关闭 Scaffold 的 content lambda
        
        // 底部状态指示器
        BottomOperationStatusIndicator(
            operationState = operationState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    } // 关闭外层 Box
}
    
    // Nextcloud 配置对话框
    if (showNextcloudConfigDialog) {
        NextcloudConfigDialog(
            serverUrl = nextcloudServerUrl,
            username = nextcloudUsername,
            password = nextcloudPassword,
            onDismiss = { showNextcloudConfigDialog = false },
            onSave = { url, user, pass ->
                nextcloudServerUrl = url
                nextcloudUsername = user
                nextcloudPassword = pass
                prefs.edit()
                    .putString("nextcloud_server_url", url)
                    .putString("nextcloud_username", user)
                    .putString("nextcloud_password", pass)
                    .apply()
                if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                    autoSyncEnabled = false
                    prefs.edit().putBoolean("auto_sync_enabled", false).apply()
                    com.example.itemremindertool.utils.CloudSyncScheduler.cancelSync(context)
                } else if (autoSyncEnabled) {
                    // 如果自动同步已启用，重新调度
                    com.example.itemremindertool.utils.CloudSyncScheduler.scheduleSync(context)
                }
                showNextcloudConfigDialog = false
            }
        )
    }

    if (showOAuthConfigDialog && oauthConfigProviderId != null) {
        OAuthClientIdDialog(
            providerName = CloudProviderRegistry.getProvider(oauthConfigProviderId).displayName,
            clientId = oauthClientIdInput,
            clientSecret = oauthClientSecretInput,
            showClientSecret = CloudProviderRegistry.getProvider(oauthConfigProviderId)
                .getOAuthConfig(context)
                ?.requiresClientSecret == true,
            onDismiss = { showOAuthConfigDialog = false },
            onSave = { clientId, clientSecret ->
                val provider = CloudProviderRegistry.getProvider(oauthConfigProviderId)
                val oauthConfig = provider.getOAuthConfig(context)
                if (oauthConfig != null) {
                    prefs.edit().putString(oauthConfig.clientIdPrefKey, clientId).apply()
                    when (provider.id) {
                        "google_drive" -> googleClientId = clientId
                        "dropbox" -> dropboxClientId = clientId
                        "aliyun_drive" -> aliyunClientId = clientId
                        "baidu_netdisk" -> baiduClientId = clientId
                    }
                    oauthConfig.clientSecretPrefKey?.let { key ->
                        prefs.edit().putString(key, clientSecret).apply()
                        when (provider.id) {
                            "aliyun_drive" -> aliyunClientSecret = clientSecret
                            "baidu_netdisk" -> baiduClientSecret = clientSecret
                        }
                    }
                    oauthClientIdInput = ""
                    oauthClientSecretInput = ""
                    showOAuthConfigDialog = false
                    val serviceConfig = AuthorizationServiceConfiguration(
                        oauthConfig.authEndpoint,
                        oauthConfig.tokenEndpoint
                    )
                    pendingAuthProviderId = provider.id
                    val intent = AppAuthManager.getAuthorizationRequestIntent(
                        context,
                        serviceConfig,
                        clientId,
                        oauthConfig.redirectUri,
                        oauthConfig.scopes,
                        oauthConfig.additionalParameters
                    )
                    authLauncher.launch(intent)
                }
            }
        )
    }
    
    // 高级功能对话框
    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
}

@Composable
private fun NextcloudConfigDialog(
    serverUrl: String,
    username: String,
    password: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var newServerUrl by remember { mutableStateOf(serverUrl) }
    var newUsername by remember { mutableStateOf(username) }
    var newPassword by remember { mutableStateOf(password) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    ModernSettingsDialog(
        title = stringResource(R.string.nextcloud_settings),
        icon = Icons.Default.Cloud,
        onDismiss = onDismiss,
        onConfirm = { onSave(newServerUrl.trim(), newUsername.trim(), newPassword) },
        confirmText = stringResource(R.string.save),
        dismissText = stringResource(R.string.cancel)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = newServerUrl,
                onValueChange = { newServerUrl = it },
                label = { Text(stringResource(R.string.nextcloud_server_url)) },
                placeholder = { Text("https://nextcloud.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text(stringResource(R.string.nextcloud_username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(stringResource(R.string.nextcloud_password)) },
                placeholder = { Text(stringResource(R.string.nextcloud_password_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun OAuthClientIdDialog(
    providerName: String,
    clientId: String,
    clientSecret: String,
    showClientSecret: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var newClientId by remember { mutableStateOf(clientId) }
    var newClientSecret by remember { mutableStateOf(clientSecret) }
    val isValid = newClientId.isNotBlank() && (!showClientSecret || newClientSecret.isNotBlank())
    ModernSettingsDialog(
        title = stringResource(R.string.oauth_client_id_title, providerName),
        icon = Icons.Default.VpnKey,
        onDismiss = onDismiss,
        onConfirm = { onSave(newClientId.trim(), newClientSecret.trim()) },
        confirmEnabled = isValid,
        confirmText = stringResource(R.string.save),
        dismissText = stringResource(R.string.cancel)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.oauth_client_id_desc),
                style = MaterialTheme.typography.bodySmall,
                color = ColorHelpers.getGroup4TextColor(0.7f)
            )
            OutlinedTextField(
                value = newClientId,
                onValueChange = { newClientId = it },
                label = { Text(stringResource(R.string.oauth_client_id)) },
                placeholder = { Text(stringResource(R.string.oauth_client_id_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (showClientSecret) {
                OutlinedTextField(
                    value = newClientSecret,
                    onValueChange = { newClientSecret = it },
                    label = { Text(stringResource(R.string.oauth_client_secret)) },
                    placeholder = { Text(stringResource(R.string.oauth_client_secret_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

