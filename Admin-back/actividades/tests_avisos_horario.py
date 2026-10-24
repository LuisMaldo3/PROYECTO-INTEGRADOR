"""Pruebas aisladas: cambios de horario, deduplicación de inscritos y cancelaciones."""
from unittest import mock
from types import SimpleNamespace
from django.test import SimpleTestCase
from .cambios_horario import detectar_cambios_horario
from .avisos_horario import crear_avisos_inscritos


class ColeccionInscripciones:
    def find(self, filtro):
        return [
            {"actividadId": "actividad-1", "usuarioId": "alumno-1", "estado": "inscrito"},
            {"actividadId": "actividad-1", "usuarioId": "alumno-1", "estado": "inscrito"},
            {"actividadId": "actividad-1", "usuarioId": "alumno-2", "estado": "cancelada"},
        ]


class ColeccionNotificaciones:
    def __init__(self):
        self.docs = []

    def insert_many(self, docs, ordered=True):
        self.docs.extend(docs)
        return SimpleNamespace(inserted_ids=list(range(len(docs))))


class AvisoHorarioTests(SimpleTestCase):
    def test_detecta_solo_los_campos_cambiados(self):
        cambios = detectar_cambios_horario({"fecha": "2026-10-28", "horaInicio": "10:00", "horaFin": None},
                                           {"fecha": "2026-10-28", "horaInicio": "12:00", "horaFin": ""})
        self.assertEqual(list(cambios), ["horaInicio"])

    def test_genera_un_aviso_por_inscrito_activo(self):
        notas = ColeccionNotificaciones()
        with mock.patch("actividades.avisos_horario.mongo.inscripciones", return_value=ColeccionInscripciones()), \
             mock.patch("actividades.avisos_horario.mongo.notificaciones", return_value=notas):
            creados, inscritos = crear_avisos_inscritos("actividad-1", "Seminario", {"horaInicio": {"anterior": "10:00", "nuevo": "12:00"}}, "admin-1")
        self.assertEqual((creados, inscritos), (1, 1))
        self.assertEqual(notas.docs[0]["usuarioId"], "alumno-1")
        self.assertFalse(notas.docs[0]["leida"])
