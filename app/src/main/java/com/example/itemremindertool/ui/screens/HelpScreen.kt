package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.theme.ColorHelpers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SectionCard(
                body = stringResource(R.string.help_intro)
            )
            Spacer(modifier = Modifier.height(10.dp))

            SectionCard(
                title = stringResource(R.string.help_section_items_title),
                body = stringResource(R.string.help_section_items_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_warehouse_title),
                body = stringResource(R.string.help_section_warehouse_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_tags_title),
                body = stringResource(R.string.help_section_tags_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_shopping_title),
                body = stringResource(R.string.help_section_shopping_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_tools_title),
                body = stringResource(R.string.help_section_tools_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_alert_title),
                body = stringResource(R.string.help_section_alert_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_data_title),
                body = stringResource(R.string.help_section_data_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_appearance_title),
                body = stringResource(R.string.help_section_appearance_body)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectionCard(
                title = stringResource(R.string.help_section_tips_title),
                body = stringResource(R.string.help_section_tips_body)
            )
        }
    }
}

@Composable
private fun SectionCard(
    body: String,
    title: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorHelpers.getGroup4TextColor()
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor(0.78f)
            )
        }
    }
}
