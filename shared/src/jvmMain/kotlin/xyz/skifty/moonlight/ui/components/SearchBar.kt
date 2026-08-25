package xyz.skifty.moonlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_clear_search
import moonlight.shared.generated.resources.cd_search
import moonlight.shared.generated.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource

private val SEARCH_BAR_HEIGHT = 40.dp
private val SEARCH_BAR_ICON_SIZE = 18.dp

/** Always-visible search field at the top of the app (see JvmApp), self-drawn on [BasicTextField]
 *  rather than Material3's [androidx.compose.material3.OutlinedTextField] - same approach as
 *  [ProgressSlider]/[MiniVolumeSlider] and for a related reason: OutlinedTextField's internal
 *  padding assumes its default ~56dp height, so forcing it shorter via a plain height modifier
 *  clips the text instead of shrinking that padding to fit.
 *
 *  Typing into it drives the main content area to Screen.Search; clearing it - either by deleting
 *  all characters by hand or via the trailing clear button below, both of which just call
 *  [onQueryChange] with an empty string - returns to whichever screen was active before. */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(SEARCH_BAR_HEIGHT)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(Res.string.cd_search),
            modifier = Modifier.size(SEARCH_BAR_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(Res.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(Res.string.cd_clear_search),
                    modifier = Modifier.size(SEARCH_BAR_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
