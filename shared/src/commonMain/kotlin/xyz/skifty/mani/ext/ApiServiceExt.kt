package xyz.skifty.mani.ext

import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Stars/unstars [songInfo] server-side, optimistically flipping [SongInfo.starred] first and
 *  reverting it if the request fails. Refreshes [playlistLibrary] on success - Liked Songs is
 *  itself a pseudo-playlist, so starring/unstarring changes its membership the same way adding or
 *  removing a song from a real playlist does, and its cache needs the same invalidation. Also
 *  patches [playbackQueue]'s own copy of this song, if it's currently queued - [songInfo] is very
 *  often a different SongInfo instance than whichever one the queue holds for the same song id
 *  (e.g. activeSongInfo vs. PlaybackQueue.songs), and without this, replaying that queued copy
 *  later (repeat-one, or switching back to it) would silently undo the toggle - see
 *  [PlaybackQueue.updateStarred]. Shared by every place a star/unstar toggle appears (song rows,
 *  the now-playing title, the now-playing panel) so they all get identical behavior for free. */
suspend fun ApiService.toggleStar(
    songInfo: SongInfo,
    playlistLibrary: PlaylistLibrary,
    playbackQueue: PlaybackQueue,
) {
    val songId = songInfo.songId
        ?: return
    val wasStarred = songInfo.starred
    songInfo.starred = !wasStarred
    val result = if (wasStarred) {
        unstar(songId)
    } else {
        star(songId)
    }
    if (result.isFailure) {
        songInfo.starred = wasStarred
    } else {
        playlistLibrary.refreshPlaylists(this)
        playbackQueue.updateStarred(songId, !wasStarred)
    }
}
