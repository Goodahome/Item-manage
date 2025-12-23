package com.example.itemremindertool

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.content.Context
import android.content.SharedPreferences
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
import com.example.itemremindertool.ui.viewmodel.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemremindertool.utils.AppConfigManager
import com.example.itemremindertool.utils.LocaleHelper
import com.example.itemremindertool.utils.IconManager
import com.example.itemremindertool.utils.AppRefreshManager
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // 在创建 Activity 之前设置语言环境
        val language = LocaleHelper.getCurrentLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, language))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 设置窗口背景色，避免转场动画时显示白色
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 确保语言设置已应用
        val language = LocaleHelper.getCurrentLanguage(this)
        LocaleHelper.setLocale(this, language)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val itemRepository = ItemRepository(database.itemDao(), database.deletedRecordDao(), applicationContext)
        val categoryRepository = CategoryRepository(database.categoryDao())
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

@androidx.compose.runtime.Composable
fun ItemReminderToolApp(
    itemRepository: ItemRepository,
    categoryRepository: CategoryRepository,
    shoppingItemRepository: ShoppingItemRepository,
    warehouseRepository: WarehouseRepository,
    tagManager: TagManager,
    accessHistoryManager: AccessHistoryManager
) {
    val navController = rememberNavController()
    
    // 从导航控制器的当前状态获取实际的目标路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 根据路由找到对应的 Screen，如果找不到则默认为 Dashboard
    val currentDestination = remember(currentRoute) {
        when {
            currentRoute == Screen.Dashboard.route -> Screen.Dashboard
            currentRoute == Screen.AllItems.route -> Screen.AllItems
            currentRoute == Screen.Tags.route -> Screen.Tags
            currentRoute == Screen.ShoppingList.route -> Screen.ShoppingList
            currentRoute == Screen.Warehouses.route -> Screen.Warehouses
            currentRoute == Screen.Settings.route -> Screen.Settings
            currentRoute?.startsWith(Screen.FilteredItems.route.split("/")[0]) == true -> Screen.AllItems
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
    var isUnlocked by remember { mutableStateOf(false) }
    var shouldShowLockScreen by remember { mutableStateOf(false) }
    
    // 监听密码状态变化
    LaunchedEffect(Unit) {
        // 使用 snapshotFlow 监听 SharedPreferences 的变化
        snapshotFlow {
            prefs.getBoolean("password_enabled", false)
        }.collect { enabled ->
            isPasswordEnabled = enabled
            // 启用密码时不立即锁屏，只在应用进入后台后再锁屏
            if (!enabled) {
                // 如果密码被禁用，解锁
                isUnlocked = true
                shouldShowLockScreen = false
            }
        }
    }
    
    // 检查是否需要显示锁屏（初始状态）
    LaunchedEffect(Unit) {
        if (isPasswordEnabled) {
            // 应用启动时，如果密码已启用，需要验证密码
            shouldShowLockScreen = true
        } else {
            isUnlocked = true
        }
    }
    
    // 监听应用生命周期，当应用进入后台时标记需要锁屏，从后台返回时检查密码
    // 注意：如果正在处理 ActivityResult（如选择图片），不应该触发密码验证
    var isProcessingActivityResult by remember { mutableStateOf(false) }
    var shouldLockOnResume by remember { mutableStateOf(false) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // 应用进入后台时，如果密码已启用且已解锁，标记需要在恢复时锁屏
                    if (isPasswordEnabled && isUnlocked && !isProcessingActivityResult) {
                        shouldLockOnResume = true
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // 从后台返回时，如果需要锁屏且密码已启用，显示锁屏
                    if (shouldLockOnResume && isPasswordEnabled && !isProcessingActivityResult) {
                        shouldShowLockScreen = true
                        isUnlocked = false
                        shouldLockOnResume = false
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
        factory = ShoppingItemViewModelFactory(shoppingItemRepository)
    )

    val warehouseViewModel: WarehouseViewModel = viewModel(
        factory = WarehouseViewModelFactory(warehouseRepository, itemRepository)
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 维护当前选中的容器ID状态
    var selectedWarehouseId by remember { mutableStateOf<Long?>(null) }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
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
                    // 标题行，包含标题和关闭按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.menu),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = com.example.itemremindertool.ui.theme.ColorHelpers.getGroup4TextColor()
                            )
                        }
                    }
                    Divider()
                    
                    NavigationDrawerItem(
                        icon = { Icon(Screen.AllItems.icon, null) },
                        label = { Text(stringResource(R.string.nav_all_items)) },
                        selected = currentDestination == Screen.AllItems,
                        onClick = {
                            navController.navigate(Screen.AllItems.route) {
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
                        icon = { Icon(Screen.Tags.icon, null) },
                        label = { Text(stringResource(R.string.nav_tag_management)) },
                        selected = currentDestination == Screen.Tags,
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
                        icon = { Icon(Screen.ShoppingList.icon, null) },
                        label = { Text(stringResource(R.string.nav_shopping_basket)) },
                        selected = currentDestination == Screen.ShoppingList,
                        onClick = {
                            navController.navigate(Screen.ShoppingList.route) {
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
                        icon = { Icon(Screen.Warehouses.icon, null) },
                        label = { Text(stringResource(R.string.nav_warehouse_management)) },
                        selected = currentDestination == Screen.Warehouses,
                        onClick = {
                            navController.navigate(Screen.Warehouses.route) {
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
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Screen.Settings.icon, null) },
                        label = { Text(stringResource(R.string.settings)) },
                        selected = currentDestination == Screen.Settings,
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
                    onAddItem = { warehouseId ->
                        // 保存当前选中的容器ID，用于设置初始容器
                        if (warehouseId != null) {
                            selectedWarehouseId = warehouseId
                        } else {
                            // 从所有物品页面添加时，清理 selectedWarehouseId
                            selectedWarehouseId = null
                        }
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onViewItem = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    onScanBarcode = { navController.navigate(Screen.BarcodeScanner.route) },
                    onItemRecognition = { navController.navigate(Screen.ItemRecognition.route) },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToItems = { filterType ->
                        // 跳转到筛选后的物品列表
                        if (filterType != null) {
                            navController.navigate(Screen.FilteredItems.createRoute(filterType)) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(Screen.AllItems.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToShoppingList = {
                        navController.navigate(Screen.ShoppingList.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToWarehouses = {
                        navController.navigate(Screen.Warehouses.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onAddWarehouse = {
                        navController.navigate(Screen.AddWarehouse.route)
                    },
                    onAddChildWarehouse = { parentId ->
                        selectedWarehouseId = parentId // 保存父容器ID
                        navController.navigate(Screen.AddChildWarehouse.createRoute(parentId))
                    },
                    onEditWarehouse = { warehouseId ->
                        navController.navigate(Screen.EditWarehouse.createRoute(warehouseId))
                    },
                    onDeleteWarehouse = { warehouse ->
                        warehouseViewModel.deleteWarehouse(warehouse)
                    },
                    onAddAlert = { item ->
                        navController.navigate(Screen.ItemReminderSettings.createRoute(item.id))
                    },
                    onNavigateToWarehouseItemsTab = { warehouseId ->
                        selectedWarehouseId = warehouseId
                        // 直接导航，不使用 popUpTo，让返回按钮正常工作
                        navController.navigate(Screen.WarehouseItemsTab.createRoute(warehouseId)) {
                            launchSingleTop = true
                        }
                    },
                    // 传递 selectedWarehouseId，确保返回时保持在当前容器页面
                    initialSelectedWarehouseId = selectedWarehouseId,
                    onSelectedWarehouseIdChanged = { warehouseId ->
                        selectedWarehouseId = warehouseId
                    }
                )
            }

            composable(Screen.AddItem.route) {
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                val pendingFeatureCode by itemViewModel.pendingFeatureCode.collectAsState()
                ItemEditScreen(
                    itemId = null,
                    viewModel = itemViewModel,
                    categories = emptyList(),
                    warehouses = warehouses,
                    tagManager = tagManager,
                    initialFeatureCode = pendingFeatureCode,
                    // 从所有物品页面进入时，不传递容器ID（为 null）
                    initialWarehouseId = if (selectedWarehouseId == null) null else selectedWarehouseId,
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
                        android.util.Log.d("MainActivity", "特征提取完成，featureCode: ${featureCode?.substring(0, minOf(50, featureCode?.length ?: 0)) ?: "null"}...")
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
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                
                // 加载物品并设置 selectedWarehouseId，以便返回时保持在当前容器页面
                LaunchedEffect(itemId) {
                    itemViewModel.loadItem(itemId)
                }
                val selectedItem by itemViewModel.uiState.collectAsState()
                LaunchedEffect(selectedItem.selectedItem) {
                    selectedItem.selectedItem?.warehouseId?.let { warehouseId ->
                        selectedWarehouseId = warehouseId
                    }
                }
                
                ItemEditScreen(
                    itemId = itemId,
                    viewModel = itemViewModel,
                    categories = emptyList(),
                    warehouses = warehouses,
                    tagManager = tagManager,
                    onNavigateBack = { 
                        // 不清理 selectedWarehouseId，保持在当前选中的容器页面
                        navController.popBackOrDashboard() 
                    }
                )
            }

            composable(
                route = Screen.ItemDetail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                val itemReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel>()
                
                com.example.itemremindertool.ui.screens.ItemDetailScreen(
                    itemId = itemId,
                    itemViewModel = itemViewModel,
                    reminderViewModel = itemReminderViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onEditItem = { editItemId ->
                        navController.navigate(Screen.EditItem.createRoute(editItemId))
                    },
                    onAddAlert = { item ->
                        navController.navigate(Screen.ItemReminderSettings.createRoute(item.id))
                    }
                )
            }

            composable(
                route = Screen.ItemReminderSettings.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                val itemReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel>()
                
                // 加载物品信息
                LaunchedEffect(itemId) {
                    itemViewModel.loadItem(itemId)
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
                val context = LocalContext.current
                val prefs = remember { 
                    context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) 
                }
                val homeLayoutStyle = remember {
                    val savedStyle = prefs.getString("home_layout_style", "discord")
                    if (savedStyle == "discord") {
                        com.example.itemremindertool.ui.screens.HomeLayoutStyle.DISCORD
                    } else {
                        com.example.itemremindertool.ui.screens.HomeLayoutStyle.CLASSIC
                    }
                }
                
                BarcodeScannerScreen(
                    onBarcodeScanned = { scannedValue ->
                        // 先关闭扫描页面
                        navController.popBackStack()
                        
                        // 尝试解析为容器二维码
                        val warehouseInfo = com.example.itemremindertool.utils.QRCodeUtils.decodeWarehouseInfo(scannedValue)
                        if (warehouseInfo != null) {
                            // 是容器二维码，打开对应容器
                            if (homeLayoutStyle == com.example.itemremindertool.ui.screens.HomeLayoutStyle.DISCORD) {
                                // Discord风格：直接导航到Dashboard并选中容器
                                selectedWarehouseId = warehouseInfo.id
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                // 经典风格：跳转到容器页面
                                navController.navigate(Screen.WarehouseItemsTab.createRoute(warehouseInfo.id)) {
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            // 不是容器二维码，尝试作为条形码查找物品
                            itemViewModel.getItemByBarcode(scannedValue) { item ->
                                if (item != null) {
                                    // 找到物品，打开物品信息页面
                                    navController.navigate(Screen.ItemDetail.createRoute(item.id)) {
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

            composable(Screen.ShoppingList.route) {
                ShoppingListScreen(
                    viewModel = shoppingItemViewModel,
                    onAddItem = { navController.navigate(Screen.AddShoppingItem.route) },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditShoppingItem.createRoute(itemId))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(Screen.AddShoppingItem.route) {
                ShoppingItemEditScreen(
                    itemId = null,
                    viewModel = shoppingItemViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.EditShoppingItem.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                ShoppingItemEditScreen(
                    itemId = itemId,
                    viewModel = shoppingItemViewModel,
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
            
            composable(Screen.AppearanceSettings.route) {
                AppearanceSettingsScreen(
                    onNavigateBack = { navController.popBackOrDashboard() },
                    onNavigateToTheme = { navController.navigate(Screen.ThemeSelection.route) },
                    onNavigateToColorScheme = { navController.navigate(Screen.ColorSchemeSelection.route) },
                    onNavigateToIcon = { navController.navigate(Screen.IconSelection.route) }
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

            composable(Screen.Warehouses.route) {
                WarehousesScreen(
                    viewModel = warehouseViewModel,
                    itemViewModel = itemViewModel,
                    onAddWarehouse = { navController.navigate(Screen.AddWarehouse.route) },
                    onEditWarehouse = { warehouseId ->
                        navController.navigate(Screen.EditWarehouse.createRoute(warehouseId))
                    },
                    onViewItems = { warehouseId ->
                        // 导航到容器详情页面，与首页点击容器卡片的行为一致
                        navController.navigate(Screen.WarehouseItemsTab.createRoute(warehouseId))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(Screen.AddWarehouse.route) {
                WarehouseEditScreen(
                    warehouseId = null,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { 
                        // 不清理 selectedWarehouseId，保持在当前选中的容器页面（如果是从容器页面添加）
                        navController.popBackOrDashboard() 
                    }
                )
            }

            composable(
                route = Screen.AddChildWarehouse.route,
                arguments = listOf(navArgument("parentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val parentId = backStackEntry.arguments?.getLong("parentId") ?: return@composable
                WarehouseEditScreen(
                    warehouseId = null,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { 
                        // 不清理 selectedWarehouseId，保持在当前选中的容器页面
                        navController.popBackOrDashboard()
                    },
                    initialParentId = parentId,
                    onSaveSuccess = { savedParentId ->
                        // 保存成功后，不设置 selectedWarehouseId，避免返回时意外导航
                        // selectedWarehouseId 应该只在用户主动点击容器时设置
                    }
                )
            }

            composable(
                route = Screen.EditWarehouse.route,
                arguments = listOf(navArgument("warehouseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val warehouseId = backStackEntry.arguments?.getLong("warehouseId") ?: return@composable
                WarehouseEditScreen(
                    warehouseId = warehouseId,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(
                route = Screen.WarehouseItems.route,
                arguments = listOf(navArgument("warehouseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val warehouseId = backStackEntry.arguments?.getLong("warehouseId") ?: return@composable
                WarehouseItemsScreen(
                    warehouseId = warehouseId,
                    warehouseViewModel = warehouseViewModel,
                    itemViewModel = itemViewModel,
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onViewItem = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() }
                )
            }

            composable(Screen.AllItems.route) {
                AllItemsScreen(
                    itemViewModel = itemViewModel,
                    shoppingItemViewModel = shoppingItemViewModel,
                    warehouseViewModel = warehouseViewModel,
                    onAddItem = { 
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() },
                    filterType = null
                )
            }
            
            composable(
                route = Screen.FilteredItems.route,
                arguments = listOf(navArgument("filterType") { type = NavType.StringType })
            ) { backStackEntry ->
                val filterType = backStackEntry.arguments?.getString("filterType")
                AllItemsScreen(
                    itemViewModel = itemViewModel,
                    shoppingItemViewModel = shoppingItemViewModel,
                    warehouseViewModel = warehouseViewModel,
                    onAddItem = { 
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onNavigateBack = { navController.popBackOrDashboard() },
                    filterType = filterType
                )
            }

            composable(
                route = Screen.WarehouseItemsTab.route,
                arguments = listOf(navArgument("warehouseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val warehouseId = backStackEntry.arguments?.getLong("warehouseId") ?: return@composable
                // 创建一个带有标签页的容器详情页面（容器信息 + 容器物品）
                WarehouseDetailScreen(
                    warehouseId = warehouseId,
                    warehouseViewModel = warehouseViewModel,
                    itemViewModel = itemViewModel,
                    shoppingItemViewModel = shoppingItemViewModel,
                    accessHistoryManager = accessHistoryManager,
                    onAddItem = { warehouseId ->
                        // 保存当前选中的容器ID，用于设置初始容器
                        selectedWarehouseId = warehouseId
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemId ->
                        // 设置 selectedWarehouseId，以便返回时保持在当前容器页面
                        selectedWarehouseId = warehouseId
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onViewItem = { itemId ->
                        // 导航到物品详情页面
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    onAddChildWarehouse = { parentId ->
                        navController.navigate(Screen.AddChildWarehouse.createRoute(parentId))
                    },
                    onEditWarehouse = { warehouseId ->
                        navController.navigate(Screen.EditWarehouse.createRoute(warehouseId))
                    },
                    onDeleteWarehouse = { warehouse ->
                        warehouseViewModel.deleteWarehouse(warehouse)
                        navController.popBackOrDashboard()
                    },
                    onNavigateToWarehouseItemsTab = { childWarehouseId ->
                        // 正常导航到子容器详情页面，保持导航栈，让返回按钮能够正常工作
                        navController.navigate(Screen.WarehouseItemsTab.createRoute(childWarehouseId))
                    },
                    onNavigateBack = { 
                        // 返回时清理 selectedWarehouseId，避免意外导航到容器页面
                        selectedWarehouseId = null
                        navController.popBackOrDashboard() 
                    },
                    onNavigateToParentWarehouse = { targetWarehouseId ->
                        // 导航到目标容器（可能是父容器或更上层的容器）
                        // 策略：尝试弹出到目标容器，如果不存在则导航到它
                        val targetRoute = Screen.WarehouseItemsTab.createRoute(targetWarehouseId)
                        
                        // 尝试弹出到目标路由（如果存在）
                        val popped = navController.popBackStack(targetRoute, inclusive = false)
                        
                        if (!popped) {
                            // 如果目标路由不在栈中，直接导航到它
                            // 先弹出当前页面，然后导航到目标
                            navController.popBackOrDashboard()
                            navController.navigate(targetRoute)
                        }
                    }
                )
            }
        }
        }
    }
}
