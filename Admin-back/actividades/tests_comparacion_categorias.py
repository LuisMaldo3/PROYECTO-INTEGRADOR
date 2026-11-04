"""US-40: pruebas puras, sin conexión real a MongoDB."""
import unittest
from .comparacion_categorias import preparar_comparacion
from .reporte_evento import preparar_reporte
from .competencias import AREAS_TECSUP


class ComparacionCategoriasTests(unittest.TestCase):
    def setUp(self):
        self.actividades = [
            {'_id': 'a', 'nombre': 'Fútbol', 'categoriaId': 'deportes', 'tipoActividad': 'competencia', 'modalidad': 'equipos', 'estado': 'programada'},
            {'_id': 'b', 'nombre': 'Robótica', 'categoriaId': 'tecnologia', 'tipoActividad': 'competencia', 'estado': 'programada'},
            {'_id': 'c', 'nombre': 'Charla', 'categoriaId': 'tecnologia', 'tipoActividad': 'actividad', 'estado': 'programada'},
            {'_id': 'd', 'nombre': 'Cancelada', 'categoriaId': 'deportes', 'tipoActividad': 'competencia', 'estado': 'cancelada'},
        ]
        self.categorias = [{'_id': 'deportes', 'nombre': 'Deportes'}, {'_id': 'tecnologia', 'nombre': 'Tecnología'}, {'_id': 'vacía', 'nombre': 'Arte'}]
        self.inscritos = {'actividades': [
            {'id': 'a', 'unidadCupo': 'equipos', 'personasInscritas': 6, 'equiposRegistrados': 2, 'equiposCompletos': 1, 'equiposIncompletos': 1, 'ocupados': 2, 'capacidad': 3},
            {'id': 'b', 'unidadCupo': 'estudiantes', 'personasInscritas': 3, 'ocupados': 3, 'capacidad': 8},
            {'id': 'c', 'unidadCupo': 'estudiantes', 'personasInscritas': 4, 'ocupados': 4, 'capacidad': 6},
            {'id': 'd', 'unidadCupo': 'estudiantes', 'personasInscritas': 40, 'ocupados': 40, 'capacidad': 40},
        ]}
        self.resultados = [
            {'_id': 'r1', 'actividadId': 'a', 'estado': 'publicado', 'publicadoEn': '2026-11-09T10:00:00',
             'posiciones': [{'inscripcionId': 'x', 'puesto': 1, 'puntos': 10, 'areaTecsup': AREAS_TECSUP[0]}]},
            {'_id': 'r2', 'actividadId': 'a', 'estado': 'publicado', 'publicadoEn': '2026-11-10T10:00:00',
             'posiciones': [{'inscripcionId': 'x', 'puesto': 1, 'puntos': 20, 'areaTecsup': AREAS_TECSUP[0]},
                           {'inscripcionId': 'y', 'puesto': 2, 'puntos': 5, 'areaTecsup': AREAS_TECSUP[1]}]},
            {'_id': 'r3', 'actividadId': 'd', 'estado': 'publicado',
             'posiciones': [{'inscripcionId': 'z', 'puesto': 1, 'puntos': 900, 'areaTecsup': AREAS_TECSUP[0]}]},
            {'_id': 'r4', 'actividadId': 'b', 'estado': 'pendiente',
             'posiciones': [{'inscripcionId': 'w', 'puesto': 1, 'puntos': 500, 'areaTecsup': AREAS_TECSUP[2]}]},
        ]

    def comparar(self):
        reporte = preparar_reporte(self.actividades, self.resultados, self.inscritos)
        return preparar_comparacion(self.actividades, self.categorias, self.inscritos, reporte)

    def test_categorias_participacion_real_no_canceladas(self):
        r = self.comparar()
        assert r['totales']['participaciones'] == 13
        dep = next(c for c in r['categorias'] if c['id'] == 'deportes')
        assert dep['personasInscritas'] == 6
        assert dep['actividades'] == 1
        assert dep['cuposEquiposOcupados'] == 2
        assert dep['cuposPersonasOcupados'] == 0
        assert dep['equiposCompletos'] == 1
        assert dep['equiposIncompletos'] == 1

    def test_categorias_vacias_mostradas_sin_datos_falsos(self):
        arte = next(c for c in self.comparar()['categorias'] if c['id'] == 'vacía')
        assert arte['actividades'] == arte['personasInscritas'] == 0

    def test_no_duplica_puntos_y_excluye_canceladas_y_pendientes(self):
        r = self.comparar()
        assert r['totales']['puntosOficiales'] == 25
        assert r['clasificacion']['totalCompetencias'] == 1
        assert len(r['clasificacion']['areas']) == 5
        assert r['clasificacion']['areas'][0]['puntos'] == 20
        assert len(r['competencias']) == 1
        assert r['competencias'][0]['categoriaId'] == 'deportes'
        assert self.comparar()['totales']['puntosOficiales'] == 25

    def test_porcentajes_sumados_son_participaciones_no_personas_unicas(self):
        r = self.comparar()
        assert round(sum(c['porcentajeParticipaciones'] for c in r['categorias']), 1) == 100.0

    def test_sin_actividades(self):
        reporte = preparar_reporte([], [], {'actividades': []})
        resultado = preparar_comparacion([], [], {'actividades': []}, reporte)
        assert resultado['totales']['participaciones'] == 0
        assert len(resultado['clasificacion']['areas']) == 5


if __name__ == '__main__':
    unittest.main()
