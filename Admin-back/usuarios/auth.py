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

import jwt
from django.conf import settings
from django.http import JsonResponse

from . import mongo
from .vistas_util import buscar_por_id


def _error(estado, codigo, mensaje):
    # Mismo formato que Usuario-back: { codigo, mensaje }
    return JsonResponse({"codigo": codigo, "mensaje": mensaje}, status=estado)


def _autenticar(request, roles, codigo_rol, mensaje_rol):
    """Comprueba token, sesión y rol ACTUAL de la base. Devuelve (claims, usuario, None) o (None, None, error)."""
    cabecera = request.headers.get("Authorization", "")
    if not cabecera.startswith("Bearer "):
        return None, None, _error(401, "SIN_TOKEN", "Falta el token de acceso.")
    token = cabecera[len("Bearer "):].strip()

    if len(settings.JWT_SECRET.encode("utf-8")) < 32:
        return None, None, _error(500, "CONFIG_INCOMPLETA", "JWT_SECRET no esta configurado (minimo 32 caracteres).")

    try:
        claims = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=[settings.JWT_ALGORITMO],
            options={"require": ["exp", "sub"]},
        )
    except jwt.PyJWTError:
        return None, None, _error(401, "TOKEN_INVALIDO", "Token invalido o vencido.")

    jti = claims.get("jti")
    if jti and mongo.tokens_revocados().find_one({"_id": jti}):
        return None, None, _error(401, "SESION_CERRADA", "La sesion fue cerrada.")

    usuario = buscar_por_id(mongo.usuarios(), claims["sub"], {"rol": 1, "activo": 1})
    if not usuario or not usuario.get("activo", False):
        return None, None, _error(401, "CUENTA_NO_VALIDA", "La cuenta no existe o esta desactivada.")
    if str(usuario.get("rol", "")).strip().lower() not in roles:
        return None, None, _error(403, codigo_rol, mensaje_rol)
    return claims, usuario, None


def exigir_admin(vista):
    @wraps(vista)
    def envoltura(request, *args, **kwargs):
        claims, _, error = _autenticar(request, {settings.ROL_ADMIN}, "SOLO_ADMIN", "Solo los administradores pueden entrar aqui.")
        if error:
            return error
        request.admin_id = claims["sub"]
        return vista(request, *args, **kwargs)

    return envoltura


ROL_ORGANIZADOR = "organizador"


def exigir_organizador(vista):
    """Para rutas del organizador: entra un organizador o un administrador, y la vista decide qué le toca
    a cada uno (request.usuario_id y request.rol). Un estudiante nunca entra."""
    @wraps(vista)
    def envoltura(request, *args, **kwargs):
        claims, usuario, error = _autenticar(
            request, {ROL_ORGANIZADOR, settings.ROL_ADMIN}, "SOLO_ORGANIZADOR", "Solo los organizadores pueden entrar aqui.")
        if error:
            return error
        request.usuario_id = claims["sub"]
        request.rol = str(usuario.get("rol", "")).strip().lower()
        return vista(request, *args, **kwargs)

    return envoltura
