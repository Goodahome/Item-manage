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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.ui.components.CameraCaptureDialog
import com.example.itemremindertool.ui.components.ImageCropDialog
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemEditScreen(
    itemId: Long?,
    viewModel: ItemViewModel,
    categories: List<com.example.itemremindertool.data.model.Category>,
    warehouses: List<com.example.itemremindertool.data.model.Warehouse>,
    tagManager: com.example.itemremindertool.data.TagManager,
    onNavigateBack: () -> Unit,
    initialFeatureCode: String? = null, // 初始特征码（从识别页面传入）
    initialWarehouseId: Long? = null, // 初始容器ID（从容器页面传入）
    modifier: Modifier = Modifier
) {
    // ==================== 基础字段 ====================
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    // 如果 initialWarehouseId 为 null，则不默认填充容器（问题2的修复）
    var selectedWarehouseId by remember { mutableStateOf<Long?>(initialWarehouseId) }
    var warehouseError by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf(setOf<String>()) }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var barcode by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var enableStockAlert by remember { mutableStateOf(false) } // 库存提醒开关，默认为false
    var imageUri by remember { mutableStateOf<String?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    // 从 ViewModel 获取待处理的特征码
    val pendingFeatureCode by viewModel.pendingFeatureCode.collectAsState()
    var featureCode by remember { mutableStateOf<String?>(initialFeatureCode ?: pendingFeatureCode) }

    // ==================== 标签输入专用状态 ====================
    var isTagInputFocused by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf("") }
    val tagFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ==================== 状态判断 ====================
    var selectedStatus by remember { mutableStateOf<ItemStatus?>(null) }
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
    val displayStatus = if (isExpired) ItemStatus.EXPIRED else (selectedStatus ?: ItemStatus.NORMAL)

    // ==================== 监听待处理特征码变化 ====================
    LaunchedEffect(pendingFeatureCode) {
        if (pendingFeatureCode != null && featureCode == null) {
            featureCode = pendingFeatureCode
            android.util.Log.d("ItemEditScreen", "从 ViewModel 获取特征码: ${featureCode?.substring(0, minOf(50, featureCode?.length ?: 0))}...")
        }
    }

    // ==================== 加载已有物品数据 ====================
    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.loadItem(itemId)
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
    LaunchedEffect(itemId, selectedItem.selectedItem, initialWarehouseId) {
        if (itemId != null) {
            // 编辑模式：加载已有物品数据
        selectedItem.selectedItem?.let { item ->
            name = item.name
            description = item.description
            selectedCategoryId = item.categoryId
            selectedWarehouseId = item.warehouseId
                tags = item.tags.toSet()
            price = item.price?.toString() ?: ""
            quantity = item.quantity.toString()
            barcode = item.barcode ?: ""
            expiryDate = item.expiryDate
            enableStockAlert = item.enableStockAlert
                imageUri = item.imageUri
                featureCode = item.featureCode
                // 初始化状态
                val statusInTags = item.tags.firstOrNull { it in listOf("正常", "损坏", "遗失") }
                selectedStatus = when (statusInTags) {
                    "正常" -> ItemStatus.NORMAL
                    "损坏" -> ItemStatus.DAMAGED
                    "遗失" -> ItemStatus.LOST
                    else -> ItemStatus.NORMAL
                }
            }
        } else {
            // 新建模式：设置初始容器
            if (initialWarehouseId != null && selectedWarehouseId == null) {
                selectedWarehouseId = initialWarehouseId
            }
        }
    }
    
    // 确保在新建模式下，初始容器ID正确设置
    LaunchedEffect(initialWarehouseId) {
        if (itemId == null && initialWarehouseId != null) {
            selectedWarehouseId = initialWarehouseId
            selectedStatus = ItemStatus.NORMAL
        }
    }

    // ==================== Scaffold ====================
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(if (itemId == null) stringResource(R.string.add_item) else stringResource(R.string.edit_item)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedWarehouseId == null) {
                                warehouseError = true
                                return@TextButton
                            } else {
                                warehouseError = false
                            }
                            val item = Item(
                                id = itemId ?: 0L,
                                name = name,
                                description = description,
                                categoryId = selectedCategoryId,
                                warehouseId = selectedWarehouseId,
                                tags = tags.toList(),
                                price = price.toDoubleOrNull(),
                                quantity = quantity.toIntOrNull() ?: 1,
                                barcode = barcode.ifEmpty { null },
                                expiryDate = expiryDate,
                                imageUri = imageUri,
                                featureCode = featureCode,
                                enableStockAlert = enableStockAlert
                            )
                            if (itemId == null) {
                                viewModel.insertItem(item)
                            } else {
                                viewModel.updateItem(item.copy(id = itemId!!))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
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
            val cardHeightPx = remember { with(density) { 200.dp.toPx().toInt() } }
            
            // 从相册选择图片的启动器
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    // 处理选中的图片
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        
                        if (bitmap != null) {
                            // 显示裁剪对话框
                            bitmapToCrop = bitmap
                            showCropDialog = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
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
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ColorHelpers.getGroup4IconColor())
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.take_photo), color = ColorHelpers.getGroup4TextColor())
                }
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorHelpers.getGroup2SettingsBtnColor()
                    )
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ColorHelpers.getGroup4IconColor())
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_from_gallery), color = ColorHelpers.getGroup4TextColor())
                }
                if (imageUri != null) {
                    IconButton(onClick = { imageUri = null }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_image), tint = ColorHelpers.getGroup4IconColor())
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
                    value = warehouses.find { it.id == selectedWarehouseId }?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.warehouse)) },
                    isError = warehouseError,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWarehouse) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedWarehouse,
                    onDismissRequest = { expandedWarehouse = false }
                ) {
                    warehouses.forEach { warehouse ->
                        DropdownMenuItem(
                            text = { Text(warehouse.name) },
                            onClick = {
                                selectedWarehouseId = warehouse.id
                                warehouseError = false
                                expandedWarehouse = false
                            }
                        )
                    }
                }
            }
            if (warehouseError) {
                Text(
                    text = stringResource(R.string.warehouse) + " 必填，请选择容器",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ==================== 标签 + 自定义标签（核心改动）===================
            Text(stringResource(R.string.status_tags), style = MaterialTheme.typography.labelLarge)

            // 获取所有已创建的标签
            val allTags by tagManager.allTags.collectAsState()
            val statusLabels = listOf("正常", "损坏", "遗失")
            val customTags = allTags.filter { it !in statusLabels && it != "过期" }

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
                // 1. 可切换的标签（正常、损坏、遗失）- 支持多选，选中高亮60%黄色，保持边框
                items(listOf(ItemStatus.NORMAL, ItemStatus.DAMAGED, ItemStatus.LOST)) { status ->
                    val statusLabel = getStatusLabel(status)
                    val isSelected = tags.contains(statusLabel)
                    
                    FilterChip(
                        selected = isSelected && !isExpired,
                        onClick = {
                            if (!isExpired) {
                                if (isSelected) {
                                    // 取消选择：从tags中移除
                                    tags = tags - statusLabel
                                } else {
                                    // 选择：支持多选，添加当前状态（不限制数量）
                                    tags = tags + statusLabel
                                    selectedStatus = status
                                }
                            }
                        },
                        label = { Text(statusLabel, color = ColorHelpers.getGroup4TextColor()) },
                        enabled = true,
                        // 为选中/未选中状态都添加可见边框，选中使用主色，未选中使用轮廓色
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected && !isExpired,
                            borderColor = if (isSelected && !isExpired)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
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

                // 2. 过期标签（自动显示）
                if (isExpired) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            enabled = false,
                            label = { Text(stringResource(R.string.expired_tag), color = ColorHelpers.getGroup4TextColor()) },
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
                        FilterChip(
                            selected = true,
                            onClick = { tags = tags - tag },
                            enabled = true,
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(tag)
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = MaterialTheme.colorScheme.primary,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
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
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
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
                            .height(40.dp)
                            .widthIn(min = if (isTagInputFocused) 140.dp else 108.dp, max = 120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isTagInputFocused)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else
                                    Color.Transparent
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isTagInputFocused)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                enabled = !isTagInputFocused,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isTagInputFocused = true }
                            .padding(horizontal = 16.dp),
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
                                        if (text.isNotEmpty() && text !in tags && text !in listOf("正常", "损坏", "遗失", "过期")) {
                                            // 保存到全局标签列表
                                            tagManager.addTag(text)
                                            // 自动选中新添加的标签
                                            tags = tags + text
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
                                if (text.isNotEmpty() && text !in tags && text !in listOf("正常", "损坏", "遗失", "过期")) {
                                    // 保存到全局标签列表
                                    tagManager.addTag(text)
                                    // 自动选中新添加的标签
                                    tags = tags + text
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
                                            .fillMaxSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                isTagInputFocused = true
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "添加标签",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
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
                                            text = "输入标签…",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
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
                modifier = Modifier.fillMaxWidth()
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
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text(stringResource(R.string.barcode)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.QrCode, null) }
            )

            // ==================== 特征码（只读显示）====================
            if (featureCode != null) {
                OutlinedTextField(
                    value = "特征码已生成 (${featureCode!!.length} 字符)",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.feature_code)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.ImageSearch, null) },
                    trailingIcon = {
                        IconButton(onClick = { featureCode = null }) {
                            Icon(Icons.Default.Close, stringResource(R.string.clear_feature_code))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // ==================== 到期日期 ====================
            OutlinedTextField(
                value = expiryDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.expiry_date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, null)
                    }
                }
            )

            // ==================== 日期选择器 ====================
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = expiryDate?.time
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val zone = ZoneId.systemDefault()
                                val localDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                                val startOfDay = localDate.atStartOfDay(zone).toInstant().toEpochMilli()
                                expiryDate = Date(startOfDay)
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            // ==================== 拍照对话框 ====================
            if (showCameraDialog) {
                CameraCaptureDialog(
                    onImageCaptured = { imagePath ->
                        showCameraDialog = false
                        if (imagePath != null) {
                            // 加载图片并自动裁剪（相机已有引导框，直接按框裁剪）
                            val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                            if (bitmap != null) {
                                val croppedBitmap = ImageUtils.cropImageToCardSize(
                                    bitmap,
                                    cardWidthPx,
                                    cardHeightPx
                                )
                                // 保存裁剪后的图片
                                val fileName = "item_${itemId ?: System.currentTimeMillis()}.jpg"
                                val savedPath = ImageUtils.saveImageToInternalStorage(
                                    context,
                                    croppedBitmap,
                                    fileName
                                )
                                imageUri = savedPath
                                // 删除原始临时文件
                                ImageUtils.deleteImageFile(imagePath)
                            }
                        }
                    },
                    onDismiss = { showCameraDialog = false },
                    cardWidth = cardWidthPx,
                    cardHeight = cardHeightPx
                )
            }
            
            // 相册选择后的裁剪对话框
            if (showCropDialog && bitmapToCrop != null) {
                ImageCropDialog(
                    bitmap = bitmapToCrop!!,
                    onCropped = { croppedBitmap ->
                        showCropDialog = false
                        // 将裁剪后的图片缩放到目标卡片尺寸
                        val scaledBitmap = ImageUtils.scaleCroppedBitmapToCardSize(
                            croppedBitmap,
                            cardWidthPx,
                            cardHeightPx
                        )
                        // 保存裁剪后的图片
                        val fileName = "item_${itemId ?: System.currentTimeMillis()}.jpg"
                        val savedPath = ImageUtils.saveImageToInternalStorage(
                            context,
                            scaledBitmap,
                            fileName
                        )
                        imageUri = savedPath
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
}

// ==================== 辅助函数 ====================
fun getStatusLabel(status: ItemStatus): String = when (status) {
        ItemStatus.NORMAL -> "正常"
        ItemStatus.DAMAGED -> "损坏"
        ItemStatus.LOST -> "遗失"
        ItemStatus.EXPIRED -> "过期"
    }

// ==================== 自定义虚线 Modifier ====================
fun Modifier.dashedBorder(
    strokeWidth: Dp,
    color: Color,
    dashLength: Dp,
    gapLength: Dp,
    cornerRadius: Dp
): Modifier = this.then(
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