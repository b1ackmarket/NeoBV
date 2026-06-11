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
        indexRange: String?,
        widevinePssh: String? = null,
        audioCodec: String? = null,
        audioBandwidth: Int? = null,
        audioInitialization: String? = null,
        audioIndexRange: String? = null,
        audioWidevinePssh: String? = null,
        durationSeconds: Int = 0
    ): String {
        val escapedVideoUrl = videoUrl.xmlEscape()
        val escapedAudioUrl = audioUrl?.xmlEscape()
        val bufferTime = 1.5
        val safeFrameRate = frameRate.ifBlank { "24" }
        val videoPssh = widevinePssh?.takeIf { it.isNotBlank() }
        val audioPssh = audioWidevinePssh?.takeIf { it.isNotBlank() } ?: videoPssh

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<MPD xmlns="urn:mpeg:dash:schema:mpd:2011"""")
            appendLine("""     xmlns:cenc="urn:mpeg:cenc:2013"""")
            appendLine("""     profiles="urn:mpeg:dash:profile:isoff-on-demand:2011"""")
            appendLine("""     minBufferTime="PT${bufferTime}S"""")
            if (durationSeconds > 0) {
                appendLine("""     mediaPresentationDuration="PT${durationSeconds}S"""")
            }
            appendLine("""     type="static">""")
            appendLine("""  <Period>""")
            appendLine("""    <AdaptationSet mimeType="video/mp4" contentType="video" subsegmentAlignment="true" subsegmentStartsWithSAP="1">""")
            appendContentProtection(videoPssh)
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
                val safeAudioCodec = audioCodec?.takeIf { it.isNotBlank() } ?: "mp4a.40.2"
                val safeAudioBandwidth = audioBandwidth?.takeIf { it > 0 } ?: 192000
                appendLine("""    <AdaptationSet mimeType="audio/mp4" contentType="audio" subsegmentAlignment="true" subsegmentStartsWithSAP="1">""")
                appendContentProtection(audioPssh)
                appendLine("""      <Representation id="audio" codecs="$safeAudioCodec" bandwidth="$safeAudioBandwidth">""")
                appendLine("""        <BaseURL>$escapedAudioUrl</BaseURL>""")
                if (!audioInitialization.isNullOrBlank() && !audioIndexRange.isNullOrBlank()) {
                    appendLine("""        <SegmentBase indexRange="$audioIndexRange">""")
                    appendLine("""          <Initialization range="$audioInitialization"/>""")
                    appendLine("""        </SegmentBase>""")
                }
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

    private fun StringBuilder.appendContentProtection(pssh: String?) {
        if (pssh.isNullOrBlank()) return
        appendLine("""      <ContentProtection schemeIdUri="urn:mpeg:dash:mp4protection:2011" value="cenc"/>""")
        appendLine("""      <ContentProtection schemeIdUri="urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed">""")
        appendLine("""        <cenc:pssh>${pssh.xmlEscape()}</cenc:pssh>""")
        appendLine("""      </ContentProtection>""")
    }
}
