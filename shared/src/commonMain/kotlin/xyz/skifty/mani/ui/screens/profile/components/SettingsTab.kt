package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.em
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.profile_account_section
import mani.shared.generated.resources.profile_autoplay_label
import mani.shared.generated.resources.profile_logout_button
import mani.shared.generated.resources.profile_logout_description
import mani.shared.generated.resources.profile_playback_section
import mani.shared.generated.resources.profile_signed_in_as
import org.jetbrains.compose.resources.stringResource

/** The Profile screen's Settings tab: Playback (Autoplay everywhere, +an Android-only visualizer
 *  toggle), an optional desktop-only Appearance section ([platformAppearanceSection] - a slot
 *  rather than an expect/actual, since it needs to carry the jvm-only `AppLanguage` type its
 *  caller already holds), and Account (who's signed in + Log Out). */
@Composable
fun SettingsTab(
    showVisualizer: Boolean,
    onShowVisualizerChange: (Boolean) -> Unit,
    autoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    username: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    platformAppearanceSection: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(stringResource(Res.string.profile_playback_section))
        SettingsRow(
            label = stringResource(Res.string.profile_autoplay_label),
            trailing = {
                Switch(
                    checked = autoplayEnabled,
                    onCheckedChange = onAutoplayChange,
                )
            },
        )
        ProfileVisualizerRow(
            showVisualizer = showVisualizer,
            onShowVisualizerChange = onShowVisualizerChange,
        )

        platformAppearanceSection?.invoke()

        SectionHeader(stringResource(Res.string.profile_account_section))
        SettingsRow(
            label = stringResource(Res.string.profile_signed_in_as, username),
            description = stringResource(Res.string.profile_logout_description),
            showDivider = false,
            trailing = {
                TextButton(onClick = onLogout) {
                    Text(stringResource(Res.string.profile_logout_button))
                }
            },
        )
    }
}

/** A small uppercase, letter-spaced section label ("TOP SONG", "PLAYBACK", …) - matches the
 *  design's `.sec` class (`text-transform:uppercase; letter-spacing:.09em`) used identically
 *  across both the Recap and Settings tabs. Uppercased at display time rather than in the string
 *  resources themselves, so the underlying strings stay normal title-case text everywhere else. */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.09.em),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
