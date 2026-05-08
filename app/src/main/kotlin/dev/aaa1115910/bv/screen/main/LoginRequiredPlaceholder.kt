package dev.aaa1115910.bv.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.ifElse

@Composable
fun LoginRequiredPlaceholder(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(R.string.login_required_account_message),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Button(
            modifier = Modifier.ifElse(
                focusRequester != null,
                Modifier.focusRequester(focusRequester ?: FocusRequester.Default)
            ),
            onClick = onLogin
        ) {
            Text(text = stringResource(R.string.sms_login_button_login))
        }
    }
}
