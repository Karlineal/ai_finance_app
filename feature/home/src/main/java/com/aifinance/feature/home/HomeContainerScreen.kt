package com.aifinance.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeContainerScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToAssetManagement: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTopTab by rememberSaveable { mutableStateOf(HomeTopTab.RECORD) }

    Column(modifier = modifier.fillMaxSize()) {
        TopRecordAiBar(
            selectedTab = selectedTopTab,
            onMenuClick = onOpenDrawer,
            onTabSelected = { selectedTopTab = it },
            modifier = Modifier.padding(top = 4.dp),
        )

        when (selectedTopTab) {
            HomeTopTab.RECORD -> {
                RecordHomeContent(
                    onNavigateToAssetManagement = onNavigateToAssetManagement,
                )
            }

            HomeTopTab.AI_ASSISTANT -> {
                AiAssistantScreen()
            }
        }
    }
}
