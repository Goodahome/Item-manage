package com.example.itemremindertool.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ItemImagePreviewDialog(
    item: Item,
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onAddToShoppingCart: (() -> Unit)? = null,
    onMoveToContainer: (() -> Unit)? = null,
    onAddAlert: (() -> Unit)? = null,
    onEnterMultiSelect: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val primaryImagePath = remember(item.imageUris, item.primaryImageIndex, item.imageUri) {
        if (item.imageUris.isNotEmpty() && item.primaryImageIndex < item.imageUris.size) {
            item.imageUris[item.primaryImageIndex]
        } else {
            item.imageUri
        }
    }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(primaryImagePath) {
        bitmap = null
        if (primaryImagePath != null) {
            scope.launch(Dispatchers.IO) {
                val loaded = ImageUtils.loadThumbnail(context, primaryImagePath, maxSize = 1200)
                if (loaded != null) {
                    withContext(Dispatchers.Main) {
                        bitmap = loaded
                    }
                }
            }
        }
    }

    val multiSelectAction = onEnterMultiSelect?.let {
        PreviewAction(Icons.Default.CheckCircle, R.string.enter_multi_select, it)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(16.dp)
                .clickable(onClick = onDismiss)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clickable(onClick = { })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 380.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorHelpers.getGroup2PageBgColor()),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.preview_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = stringResource(R.string.preview_image),
                                tint = ColorHelpers.getGroup4IconColor(0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    multiSelectAction?.let { action ->
                        Button(
                            onClick = {
                                action.onClick()
                                onDismiss()
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .heightIn(min = 40.dp)
                        ) {
                            Icon(
                                action.icon,
                                contentDescription = stringResource(action.labelRes)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(action.labelRes))
                        }
                    }
                }
            }
        }
    }
}

private data class PreviewAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)
