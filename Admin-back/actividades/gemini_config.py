"""Configuración local segura de Gemini, sin duplicar ni publicar claves.

Django lee primero su variable de entorno. Si falta, en desarrollo local puede
leer el .env del backend de usuario, siempre que sea una carpeta hermana.
No modifica .env, no incluye claves en respuestas HTTP, logs ni archivos ZIP.
"""
import os
import re
from pathlib import Path
from dotenv import dotenv_values

_RAIZ = Path(__file__).resolve().parents[2]
_MODELO_POR_DEFECTO = 'gemini-2.5-flash'


def _configuracion_usuario():
    ruta = _RAIZ / 'Usuario-back' / '.env'
    if not ruta.is_file():
        return {}
    try:
        return dotenv_values(ruta)
    except (OSError, UnicodeError):
        return {}


def credenciales_gemini():
    """Devuelve (clave, modelo); la clave NUNCA sale del proceso Python."""
    ajustes = _configuracion_usuario()
    clave = (os.environ.get('GEMINI_API_KEY') or ajustes.get('GEMINI_API_KEY') or '').strip()
    modelo = (os.environ.get('GEMINI_MODEL') or ajustes.get('GEMINI_MODEL') or _MODELO_POR_DEFECTO).strip()
    if not re.fullmatch(r'[a-zA-Z0-9._-]{3,80}', modelo):
        modelo = _MODELO_POR_DEFECTO
    return clave, modelo


# ---------------------------------------------------------------------------
# Llamada a Gemini con modelo de respaldo y errores que explican la causa
# ---------------------------------------------------------------------------
import json  # noqa: E402
from urllib import error as _error, request as _request  # noqa: E402

# Si el modelo configurado ya no existe en Google (404), se prueba el siguiente. Google anunció que
# gemini-2.5-flash pasa a responder con gemini-3.8-flash desde el 1 de diciembre de 2026.
MODELOS_DE_RESPALDO = ('gemini-3.8-flash', 'gemini-2.5-flash')

MENSAJES = {
    'sin_clave': 'Falta la clave de la IA. Agrega GEMINI_API_KEY en Admin-back/.env (o en Usuario-back/.env) y reinicia el servidor.',
    'clave_invalida': 'Google rechazó la clave de la IA (GEMINI_API_KEY). Revisa que esté completa, activa y con permiso para Gemini.',
    'cuota': 'Se alcanzó el límite de uso gratuito de la IA. Espera unos minutos o revisa tu cuota en Google AI Studio.',
    'modelo_no_disponible': 'El modelo de IA configurado ya no existe en Google y ninguno de respaldo respondió. Cambia GEMINI_MODEL por uno vigente.',
    'sin_conexion': 'No se pudo conectar con Google. Revisa la conexión a internet del servidor.',
    'error': 'Google no pudo responder ahora. Inténtalo de nuevo en unos minutos.',
}


class GeminiError(Exception):
    """Falla de la IA con un tipo (`sin_clave`, `clave_invalida`, `clave_vencida`, `api_desactivada`, `clave_restringida`,
    `sin_permiso`, `region`, `cuota`, `modelo_no_disponible`, `sin_conexion`, `error`), un mensaje en español listo para
    mostrar y, si Google explicó la causa, `detalle` con su texto (sin la clave)."""

    def __init__(self, tipo, mensaje=None, detalle=None):
        super().__init__(mensaje or MENSAJES.get(tipo, MENSAJES['error']))
        self.tipo = tipo
        self.detalle = detalle


# Qué hacer en cada caso de clave rechazada (Google lo indica en su respuesta).
MENSAJES.update({
    'clave_vencida': 'La clave de Gemini venció. Crea una nueva en Google AI Studio (aistudio.google.com → Get API key) y reemplázala en el .env.',
    'api_desactivada': 'La API de Gemini («Generative Language API») está desactivada en el proyecto de Google de esa clave. Actívala en Google Cloud Console o crea la clave desde Google AI Studio, que la activa sola.',
    'clave_restringida': 'La clave tiene restricciones que no permiten usarla desde el servidor (por ejemplo, solo para una app Android o un sitio web) o no incluye la API de Gemini. Usa una clave creada en Google AI Studio, sin restricción de aplicación. Ojo: la clave de Firebase de la app (google-services.json) no sirve para esto.',
    'sin_permiso': 'Google no le da permiso a esta clave para usar Gemini. Revisa en Google AI Studio que la clave esté activa y en un proyecto con la API de Gemini habilitada.',
    'region': 'Google no ofrece la API de Gemini en la región desde donde sale la conexión del servidor.',
})

_CLAVE_EN_TEXTO = re.compile(r'AIza[0-9A-Za-z_\-]{10,}|key=[^&\s"]+')


def _detalle_google(cuerpo):
    """(status, motivo, mensaje) de la respuesta de error de Google, sin rastro de la clave."""
    try:
        error_g = json.loads(cuerpo or '{}').get('error') or {}
    except ValueError:
        error_g = {}
    motivos = [d.get('reason') for d in error_g.get('details') or [] if isinstance(d, dict) and d.get('reason')]
    mensaje = _CLAVE_EN_TEXTO.sub('[clave]', str(error_g.get('message') or ''))[:300]
    return str(error_g.get('status') or ''), (motivos[0] if motivos else ''), mensaje


def _clasificar(codigo, cuerpo):
    status, motivo, mensaje = _detalle_google(cuerpo)
    texto = f'{status} {motivo} {mensaje} {cuerpo or ""}'.lower()
    if codigo == 404:
        return 'modelo_no_disponible'
    if codigo == 429 or 'resource_exhausted' in texto:
        return 'cuota'
    if 'expired' in texto:
        return 'clave_vencida'
    if 'api_key_invalid' in texto or 'api key not valid' in texto:
        return 'clave_invalida'
    if 'service_disabled' in texto or 'has not been used' in texto or ' is disabled' in texto:
        return 'api_desactivada'
    if 'blocked' in texto or 'referer' in texto or 'referrer' in texto or 'android' in texto or 'ios client' in texto or 'ip address' in texto:
        return 'clave_restringida'
    if 'location is not supported' in texto or 'user location' in texto:
        return 'region'
    if codigo in (401, 403) or 'permission_denied' in texto:
        return 'sin_permiso' if codigo == 403 or 'permission_denied' in texto else 'clave_invalida'
    return 'error'


def llamar_gemini(cuerpo, clave, modelo, timeout=10, abrir=None):
    """Envía `cuerpo` (dict) a Gemini y devuelve la respuesta (dict). Si el modelo no existe prueba los de respaldo.
    Lanza GeminiError con la causa real; la clave nunca sale en los mensajes."""
    if not clave:
        raise GeminiError('sin_clave')
    abrir = abrir or _request.urlopen
    modelos = [modelo] + [m for m in MODELOS_DE_RESPALDO if m != modelo]
    datos = json.dumps(cuerpo).encode('utf-8')
    for nombre in modelos:
        peticion = _request.Request(f'https://generativelanguage.googleapis.com/v1beta/models/{nombre}:generateContent', data=datos,
                                    method='POST', headers={'Content-Type': 'application/json', 'x-goog-api-key': clave})
        try:
            with abrir(peticion, timeout=timeout) as respuesta:
                datos_respuesta = json.loads(respuesta.read(250_000).decode('utf-8'))
                if isinstance(datos_respuesta, dict):
                    datos_respuesta['modeloUsado'] = nombre
                return datos_respuesta
        except _error.HTTPError as e:
            try:
                detalle = e.read(2000).decode('utf-8', 'replace')
            except Exception:  # noqa: BLE001
                detalle = ''
            tipo = _clasificar(e.code, detalle)
            if tipo == 'modelo_no_disponible':
                continue  # prueba el siguiente modelo
            raise GeminiError(tipo, detalle=_detalle_google(detalle)[2] or None) from None
        except (_error.URLError, TimeoutError, OSError):
            raise GeminiError('sin_conexion') from None
        except (ValueError, UnicodeError):
            raise GeminiError('error', 'Google respondió algo que no se pudo leer.') from None
    raise GeminiError('modelo_no_disponible')


def diagnostico(probar=False, llamar=None):
    """Estado de la IA para el panel: si hay clave, de dónde sale, qué modelo usa y, si se pide, una prueba real con Google.
    Nunca devuelve la clave."""
    clave, modelo = credenciales_gemini()
    en_entorno = bool((os.environ.get('GEMINI_API_KEY') or '').strip())
    en_spring = bool((_configuracion_usuario().get('GEMINI_API_KEY') or '').strip())
    estado = {
        'configurada': bool(clave), 'modelo': modelo, 'respaldo': list(MODELOS_DE_RESPALDO),
        'origen': 'Admin-back/.env (o variable de entorno)' if en_entorno else 'Usuario-back/.env' if en_spring else None,
        'claveEnSpring': en_spring,
        'probada': False, 'ok': None, 'tipo': None, 'mensaje': None if clave else MENSAJES['sin_clave'], 'detalleGoogle': None, 'modeloUsado': None, 'respuesta': None,
    }
    if not probar or not clave:
        return estado
    estado['probada'] = True
    try:
        r = (llamar or llamar_gemini)({'contents': [{'parts': [{'text': 'Responde solo con la frase: VYNK conectado.'}]}],
                                       'generationConfig': {'temperature': 0, 'maxOutputTokens': 20}}, clave, modelo, timeout=15)
    except GeminiError as e:
        estado.update(ok=False, tipo=e.tipo, mensaje=str(e), detalleGoogle=getattr(e, 'detalle', None))
        return estado
    texto = ' '.join(p.get('text', '') for c in r.get('candidates', []) for p in c.get('content', {}).get('parts', []) if isinstance(p, dict)).strip()
    estado.update(ok=True, modeloUsado=r.get('modeloUsado') or modelo, respuesta=texto[:120],
                  mensaje='La IA respondió correctamente.' if (r.get('modeloUsado') or modelo) == modelo else
                  f'La IA respondió con el modelo de respaldo {r.get("modeloUsado")}: el configurado ({modelo}) ya no existe en Google. Conviene cambiar GEMINI_MODEL.')
    return estado
