package xyz.skifty.mani.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val SCROLLBAR_WIDTH = 6.dp
private val SCROLLBAR_MINIMAL_HEIGHT = 24.dp
private const val SCROLLBAR_FADE_DELAY_MS = 1000L

/** A thin, square, auto-hiding vertical scrollbar for [scrollState] - visible while
 *  actively scrolling or hovered, fading out [SCROLLBAR_FADE_DELAY_MS] after both stop, rather
 *  than a permanently-visible OS-style one (matches e.g. Spotify's desktop scrollbar). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AutoHidingScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    var isHovered by remember { mutableStateOf(false) }
    var isRecentlyScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        isRecentlyScrolled = true
        delay(SCROLLBAR_FADE_DELAY_MS)
        isRecentlyScrolled = false
    }

    val alpha by animateFloatAsState(
        targetValue = if (isHovered || isRecentlyScrolled) 1f else 0f,
        label = "scrollbarAlpha",
    )

    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier
            .width(SCROLLBAR_WIDTH)
            .alpha(alpha)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false },
        style = ScrollbarStyle(
            minimalHeight = SCROLLBAR_MINIMAL_HEIGHT,
            thickness = SCROLLBAR_WIDTH,
            shape = RectangleShape,
            hoverDurationMillis = 300,
            unhoverColor = trackColor,
            hoverColor = trackColor,
        ),
    )
}
