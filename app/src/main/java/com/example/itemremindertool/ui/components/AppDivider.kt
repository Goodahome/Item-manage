package com.example.itemremindertool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.ui.theme.ColorHelpers

@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    thickness: Dp = 2.dp,
    color: Color = ColorHelpers.getDividerColor()
) {
    val widthModifier = width?.let { Modifier.width(it) } ?: Modifier.fillMaxWidth()
    Box(
        modifier = modifier
            .then(widthModifier)
            .height(thickness)
            .clip(RoundedCornerShape(percent = 50))
            .background(color)
    )
}
