package com.example.itemremindertool.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemremindertool.R
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.ui.components.BottomOperationStatusIndicator
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.viewmodel.ExcelImportExportViewModel
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.utils.ExcelImportExportUtils
import com.example.itemremindertool.utils.ExcelImportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelImportExportScreen(
    itemViewModel: ItemViewModel,
    tagManager: TagManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ExcelImportExportViewModel = viewModel()
    val operationState by viewModel.operationState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.showSaving()
            val result = exportToExcel(context, uri, itemViewModel)
            result.fold(
                onSuccess = { count ->
                    viewModel.showSuccess(context.getString(R.string.excel_export_success, count))
                },
                onFailure = { error ->
                    viewModel.showError(context.getString(R.string.excel_export_failed, error.message ?: ""))
                }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.showSaving()
            val result = importFromExcel(context, uri, tagManager)
            result.fold(
                onSuccess = { summary ->
                    viewModel.showSuccess(
                        context.getString(
                            R.string.excel_import_success,
                            summary.importedCount,
                            summary.skippedCount,
                            summary.mergedCount
                        )
                    )
                },
                onFailure = { error ->
                    viewModel.showError(context.getString(R.string.excel_import_failed, error.message ?: ""))
                }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GradientTopAppBar(
                    title = { Text(stringResource(R.string.excel_import_export_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorHelpers.getGroup3CardBgColor()
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.excel_import_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = stringResource(R.string.excel_import_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { importLauncher.launch("application/vnd.ms-excel") }
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.excel_import_button),
                                    modifier = Modifier.padding(start = 0.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorHelpers.getGroup3CardBgColor()
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.excel_export_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = stringResource(R.string.excel_export_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                    val defaultName = "ItemReminder_${formatter.format(Date())}.xls"
                                    exportLauncher.launch(defaultName)
                                }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.excel_export_button),
                                    modifier = Modifier.padding(start = 0.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorHelpers.getGroup3CardBgColor()
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.excel_import_export_hint_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorHelpers.getGroup4TextColor()
                            )
                            Text(
                                text = stringResource(R.string.excel_import_export_hint_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.7f)
                            )
                        }
                    }
                }
            }
        }

        BottomOperationStatusIndicator(
            operationState = operationState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private suspend fun exportToExcel(
    context: Context,
    uri: Uri,
    itemViewModel: ItemViewModel
): Result<Int> = withContext(Dispatchers.IO) {
    val outputStream = context.contentResolver.openOutputStream(uri)
        ?: return@withContext Result.failure(IOException(context.getString(R.string.excel_file_write_failed)))
    outputStream.use { output ->
        val database = AppDatabase.getDatabase(context)
        val items = itemViewModel.getAllItemsList()
        val warehouses = database.warehouseDao().getAllWarehousesSync()
        ExcelImportExportUtils.exportItemsToExcel(context, items, warehouses, output)
    }
}

private suspend fun importFromExcel(
    context: Context,
    uri: Uri,
    tagManager: TagManager
): Result<ExcelImportSummary> = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: return@withContext Result.failure(IOException(context.getString(R.string.excel_file_read_failed)))
    inputStream.use { input ->
        ExcelImportExportUtils.importItemsFromExcel(context, input, tagManager)
    }
}
