package com.example.itemremindertool.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.itemremindertool.R

/**
 * 图片裁剪对话框，支持拖动裁剪框选择区域
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropDialog(
    bitmap: Bitmap,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    cardWidth: Int = 400, // 目标卡片宽度（px）
    cardHeight: Int = 200 // 目标卡片高度（px）
) {
    val density = LocalDensity.current
    
    // 计算裁剪框的初始尺寸和位置
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var frameOffset by remember { mutableStateOf(Offset.Zero) }
    
    // 裁剪框的宽高比
    val aspectRatio = cardWidth.toFloat() / cardHeight.toFloat()
    
    // 计算裁剪框的初始尺寸（基于图片和画布）
    val frameSize = remember(canvasSize, cardWidth, cardHeight) {
        if (canvasSize.width == 0 || canvasSize.height == 0) {
            Size(cardWidth.toFloat(), cardHeight.toFloat())
        } else {
            // 裁剪框占画布的60%左右
            val maxFrameWidth = canvasSize.width * 0.6f
            val maxFrameHeight = canvasSize.height * 0.6f
            
            val frameWidth: Float
            val frameHeight: Float
            
            if (maxFrameWidth / aspectRatio <= maxFrameHeight) {
                frameWidth = maxFrameWidth
                frameHeight = frameWidth / aspectRatio
            } else {
                frameHeight = maxFrameHeight
                frameWidth = frameHeight * aspectRatio
            }
            
            Size(frameWidth, frameHeight)
        }
    }
    
    // 初始化裁剪框位置（居中）
    LaunchedEffect(canvasSize, frameSize) {
        if (canvasSize.width > 0 && frameOffset == Offset.Zero) {
            frameOffset = Offset(
                (canvasSize.width - frameSize.width) / 2,
                (canvasSize.height - frameSize.height) / 2
            )
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.crop_image)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                // 执行裁剪
                                val croppedBitmap = cropBitmap(
                                    bitmap,
                                    frameOffset,
                                    frameSize,
                                    canvasSize
                                )
                                onCropped(croppedBitmap)
                            }
                        ) {
                            Icon(Icons.Default.Check, stringResource(R.string.confirm))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
            ) {
                // 显示图片和裁剪框
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            canvasSize = coordinates.size
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // 更新裁剪框位置
                                val newOffset = frameOffset + dragAmount
                                // 限制在画布范围内
                                frameOffset = Offset(
                                    x = newOffset.x.coerceIn(0f, canvasSize.width - frameSize.width),
                                    y = newOffset.y.coerceIn(0f, canvasSize.height - frameSize.height)
                                )
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    if (canvasWidth == 0f || canvasHeight == 0f) return@Canvas
                    
                    // 计算图片显示的尺寸和位置（保持宽高比，fit center）
                    val bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val canvasAspectRatio = canvasWidth / canvasHeight
                    
                    val drawWidth: Float
                    val drawHeight: Float
                    val drawLeft: Float
                    val drawTop: Float
                    
                    if (bitmapAspectRatio > canvasAspectRatio) {
                        // 图片更宽，以宽度为准
                        drawWidth = canvasWidth
                        drawHeight = canvasWidth / bitmapAspectRatio
                        drawLeft = 0f
                        drawTop = (canvasHeight - drawHeight) / 2
                    } else {
                        // 图片更高，以高度为准
                        drawHeight = canvasHeight
                        drawWidth = canvasHeight * bitmapAspectRatio
                        drawTop = 0f
                        drawLeft = (canvasWidth - drawWidth) / 2
                    }
                    
                    // 绘制图片
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(drawLeft.toInt(), drawTop.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt())
                    )
                    
                    // 绘制半透明遮罩（裁剪框外的区域）
                    // 上方
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(0f, 0f),
                        size = Size(canvasWidth, frameOffset.y)
                    )
                    // 下方
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(0f, frameOffset.y + frameSize.height),
                        size = Size(canvasWidth, canvasHeight - frameOffset.y - frameSize.height)
                    )
                    // 左侧
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(0f, frameOffset.y),
                        size = Size(frameOffset.x, frameSize.height)
                    )
                    // 右侧
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(frameOffset.x + frameSize.width, frameOffset.y),
                        size = Size(canvasWidth - frameOffset.x - frameSize.width, frameSize.height)
                    )
                    
                    // 绘制裁剪框边框（虚线）
                    drawRoundRect(
                        color = Color.White,
                        topLeft = frameOffset,
                        size = frameSize,
                        cornerRadius = CornerRadius(16f, 16f),
                        style = Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                        )
                    )
                    
                    // 绘制四个角的标记
                    val cornerSize = 30f
                    val cornerThickness = 4f
                    
                    // 左上角
                    drawLine(
                        color = Color.White,
                        start = frameOffset,
                        end = Offset(frameOffset.x + cornerSize, frameOffset.y),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = Color.White,
                        start = frameOffset,
                        end = Offset(frameOffset.x, frameOffset.y + cornerSize),
                        strokeWidth = cornerThickness
                    )
                    
                    // 右上角
                    drawLine(
                        color = Color.White,
                        start = Offset(frameOffset.x + frameSize.width, frameOffset.y),
                        end = Offset(frameOffset.x + frameSize.width - cornerSize, frameOffset.y),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(frameOffset.x + frameSize.width, frameOffset.y),
                        end = Offset(frameOffset.x + frameSize.width, frameOffset.y + cornerSize),
                        strokeWidth = cornerThickness
                    )
                    
                    // 左下角
                    drawLine(
                        color = Color.White,
                        start = Offset(frameOffset.x, frameOffset.y + frameSize.height),
                        end = Offset(frameOffset.x + cornerSize, frameOffset.y + frameSize.height),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(frameOffset.x, frameOffset.y + frameSize.height),
                        end = Offset(frameOffset.x, frameOffset.y + frameSize.height - cornerSize),
                        strokeWidth = cornerThickness
                    )
                    
                    // 右下角
                    drawLine(
                        color = Color.White,
                        start = Offset(frameOffset.x + frameSize.width, frameOffset.y + frameSize.height),
                        end = Offset(frameOffset.x + frameSize.width - cornerSize, frameOffset.y + frameSize.height),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(frameOffset.x + frameSize.width, frameOffset.y + frameSize.height),
                        end = Offset(frameOffset.x + frameSize.width, frameOffset.y + frameSize.height - cornerSize),
                        strokeWidth = cornerThickness
                    )
                }
                
                // 底部提示文字
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.drag_frame_to_crop),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 根据裁剪框位置裁剪图片
 */
private fun cropBitmap(
    bitmap: Bitmap,
    frameOffset: Offset,
    frameSize: Size,
    canvasSize: IntSize
): Bitmap {
    // 计算图片在画布上的显示位置和尺寸
    val canvasWidth = canvasSize.width.toFloat()
    val canvasHeight = canvasSize.height.toFloat()
    
    val bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val canvasAspectRatio = canvasWidth / canvasHeight
    
    val drawWidth: Float
    val drawHeight: Float
    val drawLeft: Float
    val drawTop: Float
    
    if (bitmapAspectRatio > canvasAspectRatio) {
        drawWidth = canvasWidth
        drawHeight = canvasWidth / bitmapAspectRatio
        drawLeft = 0f
        drawTop = (canvasHeight - drawHeight) / 2
    } else {
        drawHeight = canvasHeight
        drawWidth = canvasHeight * bitmapAspectRatio
        drawTop = 0f
        drawLeft = (canvasWidth - drawWidth) / 2
    }
    
    // 计算裁剪框在原图中的位置
    val scaleX = bitmap.width / drawWidth
    val scaleY = bitmap.height / drawHeight
    
    val cropX = ((frameOffset.x - drawLeft) * scaleX).toInt().coerceIn(0, bitmap.width)
    val cropY = ((frameOffset.y - drawTop) * scaleY).toInt().coerceIn(0, bitmap.height)
    val cropWidth = (frameSize.width * scaleX).toInt().coerceIn(1, bitmap.width - cropX)
    val cropHeight = (frameSize.height * scaleY).toInt().coerceIn(1, bitmap.height - cropY)
    
    return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
}

