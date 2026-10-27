"""Validaciones aisladas de cambios de ubicación y notificaciones."""
from unittest import mock
from types import SimpleNamespace
from django.test import SimpleTestCase
from .cambios_ubicacion import detectar_cambio_ubicacion
from .notificaciones_inscritos import notificar_cambio_ubicacion, notificar_cancelacion


class InscripcionesFalsas:
    def find(self, filtro):
        return [{"usuarioId": "u-1", "estado": "inscrito"},
                {"usuarioId": "u-1", "estado": "inscrito"},
                {"usuarioId": "u-2", "estado": "cancelada"}]


class NotificacionesFalsas:
    def __init__(self): self.docs = []
    def insert_many(self, docs, ordered=True):
        self.docs.extend(docs)
        return SimpleNamespace(inserted_ids=list(range(len(docs))))


class CambiosUbicacionTests(SimpleTestCase):
    def test_no_detecta_cambio_cuando_es_misma_ubicacion(self):
        self.assertEqual(detectar_cambio_ubicacion({"ubicacionId":"l1"}, {"ubicacionId":"l1"}), {})

    def test_detecta_ubicacion_nueva(self):
        self.assertEqual(detectar_cambio_ubicacion({"ubicacionId":"l1"}, {"ubicacionId":"l2"})["ubicacionId"]["nuevo"], "l2")

    def test_notifica_una_vez_por_inscrito_y_excluye_cancelados(self):
        col = NotificacionesFalsas()
        with mock.patch("actividades.notificaciones_inscritos.mongo.inscripciones", return_value=InscripcionesFalsas()), \
             mock.patch("actividades.notificaciones_inscritos.mongo.notificaciones", return_value=col):
            self.assertEqual(notificar_cambio_ubicacion("a1", "Conferencia", "Aula 1", "Aula 2", "admin"), (1, 1))
            self.assertEqual(notificar_cancelacion("a1", "Conferencia", "admin"), (1, 1))
        self.assertEqual([d["tipo"] for d in col.docs], ["CAMBIO_UBICACION", "CANCELACION_ACTIVIDAD"])
        self.assertTrue(all(d["usuarioId"] == "u-1" for d in col.docs))
