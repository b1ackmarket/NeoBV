package dev.aaa1115910.bv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.activities.PreviewAwareComponentActivity
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.screen.VideoInfoScreen
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoInfoActivity : PreviewAwareComponentActivity() {
    companion object {
        fun actionStart(
            context: Context,
            aid: Long,
            epid: Int? = null,
            fromSeason: Boolean = false,
            fromController: Boolean = false,
            proxyArea: ProxyArea = ProxyArea.MainLand
        ) {
            if (epid != null) {
                SeasonInfoActivity.actionStart(
                    context = context,
                    epId = epid,
                    proxyArea = proxyArea
                )
                return
            }

            context.startActivity(
                Intent(context, VideoInfoActivity::class.java).apply {
                    putExtra("aid", aid)
                    putExtra("fromSeason", fromSeason)
                    putExtra("fromController", fromController)
                    putExtra("proxy_area", proxyArea.ordinal)
                }
            )
        }
    }

    private val videoDetailViewModel: VideoDetailViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                VideoInfoScreen()
            }
        }
        getParamsFromIntent()
    }

    private fun getParamsFromIntent() {
        if (intent.hasExtra("aid")) {
            val aid = intent.getLongExtra("aid", 170001)
            val fromSeason = intent.getBooleanExtra("fromSeason", false)
            val fromController = intent.getBooleanExtra("fromController", false)
            val proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)]

            videoDetailViewModel.init(
                aid = aid,
                fromSeason = fromSeason,
                fromController = fromController,
                proxyArea = proxyArea,
            )
        }
    }
}
