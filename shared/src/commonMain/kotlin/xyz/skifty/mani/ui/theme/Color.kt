package xyz.skifty.mani.ui.theme

import androidx.compose.ui.graphics.Color

// Background / surfaces - the near-black navy behind everything in the reference screenshot.
val SpotifyBackground = Color(0xFF0B0E14)
val SpotifySurfaceVariant = Color(0xFF1C212C)
val SpotifyOutlineVariant = Color(0xFF2A2F3A)

// The very top of ManiTheme's main-background gradient - a touch lighter than SpotifyBackground,
// which the gradient fades back down to by the bottom of the window, so it still meshes seamlessly
// with the flat SpotifyBackground-toned Sidebar/NowPlayingPanel/etc. around and below it.
val SpotifyBackgroundGradientTop = Color(0xFF10141F)

// NowPlayingBottomWidget's own background - a shade darker than SpotifyBackground so the bar
// reads as a distinct surface instead of blending into the (surfaceContainer-toned) Sidebar/
// content behind it, which deliberately shares SpotifyBackground's exact value - see Theme.kt.
val SpotifyBottomBarBackground = Color(0xFF070A10)

// Text.
val SpotifyOnBackground = Color(0xFFFFFFFF)
val SpotifyOnSurfaceVariant = Color(0xFFA7A7A7)

// Primary accent - the circular Play button's blue.
val SpotifyPrimary = Color(0xFF2D8CFF)
val SpotifyOnPrimary = Color(0xFFFFFFFF)
val SpotifyPrimaryContainer = Color(0xFF1E3A5F)
val SpotifyOnPrimaryContainer = Color(0xFFD3E4FF)

// Tertiary - Spotify's actual brand green, used for the song progress bar and volume slider,
// deliberately distinct from the screenshot's blue accent used everywhere else.
val SpotifyTertiary = Color(0xFF1DB954)
val SpotifyOnTertiary = Color(0xFF00210B)
