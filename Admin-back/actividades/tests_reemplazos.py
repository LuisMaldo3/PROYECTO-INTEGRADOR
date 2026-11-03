"""US-36 · Pruebas unitarias de reemplazos y cupos, sin MongoDB real."""
from datetime import datetime, timezone
from unittest import mock
from django.test import RequestFactory, SimpleTestCase
import json

from . import reemplazos, views


class ColeccionFalsa:
    def __init__(self, docs):
        self.docs = docs
    def find(self, *_args, **_kwargs):
        return [dict(d) for d in self.docs]


class ReemplazosTests(SimpleTestCase):
    def setUp(self):
        self.parches = [
            mock.patch('actividades.mongo.actividades', return_value=ColeccionFalsa([
                {'_id': 'a', 'nombre': 'Taller', 'maximoParticipantes': 2, 'estado': 'programada'},
                {'_id': 'b', 'nombre': 'Torneo', 'tipoActividad': 'competencia', 'modalidad': 'equipos',
                 'maximoParticipantes': 1, 'estado': 'programada'},
            ])),
            mock.patch('actividades.mongo.inscripciones', return_value=ColeccionFalsa([
                {'actividadId': 'a', 'estudianteId': '1'},
                {'actividadId': 'a', 'estudianteId': '2'},
                {'actividadId': 'a', 'estudianteId': '3'},
                {'actividadId': 'b', '_id': 'eq', 'equipoId': 'eq', 'estudianteId': 'cap',
                 'nombreEquipo': 'Los Azules', 'integrantes': ['cap', 'jugador']},
                {'actividadId': 'b', 'estudianteId': 'jugador', 'equipoId': 'eq', 'rolEquipo': 'integrante'},
            ])),
            mock.patch('actividades.mongo.reemplazos_inscripciones', return_value=ColeccionFalsa([
                {'_id': 'r1', 'actividadId': 'a', 'estudianteSalienteId': '1',
                 'estudianteEntranteId': '3', 'estado': 'realizado', 'organizadorId': 'org',
                 'fecha': datetime(2026, 11, 10, tzinfo=timezone.utc)},
                {'_id': 'r2', 'actividadId': 'a', 'estudianteSalienteId': '2',
                 'estudianteEntranteId': '4', 'estado': 'pendiente'},
            ])),
        ]
        for parche in self.parches:
            parche.start()

    def tearDown(self):
        for parche in self.parches:
            parche.stop()

    def test_conserva_capacidad_y_marca_exceso_sin_modificar(self):
        datos = reemplazos.resumen_reemplazos()
        por_id = {a['id']: a for a in datos['actividades']}
        self.assertEqual(por_id['a']['capacidad'], 2)
        self.assertEqual(por_id['a']['ocupados'], 3)
        self.assertTrue(por_id['a']['excedeCupo'])
        self.assertEqual(por_id['a']['cuposDisponibles'], 0)
        self.assertEqual(por_id['b']['ocupados'], 1)  # un equipo, no dos alumnos
        self.assertFalse(por_id['b']['excedeCupo'])
        self.assertEqual(datos['totales']['actividadesConExceso'], 1)

    def test_solo_registra_reemplazos_confirmados(self):
        datos = reemplazos.resumen_reemplazos()
        por_id = {a['id']: a for a in datos['actividades']}
        self.assertEqual(por_id['a']['reemplazosConfirmados'], 1)
        self.assertEqual(len(por_id['a']['historial']), 1)
        self.assertEqual(por_id['a']['historial'][0]['fecha'], '2026-11-10T00:00:00+00:00')
        self.assertEqual(datos['totales']['reemplazosConfirmados'], 1)

    def test_sin_auditoria_no_inventa_reemplazos(self):
        with mock.patch('actividades.mongo.reemplazos_inscripciones', return_value=ColeccionFalsa([])):
            datos = reemplazos.resumen_reemplazos()
        self.assertEqual(datos['totales']['reemplazosConfirmados'], 0)
        self.assertFalse(datos['historialDisponible'])

    def test_endpoint_json_solo_lectura(self):
        # Desenvuelve require_GET, exigir_admin y exigir_permiso.
        vista = views.seguimiento_reemplazos
        while hasattr(vista, '__wrapped__'):
            vista = vista.__wrapped__
        request = RequestFactory().get('/api/estadisticas/reemplazos/')
        respuesta = vista(request)
        self.assertEqual(respuesta.status_code, 200)
        self.assertIn('actividades', json.loads(respuesta.content))
