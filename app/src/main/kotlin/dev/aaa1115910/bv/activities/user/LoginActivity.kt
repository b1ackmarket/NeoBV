package dev.aaa1115910.bv.activities.user

import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.screen.login.LoginScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class LoginActivity : ImmersiveComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVTheme {
                LoginScreen()
            }
        }
    }
}
