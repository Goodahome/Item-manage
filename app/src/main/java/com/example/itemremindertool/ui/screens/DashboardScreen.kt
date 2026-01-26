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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
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
import com.example.itemremindertool.ui.components.HighlightShape
import com.example.itemremindertool.ui.components.HighlightedArea
import com.example.itemremindertool.ui.components.OnboardingAnchorKey
import com.example.itemremindertool.ui.components.OnboardingHint
import com.example.itemremindertool.ui.components.OnboardingOverlay
import com.example.itemremindertool.ui.components.OnboardingStep
import com.example.itemremindertool.ui.components.DynamicBannerAd
import com.example.itemremindertool.ui.components.DraggableFab
import com.example.itemremindertool.ui.components.AppFloatingActionButton
import com.example.itemremindertool.ui.components.AppDivider
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.components.WarehouseSelectionBottomSheet
import com.example.itemremindertool.ui.components.CameraCaptureDialog
import com.example.itemremindertool.ui.components.ImageCropDialog
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import java.io.File
import com.example.itemremindertool.ui.theme.LocalAppSettings
import com.example.itemremindertool.ui.viewmodel.DashboardViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.viewmodel.OperationState
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.utils.SyncStateManager
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

private fun firstDisplayChar(text: String): String {
    if (text.isBlank()) return "?"
    val codePoint = text.codePointAt(0)
    val count = Character.charCount(codePoint)
    return text.substring(0, count)
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
    val onboardingAnchors = remember { mutableStateMapOf<OnboardingAnchorKey, androidx.compose.ui.geometry.Rect>() }
    val savedOnboardingStep = remember {
        prefs.getString("onboarding_step", null)?.let { stepName ->
            runCatching { OnboardingStep.valueOf(stepName) }.getOrNull()
        }
    }
    var currentOnboardingStep by remember {
        mutableStateOf(savedOnboardingStep ?: OnboardingStep.HOME_TOP_BAR)
    }
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding.value) }

    fun updateOnboardingAnchor(
        key: OnboardingAnchorKey,
        coordinates: androidx.compose.ui.layout.LayoutCoordinates
    ) {
        val rect = coordinates.boundsInRoot()
        if (rect.width > 1f && rect.height > 1f) {
            onboardingAnchors[key] = rect
        }
    }

    fun setOnboardingStep(step: OnboardingStep) {
        currentOnboardingStep = step
        prefs.edit().putString("onboarding_step", step.name).apply()
    }

    fun completeOnboarding() {
        prefs.edit()
            .putBoolean("has_completed_onboarding", true)
            .remove("onboarding_step")
            .apply()
        hasCompletedOnboarding.value = true
        showOnboarding = false
    }

    fun nextOnboardingStep(step: OnboardingStep): OnboardingStep {
        return when (step) {
            OnboardingStep.HOME_TOP_BAR -> OnboardingStep.HOME_SEARCH
            OnboardingStep.HOME_SEARCH -> OnboardingStep.HOME_STATS_CONTAINER_BUTTON
            OnboardingStep.HOME_STATS_CONTAINER_BUTTON -> OnboardingStep.HOME_STATS_CONTAINER_PAGE
            OnboardingStep.HOME_STATS_CONTAINER_PAGE -> OnboardingStep.HOME_STATS_ITEM_BUTTON
            OnboardingStep.HOME_STATS_ITEM_BUTTON -> OnboardingStep.HOME_STATS_ITEM_PAGE
            OnboardingStep.HOME_STATS_ITEM_PAGE -> OnboardingStep.HOME_STATS_SHOPPING_BUTTON
            OnboardingStep.HOME_STATS_SHOPPING_BUTTON -> OnboardingStep.HOME_STATS_SHOPPING_PAGE
            OnboardingStep.HOME_STATS_SHOPPING_PAGE -> OnboardingStep.HOME_SIDEBAR_ADD
            OnboardingStep.HOME_SIDEBAR_ADD -> OnboardingStep.HOME_SIDEBAR_SAMPLE
            OnboardingStep.HOME_SIDEBAR_SAMPLE -> OnboardingStep.WAREHOUSE_CHILDREN_BREADCRUMB
            OnboardingStep.WAREHOUSE_CHILDREN_BREADCRUMB -> OnboardingStep.WAREHOUSE_TAG_FILTER
            OnboardingStep.WAREHOUSE_TAG_FILTER -> OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE
            OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE -> OnboardingStep.WAREHOUSE_GRID_ITEM
            OnboardingStep.WAREHOUSE_GRID_ITEM -> OnboardingStep.WAREHOUSE_INFO_CARD
            OnboardingStep.WAREHOUSE_INFO_CARD -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }
    }

    fun advanceOnboarding() {
        if (currentOnboardingStep == OnboardingStep.COMPLETE) {
            completeOnboarding()
        } else {
            setOnboardingStep(nextOnboardingStep(currentOnboardingStep))
        }
    }
    
    // 侧边栏图标圆形设置
    var sidebarIconCircle by remember { mutableStateOf(prefs.getBoolean("sidebar_icon_circle", false)) }
    var sidebarIconOutline by remember { mutableStateOf(prefs.getBoolean("sidebar_icon_outline", false)) }
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "sidebar_icon_circle") {
                sidebarIconCircle = prefs.getBoolean("sidebar_icon_circle", false)
            }
            if (key == "sidebar_icon_outline") {
                sidebarIconOutline = prefs.getBoolean("sidebar_icon_outline", false)
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
    val sampleWarehouseId = remember(warehouses) { warehouses.firstOrNull()?.id }
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
    
    // 容器物品数量映射（包含所有容器，不仅仅是顶层容器）
    // 使用 derivedStateOf 优化性能，避免在主线程进行重复计算
    val warehouseItemCounts by remember {
        derivedStateOf {
            // 使用 groupBy 一次性分组，然后计算每个容器的数量，性能更好
            // 过滤掉 null 值，确保类型为 Map<Long, Int>
            items.filter { it.warehouseId != null }
                .groupBy { it.warehouseId!! }
                .mapValues { it.value.size }
        }
    }
    
    // 预先构建容器ID到容器名称的映射，避免在列表项中重复查找
    val warehouseNameMap by remember(allWarehouses) {
        derivedStateOf {
            allWarehouses.associate { it.id to it.name }
        }
    }
    
    // 选中的容器ID状态（null表示显示首页）
    // 以外部传入的 initialSelectedWarehouseId 作为单一数据源，避免双向同步导致状态错乱
    val selectedWarehouseId = initialSelectedWarehouseId
    
    // 当容器列表变化时，如果选中的容器已被删除，返回首页
    LaunchedEffect(allWarehouses, selectedWarehouseId) {
        // 仓库列表加载完成且不为空时才进行校验，避免列表为空导致误清空选中状态
        if (selectedWarehouseId != null && allWarehouses.isNotEmpty() && allWarehouses.none { it.id == selectedWarehouseId }) {
            onSelectedWarehouseIdChanged(null) // 返回首页
        }
    }

    // 按系统返回键：若在子容器则返回父容器，否则回到首页
    val currentWarehouse = allWarehouses.find { it.id == selectedWarehouseId }
    val parentId = currentWarehouse?.parentId
    BackHandler(enabled = selectedWarehouseId != null) {
        if (parentId != null) {
            onSelectedWarehouseIdChanged(parentId)
        } else {
            onSelectedWarehouseIdChanged(null)
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
    
    
    // 高级功能对话框状态
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    val billingManager = remember {
        if (FeatureFlags.ENABLE_PURCHASE_FEATURE) {
            com.example.itemremindertool.billing.BillingManager(context).apply {
                initialize()
            }
        } else {
            null
        }
    }
    
    // 获取密度和卡片尺寸（使用正方形）
    val density = LocalDensity.current
    val cardWidthPx = remember { with(density) { 400.dp.toPx().toInt() } }
    val cardHeightPx = remember { with(density) { 400.dp.toPx().toInt() } } // 改为正方形
    
    Scaffold(
        topBar = {
            val appSettings = LocalAppSettings.current
            GradientTopAppBar(
                title = { 
                    Text(appSettings.appName)
                },
                modifier = Modifier.onGloballyPositioned {
                    updateOnboardingAnchor(OnboardingAnchorKey.TOP_BAR, it)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick,
                        enabled = !showOnboarding
                    ) {
                        Icon(Icons.Default.Menu, stringResource(R.string.settings))
                    }
                },
                actions = {
                    // 显示模式切换按钮（网格/整行）
                    val displayModeManager = remember { com.example.itemremindertool.config.ItemDisplayModeManager.getInstance(context) }
                    val displayMode by displayModeManager.displayMode.collectAsState()
                    
                    IconButton(
                        onClick = {
                            val wasListMode = displayMode == com.example.itemremindertool.config.ItemDisplayMode.LIST
                            displayModeManager.toggleDisplayMode()
                            if (showOnboarding &&
                                currentOnboardingStep == OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE &&
                                wasListMode
                            ) {
                                setOnboardingStep(OnboardingStep.WAREHOUSE_GRID_ITEM)
                            }
                        },
                        enabled = !showOnboarding || currentOnboardingStep == OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE,
                        modifier = Modifier.onGloballyPositioned {
                            updateOnboardingAnchor(OnboardingAnchorKey.TOP_BAR_LAYOUT_TOGGLE, it)
                        }
                    ) {
                        Icon(
                            if (displayMode == com.example.itemremindertool.config.ItemDisplayMode.GRID) {
                                Icons.Default.ViewColumn // 切换到列表模式图标
                            } else {
                                Icons.Default.ViewModule // 切换到网格模式图标
                            },
                            contentDescription = if (displayMode == com.example.itemremindertool.config.ItemDisplayMode.GRID) {
                                stringResource(R.string.item_display_mode_list)
                            } else {
                                stringResource(R.string.item_display_mode_grid)
                            }
                        )
                    }
                    // 云端同步按钮
                    val topBarBgColor = ColorHelpers.getTopBarGradientStart()
                    val syncRotation = if (isRefreshing) {
                        val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                        infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = -360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing)
                            ),
                            label = "sync_rotation_value"
                        ).value
                    } else {
                        0f
                    }
                    IconButton(
                        onClick = {
                            if (isRefreshing) return@IconButton
                            val serverUrl = prefs.getString("nextcloud_server_url", "") ?: ""
                            val username = prefs.getString("nextcloud_username", "") ?: ""
                            val password = prefs.getString("nextcloud_password", "") ?: ""
                            val hasCloudConfig = serverUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
                            if (hasCloudConfig) {
                                Log.d("DashboardScreen", "手动触发云端同步")
                                isRefreshing = true
                                com.example.itemremindertool.utils.CloudSyncScheduler.syncNow(context)
                            } else {
                                Log.d("DashboardScreen", "未配置云端同步，仅刷新本地数据")
                                isRefreshing = true
                                scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    dashboardViewModel.refresh()
                                    isRefreshing = false
                                }
                            }
                        },
                        enabled = !showOnboarding
                    ) {
                        Icon(
                            imageVector = if (isRefreshing) Icons.Default.Sync else Icons.Default.CloudUpload,
                            contentDescription = stringResource(R.string.cloud_storage),
                            tint = ColorHelpers.getTopBarTextColor(topBarBgColor),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(syncRotation)
                        )
                    }
                    // 拍照快速添加物品按钮（替换搜索按钮）
                    IconButton(
                        onClick = {
                            // 显示容器选择弹窗
                            showWarehouseSelectionForQuickAdd = true
                        },
                        enabled = !showOnboarding
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.quick_add_item),
                            tint = ColorHelpers.getTopBarTextColor(topBarBgColor)
                        )
                    }
                    // 首页：显示扫码按钮
                    IconButton(
                        onClick = onScanBarcode,
                        enabled = !showOnboarding
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.barcode_scanner),
                            tint = ColorHelpers.getTopBarTextColor(topBarBgColor)
                        )
                    }
                }
            )
        },
        floatingActionButton = {},
        contentWindowInsets = WindowInsets(0.dp) // 不使用系统 insets，手动控制 padding
    ) { paddingValues ->
        // 获取待购物品数量（用于侧边栏首页统计与列表切换）
        val shoppingItems by shoppingItemViewModel.shoppingItems.collectAsState(initial = emptyList())
        val activeShoppingItemsCount = remember(shoppingItems) {
            shoppingItems.count { !it.isCompleted }
        }

        var isBannerAdLoaded by remember { mutableStateOf(false) }
        val bottomInset = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        val adBottomPadding = if (isBannerAdLoaded) 90.dp + bottomInset + 8.dp else 0.dp
        
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            // 侧边栏风格主布局（添加顶部padding为搜索框留出空间）
            // 生成二维码对话框状态
            var showQRCodeDialog by remember { mutableStateOf(false) }
            var selectedWarehouseForQRCode by remember { mutableStateOf<Warehouse?>(null) }
            
            SidebarStyleMainLayout(
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
                showSearchBox = showSearchBox,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = {
                    searchQuery = ""
                },
                contentPadding = paddingValues,
                adBottomPadding = adBottomPadding,
                onWarehouseSelect = { warehouse ->
                    onSelectedWarehouseIdChanged(warehouse.id)
                    if (showOnboarding &&
                        currentOnboardingStep == OnboardingStep.HOME_SIDEBAR_SAMPLE &&
                        sampleWarehouseId != null &&
                        warehouse.id == sampleWarehouseId
                    ) {
                        setOnboardingStep(OnboardingStep.WAREHOUSE_CHILDREN_BREADCRUMB)
                    }
                },
                onHomeClick = {
                    // 点击首页图标，取消容器选中，显示统计和提醒
                    onSelectedWarehouseIdChanged(null)
                },
                onSubWarehouseClick = { subWarehouse ->
                    // 点击子容器，切换到该子容器显示其物品
                    onSelectedWarehouseIdChanged(subWarehouse.id)
                },
                onAddWarehouse = onAddWarehouse,
                onNavigateToWarehouseItemsTab = onNavigateToWarehouseItemsTab,
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
                useCircleIcon = sidebarIconCircle,
                useOutlineIcon = sidebarIconOutline,
                onboardingEnabled = showOnboarding,
                onboardingStep = currentOnboardingStep,
                onAdvanceOnboarding = { advanceOnboarding() },
                onSetOnboardingStep = { step -> setOnboardingStep(step) },
                onUpdateOnboardingAnchor = { key, coords -> updateOnboardingAnchor(key, coords) },
                modifier = Modifier
                    .fillMaxSize()
            )

            // 可拖拽的主 FAB（侧边栏风格首页）
            val isShoppingListVisible = remember(selectedWarehouseId) {
                selectedWarehouseId == null
            }
            val fabBackground = ColorHelpers.getGroup5FabColor()
            val fabIconColor = ColorHelpers.getGroup4IconColorByContrast(fabBackground)
            val sidebarOccupiedWidth = 66.dp + 6.dp + 4.dp
            val fabBoundsPadding = PaddingValues(
                start = sidebarOccupiedWidth,
                top = paddingValues.calculateTopPadding() + 8.dp,
                end = 12.dp,
                bottom = UIConstants.FAB_BOTTOM_PADDING + adBottomPadding
            )
            DraggableFab(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                boundsPadding = fabBoundsPadding
            ) { fabModifier ->
                AppFloatingActionButton(
                    onClick = {
                        // 直接添加物品
                        // 如果当前在购物列表页面，设置标记以便 ItemEditScreen 知道需要添加到购物篮
                        if (isShoppingListVisible) {
                            val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("add_to_shopping_list_after_save", true).apply()
                        }
                        // 如果当前选中了容器，则带入容器ID
                        onAddItem(selectedWarehouseId)
                    },
                    backgroundColor = fabBackground,
                    modifier = fabModifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_item)
                    )
                }
            }

            // 固定底部广告（覆盖左侧列表和右侧内容，仅在加载成功后占位）
            val adOverlayHeight = if (isBannerAdLoaded) 90.dp + bottomInset + 8.dp else 0.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(adOverlayHeight)
                    .background(
                        if (isBannerAdLoaded) ColorHelpers.getGroup2PageBgColor() else Color.Transparent
                    )
                    .zIndex(1f)
            ) {
                DynamicBannerAd(
                    modifier = if (isBannerAdLoaded) {
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .align(Alignment.TopCenter)
                    } else {
                        Modifier.size(0.dp)
                    },
                    height = 90.dp, // 与物品卡片相同的高度
                    onAdLoaded = { loaded ->
                        isBannerAdLoaded = loaded
                    }
                )
            }
            
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
            
            // 底部状态指示器
            BottomOperationStatusIndicator(
                operationState = operationState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            
            // 首次使用引导覆盖层
            if (showOnboarding) {
                val onboardingHint = when (currentOnboardingStep) {
                    OnboardingStep.HOME_TOP_BAR -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_topbar_title),
                        description = stringResource(R.string.onboarding_home_topbar_desc),
                        requiresClick = false
                    )
                    OnboardingStep.HOME_SEARCH -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_search_title),
                        description = stringResource(R.string.onboarding_home_search_desc),
                        requiresClick = false
                    )
                    OnboardingStep.HOME_STATS_CONTAINER_BUTTON -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_stat_container_button_title),
                        description = stringResource(R.string.onboarding_home_stat_container_button_desc),
                        requiresClick = true
                    )
                    OnboardingStep.HOME_STATS_CONTAINER_PAGE -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_stat_container_page_title),
                        description = stringResource(R.string.onboarding_home_stat_container_page_desc),
                        requiresClick = false
                    )
                    OnboardingStep.HOME_STATS_ITEM_BUTTON -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_stat_item_button_title),
                        description = stringResource(R.string.onboarding_home_stat_item_button_desc),
                        requiresClick = true
                    )
                    OnboardingStep.HOME_STATS_ITEM_PAGE -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_stat_item_page_title),
                        description = stringResource(R.string.onboarding_home_stat_item_page_desc),
                        requiresClick = false
                    )
                    OnboardingStep.HOME_STATS_SHOPPING_BUTTON -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_stat_shopping_button_title),
                        description = stringResource(R.string.onboarding_home_stat_shopping_button_desc),
                        requiresClick = true
                    )
                    OnboardingStep.HOME_STATS_SHOPPING_PAGE -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_stat_shopping_page_title),
                        description = stringResource(R.string.onboarding_home_stat_shopping_page_desc),
                        requiresClick = false
                    )
                    OnboardingStep.HOME_SIDEBAR_ADD -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_sidebar_add_title),
                        description = stringResource(R.string.onboarding_home_sidebar_add_desc),
                        requiresClick = false
                    )
                    OnboardingStep.HOME_SIDEBAR_SAMPLE -> OnboardingHint(
                        title = stringResource(R.string.onboarding_home_sidebar_sample_title),
                        description = stringResource(R.string.onboarding_home_sidebar_sample_desc),
                        requiresClick = true
                    )
                    OnboardingStep.WAREHOUSE_CHILDREN_BREADCRUMB -> OnboardingHint(
                        title = stringResource(R.string.onboarding_warehouse_children_breadcrumb_title),
                        description = stringResource(R.string.onboarding_warehouse_children_breadcrumb_desc),
                        requiresClick = false
                    )
                    OnboardingStep.WAREHOUSE_TAG_FILTER -> OnboardingHint(
                        title = stringResource(R.string.onboarding_warehouse_tag_filter_title),
                        description = stringResource(R.string.onboarding_warehouse_tag_filter_desc),
                        requiresClick = false
                    )
                    OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE -> OnboardingHint(
                        title = stringResource(R.string.onboarding_warehouse_layout_toggle_title),
                        description = stringResource(R.string.onboarding_warehouse_layout_toggle_desc),
                        requiresClick = true
                    )
                    OnboardingStep.WAREHOUSE_GRID_ITEM -> OnboardingHint(
                        title = stringResource(R.string.onboarding_warehouse_grid_item_title),
                        description = stringResource(R.string.onboarding_warehouse_grid_item_desc),
                        requiresClick = true
                    )
                    OnboardingStep.WAREHOUSE_INFO_CARD -> OnboardingHint(
                        title = stringResource(R.string.onboarding_warehouse_info_card_title),
                        description = stringResource(R.string.onboarding_warehouse_info_card_desc),
                        requiresClick = false
                    )
                    OnboardingStep.COMPLETE -> OnboardingHint(
                        title = stringResource(R.string.onboarding_complete_title),
                        description = stringResource(R.string.onboarding_complete_description),
                        requiresClick = false,
                        showFinger = false
                    )
                }

                val highlightedArea = when (currentOnboardingStep) {
                    OnboardingStep.HOME_TOP_BAR -> onboardingAnchors[OnboardingAnchorKey.TOP_BAR]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_SEARCH -> onboardingAnchors[OnboardingAnchorKey.SEARCH_BOX]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_STATS_CONTAINER_BUTTON -> onboardingAnchors[OnboardingAnchorKey.STAT_CONTAINER]?.let {
                        HighlightedArea(it, HighlightShape.CIRCLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_STATS_CONTAINER_PAGE -> onboardingAnchors[OnboardingAnchorKey.STAT_PAGE_CONTAINER]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_STATS_ITEM_BUTTON -> onboardingAnchors[OnboardingAnchorKey.STAT_ITEM]?.let {
                        HighlightedArea(it, HighlightShape.CIRCLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_STATS_ITEM_PAGE -> onboardingAnchors[OnboardingAnchorKey.STAT_PAGE_ITEM]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_STATS_SHOPPING_BUTTON -> onboardingAnchors[OnboardingAnchorKey.STAT_SHOPPING]?.let {
                        HighlightedArea(it, HighlightShape.CIRCLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_STATS_SHOPPING_PAGE -> onboardingAnchors[OnboardingAnchorKey.STAT_PAGE_SHOPPING]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_SIDEBAR_ADD -> onboardingAnchors[OnboardingAnchorKey.SIDEBAR_ADD]?.let {
                        HighlightedArea(it, HighlightShape.CIRCLE, paddingDp = 6f)
                    }
                    OnboardingStep.HOME_SIDEBAR_SAMPLE -> onboardingAnchors[OnboardingAnchorKey.SIDEBAR_SAMPLE]?.let {
                        HighlightedArea(it, HighlightShape.CIRCLE, paddingDp = 6f)
                    }
                    OnboardingStep.WAREHOUSE_CHILDREN_BREADCRUMB -> onboardingAnchors[OnboardingAnchorKey.SUBWAREHOUSE_ROW]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.WAREHOUSE_TAG_FILTER -> onboardingAnchors[OnboardingAnchorKey.TAG_FILTER]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE -> onboardingAnchors[OnboardingAnchorKey.TOP_BAR_LAYOUT_TOGGLE]?.let {
                        HighlightedArea(it, HighlightShape.CIRCLE, paddingDp = 6f)
                    }
                    OnboardingStep.WAREHOUSE_GRID_ITEM -> onboardingAnchors[OnboardingAnchorKey.GRID_ITEM]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.WAREHOUSE_INFO_CARD -> onboardingAnchors[OnboardingAnchorKey.INFO_CARD]?.let {
                        HighlightedArea(it, HighlightShape.RECTANGLE, paddingDp = 6f)
                    }
                    OnboardingStep.COMPLETE -> null
                }

                val shouldShowOverlay = highlightedArea != null || !onboardingHint.requiresClick
                if (shouldShowOverlay) {
                    OnboardingOverlay(
                        hint = onboardingHint,
                        highlightedArea = highlightedArea,
                        onNext = { advanceOnboarding() },
                        onSkip = { completeOnboarding() }
                    )
                }
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
                    },
                    cardWidth = cardWidthPx,
                    cardHeight = cardHeightPx
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
    
    // 高级功能对话框（仅在启用购买功能时显示）
    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false },
            onTrialStart = {
                // 试用开始后的处理（现在只有侧边栏风格，无需切换）
            }
        )
    }
} // 关闭 DashboardScreen 函数

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
    
    val infoCardBgColor = ColorHelpers.getGroup3CardBgColor()
    val textColor = ColorHelpers.getGroup4TextColor(infoCardBgColor)
    val iconColor = ColorHelpers.getGroup4IconColor(infoCardBgColor)
    val badgeBgColor = ColorHelpers.getGroup2SettingsBtnColor()
    val badgeTextColor = ColorHelpers.getContrastColor(badgeBgColor)
    val totalChildCount = remember(warehouse.id, allWarehouses) {
        countAllChildWarehouses(warehouse.id, allWarehouses)
    }
    val totalItemCount = remember(warehouse.id, allWarehouses, allItems, warehouseItemCounts) {
        countAllItemsInWarehouse(warehouse.id, allWarehouses, allItems, warehouseItemCounts)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorHelpers.getGroup2PageBgColor())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 容器图片
        if (warehouseImageBitmap != null) {
            Card(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    bitmap = warehouseImageBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = infoCardBgColor)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(badgeBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstDisplayChar(warehouse.name),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }
        }

        // 容器信息卡片（与物品详情一致的结构）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = infoCardBgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(badgeBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = warehouse.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = stringResource(R.string.items_count, itemCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.child_warehouse_info, totalChildCount, totalItemCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }

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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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

                AppDivider(
                    color = ColorHelpers.getDividerColor(),
                    thickness = 2.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconColor
                        )
                        Text(
                            text = stringResource(R.string.created_at),
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                    val dateFormat = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, java.util.Locale.getDefault()) }
                    val createdAtStr = remember(warehouse.createdAt) { dateFormat.format(warehouse.createdAt) }
                    Text(
                        text = createdAtStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }

        // 子容器和物品列表已移除，只显示容器信息
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
/**
 * 搜索框部分
 */
@Composable
fun SearchBoxSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取搜索框背景色和边框颜色
    val searchBoxBgColor = ColorHelpers.getSearchBoxBgColor()
    val searchBoxBorderColor = ColorHelpers.getSearchBoxBorderColor()
    
    // 搜索框文字/图标颜色（支持自定义配色）
    val searchBoxTextColor = ColorHelpers.getSearchBoxTextColor()
    val searchBoxIconColor = ColorHelpers.getSearchBoxIconColor()
    
    // 搜索框外层完全透明
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 0.dp)
    ) {
        // 使用 remember 来跟踪焦点状态，以便动态改变边框颜色
        var isFocused by remember { mutableStateOf(false) }
        // 自定义搜索框样式
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false
                )
                // 使用 modifier.border() 添加加粗边框（4.dp）
                .border(
                    width = 2.dp,
                    color = if (isFocused) {
                        searchBoxBorderColor // 聚焦时使用完整颜色
                    } else {
                        searchBoxBorderColor.copy(alpha = 1f) // 未聚焦时稍微透明
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            placeholder = { 
                Text(
                    stringResource(R.string.search_all_items),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = searchBoxTextColor.copy(alpha = 1f)
                ) 
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    null,
                    tint = searchBoxIconColor.copy(alpha = 1f)
                ) 
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onCloseSearch) {
                        Icon(
                            Icons.Default.Close, 
                            null, 
                            tint = searchBoxIconColor.copy(alpha = 1f)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = searchBoxBgColor, // 使用单独设置的搜索框背景色
                unfocusedContainerColor = searchBoxBgColor, // 使用单独设置的搜索框背景色
                focusedTextColor = searchBoxTextColor,
                unfocusedTextColor = searchBoxTextColor,
                // 将 OutlinedTextField 的默认边框设为透明，因为我们使用 modifier.border() 来绘制边框
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedPlaceholderColor = searchBoxTextColor.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = searchBoxTextColor.copy(alpha = 0.6f),
                focusedLeadingIconColor = searchBoxIconColor.copy(alpha = 0.7f),
                unfocusedLeadingIconColor = searchBoxIconColor.copy(alpha = 0.6f),
                focusedTrailingIconColor = searchBoxIconColor.copy(alpha = 0.7f),
                unfocusedTrailingIconColor = searchBoxIconColor.copy(alpha = 0.6f)
            ),
            singleLine = true
        )
    }
}

// ============================================================================
// 侧边栏风格新布局组件
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

    // 获取子容器（使用 derivedStateOf 优化性能）
    val childWarehouses by remember {
        derivedStateOf {
            allWarehouses.filter { it.parentId == warehouse.id }
        }
    }

    val itemCount = warehouseItemCounts[warehouse.id] ?: 0

    // 加载容器图片
    var warehouseImageBitmap by remember(warehouse.imageUri) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

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

    // 使用 Surface Variant 作为背景色
    val surfaceVariantColor = ColorHelpers.getSurfaceVariantColor()
    // 使用程序文字色作为内容颜色
    val breadcrumbTextColor = ColorHelpers.getGroup4TextColor()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = surfaceVariantColor,
        contentColor = breadcrumbTextColor,
    ) {
        // 使用 BoxWithConstraints 来获取可用高度
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            // maxHeight 是 ModalBottomSheet 提供的可用高度（已自动减去系统栏等）
            // 使用更大的高度百分比，确保所有内容（包括创建时间）都能显示
            val contentMaxHeight = maxHeight * 0.9f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = contentMaxHeight)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题栏（容器名称居中显示，使用面包屑文字颜色）
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = warehouse.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = breadcrumbTextColor
                    )
                }

                // 容器图片
                warehouseImageBitmap?.let { bitmap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = warehouse.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                AppDivider(
                    color = ColorHelpers.getDividerColor(),
                    thickness = 2.dp
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
                            color = breadcrumbTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = warehouse.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = breadcrumbTextColor
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
                            color = breadcrumbTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = warehouse.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = breadcrumbTextColor
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
                            color = breadcrumbTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${warehouse.capacity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = breadcrumbTextColor
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
                            trackColor = breadcrumbTextColor.copy(alpha = 0.2f)
                        )
                        Text(
                            text = stringResource(
                                R.string.used_capacity,
                                itemCount,
                                warehouse.capacity
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = breadcrumbTextColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // 统计信息
                AppDivider(
                    color = ColorHelpers.getDividerColor()
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
                            color = breadcrumbTextColor
                        )
                        Text(
                            text = stringResource(R.string.child_containers),
                            style = MaterialTheme.typography.bodySmall,
                            color = breadcrumbTextColor.copy(alpha = 0.7f)
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
                            color = breadcrumbTextColor
                        )
                        Text(
                            text = stringResource(R.string.warehouse_items),
                            style = MaterialTheme.typography.bodySmall,
                            color = breadcrumbTextColor.copy(alpha = 0.7f)
                        )
                    }
                }

                // 创建时间（右对齐）
                AppDivider(
                    color = ColorHelpers.getDividerColor()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = breadcrumbTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.created_at),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = breadcrumbTextColor.copy(alpha = 0.7f)
                        )
                    }
                    val dateFormat = remember {
                        java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM,
                            java.text.DateFormat.SHORT,
                            java.util.Locale.getDefault()
                        )
                    }
                    val createdAtStr =
                        remember(warehouse.createdAt) { dateFormat.format(warehouse.createdAt) }
                    Text(
                        text = createdAtStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = breadcrumbTextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
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
    useOutlineIcon: Boolean = false,
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
                    .offset(x = 6.dp) // 避开 Card 圆角裁剪区域（8dp 圆角）
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
                val bitmap = warehouseImageBitmap
                if (warehouse.imageUri != null && bitmap != null) {
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
            
            val baseColor = ColorHelpers.getGroup2SettingsBtnColor()
            val backgroundColor = if (warehouseImageBitmap != null) {
                Color.Transparent
            } else {
                baseColor
            }
            
            // 获取背景色用于计算对比度
            val bgColorForContrast = if (warehouseImageBitmap != null && isImageBright != null) {
                // 有图片时，根据图片亮度创建一个代表背景的颜色
                if (isImageBright) {
                    Color.White.copy(alpha = 0.3f) // 亮图片，使用浅色背景
                } else {
                    Color.Black.copy(alpha = 0.3f) // 暗图片，使用深色背景
                }
            } else {
                backgroundColor
            }
            
            // 根据背景色和对比度判断，返回对应的文字颜色
            val textColor = if (useOutlineIcon && warehouseImageBitmap == null) {
                baseColor
            } else {
                ColorHelpers.getGroup4TextColorByContrast(bgColorForContrast)
            }
            
            // 优化的点击检测：立即响应单击，延迟检测双击
            val scope = rememberCoroutineScope()
            var pendingClick by remember { mutableStateOf(false) }
            var clickJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            
            Box {
                val iconShapeForShadow = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .size(44.dp) // 略增尺寸
                        .then(
                            if (useOutlineIcon) {
                                Modifier
                            } else {
                                Modifier.shadow(
                                    elevation = 4.dp,
                                    shape = iconShapeForShadow,
                                    spotColor = Color.Black.copy(alpha = 0.3f),
                                    ambientColor = Color.Black.copy(alpha = 0.15f)
                                )
                            }
                        )
                        .clip(iconShapeForShadow)
                        .then(
                            if (useOutlineIcon) {
                                Modifier.border(2.dp, baseColor, iconShapeForShadow)
                            } else if (warehouseImageBitmap != null) {
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
                    val displayChar = firstDisplayChar(warehouse.name)
                    Text(
                        text = displayChar, // 支持 emoji
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
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
                    tonalElevation = 0.dp
                    ) {
                        // 编辑
                        val menuBgColor = ColorHelpers.getGroup2PageBgColor()
                        val menuTextColor = ColorHelpers.getGroup4TextColor(menuBgColor)
                        val menuIconColor = ColorHelpers.getGroup4IconColor(menuBgColor)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.edit),
                                    fontSize = 14.sp,
                                    color = menuTextColor,
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
                                    tint = menuIconColor
                                )
                            },
                            modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                            colors = MenuDefaults.itemColors(
                                textColor = menuTextColor,
                                leadingIconColor = menuIconColor
                            )
                        )

                        // 生成二维码
                        if (onGenerateQRCode != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.generate_qr_code),
                                        fontSize = 14.sp,
                                        color = menuTextColor,
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
                                        tint = menuIconColor
                                    )
                                },
                                modifier = Modifier.heightIn(min = 36.dp), // 最小高度36dp，但允许根据内容自动扩展
                                colors = MenuDefaults.itemColors(
                                    textColor = menuTextColor,
                                    leadingIconColor = menuIconColor
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
            AppDialogLayout(
                title = stringResource(R.string.confirm_delete),
                icon = Icons.Default.Delete,
                onDismiss = { showDeleteConfirmDialog = false },
                footer = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteWarehouse(warehouse)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            ) {
                Text(
                    text = "\"${warehouse.name}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ColorHelpers.getGroup4TextColor()
                )
                Text(
                    text = stringResource(R.string.delete_warehouse_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorHelpers.getGroup4TextColor(0.8f),
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
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
    useOutlineIcon: Boolean = false,
    onFirstWarehousePositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onAddButtonPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Card 容器（与右侧子容器列表样式一致）
    Card(
        modifier = modifier
            .width(66.dp)
            .fillMaxHeight()
            .padding(start = 0.dp, end = 0.dp, top = 3.dp, bottom = 3.dp),
        shape = RoundedCornerShape(12.dp), // 与右侧子容器列表一致
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 与右侧子容器列表一致
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
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
                                .offset(x = 6.dp) // 避开 Card 圆角裁剪区域（8dp 圆角）
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
                        val iconColor = if (useOutlineIcon) {
                            backgroundColor
                        } else {
                            ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp) // 略增尺寸，提升点击面积
                                .then(
                                    if (useOutlineIcon) {
                                        Modifier
                                    } else {
                                        Modifier.shadow(
                                            elevation = 4.dp,
                                            shape = iconShape,
                                            spotColor = Color.Black.copy(alpha = 0.3f),
                                            ambientColor = Color.Black.copy(alpha = 0.15f)
                                        )
                                    }
                                )
                                .clip(iconShape)
                                .then(
                                    if (useOutlineIcon) {
                                        Modifier.border(2.dp, backgroundColor, iconShape)
                                    } else {
                                        Modifier.background(backgroundColor) // 与右侧容器按钮一致
                                    }
                                )
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


            // 分隔线（两端半圆）
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(ColorHelpers.getDividerColor())
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
                itemsIndexed(warehouses, key = { _, warehouse -> warehouse.id }) { index, warehouse ->
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
                        useCircleIcon = useCircleIcon,
                        useOutlineIcon = useOutlineIcon,
                        modifier = if (index == 0 && onFirstWarehousePositioned != null) {
                            Modifier.onGloballyPositioned { onFirstWarehousePositioned(it) }
                        } else {
                            Modifier
                        }
                    )
                }

                // 添加按钮（跟随在容器图标后面）
                item {
                    val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                    val iconColor = if (useOutlineIcon) {
                        backgroundColor
                    } else {
                        ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
                    }
                    val addButtonIconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp) // 与容器图标同步增大
                            .then(
                                if (useOutlineIcon) {
                                    Modifier
                                } else {
                                    Modifier.shadow(
                                        elevation = 4.dp,
                                        shape = addButtonIconShape,
                                        spotColor = Color.Black.copy(alpha = 0.3f),
                                        ambientColor = Color.Black.copy(alpha = 0.15f)
                                    )
                                }
                            )
                            .clip(addButtonIconShape)
                            .then(
                                if (useOutlineIcon) {
                                    Modifier.border(2.dp, backgroundColor, addButtonIconShape)
                                } else {
                                    Modifier.background(backgroundColor) // 与容器图标颜色一致
                                }
                            )
                            .clickable(onClick = onAddWarehouse)
                            .then(
                                if (onAddButtonPositioned != null) {
                                    Modifier.onGloballyPositioned { onAddButtonPositioned(it) }
                                } else {
                                    Modifier
                                }
                            ),
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
    useOutlineIcon: Boolean = false,
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
            
            // 获取背景色用于计算对比度
            val bgColorForContrast = if (warehouseImageBitmap != null && isImageBright != null) {
                // 有图片时，根据图片亮度创建一个代表背景的颜色
                if (isImageBright) {
                    Color.White.copy(alpha = 0.25f) // 亮图片，使用浅色背景
                } else {
                    Color.Black.copy(alpha = 0.25f) // 暗图片，使用深色背景
                }
            } else {
                backgroundColor
            }
            
            // 根据背景色和对比度判断，返回对应的文字颜色
            val textColor = if (useOutlineIcon && warehouseImageBitmap == null) {
                backgroundColor
            } else {
                ColorHelpers.getGroup4TextColorByContrast(bgColorForContrast)
            }
            val bitmap = warehouseImageBitmap
            val iconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(40.dp) // 与添加按钮大小一致
                    .clip(iconShape)
                    .then(
                        if (useOutlineIcon) {
                            Modifier.border(2.dp, backgroundColor, iconShape)
                        } else if (bitmap == null) {
                            Modifier.background(backgroundColor)
                        } else {
                            Modifier
                        }
                    ),
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
                    val displayChar = firstDisplayChar(warehouse.name)
                    Text(
                        text = displayChar, // 支持 emoji
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor // 根据背景颜色对比度自动切换
                    )
                }
            }
            
            // 子容器名称（使用程序文字色）
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
    useOutlineIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 5.dp, top = 3.dp, bottom = 3.dp) // 右侧留出空间显示圆角
            .nestedScroll(object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // 允许下拉手势向上传递（子容器列表是横向的，不应该拦截纵向下拉）
                    return Offset.Zero
                }
            }),
        shape = RoundedCornerShape(12.dp),
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
                        val defaultSuffix = context.getString(R.string.warehouse_items_suffix)
                        val effectiveSuffix = if (PremiumFeatureManager.canAccessPremiumFeatures(context)) {
                            customSuffix
                        } else {
                            defaultSuffix
                        }
                        val unnamedText = remember {
                            prefs.getString("unnamed_warehouse", context.getString(R.string.unnamed_warehouse))
                                ?: context.getString(R.string.unnamed_warehouse)
                        }
                        val displayText = if (isLast) {
                            "${warehouse.name.ifEmpty { unnamedText }}$effectiveSuffix"
                        } else {
                            warehouse.name.ifEmpty { unnamedText }
                        }
                        
                        // 使用程序文字色
                        val breadcrumbTextColor = ColorHelpers.getGroup4TextColor()
                        val breadcrumbIconColor = ColorHelpers.getGroup4IconColor()
                        
                        Text(
                            text = displayText,
                            style = if (isLast) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) {
                                breadcrumbTextColor
                            } else {
                                breadcrumbTextColor.copy(alpha = 0.7f)
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
                                tint = breadcrumbIconColor.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                
                // 横线分隔符（使用程序文字色）
                val breadcrumbIconColor = ColorHelpers.getGroup4IconColor()
                AppDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = ColorHelpers.getDividerColor(),
                    thickness = 2.dp
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
                        useCircleIcon = useCircleIcon,
                        useOutlineIcon = useOutlineIcon
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
                        val iconColor = if (useOutlineIcon) {
                            backgroundColor
                        } else {
                            ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
                        }
                        val iconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .size(40.dp) // 与子容器大小一致
                                .clip(iconShape)
                                .then(
                                    if (useOutlineIcon) {
                                        Modifier.border(2.dp, backgroundColor, iconShape)
                                    } else {
                                        Modifier.background(backgroundColor) // 与子容器颜色一致
                                    }
                                ),
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
    onMoveToContainer: (Item) -> Unit = {},
    showMoveAction: Boolean = false,
    onAddToShoppingCart: (Item) -> Unit = {},
    onDeleteItem: (Item) -> Unit = {},
    onAddAlert: (Item) -> Unit = {},
    onQuantityChange: ((Item, Int) -> Unit)? = null, // 数量变化回调
    warehouseName: String? = null, // 容器名称（可选）
    useCircleIcon: Boolean = false, // 侧边栏风格图标形状设置
    useOutlineIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 菜单展开状态
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 获取主图路径（原图路径，用于生成缩略图）
    val primaryImagePath = remember(item.imageUris, item.primaryImageIndex, item.imageUri) {
        if (item.imageUris.isNotEmpty() && item.primaryImageIndex < item.imageUris.size) {
            item.imageUris[item.primaryImageIndex]
        } else {
            item.imageUri
        }
    }
    
    // 使用缩略图加载头像图片（异步加载，避免阻塞UI）
    var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(primaryImagePath) {
        avatarBitmap = null
        
        if (primaryImagePath != null) {
            scope.launch(Dispatchers.IO) {
                // 加载缩略图（最大200像素，用于列表头像）
                val thumbnail = ImageUtils.loadThumbnail(context, primaryImagePath, maxSize = 200)
                if (thumbnail != null) {
                    avatarBitmap = thumbnail
                }
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 5.dp, top = 3.dp, bottom = 3.dp)
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐，避免图标太靠顶部
        ) {
            // 左侧主图/占位 - 支持侧边栏风格图标形状
            val itemBackgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
            val itemTextColor = if (useOutlineIcon && avatarBitmap == null) {
                itemBackgroundColor
            } else {
                ColorHelpers.getContrastColor(itemBackgroundColor)
            }
            val itemIconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
            val displayChar = firstDisplayChar(item.name)
            val displayText = if (displayChar.length == 1) displayChar.uppercase() else displayChar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(itemIconShape)
                    .then(
                        if (useOutlineIcon) {
                            Modifier.border(2.dp, itemBackgroundColor, itemIconShape)
                        } else {
                            Modifier.background(itemBackgroundColor)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    // 图片也需要应用相同的clip，确保圆角一致
                    Image(
                        bitmap = avatarBitmap!!.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(itemIconShape), // 确保图片也应用相同的圆角
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = displayText,
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
                val expiredTagLabel = stringResource(R.string.status_expired)
                val allTagsToShow = remember(item.tags, isExpired, expiredTagLabel) {
                    if (isExpired) {
                        item.tags + expiredTagLabel
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
                                tag == expiredTagLabel -> Color(0xFFF3E5F5) // 浅紫
                                else -> {
                                    // 自定义标签使用循环颜色（柔和的浅色）
                                    TAG_COLORS[index % TAG_COLORS.size]
                                }
                            }
                            
                            val displayTag = if (tag == expiredTagLabel) {
                                expiredTagLabel
                            } else {
                                tag
                            }
                            
                            // 更紧凑的自定义标签视图，缩小文字与边框间距
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = Color.Transparent,
                                border = BorderStroke(0.5.dp, ColorHelpers.getGroup4TextColor().copy(alpha = 0.6f))
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
                            text = CurrencyUtils.formatPrice(LocalContext.current, item.price),
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
                                modifier = Modifier.size(16.dp),
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
                                modifier = Modifier.widthIn(min = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            // 增加按钮
                            IconButton(
                                onClick = { onQuantityChange(item, item.quantity + 1) },
                                modifier = Modifier.size(16.dp)
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
                        tonalElevation = 0.dp
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

                        if (showMoveAction) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.move_to_container),
                                        fontSize = 14.sp,
                                        color = ColorHelpers.getGroup4TextColor(),
                                        maxLines = 2
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onMoveToContainer(item)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DriveFileMove,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ColorHelpers.getGroup4IconColor()
                                    )
                                },
                                modifier = Modifier.heightIn(min = 36.dp),
                                colors = MenuDefaults.itemColors(
                                    textColor = ColorHelpers.getGroup4TextColor(),
                                    leadingIconColor = ColorHelpers.getGroup4IconColor()
                                )
                            )
                        }
                        
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
                        AppDivider(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = ColorHelpers.getDividerColor(),
                            thickness = 2.dp
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

    val outlineEnabled = ColorHelpers.isOutlineEnabled()
    val expiredTagLabel = stringResource(R.string.status_expired)
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        items(allTags, key = { it }) { tag ->
            val isSelected = selectedTags.contains(tag)
            
            // 获取标签的背景色
            val tagBaseColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                ColorHelpers.getGroup3CardBgColor()
            }
            val tagBgColor = if (outlineEnabled && isSelected) Color.Transparent else tagBaseColor

            // 根据背景色和对比度判断，返回对应的文字颜色
            val contrastColor = when {
                outlineEnabled && isSelected -> tagBaseColor
                !isSelected -> ColorHelpers.getGroup4TextColor()
                else -> ColorHelpers.getGroup4TextColorByContrast(tagBgColor)
            }
            
            val displayTag = if (tag == expiredTagLabel) {
                expiredTagLabel
            } else {
                tag
            }

            if (outlineEnabled && isSelected) {
                Surface(
                    modifier = Modifier
                        .height(28.dp)
                        .clickable { onTagSelected(tag) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(2.dp, tagBaseColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = contrastColor
                        )
                    }
                }
            } else {
                // 使用Card包装FilterChip以添加阴影效果
                Card(
                    modifier = Modifier
                        .height(28.dp)
                        .clickable { onTagSelected(tag) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = tagBgColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = contrastColor // 使用对比色（白色或黑色）
                        )
                    }
                }
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
    useCircleIcon: Boolean = false, // 侧边栏风格图标形状设置
    useOutlineIcon: Boolean = false,
    warehouseViewModel: WarehouseViewModel? = null,
    onTagFilterPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onGridItemPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onInfoCardPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onTagFilterMissing: (() -> Unit)? = null,
    onOnboardingGridItemClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val expiredTagLabel = stringResource(R.string.status_expired)
    
    // 预先构建容器ID到容器名称的映射，避免在列表项中重复查找
    val warehouseNameMap by remember(allWarehouses) {
        derivedStateOf {
            (allWarehouses ?: emptyList()).associate { it.id to it.name }
        }
    }
    
    // 获取展示模式
    val displayModeManager = remember { com.example.itemremindertool.config.ItemDisplayModeManager.getInstance(context) }
    val displayMode by displayModeManager.displayMode.collectAsState()
    
    // 标签筛选状态（当容器切换/物品变化时重置，避免沿用旧筛选）
    var selectedTags by remember(items) { mutableStateOf<Set<String>>(emptySet()) }
    
    // 选中的物品（仅网格模式使用）- 使用 rememberSaveable 保存配置变更
    var selectedItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var itemToMove by remember { mutableStateOf<Item?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    val canMoveItems = warehouseViewModel != null

    val selectedItem by remember(items, selectedItemId) {
        derivedStateOf {
            selectedItemId?.let { id -> items.find { it.id == id } }
        }
    }
    
    // 配置变更标志位 - 用于在配置变更时延迟复杂渲染
    var isConfigChanging by remember { mutableStateOf(false) }
    
    // 监听配置变更
    DisposableEffect(configuration) {
        // 配置变更开始
        isConfigChanging = true
        selectedItemId = null
        
        onDispose {
            // 配置变更结束
            isConfigChanging = false
        }
    }
    
    // 延迟恢复渲染，给配置变更留出时间
    LaunchedEffect(isConfigChanging) {
        if (isConfigChanging) {
            kotlinx.coroutines.delay(100) // 延迟100ms
            isConfigChanging = false
        }
    }
    
    val listState = rememberLazyListState(0, 0)
    val gridState = rememberLazyGridState()
    
    // 标签筛选变化时重置网格滚动位置
    LaunchedEffect(selectedTags) {
        if (displayMode == com.example.itemremindertool.config.ItemDisplayMode.GRID) {
            gridState.scrollToItem(0, 0)
        }
    }
    
    // 获取所有唯一标签（使用 derivedStateOf 优化性能）
    val allTags by remember(items, expiredTagLabel) {
        derivedStateOf {
            val expiredItemExists = items.any { item ->
                item.expiryDate?.let { date ->
                    val zone = ZoneId.systemDefault()
                    val nowZoned = Instant.now().atZone(zone)
                    val expiryEnd = Instant.ofEpochMilli(date.time)
                        .atZone(zone)
                        .toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(zone)
                        .plusMinutes(1)
                    !nowZoned.isBefore(expiryEnd)
                } ?: false
            }
            val tags = items.flatMap { it.tags }.distinct().toMutableList()
            if (expiredItemExists && !tags.contains(expiredTagLabel)) {
                tags.add(expiredTagLabel)
            }
            tags.sorted()
        }
    }

    LaunchedEffect(allTags.isEmpty()) {
        if (allTags.isEmpty()) {
            onTagFilterMissing?.invoke()
        }
    }
    
    // 根据选中的标签过滤物品（使用 derivedStateOf 优化性能）
    val filteredItems by remember(items, selectedTags, expiredTagLabel) {
        derivedStateOf {
            if (selectedTags.isEmpty()) {
                items
            } else {
                // 将 selectedTags 转换为 Set 以提高查找性能
                val selectedTagsSet = selectedTags.toSet()
                val includeExpired = selectedTagsSet.contains(expiredTagLabel)
                items.filter { item ->
                    val hasTagMatch = item.tags.any { tag -> selectedTagsSet.contains(tag) }
                    if (!includeExpired) {
                        hasTagMatch
                    } else {
                        val isExpired = item.expiryDate?.let { date ->
                            val zone = ZoneId.systemDefault()
                            val nowZoned = Instant.now().atZone(zone)
                            val expiryEnd = Instant.ofEpochMilli(date.time)
                                .atZone(zone)
                                .toLocalDate()
                                .plusDays(1)
                                .atStartOfDay(zone)
                                .plusMinutes(1)
                            !nowZoned.isBefore(expiryEnd)
                        } ?: false
                        hasTagMatch || isExpired
                    }
                }
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
        // 标签筛选栏（只在列表模式下显示在外部）
        if (displayMode == com.example.itemremindertool.config.ItemDisplayMode.LIST) {
            // 使用固定高度确保布局稳定，即使没有标签也保留空间
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (allTags.isNotEmpty()) 36.dp else 0.dp)
            ) {
                if (allTags.isNotEmpty()) {
                    val tagFilterModifier = if (onTagFilterPositioned != null) {
                        Modifier.onGloballyPositioned { onTagFilterPositioned(it) }
                    } else {
                        Modifier
                    }
                    TagFilterBar(
                        allTags = allTags,
                        selectedTags = selectedTags,
                        onTagSelected = { tag ->
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        },
                        modifier = tagFilterModifier
                    )
                }
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
            // 根据展示模式选择显示方式
            if (displayMode == com.example.itemremindertool.config.ItemDisplayMode.LIST) {
                // 列表模式（原有显示方式）
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
                        // 使用预先构建的映射查找容器名称，避免重复查找
                        val warehouseName = item.warehouseId?.let { warehouseNameMap[it] }
                        
                        ItemListRow(
                            item = item,
                            onClick = { onViewItem(item.id) },
                            onEditItem = onEditItem,
                            warehouseName = warehouseName, // 传递容器名称
                            useCircleIcon = useCircleIcon, // 传递侧边栏风格图标形状设置
                            useOutlineIcon = useOutlineIcon,
                            showMoveAction = canMoveItems,
                            onMoveToContainer = { movingItem ->
                                itemToMove = movingItem
                                showMoveDialog = true
                            },
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
            } else {
                // 网格模式（游戏背包样式）- 详细信息面板嵌入网格中
                
                // 配置变更期间显示加载状态，避免黑屏
                if (isConfigChanging) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // 准确计算网格列数（考虑所有padding和间距）
                val density = LocalDensity.current
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidthDp = configuration.screenWidthDp.dp
                val gridColumns = remember(screenWidthDp) {
                    // Surface padding: 12dp (左侧) + 0dp (右侧) = 12dp
                    // LazyVerticalGrid contentPadding: 12dp (左侧) + 0dp (右侧) = 12dp
                    // 总padding: 24dp
                    val totalPadding = 24.dp
                    val availableWidth = screenWidthDp - totalPadding
                    
                    // 卡片最小尺寸
                    val minCardSize = 72.dp
                    val spacing = 8.dp
                    
                    // 计算可以容纳的列数
                    var columns = 1
                    while (true) {
                        val nextColumns = columns + 1
                        val totalSpacing = spacing * (nextColumns - 1)
                        val requiredWidth = minCardSize * nextColumns + totalSpacing
                        if (requiredWidth <= availableWidth) {
                            columns = nextColumns
                        } else {
                            break
                        }
                    }
                    columns.coerceAtLeast(1)
                }
                
                // 使用 derivedStateOf 优化性能，减少不必要的重组
                // 添加 filteredItems 作为依赖，确保标签筛选变化时能正确更新
                val itemsWithDetail by remember(filteredItems, selectedItemId, gridColumns) {
                    derivedStateOf {
                        // 添加空列表检查，避免配置变更时的问题
                        if (filteredItems.isEmpty()) {
                            emptyList()
                        } else if (selectedItemId == null) {
                            filteredItems.map { it to false }
                        } else {
                            val selectedIndex = filteredItems.indexOfFirst { it.id == selectedItemId }
                            if (selectedIndex == -1) {
                                filteredItems.map { it to false }
                            } else {
                                val selected = filteredItems[selectedIndex]
                                // 计算选中物品所在行的最后一个位置
                                val rowEndIndex = ((selectedIndex / gridColumns) + 1) * gridColumns - 1
                                val insertIndex = (rowEndIndex + 1).coerceAtMost(filteredItems.size)
                                
                                buildList {
                                    filteredItems.forEachIndexed { index, item ->
                                        add(item to false)
                                        // 在这一行结束后插入详细信息面板
                                        if (index == insertIndex - 1 || (index == filteredItems.size - 1 && selectedIndex == filteredItems.size - 1)) {
                                            add(selected to true)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 确保内容已准备好，避免配置变更时渲染不完整的状态
                if (itemsWithDetail.isEmpty() && filteredItems.isNotEmpty()) {
                    // 数据正在准备中，显示加载指示器
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // 外围容器 - 与右上方子容器列表卡片样式一致
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 5.dp, top = 3.dp, bottom = 3.dp), // 右侧留出空间显示圆角
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorHelpers.getGroup3CardBgColor()
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 标签筛选栏（在网格模式下显示在 Card 内部顶部）
                            if (allTags.isNotEmpty()) {
                                val tagFilterModifier = if (onTagFilterPositioned != null) {
                                    Modifier.onGloballyPositioned { onTagFilterPositioned(it) }
                                } else {
                                    Modifier
                                }
                                TagFilterBar(
                                    allTags = allTags,
                                    selectedTags = selectedTags,
                                    onTagSelected = { tag ->
                                        selectedTags = if (selectedTags.contains(tag)) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    },
                                    modifier = tagFilterModifier.then(Modifier.padding(top = 8.dp))
                                )
                                
                                // 分割线
                                AppDivider(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                    color = ColorHelpers.getDividerColor(),
                                    thickness = 2.dp
                                )
                            }
                            
                            // 网格物品列表
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns), // 使用固定列数，确保与计算一致
                                state = gridState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .nestedScroll(object : NestedScrollConnection {
                                        override fun onPreScroll(
                                            available: Offset,
                                            source: NestedScrollSource
                                        ): Offset {
                                            return Offset.Zero
                                        }
                                    }),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(
                                    bottom = 12.dp,
                                    top = if (allTags.isNotEmpty()) 0.dp else 12.dp,
                                    start = 12.dp,
                                    end = 12.dp // 右侧留出空间显示圆角
                                )
                            ) {
                            val firstGridItemId = filteredItems.firstOrNull()?.id
                            items(
                                count = itemsWithDetail.size,
                                key = { index ->
                                    val (item, isDetail) = itemsWithDetail[index]
                                    if (isDetail) "detail_${item.id}" else "item_${item.id}"
                                },
                                span = { index ->
                                    val (_, isDetail) = itemsWithDetail[index]
                                    if (isDetail) {
                                        androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)
                                    } else {
                                        androidx.compose.foundation.lazy.grid.GridItemSpan(1)
                                    }
                                }
                            ) { index ->
                                val (item, isDetail) = itemsWithDetail[index]
                                
                                if (isDetail) {
                                    // 详细信息面板 - 嵌入网格中，居中显示并限制最大宽度
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        com.example.itemremindertool.ui.components.ItemGridDetailPanel(
                                            item = item,
                                            onQuantityChange = { newQuantity ->
                                                itemViewModel?.updateItem(item.copy(quantity = newQuantity))
                                                // 保持选中状态，ID不变
                                            },
                                            onUse = { useQuantity ->
                                                itemViewModel?.useItem(item, useQuantity)
                                                // 保持选中状态，ID不变
                                            },
                                            onViewDetails = {
                                                onViewItem(item.id)
                                            },
                                            onAddToShoppingCart = {
                                                shoppingItemViewModel?.let { vm ->
                                                    val shoppingItem = com.example.itemremindertool.data.model.ShoppingItem(
                                                        name = item.name,
                                                        description = item.description,
                                                        quantity = 1,
                                                        priority = com.example.itemremindertool.data.model.Priority.MEDIUM,
                                                        itemId = item.id
                                                    )
                                                    vm.insertShoppingItem(shoppingItem)
                                                }
                                            },
                                            onMoveToContainer = if (canMoveItems) {
                                                {
                                                    itemToMove = item
                                                    showMoveDialog = true
                                                }
                                            } else {
                                                null
                                            },
                                            onDelete = {
                                                if (onDeleteItem != {}) {
                                                    onDeleteItem(item)
                                                } else {
                                                    itemViewModel?.deleteItem(item)
                                                }
                                            },
                                            modifier = Modifier
                                                .widthIn(max = 500.dp)
                                                .then(
                                                    if (onInfoCardPositioned != null) {
                                                        Modifier.onGloballyPositioned { onInfoCardPositioned(it) }
                                                    } else {
                                                        Modifier
                                                    }
                                                ) // 限制最大宽度，平板上不会太宽
                                        )
                                    }
                                } else {
                                    // 普通物品卡片
                                    com.example.itemremindertool.ui.components.ItemGridCard(
                                        item = item,
                                        isSelected = selectedItemId == item.id,
                                        useOutlineIcon = useOutlineIcon,
                                        onClick = {
                                            val willSelect = selectedItemId != item.id
                                            selectedItemId = if (willSelect) {
                                                item.id
                                            } else {
                                                null
                                            }
                                            if (willSelect) {
                                                onOnboardingGridItemClick?.invoke()
                                            }
                                        },
                                        modifier = if (item.id == firstGridItemId && onGridItemPositioned != null) {
                                            Modifier.onGloballyPositioned { onGridItemPositioned(it) }
                                        } else {
                                            Modifier
                                        }
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

    if (showMoveDialog && itemToMove != null && warehouseViewModel != null) {
        MoveItemDialog(
            itemName = itemToMove!!.name,
            currentWarehouseId = itemToMove!!.warehouseId,
            warehouseViewModel = warehouseViewModel,
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
                showMoveDialog = false
                itemToMove = null
            }
        )
    }
}
}

/**
 * 侧边栏风格的主布局 - 整合左侧容器列和右侧内容区
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun SidebarStyleMainLayout(
    warehouses: List<Warehouse>,
    allWarehouses: List<Warehouse>,
    allItems: List<Item>,
    warehouseItemCounts: Map<Long, Int>,
    selectedWarehouseId: Long?,
    shoppingItemsCount: Int, // 待购物品数量
    alertSettingsManager: AlertSettingsManager, // 新增：提醒设置管理器
    shoppingItemViewModel: com.example.itemremindertool.ui.viewmodel.ShoppingItemViewModel? = null,
    itemViewModel: ItemViewModel? = null,
    itemReminderViewModel: com.example.itemremindertool.ui.viewmodel.ItemReminderViewModel? = null, // 物品提醒ViewModel
    activityEventViewModel: com.example.itemremindertool.ui.viewmodel.ActivityEventViewModel? = null, // 动态事件ViewModel
    warehouseViewModel: WarehouseViewModel? = null, // 新增：用于获取删除统计信息
    searchQuery: String = "", // 新增：搜索查询
    showSearchBox: Boolean = true,
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    adBottomPadding: Dp = 0.dp,
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
    onNavigateToWarehouseItemsTab: (Long) -> Unit = {}, // 导航到容器详情页面
    useCircleIcon: Boolean = true, // 新增：是否使用圆形图标
    useOutlineIcon: Boolean = false, // 新增：是否使用镂空图标
    onboardingEnabled: Boolean = false,
    onboardingStep: OnboardingStep = OnboardingStep.COMPLETE,
    onAdvanceOnboarding: () -> Unit = {},
    onSetOnboardingStep: (OnboardingStep) -> Unit = {},
    onUpdateOnboardingAnchor: (OnboardingAnchorKey, androidx.compose.ui.layout.LayoutCoordinates) -> Unit = { _, _ -> },
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
    // 直接基于参数计算，避免 derivedStateOf 误捕获导致不刷新
    val warehouseItems = if (selectedWarehouseId != null) {
        allItems.filter { it.warehouseId == selectedWarehouseId }
    } else {
        emptyList()
    }
    
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
    
    // 查看容器信息的回调（双击时导航到容器详情页面）
    val onViewWarehouseInfo: (Warehouse) -> Unit = { warehouse ->
        onNavigateToWarehouseItemsTab(warehouse.id)
    }
    
    val topPadding = contentPadding.calculateTopPadding()
    val bottomPadding = contentPadding.calculateBottomPadding()
    
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
                useCircleIcon = useCircleIcon,
                useOutlineIcon = useOutlineIcon,
                onFirstWarehousePositioned = { coords ->
                    onUpdateOnboardingAnchor(OnboardingAnchorKey.SIDEBAR_SAMPLE, coords)
                },
                onAddButtonPositioned = { coords ->
                    onUpdateOnboardingAnchor(OnboardingAnchorKey.SIDEBAR_ADD, coords)
                },
                modifier = Modifier.padding(
                    start = 6.dp,
                    end = 4.dp,
                    top = topPadding + 6.dp,
                    bottom = 0.dp + adBottomPadding
                )
            )
        
        // 右侧内容区 - 使用 Box 包装以支持下拉刷新
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(top = topPadding, bottom = bottomPadding + adBottomPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showSearchBox) {
                        SearchBoxSection(
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onCloseSearch = onCloseSearch,
                            modifier = Modifier
                                .padding(start = 6.dp, end = 5.dp, top = 3.dp, bottom = 3.dp)
                                .onGloballyPositioned {
                                    onUpdateOnboardingAnchor(OnboardingAnchorKey.SEARCH_BOX, it)
                                }
                        )
                    }
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
                                useOutlineIcon = useOutlineIcon,
                                warehouseViewModel = warehouseViewModel,
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

                    LaunchedEffect(onboardingEnabled, onboardingStep) {
                        if (!onboardingEnabled) return@LaunchedEffect
                        when (onboardingStep) {
                            OnboardingStep.HOME_STATS_CONTAINER_PAGE -> {
                                showContainerList = true
                                showItemList = false
                                showShoppingList = false
                            }
                            OnboardingStep.HOME_STATS_ITEM_PAGE -> {
                                showContainerList = false
                                showItemList = true
                                showShoppingList = false
                            }
                            OnboardingStep.HOME_STATS_SHOPPING_PAGE -> {
                                showContainerList = false
                                showItemList = false
                                showShoppingList = true
                            }
                            else -> Unit
                        }
                    }
                    
                    // 右上：统计卡片
                    HomeStatisticCards(
                        totalWarehouses = allContainers.size,
                        totalItems = allItems.size,
                        shoppingItemsCount = shoppingItemsCount,
                        isContainerSelected = showContainerList,
                        isItemSelected = showItemList,
                        isShoppingSelected = showShoppingList,
                        useCircleIcon = useCircleIcon,
                        useOutlineIcon = useOutlineIcon,
                        onContainerClick = {
                            if (onboardingEnabled && onboardingStep == OnboardingStep.HOME_STATS_CONTAINER_BUTTON) {
                                showContainerList = true
                                showItemList = false
                                showShoppingList = false
                                onSetOnboardingStep(OnboardingStep.HOME_STATS_CONTAINER_PAGE)
                            } else {
                                if (showContainerList) {
                                    showContainerList = false
                                    showItemList = false
                                    showShoppingList = false
                                } else {
                                    showContainerList = true
                                    showItemList = false
                                    showShoppingList = false
                                }
                            }
                        },
                        onItemClick = {
                            if (onboardingEnabled && onboardingStep == OnboardingStep.HOME_STATS_ITEM_BUTTON) {
                                showItemList = true
                                showContainerList = false
                                showShoppingList = false
                                onSetOnboardingStep(OnboardingStep.HOME_STATS_ITEM_PAGE)
                            } else {
                                if (showItemList) {
                                    showContainerList = false
                                    showItemList = false
                                    showShoppingList = false
                                } else {
                                    showItemList = true
                                    showContainerList = false
                                    showShoppingList = false
                                }
                            }
                        },
                        onShoppingClick = {
                            if (onboardingEnabled && onboardingStep == OnboardingStep.HOME_STATS_SHOPPING_BUTTON) {
                                showShoppingList = true
                                showContainerList = false
                                showItemList = false
                                onSetOnboardingStep(OnboardingStep.HOME_STATS_SHOPPING_PAGE)
                            } else {
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
                        },
                        onContainerPositioned = { coords ->
                            onUpdateOnboardingAnchor(OnboardingAnchorKey.STAT_CONTAINER, coords)
                        },
                        onItemPositioned = { coords ->
                            onUpdateOnboardingAnchor(OnboardingAnchorKey.STAT_ITEM, coords)
                        },
                        onShoppingPositioned = { coords ->
                            onUpdateOnboardingAnchor(OnboardingAnchorKey.STAT_SHOPPING, coords)
                        }
                    )
                    
                    // 右下：根据统计卡片点击展示对应列表
                    when {
                        showContainerList -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned {
                                        onUpdateOnboardingAnchor(OnboardingAnchorKey.STAT_PAGE_CONTAINER, it)
                                    }
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
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned {
                                        onUpdateOnboardingAnchor(OnboardingAnchorKey.STAT_PAGE_ITEM, it)
                                    }
                            ) {
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
                                    useOutlineIcon = useOutlineIcon,
                                    warehouseViewModel = warehouseViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        showShoppingList -> {
                            // 待购列表 - 增加底部 padding 避免被 FAB 挡住删除按钮
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned {
                                        onUpdateOnboardingAnchor(OnboardingAnchorKey.STAT_PAGE_SHOPPING, it)
                                    }
                            ) {
                                ShoppingListSection(
                                    shoppingItemViewModel = shoppingItemViewModel,
                                    itemViewModel = itemViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
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
                                    useCircleIcon = useCircleIcon,
                                    useOutlineIcon = useOutlineIcon,
                                    warehouseViewModel = warehouseViewModel,
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
                                useCircleIcon = useCircleIcon,
                                useOutlineIcon = useOutlineIcon,
                                modifier = Modifier.onGloballyPositioned {
                                    onUpdateOnboardingAnchor(OnboardingAnchorKey.SUBWAREHOUSE_ROW, it)
                                }
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
                            useOutlineIcon = useOutlineIcon,
                            warehouseViewModel = warehouseViewModel,
                            onTagFilterPositioned = { coords ->
                                onUpdateOnboardingAnchor(OnboardingAnchorKey.TAG_FILTER, coords)
                            },
                            onGridItemPositioned = { coords ->
                                onUpdateOnboardingAnchor(OnboardingAnchorKey.GRID_ITEM, coords)
                            },
                            onInfoCardPositioned = { coords ->
                                onUpdateOnboardingAnchor(OnboardingAnchorKey.INFO_CARD, coords)
                            },
                            onTagFilterMissing = {
                                if (onboardingEnabled && onboardingStep == OnboardingStep.WAREHOUSE_TAG_FILTER) {
                                    onSetOnboardingStep(OnboardingStep.WAREHOUSE_LAYOUT_TOGGLE)
                                }
                            },
                            onOnboardingGridItemClick = {
                                if (onboardingEnabled && onboardingStep == OnboardingStep.WAREHOUSE_GRID_ITEM) {
                                    onSetOnboardingStep(OnboardingStep.WAREHOUSE_INFO_CARD)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // 子容器删除确认对话框
        if (showDeleteDialog && warehouseToDelete != null) {
            val (childCount, itemCount) = deleteStatistics
            val parentWarehouse = warehouseToDelete!!.parentId?.let { parentId ->
                allContainers.find { it.id == parentId }
            }
            val parentName = parentWarehouse?.name ?: "父容器"
            
            AppDialogLayout(
                title = stringResource(R.string.confirm_delete),
                icon = Icons.Default.Delete,
                onDismiss = { 
                    showDeleteDialog = false
                    warehouseToDelete = null
                    isSubWarehouseDelete = false
                },
                footer = {
                    OutlinedButton(
                        onClick = { 
                            showDeleteDialog = false
                            warehouseToDelete = null
                            isSubWarehouseDelete = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val warehouse = warehouseToDelete!!
                            showDeleteDialog = false
                            warehouseToDelete = null
                            val isSubDelete = isSubWarehouseDelete
                            isSubWarehouseDelete = false
                            if (isSubDelete && warehouse.parentId != null) {
                                warehouseViewModel?.deleteSubWarehouse(warehouse)
                            } else {
                                onDeleteWarehouse(warehouse)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            ) {
                Text(
                    text = "\"${warehouseToDelete!!.name}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ColorHelpers.getGroup4TextColor()
                )
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
    isContainerSelected: Boolean = false,
    isItemSelected: Boolean = false,
    isShoppingSelected: Boolean = false,
    onContainerClick: (() -> Unit)? = null,
    onItemClick: (() -> Unit)? = null,
    onShoppingClick: () -> Unit = {},
    onContainerPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onItemPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onShoppingPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    useCircleIcon: Boolean = true,
    useOutlineIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 5.dp, top = 3.dp, bottom = 3.dp),
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
            // 容器数量 - 与左侧容器图标样式一致
            StatisticItem(
                icon = Icons.Default.Inventory2,
                value = totalWarehouses.toString(),
                label = stringResource(R.string.stat_label_warehouse),
                isSelected = isContainerSelected,
                onClick = onContainerClick,
                useCircleIcon = useCircleIcon,
                useOutlineIcon = useOutlineIcon,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onContainerPositioned != null) {
                            Modifier.onGloballyPositioned { onContainerPositioned(it) }
                        } else {
                            Modifier
                        }
                    )
            )
            
            // 分隔线
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(ColorHelpers.getDividerColor())
            )
            
            // 物品数量 - 与左侧容器图标样式一致
            StatisticItem(
                icon = Icons.Default.Category,
                value = totalItems.toString(),
                label = stringResource(R.string.stat_label_item),
                isSelected = isItemSelected,
                onClick = onItemClick,
                useCircleIcon = useCircleIcon,
                useOutlineIcon = useOutlineIcon,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onItemPositioned != null) {
                            Modifier.onGloballyPositioned { onItemPositioned(it) }
                        } else {
                            Modifier
                        }
                    )
            )

            // 分隔线
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(ColorHelpers.getDividerColor())
            )

            // 待购物品 - 与左侧容器图标样式一致（可点击）
            StatisticItem(
                icon = Icons.Default.ShoppingBag,
                value = shoppingItemsCount.toString(),
                label = stringResource(R.string.stat_label_shopping),
                isSelected = isShoppingSelected,
                onClick = onShoppingClick,
                useCircleIcon = useCircleIcon,
                useOutlineIcon = useOutlineIcon,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onShoppingPositioned != null) {
                            Modifier.onGloballyPositioned { onShoppingPositioned(it) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

/**
 * 单个统计项组件 - 与左侧容器图标样式一致
 */
@Composable
fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    useCircleIcon: Boolean = true,
    useOutlineIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    
    // 使用与左侧容器图标一致的主题色
    val outlineEnabled = useOutlineIcon
    val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
    val iconColor = if (outlineEnabled) {
        backgroundColor
    } else {
        ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
    }
    val iconShape = if (useCircleIcon) CircleShape else RoundedCornerShape(12.dp)
    
    Box(modifier = contentModifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 图标容器 - 与左侧容器图标样式一致
            Box(
                modifier = Modifier
                    .size(44.dp) // 与左侧容器图标大小一致
                    .then(
                        if (outlineEnabled) {
                            Modifier
                        } else {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = iconShape,
                                spotColor = Color.Black.copy(alpha = 0.3f),
                                ambientColor = Color.Black.copy(alpha = 0.15f)
                            )
                        }
                    )
                    .clip(iconShape)
                    .then(
                        if (outlineEnabled) {
                            Modifier.border(2.dp, backgroundColor, iconShape)
                        } else {
                            Modifier.background(backgroundColor) // 与左侧容器图标颜色一致
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor, // 根据背景颜色对比度自动切换
                    modifier = Modifier.size(22.dp) // 与左侧容器图标大小一致
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

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .width(44.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(backgroundColor)
            )
        }
    }
}

/**
 * 待购列表（右下）- 侧边栏风格首页
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
    val outlineEnabled = ColorHelpers.isOutlineEnabled()
    val actionBgColor = ColorHelpers.getGroup2SettingsBtnColor()
    val actionIconColor = if (outlineEnabled) actionBgColor else ColorHelpers.getGroup4IconColorByContrast(actionBgColor)
    val actionTextColor = if (outlineEnabled) actionBgColor else ColorHelpers.getGroup4TextColorByContrast(actionBgColor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
    ) {
        // 标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // 完成购买按钮（只在有已勾选的物品时显示）
            if (completedItems.isNotEmpty()) {
                if (outlineEnabled) {
                    OutlinedButton(
                        onClick = { showCompletePurchaseDialog = true },
                        border = BorderStroke(2.dp, actionBgColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = actionTextColor,
                            disabledContentColor = actionTextColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = actionIconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.complete_purchase),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Button(
                        onClick = { showCompletePurchaseDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionBgColor,
                            contentColor = actionTextColor,
                            disabledContainerColor = actionBgColor.copy(alpha = 0.5f),
                            disabledContentColor = actionTextColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = actionIconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.complete_purchase),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                            Text(stringResource(R.string.cancel), fontSize = 14.sp)
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fabBackground = ColorHelpers.getGroup2SettingsBtnColor()
            val deleteIconColor = fabBackground
            val nameColor = if (shoppingItem.isCompleted) {
                ColorHelpers.getGroup4TextColor(0.5f)
            } else {
                ColorHelpers.getGroup4TextColor()
            }
            val descColor = if (shoppingItem.isCompleted) {
                ColorHelpers.getGroup4TextColor(0.4f)
            } else {
                ColorHelpers.getGroup4TextColor(0.7f)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleComplete() }
            ) {
                val minNameFontSize = 12.sp
                var nameFontSize by remember(shoppingItem.name) { mutableStateOf(16.sp) }
                Text(
                    text = shoppingItem.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (shoppingItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = nameColor,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && nameFontSize > minNameFontSize) {
                            nameFontSize = (nameFontSize.value - 1f).sp
                        }
                    }
                )
                if (shoppingItem.description.isNotBlank()) {
                    Text(
                        text = shoppingItem.description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (shoppingItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = descColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quantityText = shoppingItem.quantity.toString()
                val quantityWidth = when (quantityText.length) {
                    0, 1 -> 20.dp
                    2 -> 26.dp
                    3 -> 32.dp
                    else -> 40.dp
                }
                IconButton(
                    onClick = { onQuantityChange(shoppingItem.quantity - 1) },
                    modifier = Modifier.size(22.dp),
                    enabled = !shoppingItem.isCompleted && shoppingItem.quantity > 1
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "减少数量",
                        modifier = Modifier.size(12.dp),
                        tint = if (shoppingItem.isCompleted || shoppingItem.quantity <= 1) {
                            ColorHelpers.getGroup4IconColor(0.3f)
                        } else {
                            ColorHelpers.getGroup4IconColor()
                        }
                    )
                }

                Text(
                    text = quantityText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    modifier = Modifier.widthIn(min = quantityWidth),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = { onQuantityChange(shoppingItem.quantity + 1) },
                    modifier = Modifier.size(22.dp),
                    enabled = !shoppingItem.isCompleted
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "增加数量",
                        modifier = Modifier.size(12.dp),
                        tint = if (shoppingItem.isCompleted) {
                            ColorHelpers.getGroup4IconColor(0.3f)
                        } else {
                            ColorHelpers.getGroup4IconColor()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(ColorHelpers.getGroup4TextColor().copy(alpha = 0.2f))
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = deleteIconColor
                    )
                }
            }
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
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
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
        // 左侧：时间线和图标（图标与标题对齐）
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // 整体竖线
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(color = iconColor.copy(alpha = 0.3f))
            )

            // 顶部留空（与右侧内容上边距对齐）
            val topOffset = 8.dp

            // 顶部遮盖（第一项不显示上半段）
            if (isFirst) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(topOffset + 18.dp)
                        .background(ColorHelpers.getGroup2PageBgColor())
                )
            }

            // 圆形图标（对齐标题行）
            Box(
                modifier = Modifier
                    .padding(top = topOffset)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ColorHelpers.getGroup2PageBgColor())
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

            // 底部遮盖（最后一项不显示下半段）
            if (isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .padding(top = topOffset + 36.dp)
                        .background(ColorHelpers.getGroup2PageBgColor())
                )
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

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(actionLabel, fontSize = 12.sp)
                }
            }
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
    var showForgetList by remember { mutableStateOf(false) }

    // 获取提醒设置
    val expiryReminderDays = remember { alertSettingsManager.getExpiryReminderDays() }
    val lowStockThreshold = remember { alertSettingsManager.getLowStockThreshold() }

    // 获取所有启用的自定义提醒
    val activeReminders = itemReminderViewModel?.allActiveReminders?.collectAsState(initial = emptyList())?.value ?: emptyList()
    
    // 获取所有动态事件
    val activityEvents = activityEventViewModel
        ?.recentEvents
        ?.collectAsState(initial = emptyList())
        ?.value
        ?.filterNot { it.type == com.example.itemremindertool.data.model.ActivityEventType.ITEM_VIEWED }
        ?: emptyList()
    val usedEvents = activityEventViewModel
        ?.getEventsByType(com.example.itemremindertool.data.model.ActivityEventType.ITEM_USED)
        ?.collectAsState(initial = emptyList())
        ?.value
        ?: emptyList()
    val viewedEvents = activityEventViewModel
        ?.getEventsByType(com.example.itemremindertool.data.model.ActivityEventType.ITEM_VIEWED)
        ?.collectAsState(initial = emptyList())
        ?.value
        ?: emptyList()

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { Instant.now().atZone(zone).toLocalDate() }
    val forgetInactiveDays = remember { alertSettingsManager.getForgetProtectionInactiveDays() }
    val forgetCandidates = remember(items, today, forgetInactiveDays, usedEvents, viewedEvents) {
        val cutoff = today.minusDays(forgetInactiveDays.toLong())
        val lastUsedByItem = mutableMapOf<Long, java.util.Date>()
        usedEvents.forEach { event ->
            val targetId = event.targetId ?: return@forEach
            val current = lastUsedByItem[targetId]
            if (current == null || event.createdAt.after(current)) {
                lastUsedByItem[targetId] = event.createdAt
            }
        }
        val lastViewedByItem = mutableMapOf<Long, java.util.Date>()
        viewedEvents.forEach { event ->
            val targetId = event.targetId ?: return@forEach
            val current = lastViewedByItem[targetId]
            if (current == null || event.createdAt.after(current)) {
                lastViewedByItem[targetId] = event.createdAt
            }
        }
        items.filter { item ->
            val lastUsedAt = lastUsedByItem[item.id] ?: item.updatedAt
            val lastViewedAt = lastViewedByItem[item.id] ?: item.createdAt
            val lastUsedDate = Instant.ofEpochMilli(lastUsedAt.time)
                .atZone(zone)
                .toLocalDate()
            val lastViewedDate = Instant.ofEpochMilli(lastViewedAt.time)
                .atZone(zone)
                .toLocalDate()
            !lastUsedDate.isAfter(cutoff) && !lastViewedDate.isAfter(cutoff)
        }.sortedBy { it.updatedAt.time }
    }
    
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
    val allTimeline = remember(expiringItems, lowStockItems, activeReminders, activityEvents, forgetCandidates, themeColor, expiringSoonTitle, lowStockTitle) {
        val expiringTimeline = mutableListOf<TimelineItem>()
        val otherTimeline = mutableListOf<TimelineItem>()
        
        // 添加动态事件（正常按时间排序）
        activityEvents.forEach { event ->
            val (icon, iconColor) = when (event.type) {
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_ADDED -> 
                    Icons.Default.Add to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_DELETED -> 
                    Icons.Default.Delete to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_UPDATED -> 
                    Icons.Default.Edit to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_USED -> 
                    Icons.Default.RemoveCircle to themeColor
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_VIEWED ->
                    Icons.Default.Visibility to themeColor
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
            
            // 动态生成本地化的标题和描述
            val localizedTitle = when (event.type) {
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_ADDED ->
                    context.getString(R.string.event_added_item)
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_DELETED ->
                    context.getString(R.string.event_deleted_item)
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_UPDATED ->
                    context.getString(R.string.event_updated_item)
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_USED ->
                    context.getString(R.string.event_used_item)
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_VIEWED ->
                    context.getString(R.string.event_viewed_item)
                com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_ADDED ->
                    context.getString(R.string.event_created_warehouse)
                com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_DELETED ->
                    context.getString(R.string.event_deleted_warehouse)
                com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_UPDATED ->
                    context.getString(R.string.event_updated_warehouse)
                com.example.itemremindertool.data.model.ActivityEventType.REMINDER_TRIGGERED ->
                    context.getString(R.string.event_reminder)
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_EXPIRING ->
                    context.getString(R.string.event_expiring_soon)
                com.example.itemremindertool.data.model.ActivityEventType.ITEM_LOW_STOCK ->
                    context.getString(R.string.event_low_stock)
            }
            
            // 描述保持使用 targetName（物品名或容器名），不需要翻译
            val localizedDescription = event.targetName.ifEmpty { event.description }
            
            otherTimeline.add(
                TimelineItem(
                    id = "event_${event.id}",
                    type = "event",
                    title = localizedTitle,
                    description = localizedDescription,
                    time = event.createdAt.time,
                    icon = icon,
                    iconColor = iconColor,
                    targetId = event.targetId
                )
            )
        }
        
        // 添加即将过期提醒（置顶显示）
        expiringItems.forEach { item ->
            val daysUntilExpiry = ((item.expiryDate!!.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            val description = context.getString(
                R.string.item_expiring_description,
                item.name,
                daysUntilExpiry
            )
            expiringTimeline.add(
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
        
        // 添加库存不足提醒（不置顶）
        lowStockItems.forEach { item ->
            val description = context.getString(
                R.string.item_low_stock_description,
                item.name,
                item.quantity
            )
            otherTimeline.add(
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
        
        // 添加自定义提醒（只显示一次性提醒且未过期的，不置顶）
        val currentTime = System.currentTimeMillis()
        activeReminders.forEach { reminder ->
            val item = items.find { it.id == reminder.itemId }
            if (item != null && reminder.reminderType == com.example.itemremindertool.data.model.ReminderType.ONCE) {
                val reminderTime = reminder.reminderTime?.time ?: currentTime
                
                // 只显示未过期的一次性提醒
                if (reminderTime >= currentTime) {
                    val typeStr = context.getString(R.string.reminder_type_once_display)
                    
                    otherTimeline.add(
                        TimelineItem(
                            id = "custom_${reminder.id}",
                            type = "custom",
                            title = typeStr,
                            description = "${item.name} - ${reminder.reason}",
                            time = reminderTime,
                            icon = Icons.Default.Alarm,
                            iconColor = themeColor,
                            targetId = item.id,
                            item = item,
                            reminder = reminder
                        )
                    )
                }
            }
        }

        // 添加防遗忘提醒入口（不置顶）
        if (alertSettingsManager.isForgetProtectionEnabled() && forgetCandidates.isNotEmpty()) {
            otherTimeline.add(
                TimelineItem(
                    id = "forget_summary",
                    type = "forget",
                    title = context.getString(R.string.forget_reminder_title),
                    description = context.getString(R.string.forget_reminder_summary, forgetCandidates.size),
                    time = forgetCandidates.maxOfOrNull { it.updatedAt.time } ?: System.currentTimeMillis(),
                    icon = Icons.Default.Timeline,
                    iconColor = themeColor
                )
            )
        }
        
        // 即将过期置顶（按到期时间升序），其他按时间降序
        val sortedExpiring = expiringTimeline.sortedBy { it.time }
        val sortedOthers = otherTimeline.sortedByDescending { it.time }
        sortedExpiring + sortedOthers
    }
    
    if (showForgetList) {
        ForgetReminderList(
            items = forgetCandidates,
            onBack = { showForgetList = false },
            onViewItem = onViewItem,
            modifier = modifier
        )
    } else if (allTimeline.isEmpty()) {
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
                        onClick = null, // 移除点击跳转功能
                        actionLabel = if (timelineItem.type == "forget") stringResource(R.string.view_list) else null,
                        onAction = if (timelineItem.type == "forget") ({ showForgetList = true }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgetReminderList(
    items: List<Item>,
    onBack: () -> Unit,
    onViewItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.forget_reminder_title),
                style = MaterialTheme.typography.titleMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back_to_timeline))
            }
        }

        AppDivider(color = ColorHelpers.getDividerColor(), thickness = 1.dp)

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_forget_items),
                    color = ColorHelpers.getGroup4TextColor(0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewItem(item.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = ColorHelpers.getGroup3CardBgColor()
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = stringResource(
                                    R.string.forget_item_last_used,
                                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(item.updatedAt)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
