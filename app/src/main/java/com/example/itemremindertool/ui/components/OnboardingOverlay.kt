package com.example.itemremindertool.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itemremindertool.R
import com.example.itemremindertool.ui.theme.ColorHelpers
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class OnboardingStep {
    HOME_TOP_BAR,
    HOME_SEARCH,
    HOME_STATS_CONTAINER_BUTTON,
    HOME_STATS_CONTAINER_PAGE,
    HOME_STATS_ITEM_BUTTON,
    HOME_STATS_ITEM_PAGE,
    HOME_STATS_SHOPPING_BUTTON,
    HOME_STATS_SHOPPING_PAGE,
    HOME_SIDEBAR_ADD,
    HOME_SIDEBAR_SAMPLE,
    WAREHOUSE_CHILDREN_BREADCRUMB,
    WAREHOUSE_TAG_FILTER,
    WAREHOUSE_LAYOUT_TOGGLE,
    WAREHOUSE_GRID_ITEM,
    WAREHOUSE_INFO_CARD,
    COMPLETE
}

enum class OnboardingAnchorKey {
    TOP_BAR,
    SEARCH_BOX,
    STAT_CONTAINER,
    STAT_ITEM,
    STAT_SHOPPING,
    STAT_PAGE_CONTAINER,
    STAT_PAGE_ITEM,
    STAT_PAGE_SHOPPING,
    SIDEBAR_ADD,
    SIDEBAR_SAMPLE,
    SUBWAREHOUSE_ROW,
    TAG_FILTER,
    TOP_BAR_LAYOUT_TOGGLE,
    GRID_ITEM,
    INFO_CARD
}

data class OnboardingHint(
    val title: String,
    val description: String,
    val requiresClick: Boolean,
    val showFinger: Boolean = requiresClick,
    val showTapToContinue: Boolean = !requiresClick
)

data class HighlightedArea(
    val rect: Rect,
    val shape: HighlightShape = HighlightShape.RECTANGLE,
    val paddingDp: Float = 8f,
    val cornerRadiusDp: Float = 12f
)

enum class HighlightShape {
    RECTANGLE,
    CIRCLE
}

@Composable
fun OnboardingOverlay(
    hint: OnboardingHint,
    highlightedArea: HighlightedArea?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val highlightRect = highlightedArea?.let { area ->
        with(density) {
            val paddingPx = area.paddingDp.dp.toPx()
            Rect(
                left = area.rect.left - paddingPx,
                top = area.rect.top - paddingPx,
                right = area.rect.right + paddingPx,
                bottom = area.rect.bottom + paddingPx
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val highlightCornerRadius = with(density) { (highlightedArea?.cornerRadiusDp ?: 12f).dp.toPx() }
        val fingerSize = 36.dp
        val bubblePadding = 12.dp
        val maskColor = Color.Black.copy(alpha = 0.55f)
        val highlightStrokeColor = Color.White.copy(alpha = 0.8f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(hint.requiresClick, highlightRect) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: continue
                            val position = change.position
                            val insideHighlight = highlightRect?.contains(position) == true
                            val isTapUp = change.changedToUp()

                            if (insideHighlight) {
                                if (!hint.requiresClick && isTapUp) {
                                    onNext()
                                }
                                // 允许穿透点击到目标区域
                                continue
                            }

                            event.changes.forEach { it.consume() }
                            if (!hint.requiresClick && isTapUp) {
                                onNext()
                            }
                        }
                    }
                }
        ) {
            drawRect(maskColor)

            highlightRect?.let { rect ->
                if (highlightedArea?.shape == HighlightShape.CIRCLE) {
                    val radius = max(rect.width, rect.height) / 2f
                    drawCircle(
                        color = Color.Transparent,
                        radius = radius,
                        center = rect.center,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        color = highlightStrokeColor,
                        radius = radius,
                        center = rect.center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(highlightCornerRadius, highlightCornerRadius),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = highlightStrokeColor,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(highlightCornerRadius, highlightCornerRadius),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.onboarding_skip),
                color = Color.White
            )
        }

        if (highlightRect != null && hint.showFinger) {
            val fingerOffset = with(density) {
                IntOffset(
                    x = (highlightRect.center.x - fingerSize.toPx() / 2f).roundToInt(),
                    y = (highlightRect.center.y - fingerSize.toPx() / 2f).roundToInt()
                )
            }
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(fingerSize)
                    .offset { fingerOffset }
            )
        }

        OnboardingHintBubble(
            hint = hint,
            highlightRect = highlightRect,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun OnboardingHintBubble(
    hint: OnboardingHint,
    highlightRect: Rect?,
    maxWidthPx: Float,
    maxHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var bubbleSize by remember { mutableStateOf(IntSize.Zero) }
    val bubblePaddingPx = with(density) { 12.dp.toPx() }

    val bubbleOffset = remember(highlightRect, bubbleSize, maxWidthPx, maxHeightPx) {
        if (highlightRect == null) {
            IntOffset(0, 0)
        } else {
            val centerX = highlightRect.center.x
            val preferredX = (centerX - bubbleSize.width / 2f)
            val clampedX = min(
                max(preferredX, bubblePaddingPx),
                maxWidthPx - bubbleSize.width - bubblePaddingPx
            )

            val belowY = highlightRect.bottom + bubblePaddingPx
            val aboveY = highlightRect.top - bubblePaddingPx - bubbleSize.height
            val useBelow = belowY + bubbleSize.height <= maxHeightPx
            val finalY = if (useBelow) belowY else max(aboveY, bubblePaddingPx)

            IntOffset(clampedX.roundToInt(), finalY.roundToInt())
        }
    }

    val bubbleModifier = if (highlightRect == null) {
        modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    } else {
        modifier.offset { bubbleOffset }
    }

    Card(
        modifier = bubbleModifier
            .widthIn(max = 320.dp)
            .onSizeChanged { bubbleSize = it }
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(min = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = hint.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorHelpers.getGroup4TextColor()
            )
            Text(
                text = hint.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor(0.85f),
                lineHeight = 20.sp
            )
            if (hint.showTapToContinue) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.onboarding_tap_to_continue),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorHelpers.getGroup4TextColor(0.6f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}