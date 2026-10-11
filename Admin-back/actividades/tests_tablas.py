"""Pruebas de las colecciones de actividades (US-25 · tarea 6, parte de Luis). Ejecutar:  python manage.py test

No necesitan MongoDB: usan colecciones falsas en memoria.
"""
from io import StringIO
from types import SimpleNamespace
from unittest import mock

from bson import ObjectId
from django.core.management import call_command
from django.test import SimpleTestCase

from .preparacion import CATEGORIAS_ANTIGUAS, CATEGORIAS_INICIALES, preparar, retirar_antiguas


class Cursor:
    def __init__(self, docs):
        self.docs = docs

    def __iter__(self):  # como el cursor real de pymongo: se puede recorrer sin ordenar
        return iter(self.docs)

    def sort(self, campo, sentido):
        return sorted(self.docs, key=lambda d: d.get(campo, ""), reverse=sentido < 0)


class Coleccion:
    """Colección falsa: find_one/find por igualdad, insert_one y registro de índices."""

    def __init__(self, docs=None):
        self.docs = [dict(d) for d in (docs or [])]
        self.indices = []

    def _filtrar(self, filtro):
        return [d for d in self.docs if all(d.get(k) == v for k, v in (filtro or {}).items())]

    def find_one(self, filtro, proyeccion=None):
        encontrados = self._filtrar(filtro)
        return dict(encontrados[0]) if encontrados else None

    def find(self, filtro=None, proyeccion=None):
        return Cursor([dict(d) for d in self._filtrar(filtro)])

    def insert_one(self, doc):
        doc.setdefault("_id", ObjectId())
        self.docs.append(dict(doc))
        return SimpleNamespace(inserted_id=doc["_id"])

    def count_documents(self, filtro):
        return len(self._filtrar(filtro))

    def delete_one(self, filtro):
        for d in self.docs:
            if all(d.get(k) == v for k, v in (filtro or {}).items()):
                self.docs.remove(d)
                return SimpleNamespace(deleted_count=1)
        return SimpleNamespace(deleted_count=0)

    def create_index(self, claves, **opciones):
        self.indices.append((claves, opciones))


class PrepararTests(SimpleTestCase):
    def test_crea_las_categorias_iniciales(self):
        categorias = Coleccion()
        self.assertEqual(preparar(categorias, Coleccion()), len(CATEGORIAS_INICIALES))
        self.assertEqual({d["_id"] for d in categorias.docs}, {i for i, _ in CATEGORIAS_INICIALES})
        self.assertIn({"_id": "cultural", "nombre": "Actividad cultural"}, categorias.docs)
        self.assertIn({"_id": "concurso", "nombre": "Concurso T&C Week"}, categorias.docs)

    def test_los_ids_y_nombres_no_se_repiten(self):
        ids = [i for i, _ in CATEGORIAS_INICIALES]
        nombres = [n for _, n in CATEGORIAS_INICIALES]
        self.assertEqual(len(set(ids)), len(ids))
        self.assertEqual(len(set(nombres)), len(nombres))

    def test_repetirlo_no_duplica_ni_pisa_nada(self):
        categorias = Coleccion([{"_id": "cultural", "nombre": "Cultural (editada)"}])
        preparar(categorias, Coleccion())
        self.assertEqual(preparar(categorias, Coleccion()), 0)
        self.assertEqual(len(categorias.docs), len(CATEGORIAS_INICIALES))
        self.assertEqual(categorias.find_one({"_id": "cultural"})["nombre"], "Cultural (editada)")

    def test_crea_los_indices(self):
        categorias, actividades = Coleccion(), Coleccion()
        preparar(categorias, actividades)
        self.assertEqual(categorias.indices, [("nombre", {"unique": True})])
        self.assertIn(([("creadaEn", -1)], {}), actividades.indices)
        self.assertIn(("categoriaId", {}), actividades.indices)

    def test_retira_las_categorias_antiguas_que_nadie_usa(self):
        categorias = Coleccion([{"_id": i, "nombre": i} for i in CATEGORIAS_ANTIGUAS])
        self.assertEqual(retirar_antiguas(categorias, Coleccion()), len(CATEGORIAS_ANTIGUAS))
        self.assertEqual(categorias.docs, [])

    def test_no_retira_una_categoria_antigua_que_una_actividad_usa(self):
        categorias = Coleccion([{"_id": "deportes", "nombre": "Deportes"}, {"_id": "social", "nombre": "Social"}])
        actividades = Coleccion([{"_id": ObjectId(), "nombre": "Torneo", "categoriaId": "deportes"}])
        self.assertEqual(retirar_antiguas(categorias, actividades), 1)
        self.assertEqual([c["_id"] for c in categorias.docs], ["deportes"])

    def test_retirar_no_toca_las_categorias_nuevas_y_se_puede_repetir(self):
        categorias = Coleccion([{"_id": "cultural", "nombre": "Actividad cultural"}, {"_id": "social", "nombre": "Social"}])
        self.assertEqual(retirar_antiguas(categorias, Coleccion()), 1)
        self.assertEqual(retirar_antiguas(categorias, Coleccion()), 0)
        self.assertEqual([c["_id"] for c in categorias.docs], ["cultural"])

    def test_el_comando_funciona_y_avisa(self):
        categorias, actividades = Coleccion(), Coleccion()
        salida = StringIO()
        with mock.patch("actividades.mongo.categorias", return_value=categorias), \
                mock.patch("actividades.mongo.actividades", return_value=actividades):
            call_command("preparar_actividades", stdout=salida)
        self.assertEqual(len(categorias.docs), len(CATEGORIAS_INICIALES))
        self.assertIn(f"Categorías nuevas: {len(CATEGORIAS_INICIALES)}", salida.getvalue())
        self.assertIn("Categorías antiguas retiradas: 0", salida.getvalue())
