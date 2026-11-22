
package com.tecsup.tcweek.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.tecsup.tcweek.ui.comun.presionable
import com.tecsup.tcweek.ui.comun.sombraSuave
import com.tecsup.tcweek.ui.theme.VynkBackground

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.tcweek.ui.comun.VynkLogo
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkLine
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSky
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.tecsup.tcweek.data.AvisoPendiente
import com.tecsup.tcweek.data.ErrorIntereses
import com.tecsup.tcweek.data.InteresesApi
import com.tecsup.tcweek.recordatorios.RegistroDispositivo
import kotlinx.coroutines.CancellationException

private val AzulVynk = VynkBlue
private val GrisTexto = VynkMuted

private const val RUTA_RECOMENDACIONES = "recomendaciones"
private const val RUTA_CLASIFICACION = "clasificacion"
private const val RUTA_AGENDA_SUGERIDA = "agenda-sugerida"
private const val RUTA_HORAS_LIBRES = "horas-libres"
private const val RUTA_PROGRAMACION = "programacion"
private const val RUTA_UBICACION = "ubicacion/{actividadId}"

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

    // Recordatorios: este celular queda registrado con la cuenta que acaba de entrar.
    LaunchedEffect(user.llave) {
        RegistroDispositivo(context).sincronizar()
    }
    // Actividad del aviso que el estudiante tocó: se abre cuando ya pasó la pantalla de intereses.
    val aperturaAviso by AvisoPendiente.apertura.collectAsState()

    val entradaActual by navMenu.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { VynkLogo(tamano = 22.sp) },
                actions = {
                    if (!primerIngreso) TextButton(
                        onClick = {
                            navMenu.navigate(RUTA_RECOMENDACIONES) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VynkSky, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Para ti", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VynkNavy)
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
                InicioScreen(
                    onCatalogo = { navMenu.navigate(Destino.CATALOGO.ruta) { launchSingleTop = true } },
                    onRecomendaciones = { navMenu.navigate(RUTA_RECOMENDACIONES) { launchSingleTop = true } },
                    onAgenda = { navMenu.navigate(Destino.AGENDA.ruta) { launchSingleTop = true } },
                    onAsistente = { navMenu.navigate(Destino.ASISTENTE.ruta) { launchSingleTop = true } },
                    onResultados = { navMenu.navigate(RUTA_CLASIFICACION) { launchSingleTop = true } },
                    onProgramacion = { navMenu.navigate(RUTA_PROGRAMACION) { launchSingleTop = true } },
                    onVerUbicacion = { navMenu.navigate("ubicacion/$it") }
                )
            }

            composable(Destino.CATALOGO.ruta) {
                CatalogoScreen(
                    onProgramacion = { navMenu.navigate(RUTA_PROGRAMACION) { launchSingleTop = true } },
                    onVerUbicacion = { navMenu.navigate("ubicacion/${it.id}") }
                )
            }

            // US-70: programación completa del día.
            composable(RUTA_PROGRAMACION) {
                ProgramacionScreen(
                    onVolver = { navMenu.popBackStack() },
                    onVerUbicacion = { navMenu.navigate("ubicacion/${it.id}") }
                )
            }

            // US-71: dónde es una actividad.
            composable(RUTA_UBICACION) { entrada ->
                val actividadId = entrada.arguments?.getString("actividadId").orEmpty()
                UbicacionScreen(actividadId = actividadId, onVolver = { navMenu.popBackStack() })
            }
            composable(RUTA_CLASIFICACION) {
                ClasificacionScreen(onVolver = { navMenu.popBackStack() })
            }

            composable(Destino.AGENDA.ruta) {
                AgendaScreen(onVerSugerida = {
                    navMenu.navigate(RUTA_AGENDA_SUGERIDA) { launchSingleTop = true }
                }, onVerHuecos = { navMenu.navigate(RUTA_HORAS_LIBRES) { launchSingleTop = true } })
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

            composable(RUTA_HORAS_LIBRES) {
                HorarioLibreScreen(onVolver = { navMenu.popBackStack() })
            }

            composable(RUTA_AGENDA_SUGERIDA) {
                AgendaInteligenteScreen(onVolver = { navMenu.popBackStack() })
            }

            composable(RUTA_RECOMENDACIONES) {
                RecomendacionesScreen(
                    onSubirHorario = { navMenu.navigate(RUTA_HORAS_LIBRES) { launchSingleTop = true } },
                    onIntereses = { navMenu.navigate(RUTA_INTERESES) { launchSingleTop = true } }
                )
            }
        }
    }

    val abrir = aperturaAviso
    val listo = abrir != null && !comprobandoIntereses && !primerIngreso
    // Cancelación: la actividad ya no existe para el estudiante; se muestra su agenda ya sin ella.
    LaunchedEffect(abrir, listo) {
        if (listo && abrir!!.agenda) {
            navMenu.navigate(Destino.AGENDA.ruta) {
                popUpTo(Destino.INICIO.ruta) { saveState = true }
                launchSingleTop = true
            }
            AvisoPendiente.consumir()
        }
    }
    if (listo && !abrir!!.agenda) {
        ActividadDeAviso(abrir.actividadId, user.llave, nota = abrir.nota, alCerrar = AvisoPendiente::consumir)
    }
}

/**
 * Barra inferior flotante: el destino activo se marca con una píldora que crece y muestra su nombre; los demás solo
 * con su ícono. El cambio se anima con resorte para que se sienta ágil.
 */
@Composable
private fun BarraInferior(
    destinos: List<Destino>,
    rutaActual: String?,
    onIr: (Destino) -> Unit
) {
    Box(
        Modifier.fillMaxWidth().background(VynkBackground).navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().sombraSuave(RoundedCornerShape(26.dp), 12.dp)
                .clip(RoundedCornerShape(26.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinos.forEach { destino ->
                val activo = rutaActual == destino.ruta ||
                    (destino == Destino.PERFIL && rutaActual == RUTA_INTERESES) ||
                    (destino == Destino.AGENDA && (rutaActual == RUTA_AGENDA_SUGERIDA || rutaActual == RUTA_HORAS_LIBRES)) ||
                    (destino == Destino.CATALOGO && (rutaActual == RUTA_PROGRAMACION || rutaActual == RUTA_UBICACION))
                ItemBarra(destino, activo) { onIr(destino) }
            }
        }
    }
}

@Composable
private fun ItemBarra(destino: Destino, activo: Boolean, onClick: () -> Unit) {
    val fondo by animateColorAsState(if (activo) VynkNavy else Color.Transparent, tween(260), label = "fondo")
    val icono by animateColorAsState(if (activo) Color.White else GrisTexto, tween(260), label = "icono")
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(fondo)
            .presionable(onClick = onClick)
            .semantics { contentDescription = destino.titulo; selected = activo }
            .padding(horizontal = if (activo) 14.dp else 12.dp, vertical = 10.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(destino.icono(), contentDescription = null, tint = icono, modifier = Modifier.size(22.dp))
        if (activo) {
            Spacer(Modifier.width(6.dp))
            Text(destino.titulo, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
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
