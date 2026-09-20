package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_top_artists_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.models.RecapTopArtistEntry

@Composable
fun TopArtistsList(artists: List<RecapTopArtistEntry>, totalMinutes: Double, modifier: Modifier = Modifier) {
    if (artists.isEmpty()) {
        return
    }
    val safeTotal = totalMinutes.coerceAtLeast(1.0)
    Column(modifier = modifier) {
        SectionHeader(stringResource(Res.string.recap_top_artists_title))
        artists.forEachIndexed { index, artist ->
            RankedStatRow(
                rank = index + 1,
                name = artist.name,
                shareOfTotal = (artist.totalMinutes / safeTotal).toFloat(),
                listenedLabel = formatListenedMinutes(artist.totalMinutes),
            )
        }
    }
}
