package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.Image
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
import com.example.itemremindertool.ui.theme.ColorHelpers
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseEditScreen(
    warehouseId: Long?,
    viewModel: WarehouseViewModel,
    onNavigateBack: () -> Unit,
    initialParentId: Long? = null, // 预设的父容器ID
    onSaveSuccess: ((Long?) -> Unit)? = null, // 保存成功后的回调，传递父容器ID
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(initialParentId) }
    var showParentDropdown by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 当 initialParentId 变化时更新 selectedParentId（仅在添加模式下）
    LaunchedEffect(initialParentId) {
        if (warehouseId == null) {
            selectedParentId = initialParentId
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
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
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

    // 递归查找所有子容器ID（包括子容器的子容器等）
    fun getAllChildIds(parentId: Long, warehouses: List<Warehouse>): Set<Long> {
        val childIds = mutableSetOf<Long>()
        val directChildren = warehouses.filter { it.parentId == parentId }
        childIds.addAll(directChildren.map { it.id })
        directChildren.forEach { child ->
            childIds.addAll(getAllChildIds(child.id, warehouses))
        }
        return childIds
    }

    // 过滤可用的父容器（排除当前容器、当前容器的所有子容器和层级>=5的容器）
    val availableParents = remember(allWarehouses, warehouseId) {
        if (warehouseId == null) {
            // 添加模式：只排除层级>=5的容器
            allWarehouses.filter { it.level < 5 }
        } else {
            // 编辑模式：排除当前容器、当前容器的所有子容器和层级>=5的容器
            val excludedIds = mutableSetOf<Long>()
            excludedIds.add(warehouseId)
            excludedIds.addAll(getAllChildIds(warehouseId, allWarehouses))
            
            allWarehouses.filter { 
                it.id !in excludedIds && it.level < 5
            }
        }
    }

    LaunchedEffect(warehouseId) {
        if (warehouseId != null) {
            viewModel.loadWarehouse(warehouseId)
        } else {
            // 添加新容器时，清空表单
            name = ""
            description = ""
            location = ""
            capacity = ""
            // 保持 initialParentId 的设置
            selectedParentId = initialParentId
        }
    }

    val selectedWarehouse by viewModel.uiState.collectAsState()
    LaunchedEffect(warehouseId, selectedWarehouse.selectedWarehouse) {
        // 只有在编辑模式下（warehouseId 不为 null）才填充表单
        if (warehouseId != null) {
            selectedWarehouse.selectedWarehouse?.let { warehouse ->
                name = warehouse.name
                description = warehouse.description
                location = warehouse.location
                capacity = warehouse.capacity?.toString() ?: ""
                selectedParentId = warehouse.parentId
                imageUri = warehouse.imageUri
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(if (warehouseId == null) stringResource(R.string.add_warehouse) else stringResource(R.string.edit_warehouse)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                                val unlimitedContainers = prefs.getBoolean("unlimited_containers", false)
                                
                                val level = if (selectedParentId != null) {
                                    viewModel.calculateLevel(selectedParentId)
                                } else {
                                    1
                                }
                                
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
                                
                                val warehouse = Warehouse(
                                    id = warehouseId ?: 0,
                                    name = name,
                                    description = description,
                                    location = location,
                                    capacity = capacity.toIntOrNull(),
                                    parentId = selectedParentId,
                                    level = level,
                                    imageUri = imageUri
                                )
                                if (warehouseId == null) {
                                    viewModel.insertWarehouse(warehouse)
                                    // 如果是添加新容器，保存成功后调用回调
                                    onSaveSuccess?.invoke(selectedParentId)
                                } else {
                                    viewModel.updateWarehouse(warehouse.copy(id = warehouseId))
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
            Text(
                text = stringResource(R.string.warehouse_image),
                style = MaterialTheme.typography.labelLarge,
                color = ColorHelpers.getGroup4TextColor()
            )
            
            // 显示当前图片
            if (imageUri != null) {
                val bitmap = remember(imageUri) {
                    ImageUtils.loadBitmapFromPath(imageUri!!)
                }
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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
                Button(
                    onClick = { showCameraDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorHelpers.getGroup2SettingsBtnColor()
                    )
                ) {
                    val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                    val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                    val textColor = ColorHelpers.getContrastColor(backgroundColor)
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.take_photo), color = textColor)
                }
                Button(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorHelpers.getGroup2SettingsBtnColor()
                    )
                ) {
                    val backgroundColor = ColorHelpers.getGroup2SettingsBtnColor()
                    val iconColor = ColorHelpers.getContrastColor(backgroundColor)
                    val textColor = ColorHelpers.getContrastColor(backgroundColor)
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_image), color = textColor)
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
                    value = selectedParentId?.let { parentId ->
                        availableParents.find { it.id == parentId }?.name ?: ""
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_warehouse_option)) },
                        onClick = {
                            selectedParentId = null
                            showParentDropdown = false
                        }
                    )
                    availableParents.forEach { parent ->
                        DropdownMenuItem(
                            text = { Text(parent.name) },
                            onClick = {
                                selectedParentId = parent.id
                                showParentDropdown = false
                            }
                        )
                    }
                }
            }
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
                        val fileName = "warehouse_${warehouseId ?: System.currentTimeMillis()}_${System.currentTimeMillis()}.jpg"
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
                },
                onDismiss = {
                    showCropDialog = false
                    bitmapToCrop = null
                },
                cardWidth = cardWidthPx,
                cardHeight = cardHeightPx
            )
        }
    }
}

