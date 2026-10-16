
package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.ErrorIntereses
import com.tecsup.tcweek.data.InteresesApi
import kotlinx.coroutines.CancellationException

private val AzulVynk = Color(0xFF0B63FF)
private val GrisTexto = Color(0xFF6B7A99)

private const val RUTA_RECOMENDACIONES = "recomendaciones"
private const val RUTA_AGENDA_SUGERIDA = "agenda-sugerida"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AuthRepository(context) }
    val user = remember { repository.currentSession() }

    if (user == null) {
        LaunchedEffect(Unit) {
            repository.logout()
            onLogout()
        }
        return
    }

    val rol = normalizarRol(user.rol)

    val navMenu = rememberNavController()
    var comprobandoIntereses by remember(user.llave) { mutableStateOf(true) }
    var primerIngreso by remember(user.llave) { mutableStateOf(false) }

    LaunchedEffect(user.llave) {
        try {
            val estado = InteresesApi.estado(user.llave)
            val preferencias = contextoOmitir(context)
            val clave = "pendiente_${user.correo}"
            val pendiente = preferencias.getBoolean(clave, false)
            if (estado.configurados || estado.omitido) preferencias.edit().remove(clave).apply()
            else if (pendiente) {
                try {
                    InteresesApi.omitir(user.llave)
                    preferencias.edit().remove(clave).apply()
                } catch (e: CancellationException) { throw e }
                  catch (_: Exception) { /* Reintentar en la próxima entrada. */ }
            }
            primerIngreso = !pendiente && debeSolicitarIntereses(estado.configurados, estado.omitido)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorIntereses) {
            if (e.estado == 401) {
                repository.logout()
                onLogout()
            }
            // Ante errores de red, no bloqueamos toda la app.
        } catch (e: Exception) {
            // Una respuesta inesperada del servidor tampoco debe cerrar la aplicación.
        } finally {
            comprobandoIntereses = false
        }
    }

    val entradaActual by navMenu.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VYNK",
                        fontWeight = FontWeight.ExtraBold,
                        color = AzulVynk
                    )
                },
                actions = {
                    if (!primerIngreso) TextButton(
                        onClick = {
                            navMenu.navigate(RUTA_RECOMENDACIONES) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Text(
                            text = "✦ Para ti",
                            color = AzulVynk
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!primerIngreso) BarraInferior(
                destinos = Destino.entries,
                rutaActual = rutaActual,
                onIr = { destino ->
                    navMenu.navigate(destino.ruta) {
                        popUpTo(Destino.INICIO.ruta) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { relleno ->
        if (comprobandoIntereses) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulVynk)
            }
        } else NavHost(
            navController = navMenu,
            startDestination = if (primerIngreso) RUTA_INTERESES else Destino.INICIO.ruta,
            modifier = Modifier.padding(relleno)
        ) {
            composable(Destino.INICIO.ruta) {
                InicioScreen()
            }

            composable(Destino.CATALOGO.ruta) {
                CatalogoScreen()
            }

            composable(Destino.AGENDA.ruta) {
                AgendaScreen(onVerSugerida = {
                    navMenu.navigate(RUTA_AGENDA_SUGERIDA) { launchSingleTop = true }
                })
            }

            composable(Destino.ASISTENTE.ruta) {
                AsistenteScreen()
            }

            composable(Destino.PERFIL.ruta) {
                PerfilScreen(
                    user = user,
                    rol = rol,
                    onLogout = onLogout,
                    onIntereses = {
                        navMenu.navigate(RUTA_INTERESES) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(RUTA_INTERESES) {
                InteresesScreen(
                    onVolver = {
                        navMenu.popBackStack()
                    },
                    onSesionExpirada = onLogout,
                    primerIngreso = primerIngreso,
                    onTerminar = {
                        primerIngreso = false
                        navMenu.navigate(Destino.INICIO.ruta) {
                            popUpTo(RUTA_INTERESES) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(RUTA_AGENDA_SUGERIDA) {
                AgendaSugeridaScreen(onVolver = { navMenu.popBackStack() })
            }

            composable(RUTA_RECOMENDACIONES) {
                RecomendacionesScreen()
            }
        }
    }
}

@Composable
private fun BarraInferior(
    destinos: List<Destino>,
    rutaActual: String?,
    onIr: (Destino) -> Unit
) {
    NavigationBar(
        containerColor = Color.White
    ) {
        destinos.forEach { destino ->
            NavigationBarItem(
                selected = rutaActual == destino.ruta ||
                        (
                                destino == Destino.PERFIL &&
                                        rutaActual == RUTA_INTERESES
                                ) || (destino == Destino.AGENDA && rutaActual == RUTA_AGENDA_SUGERIDA),
                onClick = {
                    onIr(destino)
                },
                icon = {
                    Icon(
                        imageVector = destino.icono(),
                        contentDescription = null
                    )
                },
                label = {
                    Text(destino.titulo)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = AzulVynk,
                    indicatorColor = AzulVynk,
                    unselectedIconColor = GrisTexto,
                    unselectedTextColor = GrisTexto
                )
            )
        }
    }
}

private fun Destino.icono(): ImageVector = when (this) {
    Destino.INICIO -> Icons.Filled.Home
    Destino.CATALOGO -> Icons.Filled.Search
    Destino.AGENDA -> Icons.Filled.Event
    Destino.ASISTENTE -> Icons.Filled.QuestionAnswer
    Destino.PERFIL -> Icons.Filled.Person
}
