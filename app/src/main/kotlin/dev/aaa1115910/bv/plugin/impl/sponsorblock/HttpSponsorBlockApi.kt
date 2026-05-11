package dev.aaa1115910.bv.plugin.impl.sponsorblock

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpSponsorBlockApi : SponsorBlockApi {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
    }

    override suspend fun getSegments(bvid: String): List<SponsorSegment> {
        val response = client.get(
            "https://bsbsb.top/api/skipSegments" +
                    "?videoID=$bvid" +
                    "&category=sponsor" +
                    "&category=selfpromo" +
                    "&category=exclusive_access" +
                    "&category=intro" +
                    "&category=outro" +
                    "&category=interaction" +
                    "&category=poi_highlight" +
                    "&category=preview" +
                    "&category=filler" +
                    "&category=music_offtopic"
        )
        return runCatching {
            response.body<List<SponsorBlockSegmentResponse>>().map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    @Serializable
    private data class SponsorBlockSegmentResponse(
        @SerialName("UUID")
        val uuid: String,
        val category: String,
        val segment: List<Double>
    ) {
        fun toDomain() = SponsorSegment(
            id = uuid,
            category = category,
            startMs = (segment.getOrElse(0) { 0.0 } * 1000).toLong(),
            endMs = (segment.getOrElse(1) { 0.0 } * 1000).toLong()
        )
    }
}
