package com.tecsup.tcweek.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tecsup.tcweek.data.repository.CacheRespuestas
import com.tecsup.tcweek.domain.CambiosAgenda
import com.tecsup.tcweek.data.repository.CargaProgramada
import com.tecsup.tcweek.data.repository.Marca
import com.tecsup.tcweek.data.repository.SesionGuardada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Estado de una pantalla con carga programada (US-76 T3 y US-77 T3). Mientras se actualiza se sigue viendo lo
 * anterior; si falla la conexión se muestra lo guardado con el aviso [sinConexion].
 */
class CargaProgramadaEstado<T>(inicial: T?, marcaInicial: Marca?, actualizado: Long) {
    var datos by mutableStateOf(inicial)
    var marca by mutableStateOf(marcaInicial)
    var actualizadoEn by mutableLongStateOf(actualizado)
    var cargando by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var sinConexion by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)
    internal var pedido by mutableIntStateOf(0)
    internal var pedidoAtendido = 0

    /** Recarga pedida por el estudiante. */
    fun actualizar() { pedido++ }

    /** Cambio local inmediato (por ejemplo quitar una tarjeta) sin esperar al servidor. */
    fun modificar(cambio: (T) -> T) { datos?.let { datos = cambio(it) } }

    val primeraCarga get() = datos == null && error == null
}

/**
 * @param clave      nombre con que se guarda la respuesta en el teléfono
 * @param memoria    lo cargado antes en esta sesión de la app (vuelve a verse al instante)
 * @param recordar   guarda en memoria lo recién cargado
 * @param desdeJson  convierte lo guardado en el teléfono
 * @param vence      hasta cuándo sirven los datos recién llegados
 * @param cargar     pide los datos al servidor (datos y JSON tal cual para guardarlo)
 */
@Composable
fun <T : Any> rememberCargaProgramada(
    clave: String,
    memoria: () -> Pair<T, Marca>?,
    recordar: (T, Marca) -> Unit,
    desdeJson: (String, Long) -> T,
    vence: (T, Long) -> Long,
    cargar: suspend (String) -> Pair<T, String>
): CargaProgramadaEstado<T> {
    val contexto = LocalContext.current
    val sesion = remember { SesionGuardada(contexto).leer() }
    val guardadas = remember { CacheRespuestas(contexto) }
    val estado = remember {
        val enMemoria = memoria()
        if (enMemoria != null) CargaProgramadaEstado(enMemoria.first, enMemoria.second, enMemoria.second.cargadoEn)
        else {
            // Lo guardado en el teléfono se muestra ya, pero se considera vencido: se actualiza al abrir.
            val guardado = sesion?.let { guardadas.leer(clave, it.correo) }
            val datos = guardado?.let { (json, en) -> runCatching { desdeJson(json, en) }.getOrNull() }
            CargaProgramadaEstado(datos, null, guardado?.second ?: 0L)
        }
    }
    val version by CambiosAgenda.version.collectAsState()

    LaunchedEffect(version, estado.pedido) {
        val cuenta = sesion
        if (cuenta == null) { estado.error = "Inicia sesión para continuar."; return@LaunchedEffect }
        val token = cuenta.llave
        estado.token = token
        var forzar = estado.pedido != estado.pedidoAtendido
        estado.pedidoAtendido = estado.pedido
        while (true) {
            val ahora = System.currentTimeMillis()
            if (forzar || CargaProgramada.debeRecargar(estado.marca, ahora, version)) {
                forzar = false
                estado.cargando = true
                try {
                    val (datos, json) = cargar(token)
                    val marca = Marca(ahora, maxOf(vence(datos, ahora), ahora + 30_000L), version)
                    estado.datos = datos
                    estado.marca = marca
                    estado.actualizadoEn = ahora
                    estado.error = null
                    estado.sinConexion = false
                    recordar(datos, marca)
                    guardadas.guardar(clave, cuenta.correo, json, ahora)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Con datos a la vista no se borran: se avisa y se reintenta en un minuto.
                    estado.error = e.message ?: "No se pudo actualizar."
                    estado.sinConexion = estado.datos != null
                    estado.marca = Marca(estado.marca?.cargadoEn ?: 0L, ahora + 60_000L, version)
                } finally {
                    estado.cargando = false
                }
            }
            delay(CargaProgramada.esperaHastaVencer(estado.marca!!, System.currentTimeMillis()))
        }
    }
    return estado
}

/** Reloj que avanza cada 30 s, para textos como «actualizado hace 3 min». */
@Composable
fun relojMinutos(): Long {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            ahora = System.currentTimeMillis()
        }
    }
    return ahora
}
