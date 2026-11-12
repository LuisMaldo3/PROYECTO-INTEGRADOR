"""La IA (Gemini): modelo de respaldo, causa real de cada falla y la clave nunca se muestra."""
import io
import json
from unittest import mock
from urllib import error

from django.test import SimpleTestCase

from . import demanda
from .gemini_config import GeminiError, MENSAJES, llamar_gemini

CLAVE = "clave-secreta-xyz-123"
CUERPO = {"contents": [{"parts": [{"text": "hola"}]}]}
OK = {"candidates": [{"content": {"parts": [{"text": "Respuesta de prueba"}]}}]}


class _Respuesta:
    def __init__(self, datos):
        self.datos = datos

    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False

    def read(self, n=-1):
        return json.dumps(self.datos).encode()


def _http(codigo, texto=""):
    return error.HTTPError("https://x", codigo, "x", {}, io.BytesIO(texto.encode()))


def _guion(*pasos):
    """Un `abrir` falso: cada llamada consume un paso (respuesta o excepción) y guarda la URL."""
    llamadas = []
    pasos = list(pasos)

    def abrir(peticion, timeout=None):
        llamadas.append(peticion.full_url)
        paso = pasos.pop(0)
        if isinstance(paso, BaseException):
            raise paso
        return _Respuesta(paso)
    abrir.llamadas = llamadas
    return abrir


class LlamarGeminiTests(SimpleTestCase):
    def test_responde_con_el_modelo_configurado(self):
        abrir = _guion(OK)
        self.assertEqual(llamar_gemini(CUERPO, CLAVE, "gemini-2.5-flash", abrir=abrir), {**OK, "modeloUsado": "gemini-2.5-flash"})
        self.assertEqual(len(abrir.llamadas), 1)
        self.assertIn("models/gemini-2.5-flash:", abrir.llamadas[0])

    def test_si_el_modelo_ya_no_existe_prueba_el_de_respaldo(self):
        abrir = _guion(_http(404, "models/gemini-2.5-flash is not found"), OK)
        self.assertEqual(llamar_gemini(CUERPO, CLAVE, "gemini-2.5-flash", abrir=abrir), {**OK, "modeloUsado": "gemini-3.8-flash"})
        self.assertIn("gemini-2.5-flash:", abrir.llamadas[0])
        self.assertIn("gemini-3.8-flash:", abrir.llamadas[1])

    def test_si_ningun_modelo_existe_lo_dice(self):
        abrir = _guion(_http(404), _http(404), _http(404))
        with self.assertRaises(GeminiError) as e:
            llamar_gemini(CUERPO, CLAVE, "gemini-viejo", abrir=abrir)
        self.assertEqual((e.exception.tipo, str(e.exception)), ("modelo_no_disponible", MENSAJES["modelo_no_disponible"]))
        self.assertEqual(len(abrir.llamadas), 3, "se probaron el configurado y los dos de respaldo")

    def test_cada_falla_explica_su_causa_y_no_reintenta_con_otro_modelo(self):
        casos = [(_http(400, '{"error":{"status":"INVALID_ARGUMENT","message":"API key not valid"}}'), "clave_invalida"), (_http(401), "clave_invalida"),
                 (_http(403, "PERMISSION_DENIED"), "sin_permiso"), (_http(429, "RESOURCE_EXHAUSTED"), "cuota"), (_http(500), "error"),
                 (error.URLError("sin red"), "sin_conexion"), (TimeoutError(), "sin_conexion")]
        for fallo, tipo in casos:
            abrir = _guion(fallo, OK)
            with self.assertRaises(GeminiError, msg=tipo) as e:
                llamar_gemini(CUERPO, CLAVE, "gemini-2.5-flash", abrir=abrir)
            self.assertEqual(e.exception.tipo, tipo)
            self.assertEqual(str(e.exception), MENSAJES[tipo])
            self.assertEqual(len(abrir.llamadas), 1, f"{tipo}: no tiene sentido probar otro modelo")

    def test_google_explica_por_que_rechaza_la_clave_y_se_traduce(self):
        def cuerpo(codigo, status, mensaje, motivo=None):
            return _http(codigo, json.dumps({"error": {"code": codigo, "status": status, "message": mensaje,
                                                        "details": [{"reason": motivo}] if motivo else []}}))
        casos = [(cuerpo(400, "INVALID_ARGUMENT", "API key expired. Please renew the API key.", "API_KEY_INVALID"), "clave_vencida", "venció"),
                 (cuerpo(403, "PERMISSION_DENIED", "Generative Language API has not been used in project 123 before or it is disabled.", "SERVICE_DISABLED"), "api_desactivada", "Generative Language API"),
                 (cuerpo(403, "PERMISSION_DENIED", "Requests from this Android client application <empty> are blocked.", "API_KEY_ANDROID_APP_BLOCKED"), "clave_restringida", "google-services.json"),
                 (cuerpo(400, "FAILED_PRECONDITION", "User location is not supported for the API use."), "region", "región"),
                 (cuerpo(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key.", "API_KEY_INVALID"), "clave_invalida", "GEMINI_API_KEY")]
        for fallo, tipo, frase in casos:
            with self.assertRaises(GeminiError, msg=tipo) as e:
                llamar_gemini(CUERPO, CLAVE, "gemini-2.5-flash", abrir=_guion(fallo))
            self.assertEqual(e.exception.tipo, tipo)
            self.assertIn(frase, str(e.exception))
            self.assertTrue(e.exception.detalle, "se conserva lo que dijo Google")

    def test_el_detalle_de_google_nunca_lleva_la_clave(self):
        fallo = _http(400, json.dumps({"error": {"message": "API key not valid: AIzaSyD-1234567890abcdefghijkl was rejected (key=AIzaSyD-1234567890abcdefghijkl)"}}))
        with self.assertRaises(GeminiError) as e:
            llamar_gemini(CUERPO, CLAVE, "m", abrir=_guion(fallo))
        self.assertNotIn("AIzaSyD-1234567890", e.exception.detalle)
        self.assertIn("[clave]", e.exception.detalle)

    def test_sin_clave_no_llama_a_google(self):
        abrir = _guion(OK)
        with self.assertRaises(GeminiError) as e:
            llamar_gemini(CUERPO, "", "gemini-2.5-flash", abrir=abrir)
        self.assertEqual((e.exception.tipo, abrir.llamadas), ("sin_clave", []))

    def test_la_clave_nunca_aparece_en_los_mensajes(self):
        for tipo in MENSAJES:
            self.assertNotIn(CLAVE, str(GeminiError(tipo)))
        with self.assertRaises(GeminiError) as e:
            llamar_gemini(CUERPO, CLAVE, "m", abrir=_guion(_http(403, f"key={CLAVE} rechazada")))
        self.assertNotIn(CLAVE, str(e.exception))

    def test_una_respuesta_ilegible_se_avisa(self):
        class Rota(_Respuesta):
            def read(self, n=-1):
                return b"<html>no es json</html>"
        with self.assertRaises(GeminiError) as e:
            llamar_gemini(CUERPO, CLAVE, "m", abrir=lambda p, timeout=None: Rota({}))
        self.assertEqual(e.exception.tipo, "error")


ACTIVIDAD = {"id": "a1", "nombre": "Taller de Arduino", "areaTecsup": "", "tipoActividad": "actividad", "estado": "programada", "fecha": "2026-11-20",
             "unidadCupo": "estudiantes", "capacidad": 10, "ocupados": 8, "porcentajeOcupacion": 80, "personasInscritas": 8, "equiposRegistrados": 0,
             "equiposCompletos": 0, "equiposIncompletos": 0, "cancelaciones": 0}


class PantallaDeDemandaTests(SimpleTestCase):
    """De punta a punta: lo que ve el administrador en «Analizar con IA» según lo que responda Google."""

    def analizar(self, *pasos):
        abrir = _guion(*pasos)
        with mock.patch("actividades.demanda.inscritos_por_actividad", return_value={"actividades": [ACTIVIDAD], "totales": {}}), \
                mock.patch("actividades.gemini_config.credenciales_gemini", return_value=(CLAVE, "gemini-2.5-flash")), \
                mock.patch("actividades.gemini_config._request.urlopen", side_effect=abrir):
            return demanda.obtener_demanda(con_ia=True)["ia"], abrir

    def test_si_google_responde_se_muestra_el_analisis(self):
        ia, _ = self.analizar(OK)
        self.assertEqual((ia["estado"], ia["texto"]), ("disponible", "Respuesta de prueba"))

    def test_con_el_modelo_retirado_funciona_igual_gracias_al_respaldo(self):
        ia, abrir = self.analizar(_http(404), OK)
        self.assertEqual(ia["estado"], "disponible")
        self.assertEqual(len(abrir.llamadas), 2)

    def test_con_la_clave_mala_el_mensaje_dice_la_causa(self):
        ia, _ = self.analizar(_http(401))
        self.assertEqual((ia["estado"], ia["mensaje"]), ("no_disponible", MENSAJES["clave_invalida"]))

    def test_sin_clave_dice_donde_ponerla(self):
        with mock.patch("actividades.demanda.inscritos_por_actividad", return_value={"actividades": [ACTIVIDAD], "totales": {}}), \
                mock.patch("actividades.gemini_config.credenciales_gemini", return_value=("", "gemini-2.5-flash")):
            ia = demanda.obtener_demanda(con_ia=True)["ia"]
        self.assertEqual((ia["estado"], ia["mensaje"]), ("sin_configurar", MENSAJES["sin_clave"]))
        self.assertIn(".env", ia["mensaje"])
