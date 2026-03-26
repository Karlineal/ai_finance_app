package com.aifinance.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary

private val InactiveTopTabText = Color(0xFFB8C0CC)
private val HandDrawBlue = Color(0xFF4F7EFF)

@Composable
fun TopRecordAiBar(
    selectedTab: HomeTopTab,
    onMenuClick: () -> Unit,
    onTabSelected: (HomeTopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "菜单",
                tint = OnSurfacePrimary,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopTabItem(
                text = "记录",
                selected = selectedTab == HomeTopTab.RECORD,
                onClick = { onTabSelected(HomeTopTab.RECORD) },
            )
            Spacer(modifier = Modifier.width(40.dp))
            TopTabItem(
                text = "AI助手",
                selected = selectedTab == HomeTopTab.AI_ASSISTANT,
                onClick = { onTabSelected(HomeTopTab.AI_ASSISTANT) },
            )
        }
    }
}

@Composable
private fun TopTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = IcokieTextStyles.titleLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OnSurfacePrimary else InactiveTopTabText,
        )

        Spacer(modifier = Modifier.height(2.dp))

        if (selected) {
            HandDrawUnderline()
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HandDrawUnderline() {
    Box(modifier = Modifier.height(8.dp)) {
        Canvas(modifier = Modifier.size(width = 42.dp, height = 8.dp)) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.72f)
                quadraticBezierTo(
                    size.width * 0.25f,
                    size.height * 0.22f,
                    size.width * 0.5f,
                    size.height * 0.68f,
                )
                quadraticBezierTo(
                    size.width * 0.74f,
                    size.height * 0.97f,
                    size.width,
                    size.height * 0.58f,
                )
            }
            drawPath(
                path = path,
                color = HandDrawBlue,
            )
            drawCircle(
                color = HandDrawBlue,
                radius = 1.6.dp.toPx(),
                center = Offset(size.width * 0.95f, size.height * 0.58f),
            )
        }
    }
}
