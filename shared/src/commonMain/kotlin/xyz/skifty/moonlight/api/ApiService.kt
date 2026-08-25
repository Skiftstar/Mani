package xyz.skifty.moonlight.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import xyz.skifty.moonlight.media.PlaylistDetails
import xyz.skifty.moonlight.media.PlaylistInfo
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.models.ResponseSongInfo
import xyz.skifty.moonlight.models.SubsonicResponseWrapper
import xyz.skifty.moonlight.util.generateSalt
import xyz.skifty.moonlight.util.md5Hex

private const val API_VERSION = "1.16.1"
private const val CLIENT_NAME = "moonlight"

data class SubsonicSession(
    val apiUrl: String,
    val username: String,
    val token: String,
    val salt: String
)

class ApiService {

    private var session: SubsonicSession? = null
    val currentSession: SubsonicSession? get() = session

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
        }
    }

    /** Fresh login — derives a new salt+token from the raw password. The password itself is never stored. */
    fun configure(apiUrl: String, username: String, password: String) {
        val salt = generateSalt()
        val token = md5Hex(password + salt)
        session = SubsonicSession(normalizeBaseUrl(apiUrl), username, token, salt)
    }

    /** Restore a session from persisted (non-secret) token+salt, e.g. on auto-login. */
    fun restoreSession(apiUrl: String, username: String, token: String, salt: String) {
        session = SubsonicSession(normalizeBaseUrl(apiUrl), username, token, salt)
    }

    fun clearSession() {
        session = null
    }

    private fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim()
            .trimEnd('/')
        return if (trimmed.startsWith(
                "http://",
                ignoreCase = true,
            ) || trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun toSongInfo(responseSongInfo: ResponseSongInfo): SongInfo {
        val songInfo = SongInfo()
        val coverArtUrl = responseSongInfo.coverArt?.let { coverArtId ->
            buildUrl("/rest/getCoverArt", mapOf("id" to coverArtId, "size" to "300"))
        }
        songInfo.setSong(
            responseSongInfo.id,
            responseSongInfo.title,
            responseSongInfo.artist,
            coverArtUrl,
            buildUrl("/rest/stream", mapOf("id" to responseSongInfo.id, "format" to "mp3")),
            responseSongInfo.duration,
            responseSongInfo.bitRate,
            responseSongInfo.suffix,
        )
        return songInfo
    }

    private fun buildUrl(path: String, extraParams: Map<String, String> = emptyMap()): String {
        val s =
            session ?: error("ApiService not configured — call configure() or restoreSession() first")
        return URLBuilder().apply {
            takeFrom(s.apiUrl)
            encodedPath = path
            parameters.append("u", s.username)
            parameters.append("t", s.token)
            parameters.append("s", s.salt)
            parameters.append("v", API_VERSION)
            parameters.append("c", CLIENT_NAME)
            parameters.append("f", "json")
            extraParams.forEach { (k, v) -> parameters.append(k, v) }
        }
            .buildString()
    }

    suspend fun callTest(): String {
        val result = httpClient.get("https://dummyjson.com/test")
        return if (result.status.isSuccess()) {
            result.bodyAsText()
        } else result.status.description
    }

    /** Verifies the current session against the server's Subsonic ping endpoint. */
    suspend fun ping(): Result<Unit> {
        return try {
            val result = httpClient.get(buildUrl("/rest/ping"))
            if (!result.status.isSuccess()) {
                Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            } else {
                val resp = result.body<SubsonicResponseWrapper>().response
                if (resp.status == "ok") {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(resp.error?.message ?: "Login failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

    suspend fun getStarredSongs(): List<SongInfo> {
        val result = httpClient.get(buildUrl("/rest/getStarred2"))

        val songInfos: MutableList<SongInfo> = mutableListOf()
        if (result.status.isSuccess()) {
            val subsonicResponseWrapper: SubsonicResponseWrapper = result.body()

            for (responseSongInfo in subsonicResponseWrapper.response.starred2?.song.orEmpty()) {
                songInfos.add(toSongInfo(responseSongInfo))
            }
        }

        return songInfos
    }

    suspend fun getPlaylists(): List<PlaylistInfo> {
        val result = httpClient.get(buildUrl("/rest/getPlaylists"))

        val playlistInfos: MutableList<PlaylistInfo> = mutableListOf()
        if (result.status.isSuccess()) {
            val subsonicResponseWrapper: SubsonicResponseWrapper = result.body()

            for (responsePlaylist in subsonicResponseWrapper.response.playlists?.playlist.orEmpty()) {
                val coverArtUrl = responsePlaylist.coverArt?.let { coverArtId ->
                    buildUrl("/rest/getCoverArt", mapOf("id" to coverArtId, "size" to "300"))
                }
                playlistInfos.add(
                    PlaylistInfo(
                        id = responsePlaylist.id,
                        name = responsePlaylist.name,
                        songCount = responsePlaylist.songCount,
                        coverArtUrl = coverArtUrl,
                    ),
                )
            }
        }

        return playlistInfos
    }

    suspend fun getPlaylist(playlistId: String): PlaylistDetails {
        val result = httpClient.get(buildUrl("/rest/getPlaylist", mapOf("id" to playlistId)))

        val subsonicResponseWrapper: SubsonicResponseWrapper = result.body()
        val responsePlaylist = subsonicResponseWrapper.response.playlist
            ?: error("Playlist $playlistId not found")

        val songInfos: MutableList<SongInfo> = mutableListOf()
        for (responseSongInfo in responsePlaylist.entry) {
            songInfos.add(toSongInfo(responseSongInfo))
        }

        val coverArtUrl = responsePlaylist.coverArt?.let { coverArtId ->
            buildUrl("/rest/getCoverArt", mapOf("id" to coverArtId, "size" to "300"))
        }
        return PlaylistDetails(
            id = responsePlaylist.id,
            name = responsePlaylist.name,
            coverArtUrl = coverArtUrl,
            ownerName = responsePlaylist.owner,
            songs = songInfos,
        )
    }

    suspend fun getSong(songId: String): SongInfo {
        val result = httpClient.get(buildUrl("/rest/getSong", mapOf("id" to songId)))

        val subsonicResponseWrapper: SubsonicResponseWrapper = result.body()
        val responseSongInfo = subsonicResponseWrapper.response.song
            ?: error("Song $songId not found")

        return toSongInfo(responseSongInfo)
    }

}
