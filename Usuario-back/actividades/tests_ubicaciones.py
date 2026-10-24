"""Pruebas de la ubicación de una actividad (US-27 · tarea 5, parte de Mishel). Ejecutar:  python manage.py test"""
import json
from datetime import datetime, timezone
from unittest import mock

from bson import ObjectId
from django.test import RequestFactory, SimpleTestCase, override_settings

from usuarios import tests as base

from . import tests_tablas as tablas
from . import views
from .ubicaciones import UBICACIONES_INICIALES, preparar_ubicaciones

UBICACIONES = [
    {"_id": "patio-central", "nombre": "Patio central"},
    {"_id": "auditorio", "nombre": "Auditorio"},
]
VALIDA = {
    "nombre": "Charla TEC",
    "descripcion": "Emprendimiento e innovación con sentido.",
    "categoriaId": "actividad-cultural",
    "ubicacionId": "auditorio",
    "fecha": "2026-10-20",
    "horaInicio": "10:00",
    "horaFin": "",
}


class TablaDeUbicacionesTests(SimpleTestCase):
    def test_crea_las_ubicaciones_de_la_tc_week(self):
        coleccion = tablas.Coleccion()
        self.assertEqual(preparar_ubicaciones(coleccion), len(UBICACIONES_INICIALES))
        self.assertEqual({d["_id"] for d in coleccion.docs}, {"auditorio", "patio-central", "polideportivo"})
        self.assertIn({"_id": "patio-central", "nombre": "Patio central"}, coleccion.docs)

    def test_ids_y_nombres_no_se_repiten(self):
        ids = [i for i, _ in UBICACIONES_INICIALES]
        nombres = [n for _, n in UBICACIONES_INICIALES]
        self.assertEqual(len(set(ids)), len(ids))
        self.assertEqual(len(set(nombres)), len(nombres))

    def test_repetirlo_no_duplica_ni_pisa_nada(self):
        coleccion = tablas.Coleccion([{"_id": "auditorio", "nombre": "Auditorio principal"}])
        preparar_ubicaciones(coleccion)
        self.assertEqual(preparar_ubicaciones(coleccion), 0)
        self.assertEqual(len(coleccion.docs), len(UBICACIONES_INICIALES))
        self.assertEqual(coleccion.find_one({"_id": "auditorio"})["nombre"], "Auditorio principal")

    def test_el_nombre_es_unico(self):
        coleccion = tablas.Coleccion()
        preparar_ubicaciones(coleccion)
        self.assertEqual(coleccion.indices, [("nombre", {"unique": True})])


@override_settings(JWT_SECRET=base.SECRETO)
class UbicacionDeUnaActividadTests(SimpleTestCase):
    def setUp(self):
        self.actividades = tablas.Coleccion()
        parches = {
            "actividades.servicio.mongo.categorias": tablas.Coleccion([{"_id": "actividad-cultural", "nombre": "Actividad cultural"}]),
            "actividades.servicio.mongo.ubicaciones": tablas.Coleccion(UBICACIONES),
            "actividades.servicio.mongo.actividades": self.actividades,
            "usuarios.auth.mongo.usuarios": base.ColeccionFalsa(base.USUARIOS),
            "usuarios.auth.mongo.tokens_revocados": base.ColeccionFalsa([]),
        }
        for objetivo, coleccion in parches.items():
            p = mock.patch(objetivo, return_value=coleccion)
            p.start()
            self.addCleanup(p.stop)
        self.rf = RequestFactory()

    def _auth(self, quien):
        return {"HTTP_AUTHORIZATION": f"Bearer {base.token(quien)}"} if quien else {}

    def _post(self, cuerpo, quien=base.ID_ADMIN):
        return views.actividades(self.rf.post("/", data=json.dumps(cuerpo), content_type="application/json", **self._auth(quien)))

    def _get(self, vista, quien=base.ID_ADMIN):
        return vista(self.rf.get("/", **self._auth(quien)))

    # --- crear con ubicación ----------------------------------------------
    def test_guarda_la_ubicacion_y_la_devuelve_con_su_nombre(self):
        r = self._post(VALIDA)
        self.assertEqual(r.status_code, 201)
        self.assertEqual(json.loads(r.content)["ubicacion"], {"id": "auditorio", "nombre": "Auditorio"})
        self.assertEqual(self.actividades.docs[0]["ubicacionId"], "auditorio")

    def test_sin_ubicacion_da_400_y_no_guarda(self):
        r = self._post({k: v for k, v in VALIDA.items() if k != "ubicacionId"})
        self.assertEqual(r.status_code, 400)
        self.assertEqual(json.loads(r.content)["errores"], {"ubicacionId": "Elige una ubicación."})
        self.assertEqual(self.actividades.docs, [])

    def test_ubicacion_vacia_o_solo_espacios_da_400(self):
        for vacia in ("", "   ", None):
            self.assertEqual(self._post({**VALIDA, "ubicacionId": vacia}).status_code, 400, repr(vacia))

    def test_ubicacion_que_no_existe_da_400(self):
        r = self._post({**VALIDA, "ubicacionId": "la-luna"})
        self.assertEqual(r.status_code, 400)
        self.assertEqual(json.loads(r.content)["errores"], {"ubicacionId": "Elige una ubicación válida."})
        self.assertEqual(self.actividades.docs, [])

    def test_tipos_raros_en_la_ubicacion_no_rompen(self):
        for raro in (5, ["auditorio"], {"id": "auditorio"}):
            self.assertEqual(self._post({**VALIDA, "ubicacionId": raro}).status_code, 400, repr(raro))

    def test_una_ubicacion_mala_no_tapa_los_otros_errores(self):
        errores = json.loads(self._post({"ubicacionId": "x"}).content)["errores"]
        self.assertEqual(sorted(errores), ["categoriaId", "descripcion", "fecha", "horaInicio", "nombre", "ubicacionId"])

    def test_se_quitan_espacios_del_id(self):
        self.assertEqual(self._post({**VALIDA, "ubicacionId": "  auditorio "}).status_code, 201)

    # --- listas ------------------------------------------------------------
    def test_la_lista_trae_la_ubicacion_y_las_viejas_dicen_null(self):
        ahora = datetime.now(timezone.utc)
        base_doc = {"descripcion": "d" * 10, "categoriaId": "actividad-cultural", "fecha": "2026-10-19", "horaInicio": "11:00",
                    "horaFin": None, "creadaPor": "x", "creadaEn": ahora}
        self.actividades.docs = [
            {"_id": ObjectId(), "nombre": "Con lugar", "ubicacionId": "patio-central", **base_doc},
            {"_id": ObjectId(), "nombre": "Lugar borrado", "ubicacionId": "demolido", **base_doc, "horaInicio": "12:00"},
            {"_id": ObjectId(), "nombre": "Vieja", **base_doc, "horaInicio": "13:00"},
        ]
        datos = json.loads(self._get(views.actividades).content)["actividades"]
        self.assertEqual([a["nombre"] for a in datos], ["Con lugar", "Lugar borrado", "Vieja"])
        self.assertEqual(datos[0]["ubicacion"], {"id": "patio-central", "nombre": "Patio central"})
        self.assertEqual(datos[1]["ubicacion"], {"id": "demolido", "nombre": "Sin ubicación"})
        self.assertIsNone(datos[2]["ubicacion"])

    def test_lista_de_ubicaciones_ordenada_por_nombre(self):
        r = self._get(views.ubicaciones)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(json.loads(r.content)["ubicaciones"], [
            {"id": "auditorio", "nombre": "Auditorio"},
            {"id": "patio-central", "nombre": "Patio central"},
        ])

    def test_lista_de_ubicaciones_requiere_admin(self):
        self.assertEqual(self._get(views.ubicaciones, quien=None).status_code, 401)
        self.assertEqual(self._get(views.ubicaciones, quien=base.ID_NORMAL).status_code, 403)

    def test_lista_de_ubicaciones_solo_acepta_get(self):
        peticion = self.rf.post("/", **self._auth(base.ID_ADMIN))
        self.assertEqual(views.ubicaciones(peticion).status_code, 405)
