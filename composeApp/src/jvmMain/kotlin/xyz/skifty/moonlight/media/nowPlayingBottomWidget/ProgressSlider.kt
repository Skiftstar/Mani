package xyz.skifty.moonlight.media.nowPlayingBottomWidget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import xyz.skifty.moonlight.media.DesktopAudioPlayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ProgressSlider(audioPlayer: DesktopAudioPlayer, positionMs: Long, maxLengthMs: Long, isDragging: Boolean, setProgress: (Long) -> Unit, setDragging: (Boolean) -> Unit) {

    val fractionFromPlayer =
        if (maxLengthMs > 0) positionMs.toFloat() / maxLengthMs else 0f

    var sliderFraction by remember { mutableStateOf(fractionFromPlayer) }

    LaunchedEffect(fractionFromPlayer) {
        if (!isDragging) {
            sliderFraction = fractionFromPlayer
        }
    }

    var isHovered by remember { mutableStateOf(false) }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        label = "thumbSize",
    )

    CompositionLocalProvider(
    LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
        Box(
            modifier = Modifier
            .fillMaxWidth()
                .height(15.dp)
                .onPointerEvent(PointerEventType.Enter, onEvent = {
                    isHovered = true
                    false
                })
                .onPointerEvent(PointerEventType.Exit, onEvent = {
                    isHovered = false
                    false
                })
                .layout { measurable, constraints ->
                    val thumbSize = 15.dp.roundToPx()
                    val newContraints = constraints.copy(
                        minWidth = constraints.maxWidth + thumbSize,
                        maxWidth = constraints.maxWidth + thumbSize
                    )

                    val placeable = measurable.measure(newContraints)

                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
        ) {


            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(20.dp),
                colors = SliderDefaults.colors(
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .graphicsLayer {
                                alpha = thumbAlpha
                            }
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(color = MaterialTheme.colorScheme.primary)
                    )
                },
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .height(6.dp)
                            .background(Color(0xFFAAAAAA))

                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(sliderFraction)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                value = sliderFraction,
                onValueChange = { fraction ->
                    setDragging(true)
                    sliderFraction = fraction
                },

                onValueChangeFinished = {
                    setDragging(false)
                    audioPlayer.seekFraction(sliderFraction)
                },
            )
        }
    }

}