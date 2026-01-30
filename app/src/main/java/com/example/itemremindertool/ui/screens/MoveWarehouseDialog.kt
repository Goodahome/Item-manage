package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.components.ButtonAutoSizeText
import com.example.itemremindertool.ui.theme.ColorHelpers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveWarehouseDialog(
    warehouse: Warehouse,
    allWarehouses: List<Warehouse>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val excludedUuids = remember(warehouse.uuid, allWarehouses) {
        val descendants = mutableSetOf<String>()
        fun collectDescendants(parentUuid: String) {
            allWarehouses.filter { it.parentUuid == parentUuid }.forEach { child ->
                if (descendants.add(child.uuid)) {
                    collectDescendants(child.uuid)
                }
            }
        }
        collectDescendants(warehouse.uuid)
        descendants + warehouse.uuid
    }

    val availableParents = remember(allWarehouses, excludedUuids) {
        allWarehouses.filterNot { excludedUuids.contains(it.uuid) }
            .sortedBy { it.name }
    }

    var selectedParentUuid by remember(warehouse.uuid, warehouse.parentUuid) {
        mutableStateOf(warehouse.parentUuid)
    }
    val isNoChange = selectedParentUuid == warehouse.parentUuid

    AppDialogLayout(
        title = stringResource(R.string.move_warehouse_title),
        icon = Icons.Default.DriveFileMove,
        onDismiss = onDismiss,
        contentModifier = Modifier.heightIn(max = 420.dp),
        footer = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                ButtonAutoSizeText(
                    text = stringResource(R.string.cancel)
                )
            }
            Button(
                onClick = {
                    onConfirm(selectedParentUuid)
                    onDismiss()
                },
                enabled = !isNoChange,
                modifier = Modifier.weight(1f)
            ) {
                ButtonAutoSizeText(
                    text = stringResource(R.string.confirm)
                )
            }
        }
    ) {
        Text(
            text = stringResource(R.string.move_warehouse_select_target),
            style = MaterialTheme.typography.bodyMedium,
            color = ColorHelpers.getGroup4TextColor(),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { selectedParentUuid = null },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedParentUuid == null) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = if (selectedParentUuid == null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = stringResource(R.string.no_warehouse_option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedParentUuid == null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableParents, key = { it.uuid }) { parent ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedParentUuid = parent.uuid },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedParentUuid == parent.uuid) {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (selectedParentUuid == parent.uuid) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parent.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedParentUuid == parent.uuid) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (parent.description.isNotEmpty()) {
                                Text(
                                    text = parent.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedParentUuid == parent.uuid) {
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
