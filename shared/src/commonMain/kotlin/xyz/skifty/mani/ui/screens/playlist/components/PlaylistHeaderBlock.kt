package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistDetails
import xyz.skifty.mani.media.SongInfo

/** The cover art/title/metadata/play-button block at the top of [xyz.skifty.mani.ui.screens.playlist.PlaylistScreen] -
 *  a per-platform layout (desktop's existing side-by-side [PlaylistHeader] + [PlaylistActionsRow],
 *  vs. Android's large-centered-cover, stacked layout matching mobile conventions), rather than
 *  one shared layout compromising on either. [filteredSongs] is what the song count/runtime shown
 *  here are computed from (not [details]' own full list), so they stay accurate while a search
 *  filter is active, with a "Filtered" line shown whenever [searchQuery] is non-blank.
 *  [searchQuery]/[onSearchQueryChange] also back the desktop-only playlist-search field itself
 *  (in [PlaylistActionsRow]) - threaded through this expect/actual boundary since that's the only
 *  shared ancestor between where the field lives and where its filtered results are consumed
 *  (`PlaylistScreen`'s song table). Android's actual only reads [searchQuery] for the "Filtered"
 *  line and doesn't call [onSearchQueryChange] at all, since it has no playlist-search field of
 *  its own here (see `PlaylistScrollContainer` for where that lives on Android instead). */
@Composable
expect fun PlaylistHeaderBlock(
    details: PlaylistDetails,
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    playlistId: String?,
    filteredSongs: List<SongInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
)
