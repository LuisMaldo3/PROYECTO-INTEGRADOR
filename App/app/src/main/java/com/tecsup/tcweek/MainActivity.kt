package com.tecsup.tcweek

import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.tecsup.tcweek.ui.theme.TCWeekTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.tecsup.tcweek.data.AvisoPendiente
import com.tecsup.tcweek.recordatorios.NotificadorAvisos
import com.tecsup.tcweek.ui.denied.AccessDeniedScreen
import com.tecsup.tcweek.ui.login.LoginScreen
import com.tecsup.tcweek.ui.main.MainScreen

class MainActivity : ComponentActivity() {

    // Una sola petición de permiso a la vez: Android cancela la segunda si se lanzan juntas.
    private val pedirNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val pedirRedLocal =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { pedirNotificacionesSiFalta() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 17+: sin este permiso, la app no puede conectarse al backend local.
        if (Build.VERSION.SDK_INT >= 37 &&
            checkSelfPermission(PERMISO_RED_LOCAL) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirRedLocal.launch(PERMISO_RED_LOCAL)
        } else {
            pedirNotificacionesSiFalta()
        }

        // Recordatorios de actividades: el canal debe existir para que el estudiante pueda ajustarlo en Ajustes.
        NotificadorAvisos.crearCanal(this)
        // Con la app cerrada, tocar un recordatorio la abre aquí; al girar la pantalla no se debe repetir.
        if (savedInstanceState == null) recibirAviso(intent)

        setContent {
            TCWeekTheme {
                AppNav()
            }
        }
    }

    /** Con la app abierta (MainActivity es singleTop), tocar un aviso llega por aquí. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recibirAviso(intent)
    }

    /** Guarda la actividad del aviso tocado (y el cambio de horario, si lo hubo); se abre cuando ya hay sesión. */
    private fun recibirAviso(intent: Intent?) {
        val actividadId = intent?.getStringExtra(NotificadorAvisos.EXTRA_ACTIVIDAD_ID) ?: return
        val nota = intent.getStringExtra(NotificadorAvisos.EXTRA_NOTA)
        val agenda = intent.getBooleanExtra(NotificadorAvisos.EXTRA_ABRIR_AGENDA, false)
        // Ya se atendió: no repetir tras recrear la pantalla.
        intent.removeExtra(NotificadorAvisos.EXTRA_ACTIVIDAD_ID)
        intent.removeExtra(NotificadorAvisos.EXTRA_NOTA)
        intent.removeExtra(NotificadorAvisos.EXTRA_ABRIR_AGENDA)
        AvisoPendiente.abrir(actividadId, nota, agenda)
    }

    /** Android 13+: sin este permiso los recordatorios no se muestran. Si lo niega, la app sigue igual. */
    private fun pedirNotificacionesSiFalta() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(PERMISO_NOTIFICACIONES) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificaciones.launch(PERMISO_NOTIFICACIONES)
        }
    }
}

private const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"
private const val PERMISO_NOTIFICACIONES = "android.permission.POST_NOTIFICATIONS"

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
