package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun PasswordLockScreen(
    onPasswordCorrect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) {
        ColorHelpers.getGroup2PageBgColor()
    } else {
        ColorHelpers.getGroup2PageBgColor()
    }
    val textColor = ColorHelpers.getGroup4TextColor()
    val iconColor = ColorHelpers.getGroup4IconColor()
    
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val savedPassword = remember {
        prefs.getString("app_password", "") ?: ""
    }
    
    // 预先获取字符串资源
    val passwordIncorrectText = stringResource(R.string.password_incorrect)
    val biometricUnlockTitle = stringResource(R.string.biometric_unlock_title)
    val biometricUnlockSubtitle = stringResource(R.string.biometric_unlock_subtitle)
    val cancelText = stringResource(R.string.cancel)
    
    // 验证密码的函数
    fun verifyPassword() {
        if (password == savedPassword) {
            keyboardController?.hide()
            onPasswordCorrect()
        } else {
            errorMessage = passwordIncorrectText
            password = ""
        }
    }
    
    // 检查生物识别是否可用
    val biometricManager = remember {
        BiometricManager.from(context)
    }
    val canUseBiometric = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    // 生物识别提示
    val executor = remember {
        ContextCompat.getMainExecutor(context)
    }
    
    val biometricPrompt = remember {
        activity?.let {
            BiometricPrompt(
                it,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onPasswordCorrect()
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // 用户取消或错误，不做任何操作
                    }
                }
            )
        }
    }
    
    // 启动时尝试生物识别
    LaunchedEffect(Unit) {
        if (canUseBiometric && activity != null && biometricPrompt != null) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(biometricUnlockTitle)
                // 移除 subtitle 避免与屏下指纹传感器提示重叠
                .setNegativeButtonText(cancelText)
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 顶部间距，让内容更靠上（可以根据需要调整这个值）
            Spacer(modifier = Modifier.height(120.dp))
            
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.enter_password),
                style = MaterialTheme.typography.headlineMedium,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.password), color = textColor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { verifyPassword() }
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = textColor.copy(alpha = 0.7f),
                    unfocusedLabelColor = textColor.copy(alpha = 0.7f),
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    focusedBorderColor = iconColor.copy(alpha = 0.7f),
                    unfocusedBorderColor = iconColor.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                            tint = iconColor
                        )
                    }
                },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { verifyPassword() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.confirm))
            }
            
            // 如果支持生物识别，显示生物识别按钮
            if (canUseBiometric && activity != null && biometricPrompt != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(biometricUnlockTitle)
                            // 移除 subtitle 避免与屏下指纹传感器提示重叠
                            .setNegativeButtonText(cancelText)
                            .build()
                        biometricPrompt.authenticate(promptInfo)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.use_biometric), color = textColor)
                }
            }
        }
    }
}

