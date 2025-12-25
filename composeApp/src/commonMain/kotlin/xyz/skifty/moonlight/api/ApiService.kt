package xyz.skifty.moonlight.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class ApiService {

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
        }
    }

    suspend fun callTest(): String {
        val result = httpClient.get {
            url {
               protocol = URLProtocol.HTTPS
               host = "dummyjson.com"
                encodedPath = "test"
            }
        }

        return if (result.status.isSuccess()) {
            result.bodyAsText()
        } else result.status.description
    }

}