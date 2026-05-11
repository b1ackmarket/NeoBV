package dev.aaa1115910.bv.plugin.core

import androidx.datastore.preferences.core.booleanPreferencesKey
import dev.aaa1115910.bv.BVApp
import kotlinx.coroutines.flow.first

object PluginStore {
    suspend fun isEnabled(pluginId: String, defaultValue: Boolean = false): Boolean {
        val key = booleanPreferencesKey("plugin_enabled_$pluginId")
        val prefs = BVApp.dataStoreManager.dataStore.data.first()
        return prefs[key] ?: defaultValue
    }

    suspend fun setEnabled(pluginId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("plugin_enabled_$pluginId")
        BVApp.dataStoreManager.editPreference(key, enabled)
    }
}
