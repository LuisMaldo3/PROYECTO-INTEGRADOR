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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import com.tecsup.tcweek.ui.comun.FranjaAreasConNombre
import com.tecsup.tcweek.ui.comun.VynkLogo
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

private val LightBlue = Color(0xFF7ED7FF)
private val BluePrimary = Color(0xFF1557D0)
private val FieldBg = Color(0xFFF2F5F9)
private val FieldBorder = Color(0xFFC6D1DF)
private val FieldText = Color(0xFF55657E)
private val DarkText = Color(0xFF0B2A5B)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Foto del campus con la marca y las cinco áreas
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            Image(
                painter = painterResource(R.drawable.bg_campus),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0xCC081C40), 0.45f to Color(0x66081C40), 1f to Color(0xF2081C40)
                    )
                )
            )
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VynkLogo(tamano = 30.sp)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Actividades, competencias y vida en el campus.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    FranjaAreasConNombre()
                }
            }
        }

        // Formulario
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Inicia sesión", style = MaterialTheme.typography.headlineSmall, color = DarkText)
            Text("Usa tu cuenta @tecsup.edu.pe.", style = MaterialTheme.typography.bodyMedium, color = FieldText)
            Spacer(Modifier.height(6.dp))

            Text("Correo institucional", style = MaterialTheme.typography.labelLarge, color = DarkText)
            LoginField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                placeholder = "tu.nombre@tecsup.edu.pe",
                leading = Icons.Default.Email,
                error = state.emailError,
                keyboardType = KeyboardType.Email,
                enabled = enabled
            )

            Text("Contraseña", style = MaterialTheme.typography.labelLarge, color = DarkText)
            LoginField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                placeholder = "Tu contraseña",
                leading = Icons.Default.Lock,
                error = state.passwordError,
                keyboardType = KeyboardType.Password,
                enabled = enabled,
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onToggleVisibility = vm::togglePasswordVisibility
            )

            state.errorMessage?.let { message ->
                Surface(color = Color(0xFFFDECEA), shape = RoundedCornerShape(10.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
            }

            Button(
                onClick = vm::onLoginClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Iniciar sesión", style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = FieldBorder)
                Text("o continúa con", style = MaterialTheme.typography.bodySmall, color = FieldText,
                    modifier = Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(Modifier.weight(1f), color = FieldBorder)
            }

            OutlinedButton(
                onClick = { vm.onGoogleClick(context) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FieldBorder)
            ) {
                Image(painter = painterResource(R.drawable.google), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Google", color = DarkText, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
            }

            Text("¿Aún no tienes contraseña? Entra con Google.", style = MaterialTheme.typography.bodySmall,
                color = FieldText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = FieldBg,
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = FieldBorder,
            focusedTextColor = DarkText,
            unfocusedTextColor = DarkText,
            disabledTextColor = FieldText
        )
    )
}