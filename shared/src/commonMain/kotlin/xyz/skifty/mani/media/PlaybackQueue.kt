package xyz.skifty.mani.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.toVibeProfileOrNull

// Autoplay prefetches once this few songs remain after the current one, and fetches this many
// similar songs at a time - see maybeFetchAutoplay().
private const val AUTOPLAY_TRIGGER_REMAINING_COUNT = 3
private const val AUTOPLAY_FETCH_COUNT = 20

/**
 * Owns the current playback queue - which songs are queued, in what order, and where playback
 * currently is within them - and drives [audioPlayer] to actually play the right one. [audioPlayer]
 * itself stays completely unaware queues exist; it just plays whatever song it's told to.
 */
class PlaybackQueue(
    private val audioPlayer: AudioPlayer,
    private val activeSongInfo: SongInfo,
    private val apiService: ApiService,
) {

    var songs: List<SongInfo> by mutableStateOf(emptyList())
        private set

    // Identifies which playlist this queue was last started from - null both before any queue has
    // started and for the Liked Songs pseudo-playlist, matching PlaylistScreen's own playlistId
    // convention. Lets a playlist screen tell whether *it* is the one currently playing, to show
    // a pause icon instead of play - always check alongside songs.isNotEmpty() AND
    // hasActiveSource too, since null alone doesn't distinguish "no queue yet"/"just a restored
    // session leftover" from "currently playing Liked Songs".
    var currentSourceId: String? by mutableStateOf(null)
        private set

    // True once the queue has actually been started via start() (a real playlist, or Liked Songs
    // with a null currentSourceId) - false for prepareSingle()'s session-restore case, which also
    // leaves currentSourceId null but was never really "playing Liked Songs" or anything else,
    // just a paused leftover from last session. Without this, opening Liked Songs and pressing
    // Play right after a restart would see currentSourceId == null (matching Liked Songs' own
    // sentinel) and wrongly conclude Liked Songs was already the active source, unpausing the
    // restored song instead of actually starting Liked Songs.
    var hasActiveSource: Boolean by mutableStateOf(false)
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

    // A fetched-but-not-yet-merged Autoplay batch, and the accumulated ids of every song ever
    // merged in from one - see maybeFetchAutoplay()/mergeAutoplaySongs(). autoplaySongIds is
    // never pruned; both upcoming/upcomingAutoplay below already only ever look at what's still
    // ahead of currentPosition regardless. Both need to be Compose state, not plain vars - a
    // Queue view reads autoplayPreview/upcomingAutoplay directly, and without mutableStateOf here
    // a fetch landing wouldn't recompose it until something else (like songs/playOrder on the
    // next skip) happened to change too.
    private var autoplayPending: List<SongInfo>? by mutableStateOf(null)
    private var autoplaySongIds: Set<String> by mutableStateOf(emptySet())

    // Songs the user has actually queued themselves - via start()/prepareSingle() (reset) or
    // addToEnd()/removeAt() (append/remove) - excluding anything Autoplay ever added. This, not
    // the full history in `songs`, is what maybeFetchAutoplay() averages a vibe profile over:
    // unlike `songs`, it's still non-empty no matter how many Autoplay batches deep playback
    // currently is (once you're deep into Autoplay, everything ahead in `songs` is Autoplay-
    // tagged), and unlike `songs` it actually shrinks when removeAt() removes a manual song, so
    // that correctly invalidates the average too.
    var manualSongs: List<SongInfo> by mutableStateOf(emptyList())
        private set

    // Also true at the very end of playOrder if an Autoplay batch is already fetched and waiting
    // to be merged in - see next() - so the skip-forward button doesn't gray out right when
    // Autoplay has more ready to play.
    val hasNext: Boolean
        get() = songs.isNotEmpty() &&
            (loopMode == LoopMode.ALL || currentPosition < playOrder.lastIndex || autoplayPending != null)

    val hasPrevious: Boolean
        get() = songs.isNotEmpty() && (loopMode == LoopMode.ALL || currentPosition > 0)

    /** The song [next] would skip to, without actually skipping - null if nothing's queued after
     *  the current position (mirrors [next]'s own wrap-on-[LoopMode.ALL] rule), for UI that wants
     *  to preview what's coming up (e.g. a "Next in Queue" panel). At the very end of playOrder,
     *  falls back to the first song of a pending Autoplay batch - same "don't hide what [next]
     *  would actually do" reasoning as [hasNext]. */
    val nextSong: SongInfo?
        get() {
            val nextPosition = currentPosition + 1
            val position = when {
                nextPosition <= playOrder.lastIndex -> nextPosition
                loopMode == LoopMode.ALL -> 0
                else -> return autoplayPending?.firstOrNull()
            }
            val songIndex = playOrder.getOrNull(position)
                ?: return null
            return songs.getOrNull(songIndex)
        }

    private fun upcomingEntries(): List<QueueEntry> =
        playOrder.drop(currentPosition + 1)
            .mapIndexedNotNull { offset, songIndex ->
                songs.getOrNull(songIndex)?.let { song -> QueueEntry(currentPosition + 1 + offset, song) }
            }

    /** Every manually-queued song still to come this pass, in actual play order (post-shuffle),
     *  each paired with its own [playOrder] index - the stable identifier [skipTo]/[removeAt]
     *  expect. Unlike [nextSong], deliberately does not wrap on [LoopMode.ALL] - this is "what's
     *  left in the queue," not an infinite preview, for a Queue view to list. Excludes any
     *  Autoplay-sourced songs - see [upcomingAutoplay] for those. */
    val upcoming: List<QueueEntry>
        get() = upcomingEntries().filterNot { entry -> entry.song.songId in autoplaySongIds }

    /** The Autoplay-sourced counterpart to [upcoming] - songs appended by [maybeFetchAutoplay]/
     *  [next], listed separately so a Queue view can show them under their own "Autoplay"
     *  section instead of blending them into the user's own queue. */
    val upcomingAutoplay: List<QueueEntry>
        get() = upcomingEntries().filter { entry -> entry.song.songId in autoplaySongIds }

    /** The Autoplay batch [maybeFetchAutoplay] has already fetched but that hasn't been reached
     *  (and so merged into [songs]/[playOrder] - see [next]) yet - exposed so a Queue view can
     *  show it under the same "Autoplay" section right away, once it's ready, rather than only
     *  once playback actually reaches it. Clicking or removing one of these goes through
     *  [skipToAutoplayPreview]/[removeFromAutoplayPreview], not [skipTo]/[removeAt] - these songs
     *  have no [playOrder] position yet. */
    val autoplayPreview: List<SongInfo>
        get() = autoplayPending.orEmpty()

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
        hasActiveSource = true
        playOrder = buildPlayOrder(
            size = newSongs.size,
            shuffle = shuffleEnabled,
            anchor = startIndex,
        )
        currentPosition = playOrder.indexOf(startIndex)
            .coerceAtLeast(0)
        // A fresh queue context shouldn't carry over an Autoplay batch (or the manual-song vibe
        // history it'd be based on) fetched for whatever was playing before.
        autoplayPending = null
        autoplaySongIds = emptySet()
        manualSongs = newSongs
        playCurrent()
    }

    /** Seeds the queue with a single already-loaded song without starting playback - the queue's
     *  analog of [AudioPlayer.prepare], for the one caller (restoring the last-played song as
     *  paused on startup) that loads a song directly through [AudioPlayer] rather than through
     *  [start]. Without this, the queue stays empty until the user separately opens a playlist and
     *  picks a song, so [next]/[previous]/[onTrackFinished] (loop-one in particular) silently do
     *  nothing the moment that restored song is resumed and finishes - confirmed by hand: this was
     *  the actual cause of a "loop-one just stops instead of replaying" report, not an
     *  [AudioPlayer]/mpv issue as it first appeared. */
    fun prepareSingle(song: SongInfo) {
        songs = listOf(song)
        currentSourceId = null
        hasActiveSource = false
        playOrder = listOf(0)
        currentPosition = 0
        autoplayPending = null
        autoplaySongIds = emptySet()
        manualSongs = listOf(song)
    }

    /** Queues [song] without interrupting playback - normally appended to the very end, but if
     *  the currently playing song is itself Autoplay-sourced (i.e. Autoplay is actively playing,
     *  not just pending - see [autoplaySongIds]), inserted right after the current song instead,
     *  so a manually queued song always plays next rather than being buried after the rest of the
     *  Autoplay batch. If nothing's queued yet, there's nothing to append to - starts a fresh
     *  single-song queue instead, same as [start] would for a plain "play this song now". Also
     *  appends to [manualSongs] and drops any still-pending Autoplay batch - a manually queued
     *  song changes the vibe profile Autoplay should be seeded from, so a stale pending batch
     *  needs recomputing, not just extending. */
    fun addToEnd(song: SongInfo) {
        if (songs.isEmpty()) {
            start(listOf(song), 0, sourceId = null)
            return
        }
        val currentSongId = playOrder.getOrNull(currentPosition)
            ?.let { songIndex -> songs.getOrNull(songIndex)?.songId }
        val insertRightAfterCurrent = currentSongId != null && currentSongId in autoplaySongIds

        val newSongIndex = songs.size
        songs = songs + song
        playOrder = if (insertRightAfterCurrent) {
            playOrder.toMutableList().apply { add(currentPosition + 1, newSongIndex) }
        } else {
            playOrder + newSongIndex
        }
        manualSongs = manualSongs + song
        autoplayPending = null
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
     *  wraps to the start only when [LoopMode.ALL]. Reaching the end of the queue while a
     *  Autoplay batch is ready ([autoplayPending]) merges it in and continues instead of
     *  stopping - this branch is only reachable under [LoopMode.OFF], since [LoopMode.ALL] wraps
     *  above it, so no explicit loop-mode check is needed here. */
    fun next() {
        if (songs.isEmpty()) {
            return
        }
        val nextPosition = currentPosition + 1
        currentPosition = when {
            nextPosition <= playOrder.lastIndex -> nextPosition
            loopMode == LoopMode.ALL -> 0
            else -> {
                val pending = autoplayPending
                    ?: return
                mergeAutoplaySongs(pending)
                nextPosition
            }
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

    /** Jumps straight to the [upcoming] entry at [position] - a "click to skip" from a Queue view.
     *  Ignores [LoopMode.ONE], same as [next]/[previous]. A no-op if [position] isn't a valid
     *  [playOrder] index. */
    fun skipTo(position: Int) {
        if (position !in playOrder.indices) {
            return
        }
        currentPosition = position
        playCurrent()
    }

    /** Removes the [upcoming] entry at [position] from the queue without affecting playback - a
     *  "remove from queue" action. Only ever an upcoming entry ([position] must be past
     *  [currentPosition]) - the currently-playing song can't be removed this way. [songs] (the
     *  identity list) is untouched, so no reindexing concerns even with shuffle active. A no-op if
     *  [position] isn't a removable index. If the removed song was manually queued (not
     *  Autoplay-sourced), also drops its first matching occurrence from [manualSongs] and any
     *  still-pending Autoplay batch - same "the vibe profile just changed" reasoning as
     *  [addToEnd]. Removing an Autoplay-sourced entry instead leaves both alone - it was never
     *  part of the profile to begin with. */
    fun removeAt(position: Int) {
        if (position <= currentPosition || position !in playOrder.indices) {
            return
        }
        val removedSongId = songs.getOrNull(playOrder[position])?.songId
        playOrder = playOrder.toMutableList().apply { removeAt(position) }
        if (removedSongId != null && removedSongId !in autoplaySongIds) {
            manualSongs = manualSongs.toMutableList().apply {
                val index = indexOfFirst { song -> song.songId == removedSongId }
                if (index != -1) {
                    removeAt(index)
                }
            }
            autoplayPending = null
        }
    }

    /** Patches the starred flag on whichever of [songs] shares [songId], if any - keeps a star
     *  toggle (which only ever mutates the specific SongInfo instance it's given, e.g.
     *  activeSongInfo) from being silently undone the next time this song is replayed from the
     *  queue's own, separate SongInfo instance - see [playCurrent]/[onTrackFinished]. */
    fun updateStarred(
        songId: String,
        starred: Boolean,
    ) {
        songs.firstOrNull { song -> song.songId == songId }
            ?.starred = starred
    }

    /** Increments the play count on whichever of [songs] shares [songId], if any - same
     *  "keep the queue's own separate SongInfo instance in sync" reasoning as [updateStarred]. */
    fun incrementPlayCount(songId: String) {
        songs.firstOrNull { song -> song.songId == songId }
            ?.let { song -> song.songPlayCount = (song.songPlayCount ?: 0) + 1 }
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

    /** Prefetches ~[AUTOPLAY_FETCH_COUNT] songs matching the average vibe profile of
     *  [manualSongs] (the songs the user has actually queued, not Autoplay's own additions), once
     *  [upcoming] has [AUTOPLAY_TRIGGER_REMAINING_COUNT] or fewer songs left in it - excluding
     *  every song already anywhere in the queue ([manualSongs] plus [autoplaySongIds]) so nothing
     *  gets suggested twice. A no-op if a batch is already pending ([addToEnd]/[removeAt] clear
     *  it whenever the manual queue - and so the average - changes, which is what makes this
     *  recompute rather than staying stale), if [loopMode] isn't [LoopMode.OFF] (nothing to
     *  prefetch for - see [next]'s doc comment on why the actual merge is structurally OFF-only
     *  regardless), if none of [manualSongs] has vibe data (e.g. a stock server), or if the fetch
     *  comes back empty. Callers are expected to invoke this reactively as the queue changes, e.g.
     *  from a `LaunchedEffect`. */
    suspend fun maybeFetchAutoplay() {
        if (loopMode != LoopMode.OFF || autoplayPending != null) {
            return
        }
        val remaining = playOrder.size - currentPosition - 1
        if (remaining > AUTOPLAY_TRIGGER_REMAINING_COUNT) {
            return
        }
        val vibeProfile = averageVibeProfile(manualSongs)
            ?: return
        val excludeSongIds = (manualSongs.mapNotNull { song -> song.songId } + autoplaySongIds).distinct()
        val similarSongs = apiService.getVibeSimilarSongs(vibeProfile, excludeSongIds, AUTOPLAY_FETCH_COUNT)
        if (similarSongs.isNotEmpty()) {
            autoplayPending = similarSongs
        }
    }

    /** The average of each of the 7 VibeNet stats across whichever of [candidateSongs] actually
     *  has vibe data (see [toVibeProfileOrNull]) - null if none of them do (e.g. a stock server
     *  that doesn't return these fields at all). */
    private fun averageVibeProfile(candidateSongs: List<SongInfo>): VibeProfile? {
        val profiles = candidateSongs.mapNotNull { song -> song.toVibeProfileOrNull() }
        if (profiles.isEmpty()) {
            return null
        }
        return VibeProfile(
            acousticness = profiles.map { profile -> profile.acousticness }.average(),
            danceability = profiles.map { profile -> profile.danceability }.average(),
            energy = profiles.map { profile -> profile.energy }.average(),
            instrumentalness = profiles.map { profile -> profile.instrumentalness }.average(),
            liveness = profiles.map { profile -> profile.liveness }.average(),
            speechiness = profiles.map { profile -> profile.speechiness }.average(),
            valence = profiles.map { profile -> profile.valence }.average(),
        )
    }

    /** Drops the still-pending Autoplay batch, if any, without touching anything already merged
     *  into [songs]/[playOrder] - used when Autoplay gets disabled entirely (see
     *  `AppShellState.setAutoplayEnabled`), so a batch fetched moments before turning it off
     *  doesn't silently get played anyway once [next] reaches the end of the queue. */
    fun clearAutoplayPreview() {
        autoplayPending = null
    }

    /** Appends a fetched Autoplay [batch] to [songs]/[playOrder] and marks its songs as
     *  Autoplay-sourced for [upcomingAutoplay] - called from [next] once the user's own queue
     *  runs out, and from [skipToAutoplayPreview] to promote the preview batch early. */
    private fun mergeAutoplaySongs(batch: List<SongInfo>) {
        val firstNewIndex = songs.size
        songs = songs + batch
        playOrder = playOrder + (firstNewIndex until songs.size)
        autoplaySongIds = autoplaySongIds + batch.mapNotNull { song -> song.songId }
        autoplayPending = null
    }

    /** Jumps straight to [songId] within the still-pending [autoplayPreview] batch, promoting the
     *  whole batch into the queue first ([mergeAutoplaySongs]) - lets the Autoplay preview
     *  section be clicked before it's naturally reached. A no-op if nothing's pending or [songId]
     *  isn't in it. */
    fun skipToAutoplayPreview(songId: String) {
        val pending = autoplayPending
            ?: return
        if (pending.none { song -> song.songId == songId }) {
            return
        }
        mergeAutoplaySongs(pending)
        val songIndex = songs.indexOfLast { song -> song.songId == songId }
        val position = playOrder.indexOf(songIndex)
        if (position == -1) {
            return
        }
        currentPosition = position
        playCurrent()
    }

    /** Drops [songId] from the still-pending [autoplayPreview] batch, before it's merged into the
     *  queue - the preview section's "remove" action. A no-op once the batch has already been
     *  merged (use [removeAt] instead). Clears [autoplayPending] back to null, not an empty list,
     *  if that was the last preview song - [maybeFetchAutoplay] only re-fetches once it's null. */
    fun removeFromAutoplayPreview(songId: String) {
        autoplayPending = autoplayPending
            ?.filterNot { song -> song.songId == songId }
            ?.takeIf { remaining -> remaining.isNotEmpty() }
    }

    /** [AudioPlayer.resume]'s queue-aware counterpart - if [audioPlayer] already played the
     *  current track through to its end ([AudioPlayer.hasReachedEnd]), restarts it from the
     *  beginning instead of trying to resume something that's already finished playing, which
     *  resume() alone can't do (there's nothing left running to unpause) - this is what was
     *  leaving playback stuck "playing" at the very end of the track after pressing play again
     *  with nothing next queued. A no-op if already playing, same as [AudioPlayer.resume]. */
    fun resume() {
        if (audioPlayer.isPlaying) {
            return
        }
        if (audioPlayer.hasReachedEnd) {
            playCurrent()
        } else {
            audioPlayer.resume()
        }
    }

    /** [AudioPlayer.togglePlayPause]'s queue-aware counterpart - see [resume]. */
    fun togglePlayPause() {
        if (audioPlayer.isPlaying) {
            audioPlayer.pauseOnly()
        } else {
            resume()
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
