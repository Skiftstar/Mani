package xyz.skifty.mani.ui.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.profile_recap_tab
import mani.shared.generated.resources.profile_settings_tab
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ui.screens.profile.components.AccountHeader
import xyz.skifty.mani.ui.screens.profile.components.ProfileTabs
import xyz.skifty.mani.ui.screens.profile.components.RecapTab
import xyz.skifty.mani.ui.screens.profile.components.SettingsTab

/** The Profile screen: account header, a Recap/Settings [ProfileTabs], and the two tabs' content -
 *  reachable from the desktop nav rail's avatar button ([xyz.skifty.mani.ui.components.Sidebar])
 *  and Android's existing bottom-nav Profile destination
 *  ([xyz.skifty.mani.ui.components.BottomNavBar]). [platformAppearanceSection] is desktop-only
 *  (see [SettingsTab]'s doc comment for why it's a slot rather than an expect/actual). */
@Composable
fun ProfileScreen(
    apiService: ApiService,
    showVisualizer: Boolean,
    onShowVisualizerChange: (Boolean) -> Unit,
    autoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    platformAppearanceSection: (@Composable () -> Unit)? = null,
) {
    val session = apiService.currentSession
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        AccountHeader(
            username = session?.username.orEmpty(),
            serverHost = session?.apiUrl?.let(::displayHost).orEmpty(),
        )

        ProfileTabs(
            selectedIndex = selectedTabIndex,
            titles = listOf(
                stringResource(Res.string.profile_recap_tab),
                stringResource(Res.string.profile_settings_tab),
            ),
            onSelect = { index -> selectedTabIndex = index },
            modifier = Modifier.padding(top = 20.dp),
        )

        Box(
            modifier = Modifier
                .padding(top = 20.dp)
                .weight(1f),
        ) {
            when (selectedTabIndex) {
                0 -> RecapTab(apiService = apiService)
                else -> SettingsTab(
                    showVisualizer = showVisualizer,
                    onShowVisualizerChange = onShowVisualizerChange,
                    autoplayEnabled = autoplayEnabled,
                    onAutoplayChange = onAutoplayChange,
                    username = session?.username.orEmpty(),
                    onLogout = onLogout,
                    platformAppearanceSection = platformAppearanceSection,
                )
            }
        }
    }
}

private fun displayHost(apiUrl: String): String =
    apiUrl.removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')
