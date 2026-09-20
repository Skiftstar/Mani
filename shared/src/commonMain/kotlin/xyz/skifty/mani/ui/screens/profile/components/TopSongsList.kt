package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_top_songs_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.models.RecapTopSongEntry

@Composable
fun TopSongsList(songs: List<RecapTopSongEntry>, totalMinutes: Double, modifier: Modifier = Modifier) {
    if (songs.isEmpty()) {
        return
    }
    val safeTotal = totalMinutes.coerceAtLeast(1.0)
    Column(modifier = modifier) {
        SectionHeader(stringResource(Res.string.recap_top_songs_title))
        songs.forEachIndexed { index, song ->
            RankedStatRow(
                rank = index + 1,
                name = song.entry.title,
                shareOfTotal = (song.totalMinutes / safeTotal).toFloat(),
                listenedLabel = formatListenedMinutes(song.totalMinutes),
            )
        }
    }
}
