"""Pruebas de la vista US-38 con Django instalado: python manage.py test actividades.tests_intereses_api"""
from unittest import mock
import json

from django.test import RequestFactory, SimpleTestCase
from . import views


class InteresesAPITests(SimpleTestCase):
    def vista_sin_decoradores(self):
        # Aislamos aquí el JSON de la autorización JWT, que se prueba en usuarios.
        vista = views.estadisticas_intereses
        while hasattr(vista, '__wrapped__'):
            vista = vista.__wrapped__
        return vista

    @mock.patch('actividades.intereses.obtener_intereses', return_value={
        'totales': {'conIntereses': 0}, 'intereses': [],
        'ia': {'estado': 'no_solicitada', 'texto': None},
    })
    def test_get_estadisticas_sin_invocar_ia(self, obtener):
        respuesta = self.vista_sin_decoradores()(RequestFactory().get('/api/estadisticas/intereses/'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertIn('intereses', json.loads(respuesta.content))
        obtener.assert_called_once_with(con_ia=False)

    @mock.patch('actividades.intereses.obtener_intereses', return_value={'intereses': []})
    def test_ia_solo_si_se_solicita_con_parametro(self, obtener):
        respuesta = self.vista_sin_decoradores()(RequestFactory().get('/api/estadisticas/intereses/?ia=1'))
        self.assertEqual(respuesta.status_code, 200)
        obtener.assert_called_once_with(con_ia=True)
