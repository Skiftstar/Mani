package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.profile_appearance_section
import mani.shared.generated.resources.profile_language_description
import mani.shared.generated.resources.profile_language_label
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.i18n.AppLanguage
import xyz.skifty.mani.ui.components.LanguageDropdown

/** The Settings tab's desktop-only Appearance section (Language) - passed into [SettingsTab] as
 *  its `platformAppearanceSection` slot from [xyz.skifty.mani.JvmApp], which is where the actual
 *  `appLanguage` state lives (its own `key(appLanguage)` wrapper is what makes changing it here
 *  take effect app-wide). Not an expect/actual like [ProfileVisualizerRow] because `AppLanguage`
 *  is a jvm-only type and can't appear in a commonMain function signature. */
@Composable
fun ProfileAppearanceSection(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Column {
        SectionHeader(
            text = stringResource(Res.string.profile_appearance_section),
            modifier = Modifier.padding(top = 20.dp),
        )
        SettingsRow(
            label = stringResource(Res.string.profile_language_label),
            description = stringResource(Res.string.profile_language_description),
            trailing = {
                LanguageDropdown(
                    selected = selectedLanguage,
                    onSelect = onLanguageChange,
                )
            },
        )
    }
}
