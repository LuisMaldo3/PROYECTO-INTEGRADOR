package com.tecsup.tcweek.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.repository.DatosDeEntrada
import com.tecsup.tcweek.ui.components.DegradadoVynk
import com.tecsup.tcweek.ui.components.FranjaAreas
import com.tecsup.tcweek.ui.components.VynkLogo
import com.tecsup.tcweek.ui.theme.ColorSeccion
import com.tecsup.tcweek.ui.theme.VynkBlueSoft
import com.tecsup.tcweek.ui.theme.VynkError
import com.tecsup.tcweek.ui.theme.VynkErrorSoft
import com.tecsup.tcweek.ui.theme.VynkMuted
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkSky
import com.tecsup.tcweek.ui.theme.VynkSubtle

/** Una opción del menú lateral: a qué ruta lleva, cómo se llama, su ícono y su color. */
data class OpcionLateral(val ruta: String, val titulo: String, val icono: ImageVector, val color: Color)

/** Un grupo de opciones con su título. */
data class GrupoLateral(val titulo: String, val opciones: List<OpcionLateral>)

/**
 * Menú lateral desplegable: cabecera con la cuenta, las opciones agrupadas y «Cerrar sesión» al final.
 * [rutaActual] marca la opción abierta; [onIr] recibe la ruta elegida.
 */
@Composable
fun MenuLateral(
    usuario: DatosDeEntrada,
    rol: String,
    grupos: List<GrupoLateral>,
    rutaActual: String?,
    cerrando: Boolean,
    onIr: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
        modifier = Modifier.fillMaxHeight()
    ) {
        CabeceraCuenta(usuario, rol)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            grupos.forEachIndexed { i, grupo ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 12.dp), color = VynkBlueSoft)
                Text(
                    grupo.titulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = VynkSubtle,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
                )
                grupo.opciones.forEach { op ->
                    val activa = rutaActual == op.ruta
                    NavigationDrawerItem(
                        selected = activa,
                        onClick = { onIr(op.ruta) },
                        icon = { IconoOpcion(op.icono, op.color, activa) },
                        label = { Text(op.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1) },
                        shape = RoundedCornerShape(14.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = op.color.copy(alpha = 0.12f),
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = VynkNavy,
                            unselectedTextColor = VynkNavy
                        )
                    )
                }
            }
        }
        Column(Modifier.navigationBarsPadding().padding(12.dp)) {
            NavigationDrawerItem(
                selected = false,
                onClick = { if (!cerrando) onCerrarSesion() },
                icon = { IconoOpcion(Icons.AutoMirrored.Filled.Logout, VynkError, false) },
                label = {
                    Text(if (cerrando) "Cerrando…" else "Cerrar sesión", style = MaterialTheme.typography.titleSmall,
                        color = VynkError)
                },
                shape = RoundedCornerShape(14.dp),
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = VynkErrorSoft)
            )
        }
    }
}

@Composable
private fun CabeceraCuenta(usuario: DatosDeEntrada, rol: String) {
    val nombre = usuario.nombre.ifBlank { "Mi cuenta" }
    val iniciales = usuario.nombre.ifBlank { usuario.correo }.trim().split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
    Column(Modifier.fillMaxWidth().background(DegradadoVynk)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            VynkLogo()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(52.dp).border(2.dp, VynkSky, CircleShape).padding(3.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iniciales, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(nombre, color = Color.White, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(usuario.correo, color = Color(0xFFD6E4F7), style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 3.dp)) {
                Text(if (rol == ROL_USUARIO) "Estudiante" else etiquetaRol(rol), color = VynkNavy,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        FranjaAreas(alto = 4)
    }
}

/** Ícono en un cuadro de color: relleno cuando la opción está abierta, tenue cuando no. */
@Composable
private fun IconoOpcion(icono: ImageVector, color: Color, activa: Boolean) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
            .background(if (activa) color else color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = if (activa) Color.White else color, modifier = Modifier.size(19.dp))
    }
}

/** Las opciones del menú lateral, agrupadas. Las rutas son las del NavHost de [MainScreen]. */
fun gruposMenuLateral(
    rutaRecomendaciones: String,
    rutaProgramacion: String,
    rutaClasificacion: String,
    rutaHorasLibres: String
): List<GrupoLateral> = listOf(
    GrupoLateral("Principal", listOf(
        OpcionLateral(Destino.INICIO.ruta, "Inicio", Icons.Filled.Home, ColorSeccion.PARA_TI),
        OpcionLateral(Destino.CATALOGO.ruta, "Actividades", Icons.Filled.Search, ColorSeccion.ACTIVIDADES),
        OpcionLateral(Destino.AGENDA.ruta, "Mi agenda", Icons.Filled.Event, ColorSeccion.AGENDA),
        OpcionLateral(Destino.ASISTENTE.ruta, "Asistente IA", Icons.Filled.QuestionAnswer, ColorSeccion.ASISTENTE)
    )),
    GrupoLateral("Descubre", listOf(
        OpcionLateral(rutaRecomendaciones, "Para ti", Icons.Filled.AutoAwesome, ColorSeccion.PARA_TI),
        OpcionLateral(rutaProgramacion, "Programación por día", Icons.Filled.CalendarMonth, ColorSeccion.PROGRAMACION),
        OpcionLateral(rutaClasificacion, "Clasificación", Icons.Filled.EmojiEvents, ColorSeccion.CLASIFICACION)
    )),
    GrupoLateral("Mi cuenta", listOf(
        OpcionLateral(Destino.PERFIL.ruta, "Mi perfil", Icons.Filled.Person, ColorSeccion.PERFIL),
        OpcionLateral(RUTA_INTERESES, "Mis intereses", Icons.Filled.Favorite, ColorSeccion.PROGRAMACION),
        OpcionLateral(rutaHorasLibres, "Mis horas libres", Icons.Filled.Schedule, ColorSeccion.AGENDA)
    ))
)
