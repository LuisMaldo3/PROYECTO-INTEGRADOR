"""US-35 · Pruebas del número de inscritos por actividad (tareas 1, 4 y T05)."""
import json
from unittest import mock

from django.test import RequestFactory, SimpleTestCase

from . import inscritos, views


def _usuario_admin(vista):
    """Simula el decorador exigir_admin: deja pasar la petición como administrador."""
    def envoltura(request, *args, **kwargs):
        request.admin_id = "admin"
        return vista(request, *args, **kwargs)
    return envoltura


class _Coleccion:
    """Colección en memoria: basta con find() para este servicio (sin depender de MongoDB)."""
    def __init__(self, documentos):
        self.documentos = documentos

    def find(self, filtro=None, proyeccion=None):
        return [dict(d) for d in self.documentos]


class ContarInscripcionesTests(SimpleTestCase):
    def test_cuenta_personas_distintas_y_excluye_canceladas(self):
        conteo = inscritos.contar_inscripciones([
            {"estudianteId": "a", "estado": "inscrita"},
            {"estudianteId": "b"},                      # sin estado = inscrita
            {"estudianteId": "a", "estado": "inscrita"},  # duplicado: misma persona
            {"estudianteId": "c", "estado": "cancelada"},
        ])
        self.assertEqual(conteo["personasInscritas"], 2)
        self.assertEqual(conteo["cancelaciones"], 1)
        self.assertEqual(conteo["equiposRegistrados"], 0)

    def test_distingue_equipos_completos_de_incompletos(self):
        registros = [
            # Equipo 1: capitán + integrante, ambos confirmados → completo
            {"_id": "e1", "estudianteId": "cap1", "nombreEquipo": "Rayo", "integrantes": ["cap1", "m1"], "equipoId": "e1"},
            {"_id": "i1", "estudianteId": "m1", "rolEquipo": "integrante", "equipoId": "e1"},
            # Equipo 2: un integrante todavía no confirmó → incompleto
            {"_id": "e2", "estudianteId": "cap2", "nombreEquipo": "Trueno", "integrantes": ["cap2", "m2"], "equipoId": "e2"},
            # Equipo 3: solo el capitán → incompleto
            {"_id": "e3", "estudianteId": "cap3", "nombreEquipo": "Solo", "integrantes": ["cap3"], "equipoId": "e3"},
        ]
        conteo = inscritos.contar_inscripciones(registros)
        self.assertEqual(conteo["personasInscritas"], 4)  # cap1, m1, cap2, cap3
        self.assertEqual(conteo["equiposRegistrados"], 3)
        self.assertEqual(conteo["equiposCompletos"], 1)
        self.assertEqual(conteo["equiposIncompletos"], 2)


class InscritosPorActividadTests(SimpleTestCase):
    def setUp(self):
        actividades = _Coleccion([
            {"_id": "taller", "nombre": "Taller de Arduino", "estado": "programada", "maximoParticipantes": 10},
            {"_id": "futbol", "nombre": "Fútbol", "estado": "programada", "tipoActividad": "competencia",
             "modalidad": "equipos", "maximoParticipantes": 4},
            {"_id": "vacia", "nombre": "Charla", "estado": "programada"},
            {"_id": "anulada", "nombre": "Feria", "estado": "cancelada"},
        ])
        inscripciones = _Coleccion([
            {"actividadId": "taller", "estudianteId": "a"},
            {"actividadId": "taller", "estudianteId": "b"},
            {"actividadId": "taller", "estudianteId": "c", "estado": "cancelada"},
            {"_id": "eq", "actividadId": "futbol", "estudianteId": "cap", "nombreEquipo": "Rayo",
             "integrantes": ["cap", "m"], "equipoId": "eq"},
            {"actividadId": "futbol", "estudianteId": "m", "rolEquipo": "integrante", "equipoId": "eq"},
            {"actividadId": "anulada", "estudianteId": "z"},
        ])
        self.parches = [mock.patch("actividades.mongo.actividades", return_value=actividades),
                        mock.patch("actividades.mongo.inscripciones", return_value=inscripciones)]
        for p in self.parches:
            p.start()

    def tearDown(self):
        for p in self.parches:
            p.stop()

    def test_resumen_por_actividad(self):
        datos = inscritos.inscritos_por_actividad()
        por_id = {a["id"]: a for a in datos["actividades"]}
        self.assertEqual(por_id["taller"]["personasInscritas"], 2)
        self.assertEqual(por_id["taller"]["ocupados"], 2)
        self.assertEqual(por_id["taller"]["porcentajeOcupacion"], 20)
        self.assertEqual(por_id["taller"]["unidadCupo"], "estudiantes")
        # Por equipos: el cupo se cuenta por equipo, no por persona.
        self.assertEqual(por_id["futbol"]["personasInscritas"], 2)
        self.assertEqual(por_id["futbol"]["ocupados"], 1)
        self.assertEqual(por_id["futbol"]["equiposCompletos"], 1)
        self.assertEqual(por_id["futbol"]["unidadCupo"], "equipos")
        self.assertEqual(por_id["futbol"]["porcentajeOcupacion"], 25)
        # Sin inscritos y sin límite de cupos.
        self.assertEqual(por_id["vacia"]["personasInscritas"], 0)
        self.assertIsNone(por_id["vacia"]["porcentajeOcupacion"])

    def test_orden_y_totales_sin_canceladas(self):
        datos = inscritos.inscritos_por_actividad()
        self.assertEqual(datos["actividades"][0]["id"], "futbol")  # 2 personas y 1 equipo, antes que el taller
        self.assertEqual(datos["totales"]["actividades"], 3)       # la cancelada no cuenta
        self.assertEqual(datos["totales"]["personasInscritas"], 4)
        self.assertEqual(datos["totales"]["equiposRegistrados"], 1)
        self.assertEqual(datos["totales"]["sinInscritos"], 1)

    def test_endpoint_responde_json(self):
        vista = _usuario_admin(views.inscritos_por_actividad.__wrapped__.__wrapped__)
        respuesta = vista(RequestFactory().get("/api/estadisticas/inscritos/"))
        self.assertEqual(respuesta.status_code, 200)
        cuerpo = json.loads(respuesta.content)
        self.assertIn("actividades", cuerpo)
        self.assertIn("totales", cuerpo)
