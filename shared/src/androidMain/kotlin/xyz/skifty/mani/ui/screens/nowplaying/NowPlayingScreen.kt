package xyz.skifty.mani.ui.screens.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.nowplaying.components.ProgressSlider
import xyz.skifty.mani.ui.screens.nowplaying.components.AlbumArt
import xyz.skifty.mani.ui.screens.nowplaying.components.NowPlayingControls
import xyz.skifty.mani.ui.screens.nowplaying.components.NowPlayingTitleRow

// How far up (as a fraction of the screen's own height) a drag needs to go before it counts as a
// deliberate swipe-to-navigate rather than a small/accidental movement that should snap back.
private const val SWIPE_UP_THRESHOLD_FRACTION = 0.12f

// Shared by the title row and the progress bar below - AlbumArt applies this same value itself
// (see its own padding), so all three line up on the same edges instead of the progress bar
// (or title) ending up wider than the cover art.
private val NOW_PLAYING_HORIZONTAL_MARGIN = 32.dp

/** The full-screen Now Playing player - cover art, title/artist with a like toggle, progress bar,
 *  and shuffle/previous/play-pause/next/loop controls. Reached from MiniPlayerBar's tap target
 *  via an animated expand transition (see AndroidApp.kt). Minimized back to wherever the user was
 *  via the system back gesture/button ([onCollapse]) - there's no on-screen minimize button.
 *  Swiping up instead navigates to the current queue's playlist ([onSwipeUp]), with the whole
 *  screen following the drag like a real interactive screen transition. */
@Composable
fun NowPlayingScreen(
    apiService: ApiService,
    audioPlayer: AudioPlayer,
    activeSongInfo: SongInfo,
    playbackQueue: PlaybackQueue,
    playlistLibrary: PlaylistLibrary,
    onCollapse: () -> Unit,
    onSwipeUp: () -> Unit,
    // Applied by AndroidApp.kt to pair this screen with MiniPlayerBar's own root via a matching
    // Modifier.sharedBounds() key, so the expand/collapse transition morphs between the two.
    cardModifier: Modifier = Modifier,
) {
    BackHandler(onBack = onCollapse)

    val scope = rememberCoroutineScope()
    var containerHeightPx by remember { mutableIntStateOf(0) }
    val offsetY = remember { Animatable(0f) }

    Column(
        modifier = cardModifier
            .fillMaxSize()
            .onSizeChanged { size -> containerHeightPx = size.height }
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetY.snapTo((offsetY.value + dragAmount).coerceAtMost(0f))
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val threshold = -containerHeightPx * SWIPE_UP_THRESHOLD_FRACTION
                            if (offsetY.value < threshold) {
                                offsetY.animateTo(
                                    targetValue = -containerHeightPx.toFloat(),
                                    animationSpec = tween(durationMillis = 220),
                                )
                                onSwipeUp()
                            } else {
                                offsetY.animateTo(0f, animationSpec = tween(durationMillis = 200))
                            }
                        }
                    },
                )
            },
        verticalArrangement = Arrangement.Center,
    ) {
        AlbumArt(coverArtUrl = activeSongInfo.songCoverArtUrl)

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NOW_PLAYING_HORIZONTAL_MARGIN),
        ) {
            NowPlayingTitleRow(
                activeSongInfo = activeSongInfo,
                apiService = apiService,
                playlistLibrary = playlistLibrary,
            )
        }

        // Extra breathing room before the progress bar, on top of ProgressSlider's own internal
        // top-aligned track.
        Spacer(modifier = Modifier.height(32.dp))

        NowPlayingProgress(audioPlayer = audioPlayer, horizontalMargin = NOW_PLAYING_HORIZONTAL_MARGIN)

        Spacer(modifier = Modifier.height(8.dp))

        NowPlayingControls(audioPlayer = audioPlayer, playbackQueue = playbackQueue)
    }
}

@Composable
private fun NowPlayingProgress(audioPlayer: AudioPlayer, horizontalMargin: Dp) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) } // avoid /0
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(audioPlayer.playbackStartedCount) {
        if (audioPlayer.playbackStartedCount > 0) {
            positionMs = audioPlayer.lastConfirmedStartPositionMs
            durationMs = 1L
        }
    }

    LaunchedEffect(audioPlayer) {
        while (true) {
            if (!isDragging) {
                positionMs = audioPlayer.currentPosition()
                val d = audioPlayer.length()
                if (d > 0) durationMs = d
            }
            delay(200)
        }
    }

    ProgressSlider(
        audioPlayer = audioPlayer,
        positionMs = positionMs,
        maxLengthMs = durationMs,
        isDragging = isDragging,
        setDragging = { dragging -> isDragging = dragging },
        setProgress = { newPos -> positionMs = newPos },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin),
    )
}
