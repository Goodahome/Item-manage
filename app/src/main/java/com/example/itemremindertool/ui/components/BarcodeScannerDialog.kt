package com.example.itemremindertool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.itemremindertool.ui.screens.BarcodeScannerScreen

/**
 * 条形码扫描对话框
 */
@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            BarcodeScannerScreen(
                onBarcodeScanned = { barcode ->
                    onBarcodeScanned(barcode)
                    onDismiss()
                },
                onNavigateBack = onDismiss
            )
        }
    }
}

