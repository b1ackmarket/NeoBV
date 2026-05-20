package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoPbp(
    @SerialName("step_sec")
    val stepSec: Int = 0,
    val events: Events? = null
) {
    @Serializable
    data class Events(
        @SerialName("default")
        val default: List<Double> = emptyList()
    )
}
