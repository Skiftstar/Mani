package xyz.skifty.moonlight.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Explicitly setting the surface-adjacent roles below rather than letting Material3 auto-derive
// them from `surface` - its Material You tonal algorithm deliberately spaces those roles apart
// for elevation contrast, which wouldn't reproduce the reference screenshot's flat, uniformly
// near-black surfaces.
private val MoonlightDarkColorScheme = darkColorScheme(
    primary = SpotifyPrimary,
    onPrimary = SpotifyOnPrimary,
    primaryContainer = SpotifyPrimaryContainer,
    onPrimaryContainer = SpotifyOnPrimaryContainer,
    tertiary = SpotifyTertiary,
    onTertiary = SpotifyOnTertiary,
    background = SpotifyBackground,
    onBackground = SpotifyOnBackground,
    surface = SpotifyBackground,
    onSurface = SpotifyOnBackground,
    surfaceVariant = SpotifySurfaceVariant,
    onSurfaceVariant = SpotifyOnSurfaceVariant,
    surfaceContainer = SpotifyBackground,
    outlineVariant = SpotifyOutlineVariant,
)

/** [fillContainer] wraps [content] in a full-size [Surface] (the default) - needed wherever
 *  content doesn't set every color explicitly itself, since that Surface is what wires up
 *  LocalContentColor for the whole subtree (a plain Modifier.background() paints the right color
 *  but never sets it, so Text/Icon composables would default to black). Pass `false` for content
 *  that already sets every color explicitly and isn't meant to fill its parent - e.g. TitleBar,
 *  which composes outside JvmApp's own MoonlightTheme call and would otherwise fight JvmApp's own
 *  content for space if both tried to fill the same window. */
@Composable
fun MoonlightTheme(
    fillContainer: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = MoonlightDarkColorScheme) {
        if (fillContainer) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
