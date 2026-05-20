package com.aifinance.feature.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.IcokieTextStyles

private val BankImportMimeTypes = arrayOf(
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/csv",
    "application/csv",
    "text/comma-separated-values",
    "application/octet-stream",
    "*/*",
)

private val WechatImportMimeTypes = arrayOf(
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/csv",
    "application/csv",
    "text/comma-separated-values",
    "application/octet-stream",
    "*/*",
)

private val AlipayImportMimeTypes = arrayOf(
    "text/csv",
    "application/csv",
    "text/comma-separated-values",
    "application/octet-stream",
    "*/*",
)

private fun mimeTypesFor(channel: ImportChannel): Array<String> = when (channel) {
    ImportChannel.WECHAT -> WechatImportMimeTypes
    ImportChannel.ALIPAY -> AlipayImportMimeTypes
    ImportChannel.BANK -> BankImportMimeTypes
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImporterScreen(
    onBack: () -> Unit,
    viewModel: ImporterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importSelectedStatement(context, it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { ImporterScreenTopBar(onBack = onBack) },
    ) { paddingValues ->
        ImporterScreenBody(
            paddingValues = paddingValues,
            uiState = uiState,
            onChannelSelected = viewModel::selectChannel,
            onImportClick = {
                viewModel.clearMessage()
                filePickerLauncher.launch(mimeTypesFor(uiState.selectedChannel))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImporterScreenTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "账单导入",
                style = IcokieTextStyles.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun ImporterScreenBody(
    paddingValues: PaddingValues,
    uiState: ImporterUiState,
    onChannelSelected: (ImportChannel) -> Unit,
    onImportClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(paddingValues)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelSelectorCard(
            selectedChannel = uiState.selectedChannel,
            onChannelSelected = onChannelSelected,
        )
        if (uiState.selectedChannel == ImportChannel.BANK) {
            BankImportPreviewCard(
                isImporting = uiState.isImporting,
                onImportClick = onImportClick,
                message = uiState.message,
            )
        } else {
            ChannelImportPreviewCard(
                channelLabel = uiState.selectedChannel.label,
                isImporting = uiState.isImporting,
                onImportClick = onImportClick,
                message = uiState.message,
            )
        }
    }
}
