package dev.aaa1115910.bv.activities.live

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        setContent {
            BVTheme {
                LivePlayerScreen()
            }
        }
    }
}
