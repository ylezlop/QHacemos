package com.example.qhacemos.datos

import com.example.qhacemos.modelo.PerfilUsuario
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GestorUsuarios {

    private val usuariosDemo = mutableListOf(
        PerfilUsuario(
            id = "demo-admin",
            nombre = "Admin Demo",
            email = "admin@qhacemos.test",
            rol = "admin"
        ),
        PerfilUsuario(
            id = "demo-usuario",
            nombre = "Usuario Demo",
            email = "usuario@qhacemos.test",
            rol = "usuario"
        )
    )

    fun validarUsuarioDemo(email: String, password: String): PerfilUsuario? {
        val credencialesValidas = when {
            email.equals("admin@qhacemos.test", ignoreCase = true) && password == "Admin1234" -> true
            email.equals("usuario@qhacemos.test", ignoreCase = true) && password == "Usuario1234" -> true
            else -> false
        }

        if (!credencialesValidas) return null

        return usuariosDemo.firstOrNull {
            it.email.equals(email, ignoreCase = true) &&
                !it.estado.equals("eliminado", ignoreCase = true)
        }
    }

    suspend fun buscarUsuarios(consulta: String): Result<List<PerfilUsuario>> = withContext(Dispatchers.IO) {
        runCatching {
            val termino = consulta.trim()

            val usuarios = if (!SupabaseCliente.estaConfigurado) {
                usuariosDemo
            } else {
                SupabaseCliente.cliente
                    .from("perfiles")
                    .select()
                    .decodeList<PerfilUsuario>()
            }

            usuarios
                .filter { !it.estado.equals("eliminado", ignoreCase = true) }
                .filter {
                    termino.isBlank() ||
                        it.nombre.contains(termino, ignoreCase = true) ||
                        it.email.contains(termino, ignoreCase = true) ||
                        it.rol.contains(termino, ignoreCase = true)
                }
                .sortedWith(compareBy<PerfilUsuario> { it.rol != "admin" }.thenBy { it.nombre.lowercase() })
        }
    }

    suspend fun actualizarUsuario(usuario: PerfilUsuario): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                val index = usuariosDemo.indexOfFirst { it.id == usuario.id }
                if (index != -1) usuariosDemo[index] = usuario
                return@runCatching index != -1
            }

            SupabaseCliente.cliente.from("perfiles").update({
                set("nombre", usuario.nombre)
                set("email", usuario.email)
                set("rol", usuario.rol)
                set("estado", usuario.estado)
            }) {
                filter { eq("id", usuario.id) }
            }
            true
        }
    }

    suspend fun suspenderUsuario(usuarioId: String): Result<Boolean> = cambiarEstado(usuarioId, "suspendido")

    suspend fun reactivarUsuario(usuarioId: String): Result<Boolean> = cambiarEstado(usuarioId, "activo")

    suspend fun eliminarUsuario(usuarioId: String): Result<Boolean> = cambiarEstado(usuarioId, "eliminado")

    private suspend fun cambiarEstado(usuarioId: String, estado: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                val index = usuariosDemo.indexOfFirst { it.id == usuarioId }
                if (index != -1) usuariosDemo[index] = usuariosDemo[index].copy(estado = estado)
                return@runCatching index != -1
            }

            SupabaseCliente.cliente.from("perfiles").update({
                set("estado", estado)
            }) {
                filter { eq("id", usuarioId) }
            }
            true
        }
    }
}
