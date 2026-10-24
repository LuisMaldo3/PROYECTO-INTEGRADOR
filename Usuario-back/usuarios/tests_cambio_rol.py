"""Pruebas del cambio de rol (US-24 · tarea 6, parte de Luis). Ejecutar:  python manage.py test usuarios

No necesitan MongoDB: reutilizan las colecciones falsas de tests.py y les suman update_one.
"""
import json
from unittest import mock

from bson import ObjectId
from django.test import RequestFactory, SimpleTestCase, override_settings

from . import tests as base
from . import views


class ColeccionEditable(base.ColeccionFalsa):
    """ColeccionFalsa + update_one con $set, lo único que necesita el cambio de rol."""

    def update_one(self, filtro, cambio):
        for d in self.docs:
            if all(d.get(k) == v for k, v in filtro.items()):
                d.update(cambio["$set"])
                return


@override_settings(JWT_SECRET=base.SECRETO)
class CambioDeRolTests(SimpleTestCase):
    def setUp(self):
        # Copia nueva por prueba: aquí sí se modifica la "base de datos".
        self.coleccion = ColeccionEditable([dict(d) for d in base.USUARIOS])
        for objetivo in ("usuarios.views.mongo.usuarios", "usuarios.auth.mongo.usuarios", "usuarios.servicio.mongo.usuarios"):
            p = mock.patch(objetivo, return_value=self.coleccion)
            p.start()
            self.addCleanup(p.stop)
        p = mock.patch("usuarios.auth.mongo.tokens_revocados", return_value=base.ColeccionFalsa([]))
        p.start()
        self.addCleanup(p.stop)
        self.rf = RequestFactory()

    def _patch(self, usuario_id, cuerpo, token_=None, crudo=None):
        extra = {"HTTP_AUTHORIZATION": f"Bearer {token_}"} if token_ else {}
        datos = crudo if crudo is not None else json.dumps(cuerpo)
        peticion = self.rf.patch("/", data=datos, content_type="application/json", **extra)
        return views.cambiar_rol_usuario(peticion, usuario_id=str(usuario_id))

    def _rol_guardado(self, usuario_id):
        return next(d["rol"] for d in self.coleccion.docs if d["_id"] == usuario_id)

    def test_admin_cambia_el_rol_y_queda_guardado(self):
        r = self._patch(base.ID_NORMAL, {"rol": "organizador"}, base.token(base.ID_ADMIN))
        self.assertEqual(r.status_code, 200)
        datos = json.loads(r.content)
        self.assertEqual(datos["rol"], "organizador")
        self.assertEqual(datos["correo"], "beto@tecsup.edu.pe")
        self.assertEqual(self._rol_guardado(base.ID_NORMAL), "organizador")

    def test_la_respuesta_no_trae_passwordhash(self):
        r = self._patch(base.ID_ADMIN, {"rol": "administrador"}, base.token(base.ID_ADMIN))
        self.assertEqual(r.status_code, 200)
        self.assertNotIn("passwordHash", r.content.decode())

    def test_usuario_inexistente_da_404(self):
        r = self._patch(ObjectId(), {"rol": "organizador"}, base.token(base.ID_ADMIN))
        self.assertEqual(r.status_code, 404)

    def test_sin_token_da_401_y_no_cambia_nada(self):
        r = self._patch(base.ID_NORMAL, {"rol": "administrador"})
        self.assertEqual(r.status_code, 401)
        self.assertEqual(self._rol_guardado(base.ID_NORMAL), "usuario")

    def test_usuario_normal_da_403_y_no_cambia_nada(self):
        r = self._patch(base.ID_NORMAL, {"rol": "administrador"}, base.token(base.ID_NORMAL))
        self.assertEqual(r.status_code, 403)
        self.assertEqual(self._rol_guardado(base.ID_NORMAL), "usuario")

    def test_json_malo_da_400(self):
        r = self._patch(base.ID_NORMAL, None, base.token(base.ID_ADMIN), crudo="{no es json")
        self.assertEqual(r.status_code, 400)
        self.assertEqual(json.loads(r.content)["codigo"], "JSON_INVALIDO")

    def test_solo_acepta_patch(self):
        peticion = self.rf.get("/", HTTP_AUTHORIZATION=f"Bearer {base.token(base.ID_ADMIN)}")
        self.assertEqual(views.cambiar_rol_usuario(peticion, usuario_id=str(base.ID_NORMAL)).status_code, 405)
