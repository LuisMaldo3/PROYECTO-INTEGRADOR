"""Verifica solo la resolución local de configuración, sin efectuar llamadas remotas."""
import unittest
from unittest.mock import patch
from . import gemini_config


class GeminiConfigTests(unittest.TestCase):
    @patch('actividades.gemini_config._configuracion_usuario', return_value={'GEMINI_API_KEY': 'clave-privada-de-prueba', 'GEMINI_MODEL': 'gemini-2.5-flash'})
    def test_reutiliza_env_del_usuario_si_admin_no_tiene_clave(self, _):
        with patch.dict('os.environ', {}, clear=True):
            clave, modelo = gemini_config.credenciales_gemini()
        self.assertEqual(clave, 'clave-privada-de-prueba')
        self.assertEqual(modelo, 'gemini-2.5-flash')

    @patch('actividades.gemini_config._configuracion_usuario', return_value={'GEMINI_API_KEY': 'clave-de-usuario'})
    def test_variable_servidor_tiene_prioridad(self, _):
        with patch.dict('os.environ', {'GEMINI_API_KEY': 'clave-de-admin', 'GEMINI_MODEL': 'gemini-2.5-flash'}):
            clave, _ = gemini_config.credenciales_gemini()
        self.assertEqual(clave, 'clave-de-admin')

    @patch('actividades.gemini_config._configuracion_usuario', return_value={'GEMINI_MODEL': '../../error'})
    def test_modelo_invalido_se_reemplaza(self, _):
        with patch.dict('os.environ', {}, clear=True):
            _, modelo = gemini_config.credenciales_gemini()
        self.assertEqual(modelo, 'gemini-2.5-flash')
