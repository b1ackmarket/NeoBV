package dev.aaa1115910.bv.activities.live

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.screen.live.LivePlayerScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class LivePlayerActivity : ComponentActivity() {
    companion object {
        fun actionStart(
            context: Context,
            roomId: Int,
            title: String,
            upName: String,
            online: Int
        ) {
            context.startActivity(
                Intent(context, LivePlayerActivity::class.java).apply {
                    putExtra("room_id", roomId)
                    putExtra("title", title)
                    putExtra("up_name", upName)
                    putExtra("online", online)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenAwake()
        setContent {
            BVTheme {
                LivePlayerScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keepScreenAwake()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun keepScreenAwake() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
