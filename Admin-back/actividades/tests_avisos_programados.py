"""Comportamiento de validación y publicación programada sin red ni Mongo real."""
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from unittest import mock
from django.test import SimpleTestCase
from .avisos import fecha_programada, AvisoInvalido, crear_aviso, publicar_pendientes


class AvisosProgramadosTests(SimpleTestCase):
    def setUp(self):
        self.ahora = datetime(2026, 10, 26, 18, tzinfo=timezone.utc)

    def test_acepta_zona_horaria_y_normaliza_a_utc(self):
        fecha = fecha_programada({"publicarEn": "2026-10-26T15:00:00-05:00"}, self.ahora)
        self.assertEqual(fecha.hour, 20)

    def test_rechaza_fecha_sin_zona(self):
        with self.assertRaises(AvisoInvalido):
            fecha_programada({"publicarEn": "2026-10-26T20:00:00"}, self.ahora)

    def test_rechaza_programacion_pasada(self):
        with self.assertRaises(AvisoInvalido):
            fecha_programada({"publicarEn": "2026-10-25T15:00:00-05:00"}, self.ahora)

    def test_aviso_programado_no_se_publica_inmediatamente(self):
        coleccion = mock.Mock()
        coleccion.insert_one.return_value.inserted_id = "nuevo"
        with mock.patch("actividades.avisos.mongo.avisos", return_value=coleccion):
            resultado = crear_aviso({"titulo": "Reunión", "mensaje": "Auditorio central",
                                    "publicarEn": "2026-10-26T15:00:00-05:00"}, "admin", self.ahora)
        self.assertEqual(resultado["estado"], "programado")
        self.assertIsNone(resultado["publicadoEn"])

    def test_publicar_pendientes_es_atomico_y_filtra_vencidos(self):
        coleccion = mock.Mock()
        coleccion.update_many.return_value = SimpleNamespace(modified_count=2)
        with mock.patch("actividades.avisos.mongo.avisos", return_value=coleccion):
            self.assertEqual(publicar_pendientes(self.ahora), 2)
        filtro = coleccion.update_many.call_args.args[0]
        self.assertEqual(filtro["estado"], "programado")
        self.assertEqual(filtro["publicarEn"], {"$lte": self.ahora})
