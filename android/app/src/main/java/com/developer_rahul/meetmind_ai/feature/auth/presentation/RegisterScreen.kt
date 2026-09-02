package com.developer_rahul.meetmind_ai.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
            .padding(24.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(GlassWhite, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Create Account",
            style = Typography.displayMedium,
            color = TextPrimary
        )

        Text(
            "Join the future of intelligent collaboration",
            style = Typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        MeetMindTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = "Full Name",
            leadingIcon = Icons.Default.Person,
            error = uiState.nameError
        )

        Spacer(modifier = Modifier.height(20.dp))

        MeetMindTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email Address",
            leadingIcon = Icons.Default.Email,
            error = uiState.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(20.dp))

        var passwordVisible by remember { mutableStateOf(false) }
        MeetMindTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            error = uiState.passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = RoseRed,
                style = Typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        MeetMindButton(
            text = "Create Account",
            onClick = viewModel::register,
            isLoading = uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "By creating an account, you agree to our Terms of Service and Privacy Policy.",
            style = Typography.labelMedium,
            color = TextDisabled,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = TextSecondary, style = Typography.bodyMedium)
            TextButton(onClick = onBack) {
                Text("Sign In", color = ElectricIndigo, style = Typography.labelLarge)
            }
        }
    }
}
