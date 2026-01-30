package com.example.itemremindertool.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.common.util.concurrent.ListenableFuture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.R
import com.example.itemremindertool.ui.theme.ColorHelpers
import androidx.compose.ui.res.stringResource
import java.io.File
import java.util.concurrent.Executors

/**
 * 拍照对话框，全屏相机预览，悬浮显示物品卡片大小的提示框
 */
@Composable
fun CameraCaptureDialog(
    onImageCaptured: (String?) -> Unit,
    onDismiss: () -> Unit,
    cardWidth: Int = 400, // 物品卡片宽度（px）
    cardHeight: Int = 400, // 物品卡片高度（px）- 使用正方形
    targetWarehouseName: String? = null // 目标容器名称（用于连续添加提示）
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    var imageFile: File? by remember { mutableStateOf(null) }
    
    // 创建 ImageCapture，使用默认方向
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setTargetRotation(android.view.Surface.ROTATION_0)
            .build()
    }
    
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = remember { 
        ProcessCameraProvider.getInstance(context)
    }
    val previewView = remember { PreviewView(context) }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val cameraProvider = cameraProviderFuture.get()
            
            // 设置预览
            val preview = Preview.Builder()
                .setTargetRotation(android.view.Surface.ROTATION_0)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
            cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // 忽略清理时的错误
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 全屏显示
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val density = LocalDensity.current
        val cardWidthDp = with(density) { cardWidth.toDp() }
        val cardHeightDp = with(density) { cardHeight.toDp() }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!hasPermission) {
                // 无权限提示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            } else {
                // 全屏相机预览
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 悬浮的提示框 - 使用 Canvas 绘制半透明遮罩和虚线框
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // 物品卡片提示框的尺寸和位置（居中）
                    val frameWidth = cardWidthDp.toPx()
                    val frameHeight = cardHeightDp.toPx()
                    val frameLeft = (canvasWidth - frameWidth) / 2
                    val frameTop = (canvasHeight - frameHeight) / 2
                    
                    // 绘制半透明遮罩（除了提示框区域）
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        size = Size(canvasWidth, frameTop) // 上方
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, frameTop + frameHeight),
                        size = Size(canvasWidth, canvasHeight - frameTop - frameHeight) // 下方
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, frameTop),
                        size = Size(frameLeft, frameHeight) // 左侧
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(frameLeft + frameWidth, frameTop),
                        size = Size(canvasWidth - frameLeft - frameWidth, frameHeight) // 右侧
                    )
                    
                    // 绘制虚线边框
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(frameLeft, frameTop),
                        size = Size(frameWidth, frameHeight),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                        )
                    )
                }
                
                // 顶部提示文字
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 主提示
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.camera_frame_hint),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // 连续添加提示（如果提供了容器名称）
                        if (targetWarehouseName != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.quick_add_continuous_mode),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.quick_add_target_warehouse, targetWarehouseName),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 底部按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 取消按钮
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // 拍照按钮
                        FloatingActionButton(
                            onClick = {
                                imageFile = ImageUtils.createImageFile(context)
                                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                                    imageFile!!
                                ).build()
                                
                                imageCapture.takePicture(
                                    outputFileOptions,
                                    Executors.newSingleThreadExecutor(),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val imagePath = imageFile?.absolutePath
                                            onImageCaptured(imagePath)
                                        }
                                        
                                        override fun onError(exception: ImageCaptureException) {
                                            onImageCaptured(null)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.size(72.dp),
                            containerColor = Color.White
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

