package dev.aaa1115910.bv.telemetry

object TelemetryConsentText {
    const val title = "用户协议与隐私政策"

    val sections = listOf(
        "NeoBV 是用于访问哔哩哔哩公开内容和账号相关功能的第三方客户端。继续使用表示你理解本软件与哔哩哔哩官方客户端并非同一产品，使用账号登录、代理、数据导入导出等功能时，应自行确认账号与数据安全。",
        "崩溃报告开关建议开启。开启后，NeoBV 会通过 Firebase Crashlytics 发送崩溃信息和少量排障字段，例如应用版本、构建类型、Android 版本、设备品牌/型号、播放器类型、默认画质、默认编码、弹幕是否开启、代理是否开启、隐身模式是否开启，以及经脱敏后的错误分类和 HTTP 状态码。",
        "匿名使用信息开关建议开启。开启后，NeoBV 会通过 Firebase Analytics 发送低频匿名事件，例如应用启动、每日活跃、粗略页面名称、视频/直播错误分类和网络类型。它用于判断问题影响范围和优先级，不用于内容推荐或广告。",
        "NeoBV 不会主动发送哔哩哔哩 UID、用户名、手机号、邮箱、Cookie、SESSDATA、bili_jct、access token、refresh token、BV/AV/CID、番剧 season/epid、直播间号、主播名、搜索词、弹幕内容、完整 URL、请求头、请求体、响应体、精确位置、Wi-Fi SSID/BSSID、广告 ID、Android ID、本地日志文件或包含用户信息的文件路径。",
        "崩溃报告和匿名使用信息会发送给第三方服务 Firebase / Google 处理。Firebase 的保存期限和处理方式以 Google Firebase 服务规则为准；NeoBV 只按最小化原则发送排障所需信息。",
        "你可以随时在设置页关闭“发送崩溃报告”和“发送匿名使用信息”。关闭后，NeoBV 会立即停止后续主动采集，并尝试删除尚未发送的崩溃报告；受 Firebase SDK 行为限制，自动崩溃采集状态在部分情况下可能需要下次启动后完全生效。",
        "不同意发送崩溃报告或匿名使用信息，也可以正常进入并使用 App。"
    )

    val fullText: String
        get() = sections.joinToString("\n\n")
}
