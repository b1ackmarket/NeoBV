package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.player.AbstractVideoPlayer

object FocusPreviewManager {
    private var activePlayer: AbstractVideoPlayer? = null

    fun attach(player: AbstractVideoPlayer) {
        activePlayer?.takeIf { it !== player }?.release()
        activePlayer = player
    }

    fun detach(player: AbstractVideoPlayer?) {
        if (player == null || activePlayer === player) {
            activePlayer = null
        }
    }

    fun stop() {
        activePlayer?.release()
        activePlayer = null
    }
}
