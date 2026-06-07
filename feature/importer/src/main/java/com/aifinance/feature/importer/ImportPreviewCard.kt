package com.aifinance.feature.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnPrimary

@Composable
fun BankImportPreviewCard(
    isImporting: Boolean,
    onImportClick: () -> Unit,
    message: String?,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "银行账单导入规则",
                style = IcokieTextStyles.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "1. 支持 Excel/CSV；2. 收入固定分类“职业收入”；3. 支出固定分类“餐饮”；4. 账单日期无具体时间时统一为 1:00；5. 仅导入收入和支出，不处理账户余额。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ImportPickFileButton(
                isImporting = isImporting,
                idleLabel = "选择银行账单文件并导入",
                onClick = onImportClick,
            )
            ImportResultMessage(message)
        }
    }
}

@Composable
fun ChannelImportPreviewCard(
    channelLabel: String,
    isImporting: Boolean,
    onImportClick: () -> Unit,
    message: String?,
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
                .padding(vertical = 24.dp, horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${channelLabel}导入规则",
                style = IcokieTextStyles.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "支持 CSV 格式，按官方导出列自动识别日期、收支、金额和交易对象。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ImportPickFileButton(
                isImporting = isImporting,
                idleLabel = "选择${channelLabel}文件并导入",
                onClick = onImportClick,
            )
            ImportResultMessage(message)
        }
    }
}

@Composable
private fun ImportPickFileButton(
    isImporting: Boolean,
    idleLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isImporting,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
    ) {
        if (isImporting) {
            ImportInProgressRow(contentColor = OnPrimary, label = "正在导入...")
        } else {
            ImportIdleUploadRow(contentColor = OnPrimary, label = idleLabel)
        }
    }
}

@Composable
private fun ImportResultMessage(message: String?) {
    message?.let {
        val color = when {
            it.startsWith("导入成功") -> MaterialTheme.colorScheme.primary
            it.startsWith("导入失败") -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}
