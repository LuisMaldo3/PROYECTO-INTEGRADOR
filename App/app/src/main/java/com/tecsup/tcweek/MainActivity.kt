package com.tecsup.tcweek

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.ui.denied.AccessDeniedScreen
import com.tecsup.tcweek.ui.login.LoginScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pedirRedLocal =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 17+: sin este permiso, la app no puede conectarse al backend local.
        if (Build.VERSION.SDK_INT >= 37 &&
            checkSelfPermission(PERMISO_RED_LOCAL) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirRedLocal.launch(PERMISO_RED_LOCAL)
        }

        setContent {
            MaterialTheme {
                AppNav()
            }
        }
    }
}

private const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    nav.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onAccessDenied = { reason ->
                    nav.navigate("denied?reason=${Uri.encode(reason)}") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "denied?reason={reason}",
            arguments = listOf(
                navArgument("reason") {
                    type = NavType.StringType
                    defaultValue = "Tu cuenta no tiene permiso para ingresar."
                }
            )
        ) { entry ->
            AccessDeniedScreen(
                reason = entry.arguments?.getString("reason").orEmpty(),
                onBackToLogin = {
                    nav.navigate("login") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onLogout = {
                    nav.navigate("login") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    val repository = remember { AuthRepository(context) }
    val user = remember { repository.currentSession() }

    if (user == null) {
        LaunchedEffect(Unit) {
            repository.logout()
            onLogout() // sesión vencida o inexistente: vuelve al login
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        )
    ) {
        Text(
            text = "¡Bienvenido a VYNK!",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(text = user.correo)
        Text(text = "Rol: ${user.rol}")

        Button(
            enabled = !closing,
            onClick = {
                closing = true

                scope.launch {
                    repository.logout()

                    try {
                        CredentialManager.create(context)
                            .clearCredentialState(
                                ClearCredentialStateRequest()
                            )
                    } catch (_: ClearCredentialException) {
                        // La sesión de VYNK ya quedó cerrada.
                    }

                    onLogout()
                }
            }
        ) {
            Text(if (closing) "Cerrando..." else "Cerrar sesión")
        }
    }
}