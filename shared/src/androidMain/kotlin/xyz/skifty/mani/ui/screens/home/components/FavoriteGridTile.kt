package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_album_art
import mani.shared.generated.resources.unknown_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo

/** One tile of [FavoritesShelf]'s Android grid - cover art fills the whole tile, with the song
 *  title overlaid at the bottom (over a gradient scrim for legibility) rather than shown
 *  separately below it, unlike [HomeSongTile]. Fixed white-on-black-scrim colors are deliberate
 *  here rather than theme tokens - this needs to stay legible over arbitrary album art regardless
 *  of the app's theme, same reasoning most apps use for text overlaid on a photo. */
@Composable
fun FavoriteGridTile(
    songInfo: SongInfo,
    size: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = songInfo.songCoverArtUrl,
            contentDescription = stringResource(Res.string.cd_album_art),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.8f),
                    ),
                ),
        )
        Text(
            text = songInfo.songName ?: stringResource(Res.string.unknown_title),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )
    }
}
