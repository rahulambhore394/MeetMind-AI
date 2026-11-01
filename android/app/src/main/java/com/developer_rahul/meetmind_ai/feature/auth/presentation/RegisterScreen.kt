package com.developer_rahul.meetmind_ai.feature.auth.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Back Navigation Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(GlassWhite, CircleShape)
                .border(1.dp, GlassBorder, CircleShape)
                .bounceClick()
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Brand Header Badge
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ElectricIndigo.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "MEETMIND AI",
                    style = Typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = NeonCyan
                )
                Text(
                    text = "Join Next-Gen Meetings",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Create Account",
            style = Typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Unlock AI transcriptions, automated action items, live translation, and AI proxy agent",
            style = Typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Form Container Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                MeetMindTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person,
                    error = uiState.nameError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                MeetMindTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    error = uiState.emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                var passwordVisible by remember { mutableStateOf(false) }
                MeetMindTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    error = uiState.passwordError,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextSecondary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            viewModel.register()
                        }
                    )
                )

                // Password Strength Meter Indicator
                if (uiState.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val strength = calculatePasswordStrength(uiState.password)
                    val strengthColor by animateColorAsState(
                        when (strength) {
                            PasswordStrength.WEAK -> RoseRed
                            PasswordStrength.FAIR -> AmberGold
                            PasswordStrength.STRONG -> EmeraldGreen
                        },
                        label = "strengthColor"
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Password Strength", style = Typography.labelSmall, color = TextSecondary)
                            Text(
                                text = strength.name,
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = strengthColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (strength.ordinal >= 0) strengthColor else BorderColor)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (strength.ordinal >= 1) strengthColor else BorderColor)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (strength.ordinal >= 2) strengthColor else BorderColor)
                            )
                        }
                    }
                }

                if (uiState.error != null) {
                    Surface(
                        color = RoseRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = RoseRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.error!!,
                                color = RoseRed,
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                MeetMindButton(
                    text = "Create Account 🚀",
                    onClick = {
                        keyboardController?.hide()
                        viewModel.register()
                    },
                    isLoading = uiState.isLoading,
                    isAiAction = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "By creating an account, you agree to MeetMind AI Terms of Service and Privacy Policy.",
            style = Typography.labelSmall,
            color = TextDisabled,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = TextSecondary, style = Typography.bodyMedium)
            TextButton(
                onClick = onBack,
                modifier = Modifier.bounceClick()
            ) {
                Text("Sign In", color = ElectricIndigo, style = Typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private enum class PasswordStrength { WEAK, FAIR, STRONG }

private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    val hasLetters = password.any { it.isLetter() }
    val hasDigits = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    return when {
        password.length >= 8 && hasLetters && hasDigits && hasSpecial -> PasswordStrength.STRONG
        password.length >= 6 && hasLetters && hasDigits -> PasswordStrength.FAIR
        else -> PasswordStrength.WEAK
    }
}
