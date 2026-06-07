package com.aifinance.feature.importer

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.ImportInProgressRow(contentColor: Color, label: String) {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = contentColor,
        strokeWidth = 2.dp,
    )
    Spacer(modifier = Modifier.size(8.dp))
    Text(text = label, color = contentColor)
}

@Composable
fun RowScope.ImportIdleUploadRow(contentColor: Color, label: String) {
    Icon(
        imageVector = Icons.Default.UploadFile,
        contentDescription = null,
        tint = contentColor,
    )
    Spacer(modifier = Modifier.size(8.dp))
    Text(text = label, color = contentColor)
}
