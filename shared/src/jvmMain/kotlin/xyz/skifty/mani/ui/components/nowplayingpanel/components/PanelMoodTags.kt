package xyz.skifty.mani.ui.components.nowplayingpanel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.vibe_acousticness
import mani.shared.generated.resources.vibe_danceability
import mani.shared.generated.resources.vibe_energy
import mani.shared.generated.resources.vibe_instrumentalness
import mani.shared.generated.resources.vibe_liveness
import mani.shared.generated.resources.vibe_speechiness
import mani.shared.generated.resources.vibe_valence
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.VibeProfile

/** Mood tag pills for [vibeProfile]'s 7 VibeNet stats, each rendered as its percentage (0-1 ->
 *  "Danceable 72%" etc.) - see [xyz.skifty.mani.ext.toVibeProfileOrNull], the only place this
 *  profile ever comes from: all 7 stats or none, so callers only reach for this once they already
 *  know every stat is available. Wraps onto multiple lines via [FlowRow] rather than a plain `Row`
 *  - the panel is a fixed [xyz.skifty.mani.ui.components.nowplayingpanel.NOW_PLAYING_PANEL_WIDTH],
 *  nowhere near wide enough to fit all 7 tags on one line. */
@Composable
fun PanelMoodTags(vibeProfile: VibeProfile, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MoodTag(stringResource(Res.string.vibe_acousticness, vibeProfile.acousticness.toPercent()))
        MoodTag(stringResource(Res.string.vibe_danceability, vibeProfile.danceability.toPercent()))
        MoodTag(stringResource(Res.string.vibe_energy, vibeProfile.energy.toPercent()))
        MoodTag(stringResource(Res.string.vibe_instrumentalness, vibeProfile.instrumentalness.toPercent()))
        MoodTag(stringResource(Res.string.vibe_liveness, vibeProfile.liveness.toPercent()))
        MoodTag(stringResource(Res.string.vibe_speechiness, vibeProfile.speechiness.toPercent()))
        MoodTag(stringResource(Res.string.vibe_valence, vibeProfile.valence.toPercent()))
    }
}

private fun Double.toPercent(): Int = (this * 100).roundToInt()

// A plain Surface pill, not an AssistChip/SuggestionChip - Material3's chip composables are all
// inherently interactive (every variant requires an onClick), which would be semantically wrong
// for a purely informational tag like this one (an assistive-tech user would hear "button" for
// something that isn't). surfaceVariant/onSurfaceVariant, not secondaryContainer/
// onSecondaryContainer - the latter is a saturated accent-color pairing meant to draw the eye
// (right for something actually interactive), too loud for a quiet info tag sitting right below
// plain onSurfaceVariant text in this same panel (PanelSongDetails' quality/play-count rows).
@Composable
private fun MoodTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
