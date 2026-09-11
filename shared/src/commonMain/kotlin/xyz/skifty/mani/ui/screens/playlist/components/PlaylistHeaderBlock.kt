package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistDetails

/** The cover art/title/metadata/play-button block at the top of [xyz.skifty.mani.ui.screens.playlist.PlaylistScreen] -
 *  a per-platform layout (desktop's existing side-by-side [PlaylistHeader] + [PlaylistActionsRow],
 *  vs. Android's large-centered-cover, stacked layout matching mobile conventions), rather than
 *  one shared layout compromising on either. [searchQuery]/[onSearchQueryChange] back the
 *  desktop-only playlist-search field in [PlaylistActionsRow] - threaded through this expect/
 *  actual boundary since that's the only shared ancestor between where the field lives and where
 *  its filtered results are consumed (`PlaylistScreen`'s song table); Android's actual doesn't use
 *  either, since it has no playlist-search UI. */
@Composable
expect fun PlaylistHeaderBlock(
    details: PlaylistDetails,
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    playlistId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
)
