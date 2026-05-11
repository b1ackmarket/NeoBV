package dev.aaa1115910.bv.screen.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.repository.LiveRoomContext
import dev.aaa1115910.bv.repository.LiveVoiceRoomMember

@Composable
fun LiveChatRoomStage(
    modifier: Modifier = Modifier,
    roomContext: LiveRoomContext,
    fallbackTitle: String,
    fallbackUpName: String
) {
    val members = roomContext.voiceMembers.take(9)
    val backdrop = roomContext.keyframe.ifBlank { roomContext.cover }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111827))
    ) {
        if (backdrop.isNotBlank()) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.28f),
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEE111827),
                            Color(0xCC1E293B),
                            Color(0xF011172A)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "聊天室直播",
                color = Color(0xFFFF6EAF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = roomContext.title.ifBlank { fallbackTitle },
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = fallbackUpName,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            if (members.isEmpty()) {
                Text(
                    text = "正在播放聊天室音频，当前房间暂无可展示麦位信息",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    members.chunked(3).forEach { rowMembers ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowMembers.forEach { member ->
                                LiveVoiceMemberCard(member = member)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveVoiceMemberCard(member: LiveVoiceRoomMember) {
    Column(
        modifier = Modifier.width(136.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            model = member.avatar,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Text(
            text = member.nickname,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (member.isMute) "静音" else member.priceText.ifBlank { "连麦中" },
            color = Color.White.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp
        )
    }
}
