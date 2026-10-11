from django.urls import path

from . import views

urlpatterns = [
    path("", views.listar_usuarios, name="listar_usuarios"),
    path("<str:usuario_id>/", views.detalle_usuario, name="detalle_usuario"),
    path("<str:usuario_id>/rol/", views.cambiar_rol_usuario, name="cambiar_rol_usuario"),
]
