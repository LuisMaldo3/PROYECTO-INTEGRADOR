package com.tecsup.tcweek.ui.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.semantics.contentDescription
import com.tecsup.tcweek.ui.components.CabeceraColor
import com.tecsup.tcweek.ui.components.oscurecer
import com.tecsup.tcweek.ui.theme.ColorSeccion
import com.tecsup.tcweek.ui.theme.VynkBackground
import com.tecsup.tcweek.ui.components.EstadoVacio
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.data.repository.AuthRepository
import com.tecsup.tcweek.data.repository.CacheRespuestas
import com.tecsup.tcweek.data.repository.MemoriaPantallas
import com.tecsup.tcweek.data.remote.InicioApi
import com.tecsup.tcweek.data.remote.RecomendacionesAppApi
import com.tecsup.tcweek.domain.CambiosAgenda
import com.tecsup.tcweek.data.remote.ErrorIntereses
import com.tecsup.tcweek.data.remote.Interes
import com.tecsup.tcweek.data.remote.InteresesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Azul = Color(0xFF1557D0)
private val Marino = Color(0xFF0B2A5B)
private val Gris = Color(0xFF55657E)
private val Borde = Color(0xFFDDE4EE)

/** Dibujo de cada interés (la lógica de cuál es cuál está en Intereses.kt). */
fun IconoInteres.dibujo(): ImageVector = when (this) {
    IconoInteres.MASCARAS -> Icons.Filled.TheaterComedy
    IconoInteres.MUSICA -> Icons.Filled.MusicNote
    IconoInteres.DANZA -> Icons.Filled.Celebration
    IconoInteres.LIBROS -> Icons.Filled.Book
    IconoInteres.CHIP -> Icons.Filled.Memory
    IconoInteres.DESTELLOS -> Icons.Filled.AutoAwesome
    IconoInteres.IDEA -> Icons.Filled.Lightbulb
    IconoInteres.JUEGO -> Icons.Filled.SportsEsports
    IconoInteres.DEPORTE -> Icons.Filled.SportsSoccer
    IconoInteres.COMIDA -> Icons.Filled.Restaurant
    IconoInteres.PREMIO -> Icons.Filled.EmojiEvents
    IconoInteres.ESTRELLA -> Icons.Filled.Star
}

/** Pantalla «Mis intereses»: la persona marca los temas que le gustan y los guarda. Se abre desde Perfil. */
@Composable
fun InteresesScreen(
    onVolver: () -> Unit,
    onSesionExpirada: () -> Unit,
    primerIngreso: Boolean = false,
    onTerminar: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository(context) }
    val token = remember { repository.currentSession()?.llave.orEmpty() }
    val scope = rememberCoroutineScope()

    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var catalogo by remember { mutableStateOf<List<Interes>>(emptyList()) }
    var guardados by remember { mutableStateOf<List<String>>(emptyList()) }
    var seleccion by remember { mutableStateOf<List<String>>(emptyList()) }
    var guardando by remember { mutableStateOf(false) }
    var omitiendo by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var avisoEsError by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }

    fun sesionExpirada(e: Exception): Boolean {
        if (e is ErrorIntereses && e.estado == 401) {
            repository.logout()
            onSesionExpirada()
            return true
        }
        return false
    }

    LaunchedEffect(token, recarga) {
        cargando = true
        error = null
        try {
            val disponibles = InteresesApi.catalogo(token)
            val mios = ordenarComoCatalogo(InteresesApi.mios(token), disponibles.map { it.id })
            catalogo = disponibles
            guardados = mios
            seleccion = mios
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!sesionExpirada(e)) error = e.message ?: "No se pudieron cargar los intereses."
        } finally {
            cargando = false
        }
    }

    fun omitirPorAhora() {
        if (omitiendo || guardando) return
        omitiendo = true
        aviso = null
        scope.launch {
            try {
                InteresesApi.omitir(token)
                contextoOmitir(context).edit().remove("pendiente_${repository.currentSession()?.correo.orEmpty()}").apply()
                onTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!sesionExpirada(e)) {
                    if (e is ErrorIntereses && (e.estado == 0 || e.estado == 404 || e.estado >= 500)) {
                        contextoOmitir(context).edit().putBoolean("pendiente_${repository.currentSession()?.correo.orEmpty()}", true).apply()
                        onTerminar()
                    } else {
                        aviso = e.message ?: "No se pudo omitir. Inténtalo nuevamente."
                        avisoEsError = true
                    }
                }
            } finally {
                omitiendo = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(VynkBackground)) {
        CabeceraColor(
            titulo = "Mis intereses",
            subtitulo = if (primerIngreso) "Elige lo que te gusta" else "Así armamos tus recomendaciones",
            icono = Icons.Filled.Favorite,
            color = ColorSeccion.ASISTENTE,
            acciones = {
                if (!primerIngreso) {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.Close, contentDescription = "Volver al perfil", tint = Color.White)
                    }
                } else {
                    TextButton(onClick = ::omitirPorAhora, enabled = !guardando && !omitiendo) {
                        Text(if (omitiendo) "Omitiendo…" else "Omitir", color = Color.White,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        )
        if (primerIngreso && avisoEsError && aviso != null) {
            Text(aviso.orEmpty(), color = Color(0xFFC0322B), modifier = Modifier.padding(12.dp))
        }

        when {
            cargando -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Azul)
            }

            error != null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline,
                titulo = "No pudimos cargar los intereses",
                texto = error.orEmpty(),
                esError = true
            ) {
                OutlinedButton(onClick = { recarga++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Reintentar")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text(textoAyuda(seleccion.size), style = MaterialTheme.typography.bodyMedium, color = Gris) }
                    // Cuadrícula de dos columnas: cada interés con su color.
                    items(catalogo.withIndex().toList().chunked(2), key = { fila -> fila.first().value.id }) { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { (indice, interes) ->
                                TarjetaInteres(
                                    interes = interes,
                                    color = colorInteres(indice),
                                    marcado = interes.id in seleccion,
                                    habilitado = !guardando && !omitiendo,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        aviso = null
                                        seleccion = alternar(seleccion, interes.id)
                                    }
                                )
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                BarraGuardar(
                    contador = textoContador(seleccion.size),
                    aviso = aviso,
                    avisoEsError = avisoEsError,
                    hayCambios = !mismosIntereses(seleccion, guardados),
                    puedeGuardar = !omitiendo && puedeGuardar(seleccion, guardados, guardando),
                    guardando = guardando,
                    onDescartar = {
                        aviso = null
                        seleccion = guardados
                    },
                    onGuardar = {
                        guardando = true
                        aviso = null
                        scope.launch {
                            try {
                                val resultado = InteresesApi.guardarConResultado(token, seleccion)
                                val quedaron = ordenarComoCatalogo(resultado.intereses, catalogo.map { it.id })
                                guardados = quedaron
                                seleccion = quedaron
                                if (resultado.recomendacionesActualizadas) {
                                    // Lo que el teléfono tenía de recomendaciones e inicio ya no corresponde: se olvida y
                                    // las pantallas lo piden de nuevo en cuanto se abren.
                                    MemoriaPantallas.olvidar()
                                    repository.currentSession()?.correo?.let { cuenta ->
                                        val guardadas = CacheRespuestas(context)
                                        guardadas.borrar(RecomendacionesAppApi.CLAVE, cuenta)
                                        guardadas.borrar(InicioApi.CLAVE, cuenta)
                                    }
                                    CambiosAgenda.avisar()
                                }
                                aviso = if (resultado.recomendacionesActualizadas)
                                    "Tus intereses se guardaron. Actualizamos tus recomendaciones."
                                else "Tus intereses se guardaron."
                                avisoEsError = false
                                if (primerIngreso) onTerminar()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                if (!sesionExpirada(e)) {
                                    aviso = e.message ?: "No se pudo guardar. Inténtalo nuevamente."
                                    avisoEsError = true
                                }
                            } finally {
                                guardando = false
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Colores para los intereses, en el orden del catálogo (se repiten si hay más). */
private val ColoresIntereses = listOf(
    Color(0xFF7C3AED), Color(0xFF1557D0), Color(0xFF0891B2), Color(0xFF0F9D58),
    Color(0xFFE07B00), Color(0xFFDB2777), Color(0xFFDC2626), Color(0xFF4F46E5)
)

private fun colorInteres(indice: Int): Color = ColoresIntereses[indice % ColoresIntereses.size]

/** Cuadro de un interés: marcado se pinta entero con su color; sin marcar, blanco con el ícono en su color. */
@Composable
private fun TarjetaInteres(
    interes: Interes,
    color: Color,
    marcado: Boolean,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(18.dp)
    Box(
        modifier
            .height(112.dp)
            .clip(forma)
            .background(if (marcado) Brush.linearGradient(listOf(color, oscurecer(color, 0.7f)))
                else Brush.linearGradient(listOf(Color.White, Color.White)))
            .border(if (marcado) 0.dp else 1.dp, if (marcado) Color.Transparent else Borde, forma)
            .clickable(enabled = habilitado, onClick = onClick)
            .semantics {
                role = Role.Checkbox
                selected = marcado
                contentDescription = interes.nombre
            }
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (marcado) Color.White.copy(alpha = 0.22f) else color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconoDeInteres(interes.id).dibujo(), contentDescription = null,
                    tint = if (marcado) Color.White else color, modifier = Modifier.size(24.dp))
            }
            Text(interes.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = if (marcado) Color.White else Marino, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(24.dp).clip(CircleShape)
                .background(if (marcado) Color.White else Color.Transparent)
                .border(if (marcado) 0.dp else 1.5.dp, if (marcado) Color.Transparent else Borde, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (marcado) Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BarraGuardar(
    contador: String,
    aviso: String?,
    avisoEsError: Boolean,
    hayCambios: Boolean,
    puedeGuardar: Boolean,
    guardando: Boolean,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(contador, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Marino)
            if (aviso != null) {
                Text(
                    aviso,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (avisoEsError) Color(0xFFC0322B) else Color(0xFF12805C)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDescartar, enabled = hayCambios && !guardando, modifier = Modifier.weight(1f)) {
                    Text("Descartar")
                }
                Button(onClick = onGuardar, enabled = puedeGuardar, modifier = Modifier.weight(1f)) {
                    Text(if (guardando) "Guardando…" else "Guardar")
                }
            }
        }
    }
}

/** Registro por cuenta para continuar sin red y sincronizar la omisión más adelante. */
fun contextoOmitir(context: android.content.Context): android.content.SharedPreferences =
    context.getSharedPreferences("vynk_intereses_omision", android.content.Context.MODE_PRIVATE)
