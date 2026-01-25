package com.example.itemremindertool.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.ui.theme.ColorHelpers

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ColorHelpers.getGroup5FabColor(),
    shape: Shape = FloatingActionButtonDefaults.shape,
    content: @Composable () -> Unit
) {
    val outlineEnabled = ColorHelpers.isOutlineEnabled()
    val containerColor = if (outlineEnabled) {
        ColorHelpers.getGroup3CardBgColor()
    } else {
        backgroundColor
    }
    val contentColor = if (outlineEnabled) {
        backgroundColor
    } else {
        ColorHelpers.getGroup4IconColorByContrast(backgroundColor)
    }
    val decoratedModifier = if (outlineEnabled) {
        modifier.border(2.dp, backgroundColor, shape)
    } else {
        modifier
    }
    val elevation = if (outlineEnabled) {
        FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp)
    } else {
        FloatingActionButtonDefaults.elevation()
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = decoratedModifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape,
        elevation = elevation
    ) {
        content()
    }
}
