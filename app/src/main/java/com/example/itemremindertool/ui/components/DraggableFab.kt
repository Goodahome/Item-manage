package com.example.itemremindertool.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
fun DraggableFab(
    modifier: Modifier = Modifier,
    boundsPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        var fabSize by remember { mutableStateOf(IntSize.Zero) }
        var position by remember { mutableStateOf(Offset.Zero) }
        var initialized by remember { mutableStateOf(false) }

        val paddingStartPx = with(density) { boundsPadding.calculateLeftPadding(layoutDirection).toPx() }
        val paddingEndPx = with(density) { boundsPadding.calculateRightPadding(layoutDirection).toPx() }
        val paddingTopPx = with(density) { boundsPadding.calculateTopPadding().toPx() }
        val paddingBottomPx = with(density) { boundsPadding.calculateBottomPadding().toPx() }

        val minX = paddingStartPx
        val minY = paddingTopPx
        val maxX = (constraints.maxWidth - fabSize.width - paddingEndPx)
            .coerceAtLeast(minX)
        val maxY = (constraints.maxHeight - fabSize.height - paddingBottomPx)
            .coerceAtLeast(minY)

        LaunchedEffect(fabSize, maxX, maxY, minX, minY) {
            if (fabSize != IntSize.Zero) {
                if (!initialized) {
                    position = Offset(maxX, maxY)
                    initialized = true
                } else {
                    position = Offset(
                        position.x.coerceIn(minX, maxX),
                        position.y.coerceIn(minY, maxY)
                    )
                }
            }
        }

        val dragModifier = Modifier
            .onSizeChanged { fabSize = it }
            .offset {
                IntOffset(position.x.roundToInt(), position.y.roundToInt())
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newX = (position.x + dragAmount.x).coerceIn(minX, maxX)
                    val newY = (position.y + dragAmount.y).coerceIn(minY, maxY)
                    position = Offset(newX, newY)
                }
            }

        Box(modifier = dragModifier) {
            content(Modifier)
        }
    }
}
