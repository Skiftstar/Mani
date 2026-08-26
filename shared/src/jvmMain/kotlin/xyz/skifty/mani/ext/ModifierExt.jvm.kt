package xyz.skifty.mani.ext

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

// Compose has no single built-in "onRightClick" modifier - detected by hand, same reasoning
// ProgressSlider.kt's own hand-rolled gesture handling gives for not using the convenience
// detectors where they don't fit.
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.detectSecondaryClick(onSecondaryClick: (positionInModifier: Offset) -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    onSecondaryClick(event.changes.first().position)
                    event.changes.forEach { change -> change.consume() }
                }
            }
        }
    }
