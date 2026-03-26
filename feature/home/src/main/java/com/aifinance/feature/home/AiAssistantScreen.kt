package com.aifinance.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary

private val AssistantSurface = Color(0xFFF8FBFF)
private val AssistantCard = Color(0xFFFFFFFF)
private val AssistantBlue = Color(0xFF4A7DFF)
private val AssistantUserBubble = Color(0xFFE6EEFF)
private val AssistantBotBubble = Color(0xFFF1F5FB)

private val suggestionGroups = listOf(
    listOf("怎么快速记一笔？", "导入账单支持哪些格式？", "如何查看月度支出趋势？"),
    listOf("资产管理怎么添加新账户？", "转账记录会怎么展示？", "怎么筛选某个月流水？"),
    listOf("如何核对收支是否平衡？", "账单导入失败怎么办？", "怎样查看不同账户总资产？"),
)

@Composable
fun AiAssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSuggestions = suggestionGroups[uiState.suggestionGroupIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AssistantSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AssistantWelcomeBlock()

        SuggestionCard(
            suggestions = currentSuggestions,
            onSwitch = { viewModel.rotateSuggestionGroup(suggestionGroups.size) },
            onSuggestionClick = viewModel::onSuggestionClick,
        )

        if (uiState.messages.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "有什么想问我的吗",
                        style = IcokieTextStyles.bodyLarge,
                        color = OnSurfaceSecondary,
                    )
                },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )

            IconButton(
                onClick = viewModel::sendMessage,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AssistantBlue),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun AssistantWelcomeBlock() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AssistantCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "嗨！我是胖咔",
                    style = IcokieTextStyles.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfacePrimary,
                )
                Text(
                    text = "帮你快速搞懂产品使用",
                    style = IcokieTextStyles.bodyLarge,
                    color = OnSurfaceSecondary,
                )
                Text(
                    text = "功能，你的专属客服",
                    style = IcokieTextStyles.bodyLarge,
                    color = OnSurfaceSecondary,
                )
            }

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE9F0FF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "插画",
                    style = IcokieTextStyles.labelMedium,
                    color = Color(0xFF7E96C9),
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestions: List<String>,
    onSwitch: () -> Unit,
    onSuggestionClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AssistantCard),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "解锁使用技巧，试试这样问",
                    style = IcokieTextStyles.titleMedium,
                    color = OnSurfacePrimary,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSwitch)
                        .background(Color(0xFFEAF1FF))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = null,
                        tint = AssistantBlue,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "换一换",
                        style = IcokieTextStyles.labelMedium,
                        color = AssistantBlue,
                    )
                }
            }

            suggestions.forEach { question ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F8FF))
                        .clickable { onSuggestionClick(question) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = question,
                        style = IcokieTextStyles.bodyLarge,
                        color = OnSurfacePrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage) {
    val isUser = message.role == AssistantRole.USER
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) AssistantUserBubble else AssistantBotBubble

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = align,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                style = IcokieTextStyles.bodyLarge,
                color = OnSurfacePrimary,
                textAlign = TextAlign.Start,
            )
        }
    }
}
