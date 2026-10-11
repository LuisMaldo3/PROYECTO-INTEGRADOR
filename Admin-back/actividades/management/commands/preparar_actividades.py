from django.core.management.base import BaseCommand

from actividades import mongo
from actividades.preparacion import preparar
from actividades.ubicaciones import preparar_ubicaciones


class Command(BaseCommand):
    help = "Crea las colecciones categorias, ubicaciones y actividades (índices y datos iniciales). Se puede repetir."

    def handle(self, *args, **opciones):
        creadas, retiradas = preparar(mongo.categorias(), mongo.actividades())
        ubicaciones = preparar_ubicaciones(mongo.ubicaciones())
        self.stdout.write(
            self.style.SUCCESS(
                f"Listo. Categorías nuevas: {creadas}. Categorías viejas retiradas: {retiradas}. Ubicaciones nuevas: {ubicaciones}."
            )
        )
