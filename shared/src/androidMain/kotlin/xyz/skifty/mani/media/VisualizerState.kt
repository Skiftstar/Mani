package xyz.skifty.mani.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// How many independent bars make up each ring - a shared constant since VisualizerState (which
// sizes this array), AudioSessionVisualizer (which fills it) and AudioVisualizer (which draws it)
// all need to agree on it.
const val VISUALIZER_SEGMENTS_PER_RING = 16

/** Live readout bridging [AudioSessionVisualizer] (the only writer) to the Now Playing audio
 *  visualizer composable (the reader) - a Koin singleton so it survives NowPlayingScreen being
 *  disposed/recomposed. [bars] is [VISUALIZER_SEGMENTS_PER_RING] independent values spanning the
 *  whole captured spectrum - every ring draws this same spectrum (just at its own radius/color/
 *  rotation direction), rather than each ring being restricted to its own frequency slice.
 *  Normalized (0f..1f), smoothed frame-to-frame - see [AudioSessionVisualizer]'s own doc comment for
 *  the full normalization approach. The whole array is replaced (never mutated in place) on every
 *  update, since Compose's snapshot system only notices a new array reference, not an in-place
 *  element write. */
class VisualizerState {

    var bars: FloatArray by mutableStateOf(FloatArray(VISUALIZER_SEGMENTS_PER_RING))
        internal set

}
