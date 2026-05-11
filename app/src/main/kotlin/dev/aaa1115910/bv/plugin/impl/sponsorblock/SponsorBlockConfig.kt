package dev.aaa1115910.bv.plugin.impl.sponsorblock

data class SponsorBlockConfig(
    val enabled: Boolean,
    val categoryPolicy: Map<String, SkipPolicy>
) {
    fun toJson(): String {
        val policies = categoryPolicy.entries.joinToString(",") { (key, value) ->
            "\"$key\":\"${value.name}\""
        }
        return """{"enabled":$enabled,"categoryPolicy":{$policies}}"""
    }

    companion object {
        val supportedCategories = listOf(
            "sponsor",
            "selfpromo",
            "exclusive_access",
            "interaction",
            "poi_highlight",
            "intro",
            "outro",
            "preview",
            "filler",
            "music_offtopic"
        )

        fun default() = SponsorBlockConfig(
            enabled = true,
            categoryPolicy = mapOf(
                "sponsor" to SkipPolicy.Prompt,
                "selfpromo" to SkipPolicy.Prompt,
                "exclusive_access" to SkipPolicy.Prompt,
                "intro" to SkipPolicy.Prompt,
                "outro" to SkipPolicy.Prompt,
                "interaction" to SkipPolicy.Disabled,
                "poi_highlight" to SkipPolicy.Disabled,
                "preview" to SkipPolicy.Disabled,
                "filler" to SkipPolicy.Disabled,
                "music_offtopic" to SkipPolicy.Disabled
            )
        )
    }
}

interface SponsorBlockConfigStore {
    suspend fun readConfig(): SponsorBlockConfig
    suspend fun writeConfig(config: SponsorBlockConfig)
    suspend fun isEnabled(): Boolean
    suspend fun setEnabled(enabled: Boolean)
}
