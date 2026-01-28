package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.viewmodel.WarehouseViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.theme.ColorHelpers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveItemDialog(
    itemName: String,
    currentWarehouseUuid: String?,
    warehouseViewModel: WarehouseViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
    var selectedWarehouseUuid by remember { mutableStateOf<String?>(null) }

    AppDialogLayout(
        title = stringResource(R.string.move_to_container),
        icon = Icons.Default.DriveFileMove,
        onDismiss = onDismiss,
        contentModifier = Modifier.heightIn(max = 420.dp),
        footer = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    onConfirm(selectedWarehouseUuid)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    ) {
        Text(
            text = stringResource(R.string.select_target_container),
            style = MaterialTheme.typography.bodyMedium,
            color = ColorHelpers.getGroup4TextColor(),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 无容器选项
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { selectedWarehouseUuid = null },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedWarehouseUuid == null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = if (selectedWarehouseUuid == null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = stringResource(R.string.no_warehouse_option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedWarehouseUuid == null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        // 容器列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(warehouses.filter { it.uuid != currentWarehouseUuid }, key = { it.uuid }) { warehouse ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedWarehouseUuid = warehouse.uuid },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedWarehouseUuid == warehouse.uuid) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (selectedWarehouseUuid == warehouse.uuid) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = warehouse.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedWarehouseUuid == warehouse.uuid) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (warehouse.description.isNotEmpty()) {
                                Text(
                                    text = warehouse.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedWarehouseUuid == warehouse.uuid) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

