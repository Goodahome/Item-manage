package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.ui.viewmodel.CategoryViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    categoryId: Long?,
    viewModel: CategoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6200EE") }

    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.loadCategory(categoryId)
        }
    }

    val selectedCategory by viewModel.uiState.collectAsState()
    LaunchedEffect(selectedCategory.selectedCategory) {
        selectedCategory.selectedCategory?.let { category ->
            name = category.name
            description = category.description
            selectedColor = category.color
        }
    }

    val colors = listOf(
        "#6200EE", "#1976D2", "#388E3C", "#F57C00",
        "#D32F2F", "#7B1FA2", "#0288D1", "#5D4037"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (categoryId == null) stringResource(R.string.add_category) else stringResource(R.string.edit_category)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val category = Category(
                                id = categoryId ?: 0,
                                name = name,
                                description = description,
                                color = selectedColor
                            )
                            if (categoryId == null) {
                                viewModel.insertCategory(category)
                            } else {
                                viewModel.updateCategory(category.copy(id = categoryId))
                            }
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name_required_field)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Text(stringResource(R.string.select_color), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color)))
                            .clickable { selectedColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

