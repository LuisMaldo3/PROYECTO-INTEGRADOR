from django.core.management.base import BaseCommand

from actividades import mongo
from actividades.preparacion import preparar, retirar_antiguas


class Command(BaseCommand):
    help = "Crea las colecciones categorias y actividades (índices y categorías iniciales). Se puede repetir."

    def handle(self, *args, **opciones):
        categorias, actividades = mongo.categorias(), mongo.actividades()
        creadas = preparar(categorias, actividades)
        retiradas = retirar_antiguas(categorias, actividades)
        self.stdout.write(self.style.SUCCESS(f"Listo. Categorías nuevas: {creadas}. Categorías antiguas retiradas: {retiradas}."))
