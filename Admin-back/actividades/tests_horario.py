"""Pruebas de fecha y horario (US-26 · tarea 6, parte de Jordi). Ejecutar:  python manage.py test"""
import json
from unittest import mock

from django.test import RequestFactory, SimpleTestCase, override_settings

from usuarios import tests as base

from . import tests_tablas as tablas
from . import views
from .horario import FIN_ANTES, validar_horario

OK = {"fecha": "2026-10-19", "horaInicio": "11:00", "horaFin": "14:30"}


class ValidarHorarioTests(SimpleTestCase):
    def test_horario_completo_es_valido(self):
        horario, errores = validar_horario(OK)
        self.assertEqual(errores, {})
        self.assertEqual(horario, OK)

    def test_la_hora_de_fin_es_opcional(self):
        for sin_fin in ({"fecha": "2026-10-19", "horaInicio": "15:30"}, {**OK, "horaFin": ""}, {**OK, "horaFin": None}, {**OK, "horaFin": "  "}):
            horario, errores = validar_horario(sin_fin)
            self.assertEqual(errores, {})
            self.assertIsNone(horario["horaFin"])

    def test_fecha_obligatoria_y_real(self):
        self.assertIn("fecha", validar_horario({**OK, "fecha": ""})[1])
        for mala in ("2026-02-30", "2026-13-01", "19/10/2026", "2026-1-5", "mañana", 20261019, None):
            self.assertEqual(validar_horario({**OK, "fecha": mala})[1].get("fecha") is not None, True, mala)
        self.assertEqual(validar_horario({**OK, "fecha": "2028-02-29"})[1], {})  # año bisiesto

    def test_hora_de_inicio_obligatoria_y_en_formato_24h(self):
        self.assertIn("horaInicio", validar_horario({**OK, "horaInicio": ""})[1])
        for mala in ("25:00", "9:00", "10:60", "abc", "11:00 a. m.", 1100):
            self.assertIn("horaInicio", validar_horario({**OK, "horaInicio": mala})[1], mala)
        for buena in ("00:00", "09:05", "23:59"):
            self.assertNotIn("horaInicio", validar_horario({**OK, "horaInicio": buena, "horaFin": ""})[1])

    def test_la_hora_de_fin_debe_ser_despues_de_la_de_inicio(self):
        self.assertEqual(validar_horario({**OK, "horaInicio": "14:30", "horaFin": "14:30"})[1], {"horaFin": FIN_ANTES})  # igual
        self.assertEqual(validar_horario({**OK, "horaInicio": "14:30", "horaFin": "11:00"})[1], {"horaFin": FIN_ANTES})  # antes
        self.assertEqual(validar_horario({**OK, "horaInicio": "14:30", "horaFin": "14:31"})[1], {})  # un minuto después

    def test_hora_de_fin_con_formato_malo_no_dice_que_es_anterior(self):
        errores = validar_horario({**OK, "horaFin": "99:99"})[1]
        self.assertEqual(list(errores), ["horaFin"])
        self.assertNotEqual(errores["horaFin"], FIN_ANTES)

    def test_si_el_inicio_esta_mal_no_se_compara_el_fin(self):
        self.assertEqual(list(validar_horario({**OK, "horaInicio": "xx", "horaFin": "10:00"})[1]), ["horaInicio"])

    def test_datos_que_no_son_un_objeto_no_rompen(self):
        self.assertEqual(sorted(validar_horario([1, 2])[1]), ["fecha", "horaInicio"])


@override_settings(JWT_SECRET=base.SECRETO)
class EndpointHorarioTests(SimpleTestCase):
    VALIDA = {"nombre": "Feria Global Day", "descripcion": "Conferencias y stands en el patio.", "categoriaId": "actividad-cultural", **OK}

    def setUp(self):
        self.actividades = tablas.Coleccion()
        parches = {
            "actividades.servicio.mongo.categorias": tablas.Coleccion([{"_id": "actividad-cultural", "nombre": "Actividad cultural"}]),
            "actividades.servicio.mongo.actividades": self.actividades,
            "usuarios.auth.mongo.usuarios": base.ColeccionFalsa(base.USUARIOS),
            "usuarios.auth.mongo.tokens_revocados": base.ColeccionFalsa([]),
        }
        for objetivo, coleccion in parches.items():
            p = mock.patch(objetivo, return_value=coleccion)
            p.start()
            self.addCleanup(p.stop)
        self.rf = RequestFactory()

    def _post(self, cuerpo):
        peticion = self.rf.post("/", data=json.dumps(cuerpo), content_type="application/json",
                                HTTP_AUTHORIZATION=f"Bearer {base.token(base.ID_ADMIN)}")
        return views.actividades(peticion)

    def test_guarda_y_devuelve_la_fecha_y_las_horas(self):
        r = self._post(self.VALIDA)
        self.assertEqual(r.status_code, 201)
        datos = json.loads(r.content)
        self.assertEqual((datos["fecha"], datos["horaInicio"], datos["horaFin"]), ("2026-10-19", "11:00", "14:30"))
        self.assertEqual(self.actividades.docs[0]["fecha"], "2026-10-19")

    def test_sin_hora_de_fin_se_guarda_vacia(self):
        r = self._post({**self.VALIDA, "horaFin": ""})
        self.assertEqual(r.status_code, 201)
        self.assertIsNone(json.loads(r.content)["horaFin"])

    def test_sin_fecha_ni_hora_de_inicio_da_400(self):
        r = self._post({k: v for k, v in self.VALIDA.items() if k not in ("fecha", "horaInicio")})
        self.assertEqual(r.status_code, 400)
        self.assertEqual(sorted(json.loads(r.content)["errores"]), ["fecha", "horaInicio"])
        self.assertEqual(self.actividades.docs, [])

    def test_fin_antes_del_inicio_da_400_y_no_guarda(self):
        r = self._post({**self.VALIDA, "horaInicio": "15:00", "horaFin": "14:00"})
        self.assertEqual(r.status_code, 400)
        self.assertEqual(json.loads(r.content)["errores"], {"horaFin": FIN_ANTES})
        self.assertEqual(self.actividades.docs, [])
