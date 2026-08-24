package xyz.skifty.moonlight.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.media.SongInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import xyz.skifty.moonlight.media.DesktopAudioPlayer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(apiService: ApiService, audioPlayer: DesktopAudioPlayer, activeSongInfo: SongInfo) {

    var songs by remember { mutableStateOf<List<SongInfo>>(listOf()) }

    LaunchedEffect(Unit) {
        songs = apiService.getStarredSongs()
    }

    Column {
        for (songInfo in songs) {
            Box(modifier = Modifier.fillMaxWidth().onClick(onClick = {
                audioPlayer.play(songInfo, activeSongInfo)
            })) {
                Text(songInfo.songName?:"undefined")
            }
        }
    }

}