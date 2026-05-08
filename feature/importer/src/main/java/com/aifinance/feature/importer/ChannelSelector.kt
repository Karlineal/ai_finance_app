package com.aifinance.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifinance.core.designsystem.theme.IcokieTextStyles

@Composable
fun ChannelSelectorCard(
    selectedChannel: ImportChannel,
    onChannelSelected: (ImportChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "导入类型",
                style = IcokieTextStyles.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ImportChannelToggleRow(
                selectedChannel = selectedChannel,
                onChannelSelected = onChannelSelected,
            )
        }
    }
}

@Composable
private fun ImportChannelToggleRow(
    selectedChannel: ImportChannel,
    onChannelSelected: (ImportChannel) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ImportChannelItem(
            label = "微信",
            icon = Icons.Default.Wallet,
            selected = selectedChannel == ImportChannel.WECHAT,
            onClick = { onChannelSelected(ImportChannel.WECHAT) },
            modifier = Modifier.weight(1f),
        )
        ImportChannelItem(
            label = "支付宝",
            icon = Icons.Default.Description,
            selected = selectedChannel == ImportChannel.ALIPAY,
            onClick = { onChannelSelected(ImportChannel.ALIPAY) },
            modifier = Modifier.weight(1f),
        )
        ImportChannelItem(
            label = "银行账单",
            icon = Icons.Default.AccountBalance,
            selected = selectedChannel == ImportChannel.BANK,
            onClick = { onChannelSelected(ImportChannel.BANK) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImportChannelItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val iconTint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}
