package xyz.skifty.mani.ui.components.nowplayingpanel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_album_art
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo

/** Large square cover art at the top of the now-playing panel. */
@Composable
fun PanelCoverArt(songInfo: SongInfo, modifier: Modifier = Modifier) {
    AsyncImage(
        model = songInfo.songCoverArtUrl,
        contentDescription = stringResource(Res.string.cd_album_art),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
    )
}
