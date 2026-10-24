"""Comprueba si Admin-back puede validar un JWT emitido por Usuario-back.

Uso (sin poner el JWT en argumentos ni en el historial):
    python manage.py diagnosticar_token

Solo valida firma/claims; los roles, sesiones revocadas y MongoDB se verifican
al llamar a las rutas protegidas.
"""
import getpass

import jwt
from django.conf import settings
from django.core.management.base import BaseCommand


class Command(BaseCommand):
    help = "Diagnostica firma y vigencia de un JWT sin mostrar el token ni el secreto."

    def handle(self, *args, **options):
        if len(settings.JWT_SECRET.encode("utf-8")) < 32:
            self.stderr.write(self.style.ERROR("JWT_SECRET de Admin-back no esta configurado (minimo 32 bytes)."))
            return

        token = getpass.getpass("Pega un token RECIEN GENERADO por Usuario-back (no se mostrara): ").strip()
        if not token:
            self.stderr.write(self.style.ERROR("No se recibio ningun token."))
            return

        try:
            claims = jwt.decode(
                token, settings.JWT_SECRET,
                algorithms=[settings.JWT_ALGORITMO],
                options={"require": ["exp", "sub"]},
            )
        except jwt.ExpiredSignatureError:
            self.stderr.write(self.style.WARNING(
                "La firma se valido, pero el JWT ya expiro. Cierra sesion, inicia nuevamente y repite."))
        except jwt.InvalidSignatureError:
            self.stderr.write(self.style.ERROR(
                "La firma NO coincide. Revisa el JWT_SECRET del emisor Usuario-back (Spring Boot): "
                "debe ser IDENTICO al de Admin-back, incluido su formato/codificacion. "
                "Reinicia Django tras corregir .env."))
        except jwt.ImmatureSignatureError:
            self.stderr.write(self.style.ERROR(
                "El JWT aun no es valido (nbf/iat). Revisa la hora del equipo y los servidores."))
        except jwt.PyJWTError as exc:
            self.stderr.write(self.style.ERROR(
                f"JWT rechazado: {type(exc).__name__}. Revisa algoritmo, campos sub y exp y formato del token."))
        else:
            self.stdout.write(self.style.SUCCESS("JWT VALIDO: firma, formato y expiracion correctos en Django."))
            self.stdout.write("Si /api/usuarios/ sigue fallando, revisa el rol 'administrador', "
                              "el campo activo, MongoDB y las sesiones revocadas.")
