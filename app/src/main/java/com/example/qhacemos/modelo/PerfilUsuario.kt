package com.example.qhacemos.modelo

import kotlinx.serialization.Serializable

@Serializable
data class PerfilUsuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "usuario"
) {
    val esAdmin: Boolean
        get() = rol.equals("admin", ignoreCase = true)

    val rolLegible: String
        get() = if (esAdmin) "Administrador" else "Usuario"
}
