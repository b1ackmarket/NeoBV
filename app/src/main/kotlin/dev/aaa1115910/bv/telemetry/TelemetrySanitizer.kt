package dev.aaa1115910.bv.telemetry

object TelemetrySanitizer {
    private val allowedEventKeys = setOf(
        "screen_name",
        "app_version",
        "build_type",
        "local_date",
        "source",
        "error_type",
        "player_type",
        "network_type"
    )
    private val forbiddenKeyParts = listOf(
        "uid",
        "mid",
        "cookie",
        "token",
        "sess",
        "jct",
        "bvid",
        "avid",
        "cid",
        "room",
        "title",
        "name",
        "keyword",
        "query",
        "url",
        "path",
        "ip"
    )

    fun sanitizeExtras(extras: Map<String, Any?>): Map<String, Any> {
        return extras.mapNotNull { (key, value) ->
            val cleanValue = sanitizeValue(key, value) ?: return@mapNotNull null
            key to cleanValue
        }.toMap()
    }

    fun sanitizeEventParams(params: Map<String, Any?>): Map<String, Any> {
        return params.mapNotNull { (key, value) ->
            if (key !in allowedEventKeys) return@mapNotNull null
            val cleanValue = if (key == "screen_name") {
                sanitizeAllowedString(value)
            } else {
                sanitizeValue(key, value)
            } ?: return@mapNotNull null
            key to cleanValue
        }.toMap()
    }

    fun sanitizeValue(key: String, value: Any?): Any? {
        val normalizedKey = key.lowercase()
        if (normalizedKey != "api_endpoint" && forbiddenKeyParts.any { normalizedKey.contains(it) }) {
            return null
        }
        return when (value) {
            is String -> sanitizeString(key, value)
            is Int, is Long, is Float, is Double, is Boolean -> value
            else -> value?.toString()?.let { sanitizeString(key, it) }
        }
    }

    private fun sanitizeAllowedString(value: Any?): Any? {
        return when (value) {
            is String -> value.take(100)
            is Int, is Long, is Float, is Double, is Boolean -> value
            else -> value?.toString()?.take(100)
        }
    }

    private fun sanitizeString(key: String, value: String): String? {
        if (key == "api_endpoint") {
            return value
                .substringBefore('?')
                .take(100)
                .takeIf { it.startsWith('/') && !it.contains("//") }
        }
        return value.take(100)
    }
}
