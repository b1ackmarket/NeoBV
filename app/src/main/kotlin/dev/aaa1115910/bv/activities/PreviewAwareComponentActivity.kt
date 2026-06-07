package dev.aaa1115910.bv.activities

import dev.aaa1115910.bv.repository.FocusPreviewManager

open class PreviewAwareComponentActivity : ImmersiveComponentActivity() {
    override fun onPause() {
        FocusPreviewManager.stop()
        super.onPause()
    }
}
