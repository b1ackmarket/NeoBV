package dev.aaa1115910.bv.activities

import androidx.activity.ComponentActivity
import dev.aaa1115910.bv.repository.FocusPreviewManager

open class PreviewAwareComponentActivity : ComponentActivity() {
    override fun onPause() {
        FocusPreviewManager.stop()
        super.onPause()
    }
}
