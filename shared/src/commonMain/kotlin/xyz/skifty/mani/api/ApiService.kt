package xyz.skifty.mani.api

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
import xyz.skifty.mani.media.PlaylistDetails
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.media.VibeProfile
import xyz.skifty.mani.models.Recap
import xyz.skifty.mani.models.ResponseSongInfo
import xyz.skifty.mani.models.SubsonicResponseWrapper
import xyz.skifty.mani.util.generateSalt
import xyz.skifty.mani.util.md5Hex
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val API_VERSION = "1.16.1"
private const val CLIENT_NAME = "mani"

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

    // internal (not private) so Recap UI code can convert a RecapTopSong's raw entry the same way
    // every other song list on the app does, without duplicating cover-art URL construction.
    internal fun toSongInfo(responseSongInfo: ResponseSongInfo): SongInfo {
        val songInfo = SongInfo()
        val coverArtUrl = responseSongInfo.coverArt?.let { coverArtId ->
            buildUrl("/rest/getCoverArt", mapOf("id" to coverArtId, "size" to "300"))
        }
        songInfo.setSong(
            id = responseSongInfo.id,
            name = responseSongInfo.title,
            artist = responseSongInfo.artist,
            coverArtUrl = coverArtUrl,
            playbackUrl = buildUrl("/rest/stream", mapOf("id" to responseSongInfo.id, "format" to "mp3")),
            durationSeconds = responseSongInfo.duration,
            bitRateKbps = responseSongInfo.bitRate,
            format = responseSongInfo.suffix,
            playCount = responseSongInfo.playCount,
            starred = responseSongInfo.starred != null,
            acousticness = responseSongInfo.acousticness,
            danceability = responseSongInfo.danceability,
            energy = responseSongInfo.energy,
            instrumentalness = responseSongInfo.instrumentalness,
            liveness = responseSongInfo.liveness,
            speechiness = responseSongInfo.speechiness,
            valence = responseSongInfo.valence,
        )
        return songInfo
    }

    /** [repeatedParams] is for query params that need to appear more than once (e.g.
     *  `getVibeSimilarTracks`'s `exclude` list) - [extraParams] alone can't represent that, since
     *  a `Map` can only hold one value per key. */
    private fun buildUrl(
        path: String,
        extraParams: Map<String, String> = emptyMap(),
        repeatedParams: List<Pair<String, String>> = emptyList(),
    ): String {
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
            repeatedParams.forEach { (k, v) -> parameters.append(k, v) }
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

    /** Songs-only Subsonic search (`artistCount=0&albumCount=0` - no album/artist results are
     *  fetched or shown). [songCount] caps how many song hits come back per call, [songOffset]
     *  pages through further results (see SearchScreen's infinite-scroll pagination); degrades to
     *  an empty list on any non-2xx response rather than throwing, so a bad/incomplete query
     *  never crashes the search screen. */
    suspend fun search3(query: String, songCount: Int = 25, songOffset: Int = 0): List<SongInfo> {
        val result = httpClient.get(
            buildUrl(
                "/rest/search3",
                mapOf(
                    "query" to query,
                    "songCount" to songCount.toString(),
                    "songOffset" to songOffset.toString(),
                    "artistCount" to "0",
                    "albumCount" to "0",
                ),
            ),
        )

        val songInfos: MutableList<SongInfo> = mutableListOf()
        if (result.status.isSuccess()) {
            val subsonicResponseWrapper: SubsonicResponseWrapper = result.body()

            for (responseSongInfo in subsonicResponseWrapper.response.searchResult3?.song.orEmpty()) {
                songInfos.add(toSongInfo(responseSongInfo))
            }
        }

        return songInfos
    }

    /** A random sample of [size] songs from across the whole library - the Home screen's
     *  "Random Songs" shelf. Degrades to an empty list on any non-2xx response, same as
     *  [getStarredSongs]/[search3], rather than throwing. */
    suspend fun getRandomSongs(size: Int): List<SongInfo> {
        val result = httpClient.get(buildUrl("/rest/getRandomSongs", mapOf("size" to size.toString())))

        val songInfos: MutableList<SongInfo> = mutableListOf()
        if (result.status.isSuccess()) {
            val subsonicResponseWrapper: SubsonicResponseWrapper = result.body()

            for (responseSongInfo in subsonicResponseWrapper.response.randomSongs?.song.orEmpty()) {
                songInfos.add(toSongInfo(responseSongInfo))
            }
        }

        return songInfos
    }

    /** Top [count] most-played songs over the last 30 days, via the custom `getRecap` endpoint
     *  this app's own Navidrome fork adds (see the README's Backend section) - not part of
     *  standard Subsonic/Navidrome, so wrapped in try/catch (unlike the other list-returning
     *  endpoints above) and degrades to an empty list on any failure, including a 404 from a
     *  stock server that doesn't have it. Each `topSong` entry's `entry` is a full song object -
     *  same shape [toSongInfo] already consumes elsewhere - so no secondary per-song fetch is
     *  needed to get cover art/duration/etc. */
    suspend fun getRecapTopSongs(count: Int): List<SongInfo> {
        return try {
            val to = Clock.System.now()
            val from = to.minus(30.days)
            val result = httpClient.get(
                buildUrl(
                    "/rest/getRecap",
                    mapOf("from" to from.toString(), "to" to to.toString(), "count" to count.toString()),
                ),
            )
            if (!result.status.isSuccess()) {
                return emptyList()
            }
            result.body<SubsonicResponseWrapper>().response.recap?.topSong.orEmpty()
                .map { topSong -> toSongInfo(topSong.entry) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** The full recap payload (summary, top songs, top artists, taste profile) for a given
     *  [from]/[to] window, via the same custom `getRecap` endpoint as [getRecapTopSongs] - powers
     *  the Profile screen's Recap tab. [from]/[to] are omitted from the request when null, per the
     *  fork's own documented defaults (`recapDateRange` in `recap.go`): an omitted `from` means
     *  "since the beginning", an omitted `to` means "now". Degrades to null (rather than an
     *  empty/zeroed [Recap]) on any failure, including a 404 from a stock server without this
     *  endpoint, so callers can tell "no data in range" apart from "endpoint unavailable". */
    suspend fun getRecap(from: Instant?, to: Instant?, count: Int): Recap? {
        return try {
            val params = buildMap {
                from?.let { put("from", it.toString()) }
                to?.let { put("to", it.toString()) }
                put("count", count.toString())
            }
            val result = httpClient.get(buildUrl("/rest/getRecap", params))
            if (!result.status.isSuccess()) {
                return null
            }
            result.body<SubsonicResponseWrapper>().response.recap
        } catch (e: Exception) {
            null
        }
    }

    /** Songs with a similar vibe to [vibeProfile] (the current queue's own averaged VibeNet
     *  stats - see [xyz.skifty.mani.media.PlaybackQueue.maybeFetchAutoplay]), excluding
     *  [excludeSongIds] (every song already anywhere in the queue, so Autoplay never re-suggests
     *  a duplicate), via the custom `getVibeSimilarTracks` endpoint this app's own Navidrome fork
     *  adds (see the README's Backend section) - powers the Autoplay queue-continuation feature.
     *  Not part of standard Subsonic/Navidrome, so wrapped in try/catch like [getRecapTopSongs]
     *  and degrades to an empty list on any failure, including a 404 from a stock server that
     *  doesn't have it. Each entry's `entry` is a full song object - same shape [toSongInfo]
     *  already consumes elsewhere - the accompanying per-entry `distance` score isn't needed here
     *  and is dropped. */
    suspend fun getVibeSimilarSongs(
        vibeProfile: VibeProfile,
        excludeSongIds: List<String>,
        count: Int,
    ): List<SongInfo> {
        return try {
            val result = httpClient.get(
                buildUrl(
                    "/rest/getVibeSimilarTracks",
                    mapOf(
                        "acousticness" to vibeProfile.acousticness.toString(),
                        "danceability" to vibeProfile.danceability.toString(),
                        "energy" to vibeProfile.energy.toString(),
                        "instrumentalness" to vibeProfile.instrumentalness.toString(),
                        "liveness" to vibeProfile.liveness.toString(),
                        "speechiness" to vibeProfile.speechiness.toString(),
                        "valence" to vibeProfile.valence.toString(),
                        "count" to count.toString(),
                    ),
                    repeatedParams = excludeSongIds.map { songId -> "exclude" to songId },
                ),
            )
            if (!result.status.isSuccess()) {
                return emptyList()
            }
            result.body<SubsonicResponseWrapper>().response.vibeSimilarTrack.orEmpty()
                .map { similarTrack -> toSongInfo(similarTrack.entry) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Stars [songId] server-side (makes it appear in getStarredSongs()/Liked Songs). */
    suspend fun star(songId: String): Result<Unit> = setStarred("/rest/star", songId)

    /** Unstars [songId] server-side. */
    suspend fun unstar(songId: String): Result<Unit> = setStarred("/rest/unstar", songId)

    private suspend fun setStarred(path: String, songId: String): Result<Unit> {
        return try {
            val result = httpClient.get(buildUrl(path, mapOf("id" to songId)))
            if (result.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

    /** Records a completed listen of [songId] - updates play counts and reports to last.fm if
     *  configured server-side. `submission` is left at its Subsonic-default of true (an actual
     *  listen), as opposed to false, which would instead post a transient "now playing" notice.
     *  [msPlayed] is a custom extension this server build accepts for precise listen-duration
     *  tracking - standard Subsonic servers simply ignore unknown params, so this stays safe to
     *  send unconditionally. */
    suspend fun scrobble(
        songId: String,
        msPlayed: Long,
    ): Result<Unit> {
        return try {
            val result = httpClient.get(
                buildUrl(
                    "/rest/scrobble",
                    mapOf("id" to songId, "msPlayed" to msPlayed.toString()),
                ),
            )
            if (result.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

    /** Adds [songId] to the end of [playlistId]. Uses `updatePlaylist`, not `createPlaylist` -
     *  calling `createPlaylist` with an existing playlist id overwrites/replaces its entire song
     *  list on Navidrome instead of appending to it, which is exactly the trap this avoids. */
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            val result = httpClient.get(
                buildUrl(
                    "/rest/updatePlaylist",
                    mapOf("playlistId" to playlistId, "songIdToAdd" to songId),
                ),
            )
            if (result.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

    /** Removes the song at zero-based [songIndex] from [playlistId]. Subsonic's `updatePlaylist`
     *  only supports removal by position (`songIndexToRemove`), not by song id - callers must
     *  pass the song's current index within [xyz.skifty.mani.media.PlaylistDetails.songs],
     *  the same order the server returned it in. */
    suspend fun removeSongFromPlaylist(playlistId: String, songIndex: Int): Result<Unit> {
        return try {
            val result = httpClient.get(
                buildUrl(
                    "/rest/updatePlaylist",
                    mapOf("playlistId" to playlistId, "songIndexToRemove" to songIndex.toString()),
                ),
            )
            if (result.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

    /** Creates a brand new, empty playlist named [name]. Uses `createPlaylist` with only a
     *  `name` param (no `playlistId`) - the same endpoint overwrites an *existing* playlist's
     *  song list entirely if a `playlistId` is passed instead, which is exactly what
     *  [addSongToPlaylist]'s doc comment warns against; omitting it here is what makes this call
     *  safe. */
    suspend fun createPlaylist(name: String): Result<PlaylistInfo> {
        return try {
            val result = httpClient.get(buildUrl("/rest/createPlaylist", mapOf("name" to name)))
            if (!result.status.isSuccess()) {
                return Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            }
            val responsePlaylist = result.body<SubsonicResponseWrapper>().response.playlist
                ?: return Result.failure(Exception("Server did not return the created playlist"))
            Result.success(
                PlaylistInfo(
                    id = responsePlaylist.id,
                    name = responsePlaylist.name,
                    songCount = responsePlaylist.songCount,
                    coverArtUrl = responsePlaylist.coverArt?.let { coverArtId ->
                        buildUrl("/rest/getCoverArt", mapOf("id" to coverArtId, "size" to "300"))
                    },
                ),
            )
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

    /** Permanently deletes the playlist identified by [playlistId] - the songs themselves aren't
     *  touched, only the playlist entity. */
    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return try {
            val result = httpClient.get(buildUrl("/rest/deletePlaylist", mapOf("id" to playlistId)))
            if (result.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned HTTP ${result.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach server: ${e.message}", e))
        }
    }

}
