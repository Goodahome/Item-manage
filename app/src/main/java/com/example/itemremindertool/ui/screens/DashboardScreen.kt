package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "仪表盘",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(getStatCards(stats)) { statCard ->
                StatCard(statCard)
            }
        }
    }
}

data class StatCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color
)

@Composable
fun StatCard(statCard: StatCardData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statCard.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = statCard.icon,
                contentDescription = statCard.title,
                tint = statCard.color,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statCard.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = statCard.color,
                    maxLines = 1
                )
                Text(
                    text = statCard.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statCard.color.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}

fun getStatCards(stats: com.example.itemremindertool.ui.viewmodel.DashboardStats): List<StatCardData> {
    return listOf(
        StatCardData(
            title = "总物品数",
            value = stats.totalItems.toString(),
            icon = Icons.Default.Inventory,
            color = Color(0xFF1976D2),
            backgroundColor = Color(0xFFE3F2FD)
        ),
        StatCardData(
            title = "正常物品",
            value = stats.normalItems.toString(),
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF388E3C),
            backgroundColor = Color(0xFFE8F5E9)
        ),
        StatCardData(
            title = "损坏物品",
            value = stats.damagedItems.toString(),
            icon = Icons.Default.Build,
            color = Color(0xFFF57C00),
            backgroundColor = Color(0xFFFFF3E0)
        ),
        StatCardData(
            title = "遗失物品",
            value = stats.lostItems.toString(),
            icon = Icons.Default.Warning,
            color = Color(0xFFD32F2F),
            backgroundColor = Color(0xFFFFEBEE)
        ),
        StatCardData(
            title = "过期物品",
            value = stats.expiredItems.toString(),
            icon = Icons.Default.Schedule,
            color = Color(0xFF7B1FA2),
            backgroundColor = Color(0xFFF3E5F5)
        ),
        StatCardData(
            title = "分类数",
            value = stats.totalCategories.toString(),
            icon = Icons.Default.Category,
            color = Color(0xFF0288D1),
            backgroundColor = Color(0xFFE1F5FE)
        ),
        StatCardData(
            title = "仓库数",
            value = stats.totalWarehouses.toString(),
            icon = Icons.Default.Warehouse,
            color = Color(0xFF5D4037),
            backgroundColor = Color(0xFFEFEBE9)
        ),
        StatCardData(
            title = "待购物品",
            value = stats.activeShoppingItems.toString(),
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFFE91E63),
            backgroundColor = Color(0xFFFCE4EC)
        )
    )
}

