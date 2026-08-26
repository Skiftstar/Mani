package xyz.skifty.mani.ui.components

import androidx.compose.ui.unit.Dp

/** Whether [PlaylistSongRow]/[PlaylistSongTable] show the index number, quality, and duration
 *  columns - true on desktop (plenty of width for a multi-column table), false on Android
 *  (narrower screens need that width back for titles instead). */
expect val showExtendedSongColumns: Boolean

/** Horizontal margin [xyz.skifty.mani.ui.screens.playlist.PlaylistScreen] applies around
 *  [PlaylistSongTable] specifically - kept separate from the header's own margin (which stays the
 *  same on both platforms) since a compact Android row (just cover+title+star, no index/quality/
 *  duration columns) already carries plenty of visual margin from [PlaylistSongRow]'s own 16dp
 *  internal padding on top of this, unlike desktop's denser multi-column rows. */
expect val playlistTableHorizontalPadding: Dp
