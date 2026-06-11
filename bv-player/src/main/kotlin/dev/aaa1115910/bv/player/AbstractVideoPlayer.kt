package dev.aaa1115910.bv.player

abstract class AbstractVideoPlayer {
    /** 播放器事件回调 */
    protected var mPlayerEventListener: VideoPlayerListener? = null

    /**
     * 初始化播放器实例
     * 视频播放器第一步：创建视频播放器
     */
    abstract fun initPlayer()

    /** 设置请求头 */
    abstract fun setHeader(headers: Map<String, String>)

    /** 设置播放地址 */
    abstract fun playUrl(videoUrl: String? = null, audioUrl: String? = null)

    /** 设置 MPD 播放地址 */
    abstract fun playDash(
        mpdUrl: String,
        drmLicenseUrl: String? = null,
        drmRequestHeaders: Map<String, String> = emptyMap()
    )

    /** 设置 HLS 播放地址 */
    abstract fun playHls(hlsUrl: String)

    /** 准备开始播放 */
    abstract fun prepare()

    /** 播放 */
    abstract fun start()

    /** 暂停 */
    abstract fun pause()

    /** 停止 */
    abstract fun stop()

    /** 重置播放器 */
    abstract fun reset()

    /** 是否正在播放 */
    abstract val isPlaying: Boolean

    /** 跳转播放位置 */
    abstract fun seekTo(time: Long)

    /** 释放播放器 */
    abstract fun release()

    /** 当前播放位置 */
    abstract val currentPosition: Long

    /** 视频总时长 */
    abstract val duration: Long

    /** 缓冲百分比 */
    abstract val bufferedPercentage: Int

    /** 设置其他播放配置 */
    abstract fun setOptions()

    /** 播放速度 */
    abstract var speed: Float

    /** 播放音量，0f 为静音，1f 为原始音量 */
    abstract var volume: Float

    /** 当前实时网络传输速度，单位 bits/second */
    abstract val tcpSpeed: Long

    /** 调试信息 */
    abstract val debugInfo: String

    /** 视频宽度 */
    abstract val videoWidth: Int

    /** 视频高度 */
    abstract val videoHeight: Int

    /**
     * 绑定VideoView，监听播放异常，完成，开始准备，视频size变化，视频信息等操作
     */
    fun setPlayerEventListener(playerEventListener: VideoPlayerListener?) {
        mPlayerEventListener = playerEventListener
    }
}
