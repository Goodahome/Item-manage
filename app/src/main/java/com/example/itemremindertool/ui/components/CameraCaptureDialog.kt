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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import java.io.File
import java.util.concurrent.Executors

/**
 * 拍照对话框，显示物品卡片大小的预览窗口
 */
@Composable
fun CameraCaptureDialog(
    onImageCaptured: (String?) -> Unit,
    onDismiss: () -> Unit,
    cardWidth: Int = 400, // 物品卡片宽度（dp转px）
    cardHeight: Int = 200 // 物品卡片高度（dp转px）
) {
    // 物品卡片是横向的，所以预览也应该是横向（宽大于高）
    val previewWidth = cardWidth
    val previewHeight = cardHeight
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
    
    // 创建 ImageCapture，设置为横向（90度旋转）
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setTargetRotation(android.view.Surface.ROTATION_90) // 横向
            .build()
    }
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val cameraProvider = cameraProviderFuture.get()
            
            // 设置预览为横向
            val preview = Preview.Builder()
                .setTargetRotation(android.view.Surface.ROTATION_90) // 横向
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
            cameraProviderFuture.get().unbindAll()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "拍照（物品卡片大小预览）",
                    style = MaterialTheme.typography.titleLarge
                )
                
                if (!hasPermission) {
                    Text(stringResource(R.string.camera_permission_required))
                } else {
                    // 相机预览区域 - 显示物品卡片大小的框（横向）
                    Box(
                        modifier = Modifier
                            .width(previewWidth.dp)
                            .height(previewHeight.dp)
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // 显示物品卡片大小的边框提示
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        )
                    }
                    
                    // 拍照按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel))
                        }
                        
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
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, stringResource(R.string.take_photo))
                        }
                    }
                }
            }
        }
    }
}

