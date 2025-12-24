package com.example.itemremindertool.ui.screens

import android.graphics.BitmapFactory
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.itemremindertool.R
import com.example.itemremindertool.data.AlertSettingsManager
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.WarehouseQRCodeDialog
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.OnboardingOverlay
import com.example.itemremindertool.ui.components.OnboardingStep
import com.example.itemremindertool.ui.components.DynamicBannerAd
import com.example.itemremindertool.ui.components.WarehouseSelectionBottomSheet
import com.example.itemremindertool.ui.components.CameraCaptureDialog
import com.example.itemremindertool.ui.components.ImageCropDialog
import com.example.itemremindertool.ui.theme.ColorHelpers
import kotlinx.coroutines.Dispatchers
import java.io.File
import com.example.itemremindertool.ui.theme.LocalAppSettings
import com.example.itemremindertool.ui.viewmodel.DashboardViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.OperationState
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.utils.SyncStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

/**
 * 首页布局风格枚举
 */
enum class HomeLayoutStyle(val key: String) {
    DISCORD("discord"),      // Discord风格（左侧容器列）
    CLASSIC("classic")       // 经典风格（卡片式布局）
}

/**
 * 获取首页布局风格偏好
 */
@Composable
fun rememberHomeLayoutStyle(): MutableState<HomeLayoutStyle> {
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) 
    }
    
    return remember {
        val savedStyle = prefs.getString("home_layout_style", HomeLayoutStyle.DISCORD.key)
        mutableStateOf(
            HomeLayoutStyle.values().find { it.key == savedStyle } ?: HomeLayoutStyle.DISCORD
        )
    }
}

/**
 * 保存首页布局风格偏好
 */
fun saveHomeLayoutStyle(context: android.content.Context, style: HomeLayoutStyle) {
    val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    prefs.edit().putString("home_layout_style", style.key).apply()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    itemViewModel: ItemViewModel,
    warehouseViewModel: com.example.itemremindertool.ui.viewmodel.WarehouseViewModel,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel,
    itemReminderViewModel: com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel? = null, // 物品提醒ViewModel
    activityEventViewModel: com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel? = null, // 动态事件ViewModel
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    onAddItem: (Long?) -> Unit, // 传递当前选中的容器ID
    onEditItem: (Long) -> Unit,
    onViewItem: (Long) -> Unit = {}, // 查看物品信息回调
    onScanBarcode: () -> Unit,
    onItemRecognition: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToItems: (String?) -> Unit = {}, // 接收筛选类型参数，null 表示显示所有物品
    onNavigateToShoppingList: () -> Unit = {},
    onNavigateToWarehouses: () -> Unit = {},
    onAddWarehouse: () -> Unit = {}, // 添加容器回调
    onAddChildWarehouse: (Long) -> Unit = {},
    onEditWarehouse: (Long) -> Unit = {}, // 编辑容器回调
    onDeleteWarehouse: (com.example.itemremindertool.data.model.Warehouse) -> Unit = {}, // 删除容器回调
    onDeleteItem: (Item) -> Unit = {}, // 删除物品回调
    onAddAlert: (Item) -> Unit = {}, // 添加提醒回调
    onNavigateToWarehouseItemsTab: (Long) -> Unit = {}, // 导航到容器物品页面
    initialSelectedWarehouseId: Long? = null, // 初始选中的容器ID
    onSelectedWarehouseIdChanged: (Long?) -> Unit = {}, // 选中容器ID变化时的回调
    modifier: Modifier = Modifier
) {
    val stats by dashboardViewModel.stats.collectAsState()
    val context = LocalContext.current
    
    // 首次启动引导状态
    val prefs = remember(context) { 
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    }
    val hasCompletedOnboarding = remember { 
        mutableStateOf(prefs.getBoolean("has_completed_onboarding", false))
    }
    var currentOnboardingStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding.value) }
    
    // Discord 图标圆形设置
    var discordIconCircle by remember { mutableStateOf(prefs.getBoolean("discord_icon_circle", false)) }
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "discord_icon_circle") {
                discordIconCircle = prefs.getBoolean("discord_icon_circle", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    
    // 监听数据库恢复时间戳，以便在恢复后刷新UI
    val restoreTimestamp = remember { 
        mutableStateOf(prefs.getLong("database_restore_timestamp", 0L))
    }
    
    // 定期检查恢复时间戳变化（每秒检查一次）
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val currentTimestamp = prefs.getLong("database_restore_timestamp", 0L)
            if (currentTimestamp != restoreTimestamp.value && currentTimestamp > 0) {
                restoreTimestamp.value = currentTimestamp
                Log.d("DashboardScreen", "检测到数据库恢复，刷新统计数据")
                dashboardViewModel.refresh()
            }
        }
    }
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    val warehouses by warehouseViewModel.topLevelWarehouses.collectAsState(initial = emptyList())
    val allWarehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
    val itemOperationState by itemViewModel.operationState.collectAsState()
    val shoppingOperationState by shoppingItemViewModel.operationState.collectAsState()
    val warehouseOperationState by warehouseViewModel.operationState.collectAsState()
    val syncState by SyncStateManager.syncState.collectAsState()
    
    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 监听同步状态，更新下拉刷新状态
    LaunchedEffect(syncState) {
        when (syncState) {
            is OperationState.Syncing -> {
                isRefreshing = true
            }
            is OperationState.Success, is OperationState.Error, is OperationState.Idle -> {
                if (isRefreshing) {
                    // 同步完成，延迟一点后关闭刷新状态并刷新数据
                    kotlinx.coroutines.delay(500)
                    isRefreshing = false
                    dashboardViewModel.refresh()
                }
            }
            else -> {
                // 其他状态不做处理
            }
        }
    }
    
    // 合并操作状态（优先级：同步成功/失败 > 购物车操作 > 容器操作 > 物品操作）
    // 注意：同步中（Syncing）状态不显示在底部指示器中，只通过下拉刷新图标显示
    val operationState = remember(shoppingOperationState, warehouseOperationState, itemOperationState, syncState) {
        when {
            // 只显示同步成功或失败，不显示同步中
            syncState is OperationState.Success || syncState is OperationState.Error -> syncState
            shoppingOperationState !is OperationState.Idle -> shoppingOperationState
            warehouseOperationState !is OperationState.Idle -> warehouseOperationState
            else -> itemOperationState
        }
    }
    
    val alertSettingsManager = remember { AlertSettingsManager(context) }
    
    // 首页布局风格状态
    val homeLayoutStyleState = rememberHomeLayoutStyle()
    var homeLayoutStyle by homeLayoutStyleState

    // 容器物品数量映射（包含所有容器，不仅仅是顶层容器）
    var warehouseItemCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    
    // 计算每个容器的物品数量（包含所有容器，不仅仅是顶层容器）
    LaunchedEffect(allWarehouses, items) {
        val counts = allWarehouses.associate { warehouse ->
            warehouse.id to items.count { it.warehouseId == warehouse.id }
        }
        warehouseItemCounts = counts
    }
    
    // 选中的容器ID状态（null表示显示首页）
    var selectedWarehouseId by remember { 
        mutableStateOf<Long?>(initialSelectedWarehouseId) // 默认为null，显示首页
    }
    
    // 同步外部传入的 initialSelectedWarehouseId 到内部状态
    // 注意：需要同时监听 initialSelectedWarehouseId 和 homeLayoutStyle，确保 Discord 风格下正确同步
    LaunchedEffect(initialSelectedWarehouseId, homeLayoutStyle) {
        // 总是同步，包括 null 值，确保返回时能正确恢复状态
        selectedWarehouseId = initialSelectedWarehouseId
    }
    
    // 当内部 selectedWarehouseId 变化时，通知外部
    LaunchedEffect(selectedWarehouseId) {
        onSelectedWarehouseIdChanged(selectedWarehouseId)
    }
    
    // 当容器列表变化时，如果选中的容器已被删除，返回首页
    LaunchedEffect(allWarehouses, selectedWarehouseId) {
        // 仓库列表加载完成且不为空时才进行校验，避免列表为空导致误清空选中状态
        if (selectedWarehouseId != null && allWarehouses.isNotEmpty() && allWarehouses.none { it.id == selectedWarehouseId }) {
            selectedWarehouseId = null // 返回首页
        }
    }

    // Discord 风格下按系统返回键：若在子容器则返回父容器，否则回到首页
    if (homeLayoutStyle == HomeLayoutStyle.DISCORD) {
        val currentWarehouse = allWarehouses.find { it.id == selectedWarehouseId }
        val parentId = currentWarehouse?.parentId
        BackHandler(enabled = selectedWarehouseId != null) {
            if (parentId != null) {
                selectedWarehouseId = parentId
            } else {
                selectedWarehouseId = null
            }
        }
    }
    
    // 搜索框显示状态（需要在 Scaffold 之前定义，以便在 topBar 中使用）
    var showSearchBox by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()
    
    // 快速添加物品相关状态
    var showWarehouseSelectionForQuickAdd by remember { mutableStateOf(false) }
    var selectedWarehouseForQuickAdd by remember { mutableStateOf<Warehouse?>(null) }
    var showCameraForQuickAdd by remember { mutableStateOf(false) }
    var showCropDialogForQuickAdd by remember { mutableStateOf(false) }
    var bitmapToCropForQuickAdd by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var capturedImagePathForQuickAdd by remember { mutableStateOf<String?>(null) }
    
    // 获取密度和卡片尺寸
    val density = LocalDensity.current
    val cardWidthPx = remember { with(density) { 400.dp.toPx().toInt() } }
    val cardHeightPx = remember { with(density) { 200.dp.toPx().toInt() } }
    
    Scaffold(
        topBar = {
            val appSettings = LocalAppSettings.current
            GradientTopAppBar(
                title = { 
                    Text(appSettings.appName)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, stringResource(R.string.settings))
                    }
                },
                actions = {
                    // 风格切换按钮
                    IconButton(
                        onClick = {
                            homeLayoutStyle = if (homeLayoutStyle == HomeLayoutStyle.DISCORD) {
                                HomeLayoutStyle.CLASSIC
                            } else {
                                HomeLayoutStyle.DISCORD
                            }
                            saveHomeLayoutStyle(context, homeLayoutStyle)
                        }
                    ) {
                        Icon(
                            if (homeLayoutStyle == HomeLayoutStyle.DISCORD) {
                                Icons.Default.ViewModule // 切换到经典风格图标
                            } else {
                                Icons.Default.ViewColumn // 切换到Discord风格图标
                            },
                            contentDescription = stringResource(R.string.switch_layout_style)
                        )
                    }
                    // 拍照快速添加物品按钮（替换搜索按钮）
                    IconButton(
                        onClick = {
                            // 显示容器选择弹窗
                            showWarehouseSelectionForQuickAdd = true
                        }
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.quick_add_item),
                            tint = ColorHelpers.getContrastColor(MaterialTheme.colorScheme.primary)
                        )
                    }
                    // 首页：显示扫码按钮
                        IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Default.QrCodeScanner, stringResource(R.string.barcode_scanner))
                        }
                }
            )
        },
        floatingActionButton = {
            // 检测是否在购物列表页面（Discord 风格且显示购物列表）
            val isShoppingListVisible = remember(homeLayoutStyle, selectedWarehouseId) {
                homeLayoutStyle == HomeLayoutStyle.DISCORD && selectedWarehouseId == null
            }
            
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)
            ) {
                // Discord 风格下，使用与左侧圆形容器图标一致的背景色；否则保持原有 FAB 颜色
                val fabBackground = if (homeLayoutStyle == HomeLayoutStyle.DISCORD) {
                    ColorHelpers.getGroup2SettingsBtnColor()
                } else {
                    ColorHelpers.getGroup5FabColor()
                }
                val fabIconColor = ColorHelpers.getContrastColor(fabBackground)

                FloatingActionButton(
                    onClick = {
                        // 如果当前在购物列表页面，设置标记以便 ItemEditScreen 知道需要添加到购物篮
                        if (isShoppingListVisible) {
                            val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("add_to_shopping_list_after_save", true).apply()
                        }
                        // 如果当前选中了容器，则带入容器ID
                        onAddItem(selectedWarehouseId)
                    },
                    containerColor = fabBackground,
                    contentColor = fabIconColor,
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_item),
                        tint = fabIconColor
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // 不使用系统 insets，手动控制 padding
    ) { paddingValues ->
        // 获取待购物品数量
        val shoppingItems by shoppingItemViewModel.shoppingItems.collectAsState(initial = emptyList())
        val activeShoppingItemsCount = remember(shoppingItems) {
            shoppingItems.count { !it.isCompleted }
        }
        
        // 下拉刷新状态
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                // 检查是否配置了云端同步
                val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                val serverUrl = prefs.getString("nextcloud_server_url", "") ?: ""
                val username = prefs.getString("nextcloud_username", "") ?: ""
                val password = prefs.getString("nextcloud_password", "") ?: ""
                
                if (serverUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    // 触发手动同步
                    Log.d("DashboardScreen", "手动触发云端同步")
                    isRefreshing = true
                    com.example.itemremindertool.utils.CloudSyncScheduler.syncNow(context)
                } else {
                    // 没有配置云端同步，只刷新本地数据
                    Log.d("DashboardScreen", "未配置云端同步，仅刷新本地数据")
                    isRefreshing = true
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        dashboardViewModel.refresh()
                        isRefreshing = false
                    }
                }
            }
        )
        
        Box(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // 根据风格切换显示不同的布局
            when (homeLayoutStyle) {
                HomeLayoutStyle.DISCORD -> {
                    // Discord风格主布局（添加顶部padding为搜索框留出空间）
                    // 生成二维码对话框状态
                    var showQRCodeDialog by remember { mutableStateOf(false) }
                    var selectedWarehouseForQRCode by remember { mutableStateOf<Warehouse?>(null) }
                    
                    DiscordStyleMainLayout(
                        warehouses = warehouses,
                        allWarehouses = allWarehouses,
                        allItems = items,
                        warehouseItemCounts = warehouseItemCounts,
                        selectedWarehouseId = selectedWarehouseId,
                        shoppingItemsCount = activeShoppingItemsCount,
                        alertSettingsManager = alertSettingsManager,
                        shoppingItemViewModel = shoppingItemViewModel,
                        itemViewModel = itemViewModel,
                        itemReminderViewModel = itemReminderViewModel,
                        activityEventViewModel = activityEventViewModel,
                        warehouseViewModel = warehouseViewModel,
                        searchQuery = searchQuery, // 传递搜索查询
                        onWarehouseSelect = { warehouse ->
                            selectedWarehouseId = warehouse.id
                        },
                        onHomeClick = {
                            // 点击首页图标，取消容器选中，显示统计和提醒
                            selectedWarehouseId = null
                        },
                        onSubWarehouseClick = { subWarehouse ->
                            // 点击子容器，切换到该子容器显示其物品
                            selectedWarehouseId = subWarehouse.id
                        },
                        onAddWarehouse = onAddWarehouse,
                        onAddChildWarehouse = { parentId ->
                            onAddChildWarehouse(parentId)
                        },
                        onEditWarehouse = onEditWarehouse,
                        onDeleteWarehouse = onDeleteWarehouse,
                        onGenerateQRCode = { warehouse ->
                            selectedWarehouseForQRCode = warehouse
                            showQRCodeDialog = true
                        },
                        onEditItem = onEditItem,
                        onViewItem = onViewItem,
                        onDeleteItem = { item ->
                            itemViewModel.deleteItem(item)
                        },
                        onAddAlert = onAddAlert,
                        pullRefreshState = pullRefreshState,
                        useCircleIcon = discordIconCircle,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (showSearchBox) 80.dp else 0.dp) // 仅在搜索框显示时为搜索框留出空间
                    )
                    
                    // 显示二维码对话框
                    if (showQRCodeDialog && selectedWarehouseForQRCode != null) {
                        WarehouseQRCodeDialog(
                            warehouse = selectedWarehouseForQRCode!!,
                            onDismiss = {
                                showQRCodeDialog = false
                                selectedWarehouseForQRCode = null
                            }
                        )
                    }
                }
                HomeLayoutStyle.CLASSIC -> {
                    // 经典风格布局（卡片式）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
            NewHomeScreenContent(
                items = items,
                warehouses = warehouses,
                allWarehouses = allWarehouses,
                warehouseItemCounts = warehouseItemCounts,
                stats = stats,
                shoppingItemViewModel = shoppingItemViewModel,
                accessHistoryManager = accessHistoryManager,
                alertSettingsManager = alertSettingsManager,
                onNavigateToItems = onNavigateToItems,
                onNavigateToShoppingList = onNavigateToShoppingList,
                onNavigateToWarehouse = onNavigateToWarehouseItemsTab,
                onEditItem = onEditItem,
                searchQuery = searchQuery,
                isSearching = isSearching,
                showSearchBox = showSearchBox, // 传递搜索框显示状态
                        modifier = Modifier.fillMaxSize()
                    )
                    }
                }
            }
            
            // 搜索框浮动在顶部（毛玻璃效果）- 仅在showSearchBox为true时显示
            if (showSearchBox) {
                SearchBoxSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCloseSearch = { 
                        searchQuery = ""
                        // 不再隐藏搜索框，只清空搜索查询
                    }
            )
            }
            
            // 底部状态指示器
            BottomOperationStatusIndicator(
                operationState = operationState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            
            // 下拉刷新指示器
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // 首次使用引导覆盖层
            if (showOnboarding) {
                OnboardingOverlay(
                    currentStep = currentOnboardingStep,
                    onNext = {
                        currentOnboardingStep = when (currentOnboardingStep) {
                            OnboardingStep.WELCOME -> OnboardingStep.HOME_AREA
                            OnboardingStep.HOME_AREA -> OnboardingStep.ADD_WAREHOUSE
                            OnboardingStep.ADD_WAREHOUSE -> OnboardingStep.ADD_ITEM
                            OnboardingStep.ADD_ITEM -> OnboardingStep.SETTINGS
                            OnboardingStep.SETTINGS -> OnboardingStep.COMPLETE
                            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
                        }
                    },
                    onSkip = {
                        // 跳过引导，标记为已完成
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                        hasCompletedOnboarding.value = true
                        showOnboarding = false
                    },
                    onComplete = {
                        // 完成引导，标记为已完成
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                        hasCompletedOnboarding.value = true
                        showOnboarding = false
                    },
                    highlightedArea = null // 可以根据步骤设置高亮区域
                )
            }
            
            // 容器选择弹窗（用于快速添加物品）
            if (showWarehouseSelectionForQuickAdd) {
                WarehouseSelectionBottomSheet(
                    warehouses = allWarehouses,
                    onWarehouseSelected = { warehouse ->
                        selectedWarehouseForQuickAdd = warehouse
                        showWarehouseSelectionForQuickAdd = false
                        showCameraForQuickAdd = true
                    },
                    onDismiss = {
                        showWarehouseSelectionForQuickAdd = false
                    }
                )
            }
            
            // 拍照对话框（快速添加物品）
            if (showCameraForQuickAdd && selectedWarehouseForQuickAdd != null) {
                CameraCaptureDialog(
                    onImageCaptured = { imagePath ->
                        showCameraForQuickAdd = false
                        if (imagePath != null) {
                            // 加载图片并显示裁剪对话框
                            scope.launch(Dispatchers.IO) {
                                val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                                if (bitmap != null) {
                                    capturedImagePathForQuickAdd = imagePath
                                    bitmapToCropForQuickAdd = bitmap
                                    showCropDialogForQuickAdd = true
                                }
                            }
                        } else {
                            selectedWarehouseForQuickAdd = null
                        }
                    },
                    onDismiss = {
                        showCameraForQuickAdd = false
                        selectedWarehouseForQuickAdd = null
                    }
                )
            }
            
            // 裁剪对话框（快速添加物品）
            if (showCropDialogForQuickAdd && bitmapToCropForQuickAdd != null && selectedWarehouseForQuickAdd != null) {
                ImageCropDialog(
                    bitmap = bitmapToCropForQuickAdd!!,
                    onCropped = { croppedBitmap ->
                        showCropDialogForQuickAdd = false
                        scope.launch(Dispatchers.IO) {
                            // 保存裁剪后的图片
                            val fileName = "item_${System.currentTimeMillis()}_${System.currentTimeMillis()}.jpg"
                            val savedPath = ImageUtils.saveImageToInternalStorage(
                                context,
                                croppedBitmap,
                                fileName
                            )
                            
                            savedPath?.let { imagePath ->
                                // 为主图创建裁剪版本（与正常添加物品一致）
                                val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                                if (bitmap != null) {
                                    val croppedBitmapForCard = ImageUtils.cropImageToCardSize(
                                        bitmap,
                                        cardWidthPx,
                                        cardHeightPx
                                    )
                                    val croppedPath = ImageUtils.getCroppedImagePath(imagePath)
                                    val croppedFileName = croppedPath?.let { File(it).name } ?: "cropped_${File(imagePath).name}"
                                    ImageUtils.saveImageToInternalStorage(
                                        context,
                                        croppedBitmapForCard,
                                        croppedFileName
                                    )
                                }
                                
                                // 创建临时物品，只保存图片，后续再维护物品信息
                                val newItem = Item(
                                    name = "未命名物品",
                                    description = "",
                                    categoryId = null,
                                    warehouseId = selectedWarehouseForQuickAdd!!.id,
                                    tags = emptyList(),
                                    price = null,
                                    quantity = 1,
                                    barcode = null,
                                    expiryDate = null,
                                    enableStockAlert = false,
                                    imageUri = imagePath,
                                    imageUris = listOf(imagePath),
                                    primaryImageIndex = 0,
                                    featureCode = null,
                                    createdAt = Date(),
                                    updatedAt = Date()
                                )
                                itemViewModel.insertItem(newItem)
                                // 刷新数据
                                dashboardViewModel.refresh()
                            }
                            
                            // 清理状态
                            bitmapToCropForQuickAdd = null
                            capturedImagePathForQuickAdd = null
                            selectedWarehouseForQuickAdd = null
                        }
                    },
                    onDismiss = {
                        showCropDialogForQuickAdd = false
                        bitmapToCropForQuickAdd = null
                        capturedImagePathForQuickAdd = null
                        selectedWarehouseForQuickAdd = null
                    },
                    cardWidth = cardWidthPx,
                    cardHeight = cardHeightPx
                )
            }
        } // 关闭 Box
    } // 关闭 Scaffold 的 content lambda
} // 关闭 DashboardScreen 函数

data class StatCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color,
    val onClick: (() -> Unit)? = null
)

@Composable
fun StatCard(statCard: StatCardData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .then(
                if (statCard.onClick != null) {
                    Modifier.clickable(onClick = statCard.onClick!!)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statCard.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = statCard.icon,
                contentDescription = statCard.title,
                tint = statCard.color,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statCard.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = statCard.color,
                    maxLines = 1
                )
                Text(
                    text = statCard.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statCard.color.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * 容器统计卡片
 */
@Composable
fun WarehouseStatCard(
    warehouse: Warehouse,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var warehouseImageBitmap by remember(warehouse.imageUri) {
        mutableStateOf<Bitmap?>(null)
    }
   
    LaunchedEffect(warehouse.imageUri) {
        warehouseImageBitmap = if (warehouse.imageUri != null) {
            try {
                val file = File(warehouse.imageUri)
                if (file.exists()) {
                    ImageUtils.loadBitmapFromPath(warehouse.imageUri)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
   
    val isImageBright = remember(warehouseImageBitmap) {
        val bitmap = warehouseImageBitmap
        if (bitmap != null) {
            try {
                ImageUtils.calculateImageBrightness(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
   
    val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4TextColor()
    }
   
    val iconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4IconColor()
    }
   
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            // ✅ 始终透明，让内部 Box 控制背景
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 背景图片（底层）
            val bitmap = warehouseImageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 没图片时显示默认背景色
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorHelpers.getGroup3CardBgColor())
                )
            }
            
            // 2. 半透明遮罩（图片时显示）
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
            
            // 3. 内容区域（图标 + 文字，永远在最上层）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = warehouse.name,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.items_count, itemCount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1
                    )
                    Text(
                        text = warehouse.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 容器信息页面
 */
@Composable
fun WarehouseInfoScreen(
    warehouse: Warehouse,
    itemCount: Int,
    childWarehouses: List<Warehouse> = emptyList(),
    warehouseItemCounts: Map<Long, Int> = emptyMap(),
    allWarehouses: List<Warehouse> = emptyList(),
    allItems: List<Item> = emptyList(),
    warehouseViewModel: WarehouseViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    onWarehouseClick: (Long) -> Unit = {},
    onEditWarehouse: () -> Unit = {},
    onEditItem: ((Long) -> Unit)? = null,
    onViewItem: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 递归计算子容器数量（包括所有子容器的子容器）
    val childWarehouseCounts = remember(childWarehouses, allWarehouses) {
        childWarehouses.associate { child ->
            child.id to countAllChildWarehouses(child.id, allWarehouses)
        }
    }
    
    // 递归计算子容器物品数量（包括所有子容器的物品）
    val childItemCounts = remember(childWarehouses, allWarehouses, allItems) {
        childWarehouses.associate { child ->
            child.id to countAllItemsInWarehouse(child.id, allWarehouses, allItems, warehouseItemCounts)
        }
    }
    
    // 加载当前容器的物品
    LaunchedEffect(warehouse.id) {
        warehouseViewModel?.loadWarehouseItems(warehouse.id)
    }
    
    // 安全地获取容器物品状态
    val warehouseItemsState = if (warehouseViewModel != null) {
        val state by warehouseViewModel.uiState.collectAsState()
        state
    } else {
        remember { com.example.itemremindertool.ui.viewmodel.WarehouseUiState() }
    }
    val warehouseItems = warehouseItemsState.warehouseItems
    
    // 移动物品对话框状态
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<Item?>(null) }
    
    // 加载容器图片（检查文件是否存在）
    val warehouseImageBitmap = remember(warehouse.imageUri) {
        if (warehouse.imageUri != null) {
            try {
                val file = File(warehouse.imageUri)
                if (file.exists()) {
                    ImageUtils.loadBitmapFromPath(warehouse.imageUri)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 计算图片亮度（如果有图片）
    val isImageBright = remember(warehouse.imageUri, warehouseImageBitmap) {
        if (warehouse.imageUri != null && warehouseImageBitmap != null) {
            try {
                ImageUtils.calculateImageBrightness(warehouseImageBitmap!!)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 根据图片亮度决定文字和图标颜色
    val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4TextColor()
    }
    
    val iconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4IconColor()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorHelpers.getGroup2PageBgColor())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 整合的容器信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (warehouseImageBitmap != null) Color.Transparent else ColorHelpers.getGroup3CardBgColor()
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // 背景图片
                if (warehouseImageBitmap != null) {
                    Image(
                        bitmap = warehouseImageBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 半透明遮罩，提高文字可读性
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }
                
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 容器图标和名称
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                            tint = iconColor,
                        modifier = Modifier.size(48.dp)
                )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                Text(
                    text = warehouse.name,
                            style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                                color = textColor
                )
                Text(
                    text = stringResource(R.string.items_count, itemCount),
                            style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.7f)
                )
            }
        }
                
                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                        color = textColor.copy(alpha = 0.2f)
                )
        
        // 容器描述
        if (warehouse.description.isNotEmpty()) {
                    Row(
                modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                                tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        Text(
                            stringResource(R.string.description),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                    color = textColor
                        )
                    Text(
                        text = warehouse.description,
                                style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                    )
                }
            }
        }
        
        // 容器位置
        if (warehouse.location.isNotEmpty()) {
                    Row(
                modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                                tint = iconColor,
                            modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.warehouse_location),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                    color = textColor
                        )
                        Text(
                            text = warehouse.location,
                                style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                        )
                }
            }
        }
        
        // 容器容量
        if (warehouse.capacity != null) {
                    Row(
                modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                                tint = iconColor,
                            modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.capacity),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                    color = textColor
                        )
                        Text(
                            text = "${warehouse.capacity}",
                                style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                        )
                        // 显示容量使用情况
                        LinearProgressIndicator(
                            progress = {
                                if (warehouse.capacity > 0) {
                                (itemCount.toFloat() / warehouse.capacity).coerceIn(0f, 1f)
                            } else {
                                0f
                                }
                            },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (warehouseImageBitmap != null && isImageBright != null) {
                                        if (isImageBright) Color.Black else Color.White
                                    } else {
                                        ColorHelpers.getGroup2SettingsBtnColor()
                                    },
                                    trackColor = textColor.copy(alpha = 0.2f)
                        )
                        Text(
                            text = stringResource(
                                R.string.used_capacity,
                                itemCount,
                                warehouse.capacity
                            ),
                            style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.6f)
                        )
                            }
                        }
                    }
                }
            }
        }

        // 子容器横向滚动显示
        if (childWarehouses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.child_containers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorHelpers.getGroup4TextColor(),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // 横向滚动的子容器列表
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                items(childWarehouses, key = { it.id }) { childWarehouse ->
                    val childWarehouseCount = childWarehouseCounts[childWarehouse.id] ?: 0
                    val childItemCount = childItemCounts[childWarehouse.id] ?: 0
                                ChildWarehouseCard(
                                    warehouse = childWarehouse,
                        warehouseCount = childWarehouseCount,
                                    itemCount = childItemCount,
                        onClick = { onWarehouseClick(childWarehouse.id) },
                        modifier = Modifier.width(96.dp)
                                )
                            }
                        }
        }
        
        // 容器物品列表
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.warehouse_items),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ColorHelpers.getGroup4TextColor(),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        if (warehouseItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ColorHelpers.getGroup4IconColor(0.6f)
                    )
                    Text(
                        stringResource(R.string.warehouse_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                }
            }
        } else {
            // 使用 Column 而不是 LazyColumn，因为外层已经有 verticalScroll
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                warehouseItems.forEach { item ->
                    ItemCard(
                        item = item,
                        onClick = {
                            onViewItem?.invoke(item.id)
                        },
                        onEdit = { 
                            onEditItem?.invoke(item.id)
                        },
                        onDelete = { itemViewModel?.deleteItem(item) },
                        onAddToShoppingCart = if (shoppingItemViewModel != null) {
                            {
                                val shoppingItem = com.example.itemremindertool.data.model.ShoppingItem(
                                    name = item.name,
                                    description = "",
                                    quantity = 1,
                                    isCompleted = false,
                                    priority = com.example.itemremindertool.data.model.Priority.MEDIUM,
                                    createdAt = Date(),
                                    imageUri = item.imageUri,
                                    itemId = item.id // 关联物品ID，用于完成购买时补充库存
                                )
                                shoppingItemViewModel.insertShoppingItem(shoppingItem)
                        }
                        } else null,
                        onMoveToContainer = if (warehouseViewModel != null && itemViewModel != null) {
                            {
                                itemToMove = item
                                showMoveDialog = true
                }
                        } else null
                    )
                }
            }
        }
    }
    
    // 移动物品对话框
    if (showMoveDialog && itemToMove != null) {
        warehouseViewModel?.let { vm ->
            MoveItemDialog(
                itemName = itemToMove!!.name,
                currentWarehouseId = itemToMove!!.warehouseId,
                warehouseViewModel = vm,
                onDismiss = {
                    showMoveDialog = false
                    itemToMove = null
                },
                onConfirm = { targetWarehouseId ->
                    val updatedItem = itemToMove!!.copy(
                        warehouseId = targetWarehouseId,
                        updatedAt = Date()
                    )
                    itemViewModel?.updateItem(updatedItem)
                    // 重新加载容器物品列表
                    vm.loadWarehouseItems(warehouse.id)
                    showMoveDialog = false
                    itemToMove = null
                }
            )
        }
    }
}

/**
 * 子容器卡片（横向滚动样式，显示统计信息）
 */
@Composable
fun ChildWarehouseCard(
    warehouse: Warehouse,
    warehouseCount: Int,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用 mutableStateOf 来存储图片，确保能正确触发重新计算
    var warehouseImageBitmap by remember(warehouse.imageUri) { 
        mutableStateOf<android.graphics.Bitmap?>(null) 
    }
    
    // 异步加载图片
    LaunchedEffect(warehouse.imageUri) {
        warehouseImageBitmap = if (warehouse.imageUri != null) {
            try {
                val file = java.io.File(warehouse.imageUri)
                if (file.exists()) {
                    ImageUtils.loadBitmapFromPath(warehouse.imageUri)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 计算图片亮度（如果有图片）
    val isImageBright = remember(warehouseImageBitmap) {
        val bitmap = warehouseImageBitmap
        if (bitmap != null) {
            try {
                ImageUtils.calculateImageBrightness(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 根据图片亮度决定文字和图标颜色
    val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4TextColor()
    }
    
    val iconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4IconColor()
    }
    
    val primaryIconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) MaterialTheme.colorScheme.primary else Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = modifier
            .width(96.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (warehouseImageBitmap != null) Color.Transparent else ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            val bitmap = warehouseImageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 半透明遮罩，提高文字可读性
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = textColor
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
                // 统计信息（有图片时不显示图标，只显示数字）
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
            Text(
                        text = "$warehouseCount",
                        fontSize = 10.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                    )
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                                tint = primaryIconColor
                    )
                        }
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                    )
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                                tint = iconColor
                    )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 递归计算所有子容器数量（包括子容器的子容器）
 */
fun countAllChildWarehouses(warehouseId: Long, allWarehouses: List<Warehouse>): Int {
    val directChildren = allWarehouses.filter { it.parentId == warehouseId }
    var count = directChildren.size
    directChildren.forEach { child ->
        count += countAllChildWarehouses(child.id, allWarehouses)
    }
    return count
}

/**
 * 递归计算容器及其所有子容器中的物品数量
 */
fun countAllItemsInWarehouse(
    warehouseId: Long,
    allWarehouses: List<Warehouse>,
    allItems: List<Item>,
    warehouseItemCounts: Map<Long, Int>
): Int {
    // 当前容器的物品数量
    var count = warehouseItemCounts[warehouseId] ?: 0
    
    // 递归计算所有子容器的物品数量
    val directChildren = allWarehouses.filter { it.parentId == warehouseId }
    directChildren.forEach { child ->
        count += countAllItemsInWarehouse(child.id, allWarehouses, allItems, warehouseItemCounts)
    }
    
    return count
}

/**
 * 新的首页内容布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreenContent(
    items: List<Item>,
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    stats: com.example.itemremindertool.ui.viewmodel.DashboardStats,
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    alertSettingsManager: AlertSettingsManager,
    onNavigateToItems: (String?) -> Unit, // 接收筛选类型参数
    onNavigateToShoppingList: () -> Unit,
    onNavigateToWarehouse: (Long) -> Unit,
    onEditItem: (Long) -> Unit,
    searchQuery: String = "",
    isSearching: Boolean = false,
    showSearchBox: Boolean = false, // 搜索框显示状态
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 获取提醒设置
    val expiryReminderDays = remember { alertSettingsManager.getExpiryReminderDays() }
    val lowStockThreshold = remember { alertSettingsManager.getLowStockThreshold() }
    
    // 搜索过滤（支持标签搜索）
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            items.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                (item.barcode?.contains(searchQuery, ignoreCase = true) == true) ||
                item.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }
    }
    
    // 根据设置计算到期提醒物品
    val calendar = Calendar.getInstance()
    val currentTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, expiryReminderDays)
    val reminderEndTime = calendar.timeInMillis
    
    val expiringItems = remember(items, currentTime, reminderEndTime, expiryReminderDays) {
        items.filter { item ->
            item.expiryDate != null && 
            item.expiryDate.time >= currentTime && 
            item.expiryDate.time <= reminderEndTime
        }
    }
    
    // 根据设置计算库存不足的物品（只包含启用了库存提醒的物品）
    val lowStockItems = remember(items, lowStockThreshold) {
        items.filter { item ->
            item.enableStockAlert && item.quantity <= lowStockThreshold
        }
    }
    
    val alertCount = expiringItems.size + lowStockItems.size
    
    // 格式化日期 - 使用系统默认格式
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
    val todayStr = remember { dateFormat.format(Date()) }
    val greeting = stringResource(R.string.greeting_today, todayStr)
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 内容列表（增加底部 padding 为固定广告预留空间 140dp + 间距 16dp）
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (showSearchBox) 80.dp else 0.dp,
                // 为底部广告（60dp）和额外间距预留空间，避免出现大片空白
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 60.dp + 16.dp
            )
        ) {
            // 搜索模式下显示搜索结果
            if (isSearching) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                if (filteredItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    stringResource(R.string.no_search_results),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.search_results, filteredItems.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onEdit = { onEditItem(item.id) },
                            onDelete = { /* 搜索模式下不提供删除功能 */ },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                // 正常首页内容
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorHelpers.getGroup4TextColor(),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }

                if (alertCount > 0) {
                    item {
                        val filterTypeAllAlerts = stringResource(R.string.filter_type_all_alerts)
                        AlertCard(
                            count = alertCount,
                            expiringCount = expiringItems.size,
                            lowStockCount = lowStockItems.size,
                            expiryReminderDays = expiryReminderDays,
                            onClick = { onNavigateToItems(filterTypeAllAlerts) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                item {
                    QuickAccessSection(
                        shoppingItemCount = stats.activeShoppingItems,
                        onNavigateToShoppingList = onNavigateToShoppingList
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    RecentlyOpenedSection(
                        allWarehouses = allWarehouses,
                        warehouseItemCounts = warehouseItemCounts,
                        items = items,
                        accessHistoryManager = accessHistoryManager,
                        onNavigateToWarehouse = onNavigateToWarehouse
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    AllLocationsSection(
                        warehouses = warehouses,
                        allWarehouses = allWarehouses,
                        items = items,
                        warehouseItemCounts = warehouseItemCounts,
                        onNavigateToWarehouse = onNavigateToWarehouse
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        // 固定底部广告（不随列表滚动）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 8.dp)
        ) {
            DynamicBannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

/**
 * 搜索框部分
 */
@Composable
fun SearchBoxSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    // 搜索框外层完全透明，只有输入框是毛玻璃效果
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    stringResource(R.string.search_all_items),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ) 
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ) 
            },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = onCloseSearch) {
                        Icon(
                            Icons.Default.Close, 
                            null, 
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorHelpers.getGroup3CardBgColor().copy(alpha = 1f),
                unfocusedContainerColor = ColorHelpers.getGroup3CardBgColor().copy(alpha = 0.9f),
                focusedTextColor = ColorHelpers.getGroup4TextColor(),
                unfocusedTextColor = ColorHelpers.getGroup4TextColor(),
                focusedBorderColor = ColorHelpers.getGroup4TextColor().copy(alpha = 0.8f),
                unfocusedBorderColor = ColorHelpers.getGroup4TextColor().copy(alpha = 0.5f),
                focusedPlaceholderColor = ColorHelpers.getGroup4TextColor(0.5f),
                unfocusedPlaceholderColor = ColorHelpers.getGroup4TextColor(0.5f),
                focusedLeadingIconColor = ColorHelpers.getGroup4IconColor(0.7f),
                unfocusedLeadingIconColor = ColorHelpers.getGroup4IconColor(0.6f),
                focusedTrailingIconColor = ColorHelpers.getGroup4IconColor(0.7f),
                unfocusedTrailingIconColor = ColorHelpers.getGroup4IconColor(0.6f)
        ),
        singleLine = true
    )
    }
}

/**
 * 提醒卡片
 */
@Composable
fun AlertCard(
    count: Int,
    expiringCount: Int,
    lowStockCount: Int,
    expiryReminderDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expiringCount > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                ColorHelpers.getGroup5AlertCardColor()
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)  // 设置阴影
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.expiring_soon_alert, expiryReminderDays, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (expiringCount > 0) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 快捷入口部分（只显示购物篮）
 */
@Composable
fun QuickAccessSection(
    shoppingItemCount: Int,
    onNavigateToShoppingList: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ColorHelpers.getGroup4TextColor(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            // 待购清单
            item {
                CommonEntryCard(
                    title = stringResource(R.string.shopping_list_entry),
                    count = shoppingItemCount,
                    onClick = onNavigateToShoppingList
                )
            }
        }
    }
}

/**
 * 常用入口卡片（正方形）
 */
@Composable
fun CommonEntryCard(
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                color = ColorHelpers.getGroup4TextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.8.dp))
            Text(
                text = stringResource(R.string.items_count_format, count),
                fontSize = 10.8.sp,
                color = ColorHelpers.getGroup4TextColor(0.6f)
            )
        }
    }
}

/**
 * 最近打开部分
 */
@Composable
fun RecentlyOpenedSection(
    allWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    items: List<Item>,
    accessHistoryManager: com.example.itemremindertool.data.AccessHistoryManager,
    onNavigateToWarehouse: (Long) -> Unit
) {
    // 获取最近访问的容器ID列表（按时间降序，最新的在前）
    val recentAccessHistory by accessHistoryManager.accessHistory.collectAsState()
    val recentWarehouses = remember(recentAccessHistory, allWarehouses) {
        recentAccessHistory
            .mapNotNull { access ->
                allWarehouses.find { it.id == access.warehouseId }
            }
            .take(10) // 最多显示10个
    }
    
    // 创建 LazyRow 的滚动状态
    val lazyListState = rememberLazyListState()
    
    // 当列表的第一个元素改变时（即最新打开的容器改变），重置滚动位置到开始
    val firstWarehouseId = remember(recentWarehouses) {
        recentWarehouses.firstOrNull()?.id
    }
    
    LaunchedEffect(firstWarehouseId) {
        // 当第一个容器改变时，重置滚动位置到开始（确保列表左对齐）
        if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
            lazyListState.animateScrollToItem(0, scrollOffset = 0)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.recently_opened),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ColorHelpers.getGroup4TextColor(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (recentWarehouses.isNotEmpty()) {
            // 横向滚动的正方形卡片（最新的在左边第一个）
            LazyRow(
                state = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(recentWarehouses, key = { it.id }) { warehouse ->
                    val itemCount = countAllItemsInWarehouse(
                        warehouse.id,
                        allWarehouses,
                        items,
                        warehouseItemCounts
                    )
                    val childWarehouseCount = countAllChildWarehouses(warehouse.id, allWarehouses)
                    RecentlyOpenedItem(
                        warehouse = warehouse,
                        itemCount = itemCount,
                        childWarehouseCount = childWarehouseCount,
                        onClick = { onNavigateToWarehouse(warehouse.id) }
                    )
                }
            }
            
        } else {
            // 空状态占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_recently_opened),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
            }
        }
    }
}

/**
 * 最近打开项（正方形卡片）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyOpenedItem(
    warehouse: Warehouse,
    itemCount: Int,
    childWarehouseCount: Int,
    onClick: () -> Unit
) {
    // 使用 mutableStateOf 来存储图片，确保能正确触发重新计算
    var warehouseImageBitmap by remember(warehouse.imageUri) { 
        mutableStateOf<android.graphics.Bitmap?>(null) 
    }
    
    // 异步加载图片
    LaunchedEffect(warehouse.imageUri) {
        warehouseImageBitmap = if (warehouse.imageUri != null) {
            try {
                val file = java.io.File(warehouse.imageUri)
                if (file.exists()) {
                    ImageUtils.loadBitmapFromPath(warehouse.imageUri)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 计算图片亮度（如果有图片）
    val isImageBright = remember(warehouseImageBitmap) {
        val bitmap = warehouseImageBitmap
        if (bitmap != null) {
            try {
                ImageUtils.calculateImageBrightness(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 根据图片亮度决定文字和图标颜色
    val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4TextColor()
    }
    
    val iconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4IconColor()
    }
    
    val primaryIconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) MaterialTheme.colorScheme.primary else Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (warehouseImageBitmap != null) Color.Transparent else ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            val bitmap = warehouseImageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 半透明遮罩，提高文字可读性
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = textColor
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
                // 统计信息（有图片时不显示图标，只显示数字）
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$childWarehouseCount",
                        fontSize = 10.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                    )
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                                tint = primaryIconColor
                    )
                        }
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                    )
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                                tint = iconColor
                    )
                        }
                    }
    }
}
        }
    }
}

/**
 * 所有容器部分
 */
@Composable
fun AllLocationsSection(
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    items: List<Item>,
    warehouseItemCounts: Map<Long, Int>,
    onNavigateToWarehouse: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.all_locations),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 使用横向滚动显示正方形卡片（与最近打开的卡片保持一致）
        if (warehouses.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(warehouses, key = { it.id }) { warehouse ->
                    val itemCount = countAllItemsInWarehouse(
                        warehouse.id,
                        allWarehouses,
                        items,
                        warehouseItemCounts
                    )
                    val childWarehouseCount = countAllChildWarehouses(warehouse.id, allWarehouses)
                    LocationCard(
                        warehouse = warehouse,
                        itemCount = itemCount,
                        childWarehouseCount = childWarehouseCount,
                        onClick = { onNavigateToWarehouse(warehouse.id) }
                    )
                }
            }
        }
    }
}

/**
 * 容器卡片（正方形）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationCard(
    warehouse: Warehouse,
    itemCount: Int,
    childWarehouseCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用 mutableStateOf 来存储图片，确保能正确触发重新计算
    var warehouseImageBitmap by remember(warehouse.imageUri) { 
        mutableStateOf<android.graphics.Bitmap?>(null) 
    }
    
    // 异步加载图片
    LaunchedEffect(warehouse.imageUri) {
        warehouseImageBitmap = if (warehouse.imageUri != null) {
            try {
                val file = java.io.File(warehouse.imageUri)
                if (file.exists()) {
                    ImageUtils.loadBitmapFromPath(warehouse.imageUri)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 计算图片亮度（如果有图片）
    val isImageBright = remember(warehouseImageBitmap) {
        val bitmap = warehouseImageBitmap
        if (bitmap != null) {
            try {
                ImageUtils.calculateImageBrightness(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    // 根据图片亮度决定文字和图标颜色
    val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4TextColor()
    }
    
    val iconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) Color.Black else Color.White
    } else {
        ColorHelpers.getGroup4IconColor()
    }
    
    val primaryIconColor = if (warehouseImageBitmap != null && isImageBright != null) {
        if (isImageBright) MaterialTheme.colorScheme.primary else Color.White
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .then(modifier)
            .width(96.dp)
            .aspectRatio(1f), // 正方形
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (warehouseImageBitmap != null) Color.Transparent else ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp,
            focusedElevation = 7.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            val bitmap = warehouseImageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 半透明遮罩，提高文字可读性
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 容器名称（居中）
            ScrollingText(
                text = warehouse.name,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = textColor
            )
            
            Spacer(modifier = Modifier.height(3.6.dp))
            
                // 统计信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 子容器数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$childWarehouseCount",
                        fontSize = 10.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                    )
                        
                        
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                                tint = primaryIconColor
                    )
                        }
                        
                }
                
                // 物品数量统计
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemCount",
                        fontSize = 10.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                    )
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(14.4.dp),
                                tint = iconColor
                            )
                        }
                        // 有图片时不显示图标
                        if (warehouseImageBitmap == null) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(14.4.dp),
                                tint = iconColor
                            )
                        }
                    }
                }
            }
        }
    }
                }
                
// ============================================================================
// Discord风格新布局组件
// ============================================================================

/**
 * 容器信息底部弹出对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseInfoBottomSheet(
    warehouse: Warehouse?,
    allWarehouses: List<Warehouse>,
    allItems: List<Item>,
    warehouseItemCounts: Map<Long, Int>,
    onDismiss: () -> Unit,
    onNavigateToWarehouse: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (warehouse == null) return
    
    // 获取子容器
    val childWarehouses = remember(warehouse.id, allWarehouses) {
        allWarehouses.filter { it.parentId == warehouse.id }
    }
    
    val itemCount = warehouseItemCounts[warehouse.id] ?: 0
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = ColorHelpers.getGroup2PageBgColor(),
        contentColor = ColorHelpers.getGroup4TextColor(),
        dragHandle = {
            // 添加拖动手柄，方便用户知道可以拖动
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(vertical = 12.dp)
                    .background(
                        ColorHelpers.getGroup4IconColor(0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        // 使用 BoxWithConstraints 来获取可用高度，设置更大的高度限制
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            // maxHeight 已经是 Dp 类型，使用 value 属性进行计算
            // 使用屏幕高度的90%，但限制在600dp到900dp之间
            val contentMaxHeight = (maxHeight.value * 0.9f).dp.coerceIn(600.dp, 900.dp)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = contentMaxHeight)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 标题栏（容器名称居中显示）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor()
                )
            }
            
            HorizontalDivider(
                color = ColorHelpers.getGroup4TextColor(0.2f)
            )
            
            // 容器描述
            if (warehouse.description.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.description),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                    Text(
                        text = warehouse.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
            }
            
            // 容器位置
            if (warehouse.location.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.warehouse_location),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                    Text(
                        text = warehouse.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                }
            }
            
            // 容器容量
            if (warehouse.capacity != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.capacity),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                    Text(
                        text = "${warehouse.capacity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (warehouse.capacity > 0) {
                                (itemCount.toFloat() / warehouse.capacity).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = ColorHelpers.getGroup2SettingsBtnColor(),
                        trackColor = ColorHelpers.getGroup4TextColor(0.2f)
                    )
                    Text(
                        text = stringResource(
                            R.string.used_capacity,
                            itemCount,
                            warehouse.capacity
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                }
            }
            
            // 统计信息
            HorizontalDivider(
                color = ColorHelpers.getGroup4TextColor(0.2f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = childWarehouses.size.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                    Text(
                        text = stringResource(R.string.child_containers),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = itemCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorHelpers.getGroup4TextColor()
                    )
                    Text(
                        text = stringResource(R.string.warehouse_items),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                }
            }
            }
        }
    }
}

/**
 * 左侧容器图标列表项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WarehouseIconItem(
    warehouse: Warehouse,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    onViewInfo: ((Warehouse) -> Unit)? = null, // 新增：查看信息回调
    onGenerateQRCode: ((Warehouse) -> Unit)? = null, // 新增：生成二维码回调
    warehouseViewModel: WarehouseViewModel? = null, // 用于获取删除统计信息
    useCircleIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    // 外层容器，用于显示选中指示器
    Box(
        modifier = modifier
            .fillMaxWidth() // 确保容器占满宽度，让指示器能正确显示
    ) {
        // 选中指示器（左侧竖条）
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp) // 与图标保持一定距离
                    .width(3.dp)
                    .height(24.dp)
                    .background(
                        ColorHelpers.getGroup2SettingsBtnColor(), // 与按钮颜色一致
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }
        
        // 图标圆形容器（居中显示）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // 加载容器图片（检查文件是否存在）
            val warehouseImageBitmap = remember(warehouse.imageUri) {
                if (warehouse.imageUri != null) {
                    try {
                        val file = java.io.File(warehouse.imageUri)
                        if (file.exists()) {
                            ImageUtils.loadBitmapFromPath(warehouse.imageUri)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }
            }
            
            // 计算图片亮度（如果有图片）
            val isImageBright = remember(warehouse.imageUri, warehouseImageBitmap) {
                if (warehouse.imageUri != null && warehouseImageBitmap != null) {
                    try {
                        ImageUtils.calculateImageBrightness(warehouseImageBitmap!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }
            }
            
            val backgroundColor = if (warehouseImageBitmap != null) {
                Color.Transparent
            } else {
                ColorHelpers.getGroup2SettingsBtnColor()
            }
            val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
                if (isImageBright) Color.Black else Color.White
            } else {
                ColorHelpers.getContrastColor(ColorHelpers.getGroup2SettingsBtnColor())
            }
            
            // 优化的点击检测：立即响应单击，延迟检测双击
            val scope = rememberCoroutineScope()
            var pendingClick by remember { mutableStateOf(false) }
            var clickJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp) // 略增尺寸
            .clip(if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp))
                        .then(
                            if (warehouseImageBitmap != null) {
                                Modifier
                            } else {
                                Modifier.background(backgroundColor) // 与右侧容器按钮一致
                            }
                        )
                        .pointerInput(warehouse.id) {
                            detectTapGestures(
                                onTap = {
                                    // 取消之前的延迟任务
                                    clickJob?.cancel()

                                    // 如果已经有待处理的单击，说明是双击
                                    if (pendingClick) {
                                        pendingClick = false
                                        onViewInfo?.invoke(warehouse)
                                    } else {
                                        // 立即执行单击
                                        onClick()
                                        // 设置待处理标志，延迟检测双击
                                        pendingClick = true
                                        clickJob = scope.launch {
                                            delay(300) // 300ms内检测双击
                                            if (pendingClick) {
                                                pendingClick = false
                                            }
                                        }
                                    }
                                },
                                onLongPress = { showMenu = true }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (warehouseImageBitmap != null) {
                        // 显示容器图片（有图片时不显示文字）
                        Image(
                            bitmap = warehouseImageBitmap.asImageBitmap(),
                            contentDescription = warehouse.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 显示容器首字母或图标（无图片时显示文字）
                    Text(
                        text = warehouse.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall, // 与右侧子容器字体大小一致
                        fontWeight = FontWeight.Bold,
                        color = textColor // 根据背景颜色对比度自动切换
                    )
                    }
                }
                
                // 长按菜单 - 添加背景层以支持点击外部关闭
                if (showMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showMenu = false }
                            .zIndex(1f)
                    )
                }
                DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .width(160.dp)
                .zIndex(2f),
            shape = RoundedCornerShape(6.dp),
            containerColor = ColorHelpers.getGroup3CardBgColor(),
            tonalElevation = 8.dp
        ) {
            // 编辑
                    DropdownMenuItem(
                text = { 
                    Text(
                        stringResource(R.string.edit),
                        fontSize = 14.sp,
                        color = ColorHelpers.getGroup4TextColor(),
                        maxLines = 2 // 允许最多2行，支持文字换行
                    ) 
                },
                        onClick = {
                            showMenu = false
                            onEditWarehouse(warehouse)
                        },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Edit, 
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = ColorHelpers.getGroup4IconColor()
                    ) 
                },
                modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                colors = MenuDefaults.itemColors(
                    textColor = ColorHelpers.getGroup4TextColor(),
                    leadingIconColor = ColorHelpers.getGroup4IconColor()
                )
            )
            
            // 生成二维码
            if (onGenerateQRCode != null) {
                DropdownMenuItem(
                    text = { 
                        Text(
                            stringResource(R.string.generate_qr_code),
                            fontSize = 14.sp,
                            color = ColorHelpers.getGroup4TextColor(),
                            maxLines = 2 // 允许最多2行，支持文字换行
                        ) 
                    },
                    onClick = {
                        showMenu = false
                        onGenerateQRCode(warehouse)
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.QrCode, 
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = ColorHelpers.getGroup4IconColor()
                        ) 
                    },
                    modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                    colors = MenuDefaults.itemColors(
                        textColor = ColorHelpers.getGroup4TextColor(),
                        leadingIconColor = ColorHelpers.getGroup4IconColor()
                    )
                )
            }
            
            // 分隔线
            //HorizontalDivider(
            //    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            //    color = ColorHelpers.getGroup4TextColor().copy(alpha = 0.1f),
            //    thickness = 0.5.dp
            //)
            
            // 删除（红色）
                    DropdownMenuItem(
                text = { 
                    Text(
                        stringResource(R.string.delete),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2 // 允许最多2行，支持文字换行
                    ) 
                },
                        onClick = {
                            showMenu = false
                    // 显示删除确认对话框
                    showDeleteConfirmDialog = true
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Delete, 
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    ) 
                },
                modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                )
            )
        }
        }
        }
        
        // 删除确认对话框（简化版，只显示风险提示）
        if (showDeleteConfirmDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorHelpers.getGroup3CardBgColor()
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 警告图标
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 标题
                        Text(
                            text = stringResource(R.string.confirm_delete),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 容器名称
                        Text(
                            text = "\"${warehouse.name}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 风险提示（简化版）
                        Text(
                            text = stringResource(R.string.delete_warehouse_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.8f),
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 取消按钮
                            TextButton(
                                onClick = { showDeleteConfirmDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "取消",
                                    fontSize = 14.sp,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                            }
                            
                            // 确认删除按钮
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                            onDeleteWarehouse(warehouse)
                        },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(
                                    "删除",
                                    fontSize = 14.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 左侧容器图标列
 */
@Composable
fun WarehouseSidebarColumn(
    warehouses: List<Warehouse>,
    selectedWarehouseId: Long?,
    warehouseItemCounts: Map<Long, Int>,
    onWarehouseClick: (Warehouse) -> Unit,
    onHomeClick: () -> Unit, // 新增：点击首页图标的回调
    onAddWarehouse: () -> Unit,
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    onViewInfo: ((Warehouse) -> Unit)? = null, // 新增：查看信息回调
    onGenerateQRCode: ((Warehouse) -> Unit)? = null, // 新增：生成二维码回调
    warehouseViewModel: WarehouseViewModel? = null, // 用于获取删除统计信息
    useCircleIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(60.dp) // 保持宽度
            .fillMaxHeight()
            // 为左侧容器列表添加浅色背景，圆角与其它卡片一致
            .clip(RoundedCornerShape(8.dp))
            .background(ColorHelpers.getGroup3CardBgColor())
            .padding(vertical = 6.dp), // 减小：8dp → 6dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // 减小：12dp → 8dp
    ) {
        // 首页图标（固定在顶部）
        val isHomeSelected = selectedWarehouseId == null
        val iconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier.fillMaxWidth() // 确保容器占满宽度，让指示器能正确显示
        ) {
            // 选中指示器（左侧竖条）
            if (isHomeSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 4.dp) // 与图标保持一定距离
                        .width(3.dp)
                        .height(24.dp)
                        .background(
                            ColorHelpers.getGroup2SettingsBtnColor(), // 与按钮颜色一致
                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                )
            }
            
            // 首页图标圆形容器（居中显示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(44.dp) // 略增尺寸，提升点击面积
                        .clip(iconShape)
                        .background(backgroundColor) // 与右侧容器按钮一致
                        .clickable(onClick = onHomeClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "首页",
                        tint = iconColor, // 根据背景颜色对比度自动切换
                        modifier = Modifier.size(22.dp) // 与外圈同比增大
                    )
                }
            }
        }
        
        // 分隔线
        Box(
            modifier = Modifier
                .width(32.dp) // 减小：40dp → 32dp
                .height(1.5.dp) // 减小：2dp → 1.5dp
                .background(ColorHelpers.getGroup4IconColor(0.3f))
        )
        
        // 容器列表（可滚动）- 支持嵌套滚动以允许下拉刷新
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // 允许下拉手势向上传递
                        return Offset.Zero
                    }
                }),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // 减小：12dp → 8dp
        ) {
            items(warehouses, key = { it.id }) { warehouse ->
                WarehouseIconItem(
                    warehouse = warehouse,
                    isSelected = warehouse.id == selectedWarehouseId,
                    itemCount = warehouseItemCounts[warehouse.id] ?: 0,
                    onClick = { onWarehouseClick(warehouse) },
                    onEditWarehouse = onEditWarehouse,
                    onDeleteWarehouse = onDeleteWarehouse,
                    onViewInfo = onViewInfo,
                    onGenerateQRCode = onGenerateQRCode,
                    warehouseViewModel = warehouseViewModel,
                    useCircleIcon = useCircleIcon
                )
            }
            
            // 添加按钮（跟随在容器图标后面）
            item {
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                Box(
                    modifier = Modifier
                        .size(44.dp) // 与容器图标同步增大
                        .clip(iconShape)
                        .background(backgroundColor) // 与容器图标颜色一致
                        .clickable(onClick = onAddWarehouse),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加容器",
                        tint = iconColor, // 根据背景颜色对比度自动切换
                        modifier = Modifier.size(22.dp) // 与容器图标大小一致
                    )
                }
            }
        }
    }
}

/**
 * 右上子容器图标项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubWarehouseIcon(
    warehouse: Warehouse,
    itemCount: Int,
    onClick: () -> Unit,
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    onViewInfo: ((Warehouse) -> Unit)? = null, // 新增：查看信息回调
    onGenerateQRCode: ((Warehouse) -> Unit)? = null, // 新增：生成二维码回调
    useCircleIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 子容器图片加载与亮度判断
    var warehouseImageBitmap by remember(warehouse.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(warehouse.imageUri) {
        warehouseImageBitmap = if (warehouse.imageUri != null) {
            try {
                val file = java.io.File(warehouse.imageUri)
                if (file.exists()) ImageUtils.loadBitmapFromPath(warehouse.imageUri) else null
            } catch (_: Exception) {
                null
            }
        } else null
    }
    val isImageBright = remember(warehouseImageBitmap) {
        val bitmap = warehouseImageBitmap
        if (bitmap != null) {
            try {
                ImageUtils.calculateImageBrightness(bitmap)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    var showMenu by remember { mutableStateOf(false) }
    
    // 优化的点击检测：延迟执行单击，以便检测双击
    val scope = rememberCoroutineScope()
    var clickJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var hasPendingClick by remember { mutableStateOf(false) }
    
    Box {
        Column(
            modifier = modifier
                .pointerInput(warehouse.id) {
                    detectTapGestures(
                        onTap = {
                            // 如果已经有待处理的单击，说明是双击
                            if (hasPendingClick) {
                                // 取消单击任务，执行双击操作
                                clickJob?.cancel()
                                clickJob = null
                                hasPendingClick = false
                                onViewInfo?.invoke(warehouse)
                            } else {
                                // 设置待处理标志
                                hasPendingClick = true
                                // 延迟执行单击，以便检测双击
                                clickJob = scope.launch {
                                    delay(250) // 250ms延迟，如果期间有第二次点击则取消
                                    if (hasPendingClick) {
                                        onClick()
                                        hasPendingClick = false
                                    }
                                    clickJob = null
                                }
                            }
                        },
                        onLongPress = { showMenu = true }
                    )
                }
                .padding(horizontal = 6.dp), // 减小：8dp → 6dp
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp) // 减小：4dp → 3dp
        ) {
            val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
            val textColor = if (warehouseImageBitmap != null && isImageBright != null) {
                if (isImageBright) Color.Black else Color.White
            } else {
                ColorHelpers.getContrastColor(backgroundColor)
            }
            val bitmap = warehouseImageBitmap
            val iconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(40.dp) // 与添加按钮大小一致
                    .clip(iconShape)
                    .background(if (bitmap == null) backgroundColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 半透明遮罩，提升文字可读性
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                } else {
                    Text(
                        text = warehouse.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall, // 减小字体
                        fontWeight = FontWeight.Bold,
                        color = textColor // 根据背景颜色对比度自动切换
                    )
                }
            }
            
            // 子容器名称
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp, // 减小字体：11.sp → 9.sp
                color = ColorHelpers.getGroup4TextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 50.dp) // 减小：60dp → 50dp
            )
        }
        
        // 添加背景层以支持点击外部关闭
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showMenu = false }
                    .zIndex(1f)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.zIndex(2f),
            shape = RoundedCornerShape(12.dp),
            containerColor = ColorHelpers.getGroup3CardBgColor(),
            tonalElevation = 8.dp
        ) {
            DropdownMenuItem(
                text = { 
                    Text(
                        stringResource(R.string.edit),
                        maxLines = 2 // 允许最多2行，支持文字换行
                    ) 
                },
                onClick = {
                    showMenu = false
                    onEditWarehouse(warehouse)
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
            )
            
            // 生成二维码选项
            if (onGenerateQRCode != null) {
                DropdownMenuItem(
                    text = { 
                        Text(
                            stringResource(R.string.generate_qr_code_action),
                            maxLines = 2 // 允许最多2行，支持文字换行
                        ) 
                    },
                    onClick = {
                        showMenu = false
                        onGenerateQRCode(warehouse)
                    },
                    leadingIcon = { Icon(Icons.Default.QrCode, null) },
                    modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                )
            }
            
            DropdownMenuItem(
                text = { 
                    Text(
                        stringResource(R.string.delete),
                        maxLines = 2 // 允许最多2行，支持文字换行
                    ) 
                },
                onClick = {
                    showMenu = false
                    onDeleteWarehouse(warehouse)
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
            )
        }
    }
}

/**
 * 右上子容器横向滚动区
 */
@Composable
fun SubWarehouseRow(
    subWarehouses: List<Warehouse>,
    warehouseItemCounts: Map<Long, Int>,
    onSubWarehouseClick: (Warehouse) -> Unit,
    onAddSubWarehouse: () -> Unit,
    onEditWarehouse: (Warehouse) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    warehousePath: List<Warehouse> = emptyList(), // 新增：面包屑导航路径
    onNavigateToWarehouse: ((Warehouse) -> Unit)? = null, // 新增：面包屑导航回调
    onViewInfo: ((Warehouse) -> Unit)? = null, // 新增：查看信息回调
    onGenerateQRCode: ((Warehouse) -> Unit)? = null, // 新增：生成二维码回调
    useCircleIcon: Boolean = true, // 新增：是否使用圆形图标
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp) // 减小：16dp,8dp → 12dp,6dp
            .nestedScroll(object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // 允许下拉手势向上传递（子容器列表是横向的，不应该拦截纵向下拉）
                    return Offset.Zero
                }
            }),
        shape = RoundedCornerShape(12.dp), // 统一为 12.dp
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 减小：2dp → 1dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 面包屑导航（如果有路径）
            if (warehousePath.isNotEmpty() && onNavigateToWarehouse != null) {
                // 使用 LazyRow 实现横向滚动
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(warehousePath.size) { index ->
                        val warehouse = warehousePath[index]
                        val isLast = index == warehousePath.size - 1
                        
                        // 容器名称（可点击），最后一个添加自定义后缀
                        val context = LocalContext.current
                        val prefs = remember {
                            context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                        }
                        val customSuffix = remember {
                            prefs.getString("warehouse_items_suffix", context.getString(R.string.warehouse_items_suffix)) 
                                ?: context.getString(R.string.warehouse_items_suffix)
                        }
                        val unnamedText = remember {
                            prefs.getString("unnamed_warehouse", context.getString(R.string.unnamed_warehouse))
                                ?: context.getString(R.string.unnamed_warehouse)
                        }
                        val displayText = if (isLast) {
                            "${warehouse.name.ifEmpty { unnamedText }}$customSuffix"
                        } else {
                            warehouse.name.ifEmpty { unnamedText }
                        }
                        
                        Text(
                            text = displayText,
                            style = if (isLast) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) {
                                ColorHelpers.getGroup4TextColor()
                            } else {
                                ColorHelpers.getGroup4TextColor(0.7f)
                            },
                            modifier = Modifier
                                .clickable { onNavigateToWarehouse(warehouse) }
                                .padding(vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 如果不是最后一个，显示箭头分隔符
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ColorHelpers.getGroup4IconColor(0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                
                // 横线分隔符
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = ColorHelpers.getGroup4IconColor(0.2f),
                    thickness = 1.dp
                )
            }
            
            // 子容器图标行
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 6.dp), // 减小：12dp,8dp → 8dp,6dp
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subWarehouses, key = { it.id }) { subWarehouse ->
                    SubWarehouseIcon(
                        warehouse = subWarehouse,
                        itemCount = warehouseItemCounts[subWarehouse.id] ?: 0,
                        onClick = { onSubWarehouseClick(subWarehouse) },
                        onEditWarehouse = onEditWarehouse,
                        onDeleteWarehouse = onDeleteWarehouse,
                        onViewInfo = onViewInfo,
                        onGenerateQRCode = onGenerateQRCode,
                        useCircleIcon = useCircleIcon
                    )
                }
                
                // 添加子容器按钮
                item {
                    Column(
                        modifier = Modifier
                            .clickable(onClick = onAddSubWarehouse)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                        val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                        val iconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .size(40.dp) // 与子容器大小一致
                                .clip(iconShape)
                                .background(backgroundColor), // 与子容器颜色一致
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                Icons.Default.Add,
                                contentDescription = "添加子容器",
                                tint = iconColor, // 根据背景颜色对比度自动切换
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Text(
                        //    text = stringResource(R.string.add_container_action),
                        //    style = MaterialTheme.typography.labelSmall,
                        //    color = ColorHelpers.getGroup4TextColor()
                        // )
                    }
                }
            }
        }
    }
}

// 标签颜色列表（提取到函数外部，避免每次重组都创建）
private val TAG_COLORS = listOf(
    Color(0xFFE3F2FD), // 浅蓝
    Color(0xFFF1F8E9), // 浅绿
    Color(0xFFFFF9C4), // 浅黄
    Color(0xFFFCE4EC), // 浅粉
    Color(0xFFE0F2F1), // 浅青
    Color(0xFFF3E5F5), // 浅紫
    Color(0xFFFFE0B2)  // 浅橙
)

/**
 * 右下物品列表项（带三个点菜单按钮）
 */
@Composable
fun ItemListRow(
    item: Item,
    onClick: () -> Unit,
    onEditItem: (Long) -> Unit = {},
    onAddToShoppingCart: (Item) -> Unit = {},
    onDeleteItem: (Item) -> Unit = {},
    onAddAlert: (Item) -> Unit = {},
    onQuantityChange: ((Item, Int) -> Unit)? = null, // 数量变化回调
    warehouseName: String? = null, // 容器名称（可选）
    useCircleIcon: Boolean = false, // Discord风格图标形状设置
    modifier: Modifier = Modifier
) {
    // 菜单展开状态
    var showMenu by remember { mutableStateOf(false) }
    
    // 获取主图路径（优先使用裁剪后的主图）
    val primaryImagePath = remember(item.imageUris, item.primaryImageIndex, item.imageUri) {
        if (item.imageUris.isNotEmpty() && item.primaryImageIndex < item.imageUris.size) {
            val originalPath = item.imageUris[item.primaryImageIndex]
            val croppedPath = ImageUtils.getCroppedImagePath(originalPath)
            // 如果裁剪后的图片存在，使用裁剪后的；否则使用原图
            if (croppedPath != null) {
            val croppedFile = java.io.File(croppedPath)
            if (croppedFile.exists()) croppedPath else originalPath
            } else {
                originalPath
            }
        } else {
            item.imageUri
        }
    }
    
    // 加载主图用于头像显示
    val avatarBitmap = remember(primaryImagePath) {
        if (primaryImagePath != null) {
            try {
                BitmapFactory.decodeFile(primaryImagePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            // .heightIn(min = 80.dp) // 设置最小高度，确保条形码等信息能够显示
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐，避免图标太靠顶部
        ) {
            // 左侧主图/占位 - 支持Discord风格图标形状
            val itemBackgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
            val itemTextColor = ColorHelpers.getContrastColor(itemBackgroundColor)
            val itemIconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(itemIconShape)
                    .background(itemBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    // 图片也需要应用相同的clip，确保圆角一致
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(itemIconShape), // 确保图片也应用相同的圆角
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = item.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = itemTextColor
                    )
                }
            }
            
            // 中间物品信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 物品名称（粗体）- 只显示一行
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 容器名称和描述在同一行显示（如果提供）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (warehouseName != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Inventory2 ,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = warehouseName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // 物品描述 - 只显示一行
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorHelpers.getGroup4TextColor(0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 标签显示：到期日结束后（次日00:01起）才算过期
                // 使用 remember 缓存计算结果，避免每次重组都重新计算
                val isExpired = remember(item.expiryDate) {
                    item.expiryDate?.let { date ->
                        val zone = ZoneId.systemDefault()
                        val nowZoned = Instant.now().atZone(zone)
                        val expiryEnd = Instant.ofEpochMilli(date.time)
                            .atZone(zone)
                            .toLocalDate()
                            .plusDays(1)          // 次日
                            .atStartOfDay(zone)   // 00:00
                            .plusMinutes(1)       // 00:01 后开始算过期
                        !nowZoned.isBefore(expiryEnd)
                    } ?: false
                }
                val allTagsToShow = remember(item.tags, isExpired) {
                    if (isExpired) {
                        item.tags + "过期"
                    } else {
                        item.tags
                    }
                }
                
                if (allTagsToShow.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(allTagsToShow.size) { index ->
                            val tag = allTagsToShow[index]
                            // 根据标签类型和索引分配不同浅颜色背景
                            val bgColor = when {
                                tag == "过期" -> Color(0xFFF3E5F5) // 浅紫
                                else -> {
                                    // 自定义标签使用循环颜色（柔和的浅色）
                                    TAG_COLORS[index % TAG_COLORS.size]
                                }
                            }
                            
                            val displayTag = when (tag) {
                                "过期" -> stringResource(R.string.status_expired)
                                else -> tag
                            }
                            
                            // 更紧凑的自定义标签视图，缩小文字与边框间距
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, ColorHelpers.getGroup4TextColor().copy(alpha = 0.6f))
                            ) {
                                Text(
                                    displayTag, 
                                    color = ColorHelpers.getGroup4TextColor(),
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 9.sp, fontSize = 8.sp)
                                )
                            }
                        }
                    }
                }
                
                // 过期日期和条码信息 - 分行显示，确保都能看到
                if (item.expiryDate != null) {
                    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) }
                    val dateStr = remember(item.expiryDate) { dateFormat.format(item.expiryDate) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ColorHelpers.getGroup4IconColor(0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = ColorHelpers.getGroup4TextColor(0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // 条形码单独一行显示，确保不会被截断
                if (item.barcode != null && item.barcode.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ColorHelpers.getGroup4IconColor(0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.barcode,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = ColorHelpers.getGroup4TextColor(0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // 右侧数量和价格信息 + 三个点菜单按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 价格显示
                    if (item.price != null) {
                        Text(
                            text = stringResource(R.string.price_with_value, item.price),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // 数量显示和快速调整按钮
                    Text(
                        text = stringResource(R.string.quantity_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                    
                    // 如果提供了数量变化回调，显示 +/- 按钮
                    if (onQuantityChange != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 减少按钮
                            IconButton(
                                onClick = { 
                                    if (item.quantity > 0) {
                                        onQuantityChange(item, item.quantity - 1)
                                    }
                                },
                                modifier = Modifier.size(24.dp),
                                enabled = item.quantity > 0
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "减少数量",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (item.quantity > 0) 
                                        ColorHelpers.getGroup4IconColor() 
                                    else 
                                        ColorHelpers.getGroup4IconColor(0.3f)
                                )
                            }
                            
                            // 数量文本
                            Text(
                                text = item.quantity.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor(),
                                modifier = Modifier.widthIn(min = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            // 增加按钮
                            IconButton(
                                onClick = { onQuantityChange(item, item.quantity + 1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "增加数量",
                                    modifier = Modifier.size(16.dp),
                                    tint = ColorHelpers.getGroup4IconColor()
                                )
                            }
                        }
                    } else {
                        // 没有回调时，只显示数量
                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    }
                }
                
                // 三个点菜单按钮
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = ColorHelpers.getGroup4IconColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.width(180.dp),
                        shape = RoundedCornerShape(6.dp),
                        containerColor = ColorHelpers.getGroup3CardBgColor(),
                        tonalElevation = 8.dp
                    ) {
                        // 编辑
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.edit),
                                    fontSize = 14.sp,
                                    color = ColorHelpers.getGroup4TextColor(),
                                    maxLines = 2 // 允许最多2行，支持文字换行
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onEditItem(item.id)
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Edit, 
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = ColorHelpers.getGroup4IconColor()
                                ) 
                            },
                            modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                            colors = MenuDefaults.itemColors(
                                textColor = ColorHelpers.getGroup4TextColor(),
                                leadingIconColor = ColorHelpers.getGroup4IconColor()
                            )
                        )
                        
                        // 添加到购物车
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.nav_shopping_basket),
                                    fontSize = 14.sp,
                                    color = ColorHelpers.getGroup4TextColor(),
                                    maxLines = 2 // 允许最多2行，支持文字换行
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onAddToShoppingCart(item)
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.ShoppingCart, 
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = ColorHelpers.getGroup4IconColor()
                                ) 
                            },
                            modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                            colors = MenuDefaults.itemColors(
                                textColor = ColorHelpers.getGroup4TextColor(),
                                leadingIconColor = ColorHelpers.getGroup4IconColor()
                            )
                        )
                        
                        // 提醒设置
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.alert_settings),
                                    fontSize = 14.sp,
                                    color = ColorHelpers.getGroup4TextColor(),
                                    maxLines = 2 // 允许最多2行，支持文字换行
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onAddAlert(item)
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Notifications, 
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = ColorHelpers.getGroup4IconColor()
                                ) 
                            },
                            modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                            colors = MenuDefaults.itemColors(
                                textColor = ColorHelpers.getGroup4TextColor(),
                                leadingIconColor = ColorHelpers.getGroup4IconColor()
                            )
                        )
                        
                        // 分隔线
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = ColorHelpers.getGroup4TextColor().copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )
                        
                        // 删除（红色）
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.delete),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2 // 允许最多2行，支持文字换行
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onDeleteItem(item)
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Delete, 
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                ) 
                            },
                            modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    }
}


/**
 * 标签筛选组件（横向滑动）
 */
@Composable
fun TagFilterBar(
    allTags: List<String>,
    selectedTags: Set<String>,
    onTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (allTags.isEmpty()) {
        return
    }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allTags, key = { it }) { tag ->
            val isSelected = selectedTags.contains(tag)
            
            // 使用Card包装FilterChip以添加阴影效果
            Card(
                modifier = Modifier
                    .height(28.dp)
                    .clickable { onTagSelected(tag) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        ColorHelpers.getGroup3CardBgColor()
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                FilterChip(
                    selected = isSelected,
                    onClick = { onTagSelected(tag) },
                    label = {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = if (isSelected) {
                                ColorHelpers.getGroup4TextColor()
                            } else {
                                ColorHelpers.getGroup4TextColor(0.8f)
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        selectedLabelColor = ColorHelpers.getGroup4TextColor(),
                        labelColor = ColorHelpers.getGroup4TextColor(0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = null, // 移除边框，因为Card已经提供了背景和阴影
                    // contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp) // 减少垂直padding
                )
            }
        }
    }
}

/**
 * 右下物品列表区域
 */
@Composable
fun ItemListSection(
    items: List<Item>,
    onEditItem: (Long) -> Unit,
    onViewItem: (Long) -> Unit = {}, // 查看物品信息回调
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    alertSettingsManager: AlertSettingsManager? = null,
    onDeleteItem: (Item) -> Unit = {},
    onAddAlert: (Item) -> Unit = {},
    allWarehouses: List<Warehouse>? = null, // 所有容器列表（用于显示容器名称）
    useCircleIcon: Boolean = false, // Discord风格图标形状设置
    modifier: Modifier = Modifier
) {
    // 标签筛选状态
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    val listState = rememberLazyListState(0, 0)
    // 获取所有唯一标签
    val allTags = remember(items) {
        items.flatMap { it.tags }.distinct().sorted()
    }
    
    // 根据选中的标签过滤物品
    val filteredItems = remember(items, selectedTags) {
        if (selectedTags.isEmpty()) {
            items
        } else {
            items.filter { item ->
                item.tags.any { tag -> selectedTags.contains(tag) }
            }
        }
    }
    
    // LazyColumn 的状态 - 使用 selectedTags 作为 key 的一部分，确保标签筛选变化时重置列表状态
    // val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0, initialFirstVisibleItemScrollOffset = 0)
    
    LaunchedEffect(filteredItems.size, selectedTags.size) {
        // 仅当取消所有标签（变全量列表）时重置顶部
        if (selectedTags.isEmpty() && filteredItems.size > items.size * 0.5f) {  // 粗略判断“变全量”（防误判）
            listState.scrollToItem(0, 0)
        }
        // 可选：筛选时若列表为空，保持顶部
        else if (filteredItems.isEmpty()) { listState.scrollToItem(0, 0) }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 标签筛选栏（只在Discord风格的物品统计列表显示）
        // 使用固定高度确保布局稳定，即使没有标签也保留空间
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (allTags.isNotEmpty()) 36.dp else 0.dp)
        ) {
            if (allTags.isNotEmpty()) {
                TagFilterBar(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onTagSelected = { tag ->
                        selectedTags = if (selectedTags.contains(tag)) {
                            selectedTags - tag
                        } else {
                            selectedTags + tag
                        }
                    }
                )
            }
        }
        
        if (filteredItems.isEmpty()) {
            // 空状态 - 使用可滚动容器以支持下拉刷新
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // 允许下拉手势向上传递
                            return Offset.Zero
                        }
                    }),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = ColorHelpers.getGroup4IconColor(0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (selectedTags.isNotEmpty()) {
                            stringResource(R.string.no_items_with_selected_tags)
                        } else {
                            stringResource(R.string.warehouse_empty)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                }
            }
        } else {
            // 使用 key 确保标签筛选变化时列表状态重置
            
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                // 允许下拉手势向上传递
                                return Offset.Zero
                            }
                        }),
                    verticalArrangement = Arrangement.spacedBy(3.dp), // 增加间距为阴影留出空间
                    contentPadding = PaddingValues(
                        bottom = 10.dp, // 为 FAB (80.dp) + 广告横幅 (90.dp) + 间距 (10.dp) 留出空间
                        top = 0.dp, 
                        start = 0.dp, 
                        end = 0.dp
                    )
                ) {
                items(filteredItems, key = { it.id }) { item ->
                // 查找物品所属的容器名称
                val warehouseName = remember(item.warehouseId, allWarehouses) {
                    allWarehouses?.find { it.id == item.warehouseId }?.name
                }
                
                ItemListRow(
                    item = item,
                    onClick = { onViewItem(item.id) },
                    onEditItem = onEditItem,
                    warehouseName = warehouseName, // 传递容器名称
                    useCircleIcon = useCircleIcon, // 传递Discord风格图标形状设置
                    onAddToShoppingCart = { item ->
                        shoppingItemViewModel?.let { vm ->
                            val shoppingItem = com.example.itemremindertool.data.model.ShoppingItem(
                                name = item.name,
                                description = item.description,
                                quantity = item.quantity,
                                priority = com.example.itemremindertool.data.model.Priority.MEDIUM,
                                itemId = item.id // 关联物品ID，用于完成购买时补充库存
                            )
                            vm.insertShoppingItem(shoppingItem)
                        }
                    },
                    onDeleteItem = { toDelete ->
                        if (onDeleteItem != {}) {
                            onDeleteItem(toDelete)
                        } else {
                            itemViewModel?.deleteItem(toDelete)
                        }
                    },
                    onAddAlert = onAddAlert,
                    onQuantityChange = { item, newQuantity ->
                        // 快速更新物品数量
                        itemViewModel?.updateItem(item.copy(quantity = newQuantity))
                    }
                )
            
            }
        }
    }
    }
}

/**
 * Discord风格的主布局 - 整合左侧容器列和右侧内容区
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun DiscordStyleMainLayout(
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    allItems: List<Item>,
    warehouseItemCounts: Map<Long, Int>,
    selectedWarehouseId: Long?,
    shoppingItemsCount: Int, // 新增：待购物品数量
    alertSettingsManager: AlertSettingsManager, // 新增：提醒设置管理器
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    itemReminderViewModel: com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel? = null, // 物品提醒ViewModel
    activityEventViewModel: com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel? = null, // 动态事件ViewModel
    warehouseViewModel: WarehouseViewModel? = null, // 新增：用于获取删除统计信息
    searchQuery: String = "", // 新增：搜索查询
    onWarehouseSelect: (Warehouse) -> Unit,
    onHomeClick: () -> Unit, // 新增：点击首页的回调
    onSubWarehouseClick: (Warehouse) -> Unit,
    onAddWarehouse: () -> Unit,
    onAddChildWarehouse: (Long) -> Unit,
    onEditWarehouse: (Long) -> Unit = {},
    onDeleteWarehouse: (Warehouse) -> Unit = {},
    onGenerateQRCode: ((Warehouse) -> Unit)? = null, // 新增：生成二维码回调
    onEditItem: (Long) -> Unit,
    onViewItem: (Long) -> Unit = {}, // 查看物品信息回调
    onDeleteItem: (Item) -> Unit = {},
    onAddAlert: (Item) -> Unit = {},
    pullRefreshState: androidx.compose.material.pullrefresh.PullRefreshState, // 下拉刷新状态
    useCircleIcon: Boolean = true, // 新增：是否使用圆形图标
    modifier: Modifier = Modifier
) {
    // 搜索过滤（支持标签搜索）
    val filteredItems = remember(allItems, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allItems.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                (item.barcode?.contains(searchQuery, ignoreCase = true) == true) ||
                item.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }
    }
    val isSearching = searchQuery.isNotBlank()
    // 计算当前容器的根容器（顶级容器）ID，用于侧边栏选中状态
    val rootWarehouseId = remember(selectedWarehouseId, allWarehouses, warehouses) {
        if (selectedWarehouseId == null) {
            null
        } else {
            // 递归查找根容器
            var currentId = selectedWarehouseId
            var currentWarehouse = allWarehouses.find { it.id == currentId }
            while (currentWarehouse != null && currentWarehouse.parentId != null) {
                currentId = currentWarehouse.parentId
                currentWarehouse = allWarehouses.find { it.id == currentId }
            }
            currentId
        }
    }
    
    // 计算当前容器的层级路径（面包屑导航）
    val breadcrumbPath = remember(selectedWarehouseId, allWarehouses, warehouses) {
        if (selectedWarehouseId == null) {
            emptyList<Warehouse>()
        } else {
            val path = mutableListOf<Warehouse>()
            var currentId = selectedWarehouseId
            
            // 合并所有容器列表，确保能找到容器
            val allContainers = (allWarehouses + warehouses).distinctBy { it.id }
            
            // 查找当前容器
            var currentWarehouse = allContainers.find { it.id == currentId }
            
            // 如果找不到当前容器，返回空列表
            if (currentWarehouse == null) {
                return@remember emptyList<Warehouse>()
            }
            
            // 从当前容器向上查找，直到根容器
            while (currentWarehouse != null) {
                path.add(0, currentWarehouse) // 添加到开头，保持从根到当前的顺序
                if (currentWarehouse.parentId != null) {
                    currentId = currentWarehouse.parentId
                    // 查找父容器
                    currentWarehouse = allContainers.find { it.id == currentId }
                } else {
                    break
                }
            }
            path
        }
    }
    
    // 获取选中容器的子容器
    val childWarehouses = remember(selectedWarehouseId, allWarehouses) {
        if (selectedWarehouseId != null) {
            allWarehouses.filter { it.parentId == selectedWarehouseId }
        } else {
            emptyList()
        }
    }
    
    // 获取选中容器的物品
    val warehouseItems = remember(selectedWarehouseId, allItems) {
        if (selectedWarehouseId != null) {
            allItems.filter { it.warehouseId == selectedWarehouseId }
        } else {
            emptyList()
        }
    }
    
    // 容器信息对话框状态
    var showWarehouseInfoDialog by remember { mutableStateOf(false) }
    var selectedWarehouseForInfo by remember { mutableStateOf<Warehouse?>(null) }
    
    // 删除确认对话框相关状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var warehouseToDelete by remember { mutableStateOf<Warehouse?>(null) }
    var deleteStatistics by remember { mutableStateOf<Pair<Int, Int>>(0 to 0) }
    var isSubWarehouseDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 合并所有容器列表
    val allContainers = remember(allWarehouses, warehouses) {
        (allWarehouses + warehouses).distinctBy { it.id }
    }
    
    // 查看容器信息的回调
    val onViewWarehouseInfo: (Warehouse) -> Unit = { warehouse ->
        selectedWarehouseForInfo = warehouse
        showWarehouseInfoDialog = true
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左侧容器图标列
            WarehouseSidebarColumn(
                warehouses = warehouses,
                selectedWarehouseId = rootWarehouseId, // 使用根容器ID保持选中状态
                warehouseItemCounts = warehouseItemCounts,
                onWarehouseClick = onWarehouseSelect,
                onHomeClick = onHomeClick,
                onAddWarehouse = onAddWarehouse,
                onEditWarehouse = { warehouse -> onEditWarehouse(warehouse.id) },
                onDeleteWarehouse = onDeleteWarehouse,
                onViewInfo = onViewWarehouseInfo,
                onGenerateQRCode = onGenerateQRCode,
                warehouseViewModel = warehouseViewModel,
                useCircleIcon = useCircleIcon
            )
        
        // 右侧内容区 - 使用 Box 包装以支持下拉刷新
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ColorHelpers.getGroup2PageBgColor())
                .pullRefresh(pullRefreshState)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 搜索模式：显示搜索结果
                    if (isSearching) {
                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = ColorHelpers.getGroup4IconColor(0.6f)
                                )
                                Text(
                                    stringResource(R.string.no_search_results),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ColorHelpers.getGroup4TextColor(0.6f)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = stringResource(R.string.search_results, filteredItems.size),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            ItemListSection(
                                items = filteredItems,
                                onEditItem = onEditItem,
                                onViewItem = onViewItem,
                                shoppingItemViewModel = shoppingItemViewModel,
                                itemViewModel = itemViewModel,
                                alertSettingsManager = alertSettingsManager,
                                onDeleteItem = onDeleteItem,
                                onAddAlert = onAddAlert,
                                useCircleIcon = useCircleIcon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // 首页状态（selectedWarehouseId == null）
                else if (selectedWarehouseId == null) {
                    // 添加待购列表切换状态
                    var showShoppingList by remember { mutableStateOf(false) }
                    var showContainerList by remember { mutableStateOf(false) }
                    var showItemList by remember { mutableStateOf(false) }
                    val allContainers = remember(allWarehouses, warehouses) {
                        (allWarehouses + warehouses).distinctBy { it.id }
                    }
                    
                    // 右上：统计卡片
                    HomeStatisticCards(
                        totalWarehouses = allContainers.size,
                        totalItems = allItems.size,
                        shoppingItemsCount = shoppingItemsCount,
                        onContainerClick = {
                            if (showContainerList) {
                                showContainerList = false
                                showItemList = false
                                showShoppingList = false
                            } else {
                                showContainerList = true
                                showItemList = false
                                showShoppingList = false
                            }
                        },
                        onItemClick = {
                            if (showItemList) {
                                showContainerList = false
                                showItemList = false
                                showShoppingList = false
                            } else {
                                showItemList = true
                                showContainerList = false
                                showShoppingList = false
                            }
                        },
                        onShoppingClick = {
                            if (showShoppingList) {
                                showShoppingList = false
                                showContainerList = false
                                showItemList = false
                            } else {
                                showShoppingList = true
                                showContainerList = false
                                showItemList = false
                            }
                        }
                    )
                    
                    // 右下：根据统计卡片点击展示对应列表
                    when {
                        showContainerList -> {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(
                                        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 90.dp + 16.dp
                                    )
                                ) {
                                    items(allContainers, key = { it.id }) { warehouse ->
                                        val childCount = remember(allContainers, warehouse) {
                                            countAllChildWarehouses(warehouse.id, allContainers)
                                        }
                                        val itemCount = remember(allContainers, allItems, warehouse) {
                                            countAllItemsInWarehouse(
                                                warehouse.id,
                                                allContainers,
                                                allItems,
                                                warehouseItemCounts
                                            )
                                        }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // 点击容器卡片，跳转到对应容器页面
                                                    onWarehouseSelect(warehouse)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = ColorHelpers.getGroup3CardBgColor()
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = warehouse.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ColorHelpers.getGroup4TextColor()
                                                )
                                                Text(
                                                    text = stringResource(R.string.child_warehouse_info, childCount, itemCount),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ColorHelpers.getGroup4TextColor(0.7f)
                                                )
                                                if (warehouse.description.isNotBlank()) {
                                                    Text(
                                                        text = warehouse.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ColorHelpers.getGroup4TextColor(0.7f),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        showItemList -> {
                            // 物品统计 - 显示所有物品及其所属容器
                            ItemListSection(
                                items = allItems,
                                onEditItem = onEditItem,
                                onViewItem = onViewItem,
                                shoppingItemViewModel = shoppingItemViewModel,
                                itemViewModel = itemViewModel,
                                alertSettingsManager = alertSettingsManager,
                                onDeleteItem = onDeleteItem,
                                onAddAlert = onAddAlert,
                                allWarehouses = allContainers, // 传递所有容器信息用于显示容器名称
                                useCircleIcon = useCircleIcon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        showShoppingList -> {
                            // 待购列表 - 增加底部 padding 避免被 FAB 挡住删除按钮
                            ShoppingListSection(
                                shoppingItemViewModel = shoppingItemViewModel,
                                itemViewModel = itemViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else -> {
                            // 提醒列表
                            AlertListSection(
                                items = allItems,
                                alertSettingsManager = alertSettingsManager,
                                itemReminderViewModel = itemReminderViewModel,
                                activityEventViewModel = activityEventViewModel,
                                onEditItem = onEditItem,
                                onViewItem = onViewItem,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // 如果有选中的容器，显示子容器和物品
                else {
                    // 搜索模式：在容器页面也显示搜索结果
                    if (isSearching) {
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = ColorHelpers.getGroup4IconColor(0.6f)
                                    )
                                    Text(
                                        stringResource(R.string.no_search_results),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ColorHelpers.getGroup4TextColor(0.6f)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = stringResource(R.string.search_results, filteredItems.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                                ItemListSection(
                                    items = filteredItems,
                                    onEditItem = onEditItem,
                                    onViewItem = onViewItem,
                                    shoppingItemViewModel = shoppingItemViewModel,
                                    itemViewModel = itemViewModel,
                                    alertSettingsManager = alertSettingsManager,
                                    onDeleteItem = onDeleteItem,
                                    onAddAlert = onAddAlert,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // 合并所有容器列表，确保能找到容器
                        val allContainers = remember(allWarehouses, warehouses) {
                            (allWarehouses + warehouses).distinctBy { it.id }
                        }
                        
                        // 计算显示路径：优先使用计算好的路径，如果为空则至少显示当前容器
                        val displayPath = remember(selectedWarehouseId, breadcrumbPath, allContainers) {
                            if (breadcrumbPath.isNotEmpty()) {
                                breadcrumbPath
                            } else {
                                // 如果路径为空，从合并的容器列表中查找当前容器
                                allContainers.find { it.id == selectedWarehouseId }
                                    ?.let { listOf(it) } ?: emptyList()
                            }
                        }
                        
                        // 右上：子容器横向滚动区（包含面包屑导航）
                        if (childWarehouses.isNotEmpty() || true) { // 总是显示，即使为空也显示添加按钮
                            SubWarehouseRow(
                                subWarehouses = childWarehouses,
                                warehouseItemCounts = warehouseItemCounts,
                                onSubWarehouseClick = onSubWarehouseClick,
                                onAddSubWarehouse = { onAddChildWarehouse(selectedWarehouseId) },
                                onEditWarehouse = { warehouse -> onEditWarehouse(warehouse.id) },
                                onDeleteWarehouse = { warehouse ->
                                    // 子容器删除也需要确认
                                    warehouseToDelete = warehouse
                                    isSubWarehouseDelete = true
                                    scope.launch {
                                        warehouseViewModel?.let { vm ->
                                            deleteStatistics = vm.getDeleteStatistics(warehouse)
                                        }
                                    }
                                    showDeleteDialog = true
                                },
                                warehousePath = displayPath, // 传入面包屑路径
                                onNavigateToWarehouse = { warehouse ->
                                    // 点击容器，导航到该容器
                                    onWarehouseSelect(warehouse)
                                },
                                onViewInfo = onViewWarehouseInfo,
                                onGenerateQRCode = onGenerateQRCode,
                                useCircleIcon = useCircleIcon
                            )
                        }
                    
                        // 右下：物品列表
                        ItemListSection(
                            items = warehouseItems,
                            onEditItem = onEditItem,
                            onViewItem = onViewItem,
                            shoppingItemViewModel = shoppingItemViewModel,
                            itemViewModel = itemViewModel,
                            alertSettingsManager = alertSettingsManager,
                            onDeleteItem = onDeleteItem,
                            onAddAlert = onAddAlert,
                            useCircleIcon = useCircleIcon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // 固定底部广告（不随列表滚动）
                Box(
                    modifier = Modifier
                        // .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 8.dp)
                ) {
                    DynamicBannerAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
        
        // 容器信息对话框
        if (showWarehouseInfoDialog) {
            WarehouseInfoBottomSheet(
                warehouse = selectedWarehouseForInfo,
                allWarehouses = allContainers,
                allItems = allItems,
                warehouseItemCounts = warehouseItemCounts,
                onDismiss = {
                    showWarehouseInfoDialog = false
                    selectedWarehouseForInfo = null
                },
                onNavigateToWarehouse = { warehouseId ->
                    // 导航到指定容器
                    allContainers.find { it.id == warehouseId }?.let { warehouse ->
                        onWarehouseSelect(warehouse)
                    }
                }
            )
        }
        
        // 子容器删除确认对话框
        if (showDeleteDialog && warehouseToDelete != null) {
            val (childCount, itemCount) = deleteStatistics
            val parentWarehouse = warehouseToDelete!!.parentId?.let { parentId ->
                allContainers.find { it.id == parentId }
            }
            val parentName = parentWarehouse?.name ?: "父容器"
            
            androidx.compose.ui.window.Dialog(onDismissRequest = { 
                showDeleteDialog = false
                warehouseToDelete = null
                isSubWarehouseDelete = false
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorHelpers.getGroup3CardBgColor()
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 警告图标
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 标题
                        Text(
                            text = stringResource(R.string.confirm_delete),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 容器名称
                        Text(
                            text = "\"${warehouseToDelete!!.name}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 风险提示
                        Text(
                            text = if (itemCount > 0) {
                                "删除后，其中的 $itemCount 个物品将移动到 $parentName"
                            } else {
                                stringResource(R.string.delete_warehouse_warning)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorHelpers.getGroup4TextColor(0.8f),
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 取消按钮
                            TextButton(
                                onClick = { 
                                    showDeleteDialog = false
                                    warehouseToDelete = null
                                    isSubWarehouseDelete = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    stringResource(R.string.cancel_button),
                                    fontSize = 14.sp,
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                            }
                            
                            // 确认删除按钮
                            Button(
                                onClick = {
                                    val warehouse = warehouseToDelete!!
                                    showDeleteDialog = false
                                    warehouseToDelete = null
                                    val isSubDelete = isSubWarehouseDelete
                                    isSubWarehouseDelete = false
                                    // 根据是否是子容器删除选择不同的删除方法
                                    if (isSubDelete && warehouse.parentId != null) {
                                        // 子容器删除：将物品移动到父容器
                                        warehouseViewModel?.deleteSubWarehouse(warehouse)
                                    } else {
                                        // 父容器删除：使用普通删除方法
                                        onDeleteWarehouse(warehouse)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(
                                    stringResource(R.string.delete),
                                    fontSize = 14.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }
            }
        }
        
    }
    }
    }
}

/**
 * 首页统计卡片（右上）- 现代化设计
 */
@Composable
fun HomeStatisticCards(
    totalWarehouses: Int,
    totalItems: Int,
    shoppingItemsCount: Int,
    onContainerClick: (() -> Unit)? = null,
    onItemClick: (() -> Unit)? = null,
    onShoppingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 容器数量 - 现代化样式
            StatisticItem(
                icon = Icons.Default.Inventory2,
                value = totalWarehouses.toString(),
                label = stringResource(R.string.stat_label_warehouse),
                iconColor = MaterialTheme.colorScheme.primary, // 使用主题色
                onClick = onContainerClick,
                modifier = Modifier.weight(1f)
            )
            
            // 分隔线
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = ColorHelpers.getGroup4TextColor(0.15f)
            )
            
            // 物品数量 - 现代化样式
            StatisticItem(
                icon = Icons.Default.Category,
                value = totalItems.toString(),
                label = stringResource(R.string.stat_label_item),
                iconColor = MaterialTheme.colorScheme.primary, // 使用主题色
                onClick = onItemClick,
                modifier = Modifier.weight(1f)
            )
            
            // 分隔线
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = ColorHelpers.getGroup4TextColor(0.15f)
            )
            
            // 待购物品 - 现代化样式（可点击）
            StatisticItem(
                icon = Icons.Default.ShoppingBag,
                value = shoppingItemsCount.toString(),
                label = stringResource(R.string.stat_label_shopping),
                iconColor = MaterialTheme.colorScheme.primary, // 使用主题色
                onClick = onShoppingClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单个统计项组件 - 现代化设计
 */
@Composable
fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val contentModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    
    Column(
        modifier = contentModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 图标容器 - 带渐变背景
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            iconColor.copy(alpha = 0.2f),
                            iconColor.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 数值和标签（同一行，数字在前文字在后）- 防止换行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorHelpers.getGroup4TextColor(),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = ColorHelpers.getGroup4TextColor(0.65f),
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 时间线动态Item - 现代化设计
 */
@Composable
fun TimelineItemView(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    timeStr: String,
    isLast: Boolean,
    isFirst: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp) // 设置最小高度，确保时间线连续
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 左侧：时间线和图标
        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 上半部分竖线（除了第一项）
//            if (!isFirst) {
//                Box(
//                    modifier = Modifier
//                        .width(3.dp)
//                        .height(12.dp)
//                        .background(
//                            color = iconColor.copy(alpha = 0.3f)
//                        )
//                )
//            } else {
//                Spacer(modifier = Modifier.height(12.dp))
//            }
            
            // 圆形图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ColorHelpers.getGroup2PageBgColor()) // 背景色遮盖时间线
                    // .border(3.dp, iconColor.copy(alpha = 0.3f), CircleShape)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // 下半部分竖线（除了最后一项）- 使用足够的高度确保连续
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(60.dp) // 使用固定高度，足够连接下一个项目
                        .background(
                            color = iconColor.copy(alpha = 0.3f)
                        )
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // 右侧：内容
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp, bottom = 12.dp)
        ) {
            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ColorHelpers.getGroup4TextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 描述
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor(0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            // 时间
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium,
                color = ColorHelpers.getGroup4TextColor(0.5f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 提醒列表项（时间线样式）- 保留用于兼容
 */
//@Composable
//fun AlertTimelineItem(
//    title: String,
//    description: String,
//    time: String,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp)
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = ColorHelpers.getGroup3CardBgColor()
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalArrangement = Arrangement.spacedBy(4.dp)
//        ) {
//            // 标题和时间
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = title,
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = ColorHelpers.getGroup4TextColor(),
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.weight(1f)
//                )
//                Text(
//                    text = time,
//                    style = MaterialTheme.typography.labelSmall,
//                    color = ColorHelpers.getGroup4TextColor(0.6f)
//                )
//            }
//
//            // 描述
//            Text(
//                text = description,
//                style = MaterialTheme.typography.labelSmall,
//                color = ColorHelpers.getGroup4TextColor(0.7f),
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
//    }
//}

/**
 * 提醒列表项
 */
@Composable
fun AlertListItem(
    title: String,
    description: String,
    icon: ImageVector,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp) // 减小：16dp,4dp → 12dp,3dp
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), // 统一为 12.dp
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp), // 减小：12dp → 10dp
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 减小：12dp → 10dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            val alertBackgroundColor = ColorHelpers.getGroup5AlertCardColor()
            val alertIconColor = ColorHelpers.getContrastColor(alertBackgroundColor)
            Box(
                modifier = Modifier
                    .size(36.dp) // 减小：40dp → 36dp
                    .clip(CircleShape)
                    .background(alertBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = alertIconColor, // 根据背景颜色对比度自动切换
                    modifier = Modifier.size(20.dp) // 减小：24dp → 20dp
                )
            }
            
            // 中间信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp) // 减小：4dp → 3dp
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium, // 减小字体
                    fontWeight = FontWeight.Bold,
                    color = ColorHelpers.getGroup4TextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall, // 减小字体
                    color = ColorHelpers.getGroup4TextColor(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 右侧时间
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = ColorHelpers.getGroup4TextColor(0.5f)
            )
        }
    }
}

/**
 * 提醒列表（右下）
 */
@Composable
fun AlertListSection(
    items: List<Item>,
    alertSettingsManager: AlertSettingsManager,
    itemReminderViewModel: com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel? = null,
    activityEventViewModel: com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel? = null,
    onEditItem: (Long) -> Unit,
    onViewItem: (Long) -> Unit = {}, // 查看物品信息回调
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 获取提醒设置
    val expiryReminderDays = remember { alertSettingsManager.getExpiryReminderDays() }
    val lowStockThreshold = remember { alertSettingsManager.getLowStockThreshold() }

    // 获取所有启用的自定义提醒
    val activeReminders = itemReminderViewModel?.allActiveReminders?.collectAsState(initial = emptyList())?.value ?: emptyList()
    
    // 获取所有动态事件
    val activityEvents = activityEventViewModel?.recentEvents?.collectAsState(initial = emptyList())?.value ?: emptyList()
    
    // 计算即将过期物品
    val calendar = Calendar.getInstance()
    val currentTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, expiryReminderDays)
    val reminderEndTime = calendar.timeInMillis
    
    val expiringItems = remember(items, currentTime, reminderEndTime) {
        items.filter { item ->
            item.expiryDate != null && 
            item.expiryDate.time >= currentTime && 
            item.expiryDate.time <= reminderEndTime
        }
    }
    
    // 计算库存不足物品
    val lowStockItems = remember(items, lowStockThreshold) {
        items.filter { item ->
            item.enableStockAlert && item.quantity <= lowStockThreshold
        }
    }
    
    // 合并所有动态和提醒，按时间排序
    data class TimelineItem(
        val id: String,
        val type: String, // "event", "expiring", "lowstock", "custom"
        val title: String,
        val description: String,
        val time: Long,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val iconColor: androidx.compose.ui.graphics.Color,
        val targetId: Long? = null,
        val item: Item? = null,
        val reminder: com.example.itemremindertool.data.model.ItemReminder? = null
    )
    
    // 获取主题色（在Composable上下文中）
    val themeColor = MaterialTheme.colorScheme.primary
    val expiringSoonTitle = stringResource(R.string.expiring_soon_title)
    val lowStockTitle = stringResource(R.string.low_stock_title)
    val allTimeline = remember(expiringItems, lowStockItems, activeReminders, activityEvents, themeColor, expiringSoonTitle, lowStockTitle) {
        val timeline = mutableListOf<TimelineItem>()
        
        // 添加动态事件
        activityEvents.forEach { event ->
            val (icon, iconColor) = when (event.type) {
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_ADDED -> 
                    Icons.Default.Add to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_DELETED -> 
                    Icons.Default.Delete to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_UPDATED -> 
                    Icons.Default.Edit to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_ADDED -> 
                    Icons.Default.Inventory2 to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_DELETED -> 
                    Icons.Default.Inventory2 to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_UPDATED -> 
                    Icons.Default.Inventory2 to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.REMINDER_TRIGGERED -> 
                    Icons.Default.Notifications to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_EXPIRING -> 
                    Icons.Default.Warning to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_LOW_STOCK -> 
                    Icons.Default.Inventory2 to themeColor
            }
            
            timeline.add(
                TimelineItem(
                    id = "event_${event.id}",
                    type = "event",
                    title = event.title,
                    description = event.description,
                    time = event.createdAt.time,
                    icon = icon,
                    iconColor = iconColor,
                    targetId = event.targetId
                )
            )
        }
        
        // 添加即将过期提醒
        expiringItems.forEach { item ->
            val daysUntilExpiry = ((item.expiryDate!!.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            val description = context.getString(
                R.string.item_expiring_description,
                item.name,
                daysUntilExpiry
            )
            timeline.add(
                TimelineItem(
                    id = "expiring_${item.id}",
                    type = "expiring",
                    title = expiringSoonTitle,
                    description = description,
                    time = item.expiryDate.time,
                    icon = Icons.Default.CalendarToday,
                    iconColor = themeColor,
                    targetId = item.id,
                    item = item
                )
            )
        }
        
        // 添加库存不足提醒
        lowStockItems.forEach { item ->
            val description = context.getString(
                R.string.item_low_stock_description,
                item.name,
                item.quantity
            )
            timeline.add(
                TimelineItem(
                    id = "lowstock_${item.id}",
                    type = "lowstock",
                    title = lowStockTitle,
                    description = description,
                    time = item.updatedAt.time,
                    icon = Icons.Default.Inventory,
                    iconColor = themeColor,
                    targetId = item.id,
                    item = item
                )
            )
        }
        
        // 添加自定义提醒
        val currentTime = System.currentTimeMillis()
        activeReminders.forEach { reminder ->
            val item = items.find { it.id == reminder.itemId }
            if (item != null) {
                val nextReminderTime = when (reminder.reminderType) {
                    com.example.itemremindertool.data.model.ReminderType.ONCE -> 
                        reminder.reminderTime?.time ?: currentTime
                    else -> currentTime
                }
                
                val typeStr = when (reminder.reminderType) {
                    com.example.itemremindertool.data.model.ReminderType.ONCE -> "一次性提醒"
                    com.example.itemremindertool.data.model.ReminderType.DAILY -> "每日提醒"
                    com.example.itemremindertool.data.model.ReminderType.MONTHLY -> "每月提醒"
                    com.example.itemremindertool.data.model.ReminderType.YEARLY -> "每年提醒"
                }
                
                timeline.add(
                    TimelineItem(
                        id = "custom_${reminder.id}",
                        type = "custom",
                        title = typeStr,
                        description = "${item.name} - ${reminder.reason}",
                        time = nextReminderTime,
                        icon = Icons.Default.Alarm,
                        iconColor = themeColor,
                        targetId = item.id,
                        item = item,
                        reminder = reminder
                    )
                )
            }
        }
        
        // 按时间降序排序（最新的在前）
        timeline.sortedByDescending { it.time }
    }
    
    if (allTimeline.isEmpty()) {
        // 空状态 - 使用可滚动容器以支持下拉刷新
        Box(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // 允许下拉手势向上传递
                        return Offset.Zero
                    }
                }),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = ColorHelpers.getGroup4IconColor(0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = stringResource(R.string.no_activity),
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
            }
        }
    } else {
        // 分页加载状态
        var displayedCount by remember { mutableStateOf(20) } // 初始加载20条
        val listState = rememberLazyListState()
        
        // 监听滚动到底部，自动加载更多
        LaunchedEffect(listState) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem?.index ?: 0
            }.collect { lastVisibleIndex ->
                // 当滚动到倒数第5项时，加载更多
                if (lastVisibleIndex >= displayedCount - 5 && displayedCount < allTimeline.size) {
                    displayedCount = minOf(displayedCount + 20, allTimeline.size)
                }
            }
        }
        
        // 时间线样式
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // 允许下拉手势向上传递
                        return Offset.Zero
                    }
                }),
            contentPadding = PaddingValues(
                start = 25.dp,
                end = 25.dp,
                top = 10.dp,
                bottom = 0.dp
            )
        ) {
            items(displayedCount) { index ->
                if (index < allTimeline.size) {
                    val timelineItem = allTimeline[index]
                    val context = LocalContext.current
                    val timeStr = remember(timelineItem.time) {
                        val now = System.currentTimeMillis()
                        val diff = now - timelineItem.time
                        when {
                            diff < 60 * 1000 -> context.getString(R.string.time_just_now)
                            diff < 60 * 60 * 1000 -> context.getString(R.string.time_minutes_ago, diff / (60 * 1000))
                            diff < 24 * 60 * 60 * 1000 -> context.getString(R.string.time_hours_ago, diff / (60 * 60 * 1000))
                            diff < 7 * 24 * 60 * 60 * 1000 -> context.getString(R.string.time_days_ago, diff / (24 * 60 * 60 * 1000))
                            else -> {
                                val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                dateTimeFormat.format(Date(timelineItem.time))
                            }
                        }
                    }
                    
                    TimelineItemView(
                        title = timelineItem.title,
                        description = timelineItem.description,
                        icon = timelineItem.icon,
                        iconColor = timelineItem.iconColor,
                        timeStr = timeStr,
                        isLast = index == displayedCount - 1 || index == allTimeline.size - 1,
                        isFirst = index == 0,
                        onClick = null // 移除点击跳转功能
                    )
                }
            }
        }
    }
}
/**
 * 待购列表（右下）- Discord 风格首页
 */
@Composable
fun ShoppingListSection(
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel?,
    itemViewModel: ItemViewModel?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shoppingItems by (shoppingItemViewModel?.shoppingItems?.collectAsState(initial = emptyList()) 
        ?: remember { mutableStateOf(emptyList()) })
    val completedItems = remember(shoppingItems) {
        shoppingItems.filter { it.isCompleted }
    }
    
    var showCompletePurchaseDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Text(
            //    text = "待购列表",
            //    style = MaterialTheme.typography.titleLarge,
            //    fontWeight = FontWeight.Bold,
            //    color = ColorHelpers.getGroup4TextColor()
            //)
            
            // 完成购买按钮（只在有已勾选的物品时显示）
            if (completedItems.isNotEmpty()) {
                Button(
                    onClick = { showCompletePurchaseDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.complete_purchase),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        
        // 待购物品列表（显示所有物品）
        if (shoppingItems.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                    Icons.Default.ShoppingCart,
                        contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = ColorHelpers.getGroup4IconColor(0.4f)
                    )
                Spacer(modifier = Modifier.height(16.dp))
                    Text(
                    stringResource(R.string.no_shopping_items),
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // 允许下拉手势向上传递
                            return Offset.Zero
                        }
                    }),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp) // 为 FAB (80.dp) + 广告横幅 (140.dp) 留出空间
            ) {
                items(shoppingItems) { shoppingItem ->
                    DashboardShoppingItemCard(
                        shoppingItem = shoppingItem,
                        onQuantityChange = { newQuantity: Int ->
                            shoppingItemViewModel?.updateShoppingItem(
                                shoppingItem.copy(quantity = newQuantity.coerceAtLeast(1))
                            )
                        },
                        onToggleComplete = {
                            shoppingItemViewModel?.updateShoppingItem(
                                shoppingItem.copy(
                                    isCompleted = !shoppingItem.isCompleted,
                                    completedAt = if (!shoppingItem.isCompleted) Date() else null
                                )
                            )
                        },
                        onDelete = {
                            shoppingItemViewModel?.deleteShoppingItem(shoppingItem)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // 完成购买确认对话框（优化后的紧凑版本）
    if (showCompletePurchaseDialog) {
        Dialog(
            onDismissRequest = { showCompletePurchaseDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ColorHelpers.getGroup3CardBgColor()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 图标和标题
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
//                    Text(
//                        text = stringResource(R.string.complete_purchase),
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 30.sp,
//                        color = ColorHelpers.getGroup4TextColor()
//                    )
                    
                    Text(
                        text = stringResource(R.string.confirm_complete_purchase),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorHelpers.getGroup4TextColor(0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮
                        OutlinedButton(
                            onClick = { showCompletePurchaseDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ColorHelpers.getGroup4TextColor()
                            )
                        ) {
                            Text(stringResource(R.string.cancel_button), fontSize = 14.sp)
                        }
                        
                        // 确定按钮
                        Button(
                            onClick = {
                                // 处理完成购买逻辑 - 只处理已勾选的物品
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    val itemDao = com.example.itemremindertool.data.database.AppDatabase
                                        .getDatabase(context)
                                        .itemDao()
                                    
                                    completedItems.forEach { shoppingItem ->
                                        // 如果有关联的物品，补充库存
                                        shoppingItem.itemId?.let { itemId ->
                                            try {
                                                val item = itemDao.getItemById(itemId)
                                                item?.let {
                                                    val updatedItem = it.copy(
                                                        quantity = it.quantity + shoppingItem.quantity
                                                    )
                                                    itemDao.updateItem(updatedItem)
                                                } ?: run {
                                                    // 如果物品不存在，记录错误但不阻止删除购物项
                                                    android.util.Log.e("ShoppingList", "物品不存在: itemId=$itemId")
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("ShoppingList", "更新物品库存失败: itemId=$itemId", e)
                                            }
                                        } ?: run {
                                            // 如果没有关联的物品ID，记录警告
                                            android.util.Log.w("ShoppingList", "购物项没有关联的物品ID: ${shoppingItem.name}")
                                        }
                                        // 删除已完成的待购物品
                                        shoppingItemViewModel?.deleteShoppingItem(shoppingItem)
                                    }
                                }
                                showCompletePurchaseDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.confirm_button), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 待购物品卡片（用于 Dashboard 内部）
 */
@Composable
fun DashboardShoppingItemCard(
    shoppingItem: ShoppingItem,
    onQuantityChange: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：勾选框和物品信息
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 勾选框
                Checkbox(
                    checked = shoppingItem.isCompleted,
                    onCheckedChange = { onToggleComplete() }
                )

                // 物品信息
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = shoppingItem.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (shoppingItem.isCompleted) {
                            ColorHelpers.getGroup4TextColor(0.5f)
                        } else {
                            ColorHelpers.getGroup4TextColor()
                        },
                        textDecoration = if (shoppingItem.isCompleted) {
                            androidx.compose.ui.text.style.TextDecoration.LineThrough
                        } else {
                            androidx.compose.ui.text.style.TextDecoration.None
                        }
                    )
                    if (shoppingItem.description.isNotBlank()) {
                        Text(
                            text = shoppingItem.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (shoppingItem.isCompleted) {
                                ColorHelpers.getGroup4TextColor(0.4f)
                            } else {
                                ColorHelpers.getGroup4TextColor(0.7f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (shoppingItem.isCompleted) {
                                androidx.compose.ui.text.style.TextDecoration.LineThrough
                            } else {
                                androidx.compose.ui.text.style.TextDecoration.None
                            }
                        )
                    }
                }
            }

            // 右侧：数量编辑和删除按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 数量编辑
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 减少按钮
                    IconButton(
                        onClick = { onQuantityChange(shoppingItem.quantity - 1) },
                        modifier = Modifier.size(32.dp),
                        enabled = !shoppingItem.isCompleted && shoppingItem.quantity > 1
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "减少数量",
                            modifier = Modifier.size(18.dp),
                            tint = if (shoppingItem.isCompleted || shoppingItem.quantity <= 1) {
                                ColorHelpers.getGroup4IconColor(0.3f)
                            } else {
                                ColorHelpers.getGroup4IconColor()
                            }
                        )
                    }

                    // 数量显示
                    Text(
                        text = shoppingItem.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (shoppingItem.isCompleted) {
                            ColorHelpers.getGroup4TextColor(0.5f)
                        } else {
                            ColorHelpers.getGroup4TextColor()
                        },
                        modifier = Modifier.widthIn(min = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 增加按钮
                    IconButton(
                        onClick = { onQuantityChange(shoppingItem.quantity + 1) },
                        modifier = Modifier.size(32.dp),
                        enabled = !shoppingItem.isCompleted
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "增加数量",
                            modifier = Modifier.size(18.dp),
                            tint = if (shoppingItem.isCompleted) {
                                ColorHelpers.getGroup4IconColor(0.3f)
                            } else {
                                ColorHelpers.getGroup4IconColor()
                            }
                        )
                    }
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
