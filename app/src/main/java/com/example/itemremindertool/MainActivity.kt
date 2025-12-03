package com.example.itemremindertool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.content.Context
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.repository.*
import com.example.itemremindertool.navigation.Screen
import com.example.itemremindertool.ui.screens.*
import com.example.itemremindertool.ui.screens.SettingsScreen
import com.example.itemremindertool.notification.NotificationScheduler
import com.example.itemremindertool.ui.theme.ItemReminderToolTheme
import com.example.itemremindertool.ui.viewmodel.*
import com.example.itemremindertool.utils.AppConfigManager
import com.example.itemremindertool.utils.LocaleHelper
import com.example.itemremindertool.utils.IconManager
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // 在创建 Activity 之前设置语言环境
        val language = LocaleHelper.getCurrentLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, language))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 确保语言设置已应用
        val language = LocaleHelper.getCurrentLanguage(this)
        LocaleHelper.setLocale(this, language)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val itemRepository = ItemRepository(database.itemDao())
        val categoryRepository = CategoryRepository(database.categoryDao())
        val shoppingItemRepository = ShoppingItemRepository(database.shoppingItemDao())
        val warehouseRepository = WarehouseRepository(database.warehouseDao())
        val tagManager = TagManager(applicationContext)

        // 启动通知调度
        NotificationScheduler.scheduleNotifications(this)
        
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
                    tagManager = tagManager
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
    tagManager: TagManager
) {
    val navController = rememberNavController()
    var currentDestination by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    
    // 密码保护相关状态
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    var isPasswordEnabled by remember { mutableStateOf(prefs.getBoolean("password_enabled", false)) }
    var isUnlocked by remember { mutableStateOf(false) }
    var shouldShowLockScreen by remember { mutableStateOf(false) }
    
    // 检查是否需要显示锁屏
    LaunchedEffect(Unit) {
        if (isPasswordEnabled) {
            shouldShowLockScreen = true
        } else {
            isUnlocked = true
        }
    }
    
    // 监听应用生命周期，当从后台返回时检查密码
    // 注意：如果正在处理 ActivityResult（如选择图片），不应该触发密码验证
    var isProcessingActivityResult by remember { mutableStateOf(false) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isPasswordEnabled && isUnlocked && !isProcessingActivityResult) {
                // 从后台返回，需要重新验证密码
                // 但如果正在处理 ActivityResult，则延迟验证
                shouldShowLockScreen = true
                isUnlocked = false
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
    var returnToAllItemsTab: (() -> Unit)? by remember { mutableStateOf(null) }
    var returnToWarehouseItemsTab: (() -> Unit)? by remember { mutableStateOf(null) }
    var isEditingFromAllItems by remember { mutableStateOf(false) } // 标记是否从所有物品页面进入编辑
    var isEditingFromWarehouseItems by remember { mutableStateOf(false) } // 标记是否从容器物品页面进入编辑
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.menu),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    Divider()
                    
                    NavigationDrawerItem(
                        icon = { Icon(Screen.Categories.icon, null) },
                        label = { Text(stringResource(R.string.nav_category_management)) },
                        selected = currentDestination == Screen.Categories,
                    onClick = {
                            currentDestination = Screen.Categories
                            navController.navigate(Screen.Categories.route) {
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
                            currentDestination = Screen.ShoppingList
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
                            currentDestination = Screen.Warehouses
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
                            currentDestination = Screen.Settings
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
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Dashboard.route) {
                currentDestination = Screen.Dashboard
                DashboardScreen(
                    dashboardViewModel = dashboardViewModel,
                    itemViewModel = itemViewModel,
                    warehouseViewModel = warehouseViewModel,
                    shoppingItemViewModel = shoppingItemViewModel,
                    onAddItem = { warehouseId ->
                        // 保存当前选中的容器ID，以便在添加物品后返回
                        if (warehouseId != null) {
                            selectedWarehouseId = warehouseId
                        }
                        navController.navigate(Screen.AddItem.route)
                    },
                    onEditItem = { itemId ->
                        // 判断是否从所有物品页面进入（selectedWarehouseId 为 null）
                        isEditingFromAllItems = selectedWarehouseId == null
                        // 判断是否从容器物品页面进入（selectedWarehouseId 不为 null）
                        isEditingFromWarehouseItems = selectedWarehouseId != null
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onScanBarcode = { navController.navigate(Screen.BarcodeScanner.route) },
                    onItemRecognition = { navController.navigate(Screen.ItemRecognition.route) },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToItems = {
                        // 切换到"所有物品"标签页（在 DashboardScreen 内部处理）
                    },
                    onNavigateToShoppingList = {
                        currentDestination = Screen.ShoppingList
                        navController.navigate(Screen.ShoppingList.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToWarehouses = {
                        currentDestination = Screen.Warehouses
                        navController.navigate(Screen.Warehouses.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onAddChildWarehouse = { parentId ->
                        selectedWarehouseId = parentId // 保存父容器ID
                        navController.navigate(Screen.AddChildWarehouse.createRoute(parentId))
                    },
                    // 使用当前的 selectedWarehouseId 初始化 Dashboard 的容器上下文；
                    // 从"所有物品"进入编辑时，这个值为 null，因此不会跳到容器页面。
                    initialSelectedWarehouseId = selectedWarehouseId,
                    onReturnToAllItemsTab = { callback ->
                        returnToAllItemsTab = callback
                    },
                    onReturnToWarehouseItemsTab = { callback ->
                        returnToWarehouseItemsTab = callback
                    }
                )
            }

            composable(Screen.AddItem.route) {
                val categories by categoryViewModel.categories.collectAsState(initial = emptyList())
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                val pendingFeatureCode by itemViewModel.pendingFeatureCode.collectAsState()
                // 判断是否从所有物品页面进入（selectedWarehouseId 为 null）
                val isFromAllItems = selectedWarehouseId == null
                // 判断是否从容器物品页面进入（selectedWarehouseId 不为 null）
                val isFromWarehouseItems = selectedWarehouseId != null
                ItemEditScreen(
                    itemId = null,
                    viewModel = itemViewModel,
                    categories = categories,
                    warehouses = warehouses,
                    tagManager = tagManager,
                    initialFeatureCode = pendingFeatureCode,
                    // 问题2修复：从所有物品页面进入时，不传递容器ID（为 null）
                    initialWarehouseId = if (isFromAllItems) null else selectedWarehouseId,
                    returnToAllItemsTab = if (isFromAllItems) returnToAllItemsTab else null, // 只有从所有物品页面进入时才传递
                    returnToWarehouseItemsTab = if (isFromWarehouseItems) returnToWarehouseItemsTab else null, // 只有从容器物品页面进入时才传递
                    onNavigateBack = { 
                        itemViewModel.clearPendingFeatureCode()
                        navController.popBackStack() 
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
                        navController.navigate(Screen.AddItem.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditItem.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                val categories by categoryViewModel.categories.collectAsState(initial = emptyList())
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                ItemEditScreen(
                    itemId = itemId,
                    viewModel = itemViewModel,
                    categories = categories,
                    warehouses = warehouses,
                    tagManager = tagManager,
                    // 问题1修复：从所有物品页面进入时，传递 returnToAllItemsTab，确保返回后保持在所有物品页面
                    returnToAllItemsTab = if (isEditingFromAllItems) returnToAllItemsTab else null,
                    // 问题3修复：从容器物品页面进入时，传递 returnToWarehouseItemsTab，确保返回后保持在容器物品页面
                    returnToWarehouseItemsTab = if (isEditingFromWarehouseItems) returnToWarehouseItemsTab else null,
                    onNavigateBack = { 
                        isEditingFromAllItems = false // 重置标志
                        isEditingFromWarehouseItems = false // 重置标志
                        navController.popBackStack() 
                    }
                )
            }

            composable(Screen.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        itemViewModel.getItemByBarcode(barcode) { item ->
                            if (item != null) {
                                navController.navigate(Screen.EditItem.createRoute(item.id))
                            } else {
                                navController.navigate(Screen.AddItem.route)
                            }
                        }
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                currentDestination = Screen.Categories
                CategoriesScreen(
                    viewModel = categoryViewModel,
                    onAddCategory = { navController.navigate(Screen.AddCategory.route) },
                    onEditCategory = { categoryId ->
                        navController.navigate(Screen.EditCategory.createRoute(categoryId))
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.AddCategory.route) {
                CategoryEditScreen(
                    categoryId = null,
                    viewModel = categoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditCategory.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
                CategoryEditScreen(
                    categoryId = categoryId,
                    viewModel = categoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ShoppingList.route) {
                currentDestination = Screen.ShoppingList
                ShoppingListScreen(
                    viewModel = shoppingItemViewModel,
                    onAddItem = { navController.navigate(Screen.AddShoppingItem.route) },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditShoppingItem.createRoute(itemId))
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.AddShoppingItem.route) {
                ShoppingItemEditScreen(
                    itemId = null,
                    viewModel = shoppingItemViewModel,
                    onNavigateBack = { navController.popBackStack() }
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                currentDestination = Screen.Settings
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAppearance = { navController.navigate(Screen.AppearanceSettings.route) },
                    onNavigateToLanguage = { navController.navigate(Screen.LanguageSettings.route) },
                    onNavigateToWarehouse = { navController.navigate(Screen.WarehouseSettings.route) },
                    onNavigateToApp = { navController.navigate(Screen.AppSettings.route) },
                    onNavigateToCloudStorage = { navController.navigate(Screen.CloudStorageSettings.route) }
                )
            }
            
            composable(Screen.AppearanceSettings.route) {
                AppearanceSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.LanguageSettings.route) {
                LanguageSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.WarehouseSettings.route) {
                WarehouseSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.AppSettings.route) {
                AppSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.CloudStorageSettings.route) {
                CloudStorageSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Warehouses.route) {
                currentDestination = Screen.Warehouses
                WarehousesScreen(
                    viewModel = warehouseViewModel,
                    onAddWarehouse = { navController.navigate(Screen.AddWarehouse.route) },
                    onEditWarehouse = { warehouseId ->
                        navController.navigate(Screen.EditWarehouse.createRoute(warehouseId))
                    },
                    onViewItems = { warehouseId ->
                        navController.navigate(Screen.WarehouseItems.createRoute(warehouseId))
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.AddWarehouse.route) {
                WarehouseEditScreen(
                    warehouseId = null,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackStack() }
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
                        navController.popBackStack()
                        // 返回到 Dashboard 后，保持父容器选中状态
                    },
                    initialParentId = parentId,
                    onSaveSuccess = { savedParentId ->
                        // 保存成功后，设置选中的容器为父容器
                        selectedWarehouseId = savedParentId
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
                    onNavigateBack = { navController.popBackStack() }
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
