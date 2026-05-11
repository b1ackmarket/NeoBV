package dev.aaa1115910.bv.network

object MpdGenerator {
    fun generate(
        videoUrl: String,
        audioUrl: String?,
        videoCodec: String,
        width: Int,
        height: Int,
        frameRate: String,
        bandwidth: Int,
        initialization: String?,
        indexRange: String?
    ): String {
        val escapedVideoUrl = videoUrl.xmlEscape()
        val escapedAudioUrl = audioUrl?.xmlEscape()
        val bufferTime = 1.5
        val safeFrameRate = frameRate.ifBlank { "24" }

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<MPD xmlns="urn:mpeg:dash:schema:mpd:2011"""")
            appendLine("""     profiles="urn:mpeg:dash:profile:isoff-on-demand:2011"""")
            appendLine("""     minBufferTime="PT${bufferTime}S"""")
            appendLine("""     type="static">""")
            appendLine("""  <Period>""")
            appendLine("""    <AdaptationSet mimeType="video/mp4" contentType="video" subsegmentAlignment="true" subsegmentStartsWithSAP="1">""")
            append("""      <Representation id="video" codecs="$videoCodec" bandwidth="$bandwidth"""")
            if (width > 0 && height > 0) {
                append(""" width="$width" height="$height" frameRate="$safeFrameRate"""")
            }
            appendLine(""">""")
            appendLine("""        <BaseURL>$escapedVideoUrl</BaseURL>""")
            if (!initialization.isNullOrBlank() && !indexRange.isNullOrBlank()) {
                appendLine("""        <SegmentBase indexRange="$indexRange">""")
                appendLine("""          <Initialization range="$initialization"/>""")
                appendLine("""        </SegmentBase>""")
            }
            appendLine("""      </Representation>""")
            appendLine("""    </AdaptationSet>""")
            if (!escapedAudioUrl.isNullOrBlank()) {
                appendLine("""    <AdaptationSet mimeType="audio/mp4" contentType="audio" subsegmentAlignment="true" subsegmentStartsWithSAP="1">""")
                appendLine("""      <Representation id="audio" codecs="mp4a.40.2" bandwidth="192000">""")
                appendLine("""        <BaseURL>$escapedAudioUrl</BaseURL>""")
                appendLine("""      </Representation>""")
                appendLine("""    </AdaptationSet>""")
            }
            appendLine("""  </Period>""")
            appendLine("""</MPD>""")
        }
    }

    private fun String.xmlEscape(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
