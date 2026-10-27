"""Ejecutar cada minuto con un planificador (cron/Task Scheduler)."""
from django.core.management.base import BaseCommand
from actividades.avisos import publicar_pendientes


class Command(BaseCommand):
    help = "Publica avisos programados cuyo instante UTC ya llegó."

    def handle(self, *args, **options):
        cantidad = publicar_pendientes()
        self.stdout.write(self.style.SUCCESS(f"Avisos publicados: {cantidad}"))
