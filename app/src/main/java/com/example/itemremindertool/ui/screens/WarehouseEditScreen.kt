package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.CameraCaptureDialog
import com.example.itemremindertool.ui.components.ImageCropDialog
import com.example.itemremindertool.ui.components.IconSelectionDialog
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.R
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseEditScreen(
    warehouseUuid: String?,
    viewModel: WarehouseViewModel,
    onNavigateBack: () -> Unit,
    initialParentUuid: String? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var selectedParentUuid by remember { mutableStateOf<String?>(initialParentUuid) }
    var showParentDropdown by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showIconSelectionDialog by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var pendingImageExt by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialParentUuid) {
        if (warehouseUuid == null) {
            selectedParentUuid = initialParentUuid
        }
    }

    val allWarehouses by viewModel.warehouses.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    // 容器图片使用1:1正方形裁剪
    val cardSizePx = remember { with(density) { 400.dp.toPx().toInt() } }
    val cardWidthPx = cardSizePx
    val cardHeightPx = cardSizePx
    
    // 从相册选择单张图片的启动器（用于裁剪）
    val prefs = remember { 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) 
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // 清除ActivityResult处理标记
        prefs.edit().putBoolean("is_processing_activity_result", false).apply()
        
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    pendingImageExt = ImageUtils.normalizeImageExtension(
                        ImageUtils.getImageExtensionFromUri(context, uri)
                    )
                    val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
                    
                    if (bitmap != null) {
                        bitmapToCrop = bitmap
                        showCropDialog = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // 裁剪图片的函数
    val cropImageForWarehouse: (String) -> Unit = { imagePath ->
        scope.launch(Dispatchers.IO) {
            val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
            if (bitmap != null) {
                val croppedBitmap = ImageUtils.cropImageToCardSize(
                    bitmap,
                    cardWidthPx,
                    cardHeightPx
                )
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

    fun getAllChildUuids(parentUuid: String, warehouses: List<Warehouse>): Set<String> {
        val direct = warehouses.filter { it.parentUuid == parentUuid }
        val uuids = direct.map { it.uuid }.toMutableSet()
        direct.forEach { uuids.addAll(getAllChildUuids(it.uuid, warehouses)) }
        return uuids
    }

    val availableParents = remember(allWarehouses, warehouseUuid) {
        if (warehouseUuid == null) {
            allWarehouses.filter { it.level < 5 }
        } else {
            val excluded = mutableSetOf(warehouseUuid)
            excluded.addAll(getAllChildUuids(warehouseUuid, allWarehouses))
            allWarehouses.filter { it.uuid !in excluded && it.level < 5 }
        }
    }

    LaunchedEffect(warehouseUuid) {
        if (warehouseUuid != null) {
            viewModel.loadWarehouse(warehouseUuid)
        } else {
            name = ""
            description = ""
            location = ""
            capacity = ""
            selectedParentUuid = initialParentUuid
        }
    }

    val selectedWarehouse by viewModel.uiState.collectAsState()
    LaunchedEffect(warehouseUuid, selectedWarehouse.selectedWarehouse) {
        if (warehouseUuid != null) {
            selectedWarehouse.selectedWarehouse?.let { warehouse ->
                name = warehouse.name
                description = warehouse.description
                location = warehouse.location
                capacity = warehouse.capacity?.toString() ?: ""
                selectedParentUuid = warehouse.parentUuid
                imageUri = warehouse.imageUri
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(if (warehouseUuid == null) stringResource(R.string.add_warehouse) else stringResource(R.string.edit_warehouse)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // 获取与返回按钮一致的颜色
                    val gradientStartColor = ColorHelpers.getTopBarGradientStart()
                    val contrastColor = ColorHelpers.getContrastColor(gradientStartColor)
                    
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = contrastColor
                        ),
                        onClick = {
                            scope.launch {
                                val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                                val canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
                                val unlimitedContainers = prefs.getBoolean("unlimited_containers", false) && canAccessPremiumFeatures
                                
                                val parentUuid = selectedParentUuid
                                val level = viewModel.calculateLevel(parentUuid)
                                
                                // 检查层级限制（除非开启无限容器模式）
                                if (!unlimitedContainers && level > 5) {
                                    // 显示提示：已达到最大层级限制
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.max_level_reached),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                                
                                val existingWarehouse = if (warehouseUuid != null) selectedWarehouse.selectedWarehouse else null
                                val warehouse = if (existingWarehouse != null) {
                                    existingWarehouse.copy(
                                        name = name,
                                        description = description,
                                        location = location,
                                        capacity = capacity.toIntOrNull(),
                                        parentUuid = parentUuid,
                                        level = level,
                                        imageUri = imageUri
                                    )
                                } else {
                                    Warehouse(
                                        name = name,
                                        description = description,
                                        location = location,
                                        capacity = capacity.toIntOrNull(),
                                        parentUuid = parentUuid,
                                        level = level,
                                        imageUri = imageUri,
                                        createdAt = java.util.Date()
                                    )
                                }
                                if (warehouseUuid == null) {
                                    viewModel.insertWarehouse(warehouse)
                                } else {
                                    viewModel.updateWarehouse(warehouse)
                                }
                                onNavigateBack()
                            }
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
                .padding(paddingValues)
                .clickable { 
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.warehouse_name_required_field)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 容器图片选择
            // 显示当前图片
            if (imageUri != null) {
                val bitmap = remember(imageUri) {
                    ImageUtils.loadBitmapFromPath(imageUri!!)
                }
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorHelpers.getGroup3CardBgColor())
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // 删除按钮
                        IconButton(
                            onClick = {
                                val uriToDelete = imageUri
                                imageUri = null
                                if (uriToDelete != null) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            ImageUtils.deleteImageAndCropped(uriToDelete)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // 拍照和相册选择按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val outlineEnabled = ColorHelpers.isOutlineEnabled()
                val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                val iconColor = if (outlineEnabled) backgroundColor else ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
                if (outlineEnabled) {
                    OutlinedButton(
                        onClick = { showCameraDialog = true },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(2.dp, backgroundColor),
                        shape = RoundedCornerShape(8.dp),
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = backgroundColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = iconColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Text(stringResource(R.string.take_photo), color = ColorHelpers.getContrastColor(backgroundColor))
                    }
                }
                val galleryBackground = ColorHelpers.getGroup2SettingsBtnColor()
                val galleryIconColor = if (outlineEnabled) galleryBackground else ColorHelpers.getGroup4IconColorByContrast(galleryBackground)
                if (outlineEnabled) {
                    OutlinedButton(
                        onClick = {
                            // 设置ActivityResult处理标记，防止密码锁屏
                            prefs.edit().putBoolean("is_processing_activity_result", true).apply()
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(2.dp, galleryBackground),
                        shape = RoundedCornerShape(8.dp),
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
                        // Text(stringResource(R.string.select_image), color = galleryBackground)
                    }
                } else {
                    Button(
                        onClick = {
                            // 设置ActivityResult处理标记，防止密码锁屏
                            prefs.edit().putBoolean("is_processing_activity_result", true).apply()
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = galleryBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = galleryIconColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Text(stringResource(R.string.select_image), color = ColorHelpers.getContrastColor(galleryBackground))
                    }
                }
                
                // 图标库选择按钮
                val iconLibraryBackground = ColorHelpers.getGroup2SettingsBtnColor()
                val iconLibraryIconColor = if (outlineEnabled) iconLibraryBackground else ColorHelpers.getGroup4IconColorByContrast(iconLibraryBackground)
                if (outlineEnabled) {
                    OutlinedButton(
                        onClick = { showIconSelectionDialog = true },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(2.dp, iconLibraryBackground),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = iconLibraryBackground,
                            disabledContentColor = iconLibraryBackground.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = null,
                            tint = iconLibraryIconColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                } else {
                    Button(
                        onClick = { showIconSelectionDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iconLibraryBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = null,
                            tint = iconLibraryIconColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.warehouse_location)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )

            OutlinedTextField(
                value = capacity,
                onValueChange = { capacity = it },
                label = { Text(stringResource(R.string.capacity_limit_optional)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Storage, null) }
            )

            // 父容器选择
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedParentUuid?.let { uuid ->
                        availableParents.find { it.uuid == uuid }?.name ?: ""
                    } ?: "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.parent_container)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showParentDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, null) },
                    placeholder = { Text(stringResource(R.string.no_warehouse_option)) }
                )
                DropdownMenu(
                    expanded = showParentDropdown,
                    onDismissRequest = { showParentDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_warehouse_option)) },
                        onClick = {
                            selectedParentUuid = null
                            showParentDropdown = false
                        }
                    )
                    availableParents.forEach { parent ->
                        DropdownMenuItem(
                            text = { Text(parent.name) },
                            onClick = {
                                selectedParentUuid = parent.uuid
                                showParentDropdown = false
                            }
                        )
                    }
                }
            }
        }
        
        // 图标库选择对话框
        if (showIconSelectionDialog) {
            IconSelectionDialog(
                onIconSelected = { iconPath ->
                    showIconSelectionDialog = false
                    scope.launch(Dispatchers.IO) {
                        val bitmap = ImageUtils.loadBitmapFromPath(iconPath)
                        if (bitmap != null) {
                            pendingImageExt = "png" // 图标库都是PNG格式
                            bitmapToCrop = bitmap
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                showCropDialog = true
                            }
                        }
                    }
                },
                onDismiss = { showIconSelectionDialog = false }
            )
        }
        
        // 拍照对话框
        if (showCameraDialog) {
            CameraCaptureDialog(
                onImageCaptured = { imagePath ->
                    showCameraDialog = false
                    if (imagePath != null) {
                        scope.launch(Dispatchers.IO) {
                            val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                            if (bitmap != null) {
                                pendingImageExt = ImageUtils.normalizeImageExtension(
                                    File(imagePath).extension
                                )
                                bitmapToCrop = bitmap
                                showCropDialog = true
                            }
                        }
                    }
                },
                onDismiss = { showCameraDialog = false },
                cardWidth = cardWidthPx,
                cardHeight = cardHeightPx
            )
        }
        
        // 裁剪对话框
        if (showCropDialog && bitmapToCrop != null) {
            ImageCropDialog(
                bitmap = bitmapToCrop!!,
                onCropped = { croppedBitmap ->
                    showCropDialog = false
                    scope.launch(Dispatchers.IO) {
                        val finalExt = pendingImageExt ?: if (croppedBitmap.hasAlpha()) "png" else "jpg"
                        val fileName = "warehouse_${warehouseUuid ?: "new"}_${System.currentTimeMillis()}.$finalExt"
                        val savedPath = ImageUtils.saveImageToInternalStorage(
                            context,
                            croppedBitmap,
                            fileName
                        )
                        savedPath?.let {
                            imageUri = it
                            cropImageForWarehouse(it)
                        }
                    }
                    bitmapToCrop = null
                    pendingImageExt = null
                },
                onDismiss = {
                    showCropDialog = false
                    bitmapToCrop = null
                    pendingImageExt = null
                },
                cardWidth = cardWidthPx,
                cardHeight = cardHeightPx
            )
        }
    }
}

