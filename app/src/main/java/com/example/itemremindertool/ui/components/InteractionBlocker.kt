package com.example.itemremindertool.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

data class ScreenInteractionBlocker(
    val isBlocked: Boolean,
    val handleBack: (action: () -> Unit) -> Unit,
    val handleForward: (action: () -> Unit) -> Unit
)

@Composable
fun rememberScreenInteractionBlocker(delayMs: Long = 150): ScreenInteractionBlocker {
    var isEntryBlocked by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }
    var resumeTick by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTick += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeTick) {
        isEntryBlocked = true
        isExiting = false
        delay(delayMs)
        isEntryBlocked = false
    }

    val isBlocked = isEntryBlocked || isExiting
    val handleBack: (action: () -> Unit) -> Unit = { action ->
        if (!isExiting) {
            isExiting = true
        }
        action()
    }
    val handleForward: (action: () -> Unit) -> Unit = { action ->
        if (!isBlocked) {
            isExiting = true
            action()
        }
    }

    return ScreenInteractionBlocker(
        isBlocked = isBlocked,
        handleBack = handleBack,
        handleForward = handleForward
    )
}

fun Modifier.blockUserInput(blocked: Boolean): Modifier {
    if (!blocked) return this
    return pointerInput(blocked) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach {
                    it.consumeDownChange()
                    it.consumePositionChange()
                }
            }
        }
    }
}
