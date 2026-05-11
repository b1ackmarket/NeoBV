package dev.aaa1115910.bv.plugin.impl.sponsorblock

import androidx.datastore.preferences.core.stringPreferencesKey
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.plugin.core.PluginStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PrefsSponsorBlockConfigStore(
    private val pluginId: String = "sponsorblock"
) : SponsorBlockConfigStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("plugin_config_$pluginId")

    override suspend fun readConfig(): SponsorBlockConfig {
        val prefs = BVApp.dataStoreManager.dataStore.data.first()
        val raw = prefs[key] ?: return SponsorBlockConfig.default()
        return runCatching {
            json.decodeFromString(PersistedSponsorBlockConfig.serializer(), raw).toDomain()
        }.getOrElse { SponsorBlockConfig.default() }
    }

    override suspend fun writeConfig(config: SponsorBlockConfig) {
        val payload = json.encodeToString(PersistedSponsorBlockConfig.fromDomain(config))
        BVApp.dataStoreManager.editPreference(key, payload)
    }

    override suspend fun isEnabled(): Boolean {
        return PluginStore.isEnabled(pluginId, defaultValue = false)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        PluginStore.setEnabled(pluginId, enabled)
    }

    @Serializable
    private data class PersistedSponsorBlockConfig(
        val enabled: Boolean,
        val categoryPolicy: Map<String, String>
    ) {
        fun toDomain(): SponsorBlockConfig = SponsorBlockConfig(
            enabled = enabled,
            categoryPolicy = categoryPolicy.mapValues { (_, value) ->
                runCatching { enumValueOf<SkipPolicy>(value) }.getOrDefault(SkipPolicy.Disabled)
            }
        )

        companion object {
            fun fromDomain(config: SponsorBlockConfig) = PersistedSponsorBlockConfig(
                enabled = config.enabled,
                categoryPolicy = config.categoryPolicy.mapValues { it.value.name }
            )
        }
    }
}
