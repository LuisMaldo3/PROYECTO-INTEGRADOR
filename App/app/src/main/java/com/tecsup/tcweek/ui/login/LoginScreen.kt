package com.tecsup.tcweek.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.tcweek.R

private val LightBlue = Color(0xFF29B6F6)
private val BluePrimary = Color(0xFF0B63FF)
private val FieldBg = Color(0xFFF5F8FD)
private val FieldBorder = Color(0xFFD9E2F1)
private val FieldText = Color(0xFF6B7A99)
private val DarkText = Color(0xFF0D1B3E)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onAccessDenied: (String) -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val currentOnAccessDenied by rememberUpdatedState(onAccessDenied)
    val enabled = !state.isLoading && !state.isSuccess

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            currentOnLoginSuccess()
        }
    }

    LaunchedEffect(state.deniedReason) {
        state.deniedReason?.let { reason ->
            currentOnAccessDenied(reason)
            vm.onDeniedHandled()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fondo
        Image(
            painter = painterResource(R.drawable.bg_login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Nombre y logo
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(color = Color.White)
                        ) {
                            append("VYN")
                        }

                        withStyle(
                            SpanStyle(color = LightBlue)
                        ) {
                            append("K")
                        }
                    },
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Image(
                    painter = painterResource(R.drawable.logo_tcweek),
                    contentDescription = "T&C Week",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .scale(1.25f),
                    contentScale = ContentScale.Fit
                )
            }

            // Formulario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(
                            topStart = 32.dp,
                            topEnd = 32.dp
                        )
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Correo institucional",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = DarkText
                )

                LoginField(
                    value = state.email,
                    onValueChange = vm::onEmailChange,
                    placeholder = "usuario@tecsup.edu.pe",
                    leading = Icons.Default.Email,
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                    enabled = enabled
                )

                Text(
                    text = "Contraseña",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = DarkText
                )

                LoginField(
                    value = state.password,
                    onValueChange = vm::onPasswordChange,
                    placeholder = "••••••••••",
                    leading = Icons.Default.Lock,
                    error = state.passwordError,
                    keyboardType = KeyboardType.Password,
                    enabled = enabled,
                    isPassword = true,
                    passwordVisible = state.passwordVisible,
                    onToggleVisibility = vm::togglePasswordVisibility
                )

                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                // Acceso con correo y contraseña
                Button(
                    onClick = vm::onLoginClick,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = BluePrimary
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "INICIAR SESIÓN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }

                // Texto visual; recuperación aún pendiente de implementar.
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = BluePrimary,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )

                // Separador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FieldBorder
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(8.dp)
                            .border(
                                width = 1.5.dp,
                                color = FieldText,
                                shape = CircleShape
                            )
                    )

                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FieldBorder
                    )
                }

                // Acceso con Google
                OutlinedButton(
                    onClick = {
                        vm.onGoogleClick(context)
                    },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = FieldBorder
                    )
                ) {
                    Image(
                        painter = painterResource(R.drawable.google),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Continuar con Google",
                        color = DarkText,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    error: String?,
    keyboardType: KeyboardType,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = FieldText
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = FieldText
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    onClick = onToggleVisibility,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                        tint = FieldText
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        isError = error != null,
        supportingText = if (error != null) {
            {
                Text(text = error)
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            disabledContainerColor = FieldBg,
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = FieldBorder,
            focusedTextColor = DarkText,
            unfocusedTextColor = DarkText,
            disabledTextColor = FieldText
        )
    )
}