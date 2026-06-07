package dev.aaa1115910.bv.activities.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.screen.settings.LogsScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class LogsActivity : ImmersiveComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BVTheme {
                LogsScreen()
            }
        }
    }
}
