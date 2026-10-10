"""Pruebas de la US-23 (tarea 7). Ejecutar:  python manage.py test usuarios

No necesitan MongoDB: se reemplazan las colecciones por unas falsas en memoria.
"""
import time
from unittest import mock

import jwt
from bson import ObjectId
from django.test import RequestFactory, SimpleTestCase, override_settings

from . import views

SECRETO = "s" * 40
ID_ADMIN = ObjectId()
ID_NORMAL = ObjectId()
ID_INACTIVO = ObjectId()


class Cursor:
    def __init__(self, docs):
        self.docs = docs

    def sort(self, campo, sentido):
        return sorted(self.docs, key=lambda d: d.get(campo, ""), reverse=sentido < 0)


class ColeccionFalsa:
    def __init__(self, docs):
        self.docs = docs

    def find_one(self, filtro, proyeccion=None):
        for d in self.docs:
            if all(d.get(k) == v for k, v in filtro.items()):
                return dict(d)
        return None

    def find(self, filtro, proyeccion=None):
        # Como pymongo con proyeccion: solo salen los campos pedidos (+ _id).
        campos = set(proyeccion or d_todos(self.docs)) | {"_id"}
        return Cursor([{k: v for k, v in d.items() if k in campos} for d in self.docs])


def d_todos(docs):
    return {k for d in docs for k in d}


USUARIOS = [
    {"_id": ID_ADMIN, "nombre": "Ana Admin", "correo": "ana@tecsup.edu.pe",
     "rol": "administrador", "activo": True, "passwordHash": "$2a$SECRETO"},
    {"_id": ID_NORMAL, "nombre": "Beto Usuario", "correo": "beto@tecsup.edu.pe",
     "rol": "usuario", "activo": True, "passwordHash": None},
    {"_id": ID_INACTIVO, "nombre": "Caro Inactiva", "correo": "caro@tecsup.edu.pe",
     "rol": "administrador", "activo": False},
]


def token(sub, jti="t-1", exp=None, secreto=SECRETO):
    claims = {"sub": str(sub), "jti": jti, "rol": "administrador",
              "exp": exp or int(time.time()) + 3600}
    return jwt.encode(claims, secreto, algorithm="HS256")


@override_settings(JWT_SECRET=SECRETO)
class ListaYDetalleUsuariosTests(SimpleTestCase):
    def setUp(self):
        self.revocados = ColeccionFalsa([])
        for objetivo in ("usuarios.views.mongo.usuarios", "usuarios.auth.mongo.usuarios"):
            p = mock.patch(objetivo, return_value=ColeccionFalsa(USUARIOS))
            p.start()
            self.addCleanup(p.stop)
        p = mock.patch("usuarios.auth.mongo.tokens_revocados", return_value=self.revocados)
        p.start()
        self.addCleanup(p.stop)
        self.rf = RequestFactory()

    def _get(self, vista, token_=None, **kw):
        extra = {"HTTP_AUTHORIZATION": f"Bearer {token_}"} if token_ else {}
        return vista(self.rf.get("/api/usuarios/", **extra), **kw)

    # --- acceso --------------------------------------------------------------
    def test_sin_token_da_401(self):
        self.assertEqual(self._get(views.listar_usuarios).status_code, 401)

    def test_token_con_otro_secreto_da_401(self):
        r = self._get(views.listar_usuarios, token(ID_ADMIN, secreto="x" * 40))
        self.assertEqual(r.status_code, 401)

    def test_token_vencido_da_401(self):
        r = self._get(views.listar_usuarios, token(ID_ADMIN, exp=int(time.time()) - 10))
        self.assertEqual(r.status_code, 401)

    def test_sesion_cerrada_da_401(self):
        self.revocados.docs.append({"_id": "t-1"})
        self.assertEqual(self._get(views.listar_usuarios, token(ID_ADMIN)).status_code, 401)

    def test_usuario_normal_da_403(self):
        self.assertEqual(self._get(views.listar_usuarios, token(ID_NORMAL)).status_code, 403)

    def test_admin_desactivado_da_401(self):
        self.assertEqual(self._get(views.listar_usuarios, token(ID_INACTIVO)).status_code, 401)

    # --- lista (tarea 1) -----------------------------------------------------
    def test_admin_ve_la_lista_sin_passwordhash(self):
        r = self._get(views.listar_usuarios, token(ID_ADMIN))
        self.assertEqual(r.status_code, 200)
        import json
        datos = json.loads(r.content)
        self.assertEqual(datos["total"], 3)
        self.assertEqual(
            sorted(datos["usuarios"][0]), ["activo", "correo", "id", "nombre", "rol"])
        self.assertNotIn("passwordHash", r.content.decode())
        self.assertNotIn("SECRETO", r.content.decode())

    # --- detalle (tarea 2) ---------------------------------------------------
    def test_detalle_existente(self):
        r = self._get(views.detalle_usuario, token(ID_ADMIN), usuario_id=str(ID_NORMAL))
        self.assertEqual(r.status_code, 200)
        import json
        self.assertEqual(json.loads(r.content)["correo"], "beto@tecsup.edu.pe")
        self.assertNotIn("passwordHash", r.content.decode())

    def test_detalle_inexistente_da_404(self):
        r = self._get(views.detalle_usuario, token(ID_ADMIN), usuario_id=str(ObjectId()))
        self.assertEqual(r.status_code, 404)

    def test_detalle_con_id_malformado_da_404(self):
        r = self._get(views.detalle_usuario, token(ID_ADMIN), usuario_id="no-es-un-id")
        self.assertEqual(r.status_code, 404)
