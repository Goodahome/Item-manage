package com.example.itemremindertool.ui.components

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.utils.QRCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseQRCodeDialog(
    warehouse: Warehouse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    
    // 生成二维码
    val qrCodeBitmap = remember(warehouse) {
        val qrContent = QRCodeUtils.encodeWarehouseInfo(warehouse)
        QRCodeUtils.generateQRCode(qrContent, width = 512, height = 512)
    }
    
    // 保存二维码到相册
    fun saveQRCodeToGallery() {
        if (qrCodeBitmap == null) {
            Toast.makeText(context, context.getString(R.string.qr_code_save_failed), Toast.LENGTH_SHORT).show()
            return
        }
        
        isSaving = true
        scope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "warehouse_${warehouse.name}_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ItemReminderTool")
                        }
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    uri?.let {
                        val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
                        outputStream?.use { stream ->
                            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            true
                        } ?: false
                    } ?: false
                }
                
                withContext(Dispatchers.Main) {
                    if (saved) {
                        Toast.makeText(context, context.getString(R.string.qr_code_saved), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.qr_code_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.qr_code_save_failed), Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSaving = false
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.warehouse_qr_code),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.qr_code),
                        modifier = Modifier.size(300.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.qr_code_generation_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = stringResource(R.string.qr_code_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { saveQRCodeToGallery() },
                    enabled = !isSaving && qrCodeBitmap != null
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.save_to_album))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

