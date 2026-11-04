"""US-39: pruebas puras de puntajes, áreas, ganadores y deduplicación.

Ejecutar con Python sin Django: python -m unittest actividades.tests_reporte_evento
Cuando Django esté instalado: python manage.py test actividades.tests_reporte_evento
"""
import unittest
from .competencias import AREAS_TECSUP
from .reporte_evento import preparar_reporte


class ResumenEventoTests(unittest.TestCase):
    def setUp(self):
        self.actividades = [
            {'_id': 'a', 'nombre': 'Robótica', 'tipoActividad': 'competencia', 'estado': 'programada'},
            {'_id': 'b', 'nombre': 'Química', 'tipoActividad': 'competencia', 'estado': 'programada'},
            {'_id': 'c', 'nombre': 'Suspendida', 'tipoActividad': 'competencia', 'estado': 'cancelada'},
            {'_id': 'd', 'nombre': 'Charla', 'tipoActividad': 'actividad', 'estado': 'programada'},
        ]
        self.inscritos = {'actividades': [
            {'id': 'a', 'personasInscritas': 4, 'equiposCompletos': 1},
            {'id': 'b', 'personasInscritas': 2, 'equiposCompletos': 0},
            {'id': 'c', 'personasInscritas': 7, 'equiposCompletos': 2},
            {'id': 'd', 'personasInscritas': 3, 'equiposCompletos': 0},
        ]}
        self.publicados = [
            {'_id': 'r1', 'actividadId': 'a', 'estado': 'publicado', 'publicadoEn': '2026-11-10T10:00:00',
             'posiciones': [
                 {'inscripcionId': 'x', 'puesto': 1, 'puntos': 10, 'nombre': 'Equipo A', 'areaTecsup': AREAS_TECSUP[0]},
                 {'inscripcionId': 'y', 'puesto': 2, 'puntos': 6, 'nombre': 'Equipo B', 'areaTecsup': AREAS_TECSUP[1]},
             ]},
            {'_id': 'r2', 'actividadId': 'b', 'estado': 'pendiente',
             'posiciones': [{'inscripcionId': 'x3', 'puesto': 1, 'puntos': 900, 'areaTecsup': AREAS_TECSUP[2]}]},
            {'_id': 'r3', 'actividadId': 'c', 'estado': 'publicado',
             'posiciones': [{'inscripcionId': 'x4', 'puesto': 1, 'puntos': 500, 'areaTecsup': AREAS_TECSUP[3]}]},
        ]

    def test_solo_resultados_publicados_de_actividades_vigentes(self):
        r = preparar_reporte(self.actividades, self.publicados, self.inscritos)
        self.assertEqual(r['totales']['competenciasConResultados'], 1)
        self.assertEqual(r['totales']['competenciasSinResultados'], 1)
        self.assertEqual(r['totales']['puntosOficiales'], 16)
        self.assertEqual(r['totales']['actividadesCanceladas'], 1)
        self.assertEqual(r['totales']['inscripcionesActivas'], 9)
        self.assertEqual(r['totales']['equiposCompletos'], 1)
        self.assertEqual(r['resultados'][0]['ganadores'][0]['nombre'], 'Equipo A')

    def test_cinco_areas_incluso_sin_puntos(self):
        r = preparar_reporte([], [], {'actividades': []})
        self.assertEqual(len(r['areas']), 5)
        self.assertEqual({x['area'] for x in r['areas']}, set(AREAS_TECSUP))
        self.assertEqual(sum(x['puntos'] for x in r['areas']), 0)

    def test_no_duplica_documentos_ni_posiciones_repetidas(self):
        repetida = {**self.publicados[0], '_id': 'r0', 'publicadoEn': '2026-11-09T09:00:00'}
        self.publicados += [repetida, {**self.publicados[0], 'posiciones': self.publicados[0]['posiciones'] +
                                  [{'inscripcionId': 'x', 'puesto': 3, 'puntos': 80, 'areaTecsup': AREAS_TECSUP[0]}]}]
        r = preparar_reporte(self.actividades, self.publicados, self.inscritos)
        self.assertEqual(r['totales']['puntosOficiales'], 16)
        self.assertEqual(len(r['resultados']), 1)
        self.assertEqual(r['totales']['posicionesOficiales'], 2)

    def test_areas_no_oficiales_y_puntos_invalidos_no_suman(self):
        datos = self.publicados[:1]
        datos[0]['posiciones'].extend([
            {'inscripcionId': 'z', 'puesto': 3, 'puntos': 100, 'areaTecsup': 'Otra sede'},
            {'inscripcionId': 'w', 'puesto': 4, 'puntos': -4, 'areaTecsup': AREAS_TECSUP[2]},
            {'inscripcionId': 'w2', 'puesto': 5, 'puntos': True, 'areaTecsup': AREAS_TECSUP[2]},
        ])
        r = preparar_reporte(self.actividades, datos, self.inscritos)
        self.assertEqual(r['totales']['puntosOficiales'], 16)

    def test_no_altera_ni_guarda_fuentes_al_recalcular(self):
        primero = preparar_reporte(self.actividades, self.publicados, self.inscritos)
        segundo = preparar_reporte(self.actividades, self.publicados, self.inscritos)
        self.assertEqual(primero['totales'], segundo['totales'])
        self.assertEqual(primero['areas'], segundo['areas'])
        self.assertEqual(primero['resultados'], segundo['resultados'])

    def test_desempate_por_primeros_puestos_y_alfabeto(self):
        r = preparar_reporte(self.actividades, self.publicados, self.inscritos)
        self.assertEqual(r['areas'][0]['area'], AREAS_TECSUP[0])
        self.assertEqual(r['areas'][0]['primerosPuestos'], 1)

    def test_sin_fingir_cierre_del_evento(self):
        r = preparar_reporte(self.actividades, self.publicados, self.inscritos)
        self.assertNotIn('ganadorGeneral', r)
        self.assertIn('no confirma automáticamente', r['aclaracion'])


if __name__ == '__main__':
    unittest.main()
