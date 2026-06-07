package dev.aaa1115910.bv.subtitle

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.subtitle.translation.SubtitleLanguages
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig
import dev.aaa1115910.bv.ui.state.SubtitleMemory
import dev.aaa1115910.bv.viewmodel.player.CustomSubtitleTrackId

sealed interface SecondarySubtitleOption {
    val id: Long

    data class BiliTrack(val subtitle: Subtitle) : SecondarySubtitleOption {
        override val id: Long = subtitle.id
    }

    data object CustomTranslation : SecondarySubtitleOption {
        override val id: Long = CustomSubtitleTrackId
    }
}

fun buildSecondarySubtitleOptions(
    tracks: List<Subtitle>,
    currentMainSubtitleId: Long,
    config: SubtitleTranslationConfig,
    preferCustom: Boolean,
    sourceSubtitleAvailable: Boolean
): List<SecondarySubtitleOption> {
    val targetLanguage = SubtitleLanguages.find(config.targetLanguage)
    val biliOptions = tracks
        .filter { it.id != -1L && it.id != currentMainSubtitleId }
        .sortedWith(
            compareByDescending<Subtitle> { track ->
                track.lang.equals(targetLanguage.code, ignoreCase = true) ||
                    track.langDoc.contains(targetLanguage.displayName)
            }.thenBy { it.id }
        )
        .map { SecondarySubtitleOption.BiliTrack(it) }

    val customOption = SecondarySubtitleOption.CustomTranslation
        .takeIf { config.verified() && sourceSubtitleAvailable }

    return if (customOption == null) {
        biliOptions
    } else if (preferCustom) {
        listOf(customOption) + biliOptions
    } else {
        biliOptions + customOption
    }
}

fun resolveDefaultSecondarySubtitleOption(
    options: List<SecondarySubtitleOption>,
    memory: SubtitleMemory?
): SecondarySubtitleOption? {
    if (memory == null) return null
    return resolveRememberedSecondarySubtitleOption(options, memory)
}

fun resolveRememberedSecondarySubtitleOption(
    options: List<SecondarySubtitleOption>,
    memory: SubtitleMemory?
): SecondarySubtitleOption? {
    if (options.isEmpty()) return null
    if (memory == null) return options.firstOrNull()
    return options.firstOrNull { option ->
        option is SecondarySubtitleOption.BiliTrack &&
            (
                option.subtitle.id == memory.id ||
                    option.subtitle.lang == memory.lang ||
                    option.subtitle.langDoc == memory.langDoc
                )
    } ?: options.firstOrNull()
}
