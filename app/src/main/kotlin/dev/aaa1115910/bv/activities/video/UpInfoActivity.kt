package dev.aaa1115910.bv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.activities.PreviewAwareComponentActivity
import dev.aaa1115910.bv.screen.user.UpSpaceScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class UpInfoActivity : PreviewAwareComponentActivity() {
    companion object {
        const val INITIAL_FOCUS_PROFILE = "profile"
        const val INITIAL_FOCUS_TABS = "tabs"
        private const val EXTRA_INITIAL_FOCUS = "initial_focus"

        fun actionStart(
            context: Context,
            mid: Long,
            name: String,
            initialFocus: String = INITIAL_FOCUS_PROFILE
        ) {
            context.startActivity(
                Intent(context, UpInfoActivity::class.java).apply {
                    putExtra("mid", mid)
                    putExtra("name", name)
                    putExtra(EXTRA_INITIAL_FOCUS, initialFocus)
                }
            )
        }

        fun readInitialFocus(intent: Intent): String =
            intent.getStringExtra(EXTRA_INITIAL_FOCUS) ?: INITIAL_FOCUS_PROFILE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                UpSpaceScreen()
            }
        }
    }
}
