package xyz.skifty.mani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_album_art
import mani.shared.generated.resources.cd_pause
import mani.shared.generated.resources.cd_play
import mani.shared.generated.resources.cd_star
import mani.shared.generated.resources.cd_unstar
import mani.shared.generated.resources.unknown_artist
import mani.shared.generated.resources.unknown_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.detectSecondaryClick
import xyz.skifty.mani.ext.qualityLabel
import xyz.skifty.mani.ext.toDurationLabel
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** One row of [PlaylistSongTable] - row number (swaps to a play/pause icon on hover), cover
 *  thumbnail + title/artist, quality, duration, and a star/unstar toggle. Whenever this row's
 *  song is the one currently loaded in [audioPlayer] (per [activeSongInfo]), its text is tinted
 *  the accent color and clicking it toggles play/pause instead of restarting the track from the
 *  beginning. Right-clicking (desktop) or long-pressing (touch) opens a context menu - see
 *  [SongContextMenuHost]. */
@Composable
fun PlaylistSongRow(
    index: Int,
    songInfo: SongInfo,
    audioPlayer: AudioPlayer,
    activeSongInfo: SongInfo,
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (PlaylistInfo) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()

    val isActive = songInfo.songId != null && songInfo.songId == activeSongInfo.songId
    val isPlaying = isActive && audioPlayer.isPlaying
    val contentColor = if (isActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
    val secondaryColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    // The menu's position is tracked in actual window coordinates (this row's own position via
    // onGloballyPositioned, plus the click's position within it) rather than raw local/anchor-
    // relative offsets - SongContextMenu is itself a Popup, and once a Popup ends up nested
    // inside another Popup's content (its "Add to Playlist" flyout is), Compose's automatic
    // anchor-bounds resolution doesn't reliably see through that nesting - see
    // FixedPositionProvider for the fix this feeds into. Only meaningful on platforms with a
    // right-click concept at all - see detectSecondaryClick's platform actuals.
    var rowPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var contextMenuPosition by remember { mutableStateOf<Offset?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates -> rowPositionInWindow = coordinates.positionInWindow() }
            .detectSecondaryClick { positionInRow ->
                contextMenuPosition = rowPositionInWindow + positionInRow
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isActive) {
                            audioPlayer.togglePlayPause()
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        contextMenuPosition = rowPositionInWindow
                    },
                )
                .hoverable(hoverInteractionSource)
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showExtendedSongColumns) {
                Box(
                    modifier = Modifier.width(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isHovered) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (isPlaying) Res.string.cd_pause else Res.string.cd_play,
                            ),
                            modifier = Modifier.size(16.dp),
                            tint = contentColor,
                        )
                    } else {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor,
                        )
                    }
                }
            }

            AsyncImage(
                model = songInfo.songCoverArtUrl,
                contentDescription = stringResource(Res.string.cd_album_art),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songInfo.songName ?: stringResource(Res.string.unknown_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = songInfo.songArtist ?: stringResource(Res.string.unknown_artist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showExtendedSongColumns) {
                Text(
                    text = songInfo.qualityLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    modifier = Modifier.width(80.dp),
                )
            }

            if (showExtendedSongColumns) {
                Text(
                    text = songInfo.songDurationSeconds?.toDurationLabel() ?: "--:--",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(48.dp),
                )
            }

            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (songInfo.starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (songInfo.starred) Res.string.cd_unstar else Res.string.cd_star,
                    ),
                    modifier = Modifier.size(20.dp),
                    tint = if (songInfo.starred) MaterialTheme.colorScheme.primary else secondaryColor,
                )
            }
        }

        SongContextMenuHost(
            positionInWindow = contextMenuPosition,
            onDismissRequest = { contextMenuPosition = null },
            songInfo = songInfo,
            apiService = apiService,
            playlistLibrary = playlistLibrary,
            onPlay = onPlay,
            onAddToQueue = onAddToQueue,
            onToggleStar = onToggleStar,
            onAddToPlaylist = onAddToPlaylist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
        )
    }
}
