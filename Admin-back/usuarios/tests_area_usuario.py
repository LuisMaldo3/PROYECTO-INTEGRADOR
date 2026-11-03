"""Verifica las cinco áreas y la actualización de inscripciones compartidas."""
import json
from unittest import mock
from django.test import RequestFactory, SimpleTestCase

from . import views, mongo


class AreaUsuarioTests(SimpleTestCase):
    def _vista(self):
        vista = views.verificar_area_usuario
        while hasattr(vista, '__wrapped__'):
            vista = vista.__wrapped__
        return vista

    def test_usuarios_mongo_declara_coleccion_inscripciones(self):
        with mock.patch('usuarios.mongo.db', return_value={'inscripciones': 'coleccion'}):
            self.assertEqual(mongo.inscripciones(), 'coleccion')

    def test_asigna_area_y_actualiza_inscripciones(self):
        solicitud = RequestFactory().patch(
            '/api/usuarios/abc/area/',
            data=json.dumps({'areaTecsup': 'Tecnología Digital'}),
            content_type='application/json')
        solicitud.admin_id = 'admin'
        usuario = {'_id': 'abc', 'nombre': 'Laura', 'areaTecsup': 'Tecnología Digital', 'areaVerificada': True}
        with mock.patch('usuarios.views.buscar_por_id', side_effect=[{'_id': 'abc'}, usuario]), \
                mock.patch('usuarios.views.mongo.usuarios') as usuarios, \
                mock.patch('usuarios.views.mongo.inscripciones') as inscripciones:
            respuesta = self._vista()(solicitud, 'abc')
        self.assertEqual(respuesta.status_code, 200)
        self.assertTrue(json.loads(respuesta.content)['areaVerificada'])
        usuarios.return_value.update_one.assert_called_once()
        inscripciones.return_value.update_many.assert_called_once_with(
            {'estudianteId': 'abc', 'estado': 'inscrita'},
            {'$set': {'areaTecsup': 'Tecnología Digital', 'areaVerificada': True}})

    def test_area_desconocida_rechazada_sin_escribir(self):
        solicitud = RequestFactory().patch(
            '/api/usuarios/abc/area/',
            data=json.dumps({'areaTecsup': 'Área 6'}), content_type='application/json')
        solicitud.admin_id = 'admin'
        with mock.patch('usuarios.views.mongo.usuarios') as usuarios:
            respuesta = self._vista()(solicitud, 'abc')
        self.assertEqual(respuesta.status_code, 400)
        usuarios.assert_not_called()
