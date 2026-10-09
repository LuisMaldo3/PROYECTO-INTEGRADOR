package com.tecsup.tcweek

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.tecsup.tcweek.ui.denied.AccessDeniedScreen
import com.tecsup.tcweek.ui.login.LoginScreen
import com.tecsup.tcweek.ui.main.MainScreen

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
            MainScreen(
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
