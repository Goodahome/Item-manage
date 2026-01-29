package com.example.itemremindertool

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavType
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.AccessHistoryManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.repository.*
import com.example.itemremindertool.navigation.Screen
import com.example.itemremindertool.ui.screens.*
import com.example.itemremindertool.ui.screens.SettingsScreen
import com.example.itemremindertool.notification.NotificationScheduler
import com.example.itemremindertool.ui.theme.ItemReminderToolTheme
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.AppDivider
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.viewmodel.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemremindertool.utils.AppConfigManager
import com.example.itemremindertool.utils.LocaleHelper
import com.example.itemremindertool.utils.IconManager
import com.example.itemremindertool.utils.AppRefreshManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.auth.AuthManager
import com.example.itemremindertool.network.RetrofitClient
import com.example.itemremindertool.network.dto.LoginRequest
import com.example.itemremindertool.network.dto.RegisterRequest
import com.example.itemremindertool.workers.SyncQueueWorker
import com.example.itemremindertool.sync.SyncManager
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu

class MainActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // 在创建 Activity 之前设置语言环境
        val language = LocaleHelper.getCurrentLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, language))
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化 SyncStateManager
        com.example.itemremindertool.utils.SyncStateManager.init(applicationContext)
        
        // 确保语言设置已应用
        val language = LocaleHelper.getCurrentLanguage(this)
        LocaleHelper.setLocale(this, language)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val itemRepository = ItemRepository(database.itemDao(), database.deletedRecordDao(), applicationContext)
        val categoryRepository = CategoryRepository(database.categoryDao(), applicationContext)
        val shoppingItemRepository = ShoppingItemRepository(database.shoppingItemDao(), applicationContext)
        val warehouseRepository = WarehouseRepository(database.warehouseDao(), database.deletedRecordDao(), database.itemDao(), applicationContext)
        val tagManager = TagManager(applicationContext)
        val accessHistoryManager = AccessHistoryManager(applicationContext)

        // 创建通知渠道（Android 8.0+）
        com.example.itemremindertool.utils.NotificationHelper.createNotificationChannel(this)
        
        // 请求通知权限（Android 13+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        
        // 启动通知调度（旧的，保留兼容性）
        NotificationScheduler.scheduleNotifications(this)
        
        // 重新调度所有提醒
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.itemremindertool.utils.ReminderScheduler.rescheduleAllReminders(this@MainActivity)
        }
        
        // 初始化云端自动同步（如果已启用）
        // 注意：不在启动时立即同步，而是通过 WorkManager 定期同步
        // 这样可以避免启动时立即上传空数据
        val syncPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val autoSyncEnabled = syncPrefs.getBoolean("auto_sync_enabled", false)
        val serverUrl = syncPrefs.getString("nextcloud_server_url", "") ?: ""
        val username = syncPrefs.getString("nextcloud_username", "") ?: ""
        val password = syncPrefs.getString("nextcloud_password", "") ?: ""
        if (autoSyncEnabled && serverUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            // 只调度定期同步任务，不立即执行
            com.example.itemremindertool.utils.CloudSyncScheduler.scheduleSync(this)
        }
        
        // 应用启动时重置同步状态（清除可能残留的同步状态）
        com.example.itemremindertool.utils.SyncStateManager.reset()
        
        // 启动同步队列后台任务（离线队列自动重试）
        val authManager = AuthManager.getInstance(applicationContext)
        if (authManager.isLoggedIn()) {
            SyncQueueWorker.schedule(this)
            SyncQueueWorker.runNow(this)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (isActive) {
                        kotlinx.coroutines.delay(5 * 60 * 1000L)
                        SyncQueueWorker.runNow(this@MainActivity)
                    }
                }
            }
        }

        // 收集数据库重置事件，触发 Activity 重建以重新绑定新的 DB 实例
        lifecycleScope.launch {
            AppRefreshManager.recreateFlow.collect {
                runOnUiThread { recreate() }
            }
        }

        // 如果存在重建标记（可能由后台同步/恢复时设置），启动时立即消费并重建
        if (AppRefreshManager.consumeFlag(this)) {
            recreate()
            return
        }
        
        // 初始化应用图标
        IconManager.initializeIcon(applicationContext)
        
        // 清理损坏的图片文件（在后台线程执行，不阻塞启动）
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.itemremindertool.utils.ImageUtils.cleanupCorruptedImages(applicationContext)
        }
        
        // 检查并应用待处理的系统级别更改
        // 注意：系统级别的名称和图标修改需要重新安装应用
        // 这里我们只是清除标志，实际修改需要用户重新安装应用
        if (AppConfigManager.hasPendingSystemChanges(applicationContext)) {
            AppConfigManager.applyPendingChanges(applicationContext)
            // 在实际应用中，可以在这里显示一个提示，告知用户需要重新安装应用
            // 才能看到系统级别的名称和图标更改
        }

        setContent {
            ItemReminderToolTheme {
                ItemReminderToolApp(
                    itemRepository = itemRepository,
                    categoryRepository = categoryRepository,
                    shoppingItemRepository = shoppingItemRepository,
                    warehouseRepository = warehouseRepository,
                    tagManager = tagManager,
                    accessHistoryManager = accessHistoryManager
                )
            }
        }
    }
}

@SuppressLint("StringFormatInvalid")
@RequiresApi(Build.VERSION_CODES.O)
@androidx.compose.runtime.Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ItemReminderToolApp(
    itemRepository: ItemRepository,
    categoryRepository: CategoryRepository,
    shoppingItemRepository: ShoppingItemRepository,
    warehouseRepository: WarehouseRepository,
    tagManager: TagManager,
    accessHistoryManager: AccessHistoryManager
) {
    fun extractErrorMessage(response: Response<*>): String? {
        val body = response.errorBody()?.string()?.trim().orEmpty()
        if (body.isEmpty()) return null
        return runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    val navController = rememberNavController()
    
    // 从导航控制器的当前状态获取实际的目标路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 根据路由找到对应的 Screen，如果找不到则默认为 Dashboard
    val currentDestination = remember(currentRoute) {
        when {
            currentRoute == Screen.Dashboard.route -> Screen.Dashboard
            currentRoute == Screen.Tags.route -> Screen.Tags
            currentRoute == Screen.ReminderList.route -> Screen.ReminderList
            currentRoute == Screen.ExcelImportExport.route -> Screen.ExcelImportExport
            currentRoute == Screen.CustomColorSettings.route -> Screen.CustomColorSettings
            currentRoute == Screen.Settings.route -> Screen.Settings
            currentRoute == Screen.Help.route -> Screen.Help
            currentRoute == Screen.About.route -> Screen.About
            else -> Screen.Dashboard // 默认返回首页
        }
    }
    
    // 密码保护相关状态
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    var isPasswordEnabled by remember { mutableStateOf(prefs.getBoolean("password_enabled", false)) }
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    var isUnlocked by remember { mutableStateOf(false) }
    var shouldShowLockScreen by remember { mutableStateOf(false) }
    
    // 使用真实的 AuthManager 管理登录状态
    val authManager = remember { AuthManager.getInstance(context) }
    var isLoggedIn by remember { mutableStateOf(authManager.isLoggedIn()) }
    var displayName by remember { mutableStateOf(authManager.getDisplayName() ?: "") }
    var accountName by remember { mutableStateOf(authManager.getAccount() ?: "") }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var editingDisplayName by remember { mutableStateOf("") }
    var editingAccountName by remember { mutableStateOf("") }
    var editingPassword by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var savedAccounts by remember { mutableStateOf(authManager.getSavedAccounts()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var accountFieldFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 监听密码状态变化
    LaunchedEffect(Unit) {
        // 使用 snapshotFlow 监听 SharedPreferences 的变化
        snapshotFlow {
            prefs.getBoolean("password_enabled", false)
        }.collect { enabled ->
            val premiumAllowed = PremiumFeatureManager.canAccessPremiumFeatures(context)
            canAccessPremiumFeatures = premiumAllowed
            val effectiveEnabled = enabled && premiumAllowed
            isPasswordEnabled = effectiveEnabled
            // 启用密码时不立即锁屏，只在应用进入后台后再锁屏
            if (!effectiveEnabled) {
                // 如果密码被禁用，解锁
                isUnlocked = true
                shouldShowLockScreen = false
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "premium_features" || key == "premium_lifetime" || key == "premium_trial_used" || key == "premium_trial_start_time") {
                val premiumAllowed = PremiumFeatureManager.canAccessPremiumFeatures(context)
                canAccessPremiumFeatures = premiumAllowed
                if (!premiumAllowed && prefs.getBoolean("password_enabled", false)) {
                    prefs.edit().putBoolean("password_enabled", false).apply()
                    isUnlocked = true
                    shouldShowLockScreen = false
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    
    // 检查是否需要显示锁屏（初始状态）
    LaunchedEffect(Unit) {
        if (isPasswordEnabled && canAccessPremiumFeatures) {
            // 应用启动时，如果密码已启用，需要验证密码
            shouldShowLockScreen = true
        } else {
            isUnlocked = true
        }
    }
    
    // 监听应用生命周期，当应用进入后台时标记需要锁屏，从后台返回时检查密码
    // 注意：如果正在处理 ActivityResult（如选择图片），不应该触发密码验证
    // 使用 SharedPreferences 来标记正在处理 ActivityResult，这样可以在各个 Screen 中设置
    var isProcessingActivityResult by remember { 
        mutableStateOf(prefs.getBoolean("is_processing_activity_result", false)) 
    }
    var shouldLockOnResume by remember { mutableStateOf(false) }
    
    // 监听 is_processing_activity_result 标记的变化
    LaunchedEffect(Unit) {
        snapshotFlow {
            prefs.getBoolean("is_processing_activity_result", false)
        }.collect { processing ->
            isProcessingActivityResult = processing
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // 应用进入后台时，如果密码已启用且已解锁，标记需要在恢复时锁屏
                    // 但不要立即标记，等待一小段时间，让 ActivityResult 有机会设置标记
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(100) // 延迟100ms，让ActivityResult有机会设置标记
                        val processing = prefs.getBoolean("is_processing_activity_result", false)
                        if (isPasswordEnabled && isUnlocked && !processing) {
                            shouldLockOnResume = true
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // 从后台返回时，延迟检查是否需要锁屏，给ActivityResult处理时间
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(200) // 延迟200ms，确保ActivityResult已经处理
                        val processing = prefs.getBoolean("is_processing_activity_result", false)
                        if (shouldLockOnResume && isPasswordEnabled && !processing) {
                            shouldShowLockScreen = true
                            isUnlocked = false
                            shouldLockOnResume = false
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 如果密码保护已启用且未解锁，显示锁屏
    if (shouldShowLockScreen && !isUnlocked) {
        PasswordLockScreen(
            onPasswordCorrect = {
                isUnlocked = true
                shouldShowLockScreen = false
            }
        )
        return
    }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            itemRepository,
            categoryRepository,
            warehouseRepository,
            shoppingItemRepository
        )
    )

    val itemViewModel: ItemViewModel = viewModel(
        factory = ItemViewModelFactory(
            context.applicationContext as Application,
            itemRepository,
            categoryRepository,
            warehouseRepository
        )
    )

    // 安全返回：若无法再出栈，则回到首页，避免栈空导致白屏/卡动画
    fun NavController.popBackOrDashboard() {
        val popped = popBackStack()
        if (!popped) {
            navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val categoryViewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModelFactory(categoryRepository)
    )

    val shoppingItemViewModel: ShoppingItemViewModel = viewModel(
        factory = ShoppingItemViewModelFactory(
            context.applicationContext as Application,
            shoppingItemRepository
        )
    )

    val warehouseViewModel: WarehouseViewModel = viewModel(
        factory = WarehouseViewModelFactory(
            context.applicationContext as Application,
            warehouseRepository,
            itemRepository
        )
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val savedWarehouseUuid = remember {
        prefs.getString("selected_warehouse_uuid", null)?.takeIf { it.isNotBlank() }
    }
    var selectedWarehouseUuid by remember { 
        mutableStateOf<String?>(savedWarehouseUuid) 
    }
    LaunchedEffect(selectedWarehouseUuid) {
        if (selectedWarehouseUuid != null) {
            prefs.edit().putString("selected_warehouse_uuid", selectedWarehouseUuid!!).apply()
        } else {
            prefs.edit().remove("selected_warehouse_uuid").apply()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true, // 启用手势以支持点击外部区域关闭
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp), // 设置侧边菜单宽度
                drawerContainerColor = com.example.itemremindertool.ui.theme.ColorHelpers.getGroup2PageBgColor(), // 与页面背景一致
                drawerContentColor = com.example.itemremindertool.ui.theme.ColorHelpers.getGroup4TextColor()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 账号信息
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val avatarChar = (displayName.ifBlank { accountName })
                                .trim()
                                .firstOrNull()
                                ?.uppercase()
                                ?: "?"
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(22.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarChar.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isLoggedIn && displayName.isNotBlank()) {
                                        displayName
                                    } else {
                                        stringResource(R.string.drawer_guest_name)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                Text(
                                    text = if (isLoggedIn && accountName.isNotBlank()) {
                                        stringResource(R.string.drawer_account_label, accountName)
                                    } else {
                                        stringResource(R.string.drawer_account_label, "-")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorHelpers.getGroup4TextColor(0.6f)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                if (isLoggedIn) {
                                    // 登出
                                    authManager.clearLoginInfo()
                                    isLoggedIn = false
                                    displayName = ""
                                    accountName = ""
                                } else {
                                    // 显示登录对话框
                                    editingAccountName = ""
                                    editingPassword = ""
                                    loginError = null
                                    accountDropdownExpanded = false
                                    showAccountDialog = true
                                }
                            }
                        ) {
                            Text(
                                text = if (isLoggedIn) {
                                    stringResource(R.string.drawer_logout)
                                } else {
                                    stringResource(R.string.drawer_login)
                                }
                            )
                        }
                    }
                    AppDivider(
                        color = ColorHelpers.getDividerColor(),
                        thickness = 2.dp
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Screen.Tags.icon, null) },
                        label = { Text(stringResource(R.string.nav_tag_management)) },
                        selected = currentDestination == Screen.Tags,
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            navController.navigate(Screen.Tags.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Screen.ReminderList.icon, null) },
                        label = { Text(stringResource(R.string.nav_reminder_list)) },
                        selected = currentDestination == Screen.ReminderList,
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            navController.navigate(Screen.ReminderList.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Screen.ExcelImportExport.icon, null) },
                        label = { Text(stringResource(R.string.nav_excel_import_export)) },
                        selected = currentDestination == Screen.ExcelImportExport,
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            navController.navigate(Screen.ExcelImportExport.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    
                    // Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Screen.Settings.icon, null) },
                        label = { Text(stringResource(R.string.settings)) },
                        selected = currentDestination == Screen.Settings,
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    
                    // Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Screen.Help.icon, null) },
                        label = { Text(stringResource(R.string.help)) },
                        selected = currentDestination == Screen.Help,
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            navController.navigate(Screen.Help.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Screen.About.icon, null) },
                        label = { Text(stringResource(R.string.about)) },
                        selected = currentDestination == Screen.About,
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            navController.navigate(Screen.About.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize()
            ) {
            composable(Screen.Dashboard.route) {
                // 获取 ViewModels
                val itemReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel>()
                val activityEventViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel>()

                DashboardScreen(
                    dashboardViewModel = dashboardViewModel,
                    itemViewModel = itemViewModel,
                    warehouseViewModel = warehouseViewModel,
                    shoppingItemViewModel = shoppingItemViewModel,
                    itemReminderViewModel = itemReminderViewModel,
                    activityEventViewModel = activityEventViewModel,
                    accessHistoryManager = accessHistoryManager,
                    onAddItem = { warehouseUuid ->
                        selectedWarehouseUuid = warehouseUuid
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemUuid ->
                        navController.navigate(Screen.EditItem.createRoute(itemUuid))
                    },
                    onViewItem = { itemUuid ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemUuid))
                    },
                    onScanBarcode = { navController.navigate(Screen.BarcodeScanner.route) },
                    onItemRecognition = { navController.navigate(Screen.ItemRecognition.route) },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAddWarehouse = {
                        navController.navigate(Screen.AddWarehouse.route)
                    },
                    onAddChildWarehouse = { parentUuid ->
                        selectedWarehouseUuid = parentUuid
                        navController.navigate(Screen.AddChildWarehouse.createRoute(parentUuid))
                    },
                    onEditWarehouse = { warehouseUuid ->
                        navController.navigate(Screen.EditWarehouse.createRoute(warehouseUuid))
                    },
                    onDeleteWarehouse = { warehouse ->
                        warehouseViewModel.deleteWarehouse(warehouse)
                    },
                    onAddAlert = { item ->
                        navController.navigate(Screen.ItemReminderSettings.createRoute(item.uuid))
                    },
                    onNavigateToWarehouseItemsTab = { warehouseUuid ->
                        selectedWarehouseUuid = warehouseUuid
                        navController.navigate(Screen.WarehouseItemsTab.createRoute(warehouseUuid)) {
                            launchSingleTop = true
                        }
                    },
                    initialSelectedWarehouseUuid = selectedWarehouseUuid,
                    onSelectedWarehouseUuidChanged = { warehouseUuid ->
                        selectedWarehouseUuid = warehouseUuid
                    }
                )
            }

            composable(Screen.AddItem.route) {
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                val pendingFeatureCode by itemViewModel.pendingFeatureCode.collectAsState()
                ItemEditScreen(
                    itemUuid = null,
                    viewModel = itemViewModel,
                    categories = emptyList(),
                    warehouses = warehouses,
                    tagManager = tagManager,
                    initialFeatureCode = pendingFeatureCode,
                    initialWarehouseUuid = selectedWarehouseUuid,
                    onNavigateBack = { 
                        itemViewModel.clearPendingFeatureCode()
                        navController.popBackOrDashboard()
                    }
                )
            }
            
            composable(Screen.ItemRecognition.route) {
                ItemRecognitionScreen(
                    onFeatureExtracted = { featureCode ->
                        // 将特征码存储到 ViewModel
                        itemViewModel.setPendingFeatureCode(featureCode)
                        val preview = featureCode?.substring(0, minOf(50, featureCode.length)) ?: "null"
                        android.util.Log.d(
                            "MainActivity",
                            context.getString(R.string.feature_extract_log, preview)
                        )
                        // 跳转到添加物品页面
                        val popped = navController.popBackStack()
                        navController.navigate(Screen.AddItem.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    },
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.EditItem.route,
                arguments = listOf(navArgument("itemUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemUuid = backStackEntry.arguments?.getString("itemUuid") ?: return@composable
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                LaunchedEffect(itemUuid) {
                    itemViewModel.loadItemByUuid(itemUuid)
                }
                val selectedItem by itemViewModel.uiState.collectAsState()
                LaunchedEffect(selectedItem.selectedItem) {
                    selectedItem.selectedItem?.warehouseUuid?.let { uuid ->
                        selectedWarehouseUuid = uuid
                    }
                }
                ItemEditScreen(
                    itemUuid = itemUuid,
                    viewModel = itemViewModel,
                    categories = emptyList(),
                    warehouses = warehouses,
                    tagManager = tagManager,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.ItemDetail.route,
                arguments = listOf(navArgument("itemUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemUuid = backStackEntry.arguments?.getString("itemUuid") ?: return@composable
                val itemReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel>()
                com.example.itemremindertool.ui.screens.ItemDetailScreen(
                    itemUuid = itemUuid,
                    itemViewModel = itemViewModel,
                    reminderViewModel = itemReminderViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onEditItem = { editItemUuid ->
                        navController.navigate(Screen.EditItem.createRoute(editItemUuid))
                    },
                    onAddAlert = { item ->
                        navController.navigate(Screen.ItemReminderSettings.createRoute(item.uuid))
                    }
                )
            }

            composable(
                route = Screen.ItemReminderSettings.route,
                arguments = listOf(navArgument("itemUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemUuid = backStackEntry.arguments?.getString("itemUuid") ?: return@composable
                val itemReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel>()
                LaunchedEffect(itemUuid) {
                    itemViewModel.loadItemByUuid(itemUuid)
                }
                val selectedItem by itemViewModel.uiState.collectAsState()
                val item = selectedItem.selectedItem
                if (item != null) {
                    com.example.itemremindertool.ui.screens.ItemReminderSettingsScreen(
                        item = item,
                        viewModel = itemReminderViewModel,
                        onNavigateBack = { navController.popBackOrDashboard() }
                    )
                }
            }

            composable(Screen.BarcodeScanner.route) {
                // 获取当前布局风格
                BarcodeScannerScreen(
                    onBarcodeScanned = { scannedValue ->
                        // 先关闭扫描页面
                        navController.popBackStack()
                        
                        // 尝试解析为容器二维码
                        val warehouseInfo = com.example.itemremindertool.utils.QRCodeUtils.decodeWarehouseInfo(scannedValue)
                        if (warehouseInfo != null) {
                            selectedWarehouseUuid = warehouseInfo.uuid
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            itemViewModel.getItemByBarcode(scannedValue) { item ->
                                if (item != null) {
                                    navController.navigate(Screen.ItemDetail.createRoute(item.uuid)) {
                                        launchSingleTop = true
                                    }
                                } else {
                                    // 未找到物品，可以提示用户或打开添加物品页面
                                    navController.navigate(Screen.AddItem.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Tags.route) {
                TagsScreen(
                    itemViewModel = itemViewModel,
                    tagManager = tagManager,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(Screen.ReminderList.route) {
                val reminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel>()
                ReminderListScreen(
                    reminderViewModel = reminderViewModel,
                    itemViewModel = itemViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onNavigateToItem = { itemUuid ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemUuid))
                    }
                )
            }

            composable(Screen.ExcelImportExport.route) {
                ExcelImportExportScreen(
                    itemViewModel = itemViewModel,
                    tagManager = tagManager,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onNavigateToAppearance = { navController.navigate(Screen.AppearanceSettings.route) },
                    onNavigateToLanguage = { navController.navigate(Screen.LanguageSettings.route) },
                    onNavigateToWarehouse = { navController.navigate(Screen.WarehouseSettings.route) },
                    onNavigateToApp = { navController.navigate(Screen.AppSettings.route) },
                    onNavigateToCloudStorage = { navController.navigate(Screen.CloudStorageSettings.route) },
                    onNavigateToAlert = { navController.navigate(Screen.AlertSettings.route) },
                    onNavigateToBackupRestore = { navController.navigate(Screen.BackupRestore.route) }
                )
            }
            
            composable(Screen.Help.route) {
                HelpScreen(
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }
            
            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }
            
            composable(Screen.AppearanceSettings.route) {
                AppearanceSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onNavigateToTheme = { navController.navigate(Screen.ThemeSelection.route) },
                    onNavigateToColorScheme = { navController.navigate(Screen.ColorSchemeSelection.route) },
                    onNavigateToIcon = { navController.navigate(Screen.IconSelection.route) },
                    onNavigateToCustomColors = { navController.navigate(Screen.CustomColorSettings.route) }
                )
            }

            composable(Screen.CustomColorSettings.route) {
                CustomColorSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }
            
            composable(Screen.ThemeSelection.route) {
                ThemeSelectionScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onApply = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable(Screen.ColorSchemeSelection.route) {
                ColorSchemeSelectionScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onApply = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable(Screen.IconSelection.route) {
                IconSelectionScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onApply = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable(Screen.LanguageSettings.route) {
                LanguageSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onApply = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable(Screen.WarehouseSettings.route) {
                WarehouseSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }
            
            composable(Screen.AppSettings.route) {
                val activityEventViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel>()
                AppSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    activityEventViewModel = activityEventViewModel
                )
            }
            
            composable(Screen.CloudStorageSettings.route) {
                val cloudStorageViewModel: CloudStorageViewModel = viewModel()
                CloudStorageSettingsScreen(
                    viewModel = cloudStorageViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }
            
            composable(Screen.AlertSettings.route) {
                AlertSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }
            
            composable(Screen.BackupRestore.route) {
                val backupRestoreViewModel: BackupRestoreViewModel = viewModel()
                BackupRestoreScreen(
                    viewModel = backupRestoreViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(Screen.AddWarehouse.route) {
                WarehouseEditScreen(
                    warehouseUuid = null,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.AddChildWarehouse.route,
                arguments = listOf(navArgument("parentUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val parentUuid = backStackEntry.arguments?.getString("parentUuid") ?: return@composable
                WarehouseEditScreen(
                    warehouseUuid = null,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() },
                    initialParentUuid = parentUuid
                )
            }

            composable(
                route = Screen.EditWarehouse.route,
                arguments = listOf(navArgument("warehouseUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val warehouseUuid = backStackEntry.arguments?.getString("warehouseUuid") ?: return@composable
                WarehouseEditScreen(
                    warehouseUuid = warehouseUuid,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.WarehouseItems.route,
                arguments = listOf(navArgument("warehouseUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val warehouseUuid = backStackEntry.arguments?.getString("warehouseUuid") ?: return@composable
                WarehouseItemsScreen(
                    warehouseUuid = warehouseUuid,
                    warehouseViewModel = warehouseViewModel,
                    itemViewModel = itemViewModel,
                    onEditItem = { itemUuid ->
                        navController.navigate(Screen.EditItem.createRoute(itemUuid))
                    },
                    onViewItem = { itemUuid ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemUuid))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.WarehouseItemsTab.route,
                arguments = listOf(navArgument("warehouseUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val warehouseUuid = backStackEntry.arguments?.getString("warehouseUuid") ?: return@composable
                WarehouseDetailScreen(
                    warehouseUuid = warehouseUuid,
                    warehouseViewModel = warehouseViewModel,
                    itemViewModel = itemViewModel,
                    shoppingItemViewModel = shoppingItemViewModel,
                    accessHistoryManager = accessHistoryManager,
                    onAddItem = { addWarehouseUuid ->
                        selectedWarehouseUuid = addWarehouseUuid
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemUuid ->
                        selectedWarehouseUuid = warehouseUuid
                        navController.navigate(Screen.EditItem.createRoute(itemUuid))
                    },
                    onViewItem = { itemUuid ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemUuid))
                    },
                    onAddChildWarehouse = { parentUuid ->
                        navController.navigate(Screen.AddChildWarehouse.createRoute(parentUuid))
                    },
                    onEditWarehouse = { whUuid ->
                        navController.navigate(Screen.EditWarehouse.createRoute(whUuid))
                    },
                    onDeleteWarehouse = { warehouse ->
                        warehouseViewModel.deleteWarehouse(warehouse)
                        navController.popBackOrDashboard()
                    },
                    onNavigateToWarehouseItemsTab = { childWarehouseUuid ->
                        navController.navigate(Screen.WarehouseItemsTab.createRoute(childWarehouseUuid))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onNavigateToParentWarehouse = { targetWarehouseUuid ->
                        val targetRoute = Screen.WarehouseItemsTab.createRoute(targetWarehouseUuid)
                        val popped = navController.popBackStack(targetRoute, inclusive = false)
                        if (!popped) {
                            navController.popBackOrDashboard()
                            navController.navigate(targetRoute)
                        }
                    }
                )
            }
        }
            // 登录对话框
            if (showAccountDialog) {
                val dialogButtonShape = MaterialTheme.shapes.medium
                val attemptLogin = {
                    val account = editingAccountName.trim()
                    val password = editingPassword.trim()
                    if (account.isEmpty() || password.isEmpty()) {
                        loginError = "账号和密码不能为空"
                    } else {
                        isLoggingIn = true
                        loginError = null

                        scope.launch {
                            try {
                                val apiService = RetrofitClient.getApiService(context)
                                val response = apiService.login(LoginRequest(account, password))
                            
                            if (response.isSuccessful && response.body()?.success == true) {
                                val authResponse = response.body()?.data
                                if (authResponse != null) {
                                    // 保存登录信息
                                    authManager.saveLoginInfo(
                                        token = authResponse.token,
                                        userUuid = authResponse.user.uuid,
                                        account = authResponse.user.account,
                                        displayName = authResponse.user.displayName
                                    )
                                    savedAccounts = authManager.getSavedAccounts()
                                    
                                    // 更新 UI 状态
                                    isLoggedIn = true
                                    displayName = authResponse.user.displayName
                                    accountName = authResponse.user.account
                                    showAccountDialog = false
                                    SyncQueueWorker.schedule(context)
                                    SyncQueueWorker.runNow(context)
                                    scope.launch {
                                        SyncManager.getInstance(context).mergeRemoteAndLocalOnce()
                                    }
                                } else {
                                    loginError = "登录失败：响应数据为空"
                                }
                            } else {
                                val errorMsg = extractErrorMessage(response) ?: "登录失败"
                                loginError = errorMsg
                            }
                            } catch (e: Exception) {
                                loginError = "登录失败：${e.message}"
                                android.util.Log.e("MainActivity", "登录异常", e)
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    }
                }
                AppDialogLayout(
                    title = stringResource(R.string.drawer_login),
                    icon = Icons.Default.Person,
                    onDismiss = {
                        if (!isLoggingIn) {
                            showAccountDialog = false
                            loginError = null
                            accountDropdownExpanded = false
                        }
                    },
                    footer = {
                        OutlinedButton(
                            onClick = {
                                showAccountDialog = false
                                loginError = null
                                accountDropdownExpanded = false
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.weight(1f),
                            shape = dialogButtonShape
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                showAccountDialog = false
                                showRegisterDialog = true
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.weight(1f),
                            shape = dialogButtonShape
                        ) {
                            Text(stringResource(R.string.register))
                        }
                    }
                ) {
                    val accountMatches = remember(editingAccountName, savedAccounts) {
                        if (editingAccountName.isBlank()) {
                            savedAccounts
                        } else {
                            val keyword = editingAccountName.trim()
                            savedAccounts.filter { saved ->
                                saved.account.contains(keyword, ignoreCase = true) ||
                                    saved.displayName.contains(keyword, ignoreCase = true)
                            }
                        }
                    }
                    val shouldShowAccountDropdown = accountFieldFocused &&
                        accountMatches.isNotEmpty()
                    LaunchedEffect(shouldShowAccountDropdown) {
                        accountDropdownExpanded = shouldShowAccountDropdown
                    }
                    ExposedDropdownMenuBox(
                        expanded = accountDropdownExpanded,
                        onExpandedChange = { expanded ->
                            accountDropdownExpanded = expanded && accountMatches.isNotEmpty()
                        }
                    ) {
                        OutlinedTextField(
                            value = editingAccountName,
                            onValueChange = { editingAccountName = it },
                            label = { Text(stringResource(R.string.drawer_account_hint)) },
                            singleLine = true,
                            enabled = !isLoggingIn,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .onFocusChanged { accountFieldFocused = it.isFocused }
                        )
                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false },
                            modifier = Modifier
                                .background(ColorHelpers.getGroup2PageBgColor())
                        ) {
                            accountMatches.forEach { saved ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${saved.displayName} (${saved.account})")
                                            IconButton(
                                                onClick = {
                                                    authManager.removeSavedAccount(saved.account)
                                                    savedAccounts = authManager.getSavedAccounts()
                                                    val hasItems = if (editingAccountName.isBlank()) {
                                                        savedAccounts.isNotEmpty()
                                                    } else {
                                                        savedAccounts.any {
                                                            it.account.contains(editingAccountName.trim(), ignoreCase = true) ||
                                                                it.displayName.contains(editingAccountName.trim(), ignoreCase = true)
                                                        }
                                                    }
                                                    accountDropdownExpanded = accountFieldFocused && hasItems
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        val switched = authManager.switchToAccount(saved.account)
                                        if (switched != null) {
                                            savedAccounts = authManager.getSavedAccounts()
                                            isLoggedIn = true
                                            displayName = switched.displayName
                                            accountName = switched.account
                                            showAccountDialog = false
                                            loginError = null
                                            SyncQueueWorker.schedule(context)
                                            SyncQueueWorker.runNow(context)
                                            scope.launch {
                                                SyncManager.getInstance(context).mergeRemoteAndLocalOnce()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editingPassword,
                        onValueChange = { editingPassword = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                attemptLogin()
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.weight(1f),
                            shape = dialogButtonShape
                        ) {
                            Text(stringResource(R.string.drawer_login))
                        }
                    }
                    if (loginError != null) {
                        Text(
                            text = loginError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // 注册对话框
            if (showRegisterDialog) {
                val dialogButtonShape = MaterialTheme.shapes.medium
                AppDialogLayout(
                    title = stringResource(R.string.register),
                    icon = Icons.Default.PersonAdd,
                    onDismiss = {
                        if (!isLoggingIn) {
                            showRegisterDialog = false
                            loginError = null
                        }
                    },
                    footer = {
                        OutlinedButton(
                            onClick = {
                                showRegisterDialog = false
                                loginError = null
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.weight(1f),
                            shape = dialogButtonShape
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val name = editingDisplayName.trim()
                                val account = editingAccountName.trim()
                                val password = editingPassword.trim()
                                
                                if (name.isEmpty() || account.isEmpty() || password.isEmpty()) {
                                    loginError = "所有字段都不能为空"
                                    return@Button
                                }
                                
                                isLoggingIn = true
                                loginError = null
                                
                                scope.launch {
                                    try {
                                        val apiService = RetrofitClient.getApiService(context)
                                        val response = apiService.register(
                                            RegisterRequest(account, name, password)
                                        )
                                        
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val authResponse = response.body()?.data
                                            if (authResponse != null) {
                                                // 保存登录信息
                                                authManager.saveLoginInfo(
                                                    token = authResponse.token,
                                                    userUuid = authResponse.user.uuid,
                                                    account = authResponse.user.account,
                                                    displayName = authResponse.user.displayName
                                                )
                                                savedAccounts = authManager.getSavedAccounts()
                                                
                                                // 更新 UI 状态
                                                isLoggedIn = true
                                                displayName = authResponse.user.displayName
                                                accountName = authResponse.user.account
                                                showRegisterDialog = false
                                                SyncQueueWorker.schedule(context)
                                                SyncQueueWorker.runNow(context)
                                                scope.launch {
                                                    SyncManager.getInstance(context).mergeRemoteAndLocalOnce()
                                                }
                                            } else {
                                                loginError = "注册失败：响应数据为空"
                                            }
                                        } else {
                                            val errorMsg = extractErrorMessage(response) ?: "注册失败"
                                            loginError = errorMsg
                                        }
                                    } catch (e: Exception) {
                                        loginError = "注册失败：${e.message}"
                                        android.util.Log.e("MainActivity", "注册异常", e)
                                    } finally {
                                        isLoggingIn = false
                                    }
                                }
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.weight(1f),
                            shape = dialogButtonShape
                        ) {
                            Text(stringResource(R.string.register))
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = editingDisplayName,
                        onValueChange = { editingDisplayName = it },
                        label = { Text(stringResource(R.string.drawer_name_hint)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingAccountName,
                        onValueChange = { editingAccountName = it },
                        label = { Text(stringResource(R.string.drawer_account_hint)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingPassword,
                        onValueChange = { editingPassword = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showRegisterDialog = false
                                showAccountDialog = true
                                accountDropdownExpanded = false
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.weight(1f),
                            shape = dialogButtonShape
                        ) {
                            Text(stringResource(R.string.back_to_login))
                        }
                    }
                    if (loginError != null) {
                        Text(
                            text = loginError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
