package xyz.skifty.mani.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Owns the current playback queue - which songs are queued, in what order, and where playback
 * currently is within them - and drives [audioPlayer] to actually play the right one. [audioPlayer]
 * itself stays completely unaware queues exist; it just plays whatever song it's told to.
 */
class PlaybackQueue(
    private val audioPlayer: AudioPlayer,
    private val activeSongInfo: SongInfo,
) {

    var songs: List<SongInfo> by mutableStateOf(emptyList())
        private set

    // Identifies which playlist this queue was last started from - null both before any queue has
    // started and for the Liked Songs pseudo-playlist, matching PlaylistScreen's own playlistId
    // convention. Lets a playlist screen tell whether *it* is the one currently playing, to show
    // a pause icon instead of play - always check alongside songs.isNotEmpty() too, since null
    // alone doesn't distinguish "no queue yet" from "currently playing Liked Songs".
    var currentSourceId: String? by mutableStateOf(null)
        private set

    // Indices into `songs`, in play order - identity order normally, shuffled (anchored at
    // whichever song was just started) when shuffleEnabled.
    private var playOrder: List<Int> by mutableStateOf(emptyList())

    // Index into playOrder, not into songs directly.
    var currentPosition: Int by mutableStateOf(-1)
        private set

    var shuffleEnabled: Boolean by mutableStateOf(false)
        private set

    // Backing state for `loopMode` below - named differently so its Kotlin-generated setter
    // doesn't clash with the setLoopMode(LoopMode) function's identical JVM signature.
    private var loopModeState: LoopMode by mutableStateOf(LoopMode.OFF)
    val loopMode: LoopMode get() = loopModeState

    val hasNext: Boolean
        get() = songs.isNotEmpty() && (loopMode == LoopMode.ALL || currentPosition < playOrder.lastIndex)

    val hasPrevious: Boolean
        get() = songs.isNotEmpty() && (loopMode == LoopMode.ALL || currentPosition > 0)

    /** Replaces the queue with [newSongs] (sourced from [sourceId] - a playlist id, or null for
     *  Liked Songs, matching PlaylistScreen's own convention) and starts playing [startIndex] -
     *  songs before it stay reachable via [previous], songs after via [next] (or, if shuffle is
     *  on, everything but [startIndex] is shuffled, with [startIndex] anchored first). */
    fun start(newSongs: List<SongInfo>, startIndex: Int, sourceId: String?) {
        if (newSongs.isEmpty() || startIndex !in newSongs.indices) {
            return
        }
        songs = newSongs
        currentSourceId = sourceId
        playOrder = buildPlayOrder(
            size = newSongs.size,
            shuffle = shuffleEnabled,
            anchor = startIndex,
        )
        currentPosition = playOrder.indexOf(startIndex)
            .coerceAtLeast(0)
        playCurrent()
    }

    /** Appends [song] to the end of the current queue without interrupting playback. If nothing's
     *  queued yet, there's nothing to append to - starts a fresh single-song queue instead, same
     *  as [start] would for a plain "play this song now". */
    fun addToEnd(song: SongInfo) {
        if (songs.isEmpty()) {
            start(listOf(song), 0, sourceId = null)
            return
        }
        songs = songs + song
        playOrder = playOrder + songs.lastIndex
    }

    fun setShuffle(enabled: Boolean) {
        if (shuffleEnabled == enabled) {
            return
        }
        shuffleEnabled = enabled
        if (songs.isEmpty()) {
            return
        }
        val currentSongIndex = playOrder.getOrElse(currentPosition) { 0 }
        playOrder = buildPlayOrder(
            size = songs.size,
            shuffle = enabled,
            anchor = currentSongIndex,
        )
        currentPosition = playOrder.indexOf(currentSongIndex)
            .coerceAtLeast(0)
    }

    fun setLoopMode(mode: LoopMode) {
        loopModeState = mode
    }

    /** Cycles OFF -> ALL -> ONE -> OFF, for the in-app loop button. */
    fun cycleLoopMode() {
        loopModeState = when (loopMode) {
            LoopMode.OFF -> LoopMode.ALL
            LoopMode.ALL -> LoopMode.ONE
            LoopMode.ONE -> LoopMode.OFF
        }
    }

    /** Manual skip forward - ignores [LoopMode.ONE] (repeat-one only affects [onTrackFinished]),
     *  wraps to the start only when [LoopMode.ALL]. */
    fun next() {
        if (songs.isEmpty()) {
            return
        }
        val nextPosition = currentPosition + 1
        currentPosition = when {
            nextPosition <= playOrder.lastIndex -> nextPosition
            loopMode == LoopMode.ALL -> 0
            else -> return
        }
        playCurrent()
    }

    /** Manual skip backward - same rules as [next]. */
    fun previous() {
        if (songs.isEmpty()) {
            return
        }
        val previousPosition = currentPosition - 1
        currentPosition = when {
            previousPosition >= 0 -> previousPosition
            loopMode == LoopMode.ALL -> playOrder.lastIndex
            else -> return
        }
        playCurrent()
    }

    /** Called when the current track finishes naturally - replays it if [LoopMode.ONE], otherwise
     *  behaves like [next]. */
    fun onTrackFinished() {
        if (songs.isEmpty()) {
            return
        }
        if (loopMode == LoopMode.ONE) {
            playCurrent()
        } else {
            next()
        }
    }

    private fun playCurrent() {
        val songIndex = playOrder.getOrNull(currentPosition)
            ?: return
        val songInfo = songs.getOrNull(songIndex)
            ?: return
        audioPlayer.play(songInfo, activeSongInfo)
    }

    private fun buildPlayOrder(size: Int, shuffle: Boolean, anchor: Int): List<Int> {
        val indices = (0 until size).toMutableList()
        if (!shuffle) {
            return indices
        }
        // Keep `anchor` first so shuffling doesn't yank the currently (or about-to-be) playing
        // track out from under the listener.
        indices.remove(anchor)
        indices.shuffle()
        return listOf(anchor) + indices
    }

}
