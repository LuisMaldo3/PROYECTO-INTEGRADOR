from django.core.management.base import BaseCommand

from actividades import mongo
from actividades.preparacion import preparar


class Command(BaseCommand):
    help = "Crea las colecciones categorias y actividades (índices y categorías iniciales). Se puede repetir."

    def handle(self, *args, **opciones):
        creadas = preparar(mongo.categorias(), mongo.actividades())
        self.stdout.write(self.style.SUCCESS(f"Listo. Categorías nuevas: {creadas}."))
