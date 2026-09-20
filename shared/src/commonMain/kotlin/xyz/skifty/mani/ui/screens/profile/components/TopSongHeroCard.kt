package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_playtime_share
import mani.shared.generated.resources.recap_stat_listened
import mani.shared.generated.resources.recap_stat_plays
import mani.shared.generated.resources.recap_top_song_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo
import kotlin.math.roundToInt

private val WIDE_COVER_SIZE = 172.dp
private val NARROW_COVER_SIZE = 104.dp

/** The range's single top song. Desktop's `.hero` (4a) puts the cover beside a single text column
 *  that includes the Plays/Listened row; mobile's `.m-hero` (6a) only puts the title/artist beside
 *  the (smaller) cover in that row - Plays/Listened sits on its own row *below* the cover+text,
 *  spanning the full width, not nested next to the cover. [isWide] switches between the two,
 *  matching [RecapTab]'s own wide/narrow breakpoint. */
@Composable
fun TopSongHeroCard(song: SongInfo, playCount: Long, listenedMinutes: Double, playtimeShare: Double, isWide: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionHeader(stringResource(Res.string.recap_top_song_title))
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isWide) 20.dp else 14.dp),
                verticalAlignment = if (isWide) Alignment.CenterVertically else Alignment.Top,
            ) {
                AsyncImage(
                    model = song.songCoverArtUrl,
                    contentDescription = song.songName,
                    modifier = Modifier
                        .size(if (isWide) WIDE_COVER_SIZE else NARROW_COVER_SIZE)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = if (isWide) Arrangement.Center else Arrangement.Top,
                ) {
                    Text(
                        text = stringResource(Res.string.recap_playtime_share, (playtimeShare * 100).roundToInt()).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.em),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = song.songName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.songArtist.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isWide) {
                        TopSongStatsRow(
                            playCount = playCount,
                            listenedMinutes = listenedMinutes,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
            if (!isWide) {
                TopSongStatsRow(
                    playCount = playCount,
                    listenedMinutes = listenedMinutes,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

// The design does *not* apply the summary stats' smaller-unit-letter treatment here - this row's
// values are plain text (`.hero .nums span b` / 6a's inline stat both have no `<small>`), so
// formatListenedMinutes (not listenedMinutesAnnotatedString) is correct.
@Composable
private fun TopSongStatsRow(playCount: Long, listenedMinutes: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        StatPair(stringResource(Res.string.recap_stat_plays), playCount.toString())
        StatPair(stringResource(Res.string.recap_stat_listened), formatListenedMinutes(listenedMinutes))
    }
}

@Composable
private fun StatPair(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.06.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
