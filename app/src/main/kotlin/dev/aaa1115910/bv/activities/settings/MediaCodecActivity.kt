package dev.aaa1115910.bv.activities.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.screen.settings.MediaCodecScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class MediaCodecActivity : ImmersiveComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                MediaCodecScreen()
            }
        }
    }
}
