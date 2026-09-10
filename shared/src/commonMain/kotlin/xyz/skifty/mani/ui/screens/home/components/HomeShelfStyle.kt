package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

/** Whether [HomeShelf] shows its prev/next paging arrows */
expect val showHomeShelfPageArrows: Boolean

/** Text style for a Home shelf's title (both [HomeShelf] and [FavoritesShelf] use this) - bigger
 *  and bolder on Android than desktop's default [Typography.titleMedium]. */
@Composable
expect fun homeShelfTitleStyle(): TextStyle
