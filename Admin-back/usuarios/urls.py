from django.urls import path

from . import views

urlpatterns = [
    path("", views.listar_usuarios, name="listar_usuarios"),
    path("<str:usuario_id>/", views.detalle_usuario, name="detalle_usuario"),
]
