package com.example.qhacemos.datos

import com.example.qhacemos.modelo.PerfilUsuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.StateFlow

private const val TABLA_PERFILES = "perfiles"

private val demoAdmin = PerfilUsuario(
    id = "demo-admin",
    nombre = "Admin Demo",
    email = "admin@qhacemos.test",
    rol = "admin"
)

private val demoUsuario = PerfilUsuario(
    id = "demo-usuario",
    nombre = "Usuario Demo",
    email = "usuario@qhacemos.test",
    rol = "usuario"
)

object GestorAutenticacion {

    private var sesionDemo: PerfilUsuario? = null

    val sessionStatus: StateFlow<SessionStatus>?
        get() = if (SupabaseCliente.estaConfigurado) {
            SupabaseCliente.cliente.auth.sessionStatus
        } else {
            null
        }

    fun usaModoDemo(): Boolean = !SupabaseCliente.estaConfigurado

    suspend fun iniciarSesion(
        email: String,
        password: String
    ): Result<PerfilUsuario> {
        val correo = email.trim()

        if (!SupabaseCliente.estaConfigurado) {
            val demo = validarUsuarioDemo(correo, password)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Modo demo: usa admin@qhacemos.test o usuario@qhacemos.test"
                    )
                )
            sesionDemo = demo
            return Result.success(demo)
        }

        return runCatching {
            SupabaseCliente.cliente.auth.signInWith(Email) {
                this.email = correo
                this.password = password
            }

            cargarPerfilActual().getOrThrow()
                ?: throw IllegalStateException("La sesion se creo, pero no se encontro el perfil.")
        }
    }

    suspend fun cerrarSesion() {
        if (!SupabaseCliente.estaConfigurado) {
            sesionDemo = null
            return
        }
        SupabaseCliente.cliente.auth.signOut()
    }

    suspend fun cargarPerfilActual(): Result<PerfilUsuario?> {
        if (!SupabaseCliente.estaConfigurado) {
            return Result.success(sesionDemo)
        }

        return runCatching {
            val usuarioActual = SupabaseCliente.cliente.auth.currentUserOrNull()
                ?: SupabaseCliente.cliente.auth.currentSessionOrNull()?.user
                ?: return@runCatching null

            val perfiles = SupabaseCliente.cliente
                .from(TABLA_PERFILES)
                .select {
                    filter {
                        eq("id", usuarioActual.id)
                    }
                }
                .decodeList<PerfilUsuario>()

            perfiles.firstOrNull() ?: PerfilUsuario(
                id = usuarioActual.id,
                nombre = usuarioActual.email?.substringBefore("@").orEmpty(),
                email = usuarioActual.email.orEmpty(),
                rol = "usuario"
            )
        }
    }

    private fun validarUsuarioDemo(
        email: String,
        password: String
    ): PerfilUsuario? {
        return when {
            email.equals("admin@qhacemos.test", ignoreCase = true) &&
                password == "Admin1234" -> demoAdmin

            email.equals("usuario@qhacemos.test", ignoreCase = true) &&
                password == "Usuario1234" -> demoUsuario

            else -> null
        }
    }
}
