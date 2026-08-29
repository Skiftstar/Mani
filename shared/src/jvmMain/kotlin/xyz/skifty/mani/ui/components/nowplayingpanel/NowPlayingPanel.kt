package xyz.skifty.mani.ui.components.nowplayingpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.now_playing_panel_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.AutoHidingScrollbar
import xyz.skifty.mani.ui.components.nowplayingpanel.components.NextInQueueCard
import xyz.skifty.mani.ui.components.nowplayingpanel.components.PanelCoverArt
import xyz.skifty.mani.ui.components.nowplayingpanel.components.PanelSongDetails
import xyz.skifty.mani.ui.components.nowplayingpanel.components.PanelTrackHeader

/** How wide the whole window needs to be for the panel to fit alongside the sidebar and main
 *  content without cramping either - see JvmApp, which hides the panel entirely below this. */
val NOW_PLAYING_PANEL_WIDTH = 320.dp

/** Right-side "Now Playing" panel, shown on desktop whenever a song is active - a title, cover
 *  art, title/artist with a like toggle, quality/play-count details, and a "Next in Queue"
 *  preview pinned to the bottom when there's an upcoming track. Mirrors Sidebar's fixed-width-
 *  Column-plus-VerticalDivider structure, but anchored to the trailing edge instead of the
 *  leading one. The cover art/title/details section scrolls independently (with the same
 *  auto-hiding scrollbar the main content area uses) so a long title/artist or extra song info
 *  never pushes the "Next in Queue" preview out of the panel entirely. */
@Composable
fun NowPlayingPanel(
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    activeSongInfo: SongInfo,
    playbackQueue: PlaybackQueue,
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .width(NOW_PLAYING_PANEL_WIDTH)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(Res.string.now_playing_panel_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    PanelCoverArt(
                        songInfo = activeSongInfo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    PanelTrackHeader(
                        activeSongInfo = activeSongInfo,
                        apiService = apiService,
                        playlistLibrary = playlistLibrary,
                    )
                    Spacer(Modifier.height(12.dp))
                    PanelSongDetails(songInfo = activeSongInfo)
                }
                AutoHidingScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }

            playbackQueue.nextSong?.let { nextSong ->
                Spacer(Modifier.height(16.dp))
                NextInQueueCard(
                    songInfo = nextSong,
                    onClick = {
                        playbackQueue.next()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
