package dev.aaa1115910.biliapi.http.entity.pgc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PgcRankData(
    val list: List<PgcRankItem> = emptyList(),
    val note: String = "",
    @SerialName("season_type")
    val seasonType: Int = 0
)

@Serializable
data class PgcRankResult(
    val list: List<PgcRankItem> = emptyList()
)

@Serializable
data class PgcRankItem(
    val cover: String = "",
    @SerialName("new_ep")
    val newEp: NewEp? = null,
    val stat: Stat? = null,
    val title: String = "",
    val url: String = "",
    @SerialName("season_id")
    val seasonId: Int = 0,
    @SerialName("season_type")
    val seasonType: Int = 0,
    val rating: JsonElement? = null
) {
    @Serializable
    data class NewEp(
        @SerialName("index_show")
        val indexShow: String = ""
    )

    @Serializable
    data class Stat(
        val follow: Int = 0,
        val view: Long = 0
    )
}
