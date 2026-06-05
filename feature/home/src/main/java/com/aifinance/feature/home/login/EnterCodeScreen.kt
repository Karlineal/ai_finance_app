package com.aifinance.feature.home.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.feature.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterCodeScreen(
    phoneNumber: String,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    val correctCode = remember { String.format("%06d", Random.nextInt(1000000)) }

    // Auto-verify when 6 digits entered
    LaunchedEffect(inputCode) {
        if (inputCode.length == 6) {
            if (inputCode == correctCode) {
                viewModel.login()
                Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                delay(300)
                onLoginSuccess()
            } else {
                Toast.makeText(context, "验证码错误，请重新输入", Toast.LENGTH_SHORT).show()
                delay(500)
                inputCode = ""
            }
        }
    }

    val maskedPhone = if (phoneNumber.length == 11) {
        "${phoneNumber.substring(0, 3)}****${phoneNumber.substring(7)}"
    } else {
        phoneNumber
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "输入验证码",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "验证码已发送至 $maskedPhone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Show the verification code directly (pseudo-login)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "您的验证码为: $correctCode",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "（伪登录模式 - 验证码直接展示）",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 6-digit code input
            BasicTextField(
                value = inputCode,
                onValueChange = {
                    if (it.length <= 6) {
                        inputCode = it.filter { char -> char.isDigit() }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(6) { index ->
                            val char = when {
                                index >= inputCode.length -> ""
                                else -> inputCode[index].toString()
                            }
                            val isFocused = index == inputCode.length
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(56.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) BrandPrimary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}
