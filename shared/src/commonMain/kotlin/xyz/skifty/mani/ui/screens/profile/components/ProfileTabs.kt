package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The Profile screen's Recap/Settings tabs, styled to match the design's `.tabs` (left-aligned,
 *  each tab sized to its own text, a colored 2dp underline only beneath the selected tab) rather
 *  than Material3's [androidx.compose.material3.SecondaryTabRow] (full-width, evenly distributed
 *  tabs) - visually a quite different shape from what the design calls for. */
@Composable
fun ProfileTabs(selectedIndex: Int, titles: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val underlineThickness = with(LocalDensity.current) { 2.dp.toPx() }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) selectedColor else unselectedColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        )
                        .padding(top = 10.dp, bottom = 11.dp)
                        .drawBehind {
                            if (selected) {
                                drawLine(
                                    color = selectedColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = underlineThickness,
                                )
                            }
                        },
                )
            }
        }
        HorizontalDivider(color = dividerColor)
    }
}
