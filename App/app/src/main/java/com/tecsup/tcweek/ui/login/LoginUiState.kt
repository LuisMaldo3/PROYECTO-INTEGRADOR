package com.tecsup.tcweek.ui.login

enum class MomentoIngreso {
    Esperando,
    Cargando,
    Correcto,
    Error
}

data class LoginUiState(
    val momento: MomentoIngreso = MomentoIngreso.Esperando,
    val mensaje: String? = null
)