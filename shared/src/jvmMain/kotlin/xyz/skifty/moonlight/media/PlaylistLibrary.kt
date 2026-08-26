package xyz.skifty.moonlight.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import xyz.skifty.moonlight.api.ApiService

/** Caches the user's playlist list for the session, fetched at most once - the "Add to Playlist"
 *  context menu needs this every time it's opened, and refetching per-open would mean a server
 *  round-trip every single hover. [playlists] stays null until the first [ensureLoaded] call
 *  completes, distinguishing "not loaded yet" from "loaded, and you simply have none".
 *
 *  Deliberately not unified with Sidebar's own separate `getPlaylists()` fetch for its sidebar
 *  list - that would be a wider playlist-data-layer refactor, out of scope for what this exists
 *  for. */
class PlaylistLibrary {

    var playlists: List<PlaylistInfo>? by mutableStateOf(null)
        private set

    // Not Compose state - only ever read inside a suspend call at click time, never rendered
    // directly, so there's nothing that needs to observe it changing.
    private val songIdsByPlaylistId = mutableMapOf<String, Set<String>>()

    suspend fun ensureLoaded(apiService: ApiService) {
        if (playlists == null) {
            playlists = apiService.getPlaylists()
        }
    }

    /** Unconditionally re-fetches [playlists], unlike [ensureLoaded] - for whenever something
     *  that could change a playlist's own metadata (its cover art in particular, which Navidrome
     *  can auto-derive from a collage of the songs it contains) just happened, e.g. a song being
     *  added to one. Every reader of [playlists] (Sidebar included) picks this up automatically
     *  on its next recomposition, since it's the same Compose state either way. */
    suspend fun refreshPlaylists(apiService: ApiService) {
        playlists = apiService.getPlaylists()
    }

    /** Whether [songId] is already in [playlistId] - fetches and caches that playlist's current
     *  song list the first time it's asked about a given playlist, reused for every later call
     *  this session (kept in sync afterward by [recordSongAdded]). */
    suspend fun containsSong(apiService: ApiService, playlistId: String, songId: String): Boolean {
        val songIds = songIdsByPlaylistId.getOrPut(playlistId) {
            apiService.getPlaylist(playlistId).songs
                .mapNotNull { song -> song.songId }
                .toSet()
        }
        return songId in songIds
    }

    /** Call after successfully adding [songId] to [playlistId], so this session's cache doesn't
     *  drift from the server and immediately re-attempting the same add is still caught. */
    fun recordSongAdded(playlistId: String, songId: String) {
        val existing = songIdsByPlaylistId[playlistId]
            ?: return // never fetched this playlist - nothing to keep in sync yet
        songIdsByPlaylistId[playlistId] = existing + songId
    }

    /** Call after successfully removing [songId] from [playlistId] - the mirror of
     *  [recordSongAdded], so a song removed and then immediately re-added isn't wrongly skipped
     *  as "already there" by [containsSong]. */
    fun recordSongRemoved(playlistId: String, songId: String) {
        val existing = songIdsByPlaylistId[playlistId]
            ?: return
        songIdsByPlaylistId[playlistId] = existing - songId
    }

}
