package com.aifinance.feature.home

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val quickPrompts = listOf(
    "记录一笔午餐支出",
    "查看本月消费分析",
    "生成月度财务报告",
    "给我一些理财建议",
)

private val suggestionGroups = listOf(
    listOf("记录一笔午餐支出", "查看本月消费分析", "生成月度财务报告", "给我一些理财建议"),
    listOf("这周花了多少钱", "我的资产有多少", "最近的消费趋势", "设置本月预算"),
    listOf("昨天有什么支出", "哪个分类花钱最多", "帮我分析消费习惯", "记账小技巧"),
)

private val CookiePrimary = Color(0xFFF5A623)
private val CookieSecondary = Color(0xFFFFE4B5)
private val CookieDark = Color(0xFFE08A00)
private val CookieFace = Color(0xFF8B4513)

@Composable
fun AiAssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.consumeStatisticsContext()
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    EmptyState(
                        onPromptClick = { prompt ->
                            viewModel.onInputChange(prompt)
                            viewModel.sendMessage()
                        },
                        suggestionGroupIndex = uiState.suggestionGroupIndex,
                        onRotateSuggestions = { viewModel.rotateSuggestionGroup(suggestionGroups.size) },
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.messages) { message ->
                            MessageItem(message = message)
                        }
                        if (uiState.isLoading) {
                            item {
                                LoadingIndicator()
                            }
                        }
                    }
                }
            }

            InputArea(
                inputText = uiState.inputText,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                isLoading = uiState.isLoading,
            )
        }
    }
}

@Composable
private fun EmptyState(onPromptClick: (String) -> Unit, suggestionGroupIndex: Int, onRotateSuggestions: () -> Unit) {
    val currentSuggestions = suggestionGroups.getOrElse(suggestionGroupIndex) { suggestionGroups.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        HeroSection()

        Spacer(modifier = Modifier.height(24.dp))

        FeatureGuideCard(
            suggestions = currentSuggestions,
            onPromptClick = onPromptClick,
            onRotate = onRotateSuggestions,
        )
    }
}

@Composable
private fun HeroSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "嗨！",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "我是iCookie",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "你的专属智能记账助手",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ICookieAvatar(size = 100.dp)
    }
}

@Composable
private fun FeatureGuideCard(suggestions: List<String>, onPromptClick: (String) -> Unit, onRotate: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = CookiePrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "试试这样问我",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                TextButton(
                    onClick = onRotate,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "换一换",
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "换一换",
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                suggestions.forEach { prompt ->
                    PromptTag(
                        text = prompt,
                        onClick = { onPromptClick(prompt) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptTag(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = ">",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun CapabilityItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegacyICookieAvatar(size: androidx.compose.ui.unit.Dp = 80.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.toPx()
            val canvasHeight = size.toPx()
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val radius = canvasWidth * 0.4f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CookiePrimary, CookieDark),
                    center = Offset(centerX, centerY),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(centerX, centerY),
            )

            val chipRadius = radius * 0.08f
            val chipPositions = listOf(
                Offset(centerX - radius * 0.3f, centerY - radius * 0.25f),
                Offset(centerX + radius * 0.25f, centerY - radius * 0.3f),
                Offset(centerX, centerY + radius * 0.1f),
                Offset(centerX - radius * 0.15f, centerY + radius * 0.35f),
                Offset(centerX + radius * 0.35f, centerY + radius * 0.15f),
                Offset(centerX - radius * 0.35f, centerY),
            )
            chipPositions.forEach { pos ->
                drawCircle(
                    color = CookieFace,
                    radius = chipRadius,
                    center = pos,
                )
            }

            val eyeRadius = radius * 0.12f
            val leftEyeCenter = Offset(centerX - radius * 0.22f, centerY - radius * 0.05f)
            val rightEyeCenter = Offset(centerX + radius * 0.22f, centerY - radius * 0.05f)

            drawCircle(
                color = Color.White,
                radius = eyeRadius,
                center = leftEyeCenter,
            )
            drawCircle(
                color = Color.White,
                radius = eyeRadius,
                center = rightEyeCenter,
            )

            val pupilRadius = eyeRadius * 0.5f
            drawCircle(
                color = Color.Black,
                radius = pupilRadius,
                center = Offset(leftEyeCenter.x, leftEyeCenter.y + pupilRadius * 0.2f),
            )
            drawCircle(
                color = Color.Black,
                radius = pupilRadius,
                center = Offset(rightEyeCenter.x, rightEyeCenter.y + pupilRadius * 0.2f),
            )

            drawCircle(
                color = Color.White,
                radius = pupilRadius * 0.35f,
                center = Offset(leftEyeCenter.x - pupilRadius * 0.3f, leftEyeCenter.y - pupilRadius * 0.3f),
            )
            drawCircle(
                color = Color.White,
                radius = pupilRadius * 0.35f,
                center = Offset(rightEyeCenter.x - pupilRadius * 0.3f, rightEyeCenter.y - pupilRadius * 0.3f),
            )

            drawArc(
                color = CookieFace,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(centerX - radius * 0.2f, centerY + radius * 0.05f),
                size = Size(radius * 0.4f, radius * 0.25f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.04f),
            )

            drawCircle(
                color = CookieDark,
                radius = radius * 0.1f,
                center = Offset(centerX - radius * 0.85f, centerY - radius * 0.3f),
            )
            drawLine(
                color = CookieDark,
                start = Offset(centerX - radius * 0.75f, centerY - radius * 0.25f),
                end = Offset(centerX - radius * 0.55f, centerY - radius * 0.15f),
                strokeWidth = radius * 0.04f,
            )
            drawCircle(
                color = CookieDark,
                radius = radius * 0.1f,
                center = Offset(centerX + radius * 0.85f, centerY - radius * 0.3f),
            )
            drawLine(
                color = CookieDark,
                start = Offset(centerX + radius * 0.75f, centerY - radius * 0.25f),
                end = Offset(centerX + radius * 0.55f, centerY - radius * 0.15f),
                strokeWidth = radius * 0.04f,
            )
        }
    }
}

private fun DrawScope.drawMiniCookie() {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2
    val radius = canvasWidth * 0.4f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CookiePrimary, CookieDark),
            center = Offset(centerX, centerY),
            radius = radius,
        ),
        radius = radius,
        center = Offset(centerX, centerY),
    )

    listOf(
        Offset(centerX - radius * 0.2f, centerY - radius * 0.2f),
        Offset(centerX + radius * 0.2f, centerY + radius * 0.1f),
        Offset(centerX, centerY + radius * 0.25f),
    ).forEach { pos ->
        drawCircle(
            color = CookieFace,
            radius = radius * 0.12f,
            center = pos,
        )
    }
}

@Composable
private fun MessageItem(message: AssistantMessage) {
    val isUser = message.role == AssistantRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CookieSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawMiniCookie()
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 20.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp,
                        ),
                    )
                    .background(
                        if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (isUser) {
                    Text(
                        text = message.content,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    MarkdownText(content = message.content)
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "我",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MarkdownText(content: String) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    fun colorHex(color: Color): String = String.format("#%06X", 0xFFFFFF and color.toArgb())

    val escapedContent = content
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")

    val htmlContent = remember(
        content,
        onSurface,
        onSurfaceVariant,
        surfaceVariant,
        surfaceContainer,
        surfaceContainerHigh,
        outlineVariant,
        primary,
    ) {
        val textColor = colorHex(onSurface)
        val codeBg = colorHex(surfaceVariant.copy(alpha = 0.7f))
        val preBg = colorHex(surfaceContainer)
        val borderColor = colorHex(outlineVariant)
        val quoteBorder = colorHex(primary)
        val quoteColor = colorHex(onSurfaceVariant)
        val thBg = colorHex(surfaceContainerHigh)

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 15px;
                    line-height: 1.7;
                    color: $textColor;
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    word-wrap: break-word;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 12px;
                    margin-bottom: 8px;
                    font-weight: 600;
                }
                h1 { font-size: 18px; }
                h2 { font-size: 16px; }
                h3 { font-size: 15px; }
                strong, b { font-weight: 600; }
                code {
                    background-color: $codeBg;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-size: 13px;
                }
                pre {
                    background-color: $preBg;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin: 8px 0;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 12px 0;
                    font-size: 14px;
                }
                th, td {
                    border: 1px solid $borderColor;
                    padding: 8px 10px;
                    text-align: left;
                }
                th {
                    background-color: $thBg;
                    font-weight: 600;
                }
                ul, ol {
                    margin: 8px 0;
                    padding-left: 20px;
                }
                li {
                    margin: 4px 0;
                }
                p {
                    margin: 6px 0;
                }
                blockquote {
                    border-left: 3px solid $quoteBorder;
                    padding-left: 12px;
                    margin: 8px 0;
                    color: $quoteColor;
                }
            </style>
        </head>
        <body>
            <div id="content"></div>
            <script>
                document.getElementById('content').innerHTML = marked.parse('$escapedContent');
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                }
            }
        },
        update = { webView ->
            val currentTag = webView.tag as? String
            if (currentTag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CookieSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                drawMiniCookie()
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    DotAnimation(index = index)
                }
            }
        }
    }
}

@Composable
private fun DotAnimation(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = index * 150),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot",
    )

    Box(
        modifier = Modifier
            .size((6f * scale).dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
    )
}

@Composable
private fun InputArea(inputText: String, onInputChange: (String) -> Unit, onSend: () -> Unit, isLoading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                enabled = !isLoading,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "有什么想问我的吗",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        IconButton(
            onClick = onSend,
            enabled = !isLoading && inputText.isNotBlank(),
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
