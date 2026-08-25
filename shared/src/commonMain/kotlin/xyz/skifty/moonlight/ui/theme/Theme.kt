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

@Composable
fun MoonlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MoonlightDarkColorScheme) {
        // A plain Modifier.background() paints the right color but never sets
        // LocalContentColor, so Text/Icon composables outside of any Surface would still
        // default to black - Surface is what actually wires the background to a matching
        // "on" content color for the whole subtree.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            content()
        }
    }
}
