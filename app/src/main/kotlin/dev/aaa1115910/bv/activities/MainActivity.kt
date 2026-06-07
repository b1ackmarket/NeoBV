package dev.aaa1115910.bv.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.aaa1115910.bv.component.settings.PrivacyConsentDialog
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.screen.MainScreen
import dev.aaa1115910.bv.screen.user.lock.UnlockUserScreen
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.android.ext.android.inject

class MainActivity : PreviewAwareComponentActivity() {

    private val userRepository: UserRepository by inject()
    private val logger = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplashScreen = true
        installSplashScreen().apply {
            setKeepOnScreenCondition { keepSplashScreen }
        }
        super.onCreate(savedInstanceState)

        setContent {
            var isCheckingUserLock by remember { mutableStateOf(true) }
//            var isMainlandChina by remember { mutableStateOf(false) }
            var userLockLocked by remember { mutableStateOf(false) }
            var hasAcceptedAgreement by remember { mutableStateOf(Prefs.hasAcceptedUserAgreement) }

            LaunchedEffect(Unit) {
                val user = userRepository.findUserByUid(userRepository.uid)
                userLockLocked = user?.lock?.isNotBlank() ?: false
                logger.info { "default user: ${user?.username}" }
                isCheckingUserLock = false
                keepSplashScreen = false
            }


            BVTheme {
                if (isCheckingUserLock) {
                    //避免在检查网络的期间加载屏幕内容，导致检查完毕后显示屏幕内容时出现初始焦点未成功设置的问题
                } else {
                    if (!hasAcceptedAgreement) {
                        PrivacyConsentDialog(
                            onAccept = {
                                Prefs.hasAcceptedUserAgreement = true
                                hasAcceptedAgreement = true
                            }
                        )
                    } else if (!userLockLocked) {
                        MainScreen()
                    } else {
                        UnlockUserScreen(
                            onUnlockSuccess = { user ->
                                logger.info { "unlock user lock for user ${user.uid}" }
                                userLockLocked = false
                            }
                        )
                    }
                }
            }
        }
    }

}
