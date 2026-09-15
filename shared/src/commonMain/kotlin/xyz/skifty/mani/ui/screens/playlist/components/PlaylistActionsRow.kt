package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_clear_search
import mani.shared.generated.resources.cd_pause
import mani.shared.generated.resources.cd_play
import mani.shared.generated.resources.cd_search
import mani.shared.generated.resources.playlist_search_placeholder
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.trackTextFieldFocus
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.SongInfo

private val SEARCH_FIELD_WIDTH = 220.dp
private val SEARCH_FIELD_HEIGHT = 40.dp
private val SEARCH_FIELD_ICON_SIZE = 18.dp

/** Transport actions for the whole playlist, sitting in their own row below the header. Shows a
 *  pause icon instead of play whenever *this* playlist is the one currently playing. Also hosts
 *  the desktop-only playlist-search affordance on the row's trailing end - a plain search icon
 *  that expands into a text field (see [searchQuery]/[onSearchQueryChange], hoisted up to
 *  `PlaylistScreen` so it can filter what the song table below shows) - self-drawn on
 *  [BasicTextField] like [SearchBar][xyz.skifty.mani.ui.components.SearchBar], not
 *  Material3's `OutlinedTextField`, for the same no-outline/static-border/fading-placeholder look
 *  established there. */
@Composable
fun PlaylistActionsRow(
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    playlistId: String?,
    songs: List<SongInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isThisPlaylistActive = playbackQueue.songs.isNotEmpty() && playbackQueue.currentSourceId == playlistId
    val isPlaying = isThisPlaylistActive && audioPlayer.isPlaying
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFieldFocusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FilledIconButton(
            onClick = {
                if (isThisPlaylistActive) {
                    audioPlayer.togglePlayPause()
                } else {
                    playbackQueue.start(songs, 0, playlistId)
                }
            },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (isPlaying) Res.string.cd_pause else Res.string.cd_play),
            )
        }

        if (isSearchExpanded) {
            Row(
                modifier = Modifier
                    .width(SEARCH_FIELD_WIDTH)
                    .height(SEARCH_FIELD_HEIGHT)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(percent = 50),
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.cd_search),
                    modifier = Modifier.size(SEARCH_FIELD_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.playlist_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFieldFocusRequester)
                            .trackTextFieldFocus(),
                    )
                }

                // Clears the query while it's non-empty (staying expanded, so the user can keep
                // typing) - only collapses the field back to just the icon once there's nothing
                // left to clear, rather than needing a second, separate button for that.
                IconButton(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        } else {
                            isSearchExpanded = false
                        }
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(Res.string.cd_clear_search),
                        modifier = Modifier.size(SEARCH_FIELD_ICON_SIZE),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            IconButton(onClick = { isSearchExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.cd_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
