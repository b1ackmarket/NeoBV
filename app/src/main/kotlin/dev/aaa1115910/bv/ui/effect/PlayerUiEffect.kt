package dev.aaa1115910.bv.ui.effect

sealed class PlayerUiEffect {
    data object PlayEnded : PlayerUiEffect()
    data object FinishActivity: PlayerUiEffect()
    data object ShowRecommendedVideos : PlayerUiEffect()
}
