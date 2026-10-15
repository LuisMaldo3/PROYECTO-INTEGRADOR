package com.tecsup.tcweek.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tecsup.tcweek.data.AuthRepository

private val AzulVynk = Color(0xFF0B63FF)
private val GrisTexto = Color(0xFF6B7A99)

/**
 * Pantalla principal después de iniciar sesión: contenido arriba y menú inferior abajo.
 * Por ahora Inicio, Catálogo, Agenda y Asistente están vacías (cada una tiene su archivo
 * para llenarla cuando toque su historia); Perfil ya tiene los datos y «Cerrar sesión».
 */
@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AuthRepository(context) }
    val user = remember { repository.currentSession() }

    if (user == null) {
        LaunchedEffect(Unit) {
            repository.logout()
            onLogout() // sesión vencida o inexistente: vuelve al login
        }
        return
    }

    val rol = normalizarRol(user.rol)

    // Navegación interna del menú inferior (la de afuera maneja login / acceso denegado / home).
    val navMenu = rememberNavController()
    val entradaActual by navMenu.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        topBar = { TopAppBar(title = { Text("VYNK", fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = AzulVynk) },
            actions = { TextButton(onClick = { navMenu.navigate("recomendaciones") { launchSingleTop = true } }) { Text("✦ Para ti", color = AzulVynk) } }) },
        bottomBar = {
            BarraInferior(
                destinos = Destino.entries,
                rutaActual = rutaActual,
                onIr = { destino ->
                    navMenu.navigate(destino.ruta) {
                        popUpTo(Destino.INICIO.ruta) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { relleno ->
        NavHost(
            navController = navMenu,
            startDestination = Destino.INICIO.ruta,
            modifier = Modifier.padding(relleno)
        ) {
            composable("recomendaciones") { RecomendacionesScreen() }
            composable("recomendaciones") { RecomendacionesScreen() }
            composable(Destino.INICIO.ruta) { InicioScreen() }
            composable(Destino.CATALOGO.ruta) { CatalogoScreen() }
            composable(Destino.AGENDA.ruta) { AgendaScreen() }
            composable(Destino.ASISTENTE.ruta) { AsistenteScreen() }
            composable(Destino.PERFIL.ruta) {
                PerfilScreen(
                    user = user,
                    rol = rol,
                    onLogout = onLogout,
                    onIntereses = { navMenu.navigate(RUTA_INTERESES) { launchSingleTop = true } }
                )
            }
            composable(RUTA_INTERESES) {
                InteresesScreen(onVolver = { navMenu.popBackStack() }, onSesionExpirada = onLogout)
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
    NavigationBar(containerColor = Color.White) {
        destinos.forEach { destino ->
            NavigationBarItem(
                selected = rutaActual == destino.ruta || (destino == Destino.PERFIL && rutaActual == RUTA_INTERESES),
                onClick = { onIr(destino) },
                icon = { Icon(imageVector = destino.icono(), contentDescription = null) },
                label = { Text(destino.titulo) },
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
