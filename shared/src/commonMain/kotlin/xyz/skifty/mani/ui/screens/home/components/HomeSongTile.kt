package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_album_art
import mani.shared.generated.resources.unknown_artist
import mani.shared.generated.resources.unknown_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo

private val TILE_WIDTH = 120.dp

@Composable
fun HomeSongTile(songInfo: SongInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(TILE_WIDTH)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = songInfo.songCoverArtUrl,
            contentDescription = stringResource(Res.string.cd_album_art),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = songInfo.songName ?: stringResource(Res.string.unknown_title),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Text(
            text = songInfo.songArtist ?: stringResource(Res.string.unknown_artist),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
