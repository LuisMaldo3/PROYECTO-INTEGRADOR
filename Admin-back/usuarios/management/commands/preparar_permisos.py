from django.core.management.base import BaseCommand
from usuarios import mongo

class Command(BaseCommand):
    help = "Crea índices de permisos administrativos; se puede ejecutar más de una vez."

    def handle(self, *args, **options):
        mongo.permisos_usuarios().create_index("actualizadoEn")
        self.stdout.write(self.style.SUCCESS("Colección de permisos preparada."))
