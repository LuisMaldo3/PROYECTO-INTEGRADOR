package com.tecsup.tcweek.ui.main

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
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.ErrorIntereses
import com.tecsup.tcweek.data.Interes
import com.tecsup.tcweek.data.InteresesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Azul = Color(0xFF0B63FF)
private val Marino = Color(0xFF0D1B3E)
private val Gris = Color(0xFF6B7A99)
private val Borde = Color(0xFFD9E1EC)

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
                onTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!sesionExpirada(e)) {
                    aviso = e.message ?: "No se pudo omitir. Inténtalo nuevamente."
                    avisoEsError = true
                }
            } finally {
                omitiendo = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!primerIngreso) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al perfil", tint = Marino)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Mis intereses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Marino)
                Text(
                    if (primerIngreso) "Personaliza tu experiencia o elige más tarde desde Perfil."
                    else "Te recomendaremos actividades según lo que elijas.",
                    style = MaterialTheme.typography.bodySmall, color = Gris
                )
            }
            if (primerIngreso) {
                TextButton(onClick = ::omitirPorAhora, enabled = !guardando && !omitiendo) {
                    Text(if (omitiendo) "Omitiendo…" else "Omitir por ahora")
                }
            }
        }
        HorizontalDivider(color = Borde)
        if (primerIngreso && avisoEsError && aviso != null) {
            Text(aviso.orEmpty(), color = Color(0xFFA12525), modifier = Modifier.padding(12.dp))
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
                    items(catalogo, key = { it.id }) { interes ->
                        TarjetaInteres(
                            interes = interes,
                            marcado = interes.id in seleccion,
                            habilitado = !guardando && !omitiendo,
                            onClick = {
                                aviso = null
                                seleccion = alternar(seleccion, interes.id)
                            }
                        )
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
                                val quedaron = ordenarComoCatalogo(InteresesApi.guardar(token, seleccion), catalogo.map { it.id })
                                guardados = quedaron
                                seleccion = quedaron
                                aviso = "Tus intereses se guardaron."
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

@Composable
private fun TarjetaInteres(interes: Interes, marcado: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        color = if (marcado) Color(0xFFF0F6FF) else Color.White,
        border = BorderStroke(if (marcado) 1.5.dp else 1.dp, if (marcado) Azul else Borde),
        modifier = Modifier.fillMaxWidth().semantics {
            role = Role.Checkbox
            selected = marcado
        }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (marcado) Azul else Color(0xFFEEF3FA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconoDeInteres(interes.id).dibujo(),
                    contentDescription = null,
                    tint = if (marcado) Color.White else Azul
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(interes.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Marino)
                if (interes.descripcion.isNotBlank()) {
                    Text(interes.descripcion, style = MaterialTheme.typography.bodySmall, color = Gris)
                }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (marcado) Azul else Color.White)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                if (marcado) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
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
                    color = if (avisoEsError) Color(0xFFA12525) else Color(0xFF14673B)
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
