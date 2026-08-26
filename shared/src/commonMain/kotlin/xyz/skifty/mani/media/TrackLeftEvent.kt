package xyz.skifty.mani.media

/** Describes a "song was left behind" transition - captured by an [AudioPlayer] right before the
 *  outgoing song's info is overwritten/cleared, using however much of it was actually listened to
 *  (see [AudioPlayer.listenedMs]). Covers a skip, a previous, starting a different queue
 *  mid-track, or an explicit stop - a natural finish is covered separately by
 *  [AudioPlayer.trackFinishedCount], though the queue's own auto-advance afterwards *also* raises
 *  this (see JvmApp's scrobbleIfNeeded, which no-ops the redundant second call rather than this
 *  needing to prevent it). */
data class TrackLeftEvent(
    val songId: String,
    val listenedMs: Long,
    val durationMs: Long,
)
