package xyz.skifty.moonlight.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Sidebar(onHomeClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onHomeClick) {
                Icon(imageVector = Icons.Filled.Home, contentDescription = "Home")
            }
            IconButton(onClick = { /* TODO: history */ }) {
                Icon(imageVector = Icons.Filled.History, contentDescription = "History")
            }
            IconButton(onClick = { /* TODO: albums */ }) {
                Icon(imageVector = Icons.Filled.Album, contentDescription = "Albums")
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
