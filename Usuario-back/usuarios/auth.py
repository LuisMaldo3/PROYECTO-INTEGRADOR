"""Acceso solo para administradores.

Django NO tiene login propio: la web de administrador inicia sesion contra
Usuario-back (Spring Boot), recibe un JWT y lo envia aqui en
"Authorization: Bearer <token>". Este modulo comprueba:
  1. firma y vencimiento del token (mismo JWT_SECRET que Spring Boot),
  2. que la sesion no haya sido cerrada (coleccion tokens_revocados),
  3. que el usuario siga existiendo, activo y con rol "administrador"
     (se lee el rol ACTUAL de la base, no el del token, que puede estar viejo).
"""
from functools import wraps
import logging

import jwt
from django.conf import settings
from django.http import JsonResponse

from . import mongo
from .vistas_util import buscar_por_id

logger = logging.getLogger(__name__)


def _error(estado, codigo, mensaje):
    # Mismo formato que Usuario-back: { codigo, mensaje }
    return JsonResponse({"codigo": codigo, "mensaje": mensaje}, status=estado)


def exigir_admin(vista):
    @wraps(vista)
    def envoltura(request, *args, **kwargs):
        cabecera = request.headers.get("Authorization", "")
        if not cabecera.startswith("Bearer "):
            return _error(401, "SIN_TOKEN", "Falta el token de acceso.")
        token = cabecera[len("Bearer "):].strip()

        if len(settings.JWT_SECRET.encode("utf-8")) < 32:
            return _error(500, "CONFIG_INCOMPLETA", "JWT_SECRET no esta configurado (minimo 32 caracteres).")

        try:
            claims = jwt.decode(
                token,
                settings.JWT_SECRET,
                algorithms=[settings.JWT_ALGORITMO],
                options={"require": ["exp", "sub"]},
            )
        except jwt.ExpiredSignatureError:
            logger.info("JWT rechazado: el token ha vencido.")
            return _error(401, "TOKEN_VENCIDO", "La sesion vencio. Inicia sesion nuevamente.")
        except jwt.InvalidSignatureError:
            # No incluir el JWT ni la clave en los logs: contienen credenciales.
            logger.warning("JWT rechazado: firma no valida. Comprobar que el JWT_SECRET de Admin-back coincida exactamente con el de Usuario-back.")
            return _error(401, "TOKEN_INVALIDO", "Token invalido o vencido.")
        except jwt.PyJWTError as exc:
            logger.warning("JWT rechazado por validacion (%s).", type(exc).__name__)
            return _error(401, "TOKEN_INVALIDO", "Token invalido o vencido.")

        jti = claims.get("jti")
        if jti and mongo.tokens_revocados().find_one({"_id": jti}):
            return _error(401, "SESION_CERRADA", "La sesion fue cerrada.")

        usuario = buscar_por_id(mongo.usuarios(), claims["sub"], {"rol": 1, "activo": 1})
        if not usuario or not usuario.get("activo", False):
            return _error(401, "CUENTA_NO_VALIDA", "La cuenta no existe o esta desactivada.")
        if str(usuario.get("rol", "")).strip().lower() != settings.ROL_ADMIN:
            return _error(403, "SOLO_ADMIN", "Solo los administradores pueden entrar aqui.")

        request.admin_id = claims["sub"]
        return vista(request, *args, **kwargs)

    return envoltura
