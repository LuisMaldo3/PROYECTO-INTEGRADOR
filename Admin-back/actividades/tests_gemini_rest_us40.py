"""Prueba el formato REST de Gemini sin usar ni revelar la clave real del usuario."""
import io
import json
import unittest
from unittest.mock import patch

from .reporte_evento import _solicitar_gemini, preparar_reporte


class GeminiRestTests(unittest.TestCase):
    def test_usa_header_autorizacion_y_modelo_y_interpreta_respuesta(self):
        reporte = preparar_reporte([], [], {'actividades': []})
        vista = io.BytesIO(json.dumps({
            'candidates': [{'content': {'parts': [{'text': 'Resumen verificable de prueba.'}]}}]
        }).encode('utf-8'))
        capturadas = []

        def falso_urlopen(peticion, timeout=None):
            capturadas.append(peticion)
            return vista

        with patch('actividades.reporte_evento.request.urlopen', side_effect=falso_urlopen):
            resultado = _solicitar_gemini(reporte, 'CLAVE_FICTICIA', 'gemini-2.5-flash')
        self.assertEqual(resultado, 'Resumen verificable de prueba.')
        self.assertIn('gemini-2.5-flash:generateContent', capturadas[0].full_url)
        self.assertEqual(capturadas[0].get_header('X-goog-api-key'), 'CLAVE_FICTICIA')
        self.assertEqual(capturadas[0].get_method(), 'POST')
