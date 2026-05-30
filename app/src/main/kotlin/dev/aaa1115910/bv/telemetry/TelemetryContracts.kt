package dev.aaa1115910.bv.telemetry

enum class TelemetryScreen(val key: String) {
    AppStart("app_start"),
    Home("home"),
    Search("search"),
    Personal("personal"),
    Dynamic("dynamic"),
    Pgc("pgc"),
    Live("live"),
    Settings("settings"),
    VideoDetail("video_detail"),
    VideoPlayer("video_player"),
    LivePlayer("live_player")
}

enum class TelemetryEvent(val key: String) {
    AppOpen("app_open"),
    DailyActive("daily_active"),
    VideoError("video_error"),
    LiveError("live_error"),
    ApiError("api_error"),
    DanmakuError("danmaku_error")
}

enum class TelemetryErrorDomain(val key: String) {
    Video("video"),
    Live("live"),
    Danmaku("danmaku"),
    Api("api"),
    Database("database"),
    Startup("startup")
}

enum class TelemetryErrorType(val key: String) {
    NetworkError("network_error"),
    ParseError("parse_error"),
    EmptyPlayUrl("empty_play_url"),
    DecodeError("decode_error"),
    Timeout("timeout"),
    AuthExpired("auth_expired"),
    Unknown("unknown")
}

enum class TelemetryNetworkType(val key: String) {
    Wifi("wifi"),
    Cellular("cellular"),
    Ethernet("ethernet"),
    Unknown("unknown")
}
