package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.components.CameraCaptureDialog
import com.example.itemremindertool.ui.components.ImageCropDialog
import com.example.itemremindertool.ui.components.BarcodeScannerDialog
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import android.app.Activity
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.edit

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemEditScreen(
    itemUuid: String?,
    viewModel: ItemViewModel,
    categories: List<com.example.itemremindertool.data.model.Category>,
    warehouses: List<com.example.itemremindertool.data.model.Warehouse>,
    tagManager: com.example.itemremindertool.data.TagManager,
    onNavigateBack: () -> Unit,
    initialFeatureCode: String? = null, // 初始特征码（从识别页面传入）
    initialWarehouseUuid: String? = null, // 初始容器UUID（从容器页面传入）
    modifier: Modifier = Modifier
) {
    // ==================== 基础字段 ====================
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryUuid by remember { mutableStateOf<String?>(null) }
    var selectedWarehouseUuid by remember { mutableStateOf<String?>(initialWarehouseUuid) }
    var warehouseError by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf(setOf<String>()) }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var barcode by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var enableStockAlert by remember { mutableStateOf(false) } // 库存提醒开关，默认为false
    var imageUri by remember { mutableStateOf<String?>(null) } // 保留向后兼容
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) } // 多张图片路径列表
    var primaryImageIndex by remember { mutableStateOf(0) } // 主图索引
    var showCameraDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var cropImageIndex by remember { mutableStateOf<Int?>(null) } // 记录正在裁剪的图片索引，null表示是新图片
    // 获取 Context 和协程作用域
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
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

    // 从 ViewModel 获取待处理的特征码
    val pendingFeatureCode by viewModel.pendingFeatureCode.collectAsState()
    var featureCode by remember {
        mutableStateOf<String?>(
            initialFeatureCode ?: pendingFeatureCode
        )
    }

    // ==================== 标签输入专用状态 ====================
    var isTagInputFocused by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf("") }
    val tagFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ==================== 状态判断 ====================
    // 到期日结束后（次日00:01起）才算过期
    val isExpired = expiryDate?.let { date ->
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

    // ==================== 监听待处理特征码变化 ====================
    LaunchedEffect(pendingFeatureCode) {
        if (pendingFeatureCode != null && featureCode == null) {
            featureCode = pendingFeatureCode
            android.util.Log.d(
                "ItemEditScreen",
                "从 ViewModel 获取特征码: ${
                    featureCode?.substring(
                        0,
                        minOf(50, featureCode?.length ?: 0)
                    )
                }..."
            )
        }
    }

    // ==================== 加载已有物品数据 ====================
    LaunchedEffect(itemUuid) {
        if (itemUuid != null) {
            viewModel.loadItemByUuid(itemUuid)
        }
    }
    LaunchedEffect(isTagInputFocused) {
        if (isTagInputFocused) {
            delay(200) // 必须等布局完成
            tagFocusRequester.requestFocus()
            delay(100)
            keyboardController?.show()
        }
    }

    val selectedItem by viewModel.uiState.collectAsState()
    
    LaunchedEffect(itemUuid, selectedItem.selectedItem, initialWarehouseUuid) {
        if (itemUuid != null) {
            selectedItem.selectedItem?.let { item ->
                name = item.name
                description = item.description
                selectedCategoryUuid = item.categoryUuid
                selectedWarehouseUuid = item.warehouseUuid
                tags = item.tags.toSet()
                price = item.price?.toString() ?: ""
                quantity = item.quantity.toString()
                barcode = item.barcode ?: ""
                expiryDate = item.expiryDate
                enableStockAlert = item.enableStockAlert
                // 加载多图：如果有imageUris则使用，否则兼容旧的imageUri
                // 同时检查图片文件是否存在，只加载存在的图片
                imageUris = if (item.imageUris.isNotEmpty()) {
                    item.imageUris.filter { path ->
                        java.io.File(path).exists()
                    }
                } else {
                    item.imageUri?.let { path ->
                        if (java.io.File(path).exists()) {
                            listOf(path)
                        } else {
                            emptyList()
                        }
                    } ?: emptyList()
                }
                // 根据实际加载的图片列表设置imageUri
                imageUri = imageUris.getOrNull(primaryImageIndex)
                // 安全设置主图索引：如果列表为空，设置为0；否则限制在有效范围内
                primaryImageIndex = if (imageUris.isEmpty()) {
                    0
                } else {
                    item.primaryImageIndex.coerceIn(0, imageUris.size - 1)
                }
                featureCode = item.featureCode
                // 初始化状态
                // val statusInTags = item.tags.firstOrNull { it in listOf("正常", "损坏", "遗失") }
                // selectedStatus = when (statusInTags) {
                //    "正常" -> ItemStatus.NORMAL
                //    "损坏" -> ItemStatus.DAMAGED
                //    "遗失" -> ItemStatus.LOST
                //    else -> ItemStatus.NORMAL
                //}
            }
        } else {
            // 新建模式：设置初始容器
            if (initialWarehouseUuid != null && selectedWarehouseUuid == null) {
                selectedWarehouseUuid = initialWarehouseUuid
            }
        }
    }

    // 确保在新建模式下，初始容器UUID正确设置
    LaunchedEffect(initialWarehouseUuid) {
        if (itemUuid == null && initialWarehouseUuid != null) {
            selectedWarehouseUuid = initialWarehouseUuid
        }
    }

    // ==================== Scaffold ====================
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = {
                    Text(
                        if (itemUuid == null) stringResource(R.string.add_item) else stringResource(
                            R.string.edit_item
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // 获取与返回按钮一致的颜色
                    val gradientStartColor = ColorHelpers.getTopBarGradientStart()
                    val contrastColor = ColorHelpers.getGroup4TextColorByContrast(gradientStartColor)
                    
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = contrastColor
                        ),
                        onClick = {
                            if (selectedWarehouseUuid == null) {
                                warehouseError = true
                                return@TextButton
                            } else {
                                warehouseError = false
                            }
                            val categoryUuid = selectedCategoryUuid
                            val warehouseUuid = selectedWarehouseUuid
                            
                            val isAdd = itemUuid == null
                            val item = Item(
                                uuid = if (isAdd) UUID.randomUUID().toString() else (selectedItem.selectedItem?.uuid ?: UUID.randomUUID().toString()),
                                name = name,
                                description = description,
                                categoryUuid = categoryUuid,
                                warehouseUuid = warehouseUuid,
                                tags = tags.toList(),
                                price = price.toDoubleOrNull(),
                                quantity = quantity.toIntOrNull() ?: 1,
                                barcode = barcode.ifEmpty { null },
                                expiryDate = expiryDate,
                                imageUri = if (imageUris.isNotEmpty()) {
                                    imageUris.getOrNull(primaryImageIndex)
                                } else {
                                    null // 如果没有图片，清空imageUri
                                },
                                imageUris = imageUris,
                                primaryImageIndex = if (imageUris.isNotEmpty()) {
                                    primaryImageIndex.coerceIn(0, imageUris.size - 1)
                                } else {
                                    0
                                },
                                featureCode = featureCode,
                                enableStockAlert = enableStockAlert,
                                isSample = if (isAdd) false else (selectedItem.selectedItem?.isSample ?: false),
                                createdAt = if (isAdd) Date() else (selectedItem.selectedItem?.createdAt ?: Date()),
                                updatedAt = Date()
                            )

                            // 检查是否需要添加到购物篮（仅在新建物品时）
                            val prefs = context.getSharedPreferences(
                                "app_settings",
                                android.content.Context.MODE_PRIVATE
                            )
                            val shouldAddToShoppingList = itemUuid == null && prefs.getBoolean(
                                "add_to_shopping_list_after_save",
                                false
                            )

                            // 保存物品
                            if (itemUuid == null) {
                                viewModel.insertItem(item) { savedItemUuid ->
                                    if (shouldAddToShoppingList) {
                                        scope.launch(Dispatchers.IO) {
                                            prefs.edit().putBoolean(
                                                "add_to_shopping_list_after_save",
                                                false
                                            ).apply()
                                            val database =
                                                com.example.itemremindertool.data.database.AppDatabase.getDatabase(
                                                    context
                                                )
                                            val savedItem =
                                                database.itemDao().getItemByUuid(savedItemUuid)
                                            savedItem?.let { saved ->
                                                val shoppingItem =
                                                    com.example.itemremindertool.data.model.ShoppingItem(
                                                        name = saved.name,
                                                        description = saved.description,
                                                        quantity = saved.quantity,
                                                        priority = com.example.itemremindertool.data.model.Priority.MEDIUM,
                                                        itemUuid = saved.uuid,
                                                        imageUri = saved.imageUri
                                                    )
                                                database.shoppingItemDao()
                                                    .insertShoppingItem(shoppingItem)
                                            }
                                        }
                                    }
                                }
                            } else {
                                viewModel.updateItem(item)
                            }
                            // 直接返回，导航栈会自动返回到打开前的页面
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .imePadding() // 添加键盘避让，确保输入框不被键盘遮挡
                .clickable {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 物品名称 ====================
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.item_name_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ==================== 物品图片 ====================
            val context = LocalContext.current
            val density = LocalDensity.current
            val cardWidthPx = remember { with(density) { 400.dp.toPx().toInt() } }
            val cardHeightPx = remember { with(density) { 400.dp.toPx().toInt() } } // 改为正方形

            // 从相册选择单张图片的启动器（用于裁剪）
            val prefs = remember { 
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) 
            }
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                // 清除ActivityResult处理标记
                prefs.edit { putBoolean("is_processing_activity_result", false) }
                
                if (uri != null) {
                    // 处理选中的图片
                    try {
                        val bitmap = ImageUtils.loadBitmapFromUri(context, uri)

                        if (bitmap != null) {
                            // 显示裁剪对话框
                            bitmapToCrop = bitmap
                            cropImageIndex = null // 新图片，索引为null
                            showCropDialog = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 裁剪图片为主图的函数（保存为单独的文件）
            // 单独定义函数，避免 remember 块嵌套导致类型推断错误
            val cropImageForPrimary: (String) -> Unit = { imagePath ->
                scope.launch(Dispatchers.IO) {
                    val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                    if (bitmap != null) {
                        val croppedBitmap = ImageUtils.cropImageToCardSize(
                            bitmap,
                            cardWidthPx,
                            cardHeightPx
                        )
                        // 保存为单独的裁剪文件
                        val croppedPath = ImageUtils.getCroppedImagePath(imagePath)
                        val croppedFileName = croppedPath?.let { File(it).name } ?: "cropped_${File(imagePath).name}"
                        ImageUtils.saveImageToInternalStorage(
                            context,
                            croppedBitmap,
                            croppedFileName
                        )
                    }
                }
            }

            // 切换主图时，只更新索引，不再自动裁剪（因为图片已经处理过了）
            val switchPrimaryImage: (Int) -> Unit = { newIndex ->
                // 切换主图时不再自动裁剪，保持原有图片
                // 如果用户需要裁剪，可以通过点击编辑按钮手动处理
            }

            // 从相册选择多张图片的启动器（弹出裁剪对话框）
            val multipleImagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia()
            ) { uris: List<Uri> ->
                // 清除ActivityResult处理标记
                prefs.edit { putBoolean("is_processing_activity_result", false) }
                
                // 选择第一张图片弹出裁剪对话框
                if (uris.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val uri = uris[0]
                            val bitmap = ImageUtils.loadBitmapFromUri(context, uri)

                            if (bitmap != null) {
                                // 显示裁剪对话框
                                bitmapToCrop = bitmap
                                cropImageIndex = null // 新图片，索引为null
                                showCropDialog = true
                            }
                            
                            // 处理剩余的图片（如果有）
                            if (uris.size > 1) {
                                uris.drop(1).forEach { remainingUri ->
                                    try {
                                        val remainingBitmap = ImageUtils.loadBitmapFromUri(context, remainingUri)

                                        if (remainingBitmap != null) {
                                            val fileName =
                                                "item_${itemUuid ?: System.currentTimeMillis()}_${System.currentTimeMillis()}.jpg"
                                            val savedPath = ImageUtils.saveImageToInternalStorage(
                                                context,
                                                remainingBitmap,
                                                fileName
                                            )
                                            savedPath?.let {
                                                imageUris = imageUris + it
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            // 设置拍照和相册的按钮样式
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val outlineEnabled = ColorHelpers.isOutlineEnabled()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                            val iconColor = if (outlineEnabled) backgroundColor else ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
                            if (outlineEnabled) {
                                OutlinedButton(
                                    onClick = { showCameraDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(2.dp, backgroundColor),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = backgroundColor,
                                        disabledContentColor = backgroundColor.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = iconColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Text(stringResource(R.string.take_photo), color = backgroundColor)
                                }
                            } else {
                                Button(
                                    onClick = { showCameraDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = backgroundColor
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = iconColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Text(stringResource(R.string.take_photo), color = ColorHelpers.getGroup4TextColorByContrast(backgroundColor))
                                }
                            }
                            val galleryBackground = ColorHelpers.getGroup2SettingsBtnColor()
                            val galleryIconColor = if (outlineEnabled) galleryBackground else ColorHelpers.getGroup4IconColorByContrast(galleryBackground)
                            if (outlineEnabled) {
                                OutlinedButton(
                                    onClick = {
                                        // 设置ActivityResult处理标记，防止密码锁屏
                                        prefs.edit { putBoolean("is_processing_activity_result", true) }
                                        multipleImagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(2.dp, galleryBackground),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = galleryBackground,
                                        disabledContentColor = galleryBackground.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = galleryIconColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Text(stringResource(R.string.select_multiple), color = galleryBackground)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        // 设置ActivityResult处理标记，防止密码锁屏
                                        prefs.edit { putBoolean("is_processing_activity_result", true) }
                                        multipleImagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = galleryBackground
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = galleryIconColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Text(stringResource(R.string.select_multiple), color = ColorHelpers.getGroup4TextColorByContrast(galleryBackground))
                                }
                            }
                        }

                        // 显示图片列表和主图选择
                        if (imageUris.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.image_list_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorHelpers.getGroup4TextColor(0.7f)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(imageUris) { index, imagePath ->
                                        Box(modifier = Modifier.size(80.dp)) {
                                            // 主图显示裁剪图，非主图显示原图
                                            val displayPath = if (index == primaryImageIndex) {
                                                val croppedPath =
                                                    ImageUtils.getCroppedImagePath(imagePath)
                                                if (croppedPath != null) {
                                                    val croppedFile = File(croppedPath)
                                                    if (croppedFile.exists()) croppedPath else imagePath
                                                } else {
                                                    imagePath
                                                }
                                            } else {
                                                imagePath // 非主图显示原图
                                            }

                                            val bitmap = remember(displayPath) {
                                                ImageUtils.loadBitmapFromPath(displayPath)
                                            }
                                            if (bitmap != null) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable {
                                                            // 点击图片卡片时，设置为主图
                                                            val oldPrimaryIndex = primaryImageIndex
                                                            if (oldPrimaryIndex != index) {
                                                                primaryImageIndex = index
                                                                // 切换主图：删除旧的裁剪文件，创建新的裁剪文件
                                                                switchPrimaryImage(index)
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = if (index == primaryImageIndex) {
                                                        BorderStroke(
                                                            3.dp,
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        null
                                                    }
                                                ) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                            
                                            // 编辑按钮（左下角）- 弹出裁剪对话框
                                            IconButton(
                                                onClick = {
                                                    // 点击编辑按钮时，弹出裁剪对话框进行手动处理
                                                    val imageBitmap = ImageUtils.loadBitmapFromPath(imagePath)
                                                    if (imageBitmap != null) {
                                                        bitmapToCrop = imageBitmap
                                                        cropImageIndex = index // 记录正在裁剪的图片索引
                                                        showCropDialog = true
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "编辑图片",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            // 删除按钮（右上角）
                                            IconButton(
                                                onClick = {
                                                    scope.launch(Dispatchers.IO) {
                                                        // 删除原图和裁剪图
                                                        ImageUtils.deleteImageAndCropped(imagePath)
                                                    }
                                                    imageUris =
                                                        imageUris.filterIndexed { i, _ -> i != index }
                                                    // 如果删除后没有图片了，清空imageUri
                                                    if (imageUris.isEmpty()) {
                                                        imageUri = null
                                                    }
                                                    if (primaryImageIndex >= imageUris.size - 1) {
                                                        primaryImageIndex =
                                                            maxOf(0, imageUris.size - 1)
                                                        // 如果删除的是主图，切换新主图（不再自动裁剪）
                                                        // switchPrimaryImage现在不再自动裁剪，保持原有图片
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "删除",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

        if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
            PremiumFeatureDialog(
                billingManager = billingManager,
                onDismiss = { showPremiumFeatureDialog = false }
            )
        }
                            }
                        }
                    }

                    // ==================== 描述 ====================
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    // ==================== 容器选择 ====================
                    var expandedWarehouse by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedWarehouse,
                        onExpandedChange = { expandedWarehouse = !expandedWarehouse }
                    ) {
                        OutlinedTextField(
                            value = warehouses.find { it.uuid == selectedWarehouseUuid }?.name ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.warehouse)) },
                            isError = warehouseError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWarehouse) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWarehouse,
                            onDismissRequest = { expandedWarehouse = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        ) {
                            warehouses.forEach { warehouse ->
                                DropdownMenuItem(
                                    text = { Text(warehouse.name) },
                                    onClick = {
                                        selectedWarehouseUuid = warehouse.uuid
                                        warehouseError = false
                                        expandedWarehouse = false
                                    }
                                )
                            }
                        }
                    }
                    if (warehouseError) {
                        Text(
                            text = stringResource(R.string.warehouse_required),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // ==================== 标签 + 自定义标签（核心改动）===================
//                    Text(
//                        stringResource(R.string.status_tags),
//                        color = MaterialTheme.colorScheme.onSurface,
//                        style = MaterialTheme.typography.labelLarge
//                    )

    // 获取所有已创建的标签 + 物品中已存在的标签（恢复后 TagManager 可能为空）
    val allTags by tagManager.allTags.collectAsState()
    val items by viewModel.items.collectAsState(initial = emptyList())
    val itemTags = remember(items) {
        items.flatMap { it.tags }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val customTags = remember(allTags, itemTags) {
        (allTags + itemTags)
            .filter { it != "过期" && it.isNotBlank() }
            .distinct()
            .sorted()
    }

                    // 自动弹出键盘和聚焦
                    LaunchedEffect(isTagInputFocused) {
                        if (isTagInputFocused) {
                            // 等待UI完全组合
                            delay(100)
                            // 尝试聚焦，可能需要多次尝试
                            var retries = 0
                            while (retries < 3) {
                                try {
                                    tagFocusRequester.requestFocus()
                                    delay(150)
                                    keyboardController?.show()
                                    break
                                } catch (e: Exception) {
                                    retries++
                                    delay(100)
                                }
                            }
                        }
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. 默认标签已移除，只保留过期标签的自动显示


                        // 2. 过期标签（自动显示）
                        if (isExpired) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { },
                                    enabled = false,
                                    label = {
                                        Text(
                                            stringResource(R.string.expired_tag),
                                            color = ColorHelpers.getGroup4TextColor()
                                        )
                                    },
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = false,
                                        selected = true,
                                        borderColor = Color(0xFFF3E5F5).copy(alpha = 0.8f),
                                        selectedBorderColor = Color(0xFFF3E5F5).copy(alpha = 0.8f),
                                        borderWidth = 1.25.dp,
                                        selectedBorderWidth = 1.25.dp
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = ColorHelpers.getGroup4TextColor(),
                                        containerColor = Color.Transparent,
                                        labelColor = ColorHelpers.getGroup4TextColor()
                                    )
                                )
                            }
                        }

                        // 3. 所有已创建的自定义标签（供选择，支持多选，选中高亮60%黄色，带删除按钮，保持边框）
                        items(customTags.toList()) { tag ->
                            val isSelected = tags.contains(tag)
                            if (isSelected) {
                                // 选中状态：显示带删除按钮的 FilterChip，透明背景，主色边框
                                // 使用 Row 包裹 FilterChip 和 X 图标，让 X 图标可以单独点击
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = true,
                                        onClick = { tags = tags - tag }, // 点击标签文本时取消选中
                                        enabled = true,
                                        label = { Text(tag) },
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = true,
                                            borderColor = MaterialTheme.colorScheme.primary,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.25.dp,
                                            selectedBorderWidth = 2.25.dp
                                        ),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color.Transparent,
                                            selectedLabelColor = ColorHelpers.getGroup4TextColor(),
                                            containerColor = Color.Transparent,
                                            labelColor = ColorHelpers.getGroup4TextColor()
                                        )
                                    )
                                    // X 图标单独可点击，点击时删除标签
                                    IconButton(
                                        onClick = {
                                            // 从标签管理器中删除标签
                                            tagManager.removeTag(tag)
                                            // 从当前选中标签中移除
                                            tags = tags - tag
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(16.dp),
                                            tint = ColorHelpers.getGroup4IconColor()
                                        )
                                    }
                                }
                            } else {
                                // 未选中状态：显示 FilterChip，透明背景，轮廓色边框
                                FilterChip(
                                    selected = false,
                                    onClick = { tags = tags + tag },
                                    enabled = true,
                                    label = { Text(tag) },
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 1f),
                                        selectedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.7f
                                        ),
                                        borderWidth = 1.25.dp,
                                        selectedBorderWidth = 1.25.dp
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Transparent,
                                        labelColor = ColorHelpers.getGroup4TextColor()
                                    )
                                )
                            }
                        }

                        // 4. 「+ 添加标签」虚线按钮 / 输入框（永远在最后一项）
                        item {
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .widthIn(
                                        min = if (isTagInputFocused) 100.dp else 108.dp,
                                        max = 100.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isTagInputFocused)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else
                                            Color.Transparent
                                    )
                                    .border(
                                        width = 1.25.dp,
                                        color = if (isTagInputFocused)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        enabled = !isTagInputFocused,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { isTagInputFocused = true }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = tagInputText,
                                    onValueChange = { if (it.length <= 12) tagInputText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .focusRequester(tagFocusRequester)
                                        .onFocusChanged { focusState ->
                                            // 失焦自动保存
                                            if (!focusState.isFocused && isTagInputFocused) {
                                                val text = tagInputText.trim()
                                                if (text.isNotEmpty() && text !in tags && text != "过期") {
                                                    val isExisting = tagManager.getAllTags().contains(text)
                                                    if (!canAccessPremiumFeatures && tagManager.isTagLimitReached() && !isExisting) {
                                                        showPremiumFeatureDialog = true
                                                    } else if (tagManager.addTag(text)) {
                                                        // 自动选中新添加的标签
                                                        tags = tags + text
                                                    }
                                                }
                                                tagInputText = ""
                                                isTagInputFocused = false
                                            }
                                        },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val text = tagInputText.trim()
                                        if (text.isNotEmpty() && text !in tags && text != "过期") {
                                            val isExisting = tagManager.getAllTags().contains(text)
                                            if (!canAccessPremiumFeatures && tagManager.isTagLimitReached() && !isExisting) {
                                                showPremiumFeatureDialog = true
                                            } else if (tagManager.addTag(text)) {
                                                // 自动选中新添加的标签
                                                tags = tags + text
                                            }
                                        }
                                        tagInputText = ""
                                        isTagInputFocused = false
                                        keyboardController?.hide()
                                    })
                                ) { innerTextField ->
                                    // 关键：用 Box 叠层布局，确保垂直居中
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 1. 非编辑状态：显示 "+ 添加标签"
                                        if (!isTagInputFocused) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .fillMaxSize()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        isTagInputFocused = true
                                                    },
                                                verticalAlignment = Alignment.CenterVertically,
//                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(0.dp))
                                                Text(
                                                    text = stringResource(R.string.add_tag),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // 2. 编辑状态：显示占位文字（当输入为空时）
                                        if (isTagInputFocused && tagInputText.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.enter_tag_hint),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.7f
                                                    ),
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(start = 2.dp)
                                                )
                                            }
                                        }

                                        // 3. 真正的输入内容（永远在最上层，垂直居中）
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 价格 & 数量 ====================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text(stringResource(R.string.price)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Text("¥") }
                        )

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text(stringResource(R.string.quantity)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // ==================== 库存提醒开关 ====================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorHelpers.getGroup2PageBgColor() // 使用Background背景
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.enable_stock_alert),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.enable_stock_alert_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = enableStockAlert,
                                onCheckedChange = { enableStockAlert = it }
                            )
                        }
                    }

                    // ==================== 条形码 ====================
                    var showBarcodeScanner by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text(stringResource(R.string.barcode)) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.QrCode, null) }
                        )
                        IconButton(
                            onClick = { showBarcodeScanner = true }
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.scan_barcode),
                                tint = ColorHelpers.getGroup2SettingsBtnColor()
                            )
                        }
                    }
                    
                    // 条形码扫描对话框
                    if (showBarcodeScanner) {
                        BarcodeScannerDialog(
                            onBarcodeScanned = { scannedBarcode ->
                                barcode = scannedBarcode
                                showBarcodeScanner = false
                            },
                            onDismiss = { showBarcodeScanner = false }
                        )
                    }

                    // ==================== 到期日期 ====================
                    val dateFormat = remember {
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                    }
                    OutlinedTextField(
                        value = expiryDate?.let {
                            dateFormat.format(it)
                        } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.expiry_date)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showDatePicker = true
                            },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showDatePicker = true
                            }) {
                                Icon(Icons.Default.DateRange, null)
                            }
                        }
                    )

                    // ==================== 日期选择器 - 使用 Material3 DatePicker ====================
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = expiryDate?.time
                    )
                    LaunchedEffect(showDatePicker, expiryDate) {
                        if (showDatePicker) {
                            datePickerState.selectedDateMillis =
                                expiryDate?.time ?: System.currentTimeMillis()
                        }
                    }
                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val selectedMillis = datePickerState.selectedDateMillis
                                    if (selectedMillis != null) {
                                        val zone = ZoneId.systemDefault()
                                        val localDate =
                                            Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate()
                                        val startOfDay =
                                            localDate.atStartOfDay(zone).toInstant().toEpochMilli()
                                        expiryDate = Date(startOfDay)
                                    }
                                    showDatePicker = false
                                }) {
                                    Text(stringResource(R.string.confirm_button))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        ) {
                            DatePicker(
                                state = datePickerState,
                                showModeToggle = false,
                                modifier = Modifier
                                    .graphicsLayer(scaleX = 0.9f, scaleY = 0.9f)
                            )
                        }
                    }

                    // ==================== 拍照对话框 ====================
                    if (showCameraDialog) {
                        CameraCaptureDialog(
                            onImageCaptured = { imagePath ->
                                showCameraDialog = false
                                if (imagePath != null) {
                                    scope.launch(Dispatchers.IO) {
                                        // 加载图片，弹出裁剪对话框
                                        val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                                        if (bitmap != null) {
                                            // 显示裁剪对话框
                                            bitmapToCrop = bitmap
                                            cropImageIndex = null // 新图片，索引为null
                                            showCropDialog = true
                                            // 删除原始临时文件
                                            ImageUtils.deleteImageFile(imagePath)
                                        }
                                    }
                                }
                            },
                            onDismiss = { showCameraDialog = false },
                            cardWidth = cardWidthPx,
                            cardHeight = cardHeightPx
                        )
                    }

                    // 相册选择单张图片后的裁剪对话框（用于手动选择单张图片的情况）
                    if (showCropDialog && bitmapToCrop != null) {
                        ImageCropDialog(
                            bitmap = bitmapToCrop!!,
                            onCropped = { croppedBitmap ->
                                showCropDialog = false
                                val currentCropIndex = cropImageIndex
                                scope.launch(Dispatchers.IO) {
                                    if (currentCropIndex != null && currentCropIndex < imageUris.size) {
                                        // 更新已存在的图片
                                        val oldPath = imageUris[currentCropIndex]
                                        // 删除旧图片和裁剪图
                                        ImageUtils.deleteImageAndCropped(oldPath)
                                        // 保存新图片
                                        val fileName =
                                            "item_${itemUuid ?: System.currentTimeMillis()}_${System.currentTimeMillis()}.jpg"
                                        val savedPath = ImageUtils.saveImageToInternalStorage(
                                            context,
                                            croppedBitmap,
                                            fileName
                                        )
                                        savedPath?.let {
                                            // 更新图片列表
                                            imageUris = imageUris.toMutableList().apply {
                                                set(currentCropIndex, it)
                                            }
                                            // 图片已经通过裁剪对话框处理过了，不再自动裁剪
                                            if (currentCropIndex == 0) {
                                                imageUri = it // 向后兼容
                                            }
                                        }
                                    } else {
                                        // 添加新图片
                                        val fileName =
                                            "item_${itemUuid ?: System.currentTimeMillis()}_${System.currentTimeMillis()}.jpg"
                                        val savedPath = ImageUtils.saveImageToInternalStorage(
                                            context,
                                            croppedBitmap,
                                            fileName
                                        )
                                        savedPath?.let {
                                            imageUris = imageUris + it
                                            if (imageUris.size == 1) {
                                                primaryImageIndex = 0
                                                // 第一张图片自动设为主图，但图片已经通过裁剪对话框处理过了，不再自动裁剪
                                            }
                                            imageUri = it // 向后兼容
                                        }
                                    }
                                }
                                bitmapToCrop = null
                                cropImageIndex = null
                            },
                            onDismiss = {
                                showCropDialog = false
                                bitmapToCrop = null
                                cropImageIndex = null
                            },
                            cardWidth = cardWidthPx,
                            cardHeight = cardHeightPx
                        )
                    }
                }
            }
        }


// ==================== 辅助函数 ====================


// 安全获取宿主 Activity，避免在 Dialog 上下文中拿不到 Activity
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ==================== 自定义虚线 Modifier ====================
fun Modifier.dashedBorder(
    strokeWidth: Dp,
    color: Color,
    dashLength: Dp,
    gapLength: Dp,
    cornerRadius: Dp
): Modifier {
    return this.then(
        Modifier.drawBehind {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                    0f
                )
            )
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                style = stroke
            )
        }
    )
}