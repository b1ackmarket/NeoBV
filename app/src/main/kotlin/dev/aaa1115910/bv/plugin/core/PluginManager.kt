package dev.aaa1115910.bv.plugin.core

import dev.aaa1115910.bv.plugin.api.PlayerPlugin

object PluginManager {
    private val playerPlugins = mutableListOf<PlayerPlugin>()

    fun register(plugin: PlayerPlugin) {
        if (playerPlugins.none { it.id == plugin.id }) {
            playerPlugins += plugin
        }
    }

    fun getPlayerPlugins(): List<PlayerPlugin> = playerPlugins.toList()

    @Suppress("UNCHECKED_CAST")
    fun <T : PlayerPlugin> getPlayerPlugin(id: String): T? {
        return playerPlugins.firstOrNull { it.id == id } as? T
    }
}
