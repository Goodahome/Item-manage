package com.example.itemremindertool.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    onDismiss: () -> Unit,
    onAddItem: () -> Unit,
    onAddWarehouse: () -> Unit,
    onScanBarcode: () -> Unit,
    onVoiceQuickNote: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.quick_add),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Divider()
            
            // 添加物品
            QuickAddItem(
                icon = Icons.Default.Add,
                title = stringResource(R.string.add_item),
                subtitle = stringResource(R.string.add_item_hint),
                onClick = {
                    onAddItem()
                    onDismiss()
                }
            )
            
            // 添加容器
            QuickAddItem(
                icon = Icons.Default.Warehouse,
                title = stringResource(R.string.add_warehouse),
                subtitle = stringResource(R.string.add_warehouse_hint),
                onClick = {
                    onAddWarehouse()
                    onDismiss()
                }
            )
            
            // 扫码添加
            QuickAddItem(
                icon = Icons.Default.QrCodeScanner,
                title = stringResource(R.string.barcode_scanner),
                subtitle = stringResource(R.string.scan_barcode_to_add),
                onClick = {
                    onScanBarcode()
                    onDismiss()
                }
            )
            
            // 语音快速记
            QuickAddItem(
                icon = Icons.Default.Mic,
                title = stringResource(R.string.voice_quick_note),
                subtitle = stringResource(R.string.voice_quick_note_hint),
                onClick = {
                    onVoiceQuickNote()
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickAddItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

