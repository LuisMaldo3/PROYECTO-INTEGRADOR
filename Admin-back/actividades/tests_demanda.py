"""US-37: pruebas del ranking, integración con US-35 y degradación sin Gemini."""
from unittest import mock
from django.test import SimpleTestCase, RequestFactory
from . import demanda, views
import json


class ColeccionFalsa:
    def __init__(self, documentos):
        self.documentos = documentos

    def find(self, *_args, **_kwargs):
        return [dict(x) for x in self.documentos]


class DemandaTests(SimpleTestCase):
    def setUp(self):
        self.actividades = [
            {'_id': 'a', 'nombre': 'Taller de programación', 'maximoParticipantes': 10,
             'estado': 'programada'},
            {'_id': 'b', 'nombre': 'Torneo de robótica', 'maximoParticipantes': 2,
             'tipoActividad': 'competencia', 'modalidad': 'equipos', 'estado': 'programada'},
            {'_id': 'c', 'nombre': 'Taller cancelado', 'maximoParticipantes': 1,
             'estado': 'cancelada'},
            {'_id': 'd', 'nombre': 'Evento sin límite', 'estado': 'programada'},
        ]
        self.inscripciones = [
            {'actividadId': 'a', 'estudianteId': str(i)} for i in range(5)
        ] + [
            {'_id': 'equipo_1', 'actividadId': 'b', 'estudianteId': 'cap',
             'nombreEquipo': 'Titanes', 'integrantes': ['cap', 'otro'], 'equipoId': 'equipo_1'},
            {'actividadId': 'b', 'estudianteId': 'otro', 'equipoId': 'equipo_1', 'rolEquipo': 'integrante'},
            {'actividadId': 'b', 'estudianteId': 'sin_confirmar', 'estado': 'cancelada'},
            {'actividadId': 'c', 'estudianteId': 'uno'},
            {'actividadId': 'd', 'estudianteId': 'x'},
        ]
        self.patches = [
            mock.patch('actividades.mongo.actividades', return_value=ColeccionFalsa(self.actividades)),
            mock.patch('actividades.mongo.inscripciones', return_value=ColeccionFalsa(self.inscripciones)),
        ]
        for p in self.patches: p.start()

    def tearDown(self):
        for p in self.patches: p.stop()

    def test_orden_por_ocupacion_y_sin_canceladas(self):
        r = demanda.obtener_demanda()
        self.assertEqual([a['id'] for a in r['actividades']], ['a', 'b', 'd'])
        self.assertEqual(r['actividades'][0]['porcentajeOcupacion'], 50)
        self.assertEqual(r['actividades'][1]['porcentajeOcupacion'], 50)
        self.assertEqual(r['totales']['actividades'], 3)

    def test_competencias_se_miden_por_equipos_y_no_por_personas(self):
        r = demanda.obtener_demanda()
        equipo = next(a for a in r['actividades'] if a['id'] == 'b')
        self.assertEqual(equipo['ocupados'], 1)
        self.assertEqual(equipo['personasInscritas'], 2)
        self.assertEqual(equipo['capacidad'], 2)
        self.assertEqual(equipo['unidadCupo'], 'equipos')
        self.assertEqual(equipo['equiposCompletos'], 1)

    def test_sin_clave_no_hay_ia_inventada_y_ranking_funciona(self):
        with mock.patch.dict('os.environ', {'GEMINI_API_KEY': ''}):
            r = demanda.obtener_demanda(con_ia=True)
        self.assertEqual(r['ia']['estado'], 'sin_configurar')
        self.assertIsNone(r['ia']['texto'])
        self.assertEqual(len(r['actividades']), 3)

    def test_error_de_red_no_invalida_estadisticas(self):
        with mock.patch.dict('os.environ', {'GEMINI_API_KEY': 'test'}):
            with mock.patch.object(demanda, '_solicitar_gemini', side_effect=TimeoutError('no conectado')):
                r = demanda.obtener_demanda(con_ia=True)
        self.assertEqual(r['ia']['estado'], 'no_disponible')
        self.assertEqual(r['totales']['actividades'], 3)

    def test_ia_se_solicita_solo_si_se_pide_explicacion(self):
        with mock.patch.dict('os.environ', {'GEMINI_API_KEY': 'test'}):
            with mock.patch.object(demanda, '_solicitar_gemini', return_value='Alta ocupación en talleres.') as llamada:
                antes = demanda.obtener_demanda()
                despues = demanda.obtener_demanda(con_ia=True)
        self.assertEqual(antes['ia']['estado'], 'no_solicitada')
        self.assertEqual(despues['ia']['estado'], 'disponible')
        llamada.assert_called_once()

    def test_endpoint_responde_json_con_ranking(self):
        # Desenvuelve los decoradores para probar solo la salida de la vista.
        vista = views.ranking_demanda
        while hasattr(vista, '__wrapped__'): vista = vista.__wrapped__
        resultado = vista(RequestFactory().get('/api/estadisticas/demanda/'))
        self.assertEqual(resultado.status_code, 200)
        self.assertIn('actividades', json.loads(resultado.content))
