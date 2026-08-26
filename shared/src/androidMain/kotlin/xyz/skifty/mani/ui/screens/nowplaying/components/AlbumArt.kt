package xyz.skifty.mani.ui.screens.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/** The Now Playing screen's large cover art. */
@Composable
fun AlbumArt(coverArtUrl: String?) {
    AsyncImage(
        model = coverArtUrl,
        contentDescription = stringResource(Res.string.cd_album_art),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
    )
}
