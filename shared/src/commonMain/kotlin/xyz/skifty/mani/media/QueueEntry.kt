package xyz.skifty.mani.media

/** One song in [PlaybackQueue.upcoming] - [position] is the entry's index into the queue's own
 *  (post-shuffle) play order, not into [PlaybackQueue.songs] - the stable identifier
 *  [PlaybackQueue.skipTo]/[PlaybackQueue.removeAt] expect. */
data class QueueEntry(
    val position: Int,
    val song: SongInfo,
)
