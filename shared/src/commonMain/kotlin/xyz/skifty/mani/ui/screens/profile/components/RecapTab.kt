package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_unavailable
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.RecapRange
import xyz.skifty.mani.media.resolve
import xyz.skifty.mani.models.Recap

private const val RECAP_LIST_LIMIT = 10
private val WIDE_LAYOUT_BREAKPOINT = 700.dp
private val TASTE_PROFILE_COLUMN_WIDTH = 280.dp
private val MOBILE_RADAR_MAX_WIDTH = 320.dp

/** The Profile screen's Recap tab (4a/6a in the design, scrolled per 3b/6b): range control,
 *  summary stats, top-song hero, top artists/songs, taste-profile radar, and the save actions.
 *  Fetches via [ApiService.getRecap] whenever [RecapRange] changes, degrading to
 *  [Res.string.recap_unavailable] if the server doesn't have the endpoint (a stock, non-forked
 *  Subsonic/Navidrome server). On wide (desktop) layouts, top songs and the taste-profile radar
 *  (with its percentage legend) sit side by side per 3b; narrower (mobile) layouts stack them and
 *  drop the legend, per 6b. */
@Composable
fun RecapTab(apiService: ApiService, modifier: Modifier = Modifier) {
    var selectedRange by remember { mutableStateOf<RecapRange>(RecapRange.Last6Months) }
    var recap by remember { mutableStateOf<Recap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedRange) {
        isLoading = true
        val (from, to) = selectedRange.resolve()
        recap = apiService.getRecap(from = from, to = to, count = RECAP_LIST_LIMIT)
        isLoading = false
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= WIDE_LAYOUT_BREAKPOINT

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RangeControl(
                range = selectedRange,
                onRangeSelected = { newRange -> selectedRange = newRange },
            )

            val currentRecap = recap
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                currentRecap == null -> {
                    Text(
                        text = stringResource(Res.string.recap_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    val totalMinutes = currentRecap.summary.totalMinutes

                    RecapSummaryStats(
                        playCount = currentRecap.summary.playCount,
                        listenedMinutes = totalMinutes,
                        uniqueSongs = currentRecap.summary.uniqueSongs,
                        uniqueArtists = currentRecap.summary.uniqueArtists,
                    )

                    currentRecap.topSong.firstOrNull()?.let { topSongEntry ->
                        TopSongHeroCard(
                            song = remember(topSongEntry) { apiService.toSongInfo(topSongEntry.entry) },
                            playCount = topSongEntry.playCount,
                            listenedMinutes = topSongEntry.totalMinutes,
                            playtimeShare = if (totalMinutes > 0) topSongEntry.totalMinutes / totalMinutes else 0.0,
                            isWide = isWide,
                        )
                    }

                    TopArtistsList(artists = currentRecap.topArtist, totalMinutes = totalMinutes)

                    if (isWide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(96.dp)) {
                            TopSongsList(
                                songs = currentRecap.topSong,
                                totalMinutes = totalMinutes,
                                modifier = Modifier.weight(1f),
                            )
                            TasteProfileRadar(
                                tasteProfile = currentRecap.tasteProfile,
                                showLegend = true,
                                modifier = Modifier.widthIn(max = TASTE_PROFILE_COLUMN_WIDTH),
                            )
                        }
                    } else {
                        TopSongsList(songs = currentRecap.topSong, totalMinutes = totalMinutes)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            TasteProfileRadar(
                                tasteProfile = currentRecap.tasteProfile,
                                showLegend = false,
                                modifier = Modifier.widthIn(max = MOBILE_RADAR_MAX_WIDTH),
                            )
                        }
                    }

                    SaveRecapActions(
                        rangeLabel = rangeButtonLabel(selectedRange),
                        songIds = currentRecap.topSong.map { song -> song.entry.id },
                        apiService = apiService,
                    )
                }
            }
        }
    }
}
