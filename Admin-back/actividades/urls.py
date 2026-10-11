from django.urls import path

from . import views

urlpatterns = [
    path("actividades/", views.actividades, name="actividades"),
    path("categorias/", views.categorias, name="categorias"),
]
