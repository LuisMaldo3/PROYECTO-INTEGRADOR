"""Pantalla «Inteligencia artificial» del panel: estado y prueba real de Gemini, sin mostrar nunca la clave."""
from unittest import mock

from django.test import RequestFactory, SimpleTestCase

from . import views
from .gemini_config import GeminiError, MENSAJES, diagnostico

CLAVE = "AIza-clave-secreta-123"
OK = lambda modelo: (lambda cuerpo, clave, m, timeout=None: {"modeloUsado": modelo, "candidates": [{"content": {"parts": [{"text": "VYNK conectado."}]}}]})


class EstadoIATests(SimpleTestCase):
    def estado(self, clave=CLAVE, en_spring=False, **kw):
        with mock.patch("actividades.gemini_config.credenciales_gemini", return_value=(clave, "gemini-2.5-flash")), \
                mock.patch("actividades.gemini_config._configuracion_usuario", return_value={"GEMINI_API_KEY": CLAVE} if en_spring else {}), \
                mock.patch.dict("os.environ", {"GEMINI_API_KEY": clave if clave and not en_spring else ""}):
            return diagnostico(**kw)

    def test_sin_clave_dice_donde_ponerla_y_no_llama_a_google(self):
        llamar = mock.Mock()
        e = self.estado(clave="", probar=True, llamar=llamar)
        self.assertEqual((e["configurada"], e["probada"], e["mensaje"]), (False, False, MENSAJES["sin_clave"]))
        llamar.assert_not_called()

    def test_sin_probar_solo_informa_la_configuracion(self):
        e = self.estado()
        self.assertEqual((e["configurada"], e["origen"], e["modelo"], e["probada"]), (True, "Admin-back/.env (o variable de entorno)", "gemini-2.5-flash", False))
        self.assertEqual(self.estado(en_spring=True)["origen"], "Usuario-back/.env")

    def test_prueba_real_correcta(self):
        e = self.estado(probar=True, llamar=OK("gemini-2.5-flash"))
        self.assertEqual((e["ok"], e["modeloUsado"], e["respuesta"]), (True, "gemini-2.5-flash", "VYNK conectado."))

    def test_si_respondio_el_modelo_de_respaldo_avisa_que_conviene_cambiarlo(self):
        e = self.estado(probar=True, llamar=OK("gemini-3.8-flash"))
        self.assertTrue(e["ok"])
        self.assertIn("respaldo gemini-3.8-flash", e["mensaje"])

    def test_si_falla_dice_la_causa(self):
        def falla(*a, **k):
            raise GeminiError("clave_invalida")
        e = self.estado(probar=True, llamar=falla)
        self.assertEqual((e["ok"], e["tipo"], e["mensaje"]), (False, "clave_invalida", MENSAJES["clave_invalida"]))

    def test_la_pantalla_muestra_lo_que_dijo_google(self):
        def falla(*a, **k):
            raise GeminiError("clave_restringida", detalle="Requests from this Android client application <empty> are blocked.")
        e = self.estado(probar=True, llamar=falla)
        self.assertEqual((e["tipo"], e["detalleGoogle"]), ("clave_restringida", "Requests from this Android client application <empty> are blocked."))

    def test_la_clave_nunca_sale(self):
        for kw in ({}, {"probar": True, "llamar": OK("gemini-2.5-flash")}):
            self.assertNotIn(CLAVE, str(self.estado(**kw)))

    def test_la_ruta_exige_administrador(self):
        self.assertEqual(views.estado_ia(RequestFactory().get("/x/")).status_code, 401)
