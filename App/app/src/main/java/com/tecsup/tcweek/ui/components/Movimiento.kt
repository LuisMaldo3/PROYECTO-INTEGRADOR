package com.tecsup.tcweek.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tecsup.tcweek.ui.theme.VynkBlue
import com.tecsup.tcweek.ui.theme.VynkNavy
import com.tecsup.tcweek.ui.theme.VynkNavy2
import com.tecsup.tcweek.ui.theme.VynkSky
import kotlinx.coroutines.delay

/*
 * Sistema de movimiento de VYNK: las mismas animaciones en todas las pantallas para que la app se sienta una sola.
 * Duraciones cortas (≤ 450 ms) y curvas suaves: el movimiento acompaña, nunca hace esperar.
 */

/** Degradado de marca para cabeceras y tarjetas destacadas (marino → marino claro → toque celeste). */
val DegradadoVynk: Brush = Brush.linearGradient(
    0f to VynkNavy, 0.65f to VynkNavy2, 1f to Color(0xFF1B4E9B),
    start = Offset(0f, 0f), end = Offset(1200f, 900f)
)

/**
 * Entrada escalonada: el elemento aparece subiendo y desvaneciéndose, con un pequeño retraso según su posición en la
 * lista. Solo ocurre la primera vez que se muestra (no se repite al volver a la pantalla).
 */
fun Modifier.aparecer(indice: Int = 0, desplazamiento: Dp = 20.dp): Modifier = composed {
    var visto by rememberSaveable { mutableStateOf(false) }
    val progreso = remember { Animatable(if (visto) 1f else 0f) }
    val px = with(LocalDensity.current) { desplazamiento.toPx() }
    LaunchedEffect(Unit) {
        if (!visto) {
            delay((indice.coerceAtMost(8) * 55).toLong())
            progreso.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            visto = true
        }
    }
    graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * px
    }
}

/** Tarjeta que «se hunde» un poco al tocarla, con efecto de onda: confirma el toque sin ser brusca. */
fun Modifier.presionable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (presionado && enabled) 0.965f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "presion"
    )
    graphicsLayer { scaleX = escala; scaleY = escala }
        .clickable(interaccion, LocalIndication.current, enabled = enabled, onClick = onClick)
}

/** Sombra suave y azulada (no gris): da profundidad sin ensuciar el diseño. */
fun Modifier.sombraSuave(forma: Shape = RoundedCornerShape(20.dp), elevacion: Dp = 10.dp): Modifier =
    shadow(elevacion, forma, clip = false, ambientColor = VynkNavy.copy(alpha = 0.10f), spotColor = VynkNavy.copy(alpha = 0.16f))

/** Bloque de carga con brillo que recorre la forma: anticipa el contenido en lugar de un círculo girando. */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(12.dp)) {
    val transicion = rememberInfiniteTransition(label = "esqueleto")
    val x by transicion.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "brillo"
    )
    Box(
        modifier.clip(forma).background(
            Brush.linearGradient(
                listOf(Color(0xFFE8EDF5), Color(0xFFF6F8FC), Color(0xFFE8EDF5)),
                start = Offset(x, 0f), end = Offset(x + 400f, 200f)
            )
        )
    )
}

/** Tarjetas de carga con la forma de una tarjeta de actividad. */
@Composable
fun EsqueletoTarjetas(cantidad: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(cantidad) {
            Column(
                Modifier.fillMaxWidth().sombraSuave(elevacion = 4.dp).clip(RoundedCornerShape(20.dp))
                    .background(Color.White).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Esqueleto(Modifier.fillMaxWidth().height(120.dp), RoundedCornerShape(14.dp))
                Esqueleto(Modifier.fillMaxWidth(0.7f).height(16.dp))
                Esqueleto(Modifier.fillMaxWidth(0.45f).height(12.dp))
            }
        }
    }
}

/** Tres puntos que saltan: el asistente está escribiendo. */
@Composable
fun PuntosEscribiendo(color: Color = VynkBlue, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "escribiendo")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val salto by transicion.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(900, delayMillis = i * 150, easing = FastOutSlowInEasing), RepeatMode.Restart
                ), label = "punto$i"
            )
            // Sube y baja en la primera mitad del ciclo; descansa en la segunda.
            val y = if (salto < 0.5f) -kotlin.math.sin(salto * 2 * Math.PI).toFloat() * 5f else 0f
            Box(
                Modifier.size(7.dp).graphicsLayer { translationY = y * density; alpha = 0.45f + (if (salto < 0.5f) 0.55f else 0f) }
                    .background(color, CircleShape)
            )
        }
    }
}

/** Punto «en línea» que late suavemente. */
@Composable
fun PuntoVivo(color: Color = Color(0xFF34D399), modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "latido")
    val halo by transicion.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing)), label = "halo"
    )
    Box(modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(14.dp).graphicsLayer { scaleX = 0.5f + halo; scaleY = 0.5f + halo; alpha = 1f - halo }
            .background(color.copy(alpha = 0.5f), CircleShape))
        Box(Modifier.size(8.dp).background(color, CircleShape))
    }
}

/**
 * Fondo de marca vivo: el degradado con dos destellos celestes que flotan muy despacio. Se usa detrás de las
 * cabeceras; el movimiento es tan lento que no distrae.
 */
@Composable
fun FondoVivo(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "fondo")
    val t by transicion.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "deriva"
    )
    // Recortado: los destellos no deben salirse de la cabecera.
    Canvas(modifier.clipToBounds()) {
        drawRect(DegradadoVynk)
        drawCircle(
            Brush.radialGradient(listOf(VynkSky.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width * (0.85f - 0.1f * t), size.height * (0.15f + 0.1f * t)), radius = size.width * 0.55f),
            radius = size.width * 0.55f, center = Offset(size.width * (0.85f - 0.1f * t), size.height * (0.15f + 0.1f * t))
        )
        drawCircle(
            Brush.radialGradient(listOf(VynkBlue.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * (0.1f + 0.12f * t), size.height * 0.95f), radius = size.width * 0.5f),
            radius = size.width * 0.5f, center = Offset(size.width * (0.1f + 0.12f * t), size.height * 0.95f)
        )
    }
}

/** Ícono dentro de un círculo con degradado del color dado: para accesos rápidos y cabeceras de tarjeta. */
@Composable
fun IconoDegradado(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp
) {
    Box(
        modifier.size(tamano).clip(RoundedCornerShape(tamano * 0.32f))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.72f)))),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(tamano * 0.5f))
    }
}
